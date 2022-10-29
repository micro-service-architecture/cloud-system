# Spring Cloud Gateway
Spring Cloud Gateway 는 API Gateway로써 사용자의 요청을 받고 적절한 마이크로서비스에 라우팅해주는 서버이다.

## 목차
* **[API Gateway](#API-Gateway)**
* **[Spring Cloud Gateway 시스템 구성 예시](#Spring-Cloud-Gateway-시스템-구성-예시)**
* **[Spring Cloud Gateway 동작 방식](#Spring-Cloud-Gateway-동작-방식)**
* **[Netty?](#Netty?)**
* **[Route? Predicates? Filters?](#Route?-Predicates?-Filters?)**
    * **[Route](#Route)**
    * **[Predicate](#Predicate)**
    * **[Filter](#Filter)**
    * **[Java DSL Route 및 Filter 설정 예시](#Java-DSL-Route-및-Filter-설정-예시)**
    * **[yml 파일 Route 및 Filter 설정 예시](#yml-파일-Route-및-Filter-설정-예시)**
        * **[GlobalFilter](#GlobalFilter)**
        * **[CustomFilter](#CustomFilter)**
        * **[LoggingFilter](#LoggingFilter)**
        * **[yml 파일 Route 및 Filter 흐름도](#yml-파일-Route-및-Filter-흐름도)**
* **[Load Balancer](#Load-Balancer)**
    * **[Scale-out의 장점](#Scale-out의-장점)**
    * **[종류](#종류)**
    * **[Load Balancer 선택 기준](#Load-Balancer-선택-기준)**
    * **[서비스별 uri 를 통한 Load Balancing](#서비스별-uri-를-통한-Load-Balancing)**

## API Gateway
Microservice Architecture(이하 MSA)에서 언급되는 컴포넌트 중 하나이며, 모든 클라이언트 요청에 대한 end point를 통합하는 서버이다. 
마치 프록시 서버처럼 동작한다. 그리고 인증 및 권한, 모니터링, logging 등 추가적인 기능이 있다. 
모든 비지니스 로직이 하나의 서버에 존재하는 Monolithic Architecture와 달리 MSA는 도메인별 데이터를 저장하고 도메인별로 하나 이상의 서버가 따로 존재한다. 
한 서비스에 한개 이상의 서버가 존재하기 때문에 이 서비스를 사용하는 클라이언트 입장에서는 다수의 end point가 생기게 되며, end point를 변경이 일어났을때, 관리하기가 힘들다. 
그래서 MSA 환경에서 서비스에 대한 도메인인 하나로 통합할 수 있는 것이 API Gateway 이다.

## Spring Cloud Gateway 시스템 구성 예시
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

## Spring Cloud Gateway 동작 방식
![image](https://user-images.githubusercontent.com/31242766/192300657-9cf5f6a2-5075-4e4d-88b5-8e76f824181e.png)

클라이언트는 Spring Cloud Gateway 로 요청을 한다. `Gateway Handler Mapping` 이 `Route` 의 조건에 일치하는 요청이라고 판단하면, 해당 `Route` 로 보내준다.
`Gateway Web Handler` 는 요청과 관련된 `Filter` 들을 통해 요청을 보낸다. `Filter` 는 요청의 기능에 따라 proxy 요청이 보내지기 전/후로 로직을 실행한다.

## Netty?
Spring Cloud Gateway 는 `Tomcat` 이 아닌 `Netty` 를 사용한다. API Gateway 는 모든 요청이 통과하는 곳이기 때문에 성능적인 측면이 증요하여 기존이 `1Thread / 1Request` 방식인 
Spring MVC 를 사용할 경우 성능적인 이슈가 발생할 수 있다. Netty 는 비동기 WAS 이고 `1Thread / Many Request` 방식이기 때문에 기존 방식보다 더 많은 요청을 처리할 수 있다. 

![image](https://user-images.githubusercontent.com/31242766/192301502-ee77c546-224a-469f-ae6e-b58c2d378f54.png)

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
#### yml 파일 Route 및 Filter 흐름도
![image](https://user-images.githubusercontent.com/31242766/192299716-ec24574a-0ddf-4be9-a5ea-cb2feec423aa.png)

## Load Balancer
부하분산 또는 로드 밸런싱은 컴퓨터 네트워크의 일종으로 중앙처리장치 혹은 저장장치와 같은 컴퓨터 자원들에게 작업을 나누는 것을 의미한다. 서버에 가해지는 부하(=로드)를 분산(=밸런싱)해주는 장치 또는 기술이다. 사업의 규모가 확장되고 클라이언트의 수가 늘어나게 되면 기존 서버만으로는 정상적인 서비스가 불가능하게 되는데, 이런 증가한 트래픽에 대처할 수 있는 방법은 크게 두 가지이다.

- `Scale-up` : Server가 더 빠르게 동작하기 위해 하드웨어의 성능을 올리는 방법.
- `Scale-out` : 하나의 Server 보다는 여러 대의 Server가 나눠서 일을 하는 방법.

### Scale-out의 장점
- 하드웨어 향상하는 비용보다 서버 한대 추가 비용이 적다.
- 여러 대의 Server 덕분에 무중단 서비스를 제공할 수 있다.

여러 대의 Server에게 균등하게 트래픽을 분산시켜주는 역할을 하는 것이 `Load Balancer` 이다.

### 종류
#### L2
Mac 주소를 바탕으로 Load Balancing 한다.
- Mac 주소란? 이더넷 하드웨어 주소라고도 하며 대부분의 네트워크 어댑터(랜카드 등)에 들어가 있는 고유 식별자이다.
#### L3
IP주소를 바탕으로 Load Balancing 한다.
#### L4
- Transport Layer(IP와 Port) Level에서 Load Balancing을 한다.
- TCP, UDP
#### L7
- Application Layer(사용자의 Request) Level에서 Load Balancing을 한다.
- URL 또는 HTTP 헤더에서 부하 분산이 가능하다.
- HTTP, HTTPS, FTP
#### HTTP
![image](https://user-images.githubusercontent.com/31242766/193258565-6494fb55-190d-44b9-8a33-f569f5ad9a27.png)

- `X-Forwarded-For` : HTTP 또는 HTTPS 로드 밸런서를 사용할 때 클라이언트의 IP 주소를 식별하는 데 도움을 준다.
- `X-Forwarded-Proto` : 클라이언트가 로드 밸런서 연결에 사용한 프로토콜(HTTP 또는 HTTPS)을 식별하는 데 도움을 준다.
- `X-Forwarded-Port` : 클라이언트가 로드 밸런서 연결에 사용한 포트를 식별하는 데 도움을 준다.

### Load Balancer 선택 기준
- `Round Robin` : 단순히 Round Robin으로 분산하는 방식이다.
  - Round Robin이란? 클라이언트로부터 받은 요청을 로드밸런싱 대상 서버에 순서대로 할당받는 방식이다. 첫 번째 요청은 첫 번째 서버, 두 번째 요청은 두 번째 서버, 세 번째 요청은 세 번째 서버에 할당합니다. 로드밸러닝 대상 서버의 성능이 동일하고 처리 시간이 짧은 애플리케이션의 경우, 균등하게 분산이 이루어지기 때문에 이 방식을 사용한다.
- `Least Connections` : 연결 수가 가장 적은 서버에 네트워크 연결방향을 정합니다. 동적인 분산 알고리즘으로 각 서버에 대한 현재 연결 수를 동적으로 카운트할 수 있고, 동적으로 변하는 요청에 대한 부하를 분산시킬 수 있습니다.

### 서비스별 uri 를 통한 Load Balancing
서비스별 uri 를 `lb://{SpringBoot Application 이름}` 으로 변경해주면 로드 밸런싱이 적용된다.

![image](https://user-images.githubusercontent.com/31242766/193260555-c1fb8012-3093-40fb-a867-a722891d3999.png)

```yml
  ...
  routes:
    - id: first-service
      uri: lb://MY-FIRST-SERVICE
      predicates:
        - Path=/first-service/**
      filters:
#       - AddRequestHeader=first-request, first-request-header2
#       - AddResponseHeader=first-response, first-response-header2
        - CustomFilter
    - id: second-service
      uri: lb://MY-SECOND-SERVICE
      predicates:
        - Path=/second-service/**
      filters:
#       - AddRequestHeader=second-request, second-request-header2
#       - AddResponseHeader=second-response, second-response-header2
        - name: CustomFilter
        - name: LoggingFilter
          args:
            baseMessage: Hi, there.
            preLogger: true
            postLogger: true
  ...
```

#### Routes 정보 변경
```yml
  ...
  routes:
#   - id: user-service
#     uri: lb://USER-SERVICE
#     predicates:
#       - Path=/user-service/**
    - id: user-service
      uri: lb://USER-SERVICE
      predicates:
        - Path=/user-service/login
        - Method=POST
      filters:
        - RemoveRequestHeader=Cookie
        - RewritePath=/user-service/(?<segment>.*), /${segment}
    - id: user-service
      uri: lb://USER-SERVICE
      predicates:
        - Path=/user-service/users
        - Method=POST
      filters:
        - RemoveRequestHeader=Cookie
        - RewritePath=/user-service/(?<segment>.*), /${segment}
    - id: user-service
      uri: lb://USER-SERVICE
      predicates:
        - Path=/user-service/**
        - Method=GET
      filters:
        - RemoveRequestHeader=Cookie
        - RewritePath=/user-service/(?<segment>.*), /${segment}
        - AuthorizationHeaderFilter
  ...
```
#### RemoveRequestHeader
GET, POST를 구분하지 않고, Request 헤더에 저장된 값을 제거하기 위해서 해당 코드를 추가한다.

![image](https://user-images.githubusercontent.com/31242766/194877882-40541752-16b6-4c77-8631-c757383bca5a.png)

#### RewritePath
RewritePath는 강제로 Path를 다시 작성한다.


## 출처
https://saramin.github.io/2022-01-20-spring-cloud-gateway-api-gateway/   
https://ooeunz.tistory.com/109    
https://nesoy.github.io/articles/2018-06/Load-Balancer    
https://dev.classmethod.jp/articles/load-balancing-types-and-algorithm/    

