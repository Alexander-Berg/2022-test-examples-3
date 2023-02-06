# -*- coding: utf-8 -*-


# Contexts
class CTXS:
    INIT = ('INIT1')
    INIT_HTTP_RESPONSE = ('INIT1:http_response:', 'INIT:http_response:')
    INIT_HANDLER = ('INIT_HANDLER')
    INIT_PRE = ('INIT_PRE')
    NOAPACHE = ('NOAPACHE_CTX')
    BLENDER = ('BLENDER')
    TEMPLATE_DATA = ('BLENDER')
    RENDERER = ('TEMPLATE_PARAMS', 'TEMPLATE_RENDERER')
    YABS_PROXY = ('YABS_PROXY_SETUP', 'YABS_PROXY')
    YABS_SETUP = ('YABS_SETUP')
    BEGEMOT_WORKERS = ('BEGEMOT_WORKERS', 'BEGEMOT_WORKERS_MISSPELL')
    WIZARD = ('WIZARDRY')
    WIZARDRY_WEB_SETUP = ('WIZARDRY:web_setup:')
    WEB_SEARCH = ('WEB_SEARCH')
    WEB_SEARCH_TEMPLATE_DATA = ('WEB_SEARCH:template_data:')
    MISSPELL = ('MISSPELL')
    REPORT = ('REPORT', 'STANDALONE_REPORT', 'INIT1')
    REQUEST = ('BUILD_REQUEST')
    XML_AUTH = ('XML_AUTH', 'XML_AUTH_SLOW')
    INFO_REQUEST = ('INFO_REQUEST')
    APP_HOST = ('APP_HOST')
    INFO_REQUEST = ('INFO_REQUEST')
    TUTOR_BACKEND = ('TUTOR_BACKEND')
    BLENDER_TEMPLATE_DATA = ('BLENDER:template_data:',)
    REALTY_POST_HTTP_SETUP = ('REALTY_POST_HTTP_SETUP',)
    HANDLER_OUTPUT = ('HANDLER_OUTPUT',)
    POST_SEARCH_TEMPLATE_DATA = ('POST_SEARCH_TEMPLATE_DATA',)
    SITE_SEARCH_TEMPLATE_DATA = ('SITE_SEARCH:template_data:')


# Top Level Domains
class TLD:
    RU = 'ru'
    UA = 'ua'
    BY = 'by'
    KZ = 'kz'
    UZ = 'uz'
    FR = 'fr'
    COM = 'com'
    COMTR = 'com.tr'
    COMGE = 'com.ge'
    AZ = 'az'
    COMAM = 'com.am'
    COIL = 'co.il'
    KG = 'kg'
    LT = 'lt'
    LV = 'lv'
    MD = 'md'
    TJ = 'tj'
    TM = 'tm'
    EE = 'ee'
    YA_TLD = [RU, UA, BY, KZ, COM, COMTR, UZ, COMGE, FR, AZ, COMAM, COIL, KG, LT, LV, MD, TJ, TM, EE]
    AUTH_TLD = [RU, BY]


class L10N:
    RU = 'ru'
    EN = 'en'
    FR = 'fr'
    DE = 'de'
    UK = 'uk'
    BE = 'be'
    KK = 'kk'
    TT = 'tt'
    TR = 'tr'
    ID = 'id'
    AZ = 'az'
    AM = 'am'
    IL = 'il'
    KG = 'kg'
    LV = 'lv'
    LT = 'lt'
    MD = 'md'
    TJ = 'tj'
    TM = 'tm'
    EE = 'ee'
    FULL = 'L_FULL'
    MOB = 'L_MOB'


class LANG:
    RU_RU = 'ru-RU'
    EN_RU = 'en-RU'
    UK_RU = 'uk-RU'
    RU_BY = 'ru-BY'
    BE_BY = 'be-BY'
    RU_UA = 'ru-UA'
    UK_UA = 'uk-UA'
    TR_TR = 'tr-TR'
    EN_US = 'en-US'
    EN_CN = 'en-CN'


# Географические названия: города, страны
class GEO:
    RU_MOSCOW = "ru_MOSCOW"
    RU_SAINT_PETESBURG = "ru_SAINTPETESBURG"
    RU_VLADIVOSTOK = "ru_VLADIVOSTOK"
    RU_BRATSK = "ru_BRATSK"
    RU_IRKUTSK = "ru_IRKUTSK"
    RU_UFA = "ru_UFA"
    RU_SMOLENSK = "ru_SMOLENSK"
    SIMFEROPOL = "SIMFEROPL"
    UA_KIEV = "ua_KIEV"
    UA_KHARKOV = "ua_KHARKOV"
    UA_CHERNIGOV = "ua_CHERNIGOV"
    UA_LUTSK = "ua_LUTSK"
    UA_TERNOPL = "ua_TERNOPL"
    BY_MINSK = "by_MINSK"
    BY_GOMEL = "by_GOMEL"
    BY_VITEBSK = "by_VITEBSK"
    BY_BREST = "by_BREST"
    BY_LIDA = "by_LIDA"
    BY_GRODNO = "by_GRODNO"
    KZ_ASTANA = "kz_ASTANA"
    KZ_KARAGANDA = "kz_KARAGANDA"
    KZ_BAIKONUR = "kz_BAIKONUR"
    KZ_KOSTANAY = "kz_KOSTANAY"
    KZ_ACTOBE = "kz_ACTOBE"
    KZ_ALMATA = "kz_ALMATA"
    UZ_TASHKENT = "uz_TASHKENT"
    FR_PARIS = "fr_PARIS"
    COMTR_ISTANBUL = "comtr_ISTANBUL"
    COMTR_BYRSA = "comtr_BYRSA"
    COMTR_IZMIR = "comtr_ISMIR"
    COMTR_ANTALIA = "comtr_ANTALIA"
    COMTR_KAYSERI = "comtr_KAYSERI"
    COMTR_MERSIN = "comtr_MERSIN"
    W_SEATTLE = "w_SEATTLE"
    W_SINGAPORE = "w_SINGAPORE"
    W_HARTUM = "w_HARTUM"
    W_OSLO = "w_OSLO"
    USA = "USA"
    CHINA = "CHINA"
    AZ_BAKU = 'az_BAKU'
    COMAM_YEREVAN = 'comam_YEREVAN'
    COIL_TELAVIV = 'coil_TELAVIV'
    KG_BISHKEK = 'kg_BISHKEK'
    LT_RIGA = 'lt_RIGA'
    LV_VILNIUS = 'lv_VILNIUS'
    MD_KISHINEV = 'md_KISHINEV'
    TJ_DUSHANBE = 'TJ_DUSHANBE'
    TM_ASHGABAT = 'tm_ASHGABAD'
    EE_TALLINN = 'ee_TALLINN'


