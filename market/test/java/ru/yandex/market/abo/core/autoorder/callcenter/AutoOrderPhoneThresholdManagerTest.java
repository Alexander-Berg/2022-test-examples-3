package ru.yandex.market.abo.core.autoorder.callcenter;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.autoorder.callcenter.model.AutoOrderCallCenterEvent;
import ru.yandex.market.abo.core.autoorder.callcenter.model.AutoOrderPhone;
import ru.yandex.market.abo.core.autoorder.callcenter.model.AutoOrderPhoneBanStatus;
import ru.yandex.market.abo.core.autoorder.callcenter.model.AutoOrderPhoneThresholdConfig;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author komarovns
 * @date 10.03.2020
 */
class AutoOrderPhoneThresholdManagerTest {
    private static final long CALLS_COUNT_THRESHOLD = 2;
    private static final long DISABLE_WINDOW_MINUTES = 10;
    private static final AutoOrderPhoneThresholdConfig CONFIG =
            new AutoOrderPhoneThresholdConfig(DISABLE_WINDOW_MINUTES, -1, CALLS_COUNT_THRESHOLD);

    @Mock
    AutoOrderPhoneManager autoOrderPhoneManager;
    @Mock
    AutoOrderPhoneService autoOrderPhoneService;
    @Mock
    AutoOrderCallCenterEventService autoOrderCallCenterEventService;
    @Mock
    AutoOrderPhoneThresholdStTicketCreator autoOrderPhoneThresholdStTicketCreator;
    @InjectMocks
    AutoOrderPhoneThresholdManager autoOrderPhoneThresholdManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUnbanPhonesWithoutCalls() {
        when(autoOrderPhoneService.loadByBanStatus(AutoOrderPhoneBanStatus.BANNED_AUTO)).thenReturn(List.of(
                createPhone("1"), createPhone("2")
        ));
        when(autoOrderCallCenterEventService.findAllAfter(any())).thenReturn(List.of(
                createEvent("1"), createEvent("1"), createEvent("3")
        ));

        autoOrderPhoneThresholdManager.unbanPhonesWithoutCalls(CONFIG);

        verify(autoOrderPhoneManager, only()).updateBanStatus("2", AutoOrderPhoneBanStatus.NOT_BANNED);
    }

    @Test
    void testBanPhonesByCallCount() {
        when(autoOrderPhoneService.loadByBanStatus(AutoOrderPhoneBanStatus.NOT_BANNED)).thenReturn(List.of(
                createPhone("1"), createPhone("2")
        ));
        when(autoOrderCallCenterEventService.findAllAfter(any())).thenReturn(List.of(
                createEvent("1"), createEvent("1"), createEvent("1"),
                createEvent("2"),
                createEvent("3"), createEvent("3"), createEvent("3")
        ));

        autoOrderPhoneThresholdManager.banPhonesByCallCount(CONFIG);

        verify(autoOrderPhoneManager, only()).updateBanStatus("1", AutoOrderPhoneBanStatus.BANNED_AUTO);
        verify(autoOrderPhoneThresholdStTicketCreator, only()).createStTicket("1", 3, DISABLE_WINDOW_MINUTES);
    }

    private static AutoOrderCallCenterEvent createEvent(String phoneNumber) {
        return new AutoOrderCallCenterEvent(null, null, phoneNumber, null);
    }

    private static AutoOrderPhone createPhone(String phoneNumber) {
        return new AutoOrderPhone(phoneNumber, null, null);
    }
}
