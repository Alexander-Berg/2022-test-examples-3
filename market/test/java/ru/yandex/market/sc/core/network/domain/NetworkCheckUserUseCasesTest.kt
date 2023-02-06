package ru.yandex.market.sc.core.network.domain

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.auth.domain.AuthUserUseCases
import ru.yandex.market.sc.core.data.user.LogoutType
import ru.yandex.market.sc.core.network.repository.CheckUserRepository

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class NetworkCheckUserUseCasesTest {
    @Mock
    private lateinit var checkUserRepository: CheckUserRepository

    @Mock
    private lateinit var authUserUseCases: AuthUserUseCases

    private lateinit var useCases: NetworkCheckUserUseCases

    @Before
    fun setUp() {
        useCases = NetworkCheckUserUseCases(checkUserRepository, authUserUseCases)
    }

    @Test
    fun logoutUser() = runTest {
        useCases.logoutUser(LogoutType.MANUAL_LOGOUT)

        verify(checkUserRepository).logoutUser(LogoutType.MANUAL_LOGOUT)
        verify(checkUserRepository).cleanSavedData()
        verify(authUserUseCases).logout()
    }
}
