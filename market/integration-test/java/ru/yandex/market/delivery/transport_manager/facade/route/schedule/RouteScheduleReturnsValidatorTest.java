package ru.yandex.market.delivery.transport_manager.facade.route.schedule;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.facade.route.schedule.validator.RouteScheduleReturnsValidator;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleDto;
import ru.yandex.market.delivery.transport_manager.model.dto.route.schedule.RouteScheduleTypeDto;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

@DatabaseSetup("/repository/route/full_routes.xml")
public class RouteScheduleReturnsValidatorTest extends AbstractContextualTest {
    @Autowired
    private RouteScheduleReturnsValidator validator;

    @Autowired
    private LMSClient lmsClient;

    @Test
    void testValid() {
        Mockito.when(lmsClient.searchPartners(SearchPartnerFilter.builder().setIds(Set.of(1L, 2L, 3L)).build()))
            .thenReturn(List.of(
                PartnerResponse.newBuilder().id(1L).partnerType(PartnerType.SORTING_CENTER).build(),
                PartnerResponse.newBuilder().id(2L).partnerType(PartnerType.SORTING_CENTER).build(),
                PartnerResponse.newBuilder().id(3L).partnerType(PartnerType.FULFILLMENT).build()
            ));
        validator.validate(new RouteScheduleDto().setRouteId(30L).setType(RouteScheduleTypeDto.ORDERS_RETURN));
    }

    @Test
    void testInvalid() {
        Mockito.when(lmsClient.searchPartners(SearchPartnerFilter.builder().setIds(Set.of(1L, 2L, 3L)).build()))
            .thenReturn(List.of(
                PartnerResponse.newBuilder().id(1L).partnerType(PartnerType.SORTING_CENTER).build(),
                PartnerResponse.newBuilder().id(2L).partnerType(PartnerType.DROPSHIP).build(),
                PartnerResponse.newBuilder().id(3L).partnerType(PartnerType.FULFILLMENT).build()
            ));
        RouteScheduleDto dto = new RouteScheduleDto().setRouteId(30L).setType(RouteScheduleTypeDto.ORDERS_RETURN);

        softly.assertThatThrownBy(() -> validator.validate(dto))
            .hasMessage("OUTBOUND point partner types must match [SORTING_CENTER]");

        Mockito.when(lmsClient.searchPartners(SearchPartnerFilter.builder().setIds(Set.of(1L, 2L, 3L)).build()))
            .thenReturn(List.of(
                PartnerResponse.newBuilder().id(1L).partnerType(PartnerType.SORTING_CENTER).build(),
                PartnerResponse.newBuilder().id(2L).partnerType(PartnerType.SORTING_CENTER).build(),
                PartnerResponse.newBuilder().id(3L).partnerType(PartnerType.DISTRIBUTION_CENTER).build()
            ));

        softly.assertThatThrownBy(() -> validator.validate(dto))
            .hasMessage("INBOUND point partner types must match [FULFILLMENT, SORTING_CENTER]");

    }

}
