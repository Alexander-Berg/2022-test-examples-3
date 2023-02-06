# coding: utf-8

"""
Тест проверяет, что если в данных МБО от моделях приходят данные о том,
что эта конкретная модель учасвтует в эксперменте, то

* выставляются правильные поисковые литералы (test_indexer)
* у экспериментальных моделей группировочные атрибуты hyper и hyper_ts заменяются
  на соотв. атрибуты базовых моделей
* из выгрузок выгружаются правильные данные (test_extractor)


https://wiki.yandex-team.ru/users/yuraaka/Contex/#logikarabotyindeksatora
https://wiki.yandex-team.ru/Market/Sluzhba-rarabotki-kontenta/Kontur-Kontent/Jeksperimenty-o-vlijanii-pokazatelejj-kachestva-kontenta-na-polzovatelskie-metriki
"""


import pytest
from hamcrest import assert_that, has_key, equal_to

from market.idx.models.yatf.test_envs.mbo_info_extractor import MboInfoExtractorTestEnv
from market.idx.yatf.matchers.env_matchers import TextFile
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

import market.proto.content.mbo.ExportReportModel_pb2 as ExportReportModel_pb2
import market.proto.content.mbo.MboParameters_pb2 as MboParameters_pb2

from market.proto.content.mbo.ExportReportModel_pb2 import (
    EXPERIMENTAL_BASE_MODEL,
    EXPERIMENTAL_MODEL,
    ExportReportModel,
    LocalizedString,
    ParameterValue,
    ParameterValueHypothesis,
    Picture,
    Relation,
    UngroupingInfo,
    Video,
)
from market.proto.content.mbo.MboParameters_pb2 import (
    Category,
    Parameter,
    Word,
)
from msku_uploader.yatf.utils import (
    make_sku_protobuf,
)
from msku_uploader.yatf.resources.mbo_yt import (
    MboAllModelsTable,
    MboModelsTable,
    MboParamsTable,
    MboSkuTable,
)


MODEL_ID = 1

MODEL_ID_EXP = 10
MODEL_ID_BASE = 11

MODEL_ID_BASE_GROUP = 100
MODEL_ID_BASE_MODIFICATION = 101

MODEL_ID_EXP_GROUP = 200
MODEL_ID_EXP_MODIFICATION = 201

PARTNER_MODEL_ID = 300

CATEGORY_ID = 90592


@pytest.fixture(scope="function")
def category_parameters():
    return Category(
        hid=CATEGORY_ID,
        parameter=[
            Parameter(
                id=15060326,
                xsl_name='licensor',
                common_filter_index=1,
                value_type='ENUM'
            ),
            Parameter(
                id=14020987,
                xsl_name='hero_global',
                published=True,
                common_filter_index=1,
                value_type='ENUM'
            ),
            Parameter(
                id=15086295,
                xsl_name='pers_model',
                published=True,
                common_filter_index=1,
                value_type='ENUM'
            )
        ]
    )


