package com.example.app;

import com.example.module.AppModule;
import com.example.service.GreetingService;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class Application {

    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new AppModule());
        GreetingService greetingService = injector.getInstance(GreetingService.class);

        String message = greetingService.greet("World");
        System.out.println(message);
    }
}
