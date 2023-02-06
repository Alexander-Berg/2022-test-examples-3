package ru.yandex.direct.mysql2grut.enummappers

import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.adgroup.model.MobileContentAdGroupNetworkTargeting
import ru.yandex.grut.objects.proto.AdGroupV2

class BiddableShowConditionEnumMapperTest : EnumMappersTestBase() {
    @Test
    fun checkMobileNetworkTypeMapping() {
        testBase(
            MobileContentAdGroupNetworkTargeting.values(),
            AdGroupEnumMappers::toGrutMobileNetworkType,
            AdGroupV2.TAdGroupV2Spec.TMobileContentDetails.ENetworkType.NT_UNKNOWN
        )
    }
}
