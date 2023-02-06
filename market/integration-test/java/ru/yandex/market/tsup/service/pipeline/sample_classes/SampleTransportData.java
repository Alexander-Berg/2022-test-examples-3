package ru.yandex.market.tsup.service.pipeline.sample_classes;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.market.tsup.core.pipeline.CubeData;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SampleTransportData implements CubeData {
    private Long transportId;
}
