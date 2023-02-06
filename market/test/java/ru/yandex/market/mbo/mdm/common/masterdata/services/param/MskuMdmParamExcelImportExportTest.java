package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.MdmCommonMskuMboService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author albina-gima
 * @date 10/1/20
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MskuMdmParamExcelImportExportTest {
    private MskuMdmParamExcelService excelService;
    private MskuMdmParamExcelExportService exportService;

    @Before
    public void setup() {
        MdmParamCache cache = TestMdmParamUtils.createParamCacheMock(TestMdmParamUtils.createDefaultKnownMdmParams());
        MdmParamProvider mdmParamProviderMock = new MdmParamProviderMock(cache);
        MdmCommonMskuMboService mdmCommonMskuMboServiceMock = Mockito.mock(MdmCommonMskuMboService.class);
        StorageKeyValueService storageKeyValueServiceMock = Mockito.mock(StorageKeyValueService.class);
        excelService = new MskuMdmParamExcelService(
            cache,
            mdmParamProviderMock,
            mdmCommonMskuMboServiceMock,
            storageKeyValueServiceMock
        );
        exportService = new MskuMdmParamExcelExportService();
    }

    @Test
    public void testExportAndImportWorkTogether() {
        long mskuId = 500L;

        var heavyGood = TestMdmParamUtils.createMskuParamValue(
            KnownMdmParams.HEAVY_GOOD,
            mskuId,
            true,
            null,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var heavyGood20 = TestMdmParamUtils.createMskuParamValue(
            KnownMdmParams.HEAVY_GOOD_20,
            mskuId,
            false,
            null,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );

        CommonMsku srcMsku = new CommonMsku(0L, mskuId)
            .setParamValues(Stream.of(heavyGood, heavyGood20).collect(Collectors.toMap(MskuParamValue::getMdmParamId,
                Function.identity())));

        // Экспортируем файл
        List<MdmParam> mdmParams = TestMdmParamUtils.createDefaultKnownMdmParams();
        var excel = exportService.exportExcelFile(
            mdmParams,
            List.of(srcMsku)
        );
        Set<Long> enabledParamIds = mdmParams.stream()
            .map(MdmParam::getId)
            .collect(Collectors.toSet());

        CommonMsku msku = new CommonMsku(0L, mskuId)
            .setParamValues(Stream.of(heavyGood, heavyGood20).collect(Collectors.toMap(MskuParamValue::getMdmParamId,
                Function.identity())));
        List<CommonMsku> expected = List.of(msku);

        // Импортируем
        List<CommonMsku> result = new ArrayList<>();
        List<String> errors = excelService.parseExcel(
            mdmParams, enabledParamIds, "filename", ExcelFileConverter.convertToBytes(excel), result
        ).getErrors();
        assertThat(errors).isEmpty();

        // Проверим, что все параметры восстановились до прежнего состояния.
        assertThat(result).containsExactlyInAnyOrderElementsOf(expected);
    }
}
