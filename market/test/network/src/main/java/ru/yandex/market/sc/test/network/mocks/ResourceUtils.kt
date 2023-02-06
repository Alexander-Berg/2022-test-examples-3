package ru.yandex.market.sc.test.network.mocks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.yandex.market.sc.core.network.vo.Resource

fun <T : Any> successResource(data: T) = MutableLiveData<Resource<T>>().apply {
    value = Resource.success(data)
} as LiveData<Resource<T>>

fun <T : Any> errorResource(message: String, data: T? = null) =
    MutableLiveData<Resource<T>>().apply {
        value = Resource.error(message, data)
    } as LiveData<Resource<T>>