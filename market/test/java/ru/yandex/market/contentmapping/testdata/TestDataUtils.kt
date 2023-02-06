package ru.yandex.market.contentmapping.testdata

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import ru.yandex.market.contentmapping.dto.data.category.parameter.CategoryParameterInfo
import ru.yandex.market.contentmapping.dto.data.category.parameter.CategoryParameterValue
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.dto.model.ShopModel
import ru.yandex.market.contentmapping.dto.model.UpdateStatus
import ru.yandex.market.contentmapping.kotlin.typealiases.CategoryId
import ru.yandex.market.contentmapping.kotlin.typealiases.ShopId
import ru.yandex.market.mbo.export.MboParameters

object TestDataUtils {
    private var nextId = 1000L
    const val TEST_SHOP_ID = 42L
    const val TEST_CATEGORY_ID: Long = 84

    fun testShop() = Shop(TEST_SHOP_ID, "Test shop")

    fun activeShop(id: ShopId, name: String = "$id shop") =
        Shop(id, name, isDatacamp = true, businessId = id, updateStatus = UpdateStatus.ENABLED)

    fun testShopModel(
        id: Long = nextId++,
        shopSku: String = "shop_sku_$id",
        shopId: ShopId = TEST_SHOP_ID,
        name: String = "ShopModel #$id ($shopSku)",
        marketCategoryId: CategoryId? = null,
    ) = ShopModel(
        id = id,
        shopId = shopId,
        shopSku = shopSku,
        name = name,
        externalCategoryId = marketCategoryId,
    )

    fun nextShopModel(): ShopModel {
        val id = nextId++
        return ShopModel(
                id = id,
                name = "ShopModel #$id",
                shopId = TEST_SHOP_ID,
                shopSku = "shop_sku_$id",
                shopCategoryName = "shop_category",
                externalCategoryId = TEST_CATEGORY_ID,
        )
    }

    fun nextParameter(name: String): CategoryParameterInfo {
        val id = nextId++
        return CategoryParameterInfoTestInstance().copy(
                parameterId = id,
                name = name,
                valueType = MboParameters.ValueType.STRING,
        )
    }

    fun nextParameterEnum(name: String, vararg optionIds: Int): CategoryParameterInfo {
        val options = Int2ObjectOpenHashMap<CategoryParameterValue>()
        for (optionId in optionIds) {
            options[optionId] = CategoryParameterValue(optionId, "name$optionId")
        }
        return nextParameter(name).copy(
                valueType = MboParameters.ValueType.ENUM,
                options = options,
        )
    }
}

fun CategoryParameterInfoTestInstance() = CategoryParameterInfo(
        parameterId = 0,
        xslName = "test_param",
        name = "test_param",
        unitName = null,
        valueType = MboParameters.ValueType.NUMERIC,
        isImportant = false,
        isMultivalue = false,
        isService = false,
        isRequiredForModelCreation = false,
        isMandatoryForSignature = false,
        commonFilterIndex = 0,
        options = Int2ObjectOpenHashMap(),
        commentForPartner = null,
        commentForOperator = null,
        minValue = null,
        maxValue = null,
)
