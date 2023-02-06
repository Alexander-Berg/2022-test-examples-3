#!/usr/bin/env python

import os
import yatest.common
import yt_utils
import shutil
from mr_utils import TableSpec
import yt.wrapper as yt
from subprocess import Popen
from extsearch.video.contstorage.protos.config_pb2 import TConfig
from google.protobuf import text_format


DIFF_TOOL = yatest.common.binary_path('extsearch/video/robot/videoplusquery/diff_tool/diff_tool')


def fix_configs(yt_server):
    configdir = '.'
    localconf = os.path.join(os.getcwd(), 'config')
    try:
        shutil.copytree(configdir, localconf)
    except:
        pass
    path = os.path.join(localconf, 'vicont.pb.txt')
    config = TConfig()
    text_format.Merge(open(path).read(), config)
    assert config.Shards == 4
    assert config.StoragePath == '//home/videotest/content/state'
    assert config.PortionsPath == '//home/videotest/content/portions'
    assert config.PreviewExportPrefix == '//home/videotest/preview/mk_portions/delta_'
    assert config.Server == ""


def prepare_test(yt_stuff, no_drop=False):
    vicont = yatest.common.binary_path('extsearch/video/contstorage/pymerge/vicont')
    yt_server = yt_stuff.get_server()

    fix_configs(yt_server)

    os.environ['YT_FORCE_PROXY'] = yt_server
    yt.config['proxy']['url'] = yt_server

    if no_drop:
        return vicont

    Popen([vicont, 'vicont.config_bootstrap', 'true']).communicate()

    tables = [
        '//home/videotest/content/state/0000/pers_keys',
        '//home/videotest/content/state/0000/urls',
        '//home/videotest/content/state/0000/new/delta',
        '//home/videotest/content/state/0000/new/delta_urls',
        '//home/videotest/content/state/0000/data/current',
    ]
    for i in tables:
        if yt.exists(i):
            yt.remove(i)
    return vicont


# NOTE: old schema
"""
def get_schema():
    schema = [
        {"name": "Hash",        "required": True,  "sort_order": "ascending", "type": "string"},
        {"name": "ContentType", "required": True,  "sort_order": "ascending", "type": "string"},
        {"name": "RecordTime",  "required": True,  "sort_order": "ascending", "type": "uint64"},
        {"name": "Url",         "required": False, "type": "string"},
        {"name": "Properties",  "required": False, "type": "string"},
        {"name": "Content",     "required": False, "type": "string"},
        {"name": "RowIndex",    "required": False, "type": "uint64"},
        {"name": "HasNextRow",  "required": False, "type": "boolean"},
    ]
    return schema
"""


def get_pers_schema():
    schema = [
        {"name": "Hash",        "required": False, "type": "string"},
        {"name": "ContentType", "required": False, "type": "string"},
        {"name": "RecordTime",  "required": False, "type": "uint64"},
        {"name": "Url",         "required": False, "type": "string"},
        {"name": "Properties",  "required": False, "type": "string"},
        {"name": "RowIndex",    "required": False, "type": "uint64"},
        {"name": "HasNextRow",  "required": False, "type": "boolean"},
        {"name": "Content",     "required": False, "type": "string"},
    ]
    return schema


def get_keys_schema():
    schema = [
        {"name": "Hash",        "required": False, "type": "string"},
        {"name": "ContentType", "required": False, "type": "string"},
        {"name": "RecordTime",  "required": False, "type": "uint64"},
        {"name": "RowIndex",    "required": False, "type": "uint64"},
        {"name": "HasNextRow",  "required": False, "type": "boolean"},
    ]
    return schema


def get_urls_schema():
    schema = [
        {"name": "Url",         "required": False, "type": "string"},
        {"name": "ContentType", "required": False, "type": "string"},
        {"name": "RecordTime",  "required": False, "type": "uint64"},
        {"name": "Hash",        "required": False, "type": "string"},
        {"name": "Properties",  "required": False, "type": "string"},
    ]
    return schema


