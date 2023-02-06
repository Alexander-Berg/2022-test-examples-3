import datetime
import json
import time

import mock
import pytest
from rtmapreduce.protos.configuration_pb2 import TOperationProtoConfig

from crypta.lib.python.yt.dyntables import kv_client
from crypta.utils.rtmr_resource_service.bin.yt_state_updater.lib.yt_state_updater import YtStateUpdater
from crypta.utils.rtmr_resource_service.lib import consts
from crypta.utils.rtmr_resource_service.lib.db_client import DbClient
from crypta.utils.rtmr_resource_service.lib.reports_client import ReportsClient
from crypta.utils.rtmr_resource_service.lib.resource import Resource


CLUSTER1 = "cluster1"
CLUSTER2 = "cluster2"
CLUSTER3 = "cluster3"
OPERATION = "myop"
CLUSTERS = [CLUSTER1, CLUSTER2]
OP_ENABLED = {
    CLUSTER1: True,
    CLUSTER2: False,
}
TTL = datetime.timedelta(seconds=20)

UP_TO_DATE = "up_to_date"
UPDATED_IN_STABLE = "updated_in_stable"
NEW_IN_PRESTABLE = "new_in_prestable"

OLD_INSTANCE = "old_instance"
NEW_INSTANCE = "new_instance"


class MockSandboxClient(object):
    def __init__(self, state):
        self.state = state

    def get_last_released_resource_id(self, resource_type, release_type):
        return self.state[release_type].get(resource_type)


class MockRTMRClient(object):
    def __init__(self, host):
        cluster, domain = host.split(".", 1)
        assert domain == "search.yandex.net"
        self.cluster = cluster

    def get_operation_config(self, operation_name):
        assert OPERATION == operation_name
        proto = TOperationProtoConfig()
        proto.Attrs.Enabled = OP_ENABLED[self.cluster]
        return proto


@pytest.fixture
def resources():
    yield {
        name: Resource(name, name)
        for name in [UP_TO_DATE, UPDATED_IN_STABLE, NEW_IN_PRESTABLE]
    }


@pytest.fixture
def master_kv_client(clean_yt_kv):
    return kv_client.make_kv_client(clean_yt_kv.master.yt_client.config["proxy"]["url"], clean_yt_kv.master.path, token="FAKE")


@pytest.fixture
def db_client(master_kv_client):
    yield DbClient(master_kv_client)


@pytest.fixture
def reports_client(master_kv_client):
    yield ReportsClient(master_kv_client, {})


@pytest.fixture
def state_latest_resource(db_client):
    result = [
        (consts.STABLE, UP_TO_DATE, 20),
        (consts.STABLE, UPDATED_IN_STABLE, 30),
        (consts.PRESTABLE, UP_TO_DATE, 20),
        (consts.PRESTABLE, UPDATED_IN_STABLE, 31),
        (consts.TESTING, UP_TO_DATE, 20),
        (consts.TESTING, UPDATED_IN_STABLE, 31),
    ]

    for args in result:
        db_client.set_latest_resource_version(*args)

    yield result


@pytest.fixture
def instance_resources(state_latest_resource):
    yield {
        (name, id_)
        for _, name, id_ in state_latest_resource
    }


@pytest.fixture
def updater(resources, db_client, reports_client):
    sandbox_client = MockSandboxClient({
        consts.STABLE: {
            UP_TO_DATE: 20,
            UPDATED_IN_STABLE: 31,
        },
        consts.PRESTABLE: {
            NEW_IN_PRESTABLE: 10,
            UP_TO_DATE: 20,
            UPDATED_IN_STABLE: 31,
        },
        consts.TESTING: {
            NEW_IN_PRESTABLE: 10,
            UP_TO_DATE: 20,
            UPDATED_IN_STABLE: 31,
        },
    })

    cluster_envs = {
        "testing_cluster": CLUSTER3,
        "clusters": [CLUSTER1, CLUSTER2],
        "tracked_operation": OPERATION,
    }

    with mock.patch("crypta.utils.rtmr_resource_service.bin.yt_state_updater.lib.yt_state_updater.api.Client", MockRTMRClient):
        yield YtStateUpdater(
            resources,
            db_client,
            reports_client,
            sandbox_client,
            cluster_envs,
            report_ttl=TTL,
            instance_ttl=TTL,
            retry_opts={
                "tries": 1,
            },
        )


def dump(replica):
    replica.yt_client.freeze_table(replica.path, sync=True)

    result = {
        row["key"]: json.loads(row["value"], parse_float=lambda x: "float")
        for row in replica.yt_client.read_table(replica.path)
    }

    replica.yt_client.unfreeze_table(replica.path, sync=True)

    return result


def test_expire_instances(db_client, updater, clean_yt_kv):
    db_client.register_instance(OLD_INSTANCE)
    first_dump = dump(clean_yt_kv.replica)

    time.sleep(TTL.total_seconds())

    db_client.register_instance(NEW_INSTANCE)
    updater.expire_instances()

    second_dump = dump(clean_yt_kv.replica)

    return {
        "first": first_dump,
        "second": second_dump,
    }


def test_update_cluster_envs(updater, clean_yt_kv):
    updater.update_cluster_envs()

    return dump(clean_yt_kv.replica)


def test_update_resources(db_client, updater, clean_yt_kv, instance_resources):
    db_client.register_instance(NEW_INSTANCE)
    db_client.set_instance_resources(NEW_INSTANCE, instance_resources)

    updater.update_resources()

    first_dump = dump(clean_yt_kv.replica)

    db_client.set_instance_resources(NEW_INSTANCE, db_client.get_resources_to_download())
    updater.update_resources()

    second_dump = dump(clean_yt_kv.replica)

    return {
        "first": first_dump,
        "second": second_dump,
    }


def test_expire_reports(reports_client, updater, clean_yt_kv):
    reports_client.report_ok(consts.STABLE, UP_TO_DATE, 20, "id1")
    first_dump = dump(clean_yt_kv.replica)

    time.sleep(TTL.total_seconds())
    reports_client.report_ok(consts.PRESTABLE, UP_TO_DATE, 20, "id2")
    updater.expire_reports()
    second_dump = dump(clean_yt_kv.replica)

    return {
        "first": first_dump,
        "second": second_dump,
    }


def test_update(db_client, reports_client, updater, instance_resources, clean_yt_kv):
    db_client.register_instance(OLD_INSTANCE)
    db_client.set_instance_resources(OLD_INSTANCE, instance_resources)
    reports_client.report_ok(consts.STABLE, UP_TO_DATE, 20, "id1")

    time.sleep(TTL.total_seconds())

    db_client.register_instance(NEW_INSTANCE)
    db_client.set_instance_resources(NEW_INSTANCE, instance_resources)
    reports_client.report_ok(consts.PRESTABLE, UP_TO_DATE, 20, "id2")

    updater.update()

    first_dump = dump(clean_yt_kv.replica)

    db_client.register_instance(NEW_INSTANCE)
    db_client.set_instance_resources(NEW_INSTANCE, db_client.get_resources_to_download())
    updater.update()

    second_dump = dump(clean_yt_kv.replica)

    return {
        "first": first_dump,
        "second": second_dump,
    }
