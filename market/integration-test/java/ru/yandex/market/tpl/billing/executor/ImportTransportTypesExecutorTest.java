package ru.yandex.market.tpl.billing.executor;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.repository.TransportTypeRepository;
import ru.yandex.market.tpl.billing.task.executor.ImportTransportTypesExecutor;
import ru.yandex.market.tpl.client.billing.BillingClient;
import ru.yandex.market.tpl.client.billing.dto.BillingTransportTypeContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingTransportTypeDto;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

/**
 * Тесты для {@link ru.yandex.market.tpl.billing.task.executor.ImportTransportTypesExecutor}
 */
public class ImportTransportTypesExecutorTest extends AbstractFunctionalTest {
    @Autowired
    private BillingClient billingClient;

    @Autowired
    private TransportTypeRepository transportTypeRepository;

    @Test
    @DbUnitDataSet(
            before = "/database/executor/importTransportTypes/before/importTransportTypes.csv",
            after = "/database/executor/importTransportTypes/after/importTransportTypes.csv")
    void testImportTransportTypes() {
        doAnswer(invocation -> new BillingTransportTypeContainerDto(List.of(
                new BillingTransportTypeDto(1L, "first", false),
                new BillingTransportTypeDto(2L, "secondChanged", true),
                new BillingTransportTypeDto(3L, "thirdNew", true)
        ))).when(billingClient).getAllTransportTypes();

        ImportTransportTypesExecutor executor = new ImportTransportTypesExecutor(
                transportTypeRepository,
                billingClient
        );

        executor.doJob();

        verify(billingClient).getAllTransportTypes();
    }
}
