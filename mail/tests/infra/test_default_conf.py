import pytest

from mail.nwsmtp.tests.lib import CLUSTERS
from mail.nwsmtp.tests.lib.default_conf import make_conf, is_url, replace_url


@pytest.mark.parametrize("url, expected", [
    ("example.com:443", "127.0.0.1:%d"),
    ("lmtp://mx.yandex.net", "lmtp://127.0.0.1:%d"),
    ("https://pass.yandex.ru", "http://127.0.0.1:%d"),
    ("https://pass.yandex.ru:443", "http://127.0.0.1:%d")
])
def test_replace_url(url, expected):
    port = 100500
    assert is_url(url) and replace_url(url, lambda: port) == expected % port


def test_is_not_url():
    assert not is_url("localhost")


@pytest.mark.cluster(CLUSTERS)
def test_make_conf(cluster):
    with make_conf(cluster) as conf:
        assert conf
