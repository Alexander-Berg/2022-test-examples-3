package ru.yandex.market.adv.shop.integration.metrika.database.repository;

import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.metrika.database.entity.BusinessMetrikaCounterEntity;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Date: 28.01.2022
 * Project: adv-shop-integration
 *
 * @author eogoreltseva
 */
class BusinessMetrikaCounterRepositoryTest extends AbstractShopIntegrationTest {

    @Autowired
    private BusinessMetrikaCounterRepository businessMetrikaCounterRepository;

    @DisplayName("Успешно получили идентификаторы счетчиков в метрике бизнесов по id бизнеса.")
    @DbUnitDataSet(
            before = "BusinessMetrikaCounterRepository/csv/findData_correctData_oneResult.csv"
    )
    @Test
    void findByBusinessId_correctData_oneResult() {
        Assertions.assertThat(businessMetrikaCounterRepository.findByBusinessId(113))
                .isPresent()
                .get()
                .isEqualTo(getBusinessMetrikaCounterEntity());
    }

    @DisplayName("Успешно получили идентификаторы счетчиков в метрике бизнесов по id счетчика.")
    @DbUnitDataSet(
            before = "BusinessMetrikaCounterRepository/csv/findData_correctData_oneResult.csv"
    )
    @Test
    void findByCounterId_correctData_oneResult() {
        Assertions.assertThat(businessMetrikaCounterRepository.findByCounterId(444))
                .isPresent()
                .get()
                .isEqualTo(getBusinessMetrikaCounterEntity());
    }

    @DisplayName("Успешно обновили идентификатор счетчика по id бизнеса.")
    @DbUnitDataSet(
            before = "BusinessMetrikaCounterRepository/csv/updateCounterIdByBusinessId_correctData_oneRow.before.csv",
            after = "BusinessMetrikaCounterRepository/csv/updateCounterIdByBusinessId_correctData_oneRow.after.csv"

    )
    @Test
    void updateCounterIdByBusinessId_correctData_oneRow() {
        Assertions.assertThat(businessMetrikaCounterRepository
                        .updateCounterIdByBusinessId(
                        333,
                        111
                        )
                )
                .isEqualTo(1);
    }

    @DisplayName("Успешно обновили дополнительные логины по id счетчика.")
    @DbUnitDataSet(
            before = "BusinessMetrikaCounterRepository/csv/" +
                    "updateAdditionalLoginsByCounterId_correctData_oneRow.before.csv",
            after = "BusinessMetrikaCounterRepository/csv/" +
                    "updateAdditionalLoginsByCounterId_correctData_oneRow.after.csv"

    )
    @Test
    void updateAdditionalLoginsByCounterId_correctData_oneRow() {
        Assertions.assertThat(businessMetrikaCounterRepository
                        .updateAdditionalLoginsByCounterId(
                                Set.of("login1", "login2", "login3"),
                                222
                        )
                )
                .isEqualTo(1);
    }

    @DisplayName("Идентификатор счетчиков в метрике бизнесов успешно сохранен в БД.")
    @DbUnitDataSet(
            after = "BusinessMetrikaCounterRepository/csv/insert_correctData_oneRow.after.csv"
    )
    @Test
    void insertCounterId_correctData_oneRow() {
        businessMetrikaCounterRepository.insertCounterId(
                111,
                222
        );
    }

    @DisplayName("Исключительная ситуация при сохранении идентификатора счетчика в метрике бизнесов, " +
            "так как он уже есть.")
    @DbUnitDataSet(
            before = "BusinessMetrikaCounterRepository/csv/insertCounterId_existData_exceptionThrown.before.csv"
    )
    @Test
    void insertCounterId_existData_exception() {
        Assertions.assertThatThrownBy(() ->
                businessMetrikaCounterRepository.insertCounterId(
                111,
                222
                )
        ).isInstanceOf(DuplicateKeyException.class);
    }

    private static BusinessMetrikaCounterEntity getBusinessMetrikaCounterEntity() {
        BusinessMetrikaCounterEntity businessMetrikaCounter = new BusinessMetrikaCounterEntity();
        businessMetrikaCounter.setBusinessId(113);
        businessMetrikaCounter.setCounterId(444);
        businessMetrikaCounter.setAdditionalLogins(Set.of("login4", "login5", "login6"));
        return businessMetrikaCounter;
    }
}
