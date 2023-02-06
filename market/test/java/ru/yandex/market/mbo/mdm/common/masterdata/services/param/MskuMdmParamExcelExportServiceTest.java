package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValueType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author albina-gima
 * @date 10/1/20
 */
public class MskuMdmParamExcelExportServiceTest {
    private static final String NO_USER = "";
    private MskuMdmParamExcelExportService service;

    @Before
    public void setup() {
        service = new MskuMdmParamExcelExportService();
    }

    @Test
    public void whenNoParamsShouldYieldEmptyExcel() {
        ExcelFile file = service.exportExcelFile(List.of(), List.of());
        assertThat(file.getHeaders()).containsExactly(
            MdmParamExcelAttributes.MSKU_ID,
            MdmParamExcelAttributes.UPDATED_BY_LOGIN,
            MdmParamExcelAttributes.UPDATED_TS,
            MdmParamExcelAttributes.UPDATED_SOURCE
        );
        assertThat(file.getLastLine()).isZero();
    }

    @Test
    public void whenBoolParamExistsShouldExportIt() {
        long mskuId = 555L;
        long mdmParamId = 100500L;
        String paramName = "Умеет плавать";
        boolean boolValue = false;

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setValueType(MdmParamValueType.BOOL)
            .setTitle(paramName);

        MskuParamValue value = new MskuParamValue();
        value.setMskuId(mskuId)
            .setMdmParamId(mdmParamId)
            .setBool(boolValue);

        CommonMsku commonMsku = new CommonMsku(0L, mskuId)
            .setParamValues(Stream.of(value).collect(Collectors.toMap(MskuParamValue::getMdmParamId,
                Function.identity())));

        ExcelFile file = service.exportExcelFile(List.of(param), List.of(commonMsku));
        assertThat(file.getHeaders()).containsExactly(
            MdmParamExcelAttributes.MSKU_ID,
            paramName,
            MdmParamExcelAttributes.UPDATED_BY_LOGIN,
            MdmParamExcelAttributes.UPDATED_TS,
            MdmParamExcelAttributes.UPDATED_SOURCE
        );
        assertThat(file.getLastLine()).isEqualTo(1);
        assertThat(file.getValuesList(file.getLastLine())).containsExactly(
            String.valueOf(mskuId),
            "нет",
            NO_USER,
            value.getUpdatedTs().toString(),
            value.getMasterDataSourceType().toString()
        );
    }
}
