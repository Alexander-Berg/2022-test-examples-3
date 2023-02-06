# -*- coding: utf-8 -*-

from report.util import *

FATAL_ERROR_MARKER = 'New report internal error'
YANDEXUID = '1656452661435929066'
SALT = u'привёЁ ḷūṭñā kampüs giriş شؤث kasımpaşşa ARŞİVLERİĞQÇ ☼🐼🐱🐲💩🇷🇺'
TEXT = 'test ' + SALT
EXTERNAL_IP = '8.8.8.8'
CALLBACK = 'a123callback'

YABS_FAS = "yabs_fas"
ENABLE_HTTPS_XMLSEARCH = "enable_https_xmlsearch"

PEOPLE_PAD_SEARCH = 'people/search/pad'
PAD = 'tablet'
PADAPP = 'padapp'
TOUCH = 'touch'
SMART = 'smart'
DESKTOP = 'desktop'
GRANNY = 'granny'
GATEWAY = 'gateway'
GATEWAY_TOUCH = 'gateway/touch'
SITESEARCH = 'sitesearch'
PEOPLE = 'people'
XML = 'xml'
SEARCH_APP = 'searchapp'
JSON_PROXY = 'json_proxy'
TV = 'tv'

PAD_ANDROID_4_3 = 'pad_android_4.4'
PAD_IPAD_7 = 'pad_ipad_7'
TOUCH_WINDOWS_PHONE = 'touch_windows_phone'

SEARCH = "/search/"
YANDSEARCH = "/yandsearch"
SEARCH_PAD = "/search/pad/"
PADSEARCH = "/padsearch"
SEARCH_SMART = "/search/smart/"
MSEARCH = "/msearch"
SEARCH_TOUCH = "/search/touch/"
TOUCHSEARCH = "/touchsearch"
SEARCH_FAMILY = "/search/family"
SEARCH_XML = "/search/xml"
SEARCH_SCHOOL = "/search/school"
SEARCH_GATEWAY = "/search/gateway"
SEARCH_SITE = "/search/site/"
SEARCH_SITE_OLD = "/sitesearch"
PEOPLE_SEARCH = "/people/search"
PEOPLE_SEARCH_PAD = "/people/search/pad"
BLOGS = "/blogs"
BLOGS_SEARCH = "/blogs/search"
SEARCHAPP = "/searchapp"
JSONPROXY = '/jsonproxy'

URL_BY_TYPE = {
    DESKTOP: SEARCH,
    PAD: SEARCH_PAD,
    PADAPP: SEARCH_PAD,
    PAD_ANDROID_4_3: SEARCH_PAD,
    PAD_IPAD_7: SEARCH_PAD,
    SMART: SEARCH_SMART,
    TOUCH: SEARCH_TOUCH,
    TOUCH_WINDOWS_PHONE: SEARCH_TOUCH,
    GATEWAY: SEARCH_GATEWAY,
    GATEWAY_TOUCH: SEARCH_GATEWAY,
    SITESEARCH: SEARCH_SITE,
    PEOPLE: PEOPLE_SEARCH,
    XML: SEARCH_XML,
    SEARCH_APP: SEARCHAPP,
    JSON_PROXY: JSONPROXY,
    TV: SEARCH
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
    GATEWAY: EXP_PARAMS_JSON_PROXY,
    GATEWAY_TOUCH: EXP_PARAMS_JSON_PROXY,
    SITESEARCH: EXP_PARAMS_DESKTOP,
    PEOPLE: exp_params("people", "desktop", "", "Chrome"),
    XML: exp_params("search-xml", "desktop", "", "Chrome"),
    SEARCH_APP: exp_params("searchapp", "touch", "android", "YandexSearch"),
    JSON_PROXY: EXP_PARAMS_JSON_PROXY,
    TV: exp_params("web", "tv", "android", "YandexBrowser")
}

