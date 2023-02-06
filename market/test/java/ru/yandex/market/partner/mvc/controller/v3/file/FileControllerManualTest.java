package ru.yandex.market.partner.mvc.controller.v3.file;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.common.framework.core.RemoteFile;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.feed.mds.FeedFileStorage;
import ru.yandex.market.core.feed.mds.StoreInfo;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.partner.mvc.controller.feed.model.MarketTemplate;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static ru.yandex.market.partner.mvc.controller.feed.model.MarketTemplate.OZON_ASSORTMENT;
import static ru.yandex.market.partner.mvc.controller.feed.model.MarketTemplate.SBER_MM_ASSORTMENT;
import static ru.yandex.market.partner.mvc.controller.feed.model.MarketTemplate.WLB_ASSORTMENT;

/**
 * Date: 12.11.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@Disabled
@DbUnitDataSet(before = "FileController/csv/uploadFeed/feedUpload.before.csv")
public class FileControllerManualTest extends FunctionalTest {

    @Autowired
    private FeedFileStorage feedFileStorage;
    @Autowired
    private EnvironmentService environmentService;

    @Test
    void manual_uploadFeed_Ozon() throws URISyntaxException, IOException {
        uploadFeed("feed-ozon.xlsx", OZON_ASSORTMENT);
    }

    @Test
    void manual_uploadFeed_Ozon_new() throws URISyntaxException, IOException {
        uploadFeed("feed-ozon-new.xlsx", OZON_ASSORTMENT);
    }

    @Test
    void manual_uploadFeed_Ozon_new_csv() throws URISyntaxException, IOException {
        environmentService.setValues("united.catalog.upload.feed.convert2csv.templates",
                List.of("OZON_ASSORTMENT", "OZON_ASSORTMENT_2"));
        uploadFeed("feed-ozon-new.xlsx", OZON_ASSORTMENT);
    }

    @Test
    void manual_uploadFeed_Sber() throws URISyntaxException, IOException {
        uploadFeed("feed-sber-mm.xlsx", SBER_MM_ASSORTMENT);
    }

    @Test
    void manual_uploadFeed_Wlb() throws URISyntaxException, IOException {
        uploadFeed("feed-wlb.xlsx", WLB_ASSORTMENT);
    }

    private void uploadFeed(String fileName, MarketTemplate marketTemplate) throws URISyntaxException, IOException {
        mockFileStorage();
        var request = createFeedUploadRequest(fileName);
        FunctionalTestHelper.post(buildUploadFeedUrl("1001", marketTemplate), request);
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

    private void mockFileStorage() throws IOException {
        Mockito.when(feedFileStorage.upload(any(), anyLong()))
                .thenAnswer(invocation -> {
                    RemoteFile remoteFile = invocation.getArgument(0);

                    File dir = new File(SystemUtils.getUserHome(), "reports");
                    FileUtils.forceMkdir(dir);
                    File target = new File(dir, remoteFile.getOriginalFilename());

                    FileUtils.copyInputStreamToFile(remoteFile.getInputStream(), target);
                    return new StoreInfo(900, "https://mds3.ya.net");
                });
    }
}
