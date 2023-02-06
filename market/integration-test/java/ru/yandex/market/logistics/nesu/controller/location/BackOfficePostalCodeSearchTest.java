package ru.yandex.market.logistics.nesu.controller.location;

import org.junit.jupiter.api.DisplayName;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.base.AbstractPostalCodeSearchTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@DisplayName("Поиск индекса по адресу")
class BackOfficePostalCodeSearchTest extends AbstractPostalCodeSearchTest {

    @Override
    protected ResultActions search(String address) throws Exception {
        return mockMvc.perform(get("/back-office/location/postal-code").param("address", address));
    }
}
