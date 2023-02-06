package ru.yandex.direct.core.entity.hypergeo.validation

import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.hypergeo.validation.HyperGeoSegmentValidationService.MAX_GEO_SEGMENT_CIRCLE_RADIUS
import ru.yandex.direct.core.entity.hypergeo.validation.HyperGeoSegmentValidationService.MIN_GEO_SEGMENT_CIRCLE_RADIUS
import ru.yandex.direct.core.entity.vcard.service.validation.PointOnMapValidator.LATITUDE_MAX
import ru.yandex.direct.core.entity.vcard.service.validation.PointOnMapValidator.LATITUDE_MIN
import ru.yandex.direct.core.entity.vcard.service.validation.PointOnMapValidator.LONGITUDE_MAX
import ru.yandex.direct.core.entity.vcard.service.validation.PointOnMapValidator.LONGITUDE_MIN
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegmentDetails
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegments
import ru.yandex.direct.core.testing.data.defaultHyperPoint
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Size.SIZE_CANNOT_BE_LESS_THAN_MIN
import ru.yandex.direct.validation.defect.ids.NumberDefectIds.MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN
import ru.yandex.direct.validation.defect.ids.NumberDefectIds.MUST_BE_LESS_THEN_OR_EQUAL_TO_MAX
import ru.yandex.direct.validation.defect.ids.StringDefectIds.CANNOT_BE_EMPTY
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.DefectId
import ru.yandex.direct.validation.result.DefectIds.CANNOT_BE_NULL
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult
import java.math.BigDecimal

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class HyperGeoSegmentValidationServiceTest {

    @Autowired
    private lateinit var hyperGeoSegmentValidationService: HyperGeoSegmentValidationService

    @Test
    fun validateHyperGeoSegments_PositiveCase() {
        hyperGeoSegmentValidationService.validateHyperGeoSegments(defaultHyperGeoSegments())
            .checkHasNoDefects()
    }

    @Test
    fun validateHyperGeoSegments_NoSegmentDetailsProvided_ValidationError() {
        val hyperGeoSegments = listOf(defaultHyperGeoSegment(segmentDetails = null))

        hyperGeoSegmentValidationService.validateHyperGeoSegments(hyperGeoSegments)
            .checkHasDefect(
                path = path(index(0), field("segmentDetails")),
                defectId = CANNOT_BE_NULL)
    }

    @Test
    fun validateHyperGeoSegments_NoRadiusProvided_ValidationError() {
        val hyperGeoSegments = listOf(
            defaultHyperGeoSegment(segmentDetails = defaultHyperGeoSegmentDetails(radius = null)))

        hyperGeoSegmentValidationService.validateHyperGeoSegments(hyperGeoSegments)
            .checkHasDefect(
                path = path(index(0), field("segmentDetails"), field("radius")),
                defectId = CANNOT_BE_NULL)
    }

    @Test
    fun validateHyperGeoSegments_RadiusGreaterThanMax_ValidationError() {
        val hyperGeoSegments = listOf(
            defaultHyperGeoSegment(segmentDetails = defaultHyperGeoSegmentDetails(radius = MAX_GEO_SEGMENT_CIRCLE_RADIUS + 1)))

        hyperGeoSegmentValidationService.validateHyperGeoSegments(hyperGeoSegments)
            .checkHasDefect(
                path = path(index(0), field("segmentDetails"), field("radius")),
                defectId = MUST_BE_LESS_THEN_OR_EQUAL_TO_MAX)
    }

    @Test
    fun validateHyperGeoSegments_RadiusLessThanMin_ValidationError() {
        val hyperGeoSegments = listOf(
            defaultHyperGeoSegment(segmentDetails = defaultHyperGeoSegmentDetails(radius = MIN_GEO_SEGMENT_CIRCLE_RADIUS - 1)))

        hyperGeoSegmentValidationService.validateHyperGeoSegments(hyperGeoSegments)
            .checkHasDefect(
                path = path(index(0), field("segmentDetails"), field("radius")),
                defectId = MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN)
    }

    @Test
    fun validateHyperGeoSegments_BlankSegmentName_ValidationError() {
        val hyperGeoSegments = listOf(
            defaultHyperGeoSegment(segmentDetails = defaultHyperGeoSegmentDetails(name = "    ")))

        hyperGeoSegmentValidationService.validateHyperGeoSegments(hyperGeoSegments)
            .checkHasDefect(
                path = path(index(0), field("segmentDetails"), field("segmentName")),
                defectId = CANNOT_BE_EMPTY)
    }

    @Test
    fun validateHyperGeoSegments_NoPointsProvided_ValidationError() {
        val hyperGeoSegments = listOf(
            defaultHyperGeoSegment(segmentDetails = defaultHyperGeoSegmentDetails(points = null)))

        hyperGeoSegmentValidationService.validateHyperGeoSegments(hyperGeoSegments)
            .checkHasDefect(
                path = path(index(0), field("segmentDetails"), field("points")),
                defectId = CANNOT_BE_NULL)
    }

    @Test
    fun validateHyperGeoSegments_EmptyPointsProvided_ValidationError() {
        val hyperGeoSegments = listOf(
            defaultHyperGeoSegment(segmentDetails = defaultHyperGeoSegmentDetails(points = emptyList())))

        hyperGeoSegmentValidationService.validateHyperGeoSegments(hyperGeoSegments)
            .checkHasDefect(
                path = path(index(0), field("segmentDetails"), field("points")),
                defectId = SIZE_CANNOT_BE_LESS_THAN_MIN)
    }

    @Test
    fun validateHyperGeoSegments_NoLatitudeProvided_ValidationError() {
        val hyperGeoSegments = listOf(defaultHyperGeoSegment(
            segmentDetails = defaultHyperGeoSegmentDetails(points = listOf(defaultHyperPoint(latitude = null)))))

        hyperGeoSegmentValidationService.validateHyperGeoSegments(hyperGeoSegments)
            .checkHasDefect(
                path = path(index(0), field("segmentDetails"), field("points"), index(0), field("latitude")),
                defectId = CANNOT_BE_NULL)
    }

    @Test
    fun validateHyperGeoSegments_LatitudeGreaterThanMax_ValidationError() {
        val hyperGeoSegments = listOf(defaultHyperGeoSegment(
            segmentDetails = defaultHyperGeoSegmentDetails(
                points = listOf(defaultHyperPoint(latitude = 1.0 + LATITUDE_MAX)))))

        hyperGeoSegmentValidationService.validateHyperGeoSegments(hyperGeoSegments)
            .checkHasDefect(
                path = path(index(0), field("segmentDetails"), field("points"), index(0), field("latitude")),
                defectId = MUST_BE_LESS_THEN_OR_EQUAL_TO_MAX)
    }

    @Test
    fun validateHyperGeoSegments_LatitudeLessThanMin_ValidationError() {
        val hyperGeoSegments = listOf(defaultHyperGeoSegment(
            segmentDetails = defaultHyperGeoSegmentDetails(
                points = listOf(defaultHyperPoint(latitude = -1.0 + LATITUDE_MIN)))))

        hyperGeoSegmentValidationService.validateHyperGeoSegments(hyperGeoSegments)
            .checkHasDefect(
                path = path(index(0), field("segmentDetails"), field("points"), index(0), field("latitude")),
                defectId = MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN)
    }

    @Test
    fun validateHyperGeoSegments_NoLongitudeProvided_ValidationError() {
        val hyperGeoSegments = listOf(defaultHyperGeoSegment(
            segmentDetails = defaultHyperGeoSegmentDetails(points = listOf(defaultHyperPoint(longitude = null)))))

        hyperGeoSegmentValidationService.validateHyperGeoSegments(hyperGeoSegments)
            .checkHasDefect(
                path = path(index(0), field("segmentDetails"), field("points"), index(0), field("longitude")),
                defectId = CANNOT_BE_NULL)
    }

    @Test
    fun validateHyperGeoSegments_LongitudeGreaterThanMax_ValidationError() {
        val hyperGeoSegments = listOf(defaultHyperGeoSegment(
            segmentDetails = defaultHyperGeoSegmentDetails(
                points = listOf(defaultHyperPoint(longitude = 1.0 + LONGITUDE_MAX)))))

        hyperGeoSegmentValidationService.validateHyperGeoSegments(hyperGeoSegments)
            .checkHasDefect(
                path = path(index(0), field("segmentDetails"), field("points"), index(0), field("longitude")),
                defectId = MUST_BE_LESS_THEN_OR_EQUAL_TO_MAX)
    }

    @Test
    fun validateHyperGeoSegments_LongitudeLessThanMin_ValidationError() {
        val hyperGeoSegments = listOf(defaultHyperGeoSegment(
            segmentDetails = defaultHyperGeoSegmentDetails(
                points = listOf(defaultHyperPoint(longitude = -1.0 + LONGITUDE_MIN)))))

        hyperGeoSegmentValidationService.validateHyperGeoSegments(hyperGeoSegments)
            .checkHasDefect(
                path = path(index(0), field("segmentDetails"), field("points"), index(0), field("longitude")),
                defectId = MUST_BE_GREATER_THAN_OR_EQUAL_TO_MIN)
    }
}

private fun <T> ValidationResult<T, Defect<*>>.checkHasNoDefects() = assertThat(this, hasNoDefectsDefinitions())
private fun <T> ValidationResult<T, Defect<*>>.checkHasDefect(path: Path, defectId: DefectId<*>) =
    assertThat(this, hasDefectDefinitionWith(validationError(path, defectId)))

private operator fun Double.plus(other: BigDecimal): Double = this + other.toDouble()
