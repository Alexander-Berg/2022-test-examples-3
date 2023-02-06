package ru.yandex.direct.core.grut.converter

import org.assertj.core.api.SoftAssertions
import org.junit.Test
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate
import ru.yandex.direct.core.grut.api.VersionedGrutObject
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType
import ru.yandex.grut.objects.proto.BannerV2.EBannerStatus
import ru.yandex.grut.objects.proto.BannerV2.TBannerV2Spec
import ru.yandex.grut.objects.proto.BannerV2.TBannerV2Status
import ru.yandex.grut.objects.proto.BannerV2.TBannerV2Status.TModerationStatus
import ru.yandex.grut.objects.proto.BannerV2.TBannerV2Status.TModerationStatus.TItemStatus
import ru.yandex.grut.objects.proto.client.Schema.TBannerV2
import ru.yandex.grut.objects.proto.client.Schema.TBannerV2Meta

private const val CAMPAIGN_ID = 1L
private const val ADGROUP_ID = 2L
private const val BANNER_ID = 3L
private const val BS_BANNER_ID = 4L
private const val BANNER_VERSION = 10L

class GrutBannerConverterTest {

    @Test
    fun convertToBannerDataForStatus_MetaFieldsTest() {
        val bannerDataForStatus = convertToBannerDataForStatus(
            VersionedGrutObject(getBanner(), 1L),
            null,
            mutableMapOf(CAMPAIGN_ID to CampaignsType.text)
        )

        SoftAssertions().apply {
            val banner = bannerDataForStatus.banner()
            assertThat(banner.id).isEqualTo(BANNER_ID)
            assertThat(banner.adgroupId).isEqualTo(ADGROUP_ID)
            assertThat(banner.bsBannerId).isEqualTo(BS_BANNER_ID)
            assertThat(banner.bannerType).isEqualTo(BannersBannerType.text)
            assertThat(banner.campaignType).isEqualTo(CampaignsType.text)
        }.assertAll()
    }

    @Test
    fun convertToBannerDataForStatus_DraftBannerStatuses() {
        val bannerDataForStatus = convertToBannerDataForStatus(
            VersionedGrutObject(getBanner(), BANNER_VERSION),
            null,
            mutableMapOf(CAMPAIGN_ID to CampaignsType.text)
        )

        SoftAssertions().apply {
            val banner = bannerDataForStatus.banner()
            assertThat(banner.statusModerate).isEqualTo(BannerStatusModerate.NEW)
            assertThat(banner.statusPostModerate).isEqualTo(BannerStatusPostModerate.NO)
            assertThat(banner.statusBsSynced).isEqualTo(StatusBsSynced.NO)
            assertThat(banner.statusActive).isEqualTo(false)
            assertThat(banner.statusArchived).isEqualTo(false)
            assertThat(banner.statusShow).isEqualTo(true)
        }.assertAll()
    }

    @Test
    fun convertToBannerDataForStatus_ActiveSyncedBannerStatuses() {
        val bannerDataForStatus = convertToBannerDataForStatus(
            VersionedGrutObject(
                getBanner(
                    bannerStatus = EBannerStatus.BST_ACTIVE_VALUE,
                    moderationVerdict = BannerStatusModerate.YES,
                ), BANNER_VERSION
            ),
            getBanner(
                bannerStatus = EBannerStatus.BST_ACTIVE_VALUE,
                moderationVerdict = BannerStatusModerate.YES,
                bannerVersion = BANNER_VERSION
            ),
            mutableMapOf(CAMPAIGN_ID to CampaignsType.text)
        )

        SoftAssertions().apply {
            val banner = bannerDataForStatus.banner()
            assertThat(banner.statusModerate).isEqualTo(BannerStatusModerate.YES)
            assertThat(banner.statusPostModerate).isEqualTo(BannerStatusPostModerate.YES)
            assertThat(banner.statusBsSynced).isEqualTo(StatusBsSynced.YES)
            assertThat(banner.statusActive).isEqualTo(true)
            assertThat(banner.statusArchived).isEqualTo(false)
            assertThat(banner.statusShow).isEqualTo(true)
        }.assertAll()
    }

