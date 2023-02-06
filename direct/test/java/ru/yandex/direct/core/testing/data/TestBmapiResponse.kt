package ru.yandex.direct.core.testing.data

import ru.yandex.direct.bmapi.client.model.BmApiError
import ru.yandex.direct.bmapi.client.model.BmApiErrorCode
import ru.yandex.direct.bmapi.client.model.BmApiFeedInfoResponse
import ru.yandex.direct.bmapi.client.model.Category
import ru.yandex.direct.core.entity.feed.model.FeedOfferExamples
import ru.yandex.direct.core.entity.feed.model.FeedType
import ru.yandex.direct.utils.fromJson

val INITIAL_OFFERS_AMOUNT = 5
val SECONDARY_OFFERS_AMOUNT = 3
val INITIAl_DOMAIN = "market.yandex.ru"
val NEW_DOMAIN = "market.yandex.com"
val VENDOR_1 = "BigStory"
val VENDOR_2 = "MediumStory"
val VENDOR_3 = "LittleStory"
val PARENT_CATEGORY_ID = "100000"
val PARENT_CATEGORY = "Parent"
val CATEGORY_ID_1 = "100001"
val CATEGORY_1 = "Category 1"
val CATEGORY_ID_2 = "100002"
val CATEGORY_2 = "Category 2"
val CATEGORY_ID_3 = "100003"
val CATEGORY_3 = "Category 3"
val OFFERS_EXAMPLES =
    "{\"data_params\":{\"101598154764\":{\"target_url\":\"https://market.yandex.ru/store--bigstory/product/1663220943?businessId=3994054&sku=101598154764\",\"price\":{\"current\":\"1118\",\"old\":\"1800\"},\"text\":{\"description_for_direct\":null,\"params_for_direct\":null,\"name\":\"Скретч-постер \\\"150 лучших фильмов\\\" от BigStory\",\"currency_iso_code\":\"RUB\"},\"click_url\":{\"text_name\":\"https://market.yandex.ru/store--bigstory/product/1663220943?businessId=3994054&sku=101598154764\",\"text_body\":\"https://market.yandex.ru/store--bigstory/product/1663220943?businessId=3994054&sku=101598154764\",\"price_current\":\"https://market.yandex.ru/store--bigstory/product/1663220943?businessId=3994054&sku=101598154764\",\"price_old\":\"https://market.yandex.ru/store--bigstory/product/1663220943?businessId=3994054&sku=101598154764\",\"image_small\":\"https://market.yandex.ru/store--bigstory/product/1663220943?businessId=3994054&sku=101598154764\",\"image_orig\":\"https://market.yandex.ru/store--bigstory/product/1663220943?businessId=3994054&sku=101598154764\",\"image_big\":\"https://market.yandex.ru/store--bigstory/product/1663220943?businessId=3994054&sku=101598154764\",\"image_huge\":\"https://market.yandex.ru/store--bigstory/product/1663220943?businessId=3994054&sku=101598154764\"},\"image\":{\"small\":{\"width\":100,\"height\":100,\"url\":\"//avatars.mds.yandex.net/get-yabs_performance/1575372/2a0000017f3a6bc62a5553f54e1057824e14/small\",\"smart-centers\":{\"16:5\":{\"x\":0,\"y\":57,\"w\":100,\"h\":31},\"4:3\":{\"x\":0,\"y\":14,\"w\":100,\"h\":75},\"16:9\":{\"x\":0,\"y\":35,\"w\":100,\"h\":56},\"1:1\":{\"x\":0,\"y\":0,\"w\":100,\"h\":100},\"3:4\":{\"x\":13,\"y\":0,\"w\":75,\"h\":100}}},\"orig\":{\"width\":500,\"height\":500,\"url\":\"//avatars.mds.yandex.net/get-yabs_performance/1575372/2a0000017f3a6bc62a5553f54e1057824e14/orig\",\"smart-centers\":null},\"big\":{\"width\":200,\"height\":200,\"url\":\"//avatars.mds.yandex.net/get-yabs_performance/1575372/2a0000017f3a6bc62a5553f54e1057824e14/big\",\"smart-centers\":{\"16:5\":{\"x\":0,\"y\":115,\"w\":200,\"h\":62},\"4:3\":{\"x\":0,\"y\":27,\"w\":200,\"h\":150},\"16:9\":{\"x\":0,\"y\":70,\"w\":200,\"h\":112},\"1:1\":{\"x\":0,\"y\":0,\"w\":200,\"h\":200},\"3:4\":{\"x\":25,\"y\":0,\"w\":150,\"h\":200}}},\"huge\":{\"width\":500,\"height\":500,\"url\":\"//avatars.mds.yandex.net/get-yabs_performance/1575372/2a0000017f3a6bc62a5553f54e1057824e14/huge\",\"smart-centers\":{\"16:5\":{\"x\":0,\"y\":287,\"w\":499,\"h\":156},\"4:3\":{\"x\":0,\"y\":68,\"w\":499,\"h\":374},\"16:9\":{\"x\":0,\"y\":176,\"w\":499,\"h\":281},\"1:1\":{\"x\":0,\"y\":0,\"w\":499,\"h\":499},\"3:4\":{\"x\":63,\"y\":0,\"w\":374,\"h\":499}}}},\"update_date\":null},\"101598152840\":{\"target_url\":\"https://market.yandex.ru/store--bigstory/product/1663223461?businessId=3994054&sku=101598152840\",\"price\":{\"current\":\"1118\",\"old\":\"1800\"},\"text\":{\"description_for_direct\":null,\"params_for_direct\":null,\"name\":\"Скретч-постер \\\"фильмы, сериалы, книги\\\" от BigStory\",\"currency_iso_code\":\"RUB\"},\"click_url\":{\"text_name\":\"https://market.yandex.ru/store--bigstory/product/1663223461?businessId=3994054&sku=101598152840\",\"text_body\":\"https://market.yandex.ru/store--bigstory/product/1663223461?businessId=3994054&sku=101598152840\",\"price_current\":\"https://market.yandex.ru/store--bigstory/product/1663223461?businessId=3994054&sku=101598152840\",\"price_old\":\"https://market.yandex.ru/store--bigstory/product/1663223461?businessId=3994054&sku=101598152840\",\"image_small\":\"https://market.yandex.ru/store--bigstory/product/1663223461?businessId=3994054&sku=101598152840\",\"image_orig\":\"https://market.yandex.ru/store--bigstory/product/1663223461?businessId=3994054&sku=101598152840\",\"image_big\":\"https://market.yandex.ru/store--bigstory/product/1663223461?businessId=3994054&sku=101598152840\",\"image_huge\":\"https://market.yandex.ru/store--bigstory/product/1663223461?businessId=3994054&sku=101598152840\"},\"image\":{\"small\":{\"width\":100,\"height\":100,\"url\":\"//avatars.mds.yandex.net/get-yabs_performance/1636023/2a0000017f3a6bd4921b100095d3d0489a4e/small\",\"smart-centers\":{\"16:5\":{\"x\":0,\"y\":57,\"w\":100,\"h\":31},\"4:3\":{\"x\":0,\"y\":11,\"w\":100,\"h\":75},\"16:9\":{\"x\":0,\"y\":34,\"w\":100,\"h\":56},\"1:1\":{\"x\":0,\"y\":0,\"w\":100,\"h\":100},\"3:4\":{\"x\":13,\"y\":0,\"w\":75,\"h\":100}}},\"orig\":{\"width\":500,\"height\":500,\"url\":\"//avatars.mds.yandex.net/get-yabs_performance/1636023/2a0000017f3a6bd4921b100095d3d0489a4e/orig\",\"smart-centers\":null},\"big\":{\"width\":200,\"height\":200,\"url\":\"//avatars.mds.yandex.net/get-yabs_performance/1636023/2a0000017f3a6bd4921b100095d3d0489a4e/big\",\"smart-centers\":{\"16:5\":{\"x\":0,\"y\":115,\"w\":200,\"h\":62},\"4:3\":{\"x\":0,\"y\":23,\"w\":200,\"h\":150},\"16:9\":{\"x\":0,\"y\":68,\"w\":200,\"h\":112},\"1:1\":{\"x\":0,\"y\":0,\"w\":200,\"h\":200},\"3:4\":{\"x\":25,\"y\":0,\"w\":150,\"h\":200}}},\"huge\":{\"width\":500,\"height\":500,\"url\":\"//avatars.mds.yandex.net/get-yabs_performance/1636023/2a0000017f3a6bd4921b100095d3d0489a4e/huge\",\"smart-centers\":{\"16:5\":{\"x\":0,\"y\":287,\"w\":499,\"h\":156},\"4:3\":{\"x\":0,\"y\":57,\"w\":499,\"h\":374},\"16:9\":{\"x\":0,\"y\":170,\"w\":499,\"h\":281},\"1:1\":{\"x\":0,\"y\":0,\"w\":499,\"h\":499},\"3:4\":{\"x\":63,\"y\":0,\"w\":374,\"h\":499}}}},\"update_date\":null}}}"
