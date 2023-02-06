package ru.yandex.market.tsup.service.data_provider.primitive.external.lms.logistic_point;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.tpl.common.data_provider.meta.FrontHttpRequestMeta;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.data_provider.filter.impl.LogisticPointFilter;
import ru.yandex.market.tsup.service.data_provider.primitive.external.lms.logistic_point.dto.LogisticPointDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static ru.yandex.market.logistics.management.entity.type.PartnerType.FULFILLMENT;
import static ru.yandex.market.tsup.TestFactory.logisticPointDto;

@DbUnitConfiguration
public class LogisticPointAddressProviderTest extends AbstractContextualTest {
    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private LogisticPointProvider pointProvider;

    @Test
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/logisticPointProviderTest.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void provide() {
        Set<Long> ids = Set.of(1L, 2L);
        Mockito.when(lmsClient.searchPartners(any()))
                .thenReturn(List.of(
                        partnerResponse(1L, "Софьино 1", "SOFINO", FULFILLMENT),
                        partnerResponse(2L, "Софьино 2", "SOFINO", FULFILLMENT)
                ));
        Mockito.when(lmsClient.getLogisticsPoints(logisticsPointFilter(ids)))
            .thenReturn(List.of(
                logisticsPointResponse(1L, "первый", 1L),
                logisticsPointResponse(2L, "второй", 2L)
            ));

        Map<Long, LogisticPointDto> result = pointProvider.provide(
            LogisticPointFilter.byIds(ids),
            new FrontHttpRequestMeta()
        );

        assertThat(result, Is.is(Map.of(
                1L, logisticPointDto(1L, "первый", null, 1L),
                2L, logisticPointDto(2L, "второй", null, 2L)
        )));
    }

    private static LogisticsPointFilter logisticsPointFilter(Set<Long> ids) {
        return LogisticsPointFilter.newBuilder()
            .ids(ids)
            .build();
    }

    private static LogisticsPointResponse logisticsPointResponse(Long id, String name, Long partnerId) {
        return LogisticsPointResponse.newBuilder()
            .id(id)
            .phones(Set.of())
            .schedule(Set.of())
            .marketBranded(false)
            .name(name)
            .partnerId(partnerId)
            .build();
    }

    private static PartnerResponse partnerResponse(long id, String name, String readableName, PartnerType partnerType) {
        return PartnerResponse.newBuilder()
                .partnerType(partnerType)
                .readableName(readableName)
                .name(name)
                .id(id)
                .build();
    }
}
