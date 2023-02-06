import pytest

from crypta.lib.python.yt import wildcard_path


@pytest.mark.parametrize("prefix,template,path,expected_result", [
    ["xxx/yyy/zzz/", "www", "xxx/yyy/zzz", False],
    ["xxx/yyy/zzz/", "www/aaa", "xxx/yyy/zzz/www/bbb", False],
    ["xxx/yyy/zzz/", "www/bbb/aaa", "xxx/yyy/zzz/www/bbb", False],
    ["xxx/yyy/zzz/", "www/aaa", "xxx/yyy/zzz/www/aaa", True],
    ["xxx/yyy/zzz/", "*/aaa", "xxx/yyy/zzz/www/aaa", True]
])
def test_path_filter_template(prefix, template, path, expected_result):
    assert expected_result == wildcard_path.path_filter_template(prefix, template, path)


@pytest.mark.parametrize("prefix,template,subtree,expected_result", [
    ["xxx/yyy/zzz/", "www", "xxx/yyy/zzz", True],
    ["xxx/yyy/zzz/", "www/aaa", "xxx/yyy/zzz/www/aaa/bbb", False],
    ["xxx/yyy/zzz/", "www/aaa", "xxx/yyy/zzz/www/bbb", False],
    ["xxx/yyy/zzz/", "www/aaa", "xxx/yyy/zzz/www/aaa", True],
    ["xxx/yyy/zzz/", "*/aaa", "xxx/yyy/zzz/www/aaa", True]
])
def test_subtree_filter_template(prefix, template, subtree, expected_result):
    assert expected_result == wildcard_path.subtree_filter_template(prefix, template, subtree)


@pytest.mark.parametrize("template,expected_result", [
    ["xxx/*/zzz", ["xxx/aaa/zzz", "xxx/bbb/zzz"]],
    ["xxx/aaa/yyy", ["xxx/aaa/yyy"]]
])
def test_unfold_template(yt_stuff, template, expected_result):
    yt_client = yt_stuff.get_yt_client()
    for i in ["aaa", "bbb"]:
        yt_client.create("table", path="xxx/{}/zzz".format(i), recursive=True)
    yt_client.create("table", path="yyy/aaa/zzz", recursive=True)
    yt_client.create("table", path="xxx/aaa/yyy", recursive=True)
    assert expected_result == sorted(wildcard_path.unfold_template(yt_client, template))
