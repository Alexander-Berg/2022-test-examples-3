package ru.yandex.market.mboc.common.processingticket;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.market.mbo.tracker.IssueMock;
import ru.yandex.market.mbo.tracker.utils.TicketType;
import ru.yandex.market.mboc.common.db.jooq.generated.mbo_category.tables.pojos.ProcessingTicketInfo;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.utils.YangPrioritiesUtil;
import ru.yandex.startrek.client.model.Issue;

/**
 * @author york
 * @since 28.07.2020
 */
public class ProcessingTicketInfoServiceTest {
    private static final LocalDate NOW = LocalDate.now();
    private static final Integer SUPPLIER_ID = 1;

    private static int offerIdSeq = 1;

    private ProcessingTicketInfoServiceForTesting processingTicketInfoServiceForTesting;

    @Before
    public void setUp() {
        processingTicketInfoServiceForTesting = new ProcessingTicketInfoServiceForTesting(
            new ProcessingTicketInfoRepositoryMock());
    }

    @Test
    public void testUpdateActiveCounts() {
        ProcessingTicketInfo info = new ProcessingTicketInfo()
            .setId(1)
            .setActiveOffers(ProcessingTicketInfoService.EMPTY_MAP)
            .setOffersByCategory(ProcessingTicketInfoService.EMPTY_MAP);
        processingTicketInfoServiceForTesting.update(info);
        Map<Long, Integer> counts = ImmutableMap.of(1L, 10, 2L, 11);
        processingTicketInfoServiceForTesting.updateActiveCounts(info, counts);
        info = processingTicketInfoServiceForTesting.getById(1);
        Assertions.assertThat(processingTicketInfoServiceForTesting.convertActiveCounts(info))
            .isEqualTo(counts);
        counts = ImmutableMap.of(2L, 11, 1L, 10);
        info.setDeadline(LocalDate.now());
        processingTicketInfoServiceForTesting.updateActiveCounts(info, counts);
        info = processingTicketInfoServiceForTesting.getById(1);
        Assertions.assertThat(info.getDeadline()).isNull(); //not updated changed order of counts
        counts = ImmutableMap.of(2L, 0, 1L, 10);
        processingTicketInfoServiceForTesting.updateActiveCounts(info, counts);
        info = processingTicketInfoServiceForTesting.getById(1);
        Assertions.assertThat(processingTicketInfoServiceForTesting.convertActiveCounts(info))
            .isEqualTo(ImmutableMap.of(1L, 10)); //zero counts are not stored

        processingTicketInfoServiceForTesting.updateActiveCounts(info, Collections.emptyMap());
        info = processingTicketInfoServiceForTesting.getById(1);
        Assertions.assertThat(info.getCompleted()).isNotNull(); //mark as completed
        Assertions.assertThat(processingTicketInfoServiceForTesting.convertActiveCounts(info))
            .isEqualTo(Collections.emptyMap());
    }

    @Test
    public void testUpdateActiveCountsByTicketIds() {
        ProcessingTicketInfo info = new ProcessingTicketInfo()
            .setId(1);
        processingTicketInfoServiceForTesting.updateActiveCounts(info,
            ImmutableMap.of(1L, 10, 2L, 5, 3L, 6));

        Map<Integer, Map<Long, Integer>> diff = new HashMap<>();
        diff.put(1, ImmutableMap.of(1L, 1, 3L, -6));
        diff.put(2, ImmutableMap.of(2L, 10));
        processingTicketInfoServiceForTesting.updateActiveCountsByTicketId(diff);

        info = processingTicketInfoServiceForTesting.getById(1);
        Assertions.assertThat(processingTicketInfoServiceForTesting.convertActiveCounts(info))
            .isEqualTo(ImmutableMap.of(1L, 11, 2L, 5));
    }

    @Test
    public void testDeadlineWithIssueNoDeadline() {
        List<Offer> offerList = Arrays.asList(offer(), offer());
        Issue issue = new IssueMock().setKey("MCP-1").setPriority("critical");
        LocalDate autoDeadLine = YangPrioritiesUtil.countDefaultDeadline(offerList.get(0).getProcessingStatus(),
            LocalDate.now());
        ProcessingTicketInfo ticketInfo = processingTicketInfoServiceForTesting.createNew(issue, TicketType.MATCHING,
            offerList);
        Assertions.assertThat(ticketInfo)
            .extracting(ProcessingTicketInfo::getDeadline, ProcessingTicketInfo::getComputedDeadline,
                ProcessingTicketInfo::getCritical)
            .containsExactly(null, autoDeadLine, true);
    }

