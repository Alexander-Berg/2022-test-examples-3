package ru.yandex.market.abo.core.autoorder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.EmptyTestWithTransactionTemplate;
import ru.yandex.market.abo.core.autoorder.callcenter.AutoOrderCallCenterEventService;
import ru.yandex.market.abo.core.autoorder.callcenter.AutoOrderPhoneManager;
import ru.yandex.market.abo.core.autoorder.callcenter.AutoOrderPhoneTicketService;
import ru.yandex.market.abo.core.autoorder.callcenter.model.AutoOrderCallCenterEvent;
import ru.yandex.market.abo.core.autoorder.callcenter.model.AutoOrderPhoneTicket;
import ru.yandex.market.abo.core.autoorder.code.AutoOrderCodeService;
import ru.yandex.market.abo.core.autoorder.response.AutoOrderResponse;
import ru.yandex.market.abo.core.autoorder.response.AutoOrderResponseService;
import ru.yandex.market.abo.core.autoorder.response.AutoOrderStatus;
import ru.yandex.market.abo.core.premod.PremodItemService;
import ru.yandex.market.abo.core.premod.PremodManager;
import ru.yandex.market.abo.core.premod.PremodTicketService;
import ru.yandex.market.abo.core.premod.model.PremodCheckType;
import ru.yandex.market.abo.core.premod.model.PremodItem;
import ru.yandex.market.abo.core.premod.model.PremodItemStatus;
import ru.yandex.market.abo.core.premod.model.PremodItemType;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.screenshot.ScreenshotService;
import ru.yandex.market.abo.core.shop.schedule.ShopWorkingPeriodManager;
import ru.yandex.market.abo.core.ticket.ProblemManager;
import ru.yandex.market.abo.core.ticket.TicketService;
import ru.yandex.market.abo.core.ticket.TicketTagService;
import ru.yandex.market.abo.core.ticket.TicketType;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.core.ticket.model.Ticket;
import ru.yandex.market.abo.gen.model.Hypothesis;
import ru.yandex.market.abo.mm.db.DbMailAccountService;
import ru.yandex.market.abo.mm.db.DbMailService;
import ru.yandex.market.abo.mm.model.Account;
import ru.yandex.market.abo.mm.model.Message;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.autoorder.AutoOrderManager.MAX_PROCESSING_TIME_MINUTES;
import static ru.yandex.market.abo.core.autoorder.AutoOrderManager.MINUTES_DELAY;
import static ru.yandex.market.abo.core.problem.model.ProblemStatus.APPROVED;
import static ru.yandex.market.abo.core.problem.model.ProblemStatus.NEW;
import static ru.yandex.market.abo.core.problem.model.ProblemTypeId.AUTO_ORDER_RESPONSE;
import static ru.yandex.market.abo.core.screenshot.ScreenshotSource.TICKET;
import static ru.yandex.market.abo.core.screenshot.ScreenshotSource.TICKET_PROBLEM;

/**
 * @author antipov93.
 */
class AutoOrderManagerTest extends EmptyTestWithTransactionTemplate {

    private static final int ORDERS_SIZE_CORE = 9;
    private static final int ORDERS_SIZE_PREMOD = 2;
    private static final long PROBLEM_ID = 11;
    private static final long YA_UID = -1L;
    private static final List<Long> HYP_IDS =
            LongStream.range(1, ORDERS_SIZE_CORE + ORDERS_SIZE_PREMOD + 1).boxed().collect(toList());
    private static final List<Long> CORE_HYP_IDS = HYP_IDS.subList(0, ORDERS_SIZE_CORE);
    private static final List<Long> PREMOD_TICKET_IDS =
            HYP_IDS.subList(ORDERS_SIZE_CORE, ORDERS_SIZE_CORE + ORDERS_SIZE_PREMOD);
    private static final LocalDateTime NOW = LocalDateTime.now().minusMinutes(MINUTES_DELAY);
    private static final LocalDateTime LATEST_LOADED_MESSAGE = nowPlus(-10);
    private static final LocalDateTime BEFORE_LATEST_MESSAGE = nowPlus(-20);
    private static final LocalDateTime AFTER_LATEST_MESSAGE = nowPlus(-5);
    private static final LocalDateTime AFTER_NOW = nowPlus(10);

    @InjectMocks
    private AutoOrderManager autoOrderManager;

