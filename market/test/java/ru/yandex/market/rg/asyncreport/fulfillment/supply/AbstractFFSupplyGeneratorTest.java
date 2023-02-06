package ru.yandex.market.rg.asyncreport.fulfillment.supply;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.googlecode.protobuf.format.JsonFormat;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feed.validation.result.XlsTestUtils;
import ru.yandex.market.core.offer.warehouse.MboDeliveryParamsClient;
import ru.yandex.market.mboc.http.MboMappingsForDelivery.SearchFulfilmentSskuParamsResponse;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

/**
 * Date: 28.12.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "FFSupplyGeneratorTest.uCat.before.csv")
class AbstractFFSupplyGeneratorTest extends FunctionalTest {

    @Autowired
    private MboDeliveryParamsClient mboDeliveryParamsClient;

    protected void mockMboDeliveryParamsClient(String filename) throws IOException {
        SearchFulfilmentSskuParamsResponse ffSkuParamsResponse =
                ffResponseFromJsonStream(
                        new ClassPathResource(
                                "data/" + filename,
                                FFSupplyGeneratorTest.class
                        ).getInputStream());

        when(mboDeliveryParamsClient.searchFulfilmentSskuParams(any(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(ffSkuParamsResponse.getFulfilmentInfoList());
    }

    @Nonnull
    private SearchFulfilmentSskuParamsResponse ffResponseFromJsonStream(InputStream jsonStream) {
        try {
            SearchFulfilmentSskuParamsResponse.Builder responseBuilder =
                    SearchFulfilmentSskuParamsResponse.newBuilder();

            JsonFormat.merge(new BufferedReader(new InputStreamReader(jsonStream)), responseBuilder);

            return responseBuilder.build();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected static void assertSheet(int expectedRowCount,
                                      ByteArrayOutputStream output,
                                      Map<XlsTestUtils.CellInfo, String> expected) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()))) {
            XlsTestUtils.assertSheet(expected, workbook, new XlsTestUtils.SheetInfo(expectedRowCount, 0, "Поставка"));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
