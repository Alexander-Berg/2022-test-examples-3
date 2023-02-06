package ru.yandex.direct.core.entity.campaign.service.operation

import jdk.jfr.Description
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.DayBudgetShowMode
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.model.WalletTypedCampaign
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.rbac.RbacRole
import ru.yandex.direct.testing.matchers.hasError
import ru.yandex.direct.testing.matchers.hasNoErrors
import ru.yandex.direct.testing.matchers.hasNoErrorsOrWarnings
import ru.yandex.direct.testing.matchers.hasNoWarnings
import ru.yandex.direct.testing.matchers.hasWarning
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.NumberDefects
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import java.math.BigDecimal

@CoreTest
@RunWith(SpringRunner::class)
class UpdateCampaignDayBudgetTest {

    private val campaignDayBudget = BigDecimal.valueOf(400)

    private lateinit var campaign: TextCampaign
    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private var shard: Int = 0

    @Autowired
    protected lateinit var steps: Steps

    @Autowired
    protected lateinit var campaignOperationService: CampaignOperationService

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.CLIENT)
        clientId = clientInfo.clientId!!
        shard = clientInfo.shard
        steps.campaignSteps().createCampaign(
            TestCampaigns.activeWalletCampaign(
                clientId,
                clientInfo.uid
            )
        )

        campaign = TestCampaigns.defaultTextCampaign()
            .withDayBudget(campaignDayBudget)
        val addOperation = campaignOperationService.createRestrictedCampaignAddOperation(
            listOf(campaign),
            clientInfo.uid,
            UidAndClientId.of(clientInfo.uid, clientInfo.clientId!!),
            CampaignOptions(),
        )
        addOperation.prepareAndApply()
    }

    @Test
    @Description(
        "Проверяем, что валидация количества изменения dayBudget в день не срабатывает, если dayBudget не " +
            "меняется. Чтобы не было такого, что попробовали 4ый раз поменять dayBudget и после этого вообще не можем" +
            " редактировать кампанию."
    )
    fun updateCampaignAfter4DayBudgetChanges_success() {
        for (i in 1..4) {
            val modelChanges = ModelChanges(campaign.id, TextCampaign::class.java)
            modelChanges.process(campaignDayBudget.plus(BigDecimal(i)), TextCampaign.DAY_BUDGET)

            val result = createUpdateOperation(modelChanges).apply()

            if (i == 4) {
                softly {
                    assertThat(result.validationResult).hasError(
                        path(index(0), field(TextCampaign.DAY_BUDGET)),
                        CampaignDefects.tooManyDayBudgetDailyChanges()
                    )
                    assertThat(result.validationResult).hasNoWarnings()
                }
            } else {
                assertThat(result.validationResult).hasNoErrorsOrWarnings()
            }
        }

        val modelChanges = ModelChanges(campaign.id, TextCampaign::class.java)
        modelChanges.process(campaign.name + " updated", TextCampaign.NAME)

        val result = createUpdateOperation(modelChanges).apply()

        assertThat(result.validationResult).hasNoErrorsOrWarnings()
    }

    @Test
    @Description(
        "Проверяем, что предупреждение о dayBudgetShowMode не выдаётся, если dayBudgetShowMode не меняется. Чтобы " +
            "не было такого, что на любое изменение кампании приходит предупреждение на dayBudgetShowMode"
    )
    fun overridenDayBudgetShowModeNoDayBudgetShowModeChange_success() {
        val walletModelChanges = ModelChanges(campaign.walletId, WalletTypedCampaign::class.java)
        walletModelChanges.process(DayBudgetShowMode.DEFAULT_, WalletTypedCampaign.DAY_BUDGET_SHOW_MODE)
        walletModelChanges.process(campaignDayBudget, WalletTypedCampaign.DAY_BUDGET)
        createUpdateOperation(walletModelChanges).apply()

        var modelChanges = ModelChanges(campaign.id, TextCampaign::class.java)
        modelChanges.process(DayBudgetShowMode.STRETCHED, TextCampaign.DAY_BUDGET_SHOW_MODE)
        createUpdateOperation(modelChanges).apply()

        modelChanges = ModelChanges(campaign.id, TextCampaign::class.java)
        modelChanges.process(campaign.name + " updated", TextCampaign.NAME)

        val result = createUpdateOperation(modelChanges).apply()

        assertThat(result.validationResult).hasNoErrorsOrWarnings()
    }

    @Test
    @Description(
        "Проверяем, что предупреждение о dayBudget не выдаётся, если dayBudget не меняется. Чтобы " +
            "не было такого, что на любое изменение кампании приходит предупреждение на dayBudget"
    )
    fun overridenDayBudgetNoDayBudgetChange_success() {
        val walletModelChanges = ModelChanges(campaign.walletId, WalletTypedCampaign::class.java)
        walletModelChanges.process(DayBudgetShowMode.DEFAULT_, WalletTypedCampaign.DAY_BUDGET_SHOW_MODE)
        walletModelChanges.process(campaignDayBudget.minus(BigDecimal.ONE), WalletTypedCampaign.DAY_BUDGET)
        createUpdateOperation(walletModelChanges).apply()

        val modelChanges = ModelChanges(campaign.id, TextCampaign::class.java)
        modelChanges.process(campaign.name + " updated", TextCampaign.NAME)

        val result = createUpdateOperation(modelChanges).apply()

        assertThat(result.validationResult).hasNoErrorsOrWarnings()
    }

    @Test
    fun dayBudgetShowModeOverridenByWallet_warning() {
        val walletModelChanges = ModelChanges(campaign.walletId, WalletTypedCampaign::class.java)
        walletModelChanges.process(DayBudgetShowMode.DEFAULT_, WalletTypedCampaign.DAY_BUDGET_SHOW_MODE)
        walletModelChanges.process(campaignDayBudget, WalletTypedCampaign.DAY_BUDGET)
        createUpdateOperation(walletModelChanges).apply()

        val modelChanges = ModelChanges(campaign.id, TextCampaign::class.java)
        modelChanges.process(DayBudgetShowMode.STRETCHED, TextCampaign.DAY_BUDGET_SHOW_MODE)

        val result = createUpdateOperation(modelChanges).apply()

        softly {
            assertThat(result.validationResult).hasWarning(
                path(index(0), field(TextCampaign.DAY_BUDGET_SHOW_MODE)),
                CampaignDefects.dayBudgetShowModeOverridenByWallet()
            )
            assertThat(result.validationResult).hasNoErrors()
        }
    }

    @Test
    fun dayBudgetOverridenByWallet_warning() {
        val walletModelChanges = ModelChanges(campaign.walletId, WalletTypedCampaign::class.java)
        walletModelChanges.process(DayBudgetShowMode.DEFAULT_, WalletTypedCampaign.DAY_BUDGET_SHOW_MODE)
        walletModelChanges.process(campaignDayBudget, WalletTypedCampaign.DAY_BUDGET)
        createUpdateOperation(walletModelChanges).apply()

        val modelChanges = ModelChanges(campaign.id, TextCampaign::class.java)
        modelChanges.process(campaignDayBudget.plus(BigDecimal.ONE), TextCampaign.DAY_BUDGET)

        val result = createUpdateOperation(modelChanges).apply()

        softly {
            assertThat(result.validationResult).hasWarning(
                path(index(0), field(TextCampaign.DAY_BUDGET)),
                CampaignDefects.dayBudgetOverridenByWallet()
            )
            assertThat(result.validationResult).hasNoErrors()
        }
    }

    @Test
    fun dayBudgetMoreThanMax_error() {
        val modelChanges = ModelChanges(campaign.id, TextCampaign::class.java)
        val minDayBudget = CurrencyCode.RUB.currency.minDayBudget
        val maxDayBudget = CurrencyCode.RUB.currency.maxDailyBudgetAmount
        modelChanges.process(
            maxDayBudget.add(BigDecimal.ONE),
            TextCampaign.DAY_BUDGET
        )

        val result = createUpdateOperation(modelChanges).apply()

        softly {
            assertThat(result.validationResult).hasError(
                path(index(0), field(TextCampaign.DAY_BUDGET)),
                NumberDefects.inInterval(minDayBudget, maxDayBudget)
            )
            assertThat(result.validationResult).hasNoWarnings()
        }
    }

    @Test
    fun dayBudgetShowModeNull_error() {
        val modelChanges = ModelChanges(campaign.id, TextCampaign::class.java)
        modelChanges.process(
            null,
            TextCampaign.DAY_BUDGET_SHOW_MODE
        )

        val result = createUpdateOperation(modelChanges).apply()

        softly {
            assertThat(result.validationResult).hasError(
                path(index(0), field(TextCampaign.DAY_BUDGET_SHOW_MODE)),
                CommonDefects.notNull()
            )
            assertThat(result.validationResult).hasNoWarnings()
        }
    }

    @Test
    fun dayBudgetNull_error() {
        val modelChanges = ModelChanges(campaign.id, TextCampaign::class.java)
        modelChanges.process(
            null,
            TextCampaign.DAY_BUDGET
        )

        val result = createUpdateOperation(modelChanges).apply()

        softly {
            assertThat(result.validationResult).hasError(
                path(index(0), field(TextCampaign.DAY_BUDGET)),
                CommonDefects.notNull()
            )
            assertThat(result.validationResult).hasNoWarnings()
        }
    }

    private fun createUpdateOperation(modelChanges: ModelChanges<out BaseCampaign>) =
        campaignOperationService.createRestrictedCampaignUpdateOperation(
            listOf(modelChanges),
            clientInfo.uid,
            UidAndClientId.of(clientInfo.uid, clientInfo.clientId!!),
            CampaignOptions(),
        )
}