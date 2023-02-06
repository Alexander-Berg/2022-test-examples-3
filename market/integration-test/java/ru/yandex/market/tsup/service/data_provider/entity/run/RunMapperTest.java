package ru.yandex.market.tsup.service.data_provider.entity.run;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.mj.generated.client.carrier.model.RunStatusDto;

public class RunMapperTest extends AbstractContextualTest {

    @Autowired
    private RunMapper runMapper;

    @ParameterizedTest
    @EnumSource(RunStatusDto.class)
    void runStatusShouldBeMappedToMovementStatus(RunStatusDto statusDto) {
        Assertions.assertThat(runMapper.mapMovementState(statusDto)).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(RunStatusDto.class)
    void runStatusShouldBeMappedToTransportationStatus(RunStatusDto statusDto) {
        Assertions.assertThat(runMapper.mapTranportationStatus(statusDto)).isNotNull();
    }
}
