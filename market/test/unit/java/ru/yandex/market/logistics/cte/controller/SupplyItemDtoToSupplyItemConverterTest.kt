package ru.yandex.market.logistics.cte.controller

import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.cte.base.SoftAssertionsSupportedTest
import ru.yandex.market.logistics.cte.client.dto.SupplyItemIdentifier
import ru.yandex.market.logistics.cte.client.dto.SupplyItemRequestDTO
import ru.yandex.market.logistics.cte.client.enums.QualityAttributeType
import ru.yandex.market.logistics.cte.client.enums.SupplyItemAttributeType
import ru.yandex.market.logistics.cte.client.enums.SupplyItemIdentifierType
import ru.yandex.market.logistics.cte.converters.SupplyItemDtoToSupplyItemConverter
import ru.yandex.market.logistics.cte.entity.group.QualityAttributeEntity
import ru.yandex.market.logistics.cte.entity.supply.CompositeKey
import ru.yandex.market.logistics.cte.entity.supply.SupplyItem
import java.math.BigDecimal
import java.util.EnumSet

internal class SupplyItemDtoToSupplyItemConverterTest: SoftAssertionsSupportedTest() {
    private val classUnderTest: SupplyItemDtoToSupplyItemConverter = SupplyItemDtoToSupplyItemConverter()

    @Test
    fun happyHath() {
        val input = provideHappyPathTestFixture()
        val result = classUnderTest.convert(UUID, getAttributes() ,input)
        assertions.assertThat(result).isEqualToIgnoringGivenFields(expectedHappyPathResult(), "createdAt")
    }

    private fun expectedHappyPathResult(): SupplyItem {
        return SupplyItem(
            uuid = UUID,
            compositeKey = CompositeKey(vendorId = VENDOR_ID, uit = UIT_IDENTIFIER),
            externalSku = SHOP_SKU,
            identifiers = IDENTIFIERS,
            orderId = ORDER_ID,
            price = PRICE,
            createdBy = CREATED_BY,
            categoryId = CATEGORY_ID,
            attributes = getAttributes(),
            boxId = BOX_ID)
    }

    private fun provideHappyPathTestFixture(): SupplyItemRequestDTO {
        return SupplyItemRequestDTO.builder()
            .boxId(BOX_ID)
            .categoryId(CATEGORY_ID)
            .createdBy(CREATED_BY)
            .criteria(ATTRIBUTES)
            .identifiers(IDENTIFIERS)
            .orderId(ORDER_ID)
            .price(PRICE)
            .externalSku(SHOP_SKU)
            .vendorId(VENDOR_ID)
            .build()
    }

    companion object {
        const val CATEGORY_ID = 102
        const val BOX_ID = "BOX_ID"
        const val CREATED_BY = "CREATED_BY"
        const val UIT_IDENTIFIER = "uit_identifier"
        val IDENTIFIERS = HashSet(
            listOf(
                SupplyItemIdentifier("truth_mark_identifier", SupplyItemIdentifierType.CIS),
                SupplyItemIdentifier(UIT_IDENTIFIER, SupplyItemIdentifierType.UIT)
            )
        )
        const val ORDER_ID = "ORDER_ID"
        val PRICE: BigDecimal = BigDecimal.valueOf(100.50)
        const val SHOP_SKU = "SHOP_SKU"
        const val VENDOR_ID: Long = 100500
        const val UUID = "UUID"
        val ATTRIBUTES: EnumSet<SupplyItemAttributeType> = EnumSet.of(
            SupplyItemAttributeType.DEFORMED,
            SupplyItemAttributeType.PACKAGE_JAMS
        )


        private fun getAttributes(): Set<QualityAttributeEntity> {
            return setOf(deformed(), packageJams())
        }

        private fun deformed(): QualityAttributeEntity {
            val qualityAttributeEntity = QualityAttributeEntity()
            qualityAttributeEntity.id = 7
            qualityAttributeEntity.name = "DEFORMED"
            qualityAttributeEntity.title = "DEFORMED"
            qualityAttributeEntity.refId = "2.2"
            qualityAttributeEntity.attributeType = QualityAttributeType.ITEM
            return qualityAttributeEntity
        }

        private fun packageJams(): QualityAttributeEntity {
            val qualityAttributeEntity = QualityAttributeEntity()
            qualityAttributeEntity.id = 2
            qualityAttributeEntity.refId = "1.2"
            qualityAttributeEntity.name = "PACKAGE_JAMS"
            qualityAttributeEntity.title = "PACKAGE_JAMS"
            qualityAttributeEntity.attributeType = QualityAttributeType.PACKAGE
            return qualityAttributeEntity
        }
    }

}
