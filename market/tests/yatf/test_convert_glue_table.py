import os
import tempfile
import pytest
import yatest

from google.protobuf import json_format
from hamcrest import assert_that
from market.idx.cron.cron_clt.lib.convert_glue_table.convert_glue_table import run_converter
from market.idx.library.glue.proto.GlueConfig_pb2 import (
    CommonConfig,
    CommonGlueField,
    ESupportedType,
    EReduceKeySchema,
)
from market.idx.marketindexer.miconfig import MiConfig
from market.idx.yatf.matchers.yt_rows_matchers import HasYtRowsBaseMatcher
from market.idx.yatf.resources.resource import FileGeneratorResource
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.glue_tables import (
    YtShortOfferKeyExternalGlueTable,
    YtFullOfferKeyExternalGlueTable,
    YtMskuKeyExternalGlueTable,
    YtModelKeyExternalGlueTable,
)


@pytest.fixture(scope='module')
def external_short_offer_glue_table(yt_server):
    path = '//home/in/glue/ext_short/2022-03-20'
    recent = '//home/in/glue/ext_short/recent'

    table = YtShortOfferKeyExternalGlueTable(
        yt_stuff=yt_server,
        path=path,
        link_paths=[recent],
        data=[
            {
                'business_id': 1,
                'offer_id': 'offer1',
                'source_int_field': 1,
            },
            {
                'business_id': 2,
                'offer_id': 'offer2',
                'source_int_field': 2,
            },
        ],
        data_columns_schema=[
            {'required': False, 'name': 'source_int_field', 'type': 'int64'},
        ],
    )
    table.create()
    return table


@pytest.fixture(scope='module')
def external_full_offer_glue_table(yt_server):
    path = '//home/in/glue/ext_full/2022-03-20'
    recent = '//home/in/glue/ext_full/recent'

    table = YtFullOfferKeyExternalGlueTable(
        yt_stuff=yt_server,
        path=path,
        link_paths=[recent],
        data=[
            {
                'business_id': 3,
                'offer_id': 'offer3',
                'shop_id': 3,
                'warehouse_id': 3,
                'source_bool_field': True,
            },
            {
                'business_id': 4,
                'offer_id': 'offer4',
                'shop_id': 4,
                'warehouse_id': 4,
                'source_bool_field': False,
            },
        ],
        data_columns_schema=[
            {'required': False, 'name': 'source_bool_field', 'type': 'boolean'},
        ],
    )
    table.create()
    return table


@pytest.fixture(scope='module')
def external_msku_glue_table(yt_server):
    path = '//home/in/glue/ext_msku/2022-03-20'
    recent = '//home/in/glue/ext_msku/recent'

    table = YtMskuKeyExternalGlueTable(
        yt_stuff=yt_server,
        path=path,
        link_paths=[recent],
        data=[
            {
                'market_sku': 100,
                'source_string_field': 'azaza',
                'source_double_field': 3.14,
            },
            {
                'market_sku': 200,
                'source_string_field': 'lolkek',
                'source_double_field': 2.7,
            },
        ],
        data_columns_schema=[
            {'required': False, 'name': 'source_string_field', 'type': 'string'},
            {'required': False, 'name': 'source_double_field', 'type': 'double'},
        ],
    )
    table.create()
    return table


@pytest.fixture(scope='module')
def external_model_glue_table(yt_server):
    path = '//home/in/glue/ext_model/2022-07-07'
    recent = '//home/in/glue/ext_model/recent'

    table = YtModelKeyExternalGlueTable(
        yt_stuff=yt_server,
        path=path,
        link_paths=[recent],
        data=[
            {
                'model_id': 1000,
                'source_string_field': 'azazaza',
            },
            {
                'model_id': 2000,
                'source_string_field': 'lolkeklol',
            },
        ],
        data_columns_schema=[
            {'required': False, 'name': 'source_string_field', 'type': 'string'},
        ],
    )
    table.create()
    return table


