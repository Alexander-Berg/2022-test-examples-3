package ru.yandex.market.partner.mvc.controller.moderation;

import java.util.Arrays;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.core.testing.FullTestingState;
import ru.yandex.market.core.testing.ShopProgram;
import ru.yandex.market.core.testing.TestingService;
import ru.yandex.market.core.testing.TestingState;
import ru.yandex.market.core.testing.TestingType;
import ru.yandex.market.partner.model.PremoderationState;
import ru.yandex.market.partner.mvc.MockPartnerRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Vadim Lyalin
 */
@RunWith(MockitoJUnitRunner.class)
public class PremoderationStateControllerTest {
    private static final long SHOP_ID = 2;
    private final MockPartnerRequest request = new MockPartnerRequest(1, 1, SHOP_ID, 3);

    private PremoderationStateController controller;
    @Mock
    private TestingService testingService;

    @Before
    public void setUp() {
        controller = new PremoderationStateController(testingService);
    }

    /**
     * Тест для магазина, который находится на нескольких проверках
     */
    @Test
    public void getPremoderationState() {
        TestingState cpaPremodState = new TestingState();
        cpaPremodState.setTestingType(TestingType.CPA_PREMODERATION);
        cpaPremodState.setStartDate(new Date());

        TestingState cpcPremodState = new TestingState();
        cpcPremodState.setTestingType(TestingType.CPC_PREMODERATION);
        cpcPremodState.setStartDate(new Date());

        TestingState cpcLiteState = new TestingState();
        cpcLiteState.setTestingType(TestingType.CPC_LITE_CHECK);

        FullTestingState fullTestingState = new FullTestingState(Arrays.asList(
                cpaPremodState, cpcPremodState, cpcLiteState));
        when(testingService.getFullTestingState(SHOP_ID)).thenReturn(fullTestingState);

        PremoderationState premoderationState = controller.getPremoderationState(request);

        assertEquals(premoderationState.getProgramStates().size(), 2);
        // Есть запись для CPC
        assertTrue(premoderationState.getProgramStates().stream()
                .anyMatch(state -> state.getProgram() == ShopProgram.CPA));
        // Есть запись для CPA
        assertTrue(premoderationState.getProgramStates().stream()
                .anyMatch(state -> state.getProgram() == ShopProgram.CPC));
        // Даты заполнены
        assertTrue(premoderationState.getProgramStates().stream().allMatch(state -> state.getStartDate() > 0));

        verify(testingService).getFullTestingState(SHOP_ID);
        verifyNoMoreInteractions(testingService);
    }

    /**
     * Тест для магазина, который находится только на лайтовых проверках
     */
    @Test
    public void getPremoderationStateForLiteModerations() {
        TestingState selfCheckState = new TestingState();
        selfCheckState.setTestingType(TestingType.SELF_CHECK);

        TestingState cpcLiteState = new TestingState();
        cpcLiteState.setTestingType(TestingType.CPC_LITE_CHECK);

        FullTestingState fullTestingState = new FullTestingState(Arrays.asList(
                selfCheckState, cpcLiteState));
        when(testingService.getFullTestingState(SHOP_ID)).thenReturn(fullTestingState);

        PremoderationState premoderationState = controller.getPremoderationState(request);

        assertTrue(premoderationState.getProgramStates().isEmpty());

        verify(testingService).getFullTestingState(SHOP_ID);
        verifyNoMoreInteractions(testingService);
    }
}
