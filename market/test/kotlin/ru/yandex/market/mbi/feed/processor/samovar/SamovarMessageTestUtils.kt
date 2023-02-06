package ru.yandex.market.mbi.feed.processor.samovar

import Market.DataCamp.API.UpdateTask
import Market.DataCamp.DataCampOfferMeta
import NKwYT.Queries
import NZoraPb.Statuscodes
import com.google.protobuf.Int64Value
import ru.yandex.common.util.IOUtils
import ru.yandex.common.util.application.EnvironmentType
import ru.yandex.kikimr.persqueue.compression.CompressionCodec
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta
import ru.yandex.market.core.campaign.model.CampaignType
import ru.yandex.market.yt.samovar.SamovarContextOuterClass
import ru.yandex.market.yt.samovar.SamovarContextOuterClass.FeedInfo
import ru.yandex.market.yt.samovar.SamovarContextOuterClass.ValidationFeedInfo
import java.time.Instant

/**
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
fun shopsDatBuilder(
    isDiscountsEnabled: Boolean = true,
    color: DataCampOfferMeta.MarketColor = DataCampOfferMeta.MarketColor.BLUE,
    url: String = "http://ya.ru",
    isMock: Boolean = false,
    localRegionTzOffset: Int = 10800
): UpdateTask.ShopsDatParameters {
    return UpdateTask.ShopsDatParameters.newBuilder()
        .setIsDiscountsEnabled(isDiscountsEnabled)
        .setColor(color)
        .setUrl(url)
        .setIsMock(isMock)
        .setLocalRegionTzOffset(localRegionTzOffset)
        .build()
}

fun validationFeedInfoBuilder(
    validationFeedId: Long = 100,
    campaignType: CampaignType? = null,
    partnerId: Long? = null
): ValidationFeedInfo {
    return ValidationFeedInfo.newBuilder().apply {
        campaignType?.let { this.campaignType = it.id }
        partnerId?.let { this.partnerId = it }
        this.validationId = validationFeedId
    }.build()
}

fun feedInfoBuilder(
    campaignType: CampaignType = CampaignType.SUPPLIER,
    shopId: Long = 0,
    businessId: Long = 0,
    feedId: Long = 0,
    url: String = "http://ya.ru",
    warehouses: List<FeedInfo.WarehouseInfo> = listOf(),
    feedType: FeedInfo.FeedType? = null,
    shopsDat: UpdateTask.ShopsDatParameters = shopsDatBuilder(),
    forcedPeriodMinutes: Long? = null
): FeedInfo {
    val builder = FeedInfo.newBuilder()
        .setUrl(url)
        .setFeedId(feedId)
        .setShopId(shopId)
        .setBusinessId(businessId)
        .setCampaignType(campaignType.id)
        .setFeedType(
            feedType
                ?: if (CampaignType.SUPPLIER == campaignType) FeedInfo.FeedType.PRICES else FeedInfo.FeedType.ASSORTMENT
        )
        .addAllWarehouses(warehouses)
        .setShopsDatParameters(shopsDat)
        .setIsPartnerInterface(true)

    forcedPeriodMinutes?.let { builder.setForcedPeriodMinutes(Int64Value.of(it)) }

    return builder.build()
}

fun itemBuilder(
    forceRefresh: Int? = null,
    environment: EnvironmentType = EnvironmentType.DEVELOPMENT,
    numberOfParts: Int = 1,
    mdsKeys: String = "123/asdf|",
    url: String = "http://ya.ru",
    httpCode: Int = 200,
    zoraCode: Int = Statuscodes.EZoraStatus.ZS_OK_VALUE,
    fetchCode: Int = Statuscodes.EFetchStatus.FS_OK_VALUE,
    lastAccess: Long = Instant.now().epochSecond,
    crc32: Int = 0,
    feedInfos: List<FeedInfo> = listOf(),
    validations: List<ValidationFeedInfo> = listOf(),
): Queries.TMarketFeedsItem {
    val contextBuilder = SamovarContextOuterClass.SamovarContext.newBuilder()
        .setEnvironment(environment.value)
        .addAllFeeds(feedInfos)
        .addAllValidationFeeds(validations)
    if (forceRefresh != null) {
        contextBuilder.forceRefreshStart = Int64Value.of(forceRefresh.toLong())
    }
    val context = contextBuilder.build()
    val originalBld = Queries.TMarketFeedsItem.newBuilder()
        .setUrl(url)
        .setMdsKeys(mdsKeys)
        .setHttpCode(httpCode.toLong())
        .setZoraStatus(zoraCode)
        .setFetchStatus(fetchCode)
        .setLastAccess(lastAccess)
        .setNumberOfParts(numberOfParts)
        .setCrc32(crc32)
    return originalBld.setContext(context.toByteString()).build()
}

fun messageBatchBuilder(
    meta: MessageMeta = MessageMeta("test".toByteArray(), 0, 0, 0, "::1", CompressionCodec.RAW, emptyMap()),
    items: List<Queries.TMarketFeedsItem> = listOf()
): MessageBatch {
    val zipped = items.map { MessageData(IOUtils.zip(it.toByteArray()), 0, meta) }
    return MessageBatch("topic", 1, zipped)
}
