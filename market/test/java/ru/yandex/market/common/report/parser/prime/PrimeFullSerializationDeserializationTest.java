package ru.yandex.market.common.report.parser.prime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skyscreamer.jsonassert.JSONAssert;
import ru.yandex.market.common.report.model.json.prime.PrimeSearchResult;
import ru.yandex.market.common.report.parser.ReportJsonTestingUtils;
import ru.yandex.market.common.report.parser.json.PrimeSearchResultParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import static ru.yandex.market.common.report.parser.ReportJsonTestingUtils.createObjectMapper;

/**
 * @author belmatter
 */
@RunWith(Parameterized.class)
public class PrimeFullSerializationDeserializationTest {

    @Parameterized.Parameter()
    public String path;

    @Parameterized.Parameters
    public static List<Object[]> testData() {
        return Arrays.asList(
                new Object[]{"/files/primeReportBook.json"},
                new Object[]{"/files/primeReportDress.json"},
                new Object[]{"/files/primeReportIphone.json"}
        );
    }

    @Test
    public void test() throws IOException, JSONException {
        BufferedReader brExpected = new BufferedReader(
                new InputStreamReader(PrimeReportParserTest.class.getResourceAsStream(path))
        );
        StringBuilder sExpected = new StringBuilder();
        while (brExpected.ready()) {
            sExpected.append(brExpected.readLine());
        }

        JSONObject objectExpected = ReportJsonTestingUtils.makeReportResponseCorrectJson(sExpected.toString());

        //---------------------------------------
        PrimeSearchResultParser<PrimeSearchResult> parser = new PrimeSearchResultParser<>(source -> source);
        PrimeSearchResult result = parser.parse(PrimeReportParserTest.class.getResourceAsStream(path));

        ObjectMapper mapper = createObjectMapper();
        String json = mapper.writeValueAsString(result);

        JSONObject objectActual = new JSONObject(json);
        JSONAssert.assertEquals(objectExpected, objectActual, false);
    }


}
