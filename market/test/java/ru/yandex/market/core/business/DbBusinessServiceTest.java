package ru.yandex.market.core.business;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяем работу с бизнесом.
 */
@DbUnitDataSet(before = "DbBusinessServiceTest.before.csv")
class DbBusinessServiceTest extends FunctionalTest {

    @Autowired
    private BusinessService businessService;

    @Test
    @DisplayName("Проверяем удаление бизнесов")
    @DbUnitDataSet(after = "DbBusinessServiceTest.after.csv")
    void removeBusinessTest() {
        //бизнес с привязанным сервисом
        Assertions.assertThatThrownBy(() -> businessService.removeBusiness(10L, 1L)).isInstanceOf(IllegalArgumentException.class);

        //бизнес с фичой лого
        businessService.removeBusiness(505L, 1L);

        //бизнес без фичи лого
        businessService.removeBusiness(30L, 1L);

        assertThat(businessService.getBusiness(10L).isDeleted()).isFalse();
        assertThat(businessService.getBusiness(30L).isDeleted()).isTrue();
        assertThat(businessService.getBusiness(40L).isDeleted()).isFalse();
        assertThat(businessService.getBusiness(505L).isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Проверяем отвязку сервиса")
    @DbUnitDataSet(after = "DbBusinessServiceTest.unlinkService.after.csv")
    void unlinkServiceTest() {
        businessService.unlinkService(1L, 1L);
        assertThat(businessService.getBusiness(10L).isDeleted()).isTrue();
        assertThat(businessService.getBusiness(30L).isDeleted()).isFalse();
        assertThat(businessService.getBusiness(40L).isDeleted()).isFalse();
        assertThat(businessService.getBusiness(505L).isDeleted()).isFalse();
    }

    @Test
    @DisplayName("Удаление бизнеса с незаверенной операцией")
    @DbUnitDataSet(after = "DbBusinessServiceTest.failRemove.after.csv")
    void failRemoveBusinessTest() {
        Assertions.assertThatThrownBy(() -> businessService.removeBusiness(40L, 1L)).isInstanceOf(IllegalArgumentException.class);
    }
}
