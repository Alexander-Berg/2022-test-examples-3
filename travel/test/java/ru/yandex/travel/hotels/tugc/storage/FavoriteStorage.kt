package ru.yandex.travel.hotels.tugc.storage

import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.jdbc.JdbcTestUtils
import java.util.Date
import javax.sql.DataSource

import ru.yandex.travel.hotels.tugc.tables.Favorite.FAVORITE
import ru.yandex.travel.hotels.tugc.entities.Favorite
import ru.yandex.travel.hotels.tugc.entities.record
import ru.yandex.travel.hotels.tugc.entities.toEntity
import ru.yandex.travel.hotels.tugc.tables.records.FavoriteRecord
import java.time.LocalDateTime

class FavoriteStorage {
    @Autowired private var dsl: DSLContext? = null

    var nextGeoId = 1
        get() = field++
    var nextPermalink = 1L
        get() = field++
    var nextYuid = 1
        get() = field++

    fun clear() = dsl!!
            .delete(FAVORITE)
            .where(FAVORITE.USER_ID.isNotNull)
            .execute()

    fun create(
        puid: Long? = null,
        yuid: String = "${nextYuid++}",
        geoId: Int = nextGeoId++,
        permalink: Long = nextPermalink++,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): Favorite {
        val record = Favorite(puid, yuid, permalink, geoId, createdAt).record()

        return dsl!!.insertInto(FAVORITE)
            .set(record)
            .returning()
            .fetchOne()
            .toEntity()
    }

    fun findByPermalink(permalink: Long): List<Favorite> {
        return dsl!!.selectFrom(FAVORITE)
            .where(FAVORITE.PERMALINK.eq(permalink))
            .fetch()
            .map(FavoriteRecord::toEntity)
    }

    fun findByUser(puid: Long?, yuid: String): List<Favorite> {
        val where = puid?.let {
            FAVORITE.USER_ID.`in`("puid_$puid", "yuid_$yuid")
        } ?: FAVORITE.USER_ID.eq("yuid_$yuid")

        return dsl!!.selectFrom(FAVORITE)
                .where(where)
                .fetch()
                .map(FavoriteRecord::toEntity)
    }
}
