package ru.yandex.market.abo.core.ticket.listener.problem;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.indexer.GenerationService;
import ru.yandex.market.abo.core.indexer.GenerationServiceTest;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.problem.model.Problem;
import ru.yandex.market.abo.core.problem.model.ProblemStatus;
import ru.yandex.market.abo.core.problem.model.ProblemTypeId;
import ru.yandex.market.abo.core.shop.ShopInfo;
import ru.yandex.market.abo.core.shop.ShopInfoService;
import ru.yandex.market.abo.core.startrek.StartrekTicketManager;
import ru.yandex.market.abo.core.ticket.OfferDbService;
import ru.yandex.market.abo.core.ticket.ProblemManager;
import ru.yandex.market.abo.core.ticket.TicketService;
import ru.yandex.market.abo.core.ticket.TicketTagService;
import ru.yandex.market.abo.core.ticket.model.CheckMethod;
import ru.yandex.market.abo.core.ticket.model.Ticket;
import ru.yandex.market.abo.core.ticket.model.TicketTag;
import ru.yandex.market.abo.gen.HypothesisService;
import ru.yandex.market.abo.gen.model.Hypothesis;
import ru.yandex.market.abo.util.FakeUsers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author imelnikov
 */
public class VendorProblemTypeListenerTest extends EmptyTest {

    @InjectMocks
    @Autowired
    VendorProblemTypeListener listener;

    @Mock
    private StartrekTicketManager stManager;
    @Mock
    private ShopInfoService shopInfoService;
    @Mock
    private OfferDbService offerDbService;

    @Autowired
    private TicketTagService tagService;
    @Autowired
    private TicketService ticketService;
    @Autowired
    private HypothesisService hypothesisService;
    @Autowired
    private ProblemManager problemManager;
    @Autowired
    private GenerationService generationService;

    @BeforeEach
    public void initMock() {
        openMocks(this);
        problemManager.setProblemListeners(Collections.singletonList(listener));
        generationService.storeGeneration(GenerationServiceTest.createGeneration(1, true));
    }

    @Test
    public void fire() {
        doReturn(new ShopInfo()).when(shopInfoService).getShopInfo(anyLong());
        Offer offer = mock(Offer.class);
        doReturn(1L).when(offer).getShopId();
        doReturn("aaa").when(offer).getName();
        doReturn(offer).when(offerDbService).loadOfferByHypId(anyLong());

        Hypothesis hypothesis = new Hypothesis(1L, 0, 1, null, 0.0, 0, "");
        hypothesisService.save(hypothesis);
        TicketTag tag = tagService.createTag(FakeUsers.PROBLEM_AUTO_APPROVER.getId());
        Ticket ticket = new Ticket(hypothesis, null, 213, CheckMethod.DEFAULT);
        ticketService.saveTicket(ticket, tag);
        Problem p = Problem.newBuilder()
                .problemTypeId(ProblemTypeId.NO_VENDOR_PROBLEM)
                .ticketId(ticket.getId())
                .status(ProblemStatus.NEW)
                .build();
        problemManager.saveProblem(p, tag);

        Mockito.verify(stManager).createTicket(any());
    }
}
