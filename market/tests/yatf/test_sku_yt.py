# coding=utf-8
import pytest

from google.protobuf import json_format

from hamcrest import (
    assert_that,
    equal_to
)
import market.proto.content.mbo.ExportReportModel_pb2 as ExportReportModel_pb2
import market.proto.content.mbo.MboParameters_pb2 as MboParameters_pb2
from market.proto.indexer import GenerationLog_pb2

from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    LocalizedString,
    ParameterValue,
    ParameterValueHypothesis,
    Picture,
    Video,
)
from market.proto.content.mbo.MboParameters_pb2 import (
    Category,
    Parameter,
    Option,
    Word,
)
import market.proto.feedparser.deprecated.OffersData_pb2
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import OfferData2GenlogProtobuf
import market.proto.ir.UltraController_pb2
from market.proto.ir.UltraController_pb2 import FormalizedParamPosition

from msku_uploader.yatf.resources.mbo_yt import (
    MboAllModelsTable,
    MboModelsTable,
    MboParamsTable,
    MboSkuTable,
)
from msku_uploader.yatf.test_env import MskuUploaderTestEnv
from msku_uploader.yatf.utils import (
    make_sku_protobuf,
    make_model_protobuf,
    compare_genlog_field
)

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from mapreduce.yt.python.table_schema import extract_column_attributes


CATEG_ID = 989040
MODEL_TITLE_CATEG_ID = 989041
MODEL_ID = 1713074440
PARTNER_MODEL_ID = 1713074441
BLUE_SKU_FEED = 475690


