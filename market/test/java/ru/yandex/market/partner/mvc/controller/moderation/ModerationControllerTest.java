package ru.yandex.market.partner.mvc.controller.moderation;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.market.core.cutoff.CutoffService;
import ru.yandex.market.core.cutoff.model.AboScreenshotDto;
import ru.yandex.market.core.cutoff.model.CutoffInfo;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.ds.info.DatasourceInformationService;
import ru.yandex.market.core.ds.info.UniShopInformation;
import ru.yandex.market.core.feature.FeatureService;
import ru.yandex.market.core.feature.model.FeatureCutoffInfo;
import ru.yandex.market.core.feature.model.FeatureCutoffMessage;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.feature.model.FeatureDescription;
import ru.yandex.market.core.feature.model.FeatureType;
import ru.yandex.market.core.feature.model.cutoff.CommonCutoffs;
import ru.yandex.market.core.feature.model.cutoff.DSBSCutoffs;
import ru.yandex.market.core.feature.model.cutoff.FeatureCustomCutoffType;
import ru.yandex.market.core.feature.precondition.FeaturePrecondition;
import ru.yandex.market.core.feature.precondition.model.PreconditionResult;
import ru.yandex.market.core.moderation.DefaultModerationService;
import ru.yandex.market.core.moderation.ModerationCutoffs;
import ru.yandex.market.core.moderation.ModerationDisabledReason;
import ru.yandex.market.core.moderation.ModerationService;
import ru.yandex.market.core.moderation.request.ModerationCause;
import ru.yandex.market.core.moderation.request.ModerationDetailsType;
import ru.yandex.market.core.moderation.request.ModerationRequestState;
import ru.yandex.market.core.moderation.request.ModerationShopProgramDetails;
import ru.yandex.market.core.moderation.request.ModerationShopProgramState;
import ru.yandex.market.core.moderation.sandbox.SandboxRepository;
import ru.yandex.market.core.moderation.sandbox.SandboxState;
import ru.yandex.market.core.moderation.sandbox.SandboxStateFactory;
import ru.yandex.market.core.moderation.sandbox.impl.DefaultSandboxStateFactory;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.partner.model.PartnerTypeAwareInfo;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingService;
import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.testing.TestingType;
import ru.yandex.market.partner.mvc.MockPartnerRequest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.feature.model.FeatureCutoffType.BY_PARTNER;
import static ru.yandex.market.core.feature.model.FeatureType.MARKETPLACE;
import static ru.yandex.market.core.feature.model.FeatureType.MARKETPLACE_SELF_DELIVERY;

/**
 * Тест на логику работы {@link ModerationController}.
 *
 * @author fbokovikov
 */
@RunWith(MockitoJUnitRunner.class)
public class ModerationControllerTest {

    private static final long SHOP_ID = 774L;
    private static final FeatureCutoffMessage RECENT_MESSAGE = new FeatureCutoffMessage(SHOP_ID,
            MARKETPLACE_SELF_DELIVERY,
            "Subject", "Body", List.of(new AboScreenshotDto(1L, "hash1"), new AboScreenshotDto(2L, "hash2")));
    private final MockPartnerRequest request = new MockPartnerRequest(1, 1, SHOP_ID, 1);
    private final SandboxStateFactory sandboxStateFactory = new DefaultSandboxStateFactory(Date::new);
    private ModerationController moderationController;

    @Mock
    private CutoffService cutoffService;

    @Mock
    private DatasourceInformationService datasourceInformationService;

    @Mock
    private SandboxRepository sandboxRepository;

    @Mock
    private TestingService testingService;

    @Mock
    private PartnerTypeAwareService partnerTypeAwareService;

    @Mock
    private FeatureService featureService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Before
    public void init() {
        ModerationService moderationService = new DefaultModerationService(
                sandboxRepository,
                sandboxStateFactory,
                cutoffService,
                datasourceInformationService,
                null,
                testingService,
                partnerTypeAwareService,
                featureService,
                applicationEventPublisher);
        moderationController = new ModerationController(moderationService);
    }

