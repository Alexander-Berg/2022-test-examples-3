import pytest
from fan_feedback.stats.logbrocker_workaround import TSKV


@pytest.fixture
def tskv_string():
    return "tskv\tkey=value\tkey1=значение1"


@pytest.fixture
def tskv_bytes(tskv_string):
    return tskv_string.encode()


@pytest.fixture
def non_tskv_string():
    return "[Thu May 19 13:49:23] key=value key1=значение1"


@pytest.fixture
def non_tskv_bytes(non_tskv_string):
    return non_tskv_string.encode()


def test_accepts_str(tskv_string):
    res = TSKV().to_dict(tskv_string)
    assert res == {"key": "value", "key1": "значение1"}


def test_accepts_bytes(tskv_bytes):
    res = TSKV().to_dict(tskv_bytes)
    assert res == {"key": "value", "key1": "значение1"}


def test_raises_exception_on_non_tskv_str(non_tskv_string):
    with pytest.raises(ValueError, match="given string is not in TSKV format"):
        TSKV().to_dict(non_tskv_string)


def test_raises_exception_on_non_tskv_bytes(non_tskv_bytes):
    with pytest.raises(ValueError, match="given string is not in TSKV format"):
        TSKV().to_dict(non_tskv_bytes)