def make_big_sku(skuid, title, model_id, category_id, is_partner_sku=False):
    return make_sku_protobuf(
        skuid=skuid,
        title=title,
        model_id=model_id,
        category_id=category_id,
        is_partner_sku=is_partner_sku,
        pictures=[
            Picture(
                xslName="XL-Picture",
                url="//avatars.mds.yandex.net/get-mpic/175985/img_id456/orig",
                width=572,
                height=598,
                url_source="aaa",
                url_orig="bbb",
            ),
            Picture(
                xslName="XL-Picture2",
                url="//avatars.mds.yandex.net/get-mpic/175985/img_id789/orig",
            ),
        ],
        parameters=[
            ParameterValue(
                param_id=1000000,
                option_id=128,
                xsl_name="NumValueSearch",
                value_source=ExportReportModel_pb2.OPERATOR_FILLED,
                user_id=307184859,
                modification_date=1480612684002,
                value_type=MboParameters_pb2.NUMERIC_ENUM,
            ),
            ParameterValue(
                param_id=1000001,
                numeric_value="127",
                xsl_name="Length",
                value_source=ExportReportModel_pb2.OPERATOR_FILLED,
                user_id=307184859,
                modification_date=1480612684002,
                value_type=MboParameters_pb2.NUMERIC,
            ),
            ParameterValue(
                param_id=1000002,
                bool_value=False,
                option_id=12976298,
                xsl_name="DiaperTable",
                value_source=ExportReportModel_pb2.OPERATOR_FILLED,
                user_id=307184859,
                modification_date=1480612684002,
                value_type=MboParameters_pb2.BOOLEAN,
            ),
            ParameterValue(
                param_id=1000003,
                type_id=1,
                option_id=12109936,
                xsl_name="BackType",
                value_source=ExportReportModel_pb2.OPERATOR_FILLED,
                user_id=307184859,
                modification_date=1480612684002,
                value_type=MboParameters_pb2.ENUM,
            ),
            ParameterValue(
                param_id=1000004,
                str_value=[LocalizedString(
                    isoCode="ru",
                    value="Fiore",
                )],
                xsl_name="name",
                value_source=ExportReportModel_pb2.AUTO,
                user_id=28027378,
                modification_date=1515710024025,
                value_type=MboParameters_pb2.STRING,
            ),
            ParameterValue(
                param_id=1000005,
                str_value=[LocalizedString(
                    isoCode="ru",
                    value="Не содержит изотопов урана 235 и других радионуклидов",
                )],
                xsl_name="description",
                value_source=ExportReportModel_pb2.AUTO,
                user_id=28027378,
                modification_date=1515710024025,
                value_type=MboParameters_pb2.STRING,
            ),
            ParameterValue(
                param_id=1000006,
                str_value=[
                    LocalizedString(isoCode="ru", value="100500"),
                    LocalizedString(isoCode="ru", value="0100500"),
                    LocalizedString(isoCode="ru", value="00100500")
                ],
                xsl_name="BarCode",
                value_source=ExportReportModel_pb2.AUTO,
                user_id=28027378,
                modification_date=1515710024025,
                value_type=MboParameters_pb2.STRING,
            ),
            ParameterValue(
                param_id=1001006,
                str_value=[LocalizedString(
                    isoCode="ru",
                    value="Крутое название",
                )],
                xsl_name="raw_vendor",
                value_source=ExportReportModel_pb2.AUTO,
                user_id=28027378,
                modification_date=1515710024025,
                value_type=MboParameters_pb2.STRING,
            ),

            ParameterValue(
                param_id=1000007,
                option_id=12109937,
                xsl_name="AutoGeneratedParam",
                value_source=ExportReportModel_pb2.RULE,
                user_id=307184859,
                modification_date=1480612684002,
                value_type=MboParameters_pb2.ENUM,
            ),
            ParameterValue(
                param_id=1000008,
                bool_value=True,
                option_id=12976298,
                xsl_name="OperatorFilledParam",
                value_source=ExportReportModel_pb2.OPERATOR_FILLED,
                user_id=307184859,
                modification_date=1480612684002,
                value_type=MboParameters_pb2.BOOLEAN,
            ),

            ParameterValue(  # cargo type should be taken into account
                param_id=1000009,
                bool_value=True,
                option_id=12976299,
                xsl_name="cargo_type_1",
                user_id=307184859,
                modification_date=1480612684002,
                value_type=MboParameters_pb2.BOOLEAN,
            ),
            ParameterValue(  # cargo type which should be skipped: bool_value == False
                param_id=1000010,
                bool_value=False,
                option_id=12976300,
                xsl_name="cargo_type_2",
                user_id=307184859,
                modification_date=1480612684002,
                value_type=MboParameters_pb2.BOOLEAN,
            ),
            ParameterValue(  # second cargo type should be taken into account
                param_id=1000012,
                bool_value=True,
                option_id=12976302,
                xsl_name="cargo_type_4",
                user_id=307184859,
                modification_date=1480612684002,
                value_type=MboParameters_pb2.BOOLEAN,
            ),

            ParameterValue(
                param_id=1000013,
                bool_value=True,
                xsl_name="exclusive",
                value_source=ExportReportModel_pb2.AUTO,
                user_id=28027378,
                modification_date=1515710024025,
                value_type=MboParameters_pb2.BOOLEAN,
            ),

            ParameterValue(
                param_id=1000014,
                bool_value=True,
                xsl_name="hype_goods",
                value_source=ExportReportModel_pb2.AUTO,
                user_id=28027378,
                modification_date=1515710024025,
                value_type=MboParameters_pb2.BOOLEAN,
            ),

            ParameterValue(
                param_id=1000015,
                numeric_value="24",
                xsl_name="subscription_term",
                value_source=ExportReportModel_pb2.AUTO,
                user_id=28027378,
                modification_date=1515710024025,
                value_type=MboParameters_pb2.NUMERIC,
            ),

            # Данные параметры будут добавлены к данным для генерации характеристик МСКУ
            ParameterValue(
                param_id=1000100,
                option_id=129,
                xsl_name="NumValueModel",
                value_source=ExportReportModel_pb2.OPERATOR_FILLED,
                user_id=307184859,
                modification_date=1480612684002,
                value_type=MboParameters_pb2.NUMERIC_ENUM,
            ),
            ParameterValue(
                param_id=1000101,
                numeric_value="126",
                xsl_name="NumericModel",
                value_source=ExportReportModel_pb2.OPERATOR_FILLED,
                user_id=307184859,
                modification_date=1480612684002,
                value_type=MboParameters_pb2.NUMERIC,
            ),
            ParameterValue(
                param_id=1000102,
                bool_value=False,
                option_id=12976298,
                xsl_name="BoolModel",
                value_source=ExportReportModel_pb2.OPERATOR_FILLED,
                user_id=307184859,
                modification_date=1480612684002,
                value_type=MboParameters_pb2.BOOLEAN,
            ),
            ParameterValue(
                param_id=1000103,
                type_id=1,
                option_id=12109936,
                xsl_name="EnumModel",
                value_source=ExportReportModel_pb2.OPERATOR_FILLED,
                user_id=307184859,
                modification_date=1480612684002,
                value_type=MboParameters_pb2.ENUM,
            ),
            ParameterValue(
                param_id=1000104,
                str_value=[LocalizedString(
                    isoCode="ru",
                    value="FioreModel",
                )],
                xsl_name="nameModel",
                value_source=ExportReportModel_pb2.AUTO,
                user_id=28027378,
                modification_date=1515710024025,
                value_type=MboParameters_pb2.STRING,
            ),

        ],
        parameter_hypothesis=[
            # Данные параметры будут добавлены к данным для генерации характеристик МСКУ
            ParameterValueHypothesis(
                param_id=1000100,
                xsl_name="NumValueModelHypo",
                user_id=307184859,
                str_value=[Word(
                    name="1000100LHypo",
                )],
                created_date=1480612684002,
                value_type=MboParameters_pb2.NUMERIC_ENUM,
            ),
            ParameterValueHypothesis(
                param_id=1000101,
                xsl_name="NumericModelHypo",
                value_type=MboParameters_pb2.NUMERIC,
                str_value=[Word(
                    name="1000101LHypo",
                )],
                user_id=307184859,
                created_date=1480612684002,
            ),
            ParameterValueHypothesis(
                param_id=1000102,
                xsl_name="BoolModelHypo",
                value_type=MboParameters_pb2.BOOLEAN,
                str_value=[Word(
                    name="1000102LHypo",
                )],
                user_id=307184859,
                created_date=1480612684002,
            ),
            ParameterValueHypothesis(
                param_id=1000103,
                xsl_name="EnumModelHypo",
                value_type=MboParameters_pb2.ENUM,
                str_value=[Word(
                    name="1000103LHypo",
                )],
                user_id=307184859,
                created_date=1480612684002,
            ),
            ParameterValueHypothesis(
                param_id=1000104,
                xsl_name="nameModelHypo",
                value_type=MboParameters_pb2.STRING,
                str_value=[Word(
                    name="1000104LHypo",
                )],
                user_id=28027378,
                created_date=1515710024025,
            ),
        ],
        videos=[
            Video(url='www.youtube.com/watch?v=1'),
            Video(url='www.youtube.com/watch?v=2'),
        ],
    )


