package ru.yandex.market.api.common.url;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.web.servlet.view.UrlBasedViewResolver;
import ru.yandex.market.api.controller.v2.UrlControllerV2;
import ru.yandex.market.api.error.InvalidParameterValueException;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.functional.Functionals;

import java.util.Arrays;
import java.util.function.Function;

/**
 * @author dimkarp93
 */
@RunWith(Parameterized.class)
@WithContext
public class UrlControllerV2Test extends UnitTestBase {

    private static final UrlControllerV2 URL_CONTROLLER = new UrlControllerV2();
    private static final Function<String, String> URL_TRANSFORM = Functionals.compose(MarketUrl::of, URL_CONTROLLER::urlTransform);

    @Parameterized.Parameter(0)
    public String inputParam;
    @Parameterized.Parameter(1)
    public String expectedResult;
    @Parameterized.Parameter(2)
    public Class<? extends Exception> expectedException;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {
                        "market.yanex.ru",
                        "",
                        InvalidParameterValueException.class
                },
                {
                        "/model/123",
                        "/desktop/model/123/?url=%2Fmodel%2F123",
                        null
                },
                {
                        "market.yandex.ru/product/23",
                        "/desktop/product/23/?url=market.yandex.ru%2Fproduct%2F23",
                        null
                },
                {
                        "https://m.market.yandex.ru/model.xml/67",
                        "/touch/model.xml/67/?url=https%3A%2F%2Fm.market.yandex.ru%2Fmodel.xml%2F67",
                        null
                },
                {
                        "market.yandex.ru/catalog/12/list?hid=213&glfilter=12:21",
                        "/desktop/catalog/12/list/?hid=213&glfilter=12:21" +
                                "&url=market.yandex.ru%2Fcatalog%2F12%2Flist%3Fhid%3D213%26glfilter%3D12%3A21",
                        null
                },
                {
                        "yandexmarket://brands/15",
                        "/mobile/brands/15/?url=yandexmarket%3A%2F%2Fbrands%2F15",
                        null
                },
        });

    }

    @Test
    public void doTest() {
        if (expectedException != null) {
            thrown.expect(expectedException);
        }
        Assert.assertEquals(
                UrlBasedViewResolver.FORWARD_URL_PREFIX + expectedResult,
                URL_TRANSFORM.apply(inputParam)
        );
    }
}