@pytest.yield_fixture(scope='module')
def glue_config(external_short_offer_glue_table, external_full_offer_glue_table, external_msku_glue_table, external_model_glue_table):
    config = CommonConfig()

    first_field = CommonGlueField()
    first_field.glue_id = 1
    first_field.declared_cpp_type = ESupportedType.INT32
    first_field.target_name = 'int_field'
    first_field.is_from_datacamp = False
    first_field.source_name = 'first_table'
    first_field.source_table_path = external_short_offer_glue_table.link
    first_field.source_field_path = 'source_int_field'
    first_field.destination_table_path = '//home/in/glue/ext_short/recent'
    first_field.reduce_key_schema = EReduceKeySchema.SHORT_OFFER_ID
    config.Fields.append(first_field)

    second_field = CommonGlueField()
    second_field.glue_id = 2
    second_field.declared_cpp_type = ESupportedType.BOOL
    second_field.target_name = 'bool_field'
    second_field.is_from_datacamp = False
    second_field.source_name = 'second_table'
    second_field.source_table_path = external_full_offer_glue_table.link
    second_field.source_field_path = 'source_bool_field'
    second_field.destination_table_path = '//home/in/glue/ext_full/recent'
    second_field.reduce_key_schema = EReduceKeySchema.FULL_OFFER_ID
    config.Fields.append(second_field)

    third_field = CommonGlueField()
    third_field.glue_id = 3
    third_field.declared_cpp_type = ESupportedType.STRING
    third_field.target_name = 'string_field'
    third_field.is_from_datacamp = False
    third_field.source_name = 'third_table'
    third_field.source_table_path = external_msku_glue_table.link
    third_field.source_field_path = 'source_string_field'
    third_field.destination_table_path = '//home/in/glue/ext_msku/recent'
    third_field.reduce_key_schema = EReduceKeySchema.MSKU
    config.Fields.append(third_field)

    fourth_field = CommonGlueField()
    fourth_field.glue_id = 4
    fourth_field.declared_cpp_type = ESupportedType.DOUBLE
    fourth_field.target_name = 'double_field'
    fourth_field.is_from_datacamp = False
    fourth_field.source_name = 'third_table'
    fourth_field.source_table_path = external_msku_glue_table.link
    fourth_field.source_field_path = 'source_double_field'
    fourth_field.destination_table_path = '//home/in/glue/ext_msku/recent'
    fourth_field.reduce_key_schema = EReduceKeySchema.MSKU
    config.Fields.append(fourth_field)

    fifth_field = CommonGlueField()
    fifth_field.glue_id = 5
    fifth_field.declared_cpp_type = ESupportedType.STRING
    fifth_field.target_name = 'string_field'
    fifth_field.is_from_datacamp = False
    fifth_field.source_name = 'fifth_table'
    fifth_field.source_table_path = external_model_glue_table.link
    fifth_field.source_field_path = 'source_string_field'
    fifth_field.destination_table_path = '//home/in/glue/ext_model/recent'
    fifth_field.reduce_key_schema = EReduceKeySchema.MODEL_ID
    config.Fields.append(fifth_field)

    return config


@pytest.yield_fixture(scope='module')
def glue_config_file(glue_config):
    json_message = json_format.MessageToJson(glue_config)
    file = FileGeneratorResource(filename='glue_config.json')
    file.dump(path=os.path.join(tempfile.mkdtemp(), file.filename))
    dirname = os.path.dirname(file.path)
    if not os.path.exists(dirname):
        os.makedirs(dirname)
    with open(file.path, 'w') as f:
        f.write(json_message)
    return file


