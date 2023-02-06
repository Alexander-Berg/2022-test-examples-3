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
from hamcrest import assert_that, all_of, has_key

from market.idx.yatf.matchers.env_matchers import HasDocs, TextFile
from market.idx.models.yatf.resources.models_indexer.barcodes import Barcodes
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.resources.models_indexer.cluster_pictures import ClusterPicturesMmap
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.idx.models.yatf.test_envs.mbo_info_extractor import MboInfoExtractorTestEnv
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    UngroupingInfo,
    Relation,
    ParameterValue,
    EXPERIMENTAL_BASE_MODEL,
    EXPERIMENTAL_MODEL,
)
from market.proto.content.mbo.MboParameters_pb2 import Category, Parameter

MODEL_ID = 1

MODEL_ID_EXP = 10
MODEL_ID_BASE = 11

MODEL_ID_BASE_GROUP = 100
MODEL_ID_BASE_MODIFICATION = 101

MODEL_ID_EXP_GROUP = 200
MODEL_ID_EXP_MODIFICATION = 201

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

    model1 = ExportReportModel(id=MODEL_ID,
                               category_id=category_parameters.hid,
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
                               ])
    models.append(model1)

    model10 = ExportReportModel(id=MODEL_ID_EXP,
                                category_id=category_parameters.hid,
                                vendor_id=CATEGORY_ID,
                                current_type='EXPERIMENTAL',
                                published_on_market=True,
                                experiment_flag="contex_1",
                                parameter_values=parameters)
    model10.relations.extend([Relation(id=MODEL_ID_BASE, type=EXPERIMENTAL_BASE_MODEL)])
    models.append(model10)

    model11 = ExportReportModel(id=MODEL_ID_BASE,
                                category_id=category_parameters.hid,
                                vendor_id=CATEGORY_ID,
                                current_type='GURU',
                                published_on_market=True,
                                parameter_values=parameters)
    model11.relations.extend([Relation(id=MODEL_ID_EXP, type=EXPERIMENTAL_MODEL)])
    models.append(model11)

    model_base_group = ExportReportModel(id=MODEL_ID_BASE_GROUP,
                                         category_id=category_parameters.hid,
                                         vendor_id=CATEGORY_ID,
                                         current_type='GURU',
                                         published_on_market=True,
                                         parameter_values=parameters)
    model_base_group.relations.extend([Relation(id=MODEL_ID_EXP_GROUP, type=EXPERIMENTAL_MODEL)])
    models.append(model_base_group)

    model_base_modification = ExportReportModel(id=MODEL_ID_BASE_MODIFICATION,
                                                category_id=category_parameters.hid,
                                                vendor_id=CATEGORY_ID,
                                                current_type='GURU',
                                                published_on_market=True,
                                                parameter_values=parameters)
    model_base_modification.relations.extend([Relation(id=MODEL_ID_EXP_MODIFICATION,
                                                        type=EXPERIMENTAL_MODEL)])
    models.append(model_base_modification)

    model_exp_group = ExportReportModel(id=MODEL_ID_EXP_GROUP,
                                        category_id=category_parameters.hid,
                                        vendor_id=CATEGORY_ID,
                                        current_type='GURU',
                                        experiment_flag="contex_1",
                                        published_on_market=True,
                                        parameter_values=parameters)
    model_exp_group.relations.extend([Relation(id=MODEL_ID_BASE_GROUP, type=EXPERIMENTAL_BASE_MODEL)])
    models.append(model_exp_group)

    model_exp_modification = ExportReportModel(id=MODEL_ID_EXP_MODIFICATION,
                                               category_id=category_parameters.hid,
                                               parent_id=MODEL_ID_EXP_GROUP,
                                               vendor_id=CATEGORY_ID,
                                               current_type='GURU',
                                               experiment_flag="contex_1",
                                               published_on_market=True,
                                               parameter_values=parameters)
    model_exp_modification.relations.extend([Relation(id=MODEL_ID_BASE_MODIFICATION,
                                                      type=EXPERIMENTAL_BASE_MODEL)])
    models.append(model_exp_modification)

    return models


