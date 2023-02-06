# coding: utf-8

from hamcrest import assert_that, is_not, empty
import pytest

import yt.wrapper as yt

from market.idx.datacamp.routines.yatf.resources.config_mock import RoutinesConfigMock
from market.idx.datacamp.yatf.utils import create_update_meta
from market.idx.yatf.resources.datacamp.datacamp_tables import DataCampPartnersTable, DataCampOutOffersTable
from market.idx.datacamp.routines.yatf.test_env import HttpRoutinesTestEnv
from market.idx.yatf.matchers.protobuf_matchers import IsSerializedProtobuf
from market.idx.datacamp.yatf.matchers.matchers import HasStatus
from market.idx.datacamp.yatf.utils import dict2tskv
import market.idx.datacamp.proto.offer.DataCampOffer_pb2 as DTC
from market.idx.yatf.matchers.yt_rows_matchers import HasDatacampYtUnitedOffersRows


PARTNERS = [
    # shop_id=1..9 - включенные магазины -> дифф считаем
    {
        'shop_id': i,
        'status': 'publish',
        'mbi': dict2tskv({
            'shop_id': i,
            'business_id': i*10,
            'datafeed_id': 100+i,
            'is_enabled': True
        })
    } for i in range(1, 10)] + [
    # shop_id=10 магазин в шопсдате выключен -> дифф не считаем
    {
        'shop_id': 10,
        'status': 'disable',
        'mbi': dict2tskv({
            'shop_id': 10,
            'business_id': 1,
            'datafeed_id': 101,
            'is_enabled': False
        })
    },
    # shop_id=20 магазина нет в шопсдат -> дифф не считаем
    # shop_id=30 магазин в шопсдате выключен, но alive -> дифф не считаем
    {
        'shop_id': 30,
        'status': 'publish',
        'mbi': dict2tskv({
            'shop_id': 30,
            'business_id': 3,
            'datafeed_id': 103,
            'is_alive': True
        })
    },
    # shop_id=40 магазин в шопсдате в PS -> дифф не считаем
    {
        'shop_id': 40,
        'status': 'publish',
        'mbi': dict2tskv({
            'shop_id': 40,
            'business_id': 4,
            'datafeed_id': 104,
            'is_tested': True
        })
    }
]

OFFERS = [
    {
        'business_id': 1,
        'offer_id': 'only_external',
        'shop_id': 2,
        'warehouse_id': 0,
        'add_to_datacamp_table': False,
        'add_to_indexation_table': True,
        'publication_status': None,
        'external_publication_status': DTC.PublicationStatus.PUBLISHED,
        'verdicts': None,
        'external_verdicts': None,
    },

    {
        'business_id': 4,
        'offer_id': 'only_datacamp_offer',
        'shop_id': 2,
        'warehouse_id': 0,
        'add_to_datacamp_table': True,
        'add_to_indexation_table': False,
        'publication_status': DTC.PublicationStatus.PUBLISHED,
        'external_publication_status': None,
        'verdicts': None,
        'external_verdicts': None,
    },

    {
        'business_id': 7,
        'offer_id': 'unknown2published',
        'shop_id': 7,
        'warehouse_id': 7,
        'add_to_datacamp_table': True,
        'add_to_indexation_table': True,
        'publication_status': DTC.PublicationStatus.UNKNOWN_STATUS,
        'external_publication_status': DTC.PublicationStatus.PUBLISHED,
        'verdicts': None,
        'external_verdicts': None,
    },

    {
        'business_id': 7,
        'offer_id': 'unknown2not_published',
        'shop_id': 7,
        'warehouse_id': 7,
        'add_to_datacamp_table': True,
        'add_to_indexation_table': True,
        'publication_status': DTC.PublicationStatus.UNKNOWN_STATUS,
        'external_publication_status': DTC.PublicationStatus.NOT_PUBLISHED,
        'verdicts': None,
        'external_verdicts': None,
    },

    {
        'business_id': 3,
        'offer_id': 'both_offer_match',
        'shop_id': 2,
        'warehouse_id': 3,
        'add_to_datacamp_table': True,
        'add_to_indexation_table': True,
        'publication_status': DTC.PublicationStatus.NOT_PUBLISHED,
        'external_publication_status': DTC.PublicationStatus.PUBLISHED,
        'verdicts': None,
        'external_verdicts': None,
    },

    {
        'business_id': 5,
        'offer_id': 'empty_verdicts',
        'shop_id': 6,
        'warehouse_id': 7,
        'add_to_datacamp_table': True,
        'add_to_indexation_table': True,
        'publication_status': None,
        'external_publication_status': None,
        'verdicts': None,
        'external_verdicts': [DTC.Explanation(code='333', details='{some=detail}')],
    },
    {
        'business_id': 5,
        'offer_id': 'new_verdicts',
        'shop_id': 7,
        'warehouse_id': 0,
        'add_to_datacamp_table': True,
        'add_to_indexation_table': True,
        'publication_status': None,
        'external_publication_status': None,
        'verdicts': [DTC.Explanation(code='333')],
        'external_verdicts': [DTC.Explanation(code='444')],
    },
    {
        'business_id': 5,
        'offer_id': 'old_verdicts',
        'shop_id': 8,
        'warehouse_id': 8,
        'add_to_datacamp_table': True,
        'add_to_indexation_table': True,
        'publication_status': None,
        'external_publication_status': None,
        'verdicts': [DTC.Explanation(code='333')],
        'external_verdicts': None,
    },

    {
        'business_id': 1,
        'offer_id': 'shop_is_disabled',
        'shop_id': 10,
        'warehouse_id': 3,
        'add_to_datacamp_table': True,
        'add_to_indexation_table': True,
        'publication_status': DTC.PublicationStatus.NOT_PUBLISHED,
        'external_publication_status': DTC.PublicationStatus.PUBLISHED,
        'verdicts': None,
        'external_verdicts': None,
    },
    {
        'business_id': 2,
        'offer_id': 'shop_is_not_in_partners',
        'shop_id': 20,
        'warehouse_id': 3,
        'add_to_datacamp_table': True,
        'add_to_indexation_table': True,
        'publication_status': DTC.PublicationStatus.NOT_PUBLISHED,
        'external_publication_status': DTC.PublicationStatus.PUBLISHED,
        'verdicts': None,
        'external_verdicts': None,
    },
    {
        'business_id': 3,
        'offer_id': 'shop_is_disabled_alive',
        'shop_id': 30,
        'warehouse_id': 3,
        'add_to_datacamp_table': True,
        'add_to_indexation_table': True,
        'publication_status': DTC.PublicationStatus.NOT_PUBLISHED,
        'external_publication_status': DTC.PublicationStatus.PUBLISHED,
        'verdicts': None,
        'external_verdicts': None,
    },
    {
        'business_id': 4,
        'offer_id': 'shop_is_in_planeshift',
        'shop_id': 40,
        'warehouse_id': 3,
        'add_to_datacamp_table': True,
        'add_to_indexation_table': True,
        'publication_status': DTC.PublicationStatus.NOT_PUBLISHED,
        'external_publication_status': DTC.PublicationStatus.PUBLISHED,
        'verdicts': None,
        'external_verdicts': None,
    },

]


