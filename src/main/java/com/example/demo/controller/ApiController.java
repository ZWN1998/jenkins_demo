package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/greet")
    public Map<String, Object> greet(@RequestParam(defaultValue = "World") String name) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Hello, " + name + "!");
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }

    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        result.put("received", body);
        result.put("timestamp", LocalDateTime.now().toString());
        return result;
    }
}