@pytest.fixture(scope="function")
def barcodes():
    # баркоды только для базовых моделей

    b = Barcodes()
    b.add_model("0001", MODEL_ID)
    b.add_model("0011", MODEL_ID_BASE)
    b.add_group_model("0101", MODEL_ID_BASE_GROUP, MODEL_ID_BASE_MODIFICATION)
    return b


@pytest.fixture(scope="function")
def resources(models, category_parameters, barcodes):
    return {
        'barcodes': barcodes,
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters),
        'cluster_pictures_mmap': ClusterPicturesMmap([]),
    }


@pytest.fixture(scope="function")
def workflow_indexer(workflow_extractor, resources):
    resources['contex_experiments'] = workflow_extractor.outputs['contex_experiments']
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.fixture(scope="function")
def workflow_extractor(resources):
    with MboInfoExtractorTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def extract_models_randx(offers):
    models_randx = {}
    for offer in list(offers.values()):
        model_id = int(offer.get('contex_exp_model_id') or offer.get("model_id") or offer.get("hyper"))
        randx = offer["randx"]

        models_randx[model_id] = int(randx)

    return models_randx


def test_indexer(workflow_indexer):
    # Check we have gl_filters for all models
    gl_models = workflow_indexer.outputs['gl_models_mmap_data'][1]

    assert_that(gl_models, all_of(
        has_key(str(MODEL_ID)),
        has_key(str(MODEL_ID_EXP)),
        has_key(str(MODEL_ID_BASE)),
    ))

    # Check we have same randx for base and experimental models
    models_randx = extract_models_randx(workflow_indexer.offers)
    assert models_randx[MODEL_ID_BASE] == models_randx[MODEL_ID_EXP]

    # Check docs' attributes and literals
    assert_that(workflow_indexer, all_of(
        # Модели без эксперимента
        HasDocs().attributes(hyper=str(MODEL_ID))
                 .attributes(hyper_ts=str(MODEL_ID))
                 .attributes(url="market.yandex.ru/product/1")
                 .literals(contex="green", anti_contex=None)
                 .literals(barcode="0001")
                 .literals(hyper_model_id=str(MODEL_ID)),

        # Базовые модели
        HasDocs().attributes(hyper=str(MODEL_ID_BASE))
                 .attributes(hyper_ts=str(MODEL_ID_BASE))
                 .attributes(url="market.yandex.ru/product/11")
                 .literals(contex="classic", anti_contex="contex_1")
                 .literals(barcode="0011")
                 .literals(hyper_model_id=str(MODEL_ID_BASE)),

        # Экспериментальные модели
        HasDocs().attributes(hyper=str(MODEL_ID_BASE))
                 .attributes(hyper_ts=str(MODEL_ID_BASE))
                 .attributes(contex_base_model_id=str(MODEL_ID_BASE))
                 .attributes(contex_exp_model_id=str(MODEL_ID_EXP))
                 .attributes(contex_exp_id="contex_1")
                 .attributes(url="market.yandex.ru/product/11")
                 .literals(contex="contex_1", anti_contex=None)
                 .literals(barcode="0011")
                 .literals(hyper_model_id=str(MODEL_ID_BASE)),

        # Групповые модели
        # Базовые
        HasDocs().attributes(hyper=str(MODEL_ID_BASE_GROUP))
                 .attributes(hyper_ts=str(MODEL_ID_BASE_GROUP))
                 .attributes(url="market.yandex.ru/product/100")
                 .literals(contex="classic", anti_contex="contex_1")
                 .literals(hyper_model_id=str(MODEL_ID_BASE_GROUP)),

        HasDocs().attributes(hyper=str(MODEL_ID_BASE_MODIFICATION))
                 .attributes(hyper_ts=str(MODEL_ID_BASE_MODIFICATION))
                 .attributes(url="market.yandex.ru/product/101")
                 .literals(contex="contex_1", anti_contex=None)
                 .literals(barcode="0101")
                 .literals(hyper_model_id=str(MODEL_ID_BASE_MODIFICATION)),

        # Экспериментальные
        HasDocs().attributes(hyper=str(MODEL_ID_BASE_GROUP))
                 .attributes(hyper_ts=str(MODEL_ID_BASE_GROUP))
                 .attributes(contex_base_model_id=str(MODEL_ID_BASE_GROUP))
                 .attributes(contex_exp_model_id=str(MODEL_ID_EXP_GROUP))
                 .attributes(contex_exp_id="contex_1")
                 .attributes(url="market.yandex.ru/product/100")
                 .literals(contex="contex_1", anti_contex=None)
                 .literals(hyper_model_id=str(MODEL_ID_BASE_GROUP)),

        HasDocs().attributes(hyper=str(MODEL_ID_BASE_MODIFICATION))
                 .attributes(hyper_ts=str(MODEL_ID_BASE_MODIFICATION))
                 .attributes(contex_base_model_id=str(MODEL_ID_BASE_MODIFICATION))
                 .attributes(contex_exp_model_id=str(MODEL_ID_EXP_MODIFICATION))
                 .attributes(contex_exp_id="contex_1")
                 .attributes(url="market.yandex.ru/product/101")
                 .literals(contex="contex_1", anti_contex=None)
                 .literals(barcode="0101")
                 .literals(hyper_model_id=str(MODEL_ID_BASE_MODIFICATION)),
    ))