class USER_AGENT:
    TOUCH_SERP32867 = 'Mozilla/5.0 (Linux; U; Android 2.3.6; ru-ru; Star TV Build/GRK39F) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1'
    DESKTOP = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15'
    PAD = 'Mozilla/5.0 (iPad; CPU OS 15_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.3 YaBrowser/22.3.4.566 Mobile/15E148 Safari/605.1'
    PADAPP = 'Mozilla/5.0 (Linux; Android 4.3; SM-P601 Build/JSS15J) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 YaBrowser/15.9.1847.18432.00 Safari/537.36 YandexSearch/3.17'
    SMART = 'Opera/9.80 (Android; Opera Mini/7.5.33361/31.1350; U; en) Presto/2.8.119 Version/11.10'
    TOUCH = 'Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.58 Mobile Safari/537.36'
    PAD_ANDROID_4_3 = 'Mozilla/5.0 (Linux; Android 4.3; ru-ru; SAMSUNG SM-P601 Build/JSS15J) AppleWebKit/537.36 (KHTML, like Gecko) Version/1.5 Chrome/28.0.1500.94 Safari/537.36'
    PAD_IPAD_7 = 'Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53'
    TOUCH_WINDOWS_PHONE = 'Mozilla/5.0 (compatible; MSIE 10.0; Windows Phone 8.0; Trident/6.0; IEMobile/10.0; ARM; Touch; NOKIA; Lumia 920)'
    GRANNY = 'Mozilla/5.0 (Windows; U; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727)'
    JAVA = 'Apache-HttpClient/4.3.5 (java 1.5)'
    DESKTOP_IE8 = 'Mozilla/5.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0; GTB7.4; InfoPath.2; SV1; .NET CLR 3.3.69573; WOW64; en-US)'
    PAD_YANDEX = 'Mozilla/5.0 (Linux; Android 11; SM-T515 Build/RP1A.200720.012; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/103.0.5060.70 Safari/537.36 YandexSearch/10.51/apad'
    UCBROWSER = 'Mozilla/5.0 (Linux; U; Android 5.0; en-US; Nexus 4 Build/LRX21T) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/10.8.0.718 U3/0.8.0 Mobile Safari/534.30'
    UCBROWSER_11 = ''.join([
        'Mozilla/5.0 (Linux; Android 10; M2003J15SC Build/QP1A.190711.020; wv) ',
        'AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/87.0.4280.101 Mobile Safari/537.36 AgentWeb/4.1.3 UCBrowser/11.6.4.950'
    ])
    UCBROWSER_12 = ''.join([
        'Mozilla/5.0 (Linux; U; Android 4.4.2; en-US; MAX-10 Build/KOT49H) ',
        'AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.2987.108 UCBrowser/12.5.5.1111 Mobile Safari/537.36'
    ])
    TIZEN_SEARCHAPP = 'Mozilla/5.0 (Linux; Tizen 2.4; SAMSUNG SM-Z130H) AppleWebKit/537.3 (KHTML, like Gecko) Version/2.4 Mobile Safari/537.3 YandexSearch/1.0.0'
    SEARCHAPP_ANDROID = ''.join([
        'Mozilla/5.0 (Linux; Android 6.0.99; Build/NPC56W; wv) AppleWebKit/537.36',
        ' (KHTML, like Gecko) Version/4.0 Chrome/50.0.2657.12 Mobile Safari/537.36 YaBrowser/16.3.3.4 YandexSearch/5.10'
    ])
    SEARCHAPP_IOS = 'Mozilla/5.0 (iPhone; CPU iPhone OS 9_3 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) YaBrowser/16.3.3.4 YaApp_iOS/1.60'
    JSON_PROXY_ANDROID = 'Yandex Search Plugin Android/315'
    MOBILE = 'Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.81 Mobile Safari/537.36'  # noqa

    OPERA_MINI_7_5 = 'Opera/9.80 (Android; Opera Mini/7.5.33361/31.1448; U; en) Presto/2.8.119 Version/11.1010'

    BADA_1_0 = 'Mozilla/5.0 (SAMSUNG; SAMSUNG-GT-S8500/S8500XXJF8; U; Bada/1.0; nl-nl) AppleWebKit/533.1 (KHTML, like Gecko) Dolfin/2.0 Mobile WVGA SMM-MMS/1.2.0 OPN-B'

    WP_10_0 = 'Mozilla/5.0 (Windows Phone 10.0; Android 6.0.1; NuAns; NEO) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.116 Mobile Safari/537.36 Edge/15.15254'
    WP_7_5 = 'Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0; HTC; Radar C110e)'
    WP_8_0 = 'Mozilla/5.0 (compatible; MSIE 10.0; Windows Phone 8.0; Trident/6.0; IEMobile/10.0; ARM; Touch; HTC; Windows Phone 8X by HTC)'
    WP_8_1 = 'mozilla/5.0 (windows phone 8.1; arm; trident/7.0; touch; rv:11.0; iemobile/11.0; nokia; lumia 520) like gecko'

    FIREFOX_MOBILE_ANDROID_67 = 'Mozilla/5.0 (Android 11; Mobile; rv:67.0) Gecko/67.0 Firefox/67.0'
    FIREFOX_MOBILE_ANDROID_68 = 'Mozilla/5.0 (Android 11; Mobile; rv:68.0) Gecko/68.0 Firefox/68.0'
    FIREFOX_MOBILE_IOS_67 = 'Mozilla/5.0 (iPhone; CPU iPhone OS 15_0_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/67.0 Mobile/15E148 Safari/605.1.15'
    FIREFOX_MOBILE_IOS_68 = 'Mozilla/5.0 (iPhone; CPU iPhone OS 15_0_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/68.0 Mobile/15E148 Safari/605.1.15'

    IPAD_7_0 = 'Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53'

    IPHONE_6_1_4 = 'Mozilla/5.0 (iPhone; CPU iPhone OS 6_1_4 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Version/6.0 Mobile/10B350 Safari/8536.25'
    IPHONE_7_0 = 'Mozilla/5.0 (iPhone; CPU iPhone OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53'
    IPHONE_8_0 = 'Mozilla/5.0 (iPhone; CPU iPhone OS 8_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/8.0 Mobile/11A465 Safari/9537.53'
    IPHONE_9_0 = 'Mozilla/5.0 (iPhone; CPU iPhone OS 9_0 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13G36 Safari/601.1'
    IPHONE_9_3_5 = 'Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_5 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13G36 Safari/601.1'
    IPHONE_10_3_1 = 'Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.0 Mobile/14E304 Safari/602.1'
    IPHONE_15_3_1 = 'Mozilla/5.0 (iPhone; CPU iPhone OS 15_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.3 Mobile/15E148 Safari/604.139'

    ANDROID_3_2 = 'Mozilla/5.0 (Linux; U; Android 3.2; nl-nl; GT-P6800 Build/HTJ85B) AppleWebKit/534.13 (KHTML, like Gecko) Version/4.0 Safari/534.13'
    ANDROID_4_2_2 = 'Mozilla/5.0 (Linux; U; Android 4.2.2; nl-nl; HTC_One_X Build/JDQ39) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30'
    ANDROID_4_4_NO_BROWSER = 'Mozilla/5.0 (Linux; Android 4.4; Nexus 5)'
    ANDROID_4_4_2 = 'Mozilla/5.0 (Linux; U; Android 4.4.2; de-de; Nexus 5 Build/KOT49H) AppleWebKit/537.16 (KHTML, like Gecko) Version/4.0 Mobile Safari/537.16'


