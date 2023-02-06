package ru.yandex.market.delivery.transport_manager.facade.transportation;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationSearchDto;
import ru.yandex.market.delivery.transport_manager.model.filter.TransportationSearchFilter;
import ru.yandex.market.logistics.lom.model.page.PageResult;

class TransportationSearchFacadeTest extends AbstractContextualTest {

    @Autowired
    private TransportationSearchFacade facade;

    @Test
    void createTransportationsIdsMap() {
        softly.assertThat(
            facade.createTransportationsIdsMap(List.of(
                new Transportation().setId(1L)
                    .setOutboundUnit(new TransportationUnit().setId(11L))
                    .setInboundUnit(new TransportationUnit().setId(12L))
                    .setMovement(new Movement().setId(10L)),
                new Transportation().setId(2L)
                    .setOutboundUnit(new TransportationUnit().setId(21L))
                    .setInboundUnit(new TransportationUnit().setId(22L))
                    .setMovement(new Movement().setId(20L))
            ))
        )
            .isEqualTo(Map.of(
                EntityType.TRANSPORTATION_UNIT, Set.of(11L, 12L, 21L, 22L),
                EntityType.MOVEMENT, Set.of(10L, 20L)
            ));
    }

    @Test
    @DatabaseSetup(
        value = {
            "/repository/facade/transportation_with_deps.xml",
            "/repository/facade/transportation_duplicated_id.xml",
        }
    )
    void correctlyChosenLastChangedAtIfEntityIdIsDuplicated() {
        PageResult<TransportationSearchDto> found = facade.search(TransportationSearchFilter.builder()
            .outboundPartnerIds(Set.of(5L))
            .build(), Pageable.unpaged());
        softly.assertThat(found.getData()
            .stream()
            .map(t -> t.getOutbound().getChangedAt())
            .filter(Objects::nonNull)
            .map(d -> d.atOffset(ZoneOffset.UTC).toLocalDate())
            .collect(Collectors.toList()))
            .contains(
                LocalDate.of(2021, 10, 1)
            );
    }
}
