package com.example.module;

import com.example.service.GreetingService;
import com.example.service.impl.GreetingServiceImpl;
import com.google.inject.AbstractModule;

public class AppModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(GreetingService.class).to(GreetingServiceImpl.class);
    }
}