class HNDL:
    ADULT = '/adult'
    ADVANCEDHTML = '/advanced.html'
    BLABLABLA = '/blablabla'
    BLOGS = "/blogs"
    BLOGS_SEARCH = "/blogs/search"
    BLOGS_SEARCH_PAD = '/blogs/search/pad'
    BLOGS_SEARCH_RSS = '/blogs/search/rss'
    BLOGS_SEARCH_TOUCH = '/blogs/search/touch'
    BROSEARCH = '/brosearch'
    CGIBIN_YANDSEARCH = '/cgi-bin/yandsearch'
    CHAT = '/chat'
    FAMILYSEARCH = '/familysearch'
    JSONPROXY = '/jsonproxy'
    LARGESEARCH = '/largesearch'
    MSEARCH = "/msearch"
    PADCACHE = '/padcache.js'
    PADSEARCH = "/padsearch"
    PREFETCH = '/prefetch.txt'
    ROBOTSTXT = '/robots.txt'
    SCHOOLSEARCH = '/schoolsearch'
    SEARCH_ADS = "/search/ads"
    SEARCH_ADS_TOUCH = "/search/touch/ads"
    SEARCH_ADS_PAD = "/search/pad/ads"
    SEARCH_ADULT = '/search/adult'
    SEARCH_ALLSUPPORTEDFLAGS = '/search/all-supported-flags'
    SEARCH_ALLSUPPORTEDPARAMS = '/search/all-supported-params'
    SEARCHAPI = '/searchapi'
    SEARCHAPP_FLD = "/searchapp/"
    SEARCHAPP_META = "/searchapp/meta"
    SEARCHAPP = "/searchapp"
    SEARCHAPP_SEARCHAPP_FLD = "/searchapp/searchapp/"
    SEARCHAPP_SEARCHAPP = "/searchapp/searchapp"
    SEARCH_CATALOGSEARCH = '/search/catalogsearch'
    SEARCH_CHECK_CONDITION = '/search/check_condition'
    SEARCH_CHECKCONFIG = '/search/checkconfig'
    SEARCH_CUSTOMIZE = '/search/customize/'
    SEARCH_DIRECTPREVIEW_PAD = '/search/direct-preview/pad'
    SEARCH_DIRECTPREVIEW = '/search/direct-preview'
    SEARCH_DIRECTPREVIEW_TOUCH = '/search/direct-preview/touch'
    SEARCH_DIRECT = "/search/direct"
    SEARCH_DIRECT_TOUCH = "/search/touch/direct"
    SEARCH_DIRECT_PAD = "/search/pad/direct"
    SEARCH_ENTITY = '/search/entity'
    SEARCH_FAMILY_FLD = "/search/family/"
    SEARCH_FAMILY = "/search/family"
    SEARCH_GATEWAY = "/search/gateway"
    SEARCH_INFOREQUEST = '/search/inforequest'
    SEARCH_NAHODKI = '/search/nahodki/'
    SEARCH_OPENSEARCHXML = '/search/opensearch.xml'
    SEARCH_PADCACHE = '/search/padcache.js'
    SEARCH_PAD_PRE = '/search/pad/pre'
    SEARCH_PAD = "/search/pad/"
    SEARCH_PREFETCH = '/search/prefetch.txt'
    SEARCH_PRE = '/search/pre'
    SEARCH_REDIRWARNING = "/search/redir_warning"
    SEARCH_RESULT = '/search/result'
    SEARCH_RESULT_TOUCH = '/search/result/touch'
    SEARCH_SCHOOL = "/search/school"
    SEARCH = "/search/"
    SEARCH_SEARCHAPI = '/search/searchapi'
    SEARCH_SITE_OLD = "/sitesearch"
    SEARCH_SITE_OPENSEARCHXML = '/search/site/opensearch.xml'
    SEARCH_SITE = "/search/site/"
    SEARCH_SMART = "/search/smart/"
    SEARCH_SUGGESTHISTORY = '/search/suggest-history'
    SEARCH_TAILLOG = '/search/tail-log'
    SEARCH_TOUCHCACHE = '/search/touchcache.js'
    SEARCH_TOUCH_PRE = '/search/touch/pre'
    SEARCH_TOUCH = "/search/touch/"
    SEARCH_TURBO = '/search/turbo'
    SEARCH_VERSIONS = '/search/versions'
    SEARCH_VIEWCONFIG = '/search/viewconfig'
    SEARCH_VL = '/search/vl'
    SEARCH_V = '/search/v'
    SEARCH_WIZARDSJSON = '/search/wizardsjson'
    SEARCH_WRONG = '/search/wrong'
    SEARCH_XML_FLD = "/search/xml/"
    SEARCH_XML = "/search/xml"
    SEARCH_YANDCACHE = '/search/yandcache.js'
    SEARCH_REPORT_ALICE = '/search/report_alice'
    TOUCHCACHE = '/touchcache.js'
    TOUCHSEARCH = "/touchsearch"
    TURBO = '/turbo'
    TUTOR_SEARCH_DOCS = '/tutor/search/docs/'
    V = '/v'
    YANDCACHE = '/yandcache.js'
    YANDPAGE = '/yandpage'
    YANDSEARCH_PRE = "/yandsearch/pre"
    YANDSEARCH = "/yandsearch"
    SEARCH_ADONLY = "/search/adonly"
    XMLSEARCH = "/xmlsearch"


