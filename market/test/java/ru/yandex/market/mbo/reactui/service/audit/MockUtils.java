package ru.yandex.market.mbo.reactui.service.audit;

import org.apache.commons.collections4.CollectionUtils;
import org.mockito.Mockito;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * @author dergachevfv
 * @since 1/13/20
 */
public class MockUtils {

    private MockUtils() {
    }

    public static BillingPricesRegistry mockBillingPricesRegistry() {
        BillingPricesRegistry errorsPricesRegistry = Mockito.mock(BillingPricesRegistry.class);
        Mockito.when(errorsPricesRegistry
            .popPrice(Mockito.anyLong(), Mockito.<List<AuditAction>>argThat(CollectionUtils::isNotEmpty)))
            .thenReturn(Optional.of(BigDecimal.ONE));
        Mockito.when(errorsPricesRegistry
            .popPrice(Mockito.anyLong(), Mockito.anyString()))
            .thenReturn(Optional.of(BigDecimal.ONE));

        BillingPricesRegistry pricesRegistry = Mockito.mock(BillingPricesRegistry.class);
        Mockito.when(pricesRegistry
            .popPrice(Mockito.anyLong(), Mockito.<List<AuditAction>>argThat(CollectionUtils::isNotEmpty)))
            .thenReturn(Optional.of(BigDecimal.ONE));
        Mockito.when(pricesRegistry
            .popPrice(Mockito.anyLong(), Mockito.anyList(), Mockito.anyList()))
            .thenAnswer(invocation -> {
                List<AuditAction> operatorActions = invocation.getArgument(1);
                List<AuditAction> inspectorActions = invocation.getArgument(2);
                if (CollectionUtils.isNotEmpty(operatorActions) || CollectionUtils.isNotEmpty(inspectorActions)) {
                    return Optional.of(BigDecimal.ONE);
                } else {
                    return Optional.empty();
                }
            });
        Mockito.when(pricesRegistry
            .popPrice(Mockito.anyLong(), Mockito.anyString()))
            .thenReturn(Optional.of(BigDecimal.ONE));
        Mockito.when(pricesRegistry.forOperatorErrors())
            .thenReturn(errorsPricesRegistry);

        return pricesRegistry;
    }
}
