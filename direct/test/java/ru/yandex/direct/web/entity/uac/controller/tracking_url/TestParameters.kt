package ru.yandex.direct.web.entity.uac.controller.tracking_url

val APP_METRICA_PARAMS: List<Map<String, Any?>> = listOf(
    mapOf(
        "os_type" to "ios",
        "value" to "{ios_ifa}",
        "name" to "ios_ifa",
        "description" to null,
        "required" to true
    ),
    mapOf(
        "os_type" to "android",
        "value" to "{google_aid}",
        "name" to "google_aid",
        "description" to null,
        "required" to true
    ),
    mapOf(
        "os_type" to null,
        "value" to "{logid}",
        "name" to "click_id",
        "description" to null,
        "required" to true
    ),
    mapOf(
        "os_type" to null,
        "value" to "{campaign_id}_{campaign_name_lat}",
        "name" to "c",
        "description" to "Название кампании (латиницей)",
        "required" to false
    ),
    mapOf(
        "os_type" to null,
        "value" to "{gbid}",
        "name" to "adgroup_id",
        "description" to "Идентификатор группы объявлений",
        "required" to false
    ),
    mapOf(
        "os_type" to null,
        "value" to "{ad_id}",
        "name" to "creative_id",
        "description" to "Идентификатор объявления",
        "required" to false
    ),
    mapOf(
        "os_type" to null,
        "value" to "{phrase_id}{retargeting_id}_{keyword}{adtarget_name}",
        "name" to "criteria",
        "description" to "Ключевое слово или условие ретаргетинга",
        "required" to false
    ),
    mapOf(
        "os_type" to null,
        "value" to "{source_type}_{source}",
        "name" to "site_id",
        "description" to "Сайт сети, где был показ (место показа)",
        "required" to false
    )
)

val APPSFLYER_PARAMS: List<Map<String, Any?>> = listOf(
    mapOf(
        "name" to "idfa",
        "os_type" to "ios",
        "description" to null,
        "value" to "{ios_ifa}",
        "required" to true
    ),
    mapOf(
        "name" to "advertising_id",
        "os_type" to "android",
        "description" to null,
        "value" to "{google_aid}",
        "required" to true
    ),
    mapOf(
        "name" to "oaid",
        "os_type" to "android",
        "description" to null,
        "value" to "{oaid}",
        "required" to true
    ),
    mapOf(
        "name" to "pid",
        "os_type" to null,
        "description" to null,
        "value" to "yandexdirect_int",
        "required" to true
    ),
    mapOf(
        "name" to "clickid",
        "os_type" to null,
        "description" to null,
        "value" to "{logid}",
        "required" to true
    ),
    mapOf(
        "description" to null,
        "name" to "is_retargeting",
        "os_type" to null,
        "required" to false,
        "value" to "true"
    ),
    mapOf(
        "name" to "c",
        "os_type" to null,
        "description" to "Название кампании",
        "value" to null,
        "required" to true
    ),
    mapOf(
        "name" to "af_c_id",
        "os_type" to null,
        "description" to "Идентификатор кампании",
        "value" to "{campaign_id}",
        "required" to true
    ),
    mapOf(
        "name" to "af_adset_id",
        "os_type" to null,
        "description" to "Идентификатор группы объявлений",
        "value" to "{gbid}",
        "required" to false
    ),
    mapOf(
        "name" to "af_ad_id",
        "os_type" to null,
        "description" to "Идентификатор объявления",
        "value" to "{ad_id}",
        "required" to false
    ),
    mapOf(
        "name" to "af_keywords",
        "os_type" to null,
        "description" to "Ключевые слова",
        "value" to "{phrase_id}{retargeting_id}_{keyword}{adtarget_name}",
        "required" to false
    ),
    mapOf(
        "name" to "af_siteid",
        "os_type" to null,
        "description" to "Сайт сети, где был показ (место показа)",
        "value" to "{source_type}_{source}",
        "required" to false
    )
)

val APPSFLYER_IMPRESSION_PARAMS: List<Map<String, Any?>> = APPSFLYER_PARAMS + listOf(
    mapOf(
        "name" to "af_ip",
        "os_type" to null,
        "description" to null,
        "value" to "{client_ip}",
        "required" to true
    ),
    mapOf(
        "name" to "af_ua",
        "os_type" to null,
        "description" to null,
        "value" to "{user_agent}",
        "required" to true
    ),
    mapOf(
        "name" to "af_lang",
        "os_type" to null,
        "description" to null,
        "value" to "{device_lang}",
        "required" to true
    )
)

