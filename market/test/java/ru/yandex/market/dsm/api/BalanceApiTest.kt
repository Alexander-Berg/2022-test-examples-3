package ru.yandex.market.dsm.api

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import ru.yandex.market.dsm.config.DsmConstants
import ru.yandex.market.dsm.core.ddd.DsmCommandService
import ru.yandex.market.dsm.core.test.AbstractDsmApiTest
import ru.yandex.market.dsm.dbqueue.DbQueueTestUtil
import ru.yandex.market.dsm.dbqueue.model.DsmDbQueue
import ru.yandex.market.dsm.domain.courier.balance.db.CourierBalanceDataDboRepository
import ru.yandex.market.dsm.domain.courier.balance.db.CourierBalancePaymentReceiver
import ru.yandex.market.dsm.domain.courier.balance.db.CourierBalanceRegistrationStatus
import ru.yandex.market.dsm.domain.courier.balance.model.CourierBalanceData
import ru.yandex.market.dsm.domain.courier.balance.service.CourierBalanceQueryService
import ru.yandex.market.dsm.domain.courier.test.CourierBalanceDataTestFactory
import ru.yandex.market.dsm.domain.courier.test.CourierTestFactory
import ru.yandex.market.tpl.common.util.TplObjectMappers
import ru.yandex.mj.generated.server.model.CourierBalanceDataDto
import ru.yandex.mj.generated.server.model.CourierBalanceDataUpsertDto

internal class BalanceApiTest : AbstractDsmApiTest() {
    @Autowired
    private lateinit var courierBalanceDataTestFactory: CourierBalanceDataTestFactory

    @Autowired
    private lateinit var courierBalanceQueryService: CourierBalanceQueryService

    @Autowired
    private lateinit var dbQueueTestUtil: DbQueueTestUtil

    @Autowired
    private lateinit var courierTestFactory: CourierTestFactory

    @Autowired
    private lateinit var courierBalanceDataDboRepository: CourierBalanceDataDboRepository

    @Autowired
    private lateinit var dsmCommandService: DsmCommandService

    @Test
    fun manualSelfemployedUpdateBalanceDataPut() {
        val courier = courierTestFactory.generateCreateCommand()
        dsmCommandService.handle(courier)

        val createDto = courierBalanceDataTestFactory.upsertDto()
        createDto.courierId = courier.id

        val response = mockMvc.perform(
            MockMvcRequestBuilders.put("/manual/selfemployed/update-balance-data")
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(TplObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(createDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        assertThat(response.response.contentAsString).isNotBlank
        val result = TplObjectMappers.TPL_API_OBJECT_MAPPER.readValue(
            response.response.contentAsString,
            CourierBalanceDataDto::class.java
        )

        val courierBalance = courierBalanceQueryService.getById(result.id)
        assertCourierBalanceUpsert(courierBalance, createDto)

        val updateDto = courierBalanceDataTestFactory.upsertDto()
        updateDto.courierId = courier.id

        val response2 = mockMvc.perform(
            MockMvcRequestBuilders.put("/manual/selfemployed/update-balance-data")
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(TplObjectMappers.TPL_API_OBJECT_MAPPER.writeValueAsString(updateDto))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        assertCourierBalanceUpsert(courierBalanceQueryService.getById(result.id), updateDto)
    }

    @Test
    fun manulPutInBalanceRegistration() {
        val courier = courierTestFactory.generateCreateCommand()
        courier.passportNumber = "7804789345"
        courier.phone = "659365395398"
        dsmCommandService.handle(courier)

        val candidateCommand = courierBalanceDataTestFactory.createCommand()
        candidateCommand.balanceRegistrationStatus = null
        candidateCommand.paymentReceiver = CourierBalancePaymentReceiver.BANK
        candidateCommand.courierId = courier.id
        val candidateId = dsmCommandService.handle(candidateCommand)

        val response = mockMvc.perform(
            MockMvcRequestBuilders.put("/manual/selfemployed/${courier.id}/courier-registration-in-balance")
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        val updateCandidate = courierBalanceDataDboRepository.findById(candidateId).get()
        assertThat(updateCandidate.balanceRegistrationStatus).isEqualTo(CourierBalanceRegistrationStatus.CREATE_CLIENT)
        dbQueueTestUtil.assertQueueHasSize(DsmDbQueue.SELF_EMPLOYED_REGISTRATION_IN_BALANCE, 1)

        //Регистрация завершена, получаем ошибку
        updateCandidate.balanceRegistrationStatus = CourierBalanceRegistrationStatus.REGISTERED
        courierBalanceDataDboRepository.save(updateCandidate)
        mockMvc.perform(
            MockMvcRequestBuilders.put("/manual/selfemployed/${courier.id}/courier-registration-in-balance")
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()

        //Необходимое поле null, получаем ошибку
        val courier2 = courierTestFactory.generateCreateCommand()
        courier2.passportNumber = "7804789345"
        courier2.phone = null
        dsmCommandService.handle(courier2)

        val candidateCommand2 = courierBalanceDataTestFactory.createCommand()
        candidateCommand2.balanceRegistrationStatus = null
        candidateCommand2.paymentReceiver = CourierBalancePaymentReceiver.BANK
        candidateCommand2.courierId = courier2.id
        val candidateId2 = dsmCommandService.handle(candidateCommand2)

        mockMvc.perform(
            MockMvcRequestBuilders.put("/manual/selfemployed/${courier2.id}/courier-registration-in-balance")
                .header(DsmConstants.TVM.TVM_HEADER, "TVM_TICKET")
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()

        dbQueueTestUtil.clear(DsmDbQueue.SELF_EMPLOYED_REGISTRATION_IN_BALANCE)
    }

    private fun assertCourierBalanceUpsert(courierBalance: CourierBalanceData, upsertDto: CourierBalanceDataUpsertDto) {
        assertThat(courierBalance.pfr).isEqualTo(upsertDto.pfr)
        assertThat(courierBalance.inn).isEqualTo(upsertDto.inn)
        assertThat(courierBalance.personBankAccount).isEqualTo(upsertDto.personBankAccount)

        assertThat(courierBalance.legalAddressPostCode).isEqualTo(upsertDto.legalAddressPostCode)
        assertThat(courierBalance.legalAddressGni).isEqualTo(upsertDto.legalAddressGni)
        assertThat(courierBalance.legalAddressRegion).isEqualTo(upsertDto.legalAddressRegion)
        assertThat(courierBalance.legalAddressCity).isEqualTo(upsertDto.legalAddressCity)
        assertThat(courierBalance.legalAddressStreet).isEqualTo(upsertDto.legalAddressStreet)
        assertThat(courierBalance.legalAddressHome).isEqualTo(upsertDto.legalAddressHome)
        assertThat(courierBalance.legalFiasGuid).isEqualTo(upsertDto.legalFiasGuid)

        assertThat(courierBalance.addressGni).isEqualTo(upsertDto.addressGni)
        assertThat(courierBalance.addressRegion).isEqualTo(upsertDto.addressRegion)
        assertThat(courierBalance.postCode).isEqualTo(upsertDto.postCode)
        assertThat(courierBalance.addressCode).isEqualTo(upsertDto.addressCode)

        assertThat(courierBalance.bankBik).isEqualTo(upsertDto.bankBik)

        assertThat(courierBalance.courierId).isEqualTo(upsertDto.courierId)
    }
}

