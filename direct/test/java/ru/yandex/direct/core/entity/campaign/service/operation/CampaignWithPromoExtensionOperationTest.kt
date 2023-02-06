package ru.yandex.direct.core.entity.campaign.service.operation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.PromoExtensionInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.test.utils.TestUtils
import ru.yandex.direct.test.utils.checkContains
import ru.yandex.direct.test.utils.checkEmpty
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotEmpty
import ru.yandex.direct.validation.result.DefectIds

@CoreTest
@RunWith(SpringRunner::class)
class RestrictedCampaignsAddOperationCampaignWithPromoExtensionTest {
    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var campaignOperationService: CampaignOperationService

    @Autowired
    private lateinit var steps: Steps

    private lateinit var promoExtension: PromoExtensionInfo
    private lateinit var anotherClientPromoExtension: PromoExtensionInfo

    @Before
    fun before() {
        promoExtension = steps.promoExtensionSteps().createDefaultPromoExtension(
            steps.userSteps().createDefaultUser().clientInfo!!
        )
        anotherClientPromoExtension = steps.promoExtensionSteps().createDefaultPromoExtension(
            steps.userSteps().createDefaultUser().clientInfo!!
        )
    }

    @Test
    fun addOwnedPromoExtensionSuccess() {
        val textCampaign = TestCampaigns.defaultTextCampaign().withPromoExtensionId(promoExtension.promoExtensionId)
        val addOperation = campaignOperationService.createRestrictedCampaignAddOperation(
            listOf(textCampaign),
            promoExtension.clientInfo.uid,
            UidAndClientId.of(promoExtension.clientInfo.uid, promoExtension.clientInfo.clientId!!),
            CampaignOptions(),
        )
        val result = addOperation.prepareAndApply()
        result.validationResult.flattenErrors().checkEmpty()
        val actualCampaign = campaignTypedRepository.getTypedCampaigns(
            promoExtension.shard, listOf(result[0].result)
        )[0] as TextCampaign
        actualCampaign.promoExtensionId.checkEquals(promoExtension.promoExtensionId)
    }

    @Test
    fun addAnotherClientsPromoExtensionValidationError() {
        val textCampaign = TestCampaigns.defaultTextCampaign()
            .withPromoExtensionId(anotherClientPromoExtension.promoExtensionId)
        val addOperation = campaignOperationService.createRestrictedCampaignAddOperation(
            listOf(textCampaign),
            promoExtension.clientInfo.uid,
            UidAndClientId.of(promoExtension.clientInfo.uid, promoExtension.clientInfo.clientId!!),
            CampaignOptions(),
        )
        val result = addOperation.prepareAndApply()
        result.validationResult.flattenErrors().checkNotEmpty()
        result.validationResult.flattenErrors()
            .map { it.defect.defectId() }
            .checkContains(DefectIds.OBJECT_NOT_FOUND)
    }
}

@CoreTest
@RunWith(SpringRunner::class)
class RestrictedCampaignsUpdateOperationCampaignWithPromoExtensionTest {
    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var campaignOperationService: CampaignOperationService

    @Autowired
    private lateinit var steps: Steps

    private lateinit var promoExtension: PromoExtensionInfo
    private lateinit var promoExtensionSecond: PromoExtensionInfo
    private lateinit var anotherClientPromoExtension: PromoExtensionInfo

    @Before
    fun before() {
        promoExtension = steps.promoExtensionSteps().createDefaultPromoExtension(
            steps.userSteps().createDefaultUser().clientInfo!!
        )
        promoExtensionSecond = steps.promoExtensionSteps().createDefaultPromoExtension(promoExtension.clientInfo)
        anotherClientPromoExtension = steps.promoExtensionSteps().createDefaultPromoExtension(
            steps.userSteps().createDefaultUser().clientInfo!!
        )
    }

    @Test
    fun updateWithOwnedPromoExtensionSuccess() {
        val textCampaign = steps.textCampaignSteps().createCampaign(
            promoExtension.clientInfo, TestCampaigns.defaultTextCampaignWithSystemFields(promoExtension.clientInfo)
        )
        val campaignModelChanges = ModelChanges(textCampaign.id, TextCampaign::class.java)
            .process(promoExtension.promoExtensionId, TextCampaign.PROMO_EXTENSION_ID)
        val options = CampaignOptions()
        val updateOperation = campaignOperationService.createRestrictedCampaignUpdateOperation(
            listOf(campaignModelChanges),
            textCampaign.uid,
            UidAndClientId.of(textCampaign.uid, textCampaign.clientId),
            options
        )
        val result = updateOperation.apply()
        result.validationResult.flattenErrors().checkEmpty()
        val actualCampaign = campaignTypedRepository.getTypedCampaigns(
            promoExtension.shard, listOf(result[0].result)
        )[0] as TextCampaign
        actualCampaign.promoExtensionId.checkEquals(promoExtension.promoExtensionId)
    }

    @Test
    fun updateNoPromoExtensionUnchanged() {
        val textCampaign = steps.textCampaignSteps().createCampaign(
            promoExtension.clientInfo,
            TestCampaigns.defaultTextCampaignWithSystemFields(promoExtension.clientInfo)
                .withPromoExtensionId(promoExtension.promoExtensionId)
        )
        val campaignModelChanges = ModelChanges(textCampaign.id, TextCampaign::class.java)
            .process("новое имя", TextCampaign.NAME)
        val options = CampaignOptions()
        val updateOperation = campaignOperationService.createRestrictedCampaignUpdateOperation(
            listOf(campaignModelChanges),
            textCampaign.uid,
            UidAndClientId.of(textCampaign.uid, textCampaign.clientId),
            options
        )
        val result = updateOperation.apply()
        TestUtils.assumeThat {sa -> sa.assertThat(result.successfulCount).isEqualTo(1)}
        TestUtils.assumeThat {sa -> sa.assertThat(result.errorCount).isEqualTo(0)}

        val actualCampaigns = campaignTypedRepository.getTypedCampaigns(
            promoExtension.shard, listOf(result.toResultList().first().result)
        )

        assertThat(actualCampaigns)
            .singleElement(InstanceOfAssertFactories.type(TextCampaign::class.java))
            .extracting { c -> c.promoExtensionId }
            .`as`("promoExtensionId не изменился")
            .isEqualTo(promoExtension.promoExtensionId)
    }

    @Test
    fun updateWithAnotherClientsPromoExtensionValidationError() {
        val textCampaign = steps.textCampaignSteps().createCampaign(
            promoExtension.clientInfo, TestCampaigns.defaultTextCampaignWithSystemFields(promoExtension.clientInfo)
        )
        val campaignModelChanges = ModelChanges(textCampaign.id, TextCampaign::class.java)
            .process(anotherClientPromoExtension.promoExtensionId, TextCampaign.PROMO_EXTENSION_ID)
        val options = CampaignOptions()
        val updateOperation = campaignOperationService.createRestrictedCampaignUpdateOperation(
            listOf(campaignModelChanges),
            textCampaign.uid,
            UidAndClientId.of(textCampaign.uid, textCampaign.clientId),
            options
        )
        val result = updateOperation.apply()
        result.validationResult.flattenErrors().checkNotEmpty()
        result.validationResult.flattenErrors()
            .map { it.defect.defectId() }
            .checkContains(DefectIds.OBJECT_NOT_FOUND)
    }
}
