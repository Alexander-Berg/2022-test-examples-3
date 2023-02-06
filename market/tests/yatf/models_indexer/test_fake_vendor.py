# coding: utf-8

"""
The test checks filling search literal 'vendor_id' for real and fake vendors.
"""


import pytest
from hamcrest import assert_that, is_not, all_of

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.global_vendors_xml import GlobalVendorsXml
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.idx.models.yatf.resources.models_indexer.cluster_pictures import ClusterPicturesMmap

from market.proto.content.mbo.ExportReportModel_pb2 import ExportReportModel
from market.proto.content.mbo.MboParameters_pb2 import Category


@pytest.fixture(scope='module', params=[90592])
def category_parameters(request):
    return Category(hid=request.param)


@pytest.fixture(scope='module')
def models(category_parameters):
    def make_model(id, vendor_id, model_type, is_group_model=False):
        return ExportReportModel(
            id=id,
            category_id=category_parameters.hid,
            vendor_id=vendor_id,
            current_type=model_type,
            group_size=int(is_group_model),
            published_on_market=True)
    return [
        make_model(0, 0, 'GURU'),
        make_model(1, 1, 'GURU'),
        make_model(2, 0, 'GURU', is_group_model=True),
        make_model(3, 1, 'GURU', is_group_model=True),
    ]


@pytest.fixture(scope='module')
def global_vendors():
    return '''
        <global-vendors>
          <vendor id="0" name="name1">
            <site>site</site>
            <picture>picture</picture>
          </vendor>
          <vendor id="1" name="yandex">
            <is-fake-vendor>true</is-fake-vendor>
          </vendor>
        </global-vendors>
    '''


@pytest.fixture(scope='module')
def workflow(models, category_parameters, global_vendors):
    resources = {
        'cluster_pictures_mmap': ClusterPicturesMmap([]),
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters),
        'global_vendors_xml': GlobalVendorsXml.from_str(global_vendors)
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_real_vendor(workflow):
    vendor_id = '0'
    assert_that(workflow, all_of(
        HasDocs().attributes(hyper='0', vendor_id=vendor_id)
                 .literals(vendor_id=vendor_id),
        HasDocs().attributes(hyper='2', vendor_id=vendor_id)
                 .literals(vendor_id=vendor_id),
    ))


def test_fake_vendor(workflow):
    vendor_id = '1'
    assert_that(workflow, all_of(
        HasDocs().attributes(hyper='1', vendor_id=vendor_id),
        is_not(HasDocs().attributes(hyper='1', vendor_id=vendor_id)
                        .literals(vendor_id=vendor_id)),
        HasDocs().attributes(hyper='3', vendor_id=vendor_id),
        is_not(HasDocs().attributes(hyper='3', vendor_id=vendor_id)
                        .literals(vendor_id=vendor_id)),
    ))
