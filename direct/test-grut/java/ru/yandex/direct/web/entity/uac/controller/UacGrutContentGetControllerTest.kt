package ru.yandex.direct.web.entity.uac.controller

import java.time.LocalDateTime
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.uac.converter.UacGrutAssetsConverter.toAssetGrut
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbContent
import ru.yandex.direct.core.entity.uac.service.GrutUacContentService
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.web.configuration.GrutDirectWebTest

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacGrutContentGetControllerTest : UacContentGetControllerTestBase() {

    @Autowired
    override lateinit var uacContentService: GrutUacContentService

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var steps: Steps

    @Before
    fun grutBefore() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UC_UAC_CREATE_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)
        grutApiService.clientGrutDao.createOrUpdateClient(ClientGrutModel(
            client = Client().withId(userInfo.clientId.asLong()).withCreateDate(LocalDateTime.now()),
            ndsHistory = listOf()))
    }


    override fun saveContent(content: UacYdbContent) {
        grutApiService.assetGrutApi.createObject(toAssetGrut(content))
    }
}