class PLATFORM:
    DESKTOP = 'desktop'
    TABLET = 'tablet'
    TOUCH = 'touch'
    SMART = 'smart'
    GRANNY = 'granny'
    PAD = 'tablet'
    TV = 'tv'
    SEARCHAPP = 'searchapp'
    SEARCHAPP_META = 'searchapp/meta'
    PADAPP = 'padapp'
    SITESEARCH = 'sitesearch'
    XML = 'xml'
    JSON_PROXY = 'json_proxy'
    PADAPP = 'padapp'
    XML = 'xml'
    JSON_PROXY = 'json_proxy'


class DEVICE:
    ANDROID = 'android'
    APAD = 'apad'
    IPAD = 'ipad'
    IPHONE = 'iphone'
    WP = 'wp'


class TEMPLATE:
    PHONE = 'phone'
    PAD = 'pad'
    GRANNY = 'granny_exp:phone'
    GRANNY2 = 'granny_freeze:phone'
    SLOW = 'granny_exp:phone'
    WEB4_PHONE = 'web4:phone'


class UA_DEVICE:
    SMART = [
        # Opera Mini/7.5
        USER_AGENT.OPERA_MINI_7_5,
        # BADA
        USER_AGENT.BADA_1_0
    ]

    FORCED_SMART = [
        # AndroidOS < 4
        USER_AGENT.ANDROID_3_2,
        # AndroidBrowser < 4.4
        USER_AGENT.ANDROID_4_2_2,
        # iOS < 9 (Safari)
        USER_AGENT.IPHONE_6_1_4,
        USER_AGENT.IPHONE_7_0,
        USER_AGENT.IPHONE_8_0,
        # Mobile MS Edge
        USER_AGENT.WP_10_0,
        # IEMobile <= 11
        USER_AGENT.WP_7_5,
        USER_AGENT.WP_8_0,
        USER_AGENT.WP_8_1,
        # AndroidBrowser 4.4+
        USER_AGENT.ANDROID_4_4_NO_BROWSER,
        USER_AGENT.ANDROID_4_4_2,
        # UCBrowser 11
        USER_AGENT.UCBROWSER_11
    ]

    WEB4 = [
        # iPhone 9
        USER_AGENT.IPHONE_9_0,
        # iPhone 9.3.5
        USER_AGENT.IPHONE_9_3_5,
        # iPhone 10.3.1
        USER_AGENT.IPHONE_10_3_1,
        # UCBrowser 12
        USER_AGENT.UCBROWSER_12
    ]

