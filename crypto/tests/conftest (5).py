import pytest

from yt import yson
import yt.wrapper as yt

from yql_utils import yql_binary_path

from crypta.graph.soup.config.python import EDGE_TYPE
from crypta.graph.soupy_indevice.lib import config

from crypta.graph.v1.python.v2.soup.soup_storage_yql_schema import SCHEMA


def create_table(ytc, path, schemadict):
    schemalist = []
    for typ, fields in schemadict.iteritems():
        for f in fields:
            schemalist.append({"name": f, "type": typ})

    schema = yson.YsonList(schemalist)
    schema.attributes["strict"] = True
    ytc.create_table(path, attributes=dict(schema=schema), ignore_existing=True, recursive=True)


@pytest.fixture(scope="module")
def indevice_soup(request, yt_stuff):
    ytc = yt_stuff.get_yt_client()
    edge_types = [et for et in EDGE_TYPE.values() if et.Props.DeviceBounds == et.Props.INDEVICE]

    soup_dir = "//soup/"
    ytc.mkdir(soup_dir[:-1])

    schema = {"string": ["id1", "id1Type", "id2", "id2Type", "sourceType", "logSource"], "any": ["dates"]}
    for et in edge_types:
        tbl = soup_dir + EDGE_TYPE.name(et)
        create_table(ytc, tbl, schema)
        yt.set_attribute(tbl, "_yql_row_spec", SCHEMA)

    data = [
        (
            "gaid",
            "deadbeef-deaf-beef-dead-deadbeefdead",
            "mm_device_id",
            "baadbaad-baad-baad-baad-baadbaadbaad",
            "app-metrica",
            "mm",
            "2018-12-01",
            "2018-12-12",
        ),
        (
            "yandexuid",
            "10555551500000000",
            "gaid",
            "deadbeef-deaf-beef-dead-deadbeefdead",
            "app-metrica-socket-android",
            "wl",
            "2018-12-01",
            "2018-12-12",
        ),
        (
            "yandexuid",
            "10666661500000000",
            "gaid",
            "deadbeef-deaf-beef-dead-deadbeefdead",
            "app-metrica-socket-android",
            "wl",
            "2018-12-12",
            "2018-12-18",
        ),
        (
            "yandexuid",
            "10777771500000000",
            "gaid",
            "deadbeef-deaf-beef-dead-deadbeefdead",
            "app-metrica-socket-android",
            "wl",
            "2018-12-01",
            "2018-12-18",
        ),
        (
            "yandexuid",
            "10888881500000000",
            "gaid",
            "deadbeef-deaf-beef-dead-deadbeefdead",
            "app-metrica-socket-android",
            "wl",
            "2018-12-01",
            "2018-12-18",
        ),
        (
            "yandexuid",
            "10555551500000000",
            "gaid",
            "abad1dea-abad-1dea-abad-1deaabad1dea",
            "app-metrica-socket-android",
            "wl",
            "2018-12-01",
            "2018-12-12",
        ),
        (
            "oaid",
            "deadbeef-deaf-beef-dead-deadbeefdead",
            "mm_device_id",
            "baadbaad-baad-baad-baad-baadbaadbaad",
            "app-metrica",
            "mm",
            "2018-12-01",
            "2018-12-12",
        ),
        (
            "yandexuid",
            "10555551500000000",
            "oaid",
            "deadbeef-deaf-beef-dead-deadbeefdead",
            "app-metrica-socket-android",
            "wl",
            "2018-12-01",
            "2018-12-12",
        ),
        (
            "yandexuid",
            "10666661500000000",
            "oaid",
            "deadbeef-deaf-beef-dead-deadbeefdead",
            "app-metrica-socket-android",
            "wl",
            "2018-12-12",
            "2018-12-18",
        ),
        (
            "yandexuid",
            "10777771500000000",
            "oaid",
            "deadbeef-deaf-beef-dead-deadbeefdead",
            "app-metrica-socket-android",
            "wl",
            "2018-12-01",
            "2018-12-18",
        ),
        (
            "yandexuid",
            "10888881500000000",
            "oaid",
            "deadbeef-deaf-beef-dead-deadbeefdead",
            "app-metrica-socket-android",
            "wl",
            "2018-12-01",
            "2018-12-18",
        ),
        (
            "yandexuid",
            "10555551500000000",
            "oaid",
            "abad1dea-abad-1dea-abad-1deaabad1dea",
            "app-metrica-socket-android",
            "wl",
            "2018-12-01",
            "2018-12-12",
        ),
        (
            "idfa",
            "F00FF00F-F00F-F00F-F00F-F00FF00FF00F",
            "mm_device_id",
            "DABBAD00-DABB-AD00-DABB-AD00DABBAD00",
            "app-metrica",
            "mm",
            "2018-12-01",
            "2018-12-12",
        ),
        (
            "yandexuid",
            "10111111500000000",
            "idfa",
            "F00FF00F-F00F-F00F-F00F-F00FF00FF00F",
            "app-metrica-socket-ios",
            "wl",
            "2018-12-01",
            "2018-12-12",
        ),
        (
            "yandexuid",
            "10222221500000000",
            "idfa",
            "F00FF00F-F00F-F00F-F00F-F00FF00FF00F",
            "app-metrica-socket-ios",
            "wl",
            "2018-12-06",
            "2018-12-12",
        ),
        (
            "gaid",
            "aaaaaaaa-aaaa-aaaa-aaaa-111111111111",
            "mm_device_id",
            "bbbbbbbb-bbbb-bbbb-bbbb-222222222222",
            "app-metrica",
            "mm",
            "2018-12-01",
            "2018-12-12",
        ),
        (
            "gaid",
            "aaaaaaaa-aaaa-aaaa-aaaa-111111111111",
            "mm_device_id",
            "cccccccc-cccc-cccc-cccc-333333333333",
            "app-metrica",
            "mm",
            "2018-12-01",
            "2018-12-12",
        ),
        (
            "gaid",
            "aaaaaaaa-aaaa-aaaa-aaaa-111111111111",
            "mm_device_id",
            "dddddddd-dddd-dddd-dddd-444444444444",
            "app-metrica",
            "mm",
            "2018-12-01",
            "2018-12-12",
        ),
        (
            "yandexuid",
            "10777771500000000",
            "mm_device_id",
            "DABBAD00-DABB-AD00-DABB-AD00DABBAD00",
            "access-yp-did",
            "access",
            "2018-12-02",
            "2018-12-02",
        ),
        (
            "mm_device_id",
            "DABBAD00-DABB-AD00-DABB-AD00DABBAD00",
            "uuid",
            "123456789012345678901234567890ab",
            "app-metrica",
            "mm",
            "2018-12-01",
            "2018-12-12",
        ),
        (
            "mm_device_id",
            "DABBAD00-DABB-AD00-DABB-AD00DABBAD00",
            "uuid",
            "cd123456789012345678901234567890",
            "app-metrica",
            "mm",
            "2018-12-01",
            "2018-12-12",
        ),
        (
            "mm_device_id",
            "DABBAD00-DABB-AD00-DABB-AD00DABBAD00",
            "uuid",
            "1234567890de12345678901234567890",
            "app-metrica",
            "mm",
            "2018-12-01",
            "2018-12-12",
        ),
    ]

    for d in data:
        tbl_name = soup_dir + "{}_{}_{}_{}".format(d[0], d[2], d[4], d[5])
        yt.write_table(
            yt.TablePath(tbl_name, append=True),
            [
                {
                    "id1Type": d[0],
                    "id1": d[1],
                    "id2Type": d[2],
                    "id2": d[3],
                    "sourceType": d[4],
                    "logSource": d[5],
                    "dates": [d[6], d[7]],
                }
            ],
        )


