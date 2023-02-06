package ru.yandex.market.sc.core.utils.ext

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class NavControllerKtTest {
    @Mock
    private lateinit var navController: NavController

    @Mock
    private lateinit var navDirections: NavDirections

    @Test(expected = ExceptionInInitializerError::class)
    fun `navigates safely`() {
        navController.safeNavigate(navDirections)

        `when`(navController.navigate(navDirections)).thenThrow(ExceptionInInitializerError::class.java)
        navController.safeNavigate(navDirections)
    }
}