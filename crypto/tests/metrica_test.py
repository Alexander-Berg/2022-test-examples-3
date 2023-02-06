from __future__ import print_function

import json
import mock
import os
import pwd
import pytest

from crypta.graph.data_import.app_metrica_day.lib.task import AppMetrikaTask
from crypta.lib.python.yql_runner.tests import (
    canonize_output,
    clean_up,
    do_stream_test,
    load_fixtures,
    FakeDate,
    FakeDateTime,
)


def extract_dicts(row):
    def split_dict_simple(value):
        return dict(pair.split(":", 1) for pair in value.split(","))

    if "connection_hist" in row:
        row["connection_hist"] = split_dict_simple(row["connection_hist"])
    if "features" in row:
        row["features"] = {
            feature.split("-", 1)[0]: split_dict_simple(feature.split("-", 1)[1])
            for feature in row["features"].split(";")
            if len(feature.split("-", 1)) == 2
        }
    return row


@mock.patch("datetime.date", FakeDate)
@mock.patch("datetime.datetime", FakeDateTime)
@mock.patch("crypta.graph.data_import.app_metrica_day.lib.task.AppMetrikaTask._set_expiration", lambda self: 42)
@clean_up(observed_paths=("//home", "//and"))
@load_fixtures(
    (
        "//home/logfeller/logs/appmetrica-yandex-events/stream/5min/2020-11-01T17:35:00",
        "/fixtures/metrica.json",
        "/fixtures/metrica.spec.json",
    ),
    (
        "//home/logfeller/logs/appmetrica-external-events/stream/5min/2020-11-01T17:40:00",
        "/fixtures/metrica_private.json",
        "/fixtures/metrica.spec.json",
    ),
    (
        "//home/logfeller/logs/browser-metrika-mobile-log/stream/5min/2020-11-01T17:40:00",
        "/fixtures/yabro.json",
        "/fixtures/metrica.spec.json",
    ),
    (
        "//home/logfeller/logs/superapp-metrika-mobile-log/stream/5min/2020-11-01T17:40:00",
        "/fixtures/yabro.json",
        "/fixtures/metrica.spec.json",
    ),
    (
        "//home/crypta-tests/stuff/state/graph/v2/soup/preprocess/cross_mobmet",
        "/fixtures/cross_mobmet.json",
        "/fixtures/cross_mobmet.spec.json",
    ),
    (
        "//home/crypta-tests/stuff/state/graph/v2/soup/preprocess/metrika_params_owners",
        "/fixtures/metrika_params_owners.json",
        "/fixtures/metrika_params_owners.spec.json",
    ),
)
@canonize_output
def test_bt_task_simple(local_yt, conf):
    """ Should check is metrika bt task work correct """

    username = pwd.getpwuid(os.getuid())[0]
    local_yt.yt_client.create("map_node", "//tmp/{0}".format(username), recursive=True, ignore_existing=True)

    result = do_stream_test(AppMetrikaTask, local_yt, conf)
    result["extra_data"] = {key: map(extract_dicts, value) for key, value in result["extra_data"].iteritems()}
    return result


@pytest.mark.skip(reason="skip slow test")
@pytest.mark.parametrize(
    "source_tables", [None, ["//and/table/x3", "//home/another/some/path/to/table/x2", "//home/some/path/to/table/x1"]]
)
@mock.patch("datetime.date", FakeDate)
@mock.patch("datetime.datetime", FakeDateTime)
@mock.patch("crypta.graph.data_import.app_metrica_day.lib.task.AppMetrikaTask._set_expiration", lambda self: 42)
@clean_up(observed_paths=("//home", "//and"))
@load_fixtures(
    (
        "//home/logfeller/logs/appmetrica-yandex-events/stream/5min/2020-11-01T17:35:00",
        "/fixtures/metrica.json",
        "/fixtures/metrica.spec.json",
    ),
    (
        "//home/logfeller/logs/appmetrica-yandex-events/stream/5min/2020-11-01T17:40:00",
        "/fixtures/metrica.json",
        "/fixtures/metrica.spec.json",
    ),
    (
        "//home/logfeller/logs/appmetrica-external-events/stream/5min/2020-11-01T17:40:00",
        "/fixtures/metrica_private.json",
        "/fixtures/metrica.spec.json",
    ),
    (
        "//home/logfeller/logs/browser-metrika-mobile-log/stream/5min/2020-11-01T17:40:00",
        "/fixtures/yabro.json",
        "/fixtures/metrica.spec.json",
    ),
    ("//home/some/path/to/table/x1", "/fixtures/metrica.json", "/fixtures/metrica.spec.json"),
    ("//home/another/some/path/to/table/x2", "/fixtures/metrica.json", "/fixtures/metrica.spec.json"),
    ("//and/table/x3", "/fixtures/metrica_private.json", "/fixtures/metrica.spec.json"),
    (
        "//home/crypta-tests/stuff/state/graph/v2/soup/preprocess/cross_mobmet",
        "/fixtures/cross_mobmet.json",
        "/fixtures/cross_mobmet.spec.json",
    ),
    (
        "//home/crypta-tests/stuff/state/graph/v2/soup/preprocess/metrika_params_owners",
        "/fixtures/metrika_params_owners.json",
        "/fixtures/metrika_params_owners.spec.json",
    ),
)
@canonize_output
def test_bt_task(local_yt, conf, source_tables):
    """ Should check is metrika bt task work correct """

    username = pwd.getpwuid(os.getuid())[0]
    local_yt.yt_client.create("map_node", "//tmp/{0}".format(username), recursive=True, ignore_existing=True)

    cls = AppMetrikaTask if not source_tables else lambda: AppMetrikaTask(source_tables=json.dumps(source_tables))
    result = do_stream_test(cls, local_yt, conf)
    result["extra_data"] = {key: map(extract_dicts, value) for key, value in result["extra_data"].iteritems()}
    return result


@mock.patch("datetime.date", FakeDate)
@mock.patch("datetime.datetime", FakeDateTime)
@mock.patch("crypta.graph.data_import.app_metrica_day.lib.task.AppMetrikaTask._set_expiration", lambda self: 42)
@clean_up(observed_paths=("//home", "//and"))
@load_fixtures(
    (
        "//home/logfeller/logs/browser-metrika-mobile-log/stream/5min/2020-11-01T17:40:00",
        "/fixtures/yabro.json",
        "/fixtures/metrica.spec.json",
    )
)
@canonize_output
def test_bt_task_onelog(local_yt, conf):
    """ Should check is metrika bt task work correct """

    username = pwd.getpwuid(os.getuid())[0]

    local_yt.yt_client.create(
        "map_node",
        "//home/logfeller/logs/appmetrica-external-events/stream/5min",
        recursive=True,
        ignore_existing=True,
    )
    local_yt.yt_client.create(
        "map_node", "//home/logfeller/logs/appmetrica-yandex-events/stream/5min", recursive=True, ignore_existing=True
    )
    local_yt.yt_client.create(
        "map_node",
        "//home/logfeller/logs/browser-metrika-mobile-log/stream/5min",
        recursive=True,
        ignore_existing=True,
    )
    local_yt.yt_client.create(
        "map_node",
        "//home/logfeller/logs/superapp-metrika-mobile-log/stream/5min",
        recursive=True,
        ignore_existing=True,
    )
    local_yt.yt_client.create("map_node", "//tmp/{0}".format(username), recursive=True, ignore_existing=True)

    result = do_stream_test(AppMetrikaTask, local_yt, conf)
    return result
