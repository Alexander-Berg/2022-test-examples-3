package ru.yandex.direct.chassis.entity.startrek

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AutocommentReleasesJobKtTest {
    private val releaseKey = "DIRECT-134428"
    private val trackerAppName = "intapi"

    @Test
    fun allOkQuery_success() {
        val actual = allOkQuery(trackerAppName, releaseKey)
        assertThat(actual)
                .isEqualTo("""
                    Status: Closed, "Beta-tested", "Awaiting Release"
                    AND "Affected Apps": !empty()
                    AND ("Affected Apps": !intapi OR "Tested Apps": intapi)
                    AND Relates: DIRECT-134428
                """.trimIndent())
    }

    @Test
    fun wontTestQuery_success() {
        val actual = wontTestQuery(releaseKey)
        assertThat(actual)
                .isEqualTo("""
                    "Affected Apps": none
                    AND Relates: DIRECT-134428
                """.trimIndent())
    }

    @Test
    fun toReviewQuery_success() {
        val actual = toReviewQuery(trackerAppName, releaseKey)
        assertThat(actual)
                .isEqualTo("""
                    (
                    Status: !Closed AND Status: !"Beta-tested" AND Status: !"Awaiting Release"
                    OR "Affected Apps": empty()
                    OR Status: Closed AND "Affected Apps": intapi AND "Tested Apps": !intapi
                    ) AND Relates: DIRECT-134428
                    AND (Queue: DIRECT OR Queue: DIRECTMIGR)
                    AND Tags: !$DIRECT_RELEASE_TESTS_TICKET_TAG
                    AND Tags: !кроссрелизные_зависимости
                    AND Type: !Release
                """.trimIndent())
    }

    @Test
    fun toExternalReviewQuery_success() {
        val actual = toExternalReviewQuery(trackerAppName, releaseKey)
        assertThat(actual)
                .isEqualTo("""
                    (
                    Status: !Closed AND Status: !"Beta-tested" AND Status: !"Awaiting Release"
                    OR "Affected Apps": empty()
                    OR Status: Closed AND "Affected Apps": intapi AND "Tested Apps": !intapi
                    ) AND Relates: DIRECT-134428
                    AND Tags:! $DIRECT_RELEASE_TESTS_TICKET_TAG
                    AND (Queue: !DIRECT AND Queue: !DIRECTMIGR)
                """.trimIndent())
    }

    @Test
    fun toTestQuery_success() {
        val actual = toTestQuery(trackerAppName, releaseKey)
        assertThat(actual)
                .isEqualTo("""
                    Status: "Beta-tested", "Awaiting Release"
                    AND "Affected Apps": intapi
                    AND "Tested Apps": !intapi
                    AND Tags:! $DIRECT_RELEASE_TESTS_TICKET_TAG
                    AND Relates: DIRECT-134428
                """.trimIndent())
    }
}
