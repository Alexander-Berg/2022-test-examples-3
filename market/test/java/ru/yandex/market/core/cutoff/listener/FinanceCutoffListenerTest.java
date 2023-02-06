package ru.yandex.market.core.cutoff.listener;

import java.util.Calendar;
import java.util.Date;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.core.cutoff.model.CutoffInfo;
import ru.yandex.market.core.cutoff.model.CutoffType;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;

/**
 * Тесты для {@link FinanceCutoffListener}.
 *
 * @author Kirill Lakhtin (klaktin@yandex-team.ru)
 */
@ExtendWith(MockitoExtension.class)
class FinanceCutoffListenerTest {

    private FinanceCutoffListener financeCutoffListener;

    @Mock
    private ParamService paramService;

    @BeforeEach
    void setUp() {
        financeCutoffListener = new FinanceCutoffListener();
        financeCutoffListener.setParamService(paramService);
    }

    @Test
    @DisplayName("При закрытии катофа будет снят параметр NEVER_PAID")
    void onClose() {
        Calendar instance = Calendar.getInstance();
        Date time = instance.getTime();
        CutoffInfo cutoffInfo = new CutoffInfo(1L, 2L, CutoffType.FINANCE, time, time);
        BooleanParamValue paramValue = new BooleanParamValue(ParamType.NEVER_PAID, 2L, true);
        Mockito.when(paramService.getParam(ParamType.NEVER_PAID, 2L)).thenReturn(paramValue);

        financeCutoffListener.onClose(cutoffInfo, 3L, false);

        Mockito.verify(paramService).deleteParam(3L, paramValue);
    }

    /**
     * Тест проверяет, что параметр NEVER_PAID будет снят, если у магазина есть платежи в момент выполнения onClose.
     */
    @Test
    void onCloseCheckHasPayments() {
        Calendar instance = Calendar.getInstance();
        Date time = instance.getTime();
        CutoffInfo cutoffInfo = new CutoffInfo(1L, 2L, CutoffType.FINANCE, time, time);
        BooleanParamValue paramValue = new BooleanParamValue(ParamType.NEVER_PAID, 2L, true);
        Mockito.when(paramService.getParam(ParamType.NEVER_PAID, 2L)).thenReturn(paramValue);

        financeCutoffListener.onClose(cutoffInfo, 3L, false);

        Mockito.verify(paramService).deleteParam(3L, paramValue);
    }
}
