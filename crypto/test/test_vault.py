import pytest

from crypta.utils.vault.server import lib


@pytest.mark.parametrize("host", [
    "iva-01.id.crypta.yandex.net",
    "sas-01.id-test.crypta.yandex.net",
    "man-01.id-exp.crypta.yandex.net",
    "dev01e.crypta.yandex.net",
    "idserv01i.rtcrypta.yandex.net",
    "idserv01ft.rtcrypta.yandex.net",
    "storm01i-dev.rtcrypta.yandex.net",
    "other.crypta.yandex.net",
    "other.rtcrypta.yandex.net",
    "01e.rtcrypta.yandex.net",
])
def test_is_allowed_host(host):
    assert lib.is_allowed_host(host)


@pytest.mark.parametrize("host", [
    "other.yandex.net",
    "crypta.yandex.net",
    "-01.id.crypta.yandex.net",
    "iva-01-.id.crypta.yandex.net",
    "iva-01_id_crypta_yandex_net",
    "iva-01_id.crypta.yandex.net",
    ".rtcrypta.yandex.net",
    ".id.crypta.yandex.net",
    "../id.crypta.yandex.net",
    "id.crypta.yandex.net example.com",
    "buglloc.com:/tmp/.rtcrypta.yandex.net"
])
def test_not_is_allowed_host(host):
    assert not lib.is_allowed_host(host)
