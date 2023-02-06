package ru.yandex.direct.api.v5.entity.bidmodifiers.delegate

import com.nhaarman.mockitokotlin2.whenever
import com.yandex.direct.api.v5.bidmodifiers.BidModifierLevelEnum
import com.yandex.direct.api.v5.bidmodifiers.BidModifiersSelectionCriteria
import com.yandex.direct.api.v5.bidmodifiers.GetRequest
import com.yandex.direct.api.v5.bidmodifiers.IncomeGradeAdjustmentFieldEnum
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.api.v5.entity.bidmodifiers.validation.GetBidModifiersValidationService
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource
import ru.yandex.direct.api.v5.testing.configuration.Api5Test
import ru.yandex.direct.common.util.PropertyFilter
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPrismaIncomeGrade
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPrismaIncomeGradeAdjustment
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionLiteral
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionOperator
import ru.yandex.direct.core.entity.bidmodifier.model.BidModifierExpressionParameter
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService.getExternalId
import ru.yandex.direct.core.entity.user.model.ApiUser
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName

@Api5Test
@RunWith(SpringRunner::class)
class GetIncomeGradeBidModifiersDelegateTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var bidModifierService: BidModifierService

    @Autowired
    private lateinit var adGroupService: AdGroupService

    @Autowired
    private lateinit var validationService: GetBidModifiersValidationService

    @Autowired
    private lateinit var propertyFilter: PropertyFilter

    @Mock
    private lateinit var auth: ApiAuthenticationSource

    private lateinit var delegate: GetBidModifiersDelegate

    private lateinit var clientInfo: ClientInfo

    @Before
    fun before() {
        MockitoAnnotations.openMocks(this)
        clientInfo = steps.clientSteps().createDefaultClient()
        whenever(auth.subclient).thenReturn(ApiUser().withClientId(clientInfo.clientId))
        whenever(auth.chiefSubclient).thenReturn(ApiUser().withClientId(clientInfo.clientId))
        whenever(auth.operator).thenReturn(ApiUser().withUid(clientInfo.uid))
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.INCOME_GRADE_BID_MODIFIER_ALLOWED, true)
        delegate = GetBidModifiersDelegate(
            auth,
            validationService,
            bidModifierService,
            propertyFilter,
            adGroupService
        )
    }

    @Test
    fun getIncomeGradeModifier() {
        val campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo)
        val adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo)
        val modifier = BidModifierPrismaIncomeGrade()
            .withAdGroupId(adGroupInfo.adGroupId)
            .withType(BidModifierType.PRISMA_INCOME_GRADE_MULTIPLIER)
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
                                        .withValueString("0")
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
                                        .withValueString("1")
                                )
                            )
                        )
                )
            )
        steps.featureSteps().addClientFeature(clientInfo.clientId, FeatureName.INCOME_GRADE_BID_MODIFIER_ALLOWED, true)
        val addResult = bidModifierService.add(listOf(modifier), clientInfo.clientId, clientInfo.uid);
        val modifierIds = addResult.result.flatMap { it.result }

        val criteria = BidModifiersSelectionCriteria()
            .withIds(modifierIds)
            .withLevels(BidModifierLevelEnum.AD_GROUP)
        val externalRequest = GetRequest().withSelectionCriteria(criteria)
            .withIncomeGradeAdjustmentFieldNames(
                IncomeGradeAdjustmentFieldEnum.BID_MODIFIER,
                IncomeGradeAdjustmentFieldEnum.GRADE,
                IncomeGradeAdjustmentFieldEnum.ENABLED
            )

        val request = delegate.convertRequest(externalRequest)
        val bidModifierGetItems = delegate.get(request)
        Assertions.assertThat(bidModifierGetItems).hasSize(2)
        val actualModifiers = bidModifierGetItems.map { it.id }
        val expectedModifiers = modifier.expressionAdjustments.map { getExternalId(it.id, modifier.type) }
        Assertions.assertThat(actualModifiers).containsSequence(expectedModifiers)
    }
}
