package ru.yandex.direct.web.entity.uac.repository

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.direct.core.entity.uac.model.AccountFeatures
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbAccountRepository
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.getValueReaderOrNull
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdLong
import ru.yandex.direct.core.entity.uac.repository.ydb.UacYdbUtils.toIdString
import ru.yandex.direct.core.entity.uac.repository.ydb.model.UacYdbAccount
import ru.yandex.direct.core.entity.uac.repository.ydb.schema.ACCOUNT
import ru.yandex.direct.test.utils.randomPositiveLong
import ru.yandex.direct.web.configuration.DirectWebTest
import ru.yandex.direct.ydb.builder.querybuilder.SelectBuilder.select
import ru.yandex.direct.ydb.client.ResultSetReaderWrapped
import ru.yandex.direct.ydb.column.Column
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit


@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class UacYdbAccountRepositoryTest : AbstractUacRepositoryTest() {

    @Autowired
    private lateinit var uacYdbAccountRepository: UacYdbAccountRepository

    private lateinit var account: UacYdbAccount

    @Before
    fun before() {
        val uid = randomPositiveLong()
        val clientId = randomPositiveLong()
        account = UacYdbAccount(
            uid = uid,
            features = AccountFeatures(),
            createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            directClientId = clientId,
        )
        uacYdbAccountRepository.saveAccount(account)
    }

    @Test
    fun testGetAccountByIdNonExisting() {
        val nonExistentId = randomPositiveLong().toIdString()
        val actualAccount = uacYdbAccountRepository.getAccountById(nonExistentId)
        assertThat(actualAccount).isNull()
    }

    @Test
    fun testGetAccountByClientIdNonExisting() {
        val nonExistingClientId = randomPositiveLong()
        val actualClient = uacYdbAccountRepository.getAccountByClientId(nonExistingClientId)
        assertThat(actualClient).isNull()
    }

    @Test
    fun testGetAccountById() {
        val actualAccount = uacYdbAccountRepository.getAccountById(account.id)
        assertThat(actualAccount).isEqualTo(account)
    }

    @Test
    fun testGetAccountByClientId() {
        val actualAccount = uacYdbAccountRepository.getAccountByClientId(account.directClientId)
        assertThat(actualAccount).isEqualTo(account)
    }

    @Test
    @TestCaseName("testSaveAccount({0})")
    @Parameters(source = UacIdsProvider::class)
    fun testSaveAccount(caseName: String, id: String) {
        val uid = randomPositiveLong()
        val clientId = randomPositiveLong()
        val account = UacYdbAccount(
            id = id,
            uid = uid,
            features = AccountFeatures(
                useRecommendedKeywords = false,
            ),
            createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS) - Duration.ofHours(2),
            directClientId = clientId,
        )

        uacYdbAccountRepository.saveAccount(account)

        val actualAccount = uacYdbAccountRepository.getAccountById(id)
        assertThat(actualAccount).isEqualTo(account)
    }

    fun provideFeatures(): Array<Array<Any?>> {
        return arrayOf(
            arrayOf(null, "null"),
            arrayOf(AccountFeatures(useRecommendedKeywords = null), """{"use_recommended_keywords":null}"""),
            arrayOf(AccountFeatures(useRecommendedKeywords = true), """{"use_recommended_keywords":true}"""),
            arrayOf(AccountFeatures(useRecommendedKeywords = false), """{"use_recommended_keywords":false}"""),
        )
    }

    @Test
    @Parameters(method = "provideFeatures")
    fun testAccountFeatures(features: AccountFeatures?, serialized: String?) {
        val clientId = randomPositiveLong()
        val account = UacYdbAccount(
            uid = randomPositiveLong(),
            features = features,
            directClientId = clientId,
        )

        uacYdbAccountRepository.saveAccount(account)

        val actualAccount = uacYdbAccountRepository.getAccountById(account.id)
        val actualFeatures = getSavedValue(account.id, ACCOUNT.FEATURES)
            .getValueReaderOrNull(ACCOUNT.FEATURES)?.json
        assertSoftly {
            it.assertThat(actualAccount).isNotNull
            it.assertThat(actualAccount!!.features).isEqualTo(features)
            it.assertThat(actualFeatures).isEqualTo(serialized)
        }
    }

    private fun <T> getSavedValue(id: String, column: Column<T>): ResultSetReaderWrapped {
        val query = select(column)
            .from(ACCOUNT)
            .where(ACCOUNT.ID.eq(id.toIdLong()))
            .queryAndParams(path)
        val result = ydbClient.executeQuery(query).getResultSet(0)
        result.next()
        return result
    }
}
