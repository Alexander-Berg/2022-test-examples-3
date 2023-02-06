# coding: utf-8

""" Проверяет прокидывание данных в файлике model_quantities.gz до поля model_quantity_value и model_quantity_unit оффера в генлоге. """

import pytest

from market.idx.offers.yatf.resources.offers_indexer.model_quantities import ModelQuantities
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog, genererate_default_pictures
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


QUANTITY_VALUE = 123
QUANTITY_UNIT = 'шт'


@pytest.fixture(scope="module")
def genlog_rows():
    # model 1 is ok: we have record with this model in model_quantities.gz
    offer1_with_quantity = default_genlog()
    offer1_with_quantity['model_id'] = 1

    # cluster 2 is fine too: we have record with this cluster in model_quantities.gz
    offer2_with_quantity = default_genlog()
    offer2_with_quantity['cluster_id'] = 2
    offer2_with_quantity['model_id'] = 0
    offer2_with_quantity['pictures'] = genererate_default_pictures()  # pictures need so we have valid cluster

    # model 3 is not ok: we don't have record with this model in model_quantities.gz
    offer_without_quantity = default_genlog()
    offer_without_quantity['model_id'] = 3

    return [
        offer1_with_quantity,
        offer2_with_quantity,
        offer_without_quantity,
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def model_quantities():
    return ModelQuantities(
        {
            1: ['number_new', QUANTITY_VALUE],
            2: ['number_new', QUANTITY_VALUE + 1]
        }
    )


@pytest.yield_fixture(scope="module")
def workflow(genlog_table, model_quantities, yt_server):
    input_table_paths = [genlog_table.get_path()]

    resources = {
        'model_quantities': model_quantities,
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


def test_quantity_field(workflow):
    assert workflow.genlog[0].model_quantity_value == str(QUANTITY_VALUE)
    assert workflow.genlog[0].model_quantity_unit == QUANTITY_UNIT
    assert workflow.genlog[1].model_quantity_value == str(QUANTITY_VALUE+1)
    assert workflow.genlog[1].model_quantity_unit == QUANTITY_UNIT
    assert not workflow.genlog[2].model_quantity_value
    assert not workflow.genlog[2].model_quantity_unit
