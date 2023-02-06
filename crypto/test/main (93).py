import os
import re
import time

from crypta.lib.python.yt.dyntables.kv_replica import KvReplica
from crypta.lib.python.yt import yt_helpers
from crypta.spine.pushers.yt_replicated_table_checker.lib import check


def run_test(juggler_client, replicated_table):
    os.environ["YT_TOKEN"] = "FAKE"
    check.check_replicated_table(juggler_client, replicated_table)

    return juggler_client.dump_events_requests()


def test_healthy(db_setup, juggler_client, replicated_table):
    return run_test(juggler_client, replicated_table)


def test_missing_tables(db_setup, clusters, juggler_client, replicated_table, table_path):
    for yt in clusters.values():
        yt.get_yt_client().remove(table_path)

    return run_test(juggler_client, replicated_table)


def test_unmounted_tables(db_setup, clusters, juggler_client, replicated_table, table_path):
    for yt in clusters.values():
        yt_client = yt.get_yt_client()
        yt_client.unmount_table(table_path)
        yt_helpers.wait_for_unmounted(yt_client, table_path)

    return run_test(juggler_client, replicated_table)


def test_wrong_replicas(db_setup, juggler_client, replicated_table):
    master, replicas = db_setup
    missing_replica = replicas[0]
    missing_replica.delete_replica()
    extra_replica = KvReplica.create(master, missing_replica.cluster_name, "//another_table", missing_replica.yt_client)

    result = run_test(juggler_client, replicated_table)
    extra_replica.delete_replica()
    return result


def test_replication_errors(db_setup, juggler_client, replicated_table, table_path):
    master, replicas = db_setup
    for replica in replicas:
        master.yt_client.alter_table_replica(replica.replica_id, mode="async")
        replica.yt_client.unmount_table(table_path)
        yt_helpers.wait_for_unmounted(replica.yt_client, table_path)

    master.yt_client.set_attribute("#{}".format(replicas[0].replica_id), "enable_replicated_table_tracker", False)
    master.yt_client.insert_rows(master.path, [{"xxx": 1, "yyy": 2}], require_sync_replica=False)
    time.sleep(10)

    requests = run_test(juggler_client, replicated_table)

    for event in requests:
        if event["service"].startswith("master"):
            event["description"] = re.sub(r"replication lag time: \d+", "replication lag time: lag", event["description"])

    return requests


def test_cluster_unavailable(juggler_client, unavailable_replicated_table):
    requests = run_test(juggler_client, unavailable_replicated_table)

    for event in requests:
        del event["description"]

    return requests
