package ru.yandex.market.logistics.cte.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.cte.client.dto.QualityAttributeDTO;
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeType;
import ru.yandex.market.logistics.cte.client.enums.StockType;
import ru.yandex.market.logistics.cte.client.enums.SupplierType;
import ru.yandex.market.logistics.cte.entity.stock.SupplyQualityMatrix;
import ru.yandex.market.logistics.cte.entity.supply.SupplyItemAttributeUtilType;

class StockEvaluationServiceTest {
    @ParameterizedTest(name="#{index}")
    @MethodSource("smokeTestArguments")
    public void smokeTest(
            Set<SupplyItemAttributeUtilType> qualityMatrixAttributes,
            SupplierType supplierType,
            Set<QualityAttributeDTO> discoveredAttributes,
            boolean expiredAndDamaged,
            StockType expectedStock
    ) {
        SupplyQualityMatrix supplyQualityMatrix = new SupplyQualityMatrix(qualityMatrixAttributes);
        StockType actualStock = new StockEvaluationService(null, null, null)
                .performEvaluation(supplyQualityMatrix, supplierType, discoveredAttributes, expiredAndDamaged);
        Assertions.assertEquals(expectedStock, actualStock);
    }

    private static List<Arguments> smokeTestArguments() {
        return Arrays.asList(
                Arguments.of(
                        Set.of(
                                new SupplyItemAttributeUtilType(packageJams(), false),
                                new SupplyItemAttributeUtilType(packageHoles(), false)
                        ),
                        SupplierType.FIRST_PARTY,
                        Set.of(),
                        false,
                        StockType.OK
                ),
                Arguments.of(
                        Set.of(
                                new SupplyItemAttributeUtilType(packageJams(), false),
                                new SupplyItemAttributeUtilType(packageHoles(), true)
                        ),
                        SupplierType.FIRST_PARTY,
                        Set.of(packageScratches()),
                        false,
                        StockType.OK),
                Arguments.of(
                        Set.of(
                                new SupplyItemAttributeUtilType(
                                        packageScratches(), false),
                                new SupplyItemAttributeUtilType(
                                        packageContamination(), false),
                                new SupplyItemAttributeUtilType(
                                        packageExtensiveDamage(), false),
                                new SupplyItemAttributeUtilType(
                                        packageScratches(), false),
                                new SupplyItemAttributeUtilType(missingParts(), false),
                                new SupplyItemAttributeUtilType(
                                        wrongOrDamagedLabels(), false),
                                new SupplyItemAttributeUtilType(
                                        deformed(), true),
                                new SupplyItemAttributeUtilType(
                                        wasUsed(), false)
                        ),
                        SupplierType.FIRST_PARTY,
                        Set.of(packageJams(),
                                packageHoles(),
                                packageExtensiveDamage(),
                                packageScratches(),
                                wrongOrDamagedLabels(),
                                deformed()),
                        false,
                        StockType.DAMAGE_DISPOSAL
                ),
                Arguments.of(
                        Set.of(
                                new SupplyItemAttributeUtilType(
                                        packageScratches(), false),
                                new SupplyItemAttributeUtilType(
                                        packageContamination(), false),
                                new SupplyItemAttributeUtilType(packageExtensiveDamage(),
                                        false),
                                new SupplyItemAttributeUtilType(wasUsed(), false),
                                new SupplyItemAttributeUtilType(deformed(), false)
                        ),
                        SupplierType.FIRST_PARTY,
                        Set.of(packageContamination(),
                                packageExtensiveDamage(),
                                wasUsed(),
                                deformed()
                        ),
                        false,
                        StockType.DAMAGE_RESELL
                ),
                Arguments.of(
                        Set.of(
                                new SupplyItemAttributeUtilType(
                                        packageScratches(), false),
                                new SupplyItemAttributeUtilType(
                                        wrongOrDamagedLabels(), false),
                                new SupplyItemAttributeUtilType(
                                        deformed(), false)
                        ),
                        SupplierType.THIRD_PARTY,
                        Set.of(packageScratches(),
                                packageJams(),
                                packageHoles(),
                                packageExtensiveDamage(),
                                deformed(),
                                wasUsed()
                        ),
                        false,
                        StockType.DAMAGE
                ),
                Arguments.of(
                        Set.of(
                                new SupplyItemAttributeUtilType(packageExtensiveDamage(),
                                        false),
                                new SupplyItemAttributeUtilType(
                                        wrongOrDamagedLabels(), false),
                                new SupplyItemAttributeUtilType(deformed(), true),
                                new SupplyItemAttributeUtilType(wasUsed(), false)
                        ),
                        SupplierType.THIRD_PARTY,
                        Set.of(packageScratches(),
                                packageJams(),
                                packageContamination(),
                                packageExtensiveDamage(),
                                deformed(),
                                wrongOrDamagedLabels()
                        ),
                        false,
                        StockType.DAMAGE
                ),
                Arguments.of(
                        Set.of(
                                new SupplyItemAttributeUtilType(packageJams(), false),
                                new SupplyItemAttributeUtilType(packageHoles(), false),
                                new SupplyItemAttributeUtilType(packageExtensiveDamage(),
                                        false),
                                new SupplyItemAttributeUtilType(deformed(), false)
                        ),
                        SupplierType.FIRST_PARTY,
                        Set.of(packageJams()),
                        true,
                        StockType.EXPIRED
                ),
                Arguments.of(
                        Set.of(
                                new SupplyItemAttributeUtilType(packageJams(), false),
                                new SupplyItemAttributeUtilType(packageHoles(), false),
                                new SupplyItemAttributeUtilType(packageExtensiveDamage(),
                                        false),
                                new SupplyItemAttributeUtilType(deformed(), false)
                        ),
                        SupplierType.THIRD_PARTY,
                        Set.of(packageHoles()),
                        true,
                        StockType.EXPIRED
                ),
                Arguments.of(
                        Set.of(
                                new SupplyItemAttributeUtilType(packageJams(), false),
                                new SupplyItemAttributeUtilType(packageHoles(), false),
                                new SupplyItemAttributeUtilType(packageExtensiveDamage(),
                                        false),
                                new SupplyItemAttributeUtilType(deformed(), false)
                        ),
                        SupplierType.THIRD_PARTY,
                        Set.of(packageHoles(), wrongOrDamagedCIS()),
                        false,
                        StockType.DAMAGE
                ),
                Arguments.of(
                        Set.of(
                                new SupplyItemAttributeUtilType(packageJams(), false),
                                new SupplyItemAttributeUtilType(packageHoles(), false),
                                new SupplyItemAttributeUtilType(packageExtensiveDamage(),
                                        false),
                                new SupplyItemAttributeUtilType(deformed(), false)
                        ),
                        SupplierType.THIRD_PARTY,
                        Set.of(wrongOrDamagedCIS()),
                        false,
                        StockType.DAMAGE_CIS
                ),
                Arguments.of(
                        Set.of(
                                new SupplyItemAttributeUtilType(
                                        packageScratches(), false),
                                new SupplyItemAttributeUtilType(
                                        packageContamination(), false),
                                new SupplyItemAttributeUtilType(packageExtensiveDamage(),
                                        false),
                                new SupplyItemAttributeUtilType(wasUsed(), false),
                                new SupplyItemAttributeUtilType(deformed(), false)
                        ),
                        SupplierType.FIRST_PARTY,
                        Set.of(packageContamination(),
                                packageExtensiveDamage(),
                                wasUsed(),
                                deformed(),
                                wrongOrDamagedCIS()
                        ),
                        false,
                        StockType.DAMAGE_CIS
                )
        );

    }