    @Mock
    private AutoOrderResponseService autoOrderResponseService;
    @Mock
    private AutoOrderCodeService autoOrderCodeService;
    @Mock
    private DbMailAccountService dbMailAccountService;
    @Mock
    private TicketService ticketService;
    @Mock
    private DbMailService dbMailService;
    @Mock
    private ShopWorkingPeriodManager shopWorkingPeriodManager;
    @Mock
    private ProblemManager problemManager;
    @Mock
    private TicketTagService ticketTagService;
    @Mock
    private ScreenshotService screenshotService;
    @Mock
    private PremodTicketService premodTicketService;
    @Mock
    private PremodItemService premodItemService;
    @Mock
    private PremodManager premodManager;
    @Mock
    private AutoOrderPhoneTicketService autoOrderPhoneTicketService;
    @Mock
    private AutoOrderCallCenterEventService autoOrderCallCenterEventService;
    @Mock
    private AutoOrderPhoneManager autoOrderPhoneManager;

    private AutoOrderResponse[] autoOrderResponses;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        initResponsesArray();

        mockAutoOrderResponseService();

        mockAutoOrderCodeService();
        mockDbMailAccountService();
        mockTicketService();
        mockPremodServices();

        mockDbMailService();
        mockProblemManager();
        mockShopWorkingPeriodManager();

