# coding: utf-8

"""
Проверяем передачу данных из файла 'model_medical_flags.gz' до 'medical_flags'
в GenerationLogger.cpp::DoCreateGenlogRecord().
"""

import pytest

from market.idx.pylibrary.offer_flags.flags import MedicalFlags
from market.idx.offers.yatf.resources.offers_indexer.model_medical_flags import ModelMedicalFlags
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog, default_shops_dat
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue
)
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join

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
MODEL_ID_WITH_EMPTY_PARAMETER_VALUES=11
MODEL_ID_WITHOUT_PARAMETER_VALUES=12

BOOKING_FEED = 100
BOOKING_FESH = 1000


def create_offer(model_id, is_booking=False):
    offer=default_genlog()
    offer['model_id']=model_id
    if is_booking:
        offer['shop_id']=BOOKING_FESH
        offer['feed_id']=BOOKING_FEED
    return offer


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        create_offer(MODEL_ID_WITH_ALL_MEDICAL_FLAGS, True),
        create_offer(MODEL_ID_WITH_MEDICINE_FLAG),
        create_offer(MODEL_ID_WITH_MEDICAL_PRODUCT_FLAG),
        create_offer(MODEL_ID_WITH_BAA_FLAG),
        create_offer(MODEL_ID_WITH_PRESCRIPTION_FLAG),
        create_offer(MODEL_ID_WITH_PSYCHOTROPIC_FLAG),
        create_offer(MODEL_ID_WITH_NARCOTIC_FLAG),
        create_offer(MODEL_ID_WITH_PRECURSOR_FLAG),
        create_offer(MODEL_ID_WITH_ETHANOL_FLAG),
        create_offer(MODEL_ID_WITHOUT_ALL_MEDICAL_FLAGS),
        create_offer(MODEL_ID_WITH_EMPTY_PARAMETER_VALUES),
        create_offer(MODEL_ID_WITHOUT_PARAMETER_VALUES),
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def create_models():
    with_all_medical_flags=ExportReportModel(
        id=MODEL_ID_WITH_ALL_MEDICAL_FLAGS,
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

    with_medicine_flag=ExportReportModel(
        id=MODEL_ID_WITH_MEDICINE_FLAG,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.DRUG_TYPE_PARAM,
                option_id=MedicalFlags.MEDICINE_TYPE_VALUE,
            ),
        ]
    )

    with_medical_product_flag=ExportReportModel(
        id=MODEL_ID_WITH_MEDICAL_PRODUCT_FLAG,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.DRUG_TYPE_PARAM,
                option_id=MedicalFlags.MEDICAL_PRODUCT_TYPE_VALUE,
            ),
        ]
    )

    with_baa_flag=ExportReportModel(
        id=MODEL_ID_WITH_BAA_FLAG,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.DRUG_TYPE_PARAM,
                option_id=MedicalFlags.BAA_TYPE_VALUE,
            ),
        ]
    )

    with_prescription_flag=ExportReportModel(
        id=MODEL_ID_WITH_PRESCRIPTION_FLAG,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.PRESCRIPTION_PARAM_NEW,
                bool_value=True,
            ),
        ]
    )

    with_psychotropic_flag=ExportReportModel(
        id=MODEL_ID_WITH_PSYCHOTROPIC_FLAG,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.PSYCHOTROPIC_PARAM,
                bool_value=True,
            ),
        ]
    )

    with_narcotic_flag=ExportReportModel(
        id=MODEL_ID_WITH_NARCOTIC_FLAG,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.NARCOTIC_PARAM,
                bool_value=True,
            ),
        ]
    )

    with_precursor_flag=ExportReportModel(
        id=MODEL_ID_WITH_PRECURSOR_FLAG,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.PRECURSOR_PARAM,
                bool_value=True,
            ),
        ]
    )

    with_ethanol_flag=ExportReportModel(
        id=MODEL_ID_WITH_ETHANOL_FLAG,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.ETHANOL_PRECENT_PARAM,
                numeric_value=str(MedicalFlags.ETHANOL_PRECENT_LIMIT+1),
            )
        ]
    )

    without_medical_flags=ExportReportModel(
        id=MODEL_ID_WITHOUT_ALL_MEDICAL_FLAGS,
        parameter_values=[
            ParameterValue(
                param_id=MedicalFlags.DRUG_TYPE_PARAM+1,
                option_id=MedicalFlags.MEDICINE_TYPE_VALUE+1,
            ),
        ]
    )

    with_empty_parameter_values=ExportReportModel(
        id=MODEL_ID_WITH_EMPTY_PARAMETER_VALUES,
        parameter_values=[]
    )

    without_parameter_values=ExportReportModel(
        id=MODEL_ID_WITHOUT_PARAMETER_VALUES,
    )

    return ModelMedicalFlags([
        with_all_medical_flags,
        without_medical_flags,
        with_medicine_flag,
        with_medical_product_flag,
        with_baa_flag,
        with_prescription_flag,
        with_psychotropic_flag,
        with_narcotic_flag,
        with_precursor_flag,
        with_ethanol_flag,
        with_empty_parameter_values,
        without_parameter_values,
    ])


