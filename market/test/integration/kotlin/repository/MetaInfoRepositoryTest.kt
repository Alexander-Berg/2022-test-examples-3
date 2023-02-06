package ru.yandex.market.logistics.calendaring.repository

import com.github.springtestdbunit.annotation.DatabaseSetup
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.calendaring.base.AbstractContextualTest
import ru.yandex.market.logistics.calendaring.client.dto.enums.RequestStatus

class MetaInfoRepositoryTest(@Autowired private val metaInfoRepository: MetaInfoRepository) : AbstractContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/repository/meta-info/before.xml"])
    fun findByBookingIdSuccessfully() {
        val metaInfoEntity = metaInfoRepository.findByBookingId(1L)!!
        softly.assertThat(metaInfoEntity).isNotNull
        softly.assertThat(metaInfoEntity.booking!!.id).isEqualTo(1L)
        softly.assertThat(metaInfoEntity.meta!!["t1"]).isEqualTo(1)
        softly.assertThat((metaInfoEntity.meta!!["nested"] as Map<*, *>)["t2"]).isEqualTo(2)
        softly.assertThat(metaInfoEntity.meta!!["str1"]).isEqualTo("hello world")
        softly.assertThat(metaInfoEntity.status!!).isEqualTo(RequestStatus.SENT_TO_SERVICE)
    }

}
