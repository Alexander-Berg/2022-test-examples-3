import base64

import pytest

from crypta.graph.bochka.lib.packers import (
    row_to_eternal_info,
    row_to_soup_edge,
    row_to_vulture,
    tskv_to_serialized_proto,
)
from crypta.graph.rt.events.proto.soup_pb2 import TSoupEvent
from crypta.graph.engine.proto.info_pb2 import TIdsInfo
from crypta.graph.soup.config.proto.bigb_pb2 import TLinks, EBbLinkUsage
from crypta.graph.soup.config.python import (  # N811 # noqa
    EDGE_TYPE as edges,
    ID_TYPE as id_type,
    LOG_SOURCE as log_source,
    SOURCE_TYPE as source_type,
    Edges,
)
from crypta.lib.python.identifiers.generic_id import GenericID


@pytest.mark.parametrize(
    "row",
    [
        dict(
            id1="00023a85-d82e-45b3-b49a-55eaadce8fef",
            id2="070b657c757375ca8ce580f7e5ab158b",
            id1Type="gaid",
            id2Type="email_md5",
            sourceType="account-manager",
            logSource="mm",
        ),
        dict(
            id1="00023a85-d82e-45b3-b49a-55eaadce8eee",
            id2="070b657c757372318ce580f7e5ab158b",
            id1Type="gaid",
            id2Type="phone_md5",
            sourceType="account-manager",
            logSource="mm",
        ),
        dict(
            id1="some@e.co",
            id2="f617c78ee13f2d74764adf17dabafde1e84b78e6a7b5a37db059f1757b4efc22",
            id1Type="email",
            id2Type="email_sha256",
            sourceType="sha256",
            logSource="preproc",
        ),
        dict(
            id1="000045BA-A3D9-4E88-885F-BBD932F29BE3",
            id2="a49d399ef3334bd4811bbaad0474f021",
            id1Type="idfa",
            id2Type="mm_device_id",
            sourceType="app-metrica",
            logSource="mm",
        ),
    ],
)
def test_row_to_soup_edge(row, frozen_time):
    """ Should check is correctly pack as proto """
    serialized = row_to_soup_edge(frozen_time, row)
    assert isinstance(serialized, str)

    proto = TSoupEvent()
    proto.ParseFromString(base64.b64decode(serialized))
    assert frozen_time == proto.Unixtime
    proto = proto.Edge
    # check edge param value
    assert source_type.by_type(proto.SourceType).Name == row["sourceType"]
    assert log_source.by_type(proto.LogSource).Name == row["logSource"]
    # check vertices
    assert id_type.by_type(GenericID(proto=proto.Vertex1).type).Name == row["id1Type"]
    assert GenericID(proto=proto.Vertex1).value == row["id1"]
    assert id_type.by_type(GenericID(proto=proto.Vertex2).type).Name == row["id2Type"]
    assert GenericID(proto=proto.Vertex2).value == row["id2"]


@pytest.mark.parametrize(
    "row",
    [
        dict(
            id1="in-va-lid e ma il@e.co",
            id2="f617c78ee13f2d74764adf17dabafde1e84b78e6a7b5a37db059f1757b4efc22",
            id1Type="email",
            id2Type="email_sha256",
            sourceType="sha256",
            logSource="preproc",
        ),
        dict(
            id1="ZZZ-A3D9-4E88-885F-BBD932F29BE3",
            id2="a49d399ef3334bd4811bbaad047zzz",
            id1Type="idfa",
            id2Type="mm_device_id",
            sourceType="app-metrica",
            logSource="mm",
        ),
        dict(
            # to short yandexuid 16 len
            id1="4685161573464276",
            id1Type="yandexuid",
            id2="66c1564994644fa9a5e73a43e934f004",
            id2Type="uuid",
            logSource="wl",
            sourceType="app-metrica-socket-android",
        ),
    ],
)
def test_row_to_soup_edge_invalid_id(row, frozen_time):
    """ Should check is correctly pack as proto invalid soup edge """
    serialized = row_to_soup_edge(frozen_time, row)
    assert serialized is None
    return
    assert isinstance(serialized, str)

    proto = TSoupEvent()
    proto.ParseFromString(base64.b64decode(serialized))
    assert frozen_time == proto.Unixtime
    proto = proto.Edge
    # check edge param value
    assert source_type.by_type(proto.SourceType).Name == row["sourceType"]
    assert log_source.by_type(proto.LogSource).Name == row["logSource"]
    # check vertices
    assert id_type.by_type(GenericID(proto=proto.Vertex1).type).Name == row["id1Type"]
    assert GenericID(proto=proto.Vertex1).value == row["id1"]
    assert id_type.by_type(GenericID(proto=proto.Vertex2).type).Name == row["id2Type"]
    assert GenericID(proto=proto.Vertex2).value == row["id2"]


