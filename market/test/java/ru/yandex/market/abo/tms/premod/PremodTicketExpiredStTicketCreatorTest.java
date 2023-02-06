package ru.yandex.market.abo.tms.premod;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.abo.core.assessor.AssessorService;
import ru.yandex.market.abo.core.assessor.model.AssessorInfo;
import ru.yandex.market.abo.core.premod.PremodShopInfoService;
import ru.yandex.market.abo.core.premod.PremodTicketService;
import ru.yandex.market.abo.core.premod.model.PremodShopInfo;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.core.startrek.StartrekTicketManager;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketFactory;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketReason;
import ru.yandex.market.util.db.ConfigurationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 29.04.2020
 */
class PremodTicketExpiredStTicketCreatorTest {

    private static final long TICKET_ID = 1111L;
    private static final long SHOP_ID = 321L;
    private static final int TICKET_LIFE_TIME = 48;

    @InjectMocks
    private PremodExpiredStCreator premodExpiredStCreator;

    @Mock
    private StartrekTicketManager startrekTicketManager;
    @Mock
    private PremodTicketService premodTicketService;
    @Mock
    private PremodShopInfoService premodShopInfoService;
    @Mock
    private AssessorService assessorService;
    @Mock
    private ConfigurationService coreConfigService;


    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        when(coreConfigService.getValueAsInt(CoreConfig.LITE_AND_PREMOD_LIFE_PERIOD.getId())).thenReturn(TICKET_LIFE_TIME);

        var assessorInfo = mock(AssessorInfo.class);
        when(assessorService.loadAssessorInfo(anyLong())).thenReturn(Optional.of(assessorInfo));
    }

    @ParameterizedTest
    @CsvSource({"false, , 0", "true, , 1", "true, 123, 2"})
    void testCreateStTickets(boolean hasNoTickets, Long userIdInPremodTicket, int createdTicketsCount) {
        PremodTicket premodTicket = mock(PremodTicket.class);
        var shop = new PremodShopInfo(SHOP_ID, "", "", null, null);
        when(premodShopInfoService.load(premodTicket)).thenReturn(shop);
        when(premodTicket.getId()).thenReturn(TICKET_ID);
        when(premodTicket.getShopId()).thenReturn(SHOP_ID);

        when(startrekTicketManager.hasNoTickets(TICKET_ID, StartrekTicketReason.PREMOD_TICKET_EXPIRED__NEED_CHECK_IMMEDIATELY))
                .thenReturn(hasNoTickets);
        when(premodTicket.getUserId()).thenReturn(userIdInPremodTicket);

        when(premodTicketService.loadPremodExpiredTickets()).thenReturn(List.of(premodTicket));
        premodExpiredStCreator.createStTickets();

        verify(startrekTicketManager, times(createdTicketsCount)).createTicket(any(StartrekTicketFactory.class));
    }
}
