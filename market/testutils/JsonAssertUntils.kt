package ru.yandex.market.mdm.service.functional.testutils

import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.ValueMatcher
import org.skyscreamer.jsonassert.comparator.CustomComparator

infix fun String.shouldBeEqualJsonStrictFindResponse(expected: String) {
    JSONAssert.assertEquals(
        expected, this, CustomComparator(
            JSONCompareMode.STRICT,
            // commonParamValues.versionFrom generates at output moment, so need to ignore it
            Customization.customization(
                "commonEntities[*].commonParamValues[*].versionFrom",
                IgnoreValueMatcher()
            ),
            Customization.customization(
                "commonEntities[*].commonParamValues[*].structs[*].commonParamValues[*].versionFrom",
                IgnoreValueMatcher()
            ),
            // some another timestamps hard to track down, so ignore all timestamps
            Customization.customization(
                "commonEntities[*].commonParamValues[*].timestamps[*]",
                IgnoreValueMatcher()
            ),
            Customization.customization(
                "commonEntities[*].commonParamValues[*].structs[*].commonParamValues[*].timestamps[*]",
                IgnoreValueMatcher()
            ),
        )
    )
}

infix fun String.shouldBeEqualJsonLenientFindResponse(expected: String) {
    JSONAssert.assertEquals(
        expected, this, CustomComparator(
            JSONCompareMode.LENIENT,
            // commonParamValues.versionFrom generates at output moment, so need to ignore it
            Customization.customization(
                "commonEntities[*].commonParamValues[*].versionFrom",
                IgnoreValueMatcher()
            ),
            Customization.customization(
                "commonEntities[*].commonParamValues[*].structs[*].commonParamValues[*].versionFrom",
                IgnoreValueMatcher()
            ),
            // some another timestamps hard to track down, so ignore all timestamps
            Customization.customization(
                "commonEntities[*].commonParamValues[*].timestamps[*]",
                IgnoreValueMatcher()
            ),
            Customization.customization(
                "commonEntities[*].commonParamValues[*].structs[*].commonParamValues[*].timestamps[*]",
                IgnoreValueMatcher()
            ),
        )
    )
}


infix fun String.shouldBeEqualJsonResponseWithoutMetadataCheck(expected: String) {
    JSONAssert.assertEquals(
        expected, this, CustomComparator(
            JSONCompareMode.STRICT,
            // commonParamValues.versionFrom generates at output moment, so need to ignore it
            Customization.customization(
                "commonEntities[*].commonParamValues[*].versionFrom",
                IgnoreValueMatcher()
            ),
            // version_to generates at output moment, so need to ignore it
            // some another timestamps hard to track down, so ignore all timestamps
            Customization.customization(
                "commonEntities[*].commonParamValues[*].timestamps[*]",
                IgnoreValueMatcher()
            ),
            Customization.customization(
                "metadata",
                IgnoreValueMatcher()
            ),
            Customization.customization(
                "commonEntities[*].commonEntityType",
                IgnoreValueMatcher()
            )
        )
    )
}

infix fun String.shouldBeEqualJsonLenient(expected: String) {
    JSONAssert.assertEquals(expected, this, JSONCompareMode.LENIENT)
}

internal class IgnoreValueMatcher : ValueMatcher<Any?> {
    override fun equal(o1: Any?, o2: Any?): Boolean {
        return true
    }
}
