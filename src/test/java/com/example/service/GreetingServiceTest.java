package com.example.service;

import com.example.module.AppModule;
import com.example.service.impl.GreetingServiceImpl;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GreetingServiceTest {

    private Injector injector;

    @BeforeEach
    void setUp() {
        injector = Guice.createInjector(new AppModule());
    }

    @Test
    void injectorProvidesGreetingService() {
        GreetingService service = injector.getInstance(GreetingService.class);
        assertNotNull(service);
        assertInstanceOf(GreetingServiceImpl.class, service);
    }

    @Test
    void greetReturnsExpectedMessage() {
        GreetingService service = injector.getInstance(GreetingService.class);
        assertEquals("Hello, World!", service.greet("World"));
    }

    @Test
    void greetHandlesEmptyName() {
        GreetingService service = injector.getInstance(GreetingService.class);
        assertEquals("Hello, !", service.greet(""));
    }

    @Test
    void singletonScopeReturnsSameInstance() {
        GreetingService first = injector.getInstance(GreetingService.class);
        GreetingService second = injector.getInstance(GreetingService.class);
        assertEquals(first, second, "Singleton-scoped service should return the same instance");
    }
}
