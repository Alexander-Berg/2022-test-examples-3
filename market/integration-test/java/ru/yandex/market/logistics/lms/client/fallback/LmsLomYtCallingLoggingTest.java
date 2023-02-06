package ru.yandex.market.logistics.lms.client.fallback;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.logistics.lms.client.models.FallbackClientLogTestInfo;
import ru.yandex.market.logistics.lms.client.utils.LmsLomClientLogUtils;
import ru.yandex.market.logistics.lom.configuration.properties.LmsYtProperties;
import ru.yandex.market.logistics.lom.entity.InternalVariable;
import ru.yandex.market.logistics.lom.entity.enums.InternalVariableType;
import ru.yandex.market.logistics.lom.lms.client.LmsLomYtClient;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.request.partnerRelation.PartnerRelationFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.request.schedule.LogisticSegmentInboundScheduleFilter;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ParametersAreNonnullByDefault
@DisplayName("Тест на логирование обращений в YT")
@DatabaseSetup("/lms/client/fallback/all_yt_methods_enabled.xml")
public class LmsLomYtCallingLoggingTest extends AbstractFallbackWithLoggedCallsTest {

    private static final String YT_ACTUAL_VERSION = "2022-03-02T08:05:24Z";
    private static final String SELECT_QUERY = "* FROM [%s]";

    @Autowired
    private LmsLomYtClient lmsLomYtClient;

    @Autowired
    private Yt hahnYt;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private YtTables ytTables;

    @Autowired
    private LmsYtProperties lmsYtProperties;

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();

