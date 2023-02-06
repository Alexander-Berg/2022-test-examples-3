package ru.yandex.market.wms.radiator.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.radiator.dto.SkuAndMiniPackDTO;
import ru.yandex.market.wms.radiator.dto.SkuMiniPackIdentitiesDto;
import ru.yandex.market.wms.radiator.dto.VInventoryMasterDTO;
import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;
import ru.yandex.market.wms.radiator.test.TestSkuAndPackDTOData;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.WH_1_ID;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/dbStocks/stocks.xml", connection = "wh1Connection"),
})
class StocksRepositoryTest_fillWithAllStocksFromDbAndReturnOldest extends IntegrationTestBackend {

    @Autowired
    private StocksRepository repository;
    @Autowired
    private Dispatcher dispatcher;


    @Test
    void fillWithAllStocksFromDbAndReturnOldest__limit_0() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    List<VInventoryMasterDTO> result = new ArrayList<>();

                    Iterable<VInventoryMasterDTO> oldest = repository.fillWithAllStocksFromDbAndReturnOldest(
                            result, null, 0);
                    List<VInventoryMasterDTO> oldestList = new ArrayList<>();
                    oldest.forEach(oldestList::add);
                    assertThat(oldestList.size(), is(equalTo(0)));

                    assertThat(result.size(), is(equalTo(3)));

                    assertIsSingleSku(result.get(0));
                    assertIsMultiSku(result.get(2));
                    assertIsIdentifiedSku(result.get(1));
                }
        );
    }


    @Test
    void fillWithAllStocksFromDbAndReturnOldest__limit_1() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    List<VInventoryMasterDTO> result = new ArrayList<>();

                    Iterable<VInventoryMasterDTO> oldest = repository.fillWithAllStocksFromDbAndReturnOldest(
                            result, LocalDateTime.MIN, 1);
                    List<VInventoryMasterDTO> oldestList = new ArrayList<>();
                    oldest.forEach(oldestList::add);
                    assertThat(oldestList.size(), is(equalTo(1)));
                    assertIsSingleSku(oldestList.get(0));

                    assertThat(result.size(), is(equalTo(3)));
                    assertIsSingleSku(result.get(0));
                    assertIsMultiSku(result.get(2));
                    assertIsIdentifiedSku(result.get(1));
                }
        );
    }

    private void assertIsIdentifiedSku(VInventoryMasterDTO identified) {
        SkuMiniPackIdentitiesDto expectedIdentified = TestSkuAndPackDTOData.mSkuIdentifiedTest();
        assertThat(identified.storerkey, is(equalTo(expectedIdentified.getStorerkey())));
        assertThat(identified.sku, is(equalTo(expectedIdentified.getSku())));
        assertThat(identified.manufacturersku, is(equalTo(expectedIdentified.getManufacturersku())));
    }

    private void assertIsSingleSku(VInventoryMasterDTO single) {
        SkuMiniPackIdentitiesDto expectedSingle = TestSkuAndPackDTOData.mSkuAutoGetStocksTest();
        assertThat(single.storerkey, is(equalTo(expectedSingle.getStorerkey())));
        assertThat(single.sku, is(equalTo(expectedSingle.getSku())));
        assertThat(single.manufacturersku, is(equalTo(expectedSingle.getManufacturersku())));
    }

    private void assertIsMultiSku(VInventoryMasterDTO multi) {
        var expectedMulti = TestSkuAndPackDTOData.mSkuRefMultibox();
        assertThat(multi.storerkey, is(equalTo(expectedMulti.getStorerkey())));
        assertThat(multi.sku, is(equalTo(expectedMulti.getSku())));
        assertThat(multi.manufacturersku, is(equalTo(expectedMulti.getManufacturersku())));
        assertThat(multi.qty, is(equalTo(BigDecimal.valueOf(40))));             // ok?
        assertThat(multi.qtyonhold, is(equalTo(BigDecimal.valueOf(40))));       // ok?
        assertThat(multi.qtydefect, is(equalTo(BigDecimal.valueOf(20))));       // ok?
        assertThat(multi.qtysurplus, is(equalTo(BigDecimal.valueOf(20))));      // ok?
    }
}
