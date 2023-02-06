package ru.yandex.market.abo.tms.resupply;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;

import ru.yandex.market.abo.core.category.CategoryService;
import ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr;
import ru.yandex.market.abo.core.resupply.stock.ResupplyQualityMatrixService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.DEFORMED;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.MISSING_PARTS;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.MISSING_PARTS_CRITICAL;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.PACKAGE_CONTAMINATION;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.PACKAGE_HOLES;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.PACKAGE_JAMS;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.PACKAGE_OPENED;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.PACKAGE_SCRATCHES;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.WAS_USED;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.WRONG_OR_DAMAGED_PAPERS;

/**
 * @author Victor Nazarov &lt;sviperll@yandex-team.ru&gt;
 */
@ParametersAreNonnullByDefault
class ResupplyQualityMatrixImportServiceTest {

    private static final String TEST_CSV_RESOURCE_PATH =
            "/ru/yandex/market/abo/tms/resupply/test-resupply-quality-attributes.csv";

    @Test
    void smokeTest() {
        CategoryTreeNode categories =
                CategoryTreeNode.of(3, "Все товары", List.of(
                        CategoryTreeNode.of(4, "Продукты", List.of(
                                CategoryTreeNode.of(5, "Безалкогольное пиво и вино")
                        )),
                        CategoryTreeNode.of(6, "Электроника", List.of(
                                CategoryTreeNode.of(7, "Портативная техника", List.of(
                                        CategoryTreeNode.of(8, "Аксессуары для наушников и гарнитур")
                                ))
                        )),
                        CategoryTreeNode.of(9, "Бытовая техника", List.of(
                                CategoryTreeNode.of(10, "Мелкая техника для кухни", List.of(
                                        CategoryTreeNode.of(11, "Приготовление напитков", List.of(
                                                CategoryTreeNode.of(12, "Аксессуары для кофемашин и кофеварок")
                                        ))
                                )),
                                CategoryTreeNode.of(13, "Техника для красоты", List.of(
                                        CategoryTreeNode.of(14, "Приборы для ухода за телом"),
                                        CategoryTreeNode.of(15, "Уход за полостью рта", List.of(
                                                CategoryTreeNode.of(16, "Аксессуары для зубных щеток и ирригаторов")
                                        ))
                                ))
                        ))
                ));
        CategoryService categoryService = new TreeNodeCategoryService(categories);
        ResupplyQualityMatrixService matrixService = Mockito.mock(ResupplyQualityMatrixService.class);
        ResupplyQualityMatrixImportService importer =
                new ResupplyQualityMatrixImportService(categoryService, matrixService);
        LocalDateTime updateTime = LocalDateTime.now();
        importer.updateQualityMatrices(updateTime, () -> getClass().getResourceAsStream(TEST_CSV_RESOURCE_PATH));
        Mockito.verify(matrixService).setCategoryQualityMatrix(
                Mockito.eq(5),
                MockitoHamcrest.argThat(sameSetAs(PACKAGE_SCRATCHES, WRONG_OR_DAMAGED_PAPERS, DEFORMED,
                        PACKAGE_JAMS, PACKAGE_CONTAMINATION, PACKAGE_HOLES, PACKAGE_OPENED, WAS_USED,
                        MISSING_PARTS, MISSING_PARTS_CRITICAL
                )),
                Mockito.eq(updateTime)
        );
        Mockito.verify(matrixService).setCategoryQualityMatrix(
                Mockito.eq(8),
                MockitoHamcrest.argThat(sameSetAs(DEFORMED, WRONG_OR_DAMAGED_PAPERS, PACKAGE_SCRATCHES,
                        WAS_USED, MISSING_PARTS, MISSING_PARTS_CRITICAL, PACKAGE_JAMS, PACKAGE_CONTAMINATION,
                        PACKAGE_HOLES, PACKAGE_OPENED)),
                Mockito.eq(updateTime)
        );
        Mockito.verify(matrixService).setCategoryQualityMatrix(
                Mockito.eq(12),
                MockitoHamcrest.argThat(sameSetAs(PACKAGE_OPENED, WAS_USED, WRONG_OR_DAMAGED_PAPERS, DEFORMED,
                        MISSING_PARTS, MISSING_PARTS_CRITICAL, PACKAGE_JAMS, PACKAGE_CONTAMINATION,
                        PACKAGE_HOLES)),
                Mockito.eq(updateTime)
        );
        Mockito.verify(matrixService).setCategoryQualityMatrix(
                Mockito.eq(14),
                MockitoHamcrest.argThat(sameSetAs(PACKAGE_OPENED, WAS_USED, WRONG_OR_DAMAGED_PAPERS, DEFORMED,
                        MISSING_PARTS, MISSING_PARTS_CRITICAL, PACKAGE_JAMS, PACKAGE_CONTAMINATION,
                        PACKAGE_HOLES)),
                Mockito.eq(updateTime)
        );
        Mockito.verify(matrixService).setCategoryQualityMatrix(
                Mockito.eq(16),
                MockitoHamcrest.argThat(sameSetAs(DEFORMED, MISSING_PARTS, MISSING_PARTS_CRITICAL,
                        PACKAGE_JAMS, PACKAGE_CONTAMINATION,
                        PACKAGE_HOLES, WRONG_OR_DAMAGED_PAPERS, WAS_USED,
                        PACKAGE_OPENED)),
                Mockito.eq(updateTime)
        );
        Mockito.verify(matrixService).setCategoryQualityMatrix(
                Mockito.eq(3),
                MockitoHamcrest.argThat(sameSetAs(DEFORMED, WRONG_OR_DAMAGED_PAPERS, WAS_USED,
                        MISSING_PARTS, MISSING_PARTS_CRITICAL, PACKAGE_JAMS, PACKAGE_CONTAMINATION, PACKAGE_HOLES,
                        PACKAGE_OPENED)),
                Mockito.eq(updateTime)
        );
        Mockito.verify(matrixService, Mockito.atLeast(15)).setCategoryQualityMatrix(anyInt(), any(Set.class), any(LocalDateTime.class));
        Mockito.verify(matrixService).deleteOldMatrices(Mockito.eq(updateTime));
        Mockito.verifyNoMoreInteractions(matrixService);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static Matcher<Set<ResupplyItemAttr>> sameSetAs(ResupplyItemAttr... set) {
        return (Matcher) Matchers.containsInAnyOrder(set);
    }
}
