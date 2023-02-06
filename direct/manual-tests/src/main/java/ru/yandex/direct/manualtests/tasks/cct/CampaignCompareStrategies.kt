package ru.yandex.direct.manualtests.tasks.cct

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import ru.yandex.direct.core.entity.addition.callout.model.Callout
import ru.yandex.direct.core.entity.adgroup.model.AdGroup
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId
import ru.yandex.direct.core.entity.bidmodifier.BidModifier
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicAdTarget
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSimple
import ru.yandex.direct.core.entity.image.model.BannerImageFormat
import ru.yandex.direct.core.entity.keyword.model.Keyword
import ru.yandex.direct.core.entity.mobileapp.model.MobileApp
import ru.yandex.direct.core.entity.performancefilter.model.PerformanceFilter
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch
import ru.yandex.direct.core.entity.retargeting.model.CampMetrikaGoal
import ru.yandex.direct.core.entity.retargeting.model.Retargeting
import ru.yandex.direct.core.entity.retargeting.model.RetargetingConditionBase
import ru.yandex.direct.core.entity.sitelink.model.SitelinkSet
import ru.yandex.direct.core.entity.vcard.model.Vcard
import ru.yandex.direct.model.Entity

object CampaignCompareStrategies {

    private val BID_MODIFIER_COMMON_FIELDS: Array<String> = arrayOf(
        "id", // id корректировок изменяется при копировании
        "campaignId", // корректировки принадлежат скопированной кампании
        "lastChange", // время копирования отличается от времени создания

        "videoAdjustment.id", // == id корректировок

        "desktopAdjustment.id", // == id корректировок

        "mobileAdjustment.id", // pkey коэффициентов
        "mobileAdjustment.lastChange", // время копирования корректировок отличается от времени создания

        "inventoryAdjustments.id", // pkey коэффициентов
        "inventoryAdjustments.lastChange", // время копирования корректировок отличается от времени создания

        "retargetingAdjustments.id", // pkey коэффициентов
        "retargetingAdjustments.lastChange", // время копирования корректировок отличается от времени создания

        "weatherAdjustments.id", // pkey коэффициентов
        "weatherAdjustments.lastChange", // время копирования корректировок отличается от времени создания

        "regionalAdjustments.id", // pkey коэффициентов
        "regionalAdjustments.lastChange", // время копирования корректировок отличается от времени создания

        "demographicsAdjustments.id", // pkey коэффициентов
        "demographicsAdjustments.lastChange", // время копирования корректировок отличается от времени создания
    )

    private val VCARD_COMMON_FIELDS: Array<String> = arrayOf(
        // Геопоиск может возвращать другие адреса
        // Либо возвращать старые адреса для визиток, которые сейчас не имеют адреса
        "autoPoint",
        "manualPoint",
        "pointType",
        "geoId",
        "precision",
        "addressId",
    )

