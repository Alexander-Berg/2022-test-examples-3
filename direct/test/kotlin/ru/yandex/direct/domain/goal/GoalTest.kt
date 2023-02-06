package ru.yandex.direct.domain.goal

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import ru.yandex.direct.domain.retargeting.ABSegmentGoal
import ru.yandex.direct.domain.retargeting.AudienceGoal
import ru.yandex.direct.domain.retargeting.BrandSafetyGoal
import ru.yandex.direct.domain.retargeting.CDPSegmentGoal
import ru.yandex.direct.domain.retargeting.ContentCategoryGoal
import ru.yandex.direct.domain.retargeting.ContentGenreGoal
import ru.yandex.direct.domain.retargeting.ECommereceGoal
import ru.yandex.direct.domain.retargeting.GoalID
import ru.yandex.direct.domain.retargeting.LalSegmentGoal
import ru.yandex.direct.domain.retargeting.MetrikaGoal
import ru.yandex.direct.domain.retargeting.MobileAppSpecialGoal
import ru.yandex.direct.domain.retargeting.MobileGoal
import ru.yandex.direct.domain.retargeting.SegmentGoal
import kotlin.time.Duration.Companion.days

class GoalTests : FunSpec({
    test("when parsing predefined goal id by MetrikaGoalID, MobileAppGoalID should be returned") {
        val goal = MetrikaGoal.ID.from(MobileAppSpecialGoal.ID.Type.ASSISTED_APP_INSTALL_WITHOUT_REINSTALLATION.value)
        goal.shouldBeInstanceOf<MobileAppSpecialGoal.ID>()
    }

    test("AudienceGoal should always have membershipLifeSpan equals 540 days") {
        val goal = AudienceGoal(AudienceGoal.ID.random())
        goal.membershipLifeSpan shouldBe 540.days
    }

    listOf(
        0L to MetrikaGoal.ID.from(0L)!!,
        1L to MetrikaGoal.ID.from(1L)!!,
        999999999L to MetrikaGoal.ID.from(999999999L)!!,
        1000000000L to SegmentGoal.ID.from(1000000000L)!!,
        1499999999L to SegmentGoal.ID.from(1499999999L)!!,
        1500000000L to LalSegmentGoal.ID.from(1500000000L)!!,
        1899999999L to LalSegmentGoal.ID.from(1899999999L)!!,
        1900000000L to MobileGoal.ID.from(1900000000L)!!,
        1999999999L to MobileGoal.ID.from(1999999999L)!!,
        2000000000L to AudienceGoal.ID.from(2000000000L)!!,
        2500000000L to ABSegmentGoal.ID.from(2500000000L)!!,
        2599999999L to ABSegmentGoal.ID.from(2599999999L)!!,
        2600000000L to CDPSegmentGoal.ID.from(2600000000L)!!,
        2999999999L to CDPSegmentGoal.ID.from(2999999999L)!!,
        3000000000L to ECommereceGoal.ID.from(3000000000L)!!,
        3899999999L to ECommereceGoal.ID.from(3899999999L)!!,
        3900000000L to null,

        // strange default from perl
        4000000000L to null,
        5000000000L to null,

        4294967295L to null,
        4294967296L to BrandSafetyGoal.ID.from(4294967296L)!!,
        4294968295L to BrandSafetyGoal.ID.from(4294968295L)!!,
        4294968296L to ContentCategoryGoal.ID.from(4294968296L)!!,
        4294970295L to ContentCategoryGoal.ID.from(4294970295L)!!,
        4294970296L to ContentGenreGoal.ID.from(4294970296L)!!,
        4294972295L to ContentGenreGoal.ID.from(4294972295L)!!,
        4294972296L to null,
    ).forEach { (id, expected) ->
        test("$id should be parsed into $expected") {
            GoalID.from(id) shouldBe expected
        }
    }
})
