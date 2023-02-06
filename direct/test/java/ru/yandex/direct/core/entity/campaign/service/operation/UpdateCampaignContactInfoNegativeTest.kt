package ru.yandex.direct.core.entity.campaign.service.operation

import jdk.jfr.Description
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.core.entity.campaign.converter.CampaignConverter
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions
import ru.yandex.direct.core.entity.vcard.model.Vcard
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.TestVcards
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbschema.ppc.Tables
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.result.MassResult
import ru.yandex.direct.testing.matchers.hasError
import ru.yandex.direct.validation.defect.ids.StringDefectIds
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@CoreTest
@RunWith(SpringRunner::class)
class UpdateCampaignContactInfoNegativeTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var campaignOperationService: CampaignOperationService

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private var shard: Int = 0

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT)
        clientId = clientInfo.clientId!!
        shard = clientInfo.shard
    }

    @Test
    fun `contactInfo is validated`() {
        val campaign = TestCampaigns.defaultTextCampaign()
            .withContactInfo(TestVcards.fullVcard())

        val addOperation = campaignOperationService.createRestrictedCampaignAddOperation(
            listOf(campaign),
            clientInfo.uid,
            UidAndClientId.of(clientInfo.uid, clientInfo.clientId!!),
            CampaignOptions.Builder().build()
        )

        addOperation.prepareAndApply()

        val modelChanges = ModelChanges(campaign.id, TextCampaign::class.java)
        modelChanges.process(
            TestVcards.fullVcard()
                .withContactPerson(
                    "Too Long Contact Person Too Long Contact " +
                        "Person Too Long Contact Person Too Long Contact Person Too Long Contact Person Too Long" +
                        " Contact Person Too Long Contact Person Too Long Contact Person Too Long Contact Person " +
                        "Too Long Contact Person Too Long Contact Person Too Long Contact Person Too Long " +
                        "Contact Person Too Long Contact Person"
                ), TextCampaign.CONTACT_INFO
        )
        val result = createUpdateOperation(modelChanges, campaign.uid, clientId)
            .apply()

        assertThat(result.validationResult).hasError(
            path(index(0), field(TextCampaign.CONTACT_INFO), field(Vcard.CONTACT_PERSON)),
            StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX
        )
    }

    @Test
    @Description(
        "Проверим, что если передать ContactInfo ПОЛНОСТЬЮ совпадающий с тем, что сейчас в базе (включая lastChange" +
            " и всё всё всё), то всё равно будет ошибка валидации (проверяем, что внутри используется именно " +
            "AppliedChanges#passed, смотри так же DIRECT-173932)."
    )
    fun `errors in contactInfo are not ignored if contactInfo same to contactInfo in db is passed`() {
        val campaign = TestCampaigns.defaultTextCampaign()
            .withContactInfo(TestVcards.fullVcard())

        val addOperation = campaignOperationService.createRestrictedCampaignAddOperation(
            listOf(campaign),
            clientInfo.uid,
            UidAndClientId.of(clientInfo.uid, clientInfo.clientId!!),
            CampaignOptions.Builder().build()
        )

        val addResult = addOperation.prepareAndApply()

        // it's important to use contactInfo from database (not just TestVcards.fullVcard()) cause database contactInfo
        // is filled with artifacts (like uid)
        val campaignFromResult = getCampaignFromResult(addResult, shard)
        val wrongContactInfo = campaignFromResult.contactInfo.withContactPerson(
            "Too Long Contact Person Too Long Contact " +
                "Person Too Long Contact Person Too Long Contact Person Too Long Contact Person Too Long Contact " +
                "Person Too Long Contact Person Too Long Contact Person Too Long Contact Person Too Long Contact " +
                "Person Too Long Contact Person Too Long Contact Person Too Long Contact Person Too Long Contact " +
                "Person"
        )

        dslContextProvider.ppc(shard)
            .update(Tables.CAMP_OPTIONS)
            .set(Tables.CAMP_OPTIONS.CONTACTINFO, CampaignConverter.vcardToDb(wrongContactInfo))
            .where(Tables.CAMP_OPTIONS.CID.eq(campaign.id))
            .execute()

        val modelChanges = ModelChanges(campaign.id, TextCampaign::class.java)
        modelChanges.process(wrongContactInfo, TextCampaign.CONTACT_INFO)
        val updateResult = createUpdateOperation(modelChanges, campaign.uid, clientId)
            .apply()

        assertThat(updateResult.validationResult).hasError(
            path(index(0), field(TextCampaign.CONTACT_INFO), field(Vcard.CONTACT_PERSON)),
            StringDefectIds.LENGTH_CANNOT_BE_MORE_THAN_MAX
        )
    }

    private fun createUpdateOperation(
        modelChanges: ModelChanges<out BaseCampaign>,
        uid: Long,
        clientId: ClientId
    ) =
        campaignOperationService.createRestrictedCampaignUpdateOperation(
            listOf(modelChanges),
            uid,
            UidAndClientId.of(uid, clientId),
            CampaignOptions(),
        )

    private fun getCampaignFromResult(
        result: MassResult<Long>,
        shard: Int
    ): TextCampaign =
        campaignTypedRepository.getTypedCampaigns(shard, listOf(result[0].result)).get(0) as TextCampaign
}