@pytest.mark.parametrize(
    "row",
    [
        dict(
            id1="00023a85-d82e-45b3-b49a-55eaadce8fef",
            id2="070b657c757375ca8ce580f7e5ab158b",
            id1Type="gaid",
            id2Type="email_md5",
            sourceType="x-not-a-source",
            logSource="mm",
        ),
        dict(
            id1="00023a85-d82e-45b3-b49a-55eaadce8eee",
            id2="070b657c757372318ce580f7e5ab158b",
            id1Type="gaid",
            id2Type="phone_md5",
            sourceType="account-manager",
            logSource="x-not-a-log",
        ),
    ],
)
def test_row_to_soup_edge_invalid_ls(row, frozen_time):
    """ Should check is correctly pack as proto invalid log source """
    serialized = row_to_soup_edge(frozen_time, row)
    assert serialized is None


@pytest.mark.parametrize(
    "row",
    [
        dict(
            id="8189231241573622976",
            id_type="yandexuid",
            date_begin="2019-11-13",
            date_end="2019-11-15",
            os_name="Windows 7",
            os_version="6.1",
            os_family="Windows",
            browser_name="Chrome",
            browser_version="67.0.3396.99",
            is_browser=True,
            is_emulator=False,
            is_mobile=False,
            is_robot=False,
            is_tablet=False,
            is_touch=False,
            is_tv=False,
        ),
        dict(
            id="3189231241574118046",
            id_type="yandexuid",
            date_begin="2019-11-19",
            date_end="2019-11-19",
            os_name="Windows 8",
            os_version="6.2",
            os_family=None,
            browser_name="MSIE",
            browser_version="11.0",
            is_browser=True,
            is_emulator=False,
            is_mobile=False,
            is_robot=False,
            is_tablet=False,
            is_touch=False,
            is_tv=False,
        ),
        dict(
            id="8189231251571229234",
            id_type="icookie",
            date_begin="2019-11-09",
            date_end="2019-11-15",
            os_name="Android Pie",
            os_version="9",
            os_family="Android",
            browser_name="ChromeMobile",
            browser_version="78.0.3904",
            is_browser=True,
            is_emulator=False,
            is_mobile=True,
            is_robot=False,
            is_tablet=False,
            is_touch=True,
            is_tv=False,
        ),
    ],
)
def test_row_to_browser_info(row, frozen_time):
    """ Should check is correctly pack yandexuid/icookie eternal as proto """
    serialized = row_to_eternal_info(frozen_time, row)
    assert isinstance(serialized, str)

    proto = TIdsInfo()
    proto.ParseFromString(serialized)

    assert len(proto.BrowsersInfo) == 1
    assert len(proto.DevicesInfo) == 0
    assert len(proto.AppsInfo) == 0

    sub_proto = proto.BrowsersInfo[0]
    # check for content
    assert id_type.by_type(GenericID(proto=sub_proto.Id).type).Name == row["id_type"]
    assert GenericID(proto=sub_proto.Id).value == row["id"]

    assert sub_proto.DateBegin == (row["date_begin"] or "")
    assert sub_proto.DateEnd == (row["date_end"] or "")

    assert sub_proto.OsName == (row["os_name"] or "")
    assert sub_proto.OsVersion == (row["os_version"] or "")
    assert sub_proto.OsFamily == (row["os_family"] or "")

    assert sub_proto.BrowserName == (row["browser_name"] or "")
    assert sub_proto.BrowserVersion == (row["browser_version"] or "")

    for key, value in row.iteritems():
        if not key.startswith("is_"):
            continue
        proto_key = "Is{key}".format(key=key.replace("is_", "").capitalize())
        assert getattr(sub_proto, proto_key) == value


