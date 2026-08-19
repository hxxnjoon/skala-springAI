# ch02_layered — 교재 1장 · 계층 구조 (API 키 불필요)

Controller · Service · Repository · Mapper. AI 를 전혀 쓰지 않는다 — **키도, 비용도 없다.**

**혼자 돈다.** 이 폴더만 열어 `./gradlew bootRun` 하면 끝이고, 다른 장 폴더를 참조하지 않는다.

---

## 실행

```bash
./gradlew bootRun          # http://localhost:8080
```

- Swagger UI — <http://localhost:8080/swagger-ui.html> ([Try it out] 으로 curl 없이 호출)
- 포트를 바꾸려면 `./gradlew bootRun --args='--server.port=8081'`

> **API 키가 필요 없다.** 모델을 부르지 않으므로 비용도 들지 않는다.

---

## 무엇이 들어 있나

| 파일 | 무엇을 보나 |
| --- | --- |
| `web/OrderController.java` | `@RestController` · `@Valid` — 검증은 여기서 끝낸다 |
| `web/OrderSearchController.java` | Mapper 를 쓰는 검색·집계 + Swagger 문서화 본보기 |
| `web/OrderExceptionHandler.java` | 예외 → 응답 변환을 한곳에서 |
| `service/OrderService.java` | `@Transactional(readOnly)` — Repository 사용 |
| `service/OrderSearchService.java` | 조회 전용 — Mapper 사용 |
| `repository/OrderRepository.java` | `JpaRepository` — 메서드 이름이 곧 쿼리 |
| `mapper/OrderMapper.java` | `@Mapper`(MyBatis) — SQL 을 직접(동적 조건·집계) |
| `mapper/OrderDtoMapper.java` | DTO 변환 담당 — 이름은 같아도 하는 일이 다르다 |
| `domain/Order.java` | `@Entity` — 밖으로 나가지 않는다 |
| `dto/OrderDtos.java` | 요청·응답 record + 검증 규칙 + `@Schema` |
| `resources/mapper/OrderMapper.xml` | 동적 SQL — `<where>` · `<if>` · `<choose>` |

핵심 규칙 네 가지가 코드로 드러나 있다.

1. **위에서 아래로만 호출** — 컨트롤러는 Repository·Mapper 를 모른다. 서비스만 안다.
2. **권한 조건은 쿼리 안에** — `findByIdAndOwnerId()`, XML 에서는 `<if>` **밖**에.
   조건에 따라 빠질 수 있는 자리에 두면 언젠가 빠진다.
3. **엔티티는 밖으로 안 나간다** — `OrderResponse` 에는 `ownerId`·`cost` 가 아예 없다.
4. **입구가 둘이어도 출구는 하나** — 엔티티에서 왔든 Mapper 가 읽은 row 에서 왔든
   `OrderDtoMapper` 를 지나 같은 응답이 된다.

#### Repository 와 Mapper — 같은 자리, 다른 방식

| | JPA Repository | MyBatis Mapper |
| --- | --- | --- |
| SQL | 메서드 이름·JPQL 로 생성 | 내가 직접 쓴다 |
| 이 예제에서 맡은 일 | 단건 조회 · 목록 · **생성(쓰기)** | **동적 조건 검색 · 집계 · 건수** |
| 돌려주는 것 | 엔티티(영속 상태) | 조회 전용 `OrderRow`·`OrderStatistic` |
| 테스트 | `@DataJpaTest` | `@MybatisTest` + `@Sql` |

`OrderMapper.xml` 의 `statistics` 쿼리에 붙은 `cast(status as varchar)` 는
**JPA 가 만든 스키마와 MyBatis 가 만나는 실제 마찰**의 예다 —
`@Enumerated(STRING)` 필드가 H2 에서 ENUM 컬럼이 되기 때문이다. 주석으로 설명해 두었다.

---

## 실행해 보기

