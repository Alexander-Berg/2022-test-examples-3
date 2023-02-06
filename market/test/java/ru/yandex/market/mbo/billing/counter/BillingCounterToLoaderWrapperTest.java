package ru.yandex.market.mbo.billing.counter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.billing.BillingProvider;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.action.BillingAction;
import ru.yandex.market.mbo.category.mappings.CategoryMappingServiceMock;
import ru.yandex.market.mbo.db.params.guru.GuruVendorsReader;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicnumber")
public class BillingCounterToLoaderWrapperTest extends AbstractBillingLoaderTest {
    private BillingCounterToLoaderWrapper wrapper;

    @Mock
    private BillingLoader loader;

    private CategoryMappingServiceMock categoryMappingService;

    @Mock
    private GuruVendorsReader vendorsReader;

    @Before
    public void setUp() {
        super.setUp();
        categoryMappingService = new CategoryMappingServiceMock();
        wrapper = new BillingCounterToLoaderWrapper(Collections.singletonList(loader));
        wrapper.setCategoryMappingService(categoryMappingService);
        wrapper.setVendorsReader(vendorsReader);
        wrapper.setBillingOperations(billingOperations);
    }

    @Test
    public void noGuruCategory() {
        when(loader.loadBillingActions(any(BillingProvider.class))).thenReturn(
            Collections.singletonList(
                new BillingAction(1L, PaidAction.FILL_MODEL_PARAMETER, ACTIONS_DATE.getTime(), 1L,
                        1L, null, -1L)));

        wrapper.doLoad(INTERVAL, tarifProvider);

        verify(operationsUpdater, times(1)).add(batchUpdateCaptor.capture());
        List<BatchUpdateData> billed = batchUpdateCaptor.getAllValues();

        assertThat(billed).containsExactlyInAnyOrder(
            createBilledAction(PaidAction.FILL_MODEL_PARAMETER, 1L, 0L)
        );
    }
}