@pytest.mark.parametrize(
    "row",
    [
        dict(
            id_type="idfa",
            id="00000556-D2F9-47E0-852E-D724A02EC607",
            date_begin="2019-06-29",
            screen_width=667,
            os_version="13.1.3",
            os="ios",
            manufacturer="Apple",
            date_end="2019-10-27",
            screen_height=375,
            model="iPhone 7",
        ),
        dict(
            id_type="idfa",
            id="0000055B-12F4-4BCB-A21C-75BA3A5DEEA9",
            date_begin="2018-04-10",
            screen_width=667,
            os_version="11.1.2",
            os="ios",
            manufacturer="Apple",
            date_end="2018-05-16",
            screen_height=375,
            model="iPhone 7",
        ),
        dict(
            id_type="gaid",
            id="00000b69-6cf7-4582-b95a-500c241e02e0",
            date_begin="2018-06-10",
            screen_width=1920,
            os_version="7.0",
            os="android",
            manufacturer="Samsung",
            date_end="2018-06-15",
            screen_height=1080,
            model="Galaxy S7 Edge",
        ),
        dict(
            id_type="gaid",
            id="00000b6c-8155-4b16-a85c-c164e4425f98",
            date_begin="2018-03-26",
            screen_width=1920,
            os_version="8.0.0",
            os="android",
            manufacturer="Sony",
            date_end="2019-05-24",
            screen_height=1080,
            model="Xperia XZ Premium",
        ),
        dict(
            id_type="mm_device_id",
            id="00000000000000000000001a4d4c8436",
            date_begin="2018-03-01",
            screen_width=None,
            os_version="10.0.16299.0",
            os="windowsphone",
            manufacturer="Gigabyte Technology Co., Ltd.",
            date_end="2018-05-19",
            screen_height=None,
            model=None,
        ),
        dict(
            id_type="mm_device_id",
            id="00000000000000000000001a4d4c85ef",
            date_begin="2019-08-09",
            screen_width=1280,
            os_version="6.2.9200.0",
            os="windowsphone",
            manufacturer="Gigabyte Technology Co., Ltd.",
            date_end="2019-08-09",
            screen_height=1024,
            model="P35-S3",
        ),
    ],
)
def test_row_to_device_info(row, frozen_time):
    """ Should check is correctly pack idfa/gaid/mm_device_id eternal as proto """
    serialized = row_to_eternal_info(frozen_time, row)
    assert isinstance(serialized, str)

    proto = TIdsInfo()
    proto.ParseFromString(serialized)

    assert len(proto.BrowsersInfo) == 0
    assert len(proto.DevicesInfo) == 1
    assert len(proto.AppsInfo) == 0

    sub_proto = proto.DevicesInfo[0]
    # check for content
    assert id_type.by_type(GenericID(proto=sub_proto.Id).type).Name == row["id_type"]
    assert GenericID(proto=sub_proto.Id).value == row["id"]

    assert sub_proto.DateBegin == (row["date_begin"] or "")
    assert sub_proto.DateEnd == (row["date_end"] or "")

    assert sub_proto.OsName == (row["os"] or "")
    assert sub_proto.OsVersion == (row["os_version"] or "")

    assert sub_proto.Model == (row["model"] or "")
    assert sub_proto.Manufacturer == (row["manufacturer"] or "")

    assert sub_proto.ScreenWidth == (row["screen_width"] or 0)
    assert sub_proto.ScreenHeight == (row["screen_height"] or 0)


