package ru.yandex.market.logistics.calendaring.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.client.dto.enums.RequestStatus
import ru.yandex.market.logistics.calendaring.model.dto.MetaInfoDTO

class MetaServiceTest(@Autowired private val metaService: MetaService) : AbstractContextualTest() {


    @Test
    @DatabaseSetup("classpath:fixtures/service/meta/saved-successfully/before.xml")
    @ExpectedDatabase(
        "classpath:fixtures/service/meta/saved-successfully/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun savedSuccessfully() {

        val metaInfoDTO = metaService.save(
            MetaInfoDTO(
                id = null,
                meta = null,
                status = RequestStatus.CREATED,
                bookingId = 1L,
                null,
                0,
                null
            )
        )
        assertions().assertThat(metaInfoDTO.bookingId).isEqualTo(1L)
        assertions().assertThat(metaInfoDTO.status).isEqualTo(RequestStatus.CREATED)

    }


}
