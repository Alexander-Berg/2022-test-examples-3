package ru.yandex.direct.grid.processing.service.group.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChange
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChange.IDS
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChange.VALUE
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChangeFinishTime
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChangeOperation
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChangeStartTime
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChangeValueUnion
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupsAggregatedState
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupsAggregatedState.AD_GROUP_IDS
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupsMassUpdate
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupsMassUpdate.CHANGES
import ru.yandex.direct.grid.processing.service.validation.GridDefectDefinitions
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import java.time.LocalDateTime

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class AdGroupInternalMassUpdateValidationServiceTest {

    @Autowired
    private lateinit var validationService: AdGroupInternalMassUpdateValidationService

    @Autowired
    private lateinit var steps: Steps

    private lateinit var clientId: ClientId
    private lateinit var internalAdGroup: AdGroupInfo

    @Before
    fun before() {
        val clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct()
        clientId = clientInfo.clientId!!
        val campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo)
        internalAdGroup = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo)
    }

    @Test
    fun validateInternalAdGroupsAggregatedState_validInput_noErrors() {
        val input = GdInternalAdGroupsAggregatedState().apply {
            adGroupIds = listOf(internalAdGroup.adGroupId)
        }

        val vr = validationService.validateGdInternalAdGroupsAggregatedState(input)
        assertThat(vr.flattenErrors()).isEmpty()
    }

    @Test
    fun validateInternalAdGroupsAggregatedState_emptyAdGroups_error() {
        val input = GdInternalAdGroupsAggregatedState().apply {
            adGroupIds = listOf()
        }

        val vr = validationService.validateGdInternalAdGroupsAggregatedState(input)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(AD_GROUP_IDS)), notEmptyCollection())
                )
            )
        )
    }

    @Test
    fun validateInternalAdGroupsAggregatedState_tooManyAdGroups_error() {
        val input = GdInternalAdGroupsAggregatedState().apply {
            adGroupIds = (1..201L).toList()
        }

        val vr = validationService.validateGdInternalAdGroupsAggregatedState(input)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(AD_GROUP_IDS)), CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX)
                )
            )
        )
    }

    @Test
    fun validateGdInternalAdGroupsMassUpdate_validInput_noErrors() {
        val input = GdInternalAdGroupsMassUpdate().apply {
            changes = listOf(
                GdInternalAdGroupFieldChange().apply {
                    ids = listOf(internalAdGroup.adGroupId)
                    operation = GdInternalAdGroupFieldChangeOperation.ADD
                    value = GdInternalAdGroupFieldChangeValueUnion().apply {
                        startTime = GdInternalAdGroupFieldChangeStartTime().apply {
                            innerValue = LocalDateTime.now()
                        }
                    }
                }
            )
        }

        val vr = validationService.validateGdInternalAdGroupsMassUpdate(input)

        assertThat(vr.flattenErrors()).isEmpty()
    }

    @Test
    fun validateGdInternalAdGroupsMassUpdate_emptyChanges_error() {
        val input = GdInternalAdGroupsMassUpdate().apply {
            changes = listOf()
        }

        val vr = validationService.validateGdInternalAdGroupsMassUpdate(input)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(CHANGES)), notEmptyCollection())
                )
            )
        )
    }

    @Test
    fun validateGdInternalAdGroupsMassUpdate_tooManyChanges_error() {
        val input = GdInternalAdGroupsMassUpdate().apply {
            changes = (1..201).map { GdInternalAdGroupFieldChange() }
        }

        val vr = validationService.validateGdInternalAdGroupsMassUpdate(input)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(CHANGES)), CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX)
                )
            )
        )
    }

    @Test
    fun validateGdInternalAdGroupsMassUpdate_oneChangeEmptyAdGroups_error() {
        val input = GdInternalAdGroupsMassUpdate().apply {
            changes = listOf(
                GdInternalAdGroupFieldChange().apply {
                    ids = listOf()
                    operation = GdInternalAdGroupFieldChangeOperation.ADD
                    value = GdInternalAdGroupFieldChangeValueUnion().apply {
                        startTime = GdInternalAdGroupFieldChangeStartTime().apply {
                            innerValue = LocalDateTime.now()
                        }
                    }
                }
            )
        }

        val vr = validationService.validateGdInternalAdGroupsMassUpdate(input)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(CHANGES), index(0), field(IDS)), notEmptyCollection())
                )
            )
        )
    }

    @Test
    fun validateGdInternalAdGroupsMassUpdate_oneChangeTooManyAdGroups_error() {
        val input = GdInternalAdGroupsMassUpdate().apply {
            changes = listOf(
                GdInternalAdGroupFieldChange().apply {
                    ids = (1..201L).toList()
                    operation = GdInternalAdGroupFieldChangeOperation.ADD
                    value = GdInternalAdGroupFieldChangeValueUnion().apply {
                        startTime = GdInternalAdGroupFieldChangeStartTime().apply {
                            innerValue = LocalDateTime.now()
                        }
                    }
                }
            )
        }

        val vr = validationService.validateGdInternalAdGroupsMassUpdate(input)

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
    fun validateGdInternalAdGroupsMassUpdate_noValuesInUnion_error() {
        val input = GdInternalAdGroupsMassUpdate().apply {
            changes = listOf(
                GdInternalAdGroupFieldChange().apply {
                    ids = listOf(internalAdGroup.adGroupId)
                    operation = GdInternalAdGroupFieldChangeOperation.ADD
                    value = GdInternalAdGroupFieldChangeValueUnion()
                }
            )
        }

        val vr = validationService.validateGdInternalAdGroupsMassUpdate(input)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(CHANGES), index(0), field(VALUE)), GridDefectDefinitions.invalidUnion())
                )
            )
        )
    }

    @Test
    fun validateGdInternalAdGroupsMassUpdate_twoValuesInUnion_error() {
        val input = GdInternalAdGroupsMassUpdate().apply {
            changes = listOf(
                GdInternalAdGroupFieldChange().apply {
                    ids = listOf(internalAdGroup.adGroupId)
                    operation = GdInternalAdGroupFieldChangeOperation.ADD
                    value = GdInternalAdGroupFieldChangeValueUnion().apply {
                        startTime = GdInternalAdGroupFieldChangeStartTime().apply {
                            innerValue = LocalDateTime.now()
                        }
                        finishTime = GdInternalAdGroupFieldChangeFinishTime().apply {
                            innerValue = LocalDateTime.now()
                        }
                    }
                }
            )
        }

        val vr = validationService.validateGdInternalAdGroupsMassUpdate(input)

        assertThat(vr).`is`(
            matchedBy(
                hasDefectDefinitionWith<Any>(
                    validationError(path(field(CHANGES), index(0), field(VALUE)), GridDefectDefinitions.invalidUnion())
                )
            )
        )
    }

}
