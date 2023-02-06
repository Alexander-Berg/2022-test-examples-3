package ru.yandex.direct.core.copyentity

import org.assertj.core.api.SoftAssertions
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.assertj.core.util.BigDecimalComparator
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.client.model.Client
import ru.yandex.direct.core.testing.data.TestUsers
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.i18n.Language
import ru.yandex.direct.model.Entity
import ru.yandex.direct.result.MassResult
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful
import java.math.BigDecimal
import java.util.Locale

object CopyEntityTestUtils {

    @JvmStatic
    fun defaultCopyContainer(): CopyOperationContainer = defaultCopyContainer(Client().withId(1))

    @JvmStatic
    fun defaultCopyContainer(client: Client) = CopyOperationContainer(
        config = CopyConfig(
            ClientId.fromLong(client.id),
            ClientId.fromLong(client.id),
            1,
            BaseCampaign::class.java, listOf<Long>()
        ),
        1, client,
        1, client,
        TestUsers.defaultUser(),
        Locale.forLanguageTag(Language.RU.langString),
    )

    @JvmStatic
    fun defaultCopyContainer(clientInfo: ClientInfo, uid: Long) = CopyOperationContainer(
        config = CopyConfig(
            clientInfo.clientId!!, clientInfo.clientId!!, uid,
            BaseCampaign::class.java, listOf<Long>(),
        ),
        clientInfo.shard, clientInfo.client!!,
        clientInfo.shard, clientInfo.client!!,
        clientInfo.chiefUserInfo!!.user!!,
        Locale.forLanguageTag(clientInfo.chiefUserInfo!!.user!!.lang.langString),
    )

    @JvmStatic
    fun defaultBetweenShardsCopyContainer(
        currencyFrom: CurrencyCode = CurrencyCode.RUB,
        currencyTo: CurrencyCode = CurrencyCode.RUB,
    ) = CopyOperationContainer(
        config = CopyConfig(ClientId.fromLong(1), ClientId.fromLong(2), 1, BaseCampaign::class.java, listOf<Long>()),
        1,
        Client()
            .withId(1)
            .withWorkCurrency(currencyFrom),
        1,
        Client()
            .withId(2)
            .withWorkCurrency(currencyTo),
        TestUsers.defaultUser(),
        Locale.forLanguageTag(Language.RU.langString),
    )

    @JvmStatic
    fun adGroupCopyConfig(
        clientInfo: ClientInfo,
        adGroupInfo: AdGroupInfo,
        campaignIdTo: Long,
        uid: Long,
    ): CopyConfig<AdGroup, Long> =
        adGroupCopyConfig(clientInfo, adGroupInfo.adGroupId, adGroupInfo.campaignId, campaignIdTo, uid)

    @JvmStatic
    fun adGroupCopyConfig(
        clientInfo: ClientInfo,
        adGroupId: Long,
        campaignId: Long,
        uid: Long,
    ): CopyConfig<AdGroup, Long> =
        adGroupCopyConfig(clientInfo, adGroupId, campaignId, campaignId, uid)

    @JvmStatic
    fun adGroupCopyConfig(
        clientInfo: ClientInfo,
        adGroupId: Long,
        campaignIdFrom: Long,
        campaignIdTo: Long,
        uid: Long,
    ): CopyConfig<AdGroup, Long> =
        adGroupCopyConfig(clientInfo, listOf(adGroupId), campaignIdFrom, campaignIdTo, uid)

    @JvmStatic
    fun adGroupCopyConfig(
        clientInfo: ClientInfo,
        adGroupIds: List<Long>,
        campaignId: Long,
        uid: Long,
    ): CopyConfig<AdGroup, Long> =
        adGroupCopyConfig(clientInfo, adGroupIds, campaignId, campaignId, uid)

    @JvmStatic
    fun adGroupCopyConfig(
        clientInfo: ClientInfo,
        adGroupIds: List<Long>,
        campaignIdFrom: Long,
        campaignIdTo: Long,
        uid: Long,
    ): CopyConfig<AdGroup, Long> {
        return CopyConfigBuilder(clientInfo.clientId!!, clientInfo.clientId!!, uid, AdGroup::class.java, adGroupIds)
            .withParentIdMapping(BaseCampaign::class.java, campaignIdFrom, campaignIdTo)
            .build()
    }

