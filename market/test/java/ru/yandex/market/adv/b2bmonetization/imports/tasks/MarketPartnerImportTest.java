package ru.yandex.market.adv.b2bmonetization.imports.tasks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tms.quartz2.model.Executor;

@DisplayName("Тест на импорт market_partner")
class MarketPartnerImportTest extends AbstractMonetizationTest {

    @Qualifier("importMarketPartners")
    @Autowired
    private Executor executor;

    @DisplayName("Импорт в непустую таблицу прошел успешно")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketPartnerImportTask.YtPartnerProgramType.class,
                    path = "//tmp/adv_unittest/dictionaries/mbi/partner_program_type/latest",
                    isDynamic = false
            ),
            before = "MarketPartnerImportTest/json/syncMarketPartner_existData_success.json"
    )
    @DbUnitDataSet(
            before = "MarketPartnerImportTest/csv/syncMarketPartner_existData_success.before.csv",
            after = "MarketPartnerImportTest/csv/syncMarketPartner_existData_success.after.csv"
    )
    @Test
    void syncMarketPartner_existData_success() {
        run("adv_unittest/",
                () -> executor.doJob(mockContext())
        );
    }
}