@pytest.fixture(scope='module')
def mbo_msku_protobufs():
    data = {
        'big_sku': make_big_sku(
            skuid=100000000001,
            title='SKU со всеми данными',
            model_id=MODEL_ID,
            category_id=CATEG_ID
        ),
        'min_sku': make_sku_protobuf(
            skuid=100000000002,
            title='SKU с минимально необходимыми данными',
            model_id=MODEL_ID,
            category_id=CATEG_ID
        ),
        'nonpub_sku': make_sku_protobuf(
            skuid=100000000003,
            title='Неопубликованный SKU',
            model_id=MODEL_ID,
            category_id=CATEG_ID
        ),
        'bad_sku': ExportReportModel(category_id=111),
        'notitle_sku': make_big_sku(
            skuid=100000000001,
            title='',
            model_id=MODEL_ID,
            category_id=CATEG_ID
        ),
        'partner_sku': make_sku_protobuf(
            skuid=100000000004,
            title='PARTNER_SKU с минимально необходимыми данными',
            model_id=PARTNER_MODEL_ID,
            category_id=CATEG_ID,
            is_partner_sku=True
        ),
    }
    data['nonpub_sku'].published_on_blue_market = False

    # order does matter
    return [
        data['big_sku'],
        data['min_sku'],
        data['nonpub_sku'],
        data['bad_sku'],
        data['notitle_sku'],
        data['partner_sku'],
    ]


@pytest.fixture(scope='module')
def mbo_models_protobufs():
    return [
        make_model_protobuf(
            model_id=MODEL_ID,
            category_id=CATEG_ID
        ),
        make_model_protobuf(
            model_id=PARTNER_MODEL_ID,
            category_id=CATEG_ID,
            is_partner_model=True
        ),
    ]


