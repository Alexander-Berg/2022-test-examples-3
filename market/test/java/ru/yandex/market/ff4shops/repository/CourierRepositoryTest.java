package ru.yandex.market.ff4shops.repository;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.model.entity.CourierEntity;

class CourierRepositoryTest extends FunctionalTest {
    @Autowired
    private CourierRepository courierRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DbUnitDataSet(after = "CourierRepositoryTest.insertCourier.after.csv")
    void insertCourier() {
        transactionTemplate.execute(ignored -> courierRepository.save(courier()));
    }

    @Nonnull
    private CourierEntity courier() {
        CourierEntity courier = new CourierEntity();
        courier.setCheckouterOrderId(47884876L);
        courier.setLastName("Иванов");
        courier.setFirstName("Иван");
        courier.setMiddleName("Иванович");
        courier.setPhoneNumber("+79991234567");
        courier.setPhoneExtension("123");
        courier.setVehicleNumber("А001AA001");
        courier.setVehicleDescription("Вишневая девятка");
        courier.setUrl("https://go.yandex/route/6ea161f870ba6574d3bd9bdd19e1e9d8?lang=ru");
        courier.setElectronicAcceptanceCertificateCode("202020");
        return courier;
    }
}
