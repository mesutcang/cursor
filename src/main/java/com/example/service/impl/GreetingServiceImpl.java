package com.example.service.impl;

import com.example.service.GreetingService;
import com.google.inject.Singleton;

@Singleton
public class GreetingServiceImpl implements GreetingService {

    @Override
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
