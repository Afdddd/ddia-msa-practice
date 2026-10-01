# 4장 실습 1: gRPC(Protobuf) 스키마 호환성

## 목적

서비스가 독립 배포되면 신/구 버전이 동시에 돌아간다.
proto 스키마를 바꾸고 **한쪽 서비스만** 재배포했을 때 무엇이 버티고 무엇이 깨지는지 직접 확인한다.

- 후방 호환(backward): 신버전 코드가 구버전 데이터를 읽을 수 있는가
- 전방 호환(forward): 구버전 코드가 신버전 데이터를 읽을 수 있는가

## 구성

```
curl ─HTTP─▶ order-service (8081) ─gRPC─▶ product-service (9090) ─▶ MySQL
             ProductServiceBlockingStub     ProductGrpcService
```

실험 대상 메시지 (시작 상태):

```proto
message GetProductResponse {
  int64  id    = 1;
  string name  = 2;
  int64  price = 3;
  int32  stock = 4;
}
```

## 실험 방법 (신/구 버전 공존 만들기)

1. 두 서비스를 같은 proto로 `bootJar` 후 `java -jar`로 실행
2. proto 수정
3. **재배포할 서비스만** `./gradlew :<service>:bootJar` 후 그 서비스만 재시작
4. curl로 호출, order-service 로그 확인

bootJar는 proto 클래스를 jar 안에 복사해서 들고 있으므로, 재시작하지 않은 서비스는 구버전 스키마로 계속 동작한다.

## 결과

| # | 변경 | 신버전 쪽 | 결과 |
|---|---|---|---|
| A | `string description = 5` 추가 | 서버만 | 정상. 구버전 클라이언트는 모르는 태그 5를 건너뜀 |
| B | `string description = 5` 추가 | 클라이언트만 | 정상. `getDescription()`은 기본값 `""` |
| C | `stock = 4` 삭제 후 `int32 discount = 4` 추가 | 서버만 | **에러 없이 오해석.** 서버가 보낸 discount `10`을 구버전 클라이언트가 `product stock: 10`으로 읽음 |
| D | `reserved 4; reserved "stock";` 상태에서 태그 4 재사용 | - | protoc 빌드 실패. 재사용 자체가 차단됨 |
| E | discount를 새 태그 `6`으로 이동 | 서버만 | TODO: 구버전 클라이언트의 stock 값 기록 |

### A/B: 필드 추가는 양방향 호환

- 바이트에는 필드 이름이 없고 **태그 번호 + 와이어 타입**만 실린다
- 모르는 태그 → 와이어 타입으로 길이를 알 수 있으니 건너뜀 (전방 호환)
- 없는 태그 → 타입별 기본값으로 채움 (후방 호환)
- 주의: proto3는 기본값(`0`, `""`)을 아예 전송하지 않는다. 그래서 받는 쪽은 **"안 보냄"과 "기본값을 보냄"을 구분할 수 없다**
  - 실험 중 `product not found: 0`이 났을 때도 같은 이유로 원인 파악이 헷갈렸다
  - 구분이 필요하면 `optional` 키워드 → `hasXxx()` 생성

### C: 태그 번호 재사용은 조용히 깨진다

- stock과 discount 모두 태그 4, 와이어 타입(varint)도 같음 → 디코딩은 완벽히 "성공"
- 로그/모니터링에 아무것도 안 잡힌 채 잘못된 재고 값으로 로직이 진행됨
- 같은 코드베이스에선 `setStock()`이 사라져 컴파일 에러로 드러나지만, **이미 배포된 다른 서비스에는 컴파일러가 개입할 수 없다**

### D: `reserved`

- 삭제한 필드의 번호/이름을 "사용 금지"로 남기는 묘비
- 런타임의 조용한 오해석을 **빌드 타임 에러**로 바꿔줌
- 이름도 막는 이유: protobuf JSON 변환 등 이름 기반 경로 보호
- 규칙: **필드를 삭제하면 번호와 이름을 반드시 `reserved`에 추가**

### E: reserved로도 해결 안 되는 것

- 오해석은 막아도, 구버전 클라이언트가 아직 쓰는 필드를 지우면 그 쪽은 기본값을 읽게 된다
- 재고 필드라면 `0` = "품절"로 해석될 수 있다 → 의미상 장애
- DDIA: 필드 삭제는 선택적(optional) 필드만 가능. 실무에선 모든 소비자가 해당 필드를 안 쓰게 된 뒤에 삭제

## 실험 중 겪은 문제 (실무 교훈)

### 1. IDE 실행 시 `NoSuchFieldError`

```
java.lang.NoSuchFieldError: Class org.core.proto.product.GetProductResponse
does not have member field 'java.lang.Object description_'
```

- IDE 실행은 두 서비스가 **같은 디스크 경로**(`proto/build/classes`)의 proto 클래스를 읽는다
- product-service 재시작 시 proto 재빌드 → 파일이 신버전으로 덮어써짐
- order-service JVM은 이미 로드한 `GetProductResponse`(구버전)와, 나중에 지연 로드한 descriptor 클래스(신버전)가 **한 프로세스 안에서 섞임**
- 스택트레이스의 `~[main/:na]`가 디렉토리에서 로드됐다는 단서
- 교훈: protobuf는 **바이트 수준 호환**을 보장할 뿐, 한 JVM 안에서 서로 다른 버전의 생성 코드가 섞이는 **바이너리(클래스) 호환**은 보장하지 않는다

### 2. 실행 중인 jar 재빌드 시 `NoClassDefFoundError`

```
Exception in thread "grpc-nio-boss-ELG-1-1" java.lang.NoClassDefFoundError:
io/netty/util/concurrent/DefaultPromise$1
```

- JVM은 jar를 열어둔 채 클래스를 필요할 때 읽는다
- 실행 중인 product-service의 jar를 `bootJar`로 다시 만들어서 파일 내용이 바뀜 → 이후 처음 필요해진 클래스를 못 찾음
- 교훈: 실행 중인 서비스의 산출물은 건드리지 않는다. 재배포할 서비스만 빌드

## 정리 (MSA 관점)

- 실제 MSA에선 각 서비스가 서로 다른 버전의 스키마로 빌드되어 있는 게 **기본 상태**
  - 흔한 방식: proto 전용 레포 → 버전 붙은 jar(`product-proto:1.3.0`)로 배포 → 서비스별로 버전 지정
  - 이 프로젝트(모노레포 + `project(':proto')`)는 한 번에 같이 빌드되므로 버전 불일치를 **일부러** 만들어야 했다
- 안전한 변경: 새 태그 번호로 필드 추가
- 위험한 변경: 태그 재사용(조용한 오해석), 사용 중인 필드 삭제(의미상 장애)
- 스키마 변경 규칙은 사람의 주의가 아니라 도구로 강제해야 한다: `reserved`, CI의 호환성 검사(`buf breaking` 등)
