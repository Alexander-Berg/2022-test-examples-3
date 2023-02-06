package ru.yandex.market.ff.repository;

import java.time.LocalDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.LogisticUnit;
import ru.yandex.market.ff.model.entity.ShopRequest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class LogisticUnitRepositoryTest extends IntegrationTest {

    @Autowired
    private LogisticUnitRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/logistic-unit/before.xml")
    @ExpectedDatabase(value = "classpath:repository/logistic-unit/after-save.xml", assertionMode = NON_STRICT)
    public void testSave() {
        LogisticUnit logisticUnit = new LogisticUnit();
        logisticUnit.setPalletId("pallet_1");
        logisticUnit.setBoxId("box_1");
        logisticUnit.setOrderId("order_1");
        logisticUnit.setBarcodes(new String[] {"barcode_1", "barcode_2"});
        logisticUnit.setShouldBeAccepted(true);
        logisticUnit.setAcceptedAt(LocalDateTime.of(2020, 2, 26, 11, 0, 25));
        logisticUnit.setMaxReceiptDate(LocalDateTime.of(2020, 4, 26, 15, 0, 25));
        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(1L);
        logisticUnit.setShopRequest(shopRequest);
        logisticUnit.setBoxesInOrder(10);
        repository.save(logisticUnit);
    }
}
