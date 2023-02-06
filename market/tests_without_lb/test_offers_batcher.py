# coding: utf-8

import pytest
import six
import zlib
from hamcrest import assert_that, has_items, equal_to

from market.idx.pylibrary.datacamp.schema import united_offers_indexation_out_table
from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.routines.yatf.test_env import (
    StatusDiffBatcherEnv,
    FreshStatusDiffBatcherEnv,
)
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC

import yt.yson
from yt import wrapper as yt


YT_HOME = '//home/datacamp/united'
PUBLICATION_STATUS_SRC_DIR = 'publication_status_dir'
PUBLICATION_STATUS_SRC_TABLENAME = '20190122_0000'

PUBLICATION_STATUS_DST_DIR = 'publication_status_batched_dir'
FRESH_PUBLICATION_STATUS_DST_DIR = 'fresh_publication_status_batched_dir'


@pytest.fixture(scope='module')
def publication_status_diff_source_data():
    return [
        {
            'business_id': 1,
            'offer_id': 'offer_a',
            'shop_id': 22,
            'warehouse_id': 33,
            'offer': DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=1,
                    offer_id='offer_a',
                    shop_id=22,
                    warehouse_id=33,
                ),
                status=DTC.OfferStatus(
                    publication=DTC.PublicationStatus(value=DTC.PublicationStatus.PUBLISHED)
                )
            ).SerializeToString()
        },
        {
            'business_id': 1,
            'offer_id': 'offer_a',
            'shop_id': 55,
            'warehouse_id': 33,
            'offer': DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=1,
                    offer_id='offer_a',
                    shop_id=55,
                    warehouse_id=33,
                ),
                status=DTC.OfferStatus(
                    publication=DTC.PublicationStatus(value=DTC.PublicationStatus.PUBLISHED)
                )
            ).SerializeToString()
        },
        {
            'business_id': 3,
            'offer_id': 'offer_b',
            'shop_id': 11,
            'warehouse_id': 77,
            'offer': DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=3,
                    offer_id='offer_b',
                    shop_id=11,
                    warehouse_id=77,
                ),
                status=DTC.OfferStatus(
                    publication=DTC.PublicationStatus(value=DTC.PublicationStatus.PUBLISHED)
                )
            ).SerializeToString()
        },
        {
            'business_id': 3,
            'offer_id': 'offer_b',
            'shop_id': 33,
            'warehouse_id': 88,
            'offer': DTC.Offer(
                identifiers=DTC.OfferIdentifiers(
                    business_id=3,
                    offer_id='offer_b',
                    shop_id=33,
                    warehouse_id=88,
                ),
                status=DTC.OfferStatus(
                    publication=DTC.PublicationStatus(value=DTC.PublicationStatus.PUBLISHED)
                )
            ).SerializeToString()
        },
    ]


@pytest.fixture(scope='module')
def publication_status_diff_tablepath():
    return yt.ypath_join(YT_HOME, PUBLICATION_STATUS_SRC_DIR, PUBLICATION_STATUS_SRC_TABLENAME)


@pytest.fixture(scope='module')
def source_tables(yt_server, publication_status_diff_tablepath, publication_status_diff_source_data):
    yt_client = yt_server.get_yt_client()
    yt_client.create('table', publication_status_diff_tablepath, recursive=True, attributes=united_offers_indexation_out_table())
    yt_client.write_table(publication_status_diff_tablepath, publication_status_diff_source_data)
    yt_client.link(publication_status_diff_tablepath, yt.ypath_join(YT_HOME, PUBLICATION_STATUS_SRC_DIR, 'recent'))


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'color': 'white',
                'yt_home': YT_HOME,
            },
            'status_diff_batcher': {
                'enable': True,
                'output_dir': PUBLICATION_STATUS_DST_DIR,
                'compress': True,
            },
            'yt': {
                'map_reduce_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
                'publication_status_diff_dir': PUBLICATION_STATUS_SRC_DIR,
            }
        })
    return config


@pytest.fixture(scope='module')
def fresh_config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'color': 'white',
                'yt_home': YT_HOME,
            },
            'fresh_status_diff_batcher': {
                'enable': True,
                'output_dir': FRESH_PUBLICATION_STATUS_DST_DIR,
            },
            'yt': {
                'map_reduce_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
                'fresh_publication_status_diff_dir': PUBLICATION_STATUS_SRC_DIR,
            }
        })
    return config


