package ru.yandex.market.logistics.nesu.api.location;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.services.auth.blackbox.BlackboxService;
import ru.yandex.market.logistics.nesu.api.auth.ApiAuthHolder;
import ru.yandex.market.logistics.nesu.base.AbstractPostalCodeSearchTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@DisplayName("Поиск индекса по адресу в Open API")
class ApiPostalCodeSearchTest extends AbstractPostalCodeSearchTest {

    @Autowired
    private BlackboxService blackboxService;
    @Autowired
    private ObjectMapper objectMapper;

    private ApiAuthHolder authHolder;

    @BeforeEach
    void setupAuth() {
        authHolder = new ApiAuthHolder(blackboxService, objectMapper);
    }

    @Override
    protected ResultActions search(String address) throws Exception {
        return mockMvc.perform(get("/api/location/postal-code")
            .headers(authHolder.authHeaders())
            .param("address", address));
    }
}
