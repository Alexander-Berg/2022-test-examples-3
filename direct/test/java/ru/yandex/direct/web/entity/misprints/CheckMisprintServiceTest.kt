package ru.yandex.direct.web.entity.misprints

import org.junit.Before
import org.junit.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import ru.yandex.direct.core.entity.misprints.MisprintFixlistService
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.web.entity.misprints.service.CheckMisprintService

class CheckMisprintServiceTest {
    private val MISPRINT_FIXLIST = listOf("any", "possible", "word", "non-special")

    @Mock
    private lateinit var misprintFixlistService: MisprintFixlistService

    @InjectMocks
    private lateinit var checkMisprintService: CheckMisprintService

    @Before
    fun init() {
        MockitoAnnotations.initMocks(this)
        Mockito.doReturn(MISPRINT_FIXLIST).`when`(misprintFixlistService).getMisprints()
    }

    @Test
    fun checkMisprints_noAny() {
        val expected = mapOf(0 to "any", 4 to "possible", 13 to "word", 18 to "non-special")
        val actual = checkMisprintService.checkMisprints("any possible word non-special")
        expected.checkEquals(actual)
    }

    @Test
    fun checkMisprints_oneWordMisprint() {
        val expected = mapOf(0 to "any")
        val actual = checkMisprintService.checkMisprints("any")
        expected.checkEquals(actual)
    }

    @Test
    fun checkMisprints_someMisprints() {
        val expected = mapOf(16 to "word")
        val actual = checkMisprintService.checkMisprints("some impossible word special")
        expected.checkEquals(actual)
    }

    @Test
    fun splitText_differentSpaces() {
        val expected = mapOf(0 to "any", 4 to "possible", 13 to "word", 18 to "non-special")
        val actual = checkMisprintService.splitText("any possible\tword non-special")
        expected.checkEquals(actual)
    }

    @Test
    fun splitText_trailingAndLeadingAndMultispaces() {
        val expected = mapOf(5 to "any", 12 to "possible", 24 to "word", 33 to "non-special")
        val actual = checkMisprintService.splitText("     any    possible    word     non-special  ")
        expected.checkEquals(actual)
    }

}
