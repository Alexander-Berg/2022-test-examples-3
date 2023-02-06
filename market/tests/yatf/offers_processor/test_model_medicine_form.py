#!/usr/bin/env python
# coding: utf-8
"""
Проверяем проставление параметров формы выпуска на офферы
"""

import pytest
from hamcrest import assert_that

from market.idx.offers.yatf.utils.fixtures import default_genlog, genererate_default_pictures
from market.idx.offers.yatf.resources.offers_indexer.model_medicine_form import ModelMedicineForm
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecordRecursive
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable


test_data = [
    {
        'offer_id': '1',
        'model_id': 1,
        'model_medicine_form_param': 123,
        'model_medicine_form_option': 456
    },
    {
        'offer_id': '2',
        'model_id': 2,
        'model_medicine_form_param': None,
        'model_medicine_form_option': None
    },
    {
        'offer_id': '3',
        'cluster_id': 3,
        'pictures': genererate_default_pictures(),  # pictures need so we have valid cluster
        'model_medicine_form_param': 111,
        'model_medicine_form_option': 222
    },
]


@pytest.fixture(scope="module")
def model_medicine_form():
    return ModelMedicineForm([
        (1, 123, 456),
        (3, 111, 222)
    ])


@pytest.fixture(scope="module")
def genlog_rows():
    rows = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            model_id=data.get('model_id', 0),
            cluster_id=data.get('cluster_id', None),
            pictures=data.get('pictures', None)
        )
        rows.append(offer)
    return rows


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table, model_medicine_form):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'model_medicine_form': model_medicine_form,
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


def test_model_medicine_form(workflow):
    for data in test_data:
        assert_that(
            workflow,
            HasGenlogRecordRecursive({
                'offer_id': data['offer_id'],
                'model_medicine_form_param': data['model_medicine_form_param'],
                'model_medicine_form_option': data['model_medicine_form_option'],
            })
        )
