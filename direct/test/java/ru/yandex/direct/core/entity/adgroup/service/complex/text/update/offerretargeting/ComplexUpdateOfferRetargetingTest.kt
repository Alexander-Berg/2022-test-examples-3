package ru.yandex.direct.core.entity.adgroup.service.complex.text.update.offerretargeting

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept
import ru.yandex.direct.core.entity.adgroup.service.complex.suboperation.update.converter.OfferRetargetingUpdateConverter
import ru.yandex.direct.core.entity.adgroup.service.complex.text.update.ComplexAdGroupUpdateOperationTestBase
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting.ID
import ru.yandex.direct.core.entity.offerretargeting.model.OfferRetargeting.LAST_CHANGE_TIME
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.defaultOfferRetargeting
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.steps.OfferRetargetingSteps
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import java.math.BigDecimal

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class ComplexUpdateOfferRetargetingTest : ComplexAdGroupUpdateOperationTestBase() {
    @Autowired
    lateinit var offerRetargetingSteps: OfferRetargetingSteps

    @Test
    fun update_AdGroupWithOfferRetargeting_RemoveAllOfferRetargetings() {
        val offerRetargeting = offerRetargetingSteps.defaultOfferRetargetingForGroup(adGroupInfo1)
        offerRetargetingSteps.addOfferRetargetingToAdGroup(offerRetargeting, adGroupInfo1)
        val adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
        adGroupForUpdate.offerRetargetings = null

        updateAndCheckResultIsEntirelySuccessful(listOf(adGroupForUpdate))
        val offerRetargetingsOfAdGroup: List<OfferRetargeting> = getOfferRetargetingsOfAdGroup(adGroupInfo1)
        assertThat(offerRetargetingsOfAdGroup).hasSize(0)
    }

    @Test
    fun update_AdGroupWithOfferRetargeting_AddValidOfferRetargeting() {
        val adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
        adGroupForUpdate.offerRetargetings = listOf(defaultOfferRetargeting)

        updateAndCheckResultIsEntirelySuccessful(listOf(adGroupForUpdate))
        val offerRetargetingsOfAdGroup: List<OfferRetargeting> = getOfferRetargetingsOfAdGroup(adGroupInfo1)
        assertThat(offerRetargetingsOfAdGroup).hasSize(1)
        val expectedOfferRetargeting = offerRetargetingSteps.defaultOfferRetargetingForGroup(adGroupInfo1)
        val compareStrategy = allFieldsExcept(
            newPath(LAST_CHANGE_TIME.name()),
            newPath(ID.name())
        )
        assertThat(offerRetargetingsOfAdGroup.single())
            .`is`(matchedBy(beanDiffer(expectedOfferRetargeting).useCompareStrategy(compareStrategy)))
    }

    /**
     * Применение обновлений для офферных ретаргетингов не изменяет поля,
     * которые не включены в [OfferRetargetingUpdateConverter].
     */
    @Test
    fun update_AdGroupWithOfferRetargeting_UpdateOfferRetargeting() {
        val offerRetargeting = offerRetargetingSteps.defaultOfferRetargetingForGroup(adGroupInfo1)
        val offerRetargetingId =
            offerRetargetingSteps.addOfferRetargetingsToAdGroup(listOf(offerRetargeting), adGroupInfo1).single()
        val adGroupForUpdate = createValidAdGroupForUpdate(adGroupInfo1)
        val offerRetargetingToUpdate = offerRetargeting
            .withId(offerRetargetingId)
            .withPrice(BigDecimal("1.23"))
        adGroupForUpdate.offerRetargetings = listOf(offerRetargetingToUpdate)

        updateAndCheckResultIsEntirelySuccessful(listOf(adGroupForUpdate))
        val offerRetargetingsOfAdGroup: List<OfferRetargeting> = getOfferRetargetingsOfAdGroup(adGroupInfo1)
        assertThat(offerRetargetingsOfAdGroup).hasSize(1)
        val compareStrategy = allFieldsExcept(
            newPath(LAST_CHANGE_TIME.name()),
        )
        val expectedOfferRetargeting = offerRetargetingSteps.defaultOfferRetargetingForGroup(adGroupInfo1)
            .withId(offerRetargetingId)
        assertThat(offerRetargetingsOfAdGroup.single())
            .`is`(matchedBy(beanDiffer(expectedOfferRetargeting).useCompareStrategy(compareStrategy)))
    }

    private fun getOfferRetargetingsOfAdGroup(adGroupInfo: AdGroupInfo): List<OfferRetargeting> =
        offerRetargetingRepository.getOfferRetargetingsByAdGroupIds(
            shard,
            clientId,
            listOf(adGroupInfo.adGroupId)
        ).values.toList()

}
