# coding: utf-8
import pytest
from hamcrest import assert_that
from hamcrest import equal_to

from market.pylibrary.proto_utils import read_base64_proto

from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv

from market.proto.content.mbo.MboParameters_pb2 import Category
from market.proto.content.mbo.ExportReportModel_pb2 import ExportReportModel
from market.proto.content.pictures_pb2 import PictureMbo

from pictures import Pictures


def _models_picture(id, category_parameters, pictures):
    proto_pictures = pictures.proto_pictures() if pictures else None
    return ExportReportModel(
        id=id,
        category_id=category_parameters.hid,
        vendor_id=152712,
        current_type='GURU',
        pictures=proto_pictures,
        published_on_market=True,
    )


def _decode_proto(proto):
    proto = {x.name: y for x, y in proto.ListFields()}
    proto.pop('xslName', '')  # xslName is not serialized

    if 'parameter' in proto:  # different names in ExportReportModel_pb2.Picture and pictures_pb2.PictureMbo
        proto['parameter_values'] = proto.pop('parameter')

    return proto


@pytest.fixture(scope="module")
def category_parameters():
    return Category(
        hid=90592
    )


@pytest.fixture(scope="module")
def main_xl_picture_only():
    return Pictures(num_add_pictures=0, package_index=0)


@pytest.fixture(scope="module")
def main_xl_picture_with_two_additional():
    return Pictures(num_add_pictures=2, package_index=3)


@pytest.fixture(scope="module")
def no_main_xl_picture_only_two_additional():
    return Pictures(num_add_pictures=2, package_index=2, has_main_picture=False)


@pytest.fixture(scope="module")
def main_xl_picture_with_invalid_url():
    return Pictures(num_add_pictures=0, package_index=4, url="null")


@pytest.fixture(scope="module")
def models(category_parameters,
           main_xl_picture_only,
           main_xl_picture_with_two_additional,
           no_main_xl_picture_only_two_additional,
           main_xl_picture_with_invalid_url):
    return [
        # Одна главная и две дополнительные
        _models_picture(2441136, category_parameters, main_xl_picture_with_two_additional),
        # Только главная картинка
        _models_picture(2441137, category_parameters, main_xl_picture_only),
        # Только дополнительные картинки
        _models_picture(2441138, category_parameters, no_main_xl_picture_only_two_additional),
        # Главная картинка с некорректным URL
        _models_picture(2441139, category_parameters, main_xl_picture_with_invalid_url),
    ]


@pytest.yield_fixture(scope="module")
def workflow(models, category_parameters):
    resources = {
        'models': ModelsPb(models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters)
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.mark.parametrize('prop_name', Pictures.index_properties_names())
def test_pictures(workflow,
                  main_xl_picture_only,
                  main_xl_picture_with_two_additional,
                  no_main_xl_picture_only_two_additional,
                  prop_name):
    properties = [
        main_xl_picture_with_two_additional.index_properties(),
        main_xl_picture_only.index_properties(),
        no_main_xl_picture_only_two_additional.index_properties(),
        None
    ]

    expected_props = {}
    for i in range(len(properties)):
        v = properties[i].get(prop_name) if properties[i] is not None else None
        expected_props[i] = v

    actual_props = {
        i: attrs.get(prop_name)
        for i, attrs in list(workflow.offers.items())
    }
    assert_that(actual_props, equal_to(expected_props), 'picture attributes')


@pytest.mark.parametrize('document', [0])
def test_proto_pictures_from_pictures_with_additional(workflow,
                                                      main_xl_picture_with_two_additional,
                                                      document):
    """С данными о картинках в pictures, поля ProtoPicInfo и AddProtoPicsInfo корректны
    """
    expected_props = [_decode_proto(pic.proto_picture())
                      for pic in [main_xl_picture_with_two_additional.main_picture] +
                      main_xl_picture_with_two_additional.add_pictures]

    offers = workflow.offers[document]
    proto_strs = [offers['ProtoPicInfo']] + offers['AddProtoPicsInfo'].split('|')
    actual_props = [_decode_proto(read_base64_proto(proto, PictureMbo)) for proto in proto_strs]

    assert_that(actual_props, equal_to(expected_props), 'picture proto attributes')


@pytest.mark.parametrize('document', [1])
def test_proto_pictures_from_pictures_main_only(workflow,
                                                main_xl_picture_only,
                                                document):
    """С данными только о главной картинке в pictures, поля ProtoPicInfo корректны
    """
    expected_props = [_decode_proto(pic.proto_picture())
                      for pic in [main_xl_picture_only.main_picture] + main_xl_picture_only.add_pictures]

    offers = workflow.offers[document]
    proto_strs = [offers['ProtoPicInfo']]
    actual_props = [_decode_proto(read_base64_proto(proto, PictureMbo)) for proto in proto_strs]

    assert_that(actual_props, equal_to(expected_props), 'picture proto attributes')
    assert 'AddPicsInfo' not in offers
    assert 'AddProtoPicsInfo' not in offers


@pytest.mark.parametrize('document', [2])
def test_proto_pictures_from_pictures_additional_only(workflow,
                                                      no_main_xl_picture_only_two_additional,
                                                      document):
    """С данными только о дополнительных картинках в pictures, поля AddProtoPicsInfo корректны
    """
    expected_props = [_decode_proto(pic.proto_picture())
                      for pic in no_main_xl_picture_only_two_additional.add_pictures]

    offers = workflow.offers[document]
    proto_strs = offers['AddProtoPicsInfo'].split('|')
    actual_props = [_decode_proto(read_base64_proto(proto, PictureMbo)) for proto in proto_strs]

    assert_that(actual_props, equal_to(expected_props), 'picture proto attributes')
    assert 'PicsInfo' not in offers
    assert 'ProtoPicsInfo' not in offers


@pytest.mark.parametrize('document', [3])
def test_proto_pictures_from_pictures_with_main_invalid_url(workflow, document):
    """С данными о XL-Picture с некорретным URL поля ProtoPicInfo и AddProtoPicsInfo пусты
    """
    offers = workflow.offers[document]
    assert 'PicInfo' not in offers
    assert 'ProtoPicInfo' not in offers
    assert 'AddPicsInfo' not in offers
    assert 'AddProtoPicsInfo' not in offers