@pytest.mark.parametrize(
    "row",
    [
        dict(
            id_type="uuid",
            id="000001feada6394c823adbec34195cb6",
            date_begin="2018-11-08",
            api_keys={"13": 3, "177945": 1337},
            os="android",
            app_version="1.7.0.6",
            date_end="2018-11-18",
            app_id="com.jundroo.SimplePlanes",
        ),
        dict(
            id_type="uuid",
            id="000002006a08222858d4277f55797058",
            date_begin="2019-03-23",
            api_keys={"13": 3},
            os="android",
            app_version="1.16.03",
            date_end="2019-03-23",
            app_id="com.extremefungames.mototrafficrace2",
        ),
        dict(
            id_type="uuid",
            id="00000200711164771719c892f390da37",
            date_begin="2018-09-23",
            api_keys={"13": 8, "218300": 480, "22675": 49, "30488": 5388, "86701": 2},
            os="android",
            app_version="3.36",
            date_end="2019-01-03",
            app_id="ru.yandex.yandexnavi",
        ),
        dict(
            id_type="uuid",
            id="000002008e8fa67ad4303293ce15b7f9",
            date_begin="2018-06-11",
            api_keys={"13": 5, "482499": 6},
            os="android",
            app_version="1.0.0",
            date_end="2018-06-11",
            app_id="opencheats.gta4.com",
        ),
    ],
)
def test_row_to_app_info(row, frozen_time):
    serialized = row_to_eternal_info(frozen_time, row)
    assert isinstance(serialized, str)

    proto = TIdsInfo()
    proto.ParseFromString(serialized)

    assert len(proto.BrowsersInfo) == 0
    assert len(proto.DevicesInfo) == 0
    assert len(proto.AppsInfo) == 1

    sub_proto = proto.AppsInfo[0]
    # check for content
    assert id_type.by_type(GenericID(proto=sub_proto.Id).type).Name == row["id_type"]
    assert GenericID(proto=sub_proto.Id).value == row["id"]

    assert sub_proto.DateBegin == (row["date_begin"] or "")
    assert sub_proto.DateEnd == (row["date_end"] or "")

    assert sub_proto.OsName == (row["os"] or "")

    assert sub_proto.AppId == (row["app_id"] or "")
    assert sub_proto.AppVersion == (row["app_version"] or "")

    assert dict(sub_proto.ApiKeys) == row["api_keys"]


@pytest.mark.parametrize(
    "row",
    [
        dict(
            id1="00023a85-d82e-45b3-b49a-55eaadce8fef",
            id2="070b657c757375ca8ce580f7e5ab158b",
            id1Type="gaid",
            id2Type="email_md5",
            sourceType="account-manager",
            logSource="mm",
        ),
        dict(
            id1="00023a85-d82e-45b3-b49a-55eaadce8eee",
            id2="070b657c757372318ce580f7e5ab158b",
            id1Type="gaid",
            id2Type="phone_md5",
            sourceType="account-manager",
            logSource="mm",
        ),
        dict(
            id1="some@e.co",
            id2="f617c78ee13f2d74764adf17dabafde1e84b78e6a7b5a37db059f1757b4efc22",
            id1Type="email",
            id2Type="email_sha256",
            sourceType="sha256",
            logSource="preproc",
        ),
        dict(
            id1="000045BA-A3D9-4E88-885F-BBD932F29BE3",
            id2="a49d399ef3334bd4811bbaad0474f021",
            id1Type="idfa",
            id2Type="mm_device_id",
            sourceType="app-metrica",
            logSource="mm",
        ),
    ],
)
def test_row_to_vulture_skip_no_bochka(row, frozen_time):
    """ Should check is correct skip bochka edge for vulture """
    serialized = row_to_vulture(frozen_time, row)
    assert serialized is None