def create_params_descr():
    return [
        Parameter(
            id=1000000,
            xsl_name='NumValueSearch',
            published=True,
            common_filter_index=1,
            value_type='NUMERIC_ENUM',
            option=[
                Option(
                    id=128,
                    name=[Word(
                        name='128',
                    )]
                )
            ]
        ),
        Parameter(
            id=1000001,
            xsl_name='Length',
            published=True,
            common_filter_index=1,
            value_type='NUMERIC'
        ),
        Parameter(
            id=1000002,
            xsl_name='DiaperTable',
            published=True,
            common_filter_index=1,
            value_type='BOOLEAN'
        ),
        Parameter(
            id=1000003,
            xsl_name='BackType',
            published=True,
            common_filter_index=1,
            value_type='ENUM',
        ),
        Parameter(
            id=1000004,
            xsl_name='name',
            published=True,
            common_filter_index=1,
            value_type='STRING'
        ),

        # Special params
        Parameter(
            id=1000005,
            xsl_name='description',
            published=True,
            common_filter_index=1,
            value_type='STRING'
        ),
        Parameter(
            id=1000006,
            xsl_name='BarCode',
            published=True,
            common_filter_index=1,
            value_type='STRING'
        ),
        Parameter(
            id=1001006,
            xsl_name='raw_vendor',
            published=True,
            common_filter_index=1,
            value_type='STRING'
        ),

        # For operator filled check
        Parameter(
            id=1000007,
            xsl_name='AutoGeneratedParam',
            published=True,
            common_filter_index=1,
            value_type='ENUM'
        ),
        Parameter(
            id=1000008,
            xsl_name='OperatorFilledParam',
            published=True,
            common_filter_index=1,
            value_type='BOOLEAN'
        ),

        # Cargo types
        Parameter(
            id=1000009,
            xsl_name='cargo_type_1',
            published=True,
            common_filter_index=1,
            value_type='BOOLEAN'
        ),
        Parameter(
            id=1000010,
            xsl_name='cargo_type_2',
            published=True,
            common_filter_index=1,
            value_type='BOOLEAN'
        ),
        Parameter(
            id=1000012,
            xsl_name='cargo_type_4',
            published=True,
            common_filter_index=1,
            value_type='BOOLEAN'
        ),

        Parameter(
            id=1000013,
            xsl_name='exclusive',
            published=True,
            common_filter_index=1,
            value_type='BOOLEAN'
        ),

        Parameter(
            id=1000014,
            xsl_name='hype_goods',
            published=True,
            common_filter_index=1,
            value_type='BOOLEAN'
        ),

        Parameter(
            id=1000015,
            xsl_name='subscription_term',
            published=True,
            common_filter_index=1,
            value_type='NUMERIC'
        ),

        # Параметр, сохряняемый для характеристик
        Parameter(
            id=1000100,
            xsl_name='NumValueModel',
            published=True,
            value_type='NUMERIC_ENUM',
            option=[
                Option(
                    id=129,
                    name=[Word(
                        name='129',
                    )]
                )
            ]
        ),
        Parameter(
            id=1000101,
            xsl_name='NumericModel',
            published=True,
            value_type='NUMERIC'
        ),
        Parameter(
            id=1000102,
            xsl_name='BoolModel',
            published=True,
            value_type='BOOLEAN'
        ),
        Parameter(
            id=1000103,
            xsl_name='EnumModel',
            published=True,
            value_type='ENUM',
        ),
        Parameter(
            id=1000104,
            xsl_name='nameModel',
            published=True,
            value_type='STRING'
        ),

    ]


@pytest.fixture(scope='module')
def mbo_category_protobufs():
    return [
        Category(
            hid=CATEG_ID,
            parameter=create_params_descr()
        ),
    ]


@pytest.fixture(scope='module')
def yt_dir():
    return get_yt_prefix()


@pytest.fixture(scope='module')
def mbo_sku_table(yt_server, yt_dir, mbo_msku_protobufs):
    return MboSkuTable(
        yt_server,
        yt_dir,
        data=[
            {
                'category_id': CATEG_ID,
                'data': mbo_sku.SerializeToString(),
            }
            for mbo_sku in mbo_msku_protobufs
        ]
    )


@pytest.fixture(scope='module')
def mbo_models_table(yt_server, yt_dir, mbo_models_protobufs):
    return MboModelsTable(
        yt_server,
        yt_dir,
        data=[
            {
                'category_id': CATEG_ID,
                'data': mbo_model.SerializeToString(),
            }
            for mbo_model in mbo_models_protobufs
        ]
    )


@pytest.fixture(scope='module')
def mbo_all_models_table(yt_server, yt_dir, mbo_models_protobufs, mbo_msku_protobufs):
    return MboAllModelsTable(
        yt_server,
        yt_dir,
        data=[
            {
                'data': mbo_model.SerializeToString(),
            }
            for mbo_model in
            mbo_models_protobufs + mbo_msku_protobufs
        ]
    )


