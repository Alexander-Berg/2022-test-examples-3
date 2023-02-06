package ru.yandex.market.core.cutoff.listener;

import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.core.cutoff.CutoffService;
import ru.yandex.market.core.cutoff.model.CutoffInfo;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.moderation.sandbox.SandboxRepository;
import ru.yandex.market.core.moderation.sandbox.impl.DefaultSandboxStateFactory;
import ru.yandex.market.core.protocol.MockProtocolService;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by lazalex on 27.09.17.
 */
public class ModerationCutoffListenerTest {

    private ModerationCutoffListener moderationCutoffListener;

    @Mock
    private CutoffService cutoffService;

    @Mock
    private SandboxRepository sandboxRepository;

    private DefaultSandboxStateFactory sandboxStateFactory;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        sandboxStateFactory = Mockito.spy(new DefaultSandboxStateFactory(Date::new));
        moderationCutoffListener = new ModerationCutoffListener(
                ShopProgram.CPC, cutoffService, sandboxRepository,
                sandboxStateFactory, new MockProtocolService());
    }

    @Test
    public void forModerationRequiredCutoffTypeShouldCreateSandboxStateWithLinkedCutoff() {
        CutoffInfo cutoffInfo = new CutoffInfo(777, 774, CutoffType.QMANAGER_OTHER, new Date(), null);

        when(cutoffService.getCutoffs(anyLong(), eq(CutoffType.ALL_CUTOFFS))).thenReturn(Collections.emptyMap());
        when(sandboxRepository.load(anyLong(), any())).thenReturn(null);
        when(sandboxStateFactory.create(anyLong(), any(), anyLong())).thenCallRealMethod();

        moderationCutoffListener.onOpen(cutoffInfo, 1, false);

        verify(sandboxStateFactory).create(eq(cutoffInfo.getDatasourceId()), eq(TestingType.CPC_LITE_CHECK), eq(cutoffInfo.getId()));
        verify(sandboxRepository, times(1)).store(any(), any());
    }

}
