package ru.yandex.market.markup3.mboc

import ru.yandex.market.mboc.http.MboCategory
import ru.yandex.market.mboc.http.SupplierOffer

fun taskOffer(
    offerId: Long,
    businessId: Long,
    categoryId: Long,
    skuType: SupplierOffer.SkuType = SupplierOffer.SkuType.TYPE_MARKET,
    processingCounter: Long = 0,
    priority: Double = 0.0,
    critical: Boolean = false,
    deadline: Long = 0,
    ticketId: TicketId = 0
) = MboCategory.GetTaskOffersResponse.TaskOffer.newBuilder()
    .setOfferId(offerId)
    .setBusinessId(businessId)
    .setCategoryId(categoryId)
    .setSuggestSkuType(skuType)
    .setProcessingCounter(processingCounter)
    .setPriority(priority)
    .setCritical(critical)
    .setDeadline(deadline)
    .setTicketId(ticketId)
