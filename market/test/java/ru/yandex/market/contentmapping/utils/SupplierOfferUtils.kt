package ru.yandex.market.contentmapping.utils

import ru.yandex.market.mboc.http.SupplierOffer

fun supplierOffer(builder: (SupplierOffer.Offer.Builder) -> Unit) =
        SupplierOffer.Offer.newBuilder().apply(builder).build()
