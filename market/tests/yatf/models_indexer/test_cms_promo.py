# coding: utf-8
import pytest
from hamcrest import assert_that, all_of

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.proto.content.mbo.ExportReportModel_pb2 import ExportReportModel
from market.proto.content.mbo.MboParameters_pb2 import Category
from market.idx.yatf.resources.mbo.cms_promo import CmsPromoPbsn, create_cms_model_promo


MODEL_1_ID = 2441135
MODEL_2_ID = 2441136
PROMO_ID = 'promo1'


@pytest.fixture(scope="module")
def category_parameters():
    return Category(
        hid=90592
    )


@pytest.fixture(scope="module")
def models(category_parameters):
    return [
        ExportReportModel(
            id=MODEL_1_ID,
            category_id=category_parameters.hid,
            vendor_id=152712,
            current_type='GURU',
            published_on_market=True,
        ),
        ExportReportModel(
            id=MODEL_2_ID,
            category_id=category_parameters.hid,
            vendor_id=152712,
            current_type='GURU',
            published_on_market=True,
        ),
    ]


@pytest.yield_fixture(scope="module")
def workflow(models, category_parameters):
    resources = {
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters),
        'cms_promo': CmsPromoPbsn([
            create_cms_model_promo(PROMO_ID, available_models=[MODEL_1_ID]),
        ]),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_cms_promo(workflow):
    """Test that model with cms promo has search literal
    """
    assert_that(workflow, all_of(
        HasDocs().attributes(hyper=str(MODEL_1_ID)).literals(cms_promo=PROMO_ID),
        HasDocs().attributes(hyper=str(MODEL_2_ID)).literals(cms_promo=None),
        HasDocs().literals(cms_promo='unused_promo')
                 .count(0),
    ))
