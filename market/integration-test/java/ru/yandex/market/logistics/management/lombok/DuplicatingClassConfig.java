package ru.yandex.market.logistics.management.lombok;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Configuration
public class DuplicatingClassConfig {

    @Bean
    @Primary
    DuplicatedClass getPrimary() {
        return new DuplicatedClass("primary");
    }

    @Bean
    @Qualifier("classB")
    DuplicatedClass getQualified() {
        return new DuplicatedClass("qualifier");
    }

    @RequiredArgsConstructor
    static class DuplicatedClass {
        final String property;
    }

    @RequiredArgsConstructor
    @Getter
    @Component
    static class EnclosingClass {
        final DuplicatedClass instanceA;

        @Qualifier("classB")
        final DuplicatedClass instanceB;
    }
}
