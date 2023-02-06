import pytest

from crypta.ltp.viewer.lib.chyt.client import ChytClient
from crypta.ltp.viewer.lib.structs.id import Id

pytest_plugins = [
    "crypta.lib.python.chyt.test_helpers.fixtures",
    "crypta.lib.python.yql.test_helpers.fixtures",
]


@pytest.fixture
def client(clean_local_yt_with_chyt, chyt_alias):
    return ChytClient(clean_local_yt_with_chyt.get_yt_client(), chyt_alias)


@pytest.fixture
def yuid():
    return Id("yandexuid", "123")


@pytest.fixture
def crypta_id():
    return Id("crypta_id", "345")
