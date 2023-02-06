package ru.yandex.market.marketpromo.core.data.excel;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.data.excel.model.AssortmentRow;
import ru.yandex.market.marketpromo.core.service.impl.CategoryTreeCache;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.generator.DatacampOfferPromoMechanics;
import ru.yandex.market.marketpromo.core.test.generator.LocalOfferPromoMechanics;
import ru.yandex.market.marketpromo.core.test.generator.Offers;
import ru.yandex.market.marketpromo.core.test.generator.Promos;
import ru.yandex.market.marketpromo.core.utils.RequestContextUtils;
import ru.yandex.market.marketpromo.model.CategoryTree;
import ru.yandex.market.marketpromo.model.LocalOffer;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.PromoStatus;
import ru.yandex.market.marketpromo.model.WarehouseFeedKey;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.marketpromo.core.data.excel.mapper.AssortmentRowMapper.assortmentRowsFromDirectDiscountOffer;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.utils.ExcelSheetMatcher.CellMatcher.row;
import static ru.yandex.market.marketpromo.core.test.utils.ExcelSheetMatcher.CellMatcher.rows;
import static ru.yandex.market.marketpromo.core.test.utils.ExcelSheetMatcher.CellMatcher.textCell;
import static ru.yandex.market.marketpromo.core.test.utils.ExcelSheetMatcher.sheetWithCells;

public class ExcelBuilderTest extends ServiceTestBase {

    public static final String categoryKitchen = "Кухня";
    public static final String categoryTechnics = "Техника";
    private final static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Autowired
    private ExcelBuilder excelBuilder;
    private CategoryTreeCache categoryTreeCache;

    static {
        RequestContextUtils.setupContext(Map.of());
    }

    @BeforeEach
    void setUp() {
        categoryTreeCache = mock(CategoryTreeCache.class);
        CategoryTree categoryTree = mock(CategoryTree.class);
        when(categoryTree.getNameById(eq(1L))).thenReturn(categoryKitchen);
        when(categoryTree.getNameById(eq(2L))).thenReturn(categoryTechnics);
        when(categoryTreeCache.get()).thenReturn(categoryTree);
    }

    @Test
    void shouldExportEmptyAssortment() throws IllegalAccessException, IOException, InvocationTargetException {
        Promo promo = Promos.promo(
                Promos.promoId("#21098"),
                Promos.promoName("Чёрная пятница"),
                Promos.starts(LocalDate.parse("23.09.2020", DATE_FORMAT).atStartOfDay()),
                Promos.ends(LocalDate.parse("01.10.2020", DATE_FORMAT).atStartOfDay()),
                Promos.deadline(LocalDate.parse("17.09.2020", DATE_FORMAT).atStartOfDay()),
                Promos.status(PromoStatus.READY),
                Promos.categories(1L, 2L),
                Promos.directDiscount(
                        minimalDiscountPercentSize(13)
                )
        );

        byte[] exportResult = excelBuilder.buildExcelAssortmentExport(
                promo, Collections.emptyList(), Collections.emptyList());

        checkPromoSheet(promo, exportResult);

        assertThat("Assortment sheet export failed. ", exportResult,
                sheetWithCells(1, rows(
                        row(0,
                                textCell(AssortmentRow.C_PARTICIPATION, equalTo("Участвует")),
                                textCell(AssortmentRow.C_MSKU, equalTo("msku")),
                                textCell(AssortmentRow.C_CURRENT_PERCENT_SIZE, equalTo("Текущий % скидки"))
                        ))));
    }

