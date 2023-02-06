package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValueType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepositoryMock;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.CategoryTree;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class CategoryMdmParamExcelExportServiceTest {
    private static final String NO_USER = "";
    private CategoryMdmParamExcelExportService service;
    private CategoryParamValueRepositoryMock categoryParamValueRepository;
    private CategoryCachingServiceMock categoryCachingService;
    private MdmParamProviderMock mdmParamProvider;
    private MdmParamCache paramCache;

    @Before
    public void setup() {
        paramCache = TestMdmParamUtils.createParamCacheMock();
        mdmParamProvider = new MdmParamProviderMock(paramCache);
        categoryParamValueRepository = new CategoryParamValueRepositoryMock();
        categoryCachingService = new CategoryCachingServiceMock();
        service = new CategoryMdmParamExcelExportService(categoryCachingService,
            categoryParamValueRepository, mdmParamProvider);
    }

    @Test
    public void whenNoParamsShouldYieldEmptyExcel() {
        ExcelFile file = service.exportExcelFile();
        assertThat(file.getHeaders()).containsExactly(
            MdmParamExcelAttributes.HID_HEADER,
            MdmParamExcelAttributes.NAME_HEADER,
            MdmParamExcelAttributes.UPDATED_BY_LOGIN,
            MdmParamExcelAttributes.UPDATED_TS,
            MdmParamExcelAttributes.UPDATED_SOURCE
        );
        assertThat(file.getLastLine()).isZero();
    }

    @Test
    public void whenStringParamExistsShouldExportIt() {
        long categoryId = 12L;
        MdmParam stringParam = paramCache.getAll().stream()
            .filter(it -> it.getValueType() == MdmParamValueType.STRING)
            .findAny().orElseThrow();
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_EXPORT, stringParam.getId());

        CategoryParamValue value = new CategoryParamValue();
        value.setCategoryId(categoryId)
            .setMdmParamId(stringParam.getId())
            .setString("14Гпк");
        categoryParamValueRepository.insert(value);

        categoryCachingService.addCategory(categoryId, "Карманные вселенные", CategoryTree.ROOT_CATEGORY_ID);

        ExcelFile file = service.exportExcelFile();
        assertThat(file.getHeaders()).containsExactly(
            MdmParamExcelAttributes.HID_HEADER,
            MdmParamExcelAttributes.NAME_HEADER,
            stringParam.getTitle(),
            MdmParamExcelAttributes.UPDATED_BY_LOGIN,
            MdmParamExcelAttributes.UPDATED_TS,
            MdmParamExcelAttributes.UPDATED_SOURCE
        );
        assertThat(file.getLastLine()).isEqualTo(1);
        assertThat(file.getValuesList(file.getLastLine())).containsExactly(
            String.valueOf(categoryId),
            "Карманные вселенные",
            "14Гпк",
            NO_USER,
            value.getUpdatedTs().toString(),
            value.getMasterDataSourceType().name()
        );
    }

    @Test
    public void whenBoolParamExistsShouldExportIt() {
        long categoryId = 12L;
        MdmParam boolParam = paramCache.getAll().stream()
            .filter(it -> it.getValueType() == MdmParamValueType.MBO_BOOL)
            .findAny().orElseThrow();
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_EXPORT, boolParam.getId());

        CategoryParamValue value = new CategoryParamValue();
        value.setCategoryId(categoryId)
            .setMdmParamId(boolParam.getId())
            .setBool(false);
        categoryParamValueRepository.insert(value);

        categoryCachingService.addCategory(categoryId, "Карманные вселенные", CategoryTree.ROOT_CATEGORY_ID);

        ExcelFile file = service.exportExcelFile();
        assertThat(file.getHeaders()).containsExactly(
            MdmParamExcelAttributes.HID_HEADER,
            MdmParamExcelAttributes.NAME_HEADER,
            boolParam.getTitle(),
            MdmParamExcelAttributes.UPDATED_BY_LOGIN,
            MdmParamExcelAttributes.UPDATED_TS,
            MdmParamExcelAttributes.UPDATED_SOURCE
        );
        assertThat(file.getLastLine()).isEqualTo(1);
        assertThat(file.getValuesList(file.getLastLine())).containsExactly(
            String.valueOf(categoryId),
            "Карманные вселенные",
            "нет",
            NO_USER,
            value.getUpdatedTs().toString(),
            value.getMasterDataSourceType().name()
        );
    }

    @Test
    public void whenNumericParamExistsShouldExportIt() {
        long categoryId = 12L;
        MdmParam numericParam = paramCache.getAll().stream()
            .filter(it -> it.getValueType() == MdmParamValueType.NUMERIC)
            .findAny().orElseThrow();
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_EXPORT, numericParam.getId());

        CategoryParamValue value = new CategoryParamValue();
        value.setCategoryId(categoryId)
            .setMdmParamId(numericParam.getId())
            .setNumeric(new BigDecimal(100));
        categoryParamValueRepository.insert(value);

        categoryCachingService.addCategory(categoryId, "Карманные вселенные", CategoryTree.ROOT_CATEGORY_ID);

        ExcelFile file = service.exportExcelFile();
        assertThat(file.getHeaders()).containsExactly(
            MdmParamExcelAttributes.HID_HEADER,
            MdmParamExcelAttributes.NAME_HEADER,
            numericParam.getTitle(),
            MdmParamExcelAttributes.UPDATED_BY_LOGIN,
            MdmParamExcelAttributes.UPDATED_TS,
            MdmParamExcelAttributes.UPDATED_SOURCE
        );
        assertThat(file.getLastLine()).isEqualTo(1);
        assertThat(file.getValuesList(file.getLastLine())).containsExactly(
            String.valueOf(categoryId),
            "Карманные вселенные",
            "100",
            NO_USER,
            value.getUpdatedTs().toString(),
            value.getMasterDataSourceType().name()
        );
    }

    @Test
    public void whenEnumParamExistsShouldExportIt() {
        long categoryId = 12L;
        MdmParam enumParam = paramCache.getAll().stream()
            .filter(it -> it.getValueType() == MdmParamValueType.MBO_ENUM)
            .findAny().orElseThrow();
        long someEnumOption = enumParam.getExternals().getOptionRenders().keySet().stream()
            .findAny()
            .orElseThrow();
        var optionName = enumParam.getExternals().getOption(someEnumOption);

        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_EXPORT, enumParam.getId());

        CategoryParamValue value = new CategoryParamValue();
        value.setCategoryId(categoryId)
            .setMdmParamId(enumParam.getId())
            .setOption(new MdmParamOption().setId(someEnumOption));
        categoryParamValueRepository.insert(value);

        categoryCachingService.addCategory(categoryId, "Карманные вселенные", CategoryTree.ROOT_CATEGORY_ID);

        ExcelFile file = service.exportExcelFile();
        assertThat(file.getHeaders()).containsExactly(
            MdmParamExcelAttributes.HID_HEADER,
            MdmParamExcelAttributes.NAME_HEADER,
            enumParam.getTitle(),
            MdmParamExcelAttributes.UPDATED_BY_LOGIN,
            MdmParamExcelAttributes.UPDATED_TS,
            MdmParamExcelAttributes.UPDATED_SOURCE
        );
        assertThat(file.getLastLine()).isEqualTo(1);
        assertThat(file.getValuesList(file.getLastLine())).containsExactly(
            String.valueOf(categoryId),
            "Карманные вселенные",
            optionName.getRenderedValue(),
            NO_USER,
            value.getUpdatedTs().toString(),
            value.getMasterDataSourceType().name()
        );
    }

    @Test
    public void whenSomeParamExcludedShouldExportOthers() {
        long categoryId = 12L;
        MdmParam showedParam = paramCache.getAll().stream()
            .filter(it -> it.getValueType() == MdmParamValueType.STRING)
            .findAny().orElseThrow();
        MdmParam excludedParam = paramCache.getAll().stream()
            .filter(it -> it.getValueType() == MdmParamValueType.NUMERIC)
            .findAny().orElseThrow();
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_EXPORT, showedParam.getId());

        CategoryParamValue value = new CategoryParamValue();
        value.setCategoryId(categoryId)
            .setMdmParamId(showedParam.getId())
            .setString("14Гпк");
        categoryParamValueRepository.insert(value);
        CategoryParamValue excludedValue = new CategoryParamValue();
        excludedValue.setCategoryId(categoryId)
            .setMdmParamId(excludedParam.getId())
            .setNumeric(new BigDecimal("4.9"));
        categoryParamValueRepository.insert(excludedValue);

        categoryCachingService.addCategory(categoryId, "Карманные вселенные", CategoryTree.ROOT_CATEGORY_ID);

        ExcelFile file = service.exportExcelFile();
        assertThat(file.getHeaders()).containsExactly(
            MdmParamExcelAttributes.HID_HEADER,
            MdmParamExcelAttributes.NAME_HEADER,
            showedParam.getTitle(),
            MdmParamExcelAttributes.UPDATED_BY_LOGIN,
            MdmParamExcelAttributes.UPDATED_TS,
            MdmParamExcelAttributes.UPDATED_SOURCE
        );
        assertThat(file.getLastLine()).isEqualTo(1);
        assertThat(file.getValuesList(file.getLastLine())).containsExactly(
            String.valueOf(categoryId),
            "Карманные вселенные",
            "14Гпк",
            NO_USER,
            value.getUpdatedTs().toString(),
            value.getMasterDataSourceType().name()
        );
    }
}
