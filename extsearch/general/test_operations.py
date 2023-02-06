import yt.wrapper as yt

import extsearch.ymusic.pylibs.yt_utils.operations as yt_ops


def test__reduce_leave_key(yt_test_dir):
    with yt.TempTable(yt_test_dir) as input_table, yt.TempTable(yt_test_dir) as output_table:
        yt.write_table(input_table, [
            {'foo': 3, 'bar': 1},
            {'foo': 1, 'bar': 2},
            {'foo': 2, 'bar': 3},
        ])

        yt_ops.reduce_leave_key(input_table, output_table, ['foo'])

        recs = list(yt.read_table(output_table))
        keys = set()
        for rec in recs:
            assert len(rec) == 1
            assert 'foo' in rec
            keys.add(rec['foo'])


def test__leave_latest_record_by_table_index__single_table(yt_test_dir):
    with yt.TempTable(yt_test_dir) as input_table, yt.TempTable(yt_test_dir) as output_table:
        yt.write_table(input_table, [
            {'foo': '1'},
            {'foo': '1'},
            {'foo': '2'},
        ])
        yt_ops.leave_latest_record_by_table_index(input_table, output_table, ['foo'])

        _assert_reduced_output_table(output_table)


def test__leave_latest_record_by_table_index__multiple_tables(yt_test_dir):
    in1 = yt.ypath_join(yt_test_dir, '1')
    yt.write_table(in1, [
        {'foo': '1'},
        {'foo': '1'},
        {'foo': '2'},
    ])
    in2 = yt.ypath_join(yt_test_dir, '2')
    yt.write_table(in2, [])

    out = yt.ypath_join(yt_test_dir, 'out')
    yt_ops.leave_latest_record_by_table_index([in1, in2], out, ['foo'])
    _assert_reduced_output_table(out)


def _assert_reduced_output_table(output_table):
    out_recs_by_key = {}
    for rec in yt.read_table(output_table):
        assert rec['foo'] not in out_recs_by_key, 'Every key must occur once in output table'
        out_recs_by_key[rec['foo']] = rec
    assert len(out_recs_by_key) == 2
    assert '1' in out_recs_by_key
    assert '2' in out_recs_by_key
