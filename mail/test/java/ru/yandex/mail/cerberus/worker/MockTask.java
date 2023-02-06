package ru.yandex.mail.cerberus.worker;

import io.micronaut.context.annotation.AliasFor;
import io.micronaut.context.annotation.Bean;
import ru.yandex.mail.cerberus.worker.api.Processor;
import ru.yandex.mail.cerberus.worker.api.Task;

import javax.inject.Named;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Bean
@Named
@Processor
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MockTask {
    @AliasFor(annotation = Processor.class, member = "value")
    @AliasFor(annotation = Named.class, member = "value")
    String value();
}
