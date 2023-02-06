package ru.yandex.market.ff.repository;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.LogisticUnit;
import ru.yandex.market.ff.model.entity.LogisticUnitError;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.enums.LogisticUnitErrorType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class LogisticUnitErrorRepositoryTest extends IntegrationTest {

    @Autowired
    private LogisticUnitErrorRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/logistic-unit-error/before.xml")
    @ExpectedDatabase(value = "classpath:repository/logistic-unit-error/after-save.xml", assertionMode = NON_STRICT)
    public void testSave() {
        LogisticUnitError logisticUnitError = new LogisticUnitError();
        logisticUnitError.setType(LogisticUnitErrorType.EXTERNAL_CREATION_ERROR);
        logisticUnitError.setErrorCode(10);
        logisticUnitError.setDescription("Заказ не найден");
        LogisticUnit logisticUnit = new LogisticUnit();
        logisticUnit.setId(1L);
        logisticUnitError.setLogisticUnit(logisticUnit);
        ShopRequest request = new ShopRequest();
        request.setId(1L);
        logisticUnitError.setShopRequest(request);
        repository.save(logisticUnitError);
    }
}
