package ru.yandex.market.contentmapping.config

import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import ru.yandex.market.contentmapping.repository.CategoryInfoRepository

@TestConfiguration
open class TestDaoConfig(db: SqlDatasourceConfig, kv: KeyValueConfig) : DaoConfig(db, kv) {
    private val categoryId = 99595L

    override fun goodsGroupRepository() = Mockito.spy(super.goodsGroupRepository())

    override fun categoryInfoRepository(): CategoryInfoRepository {
        return Mockito.mock(CategoryInfoRepository::class.java)
    }
}
