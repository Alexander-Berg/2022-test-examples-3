# coding: utf-8

"""
Проверяем передачу данных из файла 'model_vidal.gz' до поискового литерала/поля
'vidal_atc_code' в GenerationLogger.cpp::DoCreateGenlogRecord().
"""

import pytest

from market.idx.offers.yatf.resources.offers_indexer.model_vidal import ModelVidal
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue,
    LocalizedString
)
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


VIDAL_MODEL_ID=1
VIDAL_ATC_CODE=23181290
VIDAL_XLS_NAME='ATCCode'
VIDAL_ATC_CODE_VALUE='J05AX13'


@pytest.fixture(scope="module")
def genlog_rows():
    offer_with_vidal=default_genlog()
    offer_with_vidal['model_id']=VIDAL_MODEL_ID

    offer_without_vidal=default_genlog()
    offer_without_vidal['model_id']=VIDAL_MODEL_ID+1

    offer_empty_vidal=default_genlog()
    offer_empty_vidal['model_id']=VIDAL_MODEL_ID+2

    offer_empty_value_vidal=default_genlog()
    offer_empty_value_vidal['model_id']=VIDAL_MODEL_ID+3

    return [
        offer_with_vidal,
        offer_without_vidal,
        offer_empty_vidal,
        offer_empty_value_vidal,
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def model_vidal():
    vidal_model=ExportReportModel(
        id=VIDAL_MODEL_ID,
        parameter_values=[
            ParameterValue(
                param_id=VIDAL_ATC_CODE,
                xsl_name=VIDAL_XLS_NAME,
                str_value=[
                    LocalizedString(value=VIDAL_ATC_CODE_VALUE)
                ]
            )
        ]
    )

    not_vidal_model=ExportReportModel(
        id=VIDAL_MODEL_ID+1,
        parameter_values=[
            ParameterValue(
                param_id=VIDAL_ATC_CODE+1,
            )
        ]
    )

    empty_vidal_model=ExportReportModel(
        id=VIDAL_MODEL_ID+2,
        parameter_values=[
            ParameterValue(
                param_id=VIDAL_ATC_CODE,
                xsl_name=VIDAL_XLS_NAME,
                str_value=[]
            )
        ]
    )

    empty_value_vidal_model=ExportReportModel(
        id=VIDAL_MODEL_ID+3,
        parameter_values=[
            ParameterValue(
                param_id=VIDAL_ATC_CODE,
                xsl_name=VIDAL_XLS_NAME,
                str_value=[
                    LocalizedString(value="")
                ]
            )
        ]
    )

    return ModelVidal([vidal_model, not_vidal_model, empty_vidal_model, empty_value_vidal_model])


@pytest.yield_fixture(scope="module")
def workflow(genlog_table, model_vidal, yt_server):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'model_vidal': model_vidal,
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


def test_vidal_field(workflow):
    assert workflow.genlog[0].vidal_atc_code == VIDAL_ATC_CODE_VALUE
    assert not workflow.genlog[1].vidal_atc_code
    assert not workflow.genlog[2].vidal_atc_code
    assert not workflow.genlog[3].vidal_atc_code
