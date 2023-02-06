package ru.yandex.market.mbi.feed.processor.test

import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.kotlin.times
import org.mockito.verification.VerificationMode
import ru.yandex.market.logbroker.LogbrokerEventPublisher
import ru.yandex.market.logbroker.event.LogbrokerEvent

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
inline fun <reified T : LogbrokerEvent<*>> LogbrokerEventPublisher<T>.capture(
    mode: VerificationMode = times(1),
): List<T> {
    val captor = ArgumentCaptor.forClass(T::class.java)
    Mockito.verify(this, mode).publishEvent(captor.capture())
    return captor.allValues
}
