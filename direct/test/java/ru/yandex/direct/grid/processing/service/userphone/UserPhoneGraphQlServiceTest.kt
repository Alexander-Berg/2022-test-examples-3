package ru.yandex.direct.grid.processing.service.userphone

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.entity.user.model.User.PHONE
import ru.yandex.direct.core.entity.user.model.User.VERIFIED_PHONE_ID
import ru.yandex.direct.core.validation.defects.Defects.phoneMustBeVerified
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.userphone.GdPassportPhone
import ru.yandex.direct.grid.processing.model.userphone.GdUpdateUserVerifiedPhonePayload
import ru.yandex.direct.grid.processing.model.userphone.GdUserPhonesPayload
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.util.GraphQLUtils.getGdValidationResults
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect
import ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.path

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class UserPhoneGraphQlServiceTest : UserPhoneGraphQlServiceBaseTest() {

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor

    @Test
    fun userPhones_NoVerifiedPhone_OldPhoneReturned() {
        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .getUserPhones()
            .checkEquals(GdUserPhonesPayload()
                .apply {
                    directPhone = GdPassportPhone().apply {
                        phone = OLD_USER_PHONE
                        isConfirmed = false
                    }
                    passportPhones = listOf(GdPassportPhone().apply {
                        phoneId = PASSPORT_PHONE_ID
                        phone = PASSPORT_USER_PHONE
                        isConfirmed = true
                    })
                })
    }

    @Test
    fun userPhones_UserHasVerifiedPhone_NewPhoneReturned() {
        steps.userSteps().setUserProperty(userInfo, VERIFIED_PHONE_ID, PASSPORT_PHONE_ID)

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .getUserPhones()
            .checkEquals(GdUserPhonesPayload().apply {
                directPhone = GdPassportPhone().apply {
                    phoneId = PASSPORT_PHONE_ID
                    phone = PASSPORT_USER_PHONE
                    isConfirmed = true
                }
                passportPhones = listOf(GdPassportPhone().apply {
                    phoneId = PASSPORT_PHONE_ID
                    phone = PASSPORT_USER_PHONE
                    isConfirmed = true
                })
            })
    }

    @Test
    fun userPhones_UserHasVerifiedPhone_VerifiedPhoneNotFoundInPassport_OldPhoneReturned() {
        steps.userSteps().setUserProperty(userInfo, VERIFIED_PHONE_ID, NONEXISTENT_PASSPORT_PHONE_ID)

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .getUserPhones()
            .checkEquals(GdUserPhonesPayload().apply {
                directPhone = GdPassportPhone().apply {
                    phone = OLD_USER_PHONE
                    isConfirmed = false
                }
                passportPhones = listOf(GdPassportPhone().apply {
                    phoneId = PASSPORT_PHONE_ID
                    phone = PASSPORT_USER_PHONE
                    isConfirmed = true
                })
            })
    }

    @Test
    fun userPhones_UserHasNoVerifiedPhoneAndNoOldPhone_NoPhoneReturned() {
        steps.userSteps().setUserProperty(userInfo, PHONE, null)

        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .getUserPhones()
            .checkEquals(GdUserPhonesPayload().apply {
                passportPhones = listOf(GdPassportPhone().apply {
                    phoneId = PASSPORT_PHONE_ID
                    phone = PASSPORT_USER_PHONE
                    isConfirmed = true
                })
            })
    }

    @Test
    fun updateUserVerifiedPhone_PhoneFoundInPassport_VerifiedPhoneUpdated() {
        ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .updateUserVerifiedPhone(PASSPORT_PHONE_ID)
            .checkEquals(GdUpdateUserVerifiedPhonePayload().apply {
                success = true
            })

        getUser().verifiedPhoneId
            .checkEquals(PASSPORT_PHONE_ID)
    }

    @Test
    fun updateUserVerifiedPhone_PhoneNotFoundInPassport_VerifiedPhoneNotUpdated() {
        val result = ktGraphQLTestExecutor.withDefaultGraphQLContext(userInfo.user!!)
            .updateUserVerifiedPhoneRaw(NONEXISTENT_PASSPORT_PHONE_ID)

        assertThat(result.errors).hasSize(1)
        val vr = getGdValidationResults(result.errors)

        assertThat(vr).hasSize(1)
        assertThat(vr[0]).`is`(matchedBy(hasErrorsWith(
            gridDefect(path(field(VERIFIED_PHONE_ID)), phoneMustBeVerified()))))

        getUser().verifiedPhoneId
            .checkEquals(null)
    }

    private fun getUser(): User {
        return userRepository.fetchByUids(userInfo.shard, listOf(userInfo.uid))[0]
    }
}
