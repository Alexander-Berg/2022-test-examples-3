package ru.yandex.market.mdm.service.functional.testutils

import org.mockito.Mockito
import org.springframework.context.annotation.Configuration
import ru.yandex.market.mdm.auth.MdmUserRepository
import ru.yandex.market.mdm.db.config.SqlMdmDatasourceConfig
import ru.yandex.market.mdm.lib.database.MdmStorageKeyValueRepository
import ru.yandex.market.mdm.service.common_entity.config.MdmServiceRepositoryConfig

@Configuration
open class TestServiceRepositoryConfig(
) : MdmServiceRepositoryConfig(Mockito.mock(SqlMdmDatasourceConfig::class.java)) {
    override fun mdmUserRepository(): MdmUserRepository {
        return Mockito.mock(MdmUserRepository::class.java)
    }

    override fun skv(): MdmStorageKeyValueRepository {
        return Mockito.mock(MdmStorageKeyValueRepository::class.java)
    }
}
