package ru.yandex.market.mbi.feed.processor.api

import org.junit.jupiter.api.Test
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.common.test.spring.FunctionalTestHelper
import ru.yandex.market.mbi.feed.processor.FunctionalTest
import ru.yandex.market.mbi.feed.processor.test.ApiUrl
import ru.yandex.market.mbi.feed.processor.test.isEqualTo

/**
 * Тесты для [MigrationController]
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
internal class MigrationControllerTest : FunctionalTest() {

    private val url: String by ApiUrl("/migration/whitelist/check")

    @Test
    fun `partner not in env`() {
        FunctionalTestHelper.get("$url?partner_id=100&updated_at=2021-01-01T10:00:30Z")
            .isEqualTo<MigrationControllerTest>("migration/disabled.response.json")
    }

    @Test
    @DbUnitDataSet(before = ["migration/partner.before.csv"])
    fun `partner in env with future time`() {
        FunctionalTestHelper.get("$url?partner_id=100&updated_at=2021-01-01T05:00:30Z")
            .isEqualTo<MigrationControllerTest>("migration/disabled.response.json")
    }

    @Test
    @DbUnitDataSet(before = ["migration/partner.before.csv"])
    fun `partner in env with actual time`() {
        FunctionalTestHelper.get("$url?partner_id=100&updated_at=2021-01-01T10:00:30Z")
            .isEqualTo<MigrationControllerTest>("migration/enabled.response.json")
    }
}
