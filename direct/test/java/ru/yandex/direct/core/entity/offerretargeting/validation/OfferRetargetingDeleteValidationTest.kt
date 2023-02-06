package ru.yandex.direct.core.entity.offerretargeting.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.archivedCampaignModification
import ru.yandex.direct.core.entity.offerretargeting.service.OfferRetargetingOperationBaseTest
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.repository.TestCampaignRepository
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CommonDefects.objectNotFound
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class OfferRetargetingDeleteValidationTest : OfferRetargetingOperationBaseTest() {
    @Autowired
    private lateinit var offerRetargetingValidationService: OfferRetargetingValidationService

    @Autowired
    private lateinit var testCampaignRepository: TestCampaignRepository

    @Test
    fun validate_Success() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)

        val actual = offerRetargetingValidationService
            .validateDeleteOfferRetargetings(
                listOf(savedOfferRetargeting.id),
                offerRetargetingsByIds,
                operatorUid,
                clientId
            )
        assertThat(actual).`is`(matchedBy(hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun validate_NotExistingIds() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)

        val actual = offerRetargetingValidationService
            .validateDeleteOfferRetargetings(
                listOf(savedOfferRetargeting.id + 1000),
                offerRetargetingsByIds,
                operatorUid,
                clientId
            )
        assertThat(actual)
            .`is`(matchedBy(hasDefectWithDefinition<Any>(validationError(path(index(0)), objectNotFound()))))
    }

    @Test
    fun validate_ArchivedCampaign_ArchivedCampaignModification() {
        val savedOfferRetargeting = addOfferRetargetingToAdGroup(0)
        testCampaignRepository.archiveCampaign(shard, activeCampaignId)
        val actual = offerRetargetingValidationService.validateDeleteOfferRetargetings(
            listOf(savedOfferRetargeting.id),
            offerRetargetingsByIds,
            operatorUid,
            clientId
        )
        assertThat(actual)
            .`is`(matchedBy(hasDefectWithDefinition<Any>(validationError(path(index(0)), archivedCampaignModification()))))
    }
}