L_FULL = 'L_FULL'
L_MOB = 'L_MOB'
COOKIEMY_PREFER_SERP = {
    L_FULL: 'YywBAQA',
    L_MOB: 'YywBAAA',
}

L_RU = 'ru'
L_EN = 'en'
L_FR = 'fr'
L_DE = 'de'
L_UK = 'uk'
L_BE = 'be'
L_KK = 'kk'
L_TT = 'tt'
L_TR = 'tr'
L_ID = 'id'

L_AZ = 'az'
L_AM = 'am'
L_IL = 'il'
L_KG = 'kg'
L_LV = 'lv'
L_LT = 'lt'
L_MD = 'md'
L_TJ = 'tj'
L_TM = 'tm'
L_EE = 'ee'


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

RU_MOSCOW = "ru_MOSCOW"
RU_SAINT_PETESBURG = "ru_SAINTPETESBURG"
RU_VLADIVOSTOK = "ru_VLADIVOSTOK"
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

REGION = {
    FR_PARIS: "10502",
    RU_MOSCOW: "213",
    RU_SAINT_PETESBURG: "2",
    RU_VLADIVOSTOK: "75",
    RU_IRKUTSK: "63",
    RU_UFA: "172",
    RU_SMOLENSK: "12",
    SIMFEROPOL: "146",
    UA_KIEV: "143",
    UA_KHARKOV: "147",
    UA_CHERNIGOV: "966",
    UA_LUTSK: "20222",
    UA_TERNOPL: "10357",
    BY_MINSK: "157",
    BY_GOMEL: "155",
    BY_VITEBSK: "154",
    BY_BREST: "10497",
    BY_LIDA: "21144",
    BY_GRODNO: "10274",
    KZ_ASTANA: "163",
    KZ_KARAGANDA: "164",
    KZ_BAIKONUR: "10292",
    KZ_KOSTANAY: "10295",
    KZ_ACTOBE: "20273",
    KZ_ALMATA: "162",
    UZ_TASHKENT: "10335",
    COMTR_ISTANBUL: "11508",
    COMTR_BYRSA: "11504",
    COMTR_IZMIR: "11505",
    COMTR_ANTALIA: "11511",
    COMTR_KAYSERI: "103831",
    COMTR_MERSIN: "103823",
    W_SEATTLE: "91",
    W_SINGAPORE: "10619",
    W_HARTUM: "20958",
    W_OSLO: "10467",
    USA: "84",
    CHINA: "134",

    AZ_BAKU: "10253",
    COMAM_YEREVAN: "10262",
    COIL_TELAVIV: "131",
    KG_BISHKEK: "10309",
    LT_RIGA: "11474",
    LV_VILNIUS: "11475",
    MD_KISHINEV: "10313",
    TJ_DUSHANBE: "10318",
    TM_ASHGABAT: "10324",
    EE_TALLINN: "11481"

}

REGION_BY_TLD = {
    FR: REGION[FR_PARIS],
    RU: REGION[RU_MOSCOW],
    UA: REGION[UA_KIEV],
    KZ: REGION[KZ_ASTANA],
    BY: REGION[BY_MINSK],
    UZ: REGION[UZ_TASHKENT],
    COMTR: REGION[COMTR_ISTANBUL],
    COM: REGION[USA],

    AZ: REGION[AZ_BAKU],
    COMAM: REGION[COMAM_YEREVAN],
    COIL: REGION[COIL_TELAVIV],
    KG: REGION[KG_BISHKEK],
    LT: REGION[LT_RIGA],
    LV: REGION[LV_VILNIUS],
    MD: REGION[MD_KISHINEV],
    TJ: REGION[TJ_DUSHANBE],
    TM: REGION[TM_ASHGABAT],
    EE: REGION[EE_TALLINN]
}

