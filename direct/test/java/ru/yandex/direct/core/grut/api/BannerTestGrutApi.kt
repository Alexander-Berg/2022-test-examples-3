package ru.yandex.direct.core.grut.api

import com.google.protobuf.ByteString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.grut.objects.proto.BannerV2
import ru.yandex.grut.objects.proto.BannerV2.TBannerV2Status.TModerationStatus
import ru.yandex.grut.objects.proto.client.Schema

@Lazy
@Component
class BannerTestGrutApi(
    @Autowired grutContext: GrutContext,
    @Autowired properties: GrutApiProperties
) : BannerReplicationGrutApi(grutContext, properties) {
    /**
     * задает статус модерации баннера в GrUT
     */
    fun setStatusModerate(directBannerId: Long, statusModerate: BannerStatusModerate) {
        val mainStatus = serializeMainModerationStatus(statusModerate)
        val status = BannerV2.TBannerV2Status.newBuilder()
            .setModerationStatus(TModerationStatus.newBuilder().setMainStatus(mainStatus).build())
            .build()
            .toByteString()

        val meta = Schema.TBannerV2Meta.newBuilder()
            .setDirectId(directBannerId)
            .build()
            .toByteString()

        val updatedObject = UpdatedObject(
            meta = meta,
            status = status,
            setPaths = listOf(moderationStatusPath),
            spec = ByteString.EMPTY,
        )
        return updateObjects(listOf(updatedObject))
    }
}
