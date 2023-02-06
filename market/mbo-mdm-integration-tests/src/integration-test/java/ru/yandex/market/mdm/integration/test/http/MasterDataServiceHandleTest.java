package ru.yandex.market.mdm.integration.test.http;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mdm.http.MasterDataProto;
import ru.yandex.market.mdm.http.MasterDataService;
import ru.yandex.market.mdm.http.MdmCommon;
import ru.yandex.market.mdm.integration.test.config.HttpIntegrationTestConfig.CommonTestParameters;

/**
 * Тесты, вызывающие proto-ручки searchSskuMasterData / saveSskuMasterData.
 *
 * Поскольку в контексте BaseHttpIntegrationTestClass не поднимаются бины, работающие с базой,
 * а функционала удаления ранее созданных SKU МДМ не предоставляет, данный тест работает с существующей SKU в базе
 * (и каждый раз пытается ее пересохранить).
 *
 * Тесты тут только базовые (остальные - в модуле mbo-mdm):
 * 1. поиск несуществующей SSKU
 * 2. валидация существующей SSKU (или новой, если она вдруг пропала)
 * 3. фейл при валидации невалидной SSKU
 *
 * Нюанс: для успешного поиска SSKU сапплаер (mdm.integration-test.supplierId) должен также быть в таблице mdm.supplier.
 *
 * @author dmserebr
 * @date 20/04/2021
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MasterDataServiceHandleTest extends BaseHttpIntegrationTestClass {
    private static final String SHOP_SKU = "mdmMasterDataServiceHandleTest-sku";
    private static final String MISSING_SHOP_SKU = "У губ твоих конфетный, конфетный вкус";

    @Autowired
    private MasterDataService masterDataService;
    @Autowired
    private CommonTestParameters config;

    @Test
    public void testSearchMissingSskuMasterData() {
        var request = MasterDataProto.SearchSskuMasterDataRequest.newBuilder()
            .addShopSkuKeys(MdmCommon.ShopSkuKey.newBuilder()
                .setSupplierId(config.getSupplierId()).setShopSku(MISSING_SHOP_SKU))
            .build();

        var response = masterDataService.searchSskuMasterData(request);

        Assertions.assertThat(response.getSskuMasterDataCount()).isZero();
    }

    @Test
    public void testValidateValidSskuMasterData() {
        var createdSskuMasterData = MdmCommon.SskuMasterData.newBuilder()
            .setSupplierId(config.getSupplierId())
            .setShopSku(SHOP_SKU)
            .addManufacturerCountries("Эритрея")
            .setShelfLifeWithUnits(MdmCommon.TimeInUnits.newBuilder().setValue(30).setUnit(MdmCommon.TimeUnit.DAY))
            .setWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder()
                    .setBoxLengthUm(200000)
                    .setBoxWidthUm(150000)
                    .setBoxHeightUm(100000)
                    .setWeightGrossMg(1000000))
            .build();
        var saveRequest = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(createdSskuMasterData)
            .setValidateOnly(true)
            .build();

        var saveResponse = masterDataService.saveSskuMasterData(saveRequest);

        Assertions.assertThat(saveResponse.getResultsList()).hasSize(1);
        Assertions.assertThat(saveResponse.getResultsList().get(0).getStatus())
            .isEqualTo(MasterDataProto.OperationStatus.OK);
    }

    @Test
    public void testValidateInvalidSskuMasterData() {
        // не пускаем кривые SSKU с нулевыми ВГХ
        var createdSskuMasterData = MdmCommon.SskuMasterData.newBuilder()
            .setSupplierId(config.getSupplierId())
            .setShopSku(SHOP_SKU)
            .addManufacturerCountries("Эритрея")
            .setWeightDimensionsInfo(MdmCommon.WeightDimensionsInfo.newBuilder()
                .setBoxLengthUm(0)
                .setBoxWidthUm(0)
                .setBoxHeightUm(0)
                .setWeightGrossMg(0))
            .build();
        var saveRequest = MasterDataProto.SaveSskuMasterDataRequest.newBuilder()
            .addSskuMasterData(createdSskuMasterData)
            .setValidateOnly(true)
            .build();

        var saveResponse = masterDataService.saveSskuMasterData(saveRequest);
        Assertions.assertThat(saveResponse.getResultsList()).hasSize(1);
        Assertions.assertThat(saveResponse.getResultsList().get(0).getStatus())
            .isEqualTo(MasterDataProto.OperationStatus.VALIDATION_ERROR);
    }
}
