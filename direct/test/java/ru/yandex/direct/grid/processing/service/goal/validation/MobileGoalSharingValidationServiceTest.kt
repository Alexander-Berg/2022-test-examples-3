package ru.yandex.direct.grid.processing.service.goal.validation

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.hamcrest.Matcher
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.not
import org.junit.Test
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.processing.model.goal.GdMobileGoalSharingMainRep
import ru.yandex.direct.grid.processing.model.goal.GdMobileGoalSharingReq
import ru.yandex.direct.test.utils.assertj.Conditions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.DefectInfo
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult
import ru.yandex.direct.validation.util.D

private val CLIENT_ID = ClientId.fromLong(211)

class MobileGoalSharingValidationServiceTest {
    private val mobileGoalSharingValidationService = MobileGoalSharingValidationService(mock(), mock())

    @Test
    fun validateMutationInput_AllValid_Success() {
        val vr = mobileGoalSharingValidationService.validateMutationInput(
            CLIENT_ID,
            GdMobileGoalSharingReq(
                listOf(
                    GdMobileGoalSharingMainRep("loginOfClient1"),
                    GdMobileGoalSharingMainRep("loginOfClient2"),
                )
            ),
            foundUsers = mapOf(
                "loginofclient1" to User()
                    .withClientId(ClientId.fromLong(1))
                    .withUid(100)
                    .withChiefUid(100),
                "loginofclient2" to User()
                    .withClientId(ClientId.fromLong(2))
                    .withUid(200)
                    .withChiefUid(200),
            ),
            consumersOfMobileGoals = listOf(ClientId.fromLong(2)),  // уже шарим второму клиенту
            consumerToOwners = mapOf(ClientId.fromLong(2) to listOf(CLIENT_ID)),
            MobileGoalSharingValidationService.MutationInputType.ADD
        )
        assertThat(vr).doesNotContainAnyErrors()
    }

    @Test
    fun validateMutationInput_LoginsSizeNotMoreLimit_WithoutMaxElementsError() {
        val vr = mobileGoalSharingValidationService.validateMutationInput(
            CLIENT_ID,
            GdMobileGoalSharingReq(
                (1..MAX_MOBILE_GOAL_CONSUMERS).map { GdMobileGoalSharingMainRep("xxx") }
            ),
            foundUsers = mapOf(),
            consumersOfMobileGoals = listOf(),
            consumerToOwners = mapOf(),
            MobileGoalSharingValidationService.MutationInputType.ADD
        )
        assertThat(vr).doesNotContainErrors(
            validationError(path(field("mainReps")), CollectionDefectIds.Size.MAX_ELEMENTS_PER_REQUEST)
        )
    }

    @Test
    fun validateMutationInput_LoginsSizeOverLimit_Error() {
        val vr = mobileGoalSharingValidationService.validateMutationInput(
            CLIENT_ID,
            GdMobileGoalSharingReq(
                (1..MAX_MOBILE_GOAL_CONSUMERS + 1).map { GdMobileGoalSharingMainRep("xxx") }
            ),
            foundUsers = mapOf(),
            consumersOfMobileGoals = listOf(),
            consumerToOwners = mapOf(),
            MobileGoalSharingValidationService.MutationInputType.ADD
        )
        assertThat(vr).containsExactlyErrors(
            validationError(path(field("mainReps")), CollectionDefectIds.Size.MAX_ELEMENTS_PER_REQUEST)
        )
    }

    @Test
    fun validateMutationInput_LoginsNotUnique_Error() {
        val vr = mobileGoalSharingValidationService.validateMutationInput(
            CLIENT_ID,
            GdMobileGoalSharingReq(
                listOf(
                    GdMobileGoalSharingMainRep("xxx"),
                    GdMobileGoalSharingMainRep("xxx"),
                )
            ),
            foundUsers = mapOf(),
            consumersOfMobileGoals = listOf(),
            consumerToOwners = mapOf(),
            MobileGoalSharingValidationService.MutationInputType.ADD
        )
        assertThat(vr).containsExactlyErrors(
            validationError(
                path(field("mainReps"), index(0)),
                CollectionDefectIds.Gen.MUST_NOT_CONTAIN_DUPLICATED_ELEMENTS
            ),
            validationError(
                path(field("mainReps"), index(1)),
                CollectionDefectIds.Gen.MUST_NOT_CONTAIN_DUPLICATED_ELEMENTS
            ),
        )
    }

    @Test
    fun validateMutationInput_LoginDoesNotExist_Error() {
        val vr = mobileGoalSharingValidationService.validateMutationInput(
            CLIENT_ID,
            GdMobileGoalSharingReq(
                listOf(
                    GdMobileGoalSharingMainRep("xxx"),
                    GdMobileGoalSharingMainRep("yyy"),
                )
            ),
            foundUsers = mapOf("xxx" to User().withClientId(ClientId.fromLong(1))),
            consumersOfMobileGoals = listOf(),
            consumerToOwners = mapOf(),
            MobileGoalSharingValidationService.MutationInputType.ADD
        )
        assertThat(vr).containsExactlyErrors(
            validationError(
                path(field("mainReps"), index(1)),
                DefectIds.OBJECT_NOT_FOUND
            ),
        )
    }

