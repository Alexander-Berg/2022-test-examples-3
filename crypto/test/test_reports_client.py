import datetime
import time

import pytest

from crypta.lib.python.yt.dyntables import kv_client
from crypta.utils.rtmr_resource_service.lib.reports_client import ReportsClient

STABLE = "stable"
RESOURCE_1 = "resource1"
RESOURCE_2 = "resource2"
VERSION = 1
CLIENT_ID_1 = "client_id_1"
CLIENT_ID_2 = "client_id_2"
CLIENT_ID_3 = "client_id_3"


def create_client(yt_kv):
    return ReportsClient(
        kv_client.make_kv_client(yt_kv.master.yt_client.config["proxy"]["url"], yt_kv.master.path, token="FAKE"),
        {"tries": 1},
    )


@pytest.fixture
def reports_client(yt_kv):
    yield create_client(yt_kv)


def test_reports(reports_client):
    def dump():
        result = {}

        for resource in [RESOURCE_1, RESOURCE_2]:
            counts = reports_client.get_report_counts(STABLE, resource, VERSION)
            result["{}:ok_count".format(resource)] = counts.ok
            result["{}:version_count".format(resource)] = counts.version

        return result

    reports_client.report_version(STABLE, RESOURCE_1, VERSION, CLIENT_ID_1)
    reports_client.report_version(STABLE, RESOURCE_1, VERSION, CLIENT_ID_2)
    reports_client.report_ok(STABLE, RESOURCE_1, VERSION, CLIENT_ID_1)
    reports_client.report_ok(STABLE, RESOURCE_1, VERSION, CLIENT_ID_2)
    reports_client.report_version(STABLE, RESOURCE_1, VERSION, CLIENT_ID_2)

    result = {"first": dump()}

    reports_client.report_version(STABLE, RESOURCE_2, VERSION, CLIENT_ID_2)
    reports_client.report_ok(STABLE, RESOURCE_2, VERSION, CLIENT_ID_2)
    reports_client.report_ok(STABLE, RESOURCE_2, VERSION, CLIENT_ID_2)

    result["second"] = dump()

    time.sleep(5)
    reports_client.report_version(STABLE, RESOURCE_2, VERSION, CLIENT_ID_3)
    reports_client.report_version(STABLE, RESOURCE_1, VERSION, CLIENT_ID_1)
    reports_client.expire_reports(datetime.timedelta(seconds=5))

    result["third"] = dump()

    return result
