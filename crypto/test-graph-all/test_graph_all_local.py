import allure
import json
import pytest

from datetime import datetime, timedelta
from collections import defaultdict

from crypta.lib.python.identifiers.identifiers import GenericID
from crypta.lib.python.yql_runner.tests import canonize_output

from crypta.graph.soup.config.python import (  # noqa
    ID_TYPE as id_type,
    SOURCE_TYPE as source_type,
    LOG_SOURCE as log_source,
    EDGE_TYPE as edges,
)


TEST_FAILED = False

YT_DIR = "yt-data"
YT_ID = "yt-graph-all-local-test"
PROXY_PORT = 9013

DEVID_RAW_PATH = "//crypta/production/state/graph/indevice/2016-04-11/perfect/devid_month/"
PAIRS_PATH = "//crypta/production/state/graph/2016-04-11/pairs/"


def get_file_uploads():
    import yatest

    geodata = yatest.common.build_path("crypta/graph/v1/tests/sandbox-data/geodata4.bin")
    url_to_groups = yatest.common.build_path("crypta/graph/v1/tests/sandbox-data/UrlToGroups.yaml")
    dt = datetime.now() - timedelta(days=1)
    return [
        (geodata, "//statbox/statbox-dict-last/geodata4.bin"),
        (url_to_groups, dt.strftime("//statbox/statbox-dict-by-name/UrlToGroups.yaml/%Y-%m-%d")),
    ]


