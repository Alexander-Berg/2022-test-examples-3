# coding: utf-8

"""
Проверяем что медицинские флаги из выгрузки MBO передается до модельного индекса
как групповой атрибут.
"""

import pytest
from hamcrest import assert_that

from market.idx.pylibrary.offer_flags.flags import MedicalFlags
from market.idx.yatf.matchers.env_matchers import HasDocs
from market.idx.yatf.resources.mbo.models_pb import ModelsPb
from market.idx.yatf.resources.mbo.parameters_pb import ParametersPb
from market.idx.models.yatf.test_envs.models_indexer import ModelsIndexerTestEnv
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue,
)
from market.proto.content.mbo.MboParameters_pb2 import Category


CATEGORY_ID=90592  # some predefined value
MODEL_ID_WITH_ALL_MEDICAL_FLAGS=1
MODEL_ID_WITH_MEDICINE_FLAG=2
MODEL_ID_WITH_MEDICAL_PRODUCT_FLAG=3
MODEL_ID_WITH_BAA_FLAG=4
MODEL_ID_WITH_PRESCRIPTION_FLAG=5
MODEL_ID_WITH_PSYCHOTROPIC_FLAG=6
MODEL_ID_WITH_NARCOTIC_FLAG=7
MODEL_ID_WITH_PRECURSOR_FLAG=8
MODEL_ID_WITH_ETHANOL_FLAG=9
MODEL_ID_WITHOUT_ALL_MEDICAL_FLAGS=10
MODEL_ID_NOT_MEDICAL_FLAGS=11


def create_model(model_id, category_id, parameter_values):
    return ExportReportModel(
        id=model_id,
        category_id=category_id,
        parameter_values=parameter_values,
        current_type='GURU',
        published_on_market=True,
    )


@pytest.fixture(scope="module")
def category_parameters():
    return Category(
        hid=CATEGORY_ID
    )


@pytest.fixture(scope="module")
def create_models(category_parameters):
    with_all_flags=create_model(
        model_id=MODEL_ID_WITH_ALL_MEDICAL_FLAGS,
        category_id=category_parameters.hid,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.DRUG_TYPE_PARAM,
                option_id=MedicalFlags.MEDICINE_TYPE_VALUE,
            ),
            ParameterValue(
                param_id=MedicalFlags.DRUG_TYPE_PARAM,
                option_id=MedicalFlags.MEDICAL_PRODUCT_TYPE_VALUE,
            ),
            ParameterValue(
                param_id=MedicalFlags.DRUG_TYPE_PARAM,
                option_id=MedicalFlags.BAA_TYPE_VALUE,
            ),
            ParameterValue(
                param_id=MedicalFlags.PRESCRIPTION_PARAM_OLD,
                bool_value=True,
            ),
            ParameterValue(
                param_id=MedicalFlags.PSYCHOTROPIC_PARAM,
                bool_value=True,
            ),
            ParameterValue(
                param_id=MedicalFlags.NARCOTIC_PARAM,
                bool_value=True,
            ),
            ParameterValue(
                param_id=MedicalFlags.PRECURSOR_PARAM,
                bool_value=True,
            ),
            ParameterValue(
                param_id=MedicalFlags.ETHANOL_PRECENT_PARAM,
                numeric_value=str(MedicalFlags.ETHANOL_PRECENT_LIMIT+1),
            ),
        ]
    )

    with_medicine_flag=create_model(
        model_id=MODEL_ID_WITH_MEDICINE_FLAG,
        category_id=category_parameters.hid,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.DRUG_TYPE_PARAM,
                option_id=MedicalFlags.MEDICINE_TYPE_VALUE,
            ),
        ]
    )

    with_medical_product_flag=create_model(
        model_id=MODEL_ID_WITH_MEDICAL_PRODUCT_FLAG,
        category_id=category_parameters.hid,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.DRUG_TYPE_PARAM,
                option_id=MedicalFlags.MEDICAL_PRODUCT_TYPE_VALUE,
            ),
        ]
    )

    with_baa_flag=create_model(
        model_id=MODEL_ID_WITH_BAA_FLAG,
        category_id=category_parameters.hid,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.DRUG_TYPE_PARAM,
                option_id=MedicalFlags.BAA_TYPE_VALUE,
            ),
        ]
    )

    with_prescription_flag=create_model(
        model_id=MODEL_ID_WITH_PRESCRIPTION_FLAG,
        category_id=category_parameters.hid,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.PRESCRIPTION_PARAM_NEW,
                bool_value=True,
            ),
        ]
    )

    with_psychotropic_flag=create_model(
        model_id=MODEL_ID_WITH_PSYCHOTROPIC_FLAG,
        category_id=category_parameters.hid,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.PSYCHOTROPIC_PARAM,
                bool_value=True,
            ),
        ]
    )

    with_narcotic_flag=create_model(
        model_id=MODEL_ID_WITH_NARCOTIC_FLAG,
        category_id=category_parameters.hid,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.NARCOTIC_PARAM,
                bool_value=True,
            ),
        ]
    )

    with_precursor_flag=create_model(
        model_id=MODEL_ID_WITH_PRECURSOR_FLAG,
        category_id=category_parameters.hid,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.PRECURSOR_PARAM,
                bool_value=True,
            ),
        ]
    )

    with_ethanol_flag=create_model(
        model_id=MODEL_ID_WITH_ETHANOL_FLAG,
        category_id=category_parameters.hid,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.ETHANOL_PRECENT_PARAM,
                numeric_value=str(MedicalFlags.ETHANOL_PRECENT_LIMIT+1),
            )
        ]
    )

    without_flags=create_model(
        model_id=MODEL_ID_WITHOUT_ALL_MEDICAL_FLAGS,
        category_id=category_parameters.hid,
        parameter_values=[]
    )

    not_medical_flags=create_model(
        model_id=MODEL_ID_NOT_MEDICAL_FLAGS,
        category_id=category_parameters.hid,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.DRUG_TYPE_PARAM+1,
                option_id=MedicalFlags.MEDICINE_TYPE_VALUE+1,
            ),
        ]
    )

    return [
        with_all_flags,
        with_medicine_flag,
        with_medical_product_flag,
        with_baa_flag,
        with_prescription_flag,
        with_psychotropic_flag,
        with_narcotic_flag,
        with_precursor_flag,
        with_ethanol_flag,
        without_flags,
        not_medical_flags,
    ]


