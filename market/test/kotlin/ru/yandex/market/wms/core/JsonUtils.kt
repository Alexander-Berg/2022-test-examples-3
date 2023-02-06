package ru.yandex.market.wms.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

val mapper = ObjectMapper().registerKotlinModule()

inline fun <reified T> String.fromJson(): T = mapper.readValue(this)