    @Test
    void shouldExportDirectDiscountAssortment() throws InvocationTargetException, IllegalAccessException, IOException {
        Promo ddPromo = Promos.promo(
                Promos.promoId("#21098"),
                Promos.promoName("Чёрная пятница"),
                Promos.starts(LocalDate.parse("23.09.2020", DATE_FORMAT).atStartOfDay()),
                Promos.ends(LocalDate.parse("01.10.2020", DATE_FORMAT).atStartOfDay()),
                Promos.deadline(LocalDate.parse("17.09.2020", DATE_FORMAT).atStartOfDay()),
                Promos.status(PromoStatus.READY),
                Promos.category(1L, 30),
                Promos.category(2L, 10),
                Promos.directDiscount(
                        minimalDiscountPercentSize(13)
                )
        );

        LocalOffer ddOfferFirst = Offers.localOffer(
                Offers.shopSku("qxr.215928"),
                Offers.marketSku(526781L),
                Offers.name("Зубочистка"),
                Offers.categoryId(1L),
                Offers.basePrice(150),
                Offers.price(120),
                Offers.promo(
                        LocalOfferPromoMechanics.directDiscount(ddPromo.getId(),
                                DatacampOfferPromoMechanics.basePrice(BigDecimal.valueOf(200)),
                                DatacampOfferPromoMechanics.price(BigDecimal.valueOf(120))
                        ).participation(true).build()
                ),
                Offers.stocksByWarehouse(new WarehouseFeedKey(111L, 1111L), 999L),
                Offers.stocksByWarehouse(new WarehouseFeedKey(222L, 4444L), 2999L)
        );

        LocalOffer ddOfferSecond = Offers.localOffer(
                Offers.shopSku("etu.200327"),
                Offers.marketSku(190320L),
                Offers.name("Шуруповёрт"),
                Offers.categoryId(2L),
                Offers.basePrice(500),
                Offers.price(400),
                Offers.promo(
                        LocalOfferPromoMechanics.directDiscount(ddPromo.getId(),
                                DatacampOfferPromoMechanics.basePrice(BigDecimal.valueOf(500)),
                                DatacampOfferPromoMechanics.price(BigDecimal.valueOf(200))
                        ).build()
                ),
                Offers.stocksByWarehouse(new WarehouseFeedKey(111L, 1111L), 12999L),
                Offers.stocksByWarehouse(new WarehouseFeedKey(333L, 5555L), 5999L)
        );

        final List<WarehouseFeedKey> warehouseKeys = List.of(
                new WarehouseFeedKey(123L, 12L),
                new WarehouseFeedKey(111L, 1111L),
                new WarehouseFeedKey(222L, 4444L),
                new WarehouseFeedKey(333L, 5555L)
        );

        final List<WarehouseFeedKey> warehouseKeysStreaming = new ArrayList<>();

        final List<AssortmentRow> assortmentRows = assortmentRowsFromDirectDiscountOffer(
                ddPromo, List.of(ddOfferFirst, ddOfferSecond), categoryTreeCache, warehouseKeys);
        byte[] exportResult = excelBuilder.buildExcelAssortmentExport(ddPromo, assortmentRows, warehouseKeys);
        SXSSFWorkbook workbook = new SXSSFWorkbook();
        SXSSFSheet sheet = workbook.createSheet(ExcelUtils.getSheetName(AssortmentRow.class));
        sheet.trackAllColumnsForAutoSizing();
        sheet.createRow(0);
        excelBuilder.buildStreamingAssortmentSheet(assortmentRows, workbook, sheet, warehouseKeysStreaming);

        checkPromoSheet(ddPromo, exportResult);

        assertThat("Assortment sheet export failed. ", exportResult,
                sheetWithCells(1, rows(
                        row(0,
                                textCell(AssortmentRow.C_PARTICIPATION, equalTo("Участвует")),
                                textCell(AssortmentRow.C_MSKU, equalTo("msku")),
                                textCell(AssortmentRow.C_CURRENT_PERCENT_SIZE, equalTo("Текущий % скидки")),
                                textCell(AssortmentRow.C_WAREHOUSE_STOCK_FIRST, equalTo("Склад 123")),
                                textCell(AssortmentRow.C_WAREHOUSE_STOCK_FIRST + 1, equalTo("Склад 111")),
                                textCell(AssortmentRow.C_WAREHOUSE_STOCK_FIRST + 2, equalTo("Склад 222")),
                                textCell(AssortmentRow.C_WAREHOUSE_STOCK_FIRST + 3, equalTo("Склад 333"))
                        ),
                        row(1,
                                textCell(AssortmentRow.C_PARTICIPATION, equalTo("ДА")),
                                textCell(AssortmentRow.C_SSKU, equalTo(ddOfferFirst.getShopSku())),
                                textCell(AssortmentRow.C_CATEGORY, equalTo(categoryKitchen)),
                                textCell(AssortmentRow.C_WAREHOUSE_STOCK_FIRST, equalTo("1.0")),
                                textCell(AssortmentRow.C_WAREHOUSE_STOCK_FIRST + 1, equalTo("999.0")),
                                textCell(AssortmentRow.C_WAREHOUSE_STOCK_FIRST + 2, equalTo("2999.0")),
                                textCell(AssortmentRow.C_WAREHOUSE_STOCK_FIRST + 3, equalTo("0.0"))
                        ),
                        row(2,
                                textCell(AssortmentRow.C_PARTICIPATION, equalTo("НЕТ")),
                                textCell(AssortmentRow.C_CATEGORY, equalTo(categoryTechnics)),
                                textCell(AssortmentRow.C_CURRENT_PERCENT_SIZE, equalTo("20.0")),
                                textCell(AssortmentRow.C_WAREHOUSE_STOCK_FIRST, equalTo("1.0")),
                                textCell(AssortmentRow.C_WAREHOUSE_STOCK_FIRST + 1, equalTo("12999.0")),
                                textCell(AssortmentRow.C_WAREHOUSE_STOCK_FIRST + 2, equalTo("0.0")),
                                textCell(AssortmentRow.C_WAREHOUSE_STOCK_FIRST + 3, equalTo("5999.0"))
                        ))));
    }

    private void checkPromoSheet(Promo promo, byte[] exportResult) {
        assertThat("Promo sheet export failed. ", exportResult,
                sheetWithCells(0, rows(
                        row(0,
                                textCell(0, equalTo("ID акции")),
                                textCell(1, equalTo("Название")),
                                textCell(2, equalTo("Дата начала")),
                                textCell(3, equalTo("Дата окончания")),
                                textCell(4, equalTo("Дедлайн")),
                                textCell(5, equalTo("Механика"))
                        ),
                        row(1,
                                textCell(0, equalTo(promo.getPromoId())),
                                textCell(1, equalTo(promo.getName())),
                                textCell(2, equalTo(promo.getStartDate().toLocalDate().format(DATE_FORMAT))),
                                textCell(3, equalTo(promo.getEndDate().toLocalDate().format(DATE_FORMAT))),
                                textCell(4, equalTo(promo.getDeadlineAt().toLocalDate().format(DATE_FORMAT))),
                                textCell(5, equalTo(promo.getMechanicsType().getTranslation()))
                        )
                        )
                )
        );
    }


}
