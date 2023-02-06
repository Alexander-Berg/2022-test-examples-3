package ru.yandex.market.wms.radiator.repository;

import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.WH_1_ID;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/dbStocks/sku-7.xml", connection = "wh1Connection"),
        @DatabaseSetup(value = "/fixtures/dbStocks/sku-7.xml", connection = "wh2Connection"),
})
class StocksRepositoryTest_BOM extends IntegrationTestBackend {

    @Autowired
    private StocksRepository repository;
    @Autowired
    private Dispatcher dispatcher;

    private final String onlyOneBOMExists = "ROV0000000000002983412";
    private final String oneOfTwoBOMInLost = "ROV0000000000002983413";
    private final String onlyOneBOMExistsInLost = "ROV0000000000002983414";


    @Test
    void getAllStocksFromDb_justSqlIsCorrect() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    var result = repository.getAllStocksFromDb();
                    assertThat(
                            result.size(),
                            is(equalTo(7))
                    );
                }
        );
    }

    //На стоке только 1 годный BOM из 2-х, отдаем как DEFECT
    @Test
    void getAllStocksFromDb_defectBOM_onlyOneExists() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    var result = repository.getAllStocksFromDb();
                    var onlyDefectBOM = result.stream()
                            .filter(dto -> onlyOneBOMExists.equals(dto.sku))
                            .collect(Collectors.toList());
                    assertThat(onlyDefectBOM.size(), is(equalTo(1)));
                    assertThat(onlyDefectBOM.get(0).qtydefect, is(equalTo(BigDecimal.ONE)));
                    assertThat(onlyDefectBOM.get(0).qtyonhold, is(equalTo(BigDecimal.ONE)));
                    assertThat(onlyDefectBOM.get(0).qtyquarantine, is(equalTo(BigDecimal.ZERO)));
                }
        );
    }

    //На стоке только 2 BOM из 2-х: один годный, второй в ЛОСТ, отдаем как QUARANTINE
    @Test
    void getAllStocksFromDb_quarantineBOM_oneOfTwoInLost() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    var result = repository.getAllStocksFromDb();
                    var onlyQuarantineBOM = result.stream()
                            .filter(dto -> oneOfTwoBOMInLost.equals(dto.sku))
                            .collect(Collectors.toList());
                    assertThat(onlyQuarantineBOM.size(), is(equalTo(1)));
                    assertThat(onlyQuarantineBOM.get(0).qtydefect, is(equalTo(BigDecimal.ZERO)));
                    assertThat(onlyQuarantineBOM.get(0).qtyonhold, is(equalTo(BigDecimal.ONE)));
                    assertThat(onlyQuarantineBOM.get(0).qtyquarantine, is(equalTo(BigDecimal.ONE)));
                }
        );
    }

    //На стоке только 1 BOM из 3-х и он в ЛОСТ, отдаем как QUARANTINE
    @Test
    void getAllStocksFromDb_quarantineBOM_onlyOneExistsInLost() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    var result = repository.getAllStocksFromDb();
                    var onlyQuarantineBOM = result.stream()
                            .filter(dto -> onlyOneBOMExistsInLost.equals(dto.sku))
                            .collect(Collectors.toList());
                    assertThat(onlyQuarantineBOM.size(), is(equalTo(1)));
                    assertThat(onlyQuarantineBOM.get(0).qtydefect, is(equalTo(BigDecimal.ZERO)));
                    assertThat(onlyQuarantineBOM.get(0).qtyonhold, is(equalTo(BigDecimal.ONE)));
                    assertThat(onlyQuarantineBOM.get(0).qtyquarantine, is(equalTo(BigDecimal.ONE)));
                }
        );
    }
}
