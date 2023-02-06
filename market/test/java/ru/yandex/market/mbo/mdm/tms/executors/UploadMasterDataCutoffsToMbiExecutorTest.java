package ru.yandex.market.mbo.mdm.tms.executors;

import java.time.LocalDateTime;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.mbo.mdm.common.infrastructure.MdmLogbrokerServiceMock;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueServiceImpl;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.model.cutoff.OfferCutoff;
import ru.yandex.market.mboc.common.masterdata.repository.cutoff.OfferCutoffRepository;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffService;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffServiceImpl;
import ru.yandex.market.mboc.common.masterdata.services.cutoff.OfferCutoffTypeProvider;

@SuppressWarnings({"checkstyle:magicNumber", "checkstyle:lineLength"})
public class UploadMasterDataCutoffsToMbiExecutorTest extends MdmBaseDbTestClass {

    @Resource
    private OfferCutoffRepository offerCutoffRepository;

    @Resource
    private TransactionHelper transactionHelper;

    @Resource
    private StorageKeyValueServiceImpl storageKeyValueService;

    private OfferCutoffService offerCutoffService;
    private ComplexMonitoring complexMonitoring;
    private UploadMasterDataCutoffsToMbiExecutor executor;
    private MdmLogbrokerServiceMock logbrokerProducerService;

    @Before
    public void setUp() {
        logbrokerProducerService = new MdmLogbrokerServiceMock();
        complexMonitoring = new ComplexMonitoring();
        offerCutoffService = new OfferCutoffServiceImpl(offerCutoffRepository, transactionHelper);
        executor = new UploadMasterDataCutoffsToMbiExecutor(logbrokerProducerService, offerCutoffService,
            storageKeyValueService, complexMonitoring, 0
        );
        storageKeyValueService.putValue(UploadMasterDataCutoffsToMbiExecutor.CUTOFF_MAX_COUNT_KEY, 1);
        storageKeyValueService.putValue(UploadMasterDataCutoffsToMbiExecutor.CUTOFF_MAX_COUNT_BY_SHELF_LIFE_KEY, 1);
    }

    @Test
    public void checkMonitoring() {
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(1)
            .setTypeId(OfferCutoffTypeProvider.MISS_MANUFACTURER_COUNTRY_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(1)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1"));

        executor.execute();

        ComplexMonitoring.Result result = complexMonitoring.getResult();
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void checkFailedCountryMonitoring() {
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(1)
            .setTypeId(OfferCutoffTypeProvider.MISS_MANUFACTURER_COUNTRY_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(2)
            .setTypeId(OfferCutoffTypeProvider.MISS_MANUFACTURER_COUNTRY_ERROR_CODE).setErrorCode("1"));

        executor.execute();

        ComplexMonitoring.Result result = complexMonitoring.getResult();
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(result.getMessage()).contains("Max cutoff count by miss country 1 exceeded (total 2)");
    }

    @Test
    public void checkFailedShelfLifeMonitoring() {
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(1)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(2)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1"));
        executor.execute();

        ComplexMonitoring.Result result = complexMonitoring.getResult();
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(result.getMessage()).contains("Max cutoff count by miss shelf life 1 exceeded (total 2)");
    }

    @Test
    public void checkFailedSeveralReasonMonitoring() {
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(1)
            .setTypeId(OfferCutoffTypeProvider.MISS_MANUFACTURER_COUNTRY_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(2)
            .setTypeId(OfferCutoffTypeProvider.MISS_MANUFACTURER_COUNTRY_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(1)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(2)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1"));
        executor.execute();

        ComplexMonitoring.Result result = complexMonitoring.getResult();
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(result.getMessage()).contains("Max cutoff count by miss shelf life 1 exceeded (total 2)");
        Assertions.assertThat(result.getMessage()).contains("Max cutoff count by miss country 1 exceeded (total 2)");
    }

    @Test
    public void checkFailedOneOfSeveralReasonMonitoring() {
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(1)
            .setTypeId(OfferCutoffTypeProvider.MISS_MANUFACTURER_COUNTRY_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(1)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(2)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1"));
        executor.execute();

        ComplexMonitoring.Result result = complexMonitoring.getResult();
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(result.getMessage()).contains("Max cutoff count by miss shelf life 1 exceeded (total 2)");
        Assertions.assertThat(result.getMessage()).doesNotContain("miss country");
    }

    @Test
    public void checkIgnoreSuppliers() {
        storageKeyValueService.putValue(UploadMasterDataCutoffsToMbiExecutor.CUTOFF_SUPPLIERS_TO_IGNORE_KEY,
            new int[]{1});
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(1)
            .setTypeId(OfferCutoffTypeProvider.MISS_MANUFACTURER_COUNTRY_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(2)
            .setTypeId(OfferCutoffTypeProvider.MISS_MANUFACTURER_COUNTRY_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(1)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(2)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1"));

        executor.execute();

        ComplexMonitoring.Result result = complexMonitoring.getResult();
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void checkIgnoreSeveralSuppliers() {
        storageKeyValueService.putValue(UploadMasterDataCutoffsToMbiExecutor.CUTOFF_SUPPLIERS_TO_IGNORE_KEY,
            new int[]{1, 2});
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(1)
            .setTypeId(OfferCutoffTypeProvider.MISS_MANUFACTURER_COUNTRY_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(2)
            .setTypeId(OfferCutoffTypeProvider.MISS_MANUFACTURER_COUNTRY_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(1)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(2)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1"));

        executor.execute();

        ComplexMonitoring.Result result = complexMonitoring.getResult();
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void checkCutoffsUploadedToMBIAccordingToLimit() {
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(1)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1"));
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(2)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1"));

        executor.execute();

        Assertions.assertThat(logbrokerProducerService.getSuccessCount()).isEqualTo(1);
    }

    @Test
    public void checkCutoffsUploadedToMBIIgnoringAlreadyUploaded() {
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(2)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1")
            .setStateUploadTs(LocalDateTime.now()));

        executor.execute();

        Assertions.assertThat(logbrokerProducerService.getSuccessCount()).isEqualTo(0);
    }

    @Test
    public void checkCutoffsUploadedToMBIIgnoreSuppliers() {
        storageKeyValueService.putValue(UploadMasterDataCutoffsToMbiExecutor.CUTOFF_SUPPLIERS_TO_IGNORE_KEY,
            new int[]{1});
        offerCutoffRepository.insert(new OfferCutoff().setShopSku("sku1").setSupplierId(1)
            .setTypeId(OfferCutoffTypeProvider.MISS_SHELF_LIFE_ERROR_CODE).setErrorCode("1"));

        executor.execute();

        Assertions.assertThat(logbrokerProducerService.getSuccessCount()).isEqualTo(0);
    }
}