    @Test
    fun validateMutationInput_LoginIsNotMainRep_Error() {
        val vr = mobileGoalSharingValidationService.validateMutationInput(
            CLIENT_ID,
            GdMobileGoalSharingReq(
                listOf(
                    GdMobileGoalSharingMainRep("xxx"),
                )
            ),
            foundUsers = mapOf(
                "xxx" to User()
                    .withClientId(ClientId.fromLong(1))
                    .withUid(100L)
                    .withChiefUid(200L) // != uid

            ),
            consumersOfMobileGoals = listOf(),
            consumerToOwners = mapOf(),
            MobileGoalSharingValidationService.MutationInputType.ADD
        )
        assertThat(vr).containsExactlyErrors(
            validationError(
                path(field("mainReps"), index(0)),
                DefectIds.OBJECT_NOT_FOUND
            ),
        )
    }

    @Test
    fun validateMutationInput_ClientAlreadyHasAccessToAnother_Error() {
        val vr = mobileGoalSharingValidationService.validateMutationInput(
            CLIENT_ID,
            GdMobileGoalSharingReq(
                listOf(
                    GdMobileGoalSharingMainRep("xxx"),
                )
            ),
            foundUsers = mapOf(
                "xxx" to User()
                    .withClientId(ClientId.fromLong(1))
                    .withUid(100)
                    .withChiefUid(100)

            ),
            consumersOfMobileGoals = listOf(),
            consumerToOwners = mapOf(ClientId.fromLong(1) to listOf(ClientId.fromLong(666))),
            MobileGoalSharingValidationService.MutationInputType.ADD
        )
        assertThat(vr).containsExactlyErrors(
            validationError(
                path(field("mainReps"), index(0)),
                DefectIds.INVALID_VALUE
            ),
        )
    }

    @Test
    fun validateMutationInput_ForAdd_AllConsumersSizeOverLimit_Error() {
        val vr = mobileGoalSharingValidationService.validateMutationInput(
            CLIENT_ID,
            GdMobileGoalSharingReq(
                listOf(
                    GdMobileGoalSharingMainRep("loginofclient1"),
                )
            ),
            foundUsers = mapOf(
                "loginofclient1" to User()
                    .withClientId(ClientId.fromLong(201))
                    .withUid(100)
                    .withChiefUid(100),
            ),
            consumersOfMobileGoals = (1..MAX_MOBILE_GOAL_CONSUMERS).map { ClientId.fromLong(it.toLong()) },
            consumerToOwners = mapOf(),
            MobileGoalSharingValidationService.MutationInputType.ADD
        )

        assertThat(vr).containsExactlyErrors(
            validationError(path(field("mainReps")), CollectionDefectIds.Size.MAX_ELEMENTS_EXCEEDED)
        )
    }

    @Test
    fun validateMutationInput_ForRemove_AllConsumersSizeOverLimit_WithoutMaxElementsError() {
        val vr = mobileGoalSharingValidationService.validateMutationInput(
            CLIENT_ID,
            GdMobileGoalSharingReq(
                listOf(
                    GdMobileGoalSharingMainRep("loginofclient1"),
                )
            ),
            foundUsers = mapOf(
                "loginofclient1" to User()
                    .withClientId(ClientId.fromLong(201))
                    .withUid(100)
                    .withChiefUid(100),
            ),
            consumersOfMobileGoals = (1..MAX_MOBILE_GOAL_CONSUMERS).map { ClientId.fromLong(it.toLong()) },
            consumerToOwners = mapOf(),
            MobileGoalSharingValidationService.MutationInputType.REMOVE
        )

        assertThat(vr).doesNotContainAnyErrors()
    }
}

private fun <ACTUAL : ValidationResult<*, D>> ObjectAssert<ACTUAL>.doesNotContainAnyErrors() {
    extracting { it.flattenErrors() }.asList().isEmpty()
}

private fun <ACTUAL : ValidationResult<*, D>> ObjectAssert<ACTUAL>.containsExactlyErrors(
    vararg matchers: Matcher<DefectInfo<Defect<Any>>>
) {
    extracting { it.flattenErrors() }.asList().`is`(
        Conditions.matchedBy(containsInAnyOrder(*matchers))
    )
}

private fun <ACTUAL : ValidationResult<*, D>> ObjectAssert<ACTUAL>.doesNotContainErrors(
    vararg matchers: Matcher<DefectInfo<Defect<Any>>>
) {
    extracting { it.flattenErrors() }.asList().`is`(
        Conditions.matchedBy(not(hasItems(*matchers)))
    )
}
