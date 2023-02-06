package ru.yandex.market.abo.core.premod;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import one.util.streamex.StreamEx;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.CoreConfig;
import ru.yandex.market.abo.core.autoorder.response.AutoOrderResponseService;
import ru.yandex.market.abo.core.premod.model.PremodCheckMethod;
import ru.yandex.market.abo.core.premod.model.PremodCheckType;
import ru.yandex.market.abo.core.premod.model.PremodItem;
import ru.yandex.market.abo.core.premod.model.PremodItemType;
import ru.yandex.market.abo.core.premod.model.PremodProblem;
import ru.yandex.market.abo.core.premod.model.PremodTicket;
import ru.yandex.market.abo.core.premod.model.PremodTicketStatus;
import ru.yandex.market.abo.core.premod.model.PremodTicketSubstatus;
import ru.yandex.market.abo.core.premod.model.PremoderationTicketRequest;
import ru.yandex.market.abo.core.premod.service.PremodRepo;
import ru.yandex.market.abo.core.ticket.TicketType;
import ru.yandex.market.abo.mm.db.DbMailAccountService;
import ru.yandex.market.abo.mm.model.AccountStatus;
import ru.yandex.market.abo.mm.model.AccountType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.premod.model.PremodItemStatus.FAILED;
import static ru.yandex.market.abo.core.premod.model.PremodItemStatus.NEW;

/**
 * @author mixey
 * @date 10.11.2008
 */
class PremodManagerIntegrationTest extends EmptyTest {
    private static final long SHOP_ID = -2;
    private static final long USER_ID = -1;
    private static final String SHOP_NAME = "This is shop";
    private static final int AUTO_ORDER_PROBLEM = 243;

    private static final String DSBS_PREMOD_FAIL_TEMPLATE =
            "{$date} мы попросили вас прислать документы, без которых нельзя продолжить модерацию магазина {$shop_name}.";

    @Autowired
    private PremodManager premodManager;
    @Autowired
    private PremodProblemManager premodProblemManager;
    @Autowired
    private PremodTicketService premodTicketService;
    private PremodTicketService premodTicketServiceSpy;
    @Autowired
    private PremodItemService premodItemService;
    @Autowired
    private PremodProblemService premodProblemService;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;
    @Autowired
    private AutoOrderResponseService autoOrderResponseService;
    @Autowired
    private DbMailAccountService dbMailAccountService;
    @Autowired
    private PremodRepo.PremodTicketRepo premodTicketRepo;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        premodTicketServiceSpy = spy(premodTicketService);
        premodManager.setPremodTicketService(premodTicketServiceSpy);
        premodManager.setTicketListeners(new ArrayList<>());

