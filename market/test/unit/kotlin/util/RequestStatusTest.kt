package ru.yandex.market.logisitcs.calendaring.util

import org.junit.Test
import org.junit.jupiter.api.TestInstance

import ru.yandex.market.logistics.calendaring.client.dto.enums.RequestStatus
import ru.yandex.market.logistics.calendaring.util.RequestStatusMapper.toRequestStatus


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RequestStatusTest {

    val VALID_STATUS = "created"
    val NON_VALID_STATUS = "UNLOADING"

    @Test
    fun mapNonValidStatus(){
        val unknowStatus = toRequestStatus(NON_VALID_STATUS)
        val nullStatus = toRequestStatus(null)

        assert(unknowStatus == RequestStatus.UNKNOWN)
        assert(nullStatus == RequestStatus.UNKNOWN)
    }

    @Test
    fun mapValidStatus(){
        val validStatus = toRequestStatus(VALID_STATUS)
        assert(validStatus == RequestStatus.CREATED)
    }
}
