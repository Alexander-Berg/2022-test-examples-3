package ru.yandex.market.logistics.management.service.export.dynamic.source.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.DeliveryDistributorParamsProjectionDto;
import ru.yandex.market.logistics.management.domain.entity.type.StrictBoundsType;
import ru.yandex.market.logistics.management.util.CleanDatabase;

@CleanDatabase
@Sql("/data/repository/jdbc/delivery-params.sql")
class JdbcDeliveryDistributorParamsRepositoryTest extends AbstractContextualTest {
    @Autowired
    private JdbcDeliveryDistributorParamsRepository jdbcRepository;

    @Test
    public void testFindAll() {
        List<DeliveryDistributorParamsProjectionDto> found = jdbcRepository.findAll();
        softly.assertThat(found).isEqualTo(List.of(
            new DeliveryDistributorParamsProjectionDto()
                .setId(1L)
                .setDeliveryCost(BigDecimal.ONE.setScale(2, RoundingMode.UNNECESSARY))
                .setDeliveryDuration(10)
                .setFlagId(1)
                .setLocationId(2)
                .setMaxWeight(BigDecimal.TEN.setScale(2, RoundingMode.UNNECESSARY))
                .setMinWeight(BigDecimal.ZERO.setScale(2, RoundingMode.UNNECESSARY))
                .setStrictBoundsType(StrictBoundsType.FULL)
        ));
    }
}
