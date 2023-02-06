package ru.yandex.market.logistics.mqm.tms

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.DropshipDropoffTransportationInfoService

@DisplayName("Тест очистки информации о перемещениях")
class ClearDropshipDropoffTransportationInfoExecutorTest: AbstractContextualTest() {

    @Autowired
    private lateinit var dropshipDropoffTransportationInfoService: DropshipDropoffTransportationInfoService

    private lateinit var executor: ClearDropshipDropoffTransportationInfoExecutor

    @BeforeEach
    fun setup() {
        executor = ClearDropshipDropoffTransportationInfoExecutor(dropshipDropoffTransportationInfoService)
    }

    @Test
    @DatabaseSetup("/tms/processDropshipDropoffTransportationYtInfoExecutor/after/success.xml")
    @ExpectedDatabase(
        value = "/tms/processClearDropshipDropoffTransportationInfoExecutor/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешная очистка таблицы")
    fun successTest() {
        executor.run()
    }

}
