# coding: utf-8

import pytest
from hamcrest import all_of, assert_that, is_not

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.proto.content.mbo.ExportReportModel_pb2 import ExportReportModel
from market.proto.content.mbo.MboParameters_pb2 import Category


BOOK_MODEL_ID = 2441135
OTHER_MODEL_ID = 2441136


@pytest.fixture(scope="module")
def categories():
    return [
        Category(hid=90592),
        Category(hid=90593, process_as_book_category=True),
    ]


def make_model(model_id, category):
    return ExportReportModel(
        id=model_id,
        category_id=category.hid,
        vendor_id=152712,
        current_type='GURU',
        published_on_market=True,
    )


@pytest.fixture(scope="module")
def category_models(categories):
    return [
        [
            make_model(OTHER_MODEL_ID, categories[0]),
        ],
        [
            make_model(BOOK_MODEL_ID, categories[1]),
        ],
    ]


@pytest.yield_fixture(scope="module")
def workflow(category_models, categories):
    resources = {
        'models': [
            ModelsPb(models, category.hid)
            for models, category in zip(category_models, categories)
        ],
        'parameters': list(map(ParametersPb, categories)),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_book_category(workflow):
    """Проверяет что модели книжных категорий отфильтровываются
    и не попадают в индекс.
    """
    assert_that(
        workflow,
        all_of(
            is_not(HasDocs().attributes(hyper=str(BOOK_MODEL_ID))),
            HasDocs().attributes(hyper=str(OTHER_MODEL_ID))
                     .attributes(hyper_ts=str(OTHER_MODEL_ID))
                     .attributes(hyper_ts_blue=str(OTHER_MODEL_ID)),
        ),
        'Книжные категории не должны входить в индекс',
    )
