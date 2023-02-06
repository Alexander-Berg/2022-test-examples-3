package ru.yandex.direct.api.v5.entity.adgroups.converter;

import com.yandex.direct.api.v5.adgroups.SourceProcessingStatusEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.entity.adgroups.converter.GetResponseConverter.convertSourceProcessingStatusToExternal;

@RunWith(Parameterized.class)
public class GetResponseConverterConvertSourceProcessingStatusToExternalTest {

    @Parameterized.Parameter
    public StatusBLGenerated sourceProcessingStatus;

    @Parameterized.Parameter(1)
    public SourceProcessingStatusEnum expectedStatus;

    @Parameterized.Parameters(name = "{0} => {1}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {StatusBLGenerated.PROCESSING, SourceProcessingStatusEnum.UNPROCESSED},
                {StatusBLGenerated.YES, SourceProcessingStatusEnum.PROCESSED},
                {StatusBLGenerated.NO, SourceProcessingStatusEnum.EMPTY_RESULT},
        };
    }

    @Test
    public void test() {
        assertThat(convertSourceProcessingStatusToExternal(sourceProcessingStatus)).isEqualTo(expectedStatus);
    }
}
