package ru.yandex.market.wms.radiator.service.stocks.redis;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.CargoType;
import ru.yandex.market.logistic.api.model.fulfillment.CargoTypes;
import ru.yandex.market.logistic.api.model.fulfillment.Contractor;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.api.model.fulfillment.RemainingLifetimes;
import ru.yandex.market.logistic.api.model.fulfillment.Service;
import ru.yandex.market.logistic.api.model.fulfillment.ServiceType;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLife;
import ru.yandex.market.logistic.api.model.fulfillment.ShelfLives;
import ru.yandex.market.logistic.api.model.fulfillment.Tax;
import ru.yandex.market.logistic.api.model.fulfillment.TaxType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.UnitOperationType;
import ru.yandex.market.logistic.api.model.fulfillment.VatValue;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.wms.radiator.util.TimeUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

class RedisCacheReferenceItemsCodecTest {

    @Test
    void when_encodePayload_then_decodePayload() {
        var original = new Item.ItemBuilder("name", 1, BigDecimal.TEN)
                .setUnitId(new UnitId("msku", 100L, "msku"))
                .setHasLifeTime(true)
                .setArticle("article")
                .setBarcodes(List.of())
                .setBoxCapacity(2)
                .setBoxCount(3)
                .setCargoType(CargoType.BULKY_CARGO)
                .setCargoTypes(new CargoTypes(List.of(CargoType.CREASE, CargoType.VALUABLE)))
                .setCategoryId(5L)
                .setComment("comment")
                .setContractor(new Contractor("100", "contractor"))
                .setDescription("d")
                .setInboundServices(List.of(new Service(ServiceType.CHECK, "?", "de", Boolean.TRUE)))
                .setInstances(List.of())
                .setKorobyte(new Korobyte(1, 2, 3, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ZERO))
                .setLifeTime(6)
                .setRemainingLifetimes(
                        new RemainingLifetimes(
                                new ShelfLives(
                                        new ShelfLife(9),
                                        new ShelfLife(4)
                                ),
                                new ShelfLives(
                                        new ShelfLife(9),
                                        new ShelfLife(4)
                                )
                        )
                )
                .setRemovableIfAbsent(true)
                .setTax(new Tax(TaxType.VAT, VatValue.EIGHTEEN))
                .setUndefinedCount(2)
                .setUnitOperationType(UnitOperationType.CROSSDOCK)
                .setUntaxedPrice(BigDecimal.ONE)
                .setUpdatedDateTime(DateTime.fromOffsetDateTime(TimeUtils.odt(100)))
                .setUrls(List.of())
                .setVendorCodes(List.of("VC"))
                .build();

        var encoded = RedisCacheReferenceItemsCodec.encodePayload(original);
        var decoded = RedisCacheReferenceItemsCodec.decodePayload(encoded);

        assertThat(
                decoded,
                is(equalTo(original))
        );
    }
}