val FEED_TYPE = FeedType.YANDEX_MARKET
val ANOTHER_FEED_TYPE = FeedType.GOOGLE_MERCHANT
val UNKNOWN_FEED_TYPE = "KindaType"
val BMAPI_ERROR_MESSAGE = "${BmApiErrorCode.BL_ERROR_XML_FATAL.code}: BmApiErrorCode.BL_ERROR_XML_FATAL"
val BMAPI_ERROR_CODE = BmApiErrorCode.BL_ERROR_XML_FATAL

val INITIAL_RESPONSE =
    BmApiFeedInfoResponse(
        mapOf(
            CATEGORY_ID_1 to INITIAL_OFFERS_AMOUNT - 1,
            CATEGORY_ID_2 to 1
        ),
        emptyList(),
        emptyList(),
        FEED_TYPE.typedValue,
        INITIAL_OFFERS_AMOUNT.toLong(),
        mapOf(INITIAl_DOMAIN to INITIAL_OFFERS_AMOUNT),
        listOf(
            Category(PARENT_CATEGORY_ID, "0", PARENT_CATEGORY),
            Category(CATEGORY_ID_1, PARENT_CATEGORY_ID, CATEGORY_1),
            Category(CATEGORY_ID_2, CATEGORY_ID_1, CATEGORY_2)
        ),
        mapOf(
            VENDOR_1 to 1,
            VENDOR_2 to INITIAL_OFFERS_AMOUNT - 1,
        ),
        fromJson<FeedOfferExamples>(OFFERS_EXAMPLES)
    )

