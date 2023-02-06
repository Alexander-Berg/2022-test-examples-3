package ru.yandex.direct.core.entity.banner.type.href;

import java.util.Collection;

import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.type.href.BannerWithHrefConstraints.trackingUrlContainsMacros;


public class BannerWithHrefConstraintsTrackingUrlContainsMacrosTest extends BannerWithHrefConstraintsBaseTest {

    public BannerWithHrefConstraintsTrackingUrlContainsMacrosTest() {
        super(trackingUrlContainsMacros("xxx"));
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"url содержит макрос", "https://ya.ru?{xxx}", null},
                {"url не содержит макрос", "https://ya.ru", BannerDefects.trackingUrlDoesntContainMacros()},
        });
    }

}
