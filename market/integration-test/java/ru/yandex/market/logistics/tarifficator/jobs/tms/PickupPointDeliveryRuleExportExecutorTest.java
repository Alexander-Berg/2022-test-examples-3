package ru.yandex.market.logistics.tarifficator.jobs.tms;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.market.logistics.tarifficator.configuration.properties.ExportToYtProperties;
import ru.yandex.market.logistics.tarifficator.facade.yt.PickupPointDeliveryRuleYtExportSnapshotFacade;
import ru.yandex.market.logistics.tarifficator.service.yt.YtCluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DisplayName("Интеграционный тест для проверки выгрузки правил доставки ПВЗ магазинов")
class PickupPointDeliveryRuleExportExecutorTest extends AbstractExportExecutorTest {

    @Autowired
    private PickupPointDeliveryRuleYtExportSnapshotFacade pickupPointDeliveryRuleYtExportSnapshotFacade;

    private PickupPointDeliveryRuleExportExecutor pickupPointDeliveryRuleExportExecutor;

    @Override
    protected void setupExecutor() {
        pickupPointDeliveryRuleExportExecutor = new PickupPointDeliveryRuleExportExecutor(
            new ExportToYtProperties()
                .setBatchSize(1)
                .setGenerationsToKeep(1)
                .setDirectoryPath(getDirectoryPath()),
            clock,
            pickupPointDeliveryRuleYtExportSnapshotFacade,
            YtCluster.HAHN
        );
    }

    @Test
    @DisplayName("Успешная выгрузка тарифов ПВЗ магазинов в YT")
    @DatabaseSetup("/tms/export-shop-pickup-point-tariffs/before.xml")
    void exportSuccess() {
        pickupPointDeliveryRuleExportExecutor.doJob(null);

        verifyTableCreated();
        verifyRowsWritten(
            "tms/export-shop-pickup-point-tariffs/batch1.json",
            "tms/export-shop-pickup-point-tariffs/batch2.json",
            "tms/export-shop-pickup-point-tariffs/batch3.json"
        );
        verifyChunksCombined();
        verifyLinkCreated();
        verifyDirectoryContentGot();
        verifyOutdatedTablesRemoved();
    }

    @Test
    @DisplayName("Ошибка при обработке очередного батча")
    @DatabaseSetup("/tms/export-shop-pickup-point-tariffs/before.xml")
    void processBatchError() {
        RuntimeException exception = new RuntimeException("Writing error");

        doThrow(exception).when(ytTables).write(any(YPath.class), eq(YTableEntryTypes.JACKSON), any(List.class));

        softly.assertThatThrownBy(() -> pickupPointDeliveryRuleExportExecutor.doJob(null))
            .isInstanceOf(exception.getClass())
            .hasMessage(exception.getMessage());

        verifyTableCreated();
        verifyRowsWritten("tms/export-shop-pickup-point-tariffs/batch1.json");
        verifyTableRemoved();
    }

    @Nonnull
    @Override
    protected String getDirectoryPath() {
        return "//tmp/tarifficator/shop_pickup_point_tariffs";
    }

}
