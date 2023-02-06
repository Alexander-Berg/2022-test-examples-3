package ru.yandex.market.adv.shop.integration.metrika.database.repository;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.service.time.TimeService;
import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.metrika.database.entity.BusinessMetrikaUpdaterEntity;
import ru.yandex.market.adv.shop.integration.metrika.database.entity.BusinessMetrikaUpdaterEventType;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Date: 31.01.2022
 * Project: adv-shop-integration
 *
 * @author eogoreltseva
 */
class BusinessMetrikaUpdaterRepositoryTest extends AbstractShopIntegrationTest {

    @Autowired
    private BusinessMetrikaUpdaterRepository businessMetrikaUpdaterRepository;
    @Autowired
    private TimeService timeService;

    @DisplayName("Успешно получили обновления счетчиков по id бизнеса.")
    @DbUnitDataSet(
            before = "BusinessMetrikaUpdaterRepository/csv/findData_correctData_threeRows.csv"
    )
    @Test
    void findByBusinessId_correctData_twoRows() {
        List<BusinessMetrikaUpdaterEntity> businessMetrikaUpdaterEntityList =
                businessMetrikaUpdaterRepository.getBusinessMetrikaUpdater(2);

        Assertions.assertThat(businessMetrikaUpdaterEntityList)
                .size()
                .isEqualTo(2);
        Assertions.assertThat(businessMetrikaUpdaterEntityList)
                .contains(getBusinessMetrikaUpdaterEntityWithId(111L, 1L))
                .contains(getBusinessMetrikaUpdaterEntityWithId(112L, 2L));
    }

    @DisplayName("Данные по обновлению счетчиков успешно сохранены в БД.")
    @DbUnitDataSet(
            after = "BusinessMetrikaUpdaterRepository/csv/findData_withoutId_oneRow.csv"
    )
    @Test
    void save_correctData_oneRow() {

        businessMetrikaUpdaterRepository.save(getBusinessMetrikaUpdaterEntity(113L));
    }

    @DisplayName("Данные по обновлению счетчиков успешно удалены из БД.")
    @DbUnitDataSet(
            before = "BusinessMetrikaUpdaterRepository/csv/findData_correctData_threeRows.csv",
            after = "BusinessMetrikaUpdaterRepository/csv/findData_correctData_twoRows.csv"
    )
    @Test
    void delete_correctData_oneRow() {

        businessMetrikaUpdaterRepository.delete(getBusinessMetrikaUpdaterEntityWithId(113L, 3L));
    }

    @DisplayName("Данные по обновлению счетчиков успешно сохранены в БД методом insertOrUpdate.")
    @DbUnitDataSet(
            before = "BusinessMetrikaUpdaterRepository/csv/insertOrUpdate_correctData_twoRows.before.csv",
            after = "BusinessMetrikaUpdaterRepository/csv/insertOrUpdate_correctData_twoRows.after.csv"
    )
    @Test
    void insertOrUpdate_correctData_twoRows() {

        businessMetrikaUpdaterRepository.insertOrUpdate(
                111L, BusinessMetrikaUpdaterEventType.CREATE, timeService.get());
        businessMetrikaUpdaterRepository.insertOrUpdate(
                111L, BusinessMetrikaUpdaterEventType.UPDATE, timeService.get());
    }


    private BusinessMetrikaUpdaterEntity getBusinessMetrikaUpdaterEntity(Long businessId) {
        BusinessMetrikaUpdaterEntity businessMetrikaUpdaterEntity = new BusinessMetrikaUpdaterEntity();
        businessMetrikaUpdaterEntity.setBusinessId(businessId);
        businessMetrikaUpdaterEntity.setEventType(BusinessMetrikaUpdaterEventType.UPDATE);
        businessMetrikaUpdaterEntity.setUpdateTime(timeService.get());
        return businessMetrikaUpdaterEntity;
    }

    private BusinessMetrikaUpdaterEntity getBusinessMetrikaUpdaterEntityWithId(Long businessId, Long id) {
        BusinessMetrikaUpdaterEntity businessMetrikaUpdaterEntity = getBusinessMetrikaUpdaterEntity(businessId);
        businessMetrikaUpdaterEntity.setId(id);
        return businessMetrikaUpdaterEntity;
    }
}
