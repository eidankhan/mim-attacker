package com.mim.attacker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.http.*;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InterceptorService {
    private final ObjectMapper objectMapper;
    private final LoggingService loggingService;
    public InterceptorService(LoggingService loggingService) {
        this.objectMapper = new ObjectMapper();
        this.loggingService = loggingService;
    }

    public void processRequest(FullHttpRequest fullRequest) {
        String method = fullRequest.method().name();
        String uri = fullRequest.uri();
        String headers = fullRequest.headers().entries().stream()
                .map(header -> header.getKey() + ":" + header.getValue())
                .collect(Collectors.joining("; "));

        String payload = "";
        if (HttpMethod.POST.equals(fullRequest.method()) || HttpMethod.PUT.equals(fullRequest.method())) {
            ByteBuf content = fullRequest.content();
            payload = content.toString(CharsetUtil.UTF_8);
        }
        System.out.println("Writing intercepted request to csv file");
        loggingService.logToCsv("Request", method, uri, "-", headers, payload);
    }

    public void processResponse(HttpResponse response) {
        String status = response.status().toString();
        String headers = response.headers().entries().stream()
                .map(header -> header.getKey() + ":" + header.getValue())
                .collect(Collectors.joining("; "));

        String content = "";
        if (response instanceof FullHttpResponse) {
            FullHttpResponse fullResponse = (FullHttpResponse) response;
            ByteBuf buffer = fullResponse.content();
            if (buffer.isReadable()) {
                content = buffer.toString(CharsetUtil.UTF_8);
            }
        }
        System.out.println("Writing intercepted response to csv file");
        loggingService.logToCsv("Response", "-", "-", status, headers, content);
    }

    public void processRequestAsJSON(FullHttpRequest fullRequest)  {
        ObjectNode requestJson = objectMapper.createObjectNode();
        requestJson.put("type", "request");
        requestJson.put("uri", fullRequest.uri());
        requestJson.put("method", fullRequest.method().name());

        ObjectNode headersJson = objectMapper.createObjectNode();
        fullRequest.headers().forEach(header -> headersJson.put(header.getKey(), header.getValue()));
        requestJson.set("headers", headersJson);

        if (fullRequest.method().equals(HttpMethod.POST) || fullRequest.method().equals(HttpMethod.PUT)) {
            ByteBuf content = fullRequest.content();
            requestJson.put("payload", content.toString(CharsetUtil.UTF_8));
        }

        try {
            loggingService.logToJsonFile(requestJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void processResponseAsJSON(HttpResponse response) {
        ObjectNode responseJson = objectMapper.createObjectNode();
        responseJson.put("type", "response");
        responseJson.put("status", response.status().toString());

        ObjectNode headersJson = objectMapper.createObjectNode();
        response.headers().forEach(header -> headersJson.put(header.getKey(), header.getValue()));
        responseJson.set("headers", headersJson);

        if (response instanceof FullHttpResponse) {
            FullHttpResponse fullResponse = (FullHttpResponse) response;
            ByteBuf content = fullResponse.content();
            if (content.isReadable()) {
                responseJson.put("content", content.toString(CharsetUtil.UTF_8));
            }
        }

        try {
            loggingService.logToJsonFile(responseJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
