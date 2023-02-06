package ru.yandex.direct.core.entity.deal.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.DealDirect;
import ru.yandex.direct.core.entity.deal.model.StatusAdfoxSync;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestDeals;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DealRepositoryTest {

    private static final long TEST_DEAL_ID_RANGE_OFFSET = 200L;

    @Autowired
    private Steps steps;

    @Autowired
    private DealRepository repositoryUnderTest;

    private ClientInfo clientInfo;

    @Before
    public void setUp() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    /**
     * Метод {@link DealRepository#getDirectDealsByAdfoxSyncStatus(int, Collection, LimitOffset)}
     * в тестах сервисов переопределяется, чтобы не шуметь из-за мустора в тестовой БД.
     * Простреливаем этот метод тут, чтобы убедиться в корректности генерируемого SQL.
     */
    @Test
    public void getDirectDealsByAdfoxSyncStatus_success() {
        ClientId clientId = clientInfo.getClientId();
        long firstDealId = TestDeals.MAX_TEST_DEAL_ID + TEST_DEAL_ID_RANGE_OFFSET;
        int notSyncedDealCount = 5;

        List<Deal> notSyncedDeals = LongStream.range(firstDealId, firstDealId + notSyncedDealCount)
                .mapToObj(id -> TestDeals.defaultPrivateDeal(clientId, id))
                .peek(deal -> deal.setStatusAdfoxSync(StatusAdfoxSync.NO))
                .collect(Collectors.toList());

        steps.dealSteps()
                .addDeals(notSyncedDeals, clientInfo);

        List<DealDirect> actualDeals = repositoryUnderTest
                .getDirectDealsByAdfoxSyncStatus(clientInfo.getShard(), Collections.singletonList(StatusAdfoxSync.NO),
                        LimitOffset.limited(notSyncedDealCount));

        // Првоеряем только количество. Вернувшиеся сделки не обязательно будут теми, которые мы создали выше
        assertThat(actualDeals).hasSize(notSyncedDealCount);
    }
}
