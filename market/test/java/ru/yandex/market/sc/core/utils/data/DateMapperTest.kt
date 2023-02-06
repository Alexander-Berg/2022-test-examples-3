package ru.yandex.market.sc.core.utils.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.utils.data.DateMapper.DateFormat
import ru.yandex.market.sc.core.utils.data.DateMapper.getDate

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class DateMapperTest {

    @Test
    fun `is in the past test`() {
        val dateFormat = DateMapper.parseDateFormat(getDate(-1))

        assertEquals(DateFormat.IN_PAST, dateFormat)
    }

    @Test
    fun `is today test`() {
        val dateFormat = DateMapper.parseDateFormat(getDate())

        assertEquals(DateFormat.TODAY, dateFormat)
    }

    @Test
    fun `is tomorrow test`() {
        val dateFormat = DateMapper.parseDateFormat(getDate(1))

        assertEquals(DateFormat.TOMORROW, dateFormat)
    }

    @Test
    fun `is in the future test`() {
        val dateFormat = DateMapper.parseDateFormat(getDate(2))

        assertEquals(DateFormat.FUTURE, dateFormat)
    }
}