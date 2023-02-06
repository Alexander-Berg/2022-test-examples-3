package ru.yandex.market.logistics.les.service

import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.stream.Stream
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageRequest
import ru.yandex.market.logistics.les.AbstractContextualTest
import ru.yandex.market.logistics.les.admin.model.dto.requests.AdminGetEventsFilterRequest
import ru.yandex.market.logistics.les.base.EntityType
import ru.yandex.market.logistics.les.entity.ydb.EntityDao
import ru.yandex.market.logistics.les.entity.ydb.EventDao
import ru.yandex.market.logistics.les.entity.ydb.EventToEntityDao
import ru.yandex.market.logistics.les.entity.ydb.EventWithEntityDao
import ru.yandex.market.logistics.les.repository.ydb.YdbEntityRepository
import ru.yandex.market.logistics.les.repository.ydb.YdbEventRepository
import ru.yandex.market.logistics.les.repository.ydb.YdbEventToEntityRepository
import ru.yandex.market.logistics.les.service.EventSearchService.Companion.MAX_PAGE_SIZE

class EventSearchServiceTest : AbstractContextualTest() {

    @Autowired
    lateinit var eventSearchService: EventSearchService

    @MockBean
    lateinit var eventRepository: YdbEventRepository

    @MockBean
    lateinit var entityRepository: YdbEntityRepository

    @MockBean
    lateinit var eventToEntityRepository: YdbEventToEntityRepository

    @BeforeEach
    fun setUp() {
        whenever(eventRepository.loadByIds(listOf(EVENT_LONG_ID))).thenReturn(listOf(EVENT_DAO))
        whenever(eventRepository.loadByEventId(EVENT_ID, PAGEABLE)).thenReturn(listOf(EVENT_DAO))
        whenever(eventRepository.loadByEventType(EVENT_TYPE, PAGEABLE)).thenReturn(listOf(EVENT_DAO))
        whenever(eventRepository.loadBySource(SOURCE, PAGEABLE)).thenReturn(listOf(EVENT_DAO))
        whenever(eventRepository.loadBySourceAndEventType(SOURCE, EVENT_TYPE, PAGEABLE)).thenReturn(listOf(EVENT_DAO))
        whenever(eventRepository.loadWithoutFilters(PAGEABLE)).thenReturn(listOf(EVENT_DAO))


        whenever(eventToEntityRepository.loadByEntityIds(listOf(ENTITY_LONG_ID), MAX_PAGE_SIZE))
            .thenReturn(listOf(EVENT_TO_ENTITY_DAO))
        whenever(eventToEntityRepository.loadByEventIds(listOf(EVENT_LONG_ID), MAX_PAGE_SIZE))
            .thenReturn(listOf(EVENT_TO_ENTITY_DAO))

        whenever(entityRepository.loadById(ENTITY_LONG_ID)).thenReturn(ENTITY_DAO)
        whenever(entityRepository.loadByEntityId(ENTITY_ID, PAGEABLE)).thenReturn(listOf(ENTITY_DAO))
        whenever(entityRepository.loadByEntityType(ENTITY_TYPE.name, PAGEABLE)).thenReturn(listOf(ENTITY_DAO))
        whenever(entityRepository.loadByIds(listOf(ENTITY_LONG_ID))).thenReturn(listOf(ENTITY_DAO))
    }

    @Test
    fun findNonExistingEvent() {
        whenever(eventRepository.loadByEventId(EVENT_ID, PAGEABLE)).thenReturn(listOf())
        val searchResult =
            eventSearchService.findByFilter(AdminGetEventsFilterRequest(EVENT_ID, null, null, null, null), PAGEABLE)

        verify(eventRepository).loadByEventId(EVENT_ID, PAGEABLE)
        verifyNoMoreInteractions(eventToEntityRepository, entityRepository)
        searchResult shouldBe emptyList()
    }

    @Test
    fun findNonExistingEntity() {
        whenever(entityRepository.loadByEntityId(ENTITY_ID, PAGEABLE)).thenReturn(listOf())
        val searchResult =
            eventSearchService.findByFilter(AdminGetEventsFilterRequest(null, null, null, ENTITY_ID, null), PAGEABLE)

        verify(entityRepository).loadByEntityId(ENTITY_ID, PAGEABLE)
        verifyNoMoreInteractions(eventToEntityRepository, eventRepository)
        searchResult shouldBe emptyList()
    }

