package ru.yandex.market.pricelabs.integration.api;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.pricelabs.MockMvcProxy;
import ru.yandex.market.pricelabs.api.api.PublicLogsApi;
import ru.yandex.market.pricelabs.api.api.PublicLogsApiInterfaces;
import ru.yandex.market.pricelabs.api.spring.csv.OpenCsvHttpMessageConverter;
import ru.yandex.market.pricelabs.api.spring.excel.ExcelHttpMessageConverter;
import ru.yandex.market.pricelabs.generated.server.pub.model.ShopLogResponse;
import ru.yandex.market.pricelabs.integration.AbstractIntegrationSpringConfiguration;
import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.types.FilterOfferType;
import ru.yandex.market.pricelabs.model.types.FilterPriceType;
import ru.yandex.market.pricelabs.tms.ConfigurationForTests;
import ru.yandex.market.pricelabs.tms.processing.ProcessingRouter;
import ru.yandex.market.pricelabs.tms.processing.TasksController;
import ru.yandex.market.pricelabs.tms.services.database.TasksService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTestInitializer.SHOP_ID;

public class AbstractApiTests extends AbstractIntegrationSpringConfiguration {

    @Autowired
    protected PublicApiTestInitializer initializer;

    @Autowired
    protected TasksController controller;

    @Autowired
    @Qualifier("mockWebServerPartnerApi")
    protected ConfigurationForTests.MockWebServerControls mockWebServerPartnerApi;

    @Autowired
    protected TasksService tasksService;

    @Autowired
    protected ProcessingRouter router;

    @Autowired
    private PublicLogsApi publicLogsApiBean;
    private PublicLogsApiInterfaces publicLogsApi;

    public static <T> T checkResponse(ResponseEntity<T> response) {
        var body = response.getBody();
        assertNotNull(body);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return body;
    }

    static Filter getFilterForModels() {
        var filter = new Filter();
        filter.setOffer_type(FilterOfferType.ALL);
        filter.getRelativePrices().setPrice_type_from(FilterPriceType.TOP5_AVG);
        filter.getRelativePrices().setPrice_delta_from(49900011);
        return filter;
    }

    @BeforeEach
    void init() {
        publicLogsApi = buildProxy(PublicLogsApiInterfaces.class, publicLogsApiBean);

        TimingUtils.setTime(Utils.parseDateTimeAsInstant("2019-12-01T01:02:03"));
        testControls.initOnce(this.getClass(), () -> initializer.init());
        testControls.executeInParallel(
                () -> mockWebServerPartnerApi.cleanup(),
                () -> {
                    testControls.cleanupTasksService();
                    testControls.initShopLoopJob();
                },
                () -> testControls.resetExportService()
        );
    }

    protected <T> T buildProxy(Class<T> classInterface, T implementation) {
        return MockMvcProxy.buildProxy(classInterface, implementation,
                () -> List.of(new ExcelHttpMessageConverter(), new OpenCsvHttpMessageConverter()));
    }

    protected List<ShopLogResponse> getShopLogs() {
        return getShopLogs(null, null);
    }

    protected List<ShopLogResponse> getShopLogs(Integer pageSize, Integer offset) {
        return checkResponse(publicLogsApi.shopLogsGet(SHOP_ID,
                List.of(Utils.formatToDate(TimingUtils.getInstant())), pageSize, offset));
    }

    protected List<ShopLogResponse> getJobLogs(long jobId) {
        return getJobLogs(jobId, null, null);
    }

    protected List<ShopLogResponse> getJobLogs(long jobId, Integer pageSize, Integer offset) {
        return checkResponse(publicLogsApi.jobLogsGet(SHOP_ID, jobId, pageSize, offset));
    }

    protected String tempFile(ConfigurationForTests.Exports.ExportItem export) {
        return StringUtils.removeStart(export.getFilePath(), "export/tmp/");
    }

}
