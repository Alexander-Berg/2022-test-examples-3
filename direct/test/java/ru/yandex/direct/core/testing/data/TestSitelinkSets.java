package ru.yandex.direct.core.testing.data;

import java.util.List;

import javax.annotation.Nullable;

import ru.yandex.direct.core.entity.sitelink.model.Sitelink;
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;

public class TestSitelinkSets {

    public static SitelinkSet defaultSitelinkSet() {
        return defaultSitelinkSet(null);
    }

    public static SitelinkSet defaultSitelinkSet(@Nullable ClientId clientId) {
        return new SitelinkSet()
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withSitelinks(asList(sitelink1(), sitelink2(), sitelink3()));
    }

    public static SitelinkSet defaultSitelinkSet2(@Nullable ClientId clientId) {
        return new SitelinkSet()
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withSitelinks(asList(sitelink1(), sitelink2(), sitelink4()));
    }

    public static SitelinkSet defaultSitelinkSet3(@Nullable ClientId clientId) {
        return new SitelinkSet()
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withSitelinks(asList(sitelink1(), sitelink3(), sitelink4()));
    }

    public static SitelinkSet sitelinkSet(@Nullable ClientId clientId, List<Sitelink> sitelinks) {
        return new SitelinkSet()
                .withClientId(clientId != null ? clientId.asLong() : null)
                .withSitelinks(sitelinks);
    }

    private static Sitelink sitelink1() {
        return new Sitelink()
                .withTitle("О компании")
                .withDescription("сайтлинк 1")
                .withHref("https://yandex.ru/company");
    }

    private static Sitelink sitelink2() {
        return new Sitelink()
                .withTitle("Исследования")
                .withDescription("сайтлинк 2")
                .withHref("https://yandex.ru/company/researches");
    }

    private static Sitelink sitelink3() {
        return new Sitelink()
                .withTitle("Технологии")
                .withDescription("сайтлинк 3")
                .withHref("https://yandex.ru/company/technologies");
    }

    private static Sitelink sitelink4() {
        return new Sitelink()
                .withTitle("Лица")
                .withDescription("сайтлинк 4")
                .withHref("https://yandex.ru/company/people");
    }
}
