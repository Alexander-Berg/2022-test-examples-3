package ru.yandex.market.abo.core.quality_monitoring.pinger;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.cutoff.feature.FeatureCutoff;
import ru.yandex.market.abo.cpa.MbiApiService;
import ru.yandex.market.abo.cpa.pinger.PingerCutoffService;
import ru.yandex.market.abo.cpa.pinger.PingerStateChanger;
import ru.yandex.market.abo.util.telegram.TelegramMessage;
import ru.yandex.market.abo.util.telegram.TelegramSupplierService;
import ru.yandex.market.core.manager.dto.ManagerInfoDTO;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.mbi.api.client.entity.partner.PartnerInfoDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.quality_monitoring.pinger.PingerLongCutoffMonitoring.CRIT_MSG_REASON;
import static ru.yandex.market.abo.util.telegram.TelegramMessageReason.PINGER_LONG_CUTOFF;
import static ru.yandex.market.abo.util.telegram.TelegramMessageReason.PINGER_LONG_CUTOFF_FINISHED;

/**
 * @author artemmz
 * @date 23/03/2020.
 */
class PingerLongCutoffMonitoringTest {
    private static final long SHOP_ID = 4324L;
    private static final LocalDateTime SOME_TIME = LocalDateTime.now().minusMinutes(13);

    @InjectMocks
    PingerLongCutoffMonitoring pingerMonitoring;
    @Mock
    PingerStateChanger pingerStateChanger;
    @Mock
    PingerCutoffService pingerCutoffService;
    @Mock
    TelegramSupplierService telegramService;
    @Mock
    MbiApiService mbiApiService;
    @Mock
    FeatureCutoff featureCutoff;
    @Mock
    PartnerInfoDTO partnerInfo;
    @Mock
    ManagerInfoDTO managerInfo;
    @Mock
    TelegramMessage critTgMessage;
    @Mock
    TelegramMessage okTgMessage;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mbiApiService.getPartnerInfo(SHOP_ID)).thenReturn(partnerInfo);
        when(partnerInfo.getManager()).thenReturn(managerInfo);
        when(managerInfo.getUid()).thenReturn(100500L);

        when(pingerCutoffService.lastPingerCutoffs(any())).thenReturn(List.of(featureCutoff));
        when(featureCutoff.getShopId()).thenReturn(SHOP_ID);
        when(featureCutoff.getCreationTime()).thenReturn(LocalDateTime.now().minusHours(2));
        when(featureCutoff.getStatus()).thenReturn(ParamCheckStatus.FAIL);

        when(critTgMessage.getReason()).thenReturn(PINGER_LONG_CUTOFF);
        when(okTgMessage.getReason()).thenReturn(PINGER_LONG_CUTOFF_FINISHED);
        when(critTgMessage.getSourceId()).thenReturn(SHOP_ID);
        when(okTgMessage.getSourceId()).thenReturn(SHOP_ID);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void monitorCrit(boolean hasNoMsg) {
        when(telegramService.hasNoMessages(eq(SHOP_ID), eq(CRIT_MSG_REASON), any())).thenReturn(hasNoMsg);
        pingerMonitoring.sendCritMsg();
        verify(telegramService, times(hasNoMsg ? 1 : 0)).sendAndSave(any(), eq(SHOP_ID), any());
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void monitorOk(boolean lastMessageIsCrit, boolean cutoffClosed) {
        when(critTgMessage.getSendTime()).thenReturn(SOME_TIME);
        when(okTgMessage.getSendTime()).thenReturn(lastMessageIsCrit ? SOME_TIME.minusHours(1) : SOME_TIME.plusMinutes(5));
        when(telegramService.find(any(), any())).thenReturn(List.of(critTgMessage, okTgMessage));
        when(featureCutoff.getStatus()).thenReturn(cutoffClosed ? ParamCheckStatus.SUCCESS : ParamCheckStatus.FAIL);

        pingerMonitoring.sendOkAgainMsg();
        verify(telegramService, times(lastMessageIsCrit && cutoffClosed ? 1 : 0))
                .sendAndSave(any(), eq(SHOP_ID), eq(PINGER_LONG_CUTOFF_FINISHED));
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 100})
    void priority(long uid) {
        when(managerInfo.getUid()).thenReturn(uid);
        assertEquals(uid > 0, pingerMonitoring.isPrioritySupplier(SHOP_ID));
    }
}
