package ru.yandex.market.pricingmgmt.service.promo.converters

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.pricingmgmt.AbstractFunctionalTest

internal abstract class AbstractRestrictionExtendedConverterTest<KEY, MODEL, DTO, CONVERTER : RestrictionExtendedConverter<KEY, MODEL, DTO>>(
    private val notEmptyOk: List<MODEL>,
    private val notEmptyOkExpected: List<DTO>,
    private val notExistsThrows: List<MODEL>,
    private val notExistsExpected: String
) : AbstractFunctionalTest() {

    @Autowired
    lateinit var converter: CONVERTER

    @Test
    fun convert_null_ok() {
        Assertions.assertNull(converter.convert(null))
    }

    @Test
    fun convert_empty_ok() {
        Assertions.assertEquals(emptyList<DTO>(), converter.convert(emptyList()))
    }

    @Test
    fun convert_notEmpty_ok() {
        Assertions.assertEquals(notEmptyOkExpected, converter.convert(notEmptyOk))
    }

    @Test
    fun convert_nonExists_throws() {
        val e = assertThrows<RuntimeException> { converter.convert(notExistsThrows) }
        Assertions.assertEquals(notExistsExpected, e.message)
    }
}
