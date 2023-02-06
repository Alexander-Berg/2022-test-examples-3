import mock
from crypta.graph.fingerprint.match.appmetrica.lib import (
    CollectLocationFP,
    CollectPairsFromLocationFP,
    CollectLocationFPWithStats,
    CollectMobileFP,
    CollectMobileFPWithStats,
    FingerprintQuery,
)
from crypta.graph.fingerprint.lib import Paths
from crypta.lib.python.yql_runner.tests import load_fixtures, canonize_output, execute, clean_up
import yt.yson as yson


def select_all(yt, table_path):
    return list(yt.yt_client.read_table(table_path, format="json"))


def to_output_with_stats(yt, table):
    output_stats_path = "{}/@stats".format(table)
    return {table: sorted(select_all(yt, table)),
            output_stats_path: yson.yson_to_json(yt.yt_client.get(output_stats_path))}


@mock.patch.dict("os.environ", {"CRYPTA_ENVIRONMENT": "testing"})
@mock.patch.object(CollectLocationFPWithStats, "date", property(lambda self: "2020-07-21"))
@mock.patch.object(CollectLocationFP, "date", property(lambda self: "2020-07-21"))
@mock.patch.object(FingerprintQuery, "date", property(lambda self: "2020-07-21"))
@mock.patch.object(Paths, "expiration_duration", property(lambda self: -1))
@load_fixtures(
    ("//logs/appmetrica-location-log/1d/2020-07-21", "/fixtures/appmetrica-location-log.json")
)
@canonize_output
@clean_up(observed_paths=("//home", "//logs"))
def test_location_fp(local_yt):

    output_path = "//tmp/location_fp"
    task = CollectLocationFPWithStats(ids=output_path)
    execute(task)

    fingerprints = {record["id"]: record["fp"] for record in
                    local_yt.yt_client.read_table(output_path)}
    assert fingerprints["1"] == fingerprints["2"]
    assert fingerprints["3"] != fingerprints["2"]

    return to_output_with_stats(local_yt, output_path)


@mock.patch.dict("os.environ", {"CRYPTA_ENVIRONMENT": "testing"})
@mock.patch.object(CollectMobileFPWithStats, "date", property(lambda self: "2020-07-21"))
@mock.patch.object(CollectMobileFP, "date", property(lambda self: "2020-07-21"))
@mock.patch.object(FingerprintQuery, "date", property(lambda self: "2020-07-21"))
@mock.patch.object(Paths, "expiration_duration", property(lambda self: -1))
@load_fixtures(
    ("//logs/appmetrica-yandex-events/1d/2020-07-21", "/fixtures/appmetrica-yandex-events.json")
)
@canonize_output
@clean_up()
def test_mobile_fp(local_yt):

    output_path = "//tmp/mobile_fp"
    task = CollectMobileFPWithStats(ids=output_path)
    execute(task)

    fingerprints = {record["id"]: record["fp"] for record in
                    local_yt.yt_client.read_table(output_path)}
    assert fingerprints["0"] == fingerprints["1"]
    assert fingerprints["1"] != fingerprints["2"]
    assert fingerprints["2"] == fingerprints["3"]

    return to_output_with_stats(local_yt, output_path)


@mock.patch.dict("os.environ", {"CRYPTA_ENVIRONMENT": "testing"})
@mock.patch.object(CollectPairsFromLocationFP, "date", property(lambda self: "2020-07-21"))
@mock.patch.object(CollectLocationFP, "date", property(lambda self: "2020-07-21"))
@mock.patch.object(FingerprintQuery, "date", property(lambda self: "2020-07-21"))
@mock.patch.object(Paths, "expiration_duration", property(lambda self: -1))
@load_fixtures(
    ("//logs/appmetrica-location-log/1d/2020-07-21", "/fixtures/appmetrica-location-log.json")
)
@canonize_output
@clean_up(observed_paths=("//home", "//logs"))
def test_location_fp_pairs(local_yt):

    task = CollectPairsFromLocationFP()
    execute(task)
    output_path = str(task.output_pairs)

    return to_output_with_stats(local_yt, output_path)
