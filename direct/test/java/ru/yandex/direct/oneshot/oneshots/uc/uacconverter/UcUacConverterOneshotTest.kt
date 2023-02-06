package ru.yandex.direct.oneshot.oneshots.uc.uacconverter

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.core.entity.bidmodifier.AgeType
import ru.yandex.direct.core.entity.bidmodifier.AgeType._0_17
import ru.yandex.direct.core.entity.bidmodifier.AgeType._18_24
import ru.yandex.direct.core.entity.bidmodifier.AgeType._55_
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktopAdjustment
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment
import ru.yandex.direct.core.entity.bidmodifier.GenderType
import ru.yandex.direct.core.entity.uac.model.AgePoint.AGE_18
import ru.yandex.direct.core.entity.uac.model.AgePoint.AGE_25
import ru.yandex.direct.core.entity.uac.model.AgePoint.AGE_55
import ru.yandex.direct.core.entity.uac.model.DeviceType
import ru.yandex.direct.core.entity.uac.model.Gender
import ru.yandex.direct.core.entity.uac.model.HolidaySettings
import ru.yandex.direct.core.entity.uac.model.Socdem
import ru.yandex.direct.core.entity.uac.model.TimeTarget

@RunWith(JUnitParamsRunner::class)
class UcUacConverterOneshotTest {
    fun socdemTestData() = listOf(
        listOf(
            bidModifierDemographics(GenderType.FEMALE to null, GenderType.MALE to _0_17, GenderType.MALE to _55_),
            Socdem(listOf(Gender.MALE), AGE_18, AGE_55, null, null)
        ),
        listOf(
            bidModifierDemographics(null to _0_17, null to _18_24, null to _55_),
            Socdem(listOf(Gender.MALE, Gender.FEMALE), AGE_25, AGE_55, null, null)
        ),
    )

    @Test
    @Parameters(method = "socdemTestData")
    fun socdemConverterTest(modifier: BidModifierDemographics, expected: Socdem) {
        val result = UacCampaignFetcher.Utils.convertSocdem(modifier)
        assertThat(result).isEqualTo(expected)
    }

    fun deviceTextCampaignTestData() = listOf(
        listOf(
            BidModifierDesktop().withDesktopAdjustment(BidModifierDesktopAdjustment()).withEnabled(true),
            BidModifierMobile().withMobileAdjustment(BidModifierMobileAdjustment()).withEnabled(false),
            setOf(DeviceType.PHONE)
        ),
        listOf(
            BidModifierDesktop().withDesktopAdjustment(BidModifierDesktopAdjustment()).withEnabled(true),
            BidModifierMobile().withMobileAdjustment(BidModifierMobileAdjustment()).withEnabled(true),
            setOf(DeviceType.PHONE)
        ),
        listOf(
            BidModifierDesktop().withDesktopAdjustment(BidModifierDesktopAdjustment()).withEnabled(false),
            BidModifierMobile().withMobileAdjustment(BidModifierMobileAdjustment()).withEnabled(true),
            setOf(DeviceType.DESKTOP, DeviceType.TABLET)
        ),
        listOf(
            BidModifierDesktop().withDesktopAdjustment(BidModifierDesktopAdjustment()).withEnabled(false),
            BidModifierMobile().withMobileAdjustment(BidModifierMobileAdjustment()).withEnabled(false),
            setOf(DeviceType.ALL)
        ),
    )

    @Test
    @Parameters(method = "deviceTextCampaignTestData")
    fun deviceConverterTest(desktopModifier: BidModifierDesktop, mobileModifier: BidModifierMobile,
                            expected: Set<DeviceType>) {
        val result = UacCampaignFetcher.Utils.convertDeviceTypes(desktopModifier, mobileModifier)
        assertThat(result).isEqualTo(expected)
    }

    fun timeTargetTestData() = listOf(
        listOf(
            "1GHIJKLMNOPQRSTUV2GHIJKLMNOPQRSTUV3GHIJKLMNOPQRSTUV4GHIJKLMNOPQRSTUV5GHIJKLMNOPQRSTUV6HIJKLMNOPQRSTUV7IJKLMNOPQRSTUV8MNO9",
            2,
            TimeTarget(
                idTimeZone = 2,
                useWorkingWeekends = true,
                enabledHolidaysMode = true,
                holidaysSettings = HolidaySettings(
                    startHour = 12,
                    endHour = 15,
                    rateCorrections = null,
                    show = true,
                ),
                timeBoard = (1..5).map {
                    listOf(0, 0, 0, 0, 0, 0, 100, 100, 100, 100, 100, 100,
                        100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 0, 0)
                } + listOf(listOf(0, 0, 0, 0, 0, 0, 0, 100, 100, 100, 100, 100,
                    100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 0, 0)) // Saturday
                    + listOf(listOf(0, 0, 0, 0, 0, 0, 0, 0, 100, 100, 100, 100,
                    100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 0, 0)) // Sunday
            )
        )
    )

    @Test
    @Parameters(method = "timeTargetTestData")
    fun timeTargetConverterTest(timeTarget: String, timezoneId: Long, expected: TimeTarget) {
        val result = UacCampaignFetcher.Utils.convertTimeTarget(
            ru.yandex.direct.libs.timetarget.TimeTarget.parseRawString(timeTarget),
            timezoneId
        )
        assertThat(result).isEqualTo(expected)
    }

}

internal fun bidModifierDemographics(vararg data: Pair<GenderType?, AgeType?>) =
    BidModifierDemographics()
        .withEnabled(true)
        .withDemographicsAdjustments(
            data.map { BidModifierDemographicsAdjustment().withGender(it.first).withAge(it.second) }
        )

