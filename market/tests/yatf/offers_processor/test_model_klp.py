# coding: utf-8

"""
Проверяем передачу данных из файла 'model_klp.gz' до поискового литерала/поля
'klp_code' в GenerationLogger.cpp::DoCreateGenlogRecord().
"""

import pytest

from market.idx.offers.yatf.resources.offers_indexer.model_klp import ModelKlp
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.proto.content.mbo.ExportReportModel_pb2 import (
    ExportReportModel,
    ParameterValue,
    LocalizedString
)

from hamcrest import (
    assert_that,
)

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import (
    HasGenlogRecord,
)

from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join

KLP_MODEL_ID=1
NOT_KLP_MODEL_ID=KLP_MODEL_ID+1
KLP_CODE=24277150
KLP_XLS_NAME='EsklpCode'
KLP_CODE_VALUE='21.20.10.221-000010-1-00174-2000000735385'
KLP_CODE_EXPECTED='21.20.10.221-000010-1-00174'


@pytest.fixture(scope="module")
def genlog_rows():
    offer_with_klp=default_genlog()
    offer_with_klp['model_id']=KLP_MODEL_ID

    offer_without_klp=default_genlog()
    offer_without_klp['model_id']=NOT_KLP_MODEL_ID

    return [
        offer_with_klp,
        offer_without_klp,
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def model_klp():
    klp_model=ExportReportModel(
        id=KLP_MODEL_ID,
        parameter_values=[
            ParameterValue(
                param_id=KLP_CODE,
                xsl_name=KLP_XLS_NAME,
                str_value=[
                    LocalizedString(value=KLP_CODE_VALUE)
                ]
            )
        ]
    )

    not_klp_model=ExportReportModel(
        id=NOT_KLP_MODEL_ID,
        parameter_values=[
            ParameterValue(
                param_id=KLP_CODE+1,
            )
        ]
    )

    return ModelKlp([klp_model, not_klp_model])


@pytest.yield_fixture(scope="module")
def workflow(genlog_table, model_klp, yt_server):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'model_klp': model_klp,
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


def test_klp_field(workflow):
    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'klp_code': KLP_CODE_EXPECTED,
                'model_id': KLP_MODEL_ID
            }
        ),
    )
    for genlog in workflow.genlog:
        if genlog.model_id == NOT_KLP_MODEL_ID:
            assert not workflow.genlog[1].klp_code
