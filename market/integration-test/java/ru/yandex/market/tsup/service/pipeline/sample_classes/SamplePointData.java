package ru.yandex.market.tsup.service.pipeline.sample_classes;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.market.tsup.core.pipeline.CubeData;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SamplePointData implements CubeData {
    @NotNull
    private Long id;
}
