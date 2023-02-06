package ru.yandex.direct.grid.processing.model

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import ru.yandex.direct.core.entity.promoextension.model.PromoExtension
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionPrefix
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionStatus
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionType
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionUnit
import java.time.LocalDate

data class TestGdPromoExtension(
        val id: Long,
        val type: GdPromoExtensionType,
        val amount: Long?,
        val unit: GdPromoExtensionUnit?,
        val prefix: GdPromoExtensionPrefix?,
        val href: String?,
        val description: String,
        val startDate: LocalDate?,
        val finishDate: LocalDate?,
        val status: GdPromoExtensionStatus,
        val associatedCids: List<Long>,
)

fun TestGdPromoExtension?.checkGdPromoextensionEqualsExpected(
        promoExtension: PromoExtension,
        expectedStatus: GdPromoExtensionStatus,
        expectedAssociatedCids: List<Long>,
) = MatcherAssert.assertThat(this,
    Matchers.`is`(TestGdPromoExtension(
        id = promoExtension.promoExtensionId!!,
        type = GdPromoExtensionType.fromPromoactionsType(promoExtension.type),
        amount = promoExtension.amount,
        unit = promoExtension.unit?.let { GdPromoExtensionUnit.fromCoreValue(it) },
        prefix = promoExtension.prefix?.let { GdPromoExtensionPrefix.fromPromoactionsPrefix(it) },
        href = promoExtension.href,
        description = promoExtension.description,
        startDate = promoExtension.startDate,
        finishDate = promoExtension.finishDate,
        status = expectedStatus,
        associatedCids = expectedAssociatedCids,
    ))
)
