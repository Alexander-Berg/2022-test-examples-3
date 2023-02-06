import time

import pytest
from yatest import common as ycommon
from yt import wrapper as yt

from ads.bsyeti.big_rt.py_lib import YtQueuePath
from market.backbone.offers_store.yt_sync.settings import queues

ROW = {"value": "test", "codec": "none"}

ENV = {
    "YT_USER": "root",
    "YT_TOKEN": "yt_token",
    "YT_LOG_LEVEL": "Debug",
}


def get_args(cluster):
    # fmt: off
    return [
        ycommon.binary_path("market/backbone/offers_store/yt_sync/yt_sync"),
        "queue",
        "--stand", "test",
        "--cluster", cluster.get_proxy_address(),
        "--logpath", "./",
        "--verbose",
    ]
    # fmt: on


@pytest.mark.parametrize("queue", sorted(queues.QUEUES))
def test_create_queue(queue, yt_cluster):
    primary_cluster = yt_cluster.primary
    path = "//test/queue_%s" % queue.lower()

    ycommon.execute(
        get_args(primary_cluster)
        + [
            "--name",
            queue,
            "--path",
            path,
            "--commit",
        ],
        env=ENV,
        check_exit_code=True,
    )
    # check
    qpath = YtQueuePath(path)
    table_path = qpath.queue_table_path
    primary_client = primary_cluster.get_yt_client()
    assert primary_client.exists(table_path)
    assert primary_client.exists(qpath.consumers_dir_path)
    assert primary_client.exists(qpath.service_lock_dir_path)
    assert primary_client.exists(qpath.offsets_history_dir_path)
    assert primary_client.exists(qpath.consumer_offsets_table_path("test-consumer"))

    with yt.transaction.Transaction():
        primary_client.insert_rows(table_path, [ROW])

    while True:
        table_rows = list(primary_client.select_rows("[%s] from [%s]" % ("], [".join(ROW), table_path)))
        if 1 == len(table_rows):
            break
        time.sleep(1)

    assert 1 == len(table_rows)
    assert ROW == table_rows[0]


def test_recreate_queue(yt_cluster):
    queue = "datacamp"
    primary_cluster = yt_cluster.primary
    path = "//test/recreate_queue_%s" % queue.lower()

    for p in ([], ["--clean"], []):
        ycommon.execute(
            get_args(primary_cluster)
            + [
                "--name",
                queue,
                "--path",
                path,
                "--commit",
            ]
            + p,
            env=ENV,
            check_exit_code=True,
        )
        # check
        primary_client = primary_cluster.get_yt_client()
        assert primary_client.exists(YtQueuePath(path).queue_table_path)


def test_create_queue_dry_run(yt_cluster):
    queue = "datacamp"
    primary_cluster = yt_cluster.primary
    path = "//test/dryrun_queue_%s" % queue.lower()

    ycommon.execute(
        get_args(primary_cluster)
        + [
            "--name",
            queue,
            "--path",
            path,
        ],
        env=ENV,
        check_exit_code=True,
    )
    # check
    primary_client = primary_cluster.get_yt_client()
    assert not primary_client.exists(YtQueuePath(path).queue_table_path)


def test_create_queue_all(yt_cluster):
    primary_cluster = yt_cluster.primary
    ycommon.execute(
        get_args(primary_cluster)
        + [
            "--name",
            "all",
        ],
        env=ENV,
        check_exit_code=True,
    )
