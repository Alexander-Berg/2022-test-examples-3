package ru.yandex.market.adv.b2bmonetization.programs.interactor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.FeeRecommendation;
import ru.yandex.market.adv.b2bmonetization.programs.yt.entity.TestCategory;
import ru.yandex.market.adv.b2bmonetization.programs.yt.entity.TestOffer;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;

/**
 * Date: 11.07.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
class ProgramCheckInteractorTest extends AbstractMonetizationTest {

    @Autowired
    @Qualifier("checkProgram")
    private Executor checkProgram;

    @DbUnitDataSet(
            before = "ProgramCheckInteractor/csv/checkProgram_fourChangeStatusTwoRepeat_success.before.csv",
            after = "ProgramCheckInteractor/csv/checkProgram_fourChangeStatusTwoRepeat_success.after.csv"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/checkProgram_fourChangeStatusTwoRepeat_success_blue_shop_offer"
            ),
            before = "ProgramCheckInteractor/json/yt/BlueOffer/" +
                    "checkProgram_fourChangeStatusTwoRepeat_success.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/checkProgram_fourChangeStatusTwoRepeat_success_blue_shop_category"
            ),
            before = "ProgramCheckInteractor/json/yt/Category/" +
                    "checkProgram_fourChangeStatusTwoRepeat_success.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestCategory.class,
                    path = "//tmp/checkProgram_fourChangeStatusTwoRepeat_success_shop_category"
            ),
            before = "ProgramCheckInteractor/json/yt/Category/" +
                    "checkProgram_fourChangeStatusTwoRepeat_success.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = TestOffer.class,
                    path = "//tmp/checkProgram_fourChangeStatusTwoRepeat_success_shop_offer"
            ),
            before = "ProgramCheckInteractor/json/yt/WhiteOffer/" +
                    "checkProgram_fourChangeStatusTwoRepeat_success.before.json"
    )
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = FeeRecommendation.class,
                    path = "//tmp/checkProgram_fourChangeStatusTwoRepeat_success_fee_recommendation"
            )
    )
    @DisplayName("Проверка программ у партнеров, на готовность к активации.")
    @Test
    void checkProgram_fourChangeStatusTwoRepeat_success() {
        run("checkProgram_fourChangeStatusTwoRepeat_success_",
                () -> checkProgram.doJob(mockContext())
        );
    }
}