@pytest.fixture(scope="module")
def idstorage(request, yt_stuff):
    ytc = yt_stuff.get_yt_client()

    browser_schema = {
        "string": [
            "id_type",
            "id",
            "os_family",
            "os_version",
            "browser_name",
            "os_name",
            "browser_version",
            "date_begin",
            "date_end",
        ],
        "boolean": ["is_emulator", "is_touch", "is_browser", "is_robot", "is_tv", "is_tablet", "is_mobile"],
    }

    device_schema = {
        "string": ["id_type", "id", "date_begin", "date_end", "os_version", "os", "manufacturer", "model"],
        "int64": ["screen_height", "screen_width"],
    }

    app_schema = {
        "string": ["id_type", "id", "date_begin", "date_end", "os", "app_version", "app_id"],
        "any": ["api_keys"],
    }

    idstorage_dir = "//idstorage/"

    for idt in ["yandexuid", "icookie"]:
        table = "{}{}/eternal".format(idstorage_dir, idt)
        create_table(ytc, table, browser_schema)

        browsers = [
            ("10111111500000000", "somebrowser", "7.0", True),
            ("10222221500000000", "somebrowser", "7.0", True),
            ("10555551500000000", "somebrowser", "7.0", True),
            ("10666661500000000", "somebrowser", "7.0", True),
            ("10777771500000000", "otherbrowser", "7.0", True),
            ("10888881500000000", "somebrowser", "7.0", False),
        ]

        yt.write_table(
            table,
            [dict(id_type=idt, id=b[0], browser_name=b[1], browser_version=b[2], is_browser=b[3]) for b in browsers],
        )

    devices = {
        "gaid": [("abad1dea-abad-1dea-abad-1deaabad1dea", "ACME", "Emulator")],
        "oaid": [("abad1dea-abad-1dea-abad-1deaabad1de1", "ACME", "Emulator")],
        "idfa": [],
        "mm_device_id": [
            ("bbbbbbbb-bbbb-bbbb-bbbb-222222222222", "ACME", "PhoneOne"),
            ("cccccccc-cccc-cccc-cccc-333333333333", "ACME", "PhoneTwo"),
            ("dddddddd-dddd-dddd-dddd-444444444444", "ACME", "PhoneThree"),
        ],
    }

    apps = {
        ("123456789012345678901234567890ab", "android", "com.acme.app1", "1.1"),
        ("cd123456789012345678901234567890", "android", "com.acme.app1", "1.1"),
        ("1234567890de12345678901234567890", "android", "com.acme.app1", "2.2"),
    }

    for idt in ["idfa", "gaid", "oaid", "mm_device_id"]:
        table = "{}{}/eternal".format(idstorage_dir, idt)
        create_table(ytc, table, device_schema)
        yt.write_table(table, [dict(id_type=idt, id=d[0], manufacturer=d[1], model=d[2]) for d in devices[idt]])

    uuid_tbl = "{}uuid/eternal".format(idstorage_dir)
    create_table(ytc, uuid_tbl, app_schema)
    yt.write_table(uuid_tbl, [dict(id_type="uuid", id=a[0], os=a[1], app_id=a[2], app_version=a[3]) for a in apps])


@pytest.fixture(scope="module")
def indevice_conf(request, yt_stuff):
    config.YT_PROXY = yt_stuff.get_server()
    config.MRJOB_PATH = yql_binary_path("yql/tools/mrjob/mrjob")
    config.UDF_RESOLVER_PATH = yql_binary_path("yql/tools/udf_resolver/udf_resolver")
    config.UDFS_DIR = ";".join([yql_binary_path("yql/udfs"), yql_binary_path("ydb/library/yql/udfs")])