@pytest.fixture(scope='module')
def mbo_params_table(yt_server, yt_dir, mbo_category_protobufs):
    return MboParamsTable(
        yt_server,
        yt_dir,
        data=[
            {
                'hid': CATEG_ID,
                'data': mbo_category.SerializeToString()
            }
            for mbo_category in mbo_category_protobufs
        ]
    )


def create_cargo_types_table(yt_server):
    yt_client = yt_server.get_yt_client()

    schema = [
        dict(name='id', type='int64'),  # cargo_type_id
        dict(name='mbo_parameter_id', type='int64'),  # mbo_param_id
    ]

    attributes = {'schema': schema}

    table_name = '//home/test/mbo_id_to_cargo_type'
    yt_client.create(
        'table',
        table_name,
        ignore_existing=True,
        recursive=True,
        attributes=attributes
    )

    yt_client.write_table(
        table_name,
        [
            dict(id=100, mbo_parameter_id=1000009),
            dict(id=200, mbo_parameter_id=1000010),
            # param 1000008L is absent in this map
            dict(id=400, mbo_parameter_id=1000012)
        ]
    )


@pytest.yield_fixture(scope='module')
def workflow(
    yt_server,
    mbo_all_models_table,
    mbo_models_table,
    mbo_params_table,
    mbo_sku_table,
    yt_dir,
):
    create_cargo_types_table(yt_server)
    resources = {
        'mbo_all_models_table': mbo_all_models_table,
        'mbo_models_table': mbo_models_table,
        'mbo_params_table': mbo_params_table,
        'mbo_sku_table': mbo_sku_table,
    }

    with MskuUploaderTestEnv(yt_input_dir=yt_dir, **resources) as env:
        env.execute(yt_server)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_yt_table(workflow):
    return workflow.outputs.get('result_table')


@pytest.fixture(scope='module')
def result_yt_ext_table(workflow):
    return workflow.outputs.get('result_ext_table')


def test_results_exist_yt(yt_server, result_yt_table):
    assert_that(
        yt_server.get_yt_client().exists(result_yt_table.get_path()),
        'Yt table not exists'
    )


def test_results_ext_exist_yt(yt_server, result_yt_ext_table):
    assert_that(
        yt_server.get_yt_client().exists(result_yt_ext_table.get_path()),
        'Yt_ext table not exists'
    )


def test_result_table_schema_yt(result_yt_table):
    result_list = sorted(
        extract_column_attributes(list(result_yt_table.schema)),
        key=lambda column: column["name"]
    )
    expected_list = sorted(
        [
            {'required': False, "name": "msku",         "type": "uint64", "sort_order": "ascending"},
            {'required': False, "name": "feed_id",      "type": "uint64"},
            {'required': False, "name": "session_id",   "type": "uint64"},
            {'required': False, "name": "offer_id",     "type": "string"},
            {'required': False, "name": "offer",        "type": "string"},
            {'required': False, "name": "recs",         "type": "string"},
            {'required': False, "name": "promo",        "type": "string"},
            {'required': False, "name": "uc",           "type": "string"},
            {'required': False, "name": "pic",          "type": "string"},
            {'required': False, "name": "couple_id",    "type": "uint64"},
            {'required': False, "name": "diff_type",    "type": "string"},
            {'required': False, "name": "params",       "type": "string"},
            {'required': False, "name": "bids",         "type": "string"},
            {'required': False, "name": "ware_md5",     "type": "string"},
            {'required': False, "name": "disabled_flags", "type": "uint64"},
            {'required': False, "name": "published_ts", "type": "uint64"},
            {'required': False, "name": "finished_ts",  "type": "uint64"},
            {'required': False, "name": "offer_score",  "type": "double"},
            {'required': False, "name": "offer_score_policy_id",  "type": "uint64"},
        ],
        key=lambda column: column["name"]
    )
    assert_that(
        result_list,
        equal_to(expected_list),
        "Schema is incorrect"
    )


def test_result_ext_table_schema_yt(result_yt_ext_table):
    result_list = sorted(
        extract_column_attributes(list(result_yt_ext_table.schema)),
        key=lambda column: column["name"]
    )
    expected_list = sorted(
        [
            {'required': False, "name": "msku",         "type": "uint64", "sort_order": "ascending"},
            {'required': False, "name": "cargo_types",  "type": "any"},
        ],
        key=lambda column: column["name"]
    )
    assert_that(
        result_list,
        equal_to(expected_list),
        "Schema_ext is incorrect"
    )


def test_result_table_row_count_yt(result_yt_table):
    assert_that(
        len(result_yt_table.data),
        equal_to(3),
        "Incorrect yt table rows count"
    )