        mockPhoneServices();
    }

    @Test
    void testLoadOrders() {
        Collection<AutoOrder> autoOrders = autoOrderManager.loadOrders();
        assertEquals(HYP_IDS.size(), autoOrders.size());
        assertTrue(autoOrders.stream().allMatch(
                autoOrder -> autoOrder.getHypId() == autoOrder.getShopId() &&
                        autoOrder.getHypId() == autoOrder.getAccountId() &&
                        String.valueOf(autoOrder.getHypId()).equals(autoOrder.getCode())
                )
        );
    }

    /**
     * ticketId=3, failed=false, NO mail
     */
    @Test
    void testDidNotReceiveEmailAndTimeIsOver() {
        autoOrderManager.processOrderResponses();

        int index = 2;
        long hypId = 3;

        verify(problemManager, times(1)).saveProblem(
                eq(Problem.newBuilder()
                        .ticketId(hypId)
                        .status(NEW)
                        .problemTypeId(AUTO_ORDER_RESPONSE)
                        .build()),
                any()
        );

        verify(ticketTagService, atLeast(1)).createTag(YA_UID);
        verify(screenshotService, times(1)).reattachExistingScreenshots(
                hypId, TICKET,
                PROBLEM_ID, TICKET_PROBLEM
        );

        verify(autoOrderResponseService, times(1)).update(autoOrderResponses[index]);

        assertNull(autoOrderResponses[index].getReceiveTime());
        assertEquals(AutoOrderStatus.FAILED, autoOrderResponses[index].getStatus());
    }

    /**
     * ticketId=1, failed=false, mail found before deadline
     * ticketId=7, failed=false, mail found after deadline
     * ticketId=8, failed=false, mail found before deadline
     * ticketId=9, failed=false, mail found after deadline
     */
    @Test
    void testResponseHasBeenFound() {
        autoOrderManager.processOrderResponses();

        int notFailedIndex = 0;
        int notFailedAfterDeadline = 6;
        int beforeDeadline = 7;
        int afterDeadline = 8;

        Arrays.asList(notFailedIndex, notFailedAfterDeadline, beforeDeadline).forEach(index -> {
            verify(autoOrderResponseService, times(2)).update(autoOrderResponses[index]);

            assertNotNull(autoOrderResponses[index].getReceiveTime());
            assertEquals(30L, (long) autoOrderResponses[index].getProcessingTime());
        });

        assertEquals(AutoOrderStatus.SUCCESS, autoOrderResponses[notFailedIndex].getStatus());
        assertEquals(AutoOrderStatus.FAILED, autoOrderResponses[notFailedAfterDeadline].getStatus());

        assertEquals(AutoOrderStatus.SUCCESS, autoOrderResponses[beforeDeadline].getStatus());
        assertEquals(AutoOrderStatus.FAILED, autoOrderResponses[afterDeadline].getStatus());
    }

    /**
     * ticketId = 2, failed=false, NO mail
     */
    @Test
    void testDidNotReceiveEmailButTimeIsNotOver() {
        autoOrderManager.processOrderResponses();

        int index = 1;
        assertNull(autoOrderResponses[index].getReceiveTime());
        assertEquals(AutoOrderStatus.PROCESSING, autoOrderResponses[index].getStatus());
    }

    /**
     * ticketId=5, failed=true, NO mail
     */
    @Test
    void testDidNotReceiveEmailAndHasAlreadyBeenFailed() {
        autoOrderManager.processOrderResponses();

        int index = 4;
        assertNull(autoOrderResponses[index].getReceiveTime());
        assertEquals(AutoOrderStatus.FAILED, autoOrderResponses[index].getStatus());
    }

    /**
     * ticketId = 6, failed = false, NO mail
     */
    @Test
    void testDidNotReceiveEmailButDeadlineAfterLatestMessage() {
        autoOrderManager.processOrderResponses();
        int index = 5;
        assertNull(autoOrderResponses[index].getReceiveTime());
        assertEquals(AutoOrderStatus.PROCESSING, autoOrderResponses[index].getStatus());
    }

    @ParameterizedTest
    @CsvSource({"9, SUCCESS", "10, FAILED"})
    void testPremodComplete(int index, AutoOrderStatus status) {
        var itemMock = mock(PremodItem.class);
        when(itemMock.getStatus()).thenReturn(PremodItemStatus.NEW);
        when(premodItemService.loadPremodItemByTicketIdAndType(eq(autoOrderResponses[index].getHypId()), eq(PremodItemType.AUTOORDER)))
                .thenReturn(itemMock);
        autoOrderManager.processOrderResponses();
        assertNotNull(autoOrderResponses[index].getReceiveTime());
        assertEquals(status, autoOrderResponses[index].getStatus());
        var verification = AutoOrderStatus.SUCCESS == status ? times(1) : never();
        verify(itemMock, verification).setStatus(PremodItemStatus.PASS);
        verify(premodManager, verification).updatePremodItem(itemMock);
    }

    @Test
    void testAutoOrderWithPhone() {
        autoOrderManager.processOrderResponses();
        int index = 3;

        assertNotNull(autoOrderResponses[index].getReceiveTime());
        assertEquals(AutoOrderStatus.SUCCESS, autoOrderResponses[index].getStatus());
        verify(autoOrderPhoneTicketService).save(any());
        verify(autoOrderResponseService, times(2)).update(autoOrderResponses[index]);
        verify(autoOrderPhoneManager).unbindBoundPhoneByTicket(4L);
    }

    private void initResponsesArray() {
        autoOrderResponses = new AutoOrderResponse[]{
                createOrder(1, null, TicketType.CORE),
                createOrder(2, null, TicketType.CORE),
                createOrder(3, null, TicketType.CORE),
                createOrder(4, null, TicketType.CORE), //with phone
                createOrder(5, null, TicketType.CORE),
                createOrder(6, null, TicketType.CORE),
                createOrder(7, null, TicketType.CORE),
                createOrder(8, null, TicketType.CORE), // before deadline
                createOrder(9, null, TicketType.CORE), // after deadline
                createOrder(10, null, TicketType.PREMOD),
                createOrder(11, null, TicketType.PREMOD)
        };
    }

    private void mockAutoOrderResponseService() {
        when(autoOrderResponseService.loadTicketsForCheck()).thenReturn(Arrays.asList(autoOrderResponses));
    }

    private void mockAutoOrderCodeService() {
        when(autoOrderCodeService.loadCodes(HYP_IDS))
                .thenReturn(HYP_IDS.stream().collect(toMap(identity(), String::valueOf)));
    }


    private void mockDbMailAccountService() {
        var hypId2Account = StreamEx.of(HYP_IDS)
                .mapToEntry(hypId -> new Account(hypId, Long.toString(hypId), null, null))
                .toMap();
        when(dbMailAccountService.loadAccountsByHypIds(HYP_IDS))
                .thenReturn(hypId2Account);
    }

    private void mockTicketService() {
        when(ticketService.loadTicketByIds(HYP_IDS))
                .thenReturn(CORE_HYP_IDS.stream().map(id -> {
                    Hypothesis h = new Hypothesis(id, 0, 0, null, 0, 0, "");
                    h.setId(id);
                    return new Ticket(h, null, 0, CheckMethod.AUTO_ORDER);
                }).collect(toList()));

        when(ticketService.yaUidsWhoClosedTickets(any())).thenReturn(
                CORE_HYP_IDS.stream().collect(Collectors.toMap(
                        Function.identity(),
                        hypId -> YA_UID
                ))
        );
    }

    @SuppressWarnings("unchecked")
    private void mockDbMailService() {
        when(dbMailService.findMessage(eq("1"), any(), any())).thenReturn(findMessageResponse(true));
        when(dbMailService.findMessage(eq("2"), any(), any())).thenReturn(findMessageResponse(false));
        when(dbMailService.findMessage(eq("3"), any(), any())).thenReturn(findMessageResponse(false));
        when(dbMailService.findMessage(eq("4"), any(), any())).thenReturn(findMessageResponse(true));
        when(dbMailService.findMessage(eq("5"), any(), any())).thenReturn(findMessageResponse(false));
        when(dbMailService.findMessage(eq("6"), any(), any())).thenReturn(findMessageResponse(false));
        when(dbMailService.findMessage(eq("7"), any(), any())).thenReturn(findMessageResponse(true,
                Date.from(BEFORE_LATEST_MESSAGE.plusMinutes(1L).atZone(ZoneId.systemDefault()).toInstant())));
        when(dbMailService.findMessage(eq("8"), any(), any())).thenReturn(findMessageResponse(true,
                Date.from(BEFORE_LATEST_MESSAGE.minusMinutes(1).atZone(ZoneId.systemDefault()).toInstant())));
        when(dbMailService.findMessage(eq("9"), any(), any())).thenReturn(findMessageResponse(true,
                Date.from(BEFORE_LATEST_MESSAGE.plusMinutes(1).atZone(ZoneId.systemDefault()).toInstant())));
        when(dbMailService.findMessage(eq("10"), any(), any())).thenReturn(findMessageResponse(true));
        when(dbMailService.findMessage(eq("11"), any(), any())).thenReturn(findMessageResponse(true,
                Date.from(BEFORE_LATEST_MESSAGE.plusMinutes(1L).atZone(ZoneId.systemDefault()).toInstant())));

        when(dbMailService.latestMessageTimes(any())).then(inv -> {
            Collection<Long> accountIds = (Collection<Long>) inv.getArguments()[0];
            return accountIds.stream().collect(toMap(Function.identity(), acc -> LATEST_LOADED_MESSAGE));
        });
    }

    private Optional<Message> findMessageResponse(boolean find) {
        return findMessageResponse(find,
                Date.from(BEFORE_LATEST_MESSAGE.minusMinutes(5L).atZone(ZoneId.systemDefault()).toInstant())
        );
    }

    private Optional<Message> findMessageResponse(boolean find, Date date) {
        if (!find) {
            return Optional.empty();
        }
        Message message = new Message(0);
        message.setTime(date);
        return Optional.of(message);
    }

    private void mockShopWorkingPeriodManager() {
        //code has been found -> updateReceiveTime
        Stream.of(1L, 4L, 7L, 8L, 10L, 11L).forEach(id ->
                when(shopWorkingPeriodManager.workTimeDiff(eq(id), any(), any())).thenReturn(30)
        );

        // deadline has passed and there are messages after it
        Arrays.asList(1L, 4L, 7L, 8L, 9L, 10L, 11L).forEach(hypId ->
                when(shopWorkingPeriodManager.addWorkingMinutes(eq(hypId), any(), eq(MAX_PROCESSING_TIME_MINUTES)))
                        .thenReturn(BEFORE_LATEST_MESSAGE)
        );

        // 2 & 3 & 6 - code hasn't been found -> check for creating problem
        when(shopWorkingPeriodManager.addWorkingMinutes(eq(2L), any(), eq(MAX_PROCESSING_TIME_MINUTES)))
                .thenReturn(AFTER_NOW); // deadline has not passed
        when(shopWorkingPeriodManager.addWorkingMinutes(eq(3L), any(), eq(MAX_PROCESSING_TIME_MINUTES)))
                .thenReturn(BEFORE_LATEST_MESSAGE); // deadline has passed and there are messages after it
        when(shopWorkingPeriodManager.addWorkingMinutes(eq(6L), any(), eq(MAX_PROCESSING_TIME_MINUTES)))
                .thenReturn(AFTER_LATEST_MESSAGE); // deadline has passed and there aren't messages after it

        when(shopWorkingPeriodManager.addWorkingMinutes(eq(5L), any(), eq(MAX_PROCESSING_TIME_MINUTES)))
                .thenReturn(BEFORE_LATEST_MESSAGE);
    }

    private void mockPhoneServices() {
        when(autoOrderCallCenterEventService.lastEventTime()).thenReturn(LATEST_LOADED_MESSAGE);

        when(autoOrderPhoneTicketService.load(any())).thenReturn(List.of(
                new AutoOrderPhoneTicket(4, "4")
        ));

        when(autoOrderCallCenterEventService.findFirstByPhoneAfter(eq("4"), any()))
                .thenReturn(Optional.of(new AutoOrderCallCenterEvent("", "", "4", BEFORE_LATEST_MESSAGE)));
    }

    private void mockProblemManager() {
        when(problemManager.saveProblem(any(), any())).thenReturn(Problem.newBuilder()
                .id(PROBLEM_ID)
                .ticketId(3)
                .status(APPROVED)
                .problemTypeId(AUTO_ORDER_RESPONSE)
                .build()
        );
    }

    private void mockPremodServices() {
        when(premodTicketService.loadTicketsByIds(HYP_IDS))
                .thenReturn(PREMOD_TICKET_IDS.stream().map(id -> {
                    var ticket = new PremodTicket(id, 0, PremodCheckType.CPC_PREMODERATION);
                    ticket.setId(id);
                    return ticket;
                }).collect(toList()));
    }

    private static LocalDateTime nowPlus(int minutes) {
        return NOW.plusMinutes(minutes);
    }

    private static AutoOrderResponse createOrder(long hypId, LocalDateTime receiveTime, TicketType ticketType) {
        var order = new AutoOrderResponse(hypId, ticketType);
        order.setReceiveTime(receiveTime);
        return order;
    }
}
