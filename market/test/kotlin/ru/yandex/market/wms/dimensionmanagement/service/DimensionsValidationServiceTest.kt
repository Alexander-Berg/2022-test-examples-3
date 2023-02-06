package ru.yandex.market.wms.dimensionmanagement.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.market.wms.shared.libs.configproperties.dao.GlobalConfigurationDao
import ru.yandex.market.wms.common.service.DbConfigService
import ru.yandex.market.wms.dimensionmanagement.model.Dimensions
import ru.yandex.market.wms.dimensionmanagement.validation.AllDimensionsSameValidationRule
import ru.yandex.market.wms.dimensionmanagement.validation.AtLeastOneDimensionIsBigValidationRule
import ru.yandex.market.wms.dimensionmanagement.validation.DimensionIsNotPositiveValidationRule
import ru.yandex.market.wms.dimensionmanagement.validation.DimensionsValidationCode
import ru.yandex.market.wms.dimensionmanagement.validation.DimensionsValidationLevel
import ru.yandex.market.wms.dimensionmanagement.validation.DimensionsValidationResult
import ru.yandex.market.wms.dimensionmanagement.validation.HighDensityValidationRule
import ru.yandex.market.wms.dimensionmanagement.validation.LowDensityValidationRule
import java.math.BigDecimal
import java.math.BigDecimal.valueOf

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [
    DimensionsValidationService::class,
    SettingsService::class,
    DbConfigService::class,
    DimensionIsNotPositiveValidationRule::class,
    AtLeastOneDimensionIsBigValidationRule::class,
    LowDensityValidationRule::class,
    HighDensityValidationRule::class,
    AllDimensionsSameValidationRule::class
])
class DimensionsValidationServiceTest {

    @Autowired
    private lateinit var dimensionsValidationService: DimensionsValidationService

    @MockBean
    private lateinit var configurationDao: GlobalConfigurationDao

    @BeforeEach
    fun setUp() {
        whenever(configurationDao.getStringConfigValue(MAX_ALLOWED_SIDE_LENGTH)).thenReturn("400")
        whenever(configurationDao.getStringConfigValue(MAX_ALLOWED_WEIGHT)).thenReturn("100")
        whenever(configurationDao.getStringConfigValue(MIN_DENSITY)).thenReturn("0.01")
        whenever(configurationDao.getStringConfigValue(MAX_DENSITY)).thenReturn("7")
    }

    @Test
    fun shouldSuccessValidateIfDimensionsValidAndAllowedLevelOk() {
        val dimensions = Dimensions(valueOf(1.2), valueOf(30), valueOf(20), BigDecimal.TEN)
        val allowedValidationLevel = DimensionsValidationLevel.OK
        val validationResult = dimensionsValidationService.validate(dimensions, allowedValidationLevel)

        assertTrue(validationResult.isValid)
    }

     @Test
     fun shouldSuccessValidateIfDimensionsValidAndAllowedLevelWarn() {
         val dimensions = Dimensions(valueOf(1.2), valueOf(30), valueOf(20), BigDecimal.TEN)
         val allowedValidationLevel = DimensionsValidationLevel.WARN
         val validationResult = dimensionsValidationService.validate(dimensions, allowedValidationLevel)

         assertTrue(validationResult.isValid)
     }

    @Test
    fun shouldSuccessValidateIfDimensionsNotValidAndAllowedLevelWarn() {
        val dimensions = Dimensions(valueOf(1.2), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN)
        val allowedValidationLevel = DimensionsValidationLevel.WARN
        val validationResult = dimensionsValidationService.validate(dimensions, allowedValidationLevel)

        assertTrue(validationResult.isValid)
    }

    @Test
    fun shouldNoSuccessValidateIfDimensionsNotValidAndAllowedLevelOk() {
        val dimensions = Dimensions(valueOf(1.2), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN)
        val allowedValidationLevel = DimensionsValidationLevel.OK
        val validationResult = dimensionsValidationService.validate(dimensions, allowedValidationLevel)

        assertFalse(validationResult.isValid)
        assertEquals(DimensionsValidationCode.ALL_DIMENSIONS_SAME, validationResult.validationCode)
        assertEquals(DimensionsValidationLevel.WARN, validationResult.validationLevel)
    }

