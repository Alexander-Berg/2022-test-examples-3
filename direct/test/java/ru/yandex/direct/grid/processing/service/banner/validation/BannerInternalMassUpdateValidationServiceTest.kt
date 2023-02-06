package ru.yandex.direct.grid.processing.service.banner.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.InternalBanner
import ru.yandex.direct.core.entity.banner.model.InternalModerationInfo
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefectIds
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.NewInternalBannerInfo
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannerFieldChange
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannerFieldChange.IDS
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannerFieldChange.VALUE
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannerFieldChangeStatusShowAfterModeration
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannerFieldChangeTemplateVariable
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannerFieldChangeTicketUrl
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannerFieldChangeValueUnion
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannersAggregatedState
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannersAggregatedState.BANNER_IDS
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannersMassUpdate
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannersMassUpdate.CHANGES
import ru.yandex.direct.grid.processing.model.banner.GdTemplateVariable
import ru.yandex.direct.grid.processing.service.validation.GridDefectDefinitions
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class BannerInternalMassUpdateValidationServiceTest {

    @Autowired
    private lateinit var validationService: BannerInternalMassUpdateValidationService

    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientId: ClientId
    private var operatorUid: Long = 0

    private lateinit var moderatedBanner: NewInternalBannerInfo
    private lateinit var unmoderatedBanner: NewInternalBannerInfo

    @Before
    fun before() {
        val clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct()
        clientId = clientInfo.clientId!!
        operatorUid = clientInfo.chiefUserInfo!!.uid

        val campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo)
        val adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo)

        moderatedBanner = createModeratedInternalBanner(adGroupInfo)
        unmoderatedBanner = createUnmoderatedInternalBanner(adGroupInfo)
    }

    @Test
    fun validateGdInternalBannersAggregatedState_validInput_noErrors() {
        val input = GdInternalBannersAggregatedState().apply {
            bannerIds = listOf(moderatedBanner.bannerId)
        }

        val vr = validationService.validateGdInternalBannersAggregatedState(input, clientId, operatorUid)

        assertThat(vr.flattenErrors()).isEmpty()
    }

    @Test
    fun validateGdInternalBannersAggregatedState_emptyBanners_error() {
        val input = GdInternalBannersAggregatedState().apply {
            bannerIds = listOf()
        }

        val vr = validationService.validateGdInternalBannersAggregatedState(input, clientId, operatorUid)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(BANNER_IDS)), notEmptyCollection())
                )
            )
        )
    }

    @Test
    fun validateGdInternalBannersAggregatedState_tooManyBanners_error() {
        val input = GdInternalBannersAggregatedState().apply {
            bannerIds = (1..201L).toList()
        }

        val vr = validationService.validateGdInternalBannersAggregatedState(input, clientId, operatorUid)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(BANNER_IDS)), CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX)
                )
            )
        )
    }

    @Test
    fun validateGdInternalBannersAggregatedState_differentTemplateIds_error() {
        val input = GdInternalBannersAggregatedState().apply {
            bannerIds = listOf(unmoderatedBanner.bannerId, moderatedBanner.bannerId)
        }

        val vr = validationService.validateGdInternalBannersAggregatedState(input, clientId, operatorUid)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(BANNER_IDS)), BannerDefectIds.Gen.DIFFERENT_TEMPLATES_NOT_ALLOWED)
                )
            )
        )
    }

    @Test
    fun validateGdInternalBannersMassUpdate_validInput_noErrors() {
        val input = GdInternalBannersMassUpdate().apply {
            changes = listOf(
                GdInternalBannerFieldChange().apply {
                    ids = listOf(moderatedBanner.bannerId)
                    value = GdInternalBannerFieldChangeValueUnion().apply {
                        ticketUrl = GdInternalBannerFieldChangeTicketUrl().apply {
                            innerValue = "https://st.yandex-team.ru/LEGAL-115"
                        }
                    }
                }
            )
        }

        val vr = validationService.validateGdInternalBannersMassUpdate(input, clientId, operatorUid)

        assertThat(vr.flattenErrors()).isEmpty()
    }

    @Test
    fun validateGdInternalBannersMassUpdate_emptyChanges_error() {
        val input = GdInternalBannersMassUpdate().apply {
            changes = listOf()
        }

        val vr = validationService.validateGdInternalBannersMassUpdate(input, clientId, operatorUid)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(CHANGES)), notEmptyCollection())
                )
            )
        )
    }

    @Test
    fun validateGdInternalBannersMassUpdate_tooManyChanges_error() {
        val input = GdInternalBannersMassUpdate().apply {
            changes = (1..201L).map { GdInternalBannerFieldChange() }
        }

        val vr = validationService.validateGdInternalBannersMassUpdate(input, clientId, operatorUid)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(CHANGES)), CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX)
                )
            )
        )
    }

    @Test
    fun validateGdInternalBannersMassUpdate_differentTemplateIds_error() {
        val input = GdInternalBannersMassUpdate().apply {
            changes = listOf(
                GdInternalBannerFieldChange().apply {
                    ids = listOf(moderatedBanner.bannerId)
                    value = GdInternalBannerFieldChangeValueUnion().apply {
                        ticketUrl = GdInternalBannerFieldChangeTicketUrl().apply {
                            innerValue = "https://st.yandex-team.ru/LEGAL-115"
                        }
                    }
                },
                GdInternalBannerFieldChange().apply {
                    ids = listOf(unmoderatedBanner.bannerId)
                    value = GdInternalBannerFieldChangeValueUnion().apply {
                        templateVariable = GdInternalBannerFieldChangeTemplateVariable().apply {
                            innerValue = GdTemplateVariable().apply {
                                templateResourceId = TemplateResourceRepositoryMockUtils.TEMPLATE_1_RESOURCE_1_REQUIRED
                                value = "abc"
                            }
                        }
                    }
                }
            )
        }

        val vr = validationService.validateGdInternalBannersMassUpdate(input, clientId, operatorUid)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(CHANGES)), BannerDefectIds.Gen.DIFFERENT_TEMPLATES_NOT_ALLOWED)
                )
            )
        )
    }

    @Test
    fun validateGdInternalBannersMassUpdate_oneChangeEmptyBanners_error() {
        val input = GdInternalBannersMassUpdate().apply {
            changes = listOf(
                GdInternalBannerFieldChange().apply {
                    ids = listOf()
                    value = GdInternalBannerFieldChangeValueUnion().apply {
                        ticketUrl = GdInternalBannerFieldChangeTicketUrl().apply {
                            innerValue = "https://st.yandex-team.ru/LEGAL-115"
                        }
                    }
                }
            )
        }

        val vr = validationService.validateGdInternalBannersMassUpdate(input, clientId, operatorUid)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(CHANGES), index(0), field(IDS)), notEmptyCollection())
                )
            )
        )
    }

    @Test
    fun validateGdInternalBannersMassUpdate_oneChangeTooManyBanners_error() {
        val input = GdInternalBannersMassUpdate().apply {
            changes = listOf(
                GdInternalBannerFieldChange().apply {
                    ids = (1..201L).toList()
                    value = GdInternalBannerFieldChangeValueUnion().apply {
                        ticketUrl = GdInternalBannerFieldChangeTicketUrl().apply {
                            innerValue = "https://st.yandex-team.ru/LEGAL-115"
                        }
                    }
                }
            )
        }

        val vr = validationService.validateGdInternalBannersMassUpdate(input, clientId, operatorUid)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(
                        path(field(CHANGES), index(0), field(IDS)),
                        CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX
                    )
                )
            )
        )
    }

    @Test
    fun validateGdInternalBannersMassUpdate_noValuesInUnion_error() {
        val input = GdInternalBannersMassUpdate().apply {
            changes = listOf(
                GdInternalBannerFieldChange().apply {
                    ids = listOf(moderatedBanner.bannerId)
                    value = GdInternalBannerFieldChangeValueUnion()
                }
            )
        }

        val vr = validationService.validateGdInternalBannersMassUpdate(input, clientId, operatorUid)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(CHANGES), index(0), field(VALUE)), GridDefectDefinitions.invalidUnion())
                )
            )
        )
    }

    @Test
    fun validateGdInternalBannersMassUpdate_twoValuesInUnion_error() {
        val input = GdInternalBannersMassUpdate().apply {
            changes = listOf(
                GdInternalBannerFieldChange().apply {
                    ids = listOf(moderatedBanner.bannerId)
                    value = GdInternalBannerFieldChangeValueUnion().apply {
                        ticketUrl = GdInternalBannerFieldChangeTicketUrl().apply {
                            innerValue = "https://st.yandex-team.ru/LEGAL-115"
                        }
                        statusShowAfterModeration = GdInternalBannerFieldChangeStatusShowAfterModeration().apply {
                            innerValue = false
                        }
                    }
                }
            )
        }

        val vr = validationService.validateGdInternalBannersMassUpdate(input, clientId, operatorUid)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(CHANGES), index(0), field(VALUE)), GridDefectDefinitions.invalidUnion())
                )
            )
        )
    }

    @Test
    fun validateGdInternalBannersMassUpdate_templateMustBeModerated_error() {
        val input = GdInternalBannersMassUpdate().apply {
            changes = listOf(
                GdInternalBannerFieldChange().apply {
                    ids = listOf(unmoderatedBanner.bannerId)
                    value = GdInternalBannerFieldChangeValueUnion().apply {
                        ticketUrl = GdInternalBannerFieldChangeTicketUrl().apply {
                            innerValue = "https://st.yandex-team.ru/LEGAL-115"
                        }
                    }
                }
            )
        }

        val vr = validationService.validateGdInternalBannersMassUpdate(input, clientId, operatorUid)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(
                        path(field(CHANGES), index(0), field(VALUE)),
                        BannerDefectIds.Gen.TEMPLATE_MUST_BE_MODERATED
                    )
                )
            )
        )
    }

    @Test
    fun validateGdInternalBannersMassUpdate_templateResourceIdNotFound_error() {
        val input = GdInternalBannersMassUpdate().apply {
            changes = listOf(
                GdInternalBannerFieldChange().apply {
                    ids = listOf(unmoderatedBanner.bannerId)
                    value = GdInternalBannerFieldChangeValueUnion().apply {
                        templateVariable = GdInternalBannerFieldChangeTemplateVariable().apply {
                            innerValue = GdTemplateVariable().apply {
                                templateResourceId = TemplateResourceRepositoryMockUtils.TEMPLATE_5_RESOURCE_1_REQUIRED
                                value = "abc"
                            }
                        }
                    }
                }
            )
        }

        val vr = validationService.validateGdInternalBannersMassUpdate(input, clientId, operatorUid)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(
                        path(field(CHANGES), index(0), field(VALUE)),
                        BannerDefectIds.Gen.TEMPLATE_RESOURCE_ID_NOT_FOUND
                    )
                )
            )
        )
    }

    private fun createUnmoderatedInternalBanner(adGroupInfo: AdGroupInfo): NewInternalBannerInfo {
        return steps.internalBannerSteps().createInternalBanner(adGroupInfo)
    }

    private fun createModeratedInternalBanner(adGroupInfo: AdGroupInfo): NewInternalBannerInfo {
        val internalBannerInfo = steps.internalBannerSteps().createModeratedInternalBanner(
            adGroupInfo, BannerStatusModerate.YES
        )
        internalBannerInfo.getBanner<InternalBanner>().apply {
            moderationInfo = InternalModerationInfo().apply {
                sendToModeration = false
                statusShowAfterModeration = true
                customComment = "Оставьте комментарий для модератора"
                ticketUrl = "https://st.yandex-team.ru/LEGAL-113"
                isSecretAd = false
            }
            statusShow = true
        }
        return internalBannerInfo
    }
}
