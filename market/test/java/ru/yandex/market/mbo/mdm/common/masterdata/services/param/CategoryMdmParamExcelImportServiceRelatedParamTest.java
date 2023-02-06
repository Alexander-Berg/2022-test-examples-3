package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValueType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.TaskQueueRegistratorMock;

import static org.assertj.core.api.Assertions.assertThat;

public class CategoryMdmParamExcelImportServiceRelatedParamTest extends MdmBaseDbTestClass {
    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MdmParamCache mdmParamCache;

    private CategoryMdmParamExcelImportService service;
    private MdmParamProviderMock mdmParamProvider;
    private StorageKeyValueService keyValueService;

    @Before
    public void setup() {
        categoryParamValueRepository.deleteAll();
        mdmParamProvider = new MdmParamProviderMock(mdmParamCache);
        keyValueService = new StorageKeyValueServiceMock();
        service = new CategoryMdmParamExcelImportService(categoryParamValueRepository,
            new TaskQueueRegistratorMock(), mdmParamProvider, mdmParamCache, mappingsCacheRepository, keyValueService);
    }

    @Test
    public void whenOneOfMandatoryBothRelatedNotSetShouldReturnError() {
        String paramTitle1 = "Мин. срок годности (величина)";
        long mdmParamId1 = KnownMdmParams.MIN_LIMIT_SHELF_LIFE;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId1);

        String paramTitle2 = "Мин. срок годности (единица)";
        long mdmParamId2 = KnownMdmParams.MIN_LIMIT_SHELF_LIFE_UNIT;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId2);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle1);
        file.addHeader(paramTitle2);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "10", "");
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "", "дни");

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).containsExactlyInAnyOrder(
            "Ключ 959687, параметр \"Мин. срок годности (величина)\": отсутствует значение связанного параметра "
                + "\"Мин. срок годности (единица)\"",
            "Ключ 959688, параметр \"Мин. срок годности (единица)\": отсутствует значение связанного параметра "
                + "\"Мин. срок годности (величина)\"");
    }

    @Test
    public void whenBothOfMandatoryBothRelatedHasValueShouldReturnSuccessAndUpdateValues() {
        String paramTitle1 = "Мин. срок годности (величина)";
        String paramXlsName1 = "minLimitShelfLife";
        long mdmParamId1 = KnownMdmParams.MIN_LIMIT_SHELF_LIFE;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId1);

        String paramTitle2 = "Мин. срок годности (единица)";
        String paramXlsName2 = "minLimitShelfLife_Unit";
        long mdmParamId2 = KnownMdmParams.MIN_LIMIT_SHELF_LIFE_UNIT;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId2);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle1);
        file.addHeader(paramTitle2);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "10", "дни");
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "100", "годы");
        List<String> errors = service.importExcel("", bytes(file)).getErrors();

        assertThat(errors).isEmpty();

        MdmParam param1 = new MdmParam()
            .setId(mdmParamId1)
            .setTitle(paramTitle1)
            .setXslName(paramXlsName1)
            .setValueType(MdmParamValueType.NUMERIC);
        MdmParam param2 = new MdmParam()
            .setId(mdmParamId2)
            .setTitle(paramTitle2)
            .setXslName(paramXlsName2)
            .setValueType(MdmParamValueType.ENUM);
        param2.getExternals()
            .setOptionRenders(Map.of(
                1L, "часы",
                2L, "годы",
                3L, "дни",
                4L, "месяцы",
                5L, "недели"
            ));

        CategoryParamValue stripedSocksValue1 = value(959687, param1, new BigDecimal("10"));
        CategoryParamValue stripedSocksValue2 = value(959687, param2, "дни");
        CategoryParamValue hairedSocksValue1 = value(959688, param1, new BigDecimal("100"));
        CategoryParamValue hairedSocksValue2 = value(959688, param2, "годы");

        assertThat(categoryParamValueRepository.findAll()).containsExactlyInAnyOrder(
            stripedSocksValue1,
            stripedSocksValue2,
            hairedSocksValue1,
            hairedSocksValue2
        );
    }

    @Test
    public void whenOneParamsExcludedAndOtherNotSetOfMandatoryBothRelatedShouldIgnoreThemAndSaveOthers() {
        String paramTitle1 = "Мин. срок годности (величина)";
        String paramXlsName1 = "minLimitShelfLife";
        long mdmParamId1 = KnownMdmParams.MIN_LIMIT_SHELF_LIFE;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId1);

        String paramTitle2excluded = "Мин. срок годности (единица)";
        String paramXlsName2excluded = "minLimitShelfLife_Unit";
        long mdmParamId2excluded = KnownMdmParams.MIN_LIMIT_SHELF_LIFE_UNIT;

        MdmParam param3 = getAnyParamOfType(MdmParamValueType.NUMERIC);
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, param3.getId());

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle1);
        file.addHeader(paramTitle2excluded);
        file.addHeader(param3.getTitle());
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "", "дни", "100");
        List<String> errors = service.importExcel("", bytes(file)).getErrors();

        assertThat(errors).isEmpty();

        CategoryParamValue stripedSocksValue = value(959687, param3, new BigDecimal("100"));

        assertThat(categoryParamValueRepository.findAll()).containsExactlyInAnyOrder(
            stripedSocksValue
        );
    }

    @Test
    public void whenOneParamsExcludedAndOtherHasValueOfMandatoryBothRelatedShouldReturnError() {
        String paramTitle1 = "Мин. срок годности (величина)";
        long mdmParamId1 = KnownMdmParams.MIN_LIMIT_SHELF_LIFE;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId1);

        String paramTitle2excluded = "Мин. срок годности (единица)";
        String paramXlsName2excluded = "minLimitShelfLife_Unit";
        long mdmParamId2excluded = KnownMdmParams.MIN_LIMIT_SHELF_LIFE_UNIT;

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle1);
        file.addHeader(paramTitle2excluded);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "10", "дни");
        List<String> errors = service.importExcel("", bytes(file)).getErrors();

        assertThat(errors).containsExactlyInAnyOrder(
            "Ключ 959687, параметр \"Мин. срок годности (величина)\": отсутствует значение связанного параметра "
                + "\"Мин. срок годности (единица)\"");
    }

    private byte[] bytes(ExcelFile.Builder file) {
        return ExcelFileConverter.convertToBytes(file.build());
    }

    private CategoryParamValue value(long categoryId, MdmParam param, BigDecimal numeric) {
        return (CategoryParamValue) new CategoryParamValue().setCategoryId(categoryId)
            .setXslName(param.getXslName())
            .setNumeric(numeric)
            .setMdmParamId(param.getId())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
    }

    private CategoryParamValue value(long categoryId, MdmParam param, String option) {
        return (CategoryParamValue) new CategoryParamValue().setCategoryId(categoryId)
            .setXslName(param.getXslName())
            .setOption(param.getExternals().getOptionByRender(option))
            .setMdmParamId(param.getId())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
    }

    private MdmParam getAnyParamOfType(MdmParamValueType type) {
        return mdmParamCache.getAll().stream()
            .filter(it -> it.getValueType() == type)
            .findAny()
            .orElseThrow();
    }
}
