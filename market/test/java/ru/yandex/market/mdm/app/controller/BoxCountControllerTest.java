package ru.yandex.market.mdm.app.controller;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.application.monitoring.ComplexMonitoring;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;
import ru.yandex.market.mboc.common.masterdata.repository.document.QualityDocumentRepository;
import ru.yandex.market.mboc.common.masterdata.services.SskuMasterDataStorageService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

/**
 * @author amaslak
 */
@SuppressWarnings("checkstyle:magicnumber")
public class BoxCountControllerTest extends MdmBaseDbTestClass {
    private static final long SEED = 20200604L;

    private static final ShopSkuKey KEY_1P_EXTERNAL = new ShopSkuKey(SupplierConverterServiceMock.BERU_ID, "01.1p_22");
    private static final ShopSkuKey KEY_1P_INTERNAL = new ShopSkuKey(12, "1p_22");
    private static final ShopSkuKey KEY_3P = new ShopSkuKey(42, "3p_offer");
    private static final int BOX_COUNT = 656;
    private static final int BOX_COUNT_NEW = 3;

    @Autowired
    private MasterDataRepository masterDataRepository;

    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private QualityDocumentRepository qualityDocumentRepository;

    private SskuMasterDataStorageService sskuMasterDataStorageService;
    private BoxCountController boxCountController;
    private EnhancedRandom random;

    @Before
    public void setUp() {
        SupplierConverterServiceMock supplierConverterService = new SupplierConverterServiceMock();
        supplierConverterService.addInternalToExternalMapping(KEY_1P_INTERNAL, KEY_1P_EXTERNAL);

        random = TestDataUtils.defaultRandom(SEED);
        sskuMasterDataStorageService = new SskuMasterDataStorageService(
            masterDataRepository, qualityDocumentRepository, transactionHelper, new SupplierConverterServiceMock(),
            new ComplexMonitoring());
        boxCountController = new BoxCountController(
            masterDataRepository, sskuMasterDataStorageService, supplierConverterService, transactionHelper
        );

        MasterData masterData3p = TestDataUtils.generateMasterData(KEY_3P, random);
        masterData3p.setBoxCount(BOX_COUNT);
        masterDataRepository.insert(masterData3p);

        MasterData masterData1p = TestDataUtils.generateMasterData(KEY_1P_INTERNAL, random);
        masterData1p.setBoxCount(BOX_COUNT);
        masterDataRepository.insert(masterData1p);
    }

    @Test
    public void whenGet3PShouldReturnBoxCount() {
        ResponseEntity<String> stringResponseEntity = boxCountController.get(
            KEY_3P.getSupplierId(), KEY_3P.getShopSku()
        );
        Assertions.assertThat(stringResponseEntity).isNotNull();
        Assertions.assertThat(stringResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(stringResponseEntity.getBody()).isEqualTo(String.valueOf(BOX_COUNT));
    }

    @Test
    public void whenGet1PShouldReturnBoxCountForInternal() {
        ResponseEntity<String> stringResponseEntity = boxCountController.get(
            KEY_1P_INTERNAL.getSupplierId(), KEY_1P_INTERNAL.getShopSku());
        Assertions.assertThat(stringResponseEntity).isNotNull();
        Assertions.assertThat(stringResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(stringResponseEntity.getBody()).isEqualTo(String.valueOf(BOX_COUNT));
    }

    @Test
    public void whenGet1PShouldReturnBoxCountForExternal() {
        ResponseEntity<String> stringResponseEntity = boxCountController.get(
            KEY_1P_EXTERNAL.getSupplierId(), KEY_1P_EXTERNAL.getShopSku());
        Assertions.assertThat(stringResponseEntity).isNotNull();
        Assertions.assertThat(stringResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(stringResponseEntity.getBody()).isEqualTo(String.valueOf(BOX_COUNT));
    }

    @Test
    public void whenUpdate1PShouldModifyBoxCountForInternal() {
        ResponseEntity<String> stringResponseEntity = boxCountController.update(
            KEY_1P_INTERNAL.getSupplierId(), KEY_1P_INTERNAL.getShopSku(), BOX_COUNT_NEW
        );
        Assertions.assertThat(stringResponseEntity).isNotNull();
        Assertions.assertThat(stringResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(stringResponseEntity.getBody()).isEqualTo("Success");

        Integer boxCount = masterDataRepository.findById(KEY_1P_INTERNAL).getBoxCount();
        Assertions.assertThat(boxCount).isEqualTo(BOX_COUNT_NEW);
    }

    @Test
    public void whenUpdate1PShouldModifyBoxCountForExternal() {
        ResponseEntity<String> stringResponseEntity = boxCountController.update(
            KEY_1P_EXTERNAL.getSupplierId(), KEY_1P_EXTERNAL.getShopSku(), BOX_COUNT_NEW
        );
        Assertions.assertThat(stringResponseEntity).isNotNull();
        Assertions.assertThat(stringResponseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(stringResponseEntity.getBody()).isEqualTo("Success");

        Integer boxCount = masterDataRepository.findById(KEY_1P_INTERNAL).getBoxCount();
        Assertions.assertThat(boxCount).isEqualTo(BOX_COUNT_NEW);
    }

}
