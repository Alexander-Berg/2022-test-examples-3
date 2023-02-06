from yt import yson

from crypta.ltp.viewer.lib import ltp_logs
from crypta.ltp.viewer.proto import index_pb2


class Geo:
    @staticmethod
    def get_name(geoid):
        return "region-{}".format(geoid)


class Pages:
    @staticmethod
    def get_name(page_id):
        return "page-name-{}".format(page_id)

    @staticmethod
    def get_description(page_id):
        return "page-value-{}".format(page_id)


class Categories:
    @staticmethod
    def get_description(category_id):
        return "category-description-{}".format(category_id)


class Context:
    def __init__(self):
        self.geo = Geo()
        self.pages = Pages()
        self.categories = Categories()


CONTEXT = Context()


def format_record(log_type, record):
    log = ltp_logs.LOGS_DICT[log_type]
    return {
        "yt_columns": sorted(log.yt_columns),
        "description": log.format_description(record, CONTEXT),
        "additional_description": log.format_additional_description(record, CONTEXT),
    }


def test_ltp_watch():
    return format_record(
        index_pb2.LtpWatch,
        {
            "DeviceModel": "iPhone",
            "LinkText": None,
            "OSFamily": "iOS",
            "Referer": "https://yandex.ru/",
            "SearchQuery": "приказ об утверждении суот",
            "Title": "приказ об утверждении суот: 1 тыс изображений найдено в Яндекс.Картинках",
            "Url": "https://yandex.ru/images/touch/search?text=%D0%BF%D1%80%D0%B8%D0%BA%D0%B0%D0%B7%20%D0%BE%D0%B1%20%D1%83%D1%82%D0%B2%D0%B5%D1%80%D0%B6%D0%B4%D0%B5%D0%BD%D0%B8%D0%B8%20%D1%81%D1%83%D0%BE%D1%82&img_url=https%3A%2F%2Ftpk-granit.ru%2Fwp-content%2Fuploads%2F02017eab987aa6684da1f61bdd074d89_1920.jpg&pos=0&rpt=simage&source=serp",  # noqa: E501
            "BrowserName": "MobileSafari",
        },
    )


def test_ltp_visit_states():
    return format_record(
        index_pb2.LtpVisitStates,
        {
            "Duration": 35,
            "Goals_ID": yson.dumps([173271049]),
            "Referer": "https://yandex.ru/",
            "StartURL": "https://mail.yandex.ru/lite/",
            "StartURLDomain": "mail.yandex.ru",
            "LinkURL": "https://passport.yandex.ru/passport?mode=logout&yu=1820259601419642026",
        },
    )


def test_ltp_rsya_shows():
    return format_record(
        index_pb2.LtpRsyaShows,
        {
            "BannerText": "Все актуальные новости из мира большого футбола. Переходи и будь в курсе!",
            "BannerTitle": "Футбольные новости каждый день",
            "SecondTitle": "Коротко о футболе и не только",
            "SelectType": 149,
            "RegionID": 11029,
            "BannerURL": "https://vk.com/livetv_sport",
            "URLClusterID": 3477270000,
            "ProductType": "direct",
            "BannerID": 72057605891413600,
            "PageID": 347727,
            "Referer": "https://yandex.ru/images/touch/search?text=%D0%BE%20%D1%82%D0%BE%D0%B9%20%D0%B2%D0%B5%D1%81%D0%BD%D0%B5%20%D1%82%D0%B5%D0%BA%D1%81%D1%82%20%D0%BF%D0%B5%D1%81%D0%BD%D0%B8%20%D1%80%D0%B0%D1%81%D0%BF%D0%B5%D1%87%D0%B0%D1%82%D0%B0%D1%82%D1%8C&source=tabbar&app_id=ru.yandex.searchplugin&app_platform=android&app_version=22030100&appsearch_header=1&ui=webmobileapp.yandex&pos=2&rpt=simage&img_url=https%3A%2F%2Fsun9-41.userapi.com%2Fimpg%2FelZ8C4MYgOAhGmc2sW1DEcxj42cQuN0kPNRUTQ%2FZT2iEdwKWXM.jpg%3Fsize%3D604x453%26q",  # noqa: E501
            "BMCategory1ID": 200000073,
            "BMCategory2ID": 200000244,
            "BMCategory3ID": 200002041,
            "BMCategoryID": 200000073,
        },
    )


