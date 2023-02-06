import pytest
from hamcrest import assert_that, not_
from google.protobuf import json_format
import yandex.type_info.typing as ti
from yt.wrapper.schema import TableSchema
from yt.wrapper.ypath import ypath_join
from yt.wrapper.yson import YsonUint64
from market.idx.yatf.matchers.yt_rows_matchers import HasYtRowsBaseMatcher
from market.idx.offers.yatf.test_envs.glue_msku_reducer import GlueMskuReducerTestEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.library.glue.proto.GlueConfig_pb2 import FieldValue
from market.idx.library.glue.proto.GlueMessages_pb2 import MskuFields
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.proto.indexer.GenerationLog_pb2 import Record


@pytest.fixture(scope="module")
def offers():
    return [
        {
            'genlog': Record(
                business_id=1,
                offer_id='offer_available_for_fake_unknown_fake_status',
                warehouse_id=15,
                shop_id=101,
                market_sku=1234,
                is_fake_msku_offer=None
            ).SerializeToString()
        },
        {
            'genlog': Record(
                offer_id='offer_available_for_fake_fake_msku_offer',
                shop_id=102,
                market_sku=1234,
                is_fake_msku_offer=True
            ).SerializeToString()
        },
        {
            'genlog': Record(
                business_id=3,
                offer_id='offer_available_for_fake_real_msku_offer',
                warehouse_id=17,
                shop_id=103,
                market_sku=1234,
                is_fake_msku_offer=False
            ).SerializeToString()
        },
        {
            'genlog': Record(
                business_id=4,
                offer_id='offer_non_available_for_fake_unknown_fake_status',
                warehouse_id=18,
                shop_id=104,
                market_sku=1235,
                is_fake_msku_offer=None
            ).SerializeToString()
        },
        {
            'genlog': Record(
                offer_id='offer_non_available_for_fake_fake_msku_offer',
                shop_id=105,
                market_sku=1235,
                is_fake_msku_offer=True
            ).SerializeToString()
        },
        {
            'genlog': Record(
                business_id=6,
                offer_id='offer_non_available_for_fake_real_msku_offer',
                warehouse_id=20,
                shop_id=106,
                market_sku=1235,
                is_fake_msku_offer=False
            ).SerializeToString()
        },
        {
            'genlog': Record(
                business_id=7,
                offer_id='offer_unknown_status_for_fake_unknown_fake_status',
                warehouse_id=21,
                shop_id=107,
                market_sku=1236,
                is_fake_msku_offer=None
            ).SerializeToString()
        },
        {
            'genlog': Record(
                offer_id='offer_unknown_status_for_fake_fake_msku_offer',
                shop_id=108,
                market_sku=1236,
                is_fake_msku_offer=True
            ).SerializeToString()
        },
        {
            'genlog': Record(
                business_id=9,
                offer_id='offer_unknown_status_for_fake_real_msku_offer',
                warehouse_id=23,
                shop_id=109,
                market_sku=1236,
                is_fake_msku_offer=False
            ).SerializeToString()
        },
    ]


@pytest.fixture(scope="module")
def glue_msku_data():
    data = [
        json_format.MessageToDict(
            MskuFields(
                market_sku=1234,
                available_for_fake_msku=True,
                glue_fields=[
                    FieldValue(glue_id=4, bool_value=True),
                    FieldValue(glue_id=5, int64_value=1337)
                ]
            ),
            preserving_proto_field_name=True,
        ),
        json_format.MessageToDict(
            MskuFields(
                market_sku=1235,
                available_for_fake_msku=False,
                glue_fields=[
                    FieldValue(glue_id=4, bool_value=False),
                    FieldValue(glue_id=5, int64_value=1646468)
                ]
            ),
            preserving_proto_field_name=True,
        ),
        json_format.MessageToDict(
            MskuFields(
                market_sku=1236,
                available_for_fake_msku=None,
                glue_fields=[
                    FieldValue(glue_id=4, bool_value=True),
                    FieldValue(glue_id=5, int64_value=6432)
                ]
            ),
            preserving_proto_field_name=True,
        ),
    ]

    for glue_msku in data:
        glue_msku['market_sku'] = YsonUint64(glue_msku['market_sku'])
        for glue_field in glue_msku['glue_fields']:
            glue_field['glue_id'] = YsonUint64(glue_field['glue_id'])
            if glue_field.get('uint32_value') is not None:
                glue_field['uint32_value'] = YsonUint64(glue_field['uint32_value'])
            if glue_field.get('uint64_value') is not None:
                glue_field['uint64_value'] = YsonUint64(glue_field['uint64_value'])
            if glue_field.get('int64_value') is not None:
                glue_field['int64_value'] = int(glue_field['int64_value'])

    return data