@pytest.fixture(scope="function")
def models(category_parameters):
    models = []

    parameters = [
        ParameterValue(
            param_id=15060326,
            xsl_name='hero_global',
            option_id=111,
        ),
        ParameterValue(
            param_id=14020987,
            xsl_name='hero_global',
            option_id=0,
        ),
        ParameterValue(
            param_id=15086295,
            xsl_name='pers_model')
    ]

    model1 = ExportReportModel(
        id=MODEL_ID,
        category_id=CATEGORY_ID,
        vendor_id=CATEGORY_ID,
        current_type='GURU',
        published_on_market=True,
        parameter_values=parameters,
        blue_ungrouping_info=[
            UngroupingInfo(
                title="Group1",
                parameter_values=[
                    ParameterValue(
                        param_id=2,
                        option_id=21,
                    ),
                    ParameterValue(
                        param_id=1,
                        numeric_value="0.100",
                    ),
                    ParameterValue(
                        param_id=3,
                        bool_value=False,
                    ),
                ]
            ),
            UngroupingInfo(
                title="Group2",
                parameter_values=[
                    ParameterValue(
                        param_id=1,
                        numeric_value="31.456",
                    ),
                    ParameterValue(
                        param_id=2,
                        option_id=22,
                    ),
                    ParameterValue(
                        param_id=3,
                        bool_value=True,
                    ),
                ]
            ),
        ]
    )
    models.append(model1)

    model10 = ExportReportModel(
        id=MODEL_ID_EXP,
        category_id=CATEGORY_ID,
        vendor_id=CATEGORY_ID,
        current_type='EXPERIMENTAL',
        published_on_market=True,
        experiment_flag="contex_1",
        parameter_values=parameters,
        relations=[
            Relation(id=MODEL_ID_BASE, type=EXPERIMENTAL_BASE_MODEL),
        ]
    )
    models.append(model10)

    model11 = ExportReportModel(
        id=MODEL_ID_BASE,
        category_id=CATEGORY_ID,
        vendor_id=CATEGORY_ID,
        current_type='GURU',
        published_on_market=True,
        parameter_values=parameters,
        relations=[
            Relation(id=MODEL_ID_EXP, type=EXPERIMENTAL_MODEL),
        ]
    )
    models.append(model11)

    model_base_group = ExportReportModel(
        id=MODEL_ID_BASE_GROUP,
        category_id=CATEGORY_ID,
        vendor_id=CATEGORY_ID,
        current_type='GURU',
        published_on_market=True,
        parameter_values=parameters,
        relations=[
            Relation(id=MODEL_ID_EXP_GROUP, type=EXPERIMENTAL_MODEL),
        ]
    )
    models.append(model_base_group)

    model_base_modification = ExportReportModel(
        id=MODEL_ID_BASE_MODIFICATION,
        category_id=CATEGORY_ID,
        vendor_id=CATEGORY_ID,
        current_type='GURU',
        published_on_market=True,
        parameter_values=parameters,
        relations=[
            Relation(id=MODEL_ID_EXP_MODIFICATION, type=EXPERIMENTAL_MODEL),
        ]
    )
    models.append(model_base_modification)

    model_exp_group = ExportReportModel(
        id=MODEL_ID_EXP_GROUP,
        category_id=CATEGORY_ID,
        vendor_id=CATEGORY_ID,
        current_type='GURU',
        experiment_flag="contex_1",
        published_on_market=True,
        parameter_values=parameters,
        relations=[
            Relation(id=MODEL_ID_BASE_GROUP, type=EXPERIMENTAL_BASE_MODEL),
        ]
    )
    models.append(model_exp_group)

    model_exp_modification = ExportReportModel(
        id=MODEL_ID_EXP_MODIFICATION,
        category_id=CATEGORY_ID,
        parent_id=MODEL_ID_EXP_GROUP,
        vendor_id=CATEGORY_ID,
        current_type='GURU',
        experiment_flag="contex_1",
        published_on_market=True,
        parameter_values=parameters,
        relations=[
            Relation(id=MODEL_ID_BASE_MODIFICATION, type=EXPERIMENTAL_BASE_MODEL),
        ]
    )
    models.append(model_exp_modification)

    return models


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