    private static QualityAttributeDTO packageJams(){
        return new QualityAttributeDTO(2L, "PACKAGE_JAMS", "", "1.2", QualityAttributeType.PACKAGE, "");
    }

    private static QualityAttributeDTO packageHoles(){
        return new QualityAttributeDTO(4L, "PACKAGE_HOLES", "", "1.4", QualityAttributeType.PACKAGE, "");
    }

    private static QualityAttributeDTO wasDamaged(){
        return new QualityAttributeDTO(9L, "WAS_USED", "", "2.4", QualityAttributeType.ITEM, "");
    }

    private static QualityAttributeDTO wrongOrDamagedCIS(){
        return new QualityAttributeDTO(12L, "WRONG_OR_DAMAGED_CIS", "", "2.5", QualityAttributeType.ITEM, "");
    }

    private static QualityAttributeDTO packageExtensiveDamage(){
        return new QualityAttributeDTO(5L, "PACKAGE_EXTENSIVE_DAMAGE", "", "1.5", QualityAttributeType.ITEM, "");
    }

    private static QualityAttributeDTO packageContamination(){
        return new QualityAttributeDTO(3L, "PACKAGE_CONTAMINATION", "", "1.3", QualityAttributeType.PACKAGE, "");
    }

    private static QualityAttributeDTO wrongOrDamagedLabels(){
        return new QualityAttributeDTO(6L, "WRONG_OR_DAMAGED_LABELS", "", "2.1", QualityAttributeType.ITEM, "");
    }

    private static QualityAttributeDTO deformed(){
        return new QualityAttributeDTO(7L, "DEFORMED", "", "2.2", QualityAttributeType.ITEM, "");
    }

    private static QualityAttributeDTO packageScratches(){
        return new QualityAttributeDTO(1L, "PACKAGE_SCRATCHES", "", "1.1", QualityAttributeType.PACKAGE, "");
    }

    private static QualityAttributeDTO wasUsed(){
        return new QualityAttributeDTO(9L, "WAS_USED", "", "2.4", QualityAttributeType.ITEM, "");
    }

    private static QualityAttributeDTO missingParts(){
        return new QualityAttributeDTO(10L, "MISSING_PARTS", "", "3.1", QualityAttributeType.MISSING_PARTS, "");
    }
}