@pytest.yield_fixture(scope="module")
def workflow(yt_server, offers, glue_msku_data):
    offers_table = YtTableResource(yt_stuff=yt_server, path=ypath_join(get_yt_prefix(), 'input', 'offers'), data=offers)
    offers_table.dump()
    schema = TableSchema().add_column('market_sku', ti.Uint64).add_column('available_for_fake_msku', ti.Optional[ti.Bool]).add_column(
        'glue_fields',
        ti.List[
            ti.Struct[
                'glue_id': ti.Optional[ti.Uint32],
                'bool_value' : ti.Optional[ti.Bool],
                'double_value' : ti.Optional[ti.Double],
                'float_value' : ti.Optional[ti.Float],
                'int32_value' : ti.Optional[ti.Int32],
                'int64_value' : ti.Optional[ti.Int64],
                'uint32_value' : ti.Optional[ti.Uint32],
                'uint64_value' : ti.Optional[ti.Uint64],
                'string_value' : ti.Optional[ti.String],
            ]
        ],
    )
    glue_msku_table = YtTableResource(
        yt_stuff=yt_server,
        path=ypath_join(get_yt_prefix(), 'input', 'glue_msku'),
        data=glue_msku_data,
        sort_key='market_sku',
        attributes={'schema': schema},
    )
    glue_msku_table.dump()

    output_table_path = ypath_join(get_yt_prefix(), 'out_msku_keys')

    with GlueMskuReducerTestEnv(
            yt_server,
            output_yt_path=output_table_path,
            input_offers_yt_paths=[offers_table.table_path],
            input_msku_glue_yt_paths=[glue_msku_table.table_path],
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def result_data(yt_server, workflow):
    return YtTableResource(yt_stuff=yt_server, path=workflow.output_table, load=True).data


def test_available_for_fake_msku(result_data):
    """Проверяем, что клеевые поля по fake_msku приматчился только к fake_msku"""
    expected_offers = [
        {
            'business_id': None,
            'shop_id': 102,
            'warehouse_id': None,
            'offer_id': 'offer_available_for_fake_fake_msku_offer',
            'glue_fields': [
                {
                    'glue_id': 4,
                    'double_value': None,
                    'int64_value': None,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': True,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
                {
                    'glue_id': 5,
                    'double_value': None,
                    'int64_value': 1337,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': None,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
            ],
        },
    ]

    not_expected_offers = [
        {
            'business_id': 1,
            'shop_id': 101,
            'warehouse_id': 15,
            'offer_id': 'offer_available_for_fake_unknown_fake_status',
        },
        {
            'business_id': 3,
            'shop_id': 103,
            'warehouse_id': 17,
            'offer_id': 'offer_available_for_fake_real_msku_offer',
        },
    ]

    for offer in expected_offers:
        assert_that(
            result_data,
            HasYtRowsBaseMatcher([
                offer
            ])
        )

    for offer in not_expected_offers:
        assert_that(
            result_data,
            not_(HasYtRowsBaseMatcher([
                offer
            ]))
        )


def test_non_available_for_fake_msku(result_data):
    """Проверяем, что клеевые поля для не fake_msku приматчились к не fake_msku офферам"""
    expected_offers = [
        {
            'business_id': 4,
            'shop_id': 104,
            'warehouse_id': 18,
            'offer_id': 'offer_non_available_for_fake_unknown_fake_status',
            'glue_fields': [
                {
                    'glue_id': 4,
                    'double_value': None,
                    'int64_value': None,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': False,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
                {
                    'glue_id': 5,
                    'double_value': None,
                    'int64_value': 1646468,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': None,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
            ],
        },
        {
            'business_id': 6,
            'shop_id': 106,
            'warehouse_id': 20,
            'offer_id': 'offer_non_available_for_fake_real_msku_offer',
            'glue_fields': [
                {
                    'glue_id': 4,
                    'double_value': None,
                    'int64_value': None,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': False,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
                {
                    'glue_id': 5,
                    'double_value': None,
                    'int64_value': 1646468,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': None,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
            ],
        },
    ]

    not_expected_offers = [
        {
            'business_id': None,
            'shop_id': 105,
            'warehouse_id': None,
            'offer_id': 'offer_non_available_for_fake_fake_msku_offer',
        },
    ]

    for offer in expected_offers:
        assert_that(
            result_data,
            HasYtRowsBaseMatcher([
                offer
            ])
        )

    for offer in not_expected_offers:
        assert_that(
            result_data,
            not_(HasYtRowsBaseMatcher([
                offer
            ]))
        )


def test_unknown_availability_for_fake_msku(result_data):
    """Проверяем, что клеевые поля, которым безразлично на fake_msku приматчились ко всем офферам"""
    expected_offers = [
        {
            'business_id': 7,
            'shop_id': 107,
            'warehouse_id': 21,
            'offer_id': 'offer_unknown_status_for_fake_unknown_fake_status',
            'glue_fields': [
                {
                    'glue_id': 4,
                    'double_value': None,
                    'int64_value': None,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': True,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
                {
                    'glue_id': 5,
                    'double_value': None,
                    'int64_value': 6432,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': None,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
            ],
        },
        {
            'business_id': None,
            'shop_id': 108,
            'warehouse_id': None,
            'offer_id': 'offer_unknown_status_for_fake_fake_msku_offer',
            'glue_fields': [
                {
                    'glue_id': 4,
                    'double_value': None,
                    'int64_value': None,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': True,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
                {
                    'glue_id': 5,
                    'double_value': None,
                    'int64_value': 6432,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': None,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
            ],
        },
        {
            'business_id': 9,
            'shop_id': 109,
            'warehouse_id': 23,
            'offer_id': 'offer_unknown_status_for_fake_real_msku_offer',
            'glue_fields': [
                {
                    'glue_id': 4,
                    'double_value': None,
                    'int64_value': None,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': True,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
                {
                    'glue_id': 5,
                    'double_value': None,
                    'int64_value': 6432,
                    'string_value': None,
                    'int32_value': None,
                    'bool_value': None,
                    'uint32_value': None,
                    'float_value': None,
                    'uint64_value': None,
                },
            ],
        },
    ]

    for offer in expected_offers:
        assert_that(
            result_data,
            HasYtRowsBaseMatcher([
                offer
            ])
        )
