package ru.yandex.market.delivery.transport_manager.facade.scrap;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ScrapLinehaulFacadeTest extends AbstractContextualTest {
    public static final long RETURN_SORTING_CENTER_PARTNER_ID = 555;
    public static final long SCRAP_TARGET_LOGISTICS_POINT_ID = -1;

    @Autowired
    private ScrapLinehaulFacade facade;
    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2021-11-27T21:00:00.00Z"), ZoneOffset.UTC);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @ExpectedDatabase(
        value = "/repository/transportation/after/scrap_linehaul.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void create() {
        mockProperty(TmPropertyKey.RETURN_SORTING_CENTER_PARTNER_ID, RETURN_SORTING_CENTER_PARTNER_ID);
        mockProperty(TmPropertyKey.SCRAP_TARGET_LOGISTICS_POINT_ID, SCRAP_TARGET_LOGISTICS_POINT_ID);

        when(lmsClient.getLogisticsPoints(eq(createLogisticsPointFilter(1L))))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder().id(11L).build()
            ));
        when(lmsClient.getLogisticsPoints(eq(createLogisticsPointFilter(RETURN_SORTING_CENTER_PARTNER_ID))))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder().id(555_555L).build()
            ));

        facade.create(1L, 2L, LocalDateTime.of(2021, 12, 1, 10, 0), 33, "0001,0002");

        verify(lmsClient, times(2)).getLogisticsPoints(Mockito.eq(
            createLogisticsPointFilter(1L)
        ));
        verify(lmsClient, times(2)).getLogisticsPoints(Mockito.eq(
            createLogisticsPointFilter(RETURN_SORTING_CENTER_PARTNER_ID)
        ));
    }

    private LogisticsPointFilter createLogisticsPointFilter(long partnerId) {
        return LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(partnerId))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
    }

}
