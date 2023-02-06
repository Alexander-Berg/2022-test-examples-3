package ru.yandex.direct.inventori;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.inventori.model.response.error.ErrorType;
import ru.yandex.direct.inventori.model.response.error.PredictionResponseError;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class PredictionResponseErrorTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Parameterized.Parameter(0)
    public String filename;

    @Parameterized.Parameter(1)
    public ErrorType errorType;

    @Parameterized.Parameters(name = "{1}")
    public static List<Object[]> parameters() {
        return Arrays.asList(
                new Object[]{"errors/internal_error.json", ErrorType.INTERNAL_ERROR},
                new Object[]{"errors/invalid_budget.json", ErrorType.INVALID_BUDGET},
                new Object[]{"errors/invalid_cpm.json", ErrorType.INVALID_CPM},
                new Object[]{"errors/invalid_dates.json", ErrorType.INVALID_DATES},
                new Object[]{"errors/invalid_request.json", ErrorType.INVALID_REQUEST},
                new Object[]{"errors/invalid_rf.json", ErrorType.INVALID_RF},
                new Object[]{"errors/unknown_segments.json", ErrorType.UNKNOWN_SEGMENTS},
                new Object[]{"errors/unsupported_segments.json", ErrorType.UNSUPPORTED_SEGMENTS},
                new Object[]{"errors/no_groups.json", ErrorType.NO_GROUPS},
                new Object[]{"errors/unsupported_error.json", ErrorType.UNSUPPORTED_ERROR}
        );
    }

    @Test
    public void testErrorDeserialize() throws Exception {
        InputStream in = ClassLoader.getSystemResourceAsStream(filename);
        PredictionResponseError response = mapper.readerFor(PredictionResponseError.class).readValue(in);
        assertEquals(response.getType(), errorType);
    }
}
