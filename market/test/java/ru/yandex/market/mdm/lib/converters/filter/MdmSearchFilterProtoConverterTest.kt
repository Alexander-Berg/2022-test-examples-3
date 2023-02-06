package ru.yandex.market.mdm.lib.converters.filter

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmEntity
import ru.yandex.market.mdm.http.entity.MdmSearchCondition
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeValue
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeValues
import ru.yandex.market.mdm.lib.model.mdm.filter.MdmPage
import ru.yandex.market.mdm.lib.model.mdm.filter.MdmSearchCondition.EQ
import ru.yandex.market.mdm.lib.model.mdm.filter.MdmSearchConjunctivePredicates
import ru.yandex.market.mdm.lib.model.mdm.filter.MdmSearchConjunctivePredicates.Companion.mdmSearchConjunctivePredicate
import ru.yandex.market.mdm.lib.model.mdm.filter.MdmSearchDisjunctivePredicates.Companion.mdmSearchDisjunctivePredicate
import ru.yandex.market.mdm.lib.model.mdm.filter.MdmSearchFilter
import ru.yandex.market.mdm.lib.model.mdm.filter.MdmSearchPredicate

class MdmSearchFilterProtoConverterTest {

    @Test
    fun `should convert mdmFilter to GetMdmEntityBySearchKeysRequest`() {
        // given
        val value = MdmAttributeValue(int64 = 75)
        val values = MdmAttributeValues(mdmAttributeId = 42, values = listOf(value))
        val entity = mdmEntity(values = listOf(values))

        val filter = MdmSearchFilter(
            predicates = mdmSearchConjunctivePredicate(mdmSearchDisjunctivePredicate(MdmSearchPredicate(entity, EQ))),
            page = MdmPage(104, 1005),
            entity.mdmEntityTypeId,
        )

        // when
        val converted = filter.toGetMdmEntityBySearchKeysRequest()

        // then
        converted.pageSize shouldBe 104
        converted.pageToken shouldBe 1005
        converted.mdmSearchKeysCount shouldBe 1
        converted.getMdmSearchKeys(0).mdmSearchKeysCount shouldBe 1
        converted.getMdmSearchKeys(0).getMdmSearchKeys(0).condition shouldBe MdmSearchCondition.EQ
        val attributeValues = converted.getMdmSearchKeys(0).getMdmSearchKeys(0).mdmAttributeValues
        attributeValues.mdmAttributeId shouldBe 42
        attributeValues.valuesCount shouldBe 1
        attributeValues.mdmAttributePathList shouldContainExactly listOf(42)
        attributeValues.getValues(0).int64 shouldBe 75
    }

    @Test
    fun `should convert mdmFilter with entityTypeId to GetMdmEntityBySearchKeysRequest with empty predicates`() {
        // given
        val entityTypeId = 5L
        val filter = MdmSearchFilter(
            predicates = MdmSearchConjunctivePredicates(listOf()),
            page = MdmPage(),
            entityTypeId,
        )

        // when
        val converted = filter.toGetMdmEntityBySearchKeysRequest()

        // then
        converted.mdmSearchKeysCount shouldBe 1
        converted.getMdmSearchKeys(0).mdmEntityTypeId shouldBe 5L
    }

    @Test
    fun `should convert mdmFilter to GetMdmEntityByExternalKeysRequest`() {
        // given
        val value = MdmAttributeValue(int64 = 75)
        val values = MdmAttributeValues(mdmAttributeId = 42, values = listOf(value))
        val entity = mdmEntity(values = listOf(values))

        val filter = MdmSearchFilter(
            predicates = mdmSearchConjunctivePredicate(mdmSearchDisjunctivePredicate(MdmSearchPredicate(entity, EQ))),
            page = MdmPage(104, 1005),
            entity.mdmEntityTypeId,
        )

        // when
        val converted = filter.toGetMdmEntityByExternalKeysRequest()

        // then
        converted.pageSize shouldBe 104
        converted.pageToken shouldBe 1005
        converted.mdmExternalKeys.mdmExternalKeysCount shouldBe 1
        converted.mdmExternalKeys.getMdmExternalKeys(0).mdmAttributeValuesCount shouldBe 1
        val attributeValues = converted.mdmExternalKeys.getMdmExternalKeys(0).getMdmAttributeValues(0)
        attributeValues.mdmAttributeId shouldBe 42
        attributeValues.valuesCount shouldBe 1
        attributeValues.mdmAttributePathList shouldContainExactly listOf(42)
        attributeValues.getValues(0).int64 shouldBe 75
    }

    @Test
    fun `should convert mdmFilter to GetMdmEntityByExternalKeysRequest with two predicates`() {
        // given
        val value = MdmAttributeValue(int64 = 75)
        val values = MdmAttributeValues(mdmAttributeId = 7575, values = listOf(value))
        val valueInEntity = mdmEntity(values = listOf(values))

        val anotherValue = MdmAttributeValue(int64 = 42)
        val anotherValues = MdmAttributeValues(mdmAttributeId = 4242, values = listOf(anotherValue))
        val anotherValueInEntity = mdmEntity(values = listOf(anotherValues))

        val filter = MdmSearchFilter(
            predicates = mdmSearchConjunctivePredicate(mdmSearchDisjunctivePredicate(MdmSearchPredicate(valueInEntity, EQ)))
                .and(mdmSearchDisjunctivePredicate(MdmSearchPredicate(anotherValueInEntity, EQ))),
            page = MdmPage(),
            valueInEntity.mdmEntityTypeId,
        )

        // when
        val converted = filter.toGetMdmEntityByExternalKeysRequest()

        // then
        converted.mdmExternalKeys.mdmExternalKeysCount shouldBe 1
        val attributeValues = converted.mdmExternalKeys.getMdmExternalKeys(0).getMdmAttributeValues(0)
        attributeValues.getValues(0).int64 shouldBe 75
        attributeValues.mdmAttributeId shouldBe 7575
        val anotherAttributeValues = converted.mdmExternalKeys.getMdmExternalKeys(0).getMdmAttributeValues(1)
        anotherAttributeValues.getValues(0).int64 shouldBe 42
        anotherAttributeValues.mdmAttributeId shouldBe 4242
    }

    @Test
    fun `should convert mdmFilter with entityTypeId to GetMdmEntityByExternalKeysRequest with empty predicates`() {
        // given
        val entityTypeId = 5L
        val filter = MdmSearchFilter(
            predicates = MdmSearchConjunctivePredicates(listOf()),
            page = MdmPage(),
            entityTypeId,
        )

        // when
        val converted = filter.toGetMdmEntityByExternalKeysRequest()

        // then
        converted.mdmExternalKeys.mdmEntityTypeId shouldBe 5L
    }
}