def test_sharded_remove_keys(yt_stuff):
    vicont = prepare_test(yt_stuff)
    yt_utils.yt_test(
        vicont,
        args=[
            '?',
        ],
        data_path=os.getcwd(),
        input_tables=[
            TableSpec(
                'input_urls.yson',
                table_name='//home/videotest/content/state/0000/urls',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=True,
                attrs_on_load={'schema': get_urls_schema()},
                sortby=['Url', 'ContentType', 'RecordTime'],
            ),
            TableSpec(
                'input_pers.yson',
                table_name='//home/videotest/content/state/0000/data/pers.some_tag',
                mapreduce_io_flags=['-format', 'yson', '-tablewriter', '{"max_row_weight":128000000}'],
                sort_on_load=True,
                attrs_on_load={'schema': get_pers_schema()},
                sortby=['Hash', 'ContentType', 'RowIndex'],
            ),
            TableSpec(
                'input_pers_keys.yson',
                table_name='//home/videotest/content/state/0000/pers_keys',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=True,
                attrs_on_load={'schema': get_keys_schema()},
                sortby=['Hash', 'ContentType', 'RowIndex'],
            ),
            TableSpec(
                'input_urls_half.yson',
                table_name='//home/videotest/content/state/0000/delete_keys/urls',
                mapreduce_io_flags=['-format', 'yson'],
                attrs_on_load={'schema': get_urls_schema()},
            ),
        ],
        yt_stuff=yt_stuff,
    )
    urls = yt_utils.yt_test(
        vicont,
        args=[
            'vicont.remove_keys_from_urls',
            '0000'
        ],
        data_path=os.getcwd(),
        output_tables=[
            TableSpec(
                'output_new_urls.yson',
                table_name='//home/videotest/content/state/0000/urls',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_read=False,
            ),
            TableSpec(
                'output_deleted_urls.yson',
                table_name='//home/videotest/content/state/0000/new/deleted_urls',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Url', 'ContentType', 'RecordTime'],
            ),
        ],
        yt_stuff=yt_stuff,
    )
    pers_keys = yt_utils.yt_test(
        vicont,
        args=[
            'vicont.remove_keys_from_shard',
            '0000'
        ],
        data_path=os.getcwd(),
        output_tables=[
            TableSpec(
                'output_new_pers_keys.yson',
                table_name='//home/videotest/content/state/0000/pers_keys',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_read=False,
            ),
            TableSpec(
                'output_deleted_pers_keys.yson',
                table_name='//home/videotest/content/state/0000/new/deleted_keys',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Hash', 'ContentType', 'RowIndex'],
            ),
        ],
        yt_stuff=yt_stuff,
    )
    pers_data = yt_utils.yt_test(
        vicont,
        args=[
            'vicont.remove_keys_from_pers',
            '0000'
        ],
        data_path=os.getcwd(),
        output_tables=[
            TableSpec(
                'output_new_pers.yson',
                table_name='//home/videotest/content/state/0000/data/pers.some_tag',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_read=False,
            ),
            TableSpec(
                'output_deleted_pers.yson',
                table_name='//home/videotest/content/state/0000/pers_delete/deleted_rows',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Hash', 'ContentType', 'RowIndex'],
            ),
        ],
        yt_stuff=yt_stuff,
        diff_tool=[DIFF_TOOL, '--proto', 'NVideo::NContent::TContentRecord'],
    )
    return [urls, pers_keys, pers_data]


# NOTE: unused in prod
"""
def test_sharded_filter_detla_origin(yt_stuff):
    vicont = prepare_test(yt_stuff)
    return yt_utils.yt_test(
        vicont,
        args=[
            'vicont.sharded_filter_delta',
            '0000'
        ],
        data_path=os.getcwd(),
        input_tables=[
            TableSpec(
                'input_delta_yson',
                table_name='//home/videotest/content/state/0000/new/delta',
                mapreduce_io_flags=['-format', 'yson', '-tablewriter', '{"max_row_weight":128000000}'],
                sort_on_load=True,
                attrs_on_load={'schema': get_schema()},
                sortby=['Hash', 'ContentType', 'RecordTime'],
            ),
            TableSpec(
                'input_pers.yson',
                table_name='//home/videotest/content/state/0000/data/current',
                mapreduce_io_flags=['-format', 'yson', '-tablewriter', '{"max_row_weight":128000000}'],
                sort_on_load=True,
                sortby=['Hash', 'ContentType', 'RecordTime'],
            )
        ],
        output_tables=[
            TableSpec(
                'output_delta_yson',
                table_name='//home/videotest/content/state/0000/new/delta',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Hash', 'ContentType', 'RecordTime'],
            ),
            TableSpec(
                'output_delta_urls_yson',
                table_name='//home/videotest/content/state/0000/new/delta_urls',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Url', 'ContentType', 'RecordTime'],
            )
        ],
        yt_stuff=yt_stuff,
        diff_tool=[DIFF_TOOL, '--proto', 'NVideo::NContent::TContentRecord'],
    )
"""


