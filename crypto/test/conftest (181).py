import pytest

from crypta.lib.python.yt import yt_helpers

pytest_plugins = [
    "crypta.lib.python.yt.local_tests.fixtures",
]


@pytest.fixture
def clean_yt_kv(yt_kv):
    replica = yt_kv.replica
    replica.disable_replica()
    replica.yt_client.unmount_table(replica.path, sync=True)
    replica.yt_client.remove(replica.path, force=True)
    replica.create_table()
    replica.prepare_dynamic()
    replica.enable_replica()

    yt_helpers.wait_for_mounted(replica.yt_client, replica.path)

    return yt_kv
