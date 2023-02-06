package ru.beru.sortingcenter.ui.common

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import ru.beru.sortingcenter.BuildConfig
import ru.yandex.market.sc.core.network.domain.NetworkFirebaseUseCases
import ru.yandex.market.sc.core.network.repository.SharedPreferenceRepository
import ru.yandex.market.sc.core.utils.data.DateMapper
import ru.yandex.market.sc.test.network.mocks.TestFactory
import ru.yandex.market.test.arch.ext.commonViewModelTestRules
import ru.yandex.market.test.utils.getOrAwaitValue

@RunWith(MockitoJUnitRunner.StrictStubs::class)
class UserFirebaseServiceTest {
    @get:Rule
    val commonRules = commonViewModelTestRules()

    @Mock
    private lateinit var networkFirebaseUseCases: NetworkFirebaseUseCases

    @Mock
    private lateinit var sharedPreferencesRepository: SharedPreferenceRepository

    private lateinit var userFirebaseService: UserFirebaseService

    @Before
    fun setUp() {
        userFirebaseService = UserFirebaseService(networkFirebaseUseCases, sharedPreferencesRepository)
    }

    @Test
    fun `should update correctly`() = runTest {
        val version = TestFactory.getVersion(versionCode = BuildConfig.VERSION_CODE + 1)
        `when`(networkFirebaseUseCases.getVersion()).thenReturn(version)

        userFirebaseService.checkVersion()
        assertEquals(version, userFirebaseService.version.getOrAwaitValue())
    }

    @Test
    fun `equal current version do not update`() = runTest {
        val version = TestFactory.getVersion(BuildConfig.VERSION_CODE)
        `when`(networkFirebaseUseCases.getVersion()).thenReturn(version)

        userFirebaseService.checkVersion()
        assertNull(userFirebaseService.version.getOrAwaitValue())
    }

    @Test
    fun `old version do not update`() = runTest {
        val version = TestFactory.getVersion(versionCode = BuildConfig.VERSION_CODE - 1)
        `when`(networkFirebaseUseCases.getVersion()).thenReturn(version)

        userFirebaseService.checkVersion()
        assertNull(userFirebaseService.version.getOrAwaitValue())
    }

    @Test
    fun `deferred update`() = runTest {
        val date = DateMapper.parse(DateMapper.getDate(shift = 1)) ?: throw IllegalStateException()
        val version = TestFactory.getVersion(versionCode = BuildConfig.VERSION_CODE + 1, updateAfter = date)
        `when`(networkFirebaseUseCases.getVersion()).thenReturn(version)

        userFirebaseService.checkVersion()
        assertNull(userFirebaseService.version.getOrAwaitValue())
    }
}
