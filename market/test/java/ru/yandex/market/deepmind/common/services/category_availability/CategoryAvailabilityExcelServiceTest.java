package ru.yandex.market.deepmind.common.services.category_availability;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.assertions.DeepmindAssertions;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.WarehouseUsingType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Warehouse;
import ru.yandex.market.deepmind.common.repository.category.CategoryAvailabilityFilter;
import ru.yandex.market.deepmind.common.repository.category.CategoryAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.category.CategorySettingsRepository;
import ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.services.availability.warehouse.AvailableWarehouseService;
import ru.yandex.market.deepmind.common.services.availability.warehouse.AvailableWarehouseServiceImpl;
import ru.yandex.market.deepmind.common.services.category.DeepmindCategoryCachingServiceMock;
import ru.yandex.market.deepmind.common.utils.YamlTestUtil;
import ru.yandex.market.mbo.excel.ExcelFile;

import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.AVAILABLE;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.AVAILABLE_INHERITED;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.NOT_AVAILABLE;
import static ru.yandex.market.deepmind.common.background.BaseBackgroundExportable.NOT_AVAILABLE_INHERITED;
import static ru.yandex.market.deepmind.common.services.category_availability.CategoryAvailabilityExcelService.CARGO_TYPES;
import static ru.yandex.market.deepmind.common.services.category_availability.CategoryAvailabilityExcelService.CATEGORY_ID;
import static ru.yandex.market.deepmind.common.services.category_availability.CategoryAvailabilityExcelService.CATEGORY_NAME;
import static ru.yandex.market.deepmind.common.services.category_availability.CategoryAvailabilityExcelService.DEPARTMENT_NAME;
import static ru.yandex.market.deepmind.common.services.category_availability.CategoryAvailabilityExcelService.FULL_PATH;
import static ru.yandex.market.deepmind.common.services.category_availability.CategoryAvailabilityExcelService.HEADERS;
import static ru.yandex.market.deepmind.common.services.category_availability.CategoryAvailabilityExcelService.SEASON;
import static ru.yandex.market.deepmind.common.utils.excel.ExcelUtils.convertWarehouseToExcelHeader;

