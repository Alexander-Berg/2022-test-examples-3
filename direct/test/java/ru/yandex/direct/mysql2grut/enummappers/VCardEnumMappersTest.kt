package ru.yandex.direct.mysql2grut.enummappers

import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.vcard.model.PointPrecision
import ru.yandex.grut.objects.proto.Vcard

class VCardEnumMappersTest : EnumMappersTestBase() {
    @Test
    fun checkPrecisionTypeMapping() {
        testBase(
            PointPrecision.values(),
            VCardEnumMappers::precisionTypeToGrut,
            Vcard.TVCardSpec.TAddress.EAddressPrecisionType.AP_UNKNOWN
        )
    }
}
