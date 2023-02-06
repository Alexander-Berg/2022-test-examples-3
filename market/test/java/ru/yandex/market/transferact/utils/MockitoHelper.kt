package ru.yandex.market.transferact.utils

import org.mockito.ArgumentCaptor

class MockitoHelper {
    companion object {
        fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
    }
}
