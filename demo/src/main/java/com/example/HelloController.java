package com.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HelloController {

    @GetMapping("/")
    public Map<String, String> index() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("message", "Hello from Kubernetes!");
        result.put("hostname", hostname());
        result.put("time", LocalDateTime.now().toString());
        return result;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Kubernetes, pod = " + hostname();
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @GetMapping("/env")
    public Map<String, String> env() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("HOSTNAME", env("HOSTNAME"));
        result.put("MY_POD_NAME", env("MY_POD_NAME"));
        result.put("MY_POD_NAMESPACE", env("MY_POD_NAMESPACE"));
        return result;
    }

    private String hostname() {
        return env("HOSTNAME");
    }

    private String env(String name) {
        return System.getenv().getOrDefault(name, "unknown");
    }
}
