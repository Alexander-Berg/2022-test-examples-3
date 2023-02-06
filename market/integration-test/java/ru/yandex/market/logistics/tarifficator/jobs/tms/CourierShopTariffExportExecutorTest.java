package ru.yandex.market.logistics.tarifficator.jobs.tms;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.market.logistics.tarifficator.configuration.properties.ExportToYtProperties;
import ru.yandex.market.logistics.tarifficator.facade.yt.CourierShopTariffYtExportSnapshotFacade;
import ru.yandex.market.logistics.tarifficator.service.yt.YtCluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

@DisplayName("Интеграционный тест ShopCourierTariffExportExecutor")
class CourierShopTariffExportExecutorTest extends AbstractExportExecutorTest {

    @Autowired
    private CourierShopTariffYtExportSnapshotFacade courierShopTariffYtExportSnapshotFacade;

    private CourierShopTariffExportExecutor courierShopTariffExportExecutor;

    @Test
    @DisplayName("Успешная выгрузка тарифов магазинов в YT")
    @DatabaseSetup("/tms/export-shop-courier-tariffs/before.xml")
    void exportSuccess() {
        courierShopTariffExportExecutor.doJob(null);

        verifyTableCreated();
        verifyRowsWritten(
            "tms/export-shop-courier-tariffs/batch1.json",
            "tms/export-shop-courier-tariffs/batch2.json",
            "tms/export-shop-courier-tariffs/batch3.json",
            "tms/export-shop-courier-tariffs/batch4.json"
        );
        verifyChunksCombined();
        verifyLinkCreated();
        verifyDirectoryContentGot();
        verifyOutdatedTablesRemoved();
    }

    @Test
    @DisplayName("Ошибка при обработке очередного батча")
    @DatabaseSetup("/tms/export-shop-courier-tariffs/before.xml")
    void processBatchError() {
        RuntimeException exception = new RuntimeException("Writing error");

        doThrow(exception).when(ytTables).write(any(YPath.class), eq(YTableEntryTypes.JACKSON), any(List.class));

        softly.assertThatThrownBy(() -> courierShopTariffExportExecutor.doJob(null))
            .isInstanceOf(exception.getClass())
            .hasMessage(exception.getMessage());

        verifyTableCreated();
        verifyRowsWritten("tms/export-shop-courier-tariffs/batch1.json");
        verifyTableRemoved();
    }

    @Override
    protected void setupExecutor() {
        courierShopTariffExportExecutor = new CourierShopTariffExportExecutor(
            new ExportToYtProperties()
                .setBatchSize(1)
                .setGenerationsToKeep(1)
                .setDirectoryPath(getDirectoryPath()),
            clock,
            courierShopTariffYtExportSnapshotFacade,
            YtCluster.HAHN
        );
    }

    @Nonnull
    @Override
    protected String getDirectoryPath() {
        return "//tmp/tarifficator/shop_courier_tariffs";
    }

}

