package ru.yandex.direct.jobs.adfox.messaging.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.DealDirect;
import ru.yandex.direct.core.entity.deal.model.StatusAdfoxSync;
import ru.yandex.direct.core.entity.deal.repository.DealRepository;
import ru.yandex.direct.core.entity.deal.service.DealService;
import ru.yandex.direct.core.testing.data.TestDeals;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jobs.configuration.JobsTestingConfiguration;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ContextHierarchy({
        @ContextConfiguration(classes = JobsTestingConfiguration.class),
        @ContextConfiguration(classes = AdfoxDealExportServiceTest.OverridingConfiguration.class)
})
@Disabled("Не работающая фича 'частные сделки'")
@ExtendWith(SpringExtension.class)
class AdfoxDealExportServiceTest {

    private static final long TEST_DEAL_ID_RANGE_OFFSET = 100L;
    private static final int DEAL_WITH_SAME_STATUS_COUNT = 3;

    @Autowired
    private Steps steps;

    @Autowired
    private DealService dealService;


    // Следующие bean'ы берутся из OverridingConfiguration

    @Autowired
    private AdfoxDealExportService serviceUnderTest;

    @Autowired
    private DealRepositoryDataProvider dealRepositoryDataProvider;

    private ClientInfo client;
    private Integer shard;
    private Map<Long, Deal> dealsById;

    @BeforeEach
    void setUp() {
        client = steps.clientSteps().createDefaultClient();
        shard = client.getShard();
        ClientId clientId = client.getClientId();
        Long startId = TestDeals.MAX_TEST_DEAL_ID + TEST_DEAL_ID_RANGE_OFFSET;
        List<StatusAdfoxSync> statuses = asList(StatusAdfoxSync.values());
        int statusesCount = statuses.size();
        int totalDealsCount = statusesCount * DEAL_WITH_SAME_STATUS_COUNT;
        List<Deal> deals = LongStream.range(startId, startId + totalDealsCount)
                .mapToObj(id -> {
                    Deal deal = TestDeals.defaultPrivateDeal(clientId, id);
                    StatusAdfoxSync statusAdfoxSync = statuses.get((int) (id % statusesCount));
                    deal.setStatusAdfoxSync(statusAdfoxSync);
                    return deal;
                })
                .collect(Collectors.toList());

        List<DealInfo> dealInfos = steps.dealSteps().addDeals(deals, client);
        dealsById = StreamEx.of(dealInfos)
                .map(DealInfo::getDeal)
                .mapToEntry(Deal::getId)
                .invert()
                .toMap();
        /*
         * Небольшой хак, чтобы {@link DealRepository#getDirectDealsByAdfoxSyncStatus(int, Collection, LimitOffset)}
         * возвращал только сделки, относящиеся к текущему тесту
         */
        dealRepositoryDataProvider.setExistingDeals(dealsById.values());
    }

    @AfterEach
    void tearDown() {
        steps.dealSteps().deleteDeals(dealsById.values(), client);
    }

    @Test
    void pickDealsToSync_success() {
        int limit = DEAL_WITH_SAME_STATUS_COUNT * 2 - 1; // count(No) + count(Sending) - 1
        List<DealDirect> pickedDeals = serviceUnderTest.pickDealsToSync(shard, limit);

        List<Long> dealIds = mapList(pickedDeals, DealDirect::getId);
        List<Deal> dealsAfterPicking = dealService.getDeals(client.getClientId(), dealIds);

        SoftAssertions.assertSoftly(softly -> {
            // проверяем, что получили ровно столько сделок, сколько хотели
            softly.assertThat(pickedDeals).hasSize(limit);

            // проверяем, что у всех сделок статус стал 'Sending'
            softly.assertThat(dealsAfterPicking)
                    .describedAs("Deals after pickDealsToSync(..) should be in 'Sending' status")
                    .allMatch(deal -> deal.getStatusAdfoxSync() == StatusAdfoxSync.SENDING);

            // проверяем, что все возвращённые сделки имели статус синхронизации отличный от 'Yes'
            softly.assertThat(pickedDeals)
                    .describedAs("Picked deals should be only in No/Sending statuses")
                    .allSatisfy(deal -> {
                        Deal prevDeal = dealsById.get(deal.getId());
                        assertThat(prevDeal)
                                .describedAs("Previous state of deal(id=%s) should not be in synced status",
                                        deal.getId())
                                .matches(pDeal -> pDeal.getStatusAdfoxSync() != StatusAdfoxSync.YES,
                                        "statusSync != YES");
                    });
        });
    }

