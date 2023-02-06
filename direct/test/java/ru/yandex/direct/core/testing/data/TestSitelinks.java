package ru.yandex.direct.core.testing.data;

import java.util.Arrays;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.test.utils.RandomNumberUtils;

public class TestSitelinks {

    private TestSitelinks() {
    }

    public static SitelinkSet defaultSitelinkSet() {
        return defaultSitelinkSet(null);
    }

    public static SitelinkSet defaultSitelinkSet(Long clientId) {
        return new SitelinkSet()
                .withClientId(clientId)
                .withSitelinks(Arrays.asList(defaultSitelink(), defaultSitelink2()));
    }

    public static SitelinkSet defaultSitelinkSetWithTurbolandingId(Long clientId) {
        return new SitelinkSet()
                .withClientId(clientId)
                .withSitelinks(Arrays.asList(defaultSitelinkWithTurbolandingId(), defaultSitelinkWithTurbolandingId()));
    }

    public static SitelinkSet defaultSitelinkSetWithTurboHrefs(Long clientId) {
        return new SitelinkSet()
                .withClientId(clientId)
                .withSitelinks(Arrays.asList(defaultSitelinkWithTurboHref(), defaultSitelinkWithTurboHref()));
    }

    public static Sitelink defaultSitelink() {
        return new Sitelink()
                .withHref("http://ya.ru")
                .withTitle(RandomStringUtils.randomAlphanumeric(5))
                .withDescription("test");
    }

    public static Sitelink defaultSitelink2() {
        return new Sitelink()
                .withHref("http://yandex.ru")
                .withTitle(RandomStringUtils.randomAlphanumeric(5) + "2")
                .withDescription("test2");
    }

    public static Sitelink defaultSitelinkWithTurbolandingId() {
        return new Sitelink()
                .withTitle(RandomStringUtils.randomAlphanumeric(5))
                .withDescription("sitelink with turbolandingId")
                .withTurboLandingId(RandomNumberUtils.nextPositiveLong());
    }

    public static Sitelink defaultSitelinkWithTurboHref() {
        return new Sitelink()
                .withHref("http://yandex.ru/turbo" + RandomStringUtils.randomAlphanumeric(8))
                .withTitle(RandomStringUtils.randomAlphanumeric(5))
                .withDescription("sitelink with turbo href");
    }
}
