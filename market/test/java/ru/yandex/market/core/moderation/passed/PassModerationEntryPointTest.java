package ru.yandex.market.core.moderation.passed;

import java.util.List;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.core.balance.model.ClientInfo;
import ru.yandex.market.core.campaign.IdsResolver;
import ru.yandex.market.core.campaign.model.CampaignInfo;
import ru.yandex.market.core.complex.ComplexCampaignService;
import ru.yandex.market.core.complex.model.CampaignComplexInfo;
import ru.yandex.market.core.cutoff.CutoffService;
import ru.yandex.market.core.cutoff.model.CutoffInfo;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.ds.DatasourceService;
import ru.yandex.market.core.ds.DatasourceTransactionTemplate;
import ru.yandex.market.core.ds.model.DatasourceInfo;
import ru.yandex.market.core.feature.FeatureCutoffService;
import ru.yandex.market.core.moderation.TestingShop;
import ru.yandex.market.core.moderation.recommendation.SettingType;
import ru.yandex.market.core.moderation.recommendation.checker.PartnerSettingsChecker;
import ru.yandex.market.core.moderation.recommendation.service.DbPartnerSettingsRecommendationService;
import ru.yandex.market.core.moderation.sandbox.SandboxState;
import ru.yandex.market.core.moderation.sandbox.SandboxStateFactory;
import ru.yandex.market.core.moderation.sandbox.TestSandboxState;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.protocol.model.ActionContext;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.testing.TestingType;
import ru.yandex.market.core.xml.impl.NamedContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * @author zoom
 */
