package ru.yandex.market.logistics.calendaring.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest

class MetaInfoChangeSavingProcessorTest(
    @Autowired val metaInfoChangeSavingProcessor: MetaInfoChangeSavingProcessor
): AbstractContextualTest() {

    @Test
    @ExpectedDatabase(
        "classpath:fixtures/service/meta-change-event/saved-successfully/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun savedSuccessfully() {
        metaInfoChangeSavingProcessor.processSavingChanges(listOf(
            "{\"externalId\":\"123\",\"oldMeta\":{},\"newMeta\":{\"field1\":\"f1\",\"field2\":\"f2\"}," +
                "\"updatedTime\":\"2021-01-01T10:00:00\",\"source\":\"TEST\"}"
        ))
    }


    @Test
    @DatabaseSetup("classpath:fixtures/service/meta-change-event/actual-external-id/before.xml")
    @ExpectedDatabase(
        "classpath:fixtures/service/meta-change-event/actual-external-id/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun getActualExternalIdAndSourceWhenBookingIdNotNull() {
        metaInfoChangeSavingProcessor.processSavingChanges(listOf(
            "{\"externalId\":\"123\",\"oldMeta\":{},\"newMeta\":{}," +
                "\"updatedTime\":\"2021-01-01T10:00:00\",\"source\":\"TEST\",\"bookingId\":1}"
        ))
    }


}