        HttpResponse httpResponse = mock(HttpResponse.class);
        HttpEntity httpEntity = mock(HttpEntity.class);
        when(httpResponse.getEntity()).thenReturn(httpEntity);
        StatusLine statusLine = mock(StatusLine.class);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_UNAUTHORIZED);

        pgJdbcTemplate.update("INSERT INTO shop (id, name) VALUES (?, ?)", SHOP_ID, SHOP_NAME);
        pgJdbcTemplate.update("INSERT INTO ext_campaign_info (campaign_id, shop_id) VALUES (1, ?)", SHOP_ID);
        pgJdbcTemplate.update("UPDATE core_config SET value = '1' WHERE id = ?", CoreConfig.PREMOD_AUTO_ORDER.getId());
        var accountId = dbMailAccountService.storeAccount("", 0L, AccountType.AUTO_PREMOD);
        dbMailAccountService.setAccountStatus(accountId, AccountStatus.ACTIVE);

        pgJdbcTemplate.update("INSERT INTO message_template(id, name, description, message, subject, from_alias, reply_alias) " +
                "VALUES(1105, 'dsbs_premod_need_info_fail', 'Имя шаблона', ?, '---', 'AboDevelopers', 'AboDevelopers')",
                DSBS_PREMOD_FAIL_TEMPLATE);
    }

    @Test
    void provideDocsTest() {
        long ticketId = pgJdbcTemplate.queryForObject("SELECT nextval('s_hypothesis')", Long.class);
        pgJdbcTemplate.update("" +
                        "INSERT INTO premod_ticket (id, creation_time, modification_time, status_id, substatus_id, " +
                        "check_type, shop_id, try_number, check_method_id) " +
                        "VALUES ( ?, now(), now(), ?, ?, ?, ?, 1, ? )",
                ticketId,
                PremodTicketStatus.FAILED.getId(),
                PremodTicketSubstatus.NEED_INFO.getId(),
                PremodCheckType.CPC_PREMODERATION.getId(),
                SHOP_ID,
                PremodCheckMethod.DEFAULT.getId()
        );

        premodManager.provideDocuments(ticketId, PremodTicketSubstatus.PASS, USER_ID);

        PremodTicket ticket = premodTicketService.loadPremodTicket(ticketId);
        assertEquals(PremodTicketStatus.PASS, ticket.getStatus());
        assertEquals(PremodTicketSubstatus.PASS, ticket.getSubstatus());
    }

    @Test
    void provideDocsDBSFailTest() {
        PremodTicket initialTicket = new PremodTicket(SHOP_ID, 1, PremodCheckType.CPA_PREMODERATION);
        initialTicket.setStatus(PremodTicketStatus.FAILED);
        initialTicket.setSubstatus(PremodTicketSubstatus.NEED_INFO);
        initialTicket.setCreationTime(Date.valueOf("2021-06-01"));
        initialTicket.setModificationTime(Date.valueOf("2021-06-10"));
        initialTicket = premodTicketRepo.save(initialTicket);

        long ticketId = initialTicket.getId();

        premodManager.provideDocuments(ticketId, PremodTicketSubstatus.FAIL_SINGLE_CHECK, USER_ID);

        String testMessage = String.format("%s мы попросили вас прислать документы, без которых нельзя " +
                "продолжить модерацию магазина %s.", "10-06-2021", SHOP_NAME);

        PremodTicket ticket = premodTicketService.loadPremodTicket(ticketId);
        assertEquals(PremodTicketStatus.FAILED, ticket.getStatus());
        assertEquals(PremodTicketSubstatus.FAIL_SINGLE_CHECK, ticket.getSubstatus());
        assertTrue(ticket.getRecommendation().contains(testMessage),
                String.format("%s\n\nexpected to contain\n\n%s", ticket.getRecommendation(), testMessage));
    }

    @Test
    void provideDocsCPCIgnoreRecommendation() {
        String recommendation = "This is CPC recommendation";

        PremodTicket initialTicket = new PremodTicket(SHOP_ID, 1, PremodCheckType.CPC_PREMODERATION);
        initialTicket.setStatus(PremodTicketStatus.FAILED);
        initialTicket.setSubstatus(PremodTicketSubstatus.NEED_INFO);
        initialTicket.setRecommendation(recommendation);
        initialTicket = premodTicketRepo.save(initialTicket);

        long ticketId = initialTicket.getId();

        premodManager.provideDocuments(ticketId, PremodTicketSubstatus.FAIL_SINGLE_CHECK, USER_ID);

        PremodTicket ticket = premodTicketService.loadPremodTicket(ticketId);
        assertEquals(PremodTicketStatus.FAILED, ticket.getStatus());
        assertEquals(PremodTicketSubstatus.FAIL_SINGLE_CHECK, ticket.getSubstatus());
        assertEquals(recommendation, ticket.getRecommendation());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void revertAutoOrderTest_fail(boolean withOrder) {
        var item = createTicketAndLoadItem(PremodItemType.AUTOORDER);
        if (withOrder) {
            autoOrderResponseService.add(item.getTicketId(), TicketType.PREMOD);
        }
        var problem = createProblem(item.getId(), AUTO_ORDER_PROBLEM);
        premodProblemManager.addPremodProblems(item, List.of(problem), null);
        entityManager.flush();
        entityManager.clear();

        item = premodItemService.loadPremodItem(item.getId());
        assertEquals(FAILED, item.getStatus());
        assertFalse(premodProblemService.loadPremodProblemsByItem(item.getId()).isEmpty());
        assertEquals(withOrder, autoOrderResponseService.load(item.getTicketId()) != null);

        premodManager.revertAutoOrder(item.getTicketId());
        entityManager.flush();
        entityManager.clear();

        item = premodItemService.loadPremodItem(item.getId());
        assertEquals(NEW, item.getStatus());
        assertTrue(premodProblemService.loadPremodProblemsByItem(item.getId()).isEmpty());
        assertEquals(withOrder, autoOrderResponseService.load(item.getTicketId()) != null);
    }

    @Test
    void revertAutoOrderTest_newWithOrder() {
        var item = createTicketAndLoadItem(PremodItemType.AUTOORDER);
        autoOrderResponseService.add(item.getTicketId(), TicketType.PREMOD);
        entityManager.flush();
        entityManager.clear();

        item = premodItemService.loadPremodItem(item.getId());
        assertEquals(NEW, item.getStatus());
        assertTrue(premodProblemService.loadPremodProblemsByItem(item.getId()).isEmpty());
        assertNotNull(autoOrderResponseService.load(item.getTicketId()));

        premodManager.revertAutoOrder(item.getTicketId());
        entityManager.flush();
        entityManager.clear();

        item = premodItemService.loadPremodItem(item.getId());
        assertEquals(NEW, item.getStatus());
        assertTrue(premodProblemService.loadPremodProblemsByItem(item.getId()).isEmpty());
        assertNull(autoOrderResponseService.load(item.getTicketId()));
    }

    private List<PremodItem> createTicketAndLoadItems(PremodCheckType testingType) {
        long premodTicket = premodManager.createPremodTicket(
                new PremoderationTicketRequest(SHOP_ID, testingType, false, RND.nextInt(5) + 1));
        return premodItemService.loadPremodItemsByTicket(premodTicket);
    }

    private PremodItem createTicketAndLoadItem(PremodItemType itemType) {
        return StreamEx.of(createTicketAndLoadItems(PremodCheckType.CPC_PREMODERATION))
                .findFirst(item -> itemType == item.getType())
                .orElse(null);
    }

    private static PremodProblem createProblem(long itemId, int typeId) {
        return new PremodProblem(itemId, typeId, USER_ID, null);
    }
}
