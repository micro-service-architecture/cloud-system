# Zuul
Netflix에서 제공하는 API Gateway 또는 API Service 기술이다. 마이크로서비스 아키텍쳐에서 여러 클라이언트 요청을 적절한 서비스로 프록시 및 라우팅하기 위한 서비스이다. 
Zuul은 내부적으로 `Eureka 서버`를 사용하고, `부하 분산` 을 위해 `Ribbon` 을 사용한다. Zuul 또한 `Eureka Client` 이다.   

![image](https://user-images.githubusercontent.com/31242766/192134466-b19921b7-6734-4188-8def-0c114df93b54.png)

`Zuul` 은 2018년 12월부터 기능 개선 없이 유지하는 `Maintenance Mode` 로 변경되었다. `Spring boot 2.4.X` 부터 `Zuul` 을 제공하지 않는다.

![image](https://user-images.githubusercontent.com/31242766/192134632-79fc686e-a285-4740-ada1-1e4391db9754.png)

## API Gateway
Microservice Architecture(이하 MSA)에서 언급되는 컴포넌트 중 하나이며, 모든 클라이언트 요청에 대한 end point를 통합하는 서버이다. 마치 프록시 서버처럼 동작한다. 
그리고 인증 및 권한, 모니터링, logging 등 추가적인 기능이 있다. 모든 비지니스 로직이 하나의 서버에 존재하는 Monolithic Architecture와 달리 
MSA는 도메인별 데이터를 저장하고 도메인별로 하나 이상의 서버가 따로 존재한다. 한 서비스에 한개 이상의 서버가 존재하기 때문에 이 서비스를 사용하는 클라이언트
입장에서는 다수의 end point가 생기게 되며, end point를 변경이 일어났을때, 관리하기가 힘들다. 
그래서 MSA 환경에서 서비스에 대한 도메인인 하나로 통합할 수 있는 것이 API Gateway 이다.

## Zuul Filter
`Zuul Filter` 는 크게 4가지 `Filter` 로 나누어 진다.
1. PRE Filter - 라우팅전에 실행되며 필터이다. 주로 `logging`, `인증` 등이 `pre Filter` 에서 이루어진다.
2. ROUTING Filter - 요청에 대한 라우팅을 다루는 필터이다. Apache httpclient 를 사용하여 정해진 `Url` 로 보낼 수 있고 `Netflix Ribbon` 을 사용하여 동적으로 라우팅할 수도 있다.
3. POST Filter - 라우팅 후에 실행되는 필터이다. `response` 에 `HTTP header` 를 추가하거나, response 에 대한 `응답속도`, `Status Code` 등 응답에 대한 
statistics and metrics을 수집한다.
4. ERROR Filter - 에러 발생시 실행되는 필터이다.

![image](https://user-images.githubusercontent.com/31242766/192135400-3610ea79-6c12-4de1-b276-9f3b734c822f.png)

위와 같이 요청이 들어면 `PRE Filter`를 실행하고, `ROUTING Filter` 에 의해 원하는 서버로 요청을 보낸다. 원하는 서버에서 응답이 오면 `POST Filter` 를 실행시킨다.

## 출처
https://techblog.woowahan.com/2523/    
https://velog.io/@jkijki12/Zuul%EC%9D%B4%EB%9E%80    
https://happycloud-lee.tistory.com/213
