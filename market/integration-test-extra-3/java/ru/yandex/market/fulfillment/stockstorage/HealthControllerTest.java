package ru.yandex.market.fulfillment.stockstorage;

import java.util.Arrays;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.JobInfoDao;
import ru.yandex.market.fulfillment.stockstorage.service.health.ping.BalancerAvailabilityHealthChecker;
import ru.yandex.market.fulfillment.stockstorage.service.health.ping.PostgresHealthChecker;
import ru.yandex.market.fulfillment.stockstorage.service.health.ping.ShutdownHealthChecker;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

public class HealthControllerTest extends AbstractContextualTest {

    @Autowired
    JobInfoDao jobInfoDao;
    @Autowired
    private MockMvc mockMvc;
    @SpyBean
    private PostgresHealthChecker postgresHealthChecker;

    @SpyBean
    private ShutdownHealthChecker shutdownHealthChecker;

    @Autowired
    private BalancerAvailabilityHealthChecker balancerAvailabilityHealthChecker;

    @Test
    public void pingWithDbConnectionError() throws Exception {
        when(postgresHealthChecker.check())
                .thenReturn(new CheckResult(CheckResult.Level.CRITICAL, "Database connection is lost"));

        String contentAsString = mockMvc.perform(get("/ping"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        softly.assertThat(contentAsString).contains("2;", "Database connection is lost");
        verify(postgresHealthChecker, times(1)).check();
    }

    @Test
    public void pingWhenAppIsTerminating() throws Exception {
        when(shutdownHealthChecker.check())
                .thenReturn(new CheckResult(CheckResult.Level.CRITICAL, "Application is terminating"));

        String contentAsString = mockMvc.perform(get("/ping"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        softly.assertThat(contentAsString).contains("2;", "Application is terminating");
        verify(shutdownHealthChecker, times(1)).check();
    }

    @Test
    public void pingWhenAppIsTerminatingAndDbLostConnection() throws Exception {
        when(postgresHealthChecker.check())
                .thenReturn(new CheckResult(CheckResult.Level.CRITICAL, "Database connection is lost"));

        when(shutdownHealthChecker.check())
                .thenReturn(new CheckResult(CheckResult.Level.CRITICAL, "Application is terminating"));

        String contentAsString = mockMvc.perform(get("/ping"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        softly.assertThat(contentAsString).contains("2;");
        verify(shutdownHealthChecker, times(1)).check();
        verify(postgresHealthChecker, times(0)).check();
    }

    @Test
    public void pingWithWarnings() throws Exception {
        when(postgresHealthChecker.check())
                .thenReturn(new CheckResult(CheckResult.Level.WARNING, "Some warning"));

        String contentAsString = mockMvc.perform(get("/ping"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        softly.assertThat(contentAsString).contains("2;", "Some warning");
        verify(postgresHealthChecker, times(1)).check();
    }

    @Test
    public void ping() throws Exception {
        String contentAsString = mockMvc.perform(get("/ping"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .isEqualTo("0;OK");
    }

    /**
     * В БД есть ску из складов 1 и 2.
     * Активными считается только склад 1.
     * <p>
     * Должны получить ворнинг с информацией о втором складе.
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/health/wh_desync/1.xml")
    public void warehouseDesyncIsWarning1() throws Exception {
        setActiveWarehouses(1);

        String contentAsString = mockMvc.perform(get("/health/warehousesDesync"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        softly
                .assertThat(contentAsString)
                .isEqualTo("0;OK");
    }

    /**
     * В БД есть ску из складов 1 и 2.
     * Из ЛМС приходят 2 склада, у второго sync выключен
     * <p>
     * Должны получить OK.
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/health/wh_desync/1.xml")
    public void warehouseDesyncIsOkWhenOneSyncDisabled() throws Exception {
        checkDesync();
    }


    /**
     * В БД есть ску из склада 1.
     * Активным считается склады 1 и 2.
     * <p>
     * Должны получить ворнинг с информацией о втором складе.
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/health/wh_desync/2.xml")
    public void warehouseDesyncIsWarning2() throws Exception {
        setActiveWarehouses(1, 2);

        String contentAsString = mockMvc.perform(get("/health/warehousesDesync"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        softly
                .assertThat(contentAsString)
                .isEqualTo("2;SS DB does not contain data from existing warehouses [2]");
    }

    /**
     * В БД есть ску из склада 1.
     * Из ЛМС приходят склады 1 (активный) и 2 (неактивный).
     * <p>
     * Должны получить ок.
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/health/wh_desync/2.xml")
    public void warehouseDesyncIsOkWhenSyncInLmsIsOff() throws Exception {
        checkDesync();
    }

    /**
     * В БД есть ску из складов 1 и 2.
     * Активными считаются склады 1 и 2.
     * <p>
     * Должны получить 0;ОК
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/health/wh_desync/3.xml")
    public void warehouseDesyncIsOk() throws Exception {
        setActiveWarehouses(1, 2);

        String contentAsString = mockMvc.perform(get("/health/warehousesDesync"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        softly
                .assertThat(contentAsString)
                .isEqualTo("0;OK");
    }

    /**
     * Последняя попытка синхронизации wh с источником завершилась с ошибкой.
     * <p>
     * Должны получить 2;Cache sync failure detected...
     */
    @Test
    public void latestWarehouseSyncFailed() throws Exception {
        doThrow(RuntimeException.class).when(fulfillmentLmsClient).searchPartners(any(SearchPartnerFilter.class));

        warehouseSyncService.recomputeCache();

        verify(fulfillmentLmsClient, atLeastOnce()).searchPartners(any(SearchPartnerFilter.class));

        String contentAsString = mockMvc.perform(get("/health/warehousesDesync"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        softly
                .assertThat(contentAsString)
                .containsSequence("2;Cache sync failure detected");
    }

    /**
     * В лмс синк стоков вкл в СС вкл должны получить OK
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/ff_interval/both_jobs_on.xml")
    public void lmsWarehouseOnSsOn() throws Exception {
        String contentAsString = prepareLmsMock(true);

        softly.assertThat(contentAsString).isEqualTo("0;OK");
    }

    /**
     * В ЛМС синк стоков вкл, в сс одна из джоб выключена -> Warning
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/ff_interval/one_job_on.xml")
    public void lmsWarehouseOnSsOneOff() throws Exception {
        String contentAsString = prepareLmsMock(true);

        softly.assertThat(contentAsString).isEqualTo("1;SS does not sync stocks for warehouses switched on in LMS: " +
                "[3]");
    }

    /**
     * В ЛМС синк стоков вкл, в сс обе джобы выключены -> Warning
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/ff_interval/none_jobs_on.xml")
    public void lmsWarehouseOnSsBothOff() throws Exception {
        String contentAsString = prepareLmsMock(true);

        softly.assertThat(contentAsString).isEqualTo("1;SS does not sync stocks for warehouses switched on in LMS: " +
                "[3]");
    }

    /**
     * В лмс синк стоков выкл в СС выкл должны получить OK
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/ff_interval/none_jobs_on.xml")
    public void lmsWarehouseOffSsOff() throws Exception {
        String contentAsString = prepareLmsMock(false);

        softly.assertThat(contentAsString).isEqualTo("0;OK");
    }

    /**
     * В ЛМС синк стоков выкл, в сс одна из джоб выключена -> Critical
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/ff_interval/one_job_on.xml")
    public void lmsWarehouseOffSsOneOff() throws Exception {
        String contentAsString = prepareLmsMock(false);

        softly.assertThat(contentAsString).isEqualTo("0;OK");
    }

    /**
     * В ЛМС синк стоков выкл, в сс обе джобы включены -> Critical
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/ff_interval/both_jobs_on.xml")
    public void lmsWarehouseOffSsBothOn() throws Exception {
        String contentAsString = prepareLmsMock(false);

        softly.assertThat(contentAsString).isEqualTo("0;OK");
    }

    /**
     * В ЛМС синк стоков у партнера 100 выкл, у 200 вкл, в сс у партнера 100 вкл, у 200 выкл -> Critical
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/ff_interval/diff_jobs_diff_partners.xml")
    public void critAndWarnSameTime() throws Exception {
        mockSearchPartners(Arrays.asList(
                PartnerResponse.newBuilder()
                        .id(100L)
                        .autoSwitchStockSyncEnabled(true)
                        .stockSyncEnabled(false)
                        .partnerType(PartnerType.DROPSHIP)
                        .status(PartnerStatus.ACTIVE)
                        .name("Partner")
                        .build(),
                PartnerResponse.newBuilder()
                        .id(200L)
                        .autoSwitchStockSyncEnabled(true)
                        .stockSyncEnabled(true)
                        .partnerType(PartnerType.FULFILLMENT)
                        .status(PartnerStatus.ACTIVE)
                        .name("Partner")
                        .build()
        ));

        warehouseSyncService.recomputeCache();

        String contentAsString = mockMvc.perform(get("/health/lmsStockSync"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        System.out.println(contentAsString);

        softly.assertThat(contentAsString).isEqualTo(
                "1;SS does not sync stocks for warehouses switched on in LMS: [200]"
        );
    }

    @Test
    public void changeBalancerAvailabilityTest() throws Exception {
        assertPingAnswer("0;OK");
        balancerAvailabilityHealthChecker.closeAppForBalancer();
        assertPingAnswer("2;");
        balancerAvailabilityHealthChecker.openAppForBalancer();
        assertPingAnswer("0;OK");
    }

    private void assertPingAnswer(String expected) throws Exception {
        String contentAsString = mockMvc.perform(get("/ping"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        softly
                .assertThat(contentAsString)
                .containsSequence(expected);

    }

    private String prepareLmsMock(boolean stockSyncEnabled) throws Exception {
        mockSearchPartners(Arrays.asList(
                PartnerResponse.newBuilder()
                        .id(3L)
                        .autoSwitchStockSyncEnabled(true)
                        .stockSyncEnabled(stockSyncEnabled)
                        .partnerType(PartnerType.DROPSHIP)
                        .status(PartnerStatus.ACTIVE)
                        .name("Partner")
                        .build()
        ));

        warehouseSyncService.recomputeCache();

        return mockMvc.perform(get("/health/lmsStockSync"))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private void checkDesync() throws Exception {
        mockSearchPartners(Arrays.asList(
                PartnerResponse.newBuilder()
                        .id(1)
                        .status(PartnerStatus.ACTIVE)
                        .stockSyncEnabled(true)
                        .build(),
                PartnerResponse.newBuilder()
                        .id(2)
                        .status(PartnerStatus.ACTIVE)
                        .stockSyncEnabled(false)
                        .build()
        ));

        warehouseSyncService.recomputeCache();

        String contentAsString = mockMvc.perform(get("/health/warehousesDesync"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(fulfillmentLmsClient, times(1))
                .searchPartners(any(SearchPartnerFilter.class));

        softly
                .assertThat(contentAsString)
                .isEqualTo("0;OK");
    }
}
