import io

import library.python.resource as rs
import pytest
import yatest
import yatest.common.network
import yt.wrapper as yt

from crypta.ltp.viewer.lib.test_helpers.ltp_viewer_api import LtpViewerApi
from crypta.ltp.viewer.lib.test_helpers.ltp_viewer_worker import LtpViewerWorker
from crypta.lib.python import time_utils
from crypta.lib.python.yt import (
    schema_utils,
    yt_helpers,
)


pytest_plugins = [
    "crypta.lib.python.chyt.test_helpers.fixtures",
    "crypta.lib.python.logbroker.test_helpers.fixtures",
    "crypta.lib.python.ydb.test_helpers.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture(scope="session")
def frozen_time():
    yield "1600000000"


@pytest.fixture
def ltp_viewer_api(logbroker_config, frozen_time, local_ydb):
    env = {
        time_utils.CRYPTA_FROZEN_TIME_ENV: frozen_time,
        "YDB_TOKEN": "FAKE",
    }
    with yatest.common.network.PortManager() as port_manager:
        with LtpViewerApi(
            working_dir=yatest.common.test_output_path(),
            port=port_manager.get_port(),
            logbroker_config=logbroker_config,
            local_ydb=local_ydb,
            env=env,
        ) as service:
            yield service


@pytest.fixture
def ltp_viewer_api_client(ltp_viewer_api):
    return ltp_viewer_api.create_client()


@pytest.fixture
def ltp_viewer_api_config(ltp_viewer_api):
    return ltp_viewer_api.config


def create_yabs_dyntable(yt_client, name, schema, path):
    yt_client.create("table", path=path, recursive=True, attributes={"schema": schema})
    yt_client.write_table(path, io.BytesIO(rs.find(name)), yt.format.YsonFormat(format="text"), raw=True)

    yt_client.alter_table(path, dynamic=True)
    yt_client.mount_table(path, sync=True)
    yt_helpers.wait_for_mounted(yt_client, path)
    return path


@pytest.fixture
def pages(local_yt):
    schema = schema_utils.yt_dyntable_schema_from_dict(
        {
            "PageID": "uint64",
            "Name": "string",
            "Description": "string",
        },
        ["PageID"]
    )
    return create_yabs_dyntable(local_yt.get_yt_client(), "/data/pages.yson", schema, "//pages")


@pytest.fixture
def categories(local_yt):
    schema = schema_utils.yt_dyntable_schema_from_dict(
        {
            "BMCategoryID": "uint64",
            "Description": "string",
        },
        ["BMCategoryID"]
    )
    return create_yabs_dyntable(local_yt.get_yt_client(), "/data/categories.yson", schema, "//categories")


@pytest.fixture
def regions(local_yt):
    schema = schema_utils.yt_dyntable_schema_from_dict(
        {
            "RegionID": "uint64",
            "Name": "string",
            "Lang": "string",
        },
        ["RegionID"]
    )
    return create_yabs_dyntable(local_yt.get_yt_client(), "/data/regions.yson", schema, "//regions")


@pytest.fixture
def ltp_viewer_worker(logbroker_config, local_ydb, clean_local_yt_with_chyt, ltp_viewer_api, frozen_time, local_yt_and_yql_env, pages, categories, regions):
    env = dict(local_yt_and_yql_env)
    env[time_utils.CRYPTA_FROZEN_TIME_ENV] = frozen_time
    env["YDB_TOKEN"] = "FAKE"
    with yatest.common.network.PortManager() as port_manager:
        with LtpViewerWorker(
            ltp_viewer_api=ltp_viewer_api,
            working_dir=yatest.common.test_output_path(),
            logbroker_config=logbroker_config,
            yt_proxy=clean_local_yt_with_chyt.get_server(),
            stats_host="localhost",
            stats_port=port_manager.get_port(),
            local_ydb=local_ydb,
            pages=pages,
            categories=categories,
            regions=regions,
            env=env,
        ) as service:
            yield service


@pytest.fixture
def ltp_viewer_worker_config(ltp_viewer_worker):
    return ltp_viewer_worker.config
