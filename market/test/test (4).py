#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os
import logging
import tempfile
import time
import pytest
import shutil
import yatest

from datetime import (
    datetime,
    timedelta,
)
from os import environ

import market.idx.stats.statscalc.statscalc as statscalc
from market.idx.stats.statscalc.statscalc.stats_base import CleanerBase, clean_old_temps
from market.proto.indexer.GenerationLog_pb2 import Record as Record
from google.protobuf.descriptor import FieldDescriptor
from mapreduce.yt.interface.protos.extension_pb2 import column_name, key_column_name, flags, default_field_flags, EWrapperFieldFlag  # noqa
from yt.wrapper.format import create_format
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix,
)
from yt.wrapper import ypath_join
import yt.wrapper as yt
from yt import yson

environ["YT_STUFF_MAX_START_RETRIES"] = "2"

BUILDROOT = os.environ.get('ARCADIA_BUILD_ROOT')

logging.basicConfig(format="%(asctime)-15s %(message)s", level=logging.DEBUG)


STATISTICS_FILES = [
    'book_now_shop.tsv', 'book_now_model.mmap', 'is-global.pbuf.sn',
    'model_geo_stats.mmap', 'total-stats.txt', 'shop_names.csv', 'shop_promo_stats.csv', 'offers_samples.csv',
    'shop_discount_stats.csv', 'shopstat.xml', 'category_mapping_stats.pbuf.sn',
    'group_region_stats.csv', 'group_region_stats.mmap',
    'model_region_stats.csv', 'model_region_stats_guru.mmap',
    'blue_group_region_stats.csv', 'blue_group_region_stats.mmap',
    'blue_model_region_stats.csv', 'blue_group_region_stats_guru.mmap',
    'model_local_offers_geo_stats.mmap', 'model_offline_offers_geo_stats.mmap',
    'offer_filters_for_models_stat',
    'model_stats.mmap',
    'category_regional_shops.csv',
    'category_region_stats.csv', 'category_stats.csv',
    'blue_category_region_stats.csv',
    'vendor_category_stats.pbuf.sn', 'blue_vendor_category_stats.pbuf.sn',
    'category_gl_params.csv', 'guru_light_region_stats.csv',
    'blue_category_gl_params.csv', 'blue_guru_light_region_stats.csv',
    'shop_regional_categories.csv', 'blue_buybox_category_region_stats.csv',
]


class FakeConfig(object):
    def get(self, _1, _2, default):
        return default


class CleanerBaseMock(CleanerBase):
    def __init__(self, config, *args, **kwargs):
        self.config = config
        self.untouchable_generations = set(("20100101_0000",))

    def _get_generations(self):
        return [
            "20170306_0100",
            "20170306_0200",
            "20170306_0300",
            "20170306_0400",
            "20170306_0500",
            "20170306_0600",
            "20170306_0700",
            "20170306_0800",
            "20170306_0900",

            "20170307_0100",
            "20170308_0200",
            "20170309_0300",
            "20170310_0400",
            "20170311_0500",
            "20170312_0600",
            "20170313_0700",
            "20170314_0700",

            "20170313_0001",
            "20170320_0002",
            "20170327_0003",
            "20170403_0004",
            "20170410_0005",
            "20170417_0006",
            "20170424_0007",
            "20170501_0008",
            "20170508_0009",

            "20100101_0000",
        ]


def check_output_files(dst_dir):
    dst_dir_files = set(os.listdir(dst_dir))
    for filename in STATISTICS_FILES:
        assert filename in dst_dir_files


def fill_token(yt_server, config):
    yt = yt_server.get_yt_client()
    if "token" not in yt.config or yt.config["token"] is None:
        config.yt_tokenpath = "NO_TOKEN"
        return
    config.yt_token = yt.config["token"]


