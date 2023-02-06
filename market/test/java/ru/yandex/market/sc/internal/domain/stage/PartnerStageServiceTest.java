package ru.yandex.market.sc.internal.domain.stage;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.domain.stage.Stages;
import ru.yandex.market.sc.internal.controller.dto.PartnerStageDto;
import ru.yandex.market.sc.internal.test.EmbeddedDbIntTest;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbIntTest
class PartnerStageServiceTest {
    @Autowired
    private PartnerStageService partnerStageService;


    @Test
    @SneakyThrows
    void getStagesTest() {
        var stageDtoWrapper = partnerStageService.getStages();

        assertThat(stageDtoWrapper.stages())
                .usingElementComparatorIgnoringFields("displayName")
                .contains(
                        fromStages(Stages.CANCELLED),
                        fromStages(Stages.AWAITING_DIRECT),
                        fromStages(Stages.KEEPED_DIRECT),
                        fromStages(Stages.AWAITING_SORT_DIRECT),
                        fromStages(Stages.SHIPPED_DIRECT),
                        fromStages(Stages.ACCEPTING_RETURN)
                );
    }

    private static PartnerStageDto fromStages(Stages stage) {
        return new PartnerStageDto(
                stage.getId(),
                stage.name(),
                null,
                stage.getDirection().getDescription(),
                stage.getType().getDescription()
        );
    }
}
