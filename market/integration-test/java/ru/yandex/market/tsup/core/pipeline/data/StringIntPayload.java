package ru.yandex.market.tsup.core.pipeline.data;


import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import ru.yandex.market.tsup.core.pipeline.CubeData;

@Data
@Accessors(chain = true)
@NoArgsConstructor
public class StringIntPayload implements CubeData {
    @NotNull
    String a;
    int b;
}
