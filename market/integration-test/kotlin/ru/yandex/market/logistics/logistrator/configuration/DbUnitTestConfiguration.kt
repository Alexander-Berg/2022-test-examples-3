package ru.yandex.market.logistics.logistrator.configuration

import com.github.springtestdbunit.bean.DatabaseConfigBean
import org.dbunit.dataset.datatype.DataType
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration
import ru.yandex.market.logistics.test.integration.db.PostgresqlDataTypeFactoryExt
import java.sql.Types

@Configuration
@Import(DbUnitTestConfiguration::class)
class DbUnitTestConfiguration {

    @Bean
    fun dbUnitDatabaseConfig() = DatabaseConfigBean().apply {
        datatypeFactory = DataTypeFactory()
        tableType = TABLE_TYPE
    }

    private class DataTypeFactory : PostgresqlDataTypeFactoryExt() {

        override fun isEnumType(sqlTypeName: String?) = ENUM_TYPE_NAMES.contains(sqlTypeName)

        override fun createDataType(sqlType: Int, sqlTypeName: String?): DataType =
            super.createDataType(if (isEnumType(sqlTypeName)) Types.OTHER else sqlType, sqlTypeName)

        private companion object {
            val ENUM_TYPE_NAMES = setOf("request_status")
        }
    }

    private companion object {
        private val TABLE_TYPE = arrayOf("TABLE", "VIEW")
    }
}