    @Test
    void markDealsSynced_success() {
        // пытаемся пометить все сделки как синхронизованные
        serviceUnderTest.markDealsSynced(shard, dealsById.values());

        Collection<Long> dealIds = dealsById.keySet();
        List<Deal> dealsAfterSyncing = dealService.getDeals(client.getClientId(), dealIds);

        SoftAssertions.assertSoftly(softly -> {
            // проверяем, что не осталось сделок в статусе 'Sending'
            softly.assertThat(dealsAfterSyncing)
                    .describedAs("None of deals have status 'Sending'")
                    .allMatch(deal -> deal.getStatusAdfoxSync() != StatusAdfoxSync.SENDING);

            // проверяем, что у сделок со статусом отличным от 'Sending' статус не поменялся
            softly.assertThat(dealsAfterSyncing)
                    .describedAs("Only deals with 'Sending' status change their status")
                    .allSatisfy(deal -> {
                        Deal prevDeal = dealsById.get(deal.getId());
                        assertThat(prevDeal.getStatusAdfoxSync())
                                .describedAs("previous statusAdfoxSync")
                                .matches(status -> status == StatusAdfoxSync.SENDING ||
                                                status == deal.getStatusAdfoxSync(),
                                        "was 'Sending' or did not changed");
                    });

            // проверяем, что у сделок со статусом 'Sending' статус сменился на 'Yes'
            softly.assertThat(dealsAfterSyncing)
                    .describedAs("Deals with 'Sending' status should become synced")
                    .allMatch(deal -> {
                        Deal prevDeal = dealsById.get(deal.getId());
                        return prevDeal.getStatusAdfoxSync() != StatusAdfoxSync.SENDING ||
                                deal.getStatusAdfoxSync() == StatusAdfoxSync.YES;
                    });
        });
    }

    /**
     * Конфигурация подменяет {@link DealRepository} на {@link PartialDealRepositoryStub}.
     * И переопределяет bean'ы из пакета {@code ru.yandex.direct.jobs.adfox.messaging.export},
     * чтобы они ссылались на новый {@link DealRepository}.
     *
     * @see PartialDealRepositoryStub
     */
    @Configuration
    @ComponentScan(basePackages = "ru.yandex.direct.jobs.adfox.messaging.export")
    static class OverridingConfiguration {
        @Bean
        @Primary
        public DealRepository dealRepository(
                DslContextProvider dslContextProvider,
                DealRepositoryDataProvider dealRepositoryDataProvider) {
            return new PartialDealRepositoryStub(dslContextProvider, dealRepositoryDataProvider);
        }

        @Bean
        public DealRepositoryDataProvider dealRepositoryDataProvider() {
            return new DealRepositoryDataProvider();
        }
    }

    /**
     * Класс переопределяет метод {@link DealRepository#getDirectDealsByAdfoxSyncStatus},
     * чтобы обеспечить независимость данного теста от существующих в базе сделок.
     */
    @ParametersAreNonnullByDefault
    static class PartialDealRepositoryStub extends DealRepository {

        private final DealRepositoryDataProvider dealRepositoryDataProvider;

        PartialDealRepositoryStub(DslContextProvider dslContextProvider,
                                  DealRepositoryDataProvider dealRepositoryDataProvider) {
            super(dslContextProvider);
            this.dealRepositoryDataProvider = dealRepositoryDataProvider;
        }

        @Override
        public List<DealDirect> getDirectDealsByAdfoxSyncStatus(int shard, Collection<StatusAdfoxSync> statuses,
                                                                LimitOffset limitOffset) {
            return StreamEx.of(dealRepositoryDataProvider.getExistingDeals())
                    .filter(deal -> statuses.contains(deal.getStatusAdfoxSync()))
                    .skip(limitOffset.offset())
                    .limit(limitOffset.limit())
                    .toList();
        }
    }

    /**
     * Используется для хранения данных об используемых в данном тесте сделках.
     */
    static class DealRepositoryDataProvider {
        private Collection<DealDirect> existingDeals;

        Collection<DealDirect> getExistingDeals() {
            return existingDeals;
        }

        void setExistingDeals(Collection<? extends DealDirect> existingDeals) {
            this.existingDeals = new ArrayList<>(existingDeals);
        }
    }
}
