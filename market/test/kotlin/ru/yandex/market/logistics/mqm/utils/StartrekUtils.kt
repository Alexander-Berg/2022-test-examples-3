package ru.yandex.market.logistics.mqm.utils

import ru.yandex.bolts.collection.MapF
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.Update

fun extractIssueFields(issueValues: MapF<String, Update<*>>) = issueValues
    .entries()
    .map { fieldKey, fieldValue -> Pair(fieldKey, (fieldValue as ScalarUpdate).set.get() as String) }
    .toMap()

fun getTagAsList(issueUpdate: IssueUpdate, key: String): List<String> {
    val update = issueUpdate.values.getOrThrow(key) as ScalarUpdate
    return (update.set.get() as String)
        .split(",")
        .map(String::trim)
}

fun getTagAsInt(issueUpdate: IssueUpdate, key: String): Int {
    val update = issueUpdate.values.getOrThrow(key) as ScalarUpdate
    return (update.set.get() as Int)
}

fun MapF<String, Update<*>>.getAsScalarUpdate(key: String) = (getOrThrow(key) as ScalarUpdate<*>).set.get()
