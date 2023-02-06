package ru.yandex.direct.excel.processing.model.internalad

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.UaTraitsConstants
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamiliesAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamily
import ru.yandex.direct.excel.processing.model.internalad.mappers.AdGroupAdditionalTargetingMapper.invertMap

@RunWith(JUnitParamsRunner::class)
class AdGroupAdditionalTargetingRepresentationTest {

    fun testDataFor_checkAddNumberDecimalPartIfNeed() = listOf(
            listOf("1", "1.0"),
            listOf("22", "22.0"),
            listOf("321", "321.0"),

            listOf("1.0", "1.0"),
            listOf("1.23", "1.23"),
            listOf("-321", "-321"),
            listOf("abc", "abc"),
    )


    @Test
    @Parameters(method = "testDataFor_checkAddNumberDecimalPartIfNeed")
    @TestCaseName("addNumberDecimalPartIfNeed for {0} and expect {1}")
    fun checkAddNumberDecimalPartIfNeed(value: String, expectedValue: String) {
        val result = AdGroupAdditionalTargetingRepresentation.addNumberDecimalPartIfNeed(value)

        assertThat(result)
                .isEqualTo(expectedValue)
    }

    @Test
    fun checkAddPositiveAndNegativeVersionedTargetingInOneExcelRow() {
        // тест на проверку добавления прямого и обратного версионного таргетинга записанного на одной строке
        val representation = AdGroupAdditionalTargetingRepresentation()
        val osFamilyTargeting = VersionedTargetingRepresentation()
                .setPositiveValue("iOS")
                .setNegativeValue("Android")
                .setMaxVersion("123.4")
                .setMinVersion("43.0")
        representation.setVersionedAdditionalTargeting({ OsFamiliesAdGroupAdditionalTargeting() }, { OsFamily() },
                OsFamiliesAdGroupAdditionalTargeting.VALUE, invertMap(UaTraitsConstants.OS_FAMILY),
                listOf(osFamilyTargeting))

        val expectedTargetings = listOf(
                OsFamiliesAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                        .withValue(listOf(OsFamily()
                                .withTargetingValueEntryId(3L)
                                .withMaxVersion(osFamilyTargeting.maxVersion)
                                .withMinVersion(osFamilyTargeting.minVersion))
                        ),
                OsFamiliesAdGroupAdditionalTargeting()
                        .withTargetingMode(AdGroupAdditionalTargetingMode.FILTERING)
                        .withJoinType(AdGroupAdditionalTargetingJoinType.ALL)
                        .withValue(listOf(OsFamily()
                                .withTargetingValueEntryId(2L)
                                .withMaxVersion(osFamilyTargeting.maxVersion)
                                .withMinVersion(osFamilyTargeting.minVersion))
                        )
        )

        assertThat(representation.targetingList)
                .isEqualTo(expectedTargetings)
    }

}
