import yt.wrapper as yt

from extsearch.ymusic.indexer.rt_indexer.lib import squasher


def test__squasher(yt_test_dir):
    yt.write_table(yt_test_dir + '/0', [
        {'a': 1, 'foo': 'bar1', 'lastModified': 123},
        {'a': 2, 'foo2': 'bar', 'lastModified': 42}
    ])
    yt.write_table(yt_test_dir + '/4', [{'a': 1, 'foo': 'bar2', 'lastModified': 234}])

    squashed_table = squasher.squash(yt_test_dir, 'a')
    data = sorted(yt.read_table(squashed_table), key=lambda d: d['a'])

    assert len(data) == 2
    assert data[0]['a'] == 1
    assert data[0]['foo'] == 'bar2'
    assert data[0]['lastModified'] == 234
    assert data[1]['a'] == 2
    assert data[1]['foo2'] == 'bar'
    assert data[1]['lastModified'] == 42


def test__squasher__empty_dir(yt_test_dir):
    squashed_table = squasher.squash(yt_test_dir, 'foo')
    assert yt.row_count(squashed_table) == 0
