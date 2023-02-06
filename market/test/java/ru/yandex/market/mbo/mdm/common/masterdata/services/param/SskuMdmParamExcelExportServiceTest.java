package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValueType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author amaslak
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class SskuMdmParamExcelExportServiceTest {

    private static final String NO_USER = "";
    private SskuMdmParamExcelExportService service;

    @Before
    public void setup() {
        service = new SskuMdmParamExcelExportService();
    }

    @Test
    public void whenNoParamsShouldYieldEmptyExcel() {
        ExcelFile file = service.exportExcelFile(List.of(), List.of());
        assertThat(file.getHeaders()).containsExactly(
            MdmParamExcelAttributes.SUPPLIER_HEADER,
            MdmParamExcelAttributes.SHOP_SKU_HEADER,
            MdmParamExcelAttributes.UPDATED_BY_LOGIN,
            MdmParamExcelAttributes.UPDATED_TS,
            MdmParamExcelAttributes.UPDATED_SOURCE,
            MdmParamExcelAttributes.EOX_SYNC
        );
        assertThat(file.getLastLine()).isZero();
    }

    @Test
    public void whenStringParamExistsShouldExportIt() {
        ShopSkuKey shopSkuKey = new ShopSkuKey(10, "Фонарик-0");
        long mdmParamId = 100500L;
        String paramName = "Длина луча в гигапарсеках";
        String stringValue = "14Гпк";

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setValueType(MdmParamValueType.STRING)
            .setTitle(paramName);

        SskuParamValue value = new SskuParamValue();
        value.setShopSkuKey(shopSkuKey)
            .setMdmParamId(mdmParamId)
            .setString(stringValue);

        CommonSsku commonSsku = new CommonSsku(shopSkuKey)
            .setBaseValues(List.of(value));

        ExcelFile file = service.exportExcelFile(List.of(param), List.of(commonSsku));
        assertThat(file.getHeaders()).containsExactly(
            MdmParamExcelAttributes.SUPPLIER_HEADER,
            MdmParamExcelAttributes.SHOP_SKU_HEADER,
            paramName,
            MdmParamExcelAttributes.UPDATED_BY_LOGIN,
            MdmParamExcelAttributes.UPDATED_TS,
            MdmParamExcelAttributes.UPDATED_SOURCE,
            MdmParamExcelAttributes.EOX_SYNC
        );
        assertThat(file.getLastLine()).isEqualTo(1);
        assertThat(file.getValuesList(file.getLastLine())).containsExactly(
            String.valueOf(shopSkuKey.getSupplierId()),
            shopSkuKey.getShopSku(),
            stringValue,
            NO_USER,
            value.getUpdatedTs().toString(),
            value.getMasterDataSourceType().toString(),
            ""
        );
    }

    @Test
    public void whenBoolParamExistsShouldExportIt() {
        ShopSkuKey shopSkuKey = new ShopSkuKey(60, "Кружка");
        long mdmParamId = 100500L;
        String paramName = "Замкнутая термодин. система";
        boolean boolValue = false;

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setValueType(MdmParamValueType.BOOL)
            .setTitle(paramName);

        SskuParamValue value = new SskuParamValue();
        value.setShopSkuKey(shopSkuKey)
            .setMdmParamId(mdmParamId)
            .setBool(boolValue);

        CommonSsku commonSsku = new CommonSsku(shopSkuKey)
            .setBaseValues(List.of(value));

        ExcelFile file = service.exportExcelFile(List.of(param), List.of(commonSsku));
        assertThat(file.getHeaders()).containsExactly(
            MdmParamExcelAttributes.SUPPLIER_HEADER,
            MdmParamExcelAttributes.SHOP_SKU_HEADER,
            paramName,
            MdmParamExcelAttributes.UPDATED_BY_LOGIN,
            MdmParamExcelAttributes.UPDATED_TS,
            MdmParamExcelAttributes.UPDATED_SOURCE,
            MdmParamExcelAttributes.EOX_SYNC
        );
        assertThat(file.getLastLine()).isEqualTo(1);
        assertThat(file.getValuesList(file.getLastLine())).containsExactly(
            String.valueOf(shopSkuKey.getSupplierId()),
            shopSkuKey.getShopSku(),
            "нет",
            NO_USER,
            value.getUpdatedTs().toString(),
            value.getMasterDataSourceType().toString(),
            ""
        );
    }

    @Test
    public void whenNumericParamExistsShouldExportIt() {
        ShopSkuKey shopSkuKey = new ShopSkuKey(61, "Моноколесо");
        long mdmParamId = 100500L;
        String paramName = "Продолжительность жизни (до тепловой смерти; порядковое значение)";
        BigDecimal numericValue = new BigDecimal("10.111");

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setValueType(MdmParamValueType.NUMERIC)
            .setTitle(paramName);

        SskuParamValue value = new SskuParamValue();
        value.setShopSkuKey(shopSkuKey)
            .setMdmParamId(mdmParamId)
            .setNumeric(numericValue);

        CommonSsku commonSsku = new CommonSsku(shopSkuKey)
            .setBaseValues(List.of(value));

        ExcelFile file = service.exportExcelFile(List.of(param), List.of(commonSsku));
        assertThat(file.getHeaders()).containsExactly(
            MdmParamExcelAttributes.SUPPLIER_HEADER,
            MdmParamExcelAttributes.SHOP_SKU_HEADER,
            paramName,
            MdmParamExcelAttributes.UPDATED_BY_LOGIN,
            MdmParamExcelAttributes.UPDATED_TS,
            MdmParamExcelAttributes.UPDATED_SOURCE,
            MdmParamExcelAttributes.EOX_SYNC
        );
        assertThat(file.getLastLine()).isEqualTo(1);
        assertThat(file.getValuesList(file.getLastLine())).containsExactly(
            String.valueOf(shopSkuKey.getSupplierId()),
            shopSkuKey.getShopSku(),
            numericValue.toPlainString(),
            NO_USER,
            value.getUpdatedTs().toString(),
            value.getMasterDataSourceType().toString(),
            ""
        );
    }

    @Test
    public void whenEnumParamExistsShouldExportIt() {
        ShopSkuKey shopSkuKey = new ShopSkuKey(11, "Вселенная");
        long mdmParamId = 100500L;
        long flrw = 600L;
        long lambdaCDM = 601L;
        long mond = 602L;
        long teves = 603L;

        String paramName = "Космологическая модель";

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setValueType(MdmParamValueType.ENUM)
            .setTitle(paramName);
        param.getExternals().setOptionRenders(Map.of(
            flrw, "FLRW",
            lambdaCDM, "ΛCDM",
            mond, "MOND",
            teves, "TeVeS"
        ));

        SskuParamValue value = new SskuParamValue();
        value.setShopSkuKey(shopSkuKey)
            .setMdmParamId(mdmParamId)
            .setOption(new MdmParamOption().setId(lambdaCDM));

        CommonSsku commonSsku = new CommonSsku(shopSkuKey)
            .setBaseValues(List.of(value));

        ExcelFile file = service.exportExcelFile(List.of(param), List.of(commonSsku));
        assertThat(file.getHeaders()).containsExactly(
            MdmParamExcelAttributes.SUPPLIER_HEADER,
            MdmParamExcelAttributes.SHOP_SKU_HEADER,
            paramName,
            MdmParamExcelAttributes.UPDATED_BY_LOGIN,
            MdmParamExcelAttributes.UPDATED_TS,
            MdmParamExcelAttributes.UPDATED_SOURCE,
            MdmParamExcelAttributes.EOX_SYNC
        );
        assertThat(file.getLastLine()).isEqualTo(1);
        assertThat(file.getValuesList(file.getLastLine())).containsExactly(
            String.valueOf(shopSkuKey.getSupplierId()),
            shopSkuKey.getShopSku(),
            "ΛCDM",
            NO_USER,
            value.getUpdatedTs().toString(),
            value.getMasterDataSourceType().name(),
            ""
        );
    }

    @Test
    public void whenMultivalueEnumStringNumericParamExistsShouldExportIt() {
        ShopSkuKey shopSkuKey = new ShopSkuKey(15, "Солнечная система");
        long enumMdmParamId = 100500L;
        long stringMdmParamId = 100501L;
        long numericMdmParamId = 100502L;

        String enumParamName = "Группы планет";
        String stringParamName = "Названия планет земной группы";
        String numericParamName = "Массы планет земной группы (в массах Земли)";

        //mdm params
        MdmParam enumParam = createMultivalueMdmParam(enumMdmParamId, enumParamName, MdmParamValueType.ENUM);
        List<MdmParamOption> options = List.of(
            new MdmParamOption().setId(1), new MdmParamOption().setId(2)
        );
        enumParam.setOptions(options);

        enumParam.getExternals().setOptionRenders(Map.of(
            1L, "Планеты земной группы",
            2L, "Планеты-гиганты"
        ));

        MdmParam stringParam = createMultivalueMdmParam(stringMdmParamId, stringParamName, MdmParamValueType.STRING);

        MdmParam numericParam = createMultivalueMdmParam(numericMdmParamId, numericParamName,
            MdmParamValueType.NUMERIC);

        //ssku values
        SskuParamValue enumValue = new SskuParamValue();
        enumValue.setShopSkuKey(shopSkuKey)
            .setMdmParamId(enumMdmParamId)
            .setOptions(options);

        SskuParamValue stringValue = new SskuParamValue();
        stringValue.setShopSkuKey(shopSkuKey)
            .setMdmParamId(stringMdmParamId)
            .setStrings(List.of("Меркурий", "Венера", "Земля", "Марс"));

        SskuParamValue numericValue = new SskuParamValue();
        numericValue.setShopSkuKey(shopSkuKey)
            .setMdmParamId(numericMdmParamId)
            .setNumerics(List.of(new BigDecimal("0.055274"), new BigDecimal("0.815"),
                new BigDecimal("1"), new BigDecimal("0.107")));

        CommonSsku commonSsku = new CommonSsku(shopSkuKey)
            .setBaseValues(List.of(enumValue, stringValue, numericValue));

        ExcelFile file = service.exportExcelFile(List.of(enumParam, stringParam, numericParam), List.of(commonSsku));
        assertThat(file.getHeaders()).containsExactly(
            MdmParamExcelAttributes.SUPPLIER_HEADER,
            MdmParamExcelAttributes.SHOP_SKU_HEADER,
            enumParamName,
            stringParamName,
            numericParamName,
            MdmParamExcelAttributes.UPDATED_BY_LOGIN,
            MdmParamExcelAttributes.UPDATED_TS,
            MdmParamExcelAttributes.UPDATED_SOURCE,
            MdmParamExcelAttributes.EOX_SYNC
        );
        assertThat(file.getLastLine()).isEqualTo(1);
        assertThat(file.getValuesList(file.getLastLine())).containsExactly(
            String.valueOf(shopSkuKey.getSupplierId()),
            shopSkuKey.getShopSku(),
            "Планеты земной группы,Планеты-гиганты",
            "Меркурий,Венера,Земля,Марс",
            "0.055274,0.815,1,0.107",
            NO_USER,
            numericValue.getUpdatedTs().toString(),
            numericValue.getMasterDataSourceType().name(),
            ""
        );
    }

    private static MdmParam createMultivalueMdmParam(long enumMdmParamId, String enumParamName,
                                                     MdmParamValueType valueType) {
        return new MdmParam()
            .setId(enumMdmParamId)
            .setValueType(valueType)
            .setTitle(enumParamName)
            .setMultivalue(true);
    }
}
