package ru.yandex.direct.grid.processing.service.promoextension

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.promoextension.PromoExtensionRepository
import ru.yandex.direct.core.entity.promoextension.model.PromoExtension
import ru.yandex.direct.core.entity.promoextension.model.PromoExtensionUnit
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsPrefix.from
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate.No
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate.Ready
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate.Yes
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType.cashback
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType.discount
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.promoextension.GdAddPromoExtensionItem
import ru.yandex.direct.grid.processing.model.promoextension.GdAddPromoExtensionsInput
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionPayload
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionPrefix.FROM
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionType.CASHBACK
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionType.DISCOUNT
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionUnit.PCT
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionUnit.RUB
import ru.yandex.direct.grid.processing.model.promoextension.GdUpdatePromoExtensionItem
import ru.yandex.direct.grid.processing.model.promoextension.GdUpdatePromoExtensionsInput
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.processing.util.UserHelper
import ru.yandex.direct.model.KtModelChanges
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotNull
import ru.yandex.direct.test.utils.checkNull
import ru.yandex.direct.test.utils.checkSize
import java.time.LocalDate

@GridProcessingTest
@RunWith(SpringRunner::class)
class PromoExtensionGraphqlServiceMutationTest {
    private val ADD_MUTATION_NAME = "addPromoExtensions"
    private val UPDATE_MUTATION_NAME = "updatePromoExtensions"
    private val MUTATION_TEMPLATE = """
        mutation {
          %s (input: %s) {
            validationResult {
              errors {
                code
                path
                params
              }
            }
            mutationResults {
              id
            }
          }
        }
    """

    private val ADD_PROMO_EXTENSION_MUTATION = TemplateMutation(ADD_MUTATION_NAME, MUTATION_TEMPLATE,
        GdAddPromoExtensionsInput::class.java, GdPromoExtensionPayload::class.java)
    private val UPDATE_PROMO_EXTENSION_MUTATION = TemplateMutation(UPDATE_MUTATION_NAME, MUTATION_TEMPLATE,
        GdUpdatePromoExtensionsInput::class.java, GdPromoExtensionPayload::class.java)

    @Autowired
    private lateinit var promoExtensionRepository: PromoExtensionRepository