@RunWith(MockitoJUnitRunner.class)
public class PassModerationEntryPointTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ComplexCampaignService complexCampaignService;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private CutoffService cutoffService;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private MockSandboxRepositoryAdapter sandboxRepository;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private DatasourceService datasourceService;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private ParamService paramService;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SandboxStateFactory sandboxStateFactory;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private IdsResolver idsResolver;
    @Mock
    private PartnerSettingsChecker partnerSettingsChecker;
    @Mock
    private PartnerTypeAwareService partnerTypeAwareService;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private FeatureCutoffService featureCutoffService;

    @Test(expected = QManagerCutoffFoundException.class)
    public void shouldNotAllowUnconditionalModerationPassWhenQManagerExceptionExist() {

        doReturn(ImmutableMap.of(CutoffType.QMANAGER_OTHER, new CutoffInfo(1, CutoffType.QMANAGER_OTHER)))
                .when(cutoffService)
                .getCutoffs(eq(2L), any());

        TestingState testingState = new TestingState();
        testingState.setStatus(TestingStatus.PASSED);
        testingState.setTestingType(TestingType.CPC_PREMODERATION);
        SandboxState sandboxState = new TestSandboxState(testingState, null);
        doReturn(sandboxState).when(sandboxRepository).loadById(eq(111L));

        PassModerationEntryPoint entryPoint =
                new PassModerationEntryPoint(
                        new DatasourceTransactionTemplate() {
                            @Override
                            public <T> T execute(long datasourceId,
                                                 ActionContext actionContext,
                                                 Function<ShopActionContext, T> handler) {
                                return handler.apply(new ShopActionContext(1, datasourceId));
                            }
                        },
                        sandboxRepository,
                        cutoffService,
                        paramService,
                        datasourceService,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        featureCutoffService);

        entryPoint.pass(null, new TestingShop(111, 2));
    }

    @Test
    public void shouldPassGeneralLightCheckModeration() {

        doReturn(ImmutableMap.of(CutoffType.COMMON_OTHER, new CutoffInfo(1, CutoffType.COMMON_OTHER)))
                .when(cutoffService)
                .getCutoffs(eq(2L), any());

        TestingState testingState = new TestingState();
        testingState.setStatus(TestingStatus.PASSED);
        testingState.setTestingType(TestingType.GENERAL_LITE_CHECK);
        testingState.setDatasourceId(111L);
        SandboxState sandboxState = new TestSandboxState(testingState, null);
        doReturn(sandboxState).when(sandboxRepository).loadById(eq(111L));
        doReturn(testingState).when(sandboxStateFactory).getState(eq(sandboxState));
        doNothing().when(sandboxRepository).delete(any(), any());
        doReturn(true).when(cutoffService).closeCutoff(anyLong(), any(), anyLong());

        PassModerationEntryPoint entryPoint =
                new PassModerationEntryPoint(
                        new DatasourceTransactionTemplate() {
                            @Override
                            public <T> T execute(long datasourceId,
                                                 ActionContext actionContext,
                                                 Function<ShopActionContext, T> handler) {
                                return handler.apply(new ShopActionContext(1, datasourceId));
                            }
                        },
                        sandboxRepository,
                        cutoffService,
                        paramService,
                        datasourceService,
                        idsResolver,
                        null,
                        null,
                        sandboxStateFactory,
                        null,
                        null,
                        null,
                        null,
                        featureCutoffService);

        entryPoint.pass(null, new TestingShop(111, 2));
        Mockito.verify(sandboxRepository).delete(any(), eq(sandboxState));
        Mockito.verify(cutoffService).closeCutoff(eq(2L), eq(CutoffType.COMMON_OTHER), anyLong());


    }

    @Test
    public void cutoffsShouldNotInterfereWithEachOtherForGeneralCheck() {

        doReturn(ImmutableMap.of(CutoffType.COMMON_OTHER, new CutoffInfo(1, CutoffType.COMMON_OTHER),
                CutoffType.FORTESTING, new CutoffInfo(2, CutoffType.FORTESTING)))
                .when(cutoffService)
                .getCutoffs(eq(2L), any());

        TestingState testingState = new TestingState();
        testingState.setStatus(TestingStatus.PASSED);
        testingState.setTestingType(TestingType.GENERAL_LITE_CHECK);
        testingState.setDatasourceId(111L);
        SandboxState sandboxState = new TestSandboxState(testingState, null);
        doReturn(sandboxState).when(sandboxRepository).loadById(eq(111L));
        doReturn(testingState).when(sandboxStateFactory).getState(eq(sandboxState));
        doNothing().when(sandboxRepository).delete(any(), any());
        doReturn(true).when(cutoffService).closeCutoff(anyLong(), any(), anyLong());

        PassModerationEntryPoint entryPoint =
                new PassModerationEntryPoint(
                        new DatasourceTransactionTemplate() {
                            @Override
                            public <T> T execute(long datasourceId,
                                                 ActionContext actionContext,
                                                 Function<ShopActionContext, T> handler) {
                                return handler.apply(new ShopActionContext(1, datasourceId));
                            }
                        },
                        sandboxRepository,
                        cutoffService,
                        paramService,
                        datasourceService,
                        idsResolver,
                        null,
                        null,
                        sandboxStateFactory,
                        null,
                        null,
                        null,
                        null,
                        featureCutoffService);

        entryPoint.pass(null, new TestingShop(111, 2));
        Mockito.verify(sandboxRepository).delete(any(), eq(sandboxState));
        Mockito.verify(cutoffService).closeCutoff(eq(2L), eq(CutoffType.COMMON_OTHER), anyLong());
        Mockito.verify(cutoffService, Mockito.never()).closeCutoff(eq(2L), eq(CutoffType.FORTESTING), anyLong());


    }


    @Test
    public void cutoffsShouldNotInterfereWithEachOtherForCpcCheck() {

        doReturn(ImmutableMap.of(CutoffType.COMMON_OTHER, new CutoffInfo(1, CutoffType.COMMON_OTHER),
                CutoffType.FORTESTING, new CutoffInfo(2, CutoffType.FORTESTING),
                CutoffType.PARTNER_SCHEDULE, new CutoffInfo(3, CutoffType.PARTNER_SCHEDULE),
                CutoffType.CPA_PARTNER, new CutoffInfo(4, CutoffType.CPA_PARTNER)))
                .when(cutoffService)
                .getCutoffs(eq(2L), any());

        TestingState testingState = new TestingState();
        testingState.setStatus(TestingStatus.PASSED);
        testingState.setTestingType(TestingType.CPC_PREMODERATION);
        testingState.setDatasourceId(111L);
        SandboxState sandboxState = new TestSandboxState(testingState, null);
        doReturn(sandboxState).when(sandboxRepository).loadById(eq(111L));
        doReturn(testingState).when(sandboxStateFactory).getState(eq(sandboxState));
        doNothing().when(sandboxRepository).delete(any(), any());
        doReturn(true).when(cutoffService).closeCutoff(anyLong(), any(), anyLong());

        PassModerationEntryPoint entryPoint =
                new PassModerationEntryPoint(
                        new DatasourceTransactionTemplate() {
                            @Override
                            public <T> T execute(long datasourceId,
                                                 ActionContext actionContext,
                                                 Function<ShopActionContext, T> handler) {
                                return handler.apply(new ShopActionContext(1, datasourceId));
                            }
                        },
                        sandboxRepository,
                        cutoffService,
                        paramService,
                        datasourceService,
                        idsResolver,
                        null,
                        null,
                        sandboxStateFactory,
                        null,
                        null,
                        null,
                        null,
                        featureCutoffService);

        entryPoint.pass(null, new TestingShop(111, 2));
        Mockito.verify(sandboxRepository).delete(any(), eq(sandboxState));
        Mockito.verify(cutoffService).closeCutoff(eq(2L), eq(CutoffType.FORTESTING), anyLong());
        Mockito.verify(cutoffService, Mockito.never()).closeCutoff(eq(2L), eq(CutoffType.COMMON_OTHER), anyLong());


    }

    @DisplayName("Тест наличия в отправляемом уведомлении информации по проверяемым для рекомендаций настройкам магазина")
    @Test
    public void testRecommendations() {

        doReturn(ImmutableMap.of(CutoffType.FORTESTING, new CutoffInfo(2, CutoffType.FORTESTING)))
                .when(cutoffService)
                .getCutoffs(eq(2L), any());

        TestingState testingState = new TestingState();
        testingState.setStatus(TestingStatus.PASSED);
        testingState.setTestingType(TestingType.CPC_PREMODERATION);
        testingState.setDatasourceId(111L);

        SandboxState sandboxState = new TestSandboxState(testingState, null);
        doReturn(sandboxState).when(sandboxRepository).loadById(eq(111L));
        doReturn(testingState).when(sandboxStateFactory).getState(eq(sandboxState));

        doNothing().when(sandboxRepository).delete(any(), any());
        doReturn(true).when(cutoffService).closeCutoff(anyLong(), any(), anyLong());

        doReturn(SettingType.ROUND_THE_CLOCK).when(partnerSettingsChecker).getSettingType();
        doReturn(true).when(partnerSettingsChecker).isSettingMissed(anyLong());

        when(paramService.getParam(Mockito.eq(ParamType.NEVER_PAID), anyLong()))
                .thenReturn(new BooleanParamValue(ParamType.NEVER_PAID, 0L, true));

        doReturn(1111L).when(idsResolver).getCampaignId(anyLong());

        doReturn(new CampaignComplexInfo(new CampaignInfo(), new DatasourceInfo(), new ClientInfo())).when(complexCampaignService).getInfo(anyLong());

        PassModerationEntryPoint entryPoint =
                new PassModerationEntryPoint(
                        new DatasourceTransactionTemplate() {
                            @Override
                            public <T> T execute(long datasourceId,
                                                 ActionContext actionContext,
                                                 Function<ShopActionContext, T> handler) {
                                return handler.apply(new ShopActionContext(1, datasourceId));
                            }
                        },
                        sandboxRepository,
                        cutoffService,
                        paramService,
                        datasourceService,
                        idsResolver,
                        complexCampaignService,
                        notificationService,
                        sandboxStateFactory,
                        new DbPartnerSettingsRecommendationService(List.of(partnerSettingsChecker)),
                        null,
                        partnerTypeAwareService,
                        null,
                        featureCutoffService);

        entryPoint.pass(null, new TestingShop(111, 2));

        Mockito.verify(sandboxRepository).delete(any(), eq(sandboxState));
        Mockito.verify(cutoffService).closeCutoff(eq(2L), eq(CutoffType.FORTESTING), anyLong());
        Mockito.verify(partnerTypeAwareService).isSmb(111);

        Mockito.verify(notificationService, Mockito.times(1))
                .send(Mockito.anyInt(),
                        Mockito.eq(111L),
                        Mockito.argThat(arg -> arg.contains(
                                new NamedContainer("settings-recommendations", List.of(SettingType.ROUND_THE_CLOCK.name()))))
                );
    }
}