```bash
# 본인 주문 — 200
curl 'localhost:8080/ch02/orders/12345?userId=user1'
#   {"orderId":"12345","item":"무선 이어폰","status":"배송중",...}
#   ownerId·cost 는 응답에 없다

# 남의 주문 — 404 (99999 는 user2 의 주문)
curl 'localhost:8080/ch02/orders/99999?userId=user1'
#   {"message":"주문을 찾을 수 없습니다.", ...}

# 목록 · 생성
curl 'localhost:8080/ch02/orders?userId=user1'
curl -X POST 'localhost:8080/ch02/orders?userId=user1' \
     -H 'Content-Type: application/json' \
     -d '{"item":"모니터암","quantity":2,"memo":"빠른 배송"}'      # 201

# @Valid 검증 실패 — 400
curl -X POST 'localhost:8080/ch02/orders?userId=user1' \
     -H 'Content-Type: application/json' -d '{"item":"","quantity":0}'
#   {"message":"item: 상품명은 필수입니다, quantity: 수량은 1개 이상이어야 합니다"}

# 동적 조건 검색 — 조건을 넣었다 뺐다 하면 where 절이 달라진다
curl 'localhost:8080/ch02/orders/search?userId=user1'
curl 'localhost:8080/ch02/orders/search?userId=user1&status=SHIPPING'
curl 'localhost:8080/ch02/orders/search?userId=user1&keyword=키보드&sort=eta'

# 정렬 값은 화이트리스트 — 이상한 값을 넣어도 기본 정렬로 되돌아간다
curl 'localhost:8080/ch02/orders/search?userId=user1&sort=id;drop%20table%20orders'

# 집계 — SQL 한 번으로 상태별 건수·합계
curl 'localhost:8080/ch02/orders/statistics?userId=user1'
```

**콘솔에 찍히는 SQL 을 꼭 보라.** `show-sql` 을 켜 두었다.

```sql
select ... from orders o1_0
 where o1_0.id=? and o1_0.owner_id=?      -- 권한 조건이 쿼리 안에 있다
```

`findById()` 로 꺼낸 뒤 자바에서 소유자를 비교했다면, 호출부 한 군데만 빠뜨려도
남의 데이터가 나간다. **조건이 쿼리에 있으면 빠뜨릴 여지 자체가 없다.**

DB 를 직접 들여다보려면 <http://localhost:8080/h2-console>
(JDBC URL `jdbc:h2:mem:ch02`, 사용자 `sa`, 비밀번호 없음).

### 테스트

```bash
./gradlew test        # 13건
```

- `OrderServiceLayerTest` — `@DataJpaTest` + `@Import(OrderService.class)` 로
  **Service + Repository 계층만** 띄운다. 웹도 AI 도 로딩되지 않아 빠르다.
- `OrderMapperTest` — `@MybatisTest` + `@Sql` 로 **Mapper 만** 띄운다.
  동적 조건이 붙었다 빠지는지, 정렬 화이트리스트가 도는지, 소유자 조건이
  어떤 경우에도 빠지지 않는지를 SQL 수준에서 검증한다.

같은 요청이 **`http/ch02_layered.http`** 에 들어 있다 — VS Code 의 REST Client 확장에서
각 요청 위의 **[Send Request]** 를 누르면 curl 없이 응답을 볼 수 있다.

---

## 참고
- 원래 `ch13_service` 의 `AiExceptionHandler` 가 처리하던 `OrderNotFoundException`·`@Valid` 응답을 이 프로젝트의 `web/OrderExceptionHandler` 로 옮겼다. Swagger 표지 정보는 `OpenApiConfig` 에 있다.

- 버전은 Spring Boot 3.5.16 · Spring AI 1.1.8(`build.gradle` 의 `springAiVersion` 한 줄).
- 다른 장은 `../chNN_*/` 에, 실습 과제는 `../../01_*` ~ `../../17_*` 에 있다.
