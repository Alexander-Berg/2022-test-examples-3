package ru.yandex.market.ff.controller.api;

import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageClientApi;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.search.SearchSkuFilter;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.CurrentStocksByVendorResponse;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.ResultPagination;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.SearchSkuResponse;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.ff.util.TestUtils.createSkuWithSingleFitStock;

/**
 * Функциональные тесты для {@link ReportController}.
 *
 * @author avetokhin 27/10/17.
 */
class ReportControllerTest extends MvcIntegrationTest {

    private static final long SHOP_ID = 10L;
    private static final String URL = "http://test.yandex.ru";
    private static final String SKU_1 = "sku1";
    private static final String SKU_2 = "sku2";
    private static final Long SERVICE_ID_1 = 100L;

    private static final SearchSkuFilter FILTER =
            SearchSkuFilter.of(SHOP_ID, SERVICE_ID_1.intValue(), Collections.emptyList(), true, null, null);

    @Autowired
    private StockStorageClientApi stockStorageClientApi;

    @Autowired
    private StockStorageSearchClient stockStorageSearchClient;

    @BeforeEach
    void init() throws MalformedURLException {
        when(stockStorageClientApi.getAllCurrentStocksByVendors(anyList()))
                .thenReturn(new CurrentStocksByVendorResponse(Collections.emptyList(), null));
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(URL));

        doReturn(SearchSkuResponse.of(asList(createSkuWithSingleFitStock(SKU_1, 10),
                createSkuWithSingleFitStock(SKU_2, -4)),
                ResultPagination.of(500, 0, 2, 2), FILTER))
                .when(stockStorageSearchClient)
                .searchSku(argThat(arg -> arg.getPagination().getOffset() == 0));

