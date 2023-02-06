package ru.yandex.market.logistics.lrm.admin;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.repository.ydb.description.ReturnRouteHistoryTableDescription;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.ydb.integration.YdbTableDescription;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ParametersAreNonnullByDefault
public abstract class AbstractAdminIntegrationTest extends AbstractIntegrationYdbTest {

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    protected ReturnRouteHistoryTableDescription routeHistoryTableDescription;

    @AfterEach
    void noMoreLmsInteractions() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(routeHistoryTableDescription);
    }

    void verifyLmsGetLogisticsPoint(@Nullable Long pointId) {
        if (pointId == null) {
            return;
        }

        verify(lmsClient).getLogisticsPoint(pointId);
    }

    void verifyLmsGetLogisticsPoints(@Nullable Set<Long> pointIds) {
        if (pointIds == null) {
            return;
        }

        verify(lmsClient).getLogisticsPoints(LogisticsPointFilter.newBuilder().ids(pointIds).build());
    }

    void verifyLmsGetPartners(@Nullable Set<Long> partnerIds) {
        if (CollectionUtils.isEmpty(partnerIds)) {
            return;
        }

        verify(lmsClient).searchPartners(SearchPartnerFilter.builder().setIds(partnerIds).build());
    }
}
