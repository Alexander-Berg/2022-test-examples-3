from yamarec1.beans import ytc
from yamarec1.data.banklayouts import BasicDataBankLayout


def test_layout_can_list_all_indices_with_suffix(random_yt_path):
    ytc.create("table", random_yt_path + "/first/suffix", recursive=True)
    ytc.create("table", random_yt_path + "/second/no-suffix", recursive=True)
    ytc.create("table", random_yt_path + "/third/suffix", recursive=True)
    layout = BasicDataBankLayout(random_yt_path, suffix="suffix", mode="table")
    assert layout.indices == {"first", "third"}


def test_layout_can_list_all_indices_without_suffix(random_yt_path):
    ytc.create("table", random_yt_path + "/first", recursive=True)
    ytc.create("table", random_yt_path + "/second", recursive=True)
    ytc.create("table", random_yt_path + "/third", recursive=True)
    layout = BasicDataBankLayout(random_yt_path, mode="table")
    assert layout.indices == {"first", "second", "third"}


def test_layout_ignores_recent_links(random_yt_path):
    ytc.create("table", random_yt_path + "/first", recursive=True)
    ytc.create("table", random_yt_path + "/second", recursive=True)
    ytc.create("table", random_yt_path + "/recent", recursive=True)
    layout = BasicDataBankLayout(random_yt_path, mode="table")
    assert layout.indices == {"first", "second"}


def test_layout_has_no_indices_if_path_does_not_exist(random_yt_path):
    layout = BasicDataBankLayout(random_yt_path, mode="table")
    assert layout.indices == set()