# NOTE: unused in prod
"""
def test_sharded_check_delta(yt_stuff):
    vicont = prepare_test(yt_stuff)
    return yt_utils.yt_test(
        vicont,
        args=[
            'vicont.sharded_check_delta',
            '0000'
        ],
        data_path=os.getcwd(),
        input_tables=[
            TableSpec(
                'input_delta_yson',
                table_name='//home/videotest/content/state/0000/new/delta',
                mapreduce_io_flags=['-format', 'yson', '-tablewriter', '{"max_row_weight":128000000}'],
                sort_on_load=True,
                attrs_on_load={'schema': get_schema()},
                sortby=['Hash', 'ContentType', 'RecordTime'],
            )
        ],
        output_tables=[
        ],
        yt_stuff=yt_stuff
        )
"""


# NOTE: unused in prod
"""
def test_sharded_filter_delta(yt_stuff):
    vicont = prepare_test(yt_stuff)
    res = yt_utils.yt_test(
        vicont,
        args=[
            'vicont.sharded_filter_delta',
            '0000'
        ],
        data_path=os.getcwd(),
        input_tables=[
            TableSpec(
                'input_delta_yson',
                table_name='//home/videotest/content/state/0000/new/delta',
                mapreduce_io_flags=['-format', 'yson', '-tablewriter', '{"max_row_weight":128000000}'],
                sort_on_load=True,
                attrs_on_load={'schema': get_schema()},
                sortby=['Hash', 'ContentType', 'RecordTime'],
            ),
            TableSpec(
                'input_pers_keys.yson',
                table_name='//home/videotest/content/state/0000/pers_keys',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=True,
                sortby=['Hash', 'ContentType', 'RecordTime'],
            )
        ],
        output_tables=[
            TableSpec(
                'output_delta_yson',
                table_name='//home/videotest/content/state/0000/new/delta',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Hash', 'ContentType', 'RecordTime'],
            ),
            TableSpec(
                'output_delta_urls_yson',
                table_name='//home/videotest/content/state/0000/new/delta_urls',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Url', 'ContentType', 'RecordTime'],
            )
        ],
        yt_stuff=yt_stuff,
        diff_tool=[DIFF_TOOL, '--proto', 'NVideo::NContent::TContentRecord'],
    )
    return res
"""


def test_sharded_merge(yt_stuff):
    vicont = prepare_test(yt_stuff)
    return yt_utils.yt_test(
        vicont,
        args=[
            'vicont.sharded_merge',
            '0000'
        ],
        data_path=os.getcwd(),
        input_tables=[
            TableSpec(
                'input_delta_yson',
                table_name='//home/videotest/content/state/0000/new/delta',
                mapreduce_io_flags=['-format', 'yson', '-tablewriter', '{"max_row_weight":128000000}'],
                sort_on_load=True,
                attrs_on_load={'schema': get_pers_schema()},
                sortby=['Hash', 'ContentType', 'RowIndex'],
            ),
            TableSpec(
                'input_delta_keys_yson',
                table_name='//home/videotest/content/state/0000/new/delta_keys',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=True,
                attrs_on_load={'schema': get_keys_schema()},
                sortby=['Hash', 'ContentType', 'RowIndex'],
            ),
            TableSpec(
                'input_pers.yson',
                table_name='//home/videotest/content/state/0000/data/current',
                mapreduce_io_flags=['-format', 'yson', '-tablewriter', '{"max_row_weight":128000000}'],
                sort_on_load=True,
                attrs_on_load={'schema': get_pers_schema()},
                sortby=['Hash', 'ContentType', 'RowIndex'],
            ),
            TableSpec(
                'input_pers_keys.yson',
                table_name='//home/videotest/content/state/0000/pers_keys',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=True,
                attrs_on_load={'schema': get_keys_schema()},
                sortby=['Hash', 'ContentType', 'RowIndex'],
            )
        ],
        output_tables=[
            TableSpec(
                'output_pers_yson',
                table_name='//home/videotest/content/state/0000/data/current',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_read=False,
            ),
            TableSpec(
                'output_pers_keys_yson',
                table_name='//home/videotest/content/state/0000/pers_keys',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_read=False,
            )
        ],
        yt_stuff=yt_stuff,
        diff_tool=[DIFF_TOOL, '--proto', 'NVideo::NContent::TContentRecord'],
    )


