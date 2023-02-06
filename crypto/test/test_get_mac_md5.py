import pytest

from crypta.dmp.yandex.bin.make_mac_hash_yuid.lib import mac_md5


@pytest.mark.parametrize("src, ref", [
    ("34145ff165f5", "CA13EDC732803229FBBABDD7C5C86FF0"),
    ("34:14:5f:f1:65:f5", "CA13EDC732803229FBBABDD7C5C86FF0")
])
def test_get_mac_md5(src, ref):
    assert mac_md5.get_mac_md5(src) == ref


@pytest.mark.parametrize("src", [
    "",
    "not mac"
])
def test_get_mac_md5_invalid_mac(src):
    assert mac_md5.get_mac_md5(src) is None
