package ru.yandex.market.common.report.parser.productoffers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skyscreamer.jsonassert.JSONAssert;
import ru.yandex.market.common.report.model.json.productoffers.ProductOffersReportResponse;
import ru.yandex.market.common.report.parser.ReportJsonTestingUtils;
import ru.yandex.market.common.report.parser.json.ProductOffersSearchParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import static ru.yandex.market.common.report.parser.ReportJsonTestingUtils.createObjectMapper;

/**
 * @author: belmatter
 */
@RunWith(Parameterized.class)
public class ProductOffersFullSerializationTest {


    @Parameterized.Parameter()
    public String path;

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> testData() {
        return Arrays.asList(
                new Object[]{"/files/productOffersBook.json"},
                new Object[]{"/files/productOffersCluster.json"},
                new Object[]{"/files/productOffersGroup.json"},
                new Object[]{"/files/productOffersSimple.json"}
        );
    }

    @Test
    public void test() throws IOException, JSONException {
        BufferedReader brExpected = new BufferedReader(
                new InputStreamReader(ProductOffersFullSerializationTest.class.getResourceAsStream(path))
        );
        StringBuilder sExpected = new StringBuilder();
        while (brExpected.ready()) {
            sExpected.append(brExpected.readLine());
        }

        JSONObject objectExpected = ReportJsonTestingUtils.makeReportResponseCorrectJson(sExpected.toString());

        //---------------------------------------
        ProductOffersSearchParser<ProductOffersReportResponse> parser = new ProductOffersSearchParser<>(source -> source);
        ProductOffersReportResponse result =
                parser.parse(ProductOffersFullSerializationTest.class.getResourceAsStream(path));

        ObjectMapper mapper = createObjectMapper();
        String json = mapper.writeValueAsString(result);

        JSONObject objectActual = new JSONObject(json);
        JSONAssert.assertEquals(objectExpected, objectActual, false);
    }


}
