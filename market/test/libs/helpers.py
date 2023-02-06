import time
from uuid import uuid4

from yt import wrapper as yt


def gen_path(name=None):
    path = "//test/" + uuid4().hex
    if name is not None:
        path += "/" + name
    return path


def get_env(env, primary, replica=None, sync=True):
    env = env.copy()
    env["TEST_PRIMARY"] = primary.get_uri()

    if replica:
        if sync:
            env["TEST_SYNC_REPLICA"] = replica.get_uri()
        else:
            env["TEST_ASYNC_REPLICA"] = replica.get_uri()
    return env


def get_common_args(table, path):
    # fmt: off
    return [
        "--name", table,
        "--stand", "test",
        "--path", path,
        "--logpath", "./",
        "--verbose"]
    # fmt: on


def select_rows(client, query):
    for i in range(10):
        try:
            return list(client.select_rows(query))
        except Exception as exc:
            error = exc
            # Chunk data is not preloaded yet
            time.sleep(0.5)
    raise error


def insert_rows(client, table, rows, sync=True):
    with yt.Transaction():
        client.insert_rows(table, rows, require_sync_replica=sync)
