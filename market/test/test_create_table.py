import time

import pytest
from uuid import uuid4
from yatest import common as ycommon
from yt import wrapper as yt

from market.backbone.offers_store.yt_sync.settings import tables
from market.backbone.offers_store.yt_sync.test.libs.helpers import gen_path, get_env

ENV = {
    "YT_USER": "root",
    "YT_TOKEN": "yt_token",
    "YT_LOG_LEVEL": "Debug",
}


ROWS = {
    "ContentOffers": {"YabsId": 1, "BusinessId": 1000},
    "ServiceOffers": {"YabsId": 1, "BusinessId": 1000, "WarehouseId": 100, "ShopId": 10},
}


def get_args():
    # fmt: off
    return [
        ycommon.binary_path("market/backbone/offers_store/yt_sync/yt_sync"),
        "table",
        "--stand", "test",
        "--verbose",
        "--logpath", "./",
    ]
    # fmt: on


@pytest.mark.parametrize("sync", [True, False])
@pytest.mark.parametrize("table", sorted(tables.TABLES))
def test_create_table(sync, table, yt_cluster):
    primary_cluster = yt_cluster.primary
    replica_cluster = yt_cluster.secondary
    path = "//test/%s%d" % (table, sync)

    ycommon.execute(
        get_args()
        + [
            "--name",
            table,
            "--path",
            path,
            "--commit",
        ],
        env=get_env(ENV, primary_cluster, replica_cluster, sync=sync),
        check_exit_code=True,
    )
    # check
    primary_client = primary_cluster.get_yt_client()
    replica_client = replica_cluster.get_yt_client()
    assert primary_client.exists(path)
    assert replica_client.exists(path)

    row = ROWS.get(table)
    assert row is not None, "please add ROWS for table %s" % table
    with yt.transaction.Transaction():
        primary_client.insert_rows(path, [row], require_sync_replica=sync)

    while True:
        table_rows = list(replica_client.select_rows("[%s] from [%s]" % ("], [".join(row), path)))
        if sync or 1 == len(table_rows):
            break
        time.sleep(1)

    assert 1 == len(table_rows)
    assert row == table_rows[0]


def test_recreate_table(yt_cluster):
    table = "ContentOffers"
    primary_cluster = yt_cluster.primary
    replica_cluster = yt_cluster.secondary
    path = "//test/recreate_%s" % table

    for p in ([], ["--clean"], []):
        ycommon.execute(
            get_args()
            + [
                "--name",
                table,
                "--path",
                path,
                "--commit",
            ]
            + p,
            env=get_env(ENV, primary_cluster, replica_cluster),
            check_exit_code=True,
        )
        # check
        primary_client = primary_cluster.get_yt_client()
        replica_client = replica_cluster.get_yt_client()
        assert primary_client.exists(path)
        assert replica_client.exists(path)


def test_create_non_replicated_table_when_no_replicas_set(yt_cluster):
    """Test that regular (non-replicated) table is created when no replicas set.

    Used only for tests to make environment simple.
    """
    table = "ContentOffers"
    primary_cluster = yt_cluster.primary
    replica_cluster = yt_cluster.secondary
    path = "//test/simple_%s" % table

    ycommon.execute(
        get_args()
        + [
            "--name",
            table,
            "--path",
            path,
            "--commit",
        ],
        env=get_env(ENV, primary_cluster),
        check_exit_code=True,
    )
    # check
    primary_client = primary_cluster.get_yt_client()
    replica_client = replica_cluster.get_yt_client()
    assert primary_client.exists(path)
    assert not replica_client.exists(path)

    assert "replicated_table_options" not in primary_client.get("%s/@" % path)

    row = ROWS[table]
    with yt.transaction.Transaction():
        primary_client.insert_rows(path, [row])

    table_rows = list(primary_client.select_rows("[%s] from [%s]" % ("], [".join(row), path)))
    assert 1 == len(table_rows)
    assert row == table_rows[0]