    private val CAMPAIGN: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder().withIgnoredFields(
        "id", // id кампании изменяется при копировании
        "name", // в название кампании добавляется информация, что это копия
        "createTime", // время копирования отличается от времени создания
        "lastChange", // last_change равен времени копирования
        "copiedFrom", // copied_from становится равен старому cid
        "orderId", // у новой кампании всегда 0
        "statusModerate", // не копируем статусы модерации
        "statusPostModerate", // не копируем статусы модерации
        "href", // perl не копирует camp_additional_data
        "contextLimit", // сбрасываем ContextLimit для значений 254 при невалидной стратегии и 255
        "enableCpcHold", // сбрасывается в false для автобюджетных стратегий
        "rawMeaningfulGoals", // в старых кампаниях отсутствуют кавычки у value
        "favoriteForUids", // perl не копирует
        "minusKeywords", // дедупликация минус-фраз, разделение сложных слов на части
        "hasTitleSubstitution", // java сбрасывает для некоторых типов кампаний

        "timeTarget.originalTimeTarget", // затирается preset при записи
        "timeTarget.preset", // затирается preset при записи

        "strategyId", // создается новый при записи
        "strategy.strategyData.start", // Может обновиться при записи, если исходная стратегия "в прошлом"
        "strategy.strategyData.finish", // Может обновиться при записи, если исходная стратегия "в прошлом"
        "strategy.strategyData.lastUpdateTime", // Обновляется при записи

        "enableCheckPositionEvent", // TODO https://st.yandex-team.ru/DIRECT-157988#61e198391238052db406e1c3
        "allowedSsp", // TODO https://st.yandex-team.ru/DIRECT-157988#61e198391238052db406e1c3
        "enableSendAccountNews", // TODO https://st.yandex-team.ru/DIRECT-157988#61e198391238052db406e1c3
        "statusShow", // TODO https://st.yandex-team.ru/DIRECT-157988#61e198391238052db406e1c3
        "fio", // TODO https://st.yandex-team.ru/DIRECT-157988#61e57672a816ab4a95f784f9
        "email", // TODO DIRECT-161220
        "startDate", // TODO https://st.yandex-team.ru/DIRECT-157988#61e57672a816ab4a95f784f9
        "endDate", // TODO https://st.yandex-team.ru/DIRECT-157988#61e57672a816ab4a95f784f9
        "dayBudgetLastChange", // TODO https://st.yandex-team.ru/DIRECT-157988#61e57672a816ab4a95f784f9

        // Дублируются исключения для bidModifiers, потому что для кампании они внутри модели кампании, а не отдельной сущностью
        *BID_MODIFIER_COMMON_FIELDS.map {
            "bidModifiers.$it"
        }.toTypedArray(),

        // Дублируются исключения для contactInfo, потому что для кампании они внутри модели кампании, а не отдельной сущностью
        *VCARD_COMMON_FIELDS.map {
            "contactInfo.$it"
        }.toTypedArray(),
    ).withIgnoredCollectionOrderInFields(
        "metrikaCounters", // порядок теряется, не разбирался где

        "bidModifiers", // порядок теряется, нету явной сортировки в чтении/записи
        "bidModifiers.retargetingAdjustments", // порядок коэффициентов произвольный
    ).build()

    private val AD_GROUP: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder().withIgnoredFields(
        "id", // id группы изменяется при копировании
        "campaignId", // группа принадлежит скопированной кампании
        "lastChange", // время копирования отличается от времени создания
        "statusModerate", // группа модерируется
        "statusPostModerate", // группа модерируется
        "statusBsSynced", // группа заново отправляется в БК
        "tags", // сбрасываем при копировании между кампаниями

        "usersSegments.adGroupId", // Идентификатор меняется при копировании
        "usersSegments.timeCreated", // Устанавливается в момент создания новой кампании
        "usersSegments.lastSuccessUpdateTime", // Устанавливается в момент создания новой кампании

        "name", // TODO: в название группы добавляется информация, что это копия
        "priorityId", // TODO: DIRECT-160239
        "contextId", // TODO: DIRECT-160239
        "statusShowsForecast", // TODO https://st.yandex-team.ru/DIRECT-157988#61e198391238052db406e1c3
    ).build()

    private val RELEVANCE_MATCH: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder().withIgnoredFields(
        "id", // id автотаргетинга изменяется при копировании
        "campaignId", // автотаргетинг принадлежит скопированной кампании
        "adGroupId", // автотаргетинг принадлежит скопированной группе
        "lastChangeTime", // время копирования отличается от времени создания

        "price", // TODO https://st.yandex-team.ru/DIRECT-157988#61e198391238052db406e1c3
        "priceContext", // TODO https://st.yandex-team.ru/DIRECT-157988#61e198391238052db406e1c3
        "autobudgetPriority", // TODO https://st.yandex-team.ru/DIRECT-157988#61e198391238052db406e1c3
    ).build()

    private val RETARGETING: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder().withIgnoredFields(
        "id", // id ретаргетинга изменяется при копировании
        "campaignId", // автотаргетинг принадлежит скопированной кампании
        "adGroupId", // ретаргетинг принадлежит скопированной группе
        "lastChangeTime", // время копирования отличается от времени создания
    ).build()

    private val KEYWORD: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder().withIgnoredFields(
        "id", // id фразы изменяется при копировании
        "campaignId", // фраза принадлежит скопированной кампании
        "adGroupId", // фраза принадлежит скопированной группе
        "modificationTime", // время копирования отличается от времени создания
        "statusModerate", // фраза модерируется
        "showsForecast", // пересчитывается при добавлении фразы
        "wordsCount", // отличается количество после копирования, упорядоченный набор слов считается одним словом

        "price", // TODO https://st.yandex-team.ru/DIRECT-157988#61e198391238052db406e1c3
        "priceContext", // TODO https://st.yandex-team.ru/DIRECT-157988#61e198391238052db406e1c3

        "needCheckPlaceModified", // TODO https://st.yandex-team.ru/DIRECT-157988#61e198391238052db406e1c3
        "place", // TODO https://st.yandex-team.ru/DIRECT-157988#61e198391238052db406e1c3

        "autobudgetPriority", // TODO https://st.yandex-team.ru/DIRECT-157988#61e57672a816ab4a95f784f9
    ).build()

