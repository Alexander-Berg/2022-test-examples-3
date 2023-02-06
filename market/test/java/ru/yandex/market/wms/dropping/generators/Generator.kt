package ru.yandex.market.wms.dropping.generators

import ru.yandex.market.generators.generateArray
import ru.yandex.market.wms.dropping.data.dto.DropInfoDTO
import ru.yandex.market.wms.dropping.data.dto.DroppingParcelDTO
import ru.yandex.market.wms.dropping.data.dto.ParcelErrorDTO
import ru.yandex.market.wms.dropping.data.entities.Carrier
import ru.yandex.market.wms.dropping.data.entities.DroppingParcel
import kotlin.random.Random

fun generateCarrier(seed: Int): Carrier {
    val generator = Random(seed)
    return Carrier(
        company = "company: " + generator.nextInt(),
        storerKey = "storerKey: " + generator.nextInt(),
        type = "type: " + generator.nextInt(),
    )
}

fun generateDroppingParcelDTO(seed: Int): DroppingParcelDTO {
    val generator = Random(seed)
    return DroppingParcelDTO(
        carrier = generateCarrier(generator.nextInt()),
        dropId = "dropId: " + generator.nextInt(),
        orderKey = "orderKey: " + generator.nextInt(),
        orderType = "orderType: " + generator.nextInt(),
        parcelId = "parcelId: " + generator.nextInt(),
        parcelStatus = "parcelStatus: " + generator.nextInt(),
        shipDate = "shipDate: " + generator.nextInt(),
    )
}

fun generateParcel(seed: Int): DroppingParcel {
    val generator = Random(seed)
    return DroppingParcel(
        carrier = generateCarrier(generator.nextInt()),
        parcelId = "parcelId: " + generator.nextInt(),
        parcelStatus = "parcelStatus: " + generator.nextInt(),
        shipDate = "shipDate: " + generator.nextInt(),
        orderKey = "orderKey: " + generator.nextInt(),
        orderType = "orderType: " + generator.nextInt(),
        dropId = "dropId: " + generator.nextInt(),
    )
}

fun generateDropInfoDTO(seed: Int): DropInfoDTO {
    val generator = Random(seed)
    return DropInfoDTO(
        carrier = generateCarrier(generator.nextInt()),
        dropId = "dropId: " + generator.nextInt(),
        parcels = generateParcelsList(generator.nextInt()),
        status = "status: " + generator.nextInt(),
    )
}

fun generateParcelErrorDTO(seed: Int): ParcelErrorDTO {
    val generator = Random(seed)
    return ParcelErrorDTO(
        status = "status: " + generator.nextInt(),
        parcelId = "parcelId: " + generator.nextInt(),
        currentDropId = "currentDropId: " + generator.nextInt(),
    )
}

fun generateParcelsList(seed: Int) = generateArray(::generateParcel, seed, 5)
