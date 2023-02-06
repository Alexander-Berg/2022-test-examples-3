from crypta.graph.fpc.lib.tasks import (
    GetFpcFromAdstat,
    GetFpcFromWatchLog,
    GetFpcViaCookieSync,
    GetFpcViaExtfp,
    GetFpcViaFingerprint,
    GetFpcViaSerp,
    GetFpcViaTls,
)
from crypta.lib.python.yql_runner.tests import canonize_output, clean_up, load_fixtures
from crypta.lib.python.zk import fake_zk_client
from crypta.lib.python.bt.workflow import execute_sync
import crypta.graph.fpc.proto.config_pb2 as config
import library.python.resource as rs
import crypta.lib.python.bt.conf.conf as conf

import mock
import os

os.environ["ENVIRONMENT"] = "testing"

proto_config = config.TCryptaFpcConfig()
conf.use_proto(proto_config, defaults=rs.find(proto_config.DefaultsPath))


def execute(task):
    with fake_zk_client() as fake_zk:
        execute_sync(task, fake_zk, do_fork=False)


@clean_up(observed_paths=("//home", "//logs"))
@load_fixtures(
    ("//logs/bs-watch-log/1h/2019-10-03T10:00:00", "/fixtures/bs-watch-log.json"),
    ("//home/crypta/testing/state/graph/fpc/Index", "/fixtures/index.json"),
)
@canonize_output
def test_watchlog_task(local_yt):
    with mock.patch("solomon.solomon.BasePushApiReporter._push"):
        execute(GetFpcFromWatchLog())
    result = local_yt.yt_wrapper.read_table(
        "//home/crypta/testing/state/graph/fpc/fresh/2019-10-03T10:00:00_GetFpcFromWatchLog", format="json"
    )
    return [r for r in result.rows]


@clean_up(observed_paths=("//home", "//logs"))
@load_fixtures(
    ("//home/crypta/testing/state/graph/fpc/fingerprints/2021-12-07T10:00:00", "/fixtures/fingerprints.json"),
)
@canonize_output
def test_fingerprint_task(local_yt):
    with mock.patch("solomon.solomon.BasePushApiReporter._push"):
        execute(GetFpcViaFingerprint())
    result = local_yt.yt_wrapper.read_table(
        "//home/crypta/testing/state/graph/fpc/fresh/2021-12-07T10:00:00_GetFpcViaFingerprint", format="json"
    )
    return [r for r in result.rows]


@clean_up(observed_paths=("//home", "//logs"))
@load_fixtures(
    ("//logs/adstat-nginx-log/30min/2021-12-07T10:00:00", "/fixtures/adstat-log.json"),
    ("//home/crypta/testing/state/graph/fpc/Index", "/fixtures/index.json"),
)
@canonize_output
def test_adstat_task(local_yt):
    task = GetFpcFromAdstat()
    task.MAX_TABLES_PER_RUN = 1
    task.MIN_TABLES_PER_RUN = 1

    with mock.patch("solomon.solomon.BasePushApiReporter._push"):
        execute(task)
    result = local_yt.yt_wrapper.read_table(
        "//home/crypta/testing/state/graph/fpc/fresh/2021-12-07T10:00:00_GetFpcFromAdstat", format="json"
    )
    return [r for r in result.rows]


@clean_up(observed_paths=("//home", "//logs"))
@load_fixtures(
    ("//logs/bs-watch-log/1h/2019-10-03T10:00:00", "/fixtures/bs-watch-log-cookie-sync.json"),
    ("//home/crypta/testing/state/graph/fpc/Index", "/fixtures/index.json"),
)
@canonize_output
def test_cookie_sync_task(local_yt):
    with mock.patch("solomon.solomon.BasePushApiReporter._push"):
        execute(GetFpcViaCookieSync())
    result = local_yt.yt_wrapper.read_table(
        "//home/crypta/testing/state/graph/fpc/fresh/2019-10-03T10:00:00_GetFpcViaCookieSync", format="json"
    )
    return [r for r in result.rows]