def test_extractor(workflow_extractor):
    assert_that(
        workflow_extractor,
        TextFile("contex_experiments.txt.gz")
            .has_line_re(r"contex_1\t{}\t{}\t\d+".format(MODEL_ID_BASE, MODEL_ID_EXP))
    )


def test_ungrouping_params_extractor(workflow_extractor):
    """
    Проверяем выгрузку параметров расхлопывания для моделей
    Проверяется, что параметры выстроены в порядке возрастания идентификаторов, не смотря на порядок в исходных данных
    """
    assert_that(workflow_extractor,
                TextFile("ungrouping_model_params.gz").has_line("{model_id}\tb\t{param1_id}\t{param2_id}\t{param3_id}".format(model_id=MODEL_ID, param1_id=1, param2_id=2, param3_id=3)))


def test_ungrouping_values_extractor(workflow_extractor):
    """
    Проверяем выгрузку значений расхлопывания для моделей
    """
    assert_that(workflow_extractor,
                TextFile("ungrouping_models.gz").has_line("{model_id}\tb\t{values_key}\t{title}\t{group_id}".format(model_id=MODEL_ID, values_key="0.1_21_0", title="Group1", group_id=1025))
                                                .has_line("{model_id}\tb\t{values_key}\t{title}\t{group_id}".format(model_id=MODEL_ID, values_key="31.456_22_1", title="Group2", group_id=1026)))


def test_model_group_for_beru_msku_card_csv(workflow_extractor):
    """
    модель не из категории с групповыми моделями попала в model_group_for_beru_msku_card.csv
    """
    assert_that(workflow_extractor,
                TextFile("model_group_for_beru_msku_card.csv").has_line("{model_id}\t{parent_id}".format(model_id=MODEL_ID_EXP_MODIFICATION, parent_id=MODEL_ID_EXP_GROUP)))


def test_model_group_csv(workflow_extractor):
    """
    модель не из категории с групповыми моделями НЕ попала в model_group.csv
    """
    assert_that(workflow_extractor,
                TextFile("model_group.csv").has_no_line("{model_id}\t{parent_id}".format(model_id=MODEL_ID_EXP_MODIFICATION, parent_id=MODEL_ID_EXP_GROUP)))