@pytest.yield_fixture(scope='module')
def offers_batcher(
        yt_server,
        config,
        source_tables,
):
    resources = {
        'config': config,
    }
    with StatusDiffBatcherEnv(yt_server, **resources) as routines_env:
        yield routines_env


@pytest.yield_fixture(scope='module')
def fresh_offers_batcher(
        yt_server,
        fresh_config,
        source_tables,
):
    resources = {
        'config': fresh_config,
    }
    with FreshStatusDiffBatcherEnv(yt_server, **resources) as routines_env:
        yield routines_env


def convert_row(row):
    result = {}
    for k, v in row.items():
        if isinstance(v, yt.yson.yson_types.YsonStringProxy):
            result[k] = yt.yson.get_bytes(v)
        else:
            result[k] = v
    return result


def test_offers_batcher(offers_batcher, yt_server):
    yt_client = yt_server.get_yt_client()
    data = list(yt_client.read_table(yt.ypath_join(YT_HOME, PUBLICATION_STATUS_DST_DIR, 'recent')))
    data = [convert_row(row) for row in data]

    expected = [
        DTC.OffersBatch(
            offer=[
                DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=1,
                        offer_id='offer_a',
                        shop_id=22,
                        warehouse_id=33,
                    ),
                    status=DTC.OfferStatus(
                        publication=DTC.PublicationStatus(value=DTC.PublicationStatus.PUBLISHED)
                    )
                ),
                DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=1,
                        offer_id='offer_a',
                        shop_id=55,
                        warehouse_id=33,
                    ),
                    status=DTC.OfferStatus(
                        publication=DTC.PublicationStatus(value=DTC.PublicationStatus.PUBLISHED)
                    )
                )
            ]
        ).SerializeToString(),
        DTC.OffersBatch(
            offer=[
                DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=3,
                        offer_id='offer_b',
                        shop_id=11,
                        warehouse_id=77,
                    ),
                    status=DTC.OfferStatus(
                        publication=DTC.PublicationStatus(value=DTC.PublicationStatus.PUBLISHED)
                    )
                ),
                DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=3,
                        offer_id='offer_b',
                        shop_id=33,
                        warehouse_id=88,
                    ),
                    status=DTC.OfferStatus(
                        publication=DTC.PublicationStatus(value=DTC.PublicationStatus.PUBLISHED)
                    )
                )
            ]
        ).SerializeToString()
    ]

    assert_that(zlib.decompress(data[0]['batch']), equal_to(
        expected[0]
    ))

    assert_that(zlib.decompress(data[1]['batch']), equal_to(
        expected[1]
    ))

    actual_batch = [zlib.decompress(row['batch']) for row in data]
    assert_that(actual_batch, has_items(*expected))


def test_fresh_offers_batcher(fresh_offers_batcher, yt_server):
    yt_client = yt_server.get_yt_client()
    data = list(yt_client.read_table(yt.ypath_join(YT_HOME, FRESH_PUBLICATION_STATUS_DST_DIR, 'recent')))

    expected = [
        DTC.OffersBatch(
            offer=[
                DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=1,
                        offer_id='offer_a',
                        shop_id=22,
                        warehouse_id=33,
                    ),
                    status=DTC.OfferStatus(
                        publication=DTC.PublicationStatus(value=DTC.PublicationStatus.PUBLISHED)
                    )
                ),
                DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=1,
                        offer_id='offer_a',
                        shop_id=55,
                        warehouse_id=33,
                    ),
                    status=DTC.OfferStatus(
                        publication=DTC.PublicationStatus(value=DTC.PublicationStatus.PUBLISHED)
                    )
                )
            ]
        ).SerializeToString(),
        DTC.OffersBatch(
            offer=[
                DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=3,
                        offer_id='offer_b',
                        shop_id=11,
                        warehouse_id=77,
                    ),
                    status=DTC.OfferStatus(
                        publication=DTC.PublicationStatus(value=DTC.PublicationStatus.PUBLISHED)
                    )
                ),
                DTC.Offer(
                    identifiers=DTC.OfferIdentifiers(
                        business_id=3,
                        offer_id='offer_b',
                        shop_id=33,
                        warehouse_id=88,
                    ),
                    status=DTC.OfferStatus(
                        publication=DTC.PublicationStatus(value=DTC.PublicationStatus.PUBLISHED)
                    )
                )
            ]
        ).SerializeToString()
    ]

    actual_batch = [six.ensure_binary(i['batch']) for i in data]
    assert_that(actual_batch, has_items(*expected))