@pytest.fixture()
def stats_calc_config(yt_server):
    client = yt_server.get_yt_client()
    if not client.exists("//sys/pools/market-testing"):
        client.create("scheduler_pool", attributes={
            "pool_tree": "default",
            "name": "market-testing",
            "max_operation_count": 100,
            "max_running_operation_count": 100})

    config = FakeConfig()
    config.yt = True
    config.yt_bin = os.path.join(BUILDROOT, "yt/python/yt/wrapper/bin/yt_make/yt")
    config.yt_proxy = yt_server.get_server()
    config.yt_stats_calc_dir = ypath_join(get_yt_prefix(), 'testocaster')
    config.pbsncat = os.path.join(BUILDROOT, 'market/idx/tools/pbsncat/bin/pbsncat')
    config.statscalc_bin = os.path.join(BUILDROOT, "market/idx/stats/bin/stats-calc/stats-calc")
    config.statsconvert_bin = os.path.join(BUILDROOT, "market/idx/stats/bin/stats-convert/stats-convert")
    config.ybin_dir = os.path.join(BUILDROOT, "market/tools/glrs-csv2mmap-converter")
    config.working_dir = "./test_data"
    config.yt_pool = "market-testing"
    config.yt_pool_batch = "market-testing"
    config.yt_priority_pool = "market-testing"
    fill_token(yt_server, config)
    config.upload_timeout = 30 * 60  # in seconds
    config.calc_timeout = 40 * 60
    config.download_timeout = 30 * 60
    config.retry_count = 3
    config.use_uploaded_files = False
    config.stats_calc_log_dir = yatest.common.output_path("statscalc_logs")
    config.yt_idr_factor = 1
    config.stats_calc_use_all_files = False
    config.stats_calc_bypass_artifact_cache = False
    config.master_config_path = yatest.common.test_source_path('test_masterconfig.ini')
    config.proto_config = yatest.common.test_source_path('statscalc.config.prototxt')
    config.yt_separated_stats_calc_dir_for_half = False
    config.stats_calc_thread_count = 25
    config.tmp_stats_max_subdirs = 3
    config.tmp_stats_ttl = 2 * 24 * 3600
    config.async_visual_cluster_wizard_stats = True
    config.stat_run_interval = 0
    config.stats_medium = None
    config.no_csv = False
    config.no_model_guru = False
    config.no_blue_guru = False
    config.only_total = False
    config.model_guru_only_total = False
    return config


@pytest.fixture()
def stats_calc_blue_config(yt_server):
    return stats_calc_config(yt_server)


def GetType(cpp_type, outer):
    if cpp_type == FieldDescriptor.CPPTYPE_INT32 or cpp_type == FieldDescriptor.CPPTYPE_ENUM:
        return 'int32'
    if cpp_type == FieldDescriptor.CPPTYPE_INT64:
        return 'int64'
    if cpp_type == FieldDescriptor.CPPTYPE_UINT32:
        return 'uint32'
    if cpp_type == FieldDescriptor.CPPTYPE_UINT64:
        return 'uint64'
    if cpp_type == FieldDescriptor.CPPTYPE_FLOAT or cpp_type == FieldDescriptor.CPPTYPE_DOUBLE:
        return 'double'
    if cpp_type == FieldDescriptor.CPPTYPE_BOOL:
        return 'boolean' if outer else 'bool'
    if cpp_type == FieldDescriptor.CPPTYPE_STRING:
        return 'string'

    return 'void'


def GetFields(fields, outer=True):
    result = []
    for field in fields:
        result.append(GetField(field, outer))
    return result


def GetField(field, outer):
    if field.name not in ['params_entry', 'offers_delivery_info', 'offers_delivery_info_renumerated', 'api_data', 'markup_data']:
        if field.message_type is None:
            if field.label == FieldDescriptor.LABEL_REPEATED:
                if outer:
                    return dict(name=field.name, type='any', type_v3=dict(type_name='optional', item=dict(type_name='list', item=GetType(field.cpp_type, outer))))
                else:
                    return dict(name=field.name, type=dict(type_name='list', item=GetType(field.cpp_type, outer)))
            else:
                if outer:
                    return dict(name=field.name, type=GetType(field.cpp_type, outer))
                else:
                    return dict(name=field.name, type=dict(type_name='optional', item=GetType(field.cpp_type, outer)))
        else:
            # options = field.GetOptions()
            # field_flags = list(options.Extensions[flags])
            members = GetFields(field.message_type.fields, False)
            if field.label == FieldDescriptor.LABEL_REPEATED:
                if outer:
                    return dict(name=field.name, type='any', type_v3=dict(type_name='optional', item=dict(type_name='list', item=dict(type_name='struct', members=members))))
                else:
                    return dict(name=field.name, type=dict(type_name='optional', item=dict(type_name='list', item=dict(type_name='struct', members=members))))
            else:
                if outer:
                    return dict(name=field.name, type='any', type_v3=dict(type_name='optional', item=dict(type_name='struct', members=members)))
                else:
                    return dict(name=field.name, type=dict(type_name='optional', item=dict(type_name='struct', members=members)))
    else:
        if field.label == FieldDescriptor.LABEL_REPEATED:
            return dict(name=field.name, type='any', type_v3=dict(type_name='optional', item=dict(type_name='list', item='string')))
        else:
            return dict(name=field.name, type='string')


def GetValue(field, value):
    if field.cpp_type == FieldDescriptor.CPPTYPE_UINT64:
        return yt.yson.YsonUint64(value)
    if field.cpp_type == FieldDescriptor.CPPTYPE_UINT32:
        return yt.yson.YsonUint64(value)
    return value


