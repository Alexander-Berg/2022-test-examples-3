package ru.yandex.direct.web.entity.uac.validation

import graphql.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds
import ru.yandex.direct.core.entity.campaign.service.validation.DisableDomainValidationService
import ru.yandex.direct.core.entity.client.service.ClientLimitsService
import ru.yandex.direct.core.entity.sspplatform.repository.SspPlatformsRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacDisabledPlaces
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.validation.defects.params.CollectionSubsetDefectParams
import ru.yandex.direct.libs.mirrortools.utils.HostingsHandler
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.PathNode
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacDisabledPlacesValidatorTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var clientLimitsService: ClientLimitsService

    @Autowired
    private lateinit var hostingsHandler: HostingsHandler

    @Autowired
    private lateinit var disableDomainValidationService: DisableDomainValidationService

    @Autowired
    private lateinit var sspPlatformsRepository: SspPlatformsRepository

    private lateinit var uacDisabledPlacesValidator: UacDisabledPlacesValidator

    @Before
    fun before() {
        val clientInfo = steps.clientSteps().createDefaultClient()

         uacDisabledPlacesValidator = UacDisabledPlacesValidator(
            clientLimitsService,
            clientInfo.clientId!!,
            hostingsHandler,
            disableDomainValidationService,
            sspPlatformsRepository
        )
    }

    @Test
    fun apply_withDublicateDisabledPlaces() {
        val uacDisabledPlaces = UacDisabledPlaces(
            listOf("https://mail.ru", "https://mail.ru/"),
            null,
            null,
            null
        )
        val result = uacDisabledPlacesValidator.apply(uacDisabledPlaces)
        Assert.assertTrue(
            result.subResults[PathNode.Field("disabledPlaces")]?.
        errors?.contains(
            Defect(
                CampaignDefectIds.Subset.MUST_NOT_CONTAIN_DUPLICATED_STRINGS,
                CollectionSubsetDefectParams.of<String>(listOf("mail.ru"))
            )
        )!!)
    }

    @Test
    fun apply_withDublicateDisabledVideoPlaces() {
        val uacDisabledPlaces = UacDisabledPlaces(
            null,
            listOf("https://mail.ru", "https://mail.ru/"),
            null,
            null
        )
        val result = uacDisabledPlacesValidator.apply(uacDisabledPlaces)
        Assert.assertTrue(
            result.subResults[PathNode.Field("disabledVideoAdsPlaces")]?.
            errors?.contains(
                Defect(
                    CampaignDefectIds.Subset.MUST_NOT_CONTAIN_DUPLICATED_STRINGS,
                    CollectionSubsetDefectParams.of<String>(listOf("mail.ru"))
                )
            )!!)
    }

    @Test
    fun apply_withoutDublicateDisabledPlaces() {
        val uacDisabledPlaces = UacDisabledPlaces(
            listOf("https://mail.ru"),
            listOf("https://mail.ru"),
            null,
            null
        )
        val result = uacDisabledPlacesValidator.apply(uacDisabledPlaces)
        Assert.assertFalse(
            result.subResults[PathNode.Field("disabledPlaces")]?.
            hasErrors()
        !!)
    }
}
