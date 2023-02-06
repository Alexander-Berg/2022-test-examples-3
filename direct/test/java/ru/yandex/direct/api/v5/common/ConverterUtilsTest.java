package ru.yandex.direct.api.v5.common;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;

@ParametersAreNonnullByDefault
public class ConverterUtilsTest {

    @Test
    public void convertArrayIndicesTest() {
        Path original = new Path(asList(field("A"), index(0), field("B"), index(3), field("C")));
        assertThat(ConverterUtils.convertArrayIndices(original).toString()).isEqualTo("A[1].B[4].C");
    }

}
