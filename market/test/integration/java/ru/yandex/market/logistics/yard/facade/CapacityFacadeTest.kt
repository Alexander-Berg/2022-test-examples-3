package ru.yandex.market.logistics.yard.facade

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient
import ru.yandex.market.logistic.pechkin.core.dto.MessageDto
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.facade.CapacityFacadeInterface

class CapacityFacadeTest(
    @Autowired private val capacityFacade: CapacityFacadeInterface,
    @Autowired val pechkinHttpClient: PechkinHttpClient
) :
    AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/facade/capacity/check-loading/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/facade/capacity/check-loading/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testCheckLoading() {

        capacityFacade.checkLoading()
        val argument: ArgumentCaptor<MessageDto> = ArgumentCaptor.forClass(MessageDto::class.java)
        Mockito.verify(pechkinHttpClient).sendMessage(argument.capture())

        val expected = MessageDto().apply {
            sender = "FF_YARD"
            channel = "test_telegram_channel"
            message = "Объект: service FIRST\n" +
                "Причина: Превышение талонов в очереди\n" +
                "Удельная нагрузка по нормативу на окно: 4\n" +
                "Талонов в очереди: 5\n" +
                "Залогиненных окон: 0\n" +
                "Факт: 5"
        }
        assertions().assertThat(argument.value.channel).isEqualTo(expected.channel)
        assertions().assertThat(argument.value.sender).isEqualTo(expected.sender)
        assertions().assertThat(argument.value.message).isEqualTo(expected.message)
    }

}
