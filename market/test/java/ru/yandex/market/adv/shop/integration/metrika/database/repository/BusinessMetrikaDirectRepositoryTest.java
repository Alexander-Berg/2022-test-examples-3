package ru.yandex.market.adv.shop.integration.metrika.database.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.service.time.TimeService;
import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.metrika.database.entity.BusinessMetrikaDirectEntity;
import ru.yandex.market.adv.shop.integration.metrika.database.entity.DirectStatusEntity;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Date: 28.01.2022
 * Project: adv-shop-integration
 *
 * @author eogoreltseva
 */
class BusinessMetrikaDirectRepositoryTest extends AbstractShopIntegrationTest {

    @Autowired
    private BusinessMetrikaDirectRepository businessMetrikaDirectRepository;
    @Autowired
    private TimeService timeService;

    @DisplayName("Успешно получили привязку бизнеса к аккаунту в дирекете по id бизнеса.")
    @DbUnitDataSet(
            before = "BusinessMetrikaDirectRepository/csv/findData_correctData_oneResult.csv"
    )
    @Test
    void findByBusinessId_correctData_oneResult() {
        Assertions.assertThat(businessMetrikaDirectRepository.findByBusinessId(112))
                .isPresent()
                .get()
                .isEqualTo(getExpectedBusinessMetrikaDirectEntity());
    }

    @DisplayName("Успешно получили привязку бизнеса к аккаунту в дирекете по id приглашения и id пользователя.")
    @DbUnitDataSet(
            before = "BusinessMetrikaDirectRepository/csv/findData_correctData_oneResult.csv"
    )
    @Test
    void findByInvitationIdAndUserId_correctData_oneResult() {
        Assertions.assertThat(businessMetrikaDirectRepository.findByInvitationIdAndUserId(
                        "invitation2",
                        333
                        )
                )
                .isPresent()
                .get()
                .isEqualTo(getExpectedBusinessMetrikaDirectEntity());
    }

    @DisplayName("Успешно обновили привязку бизнеса к аккаунту в дирекете по id бизнеса.")
    @DbUnitDataSet(
            before = "BusinessMetrikaDirectRepository/csv/updateByBusinessId_correctData_oneRow.before.csv",
            after = "BusinessMetrikaDirectRepository/csv/updateByBusinessId_correctData_oneRow.after.csv"
    )
    @Test
    void updateByBusinessId_correctData_oneRow() {
        Assertions.assertThat(businessMetrikaDirectRepository
                                .updateByBusinessId(
                                        111,
                                        "login2",
                                        333,
                                        DirectStatusEntity.DISABLED,
                                        "invitation2",
                                        getTime()
                                )
                )
                .isEqualTo(1);
    }

    @DisplayName("Успешно обновили привязку методом insertOrUpdate (update).")
    @DbUnitDataSet(
            before = "BusinessMetrikaDirectRepository/csv/updateByBusinessId_correctData_oneRow.before.csv",
            after = "BusinessMetrikaDirectRepository/csv/updateByBusinessId_correctData_oneRow.after.csv"
    )
    @Test
    void insertOrUpdateByBusinessId_correctData_update() {
        businessMetrikaDirectRepository.insertOrUpdate(
                                111,
                                "login2",
                                333,
                                DirectStatusEntity.DISABLED,
                                "invitation2",
                                getTime()
        );
    }

    @DisplayName("Успешно создали привязку методом insertOrUpdate (insert).")
    @DbUnitDataSet(
            after = "BusinessMetrikaDirectRepository/csv/updateByBusinessId_correctData_oneRow.after.csv"
    )
    @Test
    void insertOrUpdateByBusinessId_correctData_insert() {
        businessMetrikaDirectRepository.insertOrUpdate(
                111,
                "login2",
                333,
                DirectStatusEntity.DISABLED,
                "invitation2",
                getTime()
        );
    }

    private static BusinessMetrikaDirectEntity getExpectedBusinessMetrikaDirectEntity() {
        BusinessMetrikaDirectEntity expected = new BusinessMetrikaDirectEntity();
        expected.setBusinessId(112);
        expected.setLogin("login2");
        expected.setUserId(333L);
        expected.setStatus(DirectStatusEntity.ENABLED);
        expected.setInvitationId("invitation2");
        expected.setTime(LocalDateTime.of(2022, 1, 28, 17, 32, 52)
                .atZone(ZoneOffset.systemDefault())
                .toInstant());
        return expected;
    }

    private static Instant getTime() {
        return LocalDateTime.of(2022, 1, 31, 16, 0, 50)
                .atZone(ZoneOffset.systemDefault())
                .toInstant();
    }
}