        doReturn(SearchSkuResponse.of(Collections.emptyList(),
                ResultPagination.of(500, 500, 0, 0), FILTER))
                .when(stockStorageSearchClient)
                .searchSku(argThat(arg -> arg.getPagination().getOffset() != 0));
    }

    @Test
    @DatabaseSetup("classpath:controller/report/requests.xml")
    void testNotFrozenStockReportGeneration() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get("/reports/not-frozen-stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("stockType", "0")
                        .param("supplierId", String.valueOf(SHOP_ID))
                        .param("serviceId", "100")
        )
                .andExpect(status().isOk())
                .andReturn();

        final Sheet sheet = getSheet(mvcResult);

        // Заголовок
        Row row = sheet.getRow(0);
        assertThat(row.getCell(0).getStringCellValue(), equalTo("Ваш SKU"));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("Название товара"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("Количество товаров"));

        // Первая строка
        row = sheet.getRow(1);
        assertThat(row.getCell(0).getStringCellValue(), equalTo(SKU_1));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("Магнитола"));
        assertThat(row.getCell(2).getNumericCellValue(), equalTo(10.0));

        // Вторая строка
        row = sheet.getRow(2);
        assertThat(row.getCell(0).getStringCellValue(), equalTo(SKU_2));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("-"));
        assertThat(row.getCell(2).getNumericCellValue(), equalTo(0.0));

        assertThat(sheet.getRow(3), nullValue());
    }

    @Test
    @DatabaseSetup("classpath:controller/report/commodity-circulation.xml")
    void commodityCirculation() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get("/reports/commodity-circulation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("article", "sku1")
                        .param("supplierId", String.valueOf(SHOP_ID))
                        .param("serviceId", "145")
        )
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mvcResult.getResponse().getHeader("Content-Disposition"),
                equalTo("attachment;filename=commodity-circulation.xlsx"));

        final Sheet sheet = getSheet(mvcResult);

        // Заголовок
        assertThat(sheet.getRow(2).getCell(2).getStringCellValue(), equalTo("supplier1"));
        assertThat(sheet.getRow(3).getCell(2).getStringCellValue(), equalTo("sku1"));
        assertThat(sheet.getRow(4).getCell(2).getStringCellValue(), equalTo("Marschroute FF"));

        // Первая строка
        Row row = sheet.getRow(6);
        assertThat(row.getCell(0).getNumericCellValue(), equalTo(1.0));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("id-1"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("01.12.2015 09:09:09"));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("20.11.2015 09:09:09"));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("Поставка"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Отменена"));
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(4.0));
        assertThat(row.getCell(7).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(8).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(9).getStringCellValue(), equalTo("22.11.2015 09:09:09"));

        // Вторая строка
        row = sheet.getRow(7);
        assertThat(row.getCell(0).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("id-2"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("01.03.2016 09:09:09"));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("01.01.2016 00:00:00"));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("Поставка"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Товары оприходованы"));
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(15.0));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(15.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(9).getStringCellValue(), equalTo("04.01.2016 00:15:00"));

        // Третья строка
        row = sheet.getRow(8);
        assertThat(row.getCell(0).getNumericCellValue(), equalTo(4.0));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("id-4"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("04.02.2016 00:15:00"));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("04.02.2016 00:15:00"));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("Пользовательский возврат"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Товары оприходованы"));
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(3.0));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(9).getStringCellValue(), equalTo("04.02.2016 00:15:00"));

        // Четвертая строка
        row = sheet.getRow(9);
        assertThat(row.getCell(0).getNumericCellValue(), equalTo(5.0));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("id-5"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("05.03.2016 00:15:00"));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("04.03.2016 00:15:00"));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("Пользовательский возврат"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Товары оприходованы"));
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(9).getStringCellValue(), equalTo("05.03.2016 00:15:00"));

        // Пятая строка
        row = sheet.getRow(10);
        assertThat(row.getCell(0).getNumericCellValue(), equalTo(6.0));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("id-6"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("04.05.2016 00:15:00"));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("04.05.2016 00:15:00"));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("Изъятие"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Товары переданы"));
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(7).getNumericCellValue(), equalTo(2.0));
        assertThat(row.getCell(8).getNumericCellValue(), equalTo(0.0));
        assertThat(row.getCell(9).getStringCellValue(), equalTo("04.05.2016 00:15:00"));

        // Шестая строка
        row = sheet.getRow(11);
        assertThat(row.getCell(0).getNumericCellValue(), equalTo(3.0));
        assertThat(row.getCell(1).getStringCellValue(), equalTo("id-3"));
        assertThat(row.getCell(2).getStringCellValue(), equalTo("01.09.2016 00:15:00"));
        assertThat(row.getCell(3).getStringCellValue(), equalTo("20.08.2016 00:00:00"));
        assertThat(row.getCell(4).getStringCellValue(), equalTo("Поставка"));
        assertThat(row.getCell(5).getStringCellValue(), equalTo("Утверждена"));
        assertThat(row.getCell(6).getNumericCellValue(), equalTo(5.0));
        assertThat(row.getCell(7).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(8).getStringCellValue(), equalTo(""));
        assertThat(row.getCell(9).getStringCellValue(), equalTo("25.08.2016 00:15:00"));

        assertThat(sheet.getRow(12), nullValue());
    }

    @DatabaseSetup("classpath:controller/report/periodical-report.xml")
    @Test
    void requestDailyReportTest() throws Exception {

        Mockito.doAnswer(invocation -> {
            ContentConsumer<OutputStream> response = invocation.getArgument(1);
            response.consume(IOUtils.toInputStream("myPDFContent", StandardCharsets.UTF_8));
            return null;
        }).when(mdsFfwfS3Client).download(any(), any());

        final MvcResult mvcResult = mockMvc.perform(
                get("/reports/request-daily-report/303")
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(content().bytes("myPDFContent".getBytes(StandardCharsets.UTF_8)))
                .andDo(print())
                .andReturn();

        verify(mdsFfwfS3Client, times(1))
                .download(argThat(a -> a.getKey().equals("Яндекс_Маркет_Софьино-1P-2022-04-17")), any());
        assertThat(mvcResult.getResponse().getHeader("Content-Disposition"),
                equalTo("attachment;filename=Яндекс_Маркет_Софьино-1P-2022-04-17.pdf"));
    }
}
