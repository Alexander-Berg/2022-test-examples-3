package ru.yandex.direct.web.entity.uac.controller

import java.time.LocalDateTime
import org.junit.Before
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.entity.uac.converter.UacGrutAssetsConverter.toAssetGrut
import ru.yandex.direct.core.entity.uac.createDefaultImageContent
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.grut.api.ClientGrutModel
import ru.yandex.direct.core.grut.replication.GrutApiService
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.web.configuration.GrutDirectWebTest

@GrutDirectWebTest
@RunWith(SpringJUnit4ClassRunner::class)
class UacGrutContentDeleteControllerTest : UacContentDeleteControllerTestBase() {

    @Autowired
    private lateinit var grutApiService: GrutApiService

    @Autowired
    private lateinit var steps: Steps

    @Before
    fun grutBefore() {
        steps.featureSteps().addClientFeature(userInfo.clientId, FeatureName.UC_UAC_CREATE_BRIEF_IN_GRUT_INSTEAD_OF_YDB, true)
    }

    override fun saveContent(userInfo: UserInfo): String {
        val content = createDefaultImageContent(accountId = userInfo.clientId.toString())

        grutApiService.clientGrutDao.createOrUpdateClient(ClientGrutModel(
            client = Client().withId(userInfo.clientId.asLong()).withCreateDate(LocalDateTime.now()),
            ndsHistory = listOf()))

        return grutApiService.assetGrutApi.createObject(toAssetGrut(content)).toIdString()
    }

    override fun contentExists(id: String): Boolean {
        return grutApiService.assetGrutApi.getExistingObjects(listOf(id.toIdLong())).toSet().contains(id.toIdLong())
    }
}
