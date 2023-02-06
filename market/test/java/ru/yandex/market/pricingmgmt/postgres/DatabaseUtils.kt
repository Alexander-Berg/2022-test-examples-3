package ru.yandex.market.pricingmgmt.postgres

import java.sql.ResultSet

object DatabaseUtils{
    fun <T> T?.orNull(rs: ResultSet): T? {
        return if (rs.wasNull()) null else this
    }
}