def test_big_sku_yt(result_yt_table, result_yt_ext_table):
    result_record = result_yt_table.data[0]

    fact_offer = market.proto.feedparser.deprecated.OffersData_pb2.Offer()
    fact_offer.ParseFromString(result_record["offer"])
    fact_genlog = json_format.MessageToDict(fact_offer.genlog, preserving_proto_field_name=True)

    fact_uc = market.proto.ir.UltraController_pb2.EnrichedOffer()
    fact_uc.ParseFromString(result_record["uc"])

    expected_offer = market.proto.feedparser.deprecated.OffersData_pb2.Offer(
        offer_flags=131072,
        URL="https://market.yandex.ru/product--sku-so-vsemi-dannymi/1713074440?sku=100000000001",
        autobroker_enabled=1,
        available=0,
        barcode="100500|0100500|00100500",
        classifier_good_id="0AD03F7131F42E8660263623149412B6",
        classifier_magic_id2="77A5C65B31FBA062B4AA3A0A14C644A9",
        datasource_name="market.fulfillment.test.yandex.ru",
        deliveryIncluded=0,
        description="Не содержит изотопов урана 235 и других радионуклидов",
        exclusive=True,
        feed_id=475690,
        hype_goods=True,
        is_fake_msku_offer=True,
        market_category_id=CATEG_ID,
        market_sku=100000000001,
        model="SKU со всеми данными",
        picURLS="https://avatars.mds.yandex.net/get-mpic/175985/img_id456/orig\thttps://avatars.mds.yandex.net/get-mpic/175985/img_id789/orig",
        price_expression="0 1 0 RUR RUR",
        price_from=False,
        price_scheme="10;9=5;",
        priority_regions="225",
        quality_rating=5,
        regionAttributes="225",
        ru_price=0.0,
        raw_vendor="Крутое название",
        shop_name="Тестовый магазин проекта Фулфиллмент",
        title="SKU со всеми данными",
        ware_md5="QQinNfCNchhMpLcy_YT0dQ",
        yml_date=fact_offer.genlog.yml_date,
        yx_bid=0,
        yx_cbid=0,
        yx_ds_id=431782,
        yx_shop_name="Тестовый магазин проекта Фулфиллмент",
        yx_shop_offer_id="MS100000000001",
        is_msku_published=True,
        mbo_model=ExportReportModel(
            parameter_values=[
                # Строковый параметр всегда попадает в данные для характеристик
                ParameterValue(
                    param_id=1000004,
                    str_value=[LocalizedString(
                        isoCode='ru',
                        value='Fiore'
                    )]
                ),
                ParameterValue(
                    param_id=1000005,
                    str_value=[LocalizedString(
                        isoCode='ru',
                        value='Не содержит изотопов урана 235 и других радионуклидов'
                    )]
                ),
                ParameterValue(
                    param_id=1000006,
                    str_value=[
                        LocalizedString(isoCode="ru", value="100500"),
                        LocalizedString(isoCode="ru", value="0100500"),
                        LocalizedString(isoCode="ru", value="00100500")
                    ]
                ),
                ParameterValue(
                    param_id=1001006,
                    str_value=[LocalizedString(
                        isoCode='ru',
                        value='Крутое название'
                    )]
                ),

                # Не поисковые параметры тоже попадают сюда
                ParameterValue(
                    param_id=1000100,
                    # Гумофул шаблонизатор NUMERIC_ENUM воспринимает как ENUM
                    option_id=129
                ),
                ParameterValue(
                    param_id=1000101,
                    numeric_value="126"
                ),
                ParameterValue(
                    param_id=1000102,
                    # Копируются все значения, что были заданы в MBO (хотя, для типа bool пока нужен только bool_value)
                    bool_value=False,
                    option_id=12976298
                ),
                ParameterValue(
                    param_id=1000103,
                    option_id=12109936
                ),
                ParameterValue(
                    param_id=1000104,
                    str_value=[LocalizedString(
                        isoCode='ru',
                        value='FioreModel'
                    )]
                ),

            ],
            parameter_value_hypothesis=[
                # Данные параметры будут добавлены к данным для генерации характеристик МСКУ
                ParameterValueHypothesis(
                    param_id=1000100,
                    value_type=MboParameters_pb2.NUMERIC_ENUM,
                    str_value=[Word(
                        name="1000100LHypo",
                    )],
                ),
                ParameterValueHypothesis(
                    param_id=1000101,
                    value_type=MboParameters_pb2.NUMERIC,
                    str_value=[Word(
                        name="1000101LHypo",
                    )],
                ),
                ParameterValueHypothesis(
                    param_id=1000102,
                    value_type=MboParameters_pb2.BOOLEAN,
                    str_value=[Word(
                        name="1000102LHypo",
                    )],
                ),
                ParameterValueHypothesis(
                    param_id=1000103,
                    value_type=MboParameters_pb2.ENUM,
                    str_value=[Word(
                        name="1000103LHypo",
                    )],
                ),
                ParameterValueHypothesis(
                    param_id=1000104,
                    value_type=MboParameters_pb2.STRING,
                    str_value=[Word(
                        name="1000104LHypo",
                    )],
                ),
            ],
            videos=[
                Video(url='www.youtube.com/watch?v=1'),
                Video(url='www.youtube.com/watch?v=2'),
            ]
        )
    )

    expected_genlog = json_format.MessageToDict(OfferData2GenlogProtobuf(expected_offer), preserving_proto_field_name=True)

    expected_uc = market.proto.ir.UltraController_pb2.EnrichedOffer(
        classifier_category_id=CATEG_ID,
        category_id=CATEG_ID,
        tovar_category_id=CATEG_ID,
        matched_category_id=CATEG_ID,
        matched_id=1713074440,
        model_id=1713074440,
        matched_vendor_id=966973,
        vendor_id=966973,
        cluster_id=-1,
        guru_category_id=0,
        matched_type_value=market.proto.ir.UltraController_pb2.EnrichedOffer.MATCH_OK,
        params=[
            FormalizedParamPosition(
                param_id=1000000,
                number_value=128.0,
            ),
            FormalizedParamPosition(
                param_id=1000001,
                number_value=127.0,
            ),
            FormalizedParamPosition(
                param_id=1000002,
                value_id=0,
            ),
            FormalizedParamPosition(
                param_id=1000003,
                value_id=12109936,
            ),
            FormalizedParamPosition(
                param_id=1000007,
                value_id=12109937,
            ),
            FormalizedParamPosition(
                param_id=1000008,
                value_id=1,
            ),
            FormalizedParamPosition(
                param_id=1000009,
                value_id=1,
            ),
            FormalizedParamPosition(
                param_id=1000010,
                value_id=0,
            ),
            FormalizedParamPosition(
                param_id=1000012,
                value_id=1,
            ),
            FormalizedParamPosition(
                param_id=1000013,
                value_id=1,
            ),
            FormalizedParamPosition(
                param_id=1000014,
                value_id=1,
            ),
            FormalizedParamPosition(
                param_id=1000015,
                number_value=24.0,
            ),
            # Карта переходов строиarcтся только по параметрам с базовых
            # Поэтому даже непоисковые параметры попадают на базовый поиск
            FormalizedParamPosition(
                param_id=1000100,
                number_value=129.0,
            ),
            FormalizedParamPosition(
                param_id=1000101,
                number_value=126.0,
            ),
            FormalizedParamPosition(
                param_id=1000102,
                value_id=0,
            ),
            FormalizedParamPosition(
                param_id=1000103,
                value_id=12109936,
            ),
        ],
    )

    compare_genlog_field(fact_genlog, expected_genlog)

    assert_that(
        fact_uc,
        equal_to(expected_uc),
        "uc protobuf of big sku"
    )
    assert_that(
        result_record["msku"],
        equal_to(100000000001),
        "big sku id"
    )
    assert_that(
        result_record["feed_id"],
        equal_to(BLUE_SKU_FEED),
        "big sku feed_id"
    )
    assert_that(
        result_record["offer_id"],
        equal_to("MS100000000001"),
        "big sku offer_id"
    )

    # check msku_ext table
    result_ext_record = result_yt_ext_table.data[0]
    assert_that(
        result_ext_record["msku"],
        equal_to(100000000001),
        "EXT big sku id"
    )
    assert_that(
        result_ext_record["cargo_types"],
        equal_to([100, 400]),
        "EXT cargo types"
    )


