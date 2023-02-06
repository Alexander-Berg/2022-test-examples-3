package ru.yandex.market.deepdive.executor;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.deepdive.AbstractTest;
import ru.yandex.market.deepdive.domain.client.PvzClient;
import ru.yandex.market.deepdive.domain.client.dto.PageableResponse;
import ru.yandex.market.deepdive.domain.client.dto.PvzIntPickupPointDto;

public class UpdateDataExecutorTest extends AbstractTest {
    @Value("${pvz-int.partner.id:1001047541}")
    private Long partnerId;

    @Autowired
    private UpdateDataExecutor updateDataExecutor;

    @MockBean
    private PvzClient pvzClient;

    @Test
    public void doEmptyJob() {
        Mockito.when(pvzClient.getPvzPickupPointsByPartnerIdAndPage(partnerId, 0)).thenReturn(
                new PageableResponse<PvzIntPickupPointDto>()
                        .setContent(new ArrayList<>())
                        .setTotalPages(1));

        updateDataExecutor.doRealJob(null);
    }
}
