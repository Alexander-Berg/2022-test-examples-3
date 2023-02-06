package ru.yandex.direct.grid.processing.service.promoextension

import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.hasSize
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.testPromoExtension
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate.Ready
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType.cashback
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType.discount
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType.profit
import ru.yandex.direct.grid.model.Order.ASC
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.GdLimitOffset
import ru.yandex.direct.grid.processing.model.checkGdPromoextensionEqualsExpected
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionFilter
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionOrderBy
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionOrderByField.NAME
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionStatus.ON_MODERATION
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionsContainer
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.processing.util.UserHelper
import ru.yandex.direct.test.utils.TestUtils.assumeThat

@GridProcessingTest
@RunWith(SpringRunner::class)
class PromoExtensionGraphqlServiceQueryTest {
    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor

    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientInfo: ClientInfo
    private lateinit var operator: User

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        operator = UserHelper.getUser(clientInfo.client)
        TestAuthHelper.setDirectAuthentication(operator)
        ktGraphQLTestExecutor.withDefaultGraphQLContext(operator)
    }

    @Test
    fun testDefaultSortByIdDesc() {
        val promoExtensionFirst = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)
        val promoExtensionSecond = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)
        assumeThat(promoExtensionFirst.promoExtensionId < promoExtensionSecond.promoExtensionId, `is`(true))

        val promoExtensions = ktGraphQLTestExecutor.getPromoExtensionList(
            clientInfo.login,
            gdPromoExtensionContainer(
                setOf(promoExtensionFirst.promoExtensionId, promoExtensionSecond.promoExtensionId), null)
        )
        assumeThat(promoExtensions, hasSize(2))
        softly {
            promoExtensions[0].checkGdPromoextensionEqualsExpected(
                promoExtensionSecond.promoExtension,
                ON_MODERATION,
                listOf()
            )
            promoExtensions[1].checkGdPromoextensionEqualsExpected(
                promoExtensionFirst.promoExtension,
                ON_MODERATION,
                listOf()
            )
        }
    }

    @Test
    fun testSortByNameAsc() {
        val promoExtensionFirst = steps.promoExtensionSteps().createPromoExtension(
            clientInfo,
            testPromoExtension(
                id = null,
                clientId = clientInfo.clientId!!,
                description = "промоакция",
                href = "https://yandex.ru",
                startDate = null,
                finishDate = null,
                statusModerate = Ready,
            ).copy(type = cashback)
        )
        val promoExtensionSecond = steps.promoExtensionSteps().createPromoExtension(
            clientInfo,
            testPromoExtension(
                id = null,
                clientId = clientInfo.clientId!!,
                description = "промоакция",
                href = "https://ya.ru",
                startDate = null,
                finishDate = null,
                statusModerate = Ready,
            ).copy(type = discount)
        )
        val promoExtensionThird = steps.promoExtensionSteps().createPromoExtension(
            clientInfo,
            testPromoExtension(
                id = null,
                clientId = clientInfo.clientId!!,
                description = "промоакция",
                href = "https://ya.com",
                startDate = null,
                finishDate = null,
                statusModerate = Ready,
            ).copy(type = profit)
        )
        assumeThat(promoExtensionFirst.promoExtensionId < promoExtensionSecond.promoExtensionId, `is`(true))
        assumeThat(promoExtensionSecond.promoExtensionId < promoExtensionThird.promoExtensionId, `is`(true))

        val promoExtensions = ktGraphQLTestExecutor.getPromoExtensionList(
            clientInfo.login,
            gdPromoExtensionContainer(
                setOf(promoExtensionFirst.promoExtensionId, promoExtensionSecond.promoExtensionId,
                    promoExtensionThird.promoExtensionId),
                listOf(GdPromoExtensionOrderBy(NAME, ASC))
            )
        )
        assumeThat(promoExtensions, hasSize(3))
        //Сортировка идёт по совокупному описанию
        //Правильный порядок выгода-кешбэк-скидка
        softly {
            promoExtensions[0].checkGdPromoextensionEqualsExpected(
                promoExtensionThird.promoExtension,
                ON_MODERATION,
                listOf()
            )
            promoExtensions[1].checkGdPromoextensionEqualsExpected(
                promoExtensionFirst.promoExtension,
                ON_MODERATION,
                listOf()
            )
            promoExtensions[2].checkGdPromoextensionEqualsExpected(
                promoExtensionSecond.promoExtension,
                ON_MODERATION,
                listOf()
            )
        }
    }

    private fun gdPromoExtensionContainer(promoIds: Set<Long>?, orderBy: List<GdPromoExtensionOrderBy>?) =
        GdPromoExtensionsContainer(
            filterKey = null,
            filter = GdPromoExtensionFilter(
                promoExtensionIdIn = promoIds
            ),
            limitOffset = GdLimitOffset()
                .withOffset(0)
                .withLimit(100),
            orderBy = orderBy,
            cacheKey = null,
        )
}