    @Test
    public void testDeadlineWithIssueNoDeadlineOldTicket() {
        List<Offer> offerList = Arrays.asList(offer(), offer());
        Issue issue = new IssueMock().setKey("MCP-1").setPriority("critical");
        LocalDate autoDeadLine = YangPrioritiesUtil.countDefaultDeadline(offerList.get(0));
        ProcessingTicketInfo ticketInfo = processingTicketInfoServiceForTesting.createForActiveOffers(issue,
            offerList,
            offerList);
        Assertions.assertThat(ticketInfo)
            .extracting(ProcessingTicketInfo::getDeadline, ProcessingTicketInfo::getComputedDeadline,
                ProcessingTicketInfo::getCritical)
            .containsExactly(null, autoDeadLine, true);
    }

    @Test
    public void testDeadlineWithIssueOldTicket() {
        LocalDate deadline = LocalDate.parse("2020-02-03");
        List<Offer> offerList = Arrays.asList(offer().setTicketDeadline(deadline), offer());
        Issue issue = new IssueMock().setKey("MCP-1").setPriority("critical");
        LocalDate autoDeadLine = YangPrioritiesUtil.countDefaultDeadline(offerList.get(0));
        ProcessingTicketInfo ticketInfo = processingTicketInfoServiceForTesting.createForActiveOffers(issue,
            offerList,
            offerList);
        Assertions.assertThat(ticketInfo)
            .extracting(ProcessingTicketInfo::getDeadline, ProcessingTicketInfo::getComputedDeadline,
                ProcessingTicketInfo::getCritical)
            .containsExactly(deadline, deadline, true);
    }

    @Test
    public void testDeadlineWithIssueDeadline() {
        List<Offer> offerList = Arrays.asList(offer(), offer());
        String deadlineStr = "2020-02-03";
        LocalDate deadline = LocalDate.parse(deadlineStr);
        Issue issue = new IssueMock().setKey("MCP-1").setDeadline(
            Option.of(org.joda.time.LocalDate.parse(deadlineStr))
        );
        ProcessingTicketInfo ticketInfo = processingTicketInfoServiceForTesting.createForActiveOffers(issue,
            offerList,
            offerList);
        Assertions.assertThat(ticketInfo)
            .extracting(ProcessingTicketInfo::getDeadline, ProcessingTicketInfo::getComputedDeadline,
                ProcessingTicketInfo::getCritical)
            .containsExactly(deadline, deadline, false);
    }

    @Test
    public void testDeadlineWithoutIssueNoDeadline() {
        List<Offer> offerList = Arrays.asList(
            offer(),
            offer(),
            offer());
        ProcessingTicketInfo ticketInfo = processingTicketInfoServiceForTesting.createNewNoTicket(
            Offer.ProcessingStatus.IN_MODERATION,
            offerList);
        Assertions.assertThat(ticketInfo)
            .extracting(ProcessingTicketInfo::getDeadline, ProcessingTicketInfo::getComputedDeadline,
                ProcessingTicketInfo::getCritical)
            .containsExactly(null, YangPrioritiesUtil.countDefaultDeadline(offerList.get(0)), false);
    }

    @Test
    public void testDeadlineWithoutIssueWithDeadline() {
        List<Offer> offerList = Arrays.asList(
            offer(),
            offer().setTicketDeadline(LocalDate.parse("2020-02-03")),
            offer().setTicketDeadline(LocalDate.parse("2020-02-01")));

        ProcessingTicketInfo ticketInfo = processingTicketInfoServiceForTesting.createForActiveOffers(
            null,
            offerList,
            offerList);
        Assertions.assertThat(ticketInfo)
            .extracting(ProcessingTicketInfo::getDeadline, ProcessingTicketInfo::getComputedDeadline,
                ProcessingTicketInfo::getCritical)
            .containsExactly(LocalDate.parse("2020-02-01"), LocalDate.parse("2020-02-01"), false);
    }

    private Offer offer() {
        return new Offer()
            .setId(offerIdSeq++)
            .setTitle("offer")
            .setBusinessId(SUPPLIER_ID)
            .setProcessingStatusInternal(Offer.ProcessingStatus.IN_PROCESS)
            .setProcessingStatusModifiedInternal(NOW.atStartOfDay());
    }
}
