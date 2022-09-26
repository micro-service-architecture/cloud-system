# Spring Cloud Gateway
Spring Cloud Gateway 는 API Gateway로써 사용자의 요청을 받고 적절한 마이크로서비스에 라우팅해주는 서버이다.

## API Gateway
Microservice Architecture(이하 MSA)에서 언급되는 컴포넌트 중 하나이며, 모든 클라이언트 요청에 대한 end point를 통합하는 서버이다. 
마치 프록시 서버처럼 동작한다. 그리고 인증 및 권한, 모니터링, logging 등 추가적인 기능이 있다. 
모든 비지니스 로직이 하나의 서버에 존재하는 Monolithic Architecture와 달리 MSA는 도메인별 데이터를 저장하고 도메인별로 하나 이상의 서버가 따로 존재한다. 
한 서비스에 한개 이상의 서버가 존재하기 때문에 이 서비스를 사용하는 클라이언트 입장에서는 다수의 end point가 생기게 되며, end point를 변경이 일어났을때, 관리하기가 힘들다. 
그래서 MSA 환경에서 서비스에 대한 도메인인 하나로 통합할 수 있는 것이 API Gateway 이다.

## Gateway 시스템 구성하기
![image](https://user-images.githubusercontent.com/31242766/192293167-f4c85653-748c-46cc-aafe-c7c735eb8671.png)

- `Gateway`   
실제 라우팅 및 모니터링을 진행하는 Gateway 의 본체
- `Eureka`   
Client-side `Service Discovery` 를 구성하기 위한 구성 요소
- `Fallback-Server`   
상황에 맞는 대체 응답을 주기 위한 서버    
- `Admin`   
Gateway 에 설정 정보들을 입력하기 위한 Admin
- `DB`   
Admin 에서 입력된 데이터가 저장되기 위한 저장소
- `Config-Server`   
Gateway 에 설정 정보를 동적으로 변경하기 위한 구성 요소
- `Git`   
Config-Server 가 읽을 yml 파일이 저장되는 저장소

## Netty?
Spring Cloud Gateway 는 `Tomcat` 이 아닌 `Netty` 를 사용한다. API Gateway 는 모든 요청이 통과하는 곳이기 때문에 성능적인 측면이 증요하여 기존이 `1Thread / 1Request` 방식인 
Spring MVC 를 사용할 경우 성능적인 이슈가 발생할 수 있다. Netty 는 비동기 WAS 이고 `1Thread / Many Request` 방식이기 때문에 기존 방식보다 더 많은 요청을 처리할 수 있다. 

![image](https://user-images.githubusercontent.com/31242766/192295963-9b4aad9e-7d46-4686-9daa-99db3d1971dc.png)

## Route? Predicates? Filters?
Spring Cloud Gateway 에는 크게 3가지 구성 요소가 존재한다.
### Route
고유ID, 목적지 URI, Predicate, Filter 로 구성된 구성요소이다. Gateway 로 요청된 Url 의 조건이 참인 경우 매핑된 해당 경로로 매칭을 시켜준다.
### Predicate
주어진 요청이 주어진 조건을 충족하는지 테스트하는 구성요소이다. 각 요청 경로에 대해 충족하게 되는 경우 하나 이상의 조건자를 정의할 수 있다. 만약 Predicate 에 매칭되지 않는다면
`HTTP 404 not found` 를 응답한다.
### Filter
Gateway 기준으로 들어오는 요청 및 나가는 응답에 대하여 수정을 가능하게 해주는 구성요소이다.

### Java DSL Route 및 Filter 설정 예시
```java
@Configuration
public class FilterConfiguration {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(r -> r.path("/first-service/**")
                        .filters(f -> f.addRequestHeader("first-request", "first-request-header")
                                .addResponseHeader("first-response", "first-response-header"))
                        .uri("http://localhost:8081"))
                .route(r -> r.path("/second-service/**")
                        .filters(f -> f.addRequestHeader("second-request", "second-request-header")
                                .addResponseHeader("second-response", "second-response-header"))
                        .uri("http://localhost:8082"))
                .build();
    }
}
```
### yml 파일 Route 및 Filter 설정 예시
```yml
spring:
  application:
    name: apigateway-service
  cloud:
    gateway:
      default-filters:
        - name: GlobalFilter
          args:
            baseMessage: Spring Cloud Gateway Global Filter
            preLogger: true
            postLogger: true
      routes:
        - id: first-service
          uri: http://localhost:8081/
          predicates:
            - Path=/first-service/**
          filters:
#            - AddRequestHeader=first-request, first-request-header2
#            - AddResponseHeader=first-response, first-response-header2
            - CustomFilter
        - id: second-service
          uri: http://localhost:8082/
          predicates:
            - Path=/second-service/**
          filters:
#            - AddRequestHeader=second-request, second-request-header2
#            - AddResponseHeader=second-response, second-response-header2
            - name: CustomFilter
            - name: LoggingFilter
              args:
                baseMessage: Hi, there.
                preLogger: true
                postLogger: true
```
- first-service : [localhost:8081](https://github.com/multi-module-project/cloud-service/tree/master/boot-first-service)   
- second-service : [localhost:8082](https://github.com/multi-module-project/cloud-service/tree/master/boot-second-service)

#### GlobalFilter
```java
@Component
@Slf4j
public class GlobalFilter extends AbstractGatewayFilterFactory<GlobalFilter.Config> {

    public GlobalFilter() {
        super(GlobalFilter.Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        // Global Pre Filter
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            log.info("Global Filter baseMessage : {}", config.getBaseMessage());

            if(config.isPreLogger()) {
                log.info("Global Filter Start : {}", request.getId());
            }
            // Global Post Filter
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                if(config.isPostLogger()) {
                    log.info("Global Filter End : response code -> {}", response.getStatusCode());
                }
            }));
        };
    }

    @Data
    public static class Config {
        private String baseMessage;
        private boolean preLogger;
        private boolean postLogger;
    }
}
```
#### CustomFilter
```java
@Component
@Slf4j
public class CustomFilter extends AbstractGatewayFilterFactory<CustomFilter.Config> {

    public CustomFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        // Custom Pre Filter
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            log.info("Custom PRE filter : request id -> {}", request.getId());

            /**
             * Custom Post Filter
             * 기존의 스프링에서 MVC 패턴을 이용해서 ServletRequest, ServletResponse 객체들을 이용하여
             * 웹 프로그래밍 작업을 진행했지만 Spring 5.0 에서 도입된 WebFlux 라는 기능을 사용하면
             * ServletRequest, ServletResponse 을 지원하지 않는다. 그래서 이것을 사용하도록 도와주는 것이
             * ServerWebExchange 객체이다.
             *
             * Mono 라는 객체는 Spring 5.0에서 도입된 WebFlux 기능이다.
             * 기존의 동기 방식의 서버가 아니라 비동기 방식의 서버를 지원할때 단일값 전달할 때 Mono 타입으로
             * 전달하여 사용한다.
             */
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                log.info("Custom POST filter: response code -> {}", response.getStatusCode());
            }));
        };
    }

    public static class Config {
        // Put the configuration properties
    }
}
```
#### LoggingFilter
```java
@Component
@Slf4j
public class LoggingFilter extends AbstractGatewayFilterFactory<LoggingFilter.Config> {

    public LoggingFilter() {
        super(LoggingFilter.Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        GatewayFilter filter = new OrderedGatewayFilter((exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            log.info("Logging Filter baseMessage : {}", config.getBaseMessage());

            if(config.isPreLogger()) {
                log.info("Logging PRE Filter Start : {}", request.getId());
            }
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                if(config.isPostLogger()) {
                    log.info("Logging POST Filter End : response code -> {}", response.getStatusCode());
                }
            }));
            // Ordered.HIGHEST_PRECEDENCE : 가장 우선순위
            // LOWEST_PRECEDENCE : 가장 낮은순위
        }, Ordered.LOWEST_PRECEDENCE);
        return filter;
    }

    @Data
    public static class Config {
        private String baseMessage;
        private boolean preLogger;
        private boolean postLogger;
    }
}
```