# https://wiki.yandex-team.ru/serp/report/Testovye-akkaunty/ - session_id for test-report-integration
TEST_SESSION_XML_USER = 'yandex-team-test-report'
TEST_SESSION_XML_KEY = '03.886656875:7a5ba9bad5c92e60f341e7de8c1423d3'

YP_EXPIRE = '2427381588'

COOKIE_NOAUTH = 'noauth:1427381588'

FATAL_ERROR_MARKER = 'New report internal error'
YANDEXUID = '1656452661435929066'

SALT_NO_EMOJI = u'привёЁ ḷūṭñā kampüs giriş شؤث kasımpaşşa ARŞİVLERİĞQÇ ☼'
SALT = SALT_NO_EMOJI + u'🐼🐱🐲💩🇷🇺'
TEXT = 'test ' + SALT
XML_TEXT = 'test ' + SALT_NO_EMOJI  # Surrogate pairs are forbidden in XML
EXTERNAL_IP = '8.8.8.8'
CALLBACK = 'a123callback'

YABS_FAS = "yabs_fas"
ENABLE_HTTPS_XMLSEARCH = "enable_https_xmlsearch"

PADAPP = PLATFORM.PADAPP
SITESEARCH = PLATFORM.SITESEARCH
XML = PLATFORM.XML
JSON_PROXY = PLATFORM.JSON_PROXY
PAD = PLATFORM.PAD
TOUCH = PLATFORM.TOUCH
SMART = PLATFORM.SMART
DESKTOP = PLATFORM.DESKTOP
GRANNY = PLATFORM.GRANNY
SEARCH_APP = PLATFORM.SEARCHAPP
SEARCH_APP_META = PLATFORM.SEARCHAPP_META

PAD_ANDROID_4_3 = 'pad_android_4.4'
PAD_IPAD_7 = 'pad_ipad_7'
TOUCH_WINDOWS_PHONE = 'touch_windows_phone'

SEARCH = HNDL.SEARCH
YANDSEARCH = HNDL.YANDSEARCH
SEARCH_PAD = HNDL.SEARCH_PAD
PADSEARCH = HNDL.PADSEARCH
SEARCH_SMART = HNDL.SEARCH_SMART
MSEARCH = HNDL.MSEARCH
SEARCH_TOUCH = HNDL.SEARCH_TOUCH
TOUCHSEARCH = HNDL.TOUCHSEARCH
SEARCH_FAMILY = HNDL.SEARCH_FAMILY
SEARCH_XML = HNDL.SEARCH_XML
SEARCH_SCHOOL = HNDL.SEARCH_SCHOOL
SEARCH_GATEWAY = HNDL.SEARCH_GATEWAY
SEARCH_SITE = HNDL.SEARCH_SITE
SEARCH_SITE_OLD = HNDL.SEARCH_SITE_OLD
BLOGS = HNDL.BLOGS
BLOGS_SEARCH = HNDL.BLOGS_SEARCH
SEARCHAPP = HNDL.SEARCHAPP
SEARCHAPP2 = HNDL.SEARCHAPP
SEARCHAPPMETA = HNDL.SEARCHAPP_META
JSONPROXY = HNDL.JSONPROXY

URL_BY_TYPE = {
    PLATFORM.DESKTOP: HNDL.SEARCH,
    PLATFORM.PAD: HNDL.SEARCH_PAD,
    PLATFORM.PADAPP: HNDL.SEARCH_PAD,
    PAD_ANDROID_4_3: HNDL.SEARCH_PAD,
    PAD_IPAD_7: HNDL.SEARCH_PAD,
    PLATFORM.SMART: HNDL.SEARCH_SMART,
    PLATFORM.TOUCH: HNDL.SEARCH_TOUCH,
    TOUCH_WINDOWS_PHONE: HNDL.SEARCH_TOUCH,
    PLATFORM.SITESEARCH: HNDL.SEARCH_SITE,
    PLATFORM.XML: HNDL.SEARCH_XML,
    PLATFORM.SEARCHAPP: HNDL.SEARCHAPP,
    PLATFORM.SEARCHAPP_META: HNDL.SEARCHAPP_META,
    PLATFORM.JSON_PROXY: HNDL.JSONPROXY
}


# WEBREPORT-57
def exp_params(s, d, m, b):
    return {
        "r": 213,
        "s": s,
        "d": d,
        "m": m,
        "b": b,
        "i": False
    }

EXP_PARAMS_DESKTOP = exp_params("web", "desktop", "", "Chrome")
EXP_PARAMS_JSON_PROXY = exp_params("jsonproxy", "", "", "")

