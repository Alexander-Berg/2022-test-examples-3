package ru.yandex.market.supplier;

import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.datacamp.DefaultDataCampService;
import ru.yandex.market.core.feed.supplier.db.SupplierSummaryDao;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.supplier.summary.SupplierDatacampPriceExecutor;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SupplierDatacampPriceExecutorTest extends FunctionalTest {

    private SupplierDatacampPriceExecutor executor;

    @Autowired
    private SupplierSummaryDao supplierSummaryDao;

    @Mock
    private DefaultDataCampService dataCampService;
    @Autowired
    private ProtocolService protocolService;

    @BeforeEach
    void setUp() {
        executor = new SupplierDatacampPriceExecutor(
                supplierSummaryDao,
                dataCampService,
                protocolService
        );
        doReturn(true).when(dataCampService)
                .hasOfferWithPriceInDatacamp(eq(5L));
        doReturn(true).when(dataCampService)
                .hasOfferWithPriceInDatacamp(eq(7L));
        doReturn(false).when(dataCampService)
                .hasOfferWithPriceInDatacamp(eq(9L));

    }

    @Test
    @DbUnitDataSet(
            before = "SupplierDatacampPriceExecutorTest.before.csv",
            after = "SupplierDatacampPriceExecutorTest.after.csv"
    )
    void doJob() {
        executor.doJob(null);

        verify(dataCampService, times(1))
                .updateDatacampPriceFlags(anyLong(), eq(List.of(7L)));
    }
}
