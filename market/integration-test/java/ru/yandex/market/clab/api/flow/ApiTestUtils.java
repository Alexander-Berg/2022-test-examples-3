package ru.yandex.market.clab.api.flow;

import ru.yandex.market.clab.common.service.ShopSkuKey;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiTestUtils {

    private ApiTestUtils() { }

    public static Good getGood(List<Good> goods, ShopSkuKey supplierGoodId) {
        return goods.stream()
            .filter(supplierGoodId::matchesGood)
            .findAny()
            .orElseThrow(IllegalStateException::new);
    }

    public static long getLong(Map<String, Object> values, String key) {
        String value = getString(values, key);
        assertThat(value).matches("\\d+");
        return Long.parseLong(value);
    }

    public static String getString(Map<String, Object> values, String key) {
        assertThat(values).containsKey(key);
        Object value = values.get(key);
        assertThat(value).isInstanceOf(String.class);
        return (String) value;
    }
}
