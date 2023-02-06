package ru.yandex.market.logistics.cte.service

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.client.dto.ServiceCenterItemToSendFlatDTO
import ru.yandex.market.personal.client.api.DefaultPersonalApi
import ru.yandex.market.personal.client.model.CommonType
import ru.yandex.market.personal.client.model.CommonTypeEnum
import ru.yandex.market.personal.client.model.FullName
import ru.yandex.market.personal.client.model.MultiTypeRetrieveResponseItem
import ru.yandex.market.personal.client.model.PersonalMultiTypeRetrieveResponse
import java.time.LocalDateTime

internal class PersonalServiceTest(
@Autowired
private val personalService: PersonalService,
@Autowired private val personalApi: DefaultPersonalApi) : IntegrationTest() {

    @Test
    fun enrichmentEmptyTestShouldNotFail() {
        whenever(personalApi.v1MultiTypesRetrievePost(any())).thenReturn(personalResponse())
        personalService.enrichWithPersonalData(listOf())
    }

    @Test
    fun enrichmentTest() {
        whenever(personalApi.v1MultiTypesRetrievePost(any())).thenReturn(personalResponse())
        val dto = createDto()
        personalService.enrichWithPersonalData(listOf(dto))
        assertions.assertThat(dto.personalFullName).isEqualTo("full client name")
        assertions.assertThat(dto.personalPhone).isEqualTo("+7800800800")
    }

    private fun createDto() = ServiceCenterItemToSendFlatDTO(
        id = 1,
        marketShopSku = "1",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        personalFullNameId = "1",
        personalPhoneId = "2",
        updatedAt = LocalDateTime.of(2022,12,2,12,12,0)
    )

    private fun personalResponse(): PersonalMultiTypeRetrieveResponse {
        return PersonalMultiTypeRetrieveResponse().items(
            listOf(
                MultiTypeRetrieveResponseItem()
                    .type(CommonTypeEnum.FULL_NAME)
                    .id("personal-client-full-name-id")
                    .value(
                        CommonType().fullName(
                            FullName().surname("client").forename("full").patronymic("name")
                        )
                    ),
                MultiTypeRetrieveResponseItem()
                    .type(CommonTypeEnum.PHONE)
                    .id("personal-client-phone-id")
                    .value(CommonType().phone("+7800800800"))
            )
        )
    }
}