@pytest.yield_fixture(scope="module")
def workflow(create_models, category_parameters):
    resources = {
        'models': ModelsPb(create_models, category_parameters.hid),
        'parameters': ParametersPb(category_parameters),
    }
    with ModelsIndexerTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


def test_model_with_medical_flags(workflow):
    medical_flags = MedicalFlags.MIF_NONE.value
    medical_flags |= MedicalFlags.IS_MEDICINE.value
    medical_flags |= MedicalFlags.IS_MEDICAL_PRODUCT.value
    medical_flags |= MedicalFlags.IS_BAA.value
    medical_flags |= MedicalFlags.IS_PRESCRIPTION.value
    medical_flags |= MedicalFlags.IS_PSYCHOTROPIC.value
    medical_flags |= MedicalFlags.IS_NARCOTIC.value
    medical_flags |= MedicalFlags.IS_PRECURSOR.value
    medical_flags |= MedicalFlags.IS_ETHANOL.value

    assert_that(
        workflow,
        HasDocs().attributes(hyper=str(MODEL_ID_WITH_ALL_MEDICAL_FLAGS))
        .attributes(medical_flags=str(medical_flags)),
        'Model has medical flags')


def test_model_with_medicine_flag(workflow):
    assert_that(
        workflow,
        HasDocs().attributes(hyper=str(MODEL_ID_WITH_MEDICINE_FLAG))
        .attributes(medical_flags=str(MedicalFlags.IS_MEDICINE.value)),
        'Model has medicine_flag')


def test_model_with_medical_product_flag(workflow):
    assert_that(
        workflow,
        HasDocs().attributes(hyper=str(MODEL_ID_WITH_MEDICAL_PRODUCT_FLAG))
        .attributes(medical_flags=str(MedicalFlags.IS_MEDICAL_PRODUCT.value)),
        'Model has medical product flag')


def test_model_with_baa_flag(workflow):
    assert_that(
        workflow,
        HasDocs().attributes(hyper=str(MODEL_ID_WITH_BAA_FLAG))
        .attributes(medical_flags=str(MedicalFlags.IS_BAA.value)),
        'Model has baa flag')


def test_model_with_prescription_flag(workflow):
    assert_that(
        workflow,
        HasDocs().attributes(hyper=str(MODEL_ID_WITH_PRESCRIPTION_FLAG))
        .attributes(medical_flags=str(MedicalFlags.IS_PRESCRIPTION.value)),
        'Model has prescription flag')


def test_model_with_psychotropic_flag(workflow):
    assert_that(
        workflow,
        HasDocs().attributes(hyper=str(MODEL_ID_WITH_PSYCHOTROPIC_FLAG))
        .attributes(medical_flags=str(MedicalFlags.IS_PSYCHOTROPIC.value)),
        'Model has psychotropic flag')


def test_model_with_narcotic_flag(workflow):
    assert_that(
        workflow,
        HasDocs().attributes(hyper=str(MODEL_ID_WITH_NARCOTIC_FLAG))
        .attributes(medical_flags=str(MedicalFlags.IS_NARCOTIC.value)),
        'Model has narcotic flag')


def test_model_with_precursor_flag(workflow):
    assert_that(
        workflow,
        HasDocs().attributes(hyper=str(MODEL_ID_WITH_PRECURSOR_FLAG))
        .attributes(medical_flags=str(MedicalFlags.IS_PRECURSOR.value)),
        'Model has precursor flag')


def test_model_with_ethanol_flag(workflow):
    assert_that(
        workflow,
        HasDocs().attributes(hyper=str(MODEL_ID_WITH_ETHANOL_FLAG))
        .attributes(medical_flags=str(MedicalFlags.IS_ETHANOL.value)),
        'Model has ethanol flag')


def test_model_without_medical_flags(workflow):
    assert_that(
        workflow,
        HasDocs().attributes(hyper=str(MODEL_ID_WITHOUT_ALL_MEDICAL_FLAGS))
        .attributes(medical_flags=None),
        'Model does not have medical flags')


def test_model_not_medical_flags(workflow):
    assert_that(
        workflow,
        HasDocs().attributes(hyper=str(MODEL_ID_NOT_MEDICAL_FLAGS))
        .attributes(medical_flags=None),
        'Model does not have medical flags')