""" HOW TO UPDATE IP
Example: SIMFEROPOL
1. get production host
    HOST=$(curl 'https://yandex.ru/search/' -s | perl -ne 'm{<!--\s*([a-z0-9]+-[0-9]+)\s*-->} and print qq{$1.search.yandex.net}')
2. get their log
    rsync -L --progress $HOST::logs/reqans_log ~/
3. search region_id for SIMFEROPOL from REGION constant abobe
    146
4. find few IPs from log
    egrep '^req=.*@@reg=146[^0-9]' ~/reqans_log | grep -Po '(?<=@@ip=)[^@]*' | head -n 20
5. verify IP from within report. It could match your region id
    y-local-env perl -MYxWeb::Setup -e 'load(YxWeb::Util::GeoBase);print YxWeb::Util::GeoBase::by_ip(shift)->region_id."\n"' 80.245.118.226
    146
5. write below any IP after verifying
"""
IP = {
    RU_MOSCOW: "78.108.195.43",
    RU_VLADIVOSTOK: "5.100.76.24",
    RU_IRKUTSK: "91.189.164.169",
    RU_UFA: "31.8.105.125",
    RU_SMOLENSK: "95.158.206.252",
    SIMFEROPOL: "80.245.118.226",
    UA_KIEV: "46.119.32.244",
    UA_KHARKOV: "37.55.243.30",
    UA_CHERNIGOV: "37.55.110.160",
    UA_LUTSK: "37.52.56.44",
    UA_TERNOPL: "178.92.113.42",
    BY_MINSK: "178.125.47.186",
    BY_GOMEL: "178.123.15.143",
    BY_VITEBSK: "178.124.232.166",
    BY_BREST: "90.83.144.96",
    BY_LIDA: "37.213.108.78",
    BY_GRODNO: "37.213.68.33",
    KZ_ASTANA: "2.133.218.240",
    KZ_KARAGANDA: "46.34.223.68",
    KZ_BAIKONUR: "147.30.183.86",
    KZ_KOSTANAY: "37.151.162.202",
    UZ_TASHKENT: "195.34.28.53",
    KZ_ALMATA: "92.47.244.82",
    COMTR_ISTANBUL: "2.133.218.240",
    COMTR_IZMIR: "78.178.228.90",
    COMTR_BYRSA: "46.2.189.254",
    COMTR_ANTALIA: "78.165.99.135",
    COMTR_KAYSERI: "88.228.175.185",
    COMTR_MERSIN: "88.227.23.151",
    W_SEATTLE: "70.34.30.0",
    W_SINGAPORE: "103.244.100.0",
    W_HARTUM: "154.100.95.84",
    W_OSLO: "85.19.204.192",
}

USER_AGENT_TOUCH_SERP32867 = 'Mozilla/5.0 (Linux; U; Android 2.3.6; ru-ru; Star TV Build/GRK39F) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1'

