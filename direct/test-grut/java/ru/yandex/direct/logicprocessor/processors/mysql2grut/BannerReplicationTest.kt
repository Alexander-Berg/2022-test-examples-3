package ru.yandex.direct.logicprocessor.processors.mysql2grut

import com.google.common.primitives.UnsignedLong
import com.google.common.truth.extensions.proto.FieldScopes
import com.google.common.truth.extensions.proto.ProtoTruth
import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.jooq.impl.DSL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields
import ru.yandex.direct.core.entity.banner.model.Language
import ru.yandex.direct.core.entity.banner.model.old.OldBanner
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.old.OldStatusBannerImageModerate
import ru.yandex.direct.core.entity.banner.model.old.StatusPhoneFlagModerate
import ru.yandex.direct.core.entity.banner.model.old.StatusSitelinksModerate
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.bs.common.service.BsBannerIdCalculator.calculateBsBannerId
import ru.yandex.direct.core.entity.bs.common.service.BsOrderIdCalculator
import ru.yandex.direct.core.entity.uac.grut.GrutContext
import ru.yandex.direct.core.grut.api.BannerGrut
import ru.yandex.direct.core.grut.api.BannerGrutApi
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.data.TestBanners
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestVcards
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables.BANNERS
import ru.yandex.direct.dbschema.ppc.Tables.BANNER_IMAGES
import ru.yandex.direct.dbschema.ppc.enums.BannerImagesStatusshow
import ru.yandex.direct.dbschema.ppc.enums.BannersPhoneflag
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusmoderate
import ru.yandex.direct.dbschema.ppc.enums.BannersType
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.ess.logicobjects.mysql2grut.Mysql2GrutReplicationObject
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorGrutTestConfiguration
import ru.yandex.grut.objects.proto.Banner
import ru.yandex.grut.objects.proto.BannerV2
import ru.yandex.grut.objects.proto.BannerV2.TBannerV2Spec
import ru.yandex.grut.objects.proto.BannerV2.TBannerV2Spec.TImage.EImageType
import ru.yandex.grut.objects.proto.BannerV2.TBannerV2Spec.TSitelinkSet
import ru.yandex.grut.objects.proto.BannerV2.TBannerV2Spec.TSitelinkSet.TSitelink
import ru.yandex.grut.objects.proto.BannerV2.TBannerV2Status.TModerationStatus
import ru.yandex.grut.objects.proto.BannerV2.TBannerV2Status.TModerationStatus.EVerdict
import ru.yandex.grut.objects.proto.BannerV2.TBannerV2Status.TModerationStatus.TItemStatus
import ru.yandex.grut.objects.proto.Language.ELanguage
import ru.yandex.grut.auxiliary.proto.MdsInfo.TMdsFileInfo
import ru.yandex.grut.objects.proto.MdsInfo.TAvatarsImageMeta
import ru.yandex.grut.objects.proto.MdsInfo.TAvatarsImageMeta.TFormat
import ru.yandex.grut.objects.proto.MdsInfo.TAvatarsImageMeta.TFormat.TSmartCenter
import ru.yandex.grut.objects.proto.client.Schema

