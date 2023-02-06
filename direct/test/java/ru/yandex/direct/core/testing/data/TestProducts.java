package ru.yandex.direct.core.testing.data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.product.model.ConditionName;
import ru.yandex.direct.core.entity.product.model.Product;
import ru.yandex.direct.core.entity.product.model.ProductCalcType;
import ru.yandex.direct.core.entity.product.model.ProductRestriction;
import ru.yandex.direct.core.entity.product.model.ProductRestrictionCondition;
import ru.yandex.direct.core.entity.product.model.ProductType;
import ru.yandex.direct.core.entity.product.model.ProductUnit;
import ru.yandex.direct.currency.CurrencyCode;

public class TestProducts {
    private TestProducts() {
    }

    public static ProductRestriction defaultProductRestriction() {
        return new ProductRestriction()
                .withId(0L)
                .withGroupType(AdGroupType.CPM_OUTDOOR)
                .withProductId(508594L)
                .withPublicNameKey("outdoor")
                .withPublicDescriptionKey("outdoor_description")
                .withUnitCountMin(null)
                .withUnitCountMax(null)
                .withConditionJson(
                        "[{\"availableAny\":false,\"name\":\"goal_stat\",\"required\":false,\"values\":[{\"value\": " +
                                "\"1\"}]}]"
                )
                .withConditions(List.of(
                        new ProductRestrictionCondition()
                                .withAvailableAny(false)
                                .withName(ConditionName.GOAL_STAT)
                                .withRequired(false)
                                .withValues(List.of(Map.of("value", "1")))
                ));
    }

    public static Product defaultProduct() {
        return new Product()
                .withId(503162L)
                .withProductName("Рублевый Директ, Яндекс")
                .withPublicNameKey("")
                .withPublicDescriptionKey("")
                .withThemeId(0L)
                .withType(ProductType.TEXT)
                .withPrice(BigDecimal.ONE)
                .withCurrencyCode(CurrencyCode.RUB)
                .withVat(false)
                .withUnit(ProductUnit.CLICKS)
                .withUnitName("Bucks")
                .withEngineId(7L)
                .withRate(1L)
                .withDailyShows(null)
                .withPacketSize(null)
                .withUnitScale(1L)
                .withCalcType(ProductCalcType.CPC);
    }
}
