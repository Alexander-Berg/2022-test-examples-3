package ru.yandex.direct.core.testing.data

import ru.yandex.direct.core.entity.hypergeo.model.GeoSegmentType
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeo
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSegment
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSegmentDetails
import ru.yandex.direct.core.entity.hypergeo.model.HyperPoint
import ru.yandex.direct.regions.Region.RUSSIA_REGION_ID

const val HYPER_GEO_ID = 4321L
const val HYPER_GEO_NAME = "Default hyper geo"

const val HYPER_GEO_SEGMENT_ID = 3425L
const val CLIENT_ID = 345354L

const val SEGMENT_NAME = "Default segment name"
const val SEGMENT_RADIUS = 632
const val TIMES_QUANTITY = 75
const val PERIOD_LENGTH = 57

const val LATITUDE = 34.424234
const val LONGITUDE = 45.321234
const val ADDRESS = "Default address"

fun defaultHyperGeo(
    id: Long = HYPER_GEO_ID,
    name: String = HYPER_GEO_NAME,
    hyperGeoSegments: List<HyperGeoSegment> = defaultHyperGeoSegments()
): HyperGeo =
    HyperGeo()
        .withId(id)
        .withName(name)
        .withHyperGeoSegments(hyperGeoSegments)

fun defaultHyperGeoSegments(): List<HyperGeoSegment> = listOf(defaultHyperGeoSegment())

fun defaultHyperGeoSegment(
    id: Long = HYPER_GEO_SEGMENT_ID,
    clientId: Long = CLIENT_ID,
    coveringGeo: List<Long> = listOf(RUSSIA_REGION_ID),
    segmentDetails: HyperGeoSegmentDetails? = defaultHyperGeoSegmentDetails()
): HyperGeoSegment =
    HyperGeoSegment()
        .withId(id)
        .withClientId(clientId)
        .withCoveringGeo(coveringGeo)
        .withSegmentDetails(segmentDetails)

fun defaultHyperGeoSegmentDetails(
    name: String? = SEGMENT_NAME,
    radius: Int? = SEGMENT_RADIUS,
    points: List<HyperPoint>? = defaultHyperPoints(),
    geoSegmentType: GeoSegmentType = GeoSegmentType.REGULAR,
    timesQuantity: Int? = null,
    periodLength: Int? = null,
): HyperGeoSegmentDetails =
    HyperGeoSegmentDetails()
        .withSegmentName(name)
        .withRadius(radius)
        .withPoints(points)
        .withGeoSegmentType(geoSegmentType)
        .withPeriodLength(periodLength)
        .withTimesQuantity(timesQuantity)

fun defaultHyperPoints() = listOf(defaultHyperPoint())

fun defaultHyperPoint(
    latitude: Double? = LATITUDE,
    longitude: Double? = LONGITUDE,
    address: String = ADDRESS
): HyperPoint =
    HyperPoint()
        .withLatitude(latitude)
        .withLongitude(longitude)
        .withAddress(address)
