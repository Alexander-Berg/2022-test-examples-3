package ru.yandex.market.abo.core.cutoff;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.core.abo.AboCutoff;
import ru.yandex.market.mbi.api.client.entity.CutoffActionStatus;
import ru.yandex.market.mbi.api.client.entity.abo.OpenAboCutoffRequest;
import ru.yandex.market.mbi.api.client.entity.abo.OpenAboCutoffResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CutoffManagerTest extends EmptyTest {

    @Autowired
    private CutoffManager cutoffManager;

    @Test
    public void openAboCutoff() {
        MbiApiService mbiApiService = mock(MbiApiService.class);
        OpenAboCutoffResponse status = new OpenAboCutoffResponse();
        status.setStatus(CutoffActionStatus.OK);
        when(mbiApiService.openAboCutoff(anyLong(), any(OpenAboCutoffRequest.class))).thenReturn(status);
        cutoffManager.setMbiApiService(mbiApiService);

        OpenAboCutoffRequest cutoff = new OpenAboCutoffRequest(AboCutoff.CPC_QUALITY, "comment", new Date(), "subject", "text", 2L);
        long shopId = 1L;
        cutoffManager.openAboCutoff(shopId, cutoff, null);
        cutoffManager.closeAboCutoff(shopId, AboCutoff.CPC_QUALITY, 3L, false, "text");
    }
}