    /**
     * Модерация магазина возможна по каждой из программ {@link ShopProgram} размещения на маркете.
     */
    @Test
    public void testModerationEnabled() {
        mockType(true, true);
        mockNonFatalCutoffs();
        mockOkSandboxState();
        ModerationRequestState state = moderationController.getModerationRequestState(request);
        assertTrue(moderationEnabled(state, ShopProgram.CPA));
        assertTrue(moderationEnabled(state, ShopProgram.CPC));
    }

    /**
     * CPA-фатальное отключение {@link ru.yandex.market.core.moderation.ModerationDisabledReason#FATAL_CUTOFFS}.
     * не должно мешать CPC-модерации
     */
    @Test
    public void testCpcModerationDisabledFatalCutoff() {
        mockType(false, true);
        mockFatalCutoffs();
        mockOkSandboxState();
        ModerationRequestState state = moderationController.getModerationRequestState(request);
        assertTrue(moderationDisabled(state, ShopProgram.CPA, Collections.singletonList(
                ModerationDisabledReason.FATAL_CUTOFFS
        )));
        assertTrue(moderationDisabled(state, ShopProgram.CPC, Collections.singletonList(
                ModerationDisabledReason.FATAL_CUTOFFS
        )));
    }

    /**
     * Отсутствие CPC-специфичного datasource-параметра {@link UniShopInformation} не должно мешать CPA-модерации.
     */
    @Test
    public void testCpcModerationDisabledParams() {
        mockType(true, true);
        mockNonOkParams();
        mockNonFatalCutoffs();
        mockOkSandboxState();
        ModerationRequestState state = moderationController.getModerationRequestState(request);
        assertTrue(moderationEnabled(state, ShopProgram.CPA));
        assertTrue(moderationDisabled(state, ShopProgram.CPC, Collections.singletonList(
                ModerationDisabledReason.MISSED_DATASOURCE_PARAMS
        )));
    }

    @Test
    public void testDsbsModerationNoProbles() {
        mockType(true, true);
        mockOkSandboxState();
        List<ModerationShopProgramState> problems = moderationController.getModerationShopProgramStates(request);
        assertTrue(problems.isEmpty());
    }

    @Test
    public void testProblemsSortOrder() {
        mockType(true, true);
        mockFeatureCutoffs(MARKETPLACE_SELF_DELIVERY,
                FeatureCutoffType.PINGER,
                DSBSCutoffs.QUALITY_SERIOUS,
                DSBSCutoffs.QUALITY_OTHER,
                CommonCutoffs.CLONE,
                CommonCutoffs.FRAUD
        );
        List<ModerationShopProgramState> problems = moderationController.getModerationShopProgramStates(request);
        Assertions.assertThat(problems.stream().map(ModerationShopProgramState::getDetails))
                .usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorIgnoringFields("startDate", "attemptsLeft")
                .containsExactlyElementsOf(List.of(
                        new ModerationShopProgramDetails(
                                0,
                                false,
                                null,
                                null,
                                ModerationCause.FRAUD,
                                new HashSet<>(),
                                RECENT_MESSAGE,
                                ModerationDetailsType.CUTOFF),
                        new ModerationShopProgramDetails(
                                0,
                                false,
                                null,
                                null,
                                ModerationCause.QUALITY,
                                new HashSet<>(),
                                RECENT_MESSAGE,
                                ModerationDetailsType.CUTOFF),
                        new ModerationShopProgramDetails(
                                0,
                                false,
                                null,
                                null,
                                ModerationCause.CLONE,
                                new HashSet<>(),
                                RECENT_MESSAGE,
                                ModerationDetailsType.CUTOFF),
                        new ModerationShopProgramDetails(
                                0,
                                false,
                                null,
                                null,
                                ModerationCause.PINGER,
                                Set.of(ModerationDisabledReason.FATAL_CUTOFFS,
                                        ModerationDisabledReason.MODERATION_NOT_NEEDED),
                                RECENT_MESSAGE,
                                ModerationDetailsType.CUTOFF),
                        new ModerationShopProgramDetails(
                                0,
                                false,
                                null,
                                null,
                                ModerationCause.OTHER,
                                Set.of(ModerationDisabledReason.FATAL_CUTOFFS),
                                RECENT_MESSAGE,
                                ModerationDetailsType.CUTOFF)
                ));
    }

