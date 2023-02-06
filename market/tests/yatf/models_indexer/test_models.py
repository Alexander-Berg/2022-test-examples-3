# coding: utf-8

"""
Базовый тест на корректность флагов и литералов модели
"""

import pytest
from hamcrest import assert_that, all_of

from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.models.yatf.resources.models_indexer.barcodes import Barcodes
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.models.yatf.resources.models_indexer.cluster_pictures import ClusterPicturesMmap
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.proto.content.mbo.ExportReportModel_pb2 import ExportReportModel, LocalizedString, ParameterValue
from market.proto.content.mbo.MboParameters_pb2 import Category, Parameter

from pictures import Pictures


MODEL_ID = 1
PMODEL_ID = 3

MODEL_ID_GROUP = 100
MODEL_ID_MODIFICATION = 101

CATEGORY_ID = 90592


@pytest.fixture(scope="module")
def category_parameters():
    return Category(
        hid=CATEGORY_ID,
        parameter=[
            Parameter(
                id=15060326,
                xsl_name='licensor',
                common_filter_index=1,
            ),
            Parameter(
                id=14020987,
                xsl_name='hero_global',
                published=True,
                common_filter_index=1,
            ),
            Parameter(
                id=15086295,
                xsl_name='pers_model',
                published=True,
                common_filter_index=1,
            ),
        ]
    )


def pictures2():
    return Pictures(2, 2)


def gen_name_param(name):
    return ParameterValue(xsl_name="name", str_value=[LocalizedString(isoCode="ru", value=name)])


@pytest.fixture(scope="module")
# def models(category_parameters):
def models():
    models = []

    model1 = ExportReportModel(
        id=MODEL_ID,
        category_id=CATEGORY_ID,
        vendor_id=CATEGORY_ID,
        current_type='GURU',
        published_on_market=True,
        pictures=pictures2().proto_pictures(),
        title_without_vendor=[LocalizedString(isoCode="ru", value="Smartphone iPhone X")],
        parameter_values=[gen_name_param("iPhone X")],
        source_type="GURU_DUMMY",
        aliases=[LocalizedString(isoCode="ru", value="iPhoneX")]
    )
    models.append(model1)

    model_base_group = ExportReportModel(
        id=MODEL_ID_GROUP,
        category_id=CATEGORY_ID,
        vendor_id=CATEGORY_ID,
        current_type='GURU',
        published_on_market=True,
        title_without_vendor=[LocalizedString(isoCode="ru", value="Notebook ASUS VivoBook")],
        parameter_values=[gen_name_param("ASUS VivoBook")],
    )
    models.append(model_base_group)

    model_base_modification = ExportReportModel(
        id=MODEL_ID_MODIFICATION,
        category_id=CATEGORY_ID,
        vendor_id=CATEGORY_ID,
        current_type='GURU',
        published_on_market=True,
        title_without_vendor=[LocalizedString(isoCode="ru", value="Notebook ASUS VivoBook Mod 1")],
        parameter_values=[gen_name_param("ASUS VivoBook Mod 1")],
        parent_id=MODEL_ID_GROUP,
    )
    models.append(model_base_modification)

    pmodel = ExportReportModel(
        id=PMODEL_ID,
        category_id=CATEGORY_ID,
        vendor_id=CATEGORY_ID,
        current_type='PARTNER',
        published_on_market=True,
        title_without_vendor=[LocalizedString(isoCode="ru", value="Partner Modification")]
    )
    models.append(pmodel)

    return models


@pytest.fixture(scope="module")
def barcodes():
    b = Barcodes()
    b.add_model("0001", MODEL_ID)
    b.add_model("0003", PMODEL_ID)
    b.add_group_model("0101", MODEL_ID_GROUP, MODEL_ID_MODIFICATION)
    return b


@pytest.fixture(scope="module")
def resources(models, category_parameters, barcodes):
    return {
        'barcodes': barcodes,
        'models': ModelsPb(models, category_parameters.hid),
        'cluster_pictures_mmap': ClusterPicturesMmap([]),
    }


@pytest.fixture(scope="module")
def workflow_indexer(resources):
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_indexer(workflow_indexer):
    assert_that(workflow_indexer, all_of(
        HasDocs().attributes(hyper=str(MODEL_ID))
                 .attributes(hyper_ts=str(MODEL_ID))
                 .attributes(ModelId=str(MODEL_ID))
                 .attributes(hyper_ts_blue=str(MODEL_ID))
                 .attributes(ungrouped_hyper_blue=str(MODEL_ID))
                 .attributes(has_picture=str(1))
                 .attributes(url="market.yandex.ru/product/1")
                 .attributes(is_pmodel=None)
                 .attributes(title_no_vendor="Smartphone iPhone X")
                 .attributes(name="iPhone X")
                 .attributes(vendor_id=str(CATEGORY_ID))
                 .attributes(yg=str(1))
                 .attributes(doc_type=str(2))
                 .attributes(is_guru_dummy=str(1))
                 .literals(barcode="0001")
                 .literals(host="market.yandex.ru")
                 .literals(visual="0")
                 .literals(yx_model_descr="yes"),

        # Групповые модели
        HasDocs().attributes(hyper=str(MODEL_ID_GROUP))
                 .attributes(hyper_ts=str(MODEL_ID_GROUP))
                 .attributes(ModelId=str(MODEL_ID_GROUP))
                 .attributes(hyper_ts_blue=str(MODEL_ID_GROUP))
                 .attributes(ungrouped_hyper_blue=str(MODEL_ID_GROUP))
                 .attributes(url="market.yandex.ru/product/100")
                 .attributes(is_pmodel=None)
                 .attributes(title_no_vendor="Notebook ASUS VivoBook")
                 .attributes(name="ASUS VivoBook")
                 .attributes(has_picture=str(0))
                 .attributes(yg=str(1))
                 .attributes(doc_type=str(2))
                 .literals(host="market.yandex.ru")
                 .literals(hyper_model_id=str(MODEL_ID_GROUP))
                 .literals(vendor_id=str(CATEGORY_ID))
                 .literals(visual=str(0))
                 .literals(yx_model_descr="yes"),

        HasDocs().attributes(hyper=str(MODEL_ID_MODIFICATION))
                 .attributes(hyper_ts=str(MODEL_ID_MODIFICATION))
                 .attributes(ModelId=str(MODEL_ID_MODIFICATION))
                 .attributes(hyper_ts_blue=str(MODEL_ID_MODIFICATION))
                 .attributes(ungrouped_hyper_blue=str(MODEL_ID_MODIFICATION))
                 .attributes(url="market.yandex.ru/product/101")
                 .attributes(is_pmodel=None)
                 .attributes(title_no_vendor="Notebook ASUS VivoBook Mod 1")
                 .attributes(name="ASUS VivoBook Mod 1")
                 .attributes(has_picture=str(0))
                 .attributes(yg=str(1))
                 .attributes(doc_type=str(2))
                 .literals(barcode="0101")
                 .literals(host="market.yandex.ru")
                 .literals(hyper_model_id=str(MODEL_ID_MODIFICATION))
                 .literals(parent_model_id=str(MODEL_ID_GROUP))
                 .literals(vendor_id=str(CATEGORY_ID))
                 .literals(visual=str(0))
                 .literals(yx_model_descr="yes"),

        HasDocs().attributes(hyper=str(PMODEL_ID))
                 .attributes(is_pmodel="1")
                 .attributes(ModelId=str(PMODEL_ID))
                 .attributes(title_no_vendor="Partner Modification")
                 .attributes(name=None)
                 .attributes(ungrouped_hyper_blue=str(PMODEL_ID))
    ))