    @Test
    fun convertToBannerDataForStatus_ActiveNotSyncedBannerStatuses() {
        val bannerDataForStatus = convertToBannerDataForStatus(
            VersionedGrutObject(
                getBanner(
                    bannerStatus = EBannerStatus.BST_ACTIVE_VALUE,
                    moderationVerdict = BannerStatusModerate.YES,
                ), BANNER_VERSION + 1
            ),
            getBanner(
                bannerStatus = EBannerStatus.BST_ACTIVE_VALUE,
                moderationVerdict = BannerStatusModerate.YES,
                bannerVersion = BANNER_VERSION
            ),
            mutableMapOf(CAMPAIGN_ID to CampaignsType.text)
        )

        SoftAssertions().apply {
            val banner = bannerDataForStatus.banner()
            assertThat(banner.statusModerate).isEqualTo(BannerStatusModerate.YES)
            assertThat(banner.statusPostModerate).isEqualTo(BannerStatusPostModerate.YES)
            assertThat(banner.statusBsSynced).isEqualTo(StatusBsSynced.NO)
            assertThat(banner.statusActive).isEqualTo(true)
            assertThat(banner.statusArchived).isEqualTo(false)
            assertThat(banner.statusShow).isEqualTo(true)
        }.assertAll()
    }

    @Test
    fun convertToBannerDataForStatus_StoppedSyncedBannerStatuses() {
        val bannerDataForStatus = convertToBannerDataForStatus(
            VersionedGrutObject(
                getBanner(
                    bannerStatus = EBannerStatus.BST_STOPPED_VALUE,
                    moderationVerdict = BannerStatusModerate.YES,
                ), BANNER_VERSION
            ),
            null,
            mutableMapOf(CAMPAIGN_ID to CampaignsType.text)
        )

        SoftAssertions().apply {
            val banner = bannerDataForStatus.banner()
            assertThat(banner.statusModerate).isEqualTo(BannerStatusModerate.YES)
            assertThat(banner.statusPostModerate).isEqualTo(BannerStatusPostModerate.YES)
            assertThat(banner.statusBsSynced).isEqualTo(StatusBsSynced.YES)
            assertThat(banner.statusActive).isEqualTo(false)
            assertThat(banner.statusArchived).isEqualTo(false)
            assertThat(banner.statusShow).isEqualTo(false)
        }.assertAll()
    }

    @Test
    fun convertToBannerDataForStatus_StoppedNotSyncedBannerStatuses() {
        val bannerDataForStatus = convertToBannerDataForStatus(
            VersionedGrutObject(
                getBanner(
                    bannerStatus = EBannerStatus.BST_STOPPED_VALUE,
                    moderationVerdict = BannerStatusModerate.YES,
                ), BANNER_VERSION
            ),
            getBanner(
                bannerStatus = EBannerStatus.BST_ACTIVE_VALUE,
                moderationVerdict = BannerStatusModerate.YES,
                bannerVersion = BANNER_VERSION
            ),
            mutableMapOf(CAMPAIGN_ID to CampaignsType.text)
        )

        SoftAssertions().apply {
            val banner = bannerDataForStatus.banner()
            assertThat(banner.statusModerate).isEqualTo(BannerStatusModerate.YES)
            assertThat(banner.statusPostModerate).isEqualTo(BannerStatusPostModerate.YES)
            assertThat(banner.statusBsSynced).isEqualTo(StatusBsSynced.NO)
            assertThat(banner.statusActive).isEqualTo(true)
            assertThat(banner.statusArchived).isEqualTo(false)
            assertThat(banner.statusShow).isEqualTo(false)
        }.assertAll()
    }

    @Test
    fun convertToBannerDataForStatus_ArchivedSyncedBannerStatuses() {
        val bannerDataForStatus = convertToBannerDataForStatus(
            VersionedGrutObject(
                getBanner(
                    bannerStatus = EBannerStatus.BST_ARCHIVED_VALUE,
                    moderationVerdict = BannerStatusModerate.YES,
                ), BANNER_VERSION
            ),
            null,
            mutableMapOf(CAMPAIGN_ID to CampaignsType.text)
        )

        SoftAssertions().apply {
            val banner = bannerDataForStatus.banner()
            assertThat(banner.statusModerate).isEqualTo(BannerStatusModerate.YES)
            assertThat(banner.statusPostModerate).isEqualTo(BannerStatusPostModerate.YES)
            assertThat(banner.statusBsSynced).isEqualTo(StatusBsSynced.YES)
            assertThat(banner.statusActive).isEqualTo(false)
            assertThat(banner.statusArchived).isEqualTo(true)
            assertThat(banner.statusShow).isEqualTo(false)
        }.assertAll()
    }

