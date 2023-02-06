import os

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
TX_LOG = "TxLog"


@pytest.fixture(scope="module")
def yuid():
    return Id("yuid", "1")


@pytest.fixture(scope="module")
def yuid2():
    return Id("yuid", "2")


@pytest.fixture(scope="module")
def crypta_id():
    return "111"


@pytest.fixture(scope="module")
def crypta_id2():
    return "222"


@pytest.fixture(scope="module")
def frozen_time():
    result = "1590000000"
    os.environ[time_utils.CRYPTA_FROZEN_TIME_ENV] = result
    yield result


@pytest.fixture(scope="module")
def ydb_client(local_ydb, frozen_time):
    return Client(
        local_ydb.endpoint,
        local_ydb.database,
        "FAKE",
    )


@pytest.fixture(scope="module")
def history(yuid, yuid2, crypta_id, crypta_id2, ydb_client):
    tasks = [
        Task(yuid.id_type, yuid.id, TX_LOG, "2020-10-01"),
        Task(yuid.id_type, yuid.id, TX_LOG, "2020-10-03"),
        Task(yuid.id_type, yuid.id, TX_LOG, "2020-10-04"),
        Task(yuid.id_type, yuid.id, WATCH_LOG, "2020-10-05"),
    ]
    records = [
        Record(1600000000, "description", "additional_description"),
        Record(1600000002, "description2", "additional_description2"),
        Record(1600000003, "description3", "additional_description3"),
    ]

    scheduled = ydb_client.add_graph([yuid], crypta_id, tasks, schedule_limit=1)
    for _ in range(2):
        for row in scheduled:
            scheduled = ydb_client.insert_chunk(row.date, row.log, records, Id(row.id_type, row.id))

    with ydb_client.get_session() as session, ydb_client.get_transaction(session) as tx:
        ydb_client.update_progress(session, tx, "2020-10-01", TX_LOG, yuid, crypta_id, "failed")

    ydb_client.add_graph([yuid2], crypta_id2, [Task(yuid2.id_type, yuid2.id, TX_LOG, "2020-10-01")], schedule_limit=1)
    ydb_client.insert_chunk("2020-10-01", TX_LOG, records, Id(yuid2.id_type, yuid2.id))