    @Autowired
    private lateinit var processor: GraphQlTestExecutor

    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientInfo: ClientInfo
    private lateinit var operator: User

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        operator = UserHelper.getUser(clientInfo.client)
        TestAuthHelper.setDirectAuthentication(operator)
    }

    @Test
    fun testSuccessfulPromoExtensionAdd() {
        val now = LocalDate.now()
        val firstGdPromoInput = GdAddPromoExtensionItem(
            type = DISCOUNT,
            amount = 75,
            unit = RUB,
            prefix = FROM,
            href = null,
            description = "купи слона",
            startDate = now.plusDays(1),
            finishDate = null,
        )
        val secondGdPromoInput = GdAddPromoExtensionItem(
            type = CASHBACK,
            amount = 12,
            unit = PCT,
            prefix = null,
            href = "https://ya.ru",
            description = "купи слона",
            startDate = now.plusDays(4),
            finishDate = now.plusDays(15),
        )
        val inputForAdd = GdAddPromoExtensionsInput(listOf(firstGdPromoInput, secondGdPromoInput))
        val gdAddPromoExtensionsPayload = processor.doMutationAndGetPayload(
            ADD_PROMO_EXTENSION_MUTATION, inputForAdd, operator
        )

        gdAddPromoExtensionsPayload.validationResult.checkNull()
        gdAddPromoExtensionsPayload.mutationResults.checkSize(2)
        val promoExtensionIdFirst = gdAddPromoExtensionsPayload.mutationResults[0].id!!
        val promoExtensionIdSecond = gdAddPromoExtensionsPayload.mutationResults[1].id!!

        val promoExtensionsInDb = promoExtensionRepository.getByIds(
            clientInfo.shard, listOf(promoExtensionIdFirst, promoExtensionIdSecond)

        )
        promoExtensionsInDb.checkSize(2)
        softly {
            promoExtensionsInDb[0].checkEquals(
                PromoExtension(
                    promoExtensionId = promoExtensionIdFirst,
                    clientId = clientInfo.clientId!!,
                    type = discount,
                    amount = 75,
                    unit = PromoExtensionUnit.RUB,
                    prefix = from,
                    href = null,
                    description = "купи слона",
                    startDate = now.plusDays(1),
                    finishDate = null,
                    statusModerate = Ready,
                )
            )
            promoExtensionsInDb[1].checkEquals(
                PromoExtension(
                    promoExtensionId = promoExtensionIdSecond,
                    clientId = clientInfo.clientId!!,
                    type = cashback,
                    amount = 12,
                    unit = PromoExtensionUnit.PCT,
                    prefix = null,
                    href = "https://ya.ru",
                    description = "купи слона",
                    startDate = now.plusDays(4),
                    finishDate = now.plusDays(15),
                    statusModerate = Ready,
                )
            )
        }
    }

    @Test
    fun testPromoExtensionAddOneInvalidOneSuccess() {
        val now = LocalDate.now()
        val firstGdPromoInput = GdAddPromoExtensionItem(
            type = DISCOUNT,
            amount = 75,
            unit = RUB,
            prefix = FROM,
            href = null,
            description = "очень-очень-очень ну вообще очень длинное описание огромного размера",
            startDate = now.plusDays(1),
            finishDate = null,
        )
        val secondGdPromoInput = GdAddPromoExtensionItem(
            type = CASHBACK,
            amount = null,
            unit = null,
            prefix = null,
            href = "https://ya.ru",
            description = "купи слона",
            startDate = now.plusDays(4),
            finishDate = now.plusDays(15),
        )
        val inputForAdd = GdAddPromoExtensionsInput(listOf(firstGdPromoInput, secondGdPromoInput))
        val gdAddPromoExtensionsPayload = processor.doMutationAndGetPayload(
            ADD_PROMO_EXTENSION_MUTATION, inputForAdd, operator
        )

        gdAddPromoExtensionsPayload.validationResult.checkNotNull()
        gdAddPromoExtensionsPayload.validationResult.errors.checkSize(1)
        gdAddPromoExtensionsPayload.mutationResults.checkSize(2)
        gdAddPromoExtensionsPayload.mutationResults[0].id.checkNull()
        gdAddPromoExtensionsPayload.mutationResults[1].id.checkNotNull()

        val promoExtensionSecondId = gdAddPromoExtensionsPayload.mutationResults[1].id!!
        val promoExtensionsInDb = promoExtensionRepository.getByIds(clientInfo.shard, listOf(promoExtensionSecondId))

        val promoExpected = PromoExtension(
            promoExtensionId = promoExtensionSecondId,
            clientId = clientInfo.clientId!!,
            type = cashback,
            amount = null,
            unit = null,
            prefix = null,
            href = "https://ya.ru",
            description = "купи слона",
            startDate = now.plusDays(4),
            finishDate = now.plusDays(15),
            statusModerate = Ready,
        )
        promoExtensionsInDb.checkSize(1)
        promoExtensionsInDb[0].checkEquals(promoExpected)
    }

    @Test
    fun testOneInvalidPromoExtensionAdd() {
        val now = LocalDate.now()
        val gdPromoInput = GdAddPromoExtensionItem(
            type = DISCOUNT,
            amount = 75,
            unit = RUB,
            prefix = FROM,
            href = null,
            description = "очень-очень-очень ну вообще очень длинное описание огромного размера",
            startDate = now.plusDays(1),
            finishDate = null,
        )
        val inputForAdd = GdAddPromoExtensionsInput(listOf(gdPromoInput))
        val gdAddPromoExtensionsPayload = processor.doMutationAndGetPayload(
            ADD_PROMO_EXTENSION_MUTATION, inputForAdd, operator
        )

        gdAddPromoExtensionsPayload.validationResult.checkNotNull()
        gdAddPromoExtensionsPayload.mutationResults.checkSize(1)
        gdAddPromoExtensionsPayload.mutationResults[0].id.checkNull()

        gdAddPromoExtensionsPayload.validationResult.errors.checkSize(1)
    }

    @Test
    fun testSuccessfulPromoExtensionUpdate() {
        val promoExtensionFirst = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)
        val promoExtensionSecond = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)

        val changesFirst = KtModelChanges<Long, PromoExtension>(promoExtensionFirst.promoExtensionId)
        changesFirst.process(PromoExtension::statusModerate, Yes)
        val changesSecond = KtModelChanges<Long, PromoExtension>(promoExtensionSecond.promoExtensionId)
        changesFirst.process(PromoExtension::statusModerate, No)
        promoExtensionRepository.update(clientInfo.shard, listOf(changesFirst, changesSecond))

        val now = LocalDate.now()
        val firstGdPromoInput = GdUpdatePromoExtensionItem(
            id = promoExtensionFirst.promoExtensionId,
            type = DISCOUNT,
            amount = 75,
            unit = RUB,
            prefix = FROM,
            href = null,
            description = "купи слона",
            startDate = now.plusDays(1),
            finishDate = null,
        )
        val secondGdPromoInput = GdUpdatePromoExtensionItem(
            id = promoExtensionSecond.promoExtensionId,
            type = CASHBACK,
            amount = 123456,
            unit = RUB,
            prefix = null,
            href = "https://ya.ru",
            description = "купи слона",
            startDate = now.plusDays(4),
            finishDate = now.plusDays(15),
        )
        val inputForUpdate = GdUpdatePromoExtensionsInput(listOf(firstGdPromoInput, secondGdPromoInput))
        val gdUpdatePromoExtensionsPayload = processor.doMutationAndGetPayload(
            UPDATE_PROMO_EXTENSION_MUTATION, inputForUpdate, operator
        )

        gdUpdatePromoExtensionsPayload.validationResult.checkNull()
        gdUpdatePromoExtensionsPayload.mutationResults.checkSize(2)

        val promoExtensionsInDb = promoExtensionRepository.getByIds(
            clientInfo.shard, listOf(promoExtensionFirst.promoExtensionId, promoExtensionSecond.promoExtensionId)
        )
        promoExtensionsInDb.checkSize(2)
        softly {
            promoExtensionsInDb[0].checkEquals(
                PromoExtension(
                    promoExtensionId = promoExtensionFirst.promoExtensionId,
                    clientId = clientInfo.clientId!!,
                    type = discount,
                    amount = 75,
                    unit = PromoExtensionUnit.RUB,
                    prefix = from,
                    href = null,
                    description = "купи слона",
                    startDate = now.plusDays(1),
                    finishDate = null,
                    statusModerate = Ready,
                )
            )
            promoExtensionsInDb[1].checkEquals(
                PromoExtension(
                    promoExtensionId = promoExtensionSecond.promoExtensionId,
                    clientId = clientInfo.clientId!!,
                    type = cashback,
                    amount = 123456,
                    unit = PromoExtensionUnit.RUB,
                    prefix = null,
                    href = "https://ya.ru",
                    description = "купи слона",
                    startDate = now.plusDays(4),
                    finishDate = now.plusDays(15),
                    statusModerate = Ready,
                )
            )
        }
    }

    @Test
    fun testPromoExtensionUpdateOneInvalidOneSuccess() {
        val promoExtensionFirst = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)
        val promoExtensionSecond = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)

        val changesFirst = KtModelChanges<Long, PromoExtension>(promoExtensionFirst.promoExtensionId)
        changesFirst.process(PromoExtension::statusModerate, Yes)
        val changesSecond = KtModelChanges<Long, PromoExtension>(promoExtensionSecond.promoExtensionId)
        changesFirst.process(PromoExtension::statusModerate, No)
        promoExtensionRepository.update(clientInfo.shard, listOf(changesFirst, changesSecond))

        val now = LocalDate.now()
        val firstGdPromoInput = GdUpdatePromoExtensionItem(
            id = promoExtensionFirst.promoExtensionId,
            type = DISCOUNT,
            amount = 75,
            unit = RUB,
            prefix = FROM,
            href = null,
            description = "очень-очень-очень ну вообще очень длинное описание огромного размера",
            startDate = now.plusDays(1),
            finishDate = null,
        )
        val secondGdPromoInput = GdUpdatePromoExtensionItem(
            id = promoExtensionSecond.promoExtensionId,
            type = CASHBACK,
            amount = 90,
            unit = PCT,
            prefix = null,
            href = "https://ya.ru",
            description = "купи слона",
            startDate = now.plusDays(4),
            finishDate = now.plusDays(15),
        )
        val inputForUpdate = GdUpdatePromoExtensionsInput(listOf(firstGdPromoInput, secondGdPromoInput))
        val gdUpdatePromoExtensionsPayload = processor.doMutationAndGetPayload(
            UPDATE_PROMO_EXTENSION_MUTATION, inputForUpdate, operator
        )

        gdUpdatePromoExtensionsPayload.validationResult.checkNotNull()
        gdUpdatePromoExtensionsPayload.validationResult.errors.checkSize(1)
        gdUpdatePromoExtensionsPayload.mutationResults.checkSize(2)
        gdUpdatePromoExtensionsPayload.mutationResults[0].id.checkNull()
        gdUpdatePromoExtensionsPayload.mutationResults[1].id.checkNotNull()

        val promoExtensionSecondId = gdUpdatePromoExtensionsPayload.mutationResults[1].id!!
        val promoExtensionsInDb = promoExtensionRepository.getByIds(clientInfo.shard, listOf(promoExtensionSecondId))

        val promoExpected = PromoExtension(
            promoExtensionId = promoExtensionSecondId,
            clientId = clientInfo.clientId!!,
            type = cashback,
            amount = 90,
            unit = PromoExtensionUnit.PCT,
            prefix = null,
            href = "https://ya.ru",
            description = "купи слона",
            startDate = now.plusDays(4),
            finishDate = now.plusDays(15),
            statusModerate = Ready,
        )
        promoExtensionsInDb.checkSize(1)
        promoExtensionsInDb[0].checkEquals(promoExpected)
    }

    @Test
    fun testOneInvalidPromoExtensionUpdate() {
        val promoExtensionFirst = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)
        val now = LocalDate.now()
        val gdPromoInput = GdUpdatePromoExtensionItem(
            id = promoExtensionFirst.promoExtensionId,
            type = DISCOUNT,
            amount = 75,
            unit = RUB,
            prefix = FROM,
            href = null,
            description = "очень-очень-очень ну вообще очень длинное описание огромного размера",
            startDate = now.plusDays(1),
            finishDate = null,
        )
        val inputForUpdate = GdUpdatePromoExtensionsInput(listOf(gdPromoInput))
        val gdUpdatePromoExtensionsPayload = processor.doMutationAndGetPayload(
            UPDATE_PROMO_EXTENSION_MUTATION, inputForUpdate, operator
        )

        gdUpdatePromoExtensionsPayload.validationResult.checkNotNull()
        gdUpdatePromoExtensionsPayload.mutationResults.checkSize(1)
        gdUpdatePromoExtensionsPayload.mutationResults[0].id.checkNull()

        gdUpdatePromoExtensionsPayload.validationResult.errors.checkSize(1)
    }
}