# https://st.yandex-team.ru/WEBREPORT-57#5bec40b9ca5d28001b7ce6c1
# service - deviceType - mobilePlatform - browserName
# https://a.yandex-team.ru/arc/trunk/arcadia/quality/ab_testing/scripts/adminka/adminka/src/usersplit_lib/common.py?blame=true&rev=4223535#L18
EXP_PARAMS_BY_TYPE = {
    DESKTOP: EXP_PARAMS_DESKTOP,
    PAD: exp_params("web", "tablet", "apad", "Chrome"),
    PADAPP: exp_params("searchapp", "tablet", "apad", "YandexSearch"),
    PAD_ANDROID_4_3: exp_params("web", "tablet", "apad", "Chrome"),
    PAD_IPAD_7: exp_params("web", "tablet", "ipad", "MobileSafari"),
    SMART: exp_params("web", "", "", "OperaMini"),
    TOUCH: exp_params("web", "touch", "android", "ChromeMobile"),
    TOUCH_WINDOWS_PHONE: exp_params("web", "touch", "wp", "IEMobile"),
    SITESEARCH: EXP_PARAMS_DESKTOP,
    XML: exp_params("search-xml", "desktop", "", "Chrome"),
    SEARCH_APP: exp_params("searchapp", "touch", "android", "YandexSearch"),
    SEARCH_APP_META: exp_params("searchapp", "touch", "android", "YandexSearch"),
    JSON_PROXY: EXP_PARAMS_JSON_PROXY
}

L_FULL = L10N.FULL
L_MOB = L10N.MOB
COOKIEMY_PREFER_SERP = {
    L10N.FULL: 'YywBAQA',
    L10N.MOB: 'YywBAAA',
}

L_RU = L10N.RU
L_EN = L10N.EN
L_FR = L10N.FR
L_DE = L10N.DE
L_UK = L10N.UK
L_BE = L10N.BE
L_KK = L10N.KK
L_TT = L10N.TT
L_TR = L10N.TR
L_ID = L10N.ID
L_AZ = L10N.AZ
L_AM = L10N.AM
L_IL = L10N.IL
L_KG = L10N.KG
L_LV = L10N.LV
L_LT = L10N.LT
L_MD = L10N.MD
L_TJ = L10N.TJ
L_TM = L10N.TM
L_EE = L10N.EE


COOKIEMY_LANG = {
    L_RU: 'YycCAAE2AQEA',
    L_UK: 'YycCAAI2AQEA',
    L_EN: 'YycCAAM2AQEA',
    L_KK: 'YycCAAQ2AQEA',
    L_BE: 'YycCAAU2AQEA',
    L_TT: 'YycCAAY2AQEA',
    '7': 'YycCAAc2AQEA',
    L_TR: 'YycCAAg2AQEA',
    '9': 'YycCAAk2AQEA',
    '10': 'YycCAAo2AQEA',
    '11': 'YycCAAs2AQEA',
    L_DE: 'YycCAAw2AQEA',
    L_ID: 'YycCAA02AQEA',
}


RU = TLD.RU
UA = TLD.UA
BY = TLD.BY
KZ = TLD.KZ
UZ = TLD.UZ
FR = TLD.FR
COM = TLD.COM
COMTR = TLD.COMTR
COMGE = TLD.COMGE
AZ = TLD.AZ
COMAM = TLD.COMAM
COIL = TLD.COIL
KG = TLD.KG
LT = TLD.LT
LV = TLD.LV
MD = TLD.MD
TJ = TLD.TJ
TM = TLD.TM
EE = TLD.EE
YA_TLD = TLD.YA_TLD

RU_MOSCOW = GEO.RU_MOSCOW
RU_SAINT_PETESBURG = GEO.RU_SAINT_PETESBURG
RU_VLADIVOSTOK = GEO.RU_VLADIVOSTOK
RU_IRKUTSK = GEO.RU_IRKUTSK
RU_UFA = GEO.RU_UFA
RU_SMOLENSK = GEO.RU_SMOLENSK
SIMFEROPOL = GEO.SIMFEROPOL
UA_KIEV = GEO.UA_KIEV
UA_KHARKOV = GEO.UA_KHARKOV
UA_CHERNIGOV = GEO.UA_CHERNIGOV
UA_LUTSK = GEO.UA_LUTSK
UA_TERNOPL = GEO.UA_TERNOPL
BY_MINSK = GEO.BY_MINSK
BY_GOMEL = GEO.BY_GOMEL
BY_VITEBSK = GEO.BY_VITEBSK
BY_BREST = GEO.BY_BREST
BY_LIDA = GEO.BY_LIDA
BY_GRODNO = GEO.BY_GRODNO
KZ_ASTANA = GEO.KZ_ASTANA
KZ_KARAGANDA = GEO.KZ_KARAGANDA
KZ_BAIKONUR = GEO.KZ_BAIKONUR
KZ_KOSTANAY = GEO.KZ_KOSTANAY
KZ_ACTOBE = GEO.KZ_ACTOBE
KZ_ALMATA = GEO.KZ_ALMATA
UZ_TASHKENT = GEO.UZ_TASHKENT
FR_PARIS = GEO.FR_PARIS
COMTR_ISTANBUL = GEO.COMTR_ISTANBUL
COMTR_BYRSA = GEO.COMTR_BYRSA
COMTR_IZMIR = GEO.COMTR_IZMIR
COMTR_ANTALIA = GEO.COMTR_ANTALIA
COMTR_KAYSERI = GEO.COMTR_KAYSERI
COMTR_MERSIN = GEO.COMTR_MERSIN
W_SEATTLE = GEO.W_SEATTLE
W_SINGAPORE = GEO.W_SINGAPORE
W_HARTUM = GEO.W_HARTUM
W_OSLO = GEO.W_OSLO
USA = GEO.USA
CHINA = GEO.CHINA

