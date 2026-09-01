package com.rojas.holamundo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rojas.holamundo.dto.HelloDto;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public HelloDto sayHello() {
        return new HelloDto("Hello, World!");
    }
}
