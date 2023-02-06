# coding: utf-8

"""Проверяет прокидывание published_on_blue_market до флага синего оффера.
"""

import pytest

from market.idx.yatf.resources.model_ids import ModelIds
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
from market.idx.pylibrary.offer_flags.flags import OfferFlags


@pytest.fixture(scope="module")
def genlog_rows():
    blue_model_blue_offer = default_genlog()
    blue_model_blue_offer['model_id'] = 4
    blue_model_blue_offer['is_blue_offer'] = True
    blue_model_blue_offer['binary_ware_md5'] = 'nomkVuHL2Q0wYGPdnvvfKg'

    white_model_blue_offer = default_genlog()
    white_model_blue_offer['model_id'] = 1
    white_model_blue_offer['is_blue_offer'] = True
    white_model_blue_offer['binary_ware_md5'] = 'AomkVuHL2Q0wYGPdnvvfKg'

    white_model_white_offer = default_genlog()
    white_model_white_offer['model_id'] = 1
    white_model_white_offer['binary_ware_md5'] = 'BomkVuHL2Q0wYGPdnvvfKg'

    return [
        blue_model_blue_offer,
        white_model_blue_offer,
        white_model_white_offer
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def model_ids():
    return ModelIds([1, 2, 3, 666], blue_ids=[4, 5])


@pytest.yield_fixture(scope="module")
def workflow(genlog_table, model_ids, yt_server):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'model_ids': model_ids,
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


def test_blue_flag(workflow):
    record = workflow.genlog[0]
    assert bool(record.flags & OfferFlags.MODEL_COLOR_BLUE)
