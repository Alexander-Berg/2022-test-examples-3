package ru.yandex.market.logistics.calendaring.security

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.apache.http.entity.ContentType
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ru.yandex.market.ff.client.dto.quota.QuotaDTO
import ru.yandex.market.ff.client.dto.quota.QuotaInfoDTO
import ru.yandex.market.ff.client.dto.quota.QuotaInfosDTO
import ru.yandex.market.ff.client.enums.DailyLimitsType
import ru.yandex.market.logistics.calendaring.base.IdmContextualTest
import ru.yandex.market.logistics.calendaring.config.idm.IdmRoleSecurityConfigurationAdapter
import java.time.LocalDate


class IdmAuthenticationTest: IdmContextualTest() {

    @Test
    @DatabaseSetup(
        "classpath:fixtures/security/idm-roles.xml",
        "classpath:fixtures/security/user-role.xml"
    )
    fun testIdmSuccess() {
        Mockito.`when`(ffwfClientApi!!.getQuotaInfo(
            123,
            LocalDate.of(2021, 5, 18),
            LocalDate.of(2021, 5, 19),
            listOf(DailyLimitsType.SUPPLY, DailyLimitsType.MOVEMENT_SUPPLY, DailyLimitsType.XDOCK_TRANSPORT_SUPPLY)
        )).thenReturn(QuotaInfosDTO.builder()
                .quotaInfos(listOf(
                        getQuotaInfoDTO(DailyLimitsType.SUPPLY, LocalDate.of(2021, 5, 18), null, null, 10, 20, 1, 2, 0, 0),
                        getQuotaInfoDTO(DailyLimitsType.MOVEMENT_SUPPLY, LocalDate.of(2021, 5, 18), 5, null, null, null, 5, 2, 1, 1),
                        getQuotaInfoDTO(DailyLimitsType.XDOCK_TRANSPORT_SUPPLY, LocalDate.of(2021, 5, 18), 2, null, 1, null, 0, 0, 0, 0),
                        getQuotaInfoDTO(DailyLimitsType.SUPPLY, LocalDate.of(2021, 5, 19), null, null, null, null, 0, 0, 0, 0),
                        getQuotaInfoDTO(DailyLimitsType.MOVEMENT_SUPPLY, LocalDate.of(2021, 5, 19), 5, 6, 7, 8, 9, 10, 11, 12),
                        getQuotaInfoDTO(DailyLimitsType.XDOCK_TRANSPORT_SUPPLY, LocalDate.of(2021, 5, 19), 2, null, 1, null, 0, 0, 0, 0)
                ))
            .build()
        )

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/INBOUND/123/quota/2021-05-18/2021-05-19")
                .header(IdmRoleSecurityConfigurationAdapter.USER_LOGIN_HEADER, "TEST_LOGIN_1")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isOk)
            .andReturn()
    }

    @Test
    fun testIdmBadRequestWithoutLogin() {

        Mockito.`when`(ffwfClientApi!!.getQuotaInfo(
            123,
            LocalDate.of(2021, 5, 18),
            LocalDate.of(2021, 5, 19),
            listOf(DailyLimitsType.SUPPLY, DailyLimitsType.MOVEMENT_SUPPLY)
        )).thenReturn(QuotaInfosDTO.builder()
            .quotaInfos(listOf(
                getQuotaInfoDTO(DailyLimitsType.SUPPLY, LocalDate.of(2021, 5, 18), null, null, 10, 20, 1, 2, 0, 0),
                getQuotaInfoDTO(DailyLimitsType.MOVEMENT_SUPPLY, LocalDate.of(2021, 5, 18), 5, null, null, null, 5, 2, 1, 1),
                getQuotaInfoDTO(DailyLimitsType.SUPPLY, LocalDate.of(2021, 5, 19), null, null, null, null, 0, 0, 0, 0),
                getQuotaInfoDTO(DailyLimitsType.MOVEMENT_SUPPLY, LocalDate.of(2021, 5, 19), 5, 6, 7, 8, 9, 10, 11, 12)
            ))
            .build()
        )

        mockMvc!!.perform(
            MockMvcRequestBuilders.get("/calendaring/INBOUND/123/quota/2021-05-18/2021-05-19")
                .contentType(ContentType.APPLICATION_JSON.mimeType)
        )
            .andExpect(status().isBadRequest)
            .andReturn()
    }

    private fun getQuotaInfoDTO(type: DailyLimitsType,
                                date: LocalDate,
                                firstPartyItemsQuota: Long?,
                                thirdPartyItemsQuota: Long?,
                                firstPartyPalletsQuota: Long?,
                                thirdPartyPalletsQuota: Long?,
                                firstPartyUnavailableItemsCount: Long,
                                thirdPartyUnavailableItemsCount: Long,
                                firstPartyUnavailablePalletsCount: Long,
                                thirdPartyUnavailablePalletsCount: Long
    ): QuotaInfoDTO {
        return QuotaInfoDTO.builder()
            .type(type)
            .date(date)
            .firstPartyQuotas(getQuotaDTO(firstPartyItemsQuota, firstPartyPalletsQuota,
                firstPartyUnavailableItemsCount, firstPartyUnavailablePalletsCount))
            .thirdPartyQuotas(getQuotaDTO(thirdPartyItemsQuota, thirdPartyPalletsQuota,
                thirdPartyUnavailableItemsCount, thirdPartyUnavailablePalletsCount))
            .build()
    }

    private fun getQuotaDTO(
        itemsQuota: Long?,
        palletsQuota: Long?,
        unavailableItemsCount: Long,
        unavailablePalletsCount: Long
    ): QuotaDTO {
        return QuotaDTO.builder()
            .itemsQuota(itemsQuota)
            .palletsQuota(palletsQuota)
            .unavailableItemsCount(unavailableItemsCount)
            .unavailablePalletsCount(unavailablePalletsCount)
            .build()
    }


}