val FLURRY_PARAMS: List<Map<String, Any?>> = listOf(
    mapOf(
        "value" to "{ios_ifa}",
        "name" to "ios_ifa",
        "description" to null,
        "os_type" to "ios",
        "required" to true
    ),
    mapOf(
        "value" to "{ios_ifa}",
        "name" to "ios_idfa",
        "description" to null,
        "os_type" to "ios",
        "required" to true
    ),
    mapOf(
        "value" to "{google_aid}",
        "name" to "google_aid",
        "description" to null,
        "os_type" to "android",
        "required" to true
    ),
    mapOf(
        "value" to "{google_aid}",
        "name" to "adid",
        "description" to null,
        "os_type" to "android",
        "required" to true
    ),
    mapOf(
        "value" to "{logid}",
        "name" to "reqid",
        "description" to null,
        "os_type" to null,
        "required" to true
    )
)

val MY_TRACKER_PARAMS: List<Map<String, Any?>> = listOf(
    mapOf(
        "value" to "{ios_ifa}",
        "required" to true,
        "description" to null,
        "name" to "mt_idfa",
        "os_type" to "ios"
    ),
    mapOf(
        "value" to "{google_aid}",
        "required" to true,
        "description" to null,
        "name" to "mt_gaid",
        "os_type" to "android"
    ),
    mapOf(
        "value" to "{logid}",
        "required" to true,
        "description" to null,
        "name" to "clickId",
        "os_type" to null
    ),
    mapOf(
        "value" to "{logid}",
        "required" to true,
        "description" to null,
        "name" to "regid",
        "os_type" to null
    )
)

val SINGULAR_PARAMS: List<Map<String, Any?>> = listOf(
    mapOf(
        "value" to "{ios_ifa}",
        "required" to true,
        "description" to null,
        "name" to "idfa",
        "os_type" to "ios"
    ),
    mapOf(
        "value" to "{google_aid}",
        "required" to true,
        "description" to null,
        "name" to "aifa",
        "os_type" to "android"
    ),
    mapOf(
        "value" to "{oaid}",
        "required" to true,
        "description" to null,
        "name" to "oaid",
        "os_type" to "android"
    ),
    mapOf(
        "value" to "{logid}",
        "required" to true,
        "description" to null,
        "name" to "cl",
        "os_type" to null
    )
)

val TUNE_PARAMS: List<Map<String, Any?>> = listOf(
    mapOf(
        "value" to "{ios_ifa}",
        "required" to true,
        "description" to null,
        "name" to "ios_ifa",
        "os_type" to "ios"
    ),
    mapOf(
        "value" to "{google_aid}",
        "required" to true,
        "description" to null,
        "name" to "google_aid",
        "os_type" to "android"
    ),
    mapOf(
        "value" to "{logid}",
        "required" to true,
        "description" to null,
        "name" to "publisher_ref_id",
        "os_type" to null
    )
)

val ADJUST_PARAMS: List<Map<String, Any?>> = listOf(
    mapOf(
        "description" to null,
        "name" to "idfa",
        "os_type" to "ios",
        "required" to true,
        "value" to "{ios_ifa}"
    ),
    mapOf(
        "description" to null,
        "name" to "gps_adid",
        "os_type" to "android",
        "required" to true,
        "value" to "{google_aid}"
    ),
    mapOf(
        "description" to null,
        "name" to "oaid",
        "os_type" to "android",
        "required" to true,
        "value" to "{oaid}"
    ),
    mapOf(
        "description" to null,
        "name" to "ya_click_id",
        "os_type" to null,
        "required" to true,
        "value" to "{logid}"
    ),
    mapOf(
        "description" to "Название кампании (латиницей)",
        "name" to "campaign",
        "os_type" to null,
        "required" to false,
        "value" to "{campaign_id}_{campaign_name_lat}"
    ),
    mapOf(
        "description" to "Идентификатор группы объявлений",
        "name" to "adgroup",
        "os_type" to null,
        "required" to false,
        "value" to "{gbid}"
    ),
    mapOf(
        "description" to "Объявление, ключевое слово или условие ретаргетинга",
        "name" to "creative",
        "os_type" to null,
        "required" to false,
        "value" to "{ad_id}_{phrase_id}{retargeting_id}_{keyword}{adtarget_name}"
    )
)

