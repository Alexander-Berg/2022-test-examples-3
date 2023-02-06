import datetime

import pytest

from crypta.lib.python import time_utils
from crypta.ltp.viewer.lib.structs.id import Id
from crypta.ltp.viewer.lib.structs.task import Task
from crypta.ltp.viewer.lib.structs.record import Record
from crypta.ltp.viewer.lib.ydb.client import Client

pytest_plugins = [
    "crypta.ltp.viewer.lib.test_helpers.fixtures",
]

WATCH_LOG = "WatchLog"


@pytest.fixture(scope="module")
def ydb_client(local_ydb, frozen_time):
    return Client(
        local_ydb.endpoint,
        local_ydb.database,
        "FAKE",
    )


@pytest.fixture
def expire_ttl():
    return datetime.timedelta(seconds=10000)


@pytest.fixture
def expired_history(ydb_client, frozen_time, expire_ttl):
    yuid = Id("yuid", "123")
    crypta_id = "123"

    time_utils.set_current_time(int(int(frozen_time) - 2 * expire_ttl.total_seconds()))
    tasks = [
        Task(yuid.id_type, yuid.id, WATCH_LOG, "2020-10-01"),
        Task(yuid.id_type, yuid.id, WATCH_LOG, "2020-10-02"),
    ]
    records = [
        Record(1600000000, "description", "additional_description"),
        Record(1600000002, "description2", "additional_description2"),
        Record(1600000003, "description3", "additional_description3"),
    ]

    ydb_client.add_graph([yuid], crypta_id, tasks, schedule_limit=1)
    for task in tasks:
        ydb_client.insert_chunk(task.date, task.log, records, Id(task.id_type, task.id))

    time_utils.set_current_time(frozen_time)