@pytest.mark.parametrize(
    "row",
    [
        dict(
            id1="1000024641385563342",
            id1Type="yandexuid",
            id2="098FB360-848B-44B7-8138-C190AD70F0E3",
            id2Type="distr_ui",
            logSource="eal",
            sourceType="yabro-ext-bro",
        ),
        dict(
            id1="1000046071537976599",
            id1Type="yandexuid",
            id2="8828ABE0-7F3B-4590-B097-3B1201A57465",
            id2Type="distr_ui",
            logSource="eal",
            sourceType="yabro-ext-bro",
        ),
        dict(
            id1="1000072971565097835",
            id1Type="yandexuid",
            id2="5B6BA49E-2AF9-4186-A3F9-492B5C335BBF",
            id2Type="distr_ui",
            logSource="eal",
            sourceType="yabro-ext-bro",
        ),
        dict(
            id1="1000006321529312000",
            id1Type="yandexuid",
            id2="01DF2DD5-08B8-11E7-8D8D-94DE80C8DCC3",
            id2Type="distr_ui",
            logSource="bar",
            sourceType="yasoft",
        ),
        dict(
            id1="1000008291534037351",
            id1Type="yandexuid",
            id2="83A705F8-7A46-410D-8348-5DD270DDC92A",
            id2Type="distr_ui",
            logSource="bar",
            sourceType="yasoft",
        ),
        dict(
            id1="1000028831527671684",
            id1Type="yandexuid",
            id2="5AC58EFC-2401-4A0F-814D-506BAC7932BE",
            id2Type="distr_ui",
            logSource="bar",
            sourceType="yasoft",
        ),
        dict(
            id1="1000002581509643609",
            id1Type="yandexuid",
            id2="EA23DB05-819D-F39B-95E9-2BDE49FF8A17",
            id2Type="distr_ui",
            logSource="eal",
            sourceType="yasoft",
        ),
        dict(
            id1="1000002641529335878",
            id1Type="yandexuid",
            id2="10F6D7C5-324D-FE28-1214-BC4EBC0292D4",
            id2Type="distr_ui",
            logSource="eal",
            sourceType="yasoft",
        ),
        dict(
            id1="1000003861494671260",
            id1Type="yandexuid",
            id2="9B7F8445-5283-4784-063C-640DA2800425",
            id2Type="distr_ui",
            logSource="eal",
            sourceType="yasoft",
        ),
        dict(
            id1="1000018831521370748",
            id1Type="yandexuid",
            id2="4387993621049318118",
            id2Type="icookie",
            logSource="wl",
            sourceType="cookie",
        ),
        dict(
            id1="1000031551574085341",
            id1Type="yandexuid",
            id2="426078241043076298",
            id2Type="icookie",
            logSource="wl",
            sourceType="cookie",
        ),
        dict(
            id1="1000206761562060222",
            id1Type="yandexuid",
            id2="2303216290957401706",
            id2Type="icookie",
            logSource="wl",
            sourceType="cookie",
        ),
        dict(
            id1="1000005491529127652",
            id1Type="yandexuid",
            id2="c7852e1b56d6582d8163806ed0e57136",
            id2Type="mm_device_id",
            logSource="access",
            sourceType="access-yp-did",
        ),
        dict(
            id1="1000016201555383771",
            id1Type="yandexuid",
            id2="ea6adbd1bf1e1de4fa745847d89be7bb",
            id2Type="mm_device_id",
            logSource="access",
            sourceType="access-yp-did",
        ),
        dict(
            id1="1000020051543874817",
            id1Type="yandexuid",
            id2="3761bd6014537d7a1a7fe1089211c9ca",
            id2Type="mm_device_id",
            logSource="access",
            sourceType="access-yp-did",
        ),
        dict(
            id1="1000010351552905384",
            id1Type="yandexuid",
            id2="7c8b6a1178a6873aa5aa8f310f8093e3",
            id2Type="mm_device_id",
            logSource="wl",
            sourceType="watch-yp-did-android",
        ),
        dict(
            id1="1000024921531732766",
            id1Type="yandexuid",
            id2="f19c2790d10ea7c05db59f411f2fe197",
            id2Type="mm_device_id",
            logSource="wl",
            sourceType="watch-yp-did-android",
        ),
        dict(
            id1="1000109621520270962",
            id1Type="yandexuid",
            id2="4f6e314d8dc640dc2895db47547d9444",
            id2Type="mm_device_id",
            logSource="wl",
            sourceType="watch-yp-did-android",
        ),
        dict(
            id1="1000181991504988346",
            id1Type="yandexuid",
            id2="670075814eea8aec0ea816ae9c87aa5b",
            id2Type="mm_device_id",
            logSource="wl",
            sourceType="watch-yp-did-ios",
        ),
        dict(
            id1="1000507441455906004",
            id1Type="yandexuid",
            id2="9bd78c328c43f063498e6069abd39d11",
            id2Type="mm_device_id",
            logSource="wl",
            sourceType="watch-yp-did-ios",
        ),
        dict(
            id1="1000557591524650671",
            id1Type="yandexuid",
            id2="b13250c14168f3cbf2312008c0c1155f",
            id2Type="mm_device_id",
            logSource="wl",
            sourceType="watch-yp-did-ios",
        ),
        dict(
            id1="1000000221568746079",
            id1Type="yandexuid",
            id2="2775c44f42884239a8c2a35c7ed69303",
            id2Type="uuid",
            logSource="access",
            sourceType="access-yp-did",
        ),
        dict(
            id1="1000136541572777280",
            id1Type="yandexuid",
            id2="300e7f3d6b5e44c7b3f883911cb581d3",
            id2Type="uuid",
            logSource="access",
            sourceType="access-yp-did",
        ),
        dict(
            id1="1000177991579034272",
            id1Type="yandexuid",
            id2="cf41185d560e4b0c96863d78f2f5311c",
            id2Type="uuid",
            logSource="access",
            sourceType="access-yp-did",
        ),
        dict(
            id1="1000001121568912204",
            id1Type="yandexuid",
            id2="cf719c08bfe0a4c9df19ff6b747c7534",
            id2Type="uuid",
            logSource="access",
            sourceType="app-url-redir",
        ),
        dict(
            id1="1000002211575766653",
            id1Type="yandexuid",
            id2="3946a52c8e924c8a8504585a8fa7c781",
            id2Type="uuid",
            logSource="access",
            sourceType="app-url-redir",
        ),
        dict(
            id1="1000003891511372745",
            id1Type="yandexuid",
            id2="19ad2c1b408e531ff26f7f3d3854e704",
            id2Type="uuid",
            logSource="access",
            sourceType="app-url-redir",
        ),
        dict(
            id1="1000004161471338972",
            id1Type="yandexuid",
            id2="70d896bf5ffe599f1bd670a55a80a52b",
            id2Type="uuid",
            logSource="redir",
            sourceType="app-url-redir",
        ),
        dict(
            id1="1000005151541943275",
            id1Type="yandexuid",
            id2="457a098ac9e81100a1d4d699c3a4e25e",
            id2Type="uuid",
            logSource="redir",
            sourceType="app-url-redir",
        ),
        dict(
            id1="1000005151541943275",
            id1Type="yandexuid",
            id2="469991242dd4140d9f931136acecb352",
            id2Type="uuid",
            logSource="redir",
            sourceType="app-url-redir",
        ),
        dict(
            id1="1000010411521366750",
            id1Type="yandexuid",
            id2="07457c7a61952a01625f43fcca83e15e",
            id2Type="uuid",
            logSource="wl",
            sourceType="app-url-redir",
        ),
        dict(
            id1="1000020751543689755",
            id1Type="yandexuid",
            id2="0fb158c9c5d450a3daf1f895de7668e0",
            id2Type="uuid",
            logSource="wl",
            sourceType="app-url-redir",
        ),
        dict(
            id1="1000077931557388140",
            id1Type="yandexuid",
            id2="7181d9fe11f936e0de5c68a4df56a045",
            id2Type="uuid",
            logSource="wl",
            sourceType="app-url-redir",
        ),
    ],
)
def test_row_to_vulture(row, frozen_time):
    """ Should check is correct pack bochka edge for vulture """
    serialized = row_to_vulture(frozen_time, row, prod_enable=True, exp_enable=False)
    assert isinstance(serialized, str)
    proto = TLinks()
    proto.ParseFromString(serialized)
    # check vulture usage
    assert proto.Usage == [EBbLinkUsage.PROD]
    # check source type
    assert source_type.by_type(proto.SourceType).Name == row["sourceType"]
    assert log_source.by_type(proto.LogSource).Name == row["logSource"]
    # check vertices
    assert id_type.by_type(proto.Vertices[0].IdType).Name == row["id1Type"]
    assert proto.Vertices[0].Id == row["id1"]
    assert id_type.by_type(proto.Vertices[1].IdType).Name == row["id2Type"]
    assert proto.Vertices[1].Id == row["id2"]
    # check extra info
    assert frozen_time == proto.LogEventTimestamp


TSKV_DATA = {
    "353": {
        "key": "1234",
        "value": "keyword=353\tyuid=1000000001504782564\tvalue=0100011101100101001110000101100001"
        "0011010110100101001001011001110100000101000001011100000101000101100101010000010010101101100001",
    },
    "723": {
        "key": "1234",
        "value": "keyword=723\tyuid=1000003191581909869\tvalue=14938493:3668429583:2366667574:427"
        "6644023:1969428879:1045981418:2926080368",
    },
}


@pytest.mark.parametrize("row", TSKV_DATA.values(), ids=TSKV_DATA.keys())
def test_tskv_to_serialized_proto(row, frozen_time):
    return base64.b64encode(tskv_to_serialized_proto(frozen_time, row))
