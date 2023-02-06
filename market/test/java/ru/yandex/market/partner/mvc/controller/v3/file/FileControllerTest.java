package ru.yandex.market.partner.mvc.controller.v3.file;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.annotation.Nonnull;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.mds.StoreInfo;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.mvc.controller.feed.model.MarketTemplate;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.mvc.controller.feed.model.MarketTemplate.OZON_ASSORTMENT;

/**
 * Date: 12.11.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
public class FileControllerTest extends FunctionalTest {

    private static final String XLS_STOCK = "Stock_xls-sku.xls";
    private static final String XLS_OZON = "feed-ozon.xlsx";

    @Autowired
    private FeedFileStorage feedFileStorage;
    @Autowired
    private EnvironmentService environmentService;

    @Test
    @DisplayName("Загрузка объединенного фида. Не передан файл")
    void uploadFeed_wrongUpload_error() {
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(buildUploadFeedUrl("1001", null))
        );

        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(),
                "FileController/json/uploadFeed/wrongUpload.json");
    }

    @Test
    @DisplayName("Загрузка объединенного фида. Неизвестный партнер")
    void uploadFeed_wrongCampaign_error() {
        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        buildUploadFeedUrl("1001", null),
                        createFeedUploadRequest(XLS_STOCK)
                )
        );

        JsonTestUtil.assertResponseErrorMessage(exception, this.getClass(),
                "FileController/json/uploadFeed/wrongCampaign.json");
    }

    @DbUnitDataSet(
            before = "FileController/csv/uploadFeed/feedUpload.before.csv",
            after = "FileController/csv/uploadFeed/shop.feedUpload.after.csv"
    )
    @Test
    @DisplayName("Загрузка объединенного фида. Белый")
    void uploadFeed_shopCorrectData_successful() throws URISyntaxException, IOException {
        assertUploadFeed("1007", XLS_STOCK, null, "shop");
    }

    @DbUnitDataSet(
            before = "FileController/csv/uploadFeed/feedUpload.before.csv",
            after = "FileController/csv/uploadFeed/supplier.feedUpload.after.csv"
    )
    @Test
    @DisplayName("Загрузка объединенного фида. Синий")
    void uploadFeed_supplierCorrectData_successful() throws URISyntaxException, IOException {
        assertUploadFeed("1001", XLS_STOCK, null, "supplier");
    }

    @DbUnitDataSet(
            before = "FileController/csv/uploadFeed/feedUpload.before.csv",
            after = "FileController/csv/uploadFeed/business.feedUpload.after.csv"
    )
    @Test
    @DisplayName("Загрузка объединенного фида. Бизнес>")
    void uploadFeed_businessCorrectData_successful() throws URISyntaxException, IOException {
        when(feedFileStorage.upload(any(), Mockito.anyLong()))
                .thenAnswer(invocation -> new StoreInfo(900, "https://mds3.ya.net"));

        HttpEntity<?> request = createFeedUploadRequest(XLS_STOCK);

        ResponseEntity<String> response = FunctionalTestHelper.post(buildUploadFeedUrlBusiness("100500", null),
                request);

        JsonTestUtil.assertEquals(response, this.getClass(), "FileController/json/uploadFeed/business.json");
    }

    @DbUnitDataSet(
            before = {"FileController/csv/uploadFeed/feedUpload.before.csv"},
            after = "FileController/csv/uploadFeed/supplier.ozon.feedUpload.withOrigin.csv.after.csv"
    )
    @Test
    @DisplayName("Загрузка ozon-фида в csv с сохранением оригинального")
    void uploadFeed_ozon_uploadOrigin_csv() throws URISyntaxException, IOException {
        assertUploadFeed("1001", XLS_OZON, OZON_ASSORTMENT, "supplier-ozon-with-origin-csv");
    }

    private void assertUploadFeed(String campaignId, String fileName, MarketTemplate marketTemplate, String resName)
            throws IOException, URISyntaxException {
        when(feedFileStorage.upload(any(), Mockito.anyLong()))
                .thenAnswer(invocation -> new StoreInfo(900, "https://mds3.ya.net"));

        HttpEntity<?> request = createFeedUploadRequest(fileName);

        ResponseEntity<String> response = FunctionalTestHelper.post(buildUploadFeedUrl(campaignId, marketTemplate),
                request);

        JsonTestUtil.assertEquals(response, this.getClass(), "FileController/json/uploadFeed/" + resName + ".json");
    }

    @Nonnull
    private HttpEntity<?> createFeedUploadRequest(String fileName) {
        ClassPathResource uploadedResource = new ClassPathResource("supplier/feed/" + fileName);

        MultiValueMap<String, Object> multipart = new LinkedMultiValueMap<>();
        multipart.add("upload", uploadedResource);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return new HttpEntity<>(multipart, headers);
    }


    @Nonnull
    private String buildUploadFeedUrl(@Nonnull String campaignId, MarketTemplate marketTemplate) throws URISyntaxException {
        var builder = new URIBuilder(baseUrl)
                .setPathSegments("v3", campaignId, "feed", "upload");

        if (marketTemplate != null) {
            builder.setParameter("market_template", marketTemplate.name());
        }

        return builder.build().toString();
    }

    @Nonnull
    private String buildUploadFeedUrlBusiness(@Nonnull String businessId, MarketTemplate marketTemplate) throws URISyntaxException {
        var builder = new URIBuilder(baseUrl)
                .setPathSegments("businesses", businessId, "feed", "upload");

        if (marketTemplate != null) {
            builder.setParameter("market_template", marketTemplate.name());
        }

        return builder.build().toString();
    }

}