    @Test
    public void testDbsModerationHold() {
        mockType(true, true);
        mockFeatureCutoffs(MARKETPLACE_SELF_DELIVERY,
                DSBSCutoffs.MODERATION_NEED_INFO,
                FeatureCutoffType.TESTING);
        List<ModerationShopProgramState> problems = moderationController.getModerationShopProgramStates(request);
        Assertions.assertThat(problems.stream().map(ModerationShopProgramState::getDetails))
                .usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorIgnoringFields("startDate", "attemptsLeft")
                .containsExactlyElementsOf(List.of(
                        new ModerationShopProgramDetails(
                                0,
                                false,
                                null,
                                null,
                                ModerationCause.MODERATION_NEED_INFO,
                                Set.of(ModerationDisabledReason.MODERATION_NOT_NEEDED),
                                RECENT_MESSAGE,
                                ModerationDetailsType.INFORMATION)
                ));
    }

    @Test
    public void testDsbsNonFatalProblems() {
        mockType(true, true);
        mockFeatureCutoffs(MARKETPLACE_SELF_DELIVERY, DSBSCutoffs.QUALITY_OTHER);
        List<ModerationShopProgramState> problems = moderationController.getModerationShopProgramStates(request);
        assertFalse(problems.isEmpty());
        assertTrue(problems.stream().allMatch(
                e -> e.getDetails().isModerationEnabled()
        ));
    }

    @Test
    public void testDsbsFatalProblems() {
        mockType(true, true);
        mockFeatureCutoffs(MARKETPLACE_SELF_DELIVERY, DSBSCutoffs.QUALITY_SERIOUS);
        List<ModerationShopProgramState> problems = moderationController.getModerationShopProgramStates(request);
        assertFalse(problems.isEmpty());
        assertTrue(problems.stream()
                .anyMatch(e -> !e.getDetails().isModerationEnabled())
        );
    }

    @Test
    public void testDsbsModerationDisabledReasons() {
        mockNonOkSandboxState();
        mockType(false, true);
        mockFeatureCutoffs(MARKETPLACE_SELF_DELIVERY, DSBSCutoffs.QUALITY_SERIOUS, CommonCutoffs.FEED);
        List<ModerationShopProgramState> problems = moderationController.getModerationShopProgramStates(request);
        Assertions.assertThat(problems.stream().map(ModerationShopProgramState::getDetails))
                .usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorIgnoringFields("startDate")
                .containsExactlyElementsOf(List.of(
                        new ModerationShopProgramDetails(
                                3,
                                false,
                                null,
                                null,
                                ModerationCause.QUALITY,
                                Set.of(
                                        ModerationDisabledReason.FEED_ERRORS,
                                        ModerationDisabledReason.MISSED_DATASOURCE_PARAMS
                                ),
                                RECENT_MESSAGE,
                                ModerationDetailsType.CUTOFF)
                ));
    }

    @Test
    public void testDsbsModerationNotRequiredReasons() {
        mockNonOkSandboxState();
        mockType(true, true);
        mockFeatureCutoffs(MARKETPLACE_SELF_DELIVERY, CommonCutoffs.FEED);
        mockByPartnerCutoff();
        List<ModerationShopProgramState> problems = moderationController.getModerationShopProgramStates(request);
        Assertions.assertThat(problems.stream().map(ModerationShopProgramState::getDetails))
                .usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorIgnoringFields("startDate")
                .containsExactlyElementsOf(List.of(
                        new ModerationShopProgramDetails(
                                -1,
                                false,
                                null,
                                null,
                                ModerationCause.BY_PARTNER,
                                Set.of(
                                        ModerationDisabledReason.FEED_ERRORS,
                                        ModerationDisabledReason.MODERATION_NOT_NEEDED
                                ),
                                RECENT_MESSAGE,
                                ModerationDetailsType.CUTOFF)
                ));
    }

