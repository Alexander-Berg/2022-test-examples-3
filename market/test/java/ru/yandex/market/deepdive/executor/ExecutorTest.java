package ru.yandex.market.deepdive.executor;


import java.util.ArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.deepdive.configuration.IntegrationTestConfiguration;
import ru.yandex.market.deepdive.domain.client.PvzClient;
import ru.yandex.market.deepdive.domain.client.dto.PageableResponse;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntOrderDto;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntPickupPointDto;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = IntegrationTestConfiguration.class)
@AutoConfigureMockMvc(secure = false)
@DisplayName("Тесты для executor")
public class ExecutorTest {
    @Value("${pvz-int.partner.id:1001047541}")
    private Long partnerId;

    @Autowired
    private UpdateDataExecutor updateDataExecutor;

    @MockBean
    private PvzClient pvzClient;

    @Test
    public void doEmptyJob() {
        Mockito.when(pvzClient.getPickupPointsForPage(partnerId, 0)).thenReturn(
                new PageableResponse<PvzIntPickupPointDto>()
                        .setContent(new ArrayList<>())
                        .setLast(true)
                        .setTotalPages(1));
        Mockito.when(pvzClient.getOrders(partnerId, 0)).thenReturn(
                new PageableResponse<PvzIntOrderDto>()
                        .setContent(new ArrayList<>())
                        .setLast(true)
                        .setTotalPages(1));
        updateDataExecutor.doRealJob(null);
    }
}
