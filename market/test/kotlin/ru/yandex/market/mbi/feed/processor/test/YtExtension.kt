package ru.yandex.market.mbi.feed.processor.test

import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.verification.VerificationMode
import ru.yandex.market.yt.client.YtClientProxy

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
inline fun <reified T> YtClientProxy.capture(
    ytPath: String? = null,
    mode: VerificationMode = times(1),
): List<T> {
    val captorRows = ArgumentCaptor.forClass(List::class.java)
    Mockito.verify(this, mode).insertRows(if (ytPath == null) any() else eq(ytPath), any(), captorRows.capture())
    return captorRows.allValues.flatten().map { it as T }
}
