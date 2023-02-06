import json
import pytest

from yatest import common
from mapreduce.yt.python.yt_stuff import YtStuff
from search.geo.tools.geocube.lib.runner import TRunner
from search.geo.tools.geocube.lib.commands import ALL_COMMANDS
from search.geo.tools.geocube.lib import extract_clicks_and_shows


FROZEN_DATE = "2019-10-28"

FILES = {
    "//statbox/statbox-dict-by-name/IPREG.st.bin/{date}".format(date=FROZEN_DATE): "test_data/2019_10_28.bin",
    "//statbox/statbox-dict-by-name/m_platforms_group_names.yaml/{date}".format(date=FROZEN_DATE): common.source_path("search/geo/tools/geocube/lib/ut/data/m_platforms_group_names.yaml"),
    "//statbox/statbox-dict-last/blockstat.dict": "test_data/blockstat.dict",
}

REQUIRED_MAP_NODES = [
    "//0/transaction_id",
    "//home/test/mmetrika",
    "//home/test/result",
    "//statbox/statbox-dict-by-name/IPREG.st.bin",
    "//statbox/statbox-dict-by-name/m_platforms_group_names.yaml",
    "//statbox/statbox-dict-last"
]


@pytest.fixture(scope="module")
def yt_fixture(request):
    yt_stuff = YtStuff()
    yt_stuff.start_local_yt()
    request.addfinalizer(yt_stuff.stop_local_yt)

    yt_client = yt_stuff.get_yt_client()
    yt_server = yt_stuff.get_server()

    for node in REQUIRED_MAP_NODES:
        yt_client.create("map_node", node, recursive=True, ignore_existing=True)

    for path, file_name in FILES.items():
        yt_client.write_file(path, open(common.runtime.work_path(file_name)))

    return yt_server, yt_client


def fill_tables(yt_client, tables):
    for table, file_path in tables.items():
        yt_client.create("table", table, recursive=True, ignore_existing=True)
        if file_path is not None:
            yt_client.write_table(table, json.load(open(common.runtime.work_path(file_path))))


def test_serp(yt_fixture, monkeypatch):
    yt_server, yt_client = yt_fixture
    monkeypatch.setattr(extract_clicks_and_shows, "YT_SPEC_DEFAULTS", {})
    fill_tables(yt_client, {
        "//user_sessions/pub/search/daily/{date}/clean".format(date=FROZEN_DATE): "test_data/user_sessions.json",
        "//home/logfeller/logs/map-reqans-log/1d/{date}".format(date=FROZEN_DATE): "test_data/map-req-ans.json",
        "//home/geosearch-prod/geocube/1d/{date}/serp".format(date=FROZEN_DATE): "test_data/geocube-serp.json",
    })

    TRunner(ALL_COMMANDS, [
        "do_calc",
        "--start-date", FROZEN_DATE,
        "--final-date", FROZEN_DATE,
        "--cluster=%s" % yt_server,
        "--gc-table-prefix", "//home/test/result",
        "--us-table-prefix", "//user_sessions/pub/search/daily",
        "--mr-table-prefix", "//home/logfeller/logs/map-reqans-log/1d",
        "--sort-results-by", "serpid",
        "--sort-results-by", "reqid",
        "--calculate-table", "serp",
    ]).run()
    test_records = list(yt_client.read_table("//home/test/result/{date}/serp".format(date=FROZEN_DATE)))
    return test_records


def test_mmetrika_cache(yt_fixture, monkeypatch):
    yt_server, yt_client = yt_fixture
    monkeypatch.setattr(extract_clicks_and_shows, "YT_SPEC_DEFAULTS", {})
    fill_tables(yt_client, {
        "//home/logfeller/logs/appmetrica-yandex-events/1d/{date}".format(date=FROZEN_DATE): common.source_path("search/geo/tools/geocube/lib/ut/data/mobile_metrika.json"),
        "//home/logfeller/logs/map-reqans-log/1d/{date}".format(date=FROZEN_DATE): None,
    })

    TRunner(ALL_COMMANDS, [
        "do_calc",
        "--start-date", FROZEN_DATE,
        "--final-date", FROZEN_DATE,
        "--cluster=%s" % yt_server,
        "--gc-table-prefix", "//home/test/result",
        "--mm-cache-prefix", "//home/test/mmetrics_cache",
        "--mm-table-prefix", "//home/logfeller/logs/appmetrica-yandex-events/1d",
        "--nv-table-prefix", "//home/logfeller/logs/appmetrica-yandex-events/1d",
        "--sort-results-by", "serpid",
        "--sort-results-by", "reqid",
        "--calculate-table", "mobile",
    ]).run()
    test_records = list(yt_client.read_table("//home/test/mmetrics_cache/{date}/{date}".format(date=FROZEN_DATE)))
    return test_records