@ContextConfiguration(classes = [EssLogicProcessorGrutTestConfiguration::class])
@ExtendWith(SpringExtension::class)
class BannerReplicationTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var processor: Mysql2GrutReplicationProcessor

    @Autowired
    private lateinit var replicationApiService: GrutApiService

    @Autowired
    private lateinit var bannerTypedRepository: BannerTypedRepository

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var grutContext: GrutContext

    private lateinit var clientInfo: ClientInfo
    private lateinit var campaignInfo: CampaignInfo
    private lateinit var adGroupInfo: AdGroupInfo

    private companion object {
        val acceptedModerationStatus : TModerationStatus = TModerationStatus.newBuilder()
            .setMainStatus(
                TItemStatus.newBuilder()
                    .setVerdict(EVerdict.MV_ACCEPTED.number)
                    .build()
            ).build()
    }

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        replicationApiService.clientGrutDao.createOrUpdateClients(listOf(ClientGrutModel(clientInfo.client!!, listOf())))

        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withOrderId(0L)
        campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        processor.withShard(campaignInfo.shard)

        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo)

        // реплицируем кампанию и группу
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId),
                Mysql2GrutReplicationObject(adGroupId = adGroupInfo.adGroupId),
            ))
    }

    @AfterEach
    fun after() {
        replicationApiService.clientGrutDao.deleteObjects(listOf(clientInfo.clientId!!.asLong()))
    }

    @Test
    fun createBannerTest() {
        val body = "my body"
        val title = "my title"
        val titleExtension = "extension"
        val domain = "direct.yandex.ru"
        val href = "https://$domain/test"

        val banner = TestBanners.activeTextBanner()
            .withBsBannerId(0)
            .withBody(body)
            .withTitle(title)
            .withHref(href)
            .withLanguage(Language.EN)
            .withDomain(domain)
            .withTitleExtension(titleExtension)
            .withIsMobile(false)
            .withGeoFlag(true)
        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)

        assertThatCode {
            processor.process(
                listOf(
                    Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)
                )
            )
        }.doesNotThrowAnyException()

        val createdBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        val expectedBanner = Schema.TBannerV2.newBuilder().apply {
            meta = Schema.TBannerV2Meta.newBuilder()
                .setDirectId(bannerInfo.bannerId)
                .setAdGroupId(adGroupInfo.adGroupId)
                .setCampaignId(campaignInfo.campaignId + BsOrderIdCalculator.ORDER_ID_OFFSET)
                .setSource(Banner.EBannerSource.BS_DIRECT.number)
                .setDirectType(BannerV2.EBannerType.BT_TEXT.number)
                .build()
            status = BannerV2.TBannerV2Status.newBuilder()
                .setSkipModeration(true)
                .setModerationStatus(acceptedModerationStatus)
                .build()
            spec = TBannerV2Spec.newBuilder()
                .setIsMobile(false)
                .setHref(href)
                .setDomain(domain)
                .setTitle(title)
                .setBody(body)
                .setTitleExtension(titleExtension)
                .setBody(body)
                .setLanguage(ELanguage.LANG_EN.number)
                .setFlags(
                    TBannerV2Spec.TFlags.newBuilder()
                        .setGeoflag(true)
                        .build()
                )
                .build()
        }.build()
        ProtoTruth.assertThat(createdBanner)
            .withPartialScope(FieldScopes.fromSetFields(expectedBanner))
            .isEqualTo(expectedBanner)
    }

    @Test
    fun createBannerWithSkipModerationIsFalseTest() {
        val banner = TestBanners.activeTextBanner()
        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)

        steps.featureSteps()
            .addClientFeature(clientInfo.clientId, FeatureName.MODERATION_BANNERS_IN_GRUT_ENABLED, true)

        assertThatCode {
            processor.process(
                listOf(
                    Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)
                )
            )
        }.doesNotThrowAnyException()

        val createdBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        assertThat(createdBanner).isNotNull
        assertThat(createdBanner!!.status.skipModeration).isFalse
        assertThat(createdBanner.status.hasModerationStatus()).isFalse
    }

    @Test
    fun notModeratedBannerNotCreatedTest() {
        val banner = TestBanners.activeTextBanner()
            .withStatusModerate(OldBannerStatusModerate.SENT)
            .withBsBannerId(0)
            .withBody("Body")
        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)

        assertThatCode { processor.process(listOf(Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId))) }
            .doesNotThrowAnyException()

        val createdBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        assertThat(createdBanner).isNull()
    }

    @Test
    fun createBannerWithImageTest() {
        val banner = TestBanners.activeTextBanner()
            .withBsBannerId(0)

        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)
        val bannerImageInfo = steps.bannerSteps().createBannerImage(bannerInfo,
            steps.bannerSteps().createBannerImageFormat(clientInfo),
            TestBanners.defaultBannerImage(bannerInfo.bannerId, RandomStringUtils.randomAlphanumeric(16))
                .withStatusModerate(OldStatusBannerImageModerate.YES)
                .withBsBannerId(0L)
        )

        assertThatCode {
            processor.process(
                listOf(
                    Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)
                )
            )
        }.doesNotThrowAnyException()

        val createdBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        val expectedImage = TBannerV2Spec.TImage.newBuilder()
            .setImageHash(bannerImageInfo.bannerImageFormat.imageHash)
            .setImageType(EImageType.IT_REGULAR.number)
            .setMdsFileInfo(TMdsFileInfo.newBuilder()
                .setNamespace("direct")
                .setGroupId(31337)
                .setEnvironment(TMdsFileInfo.EEnvironment.ENV_TEST.number)
                .setMdsFileName(bannerImageInfo.bannerImageFormat.imageHash)
                .build())
            .setAvatarsImageMeta(TAvatarsImageMeta.newBuilder()
                .setCrc64(UnsignedLong.valueOf("FE0A206B58E39F91", 16).toLong())
                .setColorWizBackground(0xFEFFFF)
                .setColorWizButton(0xECE3D9)
                .setColorWizButtonText(0x814300)
                .setAverageColor(0x00CCCC)
                .setMainColor(0xF8F8F8)
                .setBackgroundColors(TAvatarsImageMeta.TBackgroundColors.newBuilder()
                    .setTop(0xFFFEFF)
                    .setLeft(0xFEFFFF)
                    .setRight(0x00FFFF)
                    .setBottom(0x11FFFF)
                    .build())
                .addFormats(TFormat.newBuilder()
                    .setFormatName("x300")
                    .setWidth(300)
                    .setHeight(399)
                    .addSmartCenters(TSmartCenter.newBuilder()
                        .setX(1)
                        .setY(0)
                        .setW(300)
                        .setH(399)
                        .build())
                    .build())
                .addFormats(TFormat.newBuilder()
                    .setFormatName("x450")
                    .setWidth(450)
                    .setHeight(599)
                    // сортируются по имени формата, который не отправляется
                    .addSmartCenters(TSmartCenter.newBuilder()
                        .setX(0)
                        .setY(1)
                        .setW(450)
                        .setH(450)
                        .build())
                    .addSmartCenters(TSmartCenter.newBuilder()
                        .setX(1)
                        .setY(0)
                        .setW(449)
                        .setH(599)
                        .build())
                    .build())
                .build())
        val expectedBanner = Schema.TBannerV2.newBuilder().apply {
            meta = Schema.TBannerV2Meta.newBuilder()
                .setDirectId(bannerInfo.bannerId)
                .setAdGroupId(adGroupInfo.adGroupId)
                .setCampaignId(campaignInfo.campaignId + BsOrderIdCalculator.ORDER_ID_OFFSET)
                .setSource(Banner.EBannerSource.BS_DIRECT.number)
                .setDirectType(BannerV2.EBannerType.BT_TEXT.number)
                .setImageBannerId(calculateBsBannerId(bannerImageInfo.bannerImageId))
                .build()
            status = BannerV2.TBannerV2Status.newBuilder()
                .setSkipModeration(true)
                .setModerationStatus(
                    TModerationStatus.newBuilder()
                        .setMainStatus(
                            TItemStatus.newBuilder()
                                .setVerdict(EVerdict.MV_ACCEPTED.number)
                                .build()
                        )
                        .addImagesStatus(
                            TItemStatus.newBuilder()
                                .setVerdict(EVerdict.MV_ACCEPTED.number)
                                .build()
                        )
                        .build()
                )
                .build()
            spec = TBannerV2Spec.newBuilder()
                .addImages(expectedImage)
                .build()
        }.build()
        ProtoTruth.assertThat(createdBanner)
            .withPartialScope(FieldScopes.fromSetFields(expectedBanner))
            .isEqualTo(expectedBanner)
    }

    @ParameterizedTest
    @EnumSource(value = OldStatusBannerImageModerate::class, names = ["YES"], mode = EnumSource.Mode.EXCLUDE)
    fun addNewUnacceptedImageReplicatedWithoutImageIdTest(imageStatusModerate: OldStatusBannerImageModerate) {
        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withOrderId(0L)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        val adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo)
        val bannerInfo = steps.bannerSteps().createBanner(TestBanners.activeTextBanner().withBsBannerId(0), adGroupInfo)
        steps.bannerSteps().createBannerImage(
            bannerInfo,
            steps.bannerSteps().createBannerImageFormat(clientInfo),
            TestBanners.defaultBannerImage(bannerInfo.bannerId, RandomStringUtils.randomAlphanumeric(16))
                .withStatusModerate(imageStatusModerate)
                .withBsBannerId(0)
        )

        processor.withShard(campaignInfo.shard)
        assertThatCode {
            processor.process(
                listOf(
                    Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId),
                    Mysql2GrutReplicationObject(adGroupId = adGroupInfo.adGroupId),
                    Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)
                )
            )
        }.doesNotThrowAnyException()

        expectBannerWithoutImage(bannerInfo.bannerId, 0)
    }

    private fun expectBannerWithoutImage(bannerId: Long, expectedImageId: Long) {
        val createdBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerId)
        val expectedBanner = Schema.TBannerV2.newBuilder().apply {
            meta = Schema.TBannerV2Meta.newBuilder()
                .setDirectId(bannerId)
                .setImageBannerId(expectedImageId)
                .build()
            status = BannerV2.TBannerV2Status.newBuilder()
                .setModerationStatus(TModerationStatus.getDefaultInstance())
                .build()
            spec = TBannerV2Spec.getDefaultInstance()
        }.build()
        ProtoTruth.assertThat(createdBanner)
            .withPartialScope(
                FieldScopes.fromSetFields(expectedBanner)
                    .allowingFieldDescriptors(TModerationStatus.getDescriptor().findFieldByName("images_status"))
                    .allowingFieldDescriptors(TBannerV2Spec.getDescriptor().findFieldByName("images"))
            )
            .isEqualTo(expectedBanner)
    }

    @Test
    fun replicateAcceptedImageStatusModerateTest() {
        replicateImageStatusModerateTestBase(OldStatusBannerImageModerate.YES, EVerdict.MV_ACCEPTED)
    }

    @Test
    fun replicateRejectedImageStatusModerateTest() {
        replicateImageStatusModerateTestBase(OldStatusBannerImageModerate.NO, EVerdict.MV_REJECTED)
    }

    @Test
    fun replicateModeratingImageStatusModerateTest() {
        replicateImageStatusModerateTestBase(OldStatusBannerImageModerate.READY, EVerdict.MV_UNKNOWN)
    }

    private fun replicateImageStatusModerateTestBase(
        imageStatusModerate: OldStatusBannerImageModerate,
        expectedVerdict: EVerdict
    ) {
        val campaign = TestCampaigns.activeTextCampaign(null, null)
            .withOrderId(0L)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        val adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo)
        val bannerInfo = steps.bannerSteps().createBanner(TestBanners.activeTextBanner().withBsBannerId(0), adGroupInfo)
        val bannerImageInfo = steps.bannerSteps().createBannerImage(
            bannerInfo,
            steps.bannerSteps().createBannerImageFormat(clientInfo),
            TestBanners.defaultBannerImage(bannerInfo.bannerId, RandomStringUtils.randomAlphanumeric(16))
                .withStatusModerate(imageStatusModerate)
                .withBsBannerId(107562878)
        )

        processor.withShard(campaignInfo.shard)
        assertThatCode {
            processor.process(
                listOf(
                    Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId),
                    Mysql2GrutReplicationObject(adGroupId = adGroupInfo.adGroupId),
                    Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)
                )
            )
        }.doesNotThrowAnyException()

        val createdBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        val expectedBanner = Schema.TBannerV2.newBuilder().apply {
            meta = Schema.TBannerV2Meta.newBuilder()
                .setDirectId(bannerInfo.bannerId)
                .setImageBannerId(107562878)
                .build()
            status = BannerV2.TBannerV2Status.newBuilder()
                .setModerationStatus(
                    TModerationStatus.newBuilder()
                        .addImagesStatus(
                            TItemStatus.newBuilder()
                                .setVerdict(expectedVerdict.number)
                                .build()
                        )
                        .build()
                )
                .build()
            spec = TBannerV2Spec.newBuilder()
                .addImages(
                    TBannerV2Spec.TImage.newBuilder()
                        .setImageHash(bannerImageInfo.bannerImageFormat.imageHash)
                        .build()
                )
                .build()
        }.build()
        ProtoTruth.assertThat(createdBanner)
            .withPartialScope(FieldScopes.fromSetFields(expectedBanner))
            .isEqualTo(expectedBanner)
    }

    @Test
    fun addImageToBannerReplicateImageIdTest(){
        updateBannerAddImageTestBase(false, 74402272L)
    }

    @Test
    fun addDeletedImageToBannerWhichWasInBsReplicateImageIdTest(){
        updateBannerAddImageTestBase(true, 74402272L)
    }

    @Test
    fun addDeletedImageToBannerImageIdNotReplicatedTest(){
        updateBannerAddImageTestBase(true, 0L)
    }

    private fun updateBannerAddImageTestBase(imageDeleted: Boolean, imageBsBannerId: Long) {
        val banner = TestBanners.activeTextBanner()
            .withBsBannerId(0)

        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)
            )
        )

        val createdBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        assertThat(createdBanner).isNotNull
        assertThat(createdBanner!!.meta.imageBannerId).isEqualTo(0)

        val bannerImageInfo = steps.bannerSteps().createBannerImage(bannerInfo,
            steps.bannerSteps().createBannerImageFormat(clientInfo),
            TestBanners.defaultBannerImage(bannerInfo.bannerId, RandomStringUtils.randomAlphanumeric(16))
                .withStatusModerate(OldStatusBannerImageModerate.YES)
                .withStatusShow(!imageDeleted)
                .withBsBannerId(imageBsBannerId)
        )
        assertThatCode {
            processor.process(listOf(Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)))
        }.doesNotThrowAnyException()

        val updatedBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        val expectedBanner = Schema.TBannerV2.newBuilder().apply {
            meta = Schema.TBannerV2Meta.newBuilder()
                .setDirectId(bannerInfo.bannerId)
                .setImageBannerId(imageBsBannerId)
                .build()
            status = BannerV2.TBannerV2Status.getDefaultInstance()
            spec = TBannerV2Spec.getDefaultInstance()
        }.build()
        ProtoTruth.assertThat(updatedBanner)
            .withPartialScope(FieldScopes.fromSetFields(expectedBanner))
            .isEqualTo(expectedBanner)
    }

    @Test
    fun removingImageFromExistingBannerTest() {
        val campaign = TestCampaigns.activeTextCampaign(null, null).withOrderId(0L)
        val campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo)
        val adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo)
        val bannerInfo = steps.bannerSteps().createBanner(TestBanners.activeTextBanner().withBsBannerId(0), adGroupInfo)
        val bannerImageInfo = steps.bannerSteps().createBannerImage(bannerInfo,
            steps.bannerSteps().createBannerImageFormat(clientInfo),
            TestBanners.defaultBannerImage(bannerInfo.bannerId, RandomStringUtils.randomAlphanumeric(16))
                .withStatusModerate(OldStatusBannerImageModerate.YES)
                .withBsBannerId(0L)
        )

        processor.withShard(campaignInfo.shard)
        assertThatCode {
            processor.process(
                listOf(
                    Mysql2GrutReplicationObject(campaignId = campaignInfo.campaignId),
                    Mysql2GrutReplicationObject(adGroupId = adGroupInfo.adGroupId),
                    Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)
                )
            )
        }.doesNotThrowAnyException()

        val sourceBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        assertThat(sourceBanner).isNotNull
        assertThat(sourceBanner!!.spec.imagesList).isNotEmpty
        assertThat(sourceBanner.status.moderationStatus.imagesStatusList).isNotEmpty

        // удаляем картинку
        val imageBannerId = calculateBsBannerId(bannerImageInfo.bannerImageId)
        dslContextProvider.ppc(clientInfo.shard)
            .update(BANNER_IMAGES)
            .set(BANNER_IMAGES.STATUS_SHOW, BannerImagesStatusshow.No)
            .set(BANNER_IMAGES.BANNER_ID, imageBannerId)
            .where(BANNER_IMAGES.IMAGE_ID.eq(bannerImageInfo.bannerImageId))
            .execute()
        assertThatCode {
            processor.process(listOf(Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)))
        }.doesNotThrowAnyException()

        // проверяем, что id картинки остался, а самой картинки и ее статуса модерации — нет
        expectBannerWithoutImage(bannerInfo.bannerId, imageBannerId)
    }

    @Test
    fun createBannerWithVcardTest() {
        bannerWithVcardTest(StatusPhoneFlagModerate.YES, EVerdict.MV_ACCEPTED)
    }

    @Test
    fun bannerWithRejectedVcardTest() {
        bannerWithVcardTest(StatusPhoneFlagModerate.NO, EVerdict.MV_REJECTED)
    }

    @Test
    fun bannerWithModeratingVcardTest() {
        bannerWithVcardTest(StatusPhoneFlagModerate.SENT, EVerdict.MV_UNKNOWN)
    }

    private fun bannerWithVcardTest(bannerPhoneFlag: StatusPhoneFlagModerate, expectedVerdict: EVerdict) {
        val vcardInfo = steps.vcardSteps().createVcard(TestVcards.fullVcard(), clientInfo)
        val banner = TestBanners.activeTextBanner()
            .withBsBannerId(0)
            .withVcardId(vcardInfo.vcardId)
            .withPhoneFlag(bannerPhoneFlag)
        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)

        assertThatCode {
            processor.process(
                listOf(
                    Mysql2GrutReplicationObject(vcardId = vcardInfo.vcardId),
                    Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)
                )
            )
        }.doesNotThrowAnyException()

        val updatedBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        val expectedBanner = Schema.TBannerV2.newBuilder().apply {
            meta = Schema.TBannerV2Meta.newBuilder()
                .setDirectId(bannerInfo.bannerId)
                .build()
            status = BannerV2.TBannerV2Status.newBuilder()
                .setSkipModeration(true)
                .setModerationStatus(
                    TModerationStatus.newBuilder()
                        .setVcardStatus(
                            TItemStatus.newBuilder()
                                .setVerdict(expectedVerdict.number)
                                .build()
                        )
                        .build()
                )
                .build()
            spec = TBannerV2Spec.newBuilder()
                .setVcardId(vcardInfo.vcardId)
                .build()
        }.build()
        ProtoTruth.assertThat(updatedBanner)
            .withPartialScope(FieldScopes.fromSetFields(expectedBanner))
            .isEqualTo(expectedBanner)
    }

    @Test
    fun bannerWithAbsentVcardReplicatedWithoutVcardTest() {
        val vcardInfo = steps.vcardSteps().createVcard(TestVcards.fullVcard(), clientInfo)
        val banner = TestBanners.activeTextBanner()
            .withBsBannerId(0)
            .withVcardId(vcardInfo.vcardId)
            .withPhoneFlag(StatusPhoneFlagModerate.YES)
        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)

        assertThatCode {
            processor.process(
                listOf(
                    Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)
                )
            )
        }.doesNotThrowAnyException()

        val updatedBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        val expectedBanner = Schema.TBannerV2.newBuilder().apply {
            meta = Schema.TBannerV2Meta.newBuilder()
                .setDirectId(bannerInfo.bannerId)
                .build()
            status = BannerV2.TBannerV2Status.newBuilder()
                .setSkipModeration(true)
                .setModerationStatus(TModerationStatus.getDefaultInstance())
                .build()
            spec = TBannerV2Spec.newBuilder()
                .setVcardId(0)
                .build()
        }.build()
        ProtoTruth.assertThat(updatedBanner)
            .withPartialScope(
                FieldScopes.fromSetFields(expectedBanner)
                    .allowingFieldDescriptors(TModerationStatus.getDescriptor().findFieldByName("vcard_status"))
            ).isEqualTo(expectedBanner)
    }

    @Test
    fun removingVcardFromExistingBannerTest() {
        val vcardInfo = steps.vcardSteps().createVcard(TestVcards.fullVcard(), clientInfo)
        val banner = TestBanners.activeTextBanner()
            .withBsBannerId(0)
            .withDomain("test-domain.com")
            .withVcardId(vcardInfo.vcardId)
            .withPhoneFlag(StatusPhoneFlagModerate.YES)
        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(vcardId = vcardInfo.vcardId),
                Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId),
            ))

        val sourceBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        assertThat(sourceBanner).isNotNull
        assertThat(sourceBanner!!.spec.vcardId).isGreaterThan(0)
        assertThat(sourceBanner.status.moderationStatus.vcardStatus.verdict).isGreaterThan(0)

        // делаем апдейт, который должен проехать по репликации
        dslContextProvider.ppc(clientInfo.shard)
            .update(BANNERS)
            .set(BANNERS.VCARD_ID, DSL.castNull(Long::class.java))
            .set(BANNERS.PHONEFLAG, BannersPhoneflag.New)
            .where(BANNERS.BID.eq(bannerInfo.bannerId))
            .execute()

        // теперь реплицировать баннер
        assertThatCode {
            processor.process(
                listOf(Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId))
            )
        }.doesNotThrowAnyException()

        val updatedBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        val expectedBanner = Schema.TBannerV2.newBuilder().apply {
            meta = Schema.TBannerV2Meta.newBuilder()
                .setDirectId(bannerInfo.bannerId)
                .build()
            status = BannerV2.TBannerV2Status.newBuilder()
                .setSkipModeration(true)
                .setModerationStatus(acceptedModerationStatus)
                .build()
            spec = TBannerV2Spec.newBuilder()
                .setVcardId(0)
                .build()
        }.build()
        ProtoTruth.assertThat(updatedBanner)
            .withPartialScope(FieldScopes.fromSetFields(expectedBanner)
                .allowingFieldDescriptors(TModerationStatus.getDescriptor().findFieldByName("vcard_status")))
            .isEqualTo(expectedBanner)
    }

    /**
     * проверяем, что ни vcard_id, ни vcard_status при этом не создаются
     */
    @Test
    fun bannerWithoutVcardReplicationTest(){
        val banner = TestBanners.activeTextBanner()
            .withBsBannerId(0)
            .withDomain("test-domain.com")

            .withPhoneFlag(StatusPhoneFlagModerate.NEW)
        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)

        assertThatCode {
            processor.process(
                listOf(
                    Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId),
                )
            )
        }.doesNotThrowAnyException()

        val updatedBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        val expectedBanner = Schema.TBannerV2.newBuilder().apply {
            meta = Schema.TBannerV2Meta.newBuilder()
                .setDirectId(bannerInfo.bannerId)
                .build()
            status = BannerV2.TBannerV2Status.newBuilder()
                .setSkipModeration(true)
                .setModerationStatus(acceptedModerationStatus)
                .build()
            spec = TBannerV2Spec.newBuilder()
                .setVcardId(0)
                .build()
        }.build()
        ProtoTruth.assertThat(updatedBanner)
            .withPartialScope(FieldScopes.fromSetFields(expectedBanner)
                .allowingFieldDescriptors(TModerationStatus.getDescriptor().findFieldByName("vcard_status")))
            .isEqualTo(expectedBanner)
    }

    /**
     * Тест проверяет, что поля, которые не записываются из репликации не перетирают записанные из интерфейса значения
     * Например, sitelink_set
     */
    @Test
    fun updateBannerNotRewriteSpecTest() {
        val sitelinksSet = steps.sitelinkSetSteps().createDefaultSitelinkSet()
        val sitelinksSetNew = steps.sitelinkSetSteps().createDefaultSitelinkSet()
        val bannerTitle = "Title"
        val bannerBody = "Body"
        val bannerNewBody = "Body new"
        val banner = TestBanners.activeTextBanner()
            .withBsBannerId(0)
            .withTitle(bannerTitle)
            .withBody(bannerBody)
            .withDomain("test-domain.com")
            .withSitelinksSetId(sitelinksSet.sitelinkSetId)
            .withStatusSitelinksModerate(StatusSitelinksModerate.YES)
        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)

        val mysqlBanner = bannerTypedRepository.getTyped(clientInfo.shard, listOf(bannerInfo.bannerId))[0] as BannerWithSystemFields
        // используем другой сервис, так как хотим создать сайтлинки, которые репликация еще не умеет
        val bannerGrutApi = BannerGrutApi(grutContext)
        bannerGrutApi.createOrUpdateBanner(
            BannerGrut(
                banner = mysqlBanner,
                orderId = campaignInfo.campaignId + 100_000_000,
                turbolandingsById = mapOf(),
                sitelinkSet = sitelinksSet.sitelinkSet,
                bannerImageFormat = null
            )
        )

        val sourceBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        assertThat(sourceBanner).isNotNull
        assertThat(sourceBanner!!.spec.sitelinkSet.hasId()).isTrue

        // делаем апдейт, который должен проехать по репликации
        dslContextProvider.ppc(clientInfo.shard)
            .update(BANNERS)
            .set(BANNERS.BODY, bannerNewBody)
            .set(BANNERS.SITELINKS_SET_ID, sitelinksSetNew.sitelinkSetId)
            .where(BANNERS.BID.eq(bannerInfo.bannerId))
            .execute()
        // теперь пробуем реплицировать баннер, sitelinks не должен перетереться
        assertThatCode {
            processor.process(
                listOf(Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId))
            )
        }.doesNotThrowAnyException()

        val updatedBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        val expectedBanner = Schema.TBannerV2.newBuilder().apply {
            meta = Schema.TBannerV2Meta.newBuilder()
                .setDirectId(bannerInfo.bannerId)
                .setAdGroupId(adGroupInfo.adGroupId)
                .setCampaignId(campaignInfo.campaignId + BsOrderIdCalculator.ORDER_ID_OFFSET)
                .setSource(Banner.EBannerSource.BS_DIRECT.number)
                .setDirectType(BannerV2.EBannerType.BT_TEXT.number)
                .build()
            status = BannerV2.TBannerV2Status.newBuilder()
                .setSkipModeration(true)
                .setModerationStatus(acceptedModerationStatus)
                .build()
            spec = TBannerV2Spec.newBuilder()
                // sitelinks не должен удалиться или измениться в результате репликации
                .setSitelinkSet(
                    TSitelinkSet.newBuilder()
                        .addSitelinks(
                            TSitelink.newBuilder()
                                .setHref(sitelinksSet.sitelinkSet.sitelinks[0].href)
                                .setDescription(sitelinksSet.sitelinkSet.sitelinks[0].description)
                                .setTitle(sitelinksSet.sitelinkSet.sitelinks[0].title)
                                .build()
                        )
                        .addSitelinks(
                            TSitelink.newBuilder()
                                .setHref(sitelinksSet.sitelinkSet.sitelinks[1].href)
                                .setDescription(sitelinksSet.sitelinkSet.sitelinks[1].description)
                                .setTitle(sitelinksSet.sitelinkSet.sitelinks[1].title)
                                .build()
                        )
                        .build()
                )
                .setTitle(bannerTitle)
                // Body должен поменяться
                .setBody(bannerNewBody)
                .build()
        }.build()
        ProtoTruth.assertThat(updatedBanner)
            .withPartialScope(FieldScopes.fromSetFields(expectedBanner))
            .isEqualTo(expectedBanner)
    }

    /**
     * Тест проверяет, если в спеке есть null-значения, то обновление не упадет с ошибкой
     */
    @Test
    fun updateBannerWithNullFieldsInSpecTest() {
        val bannerInfo = steps.bannerSteps().createActiveTextBanner(adGroupInfo)

        processor.process(
            listOf(
                Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId),
            ))

        // Поменяем часть полей на null
        dslContextProvider.ppc(clientInfo.shard)
            .update(BANNERS)
            .set(BANNERS.BODY, DSL.castNull(String::class.java))
            .set(BANNERS.TITLE, DSL.castNull(String::class.java))
            .set(BANNERS.TITLE_EXTENSION, DSL.castNull(String::class.java))
            .set(BANNERS.HREF, DSL.castNull(String::class.java))
            .set(BANNERS.DOMAIN, DSL.castNull(String::class.java))
            .set(BANNERS.TYPE, BannersType.mobile)
            .where(BANNERS.BID.eq(bannerInfo.bannerId))
            .execute()
        // теперь пробуем реплицировать баннер, vcard_id не должно перетереться
        assertThatCode {
            processor.process(
                listOf(Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId))
            )
        }.doesNotThrowAnyException()

        val updatedBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        val expectedBanner = Schema.TBannerV2.newBuilder().apply {
            meta = Schema.TBannerV2Meta.newBuilder()
                .setDirectId(bannerInfo.bannerId)
                .build()
            spec = TBannerV2Spec.newBuilder()
                // проверим, что доехало измененное поле
                .setIsMobile(true)
                .build()
        }.build()
        // проверим, что затерлись поля
        val deletedFields = TBannerV2Spec.getDescriptor().fields
            .filter { setOf("body", "title", "title_extension", "href", "domain").contains(it.name) }
            .toList()
        ProtoTruth.assertThat(updatedBanner)
            .withPartialScope(FieldScopes.fromSetFields(expectedBanner).allowingFieldDescriptors(deletedFields))
            .isEqualTo(expectedBanner)
    }

    @Test
    fun replicateRejectedBannerTest() {
        val banner = TestBanners.activeTextBanner()
            .withBsBannerId(0)
            .withDomain("test-domain.com")
        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)

        // реплицируем кампанию и группу
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)
            ))
        val createdBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        assertThat(createdBanner).isNotNull

        dslContextProvider.ppc(clientInfo.shard)
            .update(BANNERS)
            .set(BANNERS.STATUS_MODERATE, BannersStatusmoderate.No)
            .where(BANNERS.BID.eq(bannerInfo.bannerId))
            .execute()
        // теперь пробуем реплицировать баннер
        assertThatCode {
            processor.process(
                listOf(Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId))
            )
        }.doesNotThrowAnyException()

        val rejectedBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        val expectedBanner = Schema.TBannerV2.newBuilder().apply {
            meta = Schema.TBannerV2Meta.newBuilder()
                .setDirectId(bannerInfo.bannerId)
                .setAdGroupId(adGroupInfo.adGroupId)
                .setCampaignId(campaignInfo.campaignId + BsOrderIdCalculator.ORDER_ID_OFFSET)
                .setSource(Banner.EBannerSource.BS_DIRECT.number)
                .setDirectType(BannerV2.EBannerType.BT_TEXT.number)
                .build()
            status = BannerV2.TBannerV2Status.newBuilder()
                .setSkipModeration(true)
                .setModerationStatus(TModerationStatus.newBuilder()
                    .setMainStatus(
                        TItemStatus.newBuilder()
                            .setVerdict(EVerdict.MV_REJECTED.number)
                            .build()
                    ).build())
                .build()
        }.build()
        ProtoTruth.assertThat(rejectedBanner)
            .withPartialScope(FieldScopes.fromSetFields(expectedBanner))
            .isEqualTo(expectedBanner)
    }

    @Test
    fun deleteBannerTest() {
        val banner = TestBanners.activeTextBanner()
            .withBsBannerId(0)
        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)

        // Создам через репликацию кампанию, группу и баннер
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)
            ))
        val createdBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        assertThat(createdBanner).isNotNull

        // Удаляем баннер в mysql
        dslContextProvider.ppc(clientInfo.shard)
            .deleteFrom(BANNERS)
            .where(BANNERS.BID.eq(bannerInfo.bannerId))
            .execute()
        // теперь пробуем реплицировать баннер
        assertThatCode {
            processor.process(
                listOf(Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId, isDeleted = true))
            )
        }.doesNotThrowAnyException()

        val deletedBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        assertThat(deletedBanner).isNull()
    }

    @Test
    fun deleteExistingBannerTest() {
        val banner = TestBanners.activeTextBanner()
            .withBsBannerId(0)
        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)

        // Создам через репликацию кампанию, группу и баннер
        processor.process(
            listOf(
                Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId)
            ))
        val createdBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        assertThat(createdBanner).isNotNull


        // теперь пробуем реплицировать на самом деле неудаленный баннер
        processor.process(
            listOf(Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId, isDeleted = true))
        )

        val deletedBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        assertThat(deletedBanner).isNotNull
    }

    /**
     * Тест проверяет, что NULL поля не приводят к падению записи
     */
    @Test
    fun createBannerWithNullableFieldsInSpecTest() {
        val banner = TestBanners.activeTextBanner()
            .withBsBannerId(0)
            .withBody(null)
            .withTitle(null)
            .withTitleExtension(null)
            .withHref(null)
            .withDomain(null)
            .withLanguage(Language.TR)
        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)

        assertThatCode {
            processor.process(
                listOf(
                    Mysql2GrutReplicationObject(bannerId = bannerInfo.bannerId),
                )
            )
        }.doesNotThrowAnyException()

        val createdBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerInfo.bannerId)
        val expectedBanner = Schema.TBannerV2.newBuilder().apply {
            meta = Schema.TBannerV2Meta.newBuilder()
                .setDirectId(bannerInfo.bannerId)
                .build()
            status = BannerV2.TBannerV2Status.newBuilder()
                .setSkipModeration(true)
                .setModerationStatus(acceptedModerationStatus)
                .build()
            spec = TBannerV2Spec.newBuilder()
                .setLanguage(ELanguage.LANG_TR.number)
                .build()
        }.build()
        // проверим, что поля пустые
        val nullFields = TBannerV2Spec.getDescriptor().fields
            .filter { setOf("body", "title", "title_extension", "href", "domain").contains(it.name) }
            .toList()
        ProtoTruth.assertThat(createdBanner)
            .withPartialScope(FieldScopes.fromSetFields(expectedBanner).allowingFieldDescriptors(nullFields))
            .isEqualTo(expectedBanner)
    }

    @Test
    fun createCpmBannerTest() {
        val createCreative = steps.creativeSteps().createCreative(clientInfo)
        val b = TestBanners.activeCpmBanner(campaignInfo.campaignId, adGroupInfo.adGroupId, createCreative.creativeId)
            .withBsBannerId(0)

        createBannerByTypeTest(b, BannerV2.EBannerType.BT_CPM)

        steps.creativeSteps().deleteCreativesByIds(clientInfo.shard, createCreative.creativeId)
    }

    @Test
    fun createCpcVideoBannerTest() {
        val createCreative = steps.creativeSteps().createCreative(clientInfo)
        val banner =
            TestBanners.activeCpcVideoBanner(campaignInfo.campaignId, adGroupInfo.adGroupId, createCreative.creativeId)
                .withBsBannerId(0)

        createBannerByTypeTest(banner, BannerV2.EBannerType.BT_CPC_VIDEO)

        steps.creativeSteps().deleteCreativesByIds(clientInfo.shard, createCreative.creativeId)
    }

    @Test
    fun createCpmIndoorBannerTest() {
        val createCreative = steps.creativeSteps().createCreative(clientInfo)
        val banner =
            TestBanners.activeCpmIndoorBanner(campaignInfo.campaignId, adGroupInfo.adGroupId, createCreative.creativeId)
                .withBsBannerId(0)

        createBannerByTypeTest(banner, BannerV2.EBannerType.BT_CPM_INDOOR)

        steps.creativeSteps().deleteCreativesByIds(clientInfo.shard, createCreative.creativeId)
    }

    @Test
    fun createCpmOutdoorBannerTest() {
        val createCreative = steps.creativeSteps().createCreative(clientInfo)
        val banner =
            TestBanners.activeCpmOutdoorBanner(campaignInfo.campaignId, adGroupInfo.adGroupId, createCreative.creativeId)
                .withBsBannerId(0)

        createBannerByTypeTest(banner, BannerV2.EBannerType.BT_CPM_OUTDOOR)

        steps.creativeSteps().deleteCreativesByIds(clientInfo.shard, createCreative.creativeId)
    }

    @Test
    fun createCpmAudioBannerTest() {
        val createCreative = steps.creativeSteps().createCreative(clientInfo)
        val banner =
            TestBanners.activeCpmAudioBanner(campaignInfo.campaignId, adGroupInfo.adGroupId, createCreative.creativeId)
                .withBsBannerId(0)

        createBannerByTypeTest(banner, BannerV2.EBannerType.BT_CPM_AUDIO)

        steps.creativeSteps().deleteCreativesByIds(clientInfo.shard, createCreative.creativeId)
    }

    @Test
    fun createContentPromotionBannerTest() {
        val banner = TestBanners.activeContentPromotionBannerEdaType(campaignInfo.campaignId, adGroupInfo.adGroupId)
                .withBsBannerId(0)

        createBannerByTypeTest(banner, BannerV2.EBannerType.BT_CONTENT_PROMOTION)
    }

    @Test
    fun createInternalBannerTest() {
        val banner = TestBanners.activeInternalBanner(campaignInfo.campaignId, adGroupInfo.adGroupId)
                .withBsBannerId(0)

        createBannerByTypeTest(banner, BannerV2.EBannerType.BT_INTERNAL)
    }

    @Test
    fun createSmartMainBannerTest() {
        val bannerInfo = steps.performanceMainBannerSteps().createPerformanceMainBanner(adGroupInfo)
        createBannerByTypeTest(bannerInfo.bannerId, BannerV2.EBannerType.BT_SMART_MAIN)
    }

    private fun createBannerByTypeTest(banner: OldBanner, expectedBannerType: BannerV2.EBannerType) {
        val bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo)
        createBannerByTypeTest(bannerInfo.bannerId, expectedBannerType)
    }

    private fun createBannerByTypeTest(bannerId: Long, expectedBannerType: BannerV2.EBannerType) {
        assertThatCode {
            processor.process(listOf(Mysql2GrutReplicationObject(bannerId = bannerId)))
        }.doesNotThrowAnyException()

        val createdBanner = replicationApiService.bannerReplicationApiService.getBannerByDirectId(bannerId)
        val expectedBanner = Schema.TBannerV2.newBuilder().apply {
            meta = Schema.TBannerV2Meta.newBuilder()
                .setDirectId(bannerId)
                .setAdGroupId(adGroupInfo.adGroupId)
                .setCampaignId(campaignInfo.campaignId + BsOrderIdCalculator.ORDER_ID_OFFSET)
                .setSource(Banner.EBannerSource.BS_DIRECT.number)
                .setDirectType(expectedBannerType.number)
                .build()
            status = BannerV2.TBannerV2Status.getDefaultInstance()
            spec = TBannerV2Spec.getDefaultInstance()
        }.build()
        ProtoTruth.assertThat(createdBanner)
            .withPartialScope(FieldScopes.fromSetFields(expectedBanner))
            .isEqualTo(expectedBanner)
    }
}
