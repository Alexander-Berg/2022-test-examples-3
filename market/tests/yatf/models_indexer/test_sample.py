# coding: utf-8
import pytest
from hamcrest import all_of, assert_that

from market.idx.yatf.matchers.env_matchers import (
    AllDocsHasValue,
    HasDocs,
    HasDocsWithValues,
)
from market.idx.models.yatf.resources.models_indexer.barcodes import Barcodes
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue,
    LocalizedString,
)
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
            id=2441135,
            category_id=category_parameters.hid,
            vendor_id=152712,
            current_type='GURU',
            published_on_market=True,
            parameter_values=[
                ParameterValue(
                    xsl_name='BarCode',
                    str_value=[
                        LocalizedString(
                            isoCode='ru',
                            value='asdf',
                        ),
                        LocalizedString(
                            isoCode='ru',
                            value='qwerty',
                        ),
                    ],
                )
            ],
        ),
    ]


@pytest.fixture(scope="module")
def barcodes():
    b = Barcodes()
    b.add_model(111, 2441135)
    b.add_model(111, 2441136)
    return b


@pytest.yield_fixture(scope="module")
def workflow(models, category_parameters, barcodes):
    resources = {
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters),
        'barcodes': barcodes
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_urls(models, workflow):
    expected_urls = ['market.yandex.ru/product/' + str(model.id)
                     for model in models]
    assert_that(workflow, HasDocsWithValues('url', expected_urls),
                'Url должны совпадать')


def test_vendor_ids(models, workflow):
    expected_vendor_ids = [str(model.vendor_id) for model in models]
    assert_that(workflow, HasDocsWithValues('vendor_id', expected_vendor_ids),
                'Vendor id должны совпадать')


def test_category_id(category_parameters, workflow):
    expected_hhid = str(category_parameters.hid)
    assert_that(workflow, AllDocsHasValue('hidd', expected_hhid),
                'Vendor id должны совпадать')


def test_barcodes(workflow):
    """Tests that barcodes from both the model and the barcode storages
    are propagated into the index
    """
    assert_that(
        workflow,
        all_of(
            HasDocs().attributes(hyper=str(2441135)).literals(barcode='111'),
            HasDocs().attributes(hyper=str(2441135)).literals(barcode='asdf'),
            HasDocs().attributes(hyper=str(2441135)).literals(barcode='qwerty'),
        ),
        'Баркоды должны подтягиваться из всех источников',
    )
