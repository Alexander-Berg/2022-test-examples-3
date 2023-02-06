# coding: utf-8

import pytest
from hamcrest import assert_that

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.proto.content.mbo.MboParameters_pb2 import Category
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.proto.content.mbo.ExportReportModel_pb2 import ExportReportModel, LocalizedString, ParameterValue
from market.idx.yatf.resources.tovar_tree_pb import MboCategory, TovarTreePb

from pictures import Pictures


@pytest.fixture(scope="module")
def tovar_tree():
    return [
        MboCategory(
            hid=1, tovar_id=0,
            unique_name="Все товары", name="Все товары",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=500, tovar_id=2, parent_hid=1,
            unique_name="Товары по ведьмаку", name="Товары по ведьмаку",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=501, tovar_id=3, parent_hid=500,
            unique_name="Ведьмаки и Чародейки", name="Ведьмаки и Чародейки",
            output_type=MboCategory.GURULIGHT),
        MboCategory(
            hid=502, tovar_id=4, parent_hid=500,
            unique_name="Нильфгаард", name="Нильфгаард",
            output_type=MboCategory.GURULIGHT),
    ]


def pictures2():
    return Pictures(2, 2)


def gen_name_param(name):
    return ParameterValue(xsl_name="name", str_value=[LocalizedString(isoCode="ru", value=name)])


@pytest.fixture(scope="module")
def categ_to_models():
    return {
        502: [
            ExportReportModel(id=3220,
                              category_id=502,
                              vendor_id=502,
                              current_type='GURU',
                              published_on_market=True,
                              pictures=pictures2().proto_pictures(),
                              title_without_vendor=[LocalizedString(isoCode="ru", value="502_0")],
                              parameter_values=[gen_name_param("502_0")]),
        ],
        501: [
            ExportReportModel(id=3223,
                              category_id=501,
                              vendor_id=501,
                              current_type='GURU',
                              published_on_market=True,
                              pictures=pictures2().proto_pictures(),
                              title_without_vendor=[LocalizedString(isoCode="ru", value="501_0")],
                              parameter_values=[gen_name_param("501_0")]),
        ]
    }


@pytest.fixture(scope="module")
def category_parameters():
    return [Category(hid=hid) for hid in [1, 500, 501, 502]]


@pytest.fixture(scope="module")
def workflow_indexer(categ_to_models, category_parameters, tovar_tree):
    resources = {
        'models': [
            ModelsPb(categ_to_models[hid], hid)
            for hid in categ_to_models
        ],
        'parameters': [
            ParametersPb(category)
            for category in category_parameters
        ],
        'tovar_categories': TovarTreePb(tovar_tree)
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_hyper_categ_id_literals(workflow_indexer):
    def check_model_hyper_categ_id_literals(model_id, hid, literals):
        assert_that(
            workflow_indexer,
            HasDocs()
            .attributes(hyper=str(model_id))
            .literals(hyper_categ_id=literals))

    check_model_hyper_categ_id_literals(3220, 502, [502, 500, 1])
    check_model_hyper_categ_id_literals(3223, 501, [501, 500, 1])