def test_sharded_merge_urls(yt_stuff):
    vicont = prepare_test(yt_stuff, no_drop=True)
    return yt_utils.yt_test(
        vicont,
        args=[
            'vicont.sharded_merge_url',
            '0000'
        ],
        data_path=os.getcwd(),
        input_tables=[
            TableSpec(
                'input_urls.yson',
                table_name='//home/videotest/content/state/0000/urls',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=True,
                attrs_on_load={'schema': get_urls_schema()},
                sortby=['Url', 'ContentType', 'RecordTime'],
            ),
            TableSpec(
                'input_delta_urls.yson',
                table_name='//home/videotest/content/state/0000/new/delta_urls',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=True,
                attrs_on_load={'schema': get_urls_schema()},
                sortby=['Url', 'ContentType', 'RecordTime'],
            ),
        ],
        output_tables=[
            TableSpec(
                'output_pers_urls_yson',
                table_name='//home/videotest/content/state/0000/urls',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_read=False,
            ),
            TableSpec(
                'output_pers_urls.diff_yson',
                table_name='//home/videotest/content/state/0000/urls.diff',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Url', 'ContentType', 'RecordTime'],
            )
        ],
        yt_stuff=yt_stuff,
        diff_tool=[DIFF_TOOL, '--proto', 'NVideo::NContent::TContentRecord'],
    )


# NOTE: unused in prod
"""
def test_resharder(yt_stuff):
    vicont = prepare_test(yt_stuff)
    return yt_utils.yt_test(
        vicont,
        args=[
            'vicont.reshard_full',
            '4',
            '8'
        ],
        data_path=os.getcwd(),
        input_tables=[
            TableSpec(
                'input_urls.yson',
                table_name='//home/videotest/content/state/0000/urls',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=True,
                sortby=['Url', 'ContentType', 'RecordTime'],
            ),
            TableSpec(
                'input_pers.yson',
                table_name='//home/videotest/content/state/0000/data/current',
                mapreduce_io_flags=['-format', 'yson', '-tablewriter', '{"max_row_weight":128000000}'],
                sort_on_load=True,
                attrs_on_load={'schema': get_schema()},
                sortby=['Hash', 'ContentType', 'RecordTime'],
            ),
            TableSpec(
                'input_pers_keys.yson',
                table_name='//home/videotest/content/state/0000/pers_keys',
                mapreduce_io_flags=['-format', 'yson'],
                sort_on_load=True,
                sortby=['Hash', 'ContentType', 'RecordTime'],
            )
        ],
        output_tables=[
            TableSpec(
                'output_pers_0000_yson',
                table_name='//home/videotest/content/state/0000/data/current',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Hash', 'ContentType', 'RecordTime'],
            ),
            TableSpec(
                'output_pers_keys_0000_yson',
                table_name='//home/videotest/content/state/0000/pers_keys',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Hash', 'ContentType', 'RecordTime'],
            ),
            TableSpec(
                'output_pers_urls_0000_yson',
                table_name='//home/videotest/content/state/0000/urls',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Url', 'ContentType', 'RecordTime'],
            ),
            TableSpec(
                'output_pers_0004_yson',
                table_name='//home/videotest/content/state/0004/data/current',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Hash', 'ContentType', 'RecordTime'],
            ),
            TableSpec(
                'output_pers_keys_0004_yson',
                table_name='//home/videotest/content/state/0004/pers_keys',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Hash', 'ContentType', 'RecordTime'],
            ),
            TableSpec(
                'output_pers_urls_0004_yson',
                table_name='//home/videotest/content/state/0004/urls',
                mapreduce_io_flags=['-format', 'yson'],
                sortby=['Url', 'ContentType', 'RecordTime'],
            )
        ],
        yt_stuff=yt_stuff,
        diff_tool=[DIFF_TOOL, '--proto', 'NVideo::NContent::TContentRecord'],
    )
"""