val ADJUST_IMPRESSION_PARAMS: List<Map<String, Any?>> = ADJUST_PARAMS + listOf(
    mapOf(
        "name" to "user_agent",
        "os_type" to null,
        "description" to null,
        "value" to "{user_agent}",
        "required" to true
    ),
    mapOf(
        "name" to "ip_address",
        "os_type" to null,
        "description" to null,
        "value" to "{client_ip}",
        "required" to true
    ),
    mapOf(
        "name" to "language",
        "os_type" to null,
        "description" to null,
        "value" to "{device_lang}",
        "required" to true
    )
)


val KOCHAVA_PARAMS: List<Map<String, Any?>> = listOf(
    mapOf(
        "value" to "1516",
        "required" to true,
        "description" to null,
        "name" to "network_id",
        "os_type" to "ios"
    ),
    mapOf(
        "value" to "idfa",
        "required" to true,
        "description" to null,
        "name" to "device_id_type",
        "os_type" to "ios"
    ),
    mapOf(
        "value" to "{ios_ifa}",
        "required" to true,
        "description" to null,
        "name" to "idfa",
        "os_type" to "ios"
    ),
    mapOf(
        "value" to "{ios_ifa}",
        "required" to true,
        "description" to null,
        "name" to "device_id",
        "os_type" to "ios"
    ),
    mapOf(
        "value" to "{ios_ifa}",
        "required" to true,
        "description" to null,
        "name" to "ios_idfa",
        "os_type" to "ios"
    ),
    mapOf(
        "value" to "1517",
        "required" to true,
        "description" to null,
        "name" to "network_id",
        "os_type" to "android"
    ),
    mapOf(
        "value" to "adid",
        "required" to true,
        "description" to null,
        "name" to "device_id_type",
        "os_type" to "android"
    ),
    mapOf(
        "value" to "{google_aid}",
        "required" to true,
        "description" to null,
        "name" to "adid",
        "os_type" to "android"
    ),
    mapOf(
        "value" to "{google_aid}",
        "required" to true,
        "description" to null,
        "name" to "device_id",
        "os_type" to "android"
    ),
    mapOf(
        "value" to "{android_id}",
        "required" to true,
        "description" to null,
        "name" to "android_id",
        "os_type" to "android"
    ),
    mapOf(
        "value" to "{source_type}_{source}",
        "required" to false,
        "description" to "Сайт сети, где был показ (место показа)",
        "name" to "site_id",
        "os_type" to null
    ),
    mapOf(
        "value" to "{ad_id}_{phrase_id}{retargeting_id}_{keyword}{adtarget_name}",
        "required" to false,
        "description" to "Объявление, ключевое слово или условие ретаргетинга",
        "name" to "creative_id",
        "os_type" to null
    )
)

val KOCHAVA_TRACKING_PARAMS: List<Map<String, Any?>> = KOCHAVA_PARAMS + listOf(
    mapOf(
        "name" to "click_id",
        "os_type" to null,
        "description" to null,
        "value" to "{logid}",
        "required" to true
    )
)

val KOCHAVA_IMPRESSION_PARAMS: List<Map<String, Any?>> = KOCHAVA_PARAMS + listOf(
    mapOf(
        "name" to "impression_id",
        "os_type" to null,
        "description" to null,
        "value" to "{logid}",
        "required" to true
    )
)

val BRANCH_PARAMS: List<Map<String, Any?>> = listOf(
    mapOf(
        "value" to "{ios_ifa}",
        "required" to true,
        "description" to null,
        "name" to "%24idfa",
        "os_type" to "ios"
    ),
    mapOf(
        "value" to "{google_aid}",
        "required" to true,
        "description" to null,
        "name" to "%24aaid",
        "os_type" to "android"
    ),
    mapOf(
        "name" to "~click_id",
        "value" to "{logid}",
        "required" to true,
        "os_type" to null,
        "description" to null
    ),
    mapOf(
        "name" to "%243p",
        "value" to "a_yandex_direct",
        "required" to true,
        "os_type" to null,
        "description" to null
    )
)
