package ru.yandex.market.abo.gen.autoorder;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.autoorder.AutoOrderAccountRotationService;
import ru.yandex.market.abo.core.autoorder.code.AutoOrderCodeService;
import ru.yandex.market.abo.core.exception.ExceptionalShopReason;
import ru.yandex.market.abo.core.exception.ExceptionalShopsService;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.offer.report.OfferService;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.core.ticket.model.Ticket;
import ru.yandex.market.abo.core.ticket.model.TicketStatus;
import ru.yandex.market.abo.core.ticket.repository.TicketRepo;
import ru.yandex.market.abo.gen.HypothesisRepo;
import ru.yandex.market.abo.gen.model.GeneratorProfile;
import ru.yandex.market.abo.gen.model.Hypothesis;
import ru.yandex.market.abo.mm.db.DbMailAccountService;
import ru.yandex.market.abo.mm.model.Account;
import ru.yandex.market.abo.mm.model.AccountType;
import ru.yandex.market.common.report.model.FeedOfferId;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.abo.core.ticket.model.CheckMethod.AUTO_ORDER;
import static ru.yandex.market.abo.core.ticket.model.CheckMethod.BASKET;
import static ru.yandex.market.abo.core.ticket.model.CheckMethod.COMPLEX;
import static ru.yandex.market.abo.core.ticket.model.CheckMethod.PHONE;
import static ru.yandex.market.abo.core.ticket.model.TicketStatus.FINISHED;
import static ru.yandex.market.abo.core.ticket.model.TicketStatus.NEW;
import static ru.yandex.market.abo.core.ticket.model.TicketStatus.OPEN;

/**
 * @author antipov93.
 */
public class AutoOrderHypothesisGeneratorTest extends EmptyTest {
    private static final String ACCOUNT = "dev@null.com";
    private static final AccountType ACCOUNT_TYPE = AccountType.AUTO_CORE;

    @Autowired
    @InjectMocks
    private AutoOrderHypothesisGenerator autoOrderHypothesisGenerator;
    @Autowired
    private JdbcTemplate pgJdbcTemplate;
    @Autowired
    private AutoOrderCodeService autoOrderCodeService;
    @Autowired
    private HypothesisRepo hypothesisRepo;
    @Autowired
    private TicketRepo ticketRepo;

    @Mock
    private ExceptionalShopsService exceptionalShopsService;
    @Mock
    private AutoOrderAccountRotationService autoOrderAccountRotationService;
    @Mock
    private DbMailAccountService dbMailAccountService;
    @Mock
    private OfferService offerService;


    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        GeneratorProfile profile = new GeneratorProfile(101, 100, "");
        autoOrderHypothesisGenerator.configure(profile);

        createShopWithoutTicket(1, true, true); // all ok -> true
        createShopWithoutTicket(2, false, true); // cpc = 'OFF' -> false
        createShopWithoutTicket(3, true, false); // not in prod base -> false
        createShopWithoutTicket(4, true, true); // has exception for that check -> false

        createShopWithTicket(5, true, FINISHED, BASKET); // old ticket -> true
        createShopWithTicket(6, false, OPEN, BASKET); // not finished ticket -> true
        createShopWithTicket(7, false, FINISHED, PHONE); // phone -> true

        createShopWithTicket(8, false, FINISHED, BASKET); // FINISHED & BASKET -> false
        createShopWithTicket(9, false, FINISHED, COMPLEX); // FINISHED & COMPLEX -> false
        createShopWithTicket(10, false, FINISHED, AUTO_ORDER); // ticket exists -> false
        createShopWithTicket(11, false, NEW, AUTO_ORDER); // NEW & AUTO_ORDER-> false
        createShopWithTicket(12, false, OPEN, AUTO_ORDER); // OPEN & AUTO_ORDER-> false


        Offer offer = mock(Offer.class);
        when(offer.getFeedOfferId()).thenReturn(FeedOfferId.from(1, "1213"));
        when(offerService.findFirstWithParams(any())).thenReturn(offer);

