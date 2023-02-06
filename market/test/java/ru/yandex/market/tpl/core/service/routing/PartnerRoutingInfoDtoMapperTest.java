package ru.yandex.market.tpl.core.service.routing;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.routing.RoutingStatus;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.routing.partner.PartnerRoutingInfo;
import ru.yandex.market.tpl.core.external.routing.api.RoutingResultStatus;
import ru.yandex.market.tpl.core.test.ClockUtil;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
class PartnerRoutingInfoDtoMapperTest {

    @MockBean
    private Clock clock;

    @BeforeEach
    void initMocks() {
        ClockUtil.initFixed(clock, LocalDate.now().atTime(PartnerRoutingInfoDtoMapper.CUT_OF_TIME));
    }

    @Test
    void shouldReturnStatusInProgress() {
        var partnerRoutingInfoDtoMapper = new PartnerRoutingInfoDtoMapper();
        var routingInfo = new PartnerRoutingInfo();

        routingInfo.setInProgress(true);

        assertThat(partnerRoutingInfoDtoMapper.getRoutingStatus(routingInfo)).isEqualTo(RoutingStatus.IN_PROGRESS);
    }

    @Test
    void shouldReturnStatusPublished() {
        var partnerRoutingInfoDtoMapper = new PartnerRoutingInfoDtoMapper();

        var routingInfo = new PartnerRoutingInfo();
        routingInfo.setSuccess(true);
        routingInfo.setPublishStatus(RoutingResultStatus.SUCCESS);

        assertThat(partnerRoutingInfoDtoMapper.getRoutingStatus(routingInfo)).isEqualTo(RoutingStatus.PUBLISHED);
    }

    @Test
    void shouldReturnStatusCanBePublished() {
        var localDate = LocalDate.now();
        LocalDateTime now = localDate.atTime(PartnerRoutingInfoDtoMapper.CUT_OF_TIME).plusMinutes(1);
        ClockUtil.initFixed(clock, now);

        var partnerRoutingInfoDtoMapper = new PartnerRoutingInfoDtoMapper();
        var routingInfo = new PartnerRoutingInfo();

        routingInfo.setInProgress(false);
        routingInfo.setSuccess(true);
        routingInfo.setRoutingDate(localDate.plusDays(1));
        routingInfo.setStartedAt(DateTimeUtil.atDefaultZone(now));

        assertThat(partnerRoutingInfoDtoMapper.getRoutingStatus(routingInfo))
                .isEqualTo(RoutingStatus.CAN_BE_PUBLISHED);
    }

}
