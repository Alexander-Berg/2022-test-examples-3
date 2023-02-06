# -*- coding: utf-8 -*-
# flake8: noqa

from __future__ import print_function

import collections
import hashlib
import json
import os
from datetime import datetime, timedelta

from crypta.graph.v1.tests.testdata.testdata_patterns import (
    ACCESS_LOG,
    AUDIMVIDEO_EMAILS_LOG,
    AUTORU_LOG,
    BAR_NAVIG_LOG,
    BS_CHEVENT_LOG,
    BS_RTB_LOG,
    CRYPTA_RT_GEO_LOG,
    CUBE_LOG,
    DEV_INFO_WITH_INCOME,
    DEV_YUID_INDEVICE_PERFECT_NO_LIMIT,
    DICTS_DEV_INFO,
    DICTS_INCOME_DATA_WITH_DEV_INFO,
    DICTS_PASSPORT_DUMP,
    DICTS_PUID_YUID,
    DICTS_PUID_YUID_YT,
    DICTS_YUID_IDS,
    DICTS_YUID_REGS,
    DICTS_YUID_UA,
    DICTS_YUID_WITH_ALL_GOOD,
    DITMSK_COOKIEMATCHING,
    DITMSK_DAY_DUMP,
    FUZZY_PAIRS,
    KINOPOISK_LOG,
    LAL_MANAGER_DATA_TO_CLASSIFY,
    MAIL_VKIDS_DAYDUMP,
    MOBILE_REDIRECT_BIND_LOG,
    MOBILE_TRACKING_LOG,
    MOBILE_TRACKING_PRIVATE_LOG,
    OAUTH_LOG,
    PASSPORT_LOG,
    PASSPORT_PHONE,
    PASSPORT_SENS,
    PASSPORT_SOCIAL,
    POSTCLICK_LOG,
    PUID_LOGIN_DICT_LOG,
    RADIUS_LOG,
    REDIR_LOG,
    SBAPI_MITB_LOG,
    SBER_PHONES_LOG,
    SENDR_CLICK_LOG,
    STATBOX_SQL_PASSPORT_ACCOUNTS,
    TICKETS_ORDER_LOG,
    VISIT_LOG,
    VK_PROFILES_DUMP,
    WATCH_LOG,
    YAMONEY_LOG,
    YAMONEY_PHONE_LOG,
    YAMONEY_YAMONEY_IN_V1,
    YUID_WITH_ALL,
)

from crypta.lib.python.identifiers.identifiers import GenericID, Email, Phone
from crypta.graph.v1.tests.testdata.testdata_helper import (  # noqa
    AccessLog,
    BaseLog,
    ComplexParametersLog,
    FPLog,
    metrica_crypt,
    SingleTableLog,
    SoupTableLog,
)
from Crypto.PublicKey import RSA
from market.lilucrm.platform_config.src.main.proto.models.Order_pb2 import Order


def convert_old_fp_to_watch_log(target, source, date):
    """Unwrap old fp format log back to primary logs to make tests pass"""
    for data in source:
        history = data["history"]
        for timestamp in history.split(","):
            timestamp, _ = timestamp.split(":", 1)
            row = dict(
                _date=date,
                clientip=data.get("ip", ""),
                uniqid=data.get("yandexuid", ""),
                cookiei=data.get("yandexuid", ""),
                useragent=data.get("user_agent", ""),
                login=data.get("login", ""),
                _logfeller_timestamp=int(timestamp),
            )
            if "vk_com_id" in data:
                row["referer"] = "https://vk.com/?mid={}".format(data["vk_com_id"])
            target.add_row(**row)


SOME_PREV_DATE = "2016-02-10"
TEST_RUN_DATE_STR = "2016-04-10"
AND_FINALLY_DATE = "2016-04-11"

FIXTURES_ROOT = os.getenv("TEST_FIXTURES_PATH")

with open(os.getenv("METRICA_RSA_KEY_PATH")) as f:
    from Crypto.Cipher import PKCS1_v1_5

    rsakey = RSA.importKey(f.read())
    rsa = PKCS1_v1_5.new(rsakey)

DESK_UA = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.2.0.3539 Yowser/2.5 Safari/537.36"
SMARTTV_UA = "Mozilla/5.0 (SMART-TV; X11; Linux armv7l) AppleWebkit/537.42 (KHTML, like Gecko) Chromium/25.0.1349.2 Chrome/25.0.1349.2 Safari/537.42"
IPHONE_UA = "Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53"
YABRO_UA = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7146 Yowser/2.5 Safari/537.36"
ANDROID_UA = "Mozilla/5.0 (Linux; Android 4.4.2; Ixion ML145 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36"
ANDROID_HTC = "Mozilla/5.0 (Linux; U; Android 4.2.2; ru-ru; HTC Desire 310 Build/JDQ39) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30 YandexSearch/5.45"

PARTNERS_FLAT_FIXTURES_PATH = "partners_flat.json"

rawdata_fp = list()
rawdata_fp.append(
    dict(
        ip="85.174.38.23",
        yandexuid="2577188551456167601",
        user_agent=DESK_UA,
        login="Lagutin2008",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="85.174.38.23",
        yandexuid="2577188551456167611",
        user_agent=DESK_UA,
        login="Lagutin2008",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="31.173.80.25",
        uuid="bf62d1b137388c21ddb64090dd02c94c",
        deviceid="cfd36eb292d2d747435a6e31b5aadb26",
        history="1460326111:mm:55.6563968:37.3975021,1460327989:mm:55.6513844:37.387669,1460354373:mm:55.6513844:37.387669,1460354373:mm:55.6513844:37.387669,1460354373:mm:55.6513844:37.387669,1460354376:mm:55.6572839:37.4029805",
    )
)
rawdata_fp.append(
    dict(
        ip="78.106.126.12",
        yandexuid="7074010371428486964",
        fuid="1030195971384194234",
        history="1460382176:r,1460382185:r",
    )
)
rawdata_fp.append(
    dict(
        ip="5.165.39.55",
        yandexuid="6927584441427992515",
        fuid="13348466971424465160",
        user_agent="Mozilla/5.0 (SMART-TV; X11; Linux armv7l) AppleWebkit/537.42 (KHTML, like Gecko) Chromium/25.0.1349.2 Chrome/25.0.1349.2 Safari/537.42",
        java="1",
        plugin_hash="156769251001",
        b_lang="c",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp.append(
    dict(
        ip="93.185.197.39",
        yandexuid="7520203571383726763",
        puid="19150427",
        history="1460374280:p,1460374280:p,1460374280:p",
    )
)
rawdata_fp.append(
    dict(
        ip="213.87.162.157",
        uuid="13cc284f279bd1702a1adb429ba68298",
        deviceid="404693f45cc376674266202bbc3b982d",
        history="1460366028:mm,1460366028:mm",
    )
)
rawdata_fp.append(
    dict(
        ip="91.204.176.18",
        yandexuid="1358811251455881991",
        fuid="283613721457621670",
        user_agent="Opera/9.80 (Android; Opera Mini/15.0.2125/37.8157; U; ru) Presto/2.12.423 Version/12.16",
        plugin_hash="383305183401",
        xoperaminiphoneua="Mozilla/5.0 (Linux; Android 4.4.2; Ixion ML145 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
        login="bars12@161.ru",
        history="1460357845:m,1460357853:m,1460357969:m",
    )
)
rawdata_fp.append(
    dict(
        ip="88.85.195.48",
        yandexuid="1495205931451997969",
        fuid="16416450621445877538",
        mail_ru_login="anoshko_yana@mail.ru",
        user_agent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7146 Yowser/2.5 Safari/537.36",
        plugin_hash="417650996401",
        login="anoshko.av",
        history="1460365678:m,1460379333:m",
    )
)
rawdata_fp.append(
    dict(
        ip="88.85.195.48",
        yandexuid="601826891455547779",
        fuid="3644952421455693848",
        user_agent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 Safari/537.36",
        plugin_hash="97806405901",
        vk_com_id="27627371",
        history="1460370877:m",
    )
)
rawdata_fp.append(
    dict(
        ip="176.125.194.65",
        yandexuid="6619110241447613888",
        fuid="5987103671447613889",
        user_agent="Mozilla/5.0 (Linux; Android 5.1; Lenovo A2010-a Build/LMY47D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.105 Mobile Safari/537.36",
        plugin_hash="216613626101",
        login="stoltat",
        history="1460348138:m,1460348138:m,1460348243:m,1460348243:m,1460348246:m,1460348246:m,1460348246:m",
    )
)
rawdata_fp.append(
    dict(
        ip="46.72.214.19",
        yandexuid="8887683331441813775",
        fuid="81155321441814303",
        user_agent="Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2403.157 YaBrowser/15.9.2403.3043 Safari/537.36",
        java="1",
        plugin_hash="247168530901",
        login="aashinova",
        history="1460360148:m,1460360148:m,1460360152:m",
    )
)
rawdata_fp.append(
    dict(
        ip="92.101.247.246",
        yandexuid="7249027431455393740",
        user_agent="Mozilla/5.0 (Linux; U; Android 4.2.2; ru-ru; GT-P5100 Build/JDQ39) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Safari/534.30",
        history="1460322066:a,1460324212:a,1460324212:a,1460324212:a,1460324213:a,1460324219:a,1460324219:a",
    )
)
rawdata_fp.append(
    dict(
        ip="46.187.50.11",
        yandexuid="9574390901437926574",
        user_agent=IPHONE_UA,
        puid="194502233",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)

rawdata_fp.append(
    dict(
        ip="78.25.121.32",
        yandexuid="5860408211418147506",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 9_2 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13C75 Safari/601.1",
        history="1460238040:a,1460238040:a,1460238041:a",
    )
)
rawdata_fp.append(
    dict(
        ip="79.165.27.223",
        yandexuid="7740095711445945300",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 9_3 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13E233 Safari/601.1",
        history="1460292286:a",
    )
)
rawdata_fp.append(
    dict(
        ip="95.153.131.5",
        yandexuid="8540896401445067609",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 8_4 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Version/8.0 Mobile/12H143 Safari/600.1.4",
        history="1460311814:a,1460311850:a",
    )
)
rawdata_fp.append(
    dict(
        ip="85.140.2.211",
        yandexuid="5299407961448206668",
        fuid="10729360511448206684",
        history="1460308429:b,1460308444:b,1460308448:b,1460310099:b,1460310117:b,1460310371:b,1460310536:b,1460310598:b,1460310694:b,1460310713:b,1460310796:b",
    )
)
rawdata_fp.append(
    dict(
        ip="91.146.45.152",
        yandexuid="354879291458845025",
        fuid="11215910881458847387",
        history="1460237478:r,1460240877:r,1460263057:r,1460263058:r,1460263073:r,1460263075:r,1460263272:r,1460263288:r,1460263293:r,1460263669:r,1460264506:r,1460264625:r,1460275104:r,1460278732:r,1460278740:r,1460281163:r,1460286988:r",
    )
)
rawdata_fp.append(
    dict(
        ip="194.226.49.43",
        yandexuid="46069391421503557",
        screen_size="360x640x32",
        fuid="11902130281425890162",
        user_agent="Mozilla/5.0 (Linux; Android 5.0; SAMSUNG SM-N900 Build/LRX21V) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/4.0 Chrome/44.0.2403.133 Mobile Safari/537.36",
        java="1",
        plugin_hash="216613626101",
        login="ev0ngertlt",
        history="1460274628:m,1460274726:m,1460274727:m,1460274727:m",
    )
)
rawdata_fp.append(
    dict(
        ip="37.122.26.189",
        yandexuid="1169456551460062417",
        fuid="15331651941460069458",
        user_agent="Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7146 Yowser/2.5 Safari/537.36",
        history="1460234435:m,1460241403:m,1460242349:m,1460242603:m",
    )
)
rawdata_fp.append(
    dict(
        ip="46.138.212.193",
        yandexuid="4422446881454020740",
        fuid="18244561191454709601",
        user_agent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7146 Safari/537.36",
        login="andrei.ponomareff-1997",
        history="1460235744:m,1460241680:m,1460243026:m,1460287022:m,1460289701:m,1460301929:m",
    )
)
rawdata_fp.append(
    dict(
        ip="5.142.37.166",
        yandexuid="5308405301441752808",
        screen_size="1280x800x32",
        fuid="18860585511441752811",
        user_agent="Mozilla/5.0 (Linux; Android 5.1.1; SGP521 Build/23.4.A.1.232) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.2.1.7529.01 Safari/537.36",
        plugin_hash="216613626101",
        login="modsever",
        history="1460242522:m,1460242532:m,1460242546:m,1460242552:m,1460242556:m,1460242612:m,1460242617:m,1460242617:m,1460242624:m,1460242624:m,1460242624:m,1460242636:m,1460242640:m,1460242640:m,1460242640:m,1460242647:m,1460242649:m,1460242679:m,1460242682:m,1460242821:m,1460242826:m",
    )
)
rawdata_fp.append(
    dict(
        ip="31.47.170.199",
        yandexuid="2512730601443681995",
        screen_size="1920x1080x24",
        fuid="19282323511434881385",
        user_agent="Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.87 Safari/537.36 OPR/36.0.2130.46",
        plugin_hash="238476119301",
        login="evarcher",
        history="1460276786:m,1460276796:m,1460296477:m",
    )
)
rawdata_fp.append(
    dict(
        ip="109.187.220.93",
        yandexuid="6779234761452927619",
        screen_size="360x640x32",
        fuid="20634471291452927637",
        user_agent="Mozilla/5.0 (Linux; Android 4.4.2; PSP5470DUO Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.2.1.7529.00 Mobile Safari/537.36",
        plugin_hash="216613626101",
        login="sveta.aleshina2015",
        history="1460244176:m,1460244299:m,1460244345:m,1460244345:m,1460244351:m,1460244428:m,1460244437:m,1460254016:m,1460254023:m,1460254023:m,1460254139:m,1460254160:m,1460254208:m,1460254225:m,1460254246:m,1460254268:m",
    )
)

rawdata_fp.append(
    dict(
        ip="217.13.91.184",
        yandexuid="111000011459458000",
        fuid="1313103671447613889",
        user_agent="Mozilla/5.0 (Linux; Android 5.1; Lenovo A2010-a Build/LMY47D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.105 Mobile Safari/537.36",
        plugin_hash="216613626101",
        history="1460348138:m,1460348138:m,1460348243:m,1460348243:m,1460348246:m,1460348246:m,1460348246:m",
    )
)
rawdata_fp.append(
    dict(
        ip="217.13.91.184",
        yandexuid="111000021459458000",
        fuid="1313103671447613889",
        user_agent="Mozilla/5.0 (Linux; Android 5.1; Lenovo A2010-a Build/LMY47D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.105 Mobile Safari/537.36",
        plugin_hash="216636333101",
        history="1460348138:m,1460348138:m,1460348243:m,1460348243:m,1460348246:m,1460348246:m,1460348246:m",
    )
)
rawdata_fp.append(
    dict(
        ip="217.13.91.184",
        yandexuid="111000031459458000",
        fuid="1313103671447613889",
        user_agent="Mozilla/5.0 (Linux; Android 5.1; Lenovo A2010-a Build/LMY47D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.105 Mobile Safari/537.36",
        plugin_hash="216613644401",
        history="1460348138:m,1460348138:m,1460348243:m,1460348243:m,1460348246:m,1460348246:m,1460348246:m",
    )
)
rawdata_fp.append(
    dict(
        ip="217.13.91.184",
        yandexuid="111000041459458000",
        fuid="1313103671447613889",
        user_agent="Mozilla/5.0 (Linux; Android 5.1; Lenovo A2010-a Build/LMY47D) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.105 Mobile Safari/537.36",
        plugin_hash="216613655501",
        history="1460348138:m,1460348138:m,1460348243:m,1460348243:m,1460348246:m,1460348246:m,1460348246:m",
    )
)

# for new mtrika_mobile_log
rawdata_fp.append(
    dict(
        ip="31.173.80.25",
        yandexuid="2577188551456167771",
        uuid="bf62d1b137388c21ddb64090dd02c94c",
        deviceid="00002907a397aea15dbfbdcf0472a111",
        history="1397034612:mm:55.6563968:37.6563968,1397034612:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.3.1 Mobile/13E233 Safari/601.1",
    )
)
rawdata_fp.append(
    dict(
        ip="31.173.80.25",
        yandexuid="2577188551456167771",
        uuid="bf62d1b137388c21ddb64090dd02c94c",
        deviceid="00002907a397aea15dbfbdcf0472a111",
        history="1397034612:mm:55.6563968:37.3975021,1397034612:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.3.1 Mobile/13E233 Safari/601.1",
    )
)
rawdata_fp.append(
    dict(
        ip="31.173.80.25",
        yandexuid="2577188551456167772",
        uuid="bf62d1b137388c21ddb64090dd02c94c",
        deviceid="00002907a397aea15dbfbdcf0472a111",
        history="1397035812:mm:55.6563968:37.3975021,1397035812:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.3.1 Mobile/13E233 Safari/601.1",
    )
)
rawdata_fp.append(
    dict(
        ip="31.173.80.26",
        yandexuid="2577188551456167772",
        uuid="bf62d1b137388c21ddb64090dd02c94c",
        deviceid="00002907a397aea15dbfbdcf0472a121",
        history="1397033772:mm:55.6563968:37.3975021,1397033772:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.3.1 Mobile/13E233 Safari/601.1",
    )
)
rawdata_fp.append(
    dict(
        ip="31.173.80.26",
        yandexuid="2577188551456167773",
        uuid="bf62d1b137388c21ddb64090dd02c94c",
        deviceid="00002907a397aea15dbfbdcf0472a131",
        history="1397033772:mm:55.6563968:37.3975021,1397033772:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.3.1 Mobile/13E233 Safari/601.1",
    )
)

# for webvisor ecomerce mobile and desk
rawdata_fp.append(
    dict(
        ip="31.173.80.25",
        yandexuid="11222221455542221",
        uuid="bf62d1b137388c21ddb64090dd02c94c",
        deviceid="00002907a397aea15dbfbdcf0472a111",
        history="1397034612:mm:55.6563968:37.6563968,1397034612:mm:55.6513844:37.387669",
        user_agent=DESK_UA,
    )
)
rawdata_fp.append(
    dict(
        ip="31.173.80.25",
        yandexuid="11222221455542221",
        uuid="bf62d1b137388c21ddb64090dd02c94c",
        deviceid="00002907a397aea15dbfbdcf0472a111",
        history="1397034612:mm:55.6563968:37.3975021,1397034612:mm:55.6513844:37.387669",
        user_agent=DESK_UA,
    )
)


# for fuzzy merged_hh vs yuid_with_all
rawdata_fp.append(
    dict(
        ip="111.22.11.11",
        yandexuid="157564101446625111",
        user_agent=IPHONE_UA,
        login="test_login111",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.11",
        yandexuid="157564101446625112",
        user_agent=DESK_UA,
        login="test_login111",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.12",
        yandexuid="157564101446625113",
        user_agent=IPHONE_UA,
        login="test_login113",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.12",
        yandexuid="157564101446625114",
        user_agent=DESK_UA,
        login="test_login114",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.13",
        yandexuid="157564101446625121",
        user_agent=IPHONE_UA,
        login="test_login115",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.13",
        yandexuid="157564101446625122",
        user_agent=DESK_UA,
        login="test_login115",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.14",
        yandexuid="157564101446625123",
        user_agent=IPHONE_UA,
        login="test_login116",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.14",
        yandexuid="157564101446625124",
        user_agent=DESK_UA,
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.15",
        yandexuid="157564101446629164",
        user_agent=IPHONE_UA,
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.15",
        yandexuid="1813903801458090754",
        user_agent=DESK_UA,
        login="test_login0754",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.21",
        yandexuid="213261281388846262",
        user_agent=DESK_UA,
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.21",
        yandexuid="2263475241459074652",
        user_agent=DESK_UA,
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.22",
        yandexuid="2565999051458967148",
        user_agent=IPHONE_UA,
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.22",
        yandexuid="7065602181459495311",
        user_agent=IPHONE_UA,
        login="test_login311",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.23",
        yandexuid="7065602181459495312",
        user_agent=IPHONE_UA,
        login="test_login311",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.23",
        yandexuid="7065602181459495313",
        user_agent=IPHONE_UA,
        login="test_login313",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.24",
        yandexuid="7065602181459495314",
        user_agent=IPHONE_UA,
        login="test_login314",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.24",
        yandexuid="7065602181459495321",
        user_agent=IPHONE_UA,
        login="test_login315",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.25",
        yandexuid="7065602181459495322",
        user_agent=IPHONE_UA,
        login="test_login315",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.25",
        yandexuid="7065602181459495323",
        user_agent=IPHONE_UA,
        login="test_login316",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.31",
        yandexuid="7065602181459495324",
        user_agent=IPHONE_UA,
        login="test_login316",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.31",
        yandexuid="7065602181459495331",
        user_agent=IPHONE_UA,
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.32",
        yandexuid="7065602181459495332",
        user_agent=IPHONE_UA,
        login="test_login317",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.32",
        yandexuid="7065602181459495333",
        user_agent=IPHONE_UA,
        login="test_login318",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.33",
        yandexuid="7065602181459495334",
        user_agent=IPHONE_UA,
        login="test_login318",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.33",
        yandexuid="7065602181459497865",
        user_agent=IPHONE_UA,
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.34",
        yandexuid="9731293591459364411",
        user_agent=DESK_UA,
        login="test_login4411",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.34",
        yandexuid="9731293591459364422",
        user_agent=DESK_UA,
        login="test_login4422",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.35",
        yandexuid="9731293591459364433",
        user_agent=DESK_UA,
        login="test_login4433",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="111.22.11.35",
        yandexuid="9731293591459364477",
        user_agent=DESK_UA,
        login="test_login4477",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="85.174.11.41",
        yandexuid="9731293591459365211",
        user_agent=DESK_UA,
        login="test_login211",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="85.174.11.41",
        yandexuid="9731293591459365212",
        user_agent=DESK_UA,
        login="test_login211",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="85.174.11.42",
        yandexuid="9731293591459365213",
        user_agent=DESK_UA,
        login="test_login213",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="85.174.11.42",
        yandexuid="9731293591459365214",
        user_agent=DESK_UA,
        login="test_login214",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="85.174.11.43",
        yandexuid="9731293591459365221",
        user_agent=DESK_UA,
        login="test_login215",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="85.174.11.43",
        yandexuid="9731293591459365222",
        user_agent=DESK_UA,
        login="test_login215",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="85.174.11.44",
        yandexuid="9731293591459365223",
        user_agent=DESK_UA,
        login="test_login216",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="85.174.11.44",
        yandexuid="9731293591459365224",
        user_agent=DESK_UA,
        login="test_login216",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="85.174.11.45",
        yandexuid="9731293591459365231",
        user_agent=DESK_UA,
        login="test_login217",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="85.174.11.45",
        yandexuid="9731293591459365232",
        user_agent=DESK_UA,
        login="test_login217",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="85.174.11.51",
        yandexuid="9731293591459365233",
        user_agent=DESK_UA,
        login="test_login218",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="85.174.11.51",
        yandexuid="9731293591459365234",
        user_agent=DESK_UA,
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)

# for pairs black list
rawdata_fp.append(
    dict(
        ip="77.22.11.11",
        yandexuid="222564101446625991",
        user_agent=IPHONE_UA,
        login="pairs_bl_login111",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="77.22.11.11",
        yandexuid="222564101446625992",
        user_agent=DESK_UA,
        login="pairs_bl_login111",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="77.22.11.11",
        yandexuid="222564101446625993",
        user_agent=IPHONE_UA,
        login="pairs_bl_login222",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="77.22.11.11",
        yandexuid="222564101446625994",
        user_agent=DESK_UA,
        login="pairs_bl_login222",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="77.22.11.11",
        yandexuid="222564101446625995",
        user_agent=IPHONE_UA,
        login="pairs_bl_login333",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)
rawdata_fp.append(
    dict(
        ip="77.22.11.11",
        yandexuid="222564101446625996",
        user_agent=DESK_UA,
        login="pairs_bl_login333",
        history="1460322914:m,1460322929:m,1460322953:m,1460323018:m,1460323053:m,1460323065:m,1460323145:m,1460323156:m,1460323199:m,1460323204:m,1460323226:m",
    )
)


rawdata_fp3 = list()
# data for radius metrics
rawdata_fp3.append(
    dict(
        ip="85.174.38.23",
        yandexuid="601826891455541112",
        mail_ru_login="mail_login",
        user_agent=DESK_UA,
        deviceid="1670a3e6c91fba9d07c53d02a551632c",
        login="WatchRadius",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp3.append(
    dict(
        ip="85.174.38.23",
        yandexuid="601826891455541113",
        user_agent=DESK_UA,
        deviceid="1670a3e6c91fba9d07c53d02a551632c",
        vk_com_id="11111",
        login="WatchRadius",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp3.append(
    dict(
        ip="85.174.38.25",
        yandexuid="601826891455541113",
        mail_ru_login="mail_login",
        user_agent=DESK_UA,
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp3.append(
    dict(
        ip="85.174.38.25",
        yandexuid="601826891455541113",
        user_agent=DESK_UA,
        vk_com_id="11111",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp3.append(
    dict(
        ip="85.174.38.23",
        yandexuid="601826891455542222",
        mail_ru_login="mail_login2",
        user_agent=DESK_UA,
        deviceid="1670a3e6c91fba9d07c53d02a551632c",
        login="WatchRadius2",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp3.append(
    dict(
        ip="85.174.38.23",
        yandexuid="601826891455542223",
        mail_ru_login="mail_login2",
        user_agent=DESK_UA,
        deviceid="1670a3e6c91fba9d07c53d02a551632c",
        login="WatchRadius2",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp3.append(
    dict(
        ip="85.174.38.23",
        yandexuid="601826891455541114",
        mail_ru_login="mail_login_mob",
        user_agent=IPHONE_UA,
        login="WatchRadiusMob",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp3.append(
    dict(
        ip="85.174.38.23",
        yandexuid="601826891455541115",
        user_agent=IPHONE_UA,
        vk_com_id="11112",
        login="WatchRadiusMob",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp3.append(
    dict(
        ip="85.174.38.25",
        yandexuid="601826891455541115",
        mail_ru_login="mail_login_mob",
        user_agent=IPHONE_UA,
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp3.append(
    dict(
        ip="85.174.38.25",
        yandexuid="601826891455541115",
        user_agent=IPHONE_UA,
        vk_com_id="11112",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp3.append(
    dict(
        ip="85.174.38.23",
        yandexuid="601826891455542224",
        mail_ru_login="mail_login_mob2",
        user_agent=IPHONE_UA,
        login="WatchRadiusMob2",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp3.append(
    dict(
        ip="85.174.38.23",
        yandexuid="601826891455542225",
        user_agent=IPHONE_UA,
        vk_com_id="11113",
        login="WatchRadiusMob2",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)

rawdata_fp3.append(
    dict(
        ip="85.174.38.33",
        yandexuid="1748232901455365413",
        user_agent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7146 Yowser/2.5 Safari/537.36",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp3.append(
    dict(
        ip="85.174.38.33",
        yandexuid="5726075371455990918",
        user_agent=IPHONE_UA,
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)

# for did pairs
rawdata_fp3.append(
    dict(
        ip="31.173.11.99",
        yandexuid="10113701463508403",
        uuid="bf62d1b137388c21ddb64090dd02c111",
        deviceid="77772907a397aea15dbfbdcf0472a111",
        history="1397034612:mm:55.6563968:37.6563968,1397034612:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
    )
)
rawdata_fp3.append(
    dict(
        ip="31.173.22.99",
        yandexuid="10113701463508401",
        uuid="bf62d1b137388c21ddb64090dd02c112",
        deviceid="77772907a397aea15dbfbdcf0472a122",
        history="1397034612:mm:55.6563968:37.6563968,1397034612:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
    )
)
rawdata_fp3.append(
    dict(
        ip="31.173.33.99",
        yandexuid="10113701463508400",
        uuid="bf62d1b137388c21ddb64090dd02c113",
        deviceid="77772907a397aea15dbfbdcf0472a133",
        history="1397034612:mm:55.6563968:37.6563968,1397034612:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
    )
)
rawdata_fp3.append(
    dict(
        ip="31.173.44.99",
        yandexuid="10113701463508339",
        uuid="bf62d1b137388c21ddb64090dd02c114",
        deviceid="77772907a397aea15dbfbdcf0472a144",
        history="1397034612:mm:55.6563968:37.6563968,1397034612:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
    )
)
rawdata_fp3.append(
    dict(
        ip="31.173.55.99",
        yandexuid="10113701463508402",
        uuid="bf62d1b137388c21ddb64090dd02c115",
        deviceid="77772907a397aea15dbfbdcf0472a155",
        history="1397034612:mm:55.6563968:37.6563968,1397034612:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
    )
)
rawdata_fp3.append(
    dict(
        ip="31.173.66.99",
        yandexuid="10113701463508337",
        uuid="bf62d1b137388c21ddb64090dd02c116",
        deviceid="77772907a397aea15dbfbdcf0472a166",
        history="1397034612:mm:55.6563968:37.6563968,1397034612:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
    )
)
rawdata_fp3.append(
    dict(
        ip="31.173.77.99",
        yandexuid="2410530891459150418",
        uuid="bf62d1b137388c21ddb64090dd02c117",
        deviceid="77772907a397aea15dbfbdcf0472a177",
        history="1397034612:mm:55.6563968:37.6563968,1397034612:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
    )
)
rawdata_fp3.append(
    dict(
        ip="31.173.88.99",
        yandexuid="2410530891459150411",
        uuid="bf62d1b137388c21ddb64090dd02c118",
        deviceid="77772907a397aea15dbfbdcf0472a188",
        history="1397034612:mm:55.6563968:37.6563968,1397034612:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
    )
)
rawdata_fp3.append(
    dict(
        ip="31.173.99.99",
        yandexuid="2410530891459150413",
        uuid="bf62d1b137388c21ddb64090dd02c119",
        deviceid="77772907a397aea15dbfbdcf0472a199",
        history="1397034612:mm:55.6563968:37.6563968,1397034612:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
    )
)
rawdata_fp3.append(
    dict(
        ip="31.173.10.99",
        yandexuid="2410530891459150414",
        uuid="bf62d1b137388c21ddb64090dd02c110",
        deviceid="77772907a397aea15dbfbdcf0472a110",
        history="1397034612:mm:55.6563968:37.6563968,1397034612:mm:55.6513844:37.387669",
        user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
    )
)

rawdata_fp3.append(
    dict(
        ip="31.173.80.26",
        yandexuid="10113701463508334",
        uuid="bf62d1b137388c21ddb64090dd02c94c",
        deviceid="00002907a397aea15dbfbdcf0472a121",
        history="1397033772:mm:55.6563968:37.3975021,1397033772:mm:55.6513844:37.387669",
        user_agent=IPHONE_UA,
    )
)
rawdata_fp3.append(
    dict(
        ip="31.173.80.26",
        yandexuid="10113701463508334",
        uuid="bf62d1b137388c21ddb64090dd02c94c",
        deviceid="00002907a397aea15dbfbdcf0472a131",
        history="1397033772:mm:55.6563968:37.3975021,1397033772:mm:55.6513844:37.387669",
        user_agent=IPHONE_UA,
    )
)


rawdata_fp2 = list()
# data for radius metrics
rawdata_fp2.append(
    dict(
        ip="15.174.38.23",
        yandexuid="101826891455541112",
        mail_ru_login="mail_login",
        user_agent=DESK_UA,
        deviceid="1670a3e6c91fba9d07c53d02a551632c",
        login="WatchRadius",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp2.append(
    dict(
        ip="15.174.38.23",
        yandexuid="101826891455541113",
        user_agent=DESK_UA,
        deviceid="1670a3e6c91fba9d07c53d02a551632c",
        vk_com_id="11111",
        login="WatchRadius",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp2.append(
    dict(
        ip="15.174.38.25",
        yandexuid="101826891455541113",
        mail_ru_login="mail_login",
        user_agent=DESK_UA,
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp2.append(
    dict(
        ip="15.174.38.25",
        yandexuid="101826891455541113",
        user_agent=DESK_UA,
        vk_com_id="11111",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp2.append(
    dict(
        ip="15.174.38.23",
        yandexuid="101826891455542222",
        mail_ru_login="mail_login2",
        user_agent=DESK_UA,
        deviceid="1670a3e6c91fba9d07c53d02a551632c",
        login="WatchRadius2",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp2.append(
    dict(
        ip="15.174.38.23",
        yandexuid="101826891455542223",
        mail_ru_login="mail_login2",
        user_agent=DESK_UA,
        deviceid="1670a3e6c91fba9d07c53d02a551632c",
        login="WatchRadius2",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp2.append(
    dict(
        ip="15.174.38.23",
        yandexuid="101826891455541114",
        mail_ru_login="mail_login_mob",
        user_agent=IPHONE_UA,
        login="WatchRadiusMob",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp2.append(
    dict(
        ip="15.174.38.23",
        yandexuid="101826891455541115",
        user_agent=IPHONE_UA,
        vk_com_id="11112",
        login="WatchRadiusMob",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp2.append(
    dict(
        ip="15.174.38.25",
        yandexuid="101826891455541115",
        mail_ru_login="mail_login_mob",
        user_agent=IPHONE_UA,
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp2.append(
    dict(
        ip="15.174.38.25",
        yandexuid="101826891455541115",
        user_agent=IPHONE_UA,
        vk_com_id="11112",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp2.append(
    dict(
        ip="15.174.38.23",
        yandexuid="101826891455542224",
        mail_ru_login="mail_login_mob2",
        user_agent=IPHONE_UA,
        login="WatchRadiusMob2",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp2.append(
    dict(
        ip="15.174.38.23",
        yandexuid="101826891455542225",
        user_agent=IPHONE_UA,
        vk_com_id="11113",
        login="WatchRadiusMob2",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp2.append(
    dict(
        ip="15.174.38.33",
        yandexuid="2748232901455365413",
        user_agent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7146 Yowser/2.5 Safari/537.36",
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)
rawdata_fp2.append(
    dict(
        ip="15.174.38.33",
        yandexuid="1726075371455990918",
        user_agent=IPHONE_UA,
        history="1460390405:m,1460390542:m,1460396965:m",
    )
)

rawdata_fp2.append(
    dict(
        ip="31.173.80.25",
        yandexuid="10113701463508333",
        uuid="bf62d1b137388c21ddb64090dd02c94c",
        deviceid="00002907a397aea15dbfbdcf0472a111",
        history="1397035812:mm:55.6563968:37.3975021,1397035812:mm:55.6513844:37.387669",
        user_agent=IPHONE_UA,
    )
)


testdata_passport_sens = BaseLog(
    path="//home/logfeller/logs/passport-sensitive-log/1d", date="2016-04-09", default_data=PASSPORT_SENS
)
testdata_passport_sens.add_row(attribute="confirmed")
testdata_passport_sens.add_row(attribute="bound")
testdata_passport_sens.add_row(attribute="secured")
testdata_passport_sens.add_row(_date="2016-04-10", attribute="confirmed")
testdata_passport_sens.add_row(_date="2016-04-10", attribute="bound")
testdata_passport_sens.add_row(_date="2016-04-10", attribute="secured")
testdata_passport_sens.add_row(_date="2016-04-11", attribute="confirmed")
testdata_passport_sens.add_row(_date="2016-04-11", attribute="bound")
testdata_passport_sens.add_row(_date="2016-04-11", attribute="secured")


testdata_passport_phone = BaseLog(
    path="//home/logfeller/logs/passport-phone-log/1d", date="2016-04-09", default_data=PASSPORT_PHONE
)
testdata_passport_phone.add_row(yandexuid="7383335981460211111", uid="28467361", phone="+79522511111")
testdata_passport_phone.add_row(yandexuid="7383335981460211111", uid="28467361", phone="+79522522222")
testdata_passport_phone.add_row(
    _date="2016-04-10", yandexuid="7383335981460211111", uid="28467361", phone="+79522511111"
)
testdata_passport_phone.add_row(
    _date="2016-04-10", yandexuid="7383335981460211111", uid="28467361", phone="+79522522222"
)
testdata_passport_phone.add_row(
    _date="2016-04-11", yandexuid="7383335981460211111", uid="28467361", phone="+79522511111"
)
testdata_passport_phone.add_row(
    _date="2016-04-11", yandexuid="7383335981460211111", uid="28467361", phone="+79522522222"
)

testdata_access = AccessLog(path="//statbox/access-log", date="2016-04-09", default_data=ACCESS_LOG)
testdata_access.add_row(
    stbx_ip="::ffff:178.64.134.57",
    ip="2a02:6b8:b000:172:922b:34ff:fecf:24b6",
    yandexuid="-",
    cookies={"yandexuid": "601826891455541113"},
)
testdata_access.add_row(
    _date="2016-04-10", stbx_ip="::ffff:46.133.27.214", ip="46.133.27.214", cookies={"yandexuid": "137418131459737825"}
)
testdata_access.add_row(
    _date="2016-04-10", stbx_ip="::ffff:46.133.56.232", ip="127.0.0.1", cookies={"yandexuid": "9672732681428384566"}
)
testdata_access.add_row(
    _date="2016-04-10", stbx_ip="::ffff:46.133.64.253", ip="127.0.0.1", cookies={"yandexuid": "683580251415739703"}
)
testdata_access.add_row(
    _date="2016-04-11", stbx_ip="::ffff:178.64.134.57", cookies={"yandexuid": "2410530891459150428"}
)
testdata_access.add_row(
    _date="2016-04-11",
    stbx_ip="::ffff:217.66.157.42",
    cookies={"raw_yandexuid": "2918597931460386402", "yandexuid": "2918597931460386402"},
)
testdata_access.add_row(
    _date="2016-04-11",
    stbx_ip="::ffff:78.25.123.196",
    cookies={"raw_yandexuid": "2925551651460374938", "yandexuid": "2925551651460374938"},
)

# test did from cookies yp
testdata_access.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 16:16:11",
    stbx_ip="::ffff:178.64.134.57",
    cookies={
        "yandexuid": "2410530891459150418",
        "yp": "1518447753.andrid.91124fd3631f22e#2147483648.did.53b84e78dbf8e7aa77ac092137057bc1#2147481111.ybrod.3#1534104691.sz.640x360x2",
    },
    user_agent="Mozilla/5.0 (Linux; Android 4.4.2; Ixion ML145 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
)
testdata_access.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 16:26:11",
    stbx_ip="::ffff:178.64.134.57",
    cookies={
        "yandexuid": "2410530891459150418",
        "yp": "1518447753.andrid.91124fd3631f22e#2147483648.did.53b84e78dbf8e7aa77ac092137057bc1#2147481111.ybrod.3#1534104691.sz.640x360x2",
    },
    user_agent="Mozilla/5.0 (Linux; Android 4.4.2; Ixion ML145 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
)
testdata_access.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 16:16:11",
    stbx_ip="::ffff:178.64.134.57",
    cookies={
        "yandexuid": "2410530891459150418",
        "yp": "1518447753.andrid.91124fd3631f22e#2147483648.did.53b84e78dbf8e7aa77ac092137057bc2#2147482222.ybrod.3#1534104691.sz.640x360x2",
    },
    user_agent="Mozilla/5.0 (iPad; CPU OS 8_1_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) YaBrowser/15.4.2272.3000.11 Mobile/12B440 Safari/600.1.4",
)
testdata_access.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 16:26:11",
    stbx_ip="::ffff:178.64.134.57",
    cookies={
        "yandexuid": "2410530891459150418",
        "yp": "1518447753.andrid.91124fd3631f22e#2147483648.did.53b84e78dbf8e7aa77ac092137057bc2#2147482222.ybrod.3#1534104691.sz.640x360x2",
    },
    user_agent="Mozilla/5.0 (iPad; CPU OS 8_1_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) YaBrowser/15.4.2272.3000.11 Mobile/12B440 Safari/600.1.4",
)
testdata_access.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 16:16:11",
    stbx_ip="::ffff:178.64.134.57",
    cookies={
        "yandexuid": "2410530891459150418",
        "yp": "1518447753.andrid.91124fd3631f22e#2147483648.did.53b84e78dbf8e7aa77ac092137057bc3#2147483333.ybrod.3#1534104691.sz.640x360x2",
    },
    user_agent="Mozilla/5.0 (Linux; Android 4.4.2; Ixion ML145 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
)
testdata_access.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 16:16:11",
    stbx_ip="::ffff:178.64.134.57",
    cookies={
        "yandexuid": "2410530891459150418",
        "yp": "1518447753.andrid.91124fd3631f22e#2147483648.did.53b84e78dbf8e7aa77ac092137057bc4#2147484444.ybrod.3#1534104691.sz.640x360x2",
    },
    user_agent="Mozilla/5.0 (iPad; CPU OS 8_1_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) YaBrowser/15.4.2272.3000.11 Mobile/12B440 Safari/600.1.4",
)

testdata_access.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 16:16:11",
    stbx_ip="::ffff:178.64.134.57",
    cookies={
        "yandexuid": "2410530891459150411",
        "yp": "1518447753.andrid.91124fd3631f22e#2147483648.did.53b84e78dbf8e7aa77ac092137057bc1#2147481111.ybrod.3#1534104691.sz.640x360x2",
    },
    user_agent="Mozilla/5.0 (Linux; Android 4.4.2; Ixion ML145 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
)
testdata_access.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 16:26:11",
    stbx_ip="::ffff:178.64.134.57",
    cookies={
        "yandexuid": "2410530891459150412",
        "yp": "1518447753.andrid.91124fd3631f22e#2147483648.did.53b84e78dbf8e7aa77ac092137057bc1#2147481111.ybrod.3#1534104691.sz.640x360x2",
    },
    user_agent="Mozilla/5.0 (Linux; Android 4.4.2; Ixion ML145 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
)
testdata_access.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 16:16:11",
    stbx_ip="::ffff:178.64.134.57",
    cookies={
        "yandexuid": "2410530891459150413",
        "yp": "1518447753.andrid.91124fd3631f22e#2147483648.did.53b84e78dbf8e7aa77ac092137057bc2#2147482222.ybrod.3#1534104691.sz.640x360x2",
    },
    user_agent="Mozilla/5.0 (iPad; CPU OS 8_1_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) YaBrowser/15.4.2272.3000.11 Mobile/12B440 Safari/600.1.4",
)
testdata_access.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 16:26:11",
    stbx_ip="::ffff:178.64.134.57",
    cookies={
        "yandexuid": "2410530891459150414",
        "yp": "1518447753.andrid.91124fd3631f22e#2147483648.did.53b84e78dbf8e7aa77ac092137057bc2#2147482222.ybrod.3#1534104691.sz.640x360x2",
    },
    user_agent="Mozilla/5.0 (iPad; CPU OS 8_1_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) YaBrowser/15.4.2272.3000.11 Mobile/12B440 Safari/600.1.4",
)
testdata_access.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 16:16:11",
    stbx_ip="::ffff:178.64.134.57",
    cookies={
        "yandexuid": "2410530891459150415",
        "yp": "1518447753.andrid.91124fd3631f22e#2147483648.did.53b84e78dbf8e7aa77ac092137057bc3#2147483333.ybrod.3#1534104691.sz.640x360x2",
    },
    user_agent="Mozilla/5.0 (Linux; Android 4.4.2; Ixion ML145 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
)
testdata_access.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 16:16:11",
    stbx_ip="::ffff:178.64.134.57",
    cookies={
        "yandexuid": "2410530891459150416",
        "yp": "1518447753.andrid.91124fd3631f22e#2147483648.did.53b84e78dbf8e7aa77ac092137057bc4#2147484444.ybrod.3#1534104691.sz.640x360x2",
    },
    user_agent="Mozilla/5.0 (iPad; CPU OS 8_1_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) YaBrowser/15.4.2272.3000.11 Mobile/12B440 Safari/600.1.4",
)

testdata_access.add_row(
    _date="2016-04-11",
    cookies={"yandexuid": "2410530891459150000"},
    user_agent="Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36",
    request="/req.php?uuid=1234",
)
testdata_access.add_row(
    _date="2016-04-11",
    cookies={"yandexuid": "2410530891459150000"},
    user_agent="Mozilla/5.0 (iPad; CPU OS 8_1_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) YaBrowser/15.4.2272.3000.11 Mobile/12B440 Safari/600.1.4",
    request="/req.php?uuid=1235",
)

testdata_kp_access = AccessLog(
    path="//home/logfeller/logs/kinopoisk-tskv-front-log/1d", date="2016-04-11", default_data=ACCESS_LOG
)
testdata_kp_access.add_row(cookies={"refresh_yandexuid": "111", "yandexuid": "9574390901437926574", "uid": "5470947"})
testdata_kp_access.add_row(
    stbx_ip="::ffff:94.45.57.12", ip="94.45.57.12", cookies={"yandexuid": "7249027431455393740", "uid": "600251"}
)
testdata_kp_access.add_row(
    stbx_ip="::ffff:94.45.57.13",
    ip="94.45.57.13",
    cookies={"yandexuid": "6619110241447613888", "uid": "1104722"},
    raw_yandexuid="6619110241447613888",
)
testdata_kp_access.add_row(
    stbx_ip="::ffff:94.45.57.14",
    ip="94.45.57.14",
    cookies={"yandexuid": "1495205931451997969", "uid": "1104722"},
    raw_yandexuid="1495205931451997969",
)
testdata_kp_access.add_row(
    _date="2016-04-10",
    stbx_ip="::ffff:94.45.57.14",
    ip="94.45.57.14",
    cookies={"yandexuid": "1495205931451997969", "uid": "1104722"},
    raw_yandexuid="1495205931451997969",
)
testdata_kp_access.add_row(
    _date="2016-04-09",
    stbx_ip="::ffff:94.45.57.14",
    ip="94.45.57.14",
    cookies={"yandexuid": "1495205931451997969", "uid": "1104722"},
    raw_yandexuid="1495205931451997969",
)


testdata_export_access = BaseLog(path="//home/logfeller/logs/export-access-log/1d", date="2016-04-11", default_data={})
testdata_export_access.add_row(_date="2016-04-09", request="dummy", iso_eventtime="2016-04-11 16:45:11")
testdata_export_access.add_row(_date="2016-04-10", request="dummy", iso_eventtime="2016-04-11 16:45:11")
testdata_export_access.add_row(
    request="/status-ie.xml?yasoft=barie&ui={4C2F9790-32F7-470F-8453-7842E5EF2FAB}&ver=3.5.3.798&os=winnt&stat=dayuse",
    cookies="yandexuid=1002505171373436385; fuid01=51020d7f525d27b9.fYoBe4MNKtcmgmRw50Ik2CakPKUGtEZcxu-LmEVQ5yjhhPtTsxT_JzSMD7xC9D2Gf8x86Ej51llKXSBRkwxD3YTirxXqKk__MnvFe0bxi_QiO-haao2ruvHHYi6BOpQJ;",
    iso_eventtime="2016-04-11 16:45:11",
)
testdata_export_access.add_row(
    request="/status.xml?brandID=yandex&clid=2309176&fd=11.04.2018&lang=ru&os=win7x64&stat=dayuse&ui=9E6BF2CC-755C-4C5F-B488-8DEBDF1911AF&ver=5.3.0.1917&yandexuid=1002505171373436385&yasoft=winsearchbar",
    iso_eventtime="2016-04-11 16:45:11",
)
testdata_export_access.add_row(
    request="/status.xml?stat=background&last_end_time=1519023028&brandID=yandex-custo&partner_id=&eid=no_seed_trials.1%3Binst_date.1519022844&bitness=64&build=custo&user_agent=Mozilla%2F5.0+(Windows+NT+10.0%3B+WOW64)+AppleWebKit%2F537.36+(KHTML%2C+like+Gecko)+Chrome%2F62.0.3202.94+YaBrowser%2F17.11.1.988+Yowser%2F2.5+Safari%2F537.36&client_id=12592404104329662676&distr_yandexuid=2252707291499545371&win_version=10.0.16299&machine_id=9ead99728edc7a52a5949e60025e1313&ld=384&ud=384&df=0&yasoft=yabrowser&ui=015271F3-F1FD-408A-A5AC-04AD3EE164BF&ver=17.11.1.988&os=winnt&klid=2270452&show=1",
    iso_eventtime="2016-04-11 16:45:11",
)
testdata_export_access.add_row(
    request="/status.xml?yasoft=yabrowser&clid=2278634&ui={D3729ED1-82A2-4450-87A6-82C5A07F3A12}&ver=18.4.1.871&os=win81&stat=dayuse&banerid=6700153293_000000000000000000000000000000000031189&bitness=64&brandID=yandex-custo&brop=MSIE&build=custo&ckp=1508921661516170962.1002505171373436385&client_id=3713547718020817049&df=0&distr_yandexuid=&eid=ExpFeatureList2.1%3bHIPSPromoBaloon.2%3bPDO.1%3bPrInc.11%3bQUIC.Disabled%3bRedownload.1%3bTFLtext.1%3bacq.2%3bact.1%3bad_hide.disabled%3badr.1%3bajax.1%3balc.1%3bantishock_component.1%3barot2.99%3barot4.2%3batn.1%3bbad.9%3bbgfld.33%3bbgg.14%3bbra.1%3bbrort.1%3bcpa.1%3bcsh.1%3bdgr.2%3bdkn.1%3bdks.1%3bdnc.1%3bdsb.1%3bdss.1%3bdv1.1%3bebb.3%3bedt.1%3besa.1%3besb.1%3bfld.1%3bflt.2%3bfnp.5%3bfsy.1%3bgst.2%3bhdp.1%3bhe2.2%3bhistoryapi.1%3bhsh.3%3bhzla.1%3bipwd.1%3bisrp.6%3bmdc.11%3bmdls.1%3bmss.1%3bned.1%3bngb.1%3bnohw.1%3bnrg.1%3bnsr.1%3bnyatr.1%3bnzsv.1%3bois.1%3bpo1.3%3bpr1.1%3bprup.1%3bps3.1%3bpsm2.2%3bpsw.10%3breadability.1%3bsbm.2%3bsbn.1%3bsbu.2%3bsearch_extension.1%3bsi9.0%3bskf.1%3bslh.2%3bsmb.1%3bspcu.1%3bsrb.0%3bsrprompt.1%3bsxp.1%3btbln.1%3bte1.8%3btlr.3%3btnp.1%3btnw.0%3btpm.2%3btrsc.4%3buts.1%3bvvd.1%3bw1d.1%3bwpe.1%3bwsc.1%3bwzn.6%3bys2.1%3bzen.1%3bzmn.10%3bzvideo.4%3binst_date.1515733420&hips_installed=0.0.0.0&installed=18.4.1.871&ld=17&machine_id=30e36037f73d3bfb62446a82230f705f&partner_id=corporate&pf=1&pok=1&searchbandapp=0&sv=18.4&ud=17&user_agent=Mozilla/5.0%20(Windows%20NT%206.3%3b%20WOW64)%20AppleWebKit/537.36%20(KHTML%2c%20like%20Gecko)%20Chrome/65.0.3325.181%20YaBrowser/18.4.1.871%20Yowser/2.5%20Safari/537.36&uv=1.2.0.1831&win_version=6.3.9600&fd=30.01.2018",
    iso_eventtime="2016-04-11 16:45:11",
)
testdata_export_access.add_row(
    request="/status.xml?yasoft=punto&clid=41281&ui={259F8607-7162-4EA9-A26A-90355E7CF4A9}&ver=4.2.6.1275&os=winnt&stat=dayuse&launchesAsAdmin=0&osver=win10x64&usesAutocorrection=0&usesDiary=0&yb_installed=0&yu=685356651499547924&yu_ch=685356651499547924&yu_edge=8859911661458315891&fd=18.03.2016",
    iso_eventtime="2016-04-11 16:45:11",
)

testdata_kinopoisk = BaseLog(
    path="//crypta/production/state/graph/dicts", table_name="kinopoisk", default_data=KINOPOISK_LOG
)
testdata_kinopoisk.add_row(kp_uid=5470947, id_value="5470947")
testdata_kinopoisk.add_row(kp_uid=600251, id_value="600251")
testdata_kinopoisk.add_row(kp_uid=1104722, id_value="1104722", email="new_email@bbb.ru")
testdata_kinopoisk.add_row(kp_uid=1104722, id_value="1104722", email="new_email@bbb.ru")


testdata_autoru = BaseLog(path="//home/logfeller/logs/autoru-front-log/1d", date="2016-04-11", default_data=AUTORU_LOG)
testdata_autoru.add_row(user_yandex_uid="2257673341447602723", email="elavrenov@gmail.com", phone_number="79052795112")
testdata_autoru.add_row(
    user_yandex_uid="2417029131465202997", email="Ssimonovs3@gmail.com", phone_number="79160550044"
)
testdata_autoru.add_row(
    user_yandex_uid="2661050951457334113", email="alepko.i83@gmail.com", phone_number="79062634636"
)

testdata_autoru.add_row(
    user_yandex_uid="340781281465314773", email="akram.talibov@gmail.com", phone_number="89263699221"
)
testdata_autoru.add_row(user_yandex_uid="6167654001394007167", email="0818359@gmail.com", phone_number="79281960833")
testdata_autoru.add_row(
    user_yandex_uid="9640378551397559582", email="mlogvinenko@gmail.com", phone_number="79686354236"
)

testdata_autoru.add_row(
    user_yandex_uid="9885733981468347246", email="trifonova78@gmail.com", phone_number="79091502295"
)
testdata_autoru.add_row(
    user_yandex_uid="9885733981468347246", email="trifonova78@gmail.com", phone_number="79096517840"
)
testdata_autoru.add_row(user_yandex_uid="632886781434121228", email="lsksputnik@gmail.com", phone_number="79518545837")


testdata_sherlock_mail_vk_dump = BaseLog(
    path="//home/sherlock/export/crypta", table_name="vk_ids", default_data=MAIL_VKIDS_DAYDUMP  # nude pattern
)

testdata_sherlock_mail_vk_dump.add_row(uid=2460, vk_id="chingol")
testdata_sherlock_mail_vk_dump.add_row(uid=2687, vk_id="id174282522")
testdata_sherlock_mail_vk_dump.add_row(uid=2861, vk_id="cc00ffee")
testdata_sherlock_mail_vk_dump.add_row(uid=4611, vk_id="iliannaili")
testdata_sherlock_mail_vk_dump.add_row(uid=4880, vk_id="tulaman")
testdata_sherlock_mail_vk_dump.add_row(uid=5656, vk_id="kurbanov56")
testdata_sherlock_mail_vk_dump.add_row(uid=6075, vk_id="seredailya")
testdata_sherlock_mail_vk_dump.add_row(uid=6336, vk_id="giddom")
testdata_sherlock_mail_vk_dump.add_row(uid=6943, vk_id="id292652960")
testdata_sherlock_mail_vk_dump.add_row(uid=8322, vk_id="take_care_yourself")
testdata_sherlock_mail_vk_dump.add_row(uid=12355, vk_id="asmekh")

testdata_ditmsk_cookiematching = SingleTableLog(
    path="//crypta/production/cookie_matching/tags/ditmsk/state", default_data=DITMSK_COOKIEMATCHING
)

testdata_ditmsk_cookiematching.add_row(
    ext_id="CgMCH1U%2B%2F7MjAnoNBDpzAgA%3D", yuid="1907967751360172097", tag="ditmsk", timestamp=1525709103
)
testdata_ditmsk_cookiematching.add_row(
    ext_id="CgMCH1U%2B1ZMjAnoNBDm0AgA%3D", yuid="3502086411377199278", tag="ditmsk", timestamp=1525709103
)
testdata_ditmsk_cookiematching.add_row(
    ext_id="CgMCH1U%2B1ZMjAnoNBDm0AgA%3D", yuid="1817745901457477516", tag="ditmsk", timestamp=1525709103
)
testdata_ditmsk_cookiematching.add_row(
    ext_id="CgMCH1U%2B1ZMjAnoNBDm0AgA%3D", yuid="7086307901319910628", tag="ditmsk", timestamp=1525709103
)
testdata_ditmsk_cookiematching.add_row(
    ext_id="CgMCH1U%2B338jGXoLBDmrAgA%3D", yuid="8493830551509391626", tag="ditmsk", timestamp=1525709103
)

testdata_ditmsk_emails1 = SingleTableLog(
    path="//crypta/production/dmp/dmp-ditmsk/raw-schematized/emails/1493363073", default_data=DITMSK_DAY_DUMP
)
testdata_ditmsk_emails2 = SingleTableLog(
    path="//crypta/production/dmp/dmp-ditmsk/raw-schematized/emails/1493363075", default_data=DITMSK_DAY_DUMP
)

testdata_ditmsk_emails1.add_row(
    dit_id="00011d93b4f44c742ec2e5ef96dfe8b2",
    dit_cookie="CgMCH1U%2B%2F7MjAnoNBDpzAgA%3D",
    email_md5="4810e421f86f0c8093fe611cb89e67e4",
    date="2017-04-11",
    ts="234234",
    src="emails",
)
testdata_ditmsk_emails1.add_row(
    dit_id="00011d93b4f44c742ec2e5ef96dfe8b2",
    dit_cookie="CgMCH1U%2B1ZMjAnoNBDm0AgA%3D",
    email_md5="d03d8913d7627e1a9e19e69ddb2763d5",
    date="2017-04-11",
    ts="234234",
    src="emails",
)
testdata_ditmsk_emails2.add_row(
    dit_id="00029835e7bade3fe55df4ee8e4988f1",
    dit_cookie="CgMCH1U%2B1ZMjAnoNBDm0AgA%3D",
    email_md5="fb49df70e32e828b3fa39a777acba733",
    date="2017-04-11",
    ts="234234",
    src="emails",
)

testdata_ditmsk_phones1 = SingleTableLog(
    path="//crypta/production/dmp/dmp-ditmsk/raw-schematized/phones/1493363073", default_data=DITMSK_DAY_DUMP
)
testdata_ditmsk_phones2 = SingleTableLog(
    path="//crypta/production/dmp/dmp-ditmsk/raw-schematized/phones/1493363075", default_data=DITMSK_DAY_DUMP
)

testdata_ditmsk_phones1.add_row(
    dit_id="00028f32d33461ea05f050382cedf480",
    dit_cookie="CgMCH1U%2B1ZMjAnoNBDm0AgA%3D",
    phone_md5="75fac33c1c22838a2e08a142496d09cc",
    date="2017-04-11",
    ts="234234",
    src="phones",
)
testdata_ditmsk_phones2.add_row(
    dit_id="00011d93b4f44c742ec2e5ef96dfe8b2",
    dit_cookie="CgMCH1U%2B338jGXoLBDmrAgA%3D",
    phone_md5="bddbaa034bea7aa369753d7a751d03a5",
    date="2017-04-11",
    ts="234234",
    src="phones",
)
testdata_ditmsk_phones2.add_row(
    dit_id="11111d93b4f44c742ec2e5ef96dfe8b2",
    dit_cookie="XXMCH1U%2B338jGXoLBDmrAgA%3D",
    phone_md5="40ee5690438a7da84802f789829de1dc",
    date="2017-04-11",
    ts="234234",
    src="phones",
)


WATCH_LOG_MAILRU = [
    ("wp/Cm8KqwpPCm8KewpFja2JqcsKfwpPCm8KeYMKkwqc=", "mixail_1908@mail.ru"),
    ("wpPCoMKWwqTCpcKXwqRywp/Ck8Kbwp5gwqTCpw==", "andrser@mail.ru"),
    ("wqLCnsKhwqbCkcKfcsKfwpPCm8KeYMKkwqc=", "plot_m@mail.ru"),
]
WATCH_LOG_VK = [("ZmVoY2VnZmdl", "436135453"), ("Y2dqZmRkaWpr", "158422789"), ("aWVrYmtrZmI=", "73909940")]
WATCH_LOG_OK = [
    ("ZmllZ2JnZWtlamJn", "473505393805"),
    ("Z2dnaWtpaGpiZWlr", "555797680379"),
    ("Z2dqaGRmamtjYmpp", "558624891087"),
]
WATCH_LOG_AVITO = [("aWNrZ2RoZms=", "71952649"), ("aWlrY2JpZWQ=", "77910732"), ("aWhkZWtrYg==", "7623990")]
testdata_watch_log = BaseLog(path="//home/logfeller/logs/bs-watch-log/1d", date="2016-04-11", default_data=WATCH_LOG)

testdata_watch_log.add_row(
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    remoteip6="::ffff:127.0.0.1",
    regionid="168",
    url="https://e.mail.ru/thread/1:c526343afd3058be:0/",
    clientip6="::ffff:46.130.14.174",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    uniqid="601826891455541112",
    cookiei="601826891455541112",
    watchid="4340609126817335168",
)
testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:mue:supervnyki@mail.ru:z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455541113",
    cookiei="601826891455541113",
    watchid="4344315929855004562",
)
testdata_watch_log.add_row(
    eventtime="1469820260",
    browserinfo="s:1366x768x24:sk:1:f:22.0.0:fpr:280672867801:cn:1:w:1349x668:mue:kds-82@mail.ru:z:480:i:20160730032409:et:1469820249:en:utf-8:v:700:c:1:la:ru:ar:1:pv:1:ls:531503482006:rqn:502:rn:227642568:hid:887360742:ds:5,181,172,168,0,0,,2582,2051,,,,2950:fp:248776783:rqnl:1:st:1469820249:u:1462190808985248003",
    unixtime="1469820260",
    source_uri="prt://yabs-rt@2a02:6b8:0:872:0:0:3c:4unknown",
    headerargs="293: fuid01=5460f0de62f7a7e9.6BvdCMm3qI52ew7dJWd6bdjU3T2RHc47MFs-DLpo0yuPub7SqmOj11z5NXD7KAL4VXV8r2bKC3mo7IdIMPbFrM7Ds1PK5C4GYDTumAzXhXfLhUqqBXsc6wC-RG0V561h; L=AV9DeGtlYEdccnJDBkpwaEcNf3sHSQZ1AQMXE19FASkDWQZ8.1443641139.11951.333685.576ba9e9b2bef5f0f2ab117888006fcd; yandex_login=saprovec2015; yandexuid=658314781415709679; _ym_uid=1456040833775074822; __utma=190882677.1177133303.1466535850.1466535850.1466535850.1; __utmz=190882677.1466535850.1.1.utmcsr=paymaster.ru|utmccn=(referral)|utmcmd=referral|utmcct=/ru-RU/Payment/Process/f95d4245-e78c-4994-aebc-a54b5b9987e7; __utmv=190882677.|3=Login=Yes=1; yabs-sid=2603634431469652581; Cookie_check=CheckCookieCheckCookie; Session_id=3:1469737686.5.0.1443641139373:b8ZK1A:10.0|336319132.0.2|149276.602758.liwIatMvJXdoCgLaWKxILye7sOM; sessionid2=3:1469737686.5.0.1443641139373:b8ZK1A:10.1|336319132.0.2|149276.200975._8R45orwfLFn8EaiuCZRpuKrYK4; yabs-frequency=/4/0W0c0A4OarV97OvN/LIDoS8Gb8G00/; yp=1759001139.udn.cDpzYXByb3ZlYzIwMTU%3D#1483510635.szm.1%3A1366x768%3A1366x668#1499386537.st_soft_stripe_s.74#1499387100.st_promobar_s.16#1495981786.st_browser_s.12#1497119368.st_browser_cl.1#1469978503.gpauto.53_363037299999995%3A83_6906791%3A6325%3A1%3A1469805703#1469892107.nps.8069216278%3Aclose; ys=wprid.1469807003881051-17626762553484740692140308-sas1-1990; _ym_isad=2",
    referer="https://mail.ru/",
    remoteip="127.0.0.1",
    iso_eventtime="2016-04-10 21:14:20",
    funiqid="6080124334883973097",
    cookiegpauto="53_363037299999995:83_6906791:6325:1:1469805703",
    remoteip6="::ffff:127.0.0.1",
    regionid="197",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2603634431469652581",
    clientip6="95.78.13.90",
    useragent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41",
    uniqid="601826891455541113",
    cookiei="601826891455541113",
    watchid="4344318555701903480",
)
testdata_watch_log.add_row(
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    remoteip6="::ffff:127.0.0.1",
    regionid="168",
    url="https://e.mail.ru/thread/1:c526343afd3058be:0/",
    clientip6="::ffff:46.130.14.174",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    uniqid="601826891455542222",
    cookiei="601826891455542222",
    watchid="4340609126817335168",
)
testdata_watch_log.add_row(
    remoteip="127.0.0.1",
    clientip="176.59.7.165",
    remoteip6="::ffff:127.0.0.1",
    regionid="168",
    url="https://e.mail.ru/thread/1:c526343afd3058be:0/",
    clientip6="::ffff:46.130.14.174",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    uniqid="601826891455541114",
    cookiei="601826891455541114",
    watchid="4340609126817335168",
)
testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:mue:supervnyki@mail.ru:z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="176.59.7.165",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455541115",
    cookiei="601826891455541115",
    watchid="4344315929855004562",
)
testdata_watch_log.add_row(
    eventtime="1469820260",
    browserinfo="s:1366x768x24:sk:1:f:22.0.0:fpr:280672867801:cn:1:w:1349x668:mue:kds-82@mail.ru:z:480:i:20160730032409:et:1469820249:en:utf-8:v:700:c:1:la:ru:ar:1:pv:1:ls:531503482006:rqn:502:rn:227642568:hid:887360742:ds:5,181,172,168,0,0,,2582,2051,,,,2950:fp:248776783:rqnl:1:st:1469820249:u:1462190808985248003",
    unixtime="1469820260",
    source_uri="prt://yabs-rt@2a02:6b8:0:872:0:0:3c:4unknown",
    headerargs="293: fuid01=5460f0de62f7a7e9.6BvdCMm3qI52ew7dJWd6bdjU3T2RHc47MFs-DLpo0yuPub7SqmOj11z5NXD7KAL4VXV8r2bKC3mo7IdIMPbFrM7Ds1PK5C4GYDTumAzXhXfLhUqqBXsc6wC-RG0V561h; L=AV9DeGtlYEdccnJDBkpwaEcNf3sHSQZ1AQMXE19FASkDWQZ8.1443641139.11951.333685.576ba9e9b2bef5f0f2ab117888006fcd; yandex_login=saprovec2015; yandexuid=658314781415709679; _ym_uid=1456040833775074822; __utma=190882677.1177133303.1466535850.1466535850.1466535850.1; __utmz=190882677.1466535850.1.1.utmcsr=paymaster.ru|utmccn=(referral)|utmcmd=referral|utmcct=/ru-RU/Payment/Process/f95d4245-e78c-4994-aebc-a54b5b9987e7; __utmv=190882677.|3=Login=Yes=1; yabs-sid=2603634431469652581; Cookie_check=CheckCookieCheckCookie; Session_id=3:1469737686.5.0.1443641139373:b8ZK1A:10.0|336319132.0.2|149276.602758.liwIatMvJXdoCgLaWKxILye7sOM; sessionid2=3:1469737686.5.0.1443641139373:b8ZK1A:10.1|336319132.0.2|149276.200975._8R45orwfLFn8EaiuCZRpuKrYK4; yabs-frequency=/4/0W0c0A4OarV97OvN/LIDoS8Gb8G00/; yp=1759001139.udn.cDpzYXByb3ZlYzIwMTU%3D#1483510635.szm.1%3A1366x768%3A1366x668#1499386537.st_soft_stripe_s.74#1499387100.st_promobar_s.16#1495981786.st_browser_s.12#1497119368.st_browser_cl.1#1469978503.gpauto.53_363037299999995%3A83_6906791%3A6325%3A1%3A1469805703#1469892107.nps.8069216278%3Aclose; ys=wprid.1469807003881051-17626762553484740692140308-sas1-1990; _ym_isad=2",
    referer="https://mail.ru/",
    remoteip="127.0.0.1",
    iso_eventtime="2016-04-10 21:14:20",
    funiqid="6080124334883973097",
    cookiegpauto="53_363037299999995:83_6906791:6325:1:1469805703",
    remoteip6="::ffff:127.0.0.1",
    regionid="197",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2603634431469652581",
    clientip6="46.147.147.143",
    useragent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41",
    uniqid="601826891455541115",
    cookiei="601826891455541115",
    watchid="4344318555701903480",
)
testdata_watch_log.add_row(
    remoteip="127.0.0.1",
    clientip="176.59.7.165",
    remoteip6="::ffff:127.0.0.1",
    regionid="168",
    url="https://e.mail.ru/thread/1:c526343afd3058be:0/",
    clientip6="::ffff:46.130.14.174",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    uniqid="601826891455542224",
    cookiei="601826891455542224",
    watchid="4340609126817335168",
)
testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:mue:supervnyki@mail.ru:z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="176.59.7.165",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455542225",
    cookiei="601826891455542225",
    watchid="4344315929855004562",
)
testdata_watch_log.add_row(
    uniqid="10113701463508333",
    cookiei="10113701463508333",
    browserinfo="di:"
    + metrica_crypt(
        '{"android_id":"62ce0fd6fb939cf2","google_aid":"1190cb06-34ea-4995-8a68-8ceed2e55760","device_id":"a6d1d72b02f423452320a5901cd1c4c6","uuid":"35a6b993b6771036cda94cf37d507d6c"}',
        rsa,
    ),
)

testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:mue:"
    + WATCH_LOG_MAILRU[0][0]
    + ":z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455547777",
    cookiei="601826891455547777",
    watchid="4344315929855004777",
)
testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:mue:"
    + WATCH_LOG_MAILRU[1][0]
    + ":z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455547777",
    cookiei="601826891455547777",
    watchid="4344315929855004777",
)
testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:mue:"
    + WATCH_LOG_MAILRU[2][0]
    + ":z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455547777",
    cookiei="601826891455547777",
    watchid="4344315929855004777",
)

testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:vid:"
    + WATCH_LOG_VK[0][0]
    + ":z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455547777",
    cookiei="601826891455547777",
    watchid="4344315929855004777",
)
testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:vid:"
    + WATCH_LOG_VK[1][0]
    + ":z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455547777",
    cookiei="601826891455547777",
    watchid="4344315929855004777",
)
testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:vid:"
    + WATCH_LOG_VK[2][0]
    + ":z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455547777",
    cookiei="601826891455547777",
    watchid="4344315929855004777",
)

testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:oid:"
    + WATCH_LOG_OK[0][0]
    + ":z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455547777",
    cookiei="601826891455547777",
    watchid="4344315929855004777",
)
testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:oid:"
    + WATCH_LOG_OK[1][0]
    + ":z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455547777",
    cookiei="601826891455547777",
    watchid="4344315929855004777",
)
testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:oid:"
    + WATCH_LOG_OK[2][0]
    + ":z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455547777",
    cookiei="601826891455547777",
    watchid="4344315929855004777",
)

testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:oid:"
    + WATCH_LOG_OK[2][0]
    + ":z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.yandex.ru/messages/inbox/uuid=aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0/blabla",
    sessid="2511260481469708000",
    clientip6="::ffff:83.149.47.33",
    useragent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
    uniqid="601826891455547111",
    cookiei="601826891455547111",
    watchid="4344315929855004111",
)
testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:oid:"
    + WATCH_LOG_OK[2][0]
    + ":z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.yandex.ru/messages/inbox/uuid=bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb0/blabla",
    clientip6="::ffff:83.149.47.33",
    useragent="Mozilla/5.0 (Windows NT 6.4; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455547111",
    cookiei="601826891455547111",
    watchid="4344315929855004111",
)

testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:aid:"
    + WATCH_LOG_AVITO[0][0]
    + ":z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455547777",
    cookiei="601826891455547777",
    watchid="4344315929855004777",
)
testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:aid:"
    + WATCH_LOG_AVITO[1][0]
    + ":z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455547777",
    cookiei="601826891455547777",
    watchid="4344315929855004777",
)
testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:aid:"
    + WATCH_LOG_AVITO[2][0]
    + ":z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=8935802481455087719; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36",
    uniqid="601826891455547777",
    cookiei="601826891455547777",
    watchid="4344315929855004777",
)
testdata_watch_log.add_row(
    eventtime="1469820260",
    browserinfo="s:1366x768x24:sk:1:f:22.0.0:fpr:280672867801:cn:1:w:1349x668:mue:kds-82@mail.ru:z:480:i:20160730032409:et:1469820249:en:utf-8:v:700:c:1:la:ru:ar:1:pv:1:ls:531503482006:rqn:502:rn:227642568:hid:887360742:ds:5,181,172,168,0,0,,2582,2051,,,,2950:fp:248776783:rqnl:1:st:1469820249:u:1462190808985248003",
    unixtime="1469820260",
    source_uri="prt://yabs-rt@2a02:6b8:0:872:0:0:3c:4unknown",
    headerargs="293: fuid01=5460f0de62f7a7e9.6BvdCMm3qI52ew7dJWd6bdjU3T2RHc47MFs-DLpo0yuPub7SqmOj11z5NXD7KAL4VXV8r2bKC3mo7IdIMPbFrM7Ds1PK5C4GYDTumAzXhXfLhUqqBXsc6wC-RG0V561h; L=AV9DeGtlYEdccnJDBkpwaEcNf3sHSQZ1AQMXE19FASkDWQZ8.1443641139.11951.333685.576ba9e9b2bef5f0f2ab117888006fcd; yandex_login=login-for-avito; yandexuid=10113701463508409; _ym_uid=1456040833775074822; __utma=190882677.1177133303.1466535850.1466535850.1466535850.1; __utmz=190882677.1466535850.1.1.utmcsr=paymaster.ru|utmccn=(referral)|utmcmd=referral|utmcct=/ru-RU/Payment/Process/f95d4245-e78c-4994-aebc-a54b5b9987e7; __utmv=190882677.|3=Login=Yes=1; yabs-sid=2603634431469652581; Cookie_check=CheckCookieCheckCookie; Session_id=3:1469737686.5.0.1443641139373:b8ZK1A:10.0|336319132.0.2|149276.602758.liwIatMvJXdoCgLaWKxILye7sOM; sessionid2=3:1469737686.5.0.1443641139373:b8ZK1A:10.1|336319132.0.2|149276.200975._8R45orwfLFn8EaiuCZRpuKrYK4; yabs-frequency=/4/0W0c0A4OarV97OvN/LIDoS8Gb8G00/; yp=1759001139.udn.cDpzYXByb3ZlYzIwMTU%3D#1483510635.szm.1%3A1366x768%3A1366x668#1499386537.st_soft_stripe_s.74#1499387100.st_promobar_s.16#1495981786.st_browser_s.12#1497119368.st_browser_cl.1#1469978503.gpauto.53_363037299999995%3A83_6906791%3A6325%3A1%3A1469805703#1469892107.nps.8069216278%3Aclose; ys=wprid.1469807003881051-17626762553484740692140308-sas1-1990; _ym_isad=2",
    referer="https://mail.ru/",
    remoteip="127.0.0.1",
    iso_eventtime="2016-04-10 21:14:20",
    funiqid="6080124334883973097",
    cookiegpauto="53_363037299999995:83_6906791:6325:1:1469805703",
    remoteip6="::ffff:127.0.0.1",
    regionid="197",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2603634431469652581",
    clientip6="95.78.13.90",
    useragent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41",
    uniqid="10113701463508409",
    cookiei="601826891455541113",
    watchid="4344318555701903480",
)
testdata_watch_log.add_row(
    eventtime="1469820260",
    browserinfo="s:1366x768x24:sk:1:f:22.0.0:fpr:280672867801:cn:1:w:1349x668:mue:kds-82@mail.ru:z:480:i:20160730032409:et:1469820249:en:utf-8:v:700:c:1:la:ru:ar:1:pv:1:ls:531503482006:rqn:502:rn:227642568:hid:887360742:ds:5,181,172,168,0,0,,2582,2051,,,,2950:fp:248776783:rqnl:1:st:1469820249:u:1462190808985248003",
    unixtime="1469820260",
    source_uri="prt://yabs-rt@2a02:6b8:0:872:0:0:3c:4unknown",
    headerargs="293: fuid01=5460f0de62f7a7e9.6BvdCMm3qI52ew7dJWd6bdjU3T2RHc47MFs-DLpo0yuPub7SqmOj11z5NXD7KAL4VXV8r2bKC3mo7IdIMPbFrM7Ds1PK5C4GYDTumAzXhXfLhUqqBXsc6wC-RG0V561h; L=AV9DeGtlYEdccnJDBkpwaEcNf3sHSQZ1AQMXE19FASkDWQZ8.1443641139.11951.333685.576ba9e9b2bef5f0f2ab117888006fcd; yandex_login=LOGIN-FOR-AVITO; yandexuid=10113701463508409; _ym_uid=1456040833775074822; __utma=190882677.1177133303.1466535850.1466535850.1466535850.1; __utmz=190882677.1466535850.1.1.utmcsr=paymaster.ru|utmccn=(referral)|utmcmd=referral|utmcct=/ru-RU/Payment/Process/f95d4245-e78c-4994-aebc-a54b5b9987e7; __utmv=190882677.|3=Login=Yes=1; yabs-sid=2603634431469652581; Cookie_check=CheckCookieCheckCookie; Session_id=3:1469737686.5.0.1443641139373:b8ZK1A:10.0|336319132.0.2|149276.602758.liwIatMvJXdoCgLaWKxILye7sOM; sessionid2=3:1469737686.5.0.1443641139373:b8ZK1A:10.1|336319132.0.2|149276.200975._8R45orwfLFn8EaiuCZRpuKrYK4; yabs-frequency=/4/0W0c0A4OarV97OvN/LIDoS8Gb8G00/; yp=1759001139.udn.cDpzYXByb3ZlYzIwMTU%3D#1483510635.szm.1%3A1366x768%3A1366x668#1499386537.st_soft_stripe_s.74#1499387100.st_promobar_s.16#1495981786.st_browser_s.12#1497119368.st_browser_cl.1#1469978503.gpauto.53_363037299999995%3A83_6906791%3A6325%3A1%3A1469805703#1469892107.nps.8069216278%3Aclose; ys=wprid.1469807003881051-17626762553484740692140308-sas1-1990; _ym_isad=2",
    referer="https://mail.ru/",
    remoteip="127.0.0.1",
    iso_eventtime="2016-04-10 21:14:20",
    funiqid="6080124334883973097",
    cookiegpauto="53_363037299999995:83_6906791:6325:1:1469805703",
    remoteip6="::ffff:127.0.0.1",
    regionid="197",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2603634431469652581",
    clientip6="95.78.13.90",
    useragent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41",
    uniqid="10113701463508409",
    cookiei="601826891455541113",
    watchid="4344318555701903480",
)
testdata_watch_log.add_row(
    eventtime="1469820261",
    browserinfo="s:1366x768x24:sk:1:f:22.0.0:fpr:280672867801:cn:1:w:1349x668:mue:kds-82@mail.ru:z:480:i:20160730032409:et:1469820249:en:utf-8:v:700:c:1:la:ru:ar:1:pv:1:ls:531503482006:rqn:502:rn:227642568:hid:887360742:ds:5,181,172,168,0,0,,2582,2051,,,,2950:fp:248776783:rqnl:1:st:1469820249:u:1462190808985248003",
    unixtime="1469820260",
    source_uri="prt://yabs-rt@2a02:6b8:0:872:0:0:3c:4unknown",
    headerargs="293: fuid01=5460f0de62f7a7e9.6BvdCMm3qI52ew7dJWd6bdjU3T2RHc47MFs-DLpo0yuPub7SqmOj11z5NXD7KAL4VXV8r2bKC3mo7IdIMPbFrM7Ds1PK5C4GYDTumAzXhXfLhUqqBXsc6wC-RG0V561h; L=AV9DeGtlYEdccnJDBkpwaEcNf3sHSQZ1AQMXE19FASkDWQZ8.1443641139.11951.333685.576ba9e9b2bef5f0f2ab117888006fcd; yandex_login=login.like.email.ok@my.awesom.com; yandexuid=10113701463508409; _ym_uid=1456040833775074822; __utma=190882677.1177133303.1466535850.1466535850.1466535850.1; __utmz=190882677.1466535850.1.1.utmcsr=paymaster.ru|utmccn=(referral)|utmcmd=referral|utmcct=/ru-RU/Payment/Process/f95d4245-e78c-4994-aebc-a54b5b9987e7; __utmv=190882677.|3=Login=Yes=1; yabs-sid=2603634431469652581; Cookie_check=CheckCookieCheckCookie; Session_id=3:1469737686.5.0.1443641139373:b8ZK1A:10.0|336319132.0.2|149276.602758.liwIatMvJXdoCgLaWKxILye7sOM; sessionid2=3:1469737686.5.0.1443641139373:b8ZK1A:10.1|336319132.0.2|149276.200975._8R45orwfLFn8EaiuCZRpuKrYK4; yabs-frequency=/4/0W0c0A4OarV97OvN/LIDoS8Gb8G00/; yp=1759001139.udn.cDpzYXByb3ZlYzIwMTU%3D#1483510635.szm.1%3A1366x768%3A1366x668#1499386537.st_soft_stripe_s.74#1499387100.st_promobar_s.16#1495981786.st_browser_s.12#1497119368.st_browser_cl.1#1469978503.gpauto.53_363037299999995%3A83_6906791%3A6325%3A1%3A1469805703#1469892107.nps.8069216278%3Aclose; ys=wprid.1469807003881051-17626762553484740692140308-sas1-1990; _ym_isad=2",
    referer="https://mail.ru/",
    remoteip="127.0.0.1",
    iso_eventtime="2016-04-10 21:14:20",
    funiqid="6080124334883973097",
    cookiegpauto="53_363037299999995:83_6906791:6325:1:1469805703",
    remoteip6="::ffff:127.0.0.1",
    regionid="197",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2603634431469652581",
    clientip6="95.78.13.90",
    useragent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41",
    uniqid="10113701463508401",
    cookiei="601826891455541111",
    watchid="4344318555701903480",
)
testdata_watch_log.add_row(
    eventtime="1469820262",
    browserinfo="s:1366x768x24:sk:1:f:22.0.0:fpr:280672867801:cn:1:w:1349x668:mue:kds-82@mail.ru:z:480:i:20160730032409:et:1469820249:en:utf-8:v:700:c:1:la:ru:ar:1:pv:1:ls:531503482006:rqn:502:rn:227642568:hid:887360742:ds:5,181,172,168,0,0,,2582,2051,,,,2950:fp:248776783:rqnl:1:st:1469820249:u:1462190808985248003",
    unixtime="1469820260",
    source_uri="prt://yabs-rt@2a02:6b8:0:872:0:0:3c:4unknown",
    headerargs="293: fuid01=5460f0de62f7a7e9.6BvdCMm3qI52ew7dJWd6bdjU3T2RHc47MFs-DLpo0yuPub7SqmOj11z5NXD7KAL4VXV8r2bKC3mo7IdIMPbFrM7Ds1PK5C4GYDTumAzXhXfLhUqqBXsc6wC-RG0V561h; L=AV9DeGtlYEdccnJDBkpwaEcNf3sHSQZ1AQMXE19FASkDWQZ8.1443641139.11951.333685.576ba9e9b2bef5f0f2ab117888006fcd; yandex_login=login.like.email.bad@xyandex.ru; yandexuid=10113701463508409; _ym_uid=1456040833775074822; __utma=190882677.1177133303.1466535850.1466535850.1466535850.1; __utmz=190882677.1466535850.1.1.utmcsr=paymaster.ru|utmccn=(referral)|utmcmd=referral|utmcct=/ru-RU/Payment/Process/f95d4245-e78c-4994-aebc-a54b5b9987e7; __utmv=190882677.|3=Login=Yes=1; yabs-sid=2603634431469652581; Cookie_check=CheckCookieCheckCookie; Session_id=3:1469737686.5.0.1443641139373:b8ZK1A:10.0|336319132.0.2|149276.602758.liwIatMvJXdoCgLaWKxILye7sOM; sessionid2=3:1469737686.5.0.1443641139373:b8ZK1A:10.1|336319132.0.2|149276.200975._8R45orwfLFn8EaiuCZRpuKrYK4; yabs-frequency=/4/0W0c0A4OarV97OvN/LIDoS8Gb8G00/; yp=1759001139.udn.cDpzYXByb3ZlYzIwMTU%3D#1483510635.szm.1%3A1366x768%3A1366x668#1499386537.st_soft_stripe_s.74#1499387100.st_promobar_s.16#1495981786.st_browser_s.12#1497119368.st_browser_cl.1#1469978503.gpauto.53_363037299999995%3A83_6906791%3A6325%3A1%3A1469805703#1469892107.nps.8069216278%3Aclose; ys=wprid.1469807003881051-17626762553484740692140308-sas1-1990; _ym_isad=2",
    referer="https://mail.ru/",
    remoteip="127.0.0.1",
    iso_eventtime="2016-04-10 21:14:20",
    funiqid="6080124334883973097",
    cookiegpauto="53_363037299999995:83_6906791:6325:1:1469805703",
    remoteip6="::ffff:127.0.0.1",
    regionid="197",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2603634431469652581",
    clientip6="95.78.13.90",
    useragent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.106 Safari/537.36 OPR/38.0.2220.41",
    uniqid="10113701463508402",
    cookiei="601826891455541112",
    watchid="4344318555701903480",
)
testdata_watch_log.add_row(
    remoteip="127.0.0.1",
    clientip="217.118.90.186",
    remoteip6="::ffff:127.0.0.1",
    regionid="168",
    url="https://e.mail.ru/thread/1:c526343afd3058be:0/",
    clientip6="::ffff:46.130.14.174",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    uniqid="very_invalid_yandexuid",
    cookiei="601826891455541112",
    watchid="4340609126817335168",
)

# graphv2 data for radius metrics
# 1748232901455365413
testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:mue:234@mail.ru:z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=1748232901455365413; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="176.59.7.165",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7146 Yowser/2.5 Safari/537.36",
    uniqid="1748232901455365413",
    cookiei="1748232901455365413",
    watchid="1748232901455365413",
)
# 5726075371455990918
testdata_watch_log.add_row(
    eventtime="1469820250",
    browserinfo="s:1366x768x24:sk:1:adb:2:f:22.0.0:fpr:352710240801:cn:1:w:1349x667:mue:2sdfs@mail.ru:z:180:i:20160729222410:et:1469820251:en:utf-8:v:700:c:1:la:ru:ar:1:nb:1:cl:198:ls:769919691117:rqn:202:rn:854091256:hid:144894966:ds:,,,,,,,8922,2331,18611,18611,12,11861:rqnl:1:st:1469820251:u:1455182046612414",
    unixtime="1469820250",
    source_uri="prt://yabs-rt@bsmc20e.yandex.ru/home/yabs/server/log.rtmr/watch.1.tskv",
    headerargs="293: fuid01=56bae06c543fb6d6.CrKHjKDmLMiYqT05ef9YMqmAYS1eny_X1Or_PCm3_mrMzrPYhd-SF4FxmfXQ_wNTihE5qfVy3zyQLl0ji0yVQYomLfJgvaei5VI_MVC-P65qo6ps3ZHNiDvu2kf13Vob; yandexuid=5726075371455990918; spravka=dD0xNDM1ODQ0NTE2O2k9ODMuMTQ5LjQ2LjIwMTt1PTE0MzU4NDQ1MTY1MzE4MDMxMjU7aD02NDQxOWE3OTJjYmEyYjYwNGE3MDQ1NWVlMThiNGFmMA==; yandex_gid=6; yabs-frequency=/4/1m0G0FZMc5TpoPTN/AIvoS7mbDGSeSd1y9IT0xHjFVoLS0002sInoS70bRiv5i71m9MUm979mO2LLGoXoS5WbLG00/; zm=m-white_bender.flex.webp.css-https%3Awww_3hm-b9Ujf4TQ9bJ2osYjORCueWs%3Al; yabs-sid=2511260481469708320; _ym_uid=1469732738856911669; yp=1500184898.dsws.22#1500184898.dswa.0#1485500740.szm.1_00:1366x768:1366x667#1474977291.ww.1#1500738139.st_soft_stripe_s.70#1494429862.st_set_s.41#1491844864.st_home_s.8#1494328734.st_promobar_s.7#1500116922.st_vb_s.10#1493128273.st_set_home_s.13#1498304852.dwss.55#1500184898.dwys.27#1499866280.st_browser_s.25#1469990458.clh.1975319#1499868603.st_browser_cl.1#1472229142.ygu.1; _ga=GA1.2.913351433.1469732761; ys=homesearchextchrome.8-19-0#wprid.1469732558021614-242874010728671787017346-man1-3572",
    remoteip="127.0.0.1",
    clientip="176.59.7.165",
    iso_eventtime="2016-04-10 21:14:10",
    funiqid="6249554188804536022",
    remoteip6="::ffff:127.0.0.1",
    url="https://e.mail.ru/messages/inbox/",
    sessid="2511260481469708320",
    clientip6="::ffff:83.149.47.85",
    useragent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
    uniqid="5726075371455990918",
    cookiei="5726075371455990918",
    watchid="5726075371455990918",
)


# social ids from watch log
testdata_watch_log.add_row(
    browserinfo="s:800x600x32:sk:1:fpr:401684195901:cn:1:w:749x478:vid:Z2NpY2dn:z:180:i:20161002224303:et:1475437384:en:windows-1251:v:723:c:1:la",
    uniqid="10113701463508503",
    cookiei="10113701463508503",
)
testdata_watch_log.add_row(
    browserinfo="j:1:s:1920x1080x24:sk:1:adb:2:f:23.0.0.162:fpr:319325903901:cn:1:w:1903x914:vid:Z2NpY2dn:z:180:i:20161002120351:et:1475399031:en:windows-1251:v:723:c:1:la:ru-ru:ar",
    uniqid="10113701463508504",
    cookiei="10113701463508504",
)
testdata_watch_log.add_row(
    browserinfo="j:1:s:1920x1080x24:sk:1:adb:2:f:23.0.0.162:oid:319901:cn:1:w:1903x914:vid:Z2NpY2dn:z:180:i:20161002120351:et:1475399031:en:windows-1251:v:723:c:1:la:ru-ru:ar",
    uniqid="10113701463508503",
    cookiei="10113701463508503",
)
testdata_watch_log.add_row(
    browserinfo="j:1:s:1920x1080x24:sk:1:adb:2:f:23.0.0.162:aid:303901:cn:1:w:1903x914:vid:aGloaWljamY=:z:180:i:20161002120351:et:1475399031:en:windows-1251:v:723:c:1:la:ru-ru:ar",
    uniqid="10113701463508503",
    cookiei="10113701463508503",
)

# vmetro testing
watch_log_vmetro_mac_addresses = ["f4-09-d8-d1-a3-73", "d8-3c-69-31-2f-a4"]
mobile_metrika_mac_names = '["wlan0","p2p0","ifb0","ifb1","ccmni0","ccmni1","ccmni2"]'
mobile_metrika_macs = '["F4:09:D8:D1:A3:73","A2:32:99:5C:F8:DC","12:CE:92:EF:0F:34","2E:19:E3:6E:4C:1D","1A:47:AF:49:B4:7E","7E:2F:30:09:E1:29","BE:FA:1B:05:5F:17"]'
# f4-09-d8-d1-a3-73 should intersect with F4:09:D8:D1:A3:73
vmetro_yuid1 = "10113701463508500"
vmetro_yuid2 = "10113701463508501"

testdata_watch_log.add_row(
    browserinfo="s:800x600x32:sk:1:fpr:401684195901:cn:1:w:749x478:et:1475437384:en:windows-1251:v:723:c:1:la",
    referer="https://login.wi-fi.ru/am/UI/Login?org=mac&service=coa&client_mac=%s&ForceAuth=true"
    % watch_log_vmetro_mac_addresses[0],
    uniqid=vmetro_yuid1,
)
testdata_watch_log.add_row(
    browserinfo="j:1:s:1920x1080x24:sk:1:adb:2:f:23.0.0.162:fpr:319325903901:cn:1:w:1903x914:z:180:i:20161002120351:et:1475399031:en:windows-1251:v:723:c:1:la:ru-ru:ar",
    referer="http://login.wi-fi.ru/am/UI/Login?org=mac&service=coa&client_mac=%s&ForceAuth=true"
    % watch_log_vmetro_mac_addresses[1],
    uniqid=vmetro_yuid2,
)
# purchase
testdata_watch_log.add_row(
    uniqid="10113701463508334",
    cookiei="10113701463508334",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-11 12:44:46",
    params='{"__ym":{"ecommerce":[{"purchase":{"actionField":{"id":251407497},"products":[{"id":1112223,"price":"5348","quantity":1}]}}]}}',
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144441:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ar:1:ls:1420450784463:rqn:2407:rn:268872224:hid:934923750:ds:,,,,,,,280,,,,,690:rqnl:1:st:1460281481:u:1455701456680556087",
    timestamp="2016-04-10 12:44:46",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    source_uri="prt://yabs-rt@bsmc17e.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
    remoteip6="::ffff:127.0.0.1",
    url="goal://elka2016-vk.ereality.org/flash_start",
)
testdata_watch_log.add_row(
    uniqid="10113701463508334",
    cookiei="10113701463508334",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-11 12:44:46",
    params='{"__ym":{"ecommerce":[{"purchase":{"actionField":{"id":"0033349283","affiliation":"site"},"products":[{"id":"885786","name":"TestProduct","category":"TestCategory","price":"2870.00","brand":"Molecula","quantity":1}]}}]}}',
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144441:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ar:1:ls:1420450784463:rqn:2407:rn:268872224:hid:934923750:ds:,,,,,,,280,,,,,690:rqnl:1:st:1460281481:u:1455701456680556087",
    timestamp="2016-04-09 12:44:46",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    source_uri="prt://yabs-rt@bsmc17e.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
    remoteip6="::ffff:127.0.0.1",
    url="goal://elka2016-vk.ereality.org/flash_start",
)
testdata_watch_log.add_row(
    uniqid="10113701463508334",
    cookiei="10113701463508334",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-11 12:44:46",
    params='{"__ym":{"ecommerce":[{"purchase":{"actionField":{"id":"0033349283","revenue":"22520","affiliation":"site"},"products":[{"id":"885786","name":"MoleculaGHMM3","category":"Dich","price":"2870.00","brand":"Molecula","quantity":1}]}}]}}',
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144441:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ar:1:ls:1420450784463:rqn:2407:rn:268872224:hid:934923750:ds:,,,,,,,280,,,,,690:rqnl:1:st:1460281481:u:1455701456680556087",
    timestamp="2016-04-10 12:44:46",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    source_uri="prt://yabs-rt@bsmc17e.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
    remoteip6="::ffff:127.0.0.1",
    url="goal://elka2016-vk.ereality.org/flash_start",
)

# did from yp
testdata_watch_log.add_row(
    uniqid="10113701463508403",
    cookiei="10113701463508403",
    eventtime="1460373600",
    headerargs="293: i=qMC6KcWQaSBoTRy2qCskjcluLx2lQRbHUaP4kMGEGlvPELBxnh3hxLpXONAYkpgkKjNsY99SsuncdQx0oYdBtMRQE9U=; yp=2147483648.andrid.309d596917f40e02#2147483648.did.886f3cd0a0fa93a19f9d35362dcc111d#2147483648.ybrod.3#1521053305.shlos.1#1531381181.sz.640x360x2#1519812568.ygu.1#1532815624.szm.2%3A640x360%3A360x562; _ym_isad=2; usst=EAAAAAAAAADwAQoOCgJkcxIIMjUzMDc2NDM",
    useragent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
)
testdata_watch_log.add_row(
    uniqid="10113701463508403",
    cookiei="10113701463508403",
    eventtime="1460373610",
    headerargs="293: i=qMC6KcWQaSBoTRy2qCskjcluLx2lQRbHUaP4kMGEGlvPELBxnh3hxLpXONAYkpgkKjNsY99SsuncdQx0oYdBtMRQE9U=; yp=2147483648.andrid.309d596917f40e02#2147483648.did.886f3cd0a0fa93a19f9d35362dcc111d#2147483648.ybrod.3#1521053305.shlos.1#1531381181.sz.640x360x2#1519812568.ygu.1#1532815624.szm.2%3A640x360%3A360x562; _ym_isad=2; usst=EAAAAAAAAADwAQoOCgJkcxIIMjUzMDc2NDM",
    useragent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
)
testdata_watch_log.add_row(
    uniqid="10113701463508401",
    cookiei="10113701463508401",
    eventtime="1460373611",
    headerargs="293: i=qMC6KcWQaSBoTRy2qCskjcluLx2lQRbHUaP4kMGEGlvPELBxnh3hxLpXONAYkpgkKjNsY99SsuncdQx0oYdBtMRQE9U=; yp=2147483648.andrid.309d596917f40e02#2147483648.did.886f3cd0a0fa93a19f9d35362dcc222d#2147483648.ybrod.3#1521053305.shlos.1#1531381181.sz.640x360x2#1519812568.ygu.1#1532815624.szm.2%3A640x360%3A360x562; _ym_isad=2; usst=EAAAAAAAAADwAQoOCgJkcxIIMjUzMDc2NDM",
    useragent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
)
testdata_watch_log.add_row(
    uniqid="10113701463508400",
    cookiei="10113701463508400",
    eventtime="1460373622",
    headerargs="293: i=qMC6KcWQaSBoTRy2qCskjcluLx2lQRbHUaP4kMGEGlvPELBxnh3hxLpXONAYkpgkKjNsY99SsuncdQx0oYdBtMRQE9U=; yp=2147483648.andrid.309d596917f40e02#2147483648.did.886f3cd0a0fa93a19f9d35362dcc333d#2147483648.ybrod.3#1521053305.shlos.1#1531381181.sz.640x360x2#1519812568.ygu.1#1532815624.szm.2%3A640x360%3A360x562; _ym_isad=2; usst=EAAAAAAAAADwAQoOCgJkcxIIMjUzMDc2NDM",
    useragent="Mozilla/5.0 (Linux; U; Android 4.2.2; ru-ru; HTC Desire 310 Build/JDQ39) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30 YandexSearch/5.45",
)
testdata_watch_log.add_row(
    uniqid="10113701463508400",
    cookiei="10113701463508400",
    eventtime="1460373632",
    headerargs="293: i=qMC6KcWQaSBoTRy2qCskjcluLx2lQRbHUaP4kMGEGlvPELBxnh3hxLpXONAYkpgkKjNsY99SsuncdQx0oYdBtMRQE9U=; yp=2147483648.andrid.309d596917f40e02#2147483648.did.886f3cd0a0fa93a19f9d35362dcc333d#2147483648.ybrod.3#1521053305.shlos.1#1531381181.sz.640x360x2#1519812568.ygu.1#1532815624.szm.2%3A640x360%3A360x562; _ym_isad=2; usst=EAAAAAAAAADwAQoOCgJkcxIIMjUzMDc2NDM",
    useragent="Mozilla/5.0 (Linux; U; Android 4.2.2; ru-ru; HTC Desire 310 Build/JDQ39) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30 YandexSearch/5.45",
)
testdata_watch_log.add_row(
    uniqid="10113701463508339",
    cookiei="10113701463508339",
    eventtime="1460373633",
    headerargs="293: i=qMC6KcWQaSBoTRy2qCskjcluLx2lQRbHUaP4kMGEGlvPELBxnh3hxLpXONAYkpgkKjNsY99SsuncdQx0oYdBtMRQE9U=; yp=2147483648.andrid.309d596917f40e02#2147483648.did.886f3cd0a0fa93a19f9d35362dcc444d#2147483648.ybrod.3#1521053305.shlos.1#1531381181.sz.640x360x2#1519812568.ygu.1#1532815624.szm.2%3A640x360%3A360x562; _ym_isad=2; usst=EAAAAAAAAADwAQoOCgJkcxIIMjUzMDc2NDM",
    useragent="Mozilla/5.0 (Linux; U; Android 4.2.2; ru-ru; HTC Desire 310 Build/JDQ39) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30 YandexSearch/5.45",
)

testdata_watch_log.add_row(
    uniqid="10113701463508402",
    cookiei="10113701463508402",
    eventtime="1460373600",
    headerargs="293: i=qMC6KcWQaSBoTRy2qCskjcluLx2lQRbHUaP4kMGEGlvPELBxnh3hxLpXONAYkpgkKjNsY99SsuncdQx0oYdBtMRQE9U=; yp=2147483648.andrid.309d596917f40e02#2147483648.did.886f3cd0a0fa93a19f9d35362dcc111d#2147483648.ybrod.3#1521053305.shlos.1#1531381181.sz.640x360x2#1519812568.ygu.1#1532815624.szm.2%3A640x360%3A360x562; _ym_isad=2; usst=EAAAAAAAAADwAQoOCgJkcxIIMjUzMDc2NDM",
    useragent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
)
testdata_watch_log.add_row(
    uniqid="10113701463508402",
    cookiei="10113701463508402",
    eventtime="1460373610",
    headerargs="293: i=qMC6KcWQaSBoTRy2qCskjcluLx2lQRbHUaP4kMGEGlvPELBxnh3hxLpXONAYkpgkKjNsY99SsuncdQx0oYdBtMRQE9U=; yp=2147483648.andrid.309d596917f40e02#2147483648.did.886f3cd0a0fa93a19f9d35362dcc111d#2147483648.ybrod.3#1521053305.shlos.1#1531381181.sz.640x360x2#1519812568.ygu.1#1532815624.szm.2%3A640x360%3A360x562; _ym_isad=2; usst=EAAAAAAAAADwAQoOCgJkcxIIMjUzMDc2NDM",
    useragent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
)
testdata_watch_log.add_row(
    uniqid="10113701463508336",
    cookiei="10113701463508336",
    eventtime="1460373611",
    headerargs="293: i=qMC6KcWQaSBoTRy2qCskjcluLx2lQRbHUaP4kMGEGlvPELBxnh3hxLpXONAYkpgkKjNsY99SsuncdQx0oYdBtMRQE9U=; yp=2147483648.andrid.309d596917f40e02#2147483648.did.886f3cd0a0fa93a19f9d35362dcc222d#2147483648.ybrod.3#1521053305.shlos.1#1531381181.sz.640x360x2#1519812568.ygu.1#1532815624.szm.2%3A640x360%3A360x562; _ym_isad=2; usst=EAAAAAAAAADwAQoOCgJkcxIIMjUzMDc2NDM",
    useragent="Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_1 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D201 Safari/9537.53",
)
testdata_watch_log.add_row(
    uniqid="10113701463508337",
    cookiei="10113701463508337",
    eventtime="1460373622",
    headerargs="293: i=qMC6KcWQaSBoTRy2qCskjcluLx2lQRbHUaP4kMGEGlvPELBxnh3hxLpXONAYkpgkKjNsY99SsuncdQx0oYdBtMRQE9U=; yp=2147483648.andrid.309d596917f40e02#2147483648.did.886f3cd0a0fa93a19f9d35362dcc333d#2147483648.ybrod.3#1521053305.shlos.1#1531381181.sz.640x360x2#1519812568.ygu.1#1532815624.szm.2%3A640x360%3A360x562; _ym_isad=2; usst=EAAAAAAAAADwAQoOCgJkcxIIMjUzMDc2NDM",
    useragent="Mozilla/5.0 (Linux; U; Android 4.2.2; ru-ru; HTC Desire 310 Build/JDQ39) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30 YandexSearch/5.45",
)
testdata_watch_log.add_row(
    uniqid="10113701463508338",
    cookiei="10113701463508338",
    eventtime="1460373632",
    headerargs="293: i=qMC6KcWQaSBoTRy2qCskjcluLx2lQRbHUaP4kMGEGlvPELBxnh3hxLpXONAYkpgkKjNsY99SsuncdQx0oYdBtMRQE9U=; yp=2147483648.andrid.309d596917f40e02#2147483648.did.886f3cd0a0fa93a19f9d35362dcc333d#2147483648.ybrod.3#1521053305.shlos.1#1531381181.sz.640x360x2#1519812568.ygu.1#1532815624.szm.2%3A640x360%3A360x562; _ym_isad=2; usst=EAAAAAAAAADwAQoOCgJkcxIIMjUzMDc2NDM",
    useragent="Mozilla/5.0 (Linux; U; Android 4.2.2; ru-ru; HTC Desire 310 Build/JDQ39) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30 YandexSearch/5.45",
)
testdata_watch_log.add_row(
    uniqid="10113701463508335",
    cookiei="10113701463508335",
    eventtime="1460373633",
    headerargs="293: i=qMC6KcWQaSBoTRy2qCskjcluLx2lQRbHUaP4kMGEGlvPELBxnh3hxLpXONAYkpgkKjNsY99SsuncdQx0oYdBtMRQE9U=; yp=2147483648.andrid.309d596917f40e02#2147483648.did.886f3cd0a0fa93a19f9d35362dcc444d#2147483648.ybrod.3#1521053305.shlos.1#1531381181.sz.640x360x2#1519812568.ygu.1#1532815624.szm.2%3A640x360%3A360x562; _ym_isad=2; usst=EAAAAAAAAADwAQoOCgJkcxIIMjUzMDc2NDM",
    useragent="Mozilla/5.0 (Linux; U; Android 4.2.2; ru-ru; HTC Desire 310 Build/JDQ39) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30 YandexSearch/5.45",
)


testdata_watch_log.add_row(
    _date="2016-04-09",
    uniqid="10113701463508333",
    cookiei="10113701463508333",
    clientport="42138",
    clientip="217.118.90.186",
    clientip6="::ffff:109.173.43.153",
    iso_eventtime="2016-04-09 21:40:20",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    browserinfo="s:320x568x32:sk:2:adb:2:fpr:216613626101:cn:1:w:980x1409:z:180:i:20160409214019:et:1460227220:en:utf-8:v:682:c:1:la:ru-ru:ls:1359029062124:rqn:20:rn:228539826:hid:2893278:ds:48,52,410,92,0,0,,1349,542,,,,2705:wn:44569:hl:1:rqnl:1:st:1460227220:u:146020227479937061:t:Rus-Massage.com",
    timestamp="2016-04-09 21:40:20",
    source_uri="prt://yabs-rt@bsmc04i.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    headerargs="293: yabs-sid=2313691931460227219; L=ey1cUQZJXQJjRAlFAHdiUGIEQk1OB3IGK1sZJwMmGREhLyUmNyYuGjZGO2soAg==.1419411098.11391.314408.07ad3cdab90f21737fefbb5143535e51; Session_id=3:1433059288.5.1.1379594369273:2O7cUw:6.0|206744251.0.2|1130000010571739.39816729.2|128933.667099.rkW_tzsWQaRPFZXjblgkCZvf644; _ym_isad=2; _ym_uid=1460191197322006264; fuid01=5130e63214d544d4.RWFdE_mgYY8VqQTZz8BouT1dzbgStpdFVYbT_DAy2JQS-_V4nrlXzNymb_fP_0nyeT3vHHIKyy5WkS3yNX6nBUrd5g6DiDlzEnaJHs0XiLKf1K1OvuAld2rvB0DlZXA8; my=YzYBAQA=; sessionid2=3:1433059288.5.0.1379594369273:2O7cUw:6.1|1130000010571739.39816729.2|128933.122480.a5LWqFGormPXIounfv6lP5SkLtA; yabs-frequency=/4/000700000007UVbM/xyznSAmZZW00/; yandex_gid=213; yandex_login=r.amiraslanov@dveri.ru; uniqid=3460322961362159108; yp=1734771098.udn.cDpyLmFtaXJhc2xhbm92QGR2ZXJpLnJ1#2147483647.xsz.320#1734771098.multib.1#1469135632.sz.320x568x2#1475350969.szm.2:320x568:320x529#1462174965.ygu.1",
    referer="http://rus-massage.com/list/city-1/area-6/district-123/subway-74/",
    remoteip6="::1",
    url="http://rus-massage.com/profile/1115",
)
testdata_watch_log.add_row(
    _date="2016-04-09",
    clientport="42138",
    clientip="176.59.7.165",
    clientip6="::ffff:109.173.43.153",
    iso_eventtime="2016-04-09 21:40:20",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    browserinfo="s:320x568x32:sk:2:adb:2:fpr:216613626101:cn:1:w:980x1409:z:180:i:20160409214019:et:1460227220:en:utf-8:v:682:c:1:la:ru-ru:ls:1359029062124:rqn:20:rn:228539826:hid:2893278:ds:48,52,410,92,0,0,,1349,542,,,,2705:wn:44569:hl:1:rqnl:1:st:1460227220:u:146020227479937061:t:Rus-Massage.com",
    timestamp="2016-04-09 21:40:20",
    source_uri="prt://yabs-rt@bsmc04i.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    headerargs="293: yabs-sid=2313691931460227219; L=ey1cUQZJXQJjRAlFAHdiUGIEQk1OB3IGK1sZJwMmGREhLyUmNyYuGjZGO2soAg==.1419411098.11391.314408.07ad3cdab90f21737fefbb5143535e51; Session_id=3:1433059288.5.1.1379594369273:2O7cUw:6.0|206744251.0.2|1130000010571739.39816729.2|128933.667099.rkW_tzsWQaRPFZXjblgkCZvf644; _ym_isad=2; _ym_uid=1460191197322006264; fuid01=5130e63214d544d4.RWFdE_mgYY8VqQTZz8BouT1dzbgStpdFVYbT_DAy2JQS-_V4nrlXzNymb_fP_0nyeT3vHHIKyy5WkS3yNX6nBUrd5g6DiDlzEnaJHs0XiLKf1K1OvuAld2rvB0DlZXA8; my=YzYBAQA=; sessionid2=3:1433059288.5.0.1379594369273:2O7cUw:6.1|1130000010571739.39816729.2|128933.122480.a5LWqFGormPXIounfv6lP5SkLtA; yabs-frequency=/4/000700000007UVbM/xyznSAmZZW00/; yandex_gid=213; yandex_login=r.amiraslanov@dveri.ru; uniqid=3460322961362159108; yp=1734771098.udn.cDpyLmFtaXJhc2xhbm92QGR2ZXJpLnJ1#2147483647.xsz.320#1734771098.multib.1#1469135632.sz.320x568x2#1475350969.szm.2:320x568:320x529#1462174965.ygu.1",
    referer="http://rus-massage.com/list/city-1/area-6/district-123/subway-74/",
    remoteip6="::1",
    url="http://rus-massage.com/profile/1115",
)
testdata_watch_log.add_row(
    _date="2016-04-09",
    uniqid="601826891455541112",
    cookiei="601826891455541112",
    clientport="56886",
    clientip="217.118.90.186",
    clientip6="::ffff:31.173.84.197",
    cookieys="wprid.1460202261559492-627982-ws27-406-TCH2",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    iso_eventtime="2016-04-09 14:44:34",
    ip_numeric="531453125",
    watch_id="1823021268362332001",
    browserinfo="s:320x568x32:sk:2:fpr:216613626101:cn:1:w:980x1409:z:180:i:20160409144433:et:1460202274:en:utf-8:v:682:c:1:la:ru-ru:ls:1359029062124:rqn:1:rn:96276557:hid:241135602:ds:3,101,306,335,1,0,,927,456,,,,1661:wn:15389:hl:1:rqnl:1:st:1460202274:u:146020227479937061:t:Rus-Massage.com",
    timestamp="2016-04-09 14:44:34",
    source_uri="prt://yabs-rt@bsmc05f.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    headerargs="293: yabs-sid=2533070101460186273; L=ey1cUQZJXQJjRAlFAHdiUGIEQk1OB3IGK1sZJwMmGREhLyUmNyYuGjZGO2soAg==.1419411098.11391.314408.07ad3cdab90f21737fefbb5143535e51; Session_id=3:1433059288.5.1.1379594369273:2O7cUw:6.0|206744251.0.2|1130000010571739.39816729.2|128933.667099.rkW_tzsWQaRPFZXjblgkCZvf644; _ym_isad=2; _ym_uid=1460191197322006264; fuid01=5130e63214d544d4.RWFdE_mgYY8VqQTZz8BouT1dzbgStpdFVYbT_DAy2JQS-_V4nrlXzNymb_fP_0nyeT3vHHIKyy5WkS3yNX6nBUrd5g6DiDlzEnaJHs0XiLKf1K1OvuAld2rvB0DlZXA8; my=YzYBAQA=; sessionid2=3:1433059288.5.0.1379594369273:2O7cUw:6.1|1130000010571739.39816729.2|128933.122480.a5LWqFGormPXIounfv6lP5SkLtA; yabs-frequency=/4/000700000007UVbM/xyznSAmZZW00/; yandex_gid=213; yandex_login=r.amiraslanov@dveri.ru; uniqid=3460322961362159108; yp=1734771098.udn.cDpyLmFtaXJhc2xhbm92QGR2ZXJpLnJ1#2147483647.xsz.320#1734771098.multib.1#1469135632.sz.320x568x2#1475350969.szm.2:320x568:320x529#1462174965.ygu.1; ys=wprid.1460202261559492-627982-ws27-406-TCH2",
    referer="https://yandex.ru/search/touch/?text=sss&lr=213&suggest_reqid=346032296136215910819942704808449",
    remoteip6="::1",
    url="http://rus-massage.com/list/city-1/area-6/district-123/subway-74/",
)
testdata_watch_log.add_row(
    _date="2016-04-09",
    clientport="42138",
    clientip="176.59.7.165",
    clientip6="::ffff:109.173.43.153",
    iso_eventtime="2016-04-09 21:40:20",
    browserinfo="s:320x568x32:sk:2:adb:2:fpr:216613626101:cn:1:w:980x1409:z:180:i:20160409214019:et:1460227220:en:utf-8:v:682:c:1:la:ru-ru:ls:1359029062124:rqn:20:rn:228539826:hid:2893278:ds:48,52,410,92,0,0,,1349,542,,,,2705:wn:44569:hl:1:rqnl:1:st:1460227220:u:146020227479937061:t:Rus-Massage.com",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    timestamp="2016-04-09 21:40:20",
    source_uri="prt://yabs-rt@bsmc04i.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    headerargs="293: yabs-sid=2313691931460227219; L=ey1cUQZJXQJjRAlFAHdiUGIEQk1OB3IGK1sZJwMmGREhLyUmNyYuGjZGO2soAg==.1419411098.11391.314408.07ad3cdab90f21737fefbb5143535e51; Session_id=3:1433059288.5.1.1379594369273:2O7cUw:6.0|206744251.0.2|1130000010571739.39816729.2|128933.667099.rkW_tzsWQaRPFZXjblgkCZvf644; _ym_isad=2; _ym_uid=1460191197322006264; fuid01=5130e63214d544d4.RWFdE_mgYY8VqQTZz8BouT1dzbgStpdFVYbT_DAy2JQS-_V4nrlXzNymb_fP_0nyeT3vHHIKyy5WkS3yNX6nBUrd5g6DiDlzEnaJHs0XiLKf1K1OvuAld2rvB0DlZXA8; my=YzYBAQA=; sessionid2=3:1433059288.5.0.1379594369273:2O7cUw:6.1|1130000010571739.39816729.2|128933.122480.a5LWqFGormPXIounfv6lP5SkLtA; yabs-frequency=/4/000700000007UVbM/xyznSAmZZW00/; yandex_gid=213; yandex_login=r.amiraslanov@dveri.ru; uniqid=3460322961362159108; yp=1734771098.udn.cDpyLmFtaXJhc2xhbm92QGR2ZXJpLnJ1#2147483647.xsz.320#1734771098.multib.1#1469135632.sz.320x568x2#1475350969.szm.2:320x568:320x529#1462174965.ygu.1",
    referer="http://rus-massage.com/list/city-1/area-6/district-123/subway-74/",
    remoteip6="::1",
    url="http://rus-massage.com/profile/1115",
)
testdata_watch_log.add_row(
    _date="2016-04-09",
    uniqid="10113701463508333",
    cookiei="10113701463508333",
    clientport="56886",
    clientip="217.118.90.186",
    clientip6="::ffff:31.173.84.197",
    cookieys="wprid.1460202261559492-627982-ws27-406-TCH2",
    iso_eventtime="2016-04-09 14:44:34",
    ip_numeric="531453125",
    watch_id="1823021268362332001",
    browserinfo="s:320x568x32:sk:2:fpr:216613626101:cn:1:w:980x1409:z:180:i:20160409144433:et:1460202274:en:utf-8:v:682:c:1:la:ru-ru:ls:1359029062124:rqn:1:rn:96276557:hid:241135602:ds:3,101,306,335,1,0,,927,456,,,,1661:wn:15389:hl:1:rqnl:1:st:1460202274:u:146020227479937061:t:Rus-Massage.com",
    timestamp="2016-04-09 14:44:34",
    source_uri="prt://yabs-rt@bsmc05f.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    headerargs="293: yabs-sid=2533070101460186273; L=ey1cUQZJXQJjRAlFAHdiUGIEQk1OB3IGK1sZJwMmGREhLyUmNyYuGjZGO2soAg==.1419411098.11391.314408.07ad3cdab90f21737fefbb5143535e51; Session_id=3:1433059288.5.1.1379594369273:2O7cUw:6.0|206744251.0.2|1130000010571739.39816729.2|128933.667099.rkW_tzsWQaRPFZXjblgkCZvf644; _ym_isad=2; _ym_uid=1460191197322006264; fuid01=5130e63214d544d4.RWFdE_mgYY8VqQTZz8BouT1dzbgStpdFVYbT_DAy2JQS-_V4nrlXzNymb_fP_0nyeT3vHHIKyy5WkS3yNX6nBUrd5g6DiDlzEnaJHs0XiLKf1K1OvuAld2rvB0DlZXA8; my=YzYBAQA=; sessionid2=3:1433059288.5.0.1379594369273:2O7cUw:6.1|1130000010571739.39816729.2|128933.122480.a5LWqFGormPXIounfv6lP5SkLtA; yabs-frequency=/4/000700000007UVbM/xyznSAmZZW00/; yandex_gid=213; yandex_login=r.amiraslanov@dveri.ru; uniqid=3460322961362159108; yp=1734771098.udn.cDpyLmFtaXJhc2xhbm92QGR2ZXJpLnJ1#2147483647.xsz.320#1734771098.multib.1#1469135632.sz.320x568x2#1475350969.szm.2:320x568:320x529#1462174965.ygu.1; ys=wprid.1460202261559492-627982-ws27-406-TCH2",
    referer="https://yandex.ru/search/touch/?text=sss&lr=213&suggest_reqid=346032296136215910819942704808449",
    remoteip6="::1",
    url="http://rus-massage.com/list/city-1/area-6/district-123/subway-74/",
)
# purchase
testdata_watch_log.add_row(
    _date="2016-04-09",
    uniqid="11222221455542221",
    cookiei="11222221455542221",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-09 12:44:46",
    params='{"__ym":{"ecommerce":[{"purchase":{"actionField":{"id":251407497},"products":[{"id":1112223,"price":"5348","quantity":1}]}}]}}',
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144441:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ar:1:ls:1420450784463:rqn:2407:rn:268872224:hid:934923750:ds:,,,,,,,280,,,,,690:rqnl:1:st:1460281481:u:1455701456680556087",
    timestamp="2016-04-10 12:44:46",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    source_uri="prt://yabs-rt@bsmc17e.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
    remoteip6="::ffff:127.0.0.1",
    url="goal://elka2016-vk.ereality.org/flash_start",
)
testdata_watch_log.add_row(
    _date="2016-04-09",
    uniqid="11222221455542221",
    cookiei="11222221455542221",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-09 12:44:46",
    params='{"__ym":{"ecommerce":[{"purchase":{"actionField":{"id":"0033349283","affiliation":"site"},"products":[{"id":"885786","name":"TestProduct","category":"TestCategory","price":"2870.00","brand":"Molecula","quantity":1}]}}]}}',
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144441:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ar:1:ls:1420450784463:rqn:2407:rn:268872224:hid:934923750:ds:,,,,,,,280,,,,,690:rqnl:1:st:1460281481:u:1455701456680556087",
    timestamp="2016-04-09 12:44:46",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    source_uri="prt://yabs-rt@bsmc17e.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
    remoteip6="::ffff:127.0.0.1",
    url="goal://elka2016-vk.ereality.org/flash_start",
)
testdata_watch_log.add_row(
    _date="2016-04-09",
    uniqid="11222221455542221",
    cookiei="11222221455542221",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-09 12:44:46",
    params='{"__ym":{"ecommerce":[{"purchase":{"actionField":{"id":"0033349283","revenue":"22520","affiliation":"site"},"products":[{"id":"885786","name":"MoleculaGHMM3","category":"Dich","price":"2870.00","brand":"Molecula","quantity":1}]}}]}}',
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144441:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ar:1:ls:1420450784463:rqn:2407:rn:268872224:hid:934923750:ds:,,,,,,,280,,,,,690:rqnl:1:st:1460281481:u:1455701456680556087",
    timestamp="2016-04-10 12:44:46",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    source_uri="prt://yabs-rt@bsmc17e.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
    remoteip6="::ffff:127.0.0.1",
    url="goal://elka2016-vk.ereality.org/flash_start",
)


testdata_watch_log.add_row(
    _date="2016-04-10",
    uniqid="10113701463508333",
    cookiei="10113701463508333",
    clientport="34986",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 12:44:46",
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144441:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ar:1:ls:1420450784463:rqn:2407:rn:268872224:hid:934923750:ds:,,,,,,,280,,,,,690:rqnl:1:st:1460281481:u:1455701456680556087",
    timestamp="2016-04-10 12:44:46",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    source_uri="prt://yabs-rt@bsmc17e.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
    remoteip6="::ffff:127.0.0.1",
    url="goal://elka2016-vk.ereality.org/flash_start",
)
testdata_watch_log.add_row(
    _date="2016-04-10",
    clientport="34986",
    clientip="176.59.7.165",
    iso_eventtime="2016-04-10 12:44:46",
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144441:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ar:1:ls:1420450784463:rqn:2407:rn:268872224:hid:934923750:ds:,,,,,,,280,,,,,690:rqnl:1:st:1460281481:u:1455701456680556087",
    timestamp="2016-04-10 12:44:46",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    source_uri="prt://yabs-rt@bsmc17e.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
    remoteip6="::ffff:127.0.0.1",
    url="goal://elka2016-vk.ereality.org/flash_start",
)
testdata_watch_log.add_row(
    _date="2016-04-10",
    uniqid="601826891455542222",
    cookiei="601826891455542222",
    clientport="57406",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 12:44:46",
    watch_id="1843786225552199373",
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144441:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ar:1:ls:1420450784463:rqn:2408:rn:137781372:hid:934923750:ds:,,,,,,,,,,,,:rqnl:1:st:1460281481:u:1455701456680556087",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    timestamp="2016-04-10 12:44:46",
    source_uri="prt://yabs-rt@bsmc09i.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
    remoteip6="::ffff:127.0.0.1",
    url="goal://elka2016-vk.ereality.org/flash_add",
)
testdata_watch_log.add_row(
    _date="2016-04-10",
    clientport="34986",
    clientip="176.59.7.165",
    iso_eventtime="2016-04-10 12:44:46",
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144441:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ar:1:ls:1420450784463:rqn:2407:rn:268872224:hid:934923750:ds:,,,,,,,280,,,,,690:rqnl:1:st:1460281481:u:1455701456680556087",
    timestamp="2016-04-10 12:44:46",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    source_uri="prt://yabs-rt@bsmc17e.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
    remoteip6="::ffff:127.0.0.1",
    url="goal://elka2016-vk.ereality.org/flash_start",
)
testdata_watch_log.add_row(
    _date="2016-04-10",
    clientport="57406",
    clientip="176.59.7.165",
    iso_eventtime="2016-04-10 12:44:46",
    watch_id="1843786225552199373",
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144441:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ar:1:ls:1420450784463:rqn:2408:rn:137781372:hid:934923750:ds:,,,,,,,,,,,,:rqnl:1:st:1460281481:u:1455701456680556087",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    timestamp="2016-04-10 12:44:46",
    source_uri="prt://yabs-rt@bsmc09i.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
    remoteip6="::ffff:127.0.0.1",
    url="goal://elka2016-vk.ereality.org/flash_add",
)
testdata_watch_log.add_row(
    _date="2016-04-10",
    uniqid="601826891455542222",
    cookiei="601826891455542222",
    clientport="34986",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 12:44:46",
    watch_id="1843786190089097103",
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144440:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ls:1420450784463:rqn:2406:rn:192191027:hid:934923750:ds:232,118,54,1,0,0,,,,,,,:rqnl:1:st:1460281481:u:1455701456680556087",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    timestamp="2016-04-10 12:44:46",
    source_uri="prt://yabs-rt@bsmc17e.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://vk.com/elka_game",
    remoteip6="::ffff:127.0.0.1",
    url="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
)
# purchase
testdata_watch_log.add_row(
    _date="2016-04-10",
    uniqid="10113701463508333",
    cookiei="10113701463508333",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 12:44:46",
    params='{"__ym":{"ecommerce":[{"purchase":{"actionField":{"id":251407497},"products":[{"id":1112223,"price":"5348","quantity":1}]}}]}}',
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144441:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ar:1:ls:1420450784463:rqn:2407:rn:268872224:hid:934923750:ds:,,,,,,,280,,,,,690:rqnl:1:st:1460281481:u:1455701456680556087",
    timestamp="2016-04-10 12:44:46",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    source_uri="prt://yabs-rt@bsmc17e.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
    remoteip6="::ffff:127.0.0.1",
    url="goal://elka2016-vk.ereality.org/flash_start",
)
testdata_watch_log.add_row(
    _date="2016-04-10",
    uniqid="10113701463508333",
    cookiei="10113701463508333",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 12:44:46",
    params='{"__ym":{"ecommerce":[{"purchase":{"actionField":{"id":"0033349283","affiliation":"site"},"products":[{"id":"885786","name":"TestProduct","category":"TestCategory","price":"2870.00","brand":"Molecula","quantity":1}]}}]}}',
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144441:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ar:1:ls:1420450784463:rqn:2407:rn:268872224:hid:934923750:ds:,,,,,,,280,,,,,690:rqnl:1:st:1460281481:u:1455701456680556087",
    timestamp="2016-04-10 12:44:46",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    source_uri="prt://yabs-rt@bsmc17e.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
    remoteip6="::ffff:127.0.0.1",
    url="goal://elka2016-vk.ereality.org/flash_start",
)
testdata_watch_log.add_row(
    _date="2016-04-10",
    uniqid="10113701463508333",
    cookiei="10113701463508333",
    clientip="217.118.90.186",
    iso_eventtime="2016-04-10 12:44:46",
    params='{"__ym":{"ecommerce":[{"purchase":{"actionField":{"id":"0033349283","revenue":"22520","affiliation":"site"},"products":[{"id":"885786","name":"MoleculaGHMM3","category":"Dich","price":"2870.00","brand":"Molecula","quantity":1}]}}]}}',
    browserinfo="s:1280x1024x24:sk:1:ifr:1:f:21.0.0:fpr:97806405901:cn:1:w:1000x978:z:300:i:20160410144441:et:1460281481:en:utf-8:v:682:c:1:la:ru:wh:1:ar:1:ls:1420450784463:rqn:2407:rn:268872224:hid:934923750:ds:,,,,,,,280,,,,,690:rqnl:1:st:1460281481:u:1455701456680556087",
    timestamp="2016-04-10 12:44:46",
    useragent="Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
    source_uri="prt://yabs-rt@bsmc17e.yandex.ru/home/yabs/server/log.rtmr/watch.tskv",
    referer="https://elka2016-vk.ereality.org/iframe?api_url=https://api.vk.com/api.php&api_id=5124326&api_settings=2367495&viewer_id=27627371&viewer_type=0&sid=1bdb5cf1fcca5a988b0a5f967f99f68d79d2a64eda4ef1c55a975e89cfff78f2f6dddf34c7a77b7e9c1ca&secret=a896078edb&access_token=55dfe0ac6ee01891eb0aa9cd14220f274d2944d0fa174fbc19c30689b64aecceed116ce28d36b8b180f04&user_id=0&group_id=0&is_app_user=1&auth_key=76ed1db554d2e99e0618ae79d1e8ca24&language=0&parent_language=0&ad_info=ElsdCQNbR1djBgNaAwJSXHt5B0Q8HTJXUVBBJRVBNwoIFjI2HA8H&is_secure=1&ads_app_id=5124326_d5b166fc4e1a5244fe&referrer=unknown&lc_name=792224cc&hash=",
    remoteip6="::ffff:127.0.0.1",
    url="goal://elka2016-vk.ereality.org/flash_start",
)

convert_old_fp_to_watch_log(testdata_watch_log, rawdata_fp, "2016-04-09")
convert_old_fp_to_watch_log(testdata_watch_log, rawdata_fp2, "2016-04-10")
convert_old_fp_to_watch_log(testdata_watch_log, rawdata_fp3, "2016-04-11")

testdata_visit_log = BaseLog(
    path="//home/logfeller/logs/visit-v2-log/1d",
    date=AND_FINALLY_DATE,
    default_data=VISIT_LOG,
    attributes={
        "schema": [
            {"name": "UserID", "required": False, "type": "uint64", "sort_order": "ascending"},
            {"name": "Goals_CallPhoneNumber", "required": False, "type": "any"},
        ]
    },
)
testdata_visit_log.add_row(UserID=5555, Goals_CallPhoneNumber=["", "79058595417", ""])
testdata_visit_log.add_row(UserID=3163327321576515206, Goals_CallPhoneNumber=["+79856844044"])
testdata_visit_log.add_row(UserID=4938737081541971461, Goals_CallPhoneNumber=["", "796723816XX", "7495236xxxx"])
testdata_visit_log.add_row(UserID=6191758311566573779, Goals_CallPhoneNumber=["", ""])
testdata_visit_log.add_row(UserID=8682265231558301423)
testdata_visit_log.add_row(UserID=9003589421574096811, Goals_CallPhoneNumber=None)
testdata_visit_log.add_row(_date=TEST_RUN_DATE_STR, UserID=9003589421574096811, Goals_CallPhoneNumber=None)

testdata_visit_private_log = BaseLog(
    path="//home/logfeller/logs/visit-v2-private-log/1d",
    date=AND_FINALLY_DATE,
    default_data=VISIT_LOG,
    attributes={
        "schema": [
            {"name": "UserID", "required": False, "type": "uint64", "sort_order": "ascending"},
            {"name": "Goals_CallPhoneNumber", "required": False, "type": "any"},
        ]
    },
)
testdata_visit_private_log.add_row(UserID=5555, Goals_CallPhoneNumber=["", "79058595417", ""])
testdata_visit_private_log.add_row(UserID=3163327321576515206, Goals_CallPhoneNumber=["+79856844044"])
testdata_visit_private_log.add_row(
    UserID=4938737081541971461, Goals_CallPhoneNumber=["", "796723816XX", "7495236xxxx"]
)
testdata_visit_private_log.add_row(UserID=6191758311566573779, Goals_CallPhoneNumber=["", ""])
testdata_visit_private_log.add_row(UserID=8682265231558301423)
testdata_visit_private_log.add_row(UserID=9003589421574096811, Goals_CallPhoneNumber=None)
testdata_visit_private_log.add_row(_date=TEST_RUN_DATE_STR, UserID=9003589421574096811, Goals_CallPhoneNumber=None)

testdata_vk_profiles_dump = BaseLog(
    path="//crypta/production/state/graph/dicts", table_name="profiles", default_data=VK_PROFILES_DUMP
)
testdata_vk_profiles_dump.add_row(id_value="id67677184")

testdata_yamoney = BaseLog(
    path="//crypta/production/state/graph/dicts/yamoney", table_name="yamoney_in_v2", default_data=YAMONEY_LOG
)
testdata_yamoney.add_row(id_type=1)
testdata_yamoney.add_row(id_type=2)
testdata_yamoney.add_row(id_type=3)
testdata_yamoney.add_row(id_type=4)
testdata_yamoney.add_row(id_type=5, id_value="17501517")  # puid
testdata_yamoney.add_row(id_type=6, id_value="10113701463508505")  # yuid
testdata_yamoney.add_row(id_type=7, yamoney_id=888)
testdata_yamoney.add_row(id_type=1, id_value="bbb")
testdata_yamoney.add_row(id_type=2, id_value="bbb")
testdata_yamoney.add_row(id_type=5, id_value="17501517")  # puid
testdata_yamoney.add_row(id_type=6, id_value="10113701463508506", yamoney_id=777)  # yuid
testdata_yamoney.add_row(id_type=7, yamoney_id=777)

testdata_yamoney_phone = BaseLog(
    path="//crypta/production/state/graph/dicts/yamoney",
    table_name="yamoney_phone_payment",
    default_data=YAMONEY_PHONE_LOG,
)
testdata_yamoney_phone.add_row(phone="9034568763")

testdata_dev_yuid_indevice_perfect_no_limit = BaseLog(
    path="//crypta/production/state/graph/dicts",
    table_name="dev_yuid_indevice_perfect_no_limit",
    default_data=DEV_YUID_INDEVICE_PERFECT_NO_LIMIT,
)
testdata_dev_yuid_indevice_perfect_no_limit.add_row(
    date="2016-03-01",
    devid="BA5EBA5E-0000-0000-0000-AAAAAAAA0001",
    mmetric_devid="BA5EBA5E-0000-0000-0000-BBBBBBBB0001",
    yuid="111111111145915146",
)
testdata_dev_yuid_indevice_perfect_no_limit.add_row(
    date="2016-04-01",
    devid="BA5EBA5E-0000-0000-0000-AAAAAAAA0002",
    mmetric_devid="BA5EBA5E-0000-0000-0000-BBBBBBBB0002",
    yuid="111111111245915146",
    yuid_browser="mobilesafari",
)
testdata_dev_yuid_indevice_perfect_no_limit.add_row(
    date="2016-04-08",
    devid="BA5EBA5E-0000-0000-0000-AAAAAAAA0003",
    mmetric_devid="BA5EBA5E-0000-0000-0000-BBBBBBBB0003",
    yuid="111111111345915146",
)
testdata_dev_yuid_indevice_perfect_no_limit.add_row(
    date="2016-04-09",
    devid="BA5EBA5E-0000-0000-0000-AAAAAAAA0004",
    mmetric_devid="BA5EBA5E-0000-0000-0000-BBBBBBBB0004",
    yuid="111111111445915146",
    yuid_browser="mobilesafari",
)


testdata_cube_log = BaseLog(
    path="//statbox/hypercube/mobile/reactivation/v2/by_device_id", date="2016-04-11", default_data=CUBE_LOG
)
testdata_cube_log.add_row(device_id="e1b4316203309f329438048ddd54f112", platform="Android")
testdata_cube_log.add_row(device_id="30EDAB1F-92E1-47A8-A95E-A0674B36A4BA")
testdata_cube_log.add_row(device_id="BA5EBA5E-0000-0000-0000-BBBBBBBB0002")

testdata_puid_login_dict = BaseLog(
    path="//crypta/production/state/graph/dicts/passport", table_name="puid_login", default_data=PUID_LOGIN_DICT_LOG
)
testdata_puid_login_dict.add_row(id_value="6661", login="6661aaa")

testdata_postclick_log = BaseLog(
    path="//home/logfeller/logs/bs-mobile-postclick-log/1d", date="2016-04-11", default_data=POSTCLICK_LOG
)
testdata_postclick_log.add_row(
    UserID=111000011459458000, ExtPostBack="advertising_id=C994D77E-03E0-45AC-98A2-8EA855B58CEE"
)
testdata_postclick_log.add_row(
    UserID=111000021459458000, ExtPostBack="mat-id=1", IDFA="BCEBAF09-2534-4027-B89A-BCCE1F1AB43A"
)
testdata_postclick_log.add_row(
    UserID=111000031459458000,
    ExtPostBack="adjust-adid=de9433b54a98c8dc482b85600a458ece&idfa=45B383F0-AA86-41A9-B171-BB18EBAA8B32",
    IDFA="45B383F0-AA86-41A9-B171-BB18EBAA8B32",
)
testdata_postclick_log.add_row(
    UserID=111000041459458000, ExtPostBack="advertising_id=C994D77E-03E0-45AC-98A2-8EA855B58CEE", _date="2016-04-10"
)
testdata_postclick_log.add_row(
    UserID=111000011459458000, ExtPostBack="advertising_id=C994D77E-03E0-45AC-98A2-8EA855B58CEE", _date="2016-04-09"
)

"advertising_id=C994D77E-03E0-45AC-98A2-8EA855B58CEE"

testdata_yuid_with_all = BaseLog(
    path="//crypta/production/state/graph/dicts", table_name="yuid_with_all", default_data=YUID_WITH_ALL, append=True
)

testdata_yuid_with_all.add_row(yuid=vmetro_yuid1, key=vmetro_yuid1, all_dates=["2016-04-10"])
testdata_yuid_with_all.add_row(yuid=vmetro_yuid2, key=vmetro_yuid2, all_dates=["2016-04-10"])
testdata_yuid_with_all.add_row(
    yuid="157564101446625111",
    key="157564101446625111",
    yandexuid=157564101446625111,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="157564101446625112",
    key="157564101446625112",
    yandexuid=157564101446625112,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="157564101446625113",
    key="157564101446625113",
    yandexuid=157564101446625113,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="157564101446625114",
    key="157564101446625114",
    yandexuid=157564101446625114,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="157564101446625121",
    key="157564101446625121",
    yandexuid=157564101446625121,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="157564101446625122",
    key="157564101446625122",
    yandexuid=157564101446625122,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="157564101446625123",
    key="157564101446625123",
    yandexuid=157564101446625123,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="157564101446625124",
    key="157564101446625124",
    yandexuid=157564101446625124,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)

# ditmsk
testdata_yuid_with_all.add_row(
    yuid="1907967751360172097",
    key="1907967751360172097",
    yandexuid=1907967751360172097,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="3502086411377199278",
    key="3502086411377199278",
    yandexuid=3502086411377199278,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="1817745901457477516",
    key="1817745901457477516",
    yandexuid=1817745901457477516,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="7086307901319910628",
    key="7086307901319910628",
    yandexuid=7086307901319910628,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="8493830551509391626",
    key="8493830551509391626",
    yandexuid=8493830551509391626,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)

# did
testdata_yuid_with_all.add_row(
    yuid="10113701463508403",
    ua_profile="m|phone|apple|ios|9.3.1",
    key="10113701463508403",
    yandexuid=10113701463508403,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="10113701463508401",
    ua_profile="m|phone|apple|ios|9.3.1",
    key="10113701463508401",
    yandexuid=10113701463508401,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="10113701463508400",
    ua_profile="m|phone|apple|ios|9.3.1",
    key="10113701463508400",
    yandexuid=10113701463508400,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="10113701463508339",
    ua_profile="m|phone|apple|ios|9.3.1",
    key="10113701463508339",
    yandexuid=10113701463508339,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="10113701463508402",
    ua_profile="m|phone|apple|ios|9.3.1",
    key="10113701463508402",
    yandexuid=10113701463508402,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="10113701463508337",
    ua_profile="m|phone|apple|ios|9.3.1",
    key="10113701463508337",
    yandexuid=10113701463508337,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="2410530891459150418",
    ua_profile="m|phone|apple|ios|9.3.1",
    key="2410530891459150418",
    yandexuid=2410530891459150418,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="2410530891459150411",
    ua_profile="m|phone|apple|ios|9.3.1",
    key="2410530891459150411",
    yandexuid=2410530891459150411,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="2410530891459150413",
    ua_profile="m|phone|apple|ios|9.3.1",
    key="2410530891459150413",
    yandexuid=2410530891459150413,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)
testdata_yuid_with_all.add_row(
    yuid="2410530891459150414",
    ua_profile="m|phone|apple|ios|9.3.1",
    key="2410530891459150414",
    yandexuid=2410530891459150414,
    all_dates=["2016-04-10"],
    good=True,
    yuid_creation_date=["2016-02-10"],
)


testdata_dev_info_yt = BaseLog(
    path="//crypta/production/state/graph/dicts", table_name="dev_info_yt", default_data={}, append=True
)
testdata_dev_info_yt.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE1",
    subkey="9223372035394559440",
    locale="ru-RU",
    device_type="phone",
    manufacturer="Apple",
    dates=["2016-04-11"],
    mmetric_devids="53b84e78dbf8e7aa77ac092137057bc1",
    os_version="9.3.1",
    ua_profile="m|phone|apple|ios|9.3.1",
)
testdata_dev_info_yt.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE2",
    subkey="9223372035394559441",
    locale="ru-RU",
    device_type="phone",
    manufacturer="Apple",
    dates=["2016-04-11"],
    mmetric_devids="53b84e78dbf8e7aa77ac092137057bc2",
    os_version="9.3.1",
    ua_profile="m|phone|apple|ios|9.3.1",
)
testdata_dev_info_yt.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE3",
    subkey="9223372035394559442",
    locale="ru-RU",
    device_type="phone",
    manufacturer="Apple",
    dates=["2016-04-11"],
    mmetric_devids="53b84e78dbf8e7aa77ac092137057bc3",
    os_version="9.3.1",
    ua_profile="m|phone|apple|ios|9.3.1",
)
testdata_dev_info_yt.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE4",
    subkey="9223372035394559443",
    locale="ru-RU",
    device_type="phone",
    manufacturer="Apple",
    dates=["2016-04-11"],
    mmetric_devids="53b84e78dbf8e7aa77ac092137057bc4",
    os_version="9.3.1",
    ua_profile="m|phone|apple|ios|9.3.1",
)
testdata_dev_info_yt.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE5",
    subkey="9223372035394559444",
    locale="ru-RU",
    device_type="phone",
    manufacturer="Apple",
    dates=["2016-04-11"],
    mmetric_devids="886f3cd0a0fa93a19f9d35362dcc111d",
    os_version="9.3.1",
    ua_profile="m|phone|apple|ios|9.3.1",
)
testdata_dev_info_yt.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE6",
    subkey="9223372035394559445",
    locale="ru-RU",
    device_type="phone",
    manufacturer="Apple",
    dates=["2016-04-11"],
    mmetric_devids="886f3cd0a0fa93a19f9d35362dcc222d",
    os_version="9.3.1",
    ua_profile="m|phone|apple|ios|9.3.1",
)
testdata_dev_info_yt.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE7",
    subkey="9223372035394559446",
    locale="ru-RU",
    device_type="phone",
    manufacturer="Apple",
    dates=["2016-04-11"],
    mmetric_devids="886f3cd0a0fa93a19f9d35362dcc333d",
    os_version="9.3.1",
    ua_profile="m|phone|apple|ios|9.3.1",
)
testdata_dev_info_yt.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE8",
    subkey="9223372035394559447",
    locale="ru-RU",
    device_type="phone",
    manufacturer="Apple",
    dates=["2016-04-11"],
    mmetric_devids="886f3cd0a0fa93a19f9d35362dcc444d",
    os_version="9.3.1",
    ua_profile="m|phone|apple|ios|9.3.1",
)


testdata_fuzzy_pairs = BaseLog(
    path="//crypta/production/state/prob-match", table_name="fuzzy_pairs_new", default_data=FUZZY_PAIRS, append=True
)

testdata_fuzzy_pairs.add_row(key="1848943511472921939_2398601481472645944")
testdata_fuzzy_pairs.add_row(key="1848943511472921939_2947837971475153894")
testdata_fuzzy_pairs.add_row(key="1858634981507855058_2357030971503056244")
testdata_fuzzy_pairs.add_row(key="1858634981507855058_8941621311501339324")
testdata_fuzzy_pairs.add_row(key="1858634981507855058_8941621311501339321")
testdata_fuzzy_pairs.add_row(key="1858635341510640946_6573432031508304404")

testdata_sdk_log = BaseLog(
    path="//home/logfeller/logs/mobile-redirect-bind-id-log/1d",
    date="2016-04-11",
    default_data=MOBILE_REDIRECT_BIND_LOG,
)

testdata_sdk_log.add_row(
    cookies="yandexuid=1380236561466524390",
    host="redirect.appmetrica.yandex.ru",
    uuid="6f46d7d80cf736a7d25ef6772268b91d",
    timestamp="2016-06-21 15:53:10",
    timezone="+0000",
    device_id="EA9FCA74-3D03-4238-8C1F-E078326D0B05",
)
testdata_sdk_log.add_row(
    cookies="yandexuid=6436305441466524394",
    host="redirect.appmetrica.yandex.ru",
    uuid="3e2b6871964bb53513eacac621970e9b",
    timestamp="2016-06-21 15:53:14",
    timezone="+0000",
    device_id="909F11DB-70C4-4376-9870-FCD52B0191ED",
)
testdata_sdk_log.add_row(
    cookies="yandexuid=6599231981466524395",
    host="redirect.appmetrica.yandex.com",
    uuid="47add5bfa456676d02ebfe7e6ed0543b",
    timestamp="2016-06-21 15:53:15",
    timezone="+0000",
    device_id="ADCE9E39-D7E4-4CD8-B91D-1F3B8B6BA1BE",
)
testdata_sdk_log.add_row(
    _date="2016-04-09",
    cookies="yandexuid=5046511531466502940",
    iso_eventtime="2016-06-21 12:55:40",
    uuid="b9049bfd1046210096e086e55adb6b6b",
    timestamp="2016-06-21 09:55:40",
    device_id="A5E0873B-DCB6-48F7-9F52-6B7F8FD74193",
)
testdata_sdk_log.add_row(
    _date="2016-04-09",
    cookies="yandexuid=5046511531466502940",
    iso_eventtime="2016-06-21 12:55:40",
    uuid="b9049bfd1046210096e086e55adb6b6b",
    timestamp="2016-06-21 09:55:40",
    device_id="A5E0873B-DCB6-48F7-9F52-6B7F8FD74193",
)
testdata_sdk_log.add_row(
    _date="2016-04-09",
    cookies="yandexuid=6010993001466221174",
    iso_eventtime="2016-06-21 12:55:42",
    uuid="bcaced7ed952fb86e25903257248a8d4",
    timestamp="2016-06-21 09:55:42",
    device_id="11421A51-1B88-4F6A-8C9E-11DAE5B768B8",
)
testdata_sdk_log.add_row(
    _date="2016-04-10",
    cookies="yandexuid=5022275111466524406",
    iso_eventtime="2016-06-21 18:53:26",
    uuid="0b2be470b9ebeabc208fe7de8ec3c10b",
    timestamp="2016-06-21 15:53:26",
    host="redirect.appmetrica.yandex.com",
    device_id="E92D8B3B-1898-41F8-BB0F-8494C1EFACB0",
)
testdata_sdk_log.add_row(
    _date="2016-04-10",
    cookies="yandexuid=5022275111466524406",
    iso_eventtime="2016-06-21 18:53:26",
    uuid="0b2be470b9ebeabc208fe7de8ec3c10b",
    timestamp="2016-06-21 15:53:26",
    host="redirect.appmetrica.yandex.com",
    device_id="E92D8B3B-1898-41F8-BB0F-8494C1EFACB0",
)
testdata_sdk_log.add_row(
    _date="2016-04-10",
    cookies="yandexuid=9362623411466524410",
    iso_eventtime="2016-06-21 18:53:30",
    uuid="7e847460ed7643df08da0b2d49c586cb",
    timestamp="2016-06-21 15:53:30",
    host="redirect.appmetrica.yandex.ru",
    device_id="3C791CEB-7455-4147-9A8B-94E7FC580BED",
)


testdata_bb_storage = BaseLog(
    path="//crypta/production/profiles/export",
    table_name="profiles_for_14days",
    default_data={
        "yandexuid": 2291492277554,
        "user_age_6s": {
            "0_17": 0.040821999999999997177,
            "18_24": 0.083685999999999996501,
            "25_34": 0.23375399999999998957,
            "35_44": 0.26731300000000002282,
            "45_54": 0.22475100000000000633,
            "55_99": 0.14966999999999999749,
        },
        "yandex_loyalty": 0.66666599999999998083,
        "lal_internal": None,
        "age_segments": {
            "0_17": 0.040821999999999997177,
            "18_24": 0.083685999999999996501,
            "25_34": 0.23375399999999998957,
            "35_44": 0.26731300000000002282,
            "45_99": 0.37442199999999997706,
        },
        "multiclass_segments": None,
        "heuristic_private": None,
        "heuristic_internal": None,
        "affinitive_sites": None,
        "top_common_site_ids": [40298240, 452665600, 242914048],
        "search_fraudness": 0.0,
        "update_time": 1492377501,
        "top_common_sites": None,
        "probabilistic_segments": {
            "319": {"0": 0.8843469999999999942},
            "101": {"0": 0.0},
            "304": {"0": 0.83724500000000001698},
            "316": {"0": 0.014184999999999999692},
            "315": {"0": 0.072932999999999997831},
            "102": {"0": 0.0},
            "8": {"1": 0.66666599999999998083, "0": 0.33333299999999999041},
            "434": {"0": 0.36271999999999998687},
            "435": {"0": 0.54925699999999999523},
        },
        "lal_common": None,
        "lal_private": None,
        "interests_composite": None,
        "gender": {"m": 0.51139100000000003998, "f": 0.48860799999999998677},
        "marketing_segments": {"38": 1.0, "22": 1.0, "96": 1.0},
        "income_segments": {"A": 0.13852200000000000624, "C": 0.38494499999999998163, "B": 0.47653200000000001113},
        "income_5_segments": {
            "A": 0.13852200000000000624,
            "B1": 0.37653200000000001113,
            "B2": 0.1,
            "C1": 0.28494499999999998163,
            "C2": 0.1,
        },
        "interests_longterm": None,
        "heuristic_segments": None,
        "yandex_services_visits": None,
        "heuristic_common": None,
        "ado_lal": {
            "1458822180": 91,
            "1458822173": 89,
            "1458038003": 84,
            "1458822188": 90,
            "1458803677": 87,
            "1458030781": 84,
            "1458803679": 90,
        },
    },
)
testdata_bb_storage.add_row(yandexuid=6513755131454170183)
testdata_bb_storage.add_row(
    yandexuid=8487166031445176273, gender={"m": 0.11139100000000003998, "f": 0.88860799999999998677}
)
testdata_bb_storage.add_row(yandexuid=69038631452506392)


testdata_tickets = BaseLog(
    path="//home/afisha/alet/user_actions/production",
    table_name="ticket_orders_aletuid",
    default_data=TICKETS_ORDER_LOG,
)

testdata_tickets.add_row(
    user_puid="38429802",
    user_yandexuid="651537761454077784",
    action_time=1473243114571,
    service_info={
        "created": 1473243114572,
        "updated": 1473243114573,
        "userInfo": {"email": "234234@mail2.ru", "phone": "+79853254553"},
        "sessionInfo": {"sessionDate": 1473243114574},
    },
)
testdata_tickets.add_row(
    user_puid="93376855",
    user_yandexuid="7429618261455561583",
    action_time=1473243114571,
    service_info={
        "created": 1473243114571,
        "updated": 1473243114571,
        "userInfo": {"email": "", "phone": "+79853254553"},
        "sessionInfo": {"sessionDate": 1473243114571},
    },
)
testdata_tickets.add_row(
    user_puid="2907844",
    user_yandexuid="1056316461383670897",
    action_time=1473233114571,
    service_info={
        "created": 1473233114571,
        "updated": 1473233114571,
        "userInfo": {"email": "sdfs@mail2.ru", "phone": ""},
        "sessionInfo": {"sessionDate": 1473233114571},
    },
)
testdata_tickets.add_row(
    user_puid="73086412",
    user_yandexuid="6558487031447442276",
    action_time=1463233114571,
    service_info={
        "created": 1463233114571,
        "updated": 1463233114571,
        "userInfo": {"email": "sfdf@yandex.ru", "phone": ""},
        "sessionInfo": {"sessionDate": 1463233114571},
    },
)
testdata_tickets.add_row(
    user_puid="3940302",
    user_yandexuid="",
    action_time=1473233114571,
    service_info={
        "created": 1473233114571,
        "updated": 1473233114571,
        "userInfo": {"email": "", "phone": "+79853254552"},
        "sessionInfo": {"sessionDate": 1473233114571},
    },
)
testdata_tickets.add_row(
    user_puid="",
    user_yandexuid="7028084711356084504",
    action_time=1463233114571,
    service_info={
        "created": 1506591185953,
        "updated": 1506591185953,
        "userInfo": {"email": "", "phone": "+79853254551"},
        "sessionInfo": {"sessionDate": 1506591185953},
    },
)


"""
For create model need:
age segment [(0, 17), (18, 24), (25, 34), (35, 44), (45, 1000)]
and
income segment [(0, 25503), (25504, 51782), (51783, MAXINT)]
"""
income_data = [
    ("06b27502fa88e45d459e10a770aaf099", 0, (1500.0, 1500.0, 1500.0)),
    ("07575519ffd68630e4cc28827112b5e1", 1, (11000.0, 11000.0, 11000.0)),
    ("09e095b02a184d924af0c61d33f3bfa0", 17, (19000.0, 19000.0, 19000.0)),
    ("0aa344455c3d9ff82bf5394106483061", 18, (1500.0, 1500.0, 1500.0)),
    ("010ca72bf270f5b7d10c5762a17b19fb", 21, (11000.0, 11000.0, 11000.0)),
    ("58ec6971daa4023454732c82751e99fa", 24, (19000.0, 19000.0, 19000.0)),
    ("0bea5d3d17301b19431f960ad763a88c", 25, (1500.0, 1500.0, 1500.0)),
    ("30EDAB1F-92E1-47A8-A95E-A0674B36A4BA", 27, (11000.0, 11000.0, 11000.0)),
    ("51B5AD57-3C4C-46E2-932E-810D3B59B2C7", 34, (19000.0, 19000.0, 19000.0)),
    ("6B12FC00-BA64-411A-B627-2F79207A447B", 35, (1500.0, 1500.0, 1500.0)),
    ("70F890A4-C735-46D4-AD93-98D7C26E90E1", 37, (11000.0, 11000.0, 11000.0)),
    ("e02d9dbd569a7551d496c2372989f64d", 44, (19000.0, 19000.0, 19000.0)),
    ("3f68302e012966002d0b82cef83788c7", 45, (1500.0, 1500.0, 1500.0)),
    ("782B819E-824B-4239-80D9-1DDD15D3B790", 71, (11000.0, 11000.0, 11000.0)),
    ("0233F335-710B-4026-86F8-F6B11D69719E", 99, (19000.0, 19000.0, 19000.0)),
]
testdata_dicts_income_data_with_dev_info = BaseLog(
    path="//crypta/production/state/graph/dicts",
    table_name="income_data_with_dev_info",
    default_data=DICTS_INCOME_DATA_WITH_DEV_INFO,
)
for rec in income_data:
    testdata_dicts_income_data_with_dev_info.add_row(
        aug=rec[2][0],
        sep=rec[2][1],
        oct=rec[2][2],
        sum=sum(rec[2]),
        yob=(datetime.now() - timedelta(days=rec[1] * 365)).strftime("%d.%m.%Y"),
        key=rec[0],
        device_id=rec[0],
    )


testdata_dicts_passport_phone_dump_unknown = BaseLog(
    path="//crypta/production/state/graph/dicts/external_dumps",
    table_name="passport_phone_dump_unknown",
    default_data=DICTS_PASSPORT_DUMP,
)
testdata_dicts_passport_phone_dump_unknown.add_row(
    puid="103299945", key="103299945", id_prefix="7904", id_value="fdae68989b74ac9d653379b590d4770c"
)
testdata_dicts_passport_phone_dump_unknown.add_row(
    puid="103299945", key="103299945", id_prefix="7904", id_value="fdae68989b74ac9d653379b590d4770c"
)
testdata_dicts_passport_phone_dump_unknown.add_row(
    puid="211539243", key="211539243", id_prefix="7978", id_value="eb37f718d957ce6b72fb016fb712a0ca"
)
testdata_dicts_passport_phone_dump_unknown.add_row(
    puid="103299945", key="103299945", id_prefix="7904", id_value="fdae68989b74ac9d653379b590d4770c"
)
testdata_dicts_passport_phone_dump_unknown.add_row(
    puid="211539243", key="211539243", id_prefix="7978", id_value="eb37f718d957ce6b72fb016fb712a0ca"
)

testdata_dicts_passport_phone_dump_phne = BaseLog(
    path="//crypta/production/state/graph/dicts/external_dumps",
    table_name="passport_phone_phne_dump_01_11_2017",
    default_data=DICTS_PASSPORT_DUMP,
)
testdata_dicts_passport_phone_dump_phne.add_row(
    puid="211539243", key="211539243", id_prefix="7978", id_value="eb37f718d957ce6b72fb016fb712a0ca"
)
testdata_dicts_passport_phone_dump_phne.add_row(
    puid="46178168", key="46178168", id_prefix="7911", id_value="5f82882588d42a6c11d97402c545c5a7"
)
testdata_dicts_passport_phone_dump_phne.add_row(
    puid="103299945", key="103299945", id_prefix="7904", id_value="fdae68989b74ac9d653379b590d4770c"
)
testdata_dicts_passport_phone_dump_phne.add_row(
    puid="211539243", key="211539243", id_prefix="7978", id_value="eb37f718d957ce6b72fb016fb712a0ca"
)
testdata_dicts_passport_phone_dump_phne.add_row(
    puid="46178168", key="46178168", id_prefix="7911", id_value="5f82882588d42a6c11d97402c545c5a7"
)
testdata_dicts_passport_phone_dump_phne.add_row(
    puid="38170735", key="38170735", id_prefix="7919", id_value="67d530c865bd6852a6f65e1ed62adb8e"
)


testdata_dicts_puid_yuid = BaseLog(
    path="//crypta/production/state/graph/dicts", table_name="puid_yuid", default_data=DICTS_PUID_YUID
)
testdata_dicts_puid_yuid.add_row(key="311111525", value="1947874031445763853")
testdata_dicts_puid_yuid.add_row(key="311111525", value="1947874031445763853")
testdata_dicts_puid_yuid.add_row(key="311338458", value="696766411409129307")
testdata_dicts_puid_yuid.add_row(key="311111525", value="1947874031445763853")
testdata_dicts_puid_yuid.add_row(key="311338458", value="696766411409129307")
testdata_dicts_puid_yuid.add_row(key="311338458", value="8962997951442059627")
testdata_dicts_puid_yuid.add_row(key="311111525", value="1947874031445763853")
testdata_dicts_puid_yuid.add_row(key="311338458", value="696766411409129307")
testdata_dicts_puid_yuid.add_row(key="311338458", value="8962997951442059627")
testdata_dicts_puid_yuid.add_row(key="311338458", value="1275077871404239998")
testdata_dicts_puid_yuid.add_row(key="311111525", value="1947874031445763853")
testdata_dicts_puid_yuid.add_row(key="311338458", value="696766411409129307")
testdata_dicts_puid_yuid.add_row(key="311338458", value="8962997951442059627")
testdata_dicts_puid_yuid.add_row(key="311338458", value="1275077871404239998")
testdata_dicts_puid_yuid.add_row(key="311394747", value="359267081445437693")
testdata_dicts_puid_yuid.add_row(key="311111525", value="1947874031445763853")
testdata_dicts_puid_yuid.add_row(key="311338458", value="696766411409129307")
testdata_dicts_puid_yuid.add_row(key="311338458", value="8962997951442059627")
testdata_dicts_puid_yuid.add_row(key="311338458", value="1275077871404239998")
testdata_dicts_puid_yuid.add_row(key="311394747", value="359267081445437693")
testdata_dicts_puid_yuid.add_row(key="311394747", value="1505327461427036757")
testdata_dicts_puid_yuid.add_row(key="311111525", value="1947874031445763853")
testdata_dicts_puid_yuid.add_row(key="311338458", value="696766411409129307")


testdata_dicts_puid_yuid_yt = BaseLog(
    path="//crypta/production/state/graph/dicts", table_name="puid_yuid_yt", default_data=DICTS_PUID_YUID_YT
)
testdata_dicts_puid_yuid_yt.add_row(
    yuid="3390117391388757081",
    puid="254075620",
    match_chain="{u'login': {u'fp': {u'a-arapowa2015': {u'2016-04-22': 173, u'2016-04-25': 156, u'2016-04-14': 77, u'2016-04-15': 7, u'2016-04-24': 60, u'2016-04-03': 27, u'2016-04-02': 54, u'2016-04-18': 154, u'2016-04-19': 46, u'2016-03-31': 16, u'2016-03-30': 32, u'2016-03-28': 89, u'2016-03-29': 66, u'2016-04-16': 44, u'2016-04-17': 21, u'2016-04-20': 10, u'2016-04-11': 2, u'2016-04-21': 54, u'2016-04-13': 61}}}}",
)
testdata_dicts_puid_yuid_yt.add_row(
    yuid="3390117391388757081",
    puid="254075620",
    match_chain="{u'login': {u'fp': {u'a-arapowa2015': {u'2016-04-22': 173, u'2016-04-25': 156, u'2016-04-14': 77, u'2016-04-15': 7, u'2016-04-24': 60, u'2016-04-03': 27, u'2016-04-02': 54, u'2016-04-18': 154, u'2016-04-19': 46, u'2016-03-31': 16, u'2016-03-30': 32, u'2016-03-28': 89, u'2016-03-29': 66, u'2016-04-16': 44, u'2016-04-17': 21, u'2016-04-20': 10, u'2016-04-11': 2, u'2016-04-21': 54, u'2016-04-13': 61}}}}",
)
testdata_dicts_puid_yuid_yt.add_row(
    yuid="5177511381442986016",
    puid="173066488",
    match_chain="{u'login': {u'fp': {u'karandeyvalentina': {u'2016-04-03': 54, u'2016-04-02': 29, u'2016-04-01': 148, u'2016-04-07': 100, u'2016-04-06': 84, u'2016-04-11': 16, u'2016-04-04': 116, u'2016-04-21': 43, u'2016-04-20': 143, u'2016-04-09': 17, u'2016-04-08': 41, u'2016-04-25': 77, u'2016-04-24': 41, u'2016-04-26': 69, u'2016-04-22': 148, u'2016-03-28': 81, u'2016-03-29': 201, u'2016-04-18': 133, u'2016-04-19': 18, u'2016-04-14': 56, u'2016-04-15': 33, u'2016-04-16': 35, u'2016-04-17': 25, u'2016-04-10': 9, u'2016-04-11': 77, u'2016-04-12': 173, u'2016-04-13': 166, u'2016-03-31': 34, u'2016-03-30': 84, u'2016-04-23': 73}}}}",
)
testdata_dicts_puid_yuid_yt.add_row(
    yuid="3390117391388757081",
    puid="254075620",
    match_chain="{u'login': {u'fp': {u'a-arapowa2015': {u'2016-04-22': 173, u'2016-04-25': 156, u'2016-04-14': 77, u'2016-04-15': 7, u'2016-04-24': 60, u'2016-04-03': 27, u'2016-04-02': 54, u'2016-04-18': 154, u'2016-04-19': 46, u'2016-03-31': 16, u'2016-03-30': 32, u'2016-03-28': 89, u'2016-03-29': 66, u'2016-04-16': 44, u'2016-04-17': 21, u'2016-04-20': 10, u'2016-04-11': 2, u'2016-04-21': 54, u'2016-04-13': 61}}}}",
)
testdata_dicts_puid_yuid_yt.add_row(
    yuid="5177511381442986016",
    puid="173066488",
    match_chain="{u'login': {u'fp': {u'karandeyvalentina': {u'2016-04-03': 54, u'2016-04-02': 29, u'2016-04-01': 148, u'2016-04-07': 100, u'2016-04-06': 84, u'2016-04-11': 16, u'2016-04-04': 116, u'2016-04-21': 43, u'2016-04-20': 143, u'2016-04-09': 17, u'2016-04-08': 41, u'2016-04-25': 77, u'2016-04-24': 41, u'2016-04-26': 69, u'2016-04-22': 148, u'2016-03-28': 81, u'2016-03-29': 201, u'2016-04-18': 133, u'2016-04-19': 18, u'2016-04-14': 56, u'2016-04-15': 33, u'2016-04-16': 35, u'2016-04-17': 25, u'2016-04-10': 9, u'2016-04-11': 77, u'2016-04-12': 173, u'2016-04-13': 166, u'2016-03-31': 34, u'2016-03-30': 84, u'2016-04-23': 73}}}}",
)


testdata_dicts_yuid_regs = BaseLog(
    path="//crypta/production/state/graph/dicts", table_name="yuid_regs", default_data=DICTS_YUID_REGS
)
testdata_dicts_yuid_regs.add_row(
    key="9119007301455357385",
    value="67|2016-03-26:313,2016-04-02:3,2016-03-29:25,2016-03-22:78,2016-03-21:9,2016-04-01:4,2016-03-23:20,2016-03-19:1,2016-03-18:32,2016-04-11:170,2016-03-30:4,2016-04-07:32,2016-03-17:74,2016-03-16:15,2016-04-12:25",
)
testdata_dicts_yuid_regs.add_row(
    key="9119007301455357385",
    value="67|2016-03-26:313,2016-04-02:3,2016-03-29:25,2016-03-22:78,2016-03-21:9,2016-04-01:4,2016-03-23:20,2016-03-19:1,2016-03-18:32,2016-04-11:170,2016-03-30:4,2016-04-07:32,2016-03-17:74,2016-03-16:15,2016-04-12:25",
)
testdata_dicts_yuid_regs.add_row(
    key="912067061435494047", value="21619|2016-03-16:11,2016-03-18:3,2016-04-12:14,2016-03-30:45"
)
testdata_dicts_yuid_regs.add_row(
    key="9119007301455357385",
    value="67|2016-03-26:313,2016-04-02:3,2016-03-29:25,2016-03-22:78,2016-03-21:9,2016-04-01:4,2016-03-23:20,2016-03-19:1,2016-03-18:32,2016-04-11:170,2016-03-30:4,2016-04-07:32,2016-03-17:74,2016-03-16:15,2016-04-12:25",
)
testdata_dicts_yuid_regs.add_row(
    key="912067061435494047", value="21619|2016-03-16:11,2016-03-18:3,2016-04-12:14,2016-03-30:45"
)
testdata_dicts_yuid_regs.add_row(
    key="2341942641448250325",
    value="65|2016-04-02:13,2016-04-01:102,2016-04-07:39,2016-04-06:10,2016-04-11:54,2016-04-04:22,2016-04-08:81,2016-03-20:43,2016-03-21:56,2016-03-22:45,2016-03-23:66,2016-03-24:23,2016-03-25:31,2016-03-26:161,2016-03-27:44,2016-03-28:35,2016-03-29:76,2016-04-10:96,2016-04-11:43,2016-04-12:67,2016-03-19:72,2016-03-18:57,2016-03-31:67,2016-03-30:57,2016-03-15:23,2016-03-14:29,2016-03-17:54,2016-03-16:53",
)
testdata_dicts_yuid_regs.add_row(
    key="9119007301455357385",
    value="67|2016-03-26:313,2016-04-02:3,2016-03-29:25,2016-03-22:78,2016-03-21:9,2016-04-01:4,2016-03-23:20,2016-03-19:1,2016-03-18:32,2016-04-11:170,2016-03-30:4,2016-04-07:32,2016-03-17:74,2016-03-16:15,2016-04-12:25",
)
testdata_dicts_yuid_regs.add_row(
    key="912067061435494047", value="21619|2016-03-16:11,2016-03-18:3,2016-04-12:14,2016-03-30:45"
)
testdata_dicts_yuid_regs.add_row(
    key="2341942641448250325",
    value="65|2016-04-02:13,2016-04-01:102,2016-04-07:39,2016-04-06:10,2016-04-11:54,2016-04-04:22,2016-04-08:81,2016-03-20:43,2016-03-21:56,2016-03-22:45,2016-03-23:66,2016-03-24:23,2016-03-25:31,2016-03-26:161,2016-03-27:44,2016-03-28:35,2016-03-29:76,2016-04-10:96,2016-04-11:43,2016-04-12:67,2016-03-19:72,2016-03-18:57,2016-03-31:67,2016-03-30:57,2016-03-15:23,2016-03-14:29,2016-03-17:54,2016-03-16:53",
)
testdata_dicts_yuid_regs.add_row(
    key="2342505821375978805",
    value="2|2016-04-03:36,2016-03-20:18,2016-04-02:47,2016-03-19:26,2016-04-09:4,2016-04-10:5;213|2016-03-19:2,2016-04-04:2,2016-03-31:1,2016-03-29:5,2016-03-30:2,2016-03-20:1,2016-03-21:3,2016-03-23:2,2016-04-07:6,2016-03-18:1,2016-04-11:2,2016-03-27:7,2016-03-28:2,2016-03-24:1,2016-03-14:2,2016-03-16:1,2016-04-11:1,2016-03-25:37",
)
testdata_dicts_yuid_regs.add_row(
    key="9119007301455357385",
    value="67|2016-03-26:313,2016-04-02:3,2016-03-29:25,2016-03-22:78,2016-03-21:9,2016-04-01:4,2016-03-23:20,2016-03-19:1,2016-03-18:32,2016-04-11:170,2016-03-30:4,2016-04-07:32,2016-03-17:74,2016-03-16:15,2016-04-12:25",
)
testdata_dicts_yuid_regs.add_row(
    key="912067061435494047", value="21619|2016-03-16:11,2016-03-18:3,2016-04-12:14,2016-03-30:45"
)


testdata_yamoney_yamoney_in_v1 = BaseLog(
    path="//crypta/production/state/graph/dicts/yamoney",
    table_name="yamoney_in_v1",
    default_data=YAMONEY_YAMONEY_IN_V1,
)
testdata_yamoney_yamoney_in_v1.add_row(
    phone="79196954666", PAYER_ENTITY_IS_WALLET="True", email="n.e.v.i@yandex.ru", PAYER_ENTITY_UID="341506301"
)
testdata_yamoney_yamoney_in_v1.add_row(
    phone="79196954666", PAYER_ENTITY_IS_WALLET="True", email="n.e.v.i@yandex.ru", PAYER_ENTITY_UID="341506301"
)
testdata_yamoney_yamoney_in_v1.add_row(
    PAYER_ENTITY_ACCOUNT_ID="410011139750650",
    phone="380632759313",
    PAYER_ENTITY_ID="29189502",
    PAYER_ENTITY_IS_WALLET="True",
    dataopen="03.10.2011",
    PAYER_ENTITY_UID="136179756",
)
testdata_yamoney_yamoney_in_v1.add_row(
    phone="79196954666", PAYER_ENTITY_IS_WALLET="True", email="n.e.v.i@yandex.ru", PAYER_ENTITY_UID="341506301"
)
testdata_yamoney_yamoney_in_v1.add_row(
    PAYER_ENTITY_ACCOUNT_ID="410011139750650",
    phone="380632759313",
    PAYER_ENTITY_ID="29189502",
    PAYER_ENTITY_IS_WALLET="True",
    dataopen="03.10.2011",
    PAYER_ENTITY_UID="136179756",
)
testdata_yamoney_yamoney_in_v1.add_row(
    PAYER_ENTITY_IDENTIFIER_FIRST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_ALIVE_DAY_ID="42401",
    PAYER_ENTITY_IDENTIFIER="a984efb5ca7756e0cbce5a544f3b6fe283158b3b",
    PAYER_ENTITY_IDENTIFIER_TYPE="2",
    PAYER_ENTITY_ID="15992859",
    PAYER_ENTITY_IS_WALLET="False",
    PAYER_ENTITY_YANDEX_UID="124308321447141881",
    PAYER_ENTITY_IDENTIFIER_IS_PRIMARY="True",
    PAYER_ENTITY_ANONYMITY_LAST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_UID="332646036",
)
testdata_yamoney_yamoney_in_v1.add_row(
    phone="79196954666", PAYER_ENTITY_IS_WALLET="True", email="n.e.v.i@yandex.ru", PAYER_ENTITY_UID="341506301"
)
testdata_yamoney_yamoney_in_v1.add_row(
    PAYER_ENTITY_ACCOUNT_ID="410011139750650",
    phone="380632759313",
    PAYER_ENTITY_ID="29189502",
    PAYER_ENTITY_IS_WALLET="True",
    dataopen="03.10.2011",
    PAYER_ENTITY_UID="136179756",
)
testdata_yamoney_yamoney_in_v1.add_row(
    PAYER_ENTITY_IDENTIFIER_FIRST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_ALIVE_DAY_ID="42401",
    PAYER_ENTITY_IDENTIFIER="a984efb5ca7756e0cbce5a544f3b6fe283158b3b",
    PAYER_ENTITY_IDENTIFIER_TYPE="2",
    PAYER_ENTITY_ID="15992859",
    PAYER_ENTITY_IS_WALLET="False",
    PAYER_ENTITY_YANDEX_UID="124308321447141881",
    PAYER_ENTITY_IDENTIFIER_IS_PRIMARY="True",
    PAYER_ENTITY_ANONYMITY_LAST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_UID="332646036",
)
testdata_yamoney_yamoney_in_v1.add_row(
    PAYER_ENTITY_IDENTIFIER_FIRST_DATE="2016-02-01 08:00:33.607000000",
    PAYER_ENTITY_ALIVE_DAY_ID="42401",
    PAYER_ENTITY_IDENTIFIER="asd@asd.asd",
    PAYER_ENTITY_IDENTIFIER_TYPE="3",
    PAYER_ENTITY_ID="15992859",
    PAYER_ENTITY_IS_WALLET="False",
    PAYER_ENTITY_YANDEX_UID="124308321447141881",
    PAYER_ENTITY_IDENTIFIER_IS_PRIMARY="False",
    PAYER_ENTITY_ANONYMITY_LAST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_UID="242159443",
)
testdata_yamoney_yamoney_in_v1.add_row(
    phone="79196954666", PAYER_ENTITY_IS_WALLET="True", email="n.e.v.i@yandex.ru", PAYER_ENTITY_UID="341506301"
)
testdata_yamoney_yamoney_in_v1.add_row(
    PAYER_ENTITY_ACCOUNT_ID="410011139750650",
    phone="380632759313",
    PAYER_ENTITY_ID="29189502",
    PAYER_ENTITY_IS_WALLET="True",
    dataopen="03.10.2011",
    PAYER_ENTITY_UID="136179756",
)
testdata_yamoney_yamoney_in_v1.add_row(
    PAYER_ENTITY_IDENTIFIER_FIRST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_ALIVE_DAY_ID="42401",
    PAYER_ENTITY_IDENTIFIER="a984efb5ca7756e0cbce5a544f3b6fe283158b3b",
    PAYER_ENTITY_IDENTIFIER_TYPE="2",
    PAYER_ENTITY_ID="15992859",
    PAYER_ENTITY_IS_WALLET="False",
    PAYER_ENTITY_YANDEX_UID="124308321447141881",
    PAYER_ENTITY_IDENTIFIER_IS_PRIMARY="True",
    PAYER_ENTITY_ANONYMITY_LAST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_UID="332646036",
)
testdata_yamoney_yamoney_in_v1.add_row(
    PAYER_ENTITY_IDENTIFIER_FIRST_DATE="2016-02-01 08:00:33.607000000",
    PAYER_ENTITY_ALIVE_DAY_ID="42401",
    PAYER_ENTITY_IDENTIFIER="asd@asd.asd",
    PAYER_ENTITY_IDENTIFIER_TYPE="3",
    PAYER_ENTITY_ID="15992859",
    PAYER_ENTITY_IS_WALLET="False",
    PAYER_ENTITY_YANDEX_UID="124308321447141881",
    PAYER_ENTITY_IDENTIFIER_IS_PRIMARY="False",
    PAYER_ENTITY_ANONYMITY_LAST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_UID="242159443",
)
testdata_yamoney_yamoney_in_v1.add_row(
    PAYER_ENTITY_IDENTIFIER_FIRST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_ALIVE_DAY_ID="42401",
    PAYER_ENTITY_IDENTIFIER="yaleksei666@yandex.ru",
    PAYER_ENTITY_IDENTIFIER_TYPE="3",
    PAYER_ENTITY_ID="15992859",
    PAYER_ENTITY_IS_WALLET="False",
    PAYER_ENTITY_YANDEX_UID="124308321447141881",
    PAYER_ENTITY_IDENTIFIER_IS_PRIMARY="False",
    PAYER_ENTITY_ANONYMITY_LAST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_UID="29534744",
)
testdata_yamoney_yamoney_in_v1.add_row(
    phone="79196954666", PAYER_ENTITY_IS_WALLET="True", email="n.e.v.i@yandex.ru", PAYER_ENTITY_UID="341506301"
)
testdata_yamoney_yamoney_in_v1.add_row(
    PAYER_ENTITY_ACCOUNT_ID="410011139750650",
    phone="380632759313",
    PAYER_ENTITY_ID="29189502",
    PAYER_ENTITY_IS_WALLET="True",
    dataopen="03.10.2011",
    PAYER_ENTITY_UID="136179756",
)
testdata_yamoney_yamoney_in_v1.add_row(
    PAYER_ENTITY_IDENTIFIER_FIRST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_ALIVE_DAY_ID="42401",
    PAYER_ENTITY_IDENTIFIER="a984efb5ca7756e0cbce5a544f3b6fe283158b3b",
    PAYER_ENTITY_IDENTIFIER_TYPE="2",
    PAYER_ENTITY_ID="15992859",
    PAYER_ENTITY_IS_WALLET="False",
    PAYER_ENTITY_YANDEX_UID="124308321447141881",
    PAYER_ENTITY_IDENTIFIER_IS_PRIMARY="True",
    PAYER_ENTITY_ANONYMITY_LAST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_UID="332646036",
)
testdata_yamoney_yamoney_in_v1.add_row(
    PAYER_ENTITY_IDENTIFIER_FIRST_DATE="2016-02-01 08:00:33.607000000",
    PAYER_ENTITY_ALIVE_DAY_ID="42401",
    PAYER_ENTITY_IDENTIFIER="asd@asd.asd",
    PAYER_ENTITY_IDENTIFIER_TYPE="3",
    PAYER_ENTITY_ID="15992859",
    PAYER_ENTITY_IS_WALLET="False",
    PAYER_ENTITY_YANDEX_UID="124308321447141881",
    PAYER_ENTITY_IDENTIFIER_IS_PRIMARY="False",
    PAYER_ENTITY_ANONYMITY_LAST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_UID="242159443",
)
testdata_yamoney_yamoney_in_v1.add_row(
    PAYER_ENTITY_IDENTIFIER_FIRST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_ALIVE_DAY_ID="42401",
    PAYER_ENTITY_IDENTIFIER="yaleksei666@yandex.ru",
    PAYER_ENTITY_IDENTIFIER_TYPE="3",
    PAYER_ENTITY_ID="15992859",
    PAYER_ENTITY_IS_WALLET="False",
    PAYER_ENTITY_YANDEX_UID="124308321447141881",
    PAYER_ENTITY_IDENTIFIER_IS_PRIMARY="False",
    PAYER_ENTITY_ANONYMITY_LAST_DATE="2016-02-01 07:59:19.180000000",
    PAYER_ENTITY_UID="29534744",
)

testdata_dev_info_with_income = BaseLog(
    path="//crypta/team/ernest/CRYPTAIS-527", table_name="dev_info_with_income", default_data=DEV_INFO_WITH_INCOME
)
testdata_dev_info_with_income.add_row(
    apps="ru.yandex.metro,com.yandex.browser,ru.yandex.yandexmaps,com.drweb,ru.yandex.yandexnavi,ru.yandex.fines,ru.yandex.yandexcity",
    uat="m|phone|samsung|android|5.0.1",
    income="2",
    model="Galaxy S4",
    manufacturer="Samsung",
    device_id="8e829e09c4ef9d68",
)
testdata_dev_info_with_income.add_row(
    apps="ru.yandex.metro,com.yandex.browser,ru.yandex.yandexmaps,com.drweb,ru.yandex.yandexnavi,ru.yandex.fines,ru.yandex.yandexcity",
    uat="m|phone|samsung|android|5.0.1",
    income="2",
    model="Galaxy S4",
    manufacturer="Samsung",
    device_id="8e829e09c4ef9d68",
)
testdata_dev_info_with_income.add_row(
    apps="ru.yandex.metro,ru.yandex.yandexnavi",
    uat="m|phone|huawei|android|5.1.1",
    income="2",
    model="Che2-L11",
    manufacturer="Huawei",
    device_id="8e85316bb84d9e48",
)
testdata_dev_info_with_income.add_row(
    apps="ru.yandex.metro,com.yandex.browser,ru.yandex.yandexmaps,com.drweb,ru.yandex.yandexnavi,ru.yandex.fines,ru.yandex.yandexcity",
    uat="m|phone|samsung|android|5.0.1",
    income="2",
    model="Galaxy S4",
    manufacturer="Samsung",
    device_id="8e829e09c4ef9d68",
)
testdata_dev_info_with_income.add_row(
    apps="ru.yandex.metro,ru.yandex.yandexnavi",
    uat="m|phone|huawei|android|5.1.1",
    income="2",
    model="Che2-L11",
    manufacturer="Huawei",
    device_id="8e85316bb84d9e48",
)
testdata_dev_info_with_income.add_row(
    apps="com.android.sharedstoragebackup,com.mediatek.lbs.em,com.vkontakte.android,ru.cian.main,com.google.android.apps.maps,com.google.android.partnersetup,com.mediatek.videoplayer,com.mediatek.schpwronoff,com.kms.free,ru.intaxi,com.google.android.onetimeinitializer,com.android.magicsmoke,com.android.packageinstaller,net.megogo.vendor,com.android.documentsui,com.google.android.googlequicksearchbox,com.android.providers.contacts,com.google.android.apps.books,com.rock.gota,com.android.wallpaper.livepicker,ru.mail,com.android.phone,com.android.backupconfirm,ru.yandex.weatherplugin,com.android.galaxy4,com.android.email,com.gismeteo.client,com.android.deskclock,com.android.videoeditor,com.yandex.browser,com.flyhelp.repair,com.android.defcontainer,com.android.providers.downloads,com.android.proxyhandler,com.android.musicvis,com.mediatek.videofavorites,com.opera.preinstall,PSB.Droid,com.mediatek.connectivity,com.android.exchange,com.mediatek.DataUsageLockScreenClient,com.android.pacprocessor,com.google.android.tag,com.android.dialer,com.cleanmaster.mguard,com.android.keychain,com.android.gallery3d,com.android.keyguard,com.google.android.marvin.talkback,com.mediatek,com.mediatek.FMRadio,com.android.systemui,com.google.android.videos,com.android.calculator2,com.android.wallpaper,com.android.inputdevices,com.google.android.apps.docs,ru.yandex.yandexmaps,com.android.wallpaper.holospiral,com.mediatek.CellConnService,com.umtgrn.pdfreader,com.android.providers.drm,com.google.android.syncadapters.contacts,com.google.android.feedback,com.android.facelock,com.mediatek.appguide.plugin,com.android.salestracking,ru.yandex.disk,com.mediatek.vlw,com.mediatek.systemupdate,com.android.browser.provider,com.mediatek.ygps,com.google.android.gsf,com.mediatek.calendarimporter,com.android.printspooler,com.mediatek.apst.target,com.android.providers.media,com.android.chrome,com.mediatek.mtklogger,com.android.providers.calendar,ru.ok.android,com.android.musicfx,ru.beeline.services,ru.sberbank.spasibo,com.android.contacts,com.google.android.gsf.login,com.android.providers.userdictionary,com.android.launcher3,com.google.android.apps.magazines,com.android.noisefield,com.estrongs.android.pop,com.android.providers.partnerbookmarks,com.android.soundrecorder,com.google.android.apps.plus,com.google.android.youtube,com.android.phasebeam,com.ijinshan.kbatterydoctor_en,com.android.simmelock,com.android.htmlviewer,com.google.android.music,com.piriform.ccleaner,ru.megafon.mlk,com.paragon.mts.ma.android,com.android.protips,com.mediatek.voicecommand,com.konka.desktimewidget,com.mediatek.systemupdate.sysoper,ru.yandex.money,com.android.settings,ru.sberbankmobile,com.android.shell,ru.yandex.searchplugin,com.android.providers.settings,ru.simpls.brs2.mobbank,com.google.android.gms,com.google.android.configupdater,com.navitel,com.mediatek.StkSelection,ru.mts.mymts,com.google.android.apps.cloudprint,com.android.externalstorage,com.google.android.tts,com.mediatek.engineermode,com.android.providers.telephony,com.android.vending,com.mediatek.voiceunlock,com.android.bluetooth,com.mediatek.thermalmanager,com.google.android.play.games,com.google.android.talk,com.ubanksu,com.google.android.inputmethod.latin,android,com.skype.raider,com.android.stk,com.google.android.calendar,com.android.certinstaller,com.google.android.backuptransport,com.android.vpndialogs,com.mediatek.bluetooth,com.megogo.application,com.google.android.gm,com.mediatek.batterywarning,com.google.android.setupwizard,com.spotoption.android.titantrade,com.ghisler.android.TotalCommander,com.android.providers.downloads.ui,com.android.mms,com.android.location.fused,com.android.providers.applications,com.opera.mini.native,com.google.android.street,ru.ntv.client,com.android.dreams.basic,com.spotoption.android.interactive_optiontarget",
    uat="m|phone|fly|android|4.4.2",
    income="1",
    model="Fly IQ4514 Quad",
    manufacturer="Fly",
    device_id="8e8afc6432a9953a",
)
testdata_dev_info_with_income.add_row(
    apps="ru.yandex.metro,com.yandex.browser,ru.yandex.yandexmaps,com.drweb,ru.yandex.yandexnavi,ru.yandex.fines,ru.yandex.yandexcity",
    uat="m|phone|samsung|android|5.0.1",
    income="2",
    model="Galaxy S4",
    manufacturer="Samsung",
    device_id="8e829e09c4ef9d68",
)
testdata_dev_info_with_income.add_row(
    apps="ru.yandex.metro,ru.yandex.yandexnavi",
    uat="m|phone|huawei|android|5.1.1",
    income="2",
    model="Che2-L11",
    manufacturer="Huawei",
    device_id="8e85316bb84d9e48",
)
testdata_dev_info_with_income.add_row(
    apps="com.android.sharedstoragebackup,com.mediatek.lbs.em,com.vkontakte.android,ru.cian.main,com.google.android.apps.maps,com.google.android.partnersetup,com.mediatek.videoplayer,com.mediatek.schpwronoff,com.kms.free,ru.intaxi,com.google.android.onetimeinitializer,com.android.magicsmoke,com.android.packageinstaller,net.megogo.vendor,com.android.documentsui,com.google.android.googlequicksearchbox,com.android.providers.contacts,com.google.android.apps.books,com.rock.gota,com.android.wallpaper.livepicker,ru.mail,com.android.phone,com.android.backupconfirm,ru.yandex.weatherplugin,com.android.galaxy4,com.android.email,com.gismeteo.client,com.android.deskclock,com.android.videoeditor,com.yandex.browser,com.flyhelp.repair,com.android.defcontainer,com.android.providers.downloads,com.android.proxyhandler,com.android.musicvis,com.mediatek.videofavorites,com.opera.preinstall,PSB.Droid,com.mediatek.connectivity,com.android.exchange,com.mediatek.DataUsageLockScreenClient,com.android.pacprocessor,com.google.android.tag,com.android.dialer,com.cleanmaster.mguard,com.android.keychain,com.android.gallery3d,com.android.keyguard,com.google.android.marvin.talkback,com.mediatek,com.mediatek.FMRadio,com.android.systemui,com.google.android.videos,com.android.calculator2,com.android.wallpaper,com.android.inputdevices,com.google.android.apps.docs,ru.yandex.yandexmaps,com.android.wallpaper.holospiral,com.mediatek.CellConnService,com.umtgrn.pdfreader,com.android.providers.drm,com.google.android.syncadapters.contacts,com.google.android.feedback,com.android.facelock,com.mediatek.appguide.plugin,com.android.salestracking,ru.yandex.disk,com.mediatek.vlw,com.mediatek.systemupdate,com.android.browser.provider,com.mediatek.ygps,com.google.android.gsf,com.mediatek.calendarimporter,com.android.printspooler,com.mediatek.apst.target,com.android.providers.media,com.android.chrome,com.mediatek.mtklogger,com.android.providers.calendar,ru.ok.android,com.android.musicfx,ru.beeline.services,ru.sberbank.spasibo,com.android.contacts,com.google.android.gsf.login,com.android.providers.userdictionary,com.android.launcher3,com.google.android.apps.magazines,com.android.noisefield,com.estrongs.android.pop,com.android.providers.partnerbookmarks,com.android.soundrecorder,com.google.android.apps.plus,com.google.android.youtube,com.android.phasebeam,com.ijinshan.kbatterydoctor_en,com.android.simmelock,com.android.htmlviewer,com.google.android.music,com.piriform.ccleaner,ru.megafon.mlk,com.paragon.mts.ma.android,com.android.protips,com.mediatek.voicecommand,com.konka.desktimewidget,com.mediatek.systemupdate.sysoper,ru.yandex.money,com.android.settings,ru.sberbankmobile,com.android.shell,ru.yandex.searchplugin,com.android.providers.settings,ru.simpls.brs2.mobbank,com.google.android.gms,com.google.android.configupdater,com.navitel,com.mediatek.StkSelection,ru.mts.mymts,com.google.android.apps.cloudprint,com.android.externalstorage,com.google.android.tts,com.mediatek.engineermode,com.android.providers.telephony,com.android.vending,com.mediatek.voiceunlock,com.android.bluetooth,com.mediatek.thermalmanager,com.google.android.play.games,com.google.android.talk,com.ubanksu,com.google.android.inputmethod.latin,android,com.skype.raider,com.android.stk,com.google.android.calendar,com.android.certinstaller,com.google.android.backuptransport,com.android.vpndialogs,com.mediatek.bluetooth,com.megogo.application,com.google.android.gm,com.mediatek.batterywarning,com.google.android.setupwizard,com.spotoption.android.titantrade,com.ghisler.android.TotalCommander,com.android.providers.downloads.ui,com.android.mms,com.android.location.fused,com.android.providers.applications,com.opera.mini.native,com.google.android.street,ru.ntv.client,com.android.dreams.basic,com.spotoption.android.interactive_optiontarget",
    uat="m|phone|fly|android|4.4.2",
    income="1",
    model="Fly IQ4514 Quad",
    manufacturer="Fly",
    device_id="8e8afc6432a9953a",
)
testdata_dev_info_with_income.add_row(
    apps="ru.yandex.yandexmaps,ru.yandex.yandexnavi",
    uat="m|phone|samsung|android|5.0",
    income="1",
    model="Galaxy S5 Dual SIM",
    manufacturer="Samsung",
    device_id="8e982c3c803cc9f9",
)
testdata_dev_info_with_income.add_row(
    apps="ru.yandex.metro,com.yandex.browser,ru.yandex.yandexmaps,com.drweb,ru.yandex.yandexnavi,ru.yandex.fines,ru.yandex.yandexcity",
    uat="m|phone|samsung|android|5.0.1",
    income="2",
    model="Galaxy S4",
    manufacturer="Samsung",
    device_id="8e829e09c4ef9d68",
)
testdata_dev_info_with_income.add_row(
    apps="ru.yandex.metro,ru.yandex.yandexnavi",
    uat="m|phone|huawei|android|5.1.1",
    income="2",
    model="Che2-L11",
    manufacturer="Huawei",
    device_id="8e85316bb84d9e48",
)
testdata_dev_info_with_income.add_row(
    apps="com.android.sharedstoragebackup,com.mediatek.lbs.em,com.vkontakte.android,ru.cian.main,com.google.android.apps.maps,com.google.android.partnersetup,com.mediatek.videoplayer,com.mediatek.schpwronoff,com.kms.free,ru.intaxi,com.google.android.onetimeinitializer,com.android.magicsmoke,com.android.packageinstaller,net.megogo.vendor,com.android.documentsui,com.google.android.googlequicksearchbox,com.android.providers.contacts,com.google.android.apps.books,com.rock.gota,com.android.wallpaper.livepicker,ru.mail,com.android.phone,com.android.backupconfirm,ru.yandex.weatherplugin,com.android.galaxy4,com.android.email,com.gismeteo.client,com.android.deskclock,com.android.videoeditor,com.yandex.browser,com.flyhelp.repair,com.android.defcontainer,com.android.providers.downloads,com.android.proxyhandler,com.android.musicvis,com.mediatek.videofavorites,com.opera.preinstall,PSB.Droid,com.mediatek.connectivity,com.android.exchange,com.mediatek.DataUsageLockScreenClient,com.android.pacprocessor,com.google.android.tag,com.android.dialer,com.cleanmaster.mguard,com.android.keychain,com.android.gallery3d,com.android.keyguard,com.google.android.marvin.talkback,com.mediatek,com.mediatek.FMRadio,com.android.systemui,com.google.android.videos,com.android.calculator2,com.android.wallpaper,com.android.inputdevices,com.google.android.apps.docs,ru.yandex.yandexmaps,com.android.wallpaper.holospiral,com.mediatek.CellConnService,com.umtgrn.pdfreader,com.android.providers.drm,com.google.android.syncadapters.contacts,com.google.android.feedback,com.android.facelock,com.mediatek.appguide.plugin,com.android.salestracking,ru.yandex.disk,com.mediatek.vlw,com.mediatek.systemupdate,com.android.browser.provider,com.mediatek.ygps,com.google.android.gsf,com.mediatek.calendarimporter,com.android.printspooler,com.mediatek.apst.target,com.android.providers.media,com.android.chrome,com.mediatek.mtklogger,com.android.providers.calendar,ru.ok.android,com.android.musicfx,ru.beeline.services,ru.sberbank.spasibo,com.android.contacts,com.google.android.gsf.login,com.android.providers.userdictionary,com.android.launcher3,com.google.android.apps.magazines,com.android.noisefield,com.estrongs.android.pop,com.android.providers.partnerbookmarks,com.android.soundrecorder,com.google.android.apps.plus,com.google.android.youtube,com.android.phasebeam,com.ijinshan.kbatterydoctor_en,com.android.simmelock,com.android.htmlviewer,com.google.android.music,com.piriform.ccleaner,ru.megafon.mlk,com.paragon.mts.ma.android,com.android.protips,com.mediatek.voicecommand,com.konka.desktimewidget,com.mediatek.systemupdate.sysoper,ru.yandex.money,com.android.settings,ru.sberbankmobile,com.android.shell,ru.yandex.searchplugin,com.android.providers.settings,ru.simpls.brs2.mobbank,com.google.android.gms,com.google.android.configupdater,com.navitel,com.mediatek.StkSelection,ru.mts.mymts,com.google.android.apps.cloudprint,com.android.externalstorage,com.google.android.tts,com.mediatek.engineermode,com.android.providers.telephony,com.android.vending,com.mediatek.voiceunlock,com.android.bluetooth,com.mediatek.thermalmanager,com.google.android.play.games,com.google.android.talk,com.ubanksu,com.google.android.inputmethod.latin,android,com.skype.raider,com.android.stk,com.google.android.calendar,com.android.certinstaller,com.google.android.backuptransport,com.android.vpndialogs,com.mediatek.bluetooth,com.megogo.application,com.google.android.gm,com.mediatek.batterywarning,com.google.android.setupwizard,com.spotoption.android.titantrade,com.ghisler.android.TotalCommander,com.android.providers.downloads.ui,com.android.mms,com.android.location.fused,com.android.providers.applications,com.opera.mini.native,com.google.android.street,ru.ntv.client,com.android.dreams.basic,com.spotoption.android.interactive_optiontarget",
    uat="m|phone|fly|android|4.4.2",
    income="1",
    model="Fly IQ4514 Quad",
    manufacturer="Fly",
    device_id="8e8afc6432a9953a",
)
testdata_dev_info_with_income.add_row(
    apps="ru.yandex.yandexmaps,ru.yandex.yandexnavi",
    uat="m|phone|samsung|android|5.0",
    income="1",
    model="Galaxy S5 Dual SIM",
    manufacturer="Samsung",
    device_id="8e982c3c803cc9f9",
)
testdata_dev_info_with_income.add_row(
    apps="ru.yandex.weatherplugin,ru.yandex.disk,ru.yandex.mail",
    uat="m|phone|samsung|android|4.1.2",
    income="1",
    model="Galaxy S3 Mini",
    manufacturer="Samsung",
    device_id="8e9f0f395fdded2a",
)
testdata_dev_info_with_income.add_row(
    apps="ru.yandex.metro,com.yandex.browser,ru.yandex.yandexmaps,com.drweb,ru.yandex.yandexnavi,ru.yandex.fines,ru.yandex.yandexcity",
    uat="m|phone|samsung|android|5.0.1",
    income="2",
    model="Galaxy S4",
    manufacturer="Samsung",
    device_id="8e829e09c4ef9d68",
)
testdata_dev_info_with_income.add_row(
    apps="ru.yandex.metro,ru.yandex.yandexnavi",
    uat="m|phone|huawei|android|5.1.1",
    income="2",
    model="Che2-L11",
    manufacturer="Huawei",
    device_id="8e85316bb84d9e48",
)
testdata_dev_info_with_income.add_row(
    apps="com.android.sharedstoragebackup,com.mediatek.lbs.em,com.vkontakte.android,ru.cian.main,com.google.android.apps.maps,com.google.android.partnersetup,com.mediatek.videoplayer,com.mediatek.schpwronoff,com.kms.free,ru.intaxi,com.google.android.onetimeinitializer,com.android.magicsmoke,com.android.packageinstaller,net.megogo.vendor,com.android.documentsui,com.google.android.googlequicksearchbox,com.android.providers.contacts,com.google.android.apps.books,com.rock.gota,com.android.wallpaper.livepicker,ru.mail,com.android.phone,com.android.backupconfirm,ru.yandex.weatherplugin,com.android.galaxy4,com.android.email,com.gismeteo.client,com.android.deskclock,com.android.videoeditor,com.yandex.browser,com.flyhelp.repair,com.android.defcontainer,com.android.providers.downloads,com.android.proxyhandler,com.android.musicvis,com.mediatek.videofavorites,com.opera.preinstall,PSB.Droid,com.mediatek.connectivity,com.android.exchange,com.mediatek.DataUsageLockScreenClient,com.android.pacprocessor,com.google.android.tag,com.android.dialer,com.cleanmaster.mguard,com.android.keychain,com.android.gallery3d,com.android.keyguard,com.google.android.marvin.talkback,com.mediatek,com.mediatek.FMRadio,com.android.systemui,com.google.android.videos,com.android.calculator2,com.android.wallpaper,com.android.inputdevices,com.google.android.apps.docs,ru.yandex.yandexmaps,com.android.wallpaper.holospiral,com.mediatek.CellConnService,com.umtgrn.pdfreader,com.android.providers.drm,com.google.android.syncadapters.contacts,com.google.android.feedback,com.android.facelock,com.mediatek.appguide.plugin,com.android.salestracking,ru.yandex.disk,com.mediatek.vlw,com.mediatek.systemupdate,com.android.browser.provider,com.mediatek.ygps,com.google.android.gsf,com.mediatek.calendarimporter,com.android.printspooler,com.mediatek.apst.target,com.android.providers.media,com.android.chrome,com.mediatek.mtklogger,com.android.providers.calendar,ru.ok.android,com.android.musicfx,ru.beeline.services,ru.sberbank.spasibo,com.android.contacts,com.google.android.gsf.login,com.android.providers.userdictionary,com.android.launcher3,com.google.android.apps.magazines,com.android.noisefield,com.estrongs.android.pop,com.android.providers.partnerbookmarks,com.android.soundrecorder,com.google.android.apps.plus,com.google.android.youtube,com.android.phasebeam,com.ijinshan.kbatterydoctor_en,com.android.simmelock,com.android.htmlviewer,com.google.android.music,com.piriform.ccleaner,ru.megafon.mlk,com.paragon.mts.ma.android,com.android.protips,com.mediatek.voicecommand,com.konka.desktimewidget,com.mediatek.systemupdate.sysoper,ru.yandex.money,com.android.settings,ru.sberbankmobile,com.android.shell,ru.yandex.searchplugin,com.android.providers.settings,ru.simpls.brs2.mobbank,com.google.android.gms,com.google.android.configupdater,com.navitel,com.mediatek.StkSelection,ru.mts.mymts,com.google.android.apps.cloudprint,com.android.externalstorage,com.google.android.tts,com.mediatek.engineermode,com.android.providers.telephony,com.android.vending,com.mediatek.voiceunlock,com.android.bluetooth,com.mediatek.thermalmanager,com.google.android.play.games,com.google.android.talk,com.ubanksu,com.google.android.inputmethod.latin,android,com.skype.raider,com.android.stk,com.google.android.calendar,com.android.certinstaller,com.google.android.backuptransport,com.android.vpndialogs,com.mediatek.bluetooth,com.megogo.application,com.google.android.gm,com.mediatek.batterywarning,com.google.android.setupwizard,com.spotoption.android.titantrade,com.ghisler.android.TotalCommander,com.android.providers.downloads.ui,com.android.mms,com.android.location.fused,com.android.providers.applications,com.opera.mini.native,com.google.android.street,ru.ntv.client,com.android.dreams.basic,com.spotoption.android.interactive_optiontarget",
    uat="m|phone|fly|android|4.4.2",
    income="1",
    model="Fly IQ4514 Quad",
    manufacturer="Fly",
    device_id="8e8afc6432a9953a",
)


testdata_bs_rtb_log = BaseLog(path="//home/logfeller/logs/bs-rtb-log/1d", date="2016-04-09", default_data=BS_RTB_LOG)
testdata_bs_rtb_log.add_row(
    showtime="1471598490",
    unixtime="1471598490",
    clid="0",
    clientip="213.87.154.184",
    queryargs="240=1&426=8739185&84=2&329=Android&337=21f44ec9-7f36-4a56-a529-88326db73750&431=480&432=320&439=3cb9bf00-3459-4a6b-b419-a6e19d009099&440=com%2EkathleenOswald%2EsolitaireGooglePlay&443=com%2EkathleenOswald%2EsolitaireGooglePlay&445=IAB25%0AIAB26%0AIAB7-39%0AIAB8-18%0AIAB8-5%0AIAB9-9&486=0%2E1",
    pageid="168628",
    regionid="213",
    clientip6="::ffff:213.87.154.184",
    uniqid="1143583185446278524",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500c:428d:5cff:fe34:fc79unknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:37:230757565",
    referer="https://dsp.yandex.ru/",
    useragent="Mozilla/5.0 (Linux; Android 4.4.2; Philips V387 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 12:21:30",
    remoteip6="::1",
    devicetype="3",
    cryptaid="1143583185446278524",
)
testdata_bs_rtb_log.add_row(
    showtime="1471598490",
    unixtime="1471598490",
    clid="0",
    clientip="213.87.154.184",
    queryargs="240=1&426=8739185&84=2&329=Android&337=21f44ec9-7f36-4a56-a529-88326db73750&431=480&432=320&439=3cb9bf00-3459-4a6b-b419-a6e19d009099&440=com%2EkathleenOswald%2EsolitaireGooglePlay&443=com%2EkathleenOswald%2EsolitaireGooglePlay&445=IAB25%0AIAB26%0AIAB7-39%0AIAB8-18%0AIAB8-5%0AIAB9-9&486=0%2E1",
    pageid="168628",
    regionid="213",
    clientip6="::ffff:213.87.154.184",
    uniqid="1143583185446278524",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500c:428d:5cff:fe34:fc79unknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:37:230757565",
    referer="https://dsp.yandex.ru/",
    useragent="Mozilla/5.0 (Linux; Android 4.4.2; Philips V387 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 12:21:30",
    remoteip6="::1",
    devicetype="3",
    cryptaid="1143583185446278524",
)
testdata_bs_rtb_log.add_row(
    showtime="1471598490",
    unixtime="1471598490",
    clid="0",
    clientip="213.87.154.184",
    queryargs="240=1&426=8739185&84=2&329=Android&337=21f44ec9-7f36-4a56-a529-88326db73750&431=480&432=320&439=3cb9bf00-3459-4a6b-b419-a6e19d009099&440=com%2EkathleenOswald%2EsolitaireGooglePlay&443=com%2EkathleenOswald%2EsolitaireGooglePlay&445=IAB25%0AIAB26%0AIAB7-39%0AIAB8-18%0AIAB8-5%0AIAB9-9&486=0%2E1",
    pageid="168628",
    regionid="213",
    clientip6="::ffff:213.87.154.184",
    uniqid="1143583185446278524",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500c:428d:5cff:fe34:fc79unknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:37:230757565",
    referer="https://dsp.yandex.ru/",
    useragent="Mozilla/5.0 (Linux; Android 4.4.2; Philips V387 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 12:21:30",
    remoteip6="::1",
    devicetype="3",
    cryptaid="1143583185446278524",
)
testdata_bs_rtb_log.add_row(
    showtime="1471598490",
    unixtime="1471598490",
    clid="0",
    clientip="213.87.154.184",
    queryargs="240=1&426=8739185&84=2&329=Android&337=21f44ec9-7f36-4a56-a529-88326db73750&431=480&432=320&439=3cb9bf00-3459-4a6b-b419-a6e19d009099&440=com%2EkathleenOswald%2EsolitaireGooglePlay&443=com%2EkathleenOswald%2EsolitaireGooglePlay&445=IAB25%0AIAB26%0AIAB7-39%0AIAB8-18%0AIAB8-5%0AIAB9-9&486=0%2E1",
    pageid="168628",
    regionid="213",
    clientip6="::ffff:213.87.154.184",
    uniqid="1143583185446278524",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500c:428d:5cff:fe34:fc79unknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:37:230757565:",
    referer="https://dsp.yandex.ru/",
    useragent="Mozilla/5.0 (Linux; Android 4.4.2; Philips V387 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 12:21:30",
    remoteip6="::1",
    devicetype="3",
    cryptaid="1143583185446278524",
)
testdata_bs_rtb_log.add_row(
    showtime="1471598490",
    unixtime="1471598490",
    clid="0",
    clientip="213.87.154.184",
    queryargs="240=1&426=8739185&84=2&329=Android&337=21f44ec9-7f36-4a56-a529-88326db73750&431=480&432=320&439=3cb9bf00-3459-4a6b-b419-a6e19d009099&440=1234567890&443=com%2EkathleenOswald%2EsolitaireGooglePlay&445=IAB25%0AIAB26%0AIAB7-39%0AIAB8-18%0AIAB8-5%0AIAB9-9&486=0%2E1",
    pageid="168628",
    regionid="213",
    clientip6="::ffff:213.87.154.184",
    uniqid="1143583185446278524",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500c:428d:5cff:fe34:fc79unknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:37:230757565:",
    referer="https://dsp.yandex.ru/",
    useragent="Mozilla/5.0 (Linux; Android 4.4.2; Philips V387 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 12:21:30",
    remoteip6="::1",
    devicetype="3",
    cryptaid="1143583185446278524",
)
testdata_bs_rtb_log.add_row(
    showtime="1471598490",
    unixtime="1471598490",
    clid="0",
    clientip="213.87.154.184",
    queryargs="240=1&426=8739185&84=2&329=Android&337=21f44ec9-7f36-4a56-a529-88326db73750&431=480&432=320&439=3cb9bf00-3459-4a6b-b419-a6e19d009099&440=http%3A%2F%2Fyandex.ru&443=com%2EkathleenOswald%2EsolitaireGooglePlay&445=IAB25%0AIAB26%0AIAB7-39%0AIAB8-18%0AIAB8-5%0AIAB9-9&486=0%2E1",
    pageid="168628",
    regionid="213",
    clientip6="::ffff:213.87.154.184",
    uniqid="1143583185446278524",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500c:428d:5cff:fe34:fc79unknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:37:230757565:",
    referer="https://dsp.yandex.ru/",
    useragent="Mozilla/5.0 (Linux; Android 4.4.2; Philips V387 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 12:21:30",
    remoteip6="::1",
    devicetype="3",
    cryptaid="1143583185446278524",
)
testdata_bs_rtb_log.add_row(
    showtime="1471603241",
    unixtime="1471603241",
    pagetoken="com.cleanmaster.mguard",
    clid="0",
    bidreqid="4811716334833897187",
    realip="95.153.131.141",
    clientip="95.153.131.141",
    queryargs="84=1&240=1&426=7952356&324=1&329=Android&337",
    pageid="154738",
    regionid="26",
    clientip6="::ffff:95.153.131.141",
    uniqid="2129028835457769567",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500f:428d:5cff:fe34:fc4bunknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:21:229037335:",
    referer="http://dsp.yandex.ru/",
    remoteip="127.0.0.1",
    useragent="Mozilla/5.0 (Linux; Android 5.0.1; GT-I9506 Build/LRX22C; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/51.0.2704.81 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 13:40:41",
    remoteip6="::ffff:127.0.0.1",
    realip6="::ffff:95.153.131.141",
    devicetype="3",
    cryptaid="2129028835457769567",
)

testdata_bs_rtb_log.add_row(
    _date="2016-04-10",
    showtime="1471598490",
    unixtime="1471598490",
    clid="0",
    clientip="213.87.154.184",
    queryargs="240=1&426=8739185&84=2&329=Android&337=21f44ec9-7f36-4a56-a529-88326db73750&431=480&432=320&439=3cb9bf00-3459-4a6b-b419-a6e19d009099&440=com%2EkathleenOswald%2EsolitaireGooglePlay&443=com%2EkathleenOswald%2EsolitaireGooglePlay&445=IAB25%0AIAB26%0AIAB7-39%0AIAB8-18%0AIAB8-5%0AIAB9-9&486=0%2E1",
    pageid="168628",
    regionid="213",
    clientip6="::ffff:213.87.154.184",
    uniqid="1143583185446278524",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500c:428d:5cff:fe34:fc79unknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:37:230757565",
    referer="https://dsp.yandex.ru/",
    useragent="Mozilla/5.0 (Linux; Android 4.4.2; Philips V387 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 12:21:30",
    remoteip6="::1",
    devicetype="3",
    cryptaid="1143583185446278524",
)
testdata_bs_rtb_log.add_row(
    _date="2016-04-10",
    showtime="1471598490",
    unixtime="1471598490",
    clid="0",
    clientip="213.87.154.184",
    queryargs="240=1&426=8739185&84=2&329=Android&337=21f44ec9-7f36-4a56-a529-88326db73750&431=480&432=320&439=3cb9bf00-3459-4a6b-b419-a6e19d009099&440=com%2EkathleenOswald%2EsolitaireGooglePlay&443=com%2EkathleenOswald%2EsolitaireGooglePlay&445=IAB25%0AIAB26%0AIAB7-39%0AIAB8-18%0AIAB8-5%0AIAB9-9&486=0%2E1",
    pageid="168628",
    regionid="213",
    clientip6="::ffff:213.87.154.184",
    uniqid="1143583185446278524",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500c:428d:5cff:fe34:fc79unknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:37:230757565",
    referer="https://dsp.yandex.ru/",
    useragent="Mozilla/5.0 (Linux; Android 4.4.2; Philips V387 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 12:21:30",
    remoteip6="::1",
    devicetype="3",
    cryptaid="1143583185446278524",
)
testdata_bs_rtb_log.add_row(
    _date="2016-04-10",
    showtime="1471598490",
    unixtime="1471598490",
    clid="0",
    clientip="213.87.154.184",
    queryargs="240=1&426=8739185&84=2&329=Android&337=21f44ec9-7f36-4a56-a529-88326db73750&431=480&432=320&439=3cb9bf00-3459-4a6b-b419-a6e19d009099&440=com%2EkathleenOswald%2EsolitaireGooglePlay&443=com%2EkathleenOswald%2EsolitaireGooglePlay&445=IAB25%0AIAB26%0AIAB7-39%0AIAB8-18%0AIAB8-5%0AIAB9-9&486=0%2E1",
    pageid="168628",
    regionid="213",
    clientip6="::ffff:213.87.154.184",
    uniqid="1143583185446278524",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500c:428d:5cff:fe34:fc79unknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:37:230757565",
    referer="https://dsp.yandex.ru/",
    useragent="Mozilla/5.0 (Linux; Android 4.4.2; Philips V387 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 12:21:30",
    remoteip6="::1",
    devicetype="3",
    cryptaid="1143583185446278524",
)
testdata_bs_rtb_log.add_row(
    _date="2016-04-10",
    showtime="1471598490",
    unixtime="1471598490",
    clid="0",
    clientip="213.87.154.184",
    queryargs="240=1&426=8739185&84=2&329=Android&337=21f44ec9-7f36-4a56-a529-88326db73750&431=480&432=320&439=3cb9bf00-3459-4a6b-b419-a6e19d009099&440=com%2EkathleenOswald%2EsolitaireGooglePlay&443=com%2EkathleenOswald%2EsolitaireGooglePlay&445=IAB25%0AIAB26%0AIAB7-39%0AIAB8-18%0AIAB8-5%0AIAB9-9&486=0%2E1",
    pageid="168628",
    regionid="213",
    clientip6="::ffff:213.87.154.184",
    uniqid="1143583185446278524",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500c:428d:5cff:fe34:fc79unknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:37:230757565:",
    referer="https://dsp.yandex.ru/",
    useragent="Mozilla/5.0 (Linux; Android 4.4.2; Philips V387 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 12:21:30",
    remoteip6="::1",
    devicetype="3",
    cryptaid="1143583185446278524",
)
testdata_bs_rtb_log.add_row(
    _date="2016-04-10",
    showtime="1471603241",
    unixtime="1471603241",
    pagetoken="com.cleanmaster.mguard",
    clid="0",
    bidreqid="4811716334833897187",
    realip="95.153.131.141",
    clientip="95.153.131.141",
    queryargs="84=1&240=1&426=7952356&324=1&329=Android&337",
    pageid="154738",
    regionid="26",
    clientip6="::ffff:95.153.131.141",
    uniqid="2129028835457769567",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500f:428d:5cff:fe34:fc4bunknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:21:229037335:",
    referer="http://dsp.yandex.ru/",
    remoteip="127.0.0.1",
    useragent="Mozilla/5.0 (Linux; Android 5.0.1; GT-I9506 Build/LRX22C; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/51.0.2704.81 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 13:40:41",
    remoteip6="::ffff:127.0.0.1",
    realip6="::ffff:95.153.131.141",
    devicetype="3",
    cryptaid="2129028835457769567",
)

testdata_bs_rtb_log.add_row(
    _date="2016-04-11",
    showtime="1471598490",
    unixtime="1471598490",
    clid="0",
    clientip="213.87.154.184",
    queryargs="240=1&426=8739185&84=2&329=Android&337=21f44ec9-7f36-4a56-a529-88326db73750&431=480&432=320&439=3cb9bf00-3459-4a6b-b419-a6e19d009099&440=com%2EkathleenOswald%2EsolitaireGooglePlay&443=com%2EkathleenOswald%2EsolitaireGooglePlay&445=IAB25%0AIAB26%0AIAB7-39%0AIAB8-18%0AIAB8-5%0AIAB9-9&486=0%2E1",
    pageid="168628",
    regionid="213",
    clientip6="::ffff:213.87.154.184",
    uniqid="1143583185446278524",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500c:428d:5cff:fe34:fc79unknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:37:230757565",
    referer="https://dsp.yandex.ru/",
    useragent="Mozilla/5.0 (Linux; Android 4.4.2; Philips V387 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 12:21:30",
    remoteip6="::1",
    devicetype="3",
    cryptaid="1143583185446278524",
)
testdata_bs_rtb_log.add_row(
    _date="2016-04-11",
    showtime="1471598490",
    unixtime="1471598490",
    clid="0",
    clientip="213.87.154.184",
    queryargs="240=1&426=8739185&84=2&329=Android&337=21f44ec9-7f36-4a56-a529-88326db73750&431=480&432=320&439=3cb9bf00-3459-4a6b-b419-a6e19d009099&440=com%2EkathleenOswald%2EsolitaireGooglePlay&443=com%2EkathleenOswald%2EsolitaireGooglePlay&445=IAB25%0AIAB26%0AIAB7-39%0AIAB8-18%0AIAB8-5%0AIAB9-9&486=0%2E1",
    pageid="168628",
    regionid="213",
    clientip6="::ffff:213.87.154.184",
    uniqid="1143583185446278524",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500c:428d:5cff:fe34:fc79unknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:37:230757565",
    referer="https://dsp.yandex.ru/",
    useragent="Mozilla/5.0 (Linux; Android 4.4.2; Philips V387 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 12:21:30",
    remoteip6="::1",
    devicetype="3",
    cryptaid="1143583185446278524",
)
testdata_bs_rtb_log.add_row(
    _date="2016-04-11",
    showtime="1471598490",
    unixtime="1471598490",
    clid="0",
    clientip="213.87.154.184",
    queryargs="240=1&426=8739185&84=2&329=Android&337=21f44ec9-7f36-4a56-a529-88326db73750&431=480&432=320&439=3cb9bf00-3459-4a6b-b419-a6e19d009099&440=com%2EkathleenOswald%2EsolitaireGooglePlay&443=com%2EkathleenOswald%2EsolitaireGooglePlay&445=IAB25%0AIAB26%0AIAB7-39%0AIAB8-18%0AIAB8-5%0AIAB9-9&486=0%2E1",
    pageid="168628",
    regionid="213",
    clientip6="::ffff:213.87.154.184",
    uniqid="1143583185446278524",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500c:428d:5cff:fe34:fc79unknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:37:230757565",
    referer="https://dsp.yandex.ru/",
    useragent="Mozilla/5.0 (Linux; Android 4.4.2; Philips V387 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 12:21:30",
    remoteip6="::1",
    devicetype="3",
    cryptaid="1143583185446278524",
)
testdata_bs_rtb_log.add_row(
    _date="2016-04-11",
    showtime="1471598490",
    unixtime="1471598490",
    clid="0",
    clientip="213.87.154.184",
    queryargs="240=1&426=8739185&84=2&329=Android&337=21f44ec9-7f36-4a56-a529-88326db73750&431=480&432=320&439=3cb9bf00-3459-4a6b-b419-a6e19d009099&440=com%2EkathleenOswald%2EsolitaireGooglePlay&443=com%2EkathleenOswald%2EsolitaireGooglePlay&445=IAB25%0AIAB26%0AIAB7-39%0AIAB8-18%0AIAB8-5%0AIAB9-9&486=0%2E1",
    pageid="168628",
    regionid="213",
    clientip6="::ffff:213.87.154.184",
    uniqid="1143583185446278524",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500c:428d:5cff:fe34:fc79unknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:37:230757565:",
    referer="https://dsp.yandex.ru/",
    useragent="Mozilla/5.0 (Linux; Android 4.4.2; Philips V387 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 12:21:30",
    remoteip6="::1",
    devicetype="3",
    cryptaid="1143583185446278524",
)
testdata_bs_rtb_log.add_row(
    _date="2016-04-11",
    showtime="1471603241",
    unixtime="1471603241",
    pagetoken="com.cleanmaster.mguard",
    clid="0",
    bidreqid="4811716334833897187",
    realip="95.153.131.141",
    clientip="95.153.131.141",
    queryargs="84=1&240=1&426=7952356&324=1&329=Android&337",
    pageid="154738",
    regionid="26",
    clientip6="::ffff:95.153.131.141",
    uniqid="2129028835457769567",
    fraudbits="0",
    source_uri="prt://yabs-rt@2a02:6b8:b020:500f:428d:5cff:fe34:fc4bunknown",
    _stbx="rt3.man--yabs-rt--bs-rtb-log:21:229037335:",
    referer="http://dsp.yandex.ru/",
    remoteip="127.0.0.1",
    useragent="Mozilla/5.0 (Linux; Android 5.0.1; GT-I9506 Build/LRX22C; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/51.0.2704.81 Mobile Safari/537.36",
    iso_eventtime="2016-08-19 13:40:41",
    remoteip6="::ffff:127.0.0.1",
    realip6="::ffff:95.153.131.141",
    devicetype="3",
    cryptaid="2129028835457769567",
)

testdata_bs_rtb_log.add_row(
    _date="2016-04-11",
    uniqid="115555551400000000",
    queryargs="245=11111111111111111111111111111110&726=0123456789abcdefABCDEF9876543210",
    pageid="280196",
)

testdata_bs_rtb_log.add_row(
    _date="2016-04-11",
    uniqid="115555551400000001",
    queryargs="245=22222222222222222222222222222220&727=avito_hash1",
    pageid="280196",
)

testdata_bs_rtb_log.add_row(
    _date="2016-04-11",
    uniqid="115555551400000002",
    queryargs="245=33333333333333333333333333333330&727=0123456789abcdefABCDEF9876543211",
    pageid="280196",
)

testdata_bs_rtb_log.add_row(
    _date="2016-04-11", uniqid="10113701463508405", queryargs="727=0123456789abcdefABCDEF9876543211", pageid="280196"
)

testdata_bs_rtb_log.add_row(
    _date="2016-04-11", uniqid="115555551400000004", queryargs="727=avito_hash4", pageid="not_avito_pageid"
)

testdata_sbapi_mitb1 = BaseLog(
    path="//home/logfeller/logs/sbapi-access-mitb-log/1d", date="2016-04-09", default_data=SBAPI_MITB_LOG
)

testdata_sbapi_mitb1.add_row(
    data=(
        "{\\x22ui\\x22:\\x22{9E23CB5D-E100-4A11-9819-0B2189D9C7A5}\\x22,"
        "\\x22yandexuids\\x22:["
        "{\\x22value\\x22:\\x221411463541466283440\\x22,\\x22browser\\x22:\\x22Firefox\\x22,\\x22id\\x22:\\x220\\x22},"
        "{\\x22value\\x22:\\x221955639651466284017\\x22,\\x22browser\\x22:\\x22Google "
        "Chrome\\x22,\\x22id\\x22:\\x221\\x22},"
        "{\\x22value\\x22:\\x225929486401473797289\\x22,\\x22browser\\x22:\\x22Google "
        "Chrome\\x22,\\x22id\\x22:\\x221\\x22},"
        "{\\x22value\\x22:\\x223359835671473797290\\x22,\\x22browser\\x22:\\x22Google "
        "Chrome\\x22,\\x22id\\x22:\\x221\\x22},"
        "{\\x22value\\x22:\\x221955639651466284017\\x22,\\x22browser\\x22:\\x22Google "
        "Chrome\\x22,\\x22id\\x22:\\x221\\x22},"
        "{\\x22value\\x22:\\x229892519401469296486\\x22,\\x22browser\\x22:\\x22Opera\\x22,\\x22id\\x22:\\x223\\x22},"
        "{\\x22value\\x22:\\x22111666501466278216\\x22,\\x22browser\\x22:\\x22Opera\\x22,\\x22id\\x22:\\x223\\x22},"
        "{\\x22value\\x22:\\x22111666501466278216\\x22,\\x22browser\\x22:\\x22Opera\\x22,\\x22id\\x22:\\x223\\x22}"
        "]}"
    )
)

testdata_sbapi_mitb1.add_row(
    data="{\\x22ui\\x22:\\x22{9E23CB5D-E100-4A11-9819-0B2189D9C7A5}\\x22,"
    "\\x22yandexuids\\x22:["
    "{\\x22value\\x22:\\x223332342342342\\x22,\\x22browser\\x22:\\x22Firefox\\x22,\\x22id\\x22:\\x220\\x22}"
    "]}"
)

testdata_sbapi_mitb2 = BaseLog(
    path="//home/logfeller/logs/sbapi-access-mitb-log/1d", date="2016-04-10", default_data=SBAPI_MITB_LOG
)

# need second day, because one-days are private mode and thrown
testdata_sbapi_mitb2.add_row(
    data="{\\x22ui\\x22:\\x22{9E23CB5D-E100-4A11-9819-0B2189D9C7A5}\\x22,"
    "\\x22yandexuids\\x22:["
    "{\\x22value\\x22:\\x221411463541466283440\\x22,\\x22browser\\x22:\\x22Firefox\\x22,\\x22id\\x22:\\x220\\x22}"
    "]}"
)
testdata_sbapi_mitb2.add_row(_date="2016-04-11")


testdata_audimvideo_emails = BaseLog(
    path="//crypta/production/state/extras/reference-bases",
    table_name="audi-mvideo-emails",
    default_data=AUDIMVIDEO_EMAILS_LOG,
)
testdata_audimvideo_emails.add_row(id_value="sfdf@yandex.ru")
testdata_audimvideo_emails.add_row(id_value="0.klass@mail.ru")
testdata_audimvideo_emails.add_row(id_value="000111@nemdom.mangosip.ru")
testdata_audimvideo_emails.add_row(id_value="testaudimvideo@yandex.ru")
testdata_audimvideo_emails.add_row(id_value="0012337766@mail.ru")

testdata_sber_phones = BaseLog(
    path="//crypta/production/state/extras/reference-bases",
    table_name="sberbank_phones_hash",
    default_data=SBER_PHONES_LOG,
)
testdata_sber_phones.add_row(id_value="e2051594d8511d3a4e54a5c5230eefbe", id_prefix="7952")
testdata_sber_phones.add_row(id_value="cb7f477630ccfdfd58c6bbcf25aca7c5", id_prefix="7963")
testdata_sber_phones.add_row(id_value="baa3934f4c3dddb4f9a9a26f1510f08a", id_prefix="7999")
testdata_sber_phones.add_row(id_value="b68ad6b5b1864a75214d491488fd4e6b", id_prefix="7983")
testdata_sber_phones.add_row(id_value="001574c912c2a3f93dd5d4d4a73ddbf5", id_prefix="7910")

# sovetnik links
testdata_sovetnik_2016_04_11 = BaseLog(path="//logs/sovetnik-users-log/1d", date="2016-04-11", default_data={})
testdata_sovetnik_2016_04_11.add_row(
    **{
        "clid": "2290154",
        "browser": "Yandex",
        "lang": "ru",
        "host": "sovetnik02h",
        "os": "Windows",
        "geo_id": "10309",
        "region": "RU",
        "type": "SOCIAL_NETWORK_PROFILE",
        "yandexuid": "157564101446625111",
        "transaction_id": "j8ch9d38wf5m9mgr88eqfaejlj0h6tdf",
        "source_uri": "prt://sovetnik-backend@sovetnik02h.market.yandex.net/var/log/yandex/sovetnik-backend/sovetnik-users.log",
        "ip": "185.66.252.103",
        "subkey": "",
        "date": "2017-04-11",
        "_logfeller_index_bucket": "//home/logfeller/index/sovetnik-backend/sovetnik-users-log/1800-1800/1507087500/1507087800",
        "tskv_format": "sovetnik-users-log",
        "id": "100014250271294",
        "script_version": "201710021925",
        "install_id": "undefined",
        "iso_eventtime": "2017-04-11 06:32:57",
        "is_mobile": "0",
        "timestamp": "1507087977",
        "client_id": "5bc759d05-e5d3-4548-ae5f-e1bf47df02b5",
        "useragent": "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 YaBrowser/17.9.0.2081 Yowser/2.5 Safari/537.36",
        "domain": "www.facebook.com",
        "unixtime": "1507087977",
        "locale": "ru",
        "_logfeller_timestamp": 1507087977,
        "_stbx": "rt3.myt--sovetnik-backend--sovetnik-users-log:0@@92200@@base64:J_9H-GYHYWQVWThOIqDePQ@@1507087977967@@1507087978@@sovetnik-users-log@@582299189",
        "aff_id": "1004",
        "type_sn": "FB",
        "url": "https://www.facebook.com/",
        "yandex_login": "shackirov.zhanybai",
    }
)
testdata_sovetnik_2016_04_11.add_row(
    **{
        "clid": "2236989",
        "browser": "Yandex",
        "lang": "ru",
        "host": "sovetnik02h",
        "os": "Windows",
        "geo_id": "213",
        "region": "RU",
        "type": "SOCIAL_NETWORK_PROFILE",
        "yandexuid": "157564101446625111",
        "transaction_id": "j8ch9pxwk2pkdzqopof4rzy9f8wllawg",
        "source_uri": "prt://sovetnik-backend@sovetnik02h.market.yandex.net/var/log/yandex/sovetnik-backend/sovetnik-users.log",
        "ip": "85.113.214.240",
        "subkey": "",
        "date": "2017-04-10",
        "_logfeller_index_bucket": "//home/logfeller/index/sovetnik-backend/sovetnik-users-log/1800-1800/1507087500/1507087800",
        "tskv_format": "sovetnik-users-log",
        "id": "528763796480",
        "script_version": "201710031623",
        "install_id": "undefined",
        "iso_eventtime": "2016-04-09 06:32:57",
        "is_mobile": "0",
        "timestamp": "1507087977",
        "client_id": "53e090abd-9edc-4ae2-8a28-287d083091ae",
        "useragent": "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 YaBrowser/17.9.0.2081 Yowser/2.5 Safari/537.36",
        "domain": "ok.ru",
        "unixtime": "1507087977",
        "locale": "ru",
        "_logfeller_timestamp": 1507087977,
        "_stbx": "rt3.myt--sovetnik-backend--sovetnik-users-log:0@@92200@@base64:J_9H-GYHYWQVWThOIqDePQ@@1507087977967@@1507087978@@sovetnik-users-log@@582299189",
        "aff_id": "1004",
        "type_sn": "OK",
        "url": "https://ok.ru/",
        "yandex_login": "abushova1683",
    }
)
testdata_sovetnik_2016_04_11.add_row(
    **{
        "clid": "2290154",
        "browser": "Yandex",
        "lang": "ru",
        "host": "sovetnik02h",
        "os": "Windows",
        "geo_id": "10309",
        "region": "RU",
        "type": "SOCIAL_NETWORK_PROFILE",
        "yandexuid": "157564101446625111",
        "transaction_id": "j8ch9d38wf5m9mgr88eqfaejlj0h6tdf",
        "source_uri": "prt://sovetnik-backend@sovetnik02h.market.yandex.net/var/log/yandex/sovetnik-backend/sovetnik-users.log",
        "ip": "185.66.252.103",
        "subkey": "",
        "date": "2017-04-10",
        "_logfeller_index_bucket": "//home/logfeller/index/sovetnik-backend/sovetnik-users-log/1800-1800/1507087500/1507087800",
        "tskv_format": "sovetnik-users-log",
        "id": "100014250271294",
        "script_version": "201710021925",
        "install_id": "undefined",
        "iso_eventtime": "2017-04-10 06:32:57",
        "is_mobile": "0",
        "timestamp": "1507087977",
        "client_id": "5bc759d05-e5d3-4548-ae5f-e1bf47df02b5",
        "useragent": "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.113 YaBrowser/17.9.0.2081 Yowser/2.5 Safari/537.36",
        "domain": "www.facebook.com",
        "unixtime": "1507087977",
        "locale": "ru",
        "_logfeller_timestamp": 1507087977,
        "_stbx": "rt3.myt--sovetnik-backend--sovetnik-users-log:0@@92200@@base64:J_9H-GYHYWQVWThOIqDePQ@@1507087977967@@1507087978@@sovetnik-users-log@@582299189",
        "aff_id": "1004",
        "type_sn": "FB",
        "url": "https://www.facebook.com/",
        "yandex_login": "shackirov.zhanybai",
    }
)


testdata_lal_manager_data_to_classify = ComplexParametersLog(
    path="//crypta/production/lal_manager", table_name="data_to_classify", default_data=LAL_MANAGER_DATA_TO_CLASSIFY
)
testdata_lal_manager_data_to_classify.add_row(
    key="7065602181459497865",
    subkey="1459713932",
    value={
        u"mm_hits": u"1459497872:1399806976,1459497881:1388788224,1459497883:1388788224,1459497884:1388788224,1459497888:1388788224,1459497899:1388788224,1459497903:1399806976,1459497924:2982144,1459498085:2071493120,1459517002:1399806976,1459517012:1388788224,1459517013:1388788224,1459517014:1388788224,1459517022:1399806976,1459517065:1399806976,1459517072:1955230987,1459517123:1399806976,1459517134:1399806976,1459517151:1261654016,1459517157:742964226,1459517158:742964226,1459517178:1399806976,1459517183:1388788224,1459574033:1399806976,1459574049:2982144,1459574126:2982144,1459574843:2982144,1459576484:1399806976,1459576491:1399806976,1459576494:2074095105,1459576499:1374877440,1459576506:1374877440,1459576524:1374877440,1459576542:1374877440,1459576566:1374877440,1459577195:1374877440,1459577366:1374877440,1459577432:1374877440,1459577448:1374877440,1459577535:1374877440,1459577625:1374877440,1459577665:1374877440,1459578050:1374877440,1459578061:1374877440,1459578083:1374877440,1459578113:1374877440,1459578162:1374877440,1459578173:1374877440,1459578279:1374877440,1459578341:1374877440,1459578482:1374877440,1459578520:1374877440,1459578539:1374877440,1459578629:1374877440,1459578797:1559706880,1459578799:1783405313,1459578799:1399806976,1459579001:1374877440,1459579329:1374877440,1459579741:2982144,1459580093:1399806976,1459580098:1399806976,1459589458:1399806976,1459589492:1399806976,1459589495:1673216000,1459589497:1673216000,1459589504:715459072,1459589505:1208906240,1459589513:715459072,1459589515:2036538625,1459589534:715459072,1459589537:1858658048,1459589581:1858658048,1459589603:715459072,1459589616:715459072,1459589622:257603840,1459589642:2074095105,1459589644:1673216000,1459589652:21743873,1459589674:1497271808,1459589738:945459456,1459589781:1399806976,1459589790:863763200,1459589791:863763200,1459589797:863763200,1459590059:863763200,1459590176:863763200,1459590185:863763200,1459590254:863763200,1459590255:863763200,1459590275:863763200,1459591362:863763200,1459591380:863763200,1459591381:863763200,1459591383:863763200,1459591388:863763200,1459591392:863763200,1459591709:863763200,1459592567:1399806976,1459592600:1399806976,1459592605:2071493120,1459610949:1399806976,1459610968:1399806976,1459610993:1737924864,1459611024:1253575174,1459611025:1253575174,1459611041:1253575174,1459611497:1399806976,1459611515:1399806976,1459611594:1399806976,1459611606:1399806976,1459613084:1399806976,1459613101:1399806976,1459613112:1029721344,1459613157:1399806976,1459613182:1399806976,1459613189:1029721344,1459613289:1399806976,1459613309:1399806976,1459613312:1029721344,1459613404:1399806976,1459613426:1399806976,1459613431:1029721344,1459613554:1399806976,1459613565:1399806976,1459613568:1029721344,1459613648:1399806976,1459613691:1399806976,1459613695:1029721344,1459613775:1399806976,1459613796:1399806976,1459613799:1029721344,1459614050:1399806976,1459614069:1399806976,1459614074:1029721344,1459614445:1399806976,1459614461:1399806976,1459614466:1029721344,1459614479:1399806976,1459614900:1399806976,1459614904:1029721344,1459616519:1399806976,1459616574:1399806976,1459616579:1029721344,1459616604:1029721344,1459617167:1399806976,1459617184:1399806976,1459617191:1029721344,1459617626:1399806976,1459617672:1399806976,1459617675:1029721344,1459617905:1399806976,1459617923:1399806976,1459617959:1615831296,1459617988:1399806976,1459618216:1399806976,1459618221:1029721344,1459618577:1399806976,1459618592:1399806976,1459618617:894761216,1459618976:1399806976,1459619017:1399806976,1459619020:1029721344,1459619068:1029721344,1459619074:1772240128,1459619075:1772240128,1459619251:1399806976,1459619267:1399806976,1459619271:1029721344,1459619445:1399806976,1459619474:1399806976,1459619477:1029721344,1459620075:1399806976,1459620098:1399806976,1459620132:1399806976,1459620141:1772240128,1459620142:1772240128,1459620159:1029721344,1459620486:1399806976,1459620495:1029721344,1459620715:1399806976,1459620728:1029721344,1459620991:1399806976,1459620998:1029721344,1459621022:1943742219,1459621031:1134726656,1459621043:742964226,1459621053:1772240128,1459621054:1772240128,1459621801:1399806976,1459621836:2071493120,1459623364:556280320,1459624801:556280320,1459625009:556280320,1459626301:556280320,1459627901:556280320,1459627937:556280320,1459627973:556280320,1459686503:1399806976,1459686535:2071493120,1459688583:556280320,1459688817:556280320,1459689195:556280320,1459690117:556280320,1459691772:556280320,1459691850:556280320,1459693052:556280320,1459693807:556280320,1459694924:556280320,1459695241:556280320,1459696239:556280320,1459697133:556280320,1459699180:556280320,1459699943:556280320,1459700859:556280320,1459704735:556280320,1459705325:556280320,1459706076:556280320,1459706815:556280320,1459707542:556280320,1459708339:556280320,1459709179:556280320,1459709220:556280320,1459710033:556280320,1459710701:556280320,1459711724:556280320,1459713887:1399806976,1459713932:1399806976",
        u"words_count": u"124911360:1,197569792:1,254880512:7,492839936:1,626206464:1,662573824:2,692308480:1,712331520:4,800386816:1,812059648:1,859007232:1,935713792:1,954380544:1,964097536:1,970236160:1,971957248:11,992103936:1,1021715456:1,1072414464:1,1138601728:1,1225300224:2,1362326528:1,1417041920:1,1502180887:1,1581753600:1,1669560064:1,1681566208:1,1779099904:2,1885646336:1,1907852544:1,1920489729:1,2027452672:1,2056663296:18,2066058496:1,2069424640:1,2082022912:1,-2105498880:1,-1954635520:2,-1908080128:1,-1874979072:1,-1803806976:1,-1782597120:2,-1669634048:1,-1647186944:1,-1576203776:1,-1570764800:24,-1529356800:2,-1523656960:1,-1479660800:7,-1433694208:1,-1426960896:1,-1339737856:1,-1317875456:6,-1295378176:1,-1214572032:6,-1198372352:1,-1157633792:1,-1136577024:1,-1124377600:1,-1115573760:1,-897837056:26,-828156928:7,-798379520:1,-677894656:1,-667539968:1,-652350464:1,-630333184:1,-561821952:1,-381622528:1,-376268544:1,-329383680:1,-321946880:1,-309923328:1,-306145792:1,-261243889:1,-163024896:1,-157404928:1,-150570240:1,-150393856:2,-107410432:1,-105772288:1,-75572224:1,-23841024:1",
        u"mm_count": u"742964226:3,1737924864:1,1858658048:2,2071493120:4,1374877440:26,863763200:16,1559706880:1,1388788224:9,1253575174:3,894761216:1,1261654016:1,1673216000:3,1955230987:1,2036538625:1,1943742219:1,1615831296:1,21743873:1,556280320:33,257603840:1,945459456:1,715459072:5,2982144:5,1399806976:73,2074095105:2,1772240128:6,1783405313:1,1497271808:1,1208906240:1,1134726656:1,1029721344:23",
        u"offset": u"4",
    },
)
testdata_lal_manager_data_to_classify.add_row(
    key="5157564101446629164",
    subkey="1460299737",
    value={
        u"cyr_words_num": u"0;0;0;0",
        u"latlon": u"53183188;44997376",
        u"referers": u"1459188939:1751672064,1459607333:1751672064,1459628369:1751672064,1459628370:1751672064,1460137195:1751672064,1460137486:1751672064,1460137487:1751672064,1460137516:1751672064,1460281355:1751672064,1460281560:1751672064,1460281561:1751672064,1460281565:1877719809,1460281590:1751672064,1460281870:1751672064,1460281871:1751672064,1460299025:1751672064,1460299047:1751672064,1460299130:1751672064,1460299148:1751672064,1460299149:1751672064,1460299306:1751672064,1460299307:1751672064,1460299737:1751672064",
        u"refmirrors": u"1459188939:1751672064,1459607333:1751672064,1459628369:1751672064,1459628370:1751672064,1460137195:1751672064,1460137486:1751672064,1460137487:1751672064,1460137516:1751672064,1460281355:1751672064,1460281560:1751672064,1460281561:1751672064,1460281565:1877719809,1460281590:1751672064,1460281870:1751672064,1460281871:1751672064,1460299025:1751672064,1460299047:1751672064,1460299130:1751672064,1460299148:1751672064,1460299149:1751672064,1460299306:1751672064,1460299307:1751672064,1460299737:1751672064",
        u"words_num": u"0;0;0;0",
        u"m_count": u"1847937024:1,1877719809:3,2113601792:2,258700800:2,559688704:2,1168242944:1,1184225537:1,151918080:1,40298240:4,489716992:1,264723458:1,952305921:1,1207535619:1,718161408:1,1540143104:3,1876992000:2",
        u"offset": u"4",
        u"m_agentId": u"8",
        u"mm_count": u"1877719809:3,1216877830:1,258700800:2,559688704:2,1168242944:1,1184225537:1,151918080:1,40298240:4,489716992:1,1783405313:2,264723458:1,952305921:1,1207535619:1,718161408:1,1540143104:3,1876992000:2",
        u"mm_hits": u"1459188939:1207535619,1459607333:40298240,1459607333:1783405313,1459607333:40298240,1459628369:40298240,1459628369:1783405313,1459628370:40298240,1460137195:1184225537,1460137486:559688704,1460137487:559688704,1460137516:151918080,1460281355:952305921,1460281560:1877719809,1460281561:1877719809,1460281565:1877719809,1460281590:718161408,1460281870:1540143104,1460281871:1540143104,1460299025:1540143104,1460299047:264723458,1460299130:1168242944,1460299148:258700800,1460299149:258700800,1460299306:1876992000,1460299307:1876992000,1460299737:1216877830,1460299737:489716992",
        u"m_interest_time_clickstream": u"1459607333:9000090;1459607333:9000090;1459628369:9000090;1459628370:9000090;1460137486:9000654;1460137487:9000654;1460137516:9000018;1460281560:9000080,9000476,9000007;1460281561:9000080,9000476,9000007;1460281565:9000080,9000476,9000007;1460281590:9000105;1460281870:9002873,9000080,9000007;1460281871:9002873,9000080,9000007;1460299025:9002873,9000080,9000007;1460299148:9000096,9000635;1460299149:9000096,9000635;1460299306:9000096,9000635;1460299307:9000096,9000635;1460299737:9000118",
        u"d_count": u"0:2,1:2,11:23",
        u"cyr_word_len": u"0;0;0;0",
        u"rids": u"49:27",
    },
)
testdata_lal_manager_data_to_classify.add_row(
    key="2286815551454655817",
    subkey="1459766606",
    value={
        u"cyr_words_num": u"2;2;2;0",
        u"latlon": u"51471920;81694598",
        u"referers": u"1457246541:139536641,1457246547:139536641,1457246576:1994241792,1457246799:242914048,1457246800:1979094272,1457246806:1979094272,1457246829:1994241792,1457246842:1994241792,1457246860:1994241792,1457295557:1980929035,1458286195:1368441088,1458286210:1368441088,1458634446:1980929035,1458691506:371277318,1458713391:1913996032,1458713396:1913996032,1458713442:1913996032,1458713472:1913996032,1458713518:1913996032,1458713527:1913996032,1458713563:1913996032,1458713604:1913996032,1459379043:1913996032,1459379058:1913996032,1459379059:1913996032,1459379067:1913996032,1459379068:1913996032,1459379162:1913996032,1459379170:1913996032,1459379182:1913996032,1459379197:1913996032,1459379216:1913996032,1459379237:1913996032,1459379257:1913996032,1459379276:1913996032",
        u"refmirrors": u"1457246541:139536641,1457246547:139536641,1457246576:1994241792,1457246799:242914048,1457246800:242914048,1457246806:242914048,1457246829:1994241792,1457246842:1994241792,1457246860:1994241792,1457295557:1980929035,1458286195:1368441088,1458286210:1368441088,1458634446:1980929035,1458691506:371277318,1458713391:1913996032,1458713396:1913996032,1458713442:1913996032,1458713472:1913996032,1458713518:1913996032,1458713527:1913996032,1458713563:1913996032,1458713604:1913996032,1459379043:1913996032,1459379058:1913996032,1459379059:1913996032,1459379067:1913996032,1459379068:1913996032,1459379162:1913996032,1459379170:1913996032,1459379182:1913996032,1459379197:1913996032,1459379216:1913996032,1459379237:1913996032,1459379257:1913996032,1459379276:1913996032",
        u"words_num": u"3;3;3;0",
        u"m_count": u"1979094272:1,1902989829:1,371277318:3,1021476098:1,921915904:4,280836096:1,1368441088:6,242914048:2,139536641:1,1994241792:9,2037344768:1,1913996032:26",
        u"offset": u"11",
        u"m_agentId": u"8",
        u"mm_count": u"1902989829:1,371277318:3,1021476098:1,921915904:4,280836096:1,1368441088:6,242914048:3,139536641:1,1994241792:9,2037344768:1,1913996032:26",
        u"mm_hits": u"1456881330:1994241792,1457246539:139536641,1457246541:1994241792,1457246547:1994241792,1457246576:1994241792,1457246778:242914048,1457246782:242914048,1457246799:242914048,1457246800:1994241792,1457246806:1994241792,1457246829:1994241792,1457246842:1994241792,1457246860:1994241792,1457295557:280836096,1458122244:921915904,1458122245:921915904,1458222897:1902989829,1458222961:921915904,1458222962:921915904,1458286166:1368441088,1458286195:1368441088,1458286210:1368441088,1458286267:1368441088,1458286376:1368441088,1458286451:1368441088,1458634446:1021476098,1458691505:371277318,1458691506:371277318,1458691506:371277318,1458713380:1913996032,1458713391:1913996032,1458713396:1913996032,1458713442:1913996032,1458713472:1913996032,1458713518:1913996032,1458713527:1913996032,1458713563:1913996032,1458713604:1913996032,1459379038:1913996032,1459379043:1913996032,1459379058:1913996032,1459379058:1913996032,1459379059:1913996032,1459379063:1913996032,1459379067:1913996032,1459379068:1913996032,1459379080:1913996032,1459379162:1913996032,1459379170:1913996032,1459379182:1913996032,1459379197:1913996032,1459379216:1913996032,1459379237:1913996032,1459379257:1913996032,1459379276:1913996032,1459766606:2037344768",
        u"catalogue": u"1457246796:3665,90",
        u"m_interest_time_clickstream": u"1458122244:9000739,9000668;1458122245:9000668,9000156,9000018,9000437;1458222961:9000668;1458222962:9000668,9000739",
        u"d_count": u"0:1,11:52",
        u"words_count": u"234742272:1,528699392:1,1839135744:1,2105736192:1,-928841472:1",
        u"cyr_word_len": u"5;5;5;0",
        u"rids": u"73:36,76:14,11453:7",
        u"r_hits": u"1457246796:291384127",
        u"catalogue_full": u"1457246796:90-80:3665-11",
    },
)


testdata_passport_social = BaseLog(
    path="//home/passport/production/socialism/crypta-dump", date="2016-04-11", default_data=PASSPORT_SOCIAL
)
testdata_passport_social.add_row(
    uid="43453761", email=u"89518545837@gmail.com", phone="b4c11119e458bc1ff36e60f66b31e57f"
)
testdata_passport_social.add_row(
    uid="43453761",
    provider_id="2",
    userid="4657466",
    email=u"89518545837@gmail.com",
    phone="6a8a244553d8676d8fef4351712ec5fc",
)
testdata_passport_social.add_row(
    uid="34138394", email=u"ps5957466@yandex.ru", phone="4b52d13c66075ad12683daaa05ee9cba"
)
testdata_passport_social.add_row(
    uid="43453762", email=u"89518545837@gmail.com", phone="c616eef1826f1fde81bc4016af84c159"
)
testdata_passport_social.add_row(uid="34138394", email=u"ps5957466@yandex.ru")
testdata_passport_social.add_row(uid="34138394", provider_id="1", email=u"ps5957466@yandex.ru")
testdata_passport_social.add_row(uid="43453763", provider_id="1", userid="4657454", email=u"ps5957466@yandex.ru")
testdata_passport_social.add_row(uid="43453769", provider_id="1", userid="4657452")

testdata_all_radius_ips_date = BaseLog(
    path="//crypta/production/state/radius/log/2016-04-11", table_name="all_radius_ips", default_data={}
)
testdata_all_radius_ips_date.add_row(ip="217.118.90.186")
testdata_all_radius_ips_date.add_row(ip="217.118.90.186")
testdata_all_radius_ips_date.add_row(ip="176.52.102.148")
testdata_all_radius_ips_date.add_row(ip="85.174.38.23")
testdata_all_radius_ips_date.add_row(ip="85.174.38.23")
testdata_all_radius_ips_date.add_row(ip="85.174.38.25")
testdata_all_radius_ips_date.add_row(ip="85.174.38.33")
testdata_all_radius_ips_date.add_row(ip="85.174.38.33")
testdata_all_radius_ips_date.add_row(ip="85.174.38.25")
testdata_all_radius_ips_date.add_row(ip="176.59.7.165")


testdata_radius_log = BaseLog(
    path="//crypta/production/state/radius/log/2016-04-11", table_name="radius_log", default_data=RADIUS_LOG
)
testdata_radius_log.add_row(ip="217.118.90.186", login="login-10")
testdata_radius_log.add_row(ip="217.118.90.186", login="login-10")
testdata_radius_log.add_row(ip="176.52.102.148", login="login-37")
testdata_radius_log.add_row(ip="85.174.38.23", login="mail_login")
testdata_radius_log.add_row(ip="85.174.38.25", login="mail_login2")
testdata_radius_log.add_row(ip="85.174.38.25", login="mail_login_mob")
testdata_radius_log.add_row(ip="85.174.38.33", login="WatchRadius")
testdata_radius_log.add_row(ip="85.174.38.33", login="WatchRadius2")
testdata_radius_log.add_row(ip="85.174.38.23", login="login-1")
testdata_radius_log.add_row(ip="213.149.4.236", login="login-5")
testdata_radius_log.add_row(ip="176.59.7.165", login="login-10")
testdata_radius_log.add_row(ip="", login="login-101")
testdata_radius_log.add_row(ip="176.59.7.166", login="")
testdata_radius_log.add_row(ip="", login="")


testdata_log_all_radius_ips = BaseLog(
    path="//crypta/production/state/radius/log", table_name="all_radius_ips", default_data={}
)
testdata_log_all_radius_ips.add_row(ip="217.118.90.186")
testdata_log_all_radius_ips.add_row(ip="217.118.90.186")
testdata_log_all_radius_ips.add_row(ip="176.52.102.148")
testdata_log_all_radius_ips.add_row(ip="85.174.38.23")
testdata_log_all_radius_ips.add_row(ip="85.174.38.23")
testdata_log_all_radius_ips.add_row(ip="85.174.38.23")
testdata_log_all_radius_ips.add_row(ip="85.174.38.25")
testdata_log_all_radius_ips.add_row(ip="85.174.38.25")
testdata_log_all_radius_ips.add_row(ip="85.174.38.33")
testdata_log_all_radius_ips.add_row(ip="85.174.38.33")
testdata_log_all_radius_ips.add_row(ip="176.59.7.165")


testdata_dicts_dev_info = ComplexParametersLog(
    path="//crypta/production/state/graph/dicts", table_name="dev_info", default_data=DICTS_DEV_INFO
)
testdata_dicts_dev_info.add_row(
    key="31b332e66c971b9e94ddcc921de85eb7",
    value={
        u"mmetric_devids": u"31b332e66c971b9e94ddcc921de85eb7",
        u"screen_width": u"960",
        u"ua_profile": u"m|phone|samsung|android|4.4.2",
        u"apps": u"ru.yandex.searchplugin,com.avito.android,com.edadeal.android",
        u"connection_hist": u"cell:189,wifi:398",
        u"os_version": u"4.4.2",
        u"device_type": u"phone",
        u"google_adv_id": u"fcbed30d-c9a1-45d3-867e-1c47921b817e",
        u"model": u"Galaxy S4 Mini",
        u"manufacturer": u"Samsung",
        u"android_id": u"10bd82293264b388",
        u"screen_height": u"540",
    },
)
testdata_dicts_dev_info.add_row(
    key="31b332e66c971b9e94ddcc921de85eb7",
    value={
        u"mmetric_devids": u"31b332e66c971b9e94ddcc921de85eb7",
        u"screen_width": u"960",
        u"ua_profile": u"m|phone|samsung|android|4.4.2",
        u"apps": u"ru.yandex.searchplugin,com.avito.android,com.edadeal.android",
        u"connection_hist": u"cell:189,wifi:398",
        u"os_version": u"4.4.2",
        u"device_type": u"phone",
        u"google_adv_id": u"fcbed30d-c9a1-45d3-867e-1c47921b817e",
        u"model": u"Galaxy S4 Mini",
        u"manufacturer": u"Samsung",
        u"android_id": u"10bd82293264b388",
        u"screen_height": u"540",
    },
)
testdata_dicts_dev_info.add_row(
    key="32364d94bc020a2366adbc3b69cde858",
    value={
        u"mmetric_devids": u"32364d94bc020a2366adbc3b69cde858",
        u"screen_width": u"1280",
        u"ua_profile": u"m|phone|un|android|4.1.2",
        u"apps": u"ru.yandex.weatherplugin,ru.yandex.yandexnavi",
        u"connection_hist": u"cell:2,wifi:12",
        u"os_version": u"4.1.2",
        u"device_type": u"phone",
        u"google_adv_id": u"06edf5c0-39e3-4389-88af-497d6f9b139c",
        u"model": u"ZP950H",
        u"manufacturer": u"ZWX",
        u"timestamp_10m": u"1460886600",
        u"android_id": u"a3bbe9f2af3c9d49",
        u"screen_height": u"720",
    },
)
testdata_dicts_dev_info.add_row(
    key="31b332e66c971b9e94ddcc921de85eb7",
    value={
        u"mmetric_devids": u"31b332e66c971b9e94ddcc921de85eb7",
        u"screen_width": u"960",
        u"ua_profile": u"m|phone|samsung|android|4.4.2",
        u"apps": u"ru.yandex.searchplugin,com.avito.android,com.edadeal.android",
        u"connection_hist": u"cell:189,wifi:398",
        u"os_version": u"4.4.2",
        u"device_type": u"phone",
        u"google_adv_id": u"fcbed30d-c9a1-45d3-867e-1c47921b817e",
        u"model": u"Galaxy S4 Mini",
        u"manufacturer": u"Samsung",
        u"android_id": u"10bd82293264b388",
        u"screen_height": u"540",
    },
)
testdata_dicts_dev_info.add_row(
    key="32364d94bc020a2366adbc3b69cde858",
    value={
        u"mmetric_devids": u"32364d94bc020a2366adbc3b69cde858",
        u"screen_width": u"1280",
        u"ua_profile": u"m|phone|un|android|4.1.2",
        u"apps": u"ru.yandex.weatherplugin,ru.yandex.yandexnavi",
        u"connection_hist": u"cell:2,wifi:12",
        u"os_version": u"4.1.2",
        u"device_type": u"phone",
        u"google_adv_id": u"06edf5c0-39e3-4389-88af-497d6f9b139c",
        u"model": u"ZP950H",
        u"manufacturer": u"ZWX",
        u"timestamp_10m": u"1460886600",
        u"android_id": u"a3bbe9f2af3c9d49",
        u"screen_height": u"720",
    },
)


testdata_dicts_yuid_with_all_good = BaseLog(
    path="//crypta/production/state/graph/dicts",
    table_name="yuid_with_all_good",
    default_data=DICTS_YUID_WITH_ALL_GOOD,
)
testdata_dicts_yuid_with_all_good.add_row(
    reg_fp_dates="{u'47': {u'2016-04-07': 21, u'2016-04-09': 1, u'2016-04-08': 99, u'2016-04-10': 28, u'2016-04-11': 478, u'2016-04-12': 9}}",
    age="0.150995,0.697246,0.110961,0.028282,0.012513",
    ip_fp_dates="{u'5.164.250.199': {u'2016-04-08': 99, u'2016-04-07': 21}, u'5.164.235.154': {u'2016-04-09': 1, u'2016-04-10': 28, u'2016-04-11': 478, u'2016-04-12': 9}}",
    sex="0.954289,0.04571",
    browser_version="16.3.0.7146",
    income="0.011687,0.577247,0.411065",
    yuid="9062563711460046733",
    ua="Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7146 Yowser/2.5 Safari/537.36",
    ua_profile="d|desk|windows|10.0",
    browser="yandexbrowser",
)
testdata_dicts_yuid_with_all_good.add_row(
    reg_fp_dates="{u'47': {u'2016-04-07': 21, u'2016-04-09': 1, u'2016-04-08': 99, u'2016-04-10': 28, u'2016-04-11': 478, u'2016-04-12': 9}}",
    age="0.150995,0.697246,0.110961,0.028282,0.012513",
    ip_fp_dates="{u'5.164.250.199': {u'2016-04-08': 99, u'2016-04-07': 21}, u'5.164.235.154': {u'2016-04-09': 1, u'2016-04-10': 28, u'2016-04-11': 478, u'2016-04-12': 9}}",
    sex="0.954289,0.04571",
    browser_version="16.3.0.7146",
    income="0.011687,0.577247,0.411065",
    yuid="9062563711460046733",
    ua="Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7146 Yowser/2.5 Safari/537.36",
    ua_profile="d|desk|windows|10.0",
    browser="yandexbrowser",
)
testdata_dicts_yuid_with_all_good.add_row(
    reg_fp_dates="{u'47': {u'2016-04-07': 21, u'2016-04-09': 1, u'2016-04-08': 99, u'2016-04-10': 28, u'2016-04-11': 478, u'2016-04-12': 9}}",
    age="0.150995,0.697246,0.110961,0.028282,0.012513",
    ip_fp_dates="{u'5.164.250.199': {u'2016-04-08': 99, u'2016-04-07': 21}, u'5.164.235.154': {u'2016-04-09': 1, u'2016-04-10': 28, u'2016-04-11': 478, u'2016-04-12': 9}}",
    sex="0.954289,0.04571",
    browser_version="16.3.0.7146",
    income="0.011687,0.577247,0.411065",
    yuid="9062563711460046733",
    ua="Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7146 Yowser/2.5 Safari/537.36",
    ua_profile="d|desk|windows|10.0",
    browser="yandexbrowser",
)

testdata_statbox_sql_passport_accounts = ComplexParametersLog(
    path="//statbox", table_name="sql_passport_accounts", default_data=STATBOX_SQL_PASSPORT_ACCOUNTS
)
testdata_statbox_sql_passport_accounts.add_row(key="64045043", value={u"login": u"Lagutin2008", u"uid": u"64045043"})
testdata_statbox_sql_passport_accounts.add_row(key="64045043", value={u"login": u"Lagutin2008", u"uid": u"64045044"})
testdata_statbox_sql_passport_accounts.add_row(
    key="64178081", value={u"login": u"WatchRadiusMob", u"uid": u"64178081", u"karma": u"6000"}
)
testdata_statbox_sql_passport_accounts.add_row(key="64045043", value={u"login": u"ankekaterina", u"uid": u"64045043"})
testdata_statbox_sql_passport_accounts.add_row(
    key="64178081", value={u"login": u"ya-eli", u"uid": u"64178081", u"karma": u"6000"}
)
testdata_statbox_sql_passport_accounts.add_row(
    key="64252556", value={u"login": u"darken-ral", u"uid": u"64252556", u"karma": u"6000"}
)


testdata_bar_navig_log = BaseLog(
    path="//home/logfeller/logs/bar-navig-log/1d", date="2016-04-09", default_data=BAR_NAVIG_LOG
)
testdata_bar_navig_log.add_row(
    iso_eventtime="2016-04-09 10:20:43",
    yasoft="android.yabrowser",
    unixtime="1460186443",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&csrc=12&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=100663045&httpstatus=200&post=0&psu=7621727070167934961&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-1618_1039-1312_1040-6872_1040.906-9802_1041-1493_1041.906-4423&title=%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81&tv=6&url=http%3A%2F%2Fwww.yandex.ru%2F&uuid=69434b21a08207525a39df84707bdd30&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@imgs28-005.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:21:71556040:",
)
testdata_bar_navig_log.add_row(
    iso_eventtime="2016-04-09 10:20:43",
    yasoft="android.yabrowser",
    unixtime="1460186443",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&csrc=12&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=100663045&httpstatus=200&post=0&psu=7621727070167934961&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-1618_1039-1312_1040-6872_1040.906-9802_1041-1493_1041.906-4423&title=%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81&tv=6&url=http%3A%2F%2Fwww.yandex.ru%2F&uuid=69434b21a08207525a39df84707bdd30&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@imgs28-005.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:21:71556040:",
)
testdata_bar_navig_log.add_row(
    iso_eventtime="2016-04-09 10:20:54",
    yasoft="android.yabrowser",
    unixtime="1460186454",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=1297626946&httpstatus=200&p1=1237771847509542750145075881094348583&p2=2709134678478331740139314909851386159&post=0&psu=2114098526131764109&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-0_1039-728_1040-1558_1040.906-2286_1041-190_1041.906-918&title=text&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@ws39-452.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:29:75573403:",
)
testdata_bar_navig_log.add_row(
    iso_eventtime="2016-04-09 10:20:43",
    yasoft="android.yabrowser",
    unixtime="1460186443",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&csrc=12&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=100663045&httpstatus=200&post=0&psu=7621727070167934961&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-1618_1039-1312_1040-6872_1040.906-9802_1041-1493_1041.906-4423&title=%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81&tv=6&url=http%3A%2F%2Fwww.yandex.ru%2F&uuid=69434b21a08207525a39df84707bdd30&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@imgs28-005.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:21:71556040:",
)
testdata_bar_navig_log.add_row(
    iso_eventtime="2016-04-09 10:20:54",
    yasoft="android.yabrowser",
    unixtime="1460186454",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=1297626946&httpstatus=200&p1=1237771847509542750145075881094348583&p2=2709134678478331740139314909851386159&post=0&psu=2114098526131764109&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-0_1039-728_1040-1558_1040.906-2286_1041-190_1041.906-918&title=testtext&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@ws39-452.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:29:75573403:",
)

testdata_bar_navig_log.add_row(
    _date="2016-04-10",
    iso_eventtime="2016-04-10 10:20:43",
    yasoft="android.yabrowser",
    unixtime="1460236986",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&csrc=12&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=100663045&httpstatus=200&post=0&psu=7621727070167934961&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-1618_1039-1312_1040-6872_1040.906-9802_1041-1493_1041.906-4423&title=%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81&tv=6&url=http%3A%2F%2Fwww.yandex.ru%2F&uuid=69434b21a08207525a39df84707bdd30&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@imgs28-005.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:21:71556040:",
)
testdata_bar_navig_log.add_row(
    _date="2016-04-10",
    iso_eventtime="2016-04-10 10:20:43",
    yasoft="android.yabrowser",
    unixtime="1460236986",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&csrc=12&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=100663045&httpstatus=200&post=0&psu=7621727070167934961&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-1618_1039-1312_1040-6872_1040.906-9802_1041-1493_1041.906-4423&title=%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81&tv=6&url=http%3A%2F%2Fwww.yandex.ru%2F&uuid=69434b21a08207525a39df84707bdd30&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@imgs28-005.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:21:71556040:",
)
testdata_bar_navig_log.add_row(
    _date="2016-04-10",
    iso_eventtime="2016-04-10 10:20:54",
    yasoft="android.yabrowser",
    unixtime="1460236986",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=1297626946&httpstatus=200&p1=1237771847509542750145075881094348583&p2=2709134678478331740139314909851386159&post=0&psu=2114098526131764109&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-0_1039-728_1040-1558_1040.906-2286_1041-190_1041.906-918&title=text&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@ws39-452.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:29:75573403:",
)
testdata_bar_navig_log.add_row(
    _date="2016-04-10",
    iso_eventtime="2016-04-10 10:20:43",
    yasoft="android.yabrowser",
    unixtime="1460236986",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&csrc=12&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=100663045&httpstatus=200&post=0&psu=7621727070167934961&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-1618_1039-1312_1040-6872_1040.906-9802_1041-1493_1041.906-4423&title=%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81&tv=6&url=http%3A%2F%2Fwww.yandex.ru%2F&uuid=69434b21a08207525a39df84707bdd30&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@imgs28-005.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:21:71556040:",
)
testdata_bar_navig_log.add_row(
    _date="2016-04-10",
    iso_eventtime="2016-04-10 10:20:54",
    yasoft="android.yabrowser",
    unixtime="1460236986",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=1297626946&httpstatus=200&p1=1237771847509542750145075881094348583&p2=2709134678478331740139314909851386159&post=0&psu=2114098526131764109&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-0_1039-728_1040-1558_1040.906-2286_1041-190_1041.906-918&title=testtext&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@ws39-452.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:29:75573403:",
)

testdata_bar_navig_log.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 10:20:43",
    yasoft="android.yabrowser",
    unixtime="1460378335",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&csrc=12&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=100663045&httpstatus=200&post=0&psu=7621727070167934961&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-1618_1039-1312_1040-6872_1040.906-9802_1041-1493_1041.906-4423&title=%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81&tv=6&url=http%3A%2F%2Fwww.yandex.ru%2F&uuid=69434b21a08207525a39df84707bdd30&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@imgs28-005.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:21:71556040:",
)
testdata_bar_navig_log.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 10:20:43",
    yasoft="android.yabrowser",
    unixtime="1460378335",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&csrc=12&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=100663045&httpstatus=200&post=0&psu=7621727070167934961&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-1618_1039-1312_1040-6872_1040.906-9802_1041-1493_1041.906-4423&title=%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81&tv=6&url=http%3A%2F%2Fwww.yandex.ru%2F&uuid=69434b21a08207525a39df84707bdd30&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@imgs28-005.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:21:71556040:",
)
testdata_bar_navig_log.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 10:20:54",
    yasoft="android.yabrowser",
    unixtime="1460378335",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=1297626946&httpstatus=200&p1=1237771847509542750145075881094348583&p2=2709134678478331740139314909851386159&post=0&psu=2114098526131764109&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-0_1039-728_1040-1558_1040.906-2286_1041-190_1041.906-918&title=text&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@ws39-452.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:29:75573403:",
)
testdata_bar_navig_log.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 10:20:43",
    yasoft="android.yabrowser",
    unixtime="1460378335",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&csrc=12&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=100663045&httpstatus=200&post=0&psu=7621727070167934961&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-1618_1039-1312_1040-6872_1040.906-9802_1041-1493_1041.906-4423&title=%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81&tv=6&url=http%3A%2F%2Fwww.yandex.ru%2F&uuid=69434b21a08207525a39df84707bdd30&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@imgs28-005.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:21:71556040:",
)
testdata_bar_navig_log.add_row(
    _date="2016-04-11",
    iso_eventtime="2016-04-11 10:20:54",
    yasoft="android.yabrowser",
    unixtime="1460378335",
    yandexuid="1747090401445659068",
    http_params="brandID=yandex&brclid=1&brl=ru&deviceid=2bc55f53bdb8b23ac06f441b85ac576e&eid=apk.1%3Bod.1%3Bpdb.0%3Bpwf.2%3Bsap.0%3Btsc.1%3Btuc.1&hip=1297626946&httpstatus=200&p1=1237771847509542750145075881094348583&p2=2709134678478331740139314909851386159&post=0&psu=2114098526131764109&r1=pecbpavgrxidsjudjqeqgfevpojmdfrueqwcsyibohqjlphevgegekhbdansprqskrsuasbmcvixoaaxdplxwfxsebxafuabtcfaf0a6181b02225412fdf23b1ffea09a2d&referer=http%3A%2F%2Fwww.yandex.ru%2F&show=1&t=1037-0_1038-0_1039-728_1040-1558_1040.906-2286_1041-190_1041.906-918&title=testtext&ver=16.2.0.5397&yasoft=android.yabrowser",
    ip="37.29.41.1",
    source_uri="prt://bar_navig@ws39-452.yandex.ru/usr/local/www/logs/current-spy-barnav-9200",
    _stbx="rt3.fol--bar_navig--bar-navig-log:29:75573403:",
)


testdata_sendr_click_log = BaseLog(
    path="//home/logfeller/logs/sendr-click-log/1d", date="2016-04-09", default_data=SENDR_CLICK_LOG
)
testdata_sendr_click_log.add_row(
    cookies="{u'zm': u'm-white_bender.flex.webp.css-https%3Awww_zNa-f_5uJcPTYK6A_vgZjL6hOW8%3Al', u'yandex_login': u'dr-ops', u'yabs-frequency': u'/4/0W0c09aMKrTXRr1N/paMmSAWaP_H4i72S95N7Ch1md2H7iYHoS9maDQyaSd2C93KFBd9mZ2HrGoXoS70abKbxC6Xp97m000ETGh1mM2HLFJwmSAWaOF8vi72e9600/', u'yandexuid': u'1480112581366481068', u'_ym_isad': u'2', u'yandex_gid': u'213', u'L': u'AVp4eVBcTGMLcGt5YlxCeX52dQFhU0wHCz9pDkNB.1465073028.12447.381002.d6de77e2c5809114b67ae1c21af0ab07', u'fuid01': u'5172d9154f757487.ibgLSKzpvKwsB3aCqORHuidWd4ihqQKwi90AyHzTEJrgS9V0p5jCS1HSjaVrvZVPtCXGuRT4VOxUcsS_tUP6PS-5PYu0ScAbpxlH7-hXeEdtNJa2L7cKuJwRGnX8is_p', u'yp': u'1724776959.multib.1#1469172237.ww.1#2147483647.ygo.1%3A213#1480841015.szm.1_00%3A1366x768%3A1366x599#1495982635.dsws.56#1495982635.dswa.0#1495982635.dwss.53#1495911986.st_soft_stripe_s.22#1495567726.st_set_s.23#1490692704.st_promobar_s.5#1495911817.st_home_s.9#1493320294.st_set_home_s.4#1467135531.cnps.9003677093%3Amax#1493378279.dwws.1#1495913898.dwys.1#1496176342.st_browser_s.3#1495993017.st_vb_s.2#1780433028.udn.cDpkci1vcHM%3D', u'ys': u'wprid.1465072337122033-11560178426718612591346519-myt1-2097#udn.cDpkci1vcHM%3D', u'my': u'YygDQoDVNzYBAQA=', u'_ym_uid': u'1465073018333340054'}",
    user_agent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
    iso_eventtime="2016-06-04 23:48:29",
    unixtime="1465073309",
    source_uri="prt://sendr@delivery1h.cmail.yandex.net/var/log/yandex/sendr/click.log",
    email="dr.ops@yandex.ru",
    allowed="True",
    event="click",
    _stbx="rt3.fol--other--other:34:435147338:",
)
testdata_sendr_click_log.add_row(
    cookies="{u'zm': u'm-white_bender.flex.webp.css-https%3Awww_zNa-f_5uJcPTYK6A_vgZjL6hOW8%3Al', u'yandex_login': u'dr-ops', u'yabs-frequency': u'/4/0W0c09aMKrTXRr1N/paMmSAWaP_H4i72S95N7Ch1md2H7iYHoS9maDQyaSd2C93KFBd9mZ2HrGoXoS70abKbxC6Xp97m000ETGh1mM2HLFJwmSAWaOF8vi72e9600/', u'yandexuid': u'1480112581366481068', u'_ym_isad': u'2', u'yandex_gid': u'213', u'L': u'AVp4eVBcTGMLcGt5YlxCeX52dQFhU0wHCz9pDkNB.1465073028.12447.381002.d6de77e2c5809114b67ae1c21af0ab07', u'fuid01': u'5172d9154f757487.ibgLSKzpvKwsB3aCqORHuidWd4ihqQKwi90AyHzTEJrgS9V0p5jCS1HSjaVrvZVPtCXGuRT4VOxUcsS_tUP6PS-5PYu0ScAbpxlH7-hXeEdtNJa2L7cKuJwRGnX8is_p', u'yp': u'1724776959.multib.1#1469172237.ww.1#2147483647.ygo.1%3A213#1480841015.szm.1_00%3A1366x768%3A1366x599#1495982635.dsws.56#1495982635.dswa.0#1495982635.dwss.53#1495911986.st_soft_stripe_s.22#1495567726.st_set_s.23#1490692704.st_promobar_s.5#1495911817.st_home_s.9#1493320294.st_set_home_s.4#1467135531.cnps.9003677093%3Amax#1493378279.dwws.1#1495913898.dwys.1#1496176342.st_browser_s.3#1495993017.st_vb_s.2#1780433028.udn.cDpkci1vcHM%3D', u'ys': u'wprid.1465072337122033-11560178426718612591346519-myt1-2097#udn.cDpkci1vcHM%3D', u'my': u'YygDQoDVNzYBAQA=', u'_ym_uid': u'1465073018333340054'}",
    user_agent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
    iso_eventtime="2016-06-04 23:48:29",
    unixtime="1465073309",
    source_uri="prt://sendr@delivery1h.cmail.yandex.net/var/log/yandex/sendr/click.log",
    email="dr.ops@yandex.ru",
    allowed="True",
    event="click",
    _stbx="rt3.fol--other--other:34:435147338:",
)
testdata_sendr_click_log.add_row(
    cookies="{}",
    user_agent="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.2 Safari/537.36",
    iso_eventtime="2016-06-04 23:26:12",
    parsed="False",
    unixtime="1465071972",
    source_uri="prt://other@5.255.227.160unknown",
    campaign_id="None",
    email="None",
    user_ip="185.30.176.26",
    allowed="None",
    event="px",
    _stbx="rt3.sas--other--other:17:455305112:",
)
testdata_sendr_click_log.add_row(
    cookies="{u'zm': u'm-white_bender.flex.webp.css-https%3Awww_zNa-f_5uJcPTYK6A_vgZjL6hOW8%3Al', u'yandex_login': u'dr-ops', u'yabs-frequency': u'/4/0W0c09aMKrTXRr1N/paMmSAWaP_H4i72S95N7Ch1md2H7iYHoS9maDQyaSd2C93KFBd9mZ2HrGoXoS70abKbxC6Xp97m000ETGh1mM2HLFJwmSAWaOF8vi72e9600/', u'yandexuid': u'1480112581366481068', u'_ym_isad': u'2', u'yandex_gid': u'213', u'L': u'AVp4eVBcTGMLcGt5YlxCeX52dQFhU0wHCz9pDkNB.1465073028.12447.381002.d6de77e2c5809114b67ae1c21af0ab07', u'fuid01': u'5172d9154f757487.ibgLSKzpvKwsB3aCqORHuidWd4ihqQKwi90AyHzTEJrgS9V0p5jCS1HSjaVrvZVPtCXGuRT4VOxUcsS_tUP6PS-5PYu0ScAbpxlH7-hXeEdtNJa2L7cKuJwRGnX8is_p', u'yp': u'1724776959.multib.1#1469172237.ww.1#2147483647.ygo.1%3A213#1480841015.szm.1_00%3A1366x768%3A1366x599#1495982635.dsws.56#1495982635.dswa.0#1495982635.dwss.53#1495911986.st_soft_stripe_s.22#1495567726.st_set_s.23#1490692704.st_promobar_s.5#1495911817.st_home_s.9#1493320294.st_set_home_s.4#1467135531.cnps.9003677093%3Amax#1493378279.dwws.1#1495913898.dwys.1#1496176342.st_browser_s.3#1495993017.st_vb_s.2#1780433028.udn.cDpkci1vcHM%3D', u'ys': u'wprid.1465072337122033-11560178426718612591346519-myt1-2097#udn.cDpkci1vcHM%3D', u'my': u'YygDQoDVNzYBAQA=', u'_ym_uid': u'1465073018333340054'}",
    user_agent="Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Safari/537.36",
    iso_eventtime="2016-06-04 23:48:29",
    unixtime="1465073309",
    source_uri="prt://sendr@delivery1h.cmail.yandex.net/var/log/yandex/sendr/click.log",
    email="dr.ops@yandex.ru",
    allowed="True",
    event="click",
    _stbx="rt3.fol--other--other:34:435147338:",
)
testdata_sendr_click_log.add_row(
    cookies="{}",
    user_agent="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.2 Safari/537.36",
    iso_eventtime="2016-06-04 23:26:12",
    parsed="False",
    unixtime="1465071972",
    source_uri="prt://other@5.255.227.160unknown",
    campaign_id="None",
    email="None",
    user_ip="185.30.176.26",
    allowed="None",
    event="px",
    _stbx="rt3.sas--other--other:17:455305112:",
)

testdata_sendr_click_log.add_row(
    _date="2016-04-10",
    cookies="{}",
    user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_2 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Mobile/13F69",
    iso_eventtime="2016-06-05 15:50:20",
    unixtime="1465131020",
    source_uri="prt://sendr@delivery2j.cmail.yandex.net/var/log/yandex/sendr/click.log",
    email="kir-roor@yandex.ru",
    allowed="None",
    event="px",
    _stbx="rt3.iva--other--other:113:63431154:",
)
testdata_sendr_click_log.add_row(
    _date="2016-04-10",
    cookies="{}",
    user_agent="Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_2 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Mobile/13F69",
    iso_eventtime="2016-06-05 15:50:20",
    unixtime="1465131020",
    source_uri="prt://sendr@delivery2j.cmail.yandex.net/var/log/yandex/sendr/click.log",
    email="kir-roor@yandex.ru",
    allowed="None",
    event="px",
    _stbx="rt3.iva--other--other:113:63431154:",
)
testdata_sendr_click_log.add_row(
    _date="2016-04-10",
    cookies="{}",
    user_agent="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1700.2 Safari/537.36",
    iso_eventtime="2016-06-05 20:19:24",
    parsed="False",
    unixtime="1465147164",
    source_uri="prt://sendr@delivery1h.cmail.yandex.net/var/log/yandex/sendr/click.log",
    campaign_id="None",
    email="None",
    user_ip="188.93.56.130",
    allowed="None",
    event="px",
    _stbx="rt3.fol--other--other:70:70501591:",
)

testdata_sendr_click_log.add_row(
    _date="2016-04-11",
    cookies="{}",
    user_agent="Mozilla/5.0 (Windows NT 5.1; rv:11.0) Gecko Firefox/11.0 (via ggpht.com GoogleImageProxy)",
    iso_eventtime="2016-06-06 21:50:12",
    unixtime="1465239012",
    source_uri="prt://sendr@delivery1p.cmail.yandex.net/var/log/yandex/sendr/click.log",
    email="roman.slepnev@gmail.com",
    allowed="None",
    event="px",
    _stbx="rt3.fol--other--other:109:46136683:",
)
testdata_sendr_click_log.add_row(
    _date="2016-04-11",
    cookies="{}",
    user_agent="Mozilla/5.0 (Windows NT 5.1; rv:11.0) Gecko Firefox/11.0 (via ggpht.com GoogleImageProxy)",
    iso_eventtime="2016-06-06 21:50:12",
    unixtime="1465239012",
    source_uri="prt://sendr@delivery1p.cmail.yandex.net/var/log/yandex/sendr/click.log",
    email="roman.slepnev@gmail.com",
    allowed="None",
    event="px",
    _stbx="rt3.fol--other--other:109:46136683:",
)
testdata_sendr_click_log.add_row(
    _date="2016-04-11",
    cookies="{}",
    user_agent="Mozilla/5.0 (compatible; YandexImageResizer/2.0; +http://yandex.com/bots)",
    iso_eventtime="2016-06-06 23:37:02",
    unixtime="1465245422",
    source_uri="prt://sendr@delivery1m.cmail.yandex.net/var/log/yandex/sendr/click.log",
    campaign_id="1709",
    email="aSEXeich@yandex.ru",
    user_ip="5.255.206.198",
    allowed="None",
    event="px",
    _stbx="rt3.fol--other--other:16:443249642:",
)

testdata_crypta_rt_geo_log = BaseLog(
    path="//home/logfeller/logs/crypta-rt-geo-log/1d", date="2016-04-09", default_data=CRYPTA_RT_GEO_LOG
)
testdata_crypta_rt_geo_log.add_row(
    acc="100",
    iso_eventtime="2016-04-09 10:11:47",
    uuid="552d417a51f793e4b5ded9a19aa08622",
    cid="1602040000000166461",
    unixtime="1460185907",
    source_uri="prt://rtcrypta@storm01e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="44.78818555360001",
    ip="217.13.91.184",
    geo_source_types="mobile_metrics,ipgeo",
    lat="48.78049483120001",
    now="1460185924",
    type="mobile_metrics_identified",
    yauid="601826891455541112",
    _stbx="rt3.iva--rtcrypta--crypta-rt-geo-log:2:124133337:",
)
testdata_crypta_rt_geo_log.add_row(
    acc="175",
    iso_eventtime="2016-04-09 10:11:47",
    uuid="552d417a51f793e4b5ded9a19aa08622",
    cid="1602040000000166461",
    unixtime="1460185907",
    source_uri="prt://rtcrypta@storm01e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="44.78818555360001",
    ip="31.128.146.113",
    geo_source_types="mobile_metrics,metrics,ipgeo,bar",
    lat="48.78049483120001",
    now="1460185924",
    type="mobile_metrics_identified",
    yauid="601826891455541112",
    _stbx="rt3.iva--rtcrypta--crypta-rt-geo-log:2:124133337:",
)
testdata_crypta_rt_geo_log.add_row(
    acc="505",
    iso_eventtime="2016-04-09 10:26:23",
    uuid="552d417a51f793e4b5ded9a19aa08622",
    cid="1602040000000166461",
    unixtime="1460186783",
    source_uri="prt://rtcrypta@storm01e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="44.78818555360001",
    ip="217.13.91.184",
    geo_source_types="mobile_metrics,ipgeo",
    lat="48.78049483120001",
    now="1460186827",
    type="mobile_metrics_identified",
    yauid="1167671261428166806",
    _stbx="rt3.iva--rtcrypta--crypta-rt-geo-log:1:133763997:",
)
testdata_crypta_rt_geo_log.add_row(
    acc="1005",
    iso_eventtime="2016-04-09 10:11:47",
    uuid="552d417a51f793e4b5ded9a19aa08622",
    cid="1602040000000166461",
    unixtime="1460185907",
    source_uri="prt://rtcrypta@storm01e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="44.78818555360001",
    ip="85.174.38.23",
    geo_source_types="mobile_metrics,ipgeo",
    lat="48.78049483120001",
    now="1460185924",
    type="map_navig_identified",
    yauid="1167671261428166806",
    _stbx="rt3.iva--rtcrypta--crypta-rt-geo-log:2:124133337:",
)
testdata_crypta_rt_geo_log.add_row(
    acc="10001",
    iso_eventtime="2016-04-09 10:26:23",
    uuid="552d417a51f793e4b5ded9a19aa08622",
    cid="1602040000000166461",
    unixtime="1460186783",
    source_uri="prt://rtcrypta@storm01e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="44.78818555360001",
    ip="217.13.91.184",
    geo_source_types="mobile_metrics,ipgeo",
    lat="48.78049483120001",
    now="1460186827",
    type="metrics_raw",
    yauid="601826891455541113",
    _stbx="rt3.iva--rtcrypta--crypta-rt-geo-log:1:133763997:",
)

testdata_crypta_rt_geo_log.add_row(
    _date="2016-04-10",
    acc="71",
    iso_eventtime="2016-04-10 01:46:12",
    uuid="dbd9e0b1ce69e8e188de9888e9d0aca6",
    cid="1604020000000540842",
    unixtime="1460241972",
    source_uri="prt://rtcrypta@storm07e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="37.4445734",
    ip="188.123.230.137",
    geo_source_types="mobile_metrics",
    lat="55.6779104",
    now="1460242006",
    type="mobile_metrics_identified",
    yauid="601826891455541114",
    _stbx="rt3.man--rtcrypta--crypta-rt-geo-log:2:172260:",
)
testdata_crypta_rt_geo_log.add_row(
    _date="2016-04-10",
    acc="151",
    iso_eventtime="2016-04-10 01:46:12",
    uuid="dbd9e0b1ce69e8e188de9888e9d0aca6",
    cid="1604020000000540842",
    unixtime="1460241972",
    source_uri="prt://rtcrypta@storm07e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="37.4445734",
    ip="217.13.91.184",
    geo_source_types="mobile_metrics,metrics,ipgeo,bar",
    lat="55.6779104",
    now="1460242006",
    type="mobile_metrics_identified",
    yauid="601826891455541114",
    _stbx="rt3.man--rtcrypta--crypta-rt-geo-log:2:172260:",
)
testdata_crypta_rt_geo_log.add_row(
    _date="2016-04-10",
    acc="511",
    iso_eventtime="2016-04-10 01:56:24",
    uuid="dbd9e0b1ce69e8e188de9888e9d0aca6",
    cid="1604020000000540842",
    unixtime="1460242584",
    source_uri="prt://rtcrypta@storm07e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="37.4447186",
    ip="217.13.91.184",
    geo_source_types="mobile_metrics,metrics,ipgeo,bar",
    lat="55.6778428",
    now="1460242586",
    type="map_navig_identified",
    yauid="101826891455542223",
    _stbx="rt3.man--rtcrypta--crypta-rt-geo-log:2:172423:",
)
testdata_crypta_rt_geo_log.add_row(
    _date="2016-04-10",
    acc="1499",
    iso_eventtime="2016-04-10 01:46:12",
    uuid="dbd9e0b1ce69e8e188de9888e9d0aca6",
    cid="1604020000000540842",
    unixtime="1460241972",
    source_uri="prt://rtcrypta@storm07e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="37.4445734",
    ip="217.13.91.184",
    geo_source_types="mobile_metrics",
    lat="55.6779104",
    now="1460242006",
    type="metrics_raw",
    yauid="601826891455541115",
    _stbx="rt3.man--rtcrypta--crypta-rt-geo-log:2:172260:",
)
testdata_crypta_rt_geo_log.add_row(
    _date="2016-04-10",
    acc="12000",
    iso_eventtime="2016-04-10 01:56:24",
    uuid="dbd9e0b1ce69e8e188de9888e9d0aca6",
    cid="1604020000000540842",
    unixtime="1460242584",
    source_uri="prt://rtcrypta@storm07e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="37.4447186",
    ip="85.174.38.23",
    geo_source_types="mobile_metrics",
    lat="55.6778428",
    now="1460242586",
    type="metrics_raw",
    yauid="1726075371455990918",
    _stbx="rt3.man--rtcrypta--crypta-rt-geo-log:2:172423:",
)

testdata_crypta_rt_geo_log.add_row(
    _date="2016-04-11",
    acc="101",
    iso_eventtime="2016-04-11 21:35:58",
    uuid="021979592b36831fde8070bbb557b329",
    cid="181746585406122669",
    unixtime="1460399758",
    source_uri="prt://rtcrypta@storm07e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="37.035312075896584",
    ip="178.217.100.100",
    geo_source_types="mobile_metrics,ipgeo",
    lat="55.600606393677324",
    now="1460399818",
    type="mobile_metrics_identified",
    yauid="1748232901455365413",
    _stbx="rt3.iva--rtcrypta--crypta-rt-geo-log:1:134712536:",
)
testdata_crypta_rt_geo_log.add_row(
    _date="2016-04-11",
    acc="149",
    iso_eventtime="2016-04-11 21:35:58",
    uuid="021979592b36831fde8070bbb557b329",
    cid="181746585406122669",
    unixtime="1460399758",
    source_uri="prt://rtcrypta@storm07e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="37.035312075896584",
    ip="178.217.100.100",
    geo_source_types="mobile_metrics,ipgeo",
    lat="55.600606393677324",
    now="1460399818",
    type="mobile_metrics_identified",
    yauid="601826891455541115",
    _stbx="rt3.iva--rtcrypta--crypta-rt-geo-log:1:134712536:",
)
testdata_crypta_rt_geo_log.add_row(
    _date="2016-04-11",
    acc="499",
    iso_eventtime="2016-04-11 21:35:58",
    uuid="021979592b36831fde8070bbb557b329",
    cid="181746585406122669",
    unixtime="1460399758",
    source_uri="prt://rtcrypta@storm07e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="37.035312075896584",
    ip="217.13.91.184",
    geo_source_types="mobile_metrics,ipgeo",
    lat="55.600606393677324",
    now="1460399818",
    type="mobile_metrics_identified",
    yauid="601826891455541115",
    _stbx="rt3.iva--rtcrypta--crypta-rt-geo-log:1:134712536:",
)
testdata_crypta_rt_geo_log.add_row(
    _date="2016-04-11",
    acc="999",
    iso_eventtime="2016-04-11 21:35:58",
    uuid="021979592b36831fde8070bbb557b329",
    cid="181746585406122669",
    unixtime="1460399758",
    source_uri="prt://rtcrypta@storm07e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="37.035312075896584",
    ip="217.13.91.184",
    geo_source_types="mobile_metrics,metrics,ipgeo,bar",
    lat="55.600606393677324",
    now="1460399823",
    type="map_navig_identified",
    yauid="1726075371455990918",
    _stbx="rt3.iva--rtcrypta--crypta-rt-geo-log:1:134712540:",
)
testdata_crypta_rt_geo_log.add_row(
    _date="2016-04-11",
    acc="9999",
    iso_eventtime="2016-04-11 21:35:58",
    uuid="021979592b36831fde8070bbb557b329",
    cid="181746585406122669",
    unixtime="1460399758",
    source_uri="prt://rtcrypta@storm07e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="37.035312075896584",
    ip="85.174.38.23",
    geo_source_types="mobile_metrics,ipgeo",
    lat="55.600606393677324",
    now="1460399818",
    type="map_navig_identified",
    yauid="601826891455541114",
    _stbx="rt3.iva--rtcrypta--crypta-rt-geo-log:1:134712536:",
)
testdata_crypta_rt_geo_log.add_row(
    _date="2016-04-11",
    acc="11999",
    iso_eventtime="2016-04-11 21:35:58",
    uuid="021979592b36831fde8070bbb557b329",
    cid="181746585406122669",
    unixtime="1460399758",
    source_uri="prt://rtcrypta@storm07e.rtcrypta.yandex.net/var/log/storm/statbox/rt-geo-log/push-client",
    lon="37.035312075896584",
    ip="217.13.91.184",
    geo_source_types="mobile_metrics,ipgeo",
    lat="55.600606393677324",
    now="1460399823",
    type="metrics_raw",
    yauid="101826891455542222",
    _stbx="rt3.iva--rtcrypta--crypta-rt-geo-log:1:134712540:",
)

testdata_mobile_tracking_log = BaseLog(
    path="//home/logfeller/logs/metrika-mobile-install-log/1d", date="2016-04-09", default_data=MOBILE_TRACKING_LOG
)
testdata_mobile_tracking_log.add_row(
    UUID="021979592b36831fde8070bbb557b329",
    RegionID="213",
    Model="iPad",
    APIKey="29733",
    AppPlatform="iOS",
    StartTimestamp="1460223976",
    DeviceIDHash="9493461388394930183",
    ClickTimestamp="1460223976",
    timestamp="2016-04-09 17:46:56",
    UrlParameters_Values="['click','ru.yandex.mail']",
    UserAgent="Mozilla/5.0 (iPad; CPU OS 8_4_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12H321",
    ClientIP="::ffff:85.31.113.126",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:57980",
    ReceiveTimestamp="1460223976",
    DeviceType="TABLET",
    OSVersion="8.4.1",
    iso_eventtime="2016-04-09 20:46:56",
    YandexUidRu="8622478831441691934",
    UrlParameters_Keys="['action','app_id']",
    StartTime="2016-04-09 20:46:16",
    Manufacturer="Apple",
)
testdata_mobile_tracking_log.add_row(
    UUID="021979592b36831fde8070bbb557b329",
    RegionID="213",
    Model="iPad",
    APIKey="29733",
    AppPlatform="iOS",
    StartTimestamp="1460223976",
    DeviceIDHash="9493461388394930183",
    ClickTimestamp="1460223976",
    timestamp="2016-04-09 17:46:56",
    UrlParameters_Values="['click','ru.yandex.mail']",
    UserAgent="Mozilla/5.0 (iPad; CPU OS 8_4_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12H321",
    ClientIP="::ffff:85.31.113.126",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:57980",
    ReceiveTimestamp="1460223976",
    DeviceType="TABLET",
    OSVersion="8.4.1",
    iso_eventtime="2016-04-09 20:46:56",
    YandexUidRu="8622478831441691934",
    UrlParameters_Keys="['action','app_id']",
    StartTime="2016-04-09 20:46:16",
    Manufacturer="Apple",
)
testdata_mobile_tracking_log.add_row(
    UUID="021979592b36831fde8070bbb557b329",
    RegionID="213",
    Model="iPad",
    APIKey="29733",
    AppPlatform="iOS",
    StartTimestamp="1460223976",
    DeviceIDHash="9493461388394930183",
    ClickTimestamp="1460223976",
    timestamp="2016-04-09 17:46:56",
    UrlParameters_Values="['click','ru.yandex.mail']",
    UserAgent="Mozilla/5.0 (iPad; CPU OS 8_4_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12H321",
    ClientIP="::ffff:85.31.113.126",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:57980:",
    ReceiveTimestamp="1460223976",
    DeviceType="TABLET",
    UniqueClickID="10847448833746539528",
    YMTrackingID="10847448833746539528",
    OSVersion="8.4.1",
    iso_eventtime="2016-04-09 20:46:56",
    YandexUidRu="8622478831441691934",
    UrlParameters_Keys="['action','app_id']",
    StartTime="2016-04-09 20:46:16",
    Manufacturer="Apple",
)
testdata_mobile_tracking_log.add_row(
    UUID="bb35f489f1ceccf89579a11f92377746",
    RegionID="213",
    Model="iPad",
    APIKey="29733",
    AppPlatform="iOS",
    StartTimestamp="1460223976",
    DeviceIDHash="9493461388394930183",
    ClickTimestamp="1460223976",
    timestamp="2016-04-09 17:46:56",
    UrlParameters_Values="['click','ru.yandex.mail']",
    UserAgent="Mozilla/5.0 (iPad; CPU OS 8_4_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12H321",
    ClientIP="::ffff:85.31.113.126",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:57980",
    ReceiveTimestamp="1460223976",
    DeviceType="TABLET",
    OSVersion="8.4.1",
    iso_eventtime="2016-04-09 20:46:56",
    YandexUidRu="8622478831441691934",
    UrlParameters_Keys="['action','app_id']",
    StartTime="2016-04-09 20:46:16",
    Manufacturer="Apple",
)
testdata_mobile_tracking_log.add_row(
    UUID="bb35f489f1ceccf89579a11f92377746",
    RegionID="213",
    Model="iPad",
    APIKey="29733",
    AppPlatform="iOS",
    StartTimestamp="1460223976",
    DeviceIDHash="9493461388394930183",
    ClickTimestamp="1460223976",
    timestamp="2016-04-09 17:46:56",
    UrlParameters_Values="['click','ru.yandex.mail']",
    UserAgent="Mozilla/5.0 (iPad; CPU OS 8_4_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12H321",
    ClientIP="::ffff:85.31.113.126",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:57980:",
    ReceiveTimestamp="1460223976",
    DeviceType="TABLET",
    UniqueClickID="10847448833746539528",
    YMTrackingID="10847448833746539528",
    OSVersion="8.4.1",
    iso_eventtime="2016-04-09 20:46:56",
    YandexUidRu="8622478831441691934",
    UrlParameters_Keys="['action','app_id']",
    StartTime="2016-04-09 20:46:16",
    Manufacturer="Apple",
)
testdata_mobile_tracking_log.add_row(
    _date="2016-04-10",
    UUID="021979592b36831fde8070bbb557b329",
    RegionID="213",
    Model="Galaxy A3",
    APIKey="106400",
    AppPlatform="android",
    StartTimestamp="1460315530",
    DeviceIDHash="5575590620955852110",
    ClickTimestamp="1460315530",
    timestamp="2016-04-10 19:12:51",
    UrlParameters_Values="['dzen-couple-t2','ru','neYB']",
    UserAgent="Mozilla/5.0 (Linux; Android 5.0.2; SM-A300F Build/LRX22G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36",
    ClientIP="::ffff:109.173.43.153",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:67659:",
    ReceiveTimestamp="1460315530",
    source_uri="prt://mobmetrika@mtcalclog03e.yandex.ru/opt/statbox_export_mobile/mobile-clicks.log",
    DeviceType="PHONE",
    OSVersion="5.0.2",
    iso_eventtime="2016-04-10 22:12:51",
    YandexUidRu="1469860561425943192",
    ReceiveTimeZone="0",
    UrlParameters_Keys="['creative','geo','target']",
    StartTime="2016-04-10 22:12:10",
    EventType="EVENT_AD_CLICK",
    Manufacturer="Samsung",
)
testdata_mobile_tracking_log.add_row(
    _date="2016-04-10",
    UUID="021979592b36831fde8070bbb557b329",
    RegionID="213",
    Model="Galaxy A3",
    APIKey="106400",
    AppPlatform="android",
    StartTimestamp="1460315530",
    DeviceIDHash="5575590620955852110",
    ClickTimestamp="1460315530",
    timestamp="2016-04-10 19:12:51",
    UrlParameters_Values="['dzen-couple-t2','ru','neYB']",
    UserAgent="Mozilla/5.0 (Linux; Android 5.0.2; SM-A300F Build/LRX22G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36",
    ClientIP="::ffff:109.173.43.153",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:67659:",
    ReceiveTimestamp="1460315530",
    source_uri="prt://mobmetrika@mtcalclog03e.yandex.ru/opt/statbox_export_mobile/mobile-clicks.log",
    DeviceType="PHONE",
    OSVersion="5.0.2",
    iso_eventtime="2016-04-10 22:12:51",
    YandexUidRu="1469860561425943192",
    ReceiveTimeZone="0",
    UrlParameters_Keys="['creative','geo','target']",
    StartTime="2016-04-10 22:12:10",
    EventType="EVENT_AD_CLICK",
    Manufacturer="Samsung",
)
testdata_mobile_tracking_log.add_row(
    _date="2016-04-10",
    UUID="021979592b36831fde8070bbb557b329",
    RequestHeaders_Names="['Accept-Language','Upgrade-Insecure-Requests','User-Agent','Host','Accept-Encoding','Referer','Cookie','Connection','Accept']",
    ClickReceiveTimestamp="1460315531",
    RegionID="213",
    ClickDateTime="2016-04-10 22:12:11",
    Model="Galaxy A3",
    APIKey="106400",
    AppPlatform="android",
    StartTimestamp="1460315531",
    DeviceIDHash="1955956076350024385",
    ClickTimestamp="1460315531",
    timestamp="2016-04-10 19:12:51",
    UrlParameters_Values="['dzen-couple-t2','ru','neYB']",
    UserAgent="Mozilla/5.0 (Linux; Android 5.0.2; SM-A300F Build/LRX22G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36",
    ClientIP="::ffff:109.173.43.153",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:67659:",
    ReceiveTimestamp="1460315531",
    source_uri="prt://mobmetrika@mtcalclog03e.yandex.ru/opt/statbox_export_mobile/mobile-clicks.log",
    DeviceType="PHONE",
    UniqueClickID="11378786936113604095",
    YMTrackingID="11378786936113604095",
    OSVersion="5.0.2",
    iso_eventtime="2016-04-10 22:12:51",
    RequestHeaders_Values="['ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4','1','Mozilla/5.0 (Linux; Android 5.0.2; SM-A300F Build/LRX22G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36','redirect.appmetrica.yandex.com','gzip, deflate, sdch','http://www.yandex.ru/','yandexuid=1469860561425943192; referrer=106400:ym_tracking_id=9873201554416477753','close','text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8']",
    YandexUidRu="1469860561425943192",
    ReceiveTimeZone="0",
    UrlParameters_Keys="['creative','geo','target']",
    StartTime="2016-04-10 22:12:11",
    EventType="EVENT_AD_CLICK",
    Manufacturer="Samsung",
)
testdata_mobile_tracking_log.add_row(
    _date="2016-04-10",
    UUID="bb35f489f1ceccf89579a11f92377746",
    RegionID="213",
    Model="Galaxy A3",
    APIKey="106400",
    AppPlatform="android",
    StartTimestamp="1460315530",
    DeviceIDHash="5575590620955852110",
    ClickTimestamp="1460315530",
    timestamp="2016-04-10 19:12:51",
    UrlParameters_Values="['dzen-couple-t2','ru','neYB']",
    UserAgent="Mozilla/5.0 (Linux; Android 5.0.2; SM-A300F Build/LRX22G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36",
    ClientIP="::ffff:109.173.43.153",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:67659:",
    ReceiveTimestamp="1460315530",
    source_uri="prt://mobmetrika@mtcalclog03e.yandex.ru/opt/statbox_export_mobile/mobile-clicks.log",
    DeviceType="PHONE",
    OSVersion="5.0.2",
    iso_eventtime="2016-04-10 22:12:51",
    YandexUidRu="1469860561425943192",
    ReceiveTimeZone="0",
    UrlParameters_Keys="['creative','geo','target']",
    StartTime="2016-04-10 22:12:10",
    EventType="EVENT_AD_CLICK",
    Manufacturer="Samsung",
)
testdata_mobile_tracking_log.add_row(
    _date="2016-04-10",
    UUID="bb35f489f1ceccf89579a11f92377746",
    RequestHeaders_Names="['Accept-Language','Upgrade-Insecure-Requests','User-Agent','Host','Accept-Encoding','Referer','Cookie','Connection','Accept']",
    ClickReceiveTimestamp="1460315531",
    RegionID="213",
    ClickDateTime="2016-04-10 22:12:11",
    Model="Galaxy A3",
    APIKey="106400",
    AppPlatform="android",
    StartTimestamp="1460315531",
    DeviceIDHash="1955956076350024385",
    ClickTimestamp="1460315531",
    timestamp="2016-04-10 19:12:51",
    UrlParameters_Values="['dzen-couple-t2','ru','neYB']",
    UserAgent="Mozilla/5.0 (Linux; Android 5.0.2; SM-A300F Build/LRX22G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36",
    ClientIP="::ffff:109.173.43.153",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:67659:",
    ReceiveTimestamp="1460315531",
    source_uri="prt://mobmetrika@mtcalclog03e.yandex.ru/opt/statbox_export_mobile/mobile-clicks.log",
    DeviceType="PHONE",
    UniqueClickID="11378786936113604095",
    YMTrackingID="11378786936113604095",
    OSVersion="5.0.2",
    iso_eventtime="2016-04-10 22:12:51",
    RequestHeaders_Values="['ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4','1','Mozilla/5.0 (Linux; Android 5.0.2; SM-A300F Build/LRX22G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.83 Mobile Safari/537.36','redirect.appmetrica.yandex.com','gzip, deflate, sdch','http://www.yandex.ru/','yandexuid=1469860561425943192; referrer=106400:ym_tracking_id=9873201554416477753','close','text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8']",
    YandexUidRu="1469860561425943192",
    ReceiveTimeZone="0",
    UrlParameters_Keys="['creative','geo','target']",
    StartTime="2016-04-10 22:12:11",
    EventType="EVENT_AD_CLICK",
    Manufacturer="Samsung",
)
testdata_mobile_tracking_log.add_row(
    _date="2016-04-11",
    UUID="021979592b36831fde8070bbb557b329",
    RegionID="43",
    Model="iPad",
    APIKey="29733",
    AppPlatform="iOS",
    StartTimestamp="1460408301",
    DeviceIDHash="18053039941442816448",
    ClickTimestamp="1460408301",
    timestamp="2016-04-11 20:58:33",
    UrlParameters_Values="['click','ru.yandex.mail']",
    UserAgent="Mozilla/5.0 (iPad; CPU OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13E238 Safari/601.1",
    ClientIP="::ffff:94.180.135.227",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:77394:",
    ReceiveTimestamp="1460408301",
    DeviceType="TABLET",
    OSVersion="9.3.1",
    iso_eventtime="2016-04-11 23:58:33",
    YandexUidRu="8265280551449681819",
    UrlParameters_Keys="['action','app_id']",
    StartTime="2016-04-11 23:58:21",
    Manufacturer="Apple",
)
testdata_mobile_tracking_log.add_row(
    _date="2016-04-11",
    UUID="021979592b36831fde8070bbb557b329",
    RegionID="43",
    Model="iPad",
    APIKey="29733",
    AppPlatform="iOS",
    StartTimestamp="1460408301",
    DeviceIDHash="18053039941442816448",
    ClickTimestamp="1460408301",
    timestamp="2016-04-11 20:58:33",
    UrlParameters_Values="['click','ru.yandex.mail']",
    UserAgent="Mozilla/5.0 (iPad; CPU OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13E238 Safari/601.1",
    ClientIP="::ffff:94.180.135.227",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:77394:",
    ReceiveTimestamp="1460408301",
    DeviceType="TABLET",
    OSVersion="9.3.1",
    iso_eventtime="2016-04-11 23:58:33",
    YandexUidRu="8265280551449681819",
    UrlParameters_Keys="['action','app_id']",
    StartTime="2016-04-11 23:58:21",
    Manufacturer="Apple",
)
testdata_mobile_tracking_log.add_row(
    _date="2016-04-11",
    UUID="021979592b36831fde8070bbb557b329",
    RegionID="43",
    Model="iPad",
    APIKey="29733",
    AppPlatform="iOS",
    StartTimestamp="1460408301",
    DeviceIDHash="18053039941442816448",
    ClickTimestamp="1460408301",
    timestamp="2016-04-11 20:58:33",
    UrlParameters_Values="['click','ru.yandex.mail']",
    UserAgent="Mozilla/5.0 (iPad; CPU OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13E238 Safari/601.1",
    ClientIP="::ffff:94.180.135.227",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:77394:",
    ReceiveTimestamp="1460408301",
    DeviceType="TABLET",
    UniqueClickID="1429741381819109728",
    YMTrackingID="1429741381819109728",
    OSVersion="9.3.1",
    iso_eventtime="2016-04-11 23:58:33",
    YandexUidRu="8265280551449681819",
    UrlParameters_Keys="['action','app_id']",
    StartTime="2016-04-11 23:58:21",
    Manufacturer="Apple",
)
testdata_mobile_tracking_log.add_row(
    _date="2016-04-11",
    UUID="bb35f489f1ceccf89579a11f92377746",
    RegionID="43",
    Model="iPad",
    APIKey="29733",
    AppPlatform="iOS",
    StartTimestamp="1460408301",
    DeviceIDHash="18053039941442816448",
    ClickTimestamp="1460408301",
    timestamp="2016-04-11 20:58:33",
    UrlParameters_Values="['click','ru.yandex.mail']",
    UserAgent="Mozilla/5.0 (iPad; CPU OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13E238 Safari/601.1",
    ClientIP="::ffff:94.180.135.227",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:77394:",
    ReceiveTimestamp="1460408301",
    DeviceType="TABLET",
    OSVersion="9.3.1",
    iso_eventtime="2016-04-11 23:58:33",
    YandexUidRu="8265280551449681819",
    UrlParameters_Keys="['action','app_id']",
    StartTime="2016-04-11 23:58:21",
    Manufacturer="Apple",
)
testdata_mobile_tracking_log.add_row(
    _date="2016-04-11",
    UUID="bb35f489f1ceccf89579a11f92377746",
    RegionID="43",
    Model="iPad",
    APIKey="29733",
    AppPlatform="iOS",
    StartTimestamp="1460408301",
    DeviceIDHash="18053039941442816448",
    ClickTimestamp="1460408301",
    timestamp="2016-04-11 20:58:33",
    UrlParameters_Values="['click','ru.yandex.mail']",
    UserAgent="Mozilla/5.0 (iPad; CPU OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13E238 Safari/601.1",
    ClientIP="::ffff:94.180.135.227",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:77394:",
    ReceiveTimestamp="1460408301",
    DeviceType="TABLET",
    UniqueClickID="1429741381819109728",
    YMTrackingID="1429741381819109728",
    OSVersion="9.3.1",
    iso_eventtime="2016-04-11 23:58:33",
    YandexUidRu="8265280551449681819",
    UrlParameters_Keys="['action','app_id']",
    StartTime="2016-04-11 23:58:21",
    Manufacturer="Apple",
)


testdata_mobile_tracking_private_log = BaseLog(
    path="//home/logfeller/logs/metrika-mobile-install-private-log/1d",
    date="2016-04-11",
    default_data=MOBILE_TRACKING_PRIVATE_LOG,
)
testdata_mobile_tracking_private_log.add_row(
    UUID="3e2b6871964bb53513eacac621970e9b",
    RegionID="213",
    Model="iPad",
    APIKey="29733",
    AppPlatform="iOS",
    StartTimestamp="1460223976",
    DeviceIDHash="9493461388394930183",
    ClickTimestamp="1460223976",
    timestamp="2016-04-09 17:46:56",
    UrlParameters_Values="['click','ru.yandex.mail']",
    UserAgent="Mozilla/5.0 (iPad; CPU OS 8_4_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12H321",
    ClientIP="::ffff:85.31.113.126",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:57980",
    ReceiveTimestamp="1460223976",
    DeviceType="TABLET",
    OSVersion="8.4.1",
    iso_eventtime="2016-04-09 20:46:56",
    YandexUidRu="8622478831441691934",
    UrlParameters_Keys="['action','app_id']",
    StartTime="2016-04-09 20:46:16",
    Manufacturer="Apple",
)
testdata_mobile_tracking_private_log.add_row(
    _date="2016-04-10",
    UUID="3e2b6871964bb53513eacac621970e9b",
    RegionID="213",
    Model="iPad",
    APIKey="29733",
    AppPlatform="iOS",
    StartTimestamp="1460223976",
    DeviceIDHash="9493461388394930183",
    ClickTimestamp="1460223976",
    timestamp="2016-04-09 17:46:56",
    UrlParameters_Values="['click','ru.yandex.mail']",
    UserAgent="Mozilla/5.0 (iPad; CPU OS 8_4_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12H321",
    ClientIP="::ffff:85.31.113.126",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:57980",
    ReceiveTimestamp="1460223976",
    DeviceType="TABLET",
    OSVersion="8.4.1",
    iso_eventtime="2016-04-09 20:46:56",
    YandexUidRu="8622478831441691934",
    UrlParameters_Keys="['action','app_id']",
    StartTime="2016-04-09 20:46:16",
    Manufacturer="Apple",
)
testdata_mobile_tracking_private_log.add_row(
    _date="2016-04-09",
    UUID="3e2b6871964bb53513eacac621970e9b",
    RegionID="213",
    Model="iPad",
    APIKey="29733",
    AppPlatform="iOS",
    StartTimestamp="1460223976",
    DeviceIDHash="9493461388394930183",
    ClickTimestamp="1460223976",
    timestamp="2016-04-09 17:46:56",
    UrlParameters_Values="['click','ru.yandex.mail']",
    UserAgent="Mozilla/5.0 (iPad; CPU OS 8_4_1 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) Mobile/12H321",
    ClientIP="::ffff:85.31.113.126",
    _stbx="rt3.iva--mobmetrika--mobile-clicks-log:0:57980",
    ReceiveTimestamp="1460223976",
    DeviceType="TABLET",
    OSVersion="8.4.1",
    iso_eventtime="2016-04-09 20:46:56",
    YandexUidRu="8622478831441691934",
    UrlParameters_Keys="['action','app_id']",
    StartTime="2016-04-09 20:46:16",
    Manufacturer="Apple",
)


testdata_oauth_log = BaseLog(path="//home/logfeller/logs/oauth-log/1d", date="2016-04-09", default_data=OAUTH_LOG)
testdata_oauth_log.add_row(
    uid="92742990",
    unixtime="1460196263",
    create_time="2016-02-09 21:51:35",
    source_uri="prt://oauth@oauth-nb4.yandex.net/var/log/yandex/oauth-server/statbox.log",
    _stbx="rt3.fol--oauth--oauth-log:1:164887069:",
    timestamp="2016-04-09 13:04:23",
    expire_time="2017-04-07 01:32:56",
    device_id="4ee6905fdb60242e183809c98969e12b",
    iso_eventtime="2016-04-09 13:04:23",
)
testdata_oauth_log.add_row(
    uid="92742990",
    unixtime="1460196263",
    create_time="2016-02-09 21:51:35",
    source_uri="prt://oauth@oauth-nb4.yandex.net/var/log/yandex/oauth-server/statbox.log",
    _stbx="rt3.fol--oauth--oauth-log:1:164887069:",
    timestamp="2016-04-09 13:04:23",
    expire_time="2017-04-07 01:32:56",
    device_id="4ee6905fdb60242e183809c98969e12b",
    iso_eventtime="2016-04-09 13:04:23",
)
testdata_oauth_log.add_row(
    uid="11121",
    unixtime="1460196264",
    app_id="ru.yandex.taxi",
    mode="issue_token",
    create_time="2016-04-09 08:53:11",
    iso_eventtime="2016-04-09 13:04:24",
    uuid="bb35f489f1ceccf89579a11f92377746",
    app_platform="Android 4.4.2 (REL)",
    user_ip="::ffff:213.87.135.117",
    _stbx="rt3.fol--oauth--oauth-log:1:164887079:",
    token_id="224453705",
    device_id="001c18cba26966f27792ccc532358d42",
    deviceid="001c18cba26966f27792ccc532358d42",
    scopes="mobile:all,yataxi:pay,yataxi:read,yataxi:write",
    timestamp="2016-04-09 13:04:24",
    source_uri="prt://oauth@oauth-nb4.yandex.net/var/log/yandex/oauth-server/statbox.log",
    client_id="22d873ed2ea14b93a36a0f5a07026458",
    expire_time="2017-04-09 13:04:17",
    manufacturer="Philips",
    device_name="Philips V387",
    issue_time="2016-04-09 13:04:17",
    request_id="465acd27fe7123913c68ba438039eb7d",
    model="Philips V387",
)
testdata_oauth_log.add_row(
    uid="92742990",
    unixtime="1460196263",
    create_time="2016-02-09 21:51:35",
    source_uri="prt://oauth@oauth-nb4.yandex.net/var/log/yandex/oauth-server/statbox.log",
    _stbx="rt3.fol--oauth--oauth-log:1:164887069:3",
    timestamp="2016-04-09 13:04:23",
    expire_time="2017-04-07 01:32:56",
    device_id="4ee6905fdb60242e183809c98969e12b",
    iso_eventtime="2016-04-09 13:04:23",
)
testdata_oauth_log.add_row(
    uid="11121",
    unixtime="1460196264",
    app_id="ru.yandex.taxi",
    mode="issue_token",
    create_time="2016-04-09 08:53:11",
    iso_eventtime="2016-04-09 13:04:24",
    uuid="bb35f489f1ceccf89579a11f92377746",
    app_platform="Android 4.4.2 (REL)",
    user_ip="::ffff:213.87.135.117",
    _stbx="rt3.fol--oauth--oauth-log:1:164887079:",
    token_id="224453705",
    device_id="001c18cba26966f27792ccc532358d42",
    deviceid="001c18cba26966f27792ccc532358d42",
    scopes="mobile:all,yataxi:pay,yataxi:read,yataxi:write",
    timestamp="2016-04-09 13:04:24",
    source_uri="prt://oauth@oauth-nb4.yandex.net/var/log/yandex/oauth-server/statbox.log",
    client_id="22d873ed2ea14b93a36a0f5a07026458",
    expire_time="2017-04-09 13:04:17",
    manufacturer="Philips",
    device_name="Philips V387",
    issue_time="2016-04-09 13:04:17",
    request_id="465acd27fe7123913c68ba438039eb7d",
    model="Philips V387",
)

testdata_oauth_log.add_row(
    _date="2016-04-10",
    uid="11121",
    unixtime="1460300844",
    app_id="ru.yandex.taxi",
    create_time="2016-04-09 08:53:11",
    iso_eventtime="2016-04-10 18:07:24",
    uuid="bb35f489f1ceccf89579a11f92377746",
    app_platform="Android 4.4.2 (REL)",
    _stbx="rt3.fol--oauth--oauth-log:1:165449797:",
    device_id="001c18cba26966f27792ccc532358d42",
    deviceid="001c18cba26966f27792ccc532358d42",
    timestamp="2016-04-10 18:07:24",
    source_uri="prt://oauth@oauth-na5.yandex.net/var/log/yandex/oauth-server/statbox.log",
    expire_time="2017-04-10 18:06:25",
    manufacturer="Philips",
    model="Philips V387",
)
testdata_oauth_log.add_row(
    _date="2016-04-10",
    uid="11121",
    unixtime="1460300844",
    app_id="ru.yandex.taxi",
    create_time="2016-04-09 08:53:11",
    iso_eventtime="2016-04-10 18:07:24",
    uuid="bb35f489f1ceccf89579a11f92377746",
    app_platform="Android 4.4.2 (REL)",
    _stbx="rt3.fol--oauth--oauth-log:1:165449797:",
    device_id="001c18cba26966f27792ccc532358d42",
    deviceid="001c18cba26966f27792ccc532358d42",
    timestamp="2016-04-10 18:07:24",
    source_uri="prt://oauth@oauth-na5.yandex.net/var/log/yandex/oauth-server/statbox.log",
    expire_time="2017-04-10 18:06:25",
    manufacturer="Philips",
    model="Philips V387",
)
testdata_oauth_log.add_row(
    _date="2016-04-10",
    uid="11121",
    unixtime="1460300844",
    app_id="ru.yandex.taxi",
    create_time="2016-04-09 08:53:11",
    consumer_ip="2a02:6b8:0:c05::105a",
    iso_eventtime="2016-04-10 18:07:24",
    uuid="bb35f489f1ceccf89579a11f92377746",
    app_platform="Android 4.4.2 (REL)",
    _stbx="rt3.fol--oauth--oauth-log:1:165449797:",
    device_id="001c18cba26966f27792ccc532358d42",
    deviceid="001c18cba26966f27792ccc532358d42",
    timestamp="2016-04-10 18:07:24",
    source_uri="prt://oauth@oauth-na5.yandex.net/var/log/yandex/oauth-server/statbox.log",
    expire_time="2017-04-10 18:06:25",
    manufacturer="Philips",
    request_id="840c43fcb990972a55c426c65e5b3aab",
    model="Philips V387",
)
testdata_oauth_log.add_row(
    _date="2016-04-10",
    uid="11121",
    unixtime="1460300844",
    app_id="ru.yandex.taxi",
    create_time="2016-04-09 08:53:11",
    iso_eventtime="2016-04-10 18:07:24",
    uuid="bb35f489f1ceccf89579a11f92377746",
    app_platform="Android 4.4.2 (REL)",
    _stbx="rt3.fol--oauth--oauth-log:1:165449797:",
    device_id="001c18cba26966f27792ccc532358d42",
    deviceid="001c18cba26966f27792ccc532358d42",
    timestamp="2016-04-10 18:07:24",
    source_uri="prt://oauth@oauth-na5.yandex.net/var/log/yandex/oauth-server/statbox.log",
    expire_time="2017-04-10 18:06:25",
    manufacturer="Philips",
    model="Philips V387",
)
testdata_oauth_log.add_row(
    _date="2016-04-10",
    uid="11121",
    unixtime="1460300844",
    app_id="ru.yandex.taxi",
    create_time="2016-04-09 08:53:11",
    consumer_ip="2a02:6b8:0:c05::105a",
    iso_eventtime="2016-04-10 18:07:24",
    uuid="bb35f489f1ceccf89579a11f92377746",
    app_platform="Android 4.4.2 (REL)",
    _stbx="rt3.fol--oauth--oauth-log:1:165449797:",
    device_id="001c18cba26966f27792ccc532358d42",
    deviceid="001c18cba26966f27792ccc532358d42",
    timestamp="2016-04-10 18:07:24",
    source_uri="prt://oauth@oauth-na5.yandex.net/var/log/yandex/oauth-server/statbox.log",
    expire_time="2017-04-10 18:06:25",
    manufacturer="Philips",
    request_id="840c43fcb990972a55c426c65e5b3aab",
    model="Philips V387",
)

testdata_oauth_log.add_row(
    _date="2016-04-11",
    unixtime="1460349281",
    iso_eventtime="2016-04-11 07:34:41",
    _stbx="rt3.iva--oauth--oauth-log:0:166691226:",
    timestamp="2016-04-11 07:34:41",
    source_uri="prt://oauth@oauth-i4.yandex.net/var/log/yandex/oauth-server/statbox.log",
    expire_time="2017-04-11 07:34:36",
)
testdata_oauth_log.add_row(
    _date="2016-04-11",
    unixtime="1460349281",
    iso_eventtime="2016-04-11 07:34:41",
    _stbx="rt3.iva--oauth--oauth-log:0:166691226:",
    timestamp="2016-04-11 07:34:41",
    source_uri="prt://oauth@oauth-i4.yandex.net/var/log/yandex/oauth-server/statbox.log",
    expire_time="2017-04-11 07:34:36",
)
testdata_oauth_log.add_row(
    _date="2016-04-11",
    unixtime="1460348228",
    consumer_ip="2a02:6b8:0:c05::105a",
    iso_eventtime="2016-04-11 07:17:08",
    _stbx="rt3.fol--oauth--oauth-log:0:155588658:",
    timestamp="2016-04-11 07:17:08",
    source_uri="prt://oauth@oauth-na3.yandex.net/var/log/yandex/oauth-server/statbox.log",
    expire_time="2017-04-11 07:16:11",
    issue_time="2016-04-11 07:16:11",
    request_id="c885087d4cb0c0418db8707c8dc7c8f6",
)
testdata_oauth_log.add_row(
    _date="2016-04-11",
    unixtime="1460349281",
    iso_eventtime="2016-04-11 07:34:41",
    _stbx="rt3.iva--oauth--oauth-log:0:166691226:",
    timestamp="2016-04-11 07:34:41",
    source_uri="prt://oauth@oauth-i4.yandex.net/var/log/yandex/oauth-server/statbox.log",
    expire_time="2017-04-11 07:34:36",
)
testdata_oauth_log.add_row(
    _date="2016-04-11",
    unixtime="1460348228",
    consumer_ip="2a02:6b8:0:c05::105a",
    iso_eventtime="2016-04-11 07:17:08",
    _stbx="rt3.fol--oauth--oauth-log:0:155588658:",
    timestamp="2016-04-11 07:17:08",
    source_uri="prt://oauth@oauth-na3.yandex.net/var/log/yandex/oauth-server/statbox.log",
    expire_time="2017-04-11 07:16:11",
    issue_time="2016-04-11 07:16:11",
    request_id="c885087d4cb0c0418db8707c8dc7c8f6",
)
testdata_oauth_log.add_row(
    _date="2016-04-11",
    unixtime="1460348229",
    consumer_ip="2a02:6b8:0:c05::105a",
    iso_eventtime="2016-04-11 07:17:09",
    _stbx="rt3.fol--oauth--oauth-log:0:155588666:",
    timestamp="2016-04-11 07:17:09",
    source_uri="prt://oauth@oauth-na3.yandex.net/var/log/yandex/oauth-server/statbox.log",
    expire_time="2017-04-11 07:16:11",
    issue_time="2016-04-11 07:16:11",
    request_id="9eb5877bbdd40795ed9ed87c6037b6d8",
)


testdata_redir_log = ComplexParametersLog(
    path="//home/logfeller/logs/common-redir-log/1d", date="2016-04-09", default_data=REDIR_LOG
)
testdata_redir_log.add_row(
    key="prt://redir@sas1-2631.search.yandex.net/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-09 18:35:45",
    value={
        u"ver": u"4875",
        u"HTTP_REFERER": u"https://yandex.ru/search/?lr=39&clid=2163430&msid=22882.7835.1460214250.85039&text=%D0%BA%D0%B8%D0%BD%D0%BE%D0%B3%D0%BE&suggest_reqid=312576068144429607843208967368320&csg=2123%2C3126%2C4%2C5%2C1%2C0%2C0&uuid=202e825ff99eb18a6e586260a5635eab",
        u"url": u"//yandex.ru/",
        u"unixtime": u"1460216145",
        u"ids": u"23421,23471,23095,23258,22688,23176,23574,23668,23644,23084,10326,23280",
        u"_stbx": u"rt3.sas--redir--redir-log:10:163782180:",
        u"path": u"690.781.2033",
        u"reg": u"39",
    },
)
testdata_redir_log.add_row(
    key="prt://redir@sas1-2631.search.yandex.net/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-09 18:35:45",
    value={
        u"ver": u"4875",
        u"HTTP_REFERER": u"https://yandex.ru/search/?lr=39&clid=2163430&msid=22882.7835.1460214250.85039&text=%D0%BA%D0%B8%D0%BD%D0%BE%D0%B3%D0%BE&suggest_reqid=312576068144429607843208967368320&csg=2123%2C3126%2C4%2C5%2C1%2C0%2C0&uuid=bf62d1b137388c21ddb64090dd02c94c",
        u"url": u"//yandex.ru/",
        u"unixtime": u"1460216145",
        u"ids": u"23421,23471,23095,23258,22688,23176,23574,23668,23644,23084,10326,23280",
        u"_stbx": u"rt3.sas--redir--redir-log:10:163782180:",
        u"path": u"690.781.2033",
        u"reg": u"39",
    },
)
testdata_redir_log.add_row(
    key="prt://redir@sas1-4857.search.yandex.net/usr/local/www/logs/current-redir-clickdaemon-18101 2016-04-09 11:11:51",
    value={
        u"ver": u"4875",
        u"mc": u"0",
        u"HTTP_REFERER": u"https://yandex.ru/search/?from=chromesearch&clid=2196598&text=%D0%B0%D0%BB%D0%B8%D1%8D%D0%BA%D1%81%D0%BF%D1%80%D0%B5%D1%81%D1%81&redircnt=1460189502.1",
        u"cts": u"1460189489412",
        u"url": u"http://ru.aliexpress.com/",
        u"unixtime": u"1460189511",
        u"vars": u"-post=bno,-main=yaca,84=85,-source=web",
        u"ids": u"23668,23644,23084,10326,23280",
        u"reqid": u"1460189503336646-5409348811413397083241496-sas1-2711",
        u"at": u"4",
        u"path": u"80.22.82",
        u"keyno": u"0",
        u"slots": u"23421,0,35;23471,0,75;23095,0,85;23408,0,56;23677,0,44;23176,0,91;23574,0,65;23063,0,97;23668,0,31;23644,0,65;23084,0,64;10326,0,84;23280,0,73",
        u"reg": u"39",
        u"_stbx": u"rt3.sas--redir--redir-log:1:167963449:",
    },
)
testdata_redir_log.add_row(
    key="prt://redir@sas1-2631.search.yandex.net/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-09 18:35:45",
    value={
        u"ver": u"4875",
        u"HTTP_REFERER": u"https://yandex.ru/search/?lr=39&clid=2163430&msid=22882.7835.1460214250.85039&text=%D0%BA%D0%B8%D0%BD%D0%BE%D0%B3%D0%BE&suggest_reqid=312576068144429607843208967368320&csg=2123%2C3126%2C4%2C5%2C1%2C0%2C0",
        u"url": u"//yandex.ru/",
        u"unixtime": u"1460216145",
        u"ids": u"23421,23471,23095,23258,22688,23176,23574,23668,23644,23084,10326,23280",
        u"_stbx": u"rt3.sas--redir--redir-log:10:163782180:",
        u"path": u"690.781.2033",
        u"reg": u"39",
    },
)
testdata_redir_log.add_row(
    key="prt://redir@sas1-4857.search.yandex.net/usr/local/www/logs/current-redir-clickdaemon-18101 2016-04-09 11:11:51",
    value={
        u"ver": u"4875",
        u"mc": u"0",
        u"HTTP_REFERER": u"https://yandex.ru/search/?from=chromesearch&clid=2196598&text=%D0%B0%D0%BB%D0%B8%D1%8D%D0%BA%D1%81%D0%BF%D1%80%D0%B5%D1%81%D1%81&redircnt=1460189502.1",
        u"cts": u"1460189489412",
        u"url": u"http://ru.aliexpress.com/",
        u"unixtime": u"1460189511",
        u"vars": u"-post=bno,-main=yaca,84=85,-source=web",
        u"ids": u"23668,23644,23084,10326,23280",
        u"reqid": u"1460189503336646-5409348811413397083241496-sas1-2711",
        u"at": u"4",
        u"path": u"80.22.82",
        u"keyno": u"0",
        u"slots": u"23421,0,35;23471,0,75;23095,0,85;23408,0,56;23677,0,44;23176,0,91;23574,0,65;23063,0,97;23668,0,31;23644,0,65;23084,0,64;10326,0,84;23280,0,73",
        u"reg": u"39",
        u"_stbx": u"rt3.sas--redir--redir-log:1:167963449:",
    },
)

testdata_redir_log.add_row(
    _date="2016-04-10",
    key="prt://redir@sas1-3801.search.yandex.net/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-10 10:31:26",
    value={
        u"HTTP_REFERER": u"http://www.horseclubs.ru/konyushni-moskvy/?lr=39&clid=2163430&uuid=202e825ff99eb18a6e586260a5635eab",
        u"cid": u"72717",
        u"url": u"https://maps.yandex.ru/",
        u"ip": u"195.16.111.5",
        u"yandexuid": u"4540183161455570421",
        u"rnd": u"0.7699978421280393",
        u"_stbx": u"rt3.fol--redir--redir-log:",
        u"unixtime": u"1460273486",
        u"path": u"2_0.326.596",
    },
)
testdata_redir_log.add_row(
    _date="2016-04-10",
    key="prt://redir@sas1-3801.search.yandex.net/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-10 10:31:26",
    value={
        u"HTTP_REFERER": u"http://www.horseclubs.ru/konyushni-moskvy/?lr=39&clid=2163430&uuid=bf62d1b137388c21ddb64090dd02c94c",
        u"cid": u"72717",
        u"url": u"https://maps.yandex.ru/",
        u"ip": u"195.16.111.5",
        u"yandexuid": u"4540183161455570421",
        u"rnd": u"0.7699978421280393",
        u"_stbx": u"rt3.fol--redir--redir-log:",
        u"unixtime": u"1460273486",
        u"path": u"2_0.326.596",
    },
)
testdata_redir_log.add_row(
    _date="2016-04-10",
    key="prt://redir@imgs28-111.yandex.ru/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-10 13:35:31",
    value={
        u"HTTP_REFERER": u"https://yandex.ru/search/?text=aa&lr=35&uuid=202e825ff99eb18a6e586260a5635eab",
        u"vars": u"-reqid=1460282091738633-796277-vsearch24-44-ATOMS-distr_serp,-showid=040217301460282091738633796277,-bannerid=pf000208,-score=11490,-product=home,-eventtype=show",
        u"cid": u"198",
        u"url": u"data=url",
        u"ip": u"62.183.125.140",
        u"yandexuid": u"7368530241456335273",
        u"pid": u"198",
        u"rnd": u"1460284525839",
        u"uah": u"1287692101",
        u"_stbx": u"rt3.fol--redir--redir-log:",
        u"unixtime": u"1460284531",
        u"path": u"tech.portal-ads.promofooter",
    },
)
testdata_redir_log.add_row(
    _date="2016-04-10",
    key="prt://redir@sas1-3801.search.yandex.net/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-10 10:31:26",
    value={
        u"HTTP_REFERER": u"http://www.horseclubs.ru/konyushni-moskvy",
        u"cid": u"72717",
        u"url": u"https://maps.yandex.ru/",
        u"ip": u"195.16.111.5",
        u"yandexuid": u"4540183161455570421",
        u"rnd": u"0.7699978421280393",
        u"_stbx": u"rt3.fol--redir--redir-log:",
        u"unixtime": u"1460273486",
        u"path": u"2_0.326.596",
    },
)
testdata_redir_log.add_row(
    _date="2016-04-10",
    key="prt://redir@imgs28-111.yandex.ru/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-10 13:35:31",
    value={
        u"HTTP_REFERER": u"https://yandex.ru/search/?text=aalr=35&uuid=13cc284f279bd1702a1adb429ba68298",
        u"vars": u"-reqid=1460282091738633-796277-vsearch24-44-ATOMS-distr_serp,-showid=040217301460282091738633796277,-bannerid=pf000208,-score=11490,-product=home,-eventtype=show",
        u"cid": u"198",
        u"url": u"data=url",
        u"ip": u"62.183.125.140",
        u"yandexuid": u"7368530241456335273",
        u"pid": u"198",
        u"rnd": u"1460284525839",
        u"uah": u"1287692101",
        u"_stbx": u"rt3.fol--redir--redir-log:2:244626296:",
        u"unixtime": u"1460284531",
        u"path": u"tech.portal-ads.promofooter",
    },
)

testdata_redir_log.add_row(
    _date="2016-04-11",
    key="prt://redir@imgs28-111.yandex.ru/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-11 11:15:27",
    value={
        u"HTTP_REFERER": u"http://yandex.ru/search/touch/?text=bb&clid=2160746&brorich=0&lr=193&uuid=202e825ff99eb18a6e586260a5635eab",
        u"cid": u"72202",
        u"unixtime": u"1460362527",
        u"_stbx": u"rt3.fol--redir--redir-log:15:245540942:",
        u"ip": u"128.70.8.104",
        u"path": u"690.1033",
    },
)
testdata_redir_log.add_row(
    _date="2016-04-11",
    key="prt://redir@imgs28-111.yandex.ru/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-11 11:15:27",
    value={
        u"HTTP_REFERER": u"http://yandex.ru/search/touch/?text=%D0%BB%D0%BE%D0%B1%D0%B8%D0%BE%20%D0%B8%D0%B7%20%D1%84%D0%B0%D1%81%D0%BE%D0%BB%D0%B8%20%D1%80%D0%B5%D1%86%D0%B5%D0%BF%D1%82%D1%8B%20%D1%81%20%D1%84%D0%BE%D1%82%D0%BE&clid=2160746&brorich=0&lr=193&uuid=bf62d1b137388c21ddb64090dd02c94c",
        u"cid": u"72202",
        u"unixtime": u"1460362527",
        u"_stbx": u"rt3.fol--redir--redir-log:15:245540942:",
        u"ip": u"128.70.8.104",
        u"path": u"690.1033",
    },
)
testdata_redir_log.add_row(
    _date="2016-04-11",
    key="prt://redir@imgs28-111.yandex.ru/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-11 23:05:09",
    value={
        u"HTTP_REFERER": u"https://yandex.ru/images/touch/search?source=wiz&p=0&text=%D1%82%D0%B0%D1%80%D1%85%D0%B0%D0%BD%D0%BA%D1%83%D1%82%20%D0%BA%D1%80%D1%8B%D0%BC%20%D1%84%D0%BE%D1%82%D0%BE&img_url=http%3A%2F%2Fcs624927.vk.me%2Fv624927980%2F3ab7a%2FgG8Hqf_mWjc.jpg&pos=6&rpt=simage&uuid=202e825ff99eb18a6e586260a5635eab",
        u"vars": u"143=28.277.584.153.1201,287=193,1042=Mozilla%2F5.0%20(iPhone%3B%20CPU%20iPhone%20OS%209_3_1%20like%20Mac%20OS%20X)%20AppleWebKit%2F601.1.46%20(KHTML%2C%20like%20Gecko)%20YaBrowser%2F16.2.0.6967.10%20Mobile%2F13E238%20Safari%2F601.1,1201.906=612,1201.789=426",
        u"cid": u"72202",
        u"ip": u"128.70.8.104",
        u"serpid": u"oo3kKbKSAqgLW6CzPB-ktA",
        u"reqid": u"undefined",
        u"_stbx": u"rt3.fol--redir--redir-log:14:250982586:",
        u"unixtime": u"1460405109",
        u"path": u"690.1201",
    },
)
testdata_redir_log.add_row(
    _date="2016-04-11",
    key="prt://redir@imgs28-111.yandex.ru/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-11 11:15:27",
    value={
        u"HTTP_REFERER": u"http://yandex.ru/search/touch/?text=%D0%BB%D0%BE%D0%B1%D0%B8%D0%BE%20%D0%B8%D0%B7%20%D1%84%D0%B0%D1%81%D0%BE%D0%BB%D0%B8%20%D1%80%D0%B5%D1%86%D0%B5%D0%BF%D1%82%D1%8B%20%D1%81%20%D1%84%D0%BE%D1%82%D0%BE&clid=2160746&brorich=0&lr=193&uuid=13cc284f279bd1702a1adb429ba68298",
        u"cid": u"72202",
        u"unixtime": u"1460362527",
        u"_stbx": u"rt3.fol--redir--redir-log:15:245540942:",
        u"ip": u"128.70.8.104",
        u"path": u"690.1033",
    },
)
testdata_redir_log.add_row(
    _date="2016-04-11",
    key="prt://redir@imgs28-111.yandex.ru/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-11 23:05:09",
    value={
        u"HTTP_REFERER": u"https://yandex.ru/images/touch/search?source=wiz&p=0&text=%D1%82%D0%B0%D1%80%D1%85%D0%B0%D0%BD%D0%BA%D1%83%D1%82%20%D0%BA%D1%80%D1%8B%D0%BC%20%D1%84%D0%BE%D1%82%D0%BE&img_url=http%3A%2F%2Fcs624927.vk.me%2Fv624927980%2F3ab7a%2FgG8Hqf_mWjc.jpg&pos=6&rpt=simage&uuid=202e825ff99eb18a6e586260a5635eab",
        u"vars": u"143=28.277.584.153.1201,287=193,1042=Mozilla%2F5.0%20(iPhone%3B%20CPU%20iPhone%20OS%209_3_1%20like%20Mac%20OS%20X)%20AppleWebKit%2F601.1.46%20(KHTML%2C%20like%20Gecko)%20YaBrowser%2F16.2.0.6967.10%20Mobile%2F13E238%20Safari%2F601.1,1201.906=612,1201.789=426",
        u"cid": u"72202",
        u"ip": u"128.70.8.104",
        u"serpid": u"oo3kKbKSAqgLW6CzPB-ktA",
        u"reqid": u"undefined",
        u"_stbx": u"rt3.fol--redir--redir-log:14:250982586:",
        u"unixtime": u"1460405109",
        u"path": u"690.1201",
    },
)
testdata_redir_log.add_row(
    _date="2016-04-11",
    key="prt://redir@imgs28-111.yandex.ru/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-11 23:06:09",
    value={
        u"HTTP_REFERER": u"https://yandex.ru/images/touch/search?source=wiz&p=0&text=%D1%82%D0%B0%D1%80%D1%85%D0%B0%D0%BD%D0%BA%D1%83%D1%82%20%D0%BA%D1%80%D1%8B%D0%BC%20%D1%84%D0%BE%D1%82%D0%BE&img_url=http%3A%2F%2Fcs624927.vk.me%2Fv624927980%2F3ab7a%2FgG8Hqf_mWjc.jpg&pos=6&rpt=simage&uuid=202e825ff99eb18a6e586260a5635eab",
        u"yandexuid": u"3658570541484926132",
        u"vars": u"143=28.277.584.153.1201,287=193,1042=Mozilla%2F5.0%20(Windows%20NT%206.1%3B%20Win64%3B%20x64)%20AppleWebKit%2F537.36%20(KHTML%2C%20like%20Gecko)%20Chrome%2F61.0.3163.100%20Safari%2F537.36,1201.906=612,1201.789=426",
        u"cid": u"72202",
        u"ip": u"128.70.8.104",
        u"serpid": u"oo3kKbKSAqgLW6CzPB-ktA",
        u"reqid": u"undefined",
        u"_stbx": u"rt3.fol--redir--redir-log:14:250982586:",
        u"unixtime": u"1460405109",
        u"path": u"690.1201",
    },
)

# test_extract_uuid_from_redirlog
uuid_contain_trash = "111e825ff99eb18a6e586260a5635eab"
uuid_first_pos = "222e825ff99eb18a6e586260a5635eab"
uuid_last_pos = "333e825ff99eb18a6e586260a5635eab"
uuid_mid_pos = "444e825ff99eb18a6e586260a5635eab"
uuid_contains_hyphens = "555e825ff99eb18a6e586260a5635eab"
uuid_bad_len = "6666e825ff99eb18a6e586260a5635eab"
testdata_redir_log.add_row(
    _date="2016-04-11",
    key="prt://redir@imgs28-111.yandex.ru/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-11 11:15:27",
    value={
        u"HTTP_REFERER": u"http://yandex.ru/search/touch/?text=bb&clid=2160746&brorich=0&lr=193&uuid=%22%5C%22%5C%5C%5C%22%5C"
        + uuid_contain_trash,
        u"cid": u"72202",
        u"unixtime": u"1460362527",
        u"_stbx": u"rt3.fol--redir--redir-log:15:245540942:",
        u"ip": u"128.70.8.104",
        u"path": u"690.1033",
    },
)
testdata_redir_log.add_row(
    _date="2016-04-11",
    key="prt://redir@imgs28-111.yandex.ru/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-11 11:15:27",
    value={
        u"HTTP_REFERER": u"http://yandex.ru/search/touch/?uuid="
        + uuid_first_pos
        + "&text=bb&clid=2160746&brorich=0&lr=193",
        u"cid": u"72202",
        u"unixtime": u"1460362527",
        u"_stbx": u"rt3.fol--redir--redir-log:15:245540942:",
        u"ip": u"128.70.8.104",
        u"path": u"690.1033",
    },
)
testdata_redir_log.add_row(
    _date="2016-04-11",
    key="prt://redir@imgs28-111.yandex.ru/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-11 11:15:27",
    value={
        u"HTTP_REFERER": u"http://yandex.ru/search/touch/?text=bb&clid=2160746&brorich=0&lr=193&uuid=" + uuid_last_pos,
        u"cid": u"72202",
        u"unixtime": u"1460362527",
        u"_stbx": u"rt3.fol--redir--redir-log:15:245540942:",
        u"ip": u"128.70.8.104",
        u"path": u"690.1033",
    },
)
testdata_redir_log.add_row(
    _date="2016-04-11",
    key="prt://redir@imgs28-111.yandex.ru/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-11 11:15:27",
    value={
        u"HTTP_REFERER": u"http://yandex.ru/search/touch/?text=bb&clid=2160746&uuid=" + uuid_mid_pos + "&lr=193",
        u"cid": u"72202",
        u"unixtime": u"1460362527",
        u"_stbx": u"rt3.fol--redir--redir-log:15:245540942:",
        u"ip": u"128.70.8.104",
        u"path": u"690.1033",
    },
)
testdata_redir_log.add_row(
    _date="2016-04-11",
    key="prt://redir@imgs28-111.yandex.ru/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-11 11:15:27",
    value={
        u"HTTP_REFERER": u"http://yandex.ru/search/touch/?text=bb&clid=2160746&brorich=0&lr=193&uuid=555e825ff-99eb18a6-e586260-a563-5eab",
        u"cid": u"72202",
        u"unixtime": u"1460362527",
        u"_stbx": u"rt3.fol--redir--redir-log:15:245540942:",
        u"ip": u"128.70.8.104",
        u"path": u"690.1033",
    },
)
testdata_redir_log.add_row(
    _date="2016-04-11",
    key="prt://redir@imgs28-111.yandex.ru/usr/local/www/logs/current-redir-clickdaemon-18100 2016-04-11 11:15:27",
    value={
        u"HTTP_REFERER": u"http://yandex.ru/search/touch/?text=bb&clid=2160746&brorich=0&lr=193&uuid=" + uuid_bad_len,
        u"cid": u"72202",
        u"unixtime": u"1460362527",
        u"_stbx": u"rt3.fol--redir--redir-log:15:245540942:",
        u"ip": u"128.70.8.104",
        u"path": u"690.1033",
    },
)

testdata_passport_log = BaseLog(
    path="//home/logfeller/logs/passport-log/1d", date="2016-04-09", default_data=PASSPORT_LOG
)
testdata_passport_log.add_row(
    ip="83.220.236.189",
    py="1",
    unixtime="1460220106",
    source_uri="prt://passport@passport-i3.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    yandexuid="8981408931460006183",
    _stbx="rt3.fol--passport--passport-log:0:543229519:",
    referer="http://www.yandex.ru/",
    iso_eventtime="2016-04-09 19:41:46",
    type="password",
    user_agent="Mozilla/5.0 (Mobile; Windows Phone 8.1; Android 4.0; ARM; Trident/7.0; Touch; rv:11.0; IEMobile/11.0; NOKIA; Lumia 625) like iPhone OS 7_0_3 Mac OS X AppleWebKit/537 (KHTML, like Gecko) Mobile Safari/537",
    input_login="mouradian",
)
testdata_passport_log.add_row(
    ip="83.220.236.189",
    py="1",
    unixtime="1460220106",
    source_uri="prt://passport@passport-i3.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    yandexuid="8981408931460006183",
    _stbx="rt3.fol--passport--passport-log:0:543229519:",
    referer="http://www.yandex.ru/",
    iso_eventtime="2016-04-09 19:41:46",
    type="password",
    user_agent="Mozilla/5.0 (Mobile; Windows Phone 8.1; Android 4.0; ARM; Trident/7.0; Touch; rv:11.0; IEMobile/11.0; NOKIA; Lumia 625) like iPhone OS 7_0_3 Mac OS X AppleWebKit/537 (KHTML, like Gecko) Mobile Safari/537",
    input_login="mouradian",
)
testdata_passport_log.add_row(
    uid="15033290",
    ip="83.220.236.189",
    py="1",
    unixtime="1460220106",
    source_uri="prt://passport@passport-i3.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    yandexuid="8981408931460006183",
    _stbx="rt3.fol--passport--passport-log:0:543229519:",
    iso_eventtime="2016-04-09 19:41:46",
    user_agent="Mozilla/5.0 (Mobile; Windows Phone 8.1; Android 4.0; ARM; Trident/7.0; Touch; rv:11.0; IEMobile/11.0; NOKIA; Lumia 625) like iPhone OS 7_0_3 Mac OS X AppleWebKit/537 (KHTML, like Gecko) Mobile Safari/537",
    input_login="mouradian",
)
testdata_passport_log.add_row(
    ip="83.220.236.189",
    py="1",
    unixtime="1460220106",
    source_uri="prt://passport@passport-i3.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    yandexuid="8981408931460006183",
    _stbx="rt3.fol--passport--passport-log:0:543229519:",
    referer="http://www.yandex.ru/",
    iso_eventtime="2016-04-09 19:41:46",
    type="password",
    user_agent="Mozilla/5.0 (Mobile; Windows Phone 8.1; Android 4.0; ARM; Trident/7.0; Touch; rv:11.0; IEMobile/11.0; NOKIA; Lumia 625) like iPhone OS 7_0_3 Mac OS X AppleWebKit/537 (KHTML, like Gecko) Mobile Safari/537",
    input_login="mouradian",
)
testdata_passport_log.add_row(
    uid="15033290",
    ip="83.220.236.189",
    py="1",
    unixtime="1460220106",
    source_uri="prt://passport@passport-i3.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    yandexuid="8981408931460006183",
    _stbx="rt3.fol--passport--passport-log:0:543229519:",
    iso_eventtime="2016-04-09 19:41:46",
    user_agent="Mozilla/5.0 (Mobile; Windows Phone 8.1; Android 4.0; ARM; Trident/7.0; Touch; rv:11.0; IEMobile/11.0; NOKIA; Lumia 625) like iPhone OS 7_0_3 Mac OS X AppleWebKit/537 (KHTML, like Gecko) Mobile Safari/537",
    input_login="mouradian",
)
testdata_passport_log.add_row(
    _date="2016-04-10",
    ip="94.199.75.12",
    py="1",
    unixtime="1460273882",
    source_uri="prt://passport@passport-m1.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    yandexuid="1622768111453529338",
    _stbx="rt3.sas--passport--passport-log:0:327527491:",
    referer="https://www.yandex.ru/",
    iso_eventtime="2016-04-10 10:38:02",
    type="password",
    user_agent="Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 Safari/537.36",
    input_login="stasi1979",
)
testdata_passport_log.add_row(
    _date="2016-04-10",
    ip="94.199.75.12",
    py="1",
    unixtime="1460273882",
    source_uri="prt://passport@passport-m1.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    yandexuid="1622768111453529338",
    _stbx="rt3.sas--passport--passport-log:0:327527491:",
    referer="https://www.yandex.ru/",
    iso_eventtime="2016-04-10 10:38:02",
    type="password",
    user_agent="Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 Safari/537.36",
    input_login="stasi1979",
)
testdata_passport_log.add_row(
    _date="2016-04-10",
    iso_eventtime="2016-04-10 10:38:02",
    uid="103411294",
    user_agent="Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 Safari/537.36",
    ip="94.199.75.12",
    yandexuid="1622768111453529338",
    py="1",
    input_login="stasi1979",
    source_uri="prt://passport@passport-m1.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    unixtime="1460273882",
    _stbx="rt3.sas--passport--passport-log:0:327527491:",
)
testdata_passport_log.add_row(
    _date="2016-04-10",
    ip="94.199.75.12",
    py="1",
    unixtime="1460273882",
    source_uri="prt://passport@passport-m1.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    yandexuid="1622768111453529338",
    _stbx="rt3.sas--passport--passport-log:0:327527491:",
    referer="https://www.yandex.ru/",
    iso_eventtime="2016-04-10 10:38:02",
    type="password",
    user_agent="Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 Safari/537.36",
    input_login="stasi1979",
)
testdata_passport_log.add_row(
    _date="2016-04-10",
    iso_eventtime="2016-04-10 10:38:02",
    uid="103411294",
    user_agent="Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 Safari/537.36",
    ip="94.199.75.12",
    yandexuid="1622768111453529338",
    py="1",
    input_login="stasi1979",
    source_uri="prt://passport@passport-m1.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    unixtime="1460273882",
    _stbx="rt3.sas--passport--passport-log:0:327527491:",
)
testdata_passport_log.add_row(
    _date="2016-04-11",
    ip="188.16.109.151",
    py="1",
    unixtime="1460388396",
    source_uri="prt://passport@passport-m4.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    yandexuid="7646818301458902093",
    _stbx="rt3.sas--passport--passport-log:0:328726763:",
    referer="https://mail.yandex.ru/",
    iso_eventtime="2016-04-11 18:26:36",
    type="password",
    user_agent="Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7596 Yowser/2.5 Safari/537.36",
    input_login="perschina.olga2012",
)
testdata_passport_log.add_row(
    _date="2016-04-11",
    ip="188.16.109.151",
    py="1",
    unixtime="1460388396",
    source_uri="prt://passport@passport-m4.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    yandexuid="7646818301458902093",
    _stbx="rt3.sas--passport--passport-log:0:328726763:",
    referer="https://mail.yandex.ru/",
    iso_eventtime="2016-04-11 18:26:36",
    type="password",
    user_agent="Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7596 Yowser/2.5 Safari/537.36",
    input_login="perschina.olga2012",
)
testdata_passport_log.add_row(
    _date="2016-04-11",
    uid="115536749",
    ip="188.16.109.151",
    py="1",
    unixtime="1460388396",
    source_uri="prt://passport@passport-m4.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    yandexuid="7646818301458902093",
    _stbx="rt3.sas--passport--passport-log:0:328726763:",
    iso_eventtime="2016-04-11 18:26:36",
    user_agent="Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7596 Yowser/2.5 Safari/537.36",
    input_login="perschina.olga2012",
)
testdata_passport_log.add_row(
    _date="2016-04-11",
    ip="188.16.109.151",
    py="1",
    unixtime="1460388396",
    source_uri="prt://passport@passport-m4.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    yandexuid="7646818301458902093",
    _stbx="rt3.sas--passport--passport-log:0:328726763:",
    referer="https://mail.yandex.ru/",
    iso_eventtime="2016-04-11 18:26:36",
    type="password",
    user_agent="Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7596 Yowser/2.5 Safari/537.36",
    input_login="perschina.olga2012",
)
testdata_passport_log.add_row(
    _date="2016-04-11",
    uid="115536749",
    ip="188.16.109.151",
    py="1",
    unixtime="1460388396",
    source_uri="prt://passport@passport-m4.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    yandexuid="7646818301458902093",
    _stbx="rt3.sas--passport--passport-log:0:328726763:",
    iso_eventtime="2016-04-11 18:26:36",
    user_agent="Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7596 Yowser/2.5 Safari/537.36",
    input_login="perschina.olga2012",
)
# oauth match
testdata_passport_log.add_row(
    _date="2016-04-11",
    uid="11121",
    ip="188.16.109.151",
    py="1",
    unixtime="1460388396",
    source_uri="prt://passport@passport-m4.yandex.net/var/log/yandex/passport-api/statbox/statbox.log",
    yandexuid="7646818301458902094",
    _stbx="rt3.sas--passport--passport-log:0:328726763:",
    iso_eventtime="2016-04-11 18:26:36",
    user_agent="Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.111 YaBrowser/16.3.0.7596 Yowser/2.5 Safari/537.36",
    input_login="perschina.olga2013",
    login="perschina.olga2013",
)


testdata_dicts_yuid_ids = ComplexParametersLog(
    path="//crypta/production/state/graph/dicts", table_name="yuid_ids", default_data=DICTS_YUID_IDS
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459364411", value={u"i": u"89.169.227.155", u"l": u"test111", u"f": u"7607980141459364414"}
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459364433", value={u"i": u"89.169.227.155", u"l": u"test222", u"f": u"7607980141459364414"}
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459364422",
    value={u"i": u"109.187.225.69", u"l": u"test111", u"m": u"test1112@mail.ru", u"f": u"11930045191431402882"},
)
testdata_dicts_yuid_ids.add_row(
    key="157564101446629164", value={u"i": u"89.169.227.155", u"m": u"test333@mail.ru", u"f": u"7607980141459364414"}
)
testdata_dicts_yuid_ids.add_row(
    key="7065602181459497865",
    value={u"i": u"109.187.225.69", u"l": u"test222", u"m": u"test555@mail.ru", u"f": u"11930045191431402882"},
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459364477",
    value={u"i": u"128.68.157.190,176.15.188.224", u"l": u"alexeylevis", u"f": u"8216267451452920616"},
)

testdata_dicts_yuid_ids.add_row(
    key="157564101446625111", value={u"i": u"109.187.225.69", u"l": u"test_login111", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="157564101446625112", value={u"i": u"128.68.157.190", u"l": u"test_login111", u"f": u"8216267451452920616"}
)
testdata_dicts_yuid_ids.add_row(
    key="157564101446625113", value={u"i": u"109.187.225.69", u"l": u"test_login113", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="157564101446625114", value={u"i": u"128.68.157.190", u"l": u"test_login114", u"f": u"8216267451452920616"}
)
testdata_dicts_yuid_ids.add_row(
    key="157564101446625121", value={u"i": u"109.187.225.69", u"l": u"test_login115", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="157564101446625122", value={u"i": u"128.68.157.190", u"l": u"test_login115", u"f": u"8216267451452920616"}
)
testdata_dicts_yuid_ids.add_row(
    key="157564101446625123", value={u"i": u"109.187.225.69", u"l": u"test_login116", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="157564101446625124", value={u"i": u"128.68.157.190", u"l": u"test_login116", u"f": u"8216267451452920616"}
)
testdata_dicts_yuid_ids.add_row(
    key="813903801458090754", value={u"i": u"109.187.225.69", u"l": u"test_login117", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="2565999051458967148", value={u"i": u"128.68.157.190", u"l": u"test_login117", u"f": u"8216267451452920616"}
)

testdata_dicts_yuid_ids.add_row(
    key="9731293591459365211", value={u"i": u"109.187.225.69", u"l": u"test_login211", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459365212", value={u"i": u"128.68.157.190", u"l": u"test_login211", u"f": u"8216267451452920616"}
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459365213", value={u"i": u"109.187.225.69", u"l": u"test_login213", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459365214", value={u"i": u"128.68.157.190", u"l": u"test_login214", u"f": u"8216267451452920616"}
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459365221", value={u"i": u"109.187.225.69", u"l": u"test_login215", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459365222", value={u"i": u"128.68.157.190", u"l": u"test_login215", u"f": u"8216267451452920616"}
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459365223", value={u"i": u"109.187.225.69", u"l": u"test_login216", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459365224", value={u"i": u"128.68.157.190", u"l": u"test_login216", u"f": u"8216267451452920616"}
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459365231", value={u"i": u"109.187.225.69", u"l": u"test_login217", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459365232", value={u"i": u"128.68.157.190", u"l": u"test_login217", u"f": u"8216267451452920616"}
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459365233", value={u"i": u"109.187.225.69", u"l": u"test_login218", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="9731293591459365234", value={u"i": u"128.68.157.190", u"l": u"test_login218", u"f": u"8216267451452920616"}
)

testdata_dicts_yuid_ids.add_row(
    key="7065602181459495311", value={u"i": u"109.187.225.69", u"l": u"test_login311", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="7065602181459495312", value={u"i": u"128.68.157.190", u"l": u"test_login311", u"f": u"8216267451452920616"}
)
testdata_dicts_yuid_ids.add_row(
    key="7065602181459495313", value={u"i": u"109.187.225.69", u"l": u"test_login313", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="7065602181459495314", value={u"i": u"128.68.157.190", u"l": u"test_login314", u"f": u"8216267451452920616"}
)
testdata_dicts_yuid_ids.add_row(
    key="7065602181459495321", value={u"i": u"109.187.225.69", u"l": u"test_login315", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="7065602181459495322", value={u"i": u"128.68.157.190", u"l": u"test_login315", u"f": u"8216267451452920616"}
)
testdata_dicts_yuid_ids.add_row(
    key="7065602181459495323", value={u"i": u"109.187.225.69", u"l": u"test_login316", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="7065602181459495324", value={u"i": u"128.68.157.190", u"l": u"test_login316", u"f": u"8216267451452920616"}
)
testdata_dicts_yuid_ids.add_row(
    key="7065602181459495331", value={u"i": u"109.187.225.69", u"l": u"", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="7065602181459495332", value={u"i": u"128.68.157.190", u"l": u"test_login317", u"f": u"8216267451452920616"}
)
testdata_dicts_yuid_ids.add_row(
    key="7065602181459495333", value={u"i": u"109.187.225.69", u"l": u"test_login318", u"f": u"11930045191431402882"}
)
testdata_dicts_yuid_ids.add_row(
    key="7065602181459495334", value={u"i": u"128.68.157.190", u"l": u"test_login318", u"f": u"8216267451452920616"}
)

testdata_dicts_yuid_ua = ComplexParametersLog(
    path="//crypta/production/state/graph/dicts", table_name="yuid_ua", default_data=DICTS_YUID_UA
)
testdata_dicts_yuid_ua.add_row(
    key="2565999051458967148",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="157564101446629164",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="7065602181459497861",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.074077,0.925922",
        u"adhocs": u"8:1:950000;316:0:68355;124:0:851686;16:1:726562;16:0:273437;409:0:729761;382:1:664062;14:1:652343;14:0:347656;318:0:172573;73:0:277343;73:1:722656;361:0:777049;460:0:506400;445:0:487193;129:0:547087;400:0:872031;444:0:688856;304:0:854843;70:1:835937;435:0:589279;18:1:820312;18:0:179687;434:0:617726;343:0:1000000;23:1:683593;23:0:316406;463:0:857299;430:0:907945;8:0:50000;464:0:934493;161:0:456214;401:0:834108;429:0:231346;396:0:843414;315:0:77743;68:0:50781;68:1:949218;383:1:726562;383:0:273437;146:0:851562;146:1:148437;382:0:335937;314:0:51150;378:0:214722;44:0:164062;44:1:835937;433:0:338327;469:0:907039;126:0:854388;70:0:164062;187:0:418344",
        u"br": u"chrome",
        u"income": u"0.022308,0.896629,0.081062",
        u"br_v": u"51.0.2700",
        u"ua": DESK_UA,
        u"age": u"0.777638,0.162609,0.023267,0.025281,0.011203",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459364477",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459364411",
    value={
        u"ua_profile": u"m|phone|apple|ios|9.3.1",
        u"sex": u"0.074077,0.925922",
        u"adhocs": u"8:1:950000;316:0:68355;124:0:851686;16:1:726562;16:0:273437;409:0:729761;382:1:664062;14:1:652343;14:0:347656;318:0:172573;73:0:277343;73:1:722656;361:0:777049;460:0:506400;445:0:487193;129:0:547087;400:0:872031;444:0:688856;304:0:854843;70:1:835937;435:0:589279;18:1:820312;18:0:179687;434:0:617726;343:0:1000000;23:1:683593;23:0:316406;463:0:857299;430:0:907945;8:0:50000;464:0:934493;161:0:456214;401:0:834108;429:0:231346;396:0:843414;315:0:77743;68:0:50781;68:1:949218;383:1:726562;383:0:273437;146:0:851562;146:1:148437;382:0:335937;314:0:51150;378:0:214722;44:0:164062;44:1:835937;433:0:338327;469:0:907039;126:0:854388;70:0:164062;187:0:418344",
        u"br": u"chrome",
        u"income": u"0.022308,0.896629,0.081062",
        u"br_v": u"51.0.2700",
        u"ua": u"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2700.0 Safari/537.36",
        u"age": u"0.777638,0.162609,0.023267,0.025281,0.011203",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="7065602181459497865",
    value={
        u"ua_profile": u"m|phone|apple|ios|9.3.1",
        u"sex": u"0.914248,0.085751",
        u"adhocs": u"160:0:334476;8:1:500000;316:0:21317;73:0:710937;470:0:923666;466:0:681206;421:0:290986;73:1:289062;464:0:847053;400:0:901646;444:0:945486;472:0:961301;266:0:403537;361:0:415474;445:0:705575;134:1:722656;134:0:277343;70:1:300781;116:0:490103;169:0:549689;461:0:344696;102:0:548681;408:0:776825;106:0:432535;460:0:392810;343:0:1000000;397:0:849634;70:0:699218;125:0:875019;398:0:844240;436:0:704609;474:0:925074;184:0:439486;8:0:500000;126:0:861254;223:0:573307;315:0:18632;174:0:504242;71:0:277343;71:1:722656;123:0:886892;146:0:707031;146:1:292968;382:0:101562;382:1:898437;454:0:586282;378:0:342793;17:0:167968;17:1:832031;13:0:679687;13:1:320312;263:0:418282",
        u"br": u"mobilesafari",
        u"income": u"0.003329,0.082645,0.914025",
        u"br_v": u"9.0",
        u"ua": u"Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13E238 Safari/601.1",
        u"age": u"0.01176,0.10573,0.603281,0.232648,0.046578",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459364412",
    value={
        u"ua_profile": u"m|phone|apple|ios|9.3.1",
        u"sex": u"0.074077,0.925922",
        u"adhocs": u"8:1:950000;316:0:68355;124:0:851686;16:1:726562;16:0:273437;409:0:729761;382:1:664062;14:1:652343;14:0:347656;318:0:172573;73:0:277343;73:1:722656;361:0:777049;460:0:506400;445:0:487193;129:0:547087;400:0:872031;444:0:688856;304:0:854843;70:1:835937;435:0:589279;18:1:820312;18:0:179687;434:0:617726;343:0:1000000;23:1:683593;23:0:316406;463:0:857299;430:0:907945;8:0:50000;464:0:934493;161:0:456214;401:0:834108;429:0:231346;396:0:843414;315:0:77743;68:0:50781;68:1:949218;383:1:726562;383:0:273437;146:0:851562;146:1:148437;382:0:335937;314:0:51150;378:0:214722;44:0:164062;44:1:835937;433:0:338327;469:0:907039;126:0:854388;70:0:164062;187:0:418344",
        u"br": u"chrome",
        u"income": u"0.022308,0.896629,0.081062",
        u"br_v": u"51.0.2700",
        u"ua": u"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2700.0 Safari/537.36",
        u"age": u"0.777638,0.162609,0.023267,0.025281,0.011203",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459364422",
    value={
        u"ua_profile": u"m|phone|apple|ios|9.3.1",
        u"sex": u"0.914248,0.085751",
        u"adhocs": u"160:0:334476;8:1:500000;316:0:21317;73:0:710937;470:0:923666;466:0:681206;421:0:290986;73:1:289062;464:0:847053;400:0:901646;444:0:945486;472:0:961301;266:0:403537;361:0:415474;445:0:705575;134:1:722656;134:0:277343;70:1:300781;116:0:490103;169:0:549689;461:0:344696;102:0:548681;408:0:776825;106:0:432535;460:0:392810;343:0:1000000;397:0:849634;70:0:699218;125:0:875019;398:0:844240;436:0:704609;474:0:925074;184:0:439486;8:0:500000;126:0:861254;223:0:573307;315:0:18632;174:0:504242;71:0:277343;71:1:722656;123:0:886892;146:0:707031;146:1:292968;382:0:101562;382:1:898437;454:0:586282;378:0:342793;17:0:167968;17:1:832031;13:0:679687;13:1:320312;263:0:418282",
        u"br": u"mobilesafari",
        u"income": u"0.003329,0.082645,0.914025",
        u"br_v": u"9.0",
        u"ua": u"Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13E238 Safari/601.1",
        u"age": u"0.01176,0.10573,0.603281,0.232648,0.046578",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459364433",
    value={
        u"ua_profile": u"m|phone|apple|ios|9.3.1",
        u"sex": u"0.914248,0.085751",
        u"adhocs": u"160:0:334476;8:1:500000;316:0:21317;73:0:710937;470:0:923666;466:0:681206;421:0:290986;73:1:289062;464:0:847053;400:0:901646;444:0:945486;472:0:961301;266:0:403537;361:0:415474;445:0:705575;134:1:722656;134:0:277343;70:1:300781;116:0:490103;169:0:549689;461:0:344696;102:0:548681;408:0:776825;106:0:432535;460:0:392810;343:0:1000000;397:0:849634;70:0:699218;125:0:875019;398:0:844240;436:0:704609;474:0:925074;184:0:439486;8:0:500000;126:0:861254;223:0:573307;315:0:18632;174:0:504242;71:0:277343;71:1:722656;123:0:886892;146:0:707031;146:1:292968;382:0:101562;382:1:898437;454:0:586282;378:0:342793;17:0:167968;17:1:832031;13:0:679687;13:1:320312;263:0:418282",
        u"br": u"mobilesafari",
        u"income": u"0.003329,0.082645,0.914025",
        u"br_v": u"9.0",
        u"ua": u"Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13E238 Safari/601.1",
        u"age": u"0.01176,0.10573,0.603281,0.232648,0.046578",
    },
)

testdata_dicts_yuid_ua.add_row(
    key="1813903801458090754",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="213261281388846262",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="2263475241459074652",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)

testdata_dicts_yuid_ua.add_row(
    key="157564101446625111",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="157564101446625112",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="157564101446625113",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="157564101446625114",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="157564101446625121",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="157564101446625122",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="157564101446625123",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="157564101446625124",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)

testdata_dicts_yuid_ua.add_row(
    key="9731293591459365211",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459365212",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459365213",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459365214",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459365221",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459365222",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459365223",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459365224",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459365231",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459365232",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459365233",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="9731293591459365234",
    value={
        u"ua_profile": u"d|desk|windows|6.1",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"chrome",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": DESK_UA,
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)

testdata_dicts_yuid_ua.add_row(
    key="7065602181459495311",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="7065602181459495312",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="7065602181459495313",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="7065602181459495314",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="7065602181459495321",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="7065602181459495322",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="7065602181459495323",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="7065602181459495324",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="7065602181459495331",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="7065602181459495332",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="7065602181459495333",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)
testdata_dicts_yuid_ua.add_row(
    key="7065602181459495334",
    value={
        u"ua_profile": u"m|phone|samsung|android|4.1.2",
        u"sex": u"0.106432,0.893567",
        u"adhocs": u"8:1:500000;316:0:7505;168:0:335486;16:1:515625;16:0:484375;468:0:587815;467:0:958694;14:1:789062;14:0:210937;73:0:335937;73:1:664062;361:0:430685;210:0:353802;272:0:342085;397:0:862670;57:0:140625;57:1:859375;97:0:402656;435:0:526954;434:0:512537;343:0:1000000;111:0:293178;377:0:603018;371:0:175781;371:1:824218;141:1:960937;141:0:39062;8:0:500000;396:0:838881;68:0:242187;68:1:757812",
        u"br": u"androidbrowser",
        u"income": u"0.043516,0.914052,0.04243",
        u"br_v": u"4.1.2",
        u"ua": u"Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; GT-N7000 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
        u"age": u"0.093118,0.376148,0.3363,0.151685,0.042746",
    },
)

testdata_bs_chevent_log = BaseLog(
    path="//home/logfeller/logs/bs-chevent-log/1d", date="2016-04-11", default_data=BS_CHEVENT_LOG
)
testdata_bs_chevent_log.add_row(
    yuid="12345611466630289", cryptaid="935164445413735719", clientip6="::ffff:91.122.119.89", clientip="91.122.119.89"
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-10",
    yuid="12345611466630289",
    cryptaid="935164445413735719",
    clientip6="::ffff:91.122.119.89",
    clientip="91.122.119.89",
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-09",
    yuid="12345611466630289",
    cryptaid="935164445413735719",
    clientip6="::ffff:91.122.119.89",
    clientip="91.122.119.89",
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-08",
    yuid="12345611466630289",
    cryptaid="935164445413735719",
    clientip6="::ffff:91.122.119.89",
    clientip="91.122.119.89",
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-07",
    yuid="12345611466630289",
    cryptaid="935164445413735719",
    clientip6="::ffff:91.122.119.89",
    clientip="91.122.119.89",
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-06",
    yuid="12345611466630289",
    cryptaid="935164445413735719",
    clientip6="::ffff:91.122.119.89",
    clientip="91.122.119.89",
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-11",
    yuid="12345611466630289",
    cryptaid="935164445413735719",
    clientip6="::ffff:91.122.119.89",
    clientip="91.122.119.89",
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-11", idfa="40DD766D-B59A-423C-A620-ACC5C7CFCF4A", rawuniqid="1556637581549141507"
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-11", idfa="40DD766D-B59A-423C-A620-ACC5C7CFCF4B", rawuniqid="2556637581549141507"
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-11", idfa="40DD766D-B59A-423C-A620-ACC5C7CFCF4C", rawuniqid="3556637581549141507"
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-11", ifv="40DD766D-B59A-423C-A620-ACC5C7CFCF4A", rawuniqid="1556637581549141507"
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-11", ifv="40DD766D-B59A-423C-A620-ACC5C7CFCF4B", rawuniqid="2556637581549141507"
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-11", ifv="40DD766D-B59A-423C-A620-ACC5C7CFCF4C", rawuniqid="3556637581549141507"
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-11", gaid="68fcbfc3-18ed-48b9-bdb8-45b5459f2a66", rawuniqid="3264815981569878512"
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-11", idfa="40DD766D-B59A-423C-A620-ACC5C7CFCF4A", rawuniqid="1556637581549141507"
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-11", oaid="68fcbfc3-18ed-48b9-bdb8-ffffff0fff4a", rawuniqid="3264815981569878512"
)
testdata_bs_chevent_log.add_row(
    _date="2016-04-11", oaid="40dd766d-b59a-423c-a620-ffffff0fff4a", rawuniqid="1556637581549141507"
)


testdata_devid_hash = BaseLog(path="//crypta/production/state/graph/dicts", table_name="devid_hash", default_data={})
# access
testdata_devid_hash.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE1",
    devidhash="4451492610968249451",
    mmetric_devid="53b84e78dbf8e7aa77ac092137057bc1",
)
testdata_devid_hash.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE2",
    devidhash="4451492610968249452",
    mmetric_devid="53b84e78dbf8e7aa77ac092137057bc2",
)
testdata_devid_hash.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE3",
    devidhash="4451492610968249453",
    mmetric_devid="53b84e78dbf8e7aa77ac092137057bc3",
)
testdata_devid_hash.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE4",
    devidhash="4451492610968249454",
    mmetric_devid="53b84e78dbf8e7aa77ac092137057bc4",
)

# watch
testdata_devid_hash.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE5",
    devidhash="4451492610968249451",
    mmetric_devid="886f3cd0a0fa93a19f9d35362dcc111d",
)
testdata_devid_hash.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE6",
    devidhash="4451492610968249452",
    mmetric_devid="886f3cd0a0fa93a19f9d35362dcc222d",
)
testdata_devid_hash.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE7",
    devidhash="4451492610968249453",
    mmetric_devid="886f3cd0a0fa93a19f9d35362dcc333d",
)
testdata_devid_hash.add_row(
    devid="11B22432-0112-4234-A353-C3E98C8BAAE8",
    devidhash="4451492610968249454",
    mmetric_devid="886f3cd0a0fa93a19f9d35362dcc444d",
)

BLACK_LIST_KEYS = [
    "222564101446625991_222564101446625992",
    "222564101446625993_222564101446625994",
    "222564101446625995_222564101446625996",
    "222564101446625003_222564101446625904",
]

testdata_pairs_black_list = BaseLog(
    path="//crypta/production/state/graph/dicts", table_name="pairs_black_list", default_data={}
)

for key in BLACK_LIST_KEYS:
    testdata_pairs_black_list.add_row(key=key, pair_type="black_list", st_number="NAPRIMER-1")

testdata_email_org_classification = BaseLog(
    path="//crypta/production/ids_storage/email", table_name="email_org_classification", default_data={}
)
testdata_email_org_classification.add_row(id="new_email@bbb.ru", id_type="email", is_org_score="0.9285015642642975")
testdata_email_org_classification.add_row(id="bars12@161.ru", id_type="email", is_org_score="0.81015642642975")
testdata_email_org_classification.add_row(id="new_email@yandex.ru", id_type="email", is_org_score="0.3785015642642975")
testdata_email_org_classification.add_row(
    id="this_email_no_in_yuid_id@testdomain.ru", id_type="email", is_org_score="0.3785015642642975"
)


testdata_idstorage_yuid = BaseLog(
    path="//crypta/production/ids_storage/yandexuid", table_name="eternal", default_data={}
)

testdata_idstorage_yuid.add_row(
    **{
        "id": "999999991543414614",
        "id_type": "yandexuid",
        "date_begin": "2016-04-01",
        "date_end": "2016-04-10",
        "browser_name": "fakebrowser",
        "browser_version": "7.0",
        "os_name": "fakeos",
        "os_family": "fakeos",
        "os_version": "7.0",
        "is_emulator": False,
        "is_browser": True,
        "is_mobile": False,
        "is_tablet": False,
        "is_touch": False,
        "is_robot": False,
        "is_tv": False,
    }
)
testdata_idstorage_yuid.add_row(
    **{
        "id": "777777771500000000",
        "id_type": "yandexuid",
        "date_begin": "2016-04-01",
        "date_end": "2016-04-10",
        "browser_name": "fakebrowser",
        "browser_version": "7.0",
        "os_name": "iosiosios",
        "os_family": "ios",
        "os_version": "7.0",
        "is_emulator": False,
        "is_browser": True,
        "is_mobile": False,
        "is_tablet": False,
        "is_touch": False,
        "is_robot": False,
        "is_tv": False,
    }
)

testdata_idstorage_icookie = BaseLog(
    path="//crypta/production/ids_storage/icookie", table_name="eternal", default_data={}
)

testdata_idstorage_icookie.add_row(
    **{
        "id": "999999991543414614",
        "id_type": "icookie",
        "date_begin": "2016-04-01",
        "date_end": "2016-04-10",
        "browser_name": "fakebrowser",
        "browser_version": "7.0",
        "os_name": "fakeos",
        "os_family": "fakeos",
        "os_version": "7.0",
        "is_emulator": False,
        "is_browser": True,
        "is_mobile": False,
        "is_tablet": False,
        "is_touch": False,
        "is_robot": False,
        "is_tv": False,
    }
)

testdata_idstorage_duid = BaseLog(path="//crypta/production/ids_storage/duid", table_name="index", default_data={})
testdata_idstorage_duid.add_row(
    **{"duid": 999999991543414614, "yandexuid": 123123321543414611, "counter_id": 1337, "last_ts": 1459606065}
)
testdata_idstorage_duid.add_row(
    **{"duid": 727278323121933132, "yandexuid": 281828128124124111, "counter_id": 1337, "last_ts": 1459606065}
)

testdata_idstorage_idfa = BaseLog(path="//crypta/production/ids_storage/idfa", table_name="eternal", default_data={})

testdata_idstorage_idfa.add_row(
    **{
        "id": "DEADBEEF-C0DE-CAFE-BABE-8BADF00DDEAD",
        "id_type": "idfa",
        "date_begin": "2016-04-01",
        "date_end": "2016-04-10",
        "manufacturer": "ACME",
        "model": "ACMEPhone",
        "os": "ios",
        "os_version": "7.0",
        "screen_width": 1234,
        "screen_height": 5678,
    }
)

testdata_idstorage_gaid = BaseLog(path="//crypta/production/ids_storage/gaid", table_name="eternal", default_data={})

testdata_idstorage_gaid.add_row(
    **{
        "id": "deadbeef-c0de-cafe-babe-8badf00ddead",
        "id_type": "gaid",
        "date_begin": "2016-04-01",
        "date_end": "2016-04-10",
        "manufacturer": "ACME",
        "model": "ACMEPhone",
        "os": "android",
        "os_version": "7.0",
        "screen_width": 1234,
        "screen_height": 5678,
    }
)

testdata_idstorage_mm_device_id = BaseLog(
    path="//crypta/production/ids_storage/mm_device_id", table_name="eternal", default_data={}
)
testdata_idstorage_mm_device_id.add_row(
    **{
        "id": "deadbeef-c0de-cafe-babe-8badf00ddead",
        "id_type": "mm_device_id",
        "date_begin": "2016-04-01",
        "date_end": "2016-04-10",
        "manufacturer": "ACME",
        "model": "ACMEPhone",
        "os": "ACMEOS",
        "os_version": "7.0",
        "screen_width": 1234,
        "screen_height": 5678,
    }
)

testdata_idstorage_mm_device_id.add_row(
    **{
        "id": "0000000000000000000050e549a6fcf4",
        "id_type": "mm_device_id",
        "date_begin": "2016-04-01",
        "date_end": "2016-04-10",
        "manufacturer": "ACME",
        "model": "iPhone",
        "os": "ios",
        "os_version": "7.0",
        "screen_width": 1234,
        "screen_height": 5678,
    }
)

testdata_idstorage_uuid = BaseLog(path="//crypta/production/ids_storage/uuid", table_name="eternal", default_data={})

testdata_idstorage_uuid.add_row(
    **{
        "id": "deadbeefc0decafebabe8badf00ddead",
        "id_type": "uuid",
        "date_begin": "2016-04-01",
        "date_end": "2016-04-10",
        "app_id": "com.acme",
        "app_version": "7.0",
        "app_platform": "ios",
        "api_keys": {"1": 1},
    }
)

testdata_idstorage_uuid.add_row(
    **{
        "id": "dc84a454090db1a3746c5d23a514b5dc",
        "id_type": "uuid",
        "date_begin": "2016-04-01",
        "date_end": "2016-04-10",
        "app_id": "ru.yandex.realapp",
        "app_version": "7.0",
        "app_platform": "ios",
        "api_keys": {"1": 1},
    }
)

testdata_bs_xuniqs_log = BaseLog(path="//home/logfeller/logs/bs-xuniqs-log/1d", date="2016-04-11", default_data={})

testdata_bs_xuniqs_log.add_row(
    pagekeywordid="154", unixtime="1512596671", xuniqid="123123", uniqid="8156730781512180560"
)
testdata_bs_xuniqs_log.add_row(
    pagekeywordid="39", unixtime="1512596671", xuniqid="10113701463508502", uniqid="8156730781512180560"
)
testdata_bs_xuniqs_log.add_row(
    pagekeywordid="245",
    unixtime="1512596671",
    xuniqid="23a05dbc2e9da8a0958f796c28a2519c",
    uniqid="8156730781512180560",
)
testdata_bs_xuniqs_log.add_row(
    pagekeywordid="302", unixtime="1512596671", xuniqid="8156730780512180560", uniqid="8156730781512180560"
)
testdata_bs_xuniqs_log.add_row(
    pagekeywordid="0", unixtime="1512596671", xuniqid="whatever", uniqid="8156730781512180560"
)
testdata_bs_xuniqs_log.add_row(
    pagekeywordid="39", unixtime="1512596671", xuniqid="8156730781512180560", uniqid="8156730781512180560"
)
testdata_bs_xuniqs_log.add_row(
    pagekeywordid="nan", unixtime="1512596671", xuniqid="13804403613570866227", uniqid="8156730781512180560"
)
testdata_bs_xuniqs_log.add_row(
    pagekeywordid="39", unixtime="1512596671", xuniqid="4444440444444444", uniqid="8156730781512180560"
)
testdata_bs_xuniqs_log.add_row(
    pagekeywordid="39", unixtime="1512596671", xuniqid="13804403613570866227", uniqid="8156730781512180560"
)
testdata_bs_xuniqs_log.add_row(
    pagekeywordid="39", unixtime="1512596671", xuniqid="8156730781512180560", uniqid="8156730780512180560"
)


valid_watchlog_yuid1 = testdata_watch_log.rows["//home/logfeller/logs/bs-watch-log/1d/2016-04-11"][0]["uniqid"]
valid_watchlog_yuid2 = testdata_watch_log.rows["//home/logfeller/logs/bs-watch-log/1d/2016-04-11"][1]["uniqid"]

testdata_vertices_20 = BaseLog(
    path="//crypta/production/state/graph/v2/matching", table_name="vertices_no_multi_profile", default_data={}
)
testdata_vertices_20.add_row(id="601826891455541113", id_type="yandexuid", cryptaId="1(YANDEXUID)")
testdata_vertices_20.add_row(id="6249554188804536022", id_type="yandexuid", cryptaId="1(YANDEXUID)")
testdata_vertices_20.add_row(id="6080124334883973097", id_type="yandexuid", cryptaId="2(YANDEXUID)")
testdata_vertices_20.add_row(id="601826891455541114", id_type="yandexuid", cryptaId="3(YANDEXUID)")

testdata_avito_phone_rainbow = BaseLog(
    path="//crypta/production/state/graph/dicts", table_name="avito-phone-rainbow", default_data={}
)
testdata_avito_phone_rainbow.add_row(avito_hash="avito_hash1", phone="79087654321")

testdata_lf_bs_hit = BaseLog(path="//home/logfeller/logs/bs-hit-log/1d", table_name="2016-04-11", default_data={})
testdata_lf_bs_hit.add_row(
    uniqid="10113701463508406", queryargs="726=0123456789abcdefABCDEF9876543212", pageid="279179"
)
testdata_lf_bs_hit.add_row(
    uniqid="10113701463508407",
    queryargs="726="
    + hashlib.md5("testings_for_work2@mail.rufy5drs34dgh13ff").hexdigest(),  # email taken from webvisor above
    pageid="279179",
)
testdata_lf_bs_hit.add_row(uniqid="10113701463508408", queryargs="726=avito_hash1", pageid="279179")
testdata_lf_bs_hit.add_row(
    uniqid="10113701463508409",
    queryargs="726=" + hashlib.md5("login.for.avito@yandex.comfy5drs34dgh13ff").hexdigest(),
    pageid="279179",
)

market_orders = []
order = Order()
order.userIds.puid = 666777
order.userIds.email = "ordermail@markets.ru"
market_orders.append(dict(fact=order.SerializeToString(), timestamp=1475566990000))
order.userIds.puid = 444555
order.userIds.yandexuid = "4413889081469865546"
order.userIds.uuid = "0000000000000000000000000000000f"
order.userIds.email = "marketemail@orders.ru"
order.userIds.phone = "89161234567"
market_orders.append(dict(fact=order.SerializeToString(), timestamp=1475653390000))

partners_phone_hashes_to_find = []
partners_email_hashes_to_find = []
partners_emails_to_find = []
partners_phones_to_find = []

current_yuid_idx_added = 0
current_auto_external_id = 0

edges_by_crypta_id = [
    {
        "id1": "601826891455541119",
        "id1Type": "yandexuid",
        "id2": "login",
        "id2Type": "login",
        "cryptaId": "shared_cryptaid",
        "dates": ["2019-01-01"],
    }
]
direct_yandexuid_by_id_type_and_id = [
    {"target_id": "601826891455541119", "id_type": "vk_name", "id": "vk_name1"},
    {"target_id": "601826891455541119", "id_type": "vk_name", "id": "vk_name2"},
]
yuid_with_all_info = [
    {
        "id": "601826891455541119",
        "ua_profile": "d|desk|windows10",
        "ip_activity_type": "active",
        "main_region_country": 225,
    },
    {
        "id": "601826891455541118",
        "ua_profile": "d|desk|windows10",
        "ip_activity_type": "active",
        "main_region_country": 225,
    },
    {
        "id": "601826891455541117",
        "ua_profile": "d|desk|windows10",
        "ip_activity_type": "active",
        "main_region_country": 225,
    },
    {
        "id": "601826891455541116",
        "ua_profile": "d|desk|windows10",
        "ip_activity_type": "active",
        "main_region_country": 225,
    },
]
vertices_no_multi_profile_by_id_type = [
    {"id": "601826891455541119", "cryptaId": "shared_cryptaid", "id_type": "yandexuid"},
    {"id": "601826891455541118", "cryptaId": "shared_cryptaid", "id_type": "yandexuid"},
    {"id": "601826891455541117", "cryptaId": "shared_cryptaid", "id_type": "yandexuid"},
    {"id": "601826891455541116", "cryptaId": "shared_cryptaid", "id_type": "yandexuid"},
]

profile_2019_01_17 = [
    {
        "yandexuid": 601826891455541119,
        "gender": {"m": 0.25},
        "user_age_6s": {"18_24": 0.25},
        "income_5_segments": {"C1": 0.20, "C2": 0.05},
    }
]
profile_2019_01_18 = [
    {
        "yandexuid": 601826891455541119,
        "gender": {"m": 0.3},
        "user_age_6s": {"18_24": 0.3},
        "income_5_segments": {"C1": 0.25, "C2": 0.05},
    }
]

heuristic_shared_desktop_yuids = [
    {"source": "Heuristic shared desktop yuid", "id": "601826891455541120", "id_type": "yandexuid"}
]


def add_yuid_with_all_row(id_type, id_value, id_value_orig=None):
    global current_yuid_idx_added
    yuid = "1195164201443702{}".format(str(current_yuid_idx_added).rjust(3, "8"))
    row_to_add = {
        "good": True,
        "yuid": yuid,
        "key": yuid,
        "yandexuid": int(yuid),
        "sources": [
            # this is quazi-sources for emails/phones not from partners
            "yamoney"
            if id_type == "phone"
            else "webvisor"
        ],
        id_type + "_dates": {id_value: {TEST_RUN_DATE_STR: 1}},
        id_type + "_orig": {id_value: id_value_orig},
        id_type
        + "_sources": {
            id_value: [
                # this is quazi-sources for emails/phones not from partners
                "yamoney"
                if id_type == "phone"
                else "webvisor"
            ]
        },
        "yuid_creation_date": [SOME_PREV_DATE],
        "all_ip_dates": [TEST_RUN_DATE_STR],
        "all_id_dates": [TEST_RUN_DATE_STR],
        "all_dates": [TEST_RUN_DATE_STR],
    }
    testdata_yuid_with_all.add_row(**row_to_add)
    current_yuid_idx_added += 1
    print("Adding to yuid_with_all:", row_to_add)


# Iterate partners dump data (@d-sun-d dicts format)
# https://github.yandex-team.ru/x-products/xprod-partners-data/blob/master/partners_data/audience.py#L105
# https://wiki.yandex-team.ru/x-products/RealTimeTriggers/Arxitektura/API/audience/?from=%252Fx-products%252FRealTimeTriggers%252FArxitektura%252FAPI%252Fauditiry%252F

found_data = []

# crm data section

phones = ["+78005553535", "+78005553536", "+78005553537", "+78005553538"]
logins = ["lol", "kek", "chebureck", "kukareku"]
emails = ["lol@yandex.ru", "kek@yandex.ru", "chebureck@yandex.ru", "kukareku@yandex.ru"]
puids = ["000", "111", "222"]

# passport data section

phone_to_hash = {
    phones[0]: "CD580BD23C9C9AD44292386D32918EE6",
    phones[1]: "A7BDD02C568E20B6B78464BAA5DB7872",
    phones[2]: "9EA1F5C9C264C2D80E4CBBEA661E8F32",
    phones[3]: "0472BDC3BEE4116DEEDB247167E8C6FB",
}

testdata_puid_login_dict.add_row(id_value=puids[0], login=logins[0])
testdata_puid_login_dict.add_row(id_value=puids[1], login=logins[1])
testdata_puid_login_dict.add_row(id_value=puids[2], login=logins[2])

testdata_passport_puid_phone_md5 = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/puid_phone_md5_passport-profile_passport-phone",
    default_data={},
    append=True,
)
testdata_passport_puid_phone_md5.add_row(
    id1=puids[0],
    id2=phone_to_hash[phones[0]],
    id1Type="puid",
    id2Type="phone_md5",
    logSource="passport-phone",
    sourceType="passport-profile",
    dates=list(),
)

testdata_passport_puid_phone_md5_dump = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/puid_phone_md5_passport-profile_passport-phone-dump",
    default_data={},
    append=True,
)
testdata_passport_puid_phone_md5_dump.add_row(
    id1=puids[1],
    id2=phone_to_hash[phones[1]],
    id1Type="puid",
    id2Type="phone_md5",
    logSource="passport-phone-dump",
    sourceType="passport-profile",
    dates=list(),
)

testdata_passport_puid_phone_md5_sensitive = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/puid_phone_md5_passport-profile_passport-sensitive",
    default_data={},
    append=True,
)
testdata_passport_puid_phone_md5_sensitive.add_row(
    id1=puids[2],
    id2=phone_to_hash[phones[2]],
    id1Type="puid",
    id2Type="phone_md5",
    logSource="passport-sensitive",
    sourceType="passport-profile",
    dates=list(),
)

testdata_hash_to_phone = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/phone_phone_md5_md5_preproc", default_data={}, append=True
)
testdata_hash_to_phone.add_row(
    id1=phones[0],
    id2=phone_to_hash[phones[0]],
    id1Type="phone",
    id2Type="phone_md5",
    logSource="preproc",
    sourceType="md5",
)

testdata_hash_to_phone.add_row(
    id1=phones[1],
    id2=phone_to_hash[phones[1]],
    id1Type="phone",
    id2Type="phone_md5",
    logSource="preproc",
    sourceType="md5",
)

testdata_hash_to_phone.add_row(
    id1=phones[2],
    id2=phone_to_hash[phones[2]],
    id1Type="phone",
    id2Type="phone_md5",
    logSource="preproc",
    sourceType="md5",
)

testdata_cloud_waitlist_email_login = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/dumps/email_login_cloud-waitlist_cloud-waitlist",
    default_data={},
    append=True,
)

testdata_cloud_waitlist_email_yandexuid = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/dumps/email_yandexuid_cloud-waitlist_cloud-waitlist",
    default_data={},
    append=True,
)

testdata_cloud_waitlist_phone_email = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/dumps/phone_email_cloud-waitlist_cloud-waitlist",
    default_data={},
    append=True,
)

testdata_cloud_waitlist_phone_login = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/dumps/phone_login_cloud-waitlist_cloud-waitlist",
    default_data={},
    append=True,
)

testdata_cloud_waitlist_login_yandexuid = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/dumps/login_yandexuid_cloud-waitlist_cloud-waitlist",
    default_data={},
    append=True,
)

testdata_cloud_waitlist_phone_yandexuid = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/dumps/phone_yandexuid_cloud-waitlist_cloud-waitlist",
    default_data={},
    append=True,
)

with open(os.path.join(FIXTURES_ROOT, PARTNERS_FLAT_FIXTURES_PATH)) as f:
    partners_flat_fixture = json.load(f)

for table_idx, (table_name, table_data) in enumerate(sorted(partners_flat_fixture.iteritems())):
    external_ids = []
    found_email_hashes_by_external_id = collections.defaultdict(dict)
    found_phone_hashes_by_external_id = collections.defaultdict(dict)
    partner_table = SingleTableLog(path=table_name, default_data={}, append=True)
    for rec_idx, rec in enumerate(table_data):
        print("Adding to {}:".format(table_name), rec)
        partner_table.add_row(**rec)
        external_id = rec.get("external_id")
        if external_id is None:
            external_id = "42_{}_42_{}".format(table_idx, rec_idx)
        external_ids.append(external_id)
        for email in list(rec.get("emails", [])) + ([rec["email"]] if rec.get("email") else []):
            if GenericID("md5", email).is_valid():
                email_hash = email
                found_email_hashes_by_external_id[external_id][email_hash] = email
            elif GenericID("email", email).is_valid():
                email_hash = GenericID("email", email).md5
                found_email_hashes_by_external_id[external_id][email_hash] = email
        for phone in list(rec.get("phones", [])) + ([rec["phone"]] if rec.get("phone") else []):
            if GenericID("md5", phone).is_valid():
                phone_hash = phone
                found_phone_hashes_by_external_id[external_id][phone_hash] = phone
            elif GenericID("phone", phone).is_valid():
                phone_hash = GenericID("phone", phone).md5
                found_phone_hashes_by_external_id[external_id][phone_hash] = phone

    for external_id in external_ids:
        found_data.append(
            {
                "email_hashes": found_email_hashes_by_external_id.get(external_id, {}),
                "phone_hashes": found_phone_hashes_by_external_id.get(external_id, {}),
            }
        )

# Add some hook ids to yuid_with_all
for idx, rec in enumerate(found_data):
    found_email_hashes = rec["email_hashes"]
    found_phone_hashes = rec["phone_hashes"]

    if (not len(found_email_hashes)) and (not len(found_phone_hashes)):
        continue

    if len(found_phone_hashes) and (not len(found_email_hashes)):
        hook_hash = sorted(found_phone_hashes.keys())[0]
        hook_value_orig = found_phone_hashes[hook_hash]
        add_yuid_with_all_row(id_type="phone", id_value=hook_hash, id_value_orig=hook_value_orig)
    elif len(found_email_hashes) and (not len(found_phone_hashes)):
        hook_hash = sorted(found_email_hashes.keys())[0]
        hook_value_orig = found_email_hashes[hook_hash]
        add_yuid_with_all_row(id_type="email", id_value=hook_hash, id_value_orig=hook_value_orig)
    elif idx % 2 == 0:
        # Add phone hook
        hook_hash = sorted(found_phone_hashes.keys())[0]
        hook_value_orig = found_phone_hashes[hook_hash]
        add_yuid_with_all_row(id_type="phone", id_value=hook_hash, id_value_orig=hook_value_orig)
    else:
        # Add email hook
        hook_hash = sorted(found_email_hashes.keys())[0]
        hook_value_orig = found_email_hashes[hook_hash]
        add_yuid_with_all_row(id_type="email", id_value=hook_hash, id_value_orig=hook_value_orig)

    partners_email_hashes_to_find.extend(sorted(filter(lambda key: key != hook_hash, found_email_hashes.keys())))
    partners_emails_to_find.extend(
        sorted(
            (
                identifier.normalize
                for identifier in filter(
                    lambda identifier: identifier.is_valid(),
                    map(
                        Email,
                        (
                            found_email_hashes[other_hash]
                            for other_hash in found_email_hashes.keys()
                            if other_hash != hook_hash
                        ),
                    ),
                )
            )
        )
    )

    partners_phone_hashes_to_find.extend(sorted(filter(lambda key: key != hook_hash, found_phone_hashes.keys())))
    partners_phones_to_find.extend(
        sorted(
            (
                identifier.normalize
                for identifier in filter(
                    lambda identifier: identifier.is_valid(),
                    map(
                        Phone,
                        (
                            found_phone_hashes[other_hash]
                            for other_hash in found_phone_hashes.keys()
                            if other_hash != hook_hash
                        ),
                    ),
                )
            )
        )
    )

testdata_autoru_event_log_table = BaseLog(
    path="//home/logfeller/logs/vertis-event-log/1d",
    date="2016-04-11",
    default_data={
        "timestamp": "2016-04-11T12:30:00.493Z",
        "_rest": {},
        "domain": "DOMAIN_AUTO",
        "environment": "STABLE",
    },
    attributes={
        "_yql_row_spec": {
            "Type": [
                "StructType",
                [
                    ["environment", ["OptionalType", ["DataType", "String"]]],
                    ["domain", ["OptionalType", ["DataType", "String"]]],
                    ["timestamp", ["OptionalType", ["DataType", "String"]]],
                    ["_rest", ["OptionalType", ["DictType", ["DataType", "String"], ["DataType", "Yson"]]]],
                ],
            ]
        }
    },
)

testdata_autoru_event_log_table.add_row(
    _rest={
        "userEvent": {"authorisation": {"user": {"id": "41860663", "phones": ["79115633661"]}}},
        "requestContext": {
            "platform": "PLATFORM_DESKTOP",
            "domain": "DOMAIN_AUTO",
            "application": "frontend",
            "yandexUid": "8549429211544236565",
        },
    }
)

testdata_autoru_event_log_table.add_row(
    _rest={
        "userEvent": {"update": {"user": {"id": "40684420", "phones": ["79520555667"]}}},
        "requestContext": {
            "platform": "PLATFORM_ANDROID",
            "metricaDeviceId": "f21d6510d800dd3d8879f05a199f4b80",
            "domain": "DOMAIN_AUTO",
            "application": "android",
        },
    }
)

testdata_autoru_event_log_table.add_row(
    _rest={
        "offerEvent": {
            "create": {
                "offer": {"auto": {"seller": {"phones": [{"phone": "79139089258", "original": "79909009009"}]}}}
            }
        },
        "requestContext": {
            "platform": "PLATFORM_IOS",
            "metricaDeviceId": "115CD00C-D8BC-4D6C-8F24-B046B6CE1921",
            "domain": "DOMAIN_AUTO",
            "application": "ios",
            "userId": "42889998",
        },
    }
)

testdata_autoru_event_log_table.add_row(
    _rest={
        "offerEvent": {
            "update": {
                "offer": {"auto": {"seller": {"phones": [{"phone": "79139089258", "original": "79131389898"}]}}}
            }
        },
        "requestContext": {
            "platform": "PLATFORM_ANDROID",
            "metricaDeviceId": "2093a8edf8bdbd49fa589dc63dbfd391",
            "domain": "DOMAIN_AUTO",
            "application": "android",
            "userId": "41885493",
        },
    }
)

testdata_autoru_warehouse_event_log_table = BaseLog(
    path="//home/verticals/broker/prod/warehouse/auto/events/1d",
    date="2016-04-11",
    default_data={"app_user_info": {}},
    attributes={
        "_yql_row_spec": {
            "Type": [
                "StructType",
                [
                    [
                        "user_info",
                        [
                            "StructType",
                            [
                                ["user_id", ["OptionalType", ["DataType", "String"]]],
                                [
                                    "app_user_info",
                                    [
                                        "StructType",
                                        [
                                            ["mobile_uuid", ["OptionalType", ["DataType", "String"]]],
                                            ["gaid", ["OptionalType", ["DataType", "String"]]],
                                            ["idfa", ["OptionalType", ["DataType", "String"]]],
                                        ],
                                    ],
                                ],
                                [
                                    "web_user_info",
                                    ["StructType", [["user_yandexuid", ["OptionalType", ["DataType", "String"]]]]],
                                ],
                            ],
                        ],
                    ]
                ],
            ]
        }
    },
)

testdata_autoru_warehouse_event_log_table.add_row(
    user_info={
        "app_user_info": {
            "gaid": "c362b69f-e9ed-4b04-ad1d-2b69cc4f00e8",
            "idfa": None,
            "mobile_uuid": "C7264A38-6B25-421F-B3C0-71CC5ED1D4DD",
        },
        "web_user_info": {"user_yandexuid": "5643964761574592010"},
        "user_id": "45302384",
    }
)

testdata_autoru_warehouse_event_log_table.add_row(
    user_info={
        "app_user_info": {
            "gaid": None,
            "idfa": "16AB5CCF-E7DC-4363-BA82-298D5F4E27AA",
            "mobile_uuid": "36e81059c5abf0422be6d5cdc035ceb5",
        },
        "web_user_info": {"user_yandexuid": "9270524911587482416"},
        "user_id": "58644922",
    }
)

testdata_soup_cooked_edges = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/cooked/soup_edges", default_data={}, append=True
)

testdata_soup_cooked_edges.add_row(id1="qwerty", id1Type="login", id2="123456", id2Type="puid")

testdata_soup_cooked_edges.add_row(id1="asdfg", id1Type="login", id2="54321", id2Type="puid")


testdata_soup_yuid_puid_fp = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/yandexuid_puid_passport-auth_fp",
    default_data={
        "id1Type": "yandexuid",
        "id2Type": "puid",
        "sourceType": "passport-auth",
        "logSource": "fp",
        "dates": ["2016-04-11"],
    },
    append=True,
)

testdata_soup_yuid_puid_fp.add_row(id1="777777771500000000", id2="10101010")


testdata_soup_puid_uuid_oauth = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/puid_uuid_app-auth_oauth",
    default_data={
        "id1Type": "puid",
        "id2Type": "uuid",
        "sourceType": "app-auth",
        "logSource": "oauth",
        "dates": ["2016-04-11"],
    },
    append=True,
)

testdata_soup_puid_uuid_oauth.add_row(id1="10101010", id2="dc84a454090db1a3746c5d23a514b5dc")


testdata_soup_mm_devid_uuid_mm = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/mm_device_id_uuid_app-metrica_mm",
    default_data={
        "id1Type": "mm_device_id",
        "id2Type": "uuid",
        "sourceType": "app-metrica",
        "logSource": "mm",
        "dates": ["2016-04-11"],
    },
    append=True,
)

testdata_soup_mm_devid_uuid_mm.add_row(id1="0000000000000000000050e549a6fcf4", id2="dc84a454090db1a3746c5d23a514b5dc")


testdata_soup_mobreport = SingleTableLog(
    path="//crypta/production/state/graph/v2/soup/yandexuid_uuid_search-app-mobreport_mob-report",
    default_data={
        "id1Type": "yandexuid",
        "id2Type": "uuid",
        "sourceType": "search-app-mobreport",
        "logSource": "mob-report",
        "dates": ["2016-04-11"],
    },
    append=True,
)

testdata_soup_mobreport.add_row(id1="777777771200000000", id2="aaaabbbbccccddddaaaabbbbccccdddd")


testdata_soup_distr_hist_ui = SoupTableLog(
    base_path="//crypta/production/state/graph/v2/soup",
    id1_type="distr_ui",
    id2_type="yandexuid",
    source_type="distr-historical",
    log_source="distr-historical",
)


testdata_soup_distr_hist_ui = SoupTableLog(
    base_path="//crypta/production/state/graph/v2/soup",
    id1_type="mm_device_id",
    id2_type="uuid",
    source_type="distr-historical",
    log_source="distr-historical",
)


testdata_soup_distr_hist_ui = SoupTableLog(
    base_path="//crypta/production/state/graph/v2/soup",
    id1_type="mm_device_id",
    id2_type="yandexuid",
    source_type="distr-historical",
    log_source="distr-historical",
)


testdata_stream_result_soup_access_uuid = SingleTableLog(
    path="//home/crypta-tests/stuff/state/graph/stream/soup/yandexuid_uuid_app-url-redir_access",
    default_data={
        "id1Type": "yandexuid",
        "id2Type": "uuid",
        "sourceType": "app-url-redir",
        "logSource": "access",
        "dates": ["2016-04-11"],
    },
    append=True,
)
testdata_stream_result_soup_access_uuid.add_row(id1="777777771200000000", id2="aaaabbbbccccddddaaaabbbbccccdddd")
testdata_stream_result_soup_access_uuid.add_row(
    id1="777777771200000000", id2="aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa0", dates=["2016-03-01"]
)


testdata_stream_result_soup_access_devid = SingleTableLog(
    path="//home/crypta-tests/stuff/state/graph/stream/soup/yandexuid_mm_device_id_access-yp-did_access",
    default_data={
        "id1Type": "yandexuid",
        "id2Type": "mm_device_id",
        "sourceType": "access-yp-did",
        "logSource": "access",
        "dates": ["2016-04-11"],
    },
    append=True,
)
testdata_stream_result_soup_access_devid.add_row(id1="2410530891459150418", id2="53b84e78dbf8e7aa77ac092137057bc1")
testdata_stream_result_soup_access_devid.add_row(
    id1="2410530891459150418", id2="bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb0", dates=["2016-03-01"]
)


testdata_stream_result_extra_data_access_1 = SingleTableLog(
    path="//home/crypta-tests/stuff/state/graph/stream/extra_data/AccessLogImportTask/access_log_ua/1",
    default_data={"dt": "2016-04-11"},
    append=True,
)
testdata_stream_result_extra_data_access_1.add_row(yandexuid="2410530891459150418", user_agent="Fake UA", os="ios")

testdata_stream_result_extra_data_access_2 = SingleTableLog(
    path="//home/crypta-tests/stuff/state/graph/stream/extra_data/AccessLogImportTask/access_log_ua/2",
    default_data={"dt": "2016-04-11"},
    append=True,
)
testdata_stream_result_extra_data_access_2.add_row(yandexuid="777777771200000000", user_agent="Fake UA", os="android")


testdata_stream_result_extra_data_wl_1 = SingleTableLog(
    path="//home/crypta-tests/stuff/state/graph/stream/extra_data/WatchLogImportTask/yuid_purchase_log/1",
    default_data={},
    append=True,
)
testdata_stream_result_extra_data_wl_1.add_row(yuid="777777771200000000", id_value="market.ya.ru", dt="2016-04-11")

testdata_stream_result_extra_data_wl_2 = SingleTableLog(
    path="//home/crypta-tests/stuff/state/graph/stream/extra_data/WatchLogImportTask/yuid_purchase_log/2",
    default_data={},
    append=True,
)
testdata_stream_result_extra_data_wl_2.add_row(yuid="2410530891459150418", id_value="ozon.ru", dt="2016-04-11")


testdata_metrika_userparams_dump_table = SingleTableLog(
    path="//home/metrika/userparams/params_01",
    default_data={},
    attributes={
        "schema": [
            {"type": "uint64", "required": False, "name": "counter_id"},
            {"type": "string", "required": False, "name": "param_path"},
            {"type": "uint64", "required": False, "name": "update_time"},
            {"type": "uint64", "required": False, "name": "user_id"},
            {"type": "double", "required": False, "name": "value_double"},
            {"type": "string", "required": False, "name": "value_string"},
        ]
    },
)

testdata_metrika_userparams_dump_table.add_row(
    counter_id=10554589,
    param_path="login",
    update_time=1589389626735,
    user_id=11443661580740964,
    value_double=0.0,
    value_string="kinokill001@gmail.com",
)

testdata_metrika_userparams_dump_table.add_row(
    counter_id=54799495,
    param_path="email",
    update_time=1603817222480,
    user_id=10046921452873914,
    value_double=0.0,
    value_string="403nika@mail.ru",
)

testdata_metrika_userparams_dump_table.add_row(
    counter_id=16221463,
    param_path="phone",
    update_time=1580366431174,
    user_id=10071391496825499,
    value_double=0.0,
    value_string="+7 (909) 675-37-27",
)

testdata_metrika_userparams_dump_table.add_row(
    counter_id=45103497,
    param_path="vk_user_id",
    update_time=1565004158721,
    user_id=12943011559151619,
    value_double=182298937.0,
    value_string="182298937",
)

testdata_metrika_userparams_dump_table.add_row(
    counter_id=51861818,
    param_path="ok_id",
    update_time=1586194786908,
    user_id=9450741191586194644,
    value_double=382402556741.0,
    value_string="",
)