AZ_BAKU = GEO.AZ_BAKU
COMAM_YEREVAN = GEO.COMAM_YEREVAN
COIL_TELAVIV = GEO.COIL_TELAVIV
KG_BISHKEK = GEO.KG_BISHKEK
LT_RIGA = GEO.LT_RIGA
LV_VILNIUS = GEO.LV_VILNIUS
MD_KISHINEV = GEO.MD_KISHINEV
TJ_DUSHANBE = GEO.TJ_DUSHANBE
TM_ASHGABAT = GEO.TM_ASHGABAT
EE_TALLINN = GEO.EE_TALLINN

REGION = {
    GEO.FR_PARIS: "10502",
    GEO.RU_MOSCOW: "213",
    GEO.RU_SAINT_PETESBURG: "2",
    GEO.RU_VLADIVOSTOK: "75",
    GEO.RU_BRATSK: "976",
    GEO.RU_IRKUTSK: "63",
    GEO.RU_UFA: "172",
    GEO.RU_SMOLENSK: "12",
    GEO.SIMFEROPOL: "146",
    GEO.UA_KIEV: "143",
    GEO.UA_KHARKOV: "147",
    GEO.UA_CHERNIGOV: "966",
    GEO.UA_LUTSK: "20222",
    GEO.UA_TERNOPL: "10357",
    GEO.BY_MINSK: "157",
    GEO.BY_GOMEL: "155",
    GEO.BY_VITEBSK: "154",
    GEO.BY_BREST: "10497",
    GEO.BY_LIDA: "21144",
    GEO.BY_GRODNO: "10274",
    GEO.KZ_ASTANA: "163",
    GEO.KZ_KARAGANDA: "164",
    GEO.KZ_BAIKONUR: "10292",
    GEO.KZ_KOSTANAY: "10295",
    GEO.KZ_ACTOBE: "20273",
    GEO.KZ_ALMATA: "162",
    GEO.UZ_TASHKENT: "10335",
    GEO.COMTR_ISTANBUL: "11508",
    GEO.COMTR_BYRSA: "11504",
    GEO.COMTR_IZMIR: "11505",
    GEO.COMTR_ANTALIA: "11511",
    GEO.COMTR_KAYSERI: "103831",
    GEO.COMTR_MERSIN: "103823",
    GEO.W_SEATTLE: "91",
    GEO.W_SINGAPORE: "10619",
    GEO.W_HARTUM: "20958",
    GEO.W_OSLO: "10467",
    GEO.USA: "84",
    GEO.CHINA: "134",

    GEO.AZ_BAKU: "10253",
    GEO.COMAM_YEREVAN: "10262",
    GEO.COIL_TELAVIV: "131",
    GEO.KG_BISHKEK: "10309",
    GEO.LT_RIGA: "11474",
    GEO.LV_VILNIUS: "11475",
    GEO.MD_KISHINEV: "10313",
    GEO.TJ_DUSHANBE: "10318",
    GEO.TM_ASHGABAT: "10324",
    GEO.EE_TALLINN: "11481"
}

REGION_BY_TLD = {
    TLD.FR: REGION[FR_PARIS],
    TLD.RU: REGION[RU_MOSCOW],
    TLD.UA: REGION[UA_KIEV],
    TLD.KZ: REGION[KZ_ASTANA],
    TLD.BY: REGION[BY_MINSK],
    TLD.UZ: REGION[UZ_TASHKENT],
    TLD.COMTR: REGION[COMTR_ISTANBUL],
    TLD.COM: REGION[USA],

    TLD.AZ: REGION[AZ_BAKU],
    TLD.COMAM: REGION[COMAM_YEREVAN],
    TLD.COIL: REGION[COIL_TELAVIV],
    TLD.KG: REGION[KG_BISHKEK],
    TLD.LT: REGION[LT_RIGA],
    TLD.LV: REGION[LV_VILNIUS],
    TLD.MD: REGION[MD_KISHINEV],
    TLD.TJ: REGION[TJ_DUSHANBE],
    TLD.TM: REGION[TM_ASHGABAT],
    TLD.EE: REGION[EE_TALLINN]
}

# HOW TO UPDATE IP
# Example: SIMFEROPOL
# 1. get production host
#     HOST=$(curl 'https://yandex.ru/search/' -s | perl -ne 'm{<!--\s*([a-z0-9]+-[0-9]+)\s*-->} and print qq{$1.search.yandex.net}')
# 2. get their log
#     rsync -L --progress $HOST::logs/reqans_log ~/
# 3. search region_id for SIMFEROPOL from REGION constant abobe
#     146
# 4. find few IPs from log
#     egrep '^req=.*@@reg=146[^0-9]' ~/reqans_log | grep -Po '(?<=@@ip=)[^@]*' | head -n 20
# 5. verify IP from within report. It could match your region id
#     y-local-env perl -MYxWeb::Setup -e 'load(YxWeb::Util::GeoBase);print YxWeb::Util::GeoBase::by_ip(shift)->region_id."\n"' 80.245.118.226
#     146
# 5. write below any IP after verifying