    @JvmStatic
    fun adGroupBetweenClientsCopyConfig(
        clientInfoFrom: ClientInfo,
        clientInfoTo: ClientInfo,
        adGroupId: Long,
        campaignIdFrom: Long,
        campaignIdTo: Long,
        uid: Long,
    ): CopyConfig<AdGroup, Long> {
        return CopyConfigBuilder(
            clientInfoFrom.clientId!!, clientInfoTo.clientId!!, uid,
            AdGroup::class.java, listOf(adGroupId),
        )
            .withParentIdMapping(BaseCampaign::class.java, campaignIdFrom, campaignIdTo)
            .build()
    }

    @JvmStatic
    fun campaignCopyConfig(
        clientInfo: ClientInfo,
        campaignInfo: CampaignInfo,
        uid: Long,
    ): CopyConfig<BaseCampaign, Long> =
        campaignCopyConfig(clientInfo, campaignInfo.campaignId, uid)

    @JvmStatic
    fun campaignCopyConfig(clientInfo: ClientInfo, campaignId: Long, uid: Long): CopyConfig<BaseCampaign, Long> {
        return CopyConfig(
            clientInfo.clientId!!, clientInfo.clientId!!, uid,
            BaseCampaign::class.java, listOf(campaignId),
        )
    }

    @JvmStatic
    fun campaignCopyConfig(
        clientInfo: ClientInfo,
        campaignId: Long,
        uid: Long,
        flags: CopyCampaignFlags,
    ): CopyConfig<BaseCampaign, Long> {
        return CopyConfig(
            clientInfo.clientId!!, clientInfo.clientId!!, uid,
            BaseCampaign::class.java, listOf(campaignId), flags,
        )
    }

    @JvmStatic
    fun campaignsBetweenClientsCopyConfig(
        clientInfoFrom: ClientInfo,
        clientInfoTo: ClientInfo,
        campaignId: Long,
        uid: Long,
    ): CopyConfig<BaseCampaign, Long> =
        campaignsBetweenClientsCopyConfig(clientInfoFrom, clientInfoTo, listOf(campaignId), uid)

    @JvmStatic
    fun campaignsBetweenClientsCopyConfig(
        clientInfoFrom: ClientInfo, clientInfoTo: ClientInfo, campaignIds: List<Long>, uid: Long,
    ): CopyConfig<BaseCampaign, Long> {
        return CopyConfig(
            clientInfoFrom.clientId!!, clientInfoTo.clientId!!, uid,
            BaseCampaign::class.java, campaignIds,
        )
    }

    fun <T : Entity<KeyT>, KeyT> copyValidEntity(
        entityClass: Class<T>,
        copyOperation: CopyOperation<*, *>,
        allowWarnings: Boolean = false,
    ): List<KeyT> {
        val result: CopyResult<*> = copyOperation.copy()
        assumeCopyResultIsSuccessful(result, allowWarnings = allowWarnings)
        return result.getEntityMappings(entityClass).values.toList()
    }

    fun <KeyT> assumeCopyResultIsSuccessful(result: CopyResult<KeyT>, allowWarnings: Boolean = false) {
        val massResult: MassResult<KeyT> = result.massResult

        softly {
            assertThat(massResult)
                .describedAs("Copy operation is not successful")
                .`is`(matchedBy(isFullySuccessful<Long>()))

            val errorsDescription = massResult.validationResult.flattenErrors()
                .joinToString("\n\t") { it.toString() }

            assertThat(massResult.validationResult.hasAnyErrors())
                .describedAs("Unexpected errors:\n\t$errorsDescription")
                .isFalse

            if (!allowWarnings) {
                val warningsDescription = massResult.validationResult.flattenWarnings()
                    .joinToString("\n\t") { it.toString() }

                assertThat(massResult.validationResult.hasAnyWarnings())
                    .describedAs("Unexpected warnings:\n\t$warningsDescription")
                    .isFalse
            }
        }
    }

    /**
     * Сравнивает два списка, на совпадение элементов без учета порядка.
     */
    @JvmStatic
    fun <T> assertListAreEqualsIgnoringOrder(
        softAssertions: SoftAssertions,
        actual: List<T>,
        expected: List<T>,
        strategy: RecursiveComparisonConfiguration,
    ) {
        // Если в массивах с целями по одному элементу, то сравним эти элементы, так вывод теста богаче
        if (actual.size == 1 && expected.size == 1) {
            softAssertions.assertThat(actual[0])
                .usingComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal::class.java)
                .usingRecursiveComparison(strategy)
                .isEqualTo(expected[0])
        } else {
            softAssertions.assertThat(actual)
                .usingComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal::class.java)
                .usingRecursiveFieldByFieldElementComparator(strategy)
                .containsExactlyInAnyOrderElementsOf(expected)
        }
    }
}
