package ru.yandex.market.abo.core.prepay;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.api.cpa.yam.dto.PrepayRequestDTO;
import ru.yandex.market.core.application.PartnerApplicationStatus;
import ru.yandex.market.mbi.api.client.MbiApiClient;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * Функциональные тесты для {@link PrepayRequestManager}.
 */
public class PrepayRequestManagerTest {
    private static final long REQUEST_ID = 18912L;

    @Mock
    private MbiApiClient mbiApiClient;

    @InjectMocks
    private PrepayRequestManager prepayRequestManager;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        PrepayRequestDTO prepayRequest = new PrepayRequestDTO();
        prepayRequest.setStatus(PartnerApplicationStatus.COMPLETED);

        doReturn(prepayRequest).when(mbiApiClient)
                .getPrepayRequest(eq(REQUEST_ID), ArgumentMatchers.isNull());
        doReturn(prepayRequest).when(mbiApiClient).getPrepayRequest(anyLong(), anyLong());
    }

    @Test
    public void testCompletedToNeedInfo() {
        prepayRequestManager.updateRequest(REQUEST_ID,
                PartnerApplicationStatus.NEED_INFO, "comment", 100L);
    }
}