@pytest.fixture(scope="function")
def skus():
    data = {
        'big_sku': make_big_sku(
            skuid=100000000001,
            title='SKU со всеми данными',
            model_id=MODEL_ID,
            category_id=CATEGORY_ID
        ),
        'min_sku': make_sku_protobuf(
            skuid=100000000002,
            title='SKU с минимально необходимыми данными',
            model_id=MODEL_ID,
            category_id=CATEGORY_ID
        ),
        'nonpub_sku': make_sku_protobuf(
            skuid=100000000003,
            title='Неопубликованный SKU',
            model_id=MODEL_ID,
            category_id=CATEGORY_ID
        ),
        'bad_sku': ExportReportModel(category_id=111),
        'notitle_sku': make_big_sku(
            skuid=100000000001,
            title='',
            model_id=MODEL_ID,
            category_id=CATEGORY_ID
        ),
        'partner_sku': make_sku_protobuf(
            skuid=100000000004,
            title='PARTNER_SKU с минимально необходимыми данными',
            model_id=PARTNER_MODEL_ID,
            category_id=CATEGORY_ID,
            is_partner_sku=True
        ),
        'contex_sku_original': make_sku_protobuf(
            skuid=100000000002,
            title='Оригинальный MSKU 100000000002',
            model_id=MODEL_ID,
            category_id=CATEGORY_ID,
            relations=[
                Relation(
                    id=MODEL_ID,
                    category_id=CATEGORY_ID,
                    type=ExportReportModel_pb2.SKU_PARENT_MODEL
                ),
                Relation(
                    id=100000000003,
                    category_id=CATEGORY_ID,
                    type=ExportReportModel_pb2.EXPERIMENTAL_MODEL,
                ),
            ]
        ),
        'contex_sku_experiment': make_sku_protobuf(
            skuid=100000000003,
            title='Экспериментальная MSKU для 100000000002',
            model_id=MODEL_ID_EXP,
            category_id=CATEGORY_ID,
            experiment_flag='some_test_id',
            relations=[
                Relation(
                    id=MODEL_ID,
                    category_id=CATEGORY_ID,
                    type=ExportReportModel_pb2.SKU_PARENT_MODEL
                ),
                Relation(
                    id=100000000002,
                    category_id=CATEGORY_ID,
                    type=ExportReportModel_pb2.EXPERIMENTAL_BASE_MODEL,
                ),
            ]
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
        data['contex_sku_original'],
        data['contex_sku_experiment']
    ]


@pytest.fixture(scope="function")
def yt_dir():
    return get_yt_prefix()


@pytest.fixture(scope="function")
def mbo_sku_table(yt_server, yt_dir, skus):
    return MboSkuTable(
        yt_server,
        yt_dir,
        data=[
            {
                'category_id': CATEGORY_ID,
                'data': mbo_sku.SerializeToString(),
            }
            for mbo_sku in skus
        ]
    )


@pytest.fixture(scope="function")
def mbo_models_table(yt_server, yt_dir, models):
    return MboModelsTable(
        yt_server,
        yt_dir,
        data=[
            {
                'category_id': CATEGORY_ID,
                'data': mbo_model.SerializeToString(),
            }
            for mbo_model in models
        ]
    )


@pytest.fixture(scope="function")
def mbo_all_models_table(yt_server, yt_dir, skus, models):
    return MboAllModelsTable(
        yt_server,
        yt_dir,
        data=[
            {
                'data': mbo_model.SerializeToString(),
            }
            for mbo_model in
            skus + models
        ]
    )


@pytest.fixture(scope="function")
def mbo_params_table(yt_server, yt_dir, category_parameters):
    return MboParamsTable(
        yt_server,
        yt_dir,
        data=[
            {
                'hid': CATEGORY_ID,
                'data': mbo_category.SerializeToString()
            }
            for mbo_category in [category_parameters]
        ]
    )


@pytest.fixture(scope="function")
def workflow(
    yt_server,
    yt_dir,
    mbo_all_models_table,
    mbo_models_table,
    mbo_params_table,
    mbo_sku_table,
):
    resources = {
        'mbo_all_models_table': mbo_all_models_table,
        'mbo_models_table': mbo_models_table,
        'mbo_params_table': mbo_params_table,
        'mbo_sku_table': mbo_sku_table,
    }

    with MboInfoExtractorTestEnv(yt_input_dir=yt_dir, **resources) as env:
        env.execute(yt_server)
        env.verify()
        yield env


def test_contex_experiments(workflow):
    assert_that(
        workflow,
        TextFile("contex_experiments.txt.gz")
            .has_line_re(r"contex_1\t{}\t{}\t\d+".format(
                MODEL_ID_BASE,
                MODEL_ID_EXP
            ))
            .has_line_re(r"contex_1\t{}\t{}\t\d+".format(
                MODEL_ID_BASE_GROUP,
                MODEL_ID_EXP_GROUP
            ))
    )


def test_contex_relations(workflow):
    assert_that(
        workflow.outputs,
        has_key('contex_relations_pbsn')
    )
    actual_ids = sorted([
        proto.id
        for proto in workflow.outputs['contex_relations_pbsn'].proto_results
    ])
    expected_ids = sorted([
        10,
        100,
        100000000001,
        100000000002,
        100000000003,
        101,
        11,
        200,
        201,
    ])
    assert_that(
        actual_ids,
        equal_to(expected_ids)
    )


def test_model_group_for_beru_msku_card(workflow):
    assert_that(
        workflow,
        TextFile("model_group_for_beru_msku_card.csv")
            .has_line_re(r"{}\t{}".format(
                MODEL_ID_EXP_MODIFICATION,
                MODEL_ID_EXP_GROUP
            ))
    )


def test_ungrouping_params_extractor(workflow):
    """
    Проверяем выгрузку параметров расхлопывания для моделей
    Проверяется, что параметры выстроены в порядке возрастания идентификаторов, не смотря на порядок в исходных данных
    """
    assert_that(
        workflow,
        TextFile("ungrouping_model_params.gz")
            .has_line("{model_id}\tb\t{param1_id}\t{param2_id}\t{param3_id}".format(
                model_id=MODEL_ID,
                param1_id=1,
                param2_id=2,
                param3_id=3
            ))
    )


def test_ungrouping_values_extractor(workflow):
    """
    Проверяем выгрузку значений расхлопывания для моделей
    """
    assert_that(
        workflow,
        TextFile("ungrouping_models.gz")
            .has_line("{model_id}\tb\t{values_key}\t{title}\t{group_id}".format(
                model_id=MODEL_ID,
                values_key="0.1_21_0",
                title="Group1",
                group_id=1025
            ))
            .has_line("{model_id}\tb\t{values_key}\t{title}\t{group_id}".format(
                model_id=MODEL_ID,
                values_key="31.456_22_1",
                title="Group2",
                group_id=1026
            ))
    )


def test_model_group_for_beru_msku_card_csv(workflow):
    """
    модель не из категории с групповыми моделями попала в
    model_group_for_beru_msku_card.csv
    """
    assert_that(
        workflow,
        TextFile("model_group_for_beru_msku_card.csv")
            .has_line("{model_id}\t{parent_id}".format(
                model_id=MODEL_ID_EXP_MODIFICATION,
                parent_id=MODEL_ID_EXP_GROUP
            ))
    )


def test_model_group_csv(workflow):
    """
    модель не из категории с групповыми моделями НЕ попала в model_group.csv
    """
    assert_that(
        workflow,
        TextFile("model_group.csv")
            .has_no_line("{model_id}\t{parent_id}".format(
                model_id=MODEL_ID_EXP_MODIFICATION,
                parent_id=MODEL_ID_EXP_GROUP
            ))
    )
