package ru.yandex.market.tsup.service.pipeline.sample_classes;

import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.market.tsup.core.pipeline.CubeData;

@Data
@NoArgsConstructor
public class SamplePayloadData implements CubeData {
    private String data;
    private SampleTransportData transportData;
}
