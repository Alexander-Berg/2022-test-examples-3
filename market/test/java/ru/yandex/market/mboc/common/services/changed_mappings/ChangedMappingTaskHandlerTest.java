package ru.yandex.market.mboc.common.services.changed_mappings;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.mbo.tracker.IssueMock;
import ru.yandex.market.mbo.tracker.TrackerServiceMock;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mbo.tracker.utils.TrackerServiceHelper;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.IssueRef;
import ru.yandex.startrek.client.model.UserRef;

public class ChangedMappingTaskHandlerTest {
    private static final long OLD_MODEL_ID = 1;
    private static final String OLD_MODEL_NAME = "OLD MODEL";
    private static final long NEW_MODEL_ID = 2;

    private TrackerServiceMock trackerServiceMock;
    private ChangedMappingTaskHandler handler;
    private TrackerServiceHelper trackerServiceHelper = new TrackerServiceHelper(null,
        null, null, null,
        null, null
    );

    @Before
    public void setup() {
        trackerServiceMock = new TrackerServiceMock();
        handler = new ChangedMappingTaskHandler(trackerServiceMock, trackerServiceHelper);
        ReflectionTestUtils.setField(handler, "queue", "TESTQUEUE");
    }

    @Test
    public void shouldCreateTicketsFromTasks() {
        handler.handle(changedMappingTask(
            Collections.emptyList(), Collections.emptyList(),
            Collections.singletonList(offer(NEW_MODEL_ID))), null);

        IssueMock issue = (IssueMock) trackerServiceMock.getAllTickets().iterator().next();

        Assertions.assertThat(issue.getSummary())
            .startsWith(ChangedMappingTaskHandler.TICKET_TITLE_PREFIX + " " + OLD_MODEL_ID + " " + OLD_MODEL_NAME);
        Assertions.assertThat(issue.getDescription().get())
            .startsWith(ChangedMappingTaskHandler.MOVED_MAPPINGS_PREFIX)
            .contains(handler.getChangeTypeDisplayName(
                MboMappings.ProductUpdateRequestInfo.ChangeType.MOVE_MAPPINGS));
        Assertions.assertThat(issue.getTags())
            .contains(trackerServiceHelper.getTicketTag(TicketType.CHANGED_SKU_MAPPINGS));
    }

    @Test
    public void shouldHaveDifferentTableIfMappingNotChanged() {
        handler.handle(changedMappingTask(
            Collections.emptyList(), Collections.emptyList(),
            Collections.singletonList(offer(OLD_MODEL_ID))), null);

        IssueMock issue = (IssueMock) trackerServiceMock.getAllTickets().iterator().next();

        Assertions.assertThat(issue.getSummary())
            .startsWith(ChangedMappingTaskHandler.TICKET_TITLE_PREFIX + " " + OLD_MODEL_ID + " " + OLD_MODEL_NAME);
        Assertions.assertThat(issue.getDescription().get())
            .startsWith(ChangedMappingTaskHandler.NOT_MOVED_MAPPINGS_PREFIX);
    }

    @Test
    public void shouldAddCategoryManagersToFollowers() {
        handler.handle(changedMappingTask(
            Collections.emptyList(), Arrays.asList("catman1", "catman2"),
            Collections.singletonList(offer(NEW_MODEL_ID))), null);

        IssueMock issue = (IssueMock) trackerServiceMock.getAllTickets().iterator().next();

        Assertions.assertThat(issue.getSummary())
            .startsWith(ChangedMappingTaskHandler.TICKET_TITLE_PREFIX + " " + OLD_MODEL_ID + " " + OLD_MODEL_NAME);
        Assertions.assertThat(issue.getFollowers())
            .extracting(UserRef::getLogin)
            .contains("catman1", "catman2");
    }

    @Test
    public void shouldLinkSourceTickets() {
        Issue sourceTicket = trackerServiceMock
            .createTicket("title", "descrp", "me", Collections.emptyList(),
                TicketType.MATCHING, Collections.emptyMap(), Collections.emptyList(),
                Collections.emptyList(), builder -> {
                });
        handler.handle(changedMappingTask(
            Collections.singletonList(sourceTicket.getKey()), Collections.emptyList(),
            Collections.singletonList(offer(NEW_MODEL_ID))), null);

        String createdTicket = trackerServiceMock.getAllTickets().stream()
            .map(IssueRef::getKey)
            .filter(s -> !s.equals(sourceTicket.getKey()))
            .findFirst()
            .get();

        Assertions.assertThat(trackerServiceMock.isLinked(createdTicket, sourceTicket.getKey())).isTrue();
    }

    @Test
    public void shouldCommentOnOldTicketIfFound() {
        ReflectionTestUtils.setField(handler, "queue", TrackerServiceMock.TEST_QUEUE);
        Issue ticket = trackerServiceMock.createTicket(
            ChangedMappingTaskHandler.TICKET_TITLE_PREFIX + " " + OLD_MODEL_ID + " " + OLD_MODEL_NAME,
            "descrp", "me",
            Collections.emptyList(),
            TicketType.MATCHING, Collections.emptyMap(), Collections.emptyList(), Collections.emptyList(),
            builder -> {
            });
        trackerServiceMock.addTag(ticket, trackerServiceHelper.getTicketTag(TicketType.CHANGED_SKU_MAPPINGS));
        handler.handle(changedMappingTask(
            Collections.emptyList(), Collections.emptyList(),
            Collections.singletonList(offer(NEW_MODEL_ID))), null);

        Collection<? extends Issue> allTickets = trackerServiceMock.getAllTickets();
        Assertions.assertThat(allTickets).hasSize(1);
        List<String> rawComments = trackerServiceMock.getRawComments(allTickets.iterator().next());
        Assertions.assertThat(rawComments).hasSize(1);
//        Assertions.assertThat(rawComments.get(0))
//            .startsWith(ChangedMappingTaskHandler.MOVED_MAPPINGS_PREFIX)
//            .contains(offer(NEW_MODEL_ID).getApprovedSkuMapping().getName());
    }


    private ChangedMappingTask changedMappingTask(List<String> sourceTicket,
                                                  List<String> catManLogins,
                                                  List<Offer> offers) {
        ChangedMappingTask changedMappingTask = new ChangedMappingTask();
        changedMappingTask.setOldSkuId(OLD_MODEL_ID);
        changedMappingTask.setOldSkuName(OLD_MODEL_NAME);
        changedMappingTask.setSuppliers(ImmutableMap
            .of(OfferTestUtils.TEST_SUPPLIER_ID, new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "test")));
        changedMappingTask.setCatManStuffLogins(catManLogins);
        changedMappingTask.setSourceTickets(sourceTicket);
        changedMappingTask.setChangeType(MboMappings.ProductUpdateRequestInfo.ChangeType.MOVE_MAPPINGS);
        changedMappingTask.setOperatorStuffLogin("operator-login");
        changedMappingTask.createMappingsFromOffers(offers, Collections.emptyMap());
        return changedMappingTask;
    }

    private Offer offer(long modelId) {
        return OfferTestUtils.simpleOffer()
            .setTitle("this is a title")
            .updateApprovedSkuMapping(OfferTestUtils.mapping(modelId), Offer.MappingConfidence.CONTENT);
    }

}
