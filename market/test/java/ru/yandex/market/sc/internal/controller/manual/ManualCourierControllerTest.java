package ru.yandex.market.sc.internal.controller.manual;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class ManualCourierControllerTest {

    private static final long UID = 123L;

    private final MockMvc mockMvc;
    private final TestFactory testFactory;

    @SneakyThrows
    @Test
    void encryptCourierQrCode() {
        var courier = testFactory.storedCourier(29L);

        mockMvc.perform(
                        get("/manual/courier/encrypt")
                                .param("courierUid", courier.getId().toString())
                                .param("shipmentDate", "2021-10-25")
                                .param("randomNumber", "127023876238")
                                .header("Authorization", "OAuth uid-" + UID)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("\"0UY22sOkPdjZcCyctkkM7UUIQIdOUKO+U04Q+yMsBJw16Q+WEXUjUNNm\""));
    }
}
