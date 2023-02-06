package ru.yandex.market.abo.core.personal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.abo.generated.client.personal_market.model.BulkRetrieveRequestItem
import ru.yandex.market.abo.generated.client.personal_market.model.BulkRetrieveResponseItem
import ru.yandex.market.abo.generated.client.personal_market.model.DataType.PHONES
import ru.yandex.market.abo.generated.client.personal_market.model.PersonalBulkRetrieveRequest
import ru.yandex.market.abo.generated.client.personal_market.model.PersonalBulkRetrieveResponse
import ru.yandex.market.abo.generated.client.personal_market.model.PersonalRetrieveRequest
import ru.yandex.market.abo.generated.client.personal_market.model.PersonalRetrieveResponse
import ru.yandex.market.abo.util.mockRetrofitCall
import ru.yandex.market.abo.generated.client.personal_market.api.bulk_retrieve.DefaultApi as PersonalBulkRetrieveApi
import ru.yandex.market.abo.generated.client.personal_market.api.multi_types_retrieve.DefaultApi as PersonalMultiTypesRetrieveApi
import ru.yandex.market.abo.generated.client.personal_market.api.retrieve.DefaultApi as PersonalRetrieveApi

/**
 * @author zilzilok
 */
class PersonalMarketServiceTest {

    private val personalBulkRetrieveApi: PersonalBulkRetrieveApi = mock()
    private val personalRetrieveApi: PersonalRetrieveApi = mock()
    private val personamMultiTypesRetrieveApi: PersonalMultiTypesRetrieveApi = mock()

    private val personalMarketService = PersonalMarketService(
        personalBulkRetrieveApi, personalRetrieveApi, personamMultiTypesRetrieveApi
    )

    @Test
    fun getPersonalPhonesByIds() {
        val response = mockRetrofitCall(
            PersonalBulkRetrieveResponse().items(
                listOf(
                    BulkRetrieveResponseItem().id(PERSONAL_PHONE_ID_1).value(PERSONAL_PHONE_1),
                    BulkRetrieveResponseItem().id(PERSONAL_PHONE_ID_2).value(PERSONAL_PHONE_2)
                )
            )
        )
        whenever(
            personalBulkRetrieveApi.v1DataTypeBulkRetrievePost(
                PHONES,
                PersonalBulkRetrieveRequest().items(
                    listOf(
                        BulkRetrieveRequestItem().id(PERSONAL_PHONE_ID_1),
                        BulkRetrieveRequestItem().id(PERSONAL_PHONE_ID_2)
                    )
                )
            )
        ).thenReturn(response)

        assertEquals(
            mapOf(
                PERSONAL_PHONE_ID_1 to PERSONAL_PHONE_1,
                PERSONAL_PHONE_ID_2 to PERSONAL_PHONE_2
            ),
            personalMarketService.getPersonalPhonesByIds(listOf(PERSONAL_PHONE_ID_1, PERSONAL_PHONE_ID_2))
        )
    }

    @Test
    fun getPersonalPhoneById() {
        val response = mockRetrofitCall(PersonalRetrieveResponse().id(PERSONAL_PHONE_ID_1).value(PERSONAL_PHONE_1))
        whenever(personalRetrieveApi.v1DataTypeRetrievePost(PHONES, PersonalRetrieveRequest().id(PERSONAL_PHONE_ID_1)))
            .thenReturn(response)

        assertEquals(PERSONAL_PHONE_1, personalMarketService.getPersonalPhoneById(PERSONAL_PHONE_ID_1))
    }

    companion object {
        private const val PERSONAL_PHONE_ID_1 = "c0dec0dedec0dec0dec0dec0dedec0de"
        private const val PERSONAL_PHONE_ID_2 = "0123456789abcdef0123456789abcdef"
        private const val PERSONAL_PHONE_1 = "+71234567891"
        private const val PERSONAL_PHONE_2 = "+70000000000"
    }
}
