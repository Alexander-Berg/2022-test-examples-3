package ru.yandex.direct.mysql2grut.enummappers

import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.creative.model.CreativeType
import ru.yandex.direct.core.entity.creative.model.SourceMediaType
import ru.yandex.direct.mysql2grut.enummappers.CreativeEnumMappers.Companion.CREATIVE_TYPE_BLACKLIST
import ru.yandex.grut.objects.proto.Creative

class CreativeEnumMappersTest : EnumMappersTestBase() {
    @Test
    fun checkCreativeTypeMapping() {
        testBase(
            CreativeType.values(),
            CreativeEnumMappers::toCreativeType,
            Creative.TCreativeMetaBase.ECreativeType.CT_UNKNOWN,
            CREATIVE_TYPE_BLACKLIST
        )
    }

    @Test
    fun checkSourceMediaTypeMapping() {
        testBase(
            SourceMediaType.values(),
            CreativeEnumMappers::sourceMediaTypeToGrut,
            Creative.TCreativeSpec.EMediaType.MT_UNKNOWN
        )
    }
}
