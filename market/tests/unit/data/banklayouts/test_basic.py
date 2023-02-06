from yamarec1.data.banklayouts import BasicDataBankLayout
from yamarec1.data.storages import Table
from yamarec1.data.storages import View


def test_layout_creates_tables_by_default():
    layout = BasicDataBankLayout("//home/me")
    storage = layout.get("some")
    assert isinstance(storage, Table)
    assert storage.path == "//home/me/some"


def test_layout_supports_path_suffixing():
    layout = BasicDataBankLayout("//home/me", suffix="thing/like/this")
    storage = layout.get("some")
    assert isinstance(storage, Table)
    assert storage.path == "//home/me/some/thing/like/this"


def test_layout_can_create_views():
    layout = BasicDataBankLayout("//home/me", suffix="thing/like/this", mode="view")
    storage = layout.get("some")
    assert isinstance(storage, View)
    assert storage.path == "//home/me/some/thing/like/this"
