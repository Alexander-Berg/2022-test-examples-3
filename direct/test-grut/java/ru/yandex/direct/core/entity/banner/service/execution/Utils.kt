package ru.yandex.direct.core.entity.banner.service.execution

import org.hamcrest.Matchers.empty
import org.junit.Assume
import ru.yandex.direct.core.entity.StatusBsSynced
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate
import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate
import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId
import ru.yandex.direct.core.entity.banner.model.Language
import ru.yandex.direct.core.entity.banner.model.TextBanner
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory
import ru.yandex.direct.core.entity.banner.service.DatabaseMode
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.model.ModelChanges
import ru.yandex.direct.operation.Applicability
import java.time.LocalDateTime

fun createTextBanner(mysqlAdGroupInfo: AdGroupInfo): TextBanner {
    return TextBanner()
        .withTitle("title")
        .withBody("text")
        .withCampaignId(mysqlAdGroupInfo.campaignId)
        .withAdGroupId(mysqlAdGroupInfo.adGroupId)
        .withHref("http://yandex.ru/?click=test")
        .withGeoFlag(false)
        .withLanguage(Language.RU_)
        .withStatusModerate(BannerStatusModerate.NEW)
        .withStatusPostModerate(BannerStatusPostModerate.NEW)
        .withLastChange(LocalDateTime.now())
        .withStatusActive(true)
        .withStatusArchived(false)
        .withStatusBsSynced(StatusBsSynced.NO)
        .withStatusShow(true)
        .withStatusSitelinksModerate(BannerStatusSitelinksModerate.NEW)
        .withVcardStatusModerate(BannerVcardStatusModerate.NEW)
}

fun addBannerToMysqlAndGrut(
    bannersAddOperationFactory: BannersAddOperationFactory,
    banner: BannerWithAdGroupId,
    userInfo: UserInfo,
    vararg clientEnabledFeatures: FeatureName,
): Long {
    return addBannersToMysqlAndGrut(bannersAddOperationFactory, listOf(banner), userInfo, *clientEnabledFeatures)[0]
}

fun addBannersToMysqlAndGrut(
    bannersAddOperationFactory: BannersAddOperationFactory,
    banners: List<BannerWithAdGroupId>,
    userInfo: UserInfo,
    vararg clientEnabledFeatures: FeatureName,
): List<Long> {
    val bannersAddOperation = bannersAddOperationFactory.createAddOperation(
        Applicability.FULL,
        false,
        banners,
        userInfo.shard,
        userInfo.clientId,
        userInfo.uid,
        ModerationMode.DEFAULT,
        false,
        false,
        false,
        DatabaseMode.MYSQL_AND_GRUT,
        clientEnabledFeatures.map { it.getName() }.toSet()
    )
    val result = bannersAddOperation.prepareAndApply()
    Assume.assumeThat(result.validationResult.flattenErrors(), empty())
    return result.toResultList().map { it.result }
}

inline fun <reified B : BannerWithAdGroupId> updateBannerInMysqlAndGrut(
    bannersUpdateOperationFactory: BannersUpdateOperationFactory,
    modelChanges: ModelChanges<B>,
    userInfo: UserInfo,
) {
    val updateOperation = bannersUpdateOperationFactory.createUpdateOperation(
        Applicability.PARTIAL,
        false,
        ModerationMode.DEFAULT,
        listOf(modelChanges),
        userInfo.shard,
        userInfo.clientId,
        userInfo.uid,
        emptySet(),
        B::class.java,
        false,
        false,
        DatabaseMode.MYSQL_AND_GRUT,
        null
    )
    val updateResult = updateOperation.prepareAndApply()
    Assume.assumeThat(updateResult.validationResult.flattenErrors(), empty())
}
