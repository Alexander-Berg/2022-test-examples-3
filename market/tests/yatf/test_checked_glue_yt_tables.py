# -*- coding: utf-8 -*-
import pytest
import tempfile
from hamcrest import assert_that, equal_to

from market.idx.marketindexer.miconfig import MiConfig

from market.idx.cron.cron_clt.lib.checked_glue_yt_tables.checked_glue_yt_tables import (
    get_glue_table_schema,
    get_source_table_fields,
    get_tables_to_check_info,

    read_glue_config,

    check_table_exists,
    check_table_is_static,
    check_table_schema_strict,
    check_table_schema_strong_mode,
    check_table_schema_key,
    check_table_schema_glue_columns,
    check_table_total_size,
    check_table_row_size,
    check_table_user_attribute,

    prepare,
    process_one_table,
)
from market.idx.library.glue.proto.GlueConfig_pb2 import EReduceKeySchema

from market.idx.yatf.resources.glue_config import GlueConfig
from market.idx.yatf.resources.glue_tables import (
    YtShortOfferKeyExternalGlueTable,
    YtFullOfferKeyExternalGlueTable,
    YtMskuKeyExternalGlueTable,
    YtModelKeyExternalGlueTable,
)
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from yt.wrapper.ypath import ypath_join

import yatest


@pytest.fixture(scope='module')
def external_table1(yt_server):
    prefix = get_yt_prefix()
    path = ypath_join(prefix, 'external_table1/2022-03-01')
    recent = ypath_join(prefix, 'external_table1/recent')

    table = YtShortOfferKeyExternalGlueTable(
        yt_stuff=yt_server,
        path=path,
        link_paths=[recent],
        data=[
            {
                'business_id': 1,
                'offer_id': 'offer1',
                'column_a': 1,
                'column_b': True,
                'not_for_glue_column': 1,
            },
            {
                'business_id': 2,
                'offer_id': 'offer2',
                'column_a': 2,
                'column_b': True,
                'not_for_glue_column': 2,
            },
        ],
        data_columns_schema=[
            {'required': False, 'name': 'column_a', 'type': 'int64'},
            {'required': False, 'name': 'not_for_glue_column', 'type': 'int64'},
            {'required': False, 'name': 'column_b', 'type': 'boolean'},

        ],
    )
    table.create()
    return table


@pytest.fixture(scope='module')
def external_table2(yt_server):
    prefix = get_yt_prefix()
    path = ypath_join(prefix, 'external_table2/2022-03-01')
    recent = ypath_join(prefix, 'external_table2/recent')

    table = YtFullOfferKeyExternalGlueTable(
        yt_stuff=yt_server,
        path=path,
        link_paths=[recent],
        data=[
            {
                'business_id': 1,
                'offer_id': 'offer1',
                'shop_id': 1,
                'warehouse_id': 1,
                'column_c': 'lalala',
                'not_for_glue_column': 1,
            },
            {
                'business_id': 2,
                'offer_id': 'offer2',
                'shop_id': 2,
                'warehouse_id': 2,
                'column_c': 'bububu',
                'not_for_glue_column': 2,
            },
        ],
        data_columns_schema=[
            {'required': False, 'name': 'column_c', 'type': 'string'},
            {'required': False, 'name': 'not_for_glue_column', 'type': 'int64'},
        ],
    )
    table.create()
    return table


@pytest.fixture(scope='module')
def external_table3(yt_server):
    prefix = get_yt_prefix()
    path = ypath_join(prefix, 'external_table3/2022-03-01')
    recent = ypath_join(prefix, 'external_table3/recent')

    table = YtMskuKeyExternalGlueTable(
        yt_stuff=yt_server,
        path=path,
        link_paths=[recent],
        data=[
            {
                'market_sku': 100,
                'column_d': 'popopo',
                'not_for_glue_column': 1,
            },
            {
                'market_sku': 200,
                'column_d': 'okokok',
                'not_for_glue_column': 111,
            },
        ],
        data_columns_schema=[
            {'required': False, 'name': 'not_for_glue_column', 'type': 'int64'},
            {'required': False, 'name': 'column_d', 'type': 'string'},
        ],
    )
    table.create()
    return table


@pytest.fixture(scope='module')
def external_table4(yt_server):
    prefix = get_yt_prefix()
    path = ypath_join(prefix, 'external_table4/2022-03-01')
    recent = ypath_join(prefix, 'external_table4/recent')

    table = YtFullOfferKeyExternalGlueTable(
        yt_stuff=yt_server,
        path=path,
        link_paths=[recent],
        data=[
            {
                'business_id': 1,
                'offer_id': 'offer1',
                'shop_id': 1,
                'warehouse_id': 1,
                'column_f': ['lalala'],
                'not_for_glue_column': 1,
            },
            {
                'business_id': 2,
                'offer_id': 'offer2',
                'shop_id': 2,
                'warehouse_id': 2,
                'column_f': ['bububu'],
                'not_for_glue_column': 22,
            },
        ],
        data_columns_schema=[
            {'required': True, 'name': 'column_f', 'type_v3': {'type_name': 'list', 'item': 'string'}},
            {'required': False, 'name': 'not_for_glue_column', 'type': 'int64'},
        ],
    )
    table.create()
    return table


