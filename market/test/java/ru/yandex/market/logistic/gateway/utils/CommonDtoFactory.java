package ru.yandex.market.logistic.gateway.utils;

import java.util.Collections;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang.RandomStringUtils;

import ru.yandex.market.logistic.gateway.common.model.common.Car;
import ru.yandex.market.logistic.gateway.common.model.common.Courier;
import ru.yandex.market.logistic.gateway.common.model.common.LegalEntity;
import ru.yandex.market.logistic.gateway.common.model.common.LocationFilter;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCode;
import ru.yandex.market.logistic.gateway.common.model.common.OrderTransferCodes;
import ru.yandex.market.logistic.gateway.common.model.common.Person;
import ru.yandex.market.logistic.gateway.common.model.common.Phone;

@UtilityClass
public class CommonDtoFactory {

    public static LocationFilter createLocationFilterRussia() {
        return LocationFilter.builder().setCountry("Россия").build();
    }

    public static Courier createCourier() {
        return Courier.builder()
            .setPartnerId(ru.yandex.market.logistic.gateway.common.model.common.ResourceId.builder().setPartnerId("2222").build())
            .setPersons(Collections.singletonList(
                Person.builder("Имя")
                        .setSurname("Фамилия")
                        .setPatronymic("Отчество")
                        .build()
                )
            )
            .setPhone(Phone.builder("71234567890").setAdditional("00").build())
            .setCar(Car.builder("о001вр799")
                .setDescription("вишневая девятка")
                .setModel("девяточка")
                .setColor("вишнёвенькая")
                .build()
            )
            .setLegalEntity(LegalEntity.builder().setAccount("test").build())
            .setUrl("https://go.yandex/route/6ea161f870ba6574d3bd9bdd19e1e9d8?lang=ru")
            .build();
    }

    public static OrderTransferCodes createOrderTransferCodes() {
        OrderTransferCodes codes = new OrderTransferCodes.OrderTransferCodesBuilder()
            .setOutbound(
                new OrderTransferCode.OrderTransferCodeBuilder()
                .setElectronicAcceptanceCertificate(RandomStringUtils.random(6))
                .build()
            )
            .setInbound(new OrderTransferCode.OrderTransferCodeBuilder()
            .setElectronicAcceptanceCertificate(RandomStringUtils.random(6)).build()
            )
            .build();
        return codes;
    }
}