    @AfterEach
    fun tearDown() {
        verifyNoMoreInteractions(eventRepository, eventToEntityRepository, entityRepository)
    }

    @ParameterizedTest
    @MethodSource("filters")
    fun findByFilter(filter: AdminGetEventsFilterRequest) {
        val searchResult = eventSearchService.findByFilter(filter, PAGEABLE)

        var searchByEventsFirst = true
        when {
            filter.eventId != null -> verify(eventRepository).loadByEventId(EVENT_ID, PAGEABLE)

            filter.entityId != null && filter.entityType != null -> verify(entityRepository).loadById(ENTITY_LONG_ID)
                .also { searchByEventsFirst = false }

            filter.entityId != null -> verify(entityRepository).loadByEntityId(ENTITY_ID, PAGEABLE)
                .also { searchByEventsFirst = false }

            filter.source != null && filter.eventType != null ->
                verify(eventRepository).loadBySourceAndEventType(SOURCE, EVENT_TYPE, PAGEABLE)

            filter.source != null -> verify(eventRepository).loadBySource(SOURCE, PAGEABLE)

            filter.eventType != null -> verify(eventRepository).loadByEventType(EVENT_TYPE, PAGEABLE)

            filter.entityType != null -> verify(entityRepository).loadByEntityType(ENTITY_TYPE.name, PAGEABLE)
                .also { searchByEventsFirst = false }

            else -> verify(eventRepository).loadWithoutFilters(PAGEABLE)
        }

        if (searchByEventsFirst) {
            verifyFindEntitiesForEvents()
        } else {
            verifyFindEventsForEntities()
        }

        searchResult shouldBe listOf(
            EventWithEntityDao(
                EVENT_DAO.eventId,
                EVENT_DAO.eventType,
                EVENT_DAO.source,
                ENTITY_DAO.entityId,
                ENTITY_DAO.entityType,
                EVENT_DAO.timestamp,
                EVENT_DAO.description
            )
        )
    }

    private fun verifyFindEntitiesForEvents() {
        verify(eventToEntityRepository).loadByEventIds(listOf(EVENT_LONG_ID), MAX_PAGE_SIZE)
        verify(entityRepository).loadByIds(listOf(ENTITY_LONG_ID))
    }

    private fun verifyFindEventsForEntities() {
        verify(eventToEntityRepository).loadByEntityIds(listOf(ENTITY_LONG_ID), MAX_PAGE_SIZE)
        verify(eventRepository).loadByIds(listOf(EVENT_LONG_ID))
    }

    companion object {
        private const val EVENT_LONG_ID = 10L
        private const val EVENT_ID = "TEST_EVENT_ID"
        private const val EVENT_TYPE = "TEST_EVENT_TYPE"
        private const val SOURCE = "TEST_SOURCE"
        private const val ENTITY_ID = "TEST_ENTITY_ID"
        private val ENTITY_TYPE = EntityType.ORDER
        private val ENTITY_LONG_ID = EntityDao(null, ENTITY_ID, ENTITY_TYPE).calcIdHash()

        private const val PAGE_SIZE = 20
        private val PAGEABLE = PageRequest.of(0, PAGE_SIZE)
        private val EVENT_DAO =
            EventDao(EVENT_LONG_ID, EVENT_ID, EVENT_TYPE, SOURCE, "payload", "description", Instant.MIN)
        private val EVENT_TO_ENTITY_DAO = EventToEntityDao(ENTITY_LONG_ID, EVENT_LONG_ID)
        private val ENTITY_DAO = EntityDao(ENTITY_LONG_ID, ENTITY_ID, ENTITY_TYPE)


        @JvmStatic
        fun filters(): Stream<Arguments> {
            val filters = mutableListOf<Arguments>()
            for (i in (0 until 32)) {
                filters.add(
                    Arguments.of(
                        AdminGetEventsFilterRequest(
                            if (checkBitInValue(i, 0)) EVENT_ID else null,
                            if (checkBitInValue(i, 1)) EVENT_TYPE else null,
                            if (checkBitInValue(i, 2)) SOURCE else null,
                            if (checkBitInValue(i, 3)) ENTITY_ID else null,
                            if (checkBitInValue(i, 4)) ENTITY_TYPE else null,
                        )
                    )
                )
            }

            return filters.stream()
        }

        private fun checkBitInValue(value: Int, bitNumber: Int) = value and (1 shl bitNumber) != 0
    }
}
