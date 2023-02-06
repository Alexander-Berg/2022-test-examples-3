import os

import yatest.common
import yt.yson as yson
import yt_utils
from mr_utils import TableSpec


def dump_yson_table(rows, file_path):
    with open(file_path, 'wb') as f:
        for row in rows:
            f.write(yson.dumps(row) + ';')


def test_simple(yt_stuff):
    prev_state = [
        {'Url': 'url1', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(0), 'IsLast': True, 'Ts': yson.YsonUint64(1600000000)},

        {'Url': 'url2', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(0), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},

        {'Url': 'url3', 'Type': 'video', 'ChunkNum': yson.YsonUint64(1), 'IsLast': True, 'Ts': yson.YsonUint64(1600000000)},

        {'Url': 'url4', 'Type': 'video', 'ChunkNum': yson.YsonUint64(1), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},

        {'Url': 'url5', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(0), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},
        {'Url': 'url5', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(1), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},
        {'Url': 'url5', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(2), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},
        {'Url': 'url5', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(3), 'IsLast': True,  'Ts': yson.YsonUint64(1600000000)},

        {'Url': 'url6', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(0), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},
        {'Url': 'url6', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(1), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},
        {'Url': 'url6', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(2), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},
        {'Url': 'url6', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(3), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},

        {'Url': 'url7', 'Type': 'video', 'ChunkNum': yson.YsonUint64(35), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},
        {'Url': 'url7', 'Type': 'video', 'ChunkNum': yson.YsonUint64(36), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},
        {'Url': 'url7', 'Type': 'video', 'ChunkNum': yson.YsonUint64(37), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},

        {'Url': 'url8', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(0), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},
        {'Url': 'url8', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(0), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},
        {'Url': 'url8', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(0), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},
        {'Url': 'url8', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(1), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},
        {'Url': 'url8', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(2), 'IsLast': False, 'Ts': yson.YsonUint64(1600000001)},
        {'Url': 'url8', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(4), 'IsLast': True,  'Ts': yson.YsonUint64(1600000000)},
        {'Url': 'url8', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(4), 'IsLast': True,  'Ts': yson.YsonUint64(1600000099)},

        {'Url': 'url9', 'Type': 'video', 'ChunkNum': yson.YsonUint64(0), 'IsLast': False, 'Ts': yson.YsonUint64(1599999999)},
        {'Url': 'url9', 'Type': 'video', 'ChunkNum': yson.YsonUint64(1), 'IsLast': False, 'Ts': yson.YsonUint64(1600000000)},
        {'Url': 'url9', 'Type': 'video', 'ChunkNum': yson.YsonUint64(2), 'IsLast': True,  'Ts': yson.YsonUint64(1600000001)},

        {'Url': 'url10', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(0), 'IsLast': False, 'Ts': yson.YsonUint64(1599999997)},
        {'Url': 'url10', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(1), 'IsLast': False, 'Ts': yson.YsonUint64(1599999998)},
        {'Url': 'url10', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(2), 'IsLast': True,  'Ts': yson.YsonUint64(1599999999)},
    ]

    portion = [
        {'Url': 'url8', 'Type': 'audio', 'ChunkNum': yson.YsonUint64(1), 'IsLast': False, 'Ts': yson.YsonUint64(1600000123)},
    ]

    dump_yson_table(prev_state, 'prev_state.yson')
    dump_yson_table(portion, 'portion.yson')

    return yt_utils.yt_test(
        yatest.common.binary_path('extsearch/video/robot/tools/chunk_stats/bin/chunk_stats'),
        [
            'merge-state',
            '--cluster', yt_stuff.get_server(),
            '--prev-state', '//prev_state',
            '--portions-dir', '//portions_dir',
            '--new-state', '//new_state',
            '--new-url-state', '//new_url_state',
            '--min-ts', '1600000000',
        ],
        os.getcwd(),
        input_tables=[
            TableSpec(
                'prev_state.yson',
                table_name='//prev_state',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=True,
                sortby=['Url', 'Type', 'ChunkNum'],
            ),
            TableSpec(
                'portion.yson',
                table_name='//portions_dir/portion',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=False,
            ),
        ],
        output_tables=[
            TableSpec(
                'new_state.json',
                table_name='//new_state',
                mapreduce_io_flags=['-format', 'json'],
                sort_on_read=False,
                sortby=['Url', 'Type', 'ChunkNum'],
            ),
            TableSpec(
                'new_url_state.json',
                table_name='//new_url_state',
                mapreduce_io_flags=['-format', 'json'],
                sort_on_read=False,
                sortby=['Url', 'Type'],
            ),
        ],
        yt_stuff=yt_stuff,
        diff_tool=[
            yatest.common.binary_path('extsearch/video/tools/json_diff/json_diff'),
        ],
    )
