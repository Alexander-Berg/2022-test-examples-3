package ru.yandex.market.ff.controller.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.configuration.DateTimeTestConfig;
import ru.yandex.market.ff.enums.FileExtension;
import ru.yandex.market.ff.service.EnvironmentParamService;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;
import ru.yandex.market.logistic.api.utils.TimeZoneUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Функциональный тест на размер файла для {@link RequestUploadController}.
 */
@SuppressWarnings("AnnotationUseStyle")
@DatabaseSetup("classpath:controller/upload-request/before.xml")
class RequestUploadControllerFileSizeTest extends AbstractRequestUploadControllerWithFileTest {

    private static final long SUPPLIER_ID = 1;
    private static final long SERVICE_ID = 555;
    private static final Instant SUPPLY_DATE = DateTimeTestConfig.FIXED_SUPPLY_INSTANT
            .plus(1, ChronoUnit.DAYS);
    private static final Instant CROSSDOCK_SUPPLY_DATE =
            DateTimeTestConfig.FIXED_NOW.toInstant(TimeZoneUtil.DEFAULT_OFFSET).plus(1, ChronoUnit.DAYS);

    private static final String COMMENT = "some comment";

    private static final int SUPPLY_INBOUND_TYPE = 0;

    @Autowired
    private EnvironmentParamService environmentParamService;

    @BeforeEach
    void init() throws MalformedURLException {
        doNothing().when(mdsS3Client).upload(any(), any());
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(FILE_URL));
        environmentParamService.clearCache();
    }

    @Test
    @JpaQueriesCount(3)
    @DatabaseSetup("classpath:controller/upload-request/small-max-grid-file-size.xml")
    void uploadSupplyXlsFileSizeExceeded() throws Exception {

        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xls", FileExtension.XLS.getMimeType());
        uploadSupplyWithDate(SUPPLIER_ID, file, null)
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getJsonFromFile("max-file-size-for-type-xls-exceeded.json")));
        verifyZeroInteractions(mdsS3Client);
    }

    @Test
    @JpaQueriesCount(3)
    @DatabaseSetup("classpath:controller/upload-request/small-max-grid-file-size.xml")
    void uploadSupplyXlsV2FileSizeExceeded() throws Exception {

        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xls", FileExtension.XLS.getMimeType(), 2);
        uploadSupplyWithDate(SUPPLIER_ID, file, null)
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getJsonFromFile("max-file-size-for-type-xls-exceeded-v2.json")));
        verifyZeroInteractions(mdsS3Client);
    }

    @Test
    @JpaQueriesCount(3)
    @DatabaseSetup("classpath:controller/upload-request/small-max-grid-file-size.xml")
    void uploadSupplyXlsxFileSizeExceeded() throws Exception {

        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType());
        uploadSupplyWithDate(SUPPLIER_ID, file, null)
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getJsonFromFile("max-file-size-for-type-xlsx-exceeded.json")));
        verifyZeroInteractions(mdsS3Client);
    }


    @Test
    @JpaQueriesCount(3)
    @DatabaseSetup("classpath:controller/upload-request/small-max-grid-file-size.xml")
    void uploadSupplyXlsxV2FileSizeExceeded() throws Exception {

        MockMultipartFile file = getValidFile(RequestType.SUPPLY, "xlsx", FileExtension.XLSX.getMimeType(), 2);
        uploadSupplyWithDate(SUPPLIER_ID, file, null)
                .andExpect(status().isBadRequest())
                .andExpect(content().json(getJsonFromFile("max-file-size-for-type-xlsx-exceeded-v2.json")));
        verifyZeroInteractions(mdsS3Client);
    }

    private ResultActions uploadSupplyWithDate(final long shopId, final MockMultipartFile file,
                                               Integer inboundType) throws Exception {

        final ImmutableMap<String, String> params = ImmutableMap.<String, String>builder()
                .put("date", getDateBasedOnInboundType(inboundType))
                .put("comment", COMMENT)
                .build();
        return upload(shopId, file, "supply", inboundType, SERVICE_ID, params);
    }

    private String getDateBasedOnInboundType(Integer inboundType) {
        return Optional.ofNullable(inboundType).orElse(SUPPLY_INBOUND_TYPE) == SUPPLY_INBOUND_TYPE
                ? SUPPLY_DATE.toString()
                : CROSSDOCK_SUPPLY_DATE.toString();
    }

}
