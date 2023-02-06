package ru.yandex.market.core.info;

import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

@DbUnitDataBaseConfig({
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.PROPERTY_TABLE_TYPE,
                value = "TABLE, VIEW"
        )
})
class BadShippingInfoTest extends FunctionalTest {

    /**
     * Проверяем наличие в shops_web.v_bad_shipping_info магазинов, у которых не настроена доставка хотя бы в  одном регионе.
     */
    @Test
    @DbUnitDataSet(before = "BadShippingInfo.before.csv", after = "BadShippingInfo.after.csv")
    void testBadShippingInfoTest() {
        // тест проверяет вьюху в after
    }
}
