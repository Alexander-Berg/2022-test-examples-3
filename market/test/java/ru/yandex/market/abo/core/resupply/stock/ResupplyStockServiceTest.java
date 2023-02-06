package ru.yandex.market.abo.core.resupply.stock;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr;
import ru.yandex.market.checkout.checkouter.order.SupplierType;

import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.DEFORMED;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyItemAttr.MISSING_PARTS;
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
class ResupplyStockServiceTest {

    @ParameterizedTest
    @MethodSource("smokeTestArguments")
    void smokeTest(
            boolean isGood,
            BigDecimal price,
            boolean isCompetenceCategory,
            Set<ResupplyItemAttr> categoryAttributes,
            SupplierType supplierType,
            Set<ResupplyItemAttr> attributes,
            ResupplyStock expectedStock
    ) {
        ResupplyQualityMatrix qualityMatrix = new ResupplyQualityMatrix(categoryAttributes);
        ResupplyStock actualStock =
                ResupplyStockService.evaluateStock(isGood, price, isCompetenceCategory, qualityMatrix, supplierType, attributes);
        Assertions.assertEquals(expectedStock, actualStock);
    }

    private static List<Arguments> smokeTestArguments() {
        return Arrays.asList(
                Arguments.of(
                        false,
                        BigDecimal.valueOf(8633),
                        true,
                        EnumSet.of(
                                PACKAGE_SCRATCHES,
                                PACKAGE_CONTAMINATION,
                                PACKAGE_OPENED,
                                MISSING_PARTS,
                                WRONG_OR_DAMAGED_PAPERS,
                                DEFORMED,
                                WAS_USED
                        ),
                        SupplierType.FIRST_PARTY,
                        EnumSet.of(
                                PACKAGE_JAMS,
                                PACKAGE_HOLES,
                                PACKAGE_OPENED,
                                WRONG_OR_DAMAGED_PAPERS,
                                DEFORMED
                        ),
                        ResupplyStock.TO_PARTNER_SERVICE_CENTER
                ),
                Arguments.of(
                        false,
                        BigDecimal.valueOf(2675),
                        false,
                        EnumSet.of(
                                PACKAGE_SCRATCHES,
                                PACKAGE_CONTAMINATION,
                                PACKAGE_OPENED,
                                WAS_USED,
                                DEFORMED
                        ),
                        SupplierType.FIRST_PARTY,
                        EnumSet.of(
                                PACKAGE_CONTAMINATION,
                                PACKAGE_OPENED,
                                WAS_USED,
                                DEFORMED
                        ),
                        ResupplyStock.RECYCLING
                ),
                Arguments.of(
                        false,
                        BigDecimal.valueOf(5707),
                        false,
                        EnumSet.of(
                                PACKAGE_CONTAMINATION,
                                WRONG_OR_DAMAGED_PAPERS,
                                DEFORMED
                        ),
                        SupplierType.FIRST_PARTY,
                        EnumSet.of(
                                PACKAGE_SCRATCHES,
                                PACKAGE_JAMS,
                                PACKAGE_HOLES,
                                PACKAGE_OPENED,
                                MISSING_PARTS,
                                WAS_USED
                        ),
                        ResupplyStock.GOOD
                ),
                Arguments.of(
                        false,
                        BigDecimal.valueOf(2185),
                        false,
                        EnumSet.of(
                                PACKAGE_OPENED,
                                WRONG_OR_DAMAGED_PAPERS,
                                DEFORMED,
                                WAS_USED
                        ),
                        SupplierType.FIRST_PARTY,
                        EnumSet.of(
                                PACKAGE_SCRATCHES,
                                PACKAGE_JAMS,
                                PACKAGE_CONTAMINATION,
                                PACKAGE_OPENED,
                                MISSING_PARTS,
                                WRONG_OR_DAMAGED_PAPERS
                        ),
                        ResupplyStock.ADDITIONAL_PROCESSING
                ),
                Arguments.of(
                        false,
                        BigDecimal.valueOf(498),
                        false,
                        EnumSet.of(
                                PACKAGE_JAMS,
                                PACKAGE_HOLES,
                                PACKAGE_OPENED,
                                DEFORMED),
                        SupplierType.FIRST_PARTY,
                        EnumSet.of(
                                PACKAGE_JAMS,
                                PACKAGE_HOLES,
                                PACKAGE_OPENED,
                                MISSING_PARTS
                        ),
                        ResupplyStock.CHARITY
                ),
                Arguments.of(
                        false,
                        BigDecimal.valueOf(498),
                        false,
                        EnumSet.of(
                                PACKAGE_JAMS,
                                PACKAGE_HOLES,
                                PACKAGE_OPENED,
                                DEFORMED),
                        SupplierType.THIRD_PARTY,
                        EnumSet.of(
                                PACKAGE_JAMS,
                                PACKAGE_HOLES,
                                PACKAGE_OPENED,
                                MISSING_PARTS
                        ),
                        ResupplyStock.BAD_3P
                )
        );
    }
}