    private val BID_MODIFIER: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder().withIgnoredFields(
        "adGroupId", // корректировки принадлежат скопированной группе
        *BID_MODIFIER_COMMON_FIELDS,
    ).build()

    private val BANNER: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder().withIgnoredFields(
        "id", // id баннера изменяется при копировании
        "campaignId", // баннер принадлежит скопированной кампании
        "adGroupId", // баннер принадлежит скопированной группе
        "lastChange", // время копирования отличается от времени создания
        "statusModerate", // баннер модерируется
        "statusPostModerate", // баннер модерируется
        "bsBannerId", // баннер заново отправляется в БК
        "creativeRelationId", // pkey в таблице banners_performance
        "creativeStatusModerate", // креатив модерируется
        "turboLandingStatusModerate", // турболендинг модерируется
        "imageId", // pkey в таблице banner_images
        "imageDateAdded", // время добавления картинки (== создания баннера) отличается
        "imageStatusModerate", // картинки модерируются
        "displayHrefStatusModerate", // модерируем дисплейхреф
        "phoneId", // perl не копирует телефоны баннеров совсем
        "statusSitelinksModerate", // сайтлинки модерируются
        "domainId", // у старых баннеров domainId мог быть null, сейчас при наличии домена всегда не null
        "language", // язык баннера определяется при создании
        "vcardStatusModerate", // визитки модерируются
        "vcardId", // визитка принадлежит скопированной компании
        "flags.flags", // npe в assertj, если мапа содержит null поля
    ).build()

    private val CAMP_METRIKA_GOAL: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder().withIgnoredFields(
        "id.campaignId", // цель принадлежит скопированной кампании
        "campaignId", // цель принадлежит скопированной кампании

        "statDate", // сбрасываем время обновления статистики (дефолтное значение поля)
        "goalsCount", // сбрасываем количество достижений цели (дефолтное значение поля)
        "contextGoalsCount", // сбрасываем количество достижений цели (дефолтное значение поля)
    ).build()


    private val VCARD: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder().withIgnoredFields(
        "id", // id визитки изменяется при копировании
        "campaignId", // визитка принадлежит скопированной кампании
        "lastChange", // время копирования отличается от времени создания
        "lastDissociation", // поле содержит дату создания визитки

        *VCARD_COMMON_FIELDS,
    ).build()


    private val MOBILE_APP: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration.builder().withIgnoredFields(
        "id", // идентификатор мобильного приложения меняется при копировании между клиентами
        "clientId", // идентификатор клиента меняется при копировании между клиентами
        "mobileContentId", // идентификатор мобильного контента меняется при копировании между клиентами
        "mobileContent.clientId", // идентификатор клиента меняется при копировании между клиентами
        "mobileContent.id" // идентификатор мобильного контента меняется при копировании между клиентами
    ).build()

    private val DEFAULT: RecursiveComparisonConfiguration = RecursiveComparisonConfiguration()

    val STRATEGY_BY_ENTITY_CLASS: Map<Class<out Entity<out Any>>, RecursiveComparisonConfiguration> = mapOf(
        BaseCampaign::class.java to CAMPAIGN,
        AdGroup::class.java to AD_GROUP,
        RelevanceMatch::class.java to RELEVANCE_MATCH,
        Retargeting::class.java to RETARGETING,
        DynamicAdTarget::class.java to DEFAULT,
        Keyword::class.java to KEYWORD,
        BidModifier::class.java to BID_MODIFIER,
        PerformanceFilter::class.java to DEFAULT,
        BannerWithAdGroupId::class.java to BANNER,
        CampMetrikaGoal::class.java to CAMP_METRIKA_GOAL,
        MobileApp::class.java to MOBILE_APP,
        Vcard::class.java to VCARD,
        HyperGeoSimple::class.java to DEFAULT,
        Callout::class.java to DEFAULT,
        RetargetingConditionBase::class.java to DEFAULT,
        SitelinkSet::class.java to DEFAULT,
        BannerImageFormat::class.java to DEFAULT,
    )

}
