# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to

from market.proto.feedparser.deprecated.OffersData_pb2 import Offer
from market.proto.indexer import GenerationLog_pb2

from market.idx.streams.src.prepare_offer_streams.yatf.test_env import (
    YtCustomOfferStreamsTestEnv
)

from market.idx.yatf.resources.yt_table_resource import YtTableResource

from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope='module')
def market_offers_1():
    return [
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='rOS1xfDFZ0pVzK6SM7ssnw',
                title='offer1',
                url='http://example.ru/offer/1'
            ),
        ),
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='NFX81k2cVVWzWA9wsa5iqg',
                title='offer2',
                url='http://example.ru/offer/2'
            ),
        ),
    ]


@pytest.fixture(scope='module')
def market_offers_2():
    return [
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='-mptmh3zfUacE0Fd9D-Ing',
                title='offer3',
                url='http://example.ru/offer/3'
            ),
        )
    ]


@pytest.fixture(scope='module')
def market_offers_3():
    return [
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='7EvryZZVaMlhJuQKZD0tVg',
                title='offer4',
                url='http://example.ru/offer/4'
            ),
        ),
    ]


@pytest.fixture(scope='module')
def offers_dir(yt_stuff,
               market_offers_1,
               market_offers_2,
               market_offers_3):
    path = '//home/test_prepare_market_for_offers/offers'

    yt_stuff.get_yt_client().create(
        'map_node',
        path,
        recursive=True,
        ignore_existing=True,
    )

    for idx, table in enumerate((market_offers_1, market_offers_2, market_offers_3)):
        data_for_table = []
        for offer in table:
            data_for_table += [
                {'offer': offer.SerializeToString()}
            ]

        table_out = YtTableResource(yt_stuff,
                                    ypath_join(path, '%04d' % idx),
                                    data_for_table)
        table_out.dump()

    return path


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, offers_dir):
    resources = {}

    with YtCustomOfferStreamsTestEnv(**resources) as env:
        env.execute(yt_stuff,
                    offers_dir,
                    '//home/test_titles_for_offers/result',
                    3)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_table(workflow):
    return workflow.outputs.get('result_path_title')


def test_titles(result_table):
    sorted_by_key = lambda lst: sorted(lst, key=lambda row: row['ware_md5'])
    assert_that(sorted_by_key(list(result_table.data)),
                equal_to(sorted_by_key([
                    {
                        'text': 'offer3',
                        'value': '1',
                        'region_id': 225,
                        'ware_md5': '-mptmh3zfUacE0Fd9D-Ing',
                        'part': 1,
                        'url': 'http://example.ru/offer/3',
                    },
                    {
                        'text': 'offer4',
                        'value': '1',
                        'region_id': 225,
                        'ware_md5': '7EvryZZVaMlhJuQKZD0tVg',
                        'part': 2,
                        'url': 'http://example.ru/offer/4',
                    },
                    {
                        'text': 'offer1',
                        'value': '1',
                        'region_id': 225,
                        'ware_md5': 'rOS1xfDFZ0pVzK6SM7ssnw',
                        'part': 0,
                        'url': 'http://example.ru/offer/1',
                    },
                    {
                        'text': 'offer2',
                        'value': '1',
                        'region_id': 225,
                        'ware_md5': 'NFX81k2cVVWzWA9wsa5iqg',
                        'part': 0,
                        'url': 'http://example.ru/offer/2',
                    },
                ])),
                'Wrong titles')
