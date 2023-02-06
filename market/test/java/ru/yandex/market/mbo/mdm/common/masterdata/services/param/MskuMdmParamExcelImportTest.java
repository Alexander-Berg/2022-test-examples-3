package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.mdm.common.infrastructure.FileStatus;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.services.msku.MdmCommonMskuMboService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

import static org.assertj.core.api.Assertions.assertThat;

public class MskuMdmParamExcelImportTest {
    private MskuMdmParamExcelService excelService;

    @Before
    public void setup() {
        MdmParamCache cache = TestMdmParamUtils.createParamCacheMock(TestMdmParamUtils.createDefaultKnownMdmParams());
        MdmParamProviderMock mdmParamProviderMock = new MdmParamProviderMock(cache);
        MdmCommonMskuMboService mdmCommonMskuMboServiceMock = Mockito.mock(MdmCommonMskuMboService.class);
        StorageKeyValueService storageKeyValueServiceMock = Mockito.mock(StorageKeyValueService.class);
        excelService = new MskuMdmParamExcelService(
            cache,
            mdmParamProviderMock,
            mdmCommonMskuMboServiceMock,
            storageKeyValueServiceMock
        );
    }

    @Test
    public void whenExcelHasLessColumnsThenOk() {
        List<MdmParam> mdmParams = TestMdmParamUtils.createDefaultKnownMdmParams();
        Set<Long> enabledParamIds = mdmParams.stream()
            .map(MdmParam::getId)
            .collect(Collectors.toSet());

        var excel = createExcelFile(10L,
            List.of("MskuId", "Полка жизнь", "Жизненное время"), List.of("10", "20"));

        // Импортируем
        List<CommonMsku> result = new ArrayList<>();
        var res = excelService.parseExcel(
            mdmParams, enabledParamIds, "filename", ExcelFileConverter.convertToBytes(excel), result
        );
        assertThat(res.getStatus()).isEqualTo(FileStatus.OK);
        assertThat(res.getErrors().size()).isEqualTo(0);
    }

    @Test
    public void whenExcelHasMoreColumnsThenFail() {
        List<MdmParam> mdmParams = TestMdmParamUtils.createDefaultKnownMdmParams();
        Set<Long> enabledParamIds = mdmParams.stream()
            .map(MdmParam::getId)
            .collect(Collectors.toSet());

        var excel = createExcelFile(10L,
            List.of("MskuId", "Полка жизнь", "Лишний", "Совсем лишний"), List.of("10", "20"));

        // Импортируем
        List<CommonMsku> result = new ArrayList<>();
        var res = excelService.parseExcel(
            mdmParams, enabledParamIds, "filename", ExcelFileConverter.convertToBytes(excel), result
        );
        assertThat(res.getStatus()).isEqualTo(FileStatus.VALIDATION_ERRORS);
        assertThat(res.getErrors()).isEqualTo(List.of("Не найдены параметры для следующих колонок Excel: Лишний, " +
            "Совсем лишний"));
    }

    public ExcelFile createExcelFile(Long mskuId, List<String> columnNames, List<String> columnValues) {
        ExcelFile.Builder excel = new ExcelFile.Builder();

        excel.addHeaders(columnNames);
        List<Object> row = new ArrayList<>();
        row.add(mskuId);
        row.addAll(columnValues);
        excel.addLine(row.toArray());

        return excel.build();
    }
}