@pytest.yield_fixture(scope='module')
def custom_shops_dat():
    shops = [
        default_shops_dat(
            name="Shop_medical_booking",
            fesh=BOOKING_FESH,
            datafeed_id=BOOKING_FEED,
            priority_region=213,
            regions=[225],
            home_region=225,
            medical_booking='true',
        ),
    ]

    return ShopsDat(shops)


@pytest.yield_fixture(scope="module")
def workflow(genlog_table, create_models, yt_server, custom_shops_dat):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'model_medical_flags': create_models,
        'shops_dat': custom_shops_dat,
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_all_medical_flags(workflow):
    medical_flags = 0
    medical_flags |= MedicalFlags.IS_MEDICINE
    medical_flags |= MedicalFlags.IS_MEDICAL_PRODUCT
    medical_flags |= MedicalFlags.IS_BAA
    medical_flags |= MedicalFlags.IS_PRESCRIPTION
    medical_flags |= MedicalFlags.IS_PSYCHOTROPIC
    medical_flags |= MedicalFlags.IS_NARCOTIC
    medical_flags |= MedicalFlags.IS_PRECURSOR
    medical_flags |= MedicalFlags.IS_ETHANOL
    medical_flags |= MedicalFlags.IS_BOOKING
    assert workflow.genlog[MODEL_ID_WITH_ALL_MEDICAL_FLAGS-1].medical_flags == medical_flags


def test_medicine_flag(workflow):
    assert workflow.genlog[MODEL_ID_WITH_MEDICINE_FLAG-1].medical_flags == MedicalFlags.IS_MEDICINE


def test_medical_product_flag(workflow):
    assert workflow.genlog[MODEL_ID_WITH_MEDICAL_PRODUCT_FLAG-1].medical_flags == MedicalFlags.IS_MEDICAL_PRODUCT


def test_baa_flag(workflow):
    assert workflow.genlog[MODEL_ID_WITH_BAA_FLAG-1].medical_flags == MedicalFlags.IS_BAA


def test_prescription_flag(workflow):
    assert workflow.genlog[MODEL_ID_WITH_PRESCRIPTION_FLAG-1].medical_flags == MedicalFlags.IS_PRESCRIPTION


def test_psychotropic_flag(workflow):
    assert workflow.genlog[MODEL_ID_WITH_PSYCHOTROPIC_FLAG-1].medical_flags == MedicalFlags.IS_PSYCHOTROPIC


def test_narcotic_flag(workflow):
    assert workflow.genlog[MODEL_ID_WITH_NARCOTIC_FLAG-1].medical_flags == MedicalFlags.IS_NARCOTIC


def test_precursor_flag(workflow):
    assert workflow.genlog[MODEL_ID_WITH_PRECURSOR_FLAG-1].medical_flags == MedicalFlags.IS_PRECURSOR


def test_ethanol_flag(workflow):
    assert workflow.genlog[MODEL_ID_WITH_ETHANOL_FLAG-1].medical_flags == MedicalFlags.IS_ETHANOL


def test_without_medical_flags(workflow):
    assert workflow.genlog[MODEL_ID_WITHOUT_ALL_MEDICAL_FLAGS-1].medical_flags == MedicalFlags.MIF_NONE


def test_with_empty_parameter_values(workflow):
    assert workflow.genlog[MODEL_ID_WITH_EMPTY_PARAMETER_VALUES-1].medical_flags == MedicalFlags.MIF_NONE


def test_without_parameter_values(workflow):
    assert workflow.genlog[MODEL_ID_WITHOUT_PARAMETER_VALUES-1].medical_flags == MedicalFlags.MIF_NONE
