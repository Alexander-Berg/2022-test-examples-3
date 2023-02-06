package ru.yandex.direct.web.entity.adgroup.controller;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

@ParametersAreNonnullByDefault
public class AdGroupControllerUtilsTest {

    @Test
    public void correctDomain() {
        String validDomain = "ya.ru";
        AdGroupControllerUtils.validateDomainWithException(validDomain);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullDomain() {
        String invalidDomain = null;
        AdGroupControllerUtils.validateDomainWithException(invalidDomain);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyDomain() {
        String invalidDomain = "";
        AdGroupControllerUtils.validateDomainWithException(invalidDomain);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidDomain() {
        String invalidDomain = "-sjla123.ru";
        AdGroupControllerUtils.validateDomainWithException(invalidDomain);
    }

}