def MessageToDict(message):
    fields = message.ListFields()
    result = {}
    for field, value in fields:
        if field.message_type is None:
            if field.label == FieldDescriptor.LABEL_REPEATED:
                result[field.name] = [GetValue(field, v) for v in value]
            else:
                result[field.name] = GetValue(field, value)
        else:
            if field.name not in ['params_entry', 'offers_delivery_info', 'offers_delivery_info_renumerated', 'api_data', 'markup_data']:
                if field.label == FieldDescriptor.LABEL_REPEATED:
                    result[field.name] = [MessageToDict(v) for v in value]
                else:
                    result[field.name] = MessageToDict(value)
            else:
                if field.label == FieldDescriptor.LABEL_REPEATED:
                    result[field.name] = [v.SerializeToString() for v in value]
                else:
                    result[field.name] = value.SerializeToString()
    return result


def write_table(yt, table_name, stream):
    format = create_format('yson')
    rows = format.load_rows(stream, raw=False)
    records = []
    for row in rows:
        record = Record()
        record.ParseFromString(yson.get_bytes(row['stat']))
        dictRec = MessageToDict(record)
        records.append(dictRec)
        if 'mbo_params' in dictRec:
            print('mbo_params', dictRec['mbo_params'])
            for param in dictRec['mbo_params']:
                if 'values' not in param:
                    print('Setting param[values]', param)
                    param['values'] = []
    for record in records:
        print(record)
    yt.write_table(table_name, records)


def create_gen_log_table(stats_runner, yt):
    table_name = stats_runner.mr_table_prefix + '/in/0000'
    schema = GetFields(Record.DESCRIPTOR.fields)
    yt.create('table', table_name,
              attributes={"schema": schema},
              recursive=True)
    write_table(yt, table_name,
                    open(yatest.common.test_source_path('./test_data/0000_generation'), 'rb'))


def run_test(stats_calc_config, yt_server, types=None, should_check_output_files=True):
    generation = "20190618_1027"
    yt = yt_server.get_yt_client()

    with statscalc.TempDir(stats_calc_config.working_dir) as tempdir:
        import os
        dst_dir = os.path.join(stats_calc_config.working_dir, generation, "stats")
        dst_search_dir = os.path.join(stats_calc_config.working_dir, generation, "search-stats-mmap")
        stats_runner = statscalc.build_runner(config=stats_calc_config,
                                              generation=generation,
                                              tmpdir=tempdir,
                                              types=types,
                                              dst_dir=dst_dir)
        create_gen_log_table(stats_runner, yt)
        os.mkdir(dst_search_dir)
        stats_runner.do()

    if should_check_output_files:
        check_output_files(dst_dir)


def test_yt(stats_calc_config, yt_server):  # noqa
    run_test(stats_calc_config, yt_server)


def test_yt_blue_shop_bucket_stats(stats_calc_blue_config, yt_server):  # noqa
    run_test(stats_calc_blue_config, yt_server, ["BlueShopBucketStats"], False)


def test_generation_to_clean():
    config = FakeConfig()
    config.stats_generations_to_keep = 3
    config.stats_generations_to_keep_daily = 7
    config.stats_generations_to_keep_weekly = 8

    generations_to_clean = [
        "20170314_0700",
        "20170313_0700",
        "20170313_0001",
        "20170312_0600",
        "20170311_0500",
        "20170310_0400",
        "20170309_0300",
        "20170308_0200",
        "20170307_0100",
        "20170306_0900",
        "20170306_0800",
        "20170306_0700",
        "20170306_0600",
        "20170306_0500",
        "20170306_0400",
        "20170306_0300",
        "20170306_0200",
        "20170306_0100",
    ]

    cleaner = CleanerBaseMock(config)
    assert sorted(cleaner._get_generations_to_clean()) == sorted(generations_to_clean)


def test_clean_old_temps(stats_calc_config):
    tmp_path = tempfile.mkdtemp()

    now = datetime.now()
    timedelta_hours = [15, 10, 72, 5, 60, 2, 8]

    for hours in timedelta_hours:
        tmp = tempfile.mkdtemp(dir=tmp_path)
        date = now - timedelta(hours=hours)
        mod_time = time.mktime(date.timetuple())
        os.utime(tmp, (mod_time, mod_time))

    clean_old_temps(tmp_path, stats_calc_config)
    subdirs = len(os.listdir(tmp_path))
    shutil.rmtree(tmp_path)
    assert subdirs == stats_calc_config.tmp_stats_max_subdirs