def build_row(business_id, offer_id, shop_id, warehouse_id, timestamp=10, publication_status=None, verdicts=None):
    offer = DTC.Offer(
        identifiers=DTC.OfferIdentifiers(
            business_id=business_id,
            offer_id=offer_id,
            shop_id=shop_id,
            warehouse_id=warehouse_id,
        )
    )
    if publication_status is not None:
        offer.status.CopyFrom(
            DTC.OfferStatus(
                publication=DTC.PublicationStatus(
                    meta=create_update_meta(timestamp, source=DTC.MARKET_IDX_GENERATION),
                    value=publication_status
                ),
            )
        )
    if verdicts is not None:
        offer.resolution.CopyFrom(
            DTC.Resolution(
                by_source=[
                    DTC.Verdicts(
                        meta=create_update_meta(timestamp, source=DTC.MARKET_IDX_GENERATION),
                        verdict=[
                            DTC.Verdict(
                                results=[
                                    DTC.ValidationResult(
                                        messages=verdicts,
                                    ),
                                ]
                            ),
                        ],
                    ),
                ],
            )
        )

    return {
        'business_id': business_id,
        'offer_id': offer_id,
        'shop_id': shop_id,
        'warehouse_id': warehouse_id,
        'offer': offer.SerializeToString(),
    }


def build_offers(offers, add_row_key, publication_status_key, verdicts_key):
    result = list()
    for offer in offers:
        if offer[add_row_key]:
            result.append(
                build_row(
                    business_id=offer['business_id'],
                    offer_id=offer['offer_id'],
                    shop_id=offer['shop_id'],
                    warehouse_id=offer['warehouse_id'],
                    timestamp=offer.get('timestamp', 10),
                    publication_status=offer[publication_status_key],
                    verdicts=offer[verdicts_key]
                )
            )
    return result


@pytest.fixture(scope='module')
def datacamp_offers():
    return build_offers(OFFERS, 'add_to_datacamp_table', 'publication_status', 'verdicts')


@pytest.fixture(scope='module')
def external_offers():
    return build_offers(OFFERS, 'add_to_indexation_table', 'external_publication_status', 'external_verdicts')


@pytest.fixture(scope='module')
def datacamp_table(yt_server, config, datacamp_offers):
    return DataCampOutOffersTable(yt_server, config.yt_white_output_dir + '/recent', data=datacamp_offers)


