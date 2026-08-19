package com.skala.ch02;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

/**
 * API 문서(OpenAPI 3) 설정 — Swagger UI 로 바로 호출해 볼 수 있게 한다.
 *
 * <ul>
 *   <li>문서(JSON) : <a href="http://localhost:8080/v3/api-docs">/v3/api-docs</a></li>
 *   <li>화면        : <a href="http://localhost:8080/swagger-ui.html">/swagger-ui.html</a></li>
 * </ul>
 *
 * <h3>왜 문서를 코드에서 만드나</h3>
 * 따로 쓴 문서는 반드시 코드와 어긋난다. 컨트롤러 시그니처에서 생성하면
 * <b>거짓말을 할 수 없다</b> — 파라미터를 바꾸면 문서가 같이 바뀐다.
 *
 * <p>여기에 더해 Swagger UI 는 <b>실행 가능한 문서</b>다. curl 을 몰라도
 * [Try it out] 으로 요청을 보낼 수 있어, 프런트·QA·기획과 같은 화면을 보고 이야기하게 된다.
 *
 * <h3>주의 — 운영에서는 열어 두지 않는다</h3>
 * 문서는 곧 공격 지도다. 운영 프로파일에서는 {@code springdoc.api-docs.enabled=false} 로
 * 끄거나, 사내망·인증 뒤에 둔다. 이 프로젝트는 학습용이라 열어 두었다.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ch02OpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("1장 — 계층 구조 예제 API")
                        .version("1.0.0")
                        .description("""
                                교재 『SpringAI 이해 및 활용 — 3일 완성』 1장 예제.

                                Controller · Service · Repository · Mapper 가 실제로 동작한다.
                                **AI 를 부르지 않으므로 API 키가 없어도 되고 비용도 들지 않는다.**
                                """)
                        .contact(new Contact().name("SKALA SpringAI 과정")))
                .addServersItem(new Server().url("http://localhost:8080").description("로컬 실행"));
    }
}
