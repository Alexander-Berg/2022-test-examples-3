package ru.yandex.travel.hotels.extranet

import io.grpc.Context
import ru.yandex.travel.credentials.UserCredentials

internal fun withCredentials(credentials: UserCredentials, f: () -> Unit) {
    val ctx = Context.current()
        .withValue(UserCredentials.KEY, credentials)
    ctx.run(f)
}