    @Test
    fun convertToBannerDataForStatus_RejectedSyncedBannerStatuses() {
        val bannerDataForStatus = convertToBannerDataForStatus(
            VersionedGrutObject(
                getBanner(
                    bannerStatus = EBannerStatus.BST_ACTIVE_VALUE,
                    moderationVerdict = BannerStatusModerate.NO,
                ), BANNER_VERSION
            ),
            null,
            mutableMapOf(CAMPAIGN_ID to CampaignsType.text)
        )

        SoftAssertions().apply {
            val banner = bannerDataForStatus.banner()
            assertThat(banner.statusModerate).isEqualTo(BannerStatusModerate.NO)
            assertThat(banner.statusPostModerate).isEqualTo(BannerStatusPostModerate.REJECTED)
            assertThat(banner.statusBsSynced).isEqualTo(StatusBsSynced.NO)
            assertThat(banner.statusActive).isEqualTo(false)
            assertThat(banner.statusArchived).isEqualTo(false)
            assertThat(banner.statusShow).isEqualTo(true)
        }.assertAll()
    }

    @Test
    fun convertToBannerDataForStatus_RejectedNotSyncedBannerStatuses() {
        val bannerDataForStatus = convertToBannerDataForStatus(
            VersionedGrutObject(
                getBanner(
                    bannerStatus = EBannerStatus.BST_ACTIVE_VALUE,
                    moderationVerdict = BannerStatusModerate.NO,
                ), BANNER_VERSION + 1
            ),
            getBanner(
                bannerStatus = EBannerStatus.BST_ACTIVE_VALUE,
                moderationVerdict = BannerStatusModerate.YES,
                bannerVersion = BANNER_VERSION
            ),
            mutableMapOf(CAMPAIGN_ID to CampaignsType.text)
        )

        SoftAssertions().apply {
            val banner = bannerDataForStatus.banner()
            assertThat(banner.statusModerate).isEqualTo(BannerStatusModerate.NO)
            assertThat(banner.statusPostModerate).isEqualTo(BannerStatusPostModerate.REJECTED)
            assertThat(banner.statusBsSynced).isEqualTo(StatusBsSynced.NO)
            assertThat(banner.statusActive).isEqualTo(true)
            assertThat(banner.statusArchived).isEqualTo(false)
            assertThat(banner.statusShow).isEqualTo(true)
        }.assertAll()
    }

    @Test
    fun convertToBannerDataForStatus_ModeratingBannerStatuses() {
        val bannerDataForStatus = convertToBannerDataForStatus(
            VersionedGrutObject(
                getBanner(
                    bannerStatus = EBannerStatus.BST_ACTIVE_VALUE,
                    moderationVerdict = BannerStatusModerate.SENT,
                ), BANNER_VERSION
            ),
            null,
            mutableMapOf(CAMPAIGN_ID to CampaignsType.text)
        )

        SoftAssertions().apply {
            val banner = bannerDataForStatus.banner()
            assertThat(banner.statusModerate).isEqualTo(BannerStatusModerate.SENT)
            assertThat(banner.statusPostModerate).isEqualTo(BannerStatusPostModerate.NO)
            assertThat(banner.statusBsSynced).isEqualTo(StatusBsSynced.NO)
            assertThat(banner.statusActive).isEqualTo(false)
            assertThat(banner.statusArchived).isEqualTo(false)
            assertThat(banner.statusShow).isEqualTo(true)
        }.assertAll()
    }
}

private fun getBanner(
    id: Long = BANNER_ID,
    bsBannerId: Long = BS_BANNER_ID,
    adGroupId: Long = ADGROUP_ID,
    campaignId: Long = CAMPAIGN_ID,
    bannerType: BannersBannerType = BannersBannerType.text,
    bannerStatus: Int = EBannerStatus.BST_DRAFT_VALUE,
    moderationVerdict: BannerStatusModerate? = null,
    bannerVersion: Long = BANNER_VERSION,
): TBannerV2 {
    return TBannerV2.newBuilder().apply {
        meta = TBannerV2Meta.newBuilder().apply {
            this.id = bsBannerId
            directId = id
            this.adGroupId = adGroupId
            directType = bannerType.toEnumValue()
            this.campaignId = campaignId
        }.build()
        spec = TBannerV2Spec.newBuilder().apply {
            status = bannerStatus
        }.build()
        status = TBannerV2Status.newBuilder().apply {
            moderationVerdict?.let {
                moderationStatus = TModerationStatus.newBuilder().apply {
                    mainStatus = TItemStatus.newBuilder().apply {
                        verdict = it.toEnumValue()
                        version = bannerVersion
                    }.build()
                }.build()
            }
        }.build()
    }.build()
}
