# coding: utf-8

from hamcrest import assert_that
import pytest

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def genlog_rows():
    offer = default_genlog()
    offer['picture_urls'] = [
        "beremvsetut.ru/upload/iblock/610/6103040979a8909055f87fb8ccb244dd.png",
        "market.yandex.ru?backdoor=true&exec=transfer_money",
    ]
    offer['picture_crcs'] = [u'asdasdasddasdsdd']

    return [
        offer
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture()
def pic_workflow(genlog_table, yt_server):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_picture_genlog_urls(pic_workflow):
    expected = {
        'picture_urls': [
            u'beremvsetut.ru/upload/iblock/610/6103040979a8909055f87fb8ccb244dd.png',
            u'market.yandex.ru?backdoor=true&exec=transfer_money'
        ]
    }
    assert_that(
        pic_workflow,
        HasGenlogRecord(expected),
        u'GenerationLog contains expected document'
    )


def test_picture_genlog_crc(pic_workflow):
    expected = {
        'picture_urls': [
            u'beremvsetut.ru/upload/iblock/610/6103040979a8909055f87fb8ccb244dd.png',
            u'market.yandex.ru?backdoor=true&exec=transfer_money'
        ],
        'picture_crcs': [u'asdasdasddasdsdd'],
    }
    assert_that(
        pic_workflow,
        HasGenlogRecord(expected),
        u'GenerationLog contains expected document'
    )
