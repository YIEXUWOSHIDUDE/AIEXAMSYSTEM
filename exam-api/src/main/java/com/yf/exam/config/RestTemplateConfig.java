package com.yf.exam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // Configure request factory to bypass proxy for localhost
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(java.net.HttpURLConnection connection, String httpMethod) throws java.io.IOException {
                super.prepareConnection(connection, httpMethod);
                
                // Check if the URL is localhost and bypass proxy
                String host = connection.getURL().getHost();
                if ("localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host)) {
                    // Set no proxy for localhost connections
                    System.setProperty("http.nonProxyHosts", "localhost|127.0.0.1|::1");
                }
            }
        };
        
        factory.setConnectTimeout(30000); // 30 seconds
        factory.setReadTimeout(60000); // 60 seconds
        restTemplate.setRequestFactory(factory);
        
        return restTemplate;
    }
}
