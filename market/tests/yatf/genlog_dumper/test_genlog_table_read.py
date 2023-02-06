# coding: utf-8
import pytest

from market.idx.offers.yatf.resources.offers_indexer.feed import create_random_ware_md5
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable, GenlogRow

from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_TABLE_RESOURCE_NAME
)
from market.idx.offers.yatf.utils.fixtures import default_offer
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix


OFFSET = 0


@pytest.fixture(scope='module')
def offers():
    return [
        GenlogRow.fromOfferData(
            default_offer(
                sequence_number=OFFSET,
                feed_id=1,
                yx_shop_offer_id='1',
                binary_ware_md5=create_random_ware_md5(),
                binary_price={}
        ))
    ]


@pytest.yield_fixture(scope="module")
def workflow(yt_server, offers):
    resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'WARE_MD5'
        ]),
        OFFERS_TABLE_RESOURCE_NAME: GenlogOffersTable(yt_server, get_yt_prefix()+ '/genlog', offers)
    }
    with GenlogDumperTestEnv(yt_stuff=yt_server, **resources) as env:
        env.execute()
        env.verify()
        yield env


def test_dumper(workflow):
    '''
    Тест проверяет, что genlog_dumper нормально принимает на вход генложную таблицу
    Должен прийти один ware_md5
    '''
    assert workflow.ware_md5.get_ware_md5(OFFSET)
