#!/usr/bin/env python

from mr_utils import TableSpec
import os
import yatest.common
import yt_utils
import yt.wrapper as yt


def create_env(server, conf):
    yt.create('map_node', '//home/videotest/portions', recursive=True)
    shards = 4
    for s in range(shards):
        yt.create('map_node', '//home/videotest/state/000%d/portion' % s, recursive=True)
    yt.create('map_node', '//home/videotest/state/vh_0000/portion', recursive=True)

    with open(conf, 'w') as f:
        f.write("""
            Shards: %d
            StoragePath: "//home/videotest/state"
            PortionsPath: "//home/videotest/state/portions"

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
        """ % (shards, server))


def test_sharded_filter_detla_origin(yt_stuff):
    conf = os.path.join(os.getcwd(), 'config')
    create_env(yt_stuff.get_server(), conf)

    zeh = yatest.common.binary_path('extsearch/video/robot/metarobot/zora_export_handler/zora_export_handler')

    return yt_utils.yt_test(
        zeh,
        args=[
            'ConcatContentV2',
            '-p', yt_stuff.get_server(),
            '-d', '//home/videotest',
            '-c', conf,
            '-e', '//home/videotest/p1',
            '-e', '//home/videotest/p2'
        ],
        data_path=os.getcwd(),
        input_tables=[
            TableSpec(
                'input_portion',
                table_name='//home/videotest/portions/p',
                mapreduce_io_flags=['-format', 'yson', '-tablewriter', '{"max_row_weight":128000000}']
            )
        ],
        output_tables=[
            TableSpec(
                'output_await_yson',
                table_name='//home/videotest/await',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['url', 'type', 'chunk_num']
            ),
            TableSpec(
                'output_results_yson',
                table_name='//home/videotest/results',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Hash', 'ContentType', 'RecordTime']
            )
        ],
        yt_stuff=yt_stuff
    )