@clean_up(observed_paths=("//home", "//logs"))
@load_fixtures(
    ("//logs/crypta-prod-ext-fp-match-log/stream/5min/2022-01-24T17:00:00", "/fixtures/ext-fp-matched-log.json"),
    ("//home/crypta/testing/state/graph/fpc/Index", "/fixtures/index.json"),
)
@canonize_output
def test_extfp_task(local_yt):

    task = GetFpcViaExtfp()
    task.MINIMUM_TABLES_TO_JOIN = 1

    with mock.patch("solomon.solomon.BasePushApiReporter._push"):
        execute(task)

    result = local_yt.yt_wrapper.read_table(
        "//home/crypta/testing/state/graph/fpc/fresh/2022-01-24T17:00:00_GetFpcViaExtfp", format="json"
    )

    return [r for r in result.rows]


@clean_up(observed_paths=("//home", "//logs"))
@load_fixtures(
    ("//logs/bs-watch-log/1h/2019-10-03T09:00:00", "/fixtures/bs-watch-log-tls-prev.json"),
    ("//logs/bs-watch-log/1h/2019-10-03T10:00:00", "/fixtures/bs-watch-log-tls.json"),
    ("//home/crypta/testing/state/graph/fpc/Index", "/fixtures/index.json"),
)
@canonize_output
def test_tls_task(local_yt):
    task = GetFpcViaTls()

    with mock.patch("solomon.solomon.BasePushApiReporter._push"):
        execute(task)

    result = local_yt.yt_wrapper.read_table(
        "//home/crypta/testing/state/graph/fpc/fresh/2019-10-03T10:00:00_GetFpcViaTls", format="json"
    )
    with_yuid = [r for r in result.rows]

    return with_yuid


@clean_up(observed_paths=("//home", "//logs"))
@load_fixtures(
    ("//home/crypta/testing/state/graph/fpc/Index", "/fixtures/index.json"),
    (
        "//logs/bs-watch-log/1h/2019-10-03T10:00:00",
        "/fixtures/bs-watch-log-ysclid.json",
        "/fixtures/bs-watch-log.spec.json",
    ),
    (
        "//logs/bs-watch-log/1h/2019-10-03T11:00:00",
        "/fixtures/bs-watch-log-ipfp.json",
        "/fixtures/bs-watch-log.spec.json",
    ),
    (
        "//logs/redir-log/30min/2019-10-03T09:30:00",
        "/fixtures/redir-log-ysclid.json",
        "/fixtures/redir-log.spec.json",
    ),
    (
        "//logs/redir-log/30min/2019-10-03T10:00:00",
        "/fixtures/redir-log-ipfp.json",
        "/fixtures/redir-log.spec.json",
    ),
    (
        "//logs/redir-log/30min/2019-10-03T10:30:00",
        "/fixtures/redir-log-ysclid.json",
        "/fixtures/redir-log.spec.json",
    ),
    (
        "//logs/redir-log/30min/2019-10-03T11:00:00",
        "/fixtures/redir-log-ipfp.json",
        "/fixtures/redir-log.spec.json",
    ),
    (
        "//logs/redir-log/30min/2019-10-03T11:30:00",
        "/fixtures/redir-log-ysclid.json",
        "/fixtures/redir-log.spec.json",
    ),
)
@canonize_output
def test_serp_task(local_yt):
    """ prepare test data join example
        https://yql.yandex-team.ru/Operations/Ya-S-QVK8GR_e9fBprtAwEm-zzrR0wjAmzFilSGfrEo=
    """
    with mock.patch("solomon.solomon.BasePushApiReporter._push"):
        execute(GetFpcViaSerp())

    def select_all(table):
        return sorted(local_yt.yt_client.read_table(table, format="json").rows)

    def select_root(root):
        tables = sorted(local_yt.yt_client.search(root, node_type="table"))
        return {table: select_all(table) for table in tables}

    return select_root("//home/crypta/testing/state/graph/fpc/fresh")
