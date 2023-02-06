package ru.yandex.market.mapi.db.mock

import java.io.Closeable

class RecipeAwarePostgres(private val portEnvName: String = DEFAULT_PORT_ENV_NAME): Closeable {

    companion object {
        private const val DEFAULT_PORT_ENV_NAME = "PG_LOCAL_PORT"
    }

    fun getPort(): Int {
        return System.getenv(portEnvName).toInt()
    }

    override fun close() {
        // noop
    }
}
