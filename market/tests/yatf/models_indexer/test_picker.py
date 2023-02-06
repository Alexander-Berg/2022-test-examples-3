# coding: utf-8
import pytest
from hamcrest import assert_that
from hamcrest import equal_to

from market.pylibrary.proto_utils import read_base64_proto

from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv

from market.proto.content.mbo.MboParameters_pb2 import Category
from market.proto.content.mbo.ExportReportModel_pb2 import ExportReportModel, ParameterValue, LocalizedString
from market.proto.content.mbo.MboParameters_pb2 import PickerImage


@pytest.fixture(scope="module")
def category_parameters():
    return Category(
        hid=90592
    )


@pytest.fixture(scope="module")
def picker_params():
    return [
        ParameterValue(xsl_name="enum_parameter", option_id=1, image_picker=PickerImage(url="url_1")),
        ParameterValue(xsl_name="enum_parameter_nopicker", option_id=2),
        ParameterValue(xsl_name="numeric_parameter", numeric_value="3", image_picker=PickerImage(url="url_3")),
        ParameterValue(xsl_name="numeric_parameter_nopicker", numeric_value="4"),
        ParameterValue(xsl_name="string_parameter", str_value=[LocalizedString(isoCode='ru', value="5")], image_picker=PickerImage(url="url_5")),
        ParameterValue(xsl_name="string_parameter_nopicker", str_value=[LocalizedString(isoCode='ru', value="6")])
    ]


@pytest.fixture(scope="module")
def models_picker(category_parameters, picker_params):
    return [
        ExportReportModel(
            id=1,
            category_id=category_parameters.hid,
            vendor_id=152712,
            current_type='GURU',
            parameter_value_links=picker_params,
            published_on_market=True,
        )
    ]


@pytest.yield_fixture(scope="module")
def workflow_pictures(models_picker, category_parameters):
    resources = {
        'models': ModelsPb(models_picker, category_parameters.hid),
        'parameters': ParametersPb(category_parameters)
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_picker(workflow_pictures, picker_params):
    expected_props = [param for param in picker_params if "_nopicker" not in param.xsl_name]

    assert(len(expected_props) > 0)
    assert(len(workflow_pictures.offers) == 1)

    proto_strs = workflow_pictures.offers[0]["ParameterValueLinks"].split('|')
    actual_props = [read_base64_proto(proto, ParameterValue) for proto in proto_strs]

    assert_that(actual_props, equal_to(expected_props),
                "picker's parameters")
