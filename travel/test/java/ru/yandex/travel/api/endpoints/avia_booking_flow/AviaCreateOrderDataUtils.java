package ru.yandex.travel.api.endpoints.avia_booking_flow;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.google.common.collect.Lists;

import ru.yandex.avia.booking.enums.DocumentType;
import ru.yandex.avia.booking.enums.Sex;
import ru.yandex.avia.booking.service.dto.form.CreateOrderForm;
import ru.yandex.avia.booking.service.dto.form.TravellerFormDTO;

class AviaCreateOrderDataUtils {

    public static CreateOrderForm createTestObject() {
        CreateOrderForm createOrderForm = new CreateOrderForm();
        createOrderForm.setVariantToken("0-0-0-0-0!some_variant_id");
        createOrderForm.setEmail("test@test.com");
        createOrderForm.setPhone("79112223322");
        createOrderForm.setUserIp("127.0.0.1");
        createOrderForm.setUserAgent("AviaCreateOrderDataUtilsTestUa");
        TravellerFormDTO traveller = new TravellerFormDTO(
                "test",
                "",
                "testLastName",
                LocalDate.of(1980, 1, 1),
                "12345783",
                LocalDate.now().plus(2, ChronoUnit.YEARS),
                DocumentType.PASSPORT,
                null,
                98552,
                Sex.MALE,
                "",
                ""
        );
        createOrderForm.setDocuments(Lists.newArrayList(traveller));
        return createOrderForm;
    }

}
