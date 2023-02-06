package ru.yandex.market.adv.b2bmonetization.campaign.interactor.file;

import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;

/**
 * Date: 18.02.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
class MdsOldOfferFileCleanerInteractorTest extends AbstractMonetizationTest {

    @Autowired
    @Qualifier("tmsOldOfferFileCleanerExecutor")
    private Executor tmsOldOfferFileCleanerExecutor;

    @DisplayName("Проверка работоспособности job tmsOldOfferFileCleanerExecutor.")
    @DbUnitDataSet(
            before = "MdsOldOfferFileCleanerInteractor/csv/remove_findAll_removeTwoAndOneException.before.csv",
            after = "MdsOldOfferFileCleanerInteractor/csv/remove_findAll_removeTwoAndOneException.after.csv"
    )
    @Test
    void remove_findAll_removeTwoAndOneException() {
        Assertions.assertThatThrownBy(() -> tmsOldOfferFileCleanerExecutor.doJob(mockContext()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File offer_wrong.xlsm doesn't exist.");
    }
}
