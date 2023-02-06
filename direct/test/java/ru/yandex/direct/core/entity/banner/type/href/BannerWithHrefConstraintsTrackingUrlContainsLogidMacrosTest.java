package ru.yandex.direct.core.entity.banner.type.href;

import java.util.Collection;

import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;

import static java.util.Arrays.asList;
import static ru.yandex.direct.core.entity.banner.type.href.BannerWithHrefConstraints.trackingUrlContainsLogidMacros;


public class BannerWithHrefConstraintsTrackingUrlContainsLogidMacrosTest extends BannerWithHrefConstraintsBaseTest {

    public BannerWithHrefConstraintsTrackingUrlContainsLogidMacrosTest() {
        super(trackingUrlContainsLogidMacros());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {"url содержит макрос logid", "https://ya.ru?{logid}", null},
                {"url не содержит макрос logid", "https://ya.ru", BannerDefects.trackingUrlDoesntContainMacros()},
                {"url не содержит макрос logid но это adjust", "https://app.adjust.com/234", null}

        });
    }

}
