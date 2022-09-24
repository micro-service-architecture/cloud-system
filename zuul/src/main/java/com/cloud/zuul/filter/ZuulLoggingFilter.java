package com.cloud.zuul.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Component
public class ZuulLoggingFilter extends ZuulFilter {
    /**
     * 해당 필터로 들어오면 실제로 어떤 동작을 하는지 말한다.
     * @return
     * @throws ZuulException
     */
    @Override
    public Object run() throws ZuulException {
        log.info("************** printing logs: ");

        RequestContext ctx = RequestContext.getCurrentContext();
        HttpServletRequest request = ctx.getRequest();
        log.info("************** " + request.getRequestURI());
        return null;
    }
    /**
     * 사전필터(pre)인지 사후필터인지 말한다.
     * @return "pre"
     */
    @Override
    public String filterType() {
        return "pre";
    }
    /**
     * 여러개의 필터가 있을 때 순서를 말한다.
     * @return 1
     */
    @Override
    public int filterOrder() {
        return 1;
    }

    /**
     * 원하는 옵션에 따라서 해당 필터를 쓸것인지
     * 안쓸것인지 말한다.
     * @return true
     */
    @Override
    public boolean shouldFilter() {
        return true;
    }
}
