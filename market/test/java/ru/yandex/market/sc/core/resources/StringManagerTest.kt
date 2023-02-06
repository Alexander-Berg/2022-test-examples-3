package ru.yandex.market.sc.core.resources

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class StringManagerTest {
    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var stringManager: StringManager

    private val stringWithoutArgument = "Test"
    private val stringArg = "123"
    private val intArg = 5
    private val stringWithArguments = "$stringWithoutArgument $stringArg $intArg"


    @Before
    fun setUp() {
        stringManager = StringManagerImpl(mockContext)
        `when`(mockContext.getString(R.string.string_for_test)).thenReturn(stringWithoutArgument)
        `when`(mockContext.getString(R.string.string_with_arguments_for_test, stringArg, intArg)).thenReturn(stringWithArguments)
    }

    @Test
    fun `string manager works correctly without arguments`() = runTest {
        assertThat(stringManager.getString(R.string.string_for_test)).isEqualTo(stringWithoutArgument)
    }

    @Test
    fun `string manager works correctly with arguments`() = runTest {
        assertThat(stringManager.getString(R.string.string_with_arguments_for_test, stringArg, intArg)).isEqualTo(stringWithArguments)
    }
}
