package ru.yandex.market.wms.radiator.repository.sku;

import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.radiator.dto.MiniPackDTO;
import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.WH_1_ID;
import static ru.yandex.market.wms.radiator.test.TestData.PACK101;
import static ru.yandex.market.wms.radiator.test.TestData.PACK102;
import static ru.yandex.market.wms.radiator.test.TestData.PACK103;
import static ru.yandex.market.wms.radiator.test.TestData.PACK104;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/dbStocks/pack-1.xml", connection = "wh1Connection"),
        @DatabaseSetup(value = "/fixtures/dbStocks/sku-1.xml", connection = "wh1Connection"),
        @DatabaseSetup(value = "/fixtures/dbStocks/pack-1.xml", connection = "wh2Connection"),
        @DatabaseSetup(value = "/fixtures/dbStocks/sku-1.xml", connection = "wh2Connection"),
})
class PackRepositoryTest extends IntegrationTestBackend {

    @Autowired
    private PackRepository repository;
    @Autowired
    private Dispatcher dispatcher;


    @Test
    void findAll() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    var expected = List.of(pack(PACK101), pack(PACK102), pack(PACK103), pack(PACK104));
                    var allPacks = repository.findAllPacks();
                    assertThat(
                            allPacks,
                            is(equalTo(expected))
                    );
                }
        );
    }

    @Test
    void findByKeys() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    assertThat(
                            repository.findMiniByKeys(List.of(PACK101, PACK102)),
                            is(equalTo(Map.of(PACK101, pack(PACK101), PACK102, pack(PACK102))))
                    );
                }
        );
    }

    private static MiniPackDTO pack(String packKey) {
        MiniPackDTO p = new MiniPackDTO();
        p.setPackkey(packKey);
        p.setHeightuom3(0d);
        p.setLengthuom3(0d);
        p.setWidthuom3(0d);
        return p;
    }
}
