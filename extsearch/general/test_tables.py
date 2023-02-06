import yt.wrapper as yt

import extsearch.ymusic.pylibs.yt_utils.tables as yt_tables


def test__remove_if_empty__empty_table(yt_test_table):
    removed = yt_tables.remove_if_empty(yt_test_table)
    assert removed
    assert not yt.exists(yt_test_table)


def test__remove_if_empty__non_empty_table(yt_test_table):
    yt.write_table(yt_test_table, [{'some': 'data'}])
    removed = yt_tables.remove_if_empty(yt_test_table)
    assert not removed
    assert yt.exists(yt_test_table)
