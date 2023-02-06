package ru.yandex.market.core.moderation.request;

import java.util.Date;
import java.util.EnumSet;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.market.core.cutoff.CutoffService;
import ru.yandex.market.core.moderation.ModerationDisabledReason;
import ru.yandex.market.core.moderation.ModerationService;
import ru.yandex.market.core.moderation.sandbox.SandboxRepository;
import ru.yandex.market.core.moderation.sandbox.SandboxState;
import ru.yandex.market.core.moderation.sandbox.impl.DefaultSandboxStateFactory;
import ru.yandex.market.core.replication.ReplicationModerationHelper;
import ru.yandex.market.core.shop.ShopActionContext;
import ru.yandex.market.core.testing.CpaCheckService;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeast;

/**
 * @author stani
 */
public class RequestModerationTest {

    private static final long ACTION_ID = 3L;
    private static final long CPA_CHECK_SHOP_ID = 1L;
    private static final long CPC_PREMODERATION_SHOP_ID = 2L;

    private ModerationRequestEntryPoint moderationRequestEntryPoint;
    private DefaultSandboxStateFactory sandboxStateFactory = new DefaultSandboxStateFactory(Date::new);
    private SandboxRepository sandboxRepository;

    @Before
    public void setUp() {
        sandboxRepository = mock(SandboxRepository.class);
        when(sandboxRepository.load(CPA_CHECK_SHOP_ID, ShopProgram.CPA))
                .thenReturn(createState(CPA_CHECK_SHOP_ID, TestingType.CPA_CHECK));
        when(sandboxRepository.load(CPC_PREMODERATION_SHOP_ID, ShopProgram.CPC)).
                thenReturn(createState(CPC_PREMODERATION_SHOP_ID, TestingType.CPC_PREMODERATION));

        ModerationService moderationService = mock(ModerationService.class);
        when(moderationService.getModerationRequestState(Mockito.anyLong()))
                .thenReturn(new ModerationRequestState(
                        ImmutableMap.of(ShopProgram.GENERAL, EnumSet.of(ModerationDisabledReason.MODERATION_NOT_NEEDED)),
                        6));
        moderationRequestEntryPoint = new ModerationRequestEntryPoint(
                sandboxRepository, sandboxStateFactory,
                mock(CpaCheckService.class), mock(CutoffService.class),
                moderationService, mock(ApplicationEventPublisher.class), mock(ReplicationModerationHelper.class));
    }

    /**
     * Тест на запуск премодерации магазина, который провалил предыдущую попытку CPC пермодерации
     * В результате заявка должна перейти в статус READY_FOR_CHECK,
     * Количество попыток нажатия кнопки увеличиться на единицу.
     */
    @Test
    public void testRequestModerationWithCpcPremTestingType() {
        TestingState previousTestingState =
                sandboxStateFactory.getState(sandboxRepository.load(CPC_PREMODERATION_SHOP_ID, ShopProgram.CPC));
        moderationRequestEntryPoint.requestCPCModeration(new ShopActionContext(ACTION_ID, CPC_PREMODERATION_SHOP_ID));
        SandboxState state = verifyLastSandboxState();
        Assert.assertEquals(state.getTestingType(), TestingType.CPC_PREMODERATION);
        TestingState testingState = sandboxStateFactory.getState(state);
        Assert.assertTrue(testingState.getAttemptNum() - previousTestingState.getAttemptNum() == 1);
    }

    private SandboxState createState(long shopId, TestingType testingType) {
        SandboxState state = sandboxStateFactory.create(shopId, testingType);
        state.enableQuickStart();
        return state;
    }

    private SandboxState verifyLastSandboxState() {
        ArgumentCaptor<SandboxState> argumentCaptor = ArgumentCaptor.forClass(SandboxState.class);
        verify(sandboxRepository, atLeast(1)).store(any(ShopActionContext.class), argumentCaptor.capture());
        return argumentCaptor.getValue();
    }
}
