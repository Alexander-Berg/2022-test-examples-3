package ru.yandex.market.logistics.cte.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import ru.yandex.market.logistics.cte.client.dto.QualityAttributeDTO
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeType
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeValueType
import ru.yandex.market.logistics.cte.client.enums.StockType
import ru.yandex.market.logistics.cte.client.enums.SupplierType

class StockEvaluationByQualityGroupServiceTest {
    @ParameterizedTest(name = "#{index}")
    @MethodSource("smokeTestArguments")
    fun smokeTest(
            attributeToType: Map<QualityAttributeDTO, QualityAttributeValueType>,
            supplierType: SupplierType,
            discoveredAttributes: Set<QualityAttributeDTO>,
            expiredAndDamaged: Boolean,
            expectedStock: StockType
    ) {
        val actualStock = StockEvaluationByQualityGroupService().performEvaluation(attributeToType,
                supplierType, discoveredAttributes, expiredAndDamaged)
        Assertions.assertEquals(expectedStock, actualStock)
    }

    companion object {
        @JvmStatic
        fun smokeTestArguments(): List<Arguments> {
            return listOf(
                    // check priority of SECURITY_SERVICE
                    Arguments.of(
                            java.util.Map.ofEntries(
                                    java.util.Map.entry(
                                            packageJams(), QualityAttributeValueType.RETURN_REJECTED),
                                    java.util.Map.entry(
                                            packageHoles(), QualityAttributeValueType.COMPENSATION),
                                    java.util.Map.entry(
                                            missingParts(), QualityAttributeValueType.SECURITY_SERVICE)
                            ),
                            SupplierType.FIRST_PARTY,
                            mutableSetOf(packageJams(), packageHoles(), missingParts()),
                            false,
                            StockType.SECURITY_SERVICE
                    ),
                    // check priority of COMPENSATION
                    Arguments.of(
                            java.util.Map.ofEntries(
                                    java.util.Map.entry(
                                            packageJams(), QualityAttributeValueType.RETURN_REJECTED),
                                    java.util.Map.entry(
                                            packageHoles(), QualityAttributeValueType.COMPENSATION),
                                    java.util.Map.entry(
                                            packageExtensiveDamage(), QualityAttributeValueType.UTIL)
                            ),
                            SupplierType.FIRST_PARTY,
                            mutableSetOf(packageJams(), packageHoles(), packageExtensiveDamage()),
                            false,
                            StockType.COMPENSATION
                    ),
                    // check priority of RETURN_REJECTED
                    Arguments.of(
                            java.util.Map.ofEntries(
                                    java.util.Map.entry(
                                            packageJams(), QualityAttributeValueType.ENABLED),
                                    java.util.Map.entry(
                                            packageHoles(), QualityAttributeValueType.RETURN_REJECTED)
                            ),
                            SupplierType.FIRST_PARTY,
                            mutableSetOf(packageJams(), packageHoles()),
                            false,
                            StockType.RETURN_DENIED
                    ),
                    // check priority of UTIL
                    Arguments.of(
                            java.util.Map.ofEntries(
                                    java.util.Map.entry(
                                            packageExtensiveDamage(), QualityAttributeValueType.UTIL),
                                    java.util.Map.entry(
                                            packageHoles(), QualityAttributeValueType.RETURN_REJECTED)
                            ),
                            SupplierType.THIRD_PARTY,
                            mutableSetOf(packageExtensiveDamage(), packageHoles()),
                            false,
                            StockType.DAMAGE_DISPOSAL
                    ),
                    // check priority of WRONG_OR_DAMAGED_CIS with THIRD_PARTY SupplierType
                    Arguments.of(
                            java.util.Map.ofEntries(
                                    java.util.Map.entry(
                                            wrongOrDamagedCIS(), QualityAttributeValueType.ENABLED)
                            ),
                            SupplierType.THIRD_PARTY,
                            mutableSetOf(wrongOrDamagedCIS()),
                            false,
                            StockType.DAMAGE_CIS
                    ),
                    // check priority of DAMAGE with THIRD_PARTY SupplierType
                    Arguments.of(
                            java.util.Map.ofEntries(
                                    java.util.Map.entry(
                                            packageExtensiveDamage(), QualityAttributeValueType.ENABLED)
                            ),
                            SupplierType.THIRD_PARTY,
                            mutableSetOf(packageExtensiveDamage()),
                            false,
                            StockType.DAMAGE
                    ),

            )
        }

        private fun packageJams(): QualityAttributeDTO {
            return QualityAttributeDTO(2L, "PACKAGE_JAMS", "", "1.2", QualityAttributeType.PACKAGE, "")
        }

        private fun packageHoles(): QualityAttributeDTO {
            return QualityAttributeDTO(4L, "PACKAGE_HOLES", "", "1.4", QualityAttributeType.PACKAGE, "")
        }

        private fun wasDamaged(): QualityAttributeDTO {
            return QualityAttributeDTO(9L, "WAS_USED", "", "2.4", QualityAttributeType.ITEM, "")
        }

        private fun wrongOrDamagedCIS(): QualityAttributeDTO {
            return QualityAttributeDTO(12L, "WRONG_OR_DAMAGED_CIS", "", "2.5", QualityAttributeType.ITEM, "")
        }

        private fun packageExtensiveDamage(): QualityAttributeDTO {
            return QualityAttributeDTO(5L, "PACKAGE_EXTENSIVE_DAMAGE", "", "1.5", QualityAttributeType.ITEM, "")
        }

        private fun packageContamination(): QualityAttributeDTO {
            return QualityAttributeDTO(3L, "PACKAGE_CONTAMINATION", "", "1.3", QualityAttributeType.PACKAGE, "")
        }

        private fun wrongOrDamagedLabels(): QualityAttributeDTO {
            return QualityAttributeDTO(6L, "WRONG_OR_DAMAGED_LABELS", "", "2.1", QualityAttributeType.ITEM, "")
        }

        private fun deformed(): QualityAttributeDTO {
            return QualityAttributeDTO(7L, "DEFORMED", "", "2.2", QualityAttributeType.ITEM, "")
        }

        private fun packageScratches(): QualityAttributeDTO {
            return QualityAttributeDTO(1L, "PACKAGE_SCRATCHES", "", "1.1", QualityAttributeType.PACKAGE, "")
        }

        private fun wasUsed(): QualityAttributeDTO {
            return QualityAttributeDTO(9L, "WAS_USED", "", "2.4", QualityAttributeType.ITEM, "")
        }

        private fun missingParts(): QualityAttributeDTO {
            return QualityAttributeDTO(10L, "MISSING_PARTS", "", "3.1", QualityAttributeType.MISSING_PARTS, "")
        }
    }
}