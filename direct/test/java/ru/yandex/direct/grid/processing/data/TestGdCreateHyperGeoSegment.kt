package ru.yandex.direct.grid.processing.data

import ru.yandex.direct.core.testing.data.ADDRESS
import ru.yandex.direct.core.testing.data.LATITUDE
import ru.yandex.direct.core.testing.data.LONGITUDE
import ru.yandex.direct.core.testing.data.SEGMENT_NAME
import ru.yandex.direct.core.testing.data.SEGMENT_RADIUS
import ru.yandex.direct.grid.processing.model.hypergeo.GdGeoSegmentType
import ru.yandex.direct.grid.processing.model.hypergeo.GdHyperPoint
import ru.yandex.direct.grid.processing.model.hypergeo.mutation.createhypergeosegment.GdCreateHyperGeoSegment

fun defaultGdCreateHyperGeoSegment(
    gdGeoSegmentType: GdGeoSegmentType? = null,
    segmentName: String = SEGMENT_NAME,
    radius: Int = SEGMENT_RADIUS,
    gdHyperPoints: List<GdHyperPoint> = listOf(defaultGdHyperPoint()),
    timesQuantity: Int? = null,
    periodLength: Int? = null,
): GdCreateHyperGeoSegment {
    return GdCreateHyperGeoSegment()
        .withGeoSegmentType(gdGeoSegmentType)
        .withSegmentName(segmentName)
        .withRadius(radius)
        .withPoints(gdHyperPoints)
        .withTimesQuantity(timesQuantity)
        .withPeriodLength(periodLength)
}

fun defaultGdHyperPoint(
    latitude: Double = LATITUDE,
    longitude: Double = LONGITUDE,
    address: String = ADDRESS,
): GdHyperPoint {
    return GdHyperPoint()
        .withAddress(address)
        .withLatitude(latitude)
        .withLongitude(longitude)
}
