package ru.yandex.market.contentmapping.utils

import org.awaitility.Awaitility
import org.awaitility.Duration
import org.awaitility.core.ConditionFactory

fun await5s(): ConditionFactory = Awaitility.await().atMost(Duration.FIVE_SECONDS)
