# coding: utf-8

import pytest
from hamcrest import assert_that, is_not

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.models.yatf.resources.models_indexer.blue_offer_models import BlueOfferModels
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.proto.content.mbo.ExportReportModel_pb2 import ExportReportModel
from market.proto.content.mbo.MboParameters_pb2 import Category


@pytest.fixture(scope="module")
def category_parameters():
    return Category(
        hid=90592
    )


@pytest.fixture(scope="module")
def models(category_parameters):
    return [
        ExportReportModel(
            id=140891,
            category_id=category_parameters.hid,
            vendor_id=152712,
            current_type='GURU',
            published_on_market=True,
            published_on_blue_market=True,
        ),
        ExportReportModel(
            id=140892,
            category_id=category_parameters.hid,
            vendor_id=152712,
            current_type='GURU',
            published_on_market=True,
            published_on_blue_market=True,
        ),
    ]


@pytest.fixture(scope="module")
def blue_offer_models():
    b = BlueOfferModels()
    b.add_model(140891)
    return b


@pytest.yield_fixture(scope="module")
def workflow(models, category_parameters, blue_offer_models):
    resources = {
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters),
        'blue_offer_models': blue_offer_models
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_blue_offer_model_group_attribute(workflow):
    """Проверяем проставление группировочного атрибута моделям из merged_blue_offer_models.txt
    """

    assert_that(
        workflow,
        HasDocs().attributes(hyper=str(140891), has_blue_offers='1'),
        'Моделям, упомянутым в merged_blue_offer_models.txt, должен проставляться '
        'группировочный атрибут has_blue_offers'
    )

    assert_that(
        workflow,
        is_not(HasDocs().attributes(hyper=str(140892), has_blue_offers='1')),
        'Моделям, не упомянутым в merged_blue_offer_models.txt, группировочный '
        'атрибут has_blue_offers проставляться не должен'
    )