def test_user_session_queries():
    return format_record(
        index_pb2.UserSessionQueries,
        {
            "Query": "рокада мед уфа официальный",
            "CorrectedQuery": "рокада мед уфа официальный",
            "RelevRegionID": 225,
            "SearchRegion": "11",
        },
    )


def test_user_session_clicked_docs():
    return format_record(
        index_pb2.UserSessionClickedDocs,
        {
            "Query": "рокада мед уфа официальный",
            "CorrectedQuery": "рокада мед уфа официальный",
            "OmniTitle": "интернет магазин стоматологических товаров рокада мед главная",
            "DocumentUrl": "https://ufa.shop.rocadamed.ru/",
            "RelevRegionNameRusV2": "Россия Республика Башкортостан Уфа",
            "Title": "Интернет магазин стоматологических товаров Рокада Мед | Главная",
            "Url": "https://ufa.shop.rocadamed.ru/",
        },
    )


def test_ltp_ecom():
    return format_record(
        index_pb2.LtpEcom,
        {
            "CounterID": 731962,
            "OfferID": "100789449745",
            "OfferIDMd5": 8211404920890246929,
            "OfferName": "100789449745",
            "OfferPrice": 4990,
            "OfferSource": "watch",
            "ActionType": "detail",
            "OfferBrand": "Itosima",
            "OfferCategory": "Беговые дорожки",
            "OfferCoupon": "delivery",
        },
    )


def test_crypta_tx_log():
    return format_record(
        index_pb2.CryptaTxLog,
        {
            "Source": "taxi",
            "Seller": "Яндекс Такси",
            "GeoId": 217063,
            "ItemQuantity": 1,
            "ItemDescription": "Россия, Киров, улица Свободы, 135В - Россия, Киров, улица Ленина, 186",
            "ItemUnitPriceRub": 79,
        },
    )


def test_ltp_browser_url_title():
    return format_record(
        index_pb2.LtpBrowserUrlTitle,
        {
            "EventName": "url opened",
            "Title": "Запись к врачу — Москва | Медицинский портал Емиас.инфо",
            "Yasoft": None,
            "APIKey": 10321,
        },
    )


def test_ltp_visit_goals_v2():
    return format_record(
        index_pb2.LtpVisitGoalsV2,
        {
            "CounterName": "DRIVE2.RU",
            "GoalName": "Показы виджета (по доскроллу)",
            "GoalPatternTypes": yson.dumps([
                "action",
                "action",
                "action",
                "action"
            ]),
            "GoalPatternUrls": yson.dumps([
                "drom_logbook_view",
                "drom_catalog_view",
                "drom_car_view",
                "drom_main_view"
            ]),
            "GoalType": "url",
            "ReachedCounter": 33911514,
            "ReachedGoal": 54604444,
            "Autobudget": False,
        }
    )


def test_ltp_rsya_clicks():
    return format_record(
        index_pb2.LtpRsyaClicks,
        {
            "BannerTitle": "Лазерная резка в Новосибирске. Немецкое оборудование.",
            "ImpressionOptions": "lua-dsp-response,interstitial,video-block,inbanner,use-shared-rtb-dsp-cache,in-app,is-ssp,mobile",
            "Options": "dsp,right-side,guarantee,picture,yclid,commerce,flat-page,stationary-connection,rtb-smart-amnesty,autobudget,relevance-match-highscored,clicked-domains-sent,bscount-responded,extended-relevance-match",  # noqa: E501
            "DetailedDeviceType": "Android",
            "BrowserName": "AndroidBrowser",
        }
    )