        YtLmsVersionsUtils.mockYtVersionTable(ytTables, lmsYtProperties, YT_ACTUAL_VERSION);
        doReturn(ytTables)
            .when(hahnYt).tables();
    }

    @AfterEach
    void tearDown() {
        verifySelectQuery(lmsYtProperties.getDynamicPartnerPath(YT_ACTUAL_VERSION), 2);
        verifySelectQuery(lmsYtProperties.getDynamicLogisticsPointPath(YT_ACTUAL_VERSION), 2);

        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        verify(hahnYt, times(11)).tables();
        verifyNoMoreInteractions(hahnYt, ytTables);
    }

    @Override
    void setWriteInClientLog(boolean loggingEnabled) {
        internalVariableRepository.save(
            new InternalVariable()
                .setType(InternalVariableType.WRITE_YT_USAGE_IN_LOG)
                .setValue(String.valueOf(loggingEnabled))
        );
    }

    @Override
    void callClientAndVerifyAndCheckLog(FallbackClientLogTestInfo testInfo) {
        testInfo.getLmsDataClientCallingAndVerifying().run();
        softly.assertThat(backLogCaptor.getResults().toString().contains(
                LmsLomClientLogUtils.ytClientCalling(testInfo.getMethodName(), testInfo.getParams())
            ))
            .isEqualTo(testInfo.isLoggingEnabled());
    }

    @Nonnull
    @Override
    public FallbackClientLogTestInfo getLogisticsPointTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    String tableName = lmsYtProperties.getDynamicLogisticsPointPath(YT_ACTUAL_VERSION);
                    mockYtSelectQueryWithEmptyList(tableName);
                    lmsLomYtClient.getLogisticsPoint(1L);
                }
            )
            .setMethodName("getLogisticsPointById")
            .setParams("id = ");
    }

    @Nonnull
    @Override
    public FallbackClientLogTestInfo getPartnerTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    String tableName = lmsYtProperties.getDynamicPartnerPath(YT_ACTUAL_VERSION);
                    mockYtSelectQueryWithEmptyOptional(tableName);
                    lmsLomYtClient.getPartner(1L);
                }
            )
            .setMethodName("getPartnerById")
            .setParams("id = 1");
    }

    @Nonnull
    @Override
    public FallbackClientLogTestInfo getPartnersTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    String tableName = lmsYtProperties.getDynamicPartnerPath(YT_ACTUAL_VERSION);
                    mockYtSelectQueryWithEmptyList(tableName);
                    lmsLomYtClient.getPartners(Set.of(1L));
                }
            )
            .setMethodName("getPartnersByIds")
            .setParams("[1]");
    }

    @Nonnull
    @Override
    public FallbackClientLogTestInfo getLogisticsPointsTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    String tableName = lmsYtProperties.getDynamicLogisticsPointPath(YT_ACTUAL_VERSION);
                    mockYtSelectQueryWithEmptyList(tableName);
                    LogisticsPointFilter filter = LogisticsPointFilter.newBuilder()
                        .ids(Set.of(1L))
                        .partnerIds(Set.of(2L))
                        .type(PointType.PICKUP_POINT)
                        .active(true)
                        .build();
                    lmsLomYtClient.getLogisticsPoints(filter);
                }
            )
            .setMethodName("getLogisticsPointsByFilter")
            .setParams("ids = [1], partnerIds = [2], type = PICKUP_POINT, active = true");

    }

    @Nonnull
    @Override
    public FallbackClientLogTestInfo getPartnerExternalParamsTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    String tableName = lmsYtProperties.getDynamicPartnerExternalParamPath(YT_ACTUAL_VERSION);
                    mockYtSelectQueryWithEmptyList(tableName);
                    Set<PartnerExternalParamType> types = Set.of(
                        PartnerExternalParamType.DISABLE_AUTO_CANCEL_AFTER_SLA
                    );
                    lmsLomYtClient.getPartnerExternalParams(types);
                    verifySelectQuery(tableName);
                }
            )
            .setMethodName("getPartnerExternalParamsByTypes")
            .setParams("types = [DISABLE_AUTO_CANCEL_AFTER_SLA]");

    }

    @Nonnull
    @Override
    public FallbackClientLogTestInfo searchPartnerRelationWithCutoffsTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    String tableName = lmsYtProperties.getDynamicPartnerRelationPath(YT_ACTUAL_VERSION);
                    mockYtSelectQueryWithEmptyList(tableName);
                    PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
                        .fromPartnersIds(Set.of(1L))
                        .toPartnersIds(Set.of(2L))
                        .build();
                    lmsLomYtClient.searchPartnerRelationWithCutoffs(partnerRelationFilter);
                    verifySelectQuery(tableName);
                }
            )
            .setMethodName("searchPartnerRelationWithCutoffs")
            .setParams("partnerFromIds = [1], partnerToIds = [2]");

    }

    @Nonnull
    @Override
    public FallbackClientLogTestInfo searchPartnerRelationsWithReturnPartnersTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    String tableName = lmsYtProperties.getDynamicPartnerRelationToPath(YT_ACTUAL_VERSION);
                    mockYtSelectQueryWithEmptyList(tableName);
                    PartnerRelationFilter partnerRelationFilter = PartnerRelationFilter.newBuilder()
                        .fromPartnersIds(Set.of(1L))
                        .build();
                    lmsLomYtClient.searchPartnerRelationsWithReturnPartners(partnerRelationFilter);
                    verifySelectQuery(tableName);
                }
            )
            .setMethodName("searchPartnerRelationsWithReturnPartners")
            .setParams("partnerFromIds = [1]");

    }

    @Nonnull
    @Override
    public FallbackClientLogTestInfo searchInboundScheduleTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    YtUtils.mockSelectRowsFromYtQueryStartsWith(
                        ytTables,
                        List.of(),
                        "schedules"
                    );
                    LogisticSegmentInboundScheduleFilter filter = new LogisticSegmentInboundScheduleFilter()
                        .setFromPartnerId(11L)
                        .setToPartnerId(22L)
                        .setDeliveryType(DeliveryType.COURIER);
                    lmsLomYtClient.searchInboundSchedule(filter);
                    YtUtils.verifySelectRowsInteractionsQueryStartsWith(
                        ytTables,
                        "schedules"
                    );
                }
            )
            .setMethodName("searchInboundSchedule")
            .setParams("fromPartnerId = 11, toPartnerId = 22, deliveryType = COURIER");

    }

    @Nonnull
    @Override
    public FallbackClientLogTestInfo searchPartnerApiSettingsMethodsTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    String tableName = lmsYtProperties.getDynamicPartnerApiSettingsPath(YT_ACTUAL_VERSION);
                    mockYtSelectQueryWithEmptyList(tableName);
                    SettingsMethodFilter settingsMethodFilter = SettingsMethodFilter.newBuilder()
                        .partnerIds(Set.of(111L))
                        .methodTypes(Set.of("updateRecipient"))
                        .build();
                    lmsLomYtClient.searchPartnerApiSettingsMethods(settingsMethodFilter);
                    verifySelectQuery(tableName);
                }
            )
            .setMethodName("searchPartnerApiSettingsMethods")
            .setParams("partnerIds = [111], methodTypes = [updateRecipient]");

    }

    @Nonnull
    @Override
    public FallbackClientLogTestInfo getScheduleDayTestInfo() {
        return new FallbackClientLogTestInfo()
            .setLmsDataClientCallingAndVerifying(
                () -> {
                    String tableName = lmsYtProperties.getDynamicScheduleDayByIdPath(YT_ACTUAL_VERSION);
                    mockYtSelectQueryWithEmptyOptional(tableName);
                    lmsLomYtClient.getScheduleDay(123L);
                    verifySelectQuery(tableName);
                }
            )
            .setMethodName("getScheduleDayById")
            .setParams("id = 123");
    }

    private void mockYtSelectQueryWithEmptyOptional(String tableName) {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            Optional.empty(),
            String.format(SELECT_QUERY, tableName)
        );
    }

    private void mockYtSelectQueryWithEmptyList(String tableName) {
        YtUtils.mockSelectRowsFromYtQueryStartsWith(
            ytTables,
            List.of(),
            String.format(SELECT_QUERY, tableName)
        );
    }

    private void verifySelectQuery(String tableName) {
        verifySelectQuery(tableName, 1);
    }

    private void verifySelectQuery(String tableName, int times) {
        YtUtils.verifySelectRowsInteractionsQueryStartsWith(
            ytTables,
            String.format(SELECT_QUERY, tableName),
            times
        );
    }
}
