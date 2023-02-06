package ru.yandex.market.abo.core.yellow.outlet.startrek;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.outlet.maps.model.MapsScheduleDay;
import ru.yandex.market.abo.core.outlet.maps.model.MapsScheduleLine;
import ru.yandex.market.abo.core.outlet.model.SimpleOutletInfo;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.core.startrek.StartrekTicketManager;
import ru.yandex.market.abo.core.startrek.model.StartrekTicket;
import ru.yandex.market.abo.core.startrek.model.StartrekTicketWithIssue;
import ru.yandex.market.abo.core.yellow.outlet.sprav.YellowSpravOutlet;
import ru.yandex.market.core.outlet.Address;
import ru.yandex.market.core.schedule.ScheduleLine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author komarovns
 * @date 12.02.2020
 */
class YellowMbiOutletStartrekTicketCreatorTest {
    private static final long SHOP_ID = 774;
    private static final long ST_TICKET_ID = 123;

    private static final long PROBLEM_OUTLET = 1;
    private static final long ALREADY_PROCESSED_OUTLET = 2;

    @Mock
    YellowOutletStartrekTicketHistoryService stTicketHistoryService;
    @Mock
    ShopInfoService shopInfoService;
    @Mock
    StartrekTicketManager startrekTicketManager;
    @Mock
    ShopInfo shopInfo;
    @InjectMocks
    YellowMbiOutletStartrekTicketCreator yellowMbiOutletStartrekTicketCreator;

    private final Map<YellowOutletProblemType, List<SimpleOutletInfo>> mbiOutletsByProblem = Map.of(
            YellowOutletProblemType.WRONG_STATUS, List.of(createMbiOutlet(PROBLEM_OUTLET)),
            YellowOutletProblemType.SCHEDULE_DIFF, List.of(createMbiOutlet(ALREADY_PROCESSED_OUTLET))
    );

    private final Map<Long, YellowSpravOutlet> spravByClosestMbi = Map.of(
            PROBLEM_OUTLET, createSpravOutlet(10),
            ALREADY_PROCESSED_OUTLET, createSpravOutlet(20)
    );

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(shopInfo.getId()).thenReturn(SHOP_ID);
        when(shopInfo.getName()).thenReturn("shop name");
        when(shopInfo.getCampaignId()).thenReturn(-1L);
        when(shopInfo.getSuperAdminUid()).thenReturn(-10L);
        when(shopInfoService.getShopInfo(SHOP_ID)).thenReturn(shopInfo);

        var stTicket = createStTicket();
        when(stTicketHistoryService.findOutletIdsWithStTicketsCreatedInLastMonth()).thenReturn(Set.of(ALREADY_PROCESSED_OUTLET));
        when(startrekTicketManager.createTicket(any())).thenReturn(new StartrekTicketWithIssue(stTicket, null));
    }

    @Test
    void testCreateStTickets() {
        yellowMbiOutletStartrekTicketCreator.createStTickets(SHOP_ID, spravByClosestMbi, mbiOutletsByProblem);

        verify(startrekTicketManager).createTicket(any());
        verify(stTicketHistoryService).saveWithMbiOutlets(Set.of(PROBLEM_OUTLET), ST_TICKET_ID);
    }

    @Test
    void testCreateStTicketAllProcessed() {
        when(stTicketHistoryService.findOutletIdsWithStTicketsCreatedInLastMonth()).thenReturn(Set.of(PROBLEM_OUTLET, ALREADY_PROCESSED_OUTLET));

        yellowMbiOutletStartrekTicketCreator.createStTickets(SHOP_ID, spravByClosestMbi, mbiOutletsByProblem);
        verify(startrekTicketManager, never()).createTicket(any());
        verify(stTicketHistoryService, never()).saveWithMbiOutlets(any(), anyLong());
    }

    @Test
    void buildDescriptionPartTest() {
        var descriptionPart = YellowMbiOutletStartrekTicketCreator.buildDescriptionPart(
                createMbiOutlet(1), createSpravOutlet(10), shopInfo
        );
        assertEquals("" +
                        "permalink: 10\n" +
                        "status: status\n" +
                        "адрес Справочник: 10\n" +
                        "адрес MBI: 1\n" +
                        "расписание Справочник: пн-вс 09:00-21:00\n" +
                        "расписание MBI: пн-вс 09:00-21:00\n" +
                        "Ссылка на Карты: https://yandex.ru/maps/org/10/\n" +
                        "Точка в партнерке: " +
                        "https://partner.market.yandex.ru/fmcg/-1/outlets/1?euid=-10\n",
                descriptionPart);
    }

    private static SimpleOutletInfo createMbiOutlet(long id) {
        var outlet = new SimpleOutletInfo();
        outlet.setId(id);
        outlet.setAddress(new Address.Builder().setCity(Long.toString(id)).build());
        outlet.setScheduleLines(List.of(new ScheduleLine(ScheduleLine.DayOfWeek.MONDAY, 6, 540, 720)));
        return outlet;
    }

    private static YellowSpravOutlet createSpravOutlet(long permalink) {
        var outlet = mock(YellowSpravOutlet.class);
        when(outlet.getPermalink()).thenReturn(permalink);
        when(outlet.getFormattedAddress()).thenReturn(Long.toString(permalink));
        when(outlet.getStatus()).thenReturn("status");
        when(outlet.getWorkIntervals()).thenReturn(List.of(new MapsScheduleLine(MapsScheduleDay.EVERYDAY, 540, 1260)));
        return outlet;
    }

    private static StartrekTicket createStTicket() {
        var ticket = mock(StartrekTicket.class);
        when(ticket.getId()).thenReturn(ST_TICKET_ID);
        return ticket;
    }
}