val SECOND_RESPONSE =
    BmApiFeedInfoResponse(
        mapOf(
            CATEGORY_ID_2 to 1,
            CATEGORY_ID_3 to SECONDARY_OFFERS_AMOUNT - 1
        ),
        emptyList(),
        emptyList(),
        FEED_TYPE.typedValue,
        SECONDARY_OFFERS_AMOUNT.toLong(),
        mapOf(NEW_DOMAIN to SECONDARY_OFFERS_AMOUNT),
        listOf(
            Category(PARENT_CATEGORY_ID, "0", PARENT_CATEGORY),
            Category(CATEGORY_ID_2, PARENT_CATEGORY_ID, CATEGORY_2),
            Category(CATEGORY_ID_3, PARENT_CATEGORY_ID, CATEGORY_3)
        ),
        mapOf(VENDOR_3 to SECONDARY_OFFERS_AMOUNT),
        fromJson<FeedOfferExamples>(OFFERS_EXAMPLES)
    )

val UNKNOWN_TYPE_RESPONSE =
    BmApiFeedInfoResponse(
        mapOf(
            CATEGORY_ID_1 to INITIAL_OFFERS_AMOUNT - 1,
            CATEGORY_ID_2 to 1
        ),
        emptyList(),
        emptyList(),
        UNKNOWN_FEED_TYPE,
        INITIAL_OFFERS_AMOUNT.toLong(),
        mapOf(INITIAl_DOMAIN to INITIAL_OFFERS_AMOUNT),
        listOf(
            Category(PARENT_CATEGORY_ID, "0", PARENT_CATEGORY),
            Category(CATEGORY_ID_1, PARENT_CATEGORY_ID, CATEGORY_1),
            Category(CATEGORY_ID_2, CATEGORY_ID_1, CATEGORY_2)
        ),
        mapOf(
            VENDOR_1 to 1,
            VENDOR_2 to INITIAL_OFFERS_AMOUNT - 1,
        ),
        fromJson<FeedOfferExamples>(OFFERS_EXAMPLES)
    )

val ANOTHER_TYPE_RESPONSE =
    BmApiFeedInfoResponse(
        mapOf(
            CATEGORY_ID_1 to INITIAL_OFFERS_AMOUNT - 1,
            CATEGORY_ID_2 to 1
        ),
        emptyList(),
        emptyList(),
        ANOTHER_FEED_TYPE.typedValue,
        INITIAL_OFFERS_AMOUNT.toLong(),
        mapOf(INITIAl_DOMAIN to INITIAL_OFFERS_AMOUNT),
        listOf(
            Category(PARENT_CATEGORY_ID, "0", PARENT_CATEGORY),
            Category(CATEGORY_ID_1, PARENT_CATEGORY_ID, CATEGORY_1),
            Category(CATEGORY_ID_2, CATEGORY_ID_1, CATEGORY_2)
        ),
        mapOf(
            VENDOR_1 to 1,
            VENDOR_2 to INITIAL_OFFERS_AMOUNT - 1,
        ),
        fromJson<FeedOfferExamples>(OFFERS_EXAMPLES)
    )

val ERROR_RESPONSE =
    BmApiFeedInfoResponse(
        null,
        emptyList(),
        listOf(BmApiError(BMAPI_ERROR_CODE, BMAPI_ERROR_MESSAGE, null)),
        null,
        null,
        null,
        null,
        null,
        null
    )

