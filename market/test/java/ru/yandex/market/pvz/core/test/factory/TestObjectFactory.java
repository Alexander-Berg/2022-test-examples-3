package ru.yandex.market.pvz.core.test.factory;

import java.time.Clock;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class TestObjectFactory {

    @Autowired
    protected Clock clock = Clock.systemDefaultZone();

    protected static String randomString(int length) {
        return RandomStringUtils.randomAlphanumeric(length);
    }
}
