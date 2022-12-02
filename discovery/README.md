# Discovery
마이크로서비스 아키텍처(MSA)로 구성되어 있는 서비스들은 각자 다른 IP와 Port를 가지고 있다.
이러한 서로 다른 서비스들의 IP와 Port 정보에 대해서 저장하고 관리할 필요가 있는데 이것을 `Service Discovery` 라고 한다.

## 목차
* **[Spring Cloud와 Spring Boot 호환성](#Spring-Cloud와-Spring-Boot-호환성)**
* **[Eureka](#Eureka)**
    * **[Eureka Server](#Eureka-Server)**
    * **[Eureka Client](#Eureka-Client)**
        * **[Random Port 지정 및 서비스를 구분하는 방법](#Random-Port-지정-및-서비스를-구분하는-방법)**
* **[애플리케이션 배포 구성](#애플리케이션-배포-구성)**
    * **[DiscoveryService 배포](#DiscoveryService-배포)**

## Spring Cloud와 Spring Boot 호환성
Spring Cloud에서 지원되는 Spring Boot의 버전이 별로도 존재하기 때문에 맞는 버전을 선택하여야 한다.

![image](https://user-images.githubusercontent.com/31242766/192144213-2d0f764a-251d-41a3-8813-4ee0c76a6232.png)

## Eureka
Eureka는 AWS와 같은 Cloud 시스템에서 서비스의 로드 밸런싱과 실패처리 등을 유연하게 가져가기 위해 
각 서비스들의 `IP` / `Port` / `InstanceId` 를 가지고 있는 REST 기반의 `미들웨어 서버`이다.
Eureka는 마이크로 서비스 기반의 아키텍처의 핵심 원칙 중 하나인 Service Discovery의 역할을 수행한다. 
MSA에서는 Service의 `IP`와 `Port`가 `일정하지 않고 지속적으로 변화한다.` 그렇기 때문에 Client에 Service의 정보를 수동으로 입력하는 것은 한계가 있다. 
Service Discovery란 이런 MSA의 상황에 적합하다.

![image](https://user-images.githubusercontent.com/31242766/192144074-44a5fe86-deb2-4891-b3f2-c8ea107f9e3b.png)


### Eureka Server
Eureka 서버를 사용하기 위해서는 Eureka 서비스 레지스트리 등록이 필요하다. @EnableEurekaServer 어노테이션 추가를 통해 레지스트리 등록 한다. 
```java
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
```
Eureka 서비스를 위한 프로퍼티 설정을 한다.
```yml
server:
  port: 8761

spring:
  application:
    name: discoveryservice

eureka:
  client:
    # true가 default 값이다.
    # Eureka 클라이언트 역할을 수행하기 위한 설정 값으로 서버 역할만 수행하기 때문에 false로 설정한다.
    register-with-eureka: false 
    # true가 default 값이다. 
    # service registry에 있는 정보를 가져올지에 대한 설정으로 false 설정 시 Eureka server를 호출하지 않게 된다. 
    # true 시에는 Eureka Client가 service registry에서 변경 사항 여부 재확인.
    fetch-registry: false 
```

### Eureka Client
#### Random Port 지정 및 서비스를 구분하는 방법
일반적인 환경에서는 지정된 PORT를 사용하여 어플리케이션을 운영한다. 
그러나 마이크로서비스 아키텍처(MSA)와 같이 LB(Load Balancer) 환경을 위해 여러 서비스를 등록하거나 자동으로 서비스가 증가되어야 하는 상황에서는 random port를 이용한다.
동일한 서비스를 여러개 실행하는 경우라면, 관리자가 매번 PORT 번호를 지정하는 것도 한계가 존재한다. 
그리고 random port 지정 시 서비스를 구분하기 위해 각각의 서비스에 instance-id로 구분하는 방법이 있다.
```java
@SpringBootApplication
@EnableDiscoveryClient
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
```
```yml
server:
  port: 0

spring:
  application:
    name: auth-service

eureka:
  instance:
    instance-id: ${spring.cloud.client.hostname}:${spring.application.instance_id:${random.value}}
  client:
    register-with-eureka: true
    fetch-registry: true
    service-url:
      defaultZone: http://127.0.0.1:8761/eureka
```

작동 확인

![image](https://user-images.githubusercontent.com/31242766/192144599-7a8042be-db31-4acb-91be-d7844e256593.png)

## 애플리케이션 배포 구성
### DiscoveryService 배포

## 출처
https://yarisong.tistory.com/41?category=1010312    
https://mangchhe.github.io/springcloud/2021/04/07/ServiceDiscoveryConcept/