    @Test
    fun shouldNoSuccessValidateIfWeightIsBigAndAllowedLevelOk() {
        val dimensions = Dimensions(valueOf(101), BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN)
        val allowedValidationLevel = DimensionsValidationLevel.OK
        val validationResult = dimensionsValidationService.validate(dimensions, allowedValidationLevel)

        assertFalse(validationResult.isValid)
        assertEquals(DimensionsValidationCode.AT_LEAST_ONE_DIMENSION_IS_BIG, validationResult.validationCode)
        assertEquals(DimensionsValidationLevel.WARN, validationResult.validationLevel)
        assertEquals(valueOf(100), validationResult.args[DimensionsValidationResult.MAX_WEIGHT_PARAMETER])
        assertEquals(valueOf(400), validationResult.args[DimensionsValidationResult.MAX_SIDE_LENGTH_PARAMETER])
    }

    @Test
    fun shouldNoSuccessValidateIfOneSideLengthIsBigAndAllowedLevelOk() {
        val dimensions = Dimensions(valueOf(1.2), valueOf(400.1), BigDecimal.TEN, BigDecimal.TEN)
        val allowedValidationLevel = DimensionsValidationLevel.OK
        val validationResult = dimensionsValidationService.validate(dimensions, allowedValidationLevel)

        assertFalse(validationResult.isValid)
        assertEquals(DimensionsValidationCode.AT_LEAST_ONE_DIMENSION_IS_BIG, validationResult.validationCode)
        assertEquals(DimensionsValidationLevel.WARN, validationResult.validationLevel)
        assertEquals(valueOf(100), validationResult.args[DimensionsValidationResult.MAX_WEIGHT_PARAMETER])
        assertEquals(valueOf(400), validationResult.args[DimensionsValidationResult.MAX_SIDE_LENGTH_PARAMETER])
    }

    @Test
    fun shouldNoSuccessValidateIfDimensionsLowDensityAndAllowedLevelOk() {
        val dimensions = Dimensions(valueOf(0.000048), valueOf(3), valueOf(2), BigDecimal.ONE)
        val allowedValidationLevel = DimensionsValidationLevel.OK
        val validationResult = dimensionsValidationService.validate(dimensions, allowedValidationLevel)

        assertFalse(validationResult.isValid)
        assertEquals(DimensionsValidationCode.LOW_DENSITY, validationResult.validationCode)
        assertEquals(DimensionsValidationLevel.WARN, validationResult.validationLevel)
        assertEquals(valueOf(0.008).setScale(5), validationResult.args[DimensionsValidationResult.DENSITY_PARAMETER])
        assertEquals(valueOf(0.01), validationResult.args[DimensionsValidationResult.MIN_DENSITY_PARAMETER])
    }

    @Test
    fun shouldNoSuccessValidateIfDimensionsHighDensityAndAllowedLevelOk() {
        val dimensions = Dimensions(valueOf(0.0486), valueOf(3), valueOf(2), BigDecimal.ONE)
        val allowedValidationLevel = DimensionsValidationLevel.OK
        val validationResult = dimensionsValidationService.validate(dimensions, allowedValidationLevel)

        assertFalse(validationResult.isValid)
        assertEquals(DimensionsValidationCode.HIGH_DENSITY, validationResult.validationCode)
        assertEquals(DimensionsValidationLevel.WARN, validationResult.validationLevel)
        assertTrue(valueOf(8.1).setScale(5) == validationResult.args[DimensionsValidationResult.DENSITY_PARAMETER])
        assertTrue(valueOf(7) == validationResult.args[DimensionsValidationResult.MAX_DENSITY_PARAMETER])
    }

    @Test
    fun shouldNoSuccessValidateIfDimensionsIsZeroAndAllowedLevelWarn() {
        val dimensions = Dimensions(valueOf(1.2), valueOf(30), valueOf(20), BigDecimal.ZERO)
        val allowedValidationLevel = DimensionsValidationLevel.WARN
        val validationResult = dimensionsValidationService.validate(dimensions, allowedValidationLevel)

        assertFalse(validationResult.isValid)
        assertEquals(DimensionsValidationCode.DIMENSION_IS_NOT_POSITIVE, validationResult.validationCode)
        assertEquals(DimensionsValidationLevel.CRIT, validationResult.validationLevel)
    }
}
