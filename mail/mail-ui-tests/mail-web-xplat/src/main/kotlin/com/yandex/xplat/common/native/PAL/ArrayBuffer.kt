package com.yandex.xplat.common

class ArrayBuffer(val byteArray: ByteArray) {
    val byteLength = byteArray.size
    fun slice(begin: Int, end: Int = byteLength) = byteArray.slice(begin..end)
}