def test_min_sku_yt(result_yt_table):
    result_record = result_yt_table.data[1]

    fact_offer = market.proto.feedparser.deprecated.OffersData_pb2.Offer()
    fact_offer.ParseFromString(result_record["offer"])
    fact_genlog = json_format.MessageToDict(fact_offer.genlog, preserving_proto_field_name=True)

    fact_uc = market.proto.ir.UltraController_pb2.EnrichedOffer()
    fact_uc.ParseFromString(result_record["uc"])

    expected_offer = market.proto.feedparser.deprecated.OffersData_pb2.Offer(
        offer_flags=131072,
        URL="https://market.yandex.ru/product--sku-s-minimalno-neobkhodimymi-dannymi/1713074440?sku=100000000002",
        autobroker_enabled=1,
        available=0,
        classifier_good_id="4FED3E945741786E5457709139F934A0",
        classifier_magic_id2="33C150F1DD9B47374CB87EEC5DC6CA25",
        datasource_name="market.fulfillment.test.yandex.ru",
        deliveryIncluded=0,
        feed_id=475690,
        is_fake_msku_offer=True,
        market_category_id=CATEG_ID,
        market_sku=100000000002,
        model="SKU с минимально необходимыми данными",
        genlog=GenerationLog_pb2.Record(
            # model_title_ext="Модельный заголовок",
            # title="SKU с минимально необходимыми данными",
            # ware_md5="rpArWN0_GNiOp9iEnyjnYw",
            # offer_id="MS100000000002"
        ),
        price_expression="0 1 0 RUR RUR",
        price_from=False,
        price_scheme="10;9=5;",
        priority_regions="225",
        quality_rating=5,
        regionAttributes="225",
        ru_price=0.0,
        shop_name="Тестовый магазин проекта Фулфиллмент",
        title="SKU с минимально необходимыми данными",
        ware_md5="rpArWN0_GNiOp9iEnyjnYw",
        yml_date=fact_offer.genlog.yml_date,
        yx_bid=0,
        yx_cbid=0,
        yx_ds_id=431782,
        yx_shop_name="Тестовый магазин проекта Фулфиллмент",
        yx_shop_offer_id="MS100000000002",
        is_msku_published=True,
    )
    expected_genlog = json_format.MessageToDict(OfferData2GenlogProtobuf(expected_offer), preserving_proto_field_name=True)

    expected_uc = market.proto.ir.UltraController_pb2.EnrichedOffer(
        classifier_category_id=CATEG_ID,
        category_id=CATEG_ID,
        tovar_category_id=CATEG_ID,
        matched_category_id=CATEG_ID,
        matched_id=1713074440,
        model_id=1713074440,
        matched_vendor_id=966973,
        vendor_id=966973,
        cluster_id=-1,
        guru_category_id=0,
        matched_type_value=market.proto.ir.UltraController_pb2.EnrichedOffer.MATCH_OK,
    )

    compare_genlog_field(fact_genlog, expected_genlog)

    assert_that(
        fact_uc,
        equal_to(expected_uc),
        "uc protobuf of min sku"
    )
    assert_that(
        result_record["msku"],
        equal_to(100000000002),
        "min sku id"
    )
    assert_that(
        result_record["feed_id"],
        equal_to(BLUE_SKU_FEED),
        "min sku feed_id"
    )
    assert_that(
        result_record["offer_id"],
        equal_to("MS100000000002"),
        "min sku offer_id"
    )


