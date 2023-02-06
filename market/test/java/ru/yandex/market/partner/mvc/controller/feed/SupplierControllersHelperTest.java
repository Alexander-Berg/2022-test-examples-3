package ru.yandex.market.partner.mvc.controller.feed;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.core.feed.SupplierFeedHelper;
import ru.yandex.market.core.supplier.SupplierState;
import ru.yandex.market.partner.mvc.exception.NotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тест для {@link SupplierControllersHelper} - класса для контроллеров для работы с фидами.
 *
 * @author Zvorygin Andrey don-dron@yandex-team.ru
 */
public class SupplierControllersHelperTest {

    private static final long CAMPAIGN_ID = 1021212;
    private static final long SUPPLIER_ID = 5454445;

    @Mock
    private SupplierFeedHelper supplierFeedHelper;
    private SupplierControllersHelper supplierControllersHelper;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
        supplierControllersHelper = new SupplierControllersHelper(supplierFeedHelper);
    }

    @Test
    @DisplayName("Проверяет получение supplierId по campaignId.")
    void getSupplierIdTest() {
        SupplierState supplierState = Mockito.mock(SupplierState.class);
        Mockito.when(supplierState.getDatasourceId())
                .thenReturn(SUPPLIER_ID);
        Mockito.when(supplierFeedHelper.getSupplier(CAMPAIGN_ID))
                .thenReturn(Optional.of(supplierState));
        assertEquals(supplierControllersHelper.getSupplierId(CAMPAIGN_ID), SUPPLIER_ID);
    }

    @Test
    @DisplayName("Проверяет появление ошибки при отсутствии SupplierState.")
    void getSupplierIdFailTest() {
        Mockito.when(supplierFeedHelper.getSupplier(CAMPAIGN_ID))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> supplierControllersHelper.getSupplierId(CAMPAIGN_ID));
    }
}
