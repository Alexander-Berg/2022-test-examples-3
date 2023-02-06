package ru.yandex.market.sc.test.network.domain.common

import android.content.SharedPreferences
import ru.yandex.market.sc.core.data.sorting_center.ApiSortingCenterMapper
import ru.yandex.market.sc.core.utils.constants.SettingsPreferences.SORTING_CENTER_KEY
import ru.yandex.market.sc.test.network.constants.AccountCredentials
import ru.yandex.market.sc.test.network.constants.Configuration
import ru.yandex.market.sc.test.network.constants.MockSortingCenter
import javax.inject.Inject

class TestSortingCenterUtils @Inject constructor(
    private val apiSortingCenterMapper: ApiSortingCenterMapper,
    private val sharedPreferences: SharedPreferences,
) {
    fun getCurrentScId(): Long {
        return getScById().id
    }

    fun getCurrentScPartnerId(): Long {
        return getScById().partnerId
    }

    fun getCurrentScToken(): String {
        return getScById().token
    }

    fun getAccountCredentials(): AccountCredentials {
        return getScById().credentials
    }

    private fun getScById(): MockSortingCenter {
        val sortingCenter = apiSortingCenterMapper.getSortingCenter(
            sharedPreferences.getString(
                SORTING_CENTER_KEY,
                null
            )
        )
        val mockSortingCenters = Configuration.sortingCenters.values
        val mockSortingCenter = mockSortingCenters.firstOrNull { it.id == sortingCenter?.id }
        return checkNotNull(mockSortingCenter) {
            "Не удалось найти СЦ $sortingCenter, в списке $mockSortingCenters"
        }
    }
}
