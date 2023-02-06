# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to

from market.proto.feedparser.deprecated.OffersData_pb2 import Offer

from market.idx.streams.src.prepare_market_streams.yatf.test_env import (
    YtPrepareMarketStreamsTestEnv
)

from market.idx.yatf.resources.yt_table_resource import YtTableResource

from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope='module')
def market_offers_1():
    return [
        Offer(ware_md5='rOS1xfDFZ0pVzK6SM7ssnw'),
        Offer(ware_md5='NFX81k2cVVWzWA9wsa5iqg'),
        Offer(ware_md5='GOFvVH3n0D7ycHCJdpVnZA'),
        Offer(ware_md5='SZRJIUMjDFhdO8nmJjcecw')
    ]


@pytest.fixture(scope='module')
def market_offers_2():
    return [
        Offer(ware_md5='-mptmh3zfUacE0Fd9D-Ing')
    ]


@pytest.fixture(scope='module')
def market_offers_3():
    return [
        Offer(ware_md5='7EvryZZVaMlhJuQKZD0tVg'),
        Offer(ware_md5='G-Um2BW1u1Jx10yISGgSGQ'),
        Offer(ware_md5='l69jJq5JvR8gSgdj4LEvrQ')
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
                {'offer': offer.SerializeToString(),
                 'ware_md5': offer.ware_md5}
            ]

        table_out = YtTableResource(yt_stuff,
                                    ypath_join(path, '%04d' % idx),
                                    data_for_table)
        table_out.dump()

    return path


@pytest.fixture(scope='module')
def recent_market_table(yt_stuff):
    data = [
        {
            'text': 'детский ночник switel bc190',
            'value': '1',
            'region_id': 225,
            'ware_md5': 'G-Um2BW1u1Jx10yISGgSGQ'
        },
        {
            'text': 'набор отверток lsf-016 narofominsk',
            'value': '1',
            'region_id': 225,
            'ware_md5': 'NFX81k2cVVWzWA9wsa5iqg'
        },
        # ware_md5 not from the offers tables
        {
            'text': 'дистиллятор бытовой',
            'value': '1',
            'region_id': 225,
            'ware_md5': 'FV4luHL58uH6N40t_FfXCQ'
        },
        {
            'text': 'micro hdmi hdmi',
            'value': '0.5',
            'region_id': 225,
            'ware_md5': 'SZRJIUMjDFhdO8nmJjcecw'
        }
    ]
    attributes = dict(schema=[
        dict(name='text', type='string'),
        dict(name='value', type='string'),
        dict(name='region_id', type='uint64'),
        dict(name='ware_md5', type='string')])

    table = YtTableResource(yt_stuff, '//home/test_prepare_market_for_offers/market', data, attributes=attributes)
    table.dump()

    return table


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, recent_market_table, offers_dir):
    resources = {}

    with YtPrepareMarketStreamsTestEnv(**resources) as env:
        env.execute(yt_stuff,
                    recent_market_table.get_path(),
                    offers_dir,
                    '//home/test_prepare_market_for_offers/result',
                    3)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_table(workflow):
    return workflow.outputs.get('result_path')


def test_results(result_table):
    sorted_by_key = lambda lst: sorted(lst, key=lambda row: row['ware_md5'])
    assert_that(sorted_by_key(list(result_table.data)),
                equal_to(sorted_by_key([
                    {
                        'text': 'детский ночник switel bc190',
                        'value': '1',
                        'region_id': 225,
                        'ware_md5': 'G-Um2BW1u1Jx10yISGgSGQ',
                        'part': 2,
                        'url' : None,
                        'table_index': 0
                    },
                    {
                        'text': 'набор отверток lsf-016 narofominsk',
                        'value': '1',
                        'region_id': 225,
                        'ware_md5': 'NFX81k2cVVWzWA9wsa5iqg',
                        'part': 0,
                        'url' : None,
                        'table_index': 0
                    },
                    {
                        'text': 'micro hdmi hdmi',
                        'value': '0.5',
                        'region_id': 225,
                        'ware_md5': 'SZRJIUMjDFhdO8nmJjcecw',
                        'part': 0,
                        'url' : None,
                        'table_index': 0
                    }
                ])),
                'Wrong descriptions')