@pytest.fixture(scope='module')
def external_table5(yt_server):
    prefix = get_yt_prefix()
    path = ypath_join(prefix, 'external_table5/2022-05-01')
    recent = ypath_join(prefix, 'external_table5/recent')

    table = YtModelKeyExternalGlueTable(
        yt_stuff=yt_server,
        path=path,
        link_paths=[recent],
         data=[
            {
                'model_id': 1000,
                'column_g': 'popopo',
                'not_for_glue_column': 1,
            },
            {
                'model_id': 2000,
                'column_g': 'okokok',
                'not_for_glue_column': 111,
            },
        ],
        data_columns_schema=[
            {'required': False, 'name': 'not_for_glue_column', 'type': 'int64'},
            {'required': False, 'name': 'column_g', 'type': 'string'},
        ],
    )
    table.create()
    return table


@pytest.fixture(scope='module')
def glue_config(
        external_table1,
        external_table2,
        external_table3,
        external_table4,
        external_table5,
):
    return GlueConfig(
        {
            'Fields': [
                {
                    'glue_id': 1,
                    'declared_cpp_type': 'INT64',
                    'target_name': 'a',
                    'is_from_datacamp': False,
                    'owner': 'marketindexer',
                    'source_name': 'ext1',
                    'source_table_path': external_table1.link,
                    'source_field_path': 'column_a',
                    'destination_table_path': '//home/in/glue/ext1/recent',
                    'data_limit_per_table': 100,
                    'data_limit_per_offer': 50,
                    'reduce_key_schema': EReduceKeySchema.SHORT_OFFER_ID,
                },
                {
                    'glue_id': 2,
                    'declared_cpp_type': 'BOOL',
                    'target_name': 'b',
                    'is_from_datacamp': False,
                    'owner': 'marketindexer',
                    'source_name': 'ext1',
                    'source_table_path': external_table1.link,
                    'source_field_path': 'column_b',
                    'destination_table_path': '//home/in/glue/ext1/recent',
                    'data_limit_per_table': 100,
                    'data_limit_per_offer': 50,
                    'reduce_key_schema': EReduceKeySchema.SHORT_OFFER_ID,
                },
                {
                    'glue_id': 3,
                    'declared_cpp_type': 'STRING',
                    'target_name': 'c',
                    'is_from_datacamp': False,
                    'owner': 'marketindexer',
                    'source_name': 'ext2',
                    'source_table_path': external_table2.link,
                    'source_field_path': 'column_c',
                    'destination_table_path': '//home/in/glue/ext2/recent',
                    'data_limit_per_table': 100,
                    'data_limit_per_offer': 50,
                    'use_as_snippet': True,
                    'reduce_key_schema': EReduceKeySchema.FULL_OFFER_ID,
                },
                {
                    'glue_id': 4,
                    'declared_cpp_type': 'STRING',
                    'target_name': 'd',
                    'is_from_datacamp': False,
                    'owner': 'marketindexer',
                    'source_name': 'ext3',
                    'source_table_path': external_table3.link,
                    'source_field_path': 'column_d',
                    'destination_table_path': '//home/in/glue/ext3/recent',
                    'data_limit_per_table': 46,
                    'data_limit_per_offer': 23,
                    'use_as_snippet': True,
                    'reduce_key_schema': EReduceKeySchema.MSKU,
                },
                {
                    'glue_id': 5,
                    'declared_cpp_type': 'STRING',
                    'target_name': 'f',
                    'is_from_datacamp': False,
                    'owner': 'marketindexer',
                    'source_name': 'ext4',
                    'source_table_path': external_table4.link,
                    'source_field_path': 'column_f',
                    'destination_table_path': '//home/in/glue/ext4/recent',
                    'data_limit_per_table': 300,
                    'data_limit_per_offer': 150,
                    'use_as_snippet': True,
                    'reduce_key_schema': EReduceKeySchema.FULL_OFFER_ID,
                    'is_array': True,
                },
                {
                    'glue_id': 6,
                    'declared_cpp_type': 'STRING',
                    'target_name': 'g',
                    'is_from_datacamp': False,
                    'owner': 'marketindexer',
                    'source_name': 'ext5',
                    'source_table_path': external_table5.link,
                    'source_field_path': 'column_g',
                    'destination_table_path': '//home/in/glue/ext5/recent',
                    'data_limit_per_table': 300,
                    'data_limit_per_offer': 150,
                    'use_as_snippet': True,
                    'reduce_key_schema': EReduceKeySchema.MODEL_ID,
                },
                {
                    'glue_id': 100500,
                    'declared_cpp_type': 'STRING',
                    'target_name': 'e',
                    'is_from_datacamp': True,
                    'owner': 'marketindexer',
                    'source_name': 'ext3',
                    'source_field_path': 'column_e',
                    'use_as_snippet': True,
                    'reduce_key_schema': EReduceKeySchema.FULL_OFFER_ID,
                },
            ],
        },
        'glue_config.json'
    )