        when(dbMailAccountService.loadAccountByHypId(anyLong()))
                .thenReturn(new Account(1, ACCOUNT, new Date(), AccountType.AUTO_CORE));

        when(exceptionalShopsService.loadShops(ExceptionalShopReason.AUTOORDER))
                .thenReturn(Set.of(4L, 8L, 9L, 10L, 11L, 12L));
    }

    @Test
    public void testGenerate() throws Exception {
        Offer offer = mock(Offer.class);
        when(offer.getFeedOfferId()).thenReturn(FeedOfferId.from(1, "1213"));
        when(offerService.findFirstWithParams(any())).thenReturn(offer);

        List<Hypothesis> generated = autoOrderHypothesisGenerator.generate();

        Set<Long> expectedShopIds = new HashSet<>(Arrays.asList(1L, 5L, 6L, 7L));
        assertEquals(expectedShopIds, generated.stream().map(Hypothesis::getShopId).collect(toSet()));
    }

    @Test
    public void testOfferHasNotBeenFound() throws Exception {
        when(offerService.findFirstWithParams(any())).thenReturn(null);

        List<Hypothesis> generated = autoOrderHypothesisGenerator.generate();
        assertEquals(0, generated.size());
    }

    @Test
    public void testBuildTicket() {
        long shopId = 14;
        Hypothesis hypothesis = hyp(shopId, 101);
        Ticket ticket = autoOrderHypothesisGenerator.buildTicket(hypothesis, 0);

        assertEquals(shopId, ticket.getShopId().longValue());
        assertEquals(AUTO_ORDER, ticket.getCheckMethod());

        Long hypId = hypothesis.getId();
        String code = autoOrderCodeService.loadCodes(Collections.singletonList(hypId)).get(hypId);
        assertNotNull(code);
        String splittedCode = code.substring(0, code.length() / 2) + " " + code.substring(code.length() / 2);

        Hypothesis loadedHypothesis = hypothesisRepo.findByIdOrNull(hypId);
        assertTrue(loadedHypothesis.getDescription().contains(splittedCode));

        verify(autoOrderAccountRotationService, times(1)).attachAccountToTicket(hypId, ACCOUNT_TYPE);
    }

    private void createShopWithTicket(long shopId, boolean old, TicketStatus status, CheckMethod checkMethod) {
        createShopAndTicket(shopId, true, true, false, old, status, checkMethod);
    }

    private void createShopWithoutTicket(long shopId, boolean cpc, boolean inProdBase) {
        createShopAndTicket(shopId, cpc, inProdBase, false, false, null, null);
    }

    private void createShopAndTicket(long shopId, boolean cpc, boolean inProdBase, boolean hasTicket,
                                     boolean old, TicketStatus status, CheckMethod checkMethod) {
        pgJdbcTemplate.update("INSERT INTO shop (id, cpc, in_prd_base, is_smb) VALUES (?, ?, ?, FALSE)",
                shopId, cpc ? "ON" : "OFF", inProdBase);
        pgJdbcTemplate.update("" +
                "INSERT INTO ext_shop_region (datasource_id, region_id, country_id) " +
                "VALUES (?, ?, ?)", shopId, Regions.MOSCOW, Regions.RUSSIA
        );

        if (!hasTicket) {
            return;
        }

        Hypothesis hyp = hyp(shopId, 1);
        ticket(hyp, DateUtils.addDays(new Date(), old ? -31 : 0), status, checkMethod);
    }

    private Hypothesis hyp(long shopId, int genId) {
        return hypothesisRepo.save(Hypothesis.builder(shopId, genId).build());
    }

    private void ticket(Hypothesis hyp, Date creationTime, TicketStatus status, CheckMethod checkMethod) {
        Ticket t = new Ticket(hyp, null, 0, checkMethod);
        t.setCreationTime(creationTime);
        t.setStatus(status);
        ticketRepo.save(t);
    }
}
