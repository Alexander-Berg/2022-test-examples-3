package ru.yandex.market.pricingmgmt.pg

import org.dbunit.dataset.datatype.DataType
import org.dbunit.dataset.datatype.DataTypeException
import ru.yandex.market.common.test.db.ddl.datatype.CustomPostgresqlDataTypeFactory
import java.sql.Types

class ExtendedPostgresqlDataTypeFactory : CustomPostgresqlDataTypeFactory() {
    companion object {
        private val ENUM_NAMES = setOf(
            "price_type",
            "journal_status",
            "day_of_week",
            "promo_type",
            "price_threshold",
            "bound_type",
            "hack_promo_status",
            "price_import_job_status",
        )
    }

    override fun isEnumType(sqlTypeName: String) =
        ENUM_NAMES.contains(sqlTypeName.lowercase())

    @Throws(DataTypeException::class)
    override fun createDataType(sqlType: Int, sqlTypeName: String): DataType {
        var dataType = sqlType
        if (isEnumType(sqlTypeName)) {
            dataType = Types.OTHER
        }

        return super.createDataType(dataType, sqlTypeName)
    }
}