USER_AGENT_DESKTOP = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.78.2 (KHTML, like Gecko) Version/7.0.6 Safari/537.78.2'
USER_AGENT_PAD = 'Mozilla/5.0 (Linux; Android 4.3; SM-P601 Build/JSS15J) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 YaBrowser/15.9.1847.18432.00 Safari/537.36'
USER_AGENT_PADAPP = 'Mozilla/5.0 (Linux; Android 4.3; SM-P601 Build/JSS15J) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.131 YaBrowser/15.9.1847.18432.00 Safari/537.36 YandexSearch/3.17'
USER_AGENT_SMART = 'Opera/9.80 (Android; Opera Mini/7.5.33361/31.1350; U; en) Presto/2.8.119 Version/11.10'
USER_AGENT_TOUCH = 'Mozilla/5.0 (Linux; Android 4.1.2; GT-I9300 Build/JZO54K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.72 Mobile Safari/537.36 OPR/16.0.1212.65583'
USER_AGENT_PAD_ANDROID_4_3 = 'Mozilla/5.0 (Linux; Android 4.3; ru-ru; SAMSUNG SM-P601 Build/JSS15J) AppleWebKit/537.36 (KHTML, like Gecko) Version/1.5 Chrome/28.0.1500.94 Safari/537.36'
USER_AGENT_PAD_IPAD_7 = 'Mozilla/5.0 (iPad; CPU OS 7_0 like Mac OS X) AppleWebKit/537.51.1 (KHTML, like Gecko) Version/7.0 Mobile/11A465 Safari/9537.53'
USER_AGENT_TOUCH_WINDOWS_PHONE = 'Mozilla/5.0 (compatible; MSIE 10.0; Windows Phone 8.0; Trident/6.0; IEMobile/10.0; ARM; Touch; NOKIA; Lumia 920)'
USER_AGENT_GRANNY = 'Mozilla/5.0 (Windows; U; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727)'
USER_AGENT_JAVA = 'Apache-HttpClient/4.3.5 (java 1.5)'
USER_AGENT_DESKTOP_IE8 = 'Mozilla/5.0 (compatible; MSIE 8.0; Windows NT 6.1; Trident/4.0; GTB7.4; InfoPath.2; SV1; .NET CLR 3.3.69573; WOW64; en-US)'
USER_AGENT_PAD_YANDEX = 'Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58K; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/47.0.2526.26 Mobile Safari/537.36 YandexSearch/4.70/apad'
USER_AGENT_UCBROWSER = 'Mozilla/5.0 (Linux; U; Android 5.0; en-US; Nexus 4 Build/LRX21T) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/10.8.0.718 U3/0.8.0 Mobile Safari/534.30'
USER_AGENT_TIZEN_SEARCHAPP = 'Mozilla/5.0 (Linux; Tizen 2.4; SAMSUNG SM-Z130H) AppleWebKit/537.3 (KHTML, like Gecko) Version/2.4 Mobile Safari/537.3 YandexSearch/1.0.0'
USER_AGENT_SEARCHAPP_ANDROID = 'Mozilla/5.0 (Linux; Android 6.0.99; Build/NPC56W; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/50.0.2657.12 Mobile Safari/537.36 YaBrowser/16.3.3.4 YandexSearch/5.10'
USER_AGENT_SEARCHAPP_IOS = 'Mozilla/5.0 (iPhone; CPU iPhone OS 9_3 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) YaBrowser/16.3.3.4 YaApp_iOS/1.60'
USER_AGENT_JSON_PROXY_ANDROID = 'Yandex Search Plugin Android/315'
USER_AGENT_TV = 'Mozilla/5.0 (Linux; Android 4.4.4; SmartTV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 YaBrowser/18.11.1.1011.01 Safari/537.36'

USERAGENT_BY_TYPE = {
    PEOPLE_PAD_SEARCH: USER_AGENT_PAD,
    DESKTOP: USER_AGENT_DESKTOP,
    PAD: USER_AGENT_PAD,
    PADAPP: USER_AGENT_PADAPP,
    SMART: USER_AGENT_SMART,
    TOUCH: USER_AGENT_TOUCH,
    TOUCH_WINDOWS_PHONE: USER_AGENT_TOUCH_WINDOWS_PHONE,
    PAD_ANDROID_4_3: USER_AGENT_PAD_ANDROID_4_3,
    PAD_IPAD_7: USER_AGENT_PAD_IPAD_7,
    GRANNY: USER_AGENT_GRANNY,
    GATEWAY: USER_AGENT_JAVA,
    GATEWAY_TOUCH: USER_AGENT_JAVA,
    SITESEARCH: USER_AGENT_DESKTOP,
    PEOPLE: USER_AGENT_DESKTOP,
    XML: USER_AGENT_JAVA,
    SEARCH_APP: USER_AGENT_SEARCHAPP_ANDROID,
    JSON_PROXY: USER_AGENT_JSON_PROXY_ANDROID,
    TV: USER_AGENT_TV
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

MOBILE_UA = [USER_AGENT_TOUCH, USER_AGENT_SMART]
MOBILE_PATH = [TOUCHSEARCH, SEARCH_TOUCH, SEARCH_SMART, MSEARCH]
