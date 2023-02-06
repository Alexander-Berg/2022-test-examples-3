package ru.yandex.market.logistics.calendaring.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType
import ru.yandex.market.logistics.calendaring.model.dto.MetaMapperErrorDTO
import ru.yandex.market.logistics.calendaring.service.mapper.MetaMapperErrorService
import ru.yandex.market.logistics.calendaring.service.mapper.MetaMapperSelectorService

class MetaMapperSelectorTest(
    @Autowired private val service: MetaMapperSelectorService,
    @Autowired private val errorService: MetaMapperErrorService,
): AbstractContextualTest() {

    @Test
    @DatabaseSetup("classpath:fixtures/service/mapper-selector/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/mapper-selector/select-success/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun selectedSuccessfully() {
        val errors: MutableList<MetaMapperErrorDTO> = java.util.ArrayList()
        val mapper = service.selectMapper("test", BookingType.SUPPLY,errors)

        assertEquals(mapper.bookingType, BookingType.SUPPLY)
        assertEquals(mapper.id, 2)
        assertEquals(mapper.source, "test")
        assertEquals(mapper.condition, "test")
        assertEquals(mapper.field, "meta")
    }

    @Test
    @DatabaseSetup("classpath:fixtures/service/mapper-selector/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/mapper-selector/select-too-many/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun selectSuccessButTooManyMappers() {
        val errors: MutableList<MetaMapperErrorDTO> = java.util.ArrayList()
        val mapper = service.selectMapper("test10", BookingType.SUPPLY, errors)
        errorService.saveAll(errors)

        assertEquals(mapper.bookingType, BookingType.SUPPLY)
        assertEquals(mapper.id, 2)
        assertEquals(mapper.source, "test")
        assertEquals(mapper.condition, "test")
        assertEquals(mapper.field, "meta")
    }

    @Test
    @DatabaseSetup("classpath:fixtures/service/mapper-selector/before.xml")

    fun selectDefaultMapper() {
        val errors: MutableList<MetaMapperErrorDTO> = java.util.ArrayList()
        val mapper = service.selectMapper("test10", BookingType.MOVEMENT_WITHDRAW,errors)

        assertEquals(mapper.id, 1)
        assertEquals(mapper.bookingType, null)
        assertEquals(mapper.source, null)
        assertEquals(mapper.condition, null)
        assertEquals(mapper.field, "meta")
    }

    @Test
    @DatabaseSetup("classpath:fixtures/service/mapper-selector/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/mapper-selector/select-success/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    fun testSearchHierarchy() {
        val errors: MutableList<MetaMapperErrorDTO> = java.util.ArrayList()
        // Search by source + booking_type
        var mapper = service.selectMapper("test", BookingType.SUPPLY, errors)
        assertNotNull(mapper)
        assertEquals(mapper.id, 2)

        // Search by source
        mapper = service.selectMapper("test", BookingType.MOVEMENT_SUPPLY, errors)
        assertNotNull(mapper)
        assertEquals(mapper.id, 2)

        // Search by booking_type
        mapper = service.selectMapper("test10", BookingType.WITHDRAW, errors)
        assertNotNull(mapper)
        assertEquals(mapper.id, 3)

    }
}