    @Test
    @DisplayName("Плашка \"Магазин на проверке\"")
    public void testDsbsModerationTesting() {
        SandboxState cpaSandboxState = sandboxStateFactory.create(new TestingState()
                .setAttemptNum(3)
                .setPushReadyButtonCount(3)
                .setStatus(TestingStatus.WAITING_FEED_FIRST_LOAD)
                .setTestingType(TestingType.CPA_PREMODERATION)
        );
        lenient().when(sandboxRepository.load(SHOP_ID, ShopProgram.CPA)).thenReturn(cpaSandboxState);
        mockType(true, true);
        mockFeatureCutoffs(MARKETPLACE_SELF_DELIVERY, FeatureCutoffType.TESTING);
        List<ModerationShopProgramState> problems = moderationController.getModerationShopProgramStates(request);
        Assertions.assertThat(problems.stream().map(ModerationShopProgramState::getDetails))
                .usingRecursiveFieldByFieldElementComparator()
                .usingElementComparatorIgnoringFields("startDate")
                .containsExactlyElementsOf(List.of(
                        new ModerationShopProgramDetails(
                                3,
                                false,
                                null,
                                null,
                                ModerationCause.TESTING,
                                Set.of(
                                        ModerationDisabledReason.MODERATION_IN_PROGRESS
                                ),
                                RECENT_MESSAGE,
                                ModerationDetailsType.INFORMATION)
                ));
    }

    /**
     * Модерация не возможна по CPA и CPC из-за превышения количества попыток
     * {@link ModerationCutoffs#MAX_ATTEMPTS_NUMBER}.
     */
    @Test
    public void testModerationDisabledAttempts() {
        mockType(false, true);
        mockNonFatalCutoffs();
        mockNonOkSandboxState();
        ModerationRequestState state = moderationController.getModerationRequestState(request);
        assertTrue(moderationDisabled(state, ShopProgram.CPC, Collections.singletonList(
                ModerationDisabledReason.NO_MORE_ATTEMPTS
        )));
        assertTrue(moderationDisabled(state, ShopProgram.CPA, Collections.singletonList(
                ModerationDisabledReason.NO_MORE_ATTEMPTS
        )));
    }

    private boolean moderationEnabled(ModerationRequestState state, ShopProgram shopProgram) {
        return state.isModerationEnabled()
                && shopProgram == ShopProgram.CPC ? CollectionUtils.isEmpty(state.getCpcModerationDisabledReasons()) :
                CollectionUtils.isEmpty(state.getCpaModerationDisabledReasons())
                        && state.getAttemptsLeft() > 0;
    }

    private boolean moderationDisabled(ModerationRequestState state, ShopProgram shopProgram,
                                       List<ModerationDisabledReason> moderationDisabledReasons) {
        return shopProgram == ShopProgram.CPA ?
                state.getCpaModerationDisabledReasons().containsAll(moderationDisabledReasons) :
                state.getCpcModerationDisabledReasons().containsAll(moderationDisabledReasons);
    }

    private void mockNonOkSandboxState() {
        SandboxState cpaSandboxState = sandboxStateFactory.create(new TestingState()
                .setAttemptNum(3)
                .setPushReadyButtonCount(3)
                .setStatus(TestingStatus.INITED)
                .setTestingType(TestingType.CPA_PREMODERATION)
                .setStartDate(new Date())
        );
        SandboxState cpcSandboxState = sandboxStateFactory.create(new TestingState()
                .setAttemptNum(3)
                .setPushReadyButtonCount(3)
                .setStatus(TestingStatus.INITED)
                .setTestingType(TestingType.CPC_PREMODERATION)
                .setStartDate(new Date())
        );
        lenient().when(sandboxRepository.load(SHOP_ID, ShopProgram.CPA)).thenReturn(cpaSandboxState);
        lenient().when(sandboxRepository.load(SHOP_ID, ShopProgram.CPC)).thenReturn(cpcSandboxState);
        when(sandboxRepository.loadMany(eq(Collections.singletonList(SHOP_ID)))).thenReturn(
                ImmutableMap.of(SHOP_ID, Arrays.asList(cpaSandboxState, cpcSandboxState))
        );
    }

    private void mockOkSandboxState() {
        SandboxState cpaState = sandboxStateFactory.create(SHOP_ID, TestingType.CPA_PREMODERATION);
        cpaState.enableQuickStart();
        SandboxState cpcState = sandboxStateFactory.create(SHOP_ID, TestingType.CPC_PREMODERATION);
        cpcState.enableQuickStart();
        when(sandboxRepository.loadMany(Collections.singletonList(SHOP_ID))).thenReturn(
                ImmutableMap.of(SHOP_ID, Arrays.asList(cpaState, cpcState)));
    }

