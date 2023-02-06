package ru.yandex.market.sc.core.auth.repository

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.yandex.passport.api.PassportApi
import com.yandex.passport.api.PassportToken
import com.yandex.passport.api.PassportUid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import ru.yandex.market.sc.core.auth.analytics.AppMetrica
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner::class)
class UserRepositoryTest {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var passportApi: PassportApi

    @Mock
    private lateinit var firebaseRepository: FirebaseRepository

    @Mock
    private lateinit var appMetrica: AppMetrica

    @Mock
    private lateinit var sharedPreferencesRepository: SharedPreferencesRepository

    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: UserRepository

    @Before
    fun setUp() {
        repository = UserRepository(
            context,
            passportApi,
            ioDispatcher,
            firebaseRepository,
            sharedPreferencesRepository,
            appMetrica
        )
    }

    @Test
    fun `sets token to null and login status when no uid is in shared preferences`() = runTest {
        `when`(sharedPreferencesRepository.uid).thenReturn(null)

        repository.initialize()

        assertThat(repository.token.getOrAwaitValue()).isNull()
        assertThat(repository.isLoggedIn.getOrAwaitValue()).isFalse()

        verify(firebaseRepository).setUserUid(null)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun `sets token and login status when uid is in shared preferences`() = runTest {
        val uid = 1L
        val passportUid = PassportUid.Factory.from(uid)

        `when`(sharedPreferencesRepository.uid).thenReturn(passportUid)
        `when`(passportApi.getToken(passportUid)).thenReturn(PassportToken { uid.toString() })

        repository.initialize()

        assertThat(repository.token.getOrAwaitValue()).isNotNull()
        assertThat(repository.isLoggedIn.getOrAwaitValue()).isTrue()

        verify(firebaseRepository).setUserUid(uid)
        verify(passportApi).getToken(PassportUid.Factory.from(uid))
    }
}
