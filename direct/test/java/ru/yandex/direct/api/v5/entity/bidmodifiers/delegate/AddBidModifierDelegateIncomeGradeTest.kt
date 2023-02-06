package ru.yandex.direct.api.v5.entity.bidmodifiers.delegate

import com.nhaarman.mockitokotlin2.whenever
import com.yandex.direct.api.v5.bidmodifiers.BidModifierAddItem
import com.yandex.direct.api.v5.bidmodifiers.IncomeGradeAdjustmentAdd
import com.yandex.direct.api.v5.general.IncomeGradeEnum
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.api.v5.converter.ResultConverter
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPrismaIncomeGrade
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPrismaIncomeGradeAdjustment
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionLiteral
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService
import ru.yandex.direct.core.entity.feature.service.FeatureService
import ru.yandex.direct.core.entity.user.model.ApiUser
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.test.utils.checkBeanDiffer
import ru.yandex.direct.test.utils.checkEquals

@Api5Test
@RunWith(SpringRunner::class)
class AddBidModifierDelegateIncomeGradeTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var bidModifierService: BidModifierService

    @Autowired
    private lateinit var resultConverter: ResultConverter

    @Autowired
    private lateinit var adGroupService: AdGroupService

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var featureService: FeatureService

    @Mock
    private lateinit var auth: ApiAuthenticationSource

    private lateinit var delegate: AddBidModifiersDelegate

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        MockitoAnnotations.openMocks(this)
        clientInfo = steps.clientSteps().createDefaultClient()
        whenever(auth.subclient).thenReturn(ApiUser().withClientId(clientInfo.clientId))
        whenever(auth.chiefSubclient).thenReturn(ApiUser().withClientId(clientInfo.clientId))
        whenever(auth.operator).thenReturn(ApiUser().withUid(clientInfo.uid))
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.INCOME_GRADE_BID_MODIFIER_ALLOWED, true)
        delegate = AddBidModifiersDelegate(
            bidModifierService,
            resultConverter,
            auth,
            adGroupService,
            ppcPropertiesSupport,
            featureService
        )
    }

    @Test
    fun addBidModifierIncomeGrade_success() {
        val campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo)
        val incomeGradeAdjustments =
            listOf(
                IncomeGradeAdjustmentAdd()
                    .withBidModifier(100)
                    .withGrade(IncomeGradeEnum.HIGH),
                IncomeGradeAdjustmentAdd()
                    .withBidModifier(120)
                    .withGrade(IncomeGradeEnum.VERY_HIGH)
            )

        val bidModifierAddItem = BidModifierAddItem()
            .withAdGroupId(adGroupInfo.adGroupId)
            .withIncomeGradeAdjustments(incomeGradeAdjustments)
        val result = delegate.processList(listOf(bidModifierAddItem))

        result.errorCount.checkEquals(0)

        val modifiers = bidModifierService.getByAdGroupIds(
            clientInfo.clientId!!,
            setOf(adGroupInfo.adGroupId),
            setOf(adGroupInfo.campaignId),
            setOf(BidModifierType.PRISMA_INCOME_GRADE_MULTIPLIER),
            BidModifierLevel.values().toSet(),
            clientInfo.uid
        )

        val expectedModifier = BidModifierPrismaIncomeGrade()
            .withAdGroupId(adGroupInfo.adGroupId)
            .withEnabled(true)
            .withExpressionAdjustments(
                listOf(
                    BidModifierPrismaIncomeGradeAdjustment()
                        .withPercent(100)
                        .withCondition(
                            listOf(
                                listOf(
                                    BidModifierExpressionLiteral()
                                        .withOperation(BidModifierExpressionOperator.EQ)
                                        .withParameter(BidModifierExpressionParameter.PRISMA_INCOME_GRADE)
                                        .withValueString("1")
                                )
                            )
                        ),
                    BidModifierPrismaIncomeGradeAdjustment()
                        .withPercent(120)
                        .withCondition(
                            listOf(
                                listOf(
                                    BidModifierExpressionLiteral()
                                        .withOperation(BidModifierExpressionOperator.EQ)
                                        .withParameter(BidModifierExpressionParameter.PRISMA_INCOME_GRADE)
                                        .withValueString("2")
                                )
                            )
                        )
                )

            )

        modifiers.size.checkEquals(1)
        modifiers.first().checkBeanDiffer(expectedModifier, DefaultCompareStrategies.onlyExpectedFields())
    }
}
