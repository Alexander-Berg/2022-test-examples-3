package ru.yandex.market.abo.tms.premod;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Value;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.premod.PremodManager;
import ru.yandex.market.abo.core.premod.PremodTicketService;
import ru.yandex.market.abo.core.premod.model.PremodCheckType;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.core.premod.model.PremoderationTicketRequest;
import ru.yandex.market.abo.util.MapUtil;
import ru.yandex.market.core.testing.TestingType;
import ru.yandex.market.mbi.api.client.entity.moderation.PremoderationReadyShopResponse;
import ru.yandex.market.monitoring.JobsMonitoring;

import static java.util.Set.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.testing.TestingType.CPA_PREMODERATION;
import static ru.yandex.market.core.testing.TestingType.CPC_PREMODERATION;

/**
 * @author komarovns
 * @date 26.08.2020
 */
class PremodProcessorTest {
    private static final long SHOP_ID = 774;

    @Mock
    PremodTicketService premodTicketService;
    @Mock
    PremodManager premodManager;
    @Mock
    JobsMonitoring premodJobsMonitoring;
    @InjectMocks
    PremodProcessor premodProcessor;

    private static final Map<Long, ShopConfig> SHOP_CONFIGS = MapUtil.by(ShopConfig::getShopId, List.of(
            config(0, of(), of()),
            config(1, of(CPC_PREMODERATION), of(CPC_PREMODERATION)),
            config(2, of(CPC_PREMODERATION), of(CPC_PREMODERATION, CPA_PREMODERATION)),
            config(3, of(), of(CPC_PREMODERATION, CPA_PREMODERATION)),
            config(4, of(CPA_PREMODERATION), of(CPC_PREMODERATION)),
            config(5, of(CPC_PREMODERATION, CPA_PREMODERATION), of())
    ));

    private static final Set<PremoderationTicketRequest> EXPECTED_CREATE = of(
            premodRequest(2, CPA_PREMODERATION),
            premodRequest(3, CPC_PREMODERATION),
            premodRequest(3, CPA_PREMODERATION),
            premodRequest(4, CPC_PREMODERATION)
    );

    private static final Set<PremoderationTicketRequest> EXPECTED_CANCEL = of(
            premodRequest(4, CPA_PREMODERATION),
            premodRequest(5, CPC_PREMODERATION),
            premodRequest(5, CPA_PREMODERATION)
    );

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void syncPremodTicketsTest() {
        var aboTickets = StreamEx.of(SHOP_CONFIGS.values()).flatMap(PremodProcessorTest::aboTickets).toList();
        var mbiShops = StreamEx.of(SHOP_CONFIGS.values()).flatMap(PremodProcessorTest::mbiShops).toList();

        var syncResult = PremodProcessor.syncPremodTickets(aboTickets, mbiShops);

        assertEquals(EXPECTED_CREATE.size(), syncResult.getNeedToCreate().size());
        assertEquals(EXPECTED_CREATE, new HashSet<>(syncResult.getNeedToCreate()));

        assertEquals(EXPECTED_CANCEL.size(), syncResult.getNeedToCancel().size());
        // не хотим ради теста переопределять у тикета equals()/hashCode(),
        // поэтому просто оборачиваем в подходящий PremoderationTicketRequest
        var actualCancel = StreamEx.of(syncResult.getNeedToCancel())
                .map(ticket -> premodRequest(ticket.getShopId(), ticket.getTestingType().toMbiTestingType()))
                .toSet();
        assertEquals(EXPECTED_CANCEL, actualCancel);
    }

    @Test
    void cancelDuplicatesTicketsTest() {
        var now = LocalDateTime.now();
        var ticketToCancel = ticket(SHOP_ID, PremodCheckType.CPC_PREMODERATION, now);
        when(premodTicketService.loadRunningTicketsByTypes(any())).thenReturn(List.of(
                ticketToCancel,
                ticket(SHOP_ID, PremodCheckType.CPC_PREMODERATION, now.minusDays(1)),
                ticket(SHOP_ID + 1, PremodCheckType.CPC_PREMODERATION, now),
                ticket(SHOP_ID + 1, PremodCheckType.CPA_PREMODERATION, now)
        ));

        premodProcessor.cancelDuplicatesTickets();

        var captor = ArgumentCaptor.forClass(PremodTicket.class);
        verify(premodManager).updatePremodTicket(captor.capture());
        assertEquals(List.of(ticketToCancel), captor.getAllValues());
    }

    private static ShopConfig config(long shopId, Set<TestingType> aboTestingTypes, Set<TestingType> mbiTestingTypes) {
        return new ShopConfig(shopId, aboTestingTypes, mbiTestingTypes);
    }

    private static PremoderationReadyShopResponse mbiShop(long shopId, TestingType testingType) {
        return new PremoderationReadyShopResponse(shopId, testingType, false, 0);
    }

    private static PremoderationTicketRequest premodRequest(long shopId, TestingType testingType) {
        return new PremoderationTicketRequest(mbiShop(shopId, testingType));
    }

    private static PremodTicket ticket(long shopId, TestingType testingType) {
        return ticket(shopId, PremodCheckType.from(testingType), LocalDateTime.now());
    }

    private static PremodTicket ticket(long shopId, PremodCheckType checkType, LocalDateTime creationTime) {
        var ticket = new PremodTicket(shopId, 0, checkType);
        ticket.setCreationTime(DateUtil.asDate(creationTime));
        return ticket;
    }

    private static StreamEx<PremodTicket> aboTickets(ShopConfig config) {
        return StreamEx.of(config.aboTestingTypes).map(testingType -> ticket(config.shopId, testingType));
    }

    private static StreamEx<PremoderationReadyShopResponse> mbiShops(ShopConfig config) {
        return StreamEx.of(config.mbiTestingTypes).map(testingType -> mbiShop(config.shopId, testingType));
    }

    @Value
    static class ShopConfig {
        long shopId;
        Set<TestingType> aboTestingTypes;
        Set<TestingType> mbiTestingTypes;
    }
}
