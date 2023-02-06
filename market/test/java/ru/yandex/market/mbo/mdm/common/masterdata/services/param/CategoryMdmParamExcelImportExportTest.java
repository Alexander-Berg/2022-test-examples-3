package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGoodGroupRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CustomsCommCodeRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.CCCodeValidationService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits.TimeUnit;
import ru.yandex.market.mboc.common.masterdata.model.cccode.CustomsCommCode;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.category.CategoryTree;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.TaskQueueRegistratorMock;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class CategoryMdmParamExcelImportExportTest extends MdmBaseDbTestClass {

    private CategoryCachingServiceMock categoryCachingService;

    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private CustomsCommCodeRepository customsCommCodeRepository;
    @Autowired
    private MdmGoodGroupRepository mdmGoodGroupRepository;

    private MdmParamCache mdmParamCache;
    private CategoryMdmParamExcelImportService importService;
    private CategoryMdmParamExcelExportService exportService;
    private MdmParamProviderMock mdmParamProvider;
    private CustomsCommCodeMarkupService customsCommCodeMarkupService;
    private TaskQueueRegistratorMock taskQueueRegistrator;

    @Before
    public void setup() {
        categoryParamValueRepository.deleteAll();
        categoryCachingService = new CategoryCachingServiceMock();

        mdmParamCache = TestMdmParamUtils.createParamCacheMock(TestMdmParamUtils.createDefaultKnownMdmParams());
        var allIds = mdmParamCache.getAll().stream().map(MdmParam::getId).collect(Collectors.toSet());
        mdmParamProvider = new MdmParamProviderMock(mdmParamCache);
        mdmParamProvider.markParamsAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, allIds);
        mdmParamProvider.markParamsAllowed(MdmParamIoType.CATEGORY_EXCEL_EXPORT, allIds);

        taskQueueRegistrator = new TaskQueueRegistratorMock();
        importService = new CategoryMdmParamExcelImportService(categoryParamValueRepository,
            taskQueueRegistrator, mdmParamProvider,
            mdmParamCache, new MappingsCacheRepositoryMock(),
            new StorageKeyValueServiceMock());
        exportService = new CategoryMdmParamExcelExportService(categoryCachingService,
            categoryParamValueRepository, mdmParamProvider);
        customsCommCodeMarkupService = new CustomsCommCodeMarkupServiceImpl(
            mdmParamCache, customsCommCodeRepository,
            new CCCodeValidationService(List.of(), customsCommCodeRepository),
            categoryParamValueRepository,
            new TaskQueueRegistratorMock(),
            mdmGoodGroupRepository,
            new MappingsCacheRepositoryMock());
    }

    @Test
    public void testExportAndImportWorkTogether() {
        long category1 = 123456L;
        long category2 = 323456L;

        // Засторим в БД ряд категорийных параметров для двух категорий
        var shelfLife1 = TestMdmParamUtils.createCategoryParamValue(
            KnownMdmParams.SHELF_LIFE,
            category1,
            null,
            4.0,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var shelfLife2 = TestMdmParamUtils.createCategoryParamValue(
            KnownMdmParams.SHELF_LIFE,
            category2,
            null,
            8.0,
            null,
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var shelfLifeUnit1 = TestMdmParamUtils.createCategoryParamValue(
            KnownMdmParams.SHELF_LIFE_UNIT,
            category1,
            null,
            null,
            null,
            new MdmParamOption().setId(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeUnit.DAY)),
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var shelfLifeUnit2 = TestMdmParamUtils.createCategoryParamValue(
            KnownMdmParams.SHELF_LIFE_UNIT,
            category2,
            null,
            null,
            null,
            new MdmParamOption().setId(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeUnit.HOUR)),
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var shelfLifeComment1 = TestMdmParamUtils.createCategoryParamValue(
            KnownMdmParams.SHELF_LIFE_COMMENT,
            category1,
            null,
            null,
            "lorem ipsum dolor sit amet",
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var shelfLifeComment2 = TestMdmParamUtils.createCategoryParamValue(
            KnownMdmParams.SHELF_LIFE_COMMENT,
            category2,
            null,
            null,
            "рыбатекст",
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var expirDatesApply1 = TestMdmParamUtils.createCategoryParamValue(
            KnownMdmParams.EXPIRATION_DATES_APPLY,
            category1,
            null,
            null,
            null,
            KnownMdmParams.EXPIRATION_DATES_REQUIRED_OPTION,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        var expirDatesApply2 = TestMdmParamUtils.createCategoryParamValue(
            KnownMdmParams.EXPIRATION_DATES_APPLY,
            category2,
            null,
            null,
            null,
            KnownMdmParams.EXPIRATION_DATES_MAY_USE_OPTION,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        categoryParamValueRepository.insertBatch(
            shelfLife1, shelfLife2,
            shelfLifeUnit1, shelfLifeUnit2,
            shelfLifeComment1, shelfLifeComment2,
            expirDatesApply1, expirDatesApply2
        );
        categoryCachingService.addCategory(category1, "Хорошие товары", CategoryTree.ROOT_CATEGORY_ID);
        categoryCachingService.addCategory(category2, "Отличные товары", CategoryTree.ROOT_CATEGORY_ID);

        // Экспортируем файл
        var excel = exportService.exportExcelFile();

        // Для чистоты эксперимента стираем в БД все настройки, чтобы потом импортнуть их заново.
        List<CategoryParamValue> expected = categoryParamValueRepository.findAll();
        categoryParamValueRepository.deleteAll();
        assertThat(categoryParamValueRepository.findAll()).isEmpty();

        // Импортируем
        List<String> errors = importService.importExcel("", ExcelFileConverter.convertToBytes(excel))
            .getErrors();
        assertThat(errors).isEmpty();

        // Проверим, что все параметры восстановились до прежнего состояния.
        assertThat(categoryParamValueRepository.findAll()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    public void whenAddCustomCommCode() {
        long categoryId = 123456L;
        CustomsCommCode customsCommCode = new CustomsCommCode().setGoodGroupId(mdmGoodGroupRepository.findAll().get(0)
            .getId()).setCode("123");
        customsCommCodeMarkupService.create(customsCommCode);
        var customCommCodeParam = TestMdmParamUtils.createCategoryParamValue(
            KnownMdmParams.CUSTOMS_COMM_CODE_PREFIX,
            categoryId,
            false,
            null,
            customsCommCode.getCode(),
            null,
            MasterDataSourceType.MDM_OPERATOR,
            Instant.now()
        );
        categoryParamValueRepository.insert(customCommCodeParam);
        categoryCachingService.addCategory(categoryId, "Хорошие товары", CategoryTree.ROOT_CATEGORY_ID);
        var excel = exportService.exportExcelFile();
        List<String> errors = importService.importExcel("", ExcelFileConverter.convertToBytes(excel))
            .getErrors();
        assertThat(errors).isEmpty();
        assertThat(taskQueueRegistrator.getTasks()).isNotEmpty();
    }
}