def test_create_table_dry_run(yt_cluster):
    table = "ServiceOffers"
    primary_cluster = yt_cluster.primary
    replica_cluster = yt_cluster.secondary
    path = "//test/dry_run_%s" % table

    ycommon.execute(
        get_args()
        + [
            "--name",
            table,
            "--path",
            path,
        ],
        env=get_env(ENV, primary_cluster),
        check_exit_code=True,
    )
    # check
    primary_client = primary_cluster.get_yt_client()
    replica_client = replica_cluster.get_yt_client()
    assert not primary_client.exists(path)
    assert not replica_client.exists(path)


def test_create_table_all(yt_cluster):
    primary_cluster = yt_cluster.primary

    ycommon.execute(
        get_args()
        + [
            "--name",
            "all",
        ],
        env=get_env(ENV, primary_cluster),
        check_exit_code=True,
    )


def test_exclusive_access_to_cluster_and_table(yt_cluster):
    primary_cluster = yt_cluster.primary

    args = get_args() + [
        "--commit",
        "--name",
    ]
    env = get_env(ENV, primary_cluster)

    pids = []
    name, path = "ContentOffers", gen_path("ContentOffers")
    for i in range(2):
        pid = ycommon.execute(args + [name, "--path", path], env=env, wait=False)
        pids.append(pid)

    pids[0].wait(check_exit_code=False)
    pids[1].wait(check_exit_code=False)

    good, bad = pids
    if good.returncode:
        good, bad = bad, good

    assert bad.returncode > 0
    assert b" since this child is locked by concurrent transaction" in bad.stderr

    assert good.returncode == 0

    # allow to run yt_sync for different tables on the same cluster
    path = gen_path()
    pids = [
        ycommon.execute(args + ["ContentOffers", "--path", path + "/ContentOffers"], env=env, wait=False),
        ycommon.execute(args + ["ServiceOffers", "--path", path + "/ServiceOffers"], env=env, wait=False),
    ]
    pids[0].wait(check_exit_code=True)
    pids[1].wait(check_exit_code=True)


@pytest.mark.parametrize("commit_arg, expected_min_data_ttl", [([], 1), (["--commit"], 0)])
def test_ensure_only_attributes(yt_cluster, commit_arg, expected_min_data_ttl):
    table = sorted(tables.TABLES)[0]
    primary_cluster = yt_cluster.primary
    replica_cluster = yt_cluster.secondary
    path = "//test/%s/%s" % (uuid4().hex, table)

    args = get_args() + ["--name", table, "--path", path]
    env = get_env(ENV, primary_cluster, replica_cluster)
    ycommon.execute(args + ["--commit"], env=env, check_exit_code=True)

    replica_client = replica_cluster.get_yt_client()

    # min_data_ttl is arbitrary attribute
    assert replica_client.get("%s/@min_data_ttl" % path) == 0
    replica_client.set("%s/@min_data_ttl" % path, 1)
    replica_client.remount_table(path)
    assert replica_client.get("%s/@min_data_ttl" % path) == 1

    args += ["--only-attributes"] + commit_arg
    ycommon.execute(args, env=env, check_exit_code=True)
    assert replica_client.get("%s/@min_data_ttl" % path) == expected_min_data_ttl


def test_do_not_allow_clean_and_only_attributes(yt_cluster):
    table = sorted(tables.TABLES)[0]
    primary_cluster = yt_cluster.primary
    replica_cluster = yt_cluster.secondary
    path = "//test/%s/%s" % (uuid4().hex, table)

    args = get_args() + [
        "--name",
        table,
        "--path",
        path,
        "--commit",
        "--only-attributes",
        "--clean",
    ]
    env = get_env(ENV, primary_cluster, replica_cluster)
    pid = ycommon.execute(args, env=env, check_exit_code=False)
    assert pid.returncode > 0
    assert b"Call to --only-attributes and --clean is ambiguous" in pid.stderr
