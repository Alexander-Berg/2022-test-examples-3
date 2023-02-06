package ru.yandex.direct.web.entity.uac.repository

import junitparams.JUnitParamsRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.uac.createDirectCampaign
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbDirectCampaignRepository
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacYdbDirectCampaignRepositoryTest : AbstractUacRepositoryTest() {

    @Autowired
    lateinit var directCampaignRepository: UacYdbDirectCampaignRepository

    @Test
    fun getDirectCampaignByIdTest() {
        val directCampaign = createDirectCampaign()
        directCampaignRepository.saveDirectCampaign(directCampaign)
        val gotCampaign = directCampaignRepository.getDirectCampaignById(directCampaign.id)
        assertThat(gotCampaign).isEqualTo(directCampaign)
    }

    @Test
    fun deleteDirectCampaignTest() {
        val directCampaign = createDirectCampaign()
        directCampaignRepository.saveDirectCampaign(directCampaign)
        directCampaignRepository.delete(directCampaign.id)
        val deletedCampaign = directCampaignRepository.getDirectCampaignById(directCampaign.id)
        assertThat(deletedCampaign).isNull()
    }

}
