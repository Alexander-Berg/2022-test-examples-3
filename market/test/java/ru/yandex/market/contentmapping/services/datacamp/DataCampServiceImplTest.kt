package ru.yandex.market.contentmapping.services.datacamp

import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass

internal class DataCampServiceImplTest : BaseAppTestClass() {
    @Autowired
    private lateinit var dataCampService: DataCampService

    @Test
    fun dummy() {

    }

    //Просто для локальных проверок. Не будет работать в тестовом окружении аркадии
    //После отладки можно удалить / модифицировать
    /*
    @Test
    fun getOffers() {
        val res = dataCampService.getOffers(1)
        assertThat(res!!.size).isEqualTo(1)
        assertThat(res[0].basic.identifiers.businessId).isEqualTo(1)
    }

    @Test
    fun getUnitedOffer() {
       val res = dataCampService.getOffer(1, "10855364-1")
        assertThat(res!!.basic.identifiers.businessId).isEqualTo(1)
        assertThat(res.basic.identifiers.offerId).isEqualTo("10855364-1")
    }

    @Test
    fun modifyUnitedOffer() {
       val res = dataCampService.getOffer(1, "10855364-1")
        assertThat(res!!.basic.identifiers.businessId).isEqualTo(1)
        assertThat(res.basic.identifiers.offerId).isEqualTo("10855364-1")

        dataCampService.modifyUnitedOffer(res.basic)
    }

    @Test
    fun batchModifyUnitedOffers() {
       val res = dataCampService.getOffer(1, "10855364-1")
        assertThat(res!!.basic.identifiers.businessId).isEqualTo(1)
        assertThat(res.basic.identifiers.offerId).isEqualTo("10855364-1")

        dataCampService.batchModifyUnitedOffers(listOf(res))
    }*/
}
