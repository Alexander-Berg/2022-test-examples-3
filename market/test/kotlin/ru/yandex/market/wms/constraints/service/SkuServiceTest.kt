package ru.yandex.market.wms.constraints.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.wms.common.spring.dao.entity.SkuId
import ru.yandex.market.wms.constraints.core.domain.RuleRestrictionParam
import ru.yandex.market.wms.constraints.core.domain.RuleRestrictionType
import ru.yandex.market.wms.constraints.core.domain.SkuCharacteristic
import ru.yandex.market.wms.constraints.core.domain.SkuSpecification
import ru.yandex.market.wms.constraints.integration.core.SkuService
import ru.yandex.market.wms.core.base.dto.SkuCharacteristicType
import ru.yandex.market.wms.core.base.request.SkuCharacteristicsRequest
import ru.yandex.market.wms.core.base.response.SkuCharacteristicsResponse
import ru.yandex.market.wms.core.client.CoreClient
import ru.yandex.market.wms.core.base.dto.SkuCharacteristic as CoreCharacteristic
import ru.yandex.market.wms.core.base.dto.SkuSpecification as CoreSpecification

class SkuServiceTest {

    private val skus = listOf(
        SkuId("465852", "ROV000001"),
        SkuId("782733", "ROV000238"),
        SkuId("3278", "89427384"),
    )
    private val coreClient = mock<CoreClient>().apply {
        whenever(getSkuCharacteristics(SkuCharacteristicsRequest(skus))).thenReturn(
            SkuCharacteristicsResponse(
                listOf(
                    CoreSpecification(skus.component1().sku, skus.component1().storerKey, null, null, listOf(
                        CoreCharacteristic(SkuCharacteristicType.CARGO_TYPE, "150"),
                        CoreCharacteristic(SkuCharacteristicType.LENGTH, "50"),
                        CoreCharacteristic(SkuCharacteristicType.WIDTH, "10.500"),
                        CoreCharacteristic(SkuCharacteristicType.HEIGHT, "3"),
                        CoreCharacteristic(SkuCharacteristicType.WEIGHT, "34.060"),
                    )),
                    CoreSpecification(skus.component2().sku, skus.component2().storerKey, "MSKU00238", "Товар 1", listOf(
                        CoreCharacteristic(SkuCharacteristicType.CARGO_TYPE, "390"),
                        CoreCharacteristic(SkuCharacteristicType.LENGTH, "5"),
                        CoreCharacteristic(SkuCharacteristicType.HEIGHT, "2"),
                    )),
                )
            )
        )
    }
    private val underTest = SkuService(coreClient)

    @Test
    fun `get sku characteristics`() {
        val response = underTest.findSku(skus)

        assertThat(response).containsExactlyInAnyOrder(
            SkuSpecification("ROV000001", "465852", null, null, listOf(
                SkuCharacteristic(RuleRestrictionType.CARGO_TYPE, RuleRestrictionParam.CARGO_TYPE, "150"),
                SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.ANY_SIDE, "50"),
                SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.ANY_SIDE, "10.500"),
                SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.ANY_SIDE, "3"),
                SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.WEIGHT, "34.060"),
                SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.MIN_SIDE, "3.000"),
                SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.MAX_SIDE, "50.000"),
                SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.MID_SIDE, "10.500"),
                SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.WLH_SUM, "63.500"),
            )),
            SkuSpecification("ROV000238", "782733", "MSKU00238", "Товар 1", listOf(
                SkuCharacteristic(RuleRestrictionType.CARGO_TYPE, RuleRestrictionParam.CARGO_TYPE, "390"),
                SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.ANY_SIDE, "5"),
                SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.ANY_SIDE, "2"),
                SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.MIN_SIDE, "2.000"),
                SkuCharacteristic(RuleRestrictionType.DIMENSION, RuleRestrictionParam.MAX_SIDE, "5.000"),
            ))
        )
    }
}
