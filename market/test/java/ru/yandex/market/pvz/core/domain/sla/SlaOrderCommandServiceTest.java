package ru.yandex.market.pvz.core.domain.sla;

import java.util.Collections;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.sla.mapper.SlaModelMapper;
import ru.yandex.market.pvz.core.domain.sla.yt.SlaOrderYtModel;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SlaOrderCommandServiceTest {

    private final SlaOrderCommandService slaOrderCommandService;
    private final SlaOrderRepository slaOrderRepository;
    private final SlaModelMapper mapper;

    @Test
    void saveAndUpdateSlaOrderRows() {
        var model = createYtModel(1000L, false);
        slaOrderCommandService.saveOrUpdateSlaOrderRows(Collections.singletonList(model));

        var slaOrders = slaOrderRepository.findAll();
        assertThat(slaOrders.size()).isEqualTo(1);
        assertThat(slaOrders.get(0)).isEqualToIgnoringGivenFields(mapper.map(model), "id", "createdAt", "updatedAt");

        model = createYtModel(1000L, true);
        slaOrderCommandService.saveOrUpdateSlaOrderRows(Collections.singletonList(model));

        slaOrders = slaOrderRepository.findAll();
        assertThat(slaOrders.size()).isEqualTo(1);
        assertThat(slaOrders.get(0)).isEqualToIgnoringGivenFields(mapper.map(model), "id", "createdAt", "updatedAt");

    }

    @Test
    void saveSlaOrderRowsWithNoExternalId() {
        var model = createYtModel(null, false);
        slaOrderCommandService.saveOrUpdateSlaOrderRows(Collections.singletonList(model));

        var slaOrders = slaOrderRepository.findAll();
        assertThat(slaOrders.size()).isEqualTo(0);
    }

    private SlaOrderYtModel createYtModel(Long externalId, Boolean acceptDelayed) {
        return SlaOrderYtModel.builder()
                .externalId(externalId)
                .acceptDelayed(acceptDelayed)
                .build();
    }

}