    private void mockNonOkParams() {
        when(datasourceInformationService.getMissedDatasourceInfo(Collections.singletonList(SHOP_ID))).thenReturn(
                ImmutableMap.of(SHOP_ID,
                        Collections.singletonList(
                                new UniShopInformation("datasource-domain")
                        )
                ));
    }

    private void mockNonFatalCutoffs() {
        when(cutoffService.getCutoffsByDatasources(Collections.singletonList(SHOP_ID))).thenReturn(
                new HashMap<>(ImmutableMap.of(
                        SHOP_ID,
                        ImmutableMap.of(
                                CutoffType.FINANCE, new CutoffInfo(1, CutoffType.FINANCE)
                        )
                ))
        );
    }

    private void mockByPartnerCutoff() {
        FeatureCutoffInfo byPartnerCutoffInfo = new FeatureCutoffInfo.Builder()
                .setDatasourceId(SHOP_ID)
                .setStartDate(new Date())
                .setFeatureType(MARKETPLACE)
                .setFeatureCutoffType(BY_PARTNER)
                .setId(100L)
                .build();

        when(featureService.getCutoff(SHOP_ID, MARKETPLACE, BY_PARTNER)).thenReturn(Optional.of(byPartnerCutoffInfo));
    }

    private void mockFeatureCutoffs(FeatureType featureType, FeatureCustomCutoffType... featureCutoffTypes) {
        List<FeatureCutoffInfo> result = Arrays.stream(featureCutoffTypes)
                .map(t -> new FeatureCutoffInfo.Builder()
                        .setDatasourceId(SHOP_ID)
                        .setStartDate(new Date())
                        .setFeatureType(featureType)
                        .setFeatureCutoffType(t)
                        .setId(100L)
                        .build()
                )
                .collect(Collectors.toUnmodifiableList());

        when(featureService.getCutoff(SHOP_ID, MARKETPLACE, BY_PARTNER)).thenReturn(Optional.empty());

        when(featureService.getCutoffs(SHOP_ID, featureType))
                .thenReturn(result);

        when(featureService.getAllLastCutoffMessages(anyCollection(), anyCollection(), anyCollection()))
                .thenReturn(Map.of(100L, new FeatureCutoffMessage(SHOP_ID, featureType, "Subject", "Body", List.of(
                        new AboScreenshotDto(1L, "hash1"), new AboScreenshotDto(2L, "hash2")))));
    }

    private void mockFatalCutoffs() {
        when(cutoffService.getCutoffsByDatasources(Collections.singletonList(SHOP_ID))).thenReturn(
                new HashMap<>(ImmutableMap.of(
                        SHOP_ID,
                        ImmutableMap.of(
                                CutoffType.FINANCE, new CutoffInfo(1, CutoffType.FINANCE),
                                CutoffType.QMANAGER_CLONE, new CutoffInfo(1, CutoffType.QMANAGER_CLONE)
                        )
                ))
        );
    }

    private void mockType(boolean isDsbs, boolean isCpc) {
        PartnerTypeAwareInfo typeInfo = PartnerTypeAwareInfo.builder()
                .setDropshipBySeller(isDsbs)
                .setCpc(isCpc)
                .setPartnerId(SHOP_ID)
                .build();

        when(partnerTypeAwareService.getPartnerTypeAwareInfo(SHOP_ID)).thenReturn(typeInfo);
        when(partnerTypeAwareService.isMarketSelfDelivery(SHOP_ID)).thenReturn(isDsbs);

        FeatureDescription description = mock(FeatureDescription.class);
        FeaturePrecondition precondition = mock(FeaturePrecondition.class);
        PreconditionResult preconditionResult = PreconditionResult.builder()
                .canEnable(isDsbs)
                .build();
        when(precondition.evaluate(anyLong())).thenReturn(preconditionResult);
        when(description.getPrecondition()).thenReturn(precondition);
        when(featureService.getDescription(MARKETPLACE_SELF_DELIVERY)).thenReturn(description);
    }
}
