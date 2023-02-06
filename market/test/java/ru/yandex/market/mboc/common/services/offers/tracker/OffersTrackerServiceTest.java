package ru.yandex.market.mboc.common.services.offers.tracker;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.bolts.collection.impl.EmptyMap;
import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.offers.OfferProcessingStrategiesHolder;
import ru.yandex.market.mboc.common.services.offers.processing.MatchingOffersProcessingStrategy;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStrategy;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;

import static org.junit.Assert.assertEquals;

/**
 * @author york
 * @since 01.09.2020
 */
public class OffersTrackerServiceTest {
    private static final int DIFF_SUPPLIERS = 4;
    private static long idSeq = 1;
    private OffersTrackerService offersTrackerService;
    private OfferRepositoryMock offerRepositoryMock;
    private TrackerServiceMock trackerServiceMock;

    @Before
    public void setUp() {
        offerRepositoryMock = new OfferRepositoryMock();
        trackerServiceMock = new TrackerServiceMock();

        MatchingOffersProcessingStrategy matchingOffersProcessingStrategy =
            Mockito.mock(MatchingOffersProcessingStrategy.class);
        Mockito.when(matchingOffersProcessingStrategy.getType()).thenReturn(TicketType.MATCHING);
        Mockito.when(matchingOffersProcessingStrategy.createTrackerTicket(Mockito.any(OffersFilter.class)))
            .then(invocation -> {
                OffersFilter filter = invocation.getArgument(0);
                Issue issue = trackerServiceMock.createTicket(
                    "title", "description", "author", TicketType.MATCHING,
                    null);
                List<Offer> offerList = offerRepositoryMock.findOffers(filter);
                offerList.forEach(o -> {
                    o.setTrackerTicket(issue);
                    o.updateProcessingStatusIfValid(Offer.ProcessingStatus.IN_PROCESS);
                });
                offerRepositoryMock.updateOffers(offerList);
                return OffersProcessingStrategy.OptionalTicket.ofTicket(issue);
        });
        //noinspection unchecked
        Mockito.when(matchingOffersProcessingStrategy.splitOffers(Mockito.anyList())).thenAnswer(
            (Answer<Stream<List<Offer>>>) invocation ->
                Stream.of((List<Offer>) invocation.getArgument(0))
        );
        OfferProcessingStrategiesHolder holder = new OfferProcessingStrategiesHolder(
            Collections.singletonList(matchingOffersProcessingStrategy)
        );
        offersTrackerService = new OffersTrackerService(trackerServiceMock, holder);
    }

    @Test
    public void testGroupAndCreateTickets() {
        var offer1 = createOffer();
        var offer2 = createOffer();
        var offer3 = createOffer();
        var offer4 = createOffer();
        var offers = Arrays.asList(offer1, offer2, offer3, offer4);
        for (var o: offers) {
            o.setSupplierId(99);
        }
        var testStrategy = Mockito.mock(OffersProcessingStrategy.class);
        var issue1 = createIssue();
        var issue2 = createIssue();
        Mockito.when(testStrategy.createTrackerTicket(Mockito.any(OffersFilter.class))).thenAnswer(
            (Answer<OffersProcessingStrategy.OptionalTicket>) invocation -> {
                var filter = (OffersFilter) invocation.getArguments()[0];
                if (filter.getOfferIds().equals(Collections.singletonList(offer1.getId()))) {
                    return OffersProcessingStrategy.OptionalTicket.ofTicket(issue1);
                }
                else if (filter.getOfferIds().equals(Arrays.asList(offer2.getId(), offer3.getId()))) {
                    return OffersProcessingStrategy.OptionalTicket.ofTicket(issue2);
                }
                else if (filter.getOfferIds().equals(Collections.singletonList(offer4.getId()))) {
                    return OffersProcessingStrategy.OptionalTicket.empty("some reason");
                }
                return OffersProcessingStrategy.OptionalTicket.ofError(
                    new IllegalArgumentException("Unknown offer filter: " + filter)
                );
            });
        Mockito.when(testStrategy.getType()).thenReturn(TicketType.CLASSIFICATION);
        Mockito.when(testStrategy.splitOffers(List.of(offer1, offer2, offer3, offer4)))
            .thenReturn(Stream.of(List.of(offer1), List.of(offer2, offer3), List.of(offer4)));
        var res = offersTrackerService.groupAndCreateTickets(offers, testStrategy, true);
        assertEquals(Arrays.asList(issue1, issue2), res);
        var res2 = offersTrackerService.groupAndCreateTickets(Collections.emptyList(), testStrategy, true);
        assertEquals(Collections.emptyList(), res2);
    }

    @SneakyThrows
    private Issue createIssue() {
        var id = idSeq++;
        //noinspection unchecked
        return new Issue(String.valueOf(id), new URI("http://localhost/" + id), "key", "summary", id,
            new EmptyMap<>(), Mockito.mock(Session.class));
    }

    private Offer createOffer() {
        long id = idSeq++;
        int supplierId = (int) (id % DIFF_SUPPLIERS);
        return new Offer()
            .setId(id)
            .setBusinessId(supplierId)
            .setShopSku("Sku-" + id)
            .addNewServiceOfferIfNotExistsForTests(new Supplier(supplierId, "test" + supplierId))
            .updateAcceptanceStatusForTests(Offer.AcceptanceStatus.OK)
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED)
            .setIsOfferContentPresent(true)
            .setShopCategoryName("shop_category_name")
            .storeOfferContent(OfferContent.builder().build())
            .setProcessingStatusInternal(Offer.ProcessingStatus.OPEN)
            .setTitle("Offer_" + id);
    }
}
