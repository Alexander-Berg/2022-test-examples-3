package ru.yandex.direct.core.testing.data

import ru.yandex.direct.core.entity.promoextension.model.PromoExtension
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsStatusmoderate
import ru.yandex.direct.dbschema.ppc.enums.PromoactionsType.discount
import ru.yandex.direct.dbutil.model.ClientId
import java.time.LocalDate

fun testPromoExtension(
    id: Long?,
    clientId: ClientId,
    description: String,
    href: String?,
    startDate: LocalDate?,
    finishDate: LocalDate?,
    statusModerate: PromoactionsStatusmoderate,
) = PromoExtension(id, clientId, discount, null, null, null, href,
    description, startDate, finishDate, statusModerate)

fun defaultPromoExtension(clientId: ClientId) = testPromoExtension(
    null, clientId, "промоакция", "https://ya.ru", null, null,
    PromoactionsStatusmoderate.Ready)
