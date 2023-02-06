package ru.yandex.market.logistics.tarifficator.jobs.tms;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.logistics.tarifficator.service.shop.DeliveryTariffService;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
public class ShopDeliveryRegionsExecutorTest extends AbstractMbiMdsS3Test {
    private static final String SHOP_DELIVERY_REGIONS =
        "mbi-premoderation/v-shop-delivery-regions/current_v-shop-delivery-regions.csv";

    @Autowired
    private DeliveryTariffService deliveryTariffService;

    private ShopDeliveryRegionsExecutor executor;

    @Override
    protected String getMdsPath() {
        return SHOP_DELIVERY_REGIONS;
    }

    @BeforeEach
    void onBefore() {
        executor = new ShopDeliveryRegionsExecutor(deliveryTariffService, mdsS3Service);
    }

    @Test
    @DisplayName("Тест джобы ShopDeliveryRegionsExecutor с пустыми таблицами")
    void testEmptyShopDeliveryRegions() {
        mockMdsClientWithResult();
        executor.doJob(null);
        softly.assertThat(result).hasLineCount(1).contains("\"REGION_ID|NUMBER\",\"SHOP_ID|NUMBER\"");
    }

    @Test
    @DisplayName("Тест джобы ShopDeliveryRegionsExecutor с заполненными данными таблицами")
    @DatabaseSetup("testExportShopDeliveryRegions.before.xml")
    void testConfiguredShopDeliveryRegions() throws IOException {
        mockMdsClientWithResult();
        executor.doJob(null);

        try (var stream = getClass().getResourceAsStream("testExportShopDeliveryRegions.csv")) {
            softly.assertThat(result).hasLineCount(4).contains(IOUtils.toString(stream));
        }
    }

    @Test
    @DisplayName("Тест джобы ShopDeliveryRegionsExecutor с ошибкой от MdsS3Client")
    void testShopDeliveryRegionsError() {
        var message = "someTestError";
        mockMdsClientWithError(message);

        softly.assertThatThrownBy(() -> executor.doJob(null))
            .isInstanceOf(RuntimeException.class)
            .hasCause(new MdsS3Exception(message));
    }
}