def test_min_psku_yt(result_yt_table):
    result_record = result_yt_table.data[2]

    fact_offer = market.proto.feedparser.deprecated.OffersData_pb2.Offer()
    fact_offer.ParseFromString(result_record["offer"])

    fact_uc = market.proto.ir.UltraController_pb2.EnrichedOffer()
    fact_uc.ParseFromString(result_record["uc"])

    assert_that(
        result_record["msku"],
        equal_to(100000000004),
        "min psku id"
    )
    assert_that(
        result_record["offer_id"],
        equal_to("MS100000000004"),
        "min psku offer_id"
    )
    assert_that(
        fact_offer.genlog.is_psku,
        equal_to(True),
        "min psku is_psku"
    )


def test_lookup_yt(yt_server, result_yt_table):
    """
    Тест проверяет, что полученная таблица является подобием key-value storage и в ней можно искать по msku
    """

    yt_client = yt_server.get_yt_client()

    expected_msku = [100000000001, 100000000002, 100000000004]
    actual_msku = []

    path = result_yt_table.get_path()

    rows = yt_client.lookup_rows(path, [
        {'msku': 100000000001},
        {'msku': 100000000002},
        {'msku': 100000000003},  # shouldn't be in the result table
        {'msku': 100000000004},
    ])
    for row in rows:
        actual_msku.append(row['msku'])

    assert_that(
        sorted(actual_msku),
        equal_to(expected_msku),
        'Some msku not found'
    )
