import pytest

from crypta.lib.python.yql_runner.tests import load_fixtures

import crypta.lib.python.bt.conf.conf as conf


@pytest.fixture(scope="session")
@load_fixtures(
    ("//logs/morda-access-log/stream/5min/6666-66-66T66:66:66", "/fixtures/morda_access_log.json"),
    ("//logs/yandex-access-log/1h/7777-77-77T77:77:77", "/fixtures/yandex_access_log.json"),
)
def access_log_symlinks(local_yt):
    yt = local_yt.yt_client
    yt.mkdir(conf.paths.access_log_symlinks, recursive=True)
    for log in ["morda-access-log", "yandex-access-log"]:
        day_dir = "//logs/{}/1d".format(log)
        yt.mkdir(day_dir)
        yt.link(day_dir, "{}/{}".format(conf.paths.access_log_symlinks, log))
