package ru.yandex.market.logistics.lom.converter;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.gateway.common.model.delivery.LegalForm;
import ru.yandex.market.logistic.gateway.common.model.delivery.Phone;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.delivery.Sender;
import ru.yandex.market.logistic.gateway.common.model.delivery.Sender.SenderBuilder;
import ru.yandex.market.logistic.gateway.common.model.delivery.Taxation;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.converter.lgw.SenderLgwConverter;
import ru.yandex.market.logistics.lom.entity.Order;
import ru.yandex.market.logistics.lom.entity.enums.StorageUnitType;
import ru.yandex.market.logistics.lom.entity.items.StorageUnit;

import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createDsWaybillSegments;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createOrder;
import static ru.yandex.market.logistics.lom.utils.ConverterTestEntitiesFactory.createStorageUnit;

class SenderLgwConverterTest extends AbstractContextualTest {

    @Autowired
    private SenderLgwConverter converter;

    private StorageUnit rootUnit;
    private StorageUnit place;

    @BeforeEach
    void setupPlaces() {
        place = createStorageUnit(StorageUnitType.PLACE, 1);
        rootUnit = createStorageUnit(StorageUnitType.ROOT, 0).setChildren(Set.of(place));
    }

    @Test
    void toExternal() {
        Order order = createOrder("LOinttest-1", place).setWaybill(createDsWaybillSegments(rootUnit));

        softly.assertThat(converter.toExternal(order))
            .isEqualToComparingFieldByField(
                fillSenderBuilder(
                    new SenderBuilder(
                        "credentials-incorporation",
                        "credentials-ogrn"
                    )
                ).build()
            );
    }

    @Test
    @DisplayName("legalForm = NONE, возвращаем null")
    void toExternalLegalFormNull() {
        Order order = createOrder("LOinttest-1", place);
        order.getCredentials().setLegalForm("NONE");
        order.setWaybill(createDsWaybillSegments(rootUnit));

        Sender actual = converter.toExternal(order);
        Sender expected = fillSenderBuilder(
            new SenderBuilder(
                "credentials-incorporation",
                "credentials-ogrn"
            )
        )
            .setLegalForm(null)
            .build();

        softly.assertThat(actual).isEqualToComparingFieldByField(expected);
    }

    private SenderBuilder fillSenderBuilder(SenderBuilder builder) {
        return builder
            .setId(ResourceId.builder().setYandexId("1").build())
            .setUrl("credentials-url")
            .setInn("credentials-inn")
            .setName("sender-name")
            .setLegalForm(LegalForm.IP)
            .setEmail("credentials-email@test-domain.com")
            .setTaxation(Taxation.OSN)
            .setPhones(List.of(new Phone("+74959999999", null)))
            .setUrl("www.sender-url.com");
    }

}
