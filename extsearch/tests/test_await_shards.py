from mr_utils import TableSpec
import yt_utils
import yt.wrapper as yt
import yatest
import os

BASE = '//home/videotest/await_sharding'
TS = 1580800000
CONF = os.path.join(os.getcwd(), 'config')
SHARDS = 4

# ffmpeg -f lavfi -i anullsrc=channel_layout=mono:sample_rate=8000 -t 1 output.ogg
with open('silence.ogg', 'rb') as f:
    SILENCE = f.read()


def dummy_row(url_id, chunk_num, timedelta=0):
    return dict(url='http://test/%s' % url_id, type='audio', codec='dummy', container='dummy', chunk_num=chunk_num,
                last=chunk_num == 2, offset=0, duration=0, session='s1', data=SILENCE,
                ts=TS - timedelta, total_duration=1000000, await_shard=None, data_size=None)


PORTIONS = [
    [
        [
            (0, 0),
            (0, 1),
            (0, 2),
            (1, 0),
            (1, 1),
            (2, 0),
            (2, 2),
            (3, 0),
            (3, 2),
            (4, 1),
            (4, 2),
            (5, 1),
            (5, 2),
            (6, 1),
            (7, 0),
            (7, 0),
            (8, 0, 3600 * 1000),
            (9, 0, 3600 * 4),
            (9, 1),
            (10, 0, 3600 * 4),
            (10, 1, 3600 * 8),
            (11, 0, 3600 * 4),
            (11, 1, 3600 * 8),
            (12, 0, 3600 * 4),
            (12, 1, 3600 * 8),
            (13, 0),
            (13, 1),
            (14, 0, 3600 * 12),
            (15, 0, 3600 * 24),
            (16, 0, 3600 * 36),
            (17, 0, 3600 * 48),
            (18, 0, 3600 * 60),
            (19, 0, 3600 * 72),
        ],
        [
            (3, 1),
            (5, 1),
            (7, 0),
            (7, 0),
            (7, 0),
        ],
    ],
    [
        [
            (7, 1),
            (7, 2, 3600 * 1000),
            (11, 2),
            (12, 2, 3600 * 1000),
            (13, 0),
            (13, 1),
            (14, 0, 3600 * 12),
            (15, 0, 3600 * 24),
            (16, 0, 3600 * 36),
            (17, 0, 3600 * 48),
            (18, 0, 3600 * 60),
            (19, 0, 3600 * 72),
        ],
    ]
]


def write_config(server):
    with open(CONF, 'w') as conf:
        conf.write(
            '''
Shards: %d
StoragePath: "%s/state"
PortionsPath: "%s/state/portions"

ShardPath {
    ShardSub: "%%4s"
    PersName: "pers"
    DeltaName: "delta"
    PortionName: "portion"
    DeleteName: "delete_keys"
    PersUrlsName: "urls"
    DeltaUrlsName: "delta_urls"
    PersKeysName: "pers_keys"
}

Server: "%s"
MaxRowWeight: 134217728
            ''' % (SHARDS, BASE, BASE, server)
        )
        conf.close()


def yt_init(yt_stuff):
    server = yt_stuff.get_server()
    if yt.exists(BASE):
        yt.remove(BASE, recursive=True)
    yt.create('map_node', yt.ypath_join(BASE, 'portions'), ignore_existing=True, recursive=True)
    yt.create('map_node', yt.ypath_join(BASE, 'state', 'portions'), recursive=True, ignore_existing=True)
    for s in range(SHARDS):
        yt.create('map_node', BASE + '/state/%04d/portion' % s, recursive=True)
    yt.create('map_node', BASE + '/state/vh_0000/portion', recursive=True)
    write_config(server)


def write_portions(step):
    for i in range(len(PORTIONS[step])):
        path = yt.ypath_join(BASE, 'portions', 'p%d' % i)
        yt.create('table', path, attributes={
            'schema': [
                {'name': 'url', 'type': 'string'},
                {'name': 'type', 'type': 'string'},
                {'name': 'codec', 'type': 'string'},
                {'name': 'container', 'type': 'string'},
                {'name': 'chunk_num', 'type': 'uint32'},
                {'name': 'last', 'type': 'boolean'},
                {'name': 'offset', 'type': 'uint64'},
                {'name': 'duration', 'type': 'uint64'},
                {'name': 'session', 'type': 'string'},
                {'name': 'data', 'type': 'string'},
                {'name': 'ts', 'type': 'uint64'},
                {'name': 'total_duration', 'type': 'uint64'},
                {'name': 'await_shard', 'type': 'uint64'},
                {'name': 'data_size', 'type': 'uint64'},
            ]}, ignore_existing=True)
        yt.write_table(path, [dummy_row(*x) for x in PORTIONS[step][i]])


def await_run(yt_stuff, i):
    server = yt_stuff.get_server()
    write_portions(i)
    zeh = yatest.common.binary_path('extsearch/video/robot/metarobot/zora_export_handler/zora_export_handler')
    return yt_utils.yt_test(
        zeh,
        args=[
            'ConcatContentV2',
            '-p', server,
            '-d', BASE,
            '-c', CONF,
            '--ts', str(TS + 3600 * 24 * i),
        ],
        data_path=os.getcwd(),
        input_tables=[
        ],
        output_tables=[
            TableSpec(
                'await_run%d.dsv' % i,
                table_name='%s/await' % BASE,
                mapreduce_io_flags=['-format', 'dsv'],
                sortby=['url', 'type', 'chunk_num']
            ),
            TableSpec(
                'stats_run%d.dsv' % i,
                table_name='%s/chunk_stats' % BASE,
                mapreduce_io_flags=['-format', 'dsv'],
                sortby=['Url', 'Type', 'ChunkNum', 'Status']
            ),
            TableSpec(
                'results_run%d.yson' % i,
                table_name='%s/results' % BASE,
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Hash', 'ContentType', 'RecordTime']
            ),
            TableSpec(
                'job_stats_run%d.dsv' % i,
                table_name='%s/job_stats' % BASE,
                mapreduce_io_flags=['-format', 'dsv'],
                sortby=['Ts', 'Step', 'Measure', 'Key']
            ),
        ],
        yt_stuff=yt_stuff
    )


def test_await_run0(yt_stuff):
    yt_init(yt_stuff)
    return await_run(yt_stuff, 0)


def test_await_run1(yt_stuff):
    return await_run(yt_stuff, 1)


if __name__ == '__main__':
    pass
