package ru.yandex.market.logistics.calendaring.config

import org.dbunit.dataset.datatype.DataType
import org.dbunit.dataset.datatype.DataTypeException
import ru.yandex.market.logistics.test.integration.db.PostgresqlDataTypeFactoryExt
import java.sql.Types


class CustomPostgresqlDataTypeFactory : PostgresqlDataTypeFactoryExt() {

    override fun isEnumType(sqlTypeName: String): Boolean {
        return DB_ENUMS.contains(sqlTypeName.toLowerCase())
    }

    @Throws(DataTypeException::class)
    override fun createDataType(sqlType: Int, sqlTypeName: String): DataType? {
        var type = sqlType
        if (isEnumType(sqlTypeName)) {
            type = Types.OTHER
        }
        return super.createDataType(type, sqlTypeName)
    }

    companion object {
        private val DB_ENUMS = setOf("booking_status","request_status","mapper_cast_type")
    }

}
