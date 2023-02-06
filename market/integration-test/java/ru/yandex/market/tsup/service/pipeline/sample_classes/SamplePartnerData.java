package ru.yandex.market.tsup.service.pipeline.sample_classes;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import ru.yandex.market.tsup.core.pipeline.CubeData;

@Data
@Accessors(chain = true)
@NoArgsConstructor
public class SamplePartnerData implements CubeData {
    private Long id;
}
