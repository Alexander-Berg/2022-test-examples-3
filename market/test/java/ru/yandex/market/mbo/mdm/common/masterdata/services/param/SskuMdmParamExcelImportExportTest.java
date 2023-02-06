package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.mdm.common.datacamp.MdmDatacampServiceMock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValueType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.MdmCommonSskuService;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits.TimeUnit;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class SskuMdmParamExcelImportExportTest {

    private SskuMdmParamExcelImportService importService;
    private SskuMdmParamExcelExportService exportService;
    private MdmDatacampServiceMock mdmDatacampService;

    @Before
    public void setup() {
        MdmParamCache cache = TestMdmParamUtils.createParamCacheMock(TestMdmParamUtils.createDefaultKnownMdmParams());
        MdmParamProviderMock mdmParamProvider = new MdmParamProviderMock(cache);
        MdmCommonSskuService mdmCommonSskuServiceMock = Mockito.mock(MdmCommonSskuService.class);
        mdmDatacampService = new MdmDatacampServiceMock();
        importService = new SskuMdmParamExcelImportService(mdmParamProvider, mdmCommonSskuServiceMock,
            mdmDatacampService);
        exportService = new SskuMdmParamExcelExportService();
    }

    @Test
    public void testExportAndImportWorkTogether() {
        ShopSkuKey key1 = new ShopSkuKey(10, "Валенок 1");
        ShopSkuKey key2 = new ShopSkuKey(12, "Стелька 65");

        var shelfLife1 = TestMdmParamUtils.createSskuParamValue(
            KnownMdmParams.SHELF_LIFE,
            key1,
            null,
            4.0,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var shelfLife2 = TestMdmParamUtils.createSskuParamValue(
            KnownMdmParams.SHELF_LIFE,
            key2,
            null,
            8.0,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var shelfLifeUnit1 = TestMdmParamUtils.createSskuParamValue(
            KnownMdmParams.SHELF_LIFE_UNIT,
            key1,
            null,
            null,
            null,
            new MdmParamOption().setId(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeUnit.DAY)),
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var shelfLifeUnit2 = TestMdmParamUtils.createSskuParamValue(
            KnownMdmParams.SHELF_LIFE_UNIT,
            key2,
            null,
            null,
            null,
            new MdmParamOption().setId(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeUnit.HOUR)),
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var shelfLifeComment1 = TestMdmParamUtils.createSskuParamValue(
            KnownMdmParams.SHELF_LIFE_COMMENT,
            key1,
            null,
            null,
            "lorem ipsum dolor sit amet",
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var shelfLifeComment2 = TestMdmParamUtils.createSskuParamValue(
            KnownMdmParams.SHELF_LIFE_COMMENT,
            key2,
            null,
            null,
            "рыбатекст",
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var expirDatesApply1 = TestMdmParamUtils.createSskuParamValue(
            KnownMdmParams.EXPIR_DATE,
            key1,
            true,
            null,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var expirDatesApply2 = TestMdmParamUtils.createSskuParamValue(
            KnownMdmParams.EXPIR_DATE,
            key2,
            false,
            null,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );

        // create param value that is ignored during import
        var weightTare = TestMdmParamUtils.createSskuParamValue(
            KnownMdmParams.WEIGHT_TARE,
            key2,
            null,
            123.0,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );

        CommonSsku srcSsku1 = new CommonSsku(key1)
            .setBaseValues(List.of(shelfLife1, shelfLifeUnit1, shelfLifeComment1, expirDatesApply1));

        CommonSsku srcSsku2 = new CommonSsku(key2)
            .setBaseValues(List.of(shelfLife2, shelfLifeUnit2, shelfLifeComment2, expirDatesApply2, weightTare));

        // Экспортируем файл
        List<MdmParam> mdmParams = TestMdmParamUtils.createDefaultKnownMdmParams();
        var excel = exportService.exportExcelFile(
            mdmParams,
            List.of(srcSsku1, srcSsku2)
        );
        Set<Long> enabledParamIds = mdmParams.stream()
            .map(MdmParam::getId)
            .filter(id -> id != weightTare.getMdmParamId())
            .collect(Collectors.toSet());

        CommonSsku ssku1 = new CommonSsku(key1)
            .setBaseValues(List.of(shelfLife1, shelfLifeUnit1, shelfLifeComment1, expirDatesApply1));
        CommonSsku ssku2 = new CommonSsku(key2)
            .setBaseValues(List.of(shelfLife2, shelfLifeUnit2, shelfLifeComment2, expirDatesApply2));
        List<CommonSsku> expected = List.of(ssku1, ssku2);

        // Импортируем
        List<CommonSsku> result = new ArrayList<>();
        List<String> errors = importService.parseExcel(
            mdmParams, enabledParamIds, "filename", ExcelFileConverter.convertToBytes(excel), "user1", result
        ).getErrors();
        assertThat(errors).isEmpty();

        // Проверим, что все параметры восстановились до прежнего состояния.
        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void testEoxSyncColumnProcessedOnImport() {
        ShopSkuKey key1 = new ShopSkuKey(10, "Валенок 1");
        ShopSkuKey key2 = new ShopSkuKey(12, "Стелька 65");

        var shelfLife1 = TestMdmParamUtils.createSskuParamValue(
            KnownMdmParams.SHELF_LIFE,
            key1,
            null,
            4.0,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var shelfLife2 = TestMdmParamUtils.createSskuParamValue(
            KnownMdmParams.SHELF_LIFE,
            key2,
            null,
            8.0,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var shelfLifeUnit1 = TestMdmParamUtils.createSskuParamValue(
            KnownMdmParams.SHELF_LIFE_UNIT,
            key1,
            null,
            null,
            null,
            new MdmParamOption().setId(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeUnit.DAY)),
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var shelfLifeUnit2 = TestMdmParamUtils.createSskuParamValue(
            KnownMdmParams.SHELF_LIFE_UNIT,
            key2,
            null,
            null,
            null,
            new MdmParamOption().setId(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeUnit.HOUR)),
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );

        CommonSsku srcSsku1 = new CommonSsku(key1).setBaseValues(List.of(shelfLife1, shelfLifeUnit1));
        CommonSsku srcSsku2 = new CommonSsku(key2).setBaseValues(List.of(shelfLife2, shelfLifeUnit2));

        // Экспортируем файл
        List<MdmParam> mdmParams = TestMdmParamUtils.createDefaultKnownMdmParams();
        var excel = exportService.exportExcelFile(
            mdmParams,
            List.of(srcSsku1, srcSsku2)
        );
        Set<Long> enabledParamIds = mdmParams.stream().map(MdmParam::getId).collect(Collectors.toSet());

        CommonSsku ssku1 = new CommonSsku(key1).setBaseValues(List.of(shelfLife1, shelfLifeUnit1));
        CommonSsku ssku2 = new CommonSsku(key2).setBaseValues(List.of(shelfLife2, shelfLifeUnit2));
        List<CommonSsku> expected = List.of(ssku1, ssku2);

        // Импортируем, проставив одному офферу EOX_SYNC
        List<CommonSsku> result = new ArrayList<>();
        ExcelFile modifiedExcel = excel.toBuilder()
            .setValue(1, MdmParamExcelAttributes.EOX_SYNC, "да")
            .build();
        List<String> errors = importService.parseExcel(
            mdmParams, enabledParamIds, "filename", ExcelFileConverter.convertToBytes(modifiedExcel), "user1", result
        ).getErrors();
        assertThat(errors).isEmpty();

        // Проверим, что все параметры восстановились до прежнего состояния.
        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);

        // И что отмеченный для синка оффер прошёл через датакемпный импортёр.
        assertThat(mdmDatacampService.getImportedOffersWithPriorities().keySet())
            .containsExactly(ssku1.getKey());
    }


    @Test
    public void testMultivalueParamExportAndImportTogether() {
        ShopSkuKey shopSkuKey = new ShopSkuKey(15, "Башмачок");
        long enumMdmParamId = 100500L;
        long stringMdmParamId = 100501L;

        String enumParamName = "Виды обуви";
        String stringParamName = "Назначение обуви";

        //mdm params
        List<MdmParamOption> options = List.of(new MdmParamOption().setId(1), new MdmParamOption().setId(2));
        MdmParam enumParam = TestMdmParamUtils.createMdmParam(enumMdmParamId,
            enumParamName,
            MdmParamValueType.ENUM,
            List.of(new MdmParamOption().setId(1), new MdmParamOption().setId(2)),
            Map.of(
                1L, "Мужская",
                2L, "Женская"
            ), true);

        MdmParam stringParam = TestMdmParamUtils.createMdmParam(stringMdmParamId,
            stringParamName,
            MdmParamValueType.STRING,
            null,
            null,
            true);

        //ssku values
        SskuParamValue enumValue = TestMdmParamUtils.createSskuParamValue(enumMdmParamId,
            shopSkuKey,
            null,
            null,
            options,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now());

        SskuParamValue stringValue = TestMdmParamUtils.createSskuParamValue(stringMdmParamId, shopSkuKey,
            null,
            List.of("Повседневная", "Домашняя", "Дорожная", "Пляжная"),
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now());

        CommonSsku commonSsku = new CommonSsku(shopSkuKey)
            .setBaseValues(List.of(enumValue, stringValue));

        // Экспортируем файл
        List<MdmParam> mdmParams = List.of(enumParam, stringParam);
        var excel = exportService.exportExcelFile(
            mdmParams,
            List.of(commonSsku)
        );
        Set<Long> enabledParamIds = mdmParams.stream()
            .map(MdmParam::getId)
            .collect(Collectors.toSet());

        CommonSsku ssku = new CommonSsku(shopSkuKey)
            .setBaseValues(List.of(stringValue, enumValue));
        List<CommonSsku> expected = List.of(ssku);

        // Импортируем
        List<CommonSsku> result = new ArrayList<>();
        List<String> errors = importService.parseExcel(
            mdmParams, enabledParamIds, "filename",
            ExcelFileConverter.convertToBytes(excel), "user1", result
        ).getErrors();
        assertThat(errors).isEmpty();

        // Проверим, что все параметры восстановились до прежнего состояния.
        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void whenMultivalueNumericParamImportShouldReturnError() {
        ShopSkuKey shopSkuKey = new ShopSkuKey(15, "Башмачок");
        long numericMdmParamId = 100502L;

        String numericParamName = "Размерная сетка (см)";

        //mdm params
        MdmParam numericParam = TestMdmParamUtils.createMdmParam(numericMdmParamId,
            numericParamName,
            MdmParamValueType.NUMERIC,
            null,
            null,
            true);

        //ssku values
        SskuParamValue numericValue = TestMdmParamUtils.createSskuParamValue(numericMdmParamId,
            shopSkuKey,
            List.of(25d, 25.5d, 26.5d, 27d),
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now());

        CommonSsku commonSsku = new CommonSsku(shopSkuKey)
            .setBaseValues(List.of(numericValue));

        // Экспортируем файл
        List<MdmParam> mdmParams = List.of(numericParam);
        var excel = exportService.exportExcelFile(
            mdmParams,
            List.of(commonSsku)
        );
        Set<Long> enabledParamIds = mdmParams.stream()
            .map(MdmParam::getId)
            .collect(Collectors.toSet());

        // Импортируем
        List<CommonSsku> result = new ArrayList<>();
        List<String> errors = importService.parseExcel(
            mdmParams, enabledParamIds, "filename",
            ExcelFileConverter.convertToBytes(excel), "user1", result
        ).getErrors();
        assertThat(errors).containsExactlyInAnyOrderElementsOf(List.of("Ключ shop_id: 15; shop_sku: Башмачок, " +
            "параметр \"Размерная сетка (см)\": Для мультизначных чисел десятичный разделитель должен быть точкой " +
            "\"25,25.5,26.5,27\""));
    }
}
