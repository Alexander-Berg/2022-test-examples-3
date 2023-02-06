package ru.yandex.market.tsup.service.tsup_property;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.tsup.domain.entity.tsup_properties.TsupProperty;
import ru.yandex.market.tsup.domain.entity.tsup_properties.TsupPropertyKey;
import ru.yandex.market.tsup.repository.mappers.TsupPropertyMapper;
import ru.yandex.market.tsup.service.data_provider.entity.route.schedule.dto.RouteScheduleTypeDto;

class TsupPropertyServiceTest {

    @Test
    void disabledScheduleTypesEmpty() {
        TsupPropertyMapper mapper = Mockito.mock(TsupPropertyMapper.class);
        TsupPropertyService tsupPropertyService = new TsupPropertyService(mapper);

        Mockito.when(mapper.findByKey(TsupPropertyKey.DISABLED_SCHEDULE_TYPES)).thenReturn(null);

        Assertions.assertThat(tsupPropertyService.disabledScheduleTypes()).isEmpty();
    }

    @Test
    void disabledScheduleTypes() {
        TsupPropertyMapper mapper = Mockito.mock(TsupPropertyMapper.class);
        TsupPropertyService tsupPropertyService = new TsupPropertyService(mapper);

        Mockito.when(mapper.findByKey(TsupPropertyKey.DISABLED_SCHEDULE_TYPES))
            .thenReturn(new TsupProperty(1L, TsupPropertyKey.DISABLED_SCHEDULE_TYPES, "AAA,, , COMMON"));

        Assertions.assertThat(tsupPropertyService.disabledScheduleTypes())
            .containsExactlyInAnyOrder(RouteScheduleTypeDto.COMMON);
    }
}