@pytest.mark.usefixtures("graph")
@pytest.mark.usefixtures("ytlocal")
@pytest.mark.usefixtures("crypta_env")
@pytest.mark.usefixtures("stream_import_dyntable")
class TestGraphAllLocal(object):
    def test_graph_run(self, graph):
        allure.attach("YT errors", (json.dumps(graph.report.errors, sort_keys=True, indent=4)))
        allure.attach("Max execution time", str(graph.report.max_time))
        assert not graph.report.errors
        assert graph.report.max_time < timedelta(minutes=10)
        if graph.run_status is None:
            assert 0, "Graph run status is not defined"
        assert graph.run_status, "Graph fail"

    @canonize_output
    def test_graph_canon_tables(self, graph):
        return sorted(
            [
                str(table)
                for root in ("//home", "//crypta", "//statbox", "//logs")
                for table in graph.yt.search(
                    root, node_type=("table",), follow_links=True, attributes=("_yql_row_spec",)
                )
            ]
        )

    def test_yuid_apps(self, graph):
        yuid_apps = "//crypta/production/state/graph/2016-04-11/yuid_apps"
        yuid_apps_upload = "//crypta/production/state/graph/2016-04-11/yuid_apps_upload"
        assert graph.yt.exists(yuid_apps)
        assert graph.yt.row_count(yuid_apps) > 0
        assert graph.yt.exists(yuid_apps_upload)
        assert graph.yt.row_count(yuid_apps_upload) > 0

    def test_no_dump_soup_dups(self, graph):
        logins = "//crypta/production/state/graph/v2/soup/puid_login_passport-profile_passport-dict"
        counts = defaultdict(int)
        for rec in graph.yt.read_table(logins):
            counts[(rec["id1"], rec["id2"])] += 1

        assert all([x == 1 for x in counts.values()])
        logins = set([x[1] for x in counts.keys()])
        assert "govshit" in logins

    def test_email_to_phone(self, graph):
        email_to_phone = "//crypta/production/state/graph/v2/soup/email_phone_email-to-phone_preproc"
        assert graph.yt.row_count(email_to_phone) == 1
        rec = next(graph.yt.read_table(email_to_phone))
        assert rec["id1"] == "89518545837@gmail.com"
        assert rec["id2"] == "+79518545837"

    def test_market_orders_log(self, graph):
        email_phone = "//crypta/production/state/graph/v2/soup/email_phone_orders_yandex-market"
        email_puid = "//crypta/production/state/graph/v2/soup/email_puid_orders_yandex-market"
        email_uuid = "//crypta/production/state/graph/v2/soup/email_uuid_orders_yandex-market"
        email_yandexuid = "//crypta/production/state/graph/v2/soup/email_yandexuid_orders_yandex-market"
        phone_puid = "//crypta/production/state/graph/v2/soup/phone_puid_orders_yandex-market"
        phone_uuid = "//crypta/production/state/graph/v2/soup/phone_uuid_orders_yandex-market"
        phone_yandexuid = "//crypta/production/state/graph/v2/soup/phone_yandexuid_orders_yandex-market"
        puid_uuid = "//crypta/production/state/graph/v2/soup/puid_uuid_orders_yandex-market"

        assert graph.yt.row_count(email_phone) == 1
        assert graph.yt.row_count(email_puid) == 2
        assert graph.yt.row_count(email_uuid) == 1
        assert graph.yt.row_count(email_yandexuid) == 1
        assert graph.yt.row_count(phone_puid) == 1
        assert graph.yt.row_count(phone_uuid) == 1
        assert graph.yt.row_count(phone_yandexuid) == 1
        assert graph.yt.row_count(puid_uuid) == 1

        rec = next(graph.yt.read_table(email_phone))
        assert rec["id1"] == "marketemail@orders.ru"
        assert rec["id2"] == "+79161234567"

    @pytest.mark.parametrize(
        "id_type,old_id",
        [
            ("yandexuid", "999999991543414614"),
            ("icookie", "999999991543414614"),
            ("idfa", "DEADBEEF-C0DE-CAFE-BABE-8BADF00DDEAD"),
            ("gaid", "deadbeef-c0de-cafe-babe-8badf00ddead"),
            ("mm_device_id", "deadbeef-c0de-cafe-babe-8badf00ddead"),
            ("uuid", "deadbeefc0decafebabe8badf00ddead"),
        ],
    )
    def test_eternal_idstorage(self, graph, id_type, old_id):
        info = list(graph.yt.read_table("//crypta/production/ids_storage/{}/eternal".format(id_type)))
        assert len(info) > 1
        assert len({x["id_type"] for x in info}) == 1
        assert info[0]["id_type"] == id_type
        assert old_id in {x["id"] for x in info}
        ids = [x["id"] for x in info]
        valid = [GenericID(id_type, x).is_valid() for x in ids]
        assert all(valid), "Waiter, there are invalid identifiers in my soup! (%s)" % str(zip(ids, valid))

    def test_yandex_drive_shared(self, graph):
        yandex_drive = "//crypta/production/state/graph/shared/yandex_drive/2016-04-11"
        assert graph.yt.exists(yandex_drive)
        assert graph.yt.row_count(yandex_drive) == 3
        rec = next(graph.yt.read_table(yandex_drive))
        assert rec["id"] == "00002907a397aea15dbfbdcf0472a112"
        assert rec["id_type"] == "mm_device_id"
        assert rec["source"] == "Yandex Drive"

        assert graph.yt.exists("//crypta/production/ids_storage/uuid/yandex_drive")
        assert graph.yt.row_count("//crypta/production/ids_storage/uuid/yandex_drive") == 2
        assert graph.yt.exists("//crypta/production/ids_storage/mm_device_id/yandex_drive")
        assert graph.yt.row_count("//crypta/production/ids_storage/mm_device_id/yandex_drive") == 1

    def test_merge_shared(self, graph):
        shared_merged = "//crypta/production/state/graph/shared/merged/2016-04-11"
        assert graph.yt.exists(shared_merged)
        assert graph.yt.row_count(shared_merged) == 5
        rec = next(graph.yt.read_table(shared_merged))
        assert rec["id"] == "00002907a397aea15dbfbdcf0472a112"
        assert rec["id_type"] == "mm_device_id"
        assert rec["source"] == ["Yandex Drive"]

        shared = "//crypta/production/ids_storage/shared/common_shared"
        assert graph.yt.exists(shared)
        assert graph.yt.row_count(shared) == 5
        rec = next(graph.yt.read_table(shared))
        assert rec["id"] == "00002907a397aea15dbfbdcf0472a112"
        assert rec["id_type"] == "mm_device_id"
        assert rec["shared_types"] == ["YANDEX_DRIVE"]

    def test_heuristic_desktop_shared_yuids(self, graph):
        heuristic_desktop_shared_yuids = "//crypta/production/state/graph/shared/heuristic_desktop_yuids/2016-04-11"
        assert graph.yt.exists(heuristic_desktop_shared_yuids)
        assert graph.yt.row_count(heuristic_desktop_shared_yuids) == 2
        rec = next(graph.yt.read_table(heuristic_desktop_shared_yuids))
        assert rec["id"] == "601826891455541119"
        assert rec["id_type"] == "yandexuid"
        assert rec["source"] == "Heuristic shared desktop yuid"

    @pytest.mark.parametrize(
        "id1_type, id2_type, source_type",
        (
            (id_type.LOGIN, id_type.EMAIL, source_type.LOGIN_TO_EMAIL),
            (id_type.EMAIL, id_type.PHONE, source_type.EMAIL_TO_PHONE),
            (id_type.EMAIL, id_type.EMAIL_MD5, source_type.MD5_HASH),
            (id_type.EMAIL, id_type.EMAIL_SHA256, source_type.SHA256_HASH),
            (id_type.EMAIL_MD5, id_type.EMAIL_SHA256, source_type.HASH_TO_HASH),
            (id_type.PHONE, id_type.PHONE_MD5, source_type.MD5_HASH),
        ),
    )
    def test_soup_cooked_soup_preprocessing_edges(self, graph, id1_type, id2_type, source_type):
        """Should check is soup preprocessing tables are correctly created"""
        table_path = "//crypta/production/state/graph/v2/soup/{table_name}".format(
            table_name=edges.name(edges.get_edge_type(id1_type, id2_type, source_type, log_source.SOUP_PREPROCESSING))
        )
        assert graph.yt.exists(table_path)
        assert graph.yt.row_count(table_path) > 0
        record = next(graph.yt.read_table(table_path))
        assert record["id1Type"] == id1_type.Name
        assert record["id2Type"] == id2_type.Name
        assert record["sourceType"] == source_type.Name
        # check is identifiers are valid
        assert GenericID(id1_type.Name, record["id1"]).is_valid()
        assert GenericID(id2_type.Name, record["id2"]).is_valid()
