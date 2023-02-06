package ru.yandex.direct.core.entity.promoextension

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds
import ru.yandex.direct.core.entity.promoextension.model.PromoExtension
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.testPromoExtension
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate.Ready
import ru.yandex.direct.model.KtModelChanges
import ru.yandex.direct.test.utils.check
import ru.yandex.direct.test.utils.checkEmpty
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotNull
import ru.yandex.direct.test.utils.checkNull
import ru.yandex.direct.test.utils.checkSize
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.ids.DateDefectIds
import ru.yandex.direct.validation.defect.ids.StringDefectIds
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import java.time.LocalDate

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class PromoExtensionServiceTest {
    @Autowired
    private lateinit var promoExtensionService: PromoExtensionService

    @Autowired
    private lateinit var promoExtensionRepository: PromoExtensionRepository

    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun testSuccessfulAdd() {
        val promoToAdd = testPromoExtension(
            id = null,
            clientId = clientInfo.clientId!!,
            description = "промоакция",
            href = "https://yandex.ru",
            startDate = null,
            finishDate = null,
            statusModerate = Ready,
        )
        val result = promoExtensionService.add(clientInfo.clientId!!, listOf(promoToAdd))
        softly {
            result.check(isFullySuccessful())
            promoToAdd.promoExtensionId.checkNotNull()
            promoToAdd.promoExtensionId.checkEquals(result.get(0).result)
        }
        promoExtensionRepository.getByIds(clientInfo.shard, listOf(promoToAdd.promoExtensionId!!)).firstOrNull()
            .checkEquals(promoToAdd)
    }

    @Test
    fun testSuccessfulDelete() {
        val promo = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo).promoExtension

        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            listOf(promo.id!!)
        ).checkSize(1)

        promoExtensionService.delete(clientInfo.clientId!!, listOf(promo.id!!))
        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            listOf(promo.id!!)
        ).checkEmpty()
    }

    @Test
    fun testDeleteByNonExistentIdNothingDeleted() {
        val promo = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo).promoExtension

        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            listOf(promo.id!!)
        ).checkSize(1)

        promoExtensionService.delete(clientInfo.clientId!!, listOf(promo.id!! + 1000000))
        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            listOf(promo.id!!)
        ).checkSize(1)
    }

    @Test
    fun testAnyDelete() {
        val promos = listOf(
            steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo).promoExtension,
            steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo).promoExtension,
            steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo).promoExtension
        )

        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            promos.map { promoExtension -> promoExtension.id!! }
        ).checkSize(3)

        promoExtensionService.delete(clientInfo.clientId!!, promos.map { promoExtension -> promoExtension.id!! })
        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            promos.map { promoExtension -> promoExtension.id!! }
        ).checkEmpty()
    }

    @Test
    fun testOneDeletedTwoUndeleted() {
        val promos = listOf(
            steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo).promoExtension,
            steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo).promoExtension,
            steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo).promoExtension
        )

        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            promos.map { promoExtension -> promoExtension.id!! }
        ).checkSize(3)

        promoExtensionService.delete(clientInfo.clientId!!, listOf(promos[0].promoExtensionId!!))
        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            promos.map { promoExtension -> promoExtension.id!! }
        ).checkSize(2)
    }

    @Test
    fun testTwoEqualsIdsDelete() {
        val promo = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo).promoExtension

        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            listOf(promo.id!!)
        ).checkSize(1)

        promoExtensionService.delete(clientInfo.clientId!!, listOf(promo.promoExtensionId!!, promo.promoExtensionId!!))
        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            listOf(promo.id!!)
        ).checkEmpty()
    }

    @Test
    fun testInvalidIdDelete() {
        val promo = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo).promoExtension

        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            listOf(promo.id!!)
        ).checkSize(1)

        val result = promoExtensionService.delete(clientInfo.clientId!!, listOf(-1))
        result.validationResult.subResults.values.toList()[0].errors[0].defectId()
            .checkEquals(DefectIds.MUST_BE_VALID_ID)
        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            listOf(promo.id!!)
        ).checkSize(1)
    }

    @Test
    fun testPromoWithCampaignInvalidDelete() {
        val promo = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo).promoExtension

        steps.textCampaignSteps().createCampaign(
            clientInfo,
            TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo).withPromoExtensionId(promo.id)
        )
        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            listOf(promo.id!!)
        ).checkSize(1)

        val result = promoExtensionService.delete(clientInfo.clientId!!, listOf(promo.id!!))
        result.validationResult.subResults.values.toList()[0].errors[0].defectId()
            .checkEquals(DefectIds.UNABLE_TO_DELETE)
        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            listOf(promo.id!!)
        ).checkSize(1)
    }

    @Test
    fun testPromoWithClientInvalidDelete() {
        val fakeClientInfo = steps.clientSteps().createDefaultClient()
        val promo = steps.promoExtensionSteps().createDefaultPromoExtension(fakeClientInfo).promoExtension

        promoExtensionRepository.getClientPromoExtensionsByIds(
            fakeClientInfo.shard,
            fakeClientInfo.clientId!!,
            listOf(promo.id!!)
        ).checkSize(1)

        promoExtensionService.delete(clientInfo.clientId!!, listOf(promo.id!!))
        val result = promoExtensionService.delete(clientInfo.clientId!!, listOf(promo.id!!))
        result.validationResult.subResults.values.toList()[0].errors[0].defectId()
            .checkEquals(DefectIds.OBJECT_NOT_FOUND)
        promoExtensionRepository.getClientPromoExtensionsByIds(
            fakeClientInfo.shard,
            fakeClientInfo.clientId!!,
            listOf(promo.id!!)
        ).checkSize(1)
    }

    @Test
    fun testPromosWithClientInvalidDelete() {
        val fakeClientInfo = steps.clientSteps().createDefaultClient()
        val promos = listOf(
            steps.promoExtensionSteps().createDefaultPromoExtension(fakeClientInfo).promoExtension,
            steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo).promoExtension,
            steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo).promoExtension
        )

        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            promos.map { promoExtension -> promoExtension.id!! }
        ).checkSize(2)

        promoExtensionService.delete(clientInfo.clientId!!, promos.map { promoExtension -> promoExtension.id!! })
        val result = promoExtensionService.delete(clientInfo.clientId!!, promos.map { promoExtension -> promoExtension.id!! })
        result.validationResult.subResults.values.toList()[0].errors[0].defectId()
            .checkEquals(DefectIds.OBJECT_NOT_FOUND)
        promoExtensionRepository.getClientPromoExtensionsByIds(
            clientInfo.shard,
            clientInfo.clientId!!,
            promos.map { promoExtension -> promoExtension.id!! }
        ).checkSize(0)
    }

    @Test
    fun testAddInvalidHrefValidationError() {
        val promoToAdd = testPromoExtension(
            id = null,
            clientId = clientInfo.clientId!!,
            description = "промоакция",
            href = "yandex.ru",//нет https
            startDate = null,
            finishDate = null,
            statusModerate = Ready,
        )
        val result = promoExtensionService.add(clientInfo.clientId!!, listOf(promoToAdd))
        softly {
            result.validationResult.check(
                hasDefectWithDefinition(
                    validationError(path(index(0), field(PromoExtension::href)), BannerDefectIds.Gen.INVALID_HREF)
                )
            )
            promoToAdd.promoExtensionId.checkNull()
            result.get(0).result.checkNull()
        }
    }

    @Test
    fun testAddInvalidDateValidationError() {
        val promoToAdd = testPromoExtension(
            id = null,
            clientId = clientInfo.clientId!!,
            description = "промоакция",
            href = "https://yandex.ru",
            startDate = LocalDate.of(2040, 11, 17),
            finishDate = LocalDate.of(2039, 12, 23),//в прошлом относительно startDate
            statusModerate = Ready,
        )
        val result = promoExtensionService.add(clientInfo.clientId!!, listOf(promoToAdd))
        softly {
            result.validationResult.check(
                hasDefectWithDefinition(
                    validationError(
                        path(index(0), field(PromoExtension::finishDate)),
                        DateDefectIds.END_DATE_MUST_BE_GREATER_THAN_OR_EQUAL_TO_START_DATE
                    )
                )
            )
            promoToAdd.promoExtensionId.checkNull()
            result.get(0).result.checkNull()
        }
    }

    @Test
    fun testAddShortDescriptionValidationError() {
        val promoToAdd = testPromoExtension(
            id = null,
            clientId = clientInfo.clientId!!,
            description = "кря",
            href = "https://yandex.ru",
            startDate = null,
            finishDate = null,
            statusModerate = Ready,
        )
        val result = promoExtensionService.add(clientInfo.clientId!!, listOf(promoToAdd))
        softly {
            result.validationResult.check(
                hasDefectWithDefinition(
                    validationError(
                        path(index(0), field(PromoExtension::description)),
                        StringDefectIds.LENGTH_CANNOT_BE_LESS_THAN_MIN
                    )
                )
            )
            promoToAdd.promoExtensionId.checkNull()
            result.get(0).result.checkNull()
        }
    }

    @Test
    fun testAddLongDescriptionValidationError() {
        val promoToAdd = testPromoExtension(
            id = null,
            clientId = clientInfo.clientId!!,
            description = "очень-очень длинное зелёное описание товара",
            href = "https://yandex.ru",
            startDate = null,
            finishDate = null,
            statusModerate = Ready,
        )
        val result = promoExtensionService.add(clientInfo.clientId!!, listOf(promoToAdd))
        softly {
            result.validationResult.check(
                hasDefectWithDefinition(
                    validationError(
                        path(index(0), field(PromoExtension::description)),
                        BannerDefectIds.String.TEXT_LENGTH_WITHOUT_TEMPLATE_MARKER_CANNOT_BE_MORE_THAN_MAX
                    )
                )
            )
            promoToAdd.promoExtensionId.checkNull()
            result.get(0).result.checkNull()
        }
    }

    @Test
    fun testSuccessfulUpdate() {
        val promoExtensionInfo = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)
        val changes = KtModelChanges<Long, PromoExtension>(promoExtensionInfo.promoExtensionId)
        changes.process(PromoExtension::amount, 150L)
        val result = promoExtensionService.update(clientInfo.clientId!!, listOf(changes))
        softly {
            result.check(isFullySuccessful())
            promoExtensionInfo.promoExtensionId.checkEquals(result.get(0).result)
        }
        promoExtensionRepository.getByIds(clientInfo.shard, listOf(promoExtensionInfo.promoExtensionId))
            .firstOrNull()
            .checkEquals(promoExtensionInfo.promoExtension.copy(amount = 150L))
    }

    @Test
    fun testUpdateInvalidHrefValidationError() {
        val promoExtensionInfo = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)
        val changes = KtModelChanges<Long, PromoExtension>(promoExtensionInfo.promoExtensionId)
        changes.process(PromoExtension::href, "yandex.ru")//нет https
        val result = promoExtensionService.update(clientInfo.clientId!!, listOf(changes))
        result.validationResult.check(
            hasDefectWithDefinition(
                validationError(path(index(0), field(PromoExtension::href)), BannerDefectIds.Gen.INVALID_HREF)
            )
        )
    }

    @Test
    fun testUpdateShortDescriptionValidationError() {
        val promoExtensionInfo = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)
        val changes = KtModelChanges<Long, PromoExtension>(promoExtensionInfo.promoExtensionId)
        changes.process(PromoExtension::description, "кря")
        val result = promoExtensionService.update(clientInfo.clientId!!, listOf(changes))
        result.validationResult.check(
            hasDefectWithDefinition(
                validationError(
                    path(index(0), field(PromoExtension::description)),
                    StringDefectIds.LENGTH_CANNOT_BE_LESS_THAN_MIN
                )
            )
        )
    }

    @Test
    fun testUpdateLongDescriptionValidationError() {
        val promoExtensionInfo = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)
        val changes = KtModelChanges<Long, PromoExtension>(promoExtensionInfo.promoExtensionId)
        changes.process(PromoExtension::description, "очень-очень длинное зелёное описание товара")
        val result = promoExtensionService.update(clientInfo.clientId!!, listOf(changes))
        result.validationResult.check(
            hasDefectWithDefinition(
                validationError(
                    path(index(0), field(PromoExtension::description)),
                    BannerDefectIds.String.TEXT_LENGTH_WITHOUT_TEMPLATE_MARKER_CANNOT_BE_MORE_THAN_MAX
                )
            )
        )
    }

    @Test
    fun testUpdateInvalidDateValidationError() {
        val promoExtensionInfo = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)
        val changes = KtModelChanges<Long, PromoExtension>(promoExtensionInfo.promoExtensionId)
        changes.process(PromoExtension::startDate, LocalDate.of(2040, 11, 17))
        changes.process(PromoExtension::finishDate, LocalDate.of(2039, 12, 23))
        val result = promoExtensionService.update(clientInfo.clientId!!, listOf(changes))
        result.validationResult.check(
            hasDefectWithDefinition(
                validationError(
                    path(index(0), field(PromoExtension::finishDate)),
                    DateDefectIds.END_DATE_MUST_BE_GREATER_THAN_OR_EQUAL_TO_START_DATE
                )
            )
        )
    }
}
