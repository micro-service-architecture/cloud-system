package com.cloud.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
