package ru.yandex.market.logistics.mqm.utils

import com.fasterxml.jackson.databind.node.POJONode

fun randomJsonNode() = POJONode(mapOf(Pair("key", "value")))
