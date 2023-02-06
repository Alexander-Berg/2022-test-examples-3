package ru.yandex.direct.core.entity.mobilecontent.repository;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.direct.core.entity.mobilecontent.model.AvailableAction;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentExternalWorldMoney;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreActionForPrices;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreCountry;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.mobilecontent.util.MobileContentUtil.getExternalWorldMoney;

public class MobileContentMappingTest {
    private static final Map<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>> TEST_MAP =
            ImmutableMap.<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>>builder()
                    .put(StoreCountry.RU.toString(), ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                            .put(StoreActionForPrices.play, getExternalWorldMoney("1.123", "CAD"))
                            .build())
                    .build();

    @Test
    public void testParsePricesToDbFormat() {
        String pricesJson = MobileContentMapping.pricesToDbFormat(TEST_MAP);
        // Тут маппинг не совпадает с перловым на 100%, но кажется что это не проблема
        // Разница в том, что в перле цифра будет в кавычках, а у нас нет
        assertThat("Сериализованное значение совпадает с ожидаемым", pricesJson, anyOf(
                equalTo("{\"RU\":{\"play\":{\"price\":1.123,\"price_currency\":\"CAD\"}}}"),
                equalTo("{\"RU\":{\"play\":{\"price_currency\":\"CAD\",\"price\":1.123}}}")
        ));
    }

    @Test
    public void testParsePricesToDbFormatWhenValueIsNull() {
        String pricesJson = MobileContentMapping.pricesToDbFormat(null);
        assertThat("Сериализованное значение совпадает с ожидаемым", pricesJson, equalTo("{}"));
    }

    @Test
    public void testParsePricesFromDbFormat() {
        Map<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>> prices = MobileContentMapping
                .pricesFromDbFormat("{\"RU\":{\"play\":{\"price\":\"1.123\",\"price_currency\":\"CAD\"}}}");

        assertThat("Десериализованное значение совпадает с ожидаемым", prices, beanDiffer(TEST_MAP));
    }

    @Test
    public void testParsePricesFromDbFormatWhenValueIsNull() {
        Map<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>> prices =
                MobileContentMapping.pricesFromDbFormat(null);
        assertTrue("Десериализованное значение совпадает с ожидаемым", prices.isEmpty());
    }

    @Test
    public void testAvailableActionFromDbFormat() {
        Set<AvailableAction> content = MobileContentMapping.availableActionFromDbFormat("download,listen");
        assertThat("Правильно распарсили действия", content, equalTo(ImmutableSet.<AvailableAction>builder()
                .add(AvailableAction.download)
                .add(AvailableAction.listen)
                .build()));
    }

    @Test
    public void testAvailableActionToDbFormat() {
        String format = MobileContentMapping.availableActionToDbFormat(ImmutableSet.<AvailableAction>builder()
                .add(AvailableAction.download)
                .add(AvailableAction.listen)
                .build());
        assertThat("Правильно распарсили действия", format, anyOf(
                equalTo("download,listen"),
                equalTo("listen,download")));
    }
}
