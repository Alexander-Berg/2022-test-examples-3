package ru.yandex.direct.traceinterception.configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ContextConfiguration(classes = TraceInterceptionTestingConfiguration.class)
@TestPropertySource(properties = {
        "trace.interceptions.cacheReloadIntervalInMs=1",
        "trace.interceptions.storageReloadIntervalInMs=1"
})
public @interface TraceInterceptionTest {
}



