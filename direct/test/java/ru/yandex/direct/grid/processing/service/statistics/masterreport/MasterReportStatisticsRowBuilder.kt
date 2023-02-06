package ru.yandex.direct.grid.processing.service.statistics.masterreport

import java.math.BigDecimal

fun buildRow(
        shows: Number?,
        clicks: Number?,
        ctr: Number?,
        conversions: Number?,
        conversionRate: Number?,
        cost: Number?,
        income: Number?,
        uniqViewers: Number?,
        roi: Number?,
        profit: Number?,
        costPerConversion: Number?,
        crr: Number?,
        avgCpc: Number?,
        bounceRatio: Number?,
        depth: Number?,
        goalStatistics: List<Map<String, Any?>>? = null
): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    putTo(map, "shows", shows)
    putTo(map, "clicks", clicks)
    putTo(map, "ctr", ctr)
    putTo(map, "conversions", conversions)
    putTo(map, "conversionRate", conversionRate)
    putTo(map, "cost", cost)
    putTo(map, "income", income)
    putTo(map, "uniqViewers", uniqViewers)
    putTo(map, "roi", roi)
    putTo(map, "profit", profit)
    putTo(map, "costPerConversion", costPerConversion)
    putTo(map, "crr", crr)
    putTo(map, "avgCpc", avgCpc)
    putTo(map, "bounceRatio", bounceRatio)
    putTo(map, "depth", depth)
    putGoalStatistics(map, goalStatistics)
    return map
}

fun buildTotalRow(
        shows: Number?,
        clicks: Number?,
        ctr: Number?,
        conversions: Number?,
        conversionRate: Number?,
        cost: Number?,
        income: Number?,
        uniqViewers: Number?,
        roi: Number?,
        profit: Number?,
        costPerConversion: Number?,
        crr: Number?,
        avgCpc: Number?,
        bounceRatio: Number?,
        depth: Number?
): Map<String, Map<String, Number?>> {
    return buildTotalRow(
            shows, null,
            clicks, null,
            ctr, null,
            conversions, null,
            conversionRate, null,
            cost, null,
            income, null,
            uniqViewers, null,
            roi, null,
            profit, null,
            costPerConversion, null,
            crr, null,
            avgCpc, null,
            bounceRatio, null,
            depth, null,
    )
}

fun buildTotalRow(
        shows: Number?,
        showsDelta: Number?,
        clicks: Number?,
        clicksDelta: Number?,
        ctr: Number?,
        ctrDelta: Number?,
        conversions: Number?,
        conversionsDelta: Number?,
        conversionRate: Number?,
        conversionRateDelta: Number?,
        cost: Number?,
        costDelta: Number?,
        income: Number?,
        incomeDelta: Number?,
        uniqViewers: Number?,
        uniqViewersDelta: Number?,
        roi: Number?,
        roiDelta: Number?,
        profit: Number?,
        profitDelta: Number?,
        costPerConversion: Number?,
        costPerConversionDelta: Number?,
        crr: Number?,
        crrDelta: Number?,
        avgCpc: Number?,
        avgCpcDelta: Number?,
        bounceRatio: Number?,
        bounceRatioDelta: Number?,
        depth: Number?,
        depthDelta: Number?,
): Map<String, Map<String, Number?>> {
    val map = mutableMapOf<String, Map<String, Number?>>()
    putTo(map, "shows", shows, showsDelta)
    putTo(map, "clicks", clicks, clicksDelta)
    putTo(map, "ctr", ctr, ctrDelta)
    putTo(map, "conversions", conversions, conversionsDelta)
    putTo(map, "conversionRate", conversionRate, conversionRateDelta)
    putTo(map, "cost", cost, costDelta)
    putTo(map, "income", income, incomeDelta)
    putTo(map, "uniqViewers", uniqViewers, uniqViewersDelta)
    putTo(map, "roi", roi, roiDelta)
    putTo(map, "profit", profit, profitDelta)
    putTo(map, "costPerConversion", costPerConversion, costPerConversionDelta)
    putTo(map, "crr", crr, crrDelta)
    putTo(map, "avgCpc", avgCpc, avgCpcDelta)
    putTo(map, "bounceRatio", bounceRatio, bounceRatioDelta)
    putTo(map, "depth", depth, depthDelta)
    return map
}

private fun putTo(map: MutableMap<String, Map<String, Number?>>, key: String, value: Number?, valueAbsDelta: Number?) {
    map[key] = mapOf("value" to convertValue(value), "valueAbsDelta" to convertValue(valueAbsDelta))
}

private fun putTo(map: MutableMap<String, Any?>, key: String, value: Number?) {
    map[key] = mapOf("value" to convertValue(value))
}

private fun putGoalStatistics(map: MutableMap<String, Any?>, stats: List<Map<String, Any?>>?) {
    map["goalStatistics"] = stats?.map {
        it.entries.map { e ->
            val key = e.key
            val value = e.value
            when {
                key == "goalId" -> key to value
                value is Number -> key to mapOf("value" to convertValue(value))
                else -> key to value
            }
        }.toMap()
    }
}

private fun convertValue(value: Number?): BigDecimal? {
    return when (value) {
        null -> null
        else -> BigDecimal.valueOf(value.toDouble())
    }
}