IP = {
    GEO.RU_MOSCOW: "78.108.195.43",
    GEO.RU_VLADIVOSTOK: "5.100.76.24",
    GEO.RU_BRATSK: "91.189.164.169",
    GEO.RU_IRKUTSK: "195.206.47.26",
    GEO.RU_UFA: "31.8.105.125",
    GEO.RU_SMOLENSK: "95.158.206.252",
    GEO.SIMFEROPOL: "80.245.118.226",
    GEO.UA_KIEV: "46.119.32.244",
    GEO.UA_KHARKOV: "37.55.243.30",
    GEO.UA_CHERNIGOV: "37.55.110.160",
    GEO.UA_LUTSK: "37.52.56.44",
    GEO.UA_TERNOPL: "178.92.113.42",
    GEO.BY_MINSK: "178.125.47.186",
    GEO.BY_GOMEL: "178.123.15.143",
    GEO.BY_VITEBSK: "178.124.232.166",
    GEO.BY_BREST: "90.83.144.96",
    GEO.BY_LIDA: "37.213.108.78",
    GEO.BY_GRODNO: "37.213.68.33",
    GEO.KZ_ASTANA: "2.133.218.240",
    GEO.KZ_KARAGANDA: "46.34.223.68",
    GEO.KZ_BAIKONUR: "147.30.183.86",
    GEO.KZ_KOSTANAY: "37.151.162.202",
    GEO.UZ_TASHKENT: "195.34.28.53",
    GEO.KZ_ALMATA: "92.47.244.82",
    GEO.COMTR_ISTANBUL: "2.133.218.240",
    GEO.COMTR_IZMIR: "78.178.228.90",
    GEO.COMTR_BYRSA: "46.2.189.254",
    GEO.COMTR_ANTALIA: "78.165.99.135",
    GEO.COMTR_KAYSERI: "88.228.175.185",
    GEO.COMTR_MERSIN: "88.227.23.151",
    GEO.W_SEATTLE: "70.34.30.0",
    GEO.W_SINGAPORE: "103.244.100.0",
    GEO.W_HARTUM: "154.100.95.84",
    GEO.W_OSLO: "85.19.204.192",
}

USER_AGENT_TOUCH_SERP32867 = USER_AGENT.TOUCH_SERP32867
USER_AGENT_DESKTOP = USER_AGENT.DESKTOP
USER_AGENT_PAD = USER_AGENT.PAD
USER_AGENT_PADAPP = USER_AGENT.PADAPP
USER_AGENT_SMART = USER_AGENT.SMART
USER_AGENT_TOUCH = USER_AGENT.TOUCH
USER_AGENT_PAD_ANDROID_4_3 = USER_AGENT.PAD_ANDROID_4_3
USER_AGENT_PAD_IPAD_7 = USER_AGENT.PAD_IPAD_7
USER_AGENT_TOUCH_WINDOWS_PHONE = USER_AGENT.TOUCH_WINDOWS_PHONE
USER_AGENT_GRANNY = USER_AGENT.GRANNY
USER_AGENT_JAVA = USER_AGENT.JAVA
USER_AGENT_DESKTOP_IE8 = USER_AGENT.DESKTOP_IE8
USER_AGENT_PAD_YANDEX = USER_AGENT.PAD_YANDEX
USER_AGENT_UCBROWSER = USER_AGENT.UCBROWSER
USER_AGENT_TIZEN_SEARCHAPP = USER_AGENT.TIZEN_SEARCHAPP
USER_AGENT_SEARCHAPP_ANDROID = USER_AGENT.SEARCHAPP_ANDROID
USER_AGENT_SEARCHAPP_IOS = USER_AGENT.SEARCHAPP_IOS
USER_AGENT_JSON_PROXY_ANDROID = USER_AGENT.JSON_PROXY_ANDROID

USERAGENT_BY_TYPE = {
    DESKTOP: USER_AGENT.DESKTOP,
    PAD: USER_AGENT.PAD,
    PADAPP: USER_AGENT.PADAPP,
    SMART: USER_AGENT.SMART,
    TOUCH: USER_AGENT.TOUCH,
    TOUCH_WINDOWS_PHONE: USER_AGENT.TOUCH_WINDOWS_PHONE,
    PAD_ANDROID_4_3: USER_AGENT.PAD_ANDROID_4_3,
    PAD_IPAD_7: USER_AGENT.PAD_IPAD_7,
    GRANNY: USER_AGENT.GRANNY,
    SITESEARCH: USER_AGENT.DESKTOP,
    XML: USER_AGENT.JAVA,
    SEARCH_APP: USER_AGENT.SEARCHAPP_ANDROID,
    SEARCH_APP_META: USER_AGENT.SEARCHAPP_ANDROID,
    JSON_PROXY: USER_AGENT.JSON_PROXY_ANDROID
}

BLACKBOX = 'BLACKBOX'
BLACKBOX_MIMINO = 'BLACKBOX_MIMINO'
SITESEARCH_INFO = 'SITESEARCH_INFO'
MISSPELL = 'MISSPELL'
WIZARD = 'WIZARD'
APP_HOST = 'APP_HOST'
SAFE_BROWSING = 'SAFE_BROWSING'
RESINFOD = 'RESINFOD'
TUNE = 'TUNE'
UPPER_COM = 'UPPER_COM'
UPPER_RKUB = 'UPPER_RKUB'
UPPER_TUR = 'UPPER_TUR'
TEMPLATES = 'TEMPLATES'

MOBILE_UA = [USER_AGENT.TOUCH, USER_AGENT.SMART]
MOBILE_PATH = [TOUCHSEARCH, SEARCH_TOUCH, SEARCH_SMART, MSEARCH]
