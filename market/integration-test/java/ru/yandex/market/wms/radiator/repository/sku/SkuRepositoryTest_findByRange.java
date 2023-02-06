package ru.yandex.market.wms.radiator.repository.sku;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.radiator.dto.MiniPackDTO;
import ru.yandex.market.wms.radiator.dto.SkuAndMiniPackDTO;
import ru.yandex.market.wms.radiator.dto.SkuMiniPackIdentitiesDto;
import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.WH_1_ID;
import static ru.yandex.market.wms.radiator.test.TestSkuAndPackDTOData.mSkuAutoGetStocksTest;
import static ru.yandex.market.wms.radiator.test.TestSkuAndPackDTOData.mSkuIdentifiedTest;
import static ru.yandex.market.wms.radiator.test.TestSkuAndPackDTOData.mSkuRefMultibox;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/dbStocks/stocks.xml", connection = "wh1Connection"),
})
class SkuRepositoryTest_findByRange extends IntegrationTestBackend {

    @Autowired
    private SkuRepository skuRepository;
    @Autowired
    private Dispatcher dispatcher;


    @Test
    void findByRange_0_2() {
        doFindByRange(WH_1_ID, List.of(mSkuAutoGetStocksTest(), mSkuRefMultibox()), 0, 2);
    }

    @Test
    void findByRange_0_100() {
        doFindByRange(WH_1_ID, List.of(mSkuAutoGetStocksTest(), mSkuRefMultibox(), mSkuIdentifiedTest()), 0, 100);
    }

    @Test
    void findByRange_0_1() {
        doFindByRange(WH_1_ID, List.of(mSkuAutoGetStocksTest()), 0, 1);
    }

    @Test
    void findByRange_1_1() {
        doFindByRange(WH_1_ID, List.of(mSkuRefMultibox()), 1, 1);
    }


    private void doFindByRange(String warehouseId, List<SkuMiniPackIdentitiesDto> expected, int offset, int limit) {
        dispatcher.withWarehouseId(
                warehouseId, () -> {
                    List<SkuMiniPackIdentitiesDto> output = skuRepository.findByRange(offset, limit);

                    assertThat(output.size(), is(equalTo(expected.size())));
                    for (int i = 0; i < output.size(); i++) {
                        verify(output.get(i), expected.get(i));
                    }
                }
        );
    }


    private static void verify(SkuMiniPackIdentitiesDto dto, SkuMiniPackIdentitiesDto expected) {
        assertThat(dto.getPackkey(), is(equalTo(expected.getPackkey())));

        assertThat(dto.getSerialkey(), is(equalTo(expected.getSerialkey())));
        assertThat(dto.getSku(), is(equalTo(expected.getSku())));
        assertThat(dto.getManufacturersku(), is(equalTo(expected.getManufacturersku())));
        assertThat(dto.getStorerkey(), is(equalTo(expected.getStorerkey())));

        assertThat(dto.getStdgrosswgt(), is(equalTo(expected.getStdgrosswgt())));
        assertThat(dto.getStdnetwgt(), is(equalTo(expected.getStdnetwgt())));
        assertThat(dto.getTare(), is(equalTo(expected.getTare())));

        assertThat(dto.getShelflifeindicator(), is(equalTo(expected.getShelflifeindicator())));
        assertThat(dto.getToexpiredays(), is(equalTo(expected.getToexpiredays())));
        assertThat(dto.getDescr(), is(equalTo(expected.getDescr())));
        assertThat(dto.getSusr1(), is(equalTo(expected.getSusr1())));
        assertThat(dto.getSusr4(), is(equalTo(expected.getSusr4())));
        assertThat(dto.getSusr5(), is(equalTo(expected.getSusr5())));
        assertThat(dto.getShelflifeonreceivingPercentage(), is(equalTo(expected.getShelflifeonreceivingPercentage())));
        assertThat(dto.getShelflifePercentage(), is(equalTo(expected.getShelflifePercentage())));
        assertThat(dto.getShelflifeEditDate(), is(equalTo(expected.getShelflifeEditDate())));

        verify(dto.getPack(), expected.getPack());
    }

    private static void verify(MiniPackDTO dto, MiniPackDTO expected) {
        if (expected == null) {
            assertThat(dto, is(equalTo(null)));
            return;
        }
        assertThat(dto.getPackkey(), is(equalTo(expected.getPackkey())));
        assertThat(dto.getHeightuom3(), is(equalTo(expected.getHeightuom3())));
        assertThat(dto.getLengthuom3(), is(equalTo(expected.getLengthuom3())));
        assertThat(dto.getWidthuom3(), is(equalTo(expected.getWidthuom3())));
    }
}
