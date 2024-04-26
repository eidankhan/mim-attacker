package com.mim.attacker.service;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class RequestInterceptorService {

    public void processRequest(FullHttpRequest fullRequest) {
        String uri = fullRequest.getUri();
        HttpMethod method = fullRequest.getMethod();

        // Log basic request details
        System.out.println("Intercepted " + method + " request to " + uri);

        // Handling headers
        if (!fullRequest.headers().isEmpty()) {
            fullRequest.headers().forEach(header -> System.out.println(header.getKey() + ": " + header.getValue()));
        }

        // Handle payload for POST and PUT methods
        if (HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method)) {
            ByteBuf content = fullRequest.content();
            String payload = content.toString(CharsetUtil.UTF_8);
            System.out.println("Payload: " + payload);
        }

        // Handle query parameters for GET requests
        if (HttpMethod.GET.equals(method)) {
            QueryStringDecoder decoder = new QueryStringDecoder(uri);
            Map<String, List<String>> queryParams = decoder.parameters();
            if (!queryParams.isEmpty()) {
                System.out.println("Query parameters: " + queryParams);
            }
        }
    }
}
