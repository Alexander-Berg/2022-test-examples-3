package ru.yandex.market.tsup.service.pipeline.sample_classes.enum_data;

import javax.annotation.Nullable;

import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.market.delivery.transport_manager.model.enums.TransportationType;
import ru.yandex.market.tsup.core.pipeline.CubeData;
import ru.yandex.market.tsup.core.pipeline.data.schedule.RouteScheduleSubtype;

@Data
@NoArgsConstructor
public class SampleEnumPayloadData implements CubeData {
    private TransportationType transportationType;
    @Nullable
    private RouteScheduleSubtype nullableField;
}
