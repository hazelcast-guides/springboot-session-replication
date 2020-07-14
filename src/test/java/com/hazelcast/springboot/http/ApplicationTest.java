package com.hazelcast.springboot.http;

import org.junit.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertTrue;

public class ApplicationTest {

    @Test
    public void testSessionReplication() throws Exception {
        // given
        String port1 = startApplication();
        String port2 = startApplication();

        // when
        ResponseEntity<?> response1 = makeRequest(port1);
        String sessionId = extractCookie(response1, "JSESSIONID");
        String hazelcastSessionId = extractCookie(response1, "hazelcast.sessionId");
        ResponseEntity<?> response2 = makeRequest(port2, sessionId, hazelcastSessionId);

        // then
        String body = response2.getBody().toString();
        assertTrue(body.substring(body.indexOf("Hits")).contains("<p>2</p>"));
    }

    private static String startApplication() {
        return new SpringApplicationBuilder(Application.class)
                .properties("server.port=0")
                .run()
                .getEnvironment()
                .getProperty("local.server.port");
    }

    private String extractCookie(ResponseEntity<?> response, String cookie) {
        return response.getHeaders().get("Set-Cookie").stream()
                .filter(s -> s.contains(cookie))
                .map(s -> s.split(";")[0])
                .map(s -> s.substring(cookie.length() + 1))
                .findFirst().orElse(null);
    }

    private static ResponseEntity<?> makeRequest(String port) {
        return makeRequest(port, null, null);
    }

    private static ResponseEntity<?> makeRequest(String port, String sessionId, String hazelcastSessionId) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        if (sessionId != null || hazelcastSessionId != null) {
            headers.add("Cookie", String.format("JSESSIONID=%s;hazelcast.sessionId=%s", sessionId, hazelcastSessionId));
        }
        return restTemplate.exchange("http://localhost:" + port, HttpMethod.GET, new HttpEntity<Object>(headers), String.class);
    }
}