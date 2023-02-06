package ru.yandex.market.tpl.courier

import android.os.Parcel
import android.os.Parcelable
import io.kotest.matchers.shouldBe
import ru.yandex.market.tpl.courier.arch.android.ext.use
import ru.yandex.market.tpl.courier.presentation.feature.photo.PhotoViewerArgs

inline fun <reified T : Parcelable> checkWriteIntoParcelAndReadBackWithoutErrors(instance: T) {
    val marshalled = Parcel.obtain().use {
        it.writeParcelable(instance, 0)
        it.marshall()
    }

    val unmarshalled = Parcel.obtain().use {
        it.unmarshall(marshalled, 0, marshalled.size)
        it.setDataPosition(0)
        it.readParcelable<T>(T::class.java.classLoader)
    }

    unmarshalled shouldBe instance
}