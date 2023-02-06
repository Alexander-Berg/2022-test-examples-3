package ru.yandex.market.logistics.cte.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.cte.base.IntegrationTest

class ServiceCenterResultServiceTest(
    @Autowired
    private val serviceCenterResultService: ServiceCenterResultService
) : IntegrationTest() {


    @Test
    @DatabaseSetup("classpath:service/service-center-result/before.xml")
    fun fundAllSuccessfully(){
        val result = serviceCenterResultService.findAll()
        assertions.assertThat(result.size).isEqualTo(2)
        assertions.assertThat(result[0].id).isEqualTo(1)
        assertions.assertThat(result[0].value).isEqualTo("test1")

    }

}