/**
 * @author kravchenko-aa
 * @date 15.04.2020
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class CategoryAvailabilityExcelServiceTest extends DeepmindBaseDbTestClass {
    private static final Warehouse WAREHOUSE_1 = new Warehouse().setId(1L).setName("1 Свиблово")
        .setType(WarehouseType.FULFILLMENT).setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT);
    private static final Warehouse WAREHOUSE_2 = new Warehouse().setId(2L).setName("2 Склад из Индианы Джонса")
        .setType(WarehouseType.FULFILLMENT).setUsingType(WarehouseUsingType.USE_FOR_FULFILLMENT);

    @Autowired
    private DeepmindWarehouseRepository deepmindWarehouseRepository;
    @Autowired
    private SeasonRepository seasonRepository;
    @Autowired
    private CategorySettingsRepository categorySettingsRepository;
    @Autowired
    private CategoryAvailabilityMatrixRepository categoryAvailabilityMatrixRepository;

    private DeepmindCategoryCachingServiceMock deepmindCategoryCachingServiceMock;
    private CategoryAvailabilityExcelService categoryAvailabilityExcelService;
    private AvailableWarehouseService availableWarehouseService;

    @Before
    public void setup() {
        deepmindCategoryCachingServiceMock = new DeepmindCategoryCachingServiceMock();
        var categoryCargoTypeService = new CategoryCargoTypeService(deepmindCategoryCachingServiceMock);
        categoryAvailabilityExcelService = new CategoryAvailabilityExcelService(
            seasonRepository,
            deepmindCategoryCachingServiceMock, categorySettingsRepository, categoryAvailabilityMatrixRepository,
            categoryCargoTypeService);
        availableWarehouseService = new AvailableWarehouseServiceImpl(
            deepmindWarehouseRepository, WarehouseUsingType.USE_FOR_FULFILLMENT);
        deepmindWarehouseRepository.save(WAREHOUSE_1, WAREHOUSE_2);
        YamlTestUtil.readCategoriesFromResources("categories/category-tree.yml")
            .forEach(category -> deepmindCategoryCachingServiceMock.addCategory(category));
    }

    public void loadCategoryAvailabilities() {
        /*
         *  CATEGORY                     SVB  JNS
         *  root (90401)                 -    false
         *   -- cat1 (2)                 true false
         *       -- cat1_1 (3)           null false
         *           -- cat1_1_1 (4)     true null
         *       -- cat1_2 (5)           null null
         *   -- cat2 (6)                 null null
         *       -- cat2_1 (7)           null null
         */
        categoryAvailabilityMatrixRepository.save(
            categoryAvailabilityMatrix(90401L, WAREHOUSE_2, false),
            categoryAvailabilityMatrix(2L, WAREHOUSE_1, true),
            categoryAvailabilityMatrix(2L, WAREHOUSE_2, false),
            categoryAvailabilityMatrix(3L, WAREHOUSE_2, false),
            categoryAvailabilityMatrix(4L, WAREHOUSE_1, true)
        );
    }

    @Test
    public void exportShouldConvertAvailabilitiesToLines() {
        loadCategoryAvailabilities();
        ExcelFile file = categoryAvailabilityExcelService.exportToExcel(
            new CategoryAvailabilityFilter(), availableWarehouseService.getAvailableWarehouses());

        List<String> headers = new ArrayList<>(HEADERS);
        availableWarehouseService.getAvailableWarehouses()
            .forEach(warehouse -> headers.add(convertWarehouseToExcelHeader(warehouse)));
        DeepmindAssertions.assertThat(file)
            .containsHeaders(headers.toArray(new String[1]))
            .hasLastLine(6)
            .containsValue(1, CATEGORY_ID, 2)
            .containsValue(1, CATEGORY_NAME, "cat1")
            .containsValue(1, DEPARTMENT_NAME, "cat1")
            .containsValue(1, FULL_PATH, "cat1")
            .containsValue(1, CARGO_TYPES, "КГТ20: вычислять индивидуально; КГТ: вычислять индивидуально")
            .containsValue(1, convertWarehouseToExcelHeader(WAREHOUSE_1), AVAILABLE)
            .containsValue(1, convertWarehouseToExcelHeader(WAREHOUSE_2), NOT_AVAILABLE)

            .containsValue(2, CATEGORY_ID, 3)
            .containsValue(2, CATEGORY_NAME, "cat1_1")
            .containsValue(2, DEPARTMENT_NAME, "cat1")
            .containsValue(2, FULL_PATH, "cat1/cat1_1")
            .containsValue(2, CARGO_TYPES, "КГТ20: вычислять индивидуально; КГТ: вычислять индивидуально")
            .containsValue(2, convertWarehouseToExcelHeader(WAREHOUSE_1), AVAILABLE_INHERITED)
            .containsValue(2, convertWarehouseToExcelHeader(WAREHOUSE_2), NOT_AVAILABLE)

            .containsValue(3, CATEGORY_ID, 4)
            .containsValue(3, CATEGORY_NAME, "cat1_1_1")
            .containsValue(3, DEPARTMENT_NAME, "cat1")
            .containsValue(3, FULL_PATH, "cat1/cat1_1/cat1_1_1")
            .containsValue(3, CARGO_TYPES, "КГТ20: вычислять индивидуально; КГТ: вычислять индивидуально")
            .containsValue(3, convertWarehouseToExcelHeader(WAREHOUSE_1), AVAILABLE)
            .containsValue(3, convertWarehouseToExcelHeader(WAREHOUSE_2), NOT_AVAILABLE_INHERITED)

            .containsValue(4, CATEGORY_ID, 5)
            .containsValue(4, CATEGORY_NAME, "cat1_2")
            .containsValue(4, DEPARTMENT_NAME, "cat1")
            .containsValue(4, FULL_PATH, "cat1/cat1_2")
            .containsValue(4, CARGO_TYPES, "КГТ20: вычислять индивидуально; КГТ: вычислять индивидуально")
            .containsValue(4, convertWarehouseToExcelHeader(WAREHOUSE_1), AVAILABLE_INHERITED)
            .containsValue(4, convertWarehouseToExcelHeader(WAREHOUSE_2), NOT_AVAILABLE_INHERITED)

            .containsValue(5, CATEGORY_ID, 6)
            .containsValue(5, CATEGORY_NAME, "cat2")
            .containsValue(5, DEPARTMENT_NAME, "cat2")
            .containsValue(5, FULL_PATH, "cat2")
            .containsValue(5, CARGO_TYPES, "КГТ20: вычислять индивидуально; КГТ: вычислять индивидуально")
            .containsValue(5, convertWarehouseToExcelHeader(WAREHOUSE_1), AVAILABLE_INHERITED)
            .containsValue(5, convertWarehouseToExcelHeader(WAREHOUSE_2), NOT_AVAILABLE_INHERITED)

            .containsValue(6, CATEGORY_ID, 7)
            .containsValue(6, CATEGORY_NAME, "cat2_1")
            .containsValue(6, DEPARTMENT_NAME, "cat2")
            .containsValue(6, FULL_PATH, "cat2/cat2_1")
            .containsValue(6, CARGO_TYPES, "КГТ20: форсировать не КГТ20; КГТ: форсировать КГТ")
            .containsValue(6, convertWarehouseToExcelHeader(WAREHOUSE_1), AVAILABLE_INHERITED)
            .containsValue(6, convertWarehouseToExcelHeader(WAREHOUSE_2), NOT_AVAILABLE_INHERITED);
    }

    @Test
    public void testExportWithoutSeasons() {
        loadCategoryAvailabilities();
        ExcelFile file = categoryAvailabilityExcelService.exportToExcel(
            new CategoryAvailabilityFilter().setWithSeasons(false),
            availableWarehouseService.getAvailableWarehouses());

        List<String> headers = new ArrayList<>(HEADERS);
        headers.remove(SEASON);
        availableWarehouseService.getAvailableWarehouses()
            .forEach(warehouse -> headers.add(convertWarehouseToExcelHeader(warehouse)));
        DeepmindAssertions.assertThat(file).containsHeaders(headers.toArray(new String[1]));
    }

    @Test
    public void exportShouldAddProperFormatting() {
        loadCategoryAvailabilities();
        ExcelFile fileWithSeasons = categoryAvailabilityExcelService.exportToExcel(
            new CategoryAvailabilityFilter(),
            availableWarehouseService.getAvailableWarehouses());
        ExcelFile fileWithoutSeasons = categoryAvailabilityExcelService.exportToExcel(
            new CategoryAvailabilityFilter().setWithSeasons(false),
            availableWarehouseService.getAvailableWarehouses());
        DeepmindAssertions.assertThat(fileWithSeasons)
            .containsFormattingForRange(List.of("H2:H7", "I2:I7"));
        DeepmindAssertions.assertThat(fileWithoutSeasons)
            .containsFormattingForRange(List.of("H2:H7", "G2:G7"));
    }

    @Test
    public void exportShouldApplyFilter() {
        loadCategoryAvailabilities();
        ExcelFile file = categoryAvailabilityExcelService.exportToExcel(
            new CategoryAvailabilityFilter().setCategoryIds(List.of(2L, 5L)),
            availableWarehouseService.getAvailableWarehouses());

        List<String> headers = new ArrayList<>(HEADERS);
        availableWarehouseService.getAvailableWarehouses()
            .forEach(warehouse -> headers.add(convertWarehouseToExcelHeader(warehouse)));
        DeepmindAssertions.assertThat(file)
            .containsHeaders(headers.toArray(new String[1]))
            .hasLastLine(2)
            .containsValue(1, CATEGORY_ID, 2)
            .containsValue(1, CATEGORY_NAME, "cat1")
            .containsValue(1, DEPARTMENT_NAME, "cat1")
            .containsValue(1, FULL_PATH, "cat1")
            .containsValue(1, convertWarehouseToExcelHeader(WAREHOUSE_1), AVAILABLE)
            .containsValue(1, convertWarehouseToExcelHeader(WAREHOUSE_2), NOT_AVAILABLE)

            .containsValue(2, CATEGORY_ID, 5)
            .containsValue(2, CATEGORY_NAME, "cat1_2")
            .containsValue(2, DEPARTMENT_NAME, "cat1")
            .containsValue(2, FULL_PATH, "cat1/cat1_2")
            .containsValue(2, convertWarehouseToExcelHeader(WAREHOUSE_1), AVAILABLE_INHERITED)
            .containsValue(2, convertWarehouseToExcelHeader(WAREHOUSE_2), NOT_AVAILABLE_INHERITED);
    }

    @Test
    public void exportShouldIgnoreUnpublishedCategories() {
        loadCategoryAvailabilities();
        deepmindCategoryCachingServiceMock.getCategory(2).get().setPublished(false);

        ExcelFile file = categoryAvailabilityExcelService.exportToExcel(
            new CategoryAvailabilityFilter().setCategoryIds(List.of(2L, 5L)),
            availableWarehouseService.getAvailableWarehouses());

        DeepmindAssertions.assertThat(file)
            .hasLastLine(1)
            .containsValue(1, CATEGORY_ID, 5)
            .containsValue(1, CATEGORY_NAME, "cat1_2")
            .containsValue(1, DEPARTMENT_NAME, "cat1");
    }

    public CategoryAvailabilityMatrix categoryAvailabilityMatrix(Long categoryId,
                                                                 Warehouse warehouse,
                                                                 boolean available) {
        return new CategoryAvailabilityMatrix()
            .setCategoryId(categoryId)
            .setAvailable(available)
            .setWarehouseId(warehouse.getId());
    }
}
