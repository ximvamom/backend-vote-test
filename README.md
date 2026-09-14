# Jajangmyeon vs Jjamppong Vote API

짜장면과 짬뽕 중 하나를 선택하는 간단한 투표 REST API입니다.

동일한 `voterId`의 중복 투표를 방지하고, 동시 요청 상황에서도 투표 데이터의 정합성을 유지하도록 구현했습니다.

## 기술 스택

* Java 21
* Spring Boot 4.1.1
* Spring Data JPA
* H2 Database
* Docker
* ngrok

## 프로젝트 구조

```text
src/main/java/com/example/api
├── ApiApplication.java
├── vote
│   ├── Vote.java
│   ├── VoteChoice.java
│   ├── VoteRepository.java
│   ├── VoteService.java
│   ├── VoteController.java
│   ├── VoteRequest.java
│   └── VoteResult.java
└── exception
    ├── ErrorResponse.java
    └── GlobalExceptionHandler.java
```

## API

### 1. Health Check

```http
GET /health
```

서버의 정상 동작 여부를 확인합니다.

### 2. 투표

```http
POST /api/vote
Content-Type: application/json
```

Request:

```json
{
  "choice": "jajang",
  "voterId": "user-123"
}
```

`choice`는 다음 두 값만 허용합니다.

* `jajang`
* `jjamppong`

정상적인 투표가 처리되면 `201 Created`를 반환합니다.

동일한 `voterId`가 이미 투표한 경우 `409 Conflict`를 반환합니다.

### 3. 투표 결과 조회

```http
GET /api/result
```

Response:

```json
{
  "jajang": 10,
  "jjamppong": 7,
  "total": 17
}
```

`total`은 실제 저장된 투표 데이터를 기준으로 계산됩니다.

---

## 중복 투표 처리

동일한 `voterId`의 중복 투표를 방지하기 위해 데이터베이스의 `UNIQUE` 제약조건을 사용했습니다.

단순히 다음과 같이 애플리케이션에서 먼저 조회한 후 저장하는 방식은 동시 요청 상황에서 race condition이 발생할 수 있습니다.

```text
1. voterId 존재 여부 조회
2. 존재하지 않으면 투표 저장
```

동시에 동일한 `voterId`로 요청이 들어오면 여러 요청이 동시에 "존재하지 않음"을 확인할 수 있기 때문입니다.

따라서 `voterId`에 DB `UNIQUE` 제약조건을 설정하여 최종적인 중복 방지를 데이터베이스에서 보장하도록 구현했습니다.

중복 `INSERT`가 발생하면 예외를 처리하여 `409 Conflict`를 반환합니다.

---

## 동시성 처리

이번 API에서는 여러 사용자가 동시에 투표하더라도 성공한 투표가 유실되지 않아야 합니다.

투표 데이터를 하나의 공유 카운터에 누적하는 방식이 아니라 **각 투표를 개별 row로 저장**하도록 설계했습니다.

따라서 서로 다른 `voterId`의 투표는 각각 독립적으로 저장되며, 별도의 공유 카운터에 대한 동시 업데이트가 발생하지 않습니다.

또한 중복 투표는 DB의 `UNIQUE` 제약조건으로 방지합니다.

### 투표 결과 집계

별도의 mutable counter를 관리하지 않고 실제 저장된 `Vote` 데이터를 `COUNT`하여 결과를 계산합니다.

이를 통해 동시 투표 과정에서 별도의 카운터 값과 실제 투표 데이터가 불일치하는 문제를 방지했습니다.

```text
jajang COUNT
+
jjamppong COUNT
=
total
```

---

## 데이터 영속성

H2의 File Database를 사용하여 투표 데이터를 파일 형태로 저장합니다.

Docker 실행 시 `/app/data` 디렉터리를 Docker Volume과 연결합니다.

```text
Docker Volume
     ↓
/app/data
     ↓
H2 File Database
```

따라서 Container를 삭제하고 다시 생성하더라도 동일한 Docker Volume을 연결하면 기존 투표 데이터를 유지할 수 있습니다.

---

## Docker

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jdk

WORKDIR /app

COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 1. 프로젝트 빌드

```bash
./gradlew clean build
```

### 2. Docker 이미지 생성

```bash
docker build -t backend-test .
```

### 3. 데이터 Volume 생성

```bash
docker volume create vote-data
```

### 4. 컨테이너 실행

```bash
docker run \
  --name vote-server \
  -p 8080:8080 \
  -v vote-data:/app/data \
  backend-test
```

서버는 다음 주소에서 실행됩니다.

```text
http://localhost:8080
```

---

## Docker 재시작 및 데이터 유지 확인

컨테이너를 삭제한 후에도 동일한 Volume을 연결하면 기존 투표 데이터를 유지할 수 있습니다.

```bash
docker stop vote-server
docker rm vote-server

docker run \
  --name vote-server \
  -p 8080:8080 \
  -v vote-data:/app/data \
  backend-test
```

재실행 후 `/api/result`를 조회하여 기존 투표 데이터가 유지되는지 확인할 수 있습니다.

기존에 투표한 `voterId`로 다시 투표하면 `409 Conflict`가 반환되어 기존 중복 투표 방지 정책도 유지됩니다.

---

## 테스트

다음 항목을 중심으로 API를 테스트했습니다.

* 정상적인 투표 요청
* 투표 결과 조회
* Health Check
* 잘못된 `choice`
* 필수 값 누락
* 잘못된 JSON 형식
* 동일 `voterId` 반복 투표
* 동일 `voterId` 동시 요청
* 서로 다른 `voterId` 100건 동시 요청
* 동시 요청 후 성공 요청 수와 결과 집계 비교
* `jajang + jjamppong = total` 검증
* 실패 및 중복 요청의 집계 반영 여부
* 서비스 재시작 후 데이터 유지
* 재시작 후 기존 `voterId` 중복 투표 차단

동시성 테스트에서는 동일한 `voterId`의 요청 중 하나만 성공하고 나머지는 중복 투표로 처리되는지 확인했으며, 서로 다른 `voterId`의 동시 투표에서는 성공한 요청 수와 최종 집계 결과가 일치하는지 확인했습니다.

---

## Public URL

ngrok을 이용하여 로컬의 8080 포트를 외부에서 접근할 수 있도록 구성했습니다.

```bash
ng
```
