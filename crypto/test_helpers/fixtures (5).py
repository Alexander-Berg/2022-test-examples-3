import pytest
import yatest.common
import yt.clickhouse as chyt
import yt.clickhouse.test_helpers as chyt_test_helpers


# TODO(cherenkov-p-a) Make common code pulbic https://a.yandex-team.ru/arc/trunk/arcadia/yt/python/yt/clickhouse/tests/test_clickhouse.py?rev=r8452000#L29
def get_chyt_defaults():
    return {
        "memory_config": {
            "footprint": 1 * 1024**3,
            "clickhouse": int(2.5 * 1024**3),
            "reader": 1 * 1024**3,
            "uncompressed_block_cache": 0,
            "compressed_block_cache": 0,
            "chunk_meta_cache": 0,
            "log_tailer": 0,
            "watchdog_oom_watermark": 0,
            "watchdog_window_oom_watermark": 0,
            "clickhouse_watermark": 1 * 1024**3,
            "memory_limit": int((1 + 2.5 + 1 + 1) * 1024**3),
            "max_server_memory_usage": int((1 + 2.5 + 1) * 1024**3),
        },
        "host_ytserver_clickhouse_path": yatest.common.build_path("yt/chyt/server/bin/ytserver-clickhouse"),
        "host_clickhouse_trampoline_path": yatest.common.build_path("yt/chyt/trampoline/clickhouse-trampoline"),
        "host_ytserver_log_tailer_path": yatest.common.build_path("yt/yt/server/log_tailer/bin/ytserver-log-tailer"),
        "cpu_limit": 1,
        "enable_monitoring": False,
        "clickhouse_config": {},
        "max_instance_count": 100,
        "enable_job_tables": True,
        "cypress_log_tailer_config_path": "//sys/clickhouse/log_tailer_config",
        "log_tailer_table_attribute_patch": {"primary_medium": "default"},
        "log_tailer_tablet_count": 1,
        "skip_version_compatibility_validation": True,
    }


# TODO(cherenkov-p-a) Make common code pulbic https://a.yandex-team.ru/arc/trunk/arcadia/yt/python/yt/clickhouse/tests/test_clickhouse.py?rev=r8452000#L65
def setup_chyt_support(client):
    client.create("document", "//sys/clickhouse/defaults", recursive=True, attributes={"value": get_chyt_defaults()}, force=True)
    client.create("map_node", "//home/clickhouse-kolkhoz", recursive=True, force=True)
    client.link("//home/clickhouse-kolkhoz", "//sys/clickhouse/kolkhoz", recursive=True, ignore_existing=True)
    client.create("document", "//sys/clickhouse/log_tailer_config", attributes={"value": chyt_test_helpers.get_clickhouse_server_config()}, force=True)
    client.create("user", attributes={"name": "yt-clickhouse-cache"}, force=True)
    client.create("user", attributes={"name": "yt-clickhouse"}, force=True)
    if "superusers" not in client.get("//sys/users/yt-clickhouse/@member_of_closure"):
        client.add_member("yt-clickhouse", "superusers")


@pytest.fixture(scope="session")
def chyt_alias():
    return "*crypta"


# TODO(cherenkov-p-a) Make public YtStuff fixture with chyt support
@pytest.fixture(scope="module")
def local_yt_with_chyt(yt, chyt_alias):
    client = yt.get_yt_client()

    setup_chyt_support(client)
    chyt.start_clique(
        instance_count=1,
        alias=chyt_alias,
        cpu_limit=1,
        abort_existing=True,
        client=client,
        wait_for_instances=True,
        enable_monitoring=False,
    )
    return yt


@pytest.fixture
def clean_local_yt_with_chyt(local_yt_with_chyt, local_yt):
    for each in local_yt.yt_client.list('//home', absolute=True):
        if each in ('//home/clickhouse-kolkhoz',):
            continue
        local_yt.yt_client.remove(each, recursive=True)

    return local_yt_with_chyt
