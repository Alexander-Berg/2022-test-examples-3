package ru.yandex.direct.mysql2grut.enummappers

import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.banner.model.ImageType
import ru.yandex.direct.core.entity.image.model.AvatarHost
import ru.yandex.grut.auxiliary.proto.MdsInfo
import ru.yandex.grut.objects.proto.BannerV2

class BannerEnumMappersTest : EnumMappersTestBase() {
    @Test
    fun checkImageTypeMapping() {
        testBase(
            ImageType.values(),
            BannerEnumMappers::imageTypeToGrut,
            BannerV2.TBannerV2Spec.TImage.EImageType.IT_UNKNOWN
        )
    }

    @Test
    fun checkAvatarHostMapping() {
        testBase(
            AvatarHost.values(),
            BannerEnumMappers::avatarsHostToEnvironment,
            MdsInfo.TMdsFileInfo.EEnvironment.ENV_UNKNOWN
        )
    }
}