@pytest.yield_fixture(scope='module')
def miconfig_data(
        yt_server,
        glue_config,
        external_table1,
        external_table2,
        external_table3
):
    yield '''
[general]
working_dir = /
glue_config_path={glue_config_path}

[yt]
yt_proxy_primary = {yt_proxy}
yt_tokenpath =

[glue]
tvm_client_id = 2009733
tvm_secret_uuid = sec-01dq7m7g2pgs0rhhq3xhn1r4d8
mail_notification_enabled = false

[checked_yt_tables]
enable=true
enable_prepare=true
'''.format(
        glue_config_path=glue_config.path,
        yt_proxy=yt_server.get_server(),
    )


@pytest.yield_fixture(scope='module')
def miconfig_path(miconfig_data):
    with tempfile.NamedTemporaryFile(mode='w+') as f:
        f.write(miconfig_data)
        f.flush()
        yield f.name


@pytest.yield_fixture(scope='module')
def mi_config(miconfig_path):
    ds_config_path = yatest.common.source_path('market/idx/marketindexer/tests/datasources.conf')
    yield MiConfig([miconfig_path], ds_config_path)


def test_all(
        mi_config,
        yt_server,
        external_table1,
        external_table2,
        external_table3,
        external_table4,
        external_table5,
):
    glue_config = read_glue_config(mi_config)
    paths = list(sorted(field.source_table_path for field in get_source_table_fields(glue_config)))
    assert len(paths) == 5
    assert paths == list(sorted([
        external_table1.link,
        external_table2.link,
        external_table3.link,
        external_table4.link,
        external_table5.link,
    ]))

    yt_client = yt_server.get_yt_client()
    result = {}
    for path in paths:
        result.update(get_glue_table_schema(yt_client, path, glue_config))

        assert check_table_exists(path, glue_config, yt_client)
        assert check_table_is_static(path, glue_config, yt_client)
        assert check_table_schema_strict(path, glue_config, yt_client)
        assert check_table_schema_strong_mode(path, glue_config, yt_client)
        assert check_table_schema_key(path, glue_config, yt_client)
        assert check_table_schema_glue_columns(path, glue_config, yt_client)
        assert check_table_total_size(path, glue_config, yt_client)
        assert check_table_row_size(path, glue_config, yt_client)
        assert check_table_user_attribute(path, glue_config, yt_client)


def test_get_tables_to_check_info(mi_config, yt_server):
    yt_client = yt_server.get_yt_client()

    check_infos = get_tables_to_check_info(mi_config, yt_client)
    config_keys_actual = sorted([check_info.config_key for check_info in check_infos])
    config_keys_expected = [
        'external_data.source_path_ext1',
        'external_data.source_path_ext2',
        'external_data.source_path_ext3',
        'external_data.source_path_ext4',
        'external_data.source_path_ext5',
    ]
    assert_that(
        config_keys_actual,
        equal_to(config_keys_expected)
    )


def test_process_one_table(mi_config, yt_server):
    yt_client = yt_server.get_yt_client()
    check_infos = get_tables_to_check_info(mi_config, yt_client)
    for check_info in check_infos:
        process_one_table(mi_config, check_info, yt_client, False, False)

    assert_that(
        list(sorted(yt_client.list('//home/in/glue/ext1'))),
        equal_to(list(sorted([
            'recent',
            '2022-03-01',
        ])))
    )
    assert_that(
        yt_client.get_attribute('//home/in/glue/ext1/recent', 'path'),
        equal_to('//home/in/glue/ext1/2022-03-01')
    )

    assert_that(
        list(sorted(yt_client.list('//home/in/glue/ext2'))),
        equal_to(list(sorted([
            'recent',
            '2022-03-01',
        ])))
    )
    assert_that(
        yt_client.get_attribute('//home/in/glue/ext2/recent', 'path'),
        equal_to('//home/in/glue/ext2/2022-03-01')
    )

    assert_that(
        list(sorted(yt_client.list('//home/in/glue/ext3'))),
        equal_to(list(sorted([
            'recent',
            '2022-03-01',
        ])))
    )
    assert_that(
        yt_client.get_attribute('//home/in/glue/ext3/recent', 'path'),
        equal_to('//home/in/glue/ext3/2022-03-01')
    )

    assert_that(
        list(sorted(yt_client.list('//home/in/glue/ext5'))),
        equal_to(list(sorted([
            'recent',
            '2022-05-01',
        ])))
    )
    assert_that(
        yt_client.get_attribute('//home/in/glue/ext5/recent', 'path'),
        equal_to('//home/in/glue/ext5/2022-05-01')
    )


def test_prepare_single(mi_config):
    prepare(mi_config, 'external_data.source_path_ext1', False, False)
    prepare(mi_config, 'external_data.source_path_ext2', False, False)
    prepare(mi_config, 'external_data.source_path_ext3', False, False)
    prepare(mi_config, 'external_data.source_path_ext4', False, False)
    prepare(mi_config, 'external_data.source_path_ext5', False, False)


def test_prepare_all(mi_config):
    prepare(mi_config, '', False, False)