@pytest.fixture(scope='module')
def partners_table(yt_server, config):
    return DataCampPartnersTable(yt_server, config.yt_partners_tablepath, data=PARTNERS)


@pytest.fixture(scope='module')
def external_table(yt_server, config, external_offers):
    return DataCampOutOffersTable(yt_server, '//home/external', data=external_offers)


@pytest.fixture(scope='module')
def config(yt_server):
    config = RoutinesConfigMock(
        yt_server,
        config={
            'general': {
                'color': 'white',
                'yt_home': '//home/datacamp/united'
            },
            'routines': {
                'enable_united_datacamp_dumper': True,
                'days_number_to_take_disabled_offer_in_index': 5,
                'enable_united_datacamp_export_dumper': True,
                'ignore_disabled_shops': True
            },
            'yt': {
                'white_out': 'white_out',
                'turbo_out': 'turbo_out',
                'direct_out': 'direct_out',
                'blue_out': 'blue_out',
                'map_reduce_proxies': [yt_server.get_yt_client().config["proxy"]["url"]],
                'publication_status_diff_dir': 'yt_publication_status_diff_dir',
                'partners_table': partners_table
            }
        })
    return config


@pytest.yield_fixture(scope='module')
def routines_http(
        yt_server,
        config,
        datacamp_table,
        external_table,
        partners_table
):
    resources = {
        'datacamp_out_table': datacamp_table,
        'external_table': external_table,
        'config': config,
        'partners_table': partners_table,
    }
    with HttpRoutinesTestEnv(yt_server, **resources) as routines_http_env:
        yield routines_http_env


@pytest.yield_fixture(scope='module')
def prepare_publication_status_diff_http_request(routines_http, config):
    response = routines_http.post('/prepare_publication_status_diff?cluster={}&color={}&table=//home/external'.format(
        config.yt_map_reduce_proxies[0],
        'white',
    ))
    return response


@pytest.yield_fixture(scope='module')
def result_offers(yt_server, config, prepare_publication_status_diff_http_request):
    yt_client = yt_server.get_yt_client()
    return list(yt_client.read_table(yt.ypath_join(config.yt_publication_status_diff_dir, 'recent')))


def test_response(prepare_publication_status_diff_http_request):
    assert_that(prepare_publication_status_diff_http_request, HasStatus(200))


def test_yql_attribute(yt_server, prepare_publication_status_diff_http_request, config):
    yt_client = yt_server.get_yt_client()
    attr = yt_client.get(config.yt_publication_status_diff_dir + '/recent/@_yql_proto_field_offer')

    assert_that(attr, is_not(None))


def test_timestamp(yt_server, prepare_publication_status_diff_http_request, config):
    yt_client = yt_server.get_yt_client()
    attr = yt_client.get(config.yt_publication_status_diff_dir + '/recent/@timestamp')

    assert_that(attr, is_not(None))


def test_result(result_offers):
    assert len(result_offers) > 0


def test_only_generation(result_offers):
    assert_that(result_offers, is_not(HasDatacampYtUnitedOffersRows([
        {
            'business_id': 1,
            'offer_id': 'only_external',
            'shop_id': 2,
            'warehouse_id': 0,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 1,
                    'offer_id': 'only_external',
                    'shop_id': 2,
                    'warehouse_id': 0,
                },
            }),
        },
    ])))


def test_only_datacamp(result_offers):
    assert_that(result_offers, is_not(HasDatacampYtUnitedOffersRows([
        {
            'business_id': 4,
            'offer_id': 'only_datacamp_offer',
            'shop_id': 2,
            'warehouse_id': 0,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 4,
                    'offer_id': 'only_datacamp_offer',
                    'shop_id': 2,
                    'warehouse_id': 0,
                },
            }),
        },
    ])))


def test_match(result_offers):
    assert_that(result_offers, HasDatacampYtUnitedOffersRows([
        {
            'business_id': 3,
            'offer_id': 'both_offer_match',
            'shop_id': 2,
            'warehouse_id': 3,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 3,
                    'offer_id': 'both_offer_match',
                    'shop_id': 2,
                    'warehouse_id': 3,
                },
                'status': {
                    'publication': {
                        'value': DTC.PublicationStatus.PUBLISHED,
                    },
                }
            }),
        },
    ]))


