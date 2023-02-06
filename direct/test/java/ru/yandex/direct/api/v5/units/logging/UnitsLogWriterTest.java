package ru.yandex.direct.api.v5.units.logging;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import ru.yandex.direct.api.v5.context.units.OperatorBrandData;
import ru.yandex.direct.api.v5.context.units.OperatorClientData;
import ru.yandex.direct.api.v5.context.units.OperatorData;
import ru.yandex.direct.api.v5.context.units.SubclientBrandData;
import ru.yandex.direct.api.v5.context.units.SubclientClientData;
import ru.yandex.direct.api.v5.context.units.SubclientData;
import ru.yandex.direct.api.v5.context.units.UnitsBucket;
import ru.yandex.direct.api.v5.context.units.UnitsLogData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
public class UnitsLogWriterTest {

    private static final String RESOURCE = "units-log-data.json";

    private static final UnitsLogData UNITS_LOG_DATA = new UnitsLogData()
            .withBucket(new UnitsBucket())
            .withOperator(new OperatorData()
                    .withBrand(new OperatorBrandData())
                    .withClient(new OperatorClientData()))
            .withSubcilent(new SubclientData()
                    .withBrand(new SubclientBrandData())
                    .withClient(new SubclientClientData()));

    private final UnitsLogWriter writer = new UnitsLogWriter();

    @Test
    public void shouldConvertToJsonOfExpectedStructure() throws Exception {
        String expected = IOUtils.toString(this.getClass()
                .getResourceAsStream(RESOURCE), "UTF-8")
                .replaceAll("\\s+", "");    // inline json

        String actual = writer.toJson(UNITS_LOG_DATA);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldNotFailIfExceptionIsThrownWhilePrintingJson() {
        UnitsLogData mockThrowingException = mock(UnitsLogData.class);
        when(mockThrowingException.getBucket()).thenThrow(new NullPointerException());

        writer.write(mockThrowingException);
    }

}
