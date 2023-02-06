package ru.yandex.market.pers.author.takeout;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.pers.author.takeout.model.TakeoutState;
import ru.yandex.market.pers.author.takeout.model.TakeoutType;
import ru.yandex.market.pers.author.takeout.model.TakeoutUrlParseResult;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.report.model.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyLong;
import static ru.yandex.market.pers.author.takeout.model.TakeoutParam.BUSINESS_ID;
import static ru.yandex.market.pers.author.takeout.model.TakeoutParam.MODEL_ID;
import static ru.yandex.market.pers.author.takeout.model.TakeoutParam.SHOP_ID;
import static ru.yandex.market.pers.author.takeout.model.TakeoutState.INVALID_PARAMS;
import static ru.yandex.market.pers.author.takeout.model.TakeoutState.INVALID_URL;
import static ru.yandex.market.pers.author.takeout.model.TakeoutState.UNSUPPORTED_URL;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.11.2021
 */
public class TakeoutUrlParserTest extends PersAuthorTest {

    @Autowired
    private TakeoutService takeoutService;

    @Autowired
    private TakeoutUrlParser parser;

    @Autowired
    private ReportService reportService;

    @BeforeEach
    public void initReport() {
        Mockito.when(reportService.getModelById(anyLong())).thenReturn(Optional.empty());
    }

    @Test
    public void testParseInvalid() {
        checkParseError("host:123:123", INVALID_URL, "Invalid url: host:123:123");
        checkParseError("http://fakemarket.yandex.ru", INVALID_URL, "non-market url requested");
        checkParseError("http://market.yandex.ru.fakeshop.ru", INVALID_URL, "non-market url requested");
        checkParseError("http://market.ru", INVALID_URL, "non-market url requested");
        checkParseError("http://market.yandex.ru", UNSUPPORTED_URL, "Unknown url - takeout not supported: ");
        checkParseError("http://market.yandex.ru/prop", UNSUPPORTED_URL, "Unknown url - takeout not supported: /prop");
        checkParseError("http://market.yandex.ru/producti",
            UNSUPPORTED_URL, "Unknown url - takeout not supported: /producti");
        checkParseError("http://market.yandex.ru/product/",
            INVALID_PARAMS, "Invalid url: can't extract all required parameters. Found: []");
        checkParseError("http://market.yandex.ru/product/abc",
            INVALID_PARAMS, "Invalid url: can't extract all required parameters. Found: []");
    }

    @Test
    public void testParseModel() {
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.MODEL, Map.of(MODEL_ID, "123")),
            parser.parseRequest("http://market.yandex.ru/product/123")
        );
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.MODEL, Map.of(MODEL_ID, "123")),
            parser.parseRequest("http://market.yandex.ru/product/123?param=value")
        );
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.MODEL, Map.of(MODEL_ID, "55341341")),
            parser.parseRequest("http://market.yandex.ru/product/55341341/reviews")
        );
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.MODEL, Map.of(MODEL_ID, "431")),
            parser.parseRequest("http://market.yandex.ru/product--slug/431")
        );

        // other
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.MODEL, Map.of(MODEL_ID, "123")),
            parser.parseRequest("market.yandex.ru/product/123")
        );
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.MODEL, Map.of(MODEL_ID, "123")),
            parser.parseRequest("http://m.market.yandex.ru/product/123")
        );
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.MODEL, Map.of(MODEL_ID, "123")),
            parser.parseRequest("https://www.market.yandex.ru/product/123")
        );
    }

    @Test
    public void testParseModelWithTransition() {
        Model expectedTransition = new Model();
        expectedTransition.setId(444532);
        Mockito.when(reportService.getModelById(123)).thenReturn(Optional.of(expectedTransition));

        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.MODEL, Map.of(MODEL_ID, "444532")),
            parser.parseRequest("http://market.yandex.ru/product/123")
        );
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.MODEL, Map.of(MODEL_ID, "111")),
            parser.parseRequest("http://market.yandex.ru/product/111")
        );
    }

    @Test
    public void testParseShop() {
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.SHOP, Map.of(SHOP_ID, "123")),
            parser.parseRequest("http://market.yandex.ru/shop/123")
        );
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.SHOP, Map.of(SHOP_ID, "123")),
            parser.parseRequest("http://market.yandex.ru/shop/123?param=value")
        );
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.SHOP, Map.of(SHOP_ID, "55341341")),
            parser.parseRequest("http://market.yandex.ru/shop/55341341/path")
        );
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.SHOP, Map.of(SHOP_ID, "431")),
            parser.parseRequest("http://market.yandex.ru/shop--slug/431")
        );
    }

    @Test
    public void testParseBusiness() {
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.BUSINESS, Map.of(BUSINESS_ID, "123")),
            parser.parseRequest("http://market.yandex.ru/business/123")
        );
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.BUSINESS, Map.of(BUSINESS_ID, "123")),
            parser.parseRequest("http://market.yandex.ru/business/123?param=value")
        );
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.BUSINESS, Map.of(BUSINESS_ID, "55341341")),
            parser.parseRequest("http://market.yandex.ru/business/55341341/path")
        );
        assertTakeoutEquals(
            TakeoutUrlParseResult.of(TakeoutType.BUSINESS, Map.of(BUSINESS_ID, "431")),
            parser.parseRequest("http://market.yandex.ru/business--slug/431")
        );
    }

    private void assertTakeoutEquals(TakeoutUrlParseResult expected, TakeoutUrlParseResult actual) {
        assertEquals(expected.getState(), actual.getState());
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getParams(), actual.getParams());
    }

    private void checkParseError(String url,
                                 TakeoutState expectedState,
                                 String expectedMessage) {
        TakeoutUrlParseResult parseResult = parser.parseRequest(url);
        assertFalse(parseResult.isOk());
        assertEquals(expectedState, parseResult.getState());
        assertEquals(expectedMessage, parseResult.getError());
    }
}
