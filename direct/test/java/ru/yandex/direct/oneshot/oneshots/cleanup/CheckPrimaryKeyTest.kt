package ru.yandex.direct.oneshot.oneshots.cleanup

import org.assertj.core.api.SoftAssertions
import org.jooq.Field
import org.jooq.impl.TableImpl
import org.jooq.impl.UpdatableRecordImpl
import org.junit.jupiter.api.Test
import ru.yandex.direct.oneshot.configuration.OneshotTest

@OneshotTest
class CheckPrimaryKeyTest {

    @Test
    fun testDeleteVcards() {
        check(DeleteVcardsDryRun.tables, "vcard_id")
    }

    @Test
    fun testDeleteBanners() {
        check(DeleteBannersDryRun.tables, "bid")
    }

    @Test
    fun testDeleteAdGroups() {
        check(DeleteAdGroupsDryRun.tables, "pid")
    }

    private fun check(mapping : Map<TableImpl<out UpdatableRecordImpl<*>>, Field<*>>, primaryKey : String ) {
        val softly = SoftAssertions()
        mapping.forEach { (table, key) ->
            softly.assertThat(key)
                .`as`("выборка из таблицы ${table.name} осуществляется по правильному ключу")
                .extracting("name")
                .isEqualTo(primaryKey)
        }
        softly.assertAll()
    }
}
