package ru.yandex.direct.web.entity.uac.converter.proto.enummappers

import org.assertj.core.api.SoftAssertions
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.aggregatedstatuses.GdSelfStatusReason
import ru.yandex.direct.core.entity.campaign.model.BrandSurveyStopReason
import ru.yandex.direct.core.entity.campaign.model.SurveyStatus
import ru.yandex.direct.core.entity.hypergeo.model.GeoSegmentType
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppAlternativeStore
import ru.yandex.direct.core.entity.uac.model.AdvType
import ru.yandex.direct.core.entity.uac.model.AgePoint
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.MediaType
import ru.yandex.direct.core.entity.uac.model.Platform
import ru.yandex.direct.core.entity.uac.model.Status
import ru.yandex.direct.mysql2grut.enummappers.EnumMappersTestBase
import ru.yandex.direct.web.entity.uac.model.UacCampaignAction
import ru.yandex.direct.web.entity.uac.model.UacCampaignServicedState
import ru.yandex.grut.objects.proto.AgeGroup
import ru.yandex.grut.objects.proto.AppInfo.TAppInfoSpec.EPlatform
import ru.yandex.grut.objects.proto.AppStore
import ru.yandex.grut.objects.proto.BrandSurvey
import ru.yandex.grut.objects.proto.Campaign
import ru.yandex.grut.objects.proto.CampaignBriefStatus
import ru.yandex.grut.objects.proto.Gender.EGender
import ru.yandex.grut.objects.proto.GeoSegment
import ru.yandex.grut.objects.proto.MediaType.EMediaType
import ru.yandex.grut.objects.proto.SelfStatusReason

@RunWith(SpringJUnit4ClassRunner::class)
class UacEnumMappersTest : EnumMappersTestBase() {

    @Before
    fun before() {
        softly = SoftAssertions()
    }

    @After
    fun after() {
        softly.assertAll()
    }

    @Test
    fun checkCampaignBriefStatusMapping() {
        testBase(
            Status.values(),
            UacEnumMappers::toProtoStatus,
            CampaignBriefStatus.ECampaignBriefStatus.CBS_UNKNOWN,
        )
    }

    @Test
    fun checkStateReasonsMapping() {
        testBase(
            GdSelfStatusReason.values(),
            UacEnumMappers::toProtoSelfStatusReason,
            SelfStatusReason.ESelfStatusReason.SSR_UNKNOWN,
            setOf(GdSelfStatusReason.NOTHING), // Deprecated
        )
    }

    @Test
    fun checkMobileAppAlternativeStoreMapping() {
        testBase(
            MobileAppAlternativeStore.values(),
            UacEnumMappers::toProtoAltAppStore,
            AppStore.EAppStore.AS_UNKNOWN,
        )
    }

    @Test
    fun checkGeoSegmentTypeMapping() {
        testBase(
            GeoSegmentType.values(),
            UacEnumMappers::toProtoGeoSegment,
            GeoSegment.EGeoSegment.GS_UNKNOWN,
        )
    }

    @Test
    fun checkBrandSurveyStatusMapping() {
        testBase(
            SurveyStatus.values(),
            UacEnumMappers::toProtoBrandSurveyStatus,
            BrandSurvey.EBrandSurveyStatus.BSS_UNKNOWN,
        )
    }

    @Test
    fun checkBrandSurveyStopReasonMapping() {
        testBase(
            BrandSurveyStopReason.values(),
            UacEnumMappers::toProtoBrandSurveyStopReason,
            BrandSurvey.EBrandSurveyStopReason.BSSR_UNKNOWN,
        )
    }

    @Test
    fun checkCampaignActionMapping() {
        testBase(
            UacCampaignAction.values(),
            UacEnumMappers::toProtoCampaignAction,
            Campaign.ECampaignAction.CA_NOT_SPECIFIED,
        )
    }

    @Test
    fun checkCampaignServicedStateMapping() {
        testBase(
            UacCampaignServicedState.values(),
            UacEnumMappers::toProtoCampaignServicedState,
            Campaign.ECampaignServicedState.CSS_UNKNOWN,
        )
    }

    @Test
    fun checkCampaignTypeMapping() {
        testBase(
            AdvType.values(),
            UacEnumMappers::toProtoCampaignType,
            Campaign.ECampaignTypeOld.CTO_UNKNOWN,
        )
    }

    @Test
    fun checkAgePointMapping() {
        testBase(
            AgePoint.values(),
            UacEnumMappers::toProtoAgeGroup,
            AgeGroup.EAgeGroup.AG_UNKNOWN,
            setOf(AgePoint.AGE_INF)
        )
    }

    @Test
    fun checkGenderMapping() {
        testBase(
            Gender.values(),
            UacEnumMappers::toProtoGender,
            EGender.G_UNKNOWN,
        )
    }

    @Test
    fun checkMediaTypeMapping() {
        testBase(
            MediaType.values(),
            UacEnumMappers::toProtoMediaType,
            EMediaType.MT_NOT_SPECIFIED,
        )
    }

    @Test
    fun checkPlatformMapping() {
        testBase(
            Platform.values(),
            UacEnumMappers::toProtoPlatform,
            EPlatform.P_NOT_SPECIFIED,
        )
    }
}
