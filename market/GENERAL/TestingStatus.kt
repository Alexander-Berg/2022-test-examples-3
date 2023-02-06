package ru.yandex.market.partner.status.wizard.model.check.moderation

import ru.yandex.market.mbi.open.api.client.model.TestingStatusDTO

enum class TestingStatus {

    UNDEFINED,
    INITED,
    READY_FOR_CHECK,
    WAITING_FEED_FIRST_LOAD,
    CHECKING,
    WAITING_FEED_LAST_LOAD,
    PASSED,
    CANCELED,
    READY_TO_FAIL,
    FAILED,
    DISABLED,
    EXPIRED,
    PENDING_CHECK_START,
    NEED_INFO;

    companion object {

        fun fromDto(testingStatusDTO: TestingStatusDTO): TestingStatus {
            return when (testingStatusDTO) {
                TestingStatusDTO.UNDEFINED -> UNDEFINED
                TestingStatusDTO.INITED -> INITED
                TestingStatusDTO.READY_FOR_CHECK -> READY_FOR_CHECK
                TestingStatusDTO.WAITING_FEED_FIRST_LOAD -> WAITING_FEED_FIRST_LOAD
                TestingStatusDTO.CHECKING -> CHECKING
                TestingStatusDTO.WAITING_FEED_LAST_LOAD -> WAITING_FEED_LAST_LOAD
                TestingStatusDTO.PASSED -> PASSED
                TestingStatusDTO.CANCELED -> CANCELED
                TestingStatusDTO.READY_TO_FAIL -> READY_TO_FAIL
                TestingStatusDTO.FAILED -> FAILED
                TestingStatusDTO.DISABLED -> DISABLED
                TestingStatusDTO.EXPIRED -> EXPIRED
                TestingStatusDTO.PENDING_CHECK_START -> PENDING_CHECK_START
                TestingStatusDTO.NEED_INFO -> NEED_INFO
            }
        }
    }
}
