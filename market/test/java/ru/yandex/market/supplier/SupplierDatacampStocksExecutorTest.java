package ru.yandex.market.supplier;

import java.util.List;

import Market.DataCamp.DataCampUnitedOffer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.supplier.summary.SupplierDatacampStocksExecutor;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;

/**
 * Функциональный тест на {@link SupplierDatacampStocksExecutor}
 */
class SupplierDatacampStocksExecutorTest extends FunctionalTest {

    @Autowired
    private SupplierDatacampStocksExecutor supplierDatacampStocksExecutor;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Test
    @DbUnitDataSet(
            before = "SupplierDatacampStocksExecutorTest.before.csv",
            after = "SupplierDatacampStocksExecutorTest.after.csv"
    )
    void test() {
        doReturn(
                SearchBusinessOffersResult.builder()
                        .setOffers(List.of(DataCampUnitedOffer.UnitedOffer.newBuilder().build()))
                        .build()
        ).when(dataCampShopClient).searchBusinessOffers(argThat(req -> req.getPartnerId().equals(7L)));
        doReturn(
                SearchBusinessOffersResult.builder()
                        .setOffers(List.of())
                        .build()
        ).when(dataCampShopClient).searchBusinessOffers(argThat(req -> req.getPartnerId().equals(9L)));

        supplierDatacampStocksExecutor.doJob(null);
    }
}
