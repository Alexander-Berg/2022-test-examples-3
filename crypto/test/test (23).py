import mock
import pytest

from crypta.lib.python import fqdn_utils


@pytest.mark.parametrize("fqdn,fqdn_with_x", [
    ("dev01e.crypta.yandex.net", "dev0Xe.crypta.yandex.net"),
    ("class05i.haze.yandex.net", "class0Xi.haze.yandex.net"),
    ("fqdn.without.digits", "fqdn.without.digits"),
])
def test_get_fqdn_with_x(fqdn, fqdn_with_x):
    with mock.patch("crypta.lib.python.fqdn_utils.socket.getfqdn", return_value=fqdn):
        assert fqdn_with_x == fqdn_utils.get_fqdn_with_x()