@pytest.yield_fixture(scope='module')
def miconfig_data(
        yt_server,
        glue_config_file
):
    return '''
[bin]
glue_tables_converter_bin = {glue_tables_converter_bin_path}

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
'''.format(
        glue_tables_converter_bin_path=yatest.common.binary_path(
            'market/idx/offers/bin/glue_tables_converter/glue_tables_converter'),
        glue_config_path=glue_config_file.path,
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


@pytest.yield_fixture(scope='module')
def workflow(mi_config):
    run_converter(mi_config)


def test_tables_existing(workflow, yt_server):
    yt_client = yt_server.get_yt_client()
    assert yt_client.exists('//home/in/glue/ext_short_converted/2022-03-20')
    assert yt_client.exists('//home/in/glue/ext_full_converted/2022-03-20')
    assert yt_client.exists('//home/in/glue/ext_msku_converted/2022-03-20')
    assert yt_client.exists('//home/in/glue/ext_model_converted/2022-07-07')


def test_recent_links_existing(workflow, yt_server):
    yt_client = yt_server.get_yt_client()
    assert yt_client.exists('//home/in/glue/ext_short_converted/recent')
    assert yt_client.exists('//home/in/glue/ext_full_converted/recent')
    assert yt_client.exists('//home/in/glue/ext_msku_converted/recent')
    assert yt_client.exists('//home/in/glue/ext_model_converted/recent')


def test_short_offer_key_table(workflow, yt_server):
    table = YtTableResource(yt_server, '//home/in/glue/ext_short_converted/recent', load=True)
    assert len(table.data) == 2

    assert_that(table.data, HasYtRowsBaseMatcher([
        {
            'offer_id': 'offer1',
            'business_id': 1,
            'glue_fields': [
                {
                    'glue_id': 1,
                    'double_value': None,
                    'int64_value': None,
                    'string_value': None,
                    'int32_value': 1,
                    'bool_value': None,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
            ]
        },
        {
            'offer_id': 'offer2',
            'business_id': 2,
            'glue_fields': [
                {
                    'glue_id': 1,
                    'double_value': None,
                    'int64_value': None,
                    'string_value': None,
                    'int32_value': 2,
                    'bool_value': None,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
            ]
        },
    ]))


def test_full_offer_key_table(workflow, yt_server):
    table = YtTableResource(yt_server, '//home/in/glue/ext_full_converted/recent', load=True)
    assert len(table.data) == 2

    assert_that(table.data, HasYtRowsBaseMatcher([
        {
            'offer_id': 'offer3',
            'shop_id': 3,
            'warehouse_id': 3,
            'business_id': 3,
            'glue_fields': [
                {
                    'glue_id': 2,
                    'double_value': None,
                    'int64_value': None,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': True,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
            ]
        },
        {
            'offer_id': 'offer4',
            'shop_id': 4,
            'warehouse_id': 4,
            'business_id': 4,
            'glue_fields': [
                {
                    'glue_id': 2,
                    'double_value': None,
                    'int64_value': None,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': False,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
            ]
        },
    ]))


def test_msku_key_table(workflow, yt_server):
    table = YtTableResource(yt_server, '//home/in/glue/ext_msku_converted/recent', load=True)
    assert len(table.data) == 2

    assert_that(table.data, HasYtRowsBaseMatcher([
        {
            'market_sku': 100,
            'available_for_fake_msku': None,
            'glue_fields': [
                {
                    'glue_id': 3,
                    'double_value': None,
                    'int64_value': None,
                    'string_value': 'azaza',
                    'int32_value': None,
                    'bool_value': None,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
                {
                    'glue_id': 4,
                    'double_value': 3.14,
                    'int64_value': None,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': None,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
            ]
        },
        {
            'market_sku': 200,
            'available_for_fake_msku': None,
            'glue_fields': [
                {
                    'glue_id': 3,
                    'double_value': None,
                    'int64_value': None,
                    'string_value': 'lolkek',
                    'int32_value': None,
                    'bool_value': None,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
                {
                    'glue_id': 4,
                    'double_value': 2.7,
                    'int64_value': None,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': None,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
            ]
        },
    ]))


def test_model_key_table(workflow, yt_server):
    table = YtTableResource(yt_server, '//home/in/glue/ext_model_converted/recent', load=True)
    assert len(table.data) == 2

    assert_that(table.data, HasYtRowsBaseMatcher([
        {
            'model_id': 1000,
            'available_for_fake_msku': None,
            'glue_fields': [
                {
                    'glue_id': 5,
                    'double_value': None,
                    'int64_value': None,
                    'string_value': 'azazaza',
                    'int32_value': None,
                    'bool_value': None,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
            ]
        },
        {
            'model_id': 2000,
            'available_for_fake_msku': None,
            'glue_fields': [
                {
                    'glue_id': 5,
                    'double_value': None,
                    'int64_value': None,
                    'string_value': 'lolkeklol',
                    'int32_value': None,
                    'bool_value': None,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
            ]
        },
    ]))
