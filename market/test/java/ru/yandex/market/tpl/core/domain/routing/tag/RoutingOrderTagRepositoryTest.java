package ru.yandex.market.tpl.core.domain.routing.tag;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.routing.tag.RoutingOrderTagType;
import ru.yandex.market.tpl.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class RoutingOrderTagRepositoryTest {

    private final RoutingOrderTagRepository routingOrderTagRepository;

    @Test
    void findByName() {
        RoutingOrderTag tag = new RoutingOrderTag("pvz", "ПВЗ", BigDecimal.TEN, BigDecimal.ONE, RoutingOrderTagType.ORDER_TYPE, Set.of());
        routingOrderTagRepository.save(tag);

        Optional<RoutingOrderTag> foundTagO = routingOrderTagRepository.findByName("pvz");
        assertThat(foundTagO).isNotEmpty();
        RoutingOrderTag foundTag = foundTagO.get();
        assertThat(foundTag.getDescription()).isEqualTo("ПВЗ");
        assertThat(foundTag.getFixedCost()).isEqualTo(BigDecimal.TEN);
        assertThat(foundTag.getCostPerOrder()).isEqualTo(BigDecimal.ONE);
        assertThat(foundTag.getType()).isEqualTo(RoutingOrderTagType.ORDER_TYPE);
    }
}
