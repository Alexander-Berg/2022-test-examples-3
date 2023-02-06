package ru.yandex.market.logistics.tarifficator.jobs.tms;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.configuration.properties.ShopProperties;
import ru.yandex.market.logistics.tarifficator.repository.shop.SelfDeliveryChangeLogRepository;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DisplayName("Тесты на джобу, удаляющую старые события из market_shop_tariff.self_delivery_changelog")
public class RemoveObsoleteShopTariffChangelogsExecutorTest extends AbstractContextualTest {

    @Autowired
    private SelfDeliveryChangeLogRepository changeLogRepository;
    @Autowired
    private ShopProperties shopProperties;
    private RemoveObsoleteShopTariffChangelogsExecutor tested;

    @BeforeEach
    void setUpConfiguration() {
        tested = new RemoveObsoleteShopTariffChangelogsExecutor(changeLogRepository, shopProperties);
    }

    @Test
    @DatabaseSetup("/tms/changelog/changelogExportDelete.before.xml")
    @ExpectedDatabase(
        value = "/tms/changelog/changelogExportDelete.after.xml",
        assertionMode = NON_STRICT
    )
    @DisplayName("Успешное исполнение джобы")
    void successfulExecution() {
        tested.doJob(null);
    }
}
