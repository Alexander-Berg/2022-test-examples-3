import yt.wrapper as yt

from extsearch.ymusic.pylibs.yt_utils import sortiness


def test__ensure_sorted_by__not_sorted():
    table = '//home/test'
    yt.write_table(table, [
        {'test': 'b'},
        {'test': 'a'}
    ])
    assert not yt.is_sorted(table)
    sortiness.ensure_sorted_by(table, ['test'])
    assert yt.is_sorted(table)


def test__ensure_sorted_by__already_sorted():
    table = '//home/test'
    yt.write_table(yt.TablePath(table, sorted_by=['test']), [
        {'test': 'a'},
        {'test': 'b'},
    ])
    assert yt.get_attribute(table, 'sorted_by') == ['test']
    sortiness.ensure_sorted_by(table, ['test'])
    assert yt.get_attribute(table, 'sorted_by') == ['test']


def test__ensure_sorted_by__sorted_by_longer_prefix():
    table = '//home/test'
    yt.write_table(yt.TablePath(table, sorted_by=['c1', 'c2']), [
        {'c1': 0, 'c2': 0},
        {'c1': 0, 'c2': 1},
        {'c1': 1, 'c2': 0},
        {'c1': 1, 'c2': 1},
    ])
    assert yt.get_attribute(table, 'sorted_by') == ['c1', 'c2']
    sortiness.ensure_sorted_by(table, ['c1'])
    assert yt.get_attribute(table, 'sorted_by') == ['c1', 'c2']


def test__ensure_sorted_by__sorted_by_another_prefix():
    table = '//home/test'
    yt.write_table(yt.TablePath(table, sorted_by=['c1', 'c2']), [
        {'c1': 0, 'c2': 0},
        {'c1': 0, 'c2': 1},
        {'c1': 1, 'c2': 0},
        {'c1': 1, 'c2': 1},
    ])
    assert yt.get_attribute(table, 'sorted_by') == ['c1', 'c2']
    sortiness.ensure_sorted_by(table, ['c2', 'c1'])
    assert yt.get_attribute(table, 'sorted_by') == ['c2', 'c1']
