package ru.yandex.market.mbi.feed.processor.yt.reader

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Тестовый ридер из yt, который возвращает замоканный поток данных.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class TestYtTableReader<T : Any>(
    private val data: List<T>
) : YtTableReader<T> {

    override fun read(): Flow<T> = flow { data.forEach { emit(it) } }
}
