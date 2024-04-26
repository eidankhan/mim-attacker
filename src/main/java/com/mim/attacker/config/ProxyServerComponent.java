package com.mim.attacker.config;

import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.littleshoot.proxy.extras.SelfSignedMitmManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

@Component
public class ProxyServerComponent implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting ProxyServer");
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
                                System.out.println("Request intercepted: " + originalRequest.getUri());
                                return null;
                            }

                            @Override
                            public HttpObject serverToProxyResponse(HttpObject httpObject) {
                                // Modify the response here as needed
                                if (httpObject instanceof HttpResponse) {
                                    HttpResponse response = (HttpResponse) httpObject;
                                    System.out.println("Response intercepted: " + response.getStatus());
                                }
                                return httpObject;
                            }
                        };
                    }
                })
                .start();  // Start the proxy server
    }
}
