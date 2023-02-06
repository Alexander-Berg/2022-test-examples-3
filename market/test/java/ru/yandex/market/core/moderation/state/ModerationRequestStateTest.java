package ru.yandex.market.core.moderation.state;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Test;

import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.ds.info.ShopInformation;
import ru.yandex.market.core.ds.info.UniShopInformation;
import ru.yandex.market.core.feature.model.FeatureCutoffType;
import ru.yandex.market.core.moderation.ModerationDisabledReason;
import ru.yandex.market.core.moderation.ShopModerationContext;
import ru.yandex.market.core.moderation.request.ModerationRequestState;
import ru.yandex.market.core.moderation.sandbox.SandboxState;
import ru.yandex.market.core.moderation.sandbox.SandboxStateFactory;
import ru.yandex.market.core.moderation.sandbox.impl.DefaultSandboxStateFactory;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.testing.TestingType;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Тесты на логику вычисления возможности магазина отправиться на модерацию
 * {@link ShopModerationContext#resolveModerationState()}
 *
 * @author fbokovikov
 */
public class ModerationRequestStateTest {

    private static final long SHOP_ID = 774L;

    private static final List<CutoffType> NON_FATAL_CUTOFFS = Arrays.asList(
            CutoffType.FINANCE
    );

    private static final List<CutoffType> CPC_PARTNER_CUTOFFS_LIST = Arrays.asList(
            CutoffType.FINANCE,
            CutoffType.CPC_PARTNER
    );

    private static final List<CutoffType> FATAL_GENERAL_CUTOFF_LIST = Arrays.asList(
            CutoffType.FINANCE,
            CutoffType.QMANAGER_CLONE
    );

    private static final List<ShopInformation> OK_PARAMS = new ArrayList<>();

    private static final List<ShopInformation> NOT_OK_CPC_PARAM_LIST =
            Collections.singletonList(new UniShopInformation("datasource-domain"));

    private SandboxStateFactory sandboxStateFactory = new DefaultSandboxStateFactory(Date::new);


    /**
     * "Позитивный" сценарий - модерация возможна как по CPA, так и по CPC
     */
    @Test
    public void testModerationEnabledBothTypes() {
        ShopModerationContext shopModerationContext = new ShopModerationContext(
                OK_PARAMS,
                NON_FATAL_CUTOFFS,
                new HashSet<>(),
                ImmutableMap.of(
                        ShopProgram.CPA, createSandboxState(TestingType.CPA_PREMODERATION),
                        ShopProgram.CPC, createSandboxState(TestingType.CPC_PREMODERATION)),
                Set.of(ShopProgram.CPC, ShopProgram.CPA)
        );
        ModerationRequestState moderationRequestState = shopModerationContext.resolveModerationState();
        assertTrue(moderationRequestState.isModerationEnabled());
        assertTrue(moderationRequestState.getTestingTypes().containsAll(Arrays.asList(ShopProgram.CPC,
                ShopProgram.CPA)));
        assertTrue(moderationRequestState.getAttemptsLeft() == 6);
        assertTrue(CollectionUtils.isEmpty(moderationRequestState.getCpaModerationDisabledReasons()));
        assertTrue(CollectionUtils.isEmpty(moderationRequestState.getCpcModerationDisabledReasons()));
    }

    private SandboxState createSandboxState(TestingType testingType) {
        SandboxState sandboxState = sandboxStateFactory.create(SHOP_ID, testingType);
        sandboxState.enableQuickStart();
        return sandboxState;
    }

    /**
     * CPA-модерация доступна, CPC-модерация не доступна, потому что не взведен тумблер CPC
     */
    @Test
    public void testModerationCpaEnabledCpcDisabledTumbler() {
        ShopModerationContext shopModerationContext = new ShopModerationContext(
                OK_PARAMS,
                CPC_PARTNER_CUTOFFS_LIST,
                new HashSet<>(),
                ImmutableMap.of(
                        ShopProgram.CPA, createSandboxState(TestingType.CPA_PREMODERATION)),
                Set.of(ShopProgram.CPA)
        );
        ModerationRequestState moderationRequestState = shopModerationContext.resolveModerationState();
        assertTrue(moderationRequestState.isModerationEnabled());
        assertTrue(moderationRequestState.getTestingTypes().contains(ShopProgram.CPA));
        assertTrue(moderationRequestState.getAttemptsLeft() == 6);
        assertTrue(CollectionUtils.isEmpty(moderationRequestState.getCpaModerationDisabledReasons()));
        assertFalse(CollectionUtils.isEmpty(moderationRequestState.getCpcModerationDisabledReasons()));
        assertTrue(moderationRequestState.getCpcModerationDisabledReasons().containsAll(Arrays.asList(
                ModerationDisabledReason.PROGRAM_IS_NOT_SELECTED,
                ModerationDisabledReason.MODERATION_NOT_NEEDED
                )
        ));
        assertFalse(
                moderationRequestState.getCpcModerationDisabledReasons()
                        .contains(ModerationDisabledReason.FATAL_CUTOFFS));
    }

    /**
     * CPA-модерация доступна, CPC-модерация не доступна по причине незаполненного обязательного параметра, а также
     * наличия фатального катоффа
     */
    @Test
    public void testModerationCpaEnabledCpcDisabledParamAndCutoff() {
        ShopModerationContext shopModerationContext = new ShopModerationContext(
                NOT_OK_CPC_PARAM_LIST,
                FATAL_GENERAL_CUTOFF_LIST,
                new HashSet<>(),
                ImmutableMap.of(
                        ShopProgram.CPA, createSandboxState(TestingType.CPA_PREMODERATION)),
                Set.of(ShopProgram.CPA, ShopProgram.CPC)
        );
        ModerationRequestState moderationRequestState = shopModerationContext.resolveModerationState();
        List<ModerationDisabledReason> cpcDisabledReasons = moderationRequestState.getCpcModerationDisabledReasons();
        List<ModerationDisabledReason> cpaDisabledReasons = moderationRequestState.getCpaModerationDisabledReasons();
        assertTrue(moderationRequestState.isModerationEnabled());
        assertFalse(moderationRequestState.getTestingTypes().isEmpty());
        assertTrue(moderationRequestState.getAttemptsLeft() == 6);
        assertTrue(CollectionUtils.isEmpty(cpaDisabledReasons));
        assertFalse(CollectionUtils.isEmpty(cpcDisabledReasons));
        assertTrue(cpcDisabledReasons.contains(ModerationDisabledReason.MISSED_DATASOURCE_PARAMS));
    }

    /**
     * CPA-модерация доступна, CPC-модерация не доступна по причине незаполненного обязательного параметра, а также
     * наличия фатального катоффа
     */
    @Test
    public void testModerationDisabledAttemptsLeft() {
        ShopModerationContext shopModerationContext = new ShopModerationContext(
                OK_PARAMS,
                NON_FATAL_CUTOFFS,
                new HashSet<>(),
                ImmutableMap.of(
                        ShopProgram.CPC, createSandboxState(2, TestingType.CPC_PREMODERATION),
                        ShopProgram.CPA, createSandboxState(4, TestingType.CPA_PREMODERATION)),
                Set.of(ShopProgram.CPA, ShopProgram.CPC)
        );
        ModerationRequestState moderationRequestState = shopModerationContext.resolveModerationState();
        List<ModerationDisabledReason> cpaDisabledReasons = moderationRequestState.getCpaModerationDisabledReasons();
        List<ModerationDisabledReason> cpcDisabledReasons = moderationRequestState.getCpcModerationDisabledReasons();
        assertTrue(!moderationRequestState.isModerationEnabled()
                && CollectionUtils.isEmpty(moderationRequestState.getTestingTypes())
                && moderationRequestState.getAttemptsLeft() <= 0
                && !CollectionUtils.isEmpty(cpaDisabledReasons)
                && !CollectionUtils.isEmpty(moderationRequestState.getCpcModerationDisabledReasons())
                && cpaDisabledReasons.contains(ModerationDisabledReason.NO_MORE_ATTEMPTS)
                && cpcDisabledReasons.contains(ModerationDisabledReason.NO_MORE_ATTEMPTS)
        );
    }

    @Test
    public void testTechnicalYmlCase() {
        ShopModerationContext shopModerationContext = new ShopModerationContext(
                OK_PARAMS,
                Arrays.asList(CutoffType.TECHNICAL_YML),
                new HashSet<>(),
                ImmutableMap.of(ShopProgram.CPA, createSandboxState(TestingType.CPA_PREMODERATION)),
                Set.of(ShopProgram.CPA, ShopProgram.CPC)
        );
        ModerationRequestState moderationRequestState = shopModerationContext.resolveModerationState();
        assertFalse(moderationRequestState.isModerationEnabled());
        List<ModerationDisabledReason> cpaDisabledReasons = moderationRequestState.getCpaModerationDisabledReasons();
        List<ModerationDisabledReason> cpcDisabledReasons = moderationRequestState.getCpcModerationDisabledReasons();
        assertThat(cpaDisabledReasons, hasItem(ModerationDisabledReason.FEED_ERRORS));
        assertThat(cpcDisabledReasons, containsInAnyOrder(ModerationDisabledReason.FEED_ERRORS,
                ModerationDisabledReason.MODERATION_NOT_NEEDED));
    }

    @Test
    public void testModerationDisabledCpcLiteCheckInProgress() {
        SandboxState requestedSandboxState = createSandboxState(TestingType.CPC_LITE_CHECK);
        requestedSandboxState.requestModeration(TestingType.CPC_LITE_CHECK);
        requestedSandboxState.startTesting();

        ShopModerationContext shopModerationContext = new ShopModerationContext(
                OK_PARAMS,
                Arrays.asList(CutoffType.COMMON_OTHER, CutoffType.CPA_GENERAL),
                new HashSet<>(),
                ImmutableMap.of(ShopProgram.GENERAL, requestedSandboxState),
                Set.of(ShopProgram.CPC, ShopProgram.CPA)
        );

        ModerationRequestState moderationRequestState = shopModerationContext.resolveModerationState();
        List<ModerationDisabledReason> cpaDisabledReasons = moderationRequestState.getCpaModerationDisabledReasons();
        List<ModerationDisabledReason> cpcDisabledReasons = moderationRequestState.getCpcModerationDisabledReasons();

        assertFalse(moderationRequestState.isModerationEnabled());
        assertThat(cpaDisabledReasons, hasItem(ModerationDisabledReason.MODERATION_IN_PROGRESS));
        assertThat(cpcDisabledReasons, hasItem(ModerationDisabledReason.MODERATION_IN_PROGRESS));
    }

    @Test
    public void testPartnerCutoffOnCpa() {
        ShopModerationContext shopModerationContext = new ShopModerationContext(
                OK_PARAMS,
                new HashSet<>(),
                Set.of(FeatureCutoffType.PRECONDITION),
                ImmutableMap.of(ShopProgram.CPA, createSandboxState(TestingType.CPA_PREMODERATION)),
                Set.of(ShopProgram.CPA)
        );
        ModerationRequestState moderationRequestState = shopModerationContext.resolveModerationState();
        assertFalse(moderationRequestState.isModerationEnabled());
        List<ModerationDisabledReason> cpaDisabledReasons = moderationRequestState.getCpaModerationDisabledReasons();
        List<ModerationDisabledReason> cpcDisabledReasons = moderationRequestState.getCpcModerationDisabledReasons();
        assertThat(cpaDisabledReasons, hasItem(ModerationDisabledReason.FATAL_CUTOFFS));
        assertThat(cpcDisabledReasons, hasItem(ModerationDisabledReason.PROGRAM_IS_NOT_SELECTED));
    }

    private SandboxState createSandboxState(int attemptsNum, TestingType testingType) {
        return sandboxStateFactory.create(new TestingState()
                .setDatasourceId(SHOP_ID)
                .setStartDate(new Date())
                .setPushReadyButtonCount(0)
                .setTestingType(testingType)
                .setStatus(TestingStatus.INITED)
                .setCancelled(true)
                .setApproved(true)
                .setUpdatedAt(new Date())
                .setIterationNum(1)
                .setRecommendations("")
                .setAttemptNum(attemptsNum)
                .setPushReadyButtonCount(attemptsNum)
        );
    }

}
