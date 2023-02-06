package ru.yandex.market.logistics.les.repository

import com.yandex.ydb.table.values.ListValue
import com.yandex.ydb.table.values.PrimitiveValue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.entity.ydb.EntityDao
import ru.yandex.market.logistics.les.repository.ydb.YdbEntityRepository
import ru.yandex.market.logistics.les.repository.ydb.description.EntitiesTableDescription.Companion.ENTITY_ID_IDX
import ru.yandex.market.logistics.les.repository.ydb.description.EntitiesTableDescription.Companion.ENTITY_TYPE_IDX
import ru.yandex.market.ydb.integration.query.YdbSelect
import ru.yandex.market.ydb.integration.utils.Converter
import ru.yandex.market.ydb.integration.utils.ListConverter

class YdbEntityRepositoryTest : AbstractContextualTest() {

    @Autowired
    lateinit var repository: YdbEntityRepository

    @Test
    fun loadById() {
        val searchId = 1L

        repository.loadById(searchId)

        val captor = argumentCaptor<YdbSelect>()
        verify(ydbTemplate).selectFirst(captor.capture(), any(), any<Converter<EntityDao>>())
        val capturedSelect = captor.firstValue

        capturedSelect.text() shouldBe SELECT_ALL_QUERY_PREFIX +
            "as entities " +
            "where (entities.id = ${capturedSelect.params().keys.first()})"
        capturedSelect.params().values.first() shouldBe searchId.toUint64()
    }

    @Test
    fun loadByIds() {
        val searchIds = listOf(1L, 2L)

        repository.loadByIds(searchIds)

        val captor = argumentCaptor<YdbSelect>()
        verify(ydbTemplate).selectList(captor.capture(), any(), any<ListConverter<EntityDao>>())
        val capturedSelect = captor.firstValue

        capturedSelect.text() shouldBe SELECT_ALL_QUERY_PREFIX +
            "as entities " +
            "where (entities.id IN ${capturedSelect.params().keys.first()})"
        capturedSelect.params().values.first() shouldBe ListValue.of(searchIds[0].toUint64(), searchIds[1].toUint64())
    }

    @Test
    fun loadByEntityId() {
        val entityId = "EnT"

        repository.loadByEntityId(entityId, PAGEABLE)

        val captor = argumentCaptor<YdbSelect>()
        verify(ydbTemplate).selectList(captor.capture(), any(), any<ListConverter<EntityDao>>())
        val capturedSelect = captor.firstValue
        val param = capturedSelect.params().entries.find { it.key != "\$limit" }!!

        capturedSelect.text() shouldBe SELECT_ALL_QUERY_PREFIX +
            "view $ENTITY_ID_IDX as entities " +
            "where (entities.entity_id = ${param.key}) " +
            "limit \$limit"
        param.value shouldBe PrimitiveValue.utf8(entityId)
        capturedSelect.limit().limit shouldBe PAGEABLE.pageSize
    }

    @Test
    fun loadByEntityType() {
        val entityType = "eTyp"

        repository.loadByEntityType(entityType, PAGEABLE)

        val captor = argumentCaptor<YdbSelect>()
        verify(ydbTemplate).selectList(captor.capture(), any(), any<ListConverter<EntityDao>>())
        val capturedSelect = captor.firstValue
        val param = capturedSelect.params().entries.find { it.key != "\$limit" }!!

        capturedSelect.text() shouldBe SELECT_ALL_QUERY_PREFIX +
            "view $ENTITY_TYPE_IDX as entities " +
            "where (entities.entity_type = ${param.key}) " +
            "limit \$limit"
        param.value shouldBe PrimitiveValue.utf8(entityType)
        capturedSelect.limit().limit shouldBe PAGEABLE.pageSize
    }

    companion object {
        private const val SELECT_ALL_QUERY_PREFIX = "select " +
            "entities.entity_id as entities_entity_id, " +
            "entities.entity_type as entities_entity_type, " +
            "entities.id as entities_id " +
            "from `null/les/entities` "

        private val PAGEABLE = PageRequest.of(0, 20)
    }
}
