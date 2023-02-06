package ru.yandex.market.tsup.service.pipeline.sample_classes;

import lombok.AllArgsConstructor;
import lombok.Data;

import ru.yandex.market.tsup.core.pipeline.CubeData;

@Data
@AllArgsConstructor
public class OutCubeData implements CubeData {
    private Long id;
}