def test_ignore_disabled_shops(result_offers):
    assert_that(result_offers, is_not(HasDatacampYtUnitedOffersRows([
        {
            'business_id': 1,
            'offer_id': 'shop_is_disabled',
            'shop_id': 10,
            'warehouse_id': 3,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 1,
                    'offer_id': 'shop_is_disabled',
                    'shop_id': 10,
                    'warehouse_id': 3,
                },
                'status': {
                    'publication': {
                        'value': DTC.PublicationStatus.PUBLISHED,
                    },
                }
            }),
        },
    ])))

    assert_that(result_offers, is_not(HasDatacampYtUnitedOffersRows([
        {
            'business_id': 2,
            'offer_id': 'shop_is_not_in_partners',
            'shop_id': 20,
            'warehouse_id': 3,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 2,
                    'offer_id': 'shop_is_not_in_partners',
                    'shop_id': 20,
                    'warehouse_id': 3,
                },
                'status': {
                    'publication': {
                        'value': DTC.PublicationStatus.PUBLISHED,
                    },
                }
            }),
        },
    ])))

    # Disabled & Alive - не считаем
    assert_that(result_offers, is_not(HasDatacampYtUnitedOffersRows([
        {
            'business_id': 3,
            'offer_id': 'shop_is_disabled_alive',
            'shop_id': 30,
            'warehouse_id': 3,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 3,
                    'offer_id': 'shop_is_disabled_alive',
                    'shop_id': 30,
                    'warehouse_id': 3,
                },
                'status': {
                    'publication': {
                        'value': DTC.PublicationStatus.PUBLISHED,
                    },
                }
            }),
        },
    ])))

    # Planeshift - не считаем
    assert_that(result_offers, is_not(HasDatacampYtUnitedOffersRows([
        {
            'business_id': 4,
            'offer_id': 'shop_is_in_planeshift',
            'shop_id': 40,
            'warehouse_id': 3,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 4,
                    'offer_id': 'shop_is_in_planeshift',
                    'shop_id': 40,
                    'warehouse_id': 3,
                },
                'status': {
                    'publication': {
                        'value': DTC.PublicationStatus.PUBLISHED,
                    },
                }
            }),
        },
    ])))


def test_status_changed(result_offers):
    assert_that(result_offers, HasDatacampYtUnitedOffersRows([
        {
            'business_id': 7,
            'offer_id': 'unknown2published',
            'shop_id': 7,
            'warehouse_id': 7,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 7,
                    'offer_id': 'unknown2published',
                    'shop_id': 7,
                    'warehouse_id': 7,
                },
                'status': {
                    'publication': {
                        'value': DTC.PublicationStatus.PUBLISHED,
                    },
                }
            }),
        },
    ]))

    assert_that(result_offers, HasDatacampYtUnitedOffersRows([
        {
            'business_id': 7,
            'offer_id': 'unknown2not_published',
            'shop_id': 7,
            'warehouse_id': 7,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 7,
                    'offer_id': 'unknown2not_published',
                    'shop_id': 7,
                    'warehouse_id': 7,
                },
                'status': {
                    'publication': {
                        'value': DTC.PublicationStatus.NOT_PUBLISHED,
                    },
                }
            }),
        },
    ]))


def test_verdict_changed(result_offers):
    assert_that(result_offers, HasDatacampYtUnitedOffersRows([
        {
            'business_id': 5,
            'offer_id': 'empty_verdicts',
            'shop_id': 6,
            'warehouse_id': 7,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 5,
                    'offer_id': 'empty_verdicts',
                    'shop_id': 6,
                    'warehouse_id': 7,
                },
                'resolution': {
                    'by_source': [{
                        'verdict': [{
                            'results': [{
                                'messages': [{
                                    'code': '333',
                                    'details': '{some=detail}',
                                }],
                            }],
                        }],
                    }],
                },
            }),
        },
    ]))
    assert_that(result_offers, HasDatacampYtUnitedOffersRows([
        {
            'business_id': 5,
            'offer_id': 'new_verdicts',
            'shop_id': 7,
            'warehouse_id': 0,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 5,
                    'offer_id': 'new_verdicts',
                    'shop_id': 7,
                    'warehouse_id': 0,
                },
                'resolution': {
                    'by_source': [{
                        'verdict': [{
                            'results': [{
                                'messages': [{
                                    'code': '444'
                                }],
                            }],
                        }],
                    }],
                },
            }),
        },
    ]))
    assert_that(result_offers, HasDatacampYtUnitedOffersRows([
        {
            'business_id': 5,
            'offer_id': 'old_verdicts',
            'shop_id': 8,
            'warehouse_id': 8,
            'offer': IsSerializedProtobuf(DTC.Offer, {
                'identifiers': {
                    'business_id': 5,
                    'offer_id': 'old_verdicts',
                    'shop_id': 8,
                    'warehouse_id': 8,
                },
                'resolution': {
                    'by_source': [{
                        'meta': {
                            'source': DTC.MARKET_IDX_GENERATION
                        },
                        'verdict': empty(),
                    }],
                }
            }),
        },
    ]))
    # we can't test verdicts merging, because yatf resource of datacamp out table is dynamic
