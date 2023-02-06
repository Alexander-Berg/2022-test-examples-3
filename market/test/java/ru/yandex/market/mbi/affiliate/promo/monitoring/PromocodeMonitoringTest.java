package ru.yandex.market.mbi.affiliate.promo.monitoring;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Objects;

import Market.DataCamp.SyncAPI.SyncGetPromo;
import com.google.protobuf.util.JsonFormat;
import okhttp3.ResponseBody;
import org.dbunit.database.DatabaseConfig;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import retrofit2.Response;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.application.monitoring.MonitoringUnit;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.mbi.affiliate.promo.config.FunctionalTestConfig;
import ru.yandex.market.mbi.affiliate.promo.dao.PromoDao;
import ru.yandex.market.mbi.affiliate.promo.dao.VarsDao;
import ru.yandex.market.mbi.affiliate.promo.stroller.DataCampStrollerClient;
import ru.yandex.market.mbi.affiliate.promo.stroller.StrollerProtoApi;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
@TestExecutionListeners(listeners = {
        DbUnitTestExecutionListener.class,
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
public class PromocodeMonitoringTest {
    @Autowired
    private PromoDao promoDao;

    @Autowired
    private DataCampStrollerClient strollerClient;
    @Autowired
    private StrollerProtoApi strollerProtoApi;

    @Autowired
    private VarsDao varsDao;

    @Autowired
    private Clock clock;

    private final ComplexMonitoring monitoring = mock(ComplexMonitoring.class);
    private MonitoringUnit unit;
    private PromocodeMonitoring promocodeMonitoring;

    @Before
    public void setUp() {
        Mockito.reset(strollerProtoApi);
        unit = new MonitoringUnit("tst-mon");
        when(monitoring.createUnit(any())).thenReturn(unit);
        promocodeMonitoring = new PromocodeMonitoring(
                monitoring, promoDao, strollerClient, varsDao, clock, 0.1, 0.2, 0.3, 600, 60);

    }

    @DbUnitDataSet(dataSource = "promoDataSource", before = "before_parent_diff.csv")
    @Test
    public void testParentDiff() throws Exception {
        testParentError("ru/yandex/market/mbi/affiliate/promo/monitoring/parent_request_with_extra.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/parent_response.json",
                containsString("aff_parent_1007"));
    }

    @DbUnitDataSet(dataSource = "promoDataSource", before = "before_parent_normal.csv")
    @Test
    public void testParentExpired() throws Exception {
        testParentError("ru/yandex/market/mbi/affiliate/promo/monitoring/parent_request.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/parent_response_expired.json",
                is("Several parent promos are too old but still in use: 1"));
    }

    @DbUnitDataSet(dataSource = "promoDataSource", before = "before_parent_broken.csv")
    @Test
    public void testParentBroken() throws Exception {
        testParentError("ru/yandex/market/mbi/affiliate/promo/monitoring/parent_request.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/parent_response.json",
                containsString("1005"));
    }

    @DbUnitDataSet(dataSource = "promoDataSource", before = "before_parent_normal.csv")
    @Test
    public void testParentAwaitingImport() throws Exception {
        testParentError(
                "ru/yandex/market/mbi/affiliate/promo/monitoring/parent_request.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/parent_response_awaiting_import.json",
                containsString("1005"));
    }

    @DbUnitDataSet(dataSource = "promoDataSource", before = "before_parent_normal.csv")
    @Test
    public void testParentAwaitingBudgetUpdate() throws Exception {
        testParentError(
                "ru/yandex/market/mbi/affiliate/promo/monitoring/parent_request.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/parent_response_awaiting_budget_update.json",
                containsString("1005"));
    }

    @DbUnitDataSet(dataSource = "promoDataSource", before = "before_promocode_diff.csv")
    @Test
    public void testPromocodeDiff() throws Exception {
        testChildError("ru/yandex/market/mbi/affiliate/promo/monitoring/parent_request_promocodes.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/parent_response_promocodes.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/child_request_with_extra.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/child_response_normal.json",
                containsString("aff_promo_3")
                );
    }

    @DbUnitDataSet(dataSource = "promoDataSource", before = "before_promocode.csv")
    @Test
    public void testPromocodeBroken() throws Exception {
        testChildError("ru/yandex/market/mbi/affiliate/promo/monitoring/parent_request_promocodes.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/parent_response_promocodes.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/child_request.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/child_response_broken.json",
                containsString("aff_promo_2")
        );
    }

    @DbUnitDataSet(dataSource = "promoDataSource", before = "before_promocode.csv")
    @Test
    public void testPromocodeAwaitingImportTooLong() throws Exception {
        testChildError("ru/yandex/market/mbi/affiliate/promo/monitoring/parent_request_promocodes.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/parent_response_promocodes.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/child_request.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/child_response_awaiting_import.json",
                containsString("aff_promo_1")
        );
    }

    @DbUnitDataSet(dataSource = "promoDataSource", before = "before_promocode.csv")
    @Test
    public void testStrollerFailure() throws Exception {
        when(strollerProtoApi.getPromosByIds(any()).execute())
                .thenReturn(Response.error(500, ResponseBody.create(null, "")));
        promocodeMonitoring.checkAndUpdateStatus();

        assertThat(unit.getStatus(), is(MonitoringStatus.CRITICAL));
        assertThat(unit.getMessage(), is("Failure during checks: Stroller returned code 500"));
    }

    @DbUnitDataSet(dataSource = "promoDataSource", before = "before_promocode.csv")
    @Test
    public void testOk() throws Exception {
        setupApiMock("ru/yandex/market/mbi/affiliate/promo/monitoring/parent_request_promocodes.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/parent_response_promocodes.json");
        setupApiMock("ru/yandex/market/mbi/affiliate/promo/monitoring/child_request.json",
                "ru/yandex/market/mbi/affiliate/promo/monitoring/child_response_normal.json");

        promocodeMonitoring.checkAndUpdateStatus();

        assertThat(unit.getStatus(), is(MonitoringStatus.OK));
    }

    private void testParentError(
            String requestFile, String protoFile, Matcher<String> messageMatcher) throws Exception {
        setupApiMock(requestFile, protoFile);

        promocodeMonitoring.checkAndUpdateStatus();

        assertThat(unit.getStatus(), is(MonitoringStatus.CRITICAL));
        assertThat(unit.getMessage(), messageMatcher);
    }

    private void testChildError(
            String parentRequestFile, String parentResponseFile,
            String childRequestFile, String childResponseFile,
            Matcher<String> messageMatcher
    ) throws Exception {
        setupApiMock(parentRequestFile, parentResponseFile);
        setupApiMock(childRequestFile, childResponseFile);

        promocodeMonitoring.checkAndUpdateStatus();

        assertThat(unit.getStatus(), is(MonitoringStatus.CRITICAL));
        assertThat(unit.getMessage(), messageMatcher);
    }

    private void setupApiMock(String requestFile, String responseFile) throws Exception {
        SyncGetPromo.GetPromoBatchRequest.Builder request = SyncGetPromo.GetPromoBatchRequest.newBuilder();
        loadProto(requestFile, request);
        SyncGetPromo.GetPromoBatchResponse.Builder response = SyncGetPromo.GetPromoBatchResponse.newBuilder();
        loadProto(responseFile, response);
        when(strollerProtoApi.getPromosByIds(eq(request.build())).execute()).thenReturn(Response.success(response.build()));
    }

    private <T extends com.google.protobuf.GeneratedMessageV3.Builder<T>> void loadProto(String fileName, T builder) throws IOException {
        try (InputStream stream =
                     PromocodeMonitoringTest.class
                             .getClassLoader()
                             .getResourceAsStream(fileName)) {
            JsonFormat.parser().merge(new String(Objects.requireNonNull(stream).readAllBytes(), StandardCharsets.UTF_8), builder);
        }
    }

}