package ru.yandex.direct.core.entity.uac.converter

import org.junit.Test
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.contentFlagsMapToGrutContentFlags
import ru.yandex.direct.core.entity.uac.converter.UacGrutCampaignConverter.toContentFlags
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.grut.objects.proto.AgePoint.EAgePoint
import ru.yandex.grut.objects.proto.ContentFlags.EBabyFood
import ru.yandex.grut.objects.proto.ContentFlags.EYaPages

class UacGrutCampaignConverterTest {

    @Test
    fun contentFlagsMapToGrutContentFlags_BabyFoodTest() {
        val flags = contentFlagsMapToGrutContentFlags(mapOf("baby_food" to "5"))
        flags.babyFood.checkEquals(EBabyFood.BF_BABY_FOOD_5)
        flags.toContentFlags().checkEquals(mapOf("baby_food" to "5"))
    }

    @Test
    fun contentFlagsMapToGrutContentFlags_AgeTest() {
        val flags = contentFlagsMapToGrutContentFlags(mapOf("age" to "18"))
        flags.age.checkEquals(EAgePoint.AP_AGE_18)
        flags.toContentFlags().checkEquals(mapOf("age" to "18"))
    }

    @Test
    fun contentFlagsMapToGrutContentFlags_YaPagesTest() {
        val flags = contentFlagsMapToGrutContentFlags(mapOf("ya_pages" to "ya_anywhere"))
        flags.yaPages.checkEquals(EYaPages.YP_YA_ANYWHERE)
        flags.toContentFlags().checkEquals(mapOf("ya_pages" to "ya_anywhere"))
    }
}
