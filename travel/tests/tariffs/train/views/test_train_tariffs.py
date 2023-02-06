# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import date, datetime, timedelta, time

import mock
import pytest
from django.test import Client
from django.utils.http import urlencode
from hamcrest import assert_that, has_entries, contains_string, has_item, has_property, string_contains_in_order
from marshmallow.utils import isoformat
from six.moves.urllib_parse import urlparse, parse_qs

from common.apps.train_order.enums import CoachType
from common.dynamic_settings.default import conf
from common.models.geo import Country, Settlement, StationExpressAlias, StationMajority
from common.models.transport import TransportType, TrainPseudoStationMap
from common.tester.factories import (
    create_settlement, create_station, create_thread, create_currency, create_deluxe_train, create_country
)
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting, replace_setting, replace_dynamic_setting_key
from common.utils.date import MSK_TIMEZONE
from common.utils.title_generator import build_default_title_common, DASH
from travel.proto.dicts.trains.station_express_alias_pb2 import TStationExpressAlias
from travel.rasp.train_api.tariffs.train.base import query, query_result_manager
from travel.rasp.train_api.tariffs.train.base.price_logging import yt_dynamic_tariffs_log
from travel.rasp.train_api.tariffs.train.base.utils import get_point_express_code
from travel.rasp.train_api.tariffs.train.factories.base import create_www_setting_cache_timeouts
from travel.rasp.train_api.tariffs.train.factories.im import ImTrainPricingResponseFactory
from travel.rasp.train_api.tariffs.train.im.send_query import TRAIN_PRICING_ENDPOINT, ImTariffsResult
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_partners.ufs.reserve_tickets import ADVERT_DOMAIN
from travel.rasp.train_api.train_purchase.core.factories import ClientContractsFactory, ClientContractFactory

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


@pytest.fixture
def trainslist_mock(httpretty):
    mock_im(httpretty, TRAIN_PRICING_ENDPOINT, json=ImTrainPricingResponseFactory(**{
        'DestinationCode': '2000000',
        'DestinationStationCode': '2006004',
        'OriginCode': '2004000',
        'OriginStationCode': '2004001',
        'Trains': [{
            'DepartureDateTime': '2016-05-15T17:26:00',
            'DestinationName': 'С-ПЕТЕР-ГЛ',
            'DestinationStationCode': 'real_to',
            'OriginName': 'МОСКВА ОКТ',
            'OriginStationCode': 'real_from',
            'TrainNumber': '020У',
            'TrainNumberToGetRoute': '020У',
            'DisplayTrainNumber': '020У',
            'CarGroups': [
                {
                    'CarType': CoachType.PLATZKARTE,
                    'MaxPrice': 1929.2,
                    'MinPrice': 1212.8,
                    'TotalPlaceQuantity': 138,
                },
                {
                    'CarType': CoachType.COMPARTMENT,
                    'MaxPrice': 3708.0,
                    'MinPrice': 3075.9,
                    'TotalPlaceQuantity': 34,
                }
            ],
            'Provider': 'P1',
        }]
    }))
    return httpretty


@replace_now('2016-04-20')
@pytest.mark.usefixtures('worker_cache_stub')
@pytest.mark.parametrize('reason_for_missing_prices', (True, False))
@pytest.mark.parametrize('protobufs_setting_value', (True, False))
def test_init_and_poll_query(reason_for_missing_prices, trainslist_mock, worker_stub,
                             protobufs_setting_value, protobuf_data_provider):
    with replace_dynamic_setting('TRAIN_PURCHASE_FEATURE_REASON_FOR_MISSING_PRICES', reason_for_missing_prices), \
         replace_dynamic_setting_key('TRAIN_BACKEND_USE_PROTOBUFS', 'alias', protobufs_setting_value):  # noqa: E126
        create_www_setting_cache_timeouts()
        create_currency(code='RUR', iso_code='RUB')

        t_train = TransportType.objects.get(pk=TransportType.TRAIN_ID)

        piter = create_settlement(title='Питер', country=Country.RUSSIA_ID, time_zone=MSK_TIMEZONE)
        pseudo_station_from = create_station(__={'codes': {'express': 'pseudo_from'}}, settlement=piter,
                                             t_type=t_train, majority=StationMajority.EXPRESS_FAKE_ID)
        station_from = create_station(__={'codes': {'express': 'real_from'}}, settlement=piter,
                                      t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID)
        station_to = create_station(__={'codes': {'express': 'real_to'}}, settlement=Settlement.MOSCOW_ID,
                                    t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID)

        thread = create_thread(
            number='020У',
            t_type=t_train,
            tz_start_time=time(17, 26),
            schedule_v1=[
                [None, 0, station_from],
                [300, None, station_to],
            ],
            __={'calculate_noderoute': True},
            title_common=build_default_title_common(t_train, [station_from, station_to]),
            title='Питер - Москва'
        )

        TrainPseudoStationMap.objects.create(number='020У', pseudo_station=pseudo_station_from, station=station_from)
        if protobufs_setting_value:
            for alias_proto in (
                TStationExpressAlias(StationId=station_from.id, Alias='МОСКВА ОКТ'),
                TStationExpressAlias(StationId=station_to.id, Alias='С-ПЕТЕР-ГЛ')
            ):
                protobuf_data_provider.alias_repo.add(alias_proto.SerializeToString())
        else:
            StationExpressAlias.objects.create(station=station_from, alias='МОСКВА ОКТ')
            StationExpressAlias.objects.create(station=station_to, alias='С-ПЕТЕР-ГЛ')

        response = Client().get('/ru/api/segments/train-tariffs/?{}'.format(
            urlencode({
                'pointFrom': 'c{}'.format(piter.id),
                'pointTo': 'c{}'.format(Settlement.MOSCOW_ID),
                'date': '2016-05-15',
                'national_version': 'ru'
            }, doseq=True)
        ))
        assert worker_stub.call_count == 1
        assert response.status_code == 200
        assert response.data == {
            'segments': [],
            'querying': True
        }

        response = Client().get('/ru/api/segments/train-tariffs/?{}'.format(
            urlencode({
                'pointFrom': 'c{}'.format(piter.id),
                'pointTo': 'c{}'.format(Settlement.MOSCOW_ID),
                'date': '2016-05-15',
                'national_version': 'ru'
            }, doseq=True)
        ))
        assert worker_stub.call_count == 1
        assert response.status_code == 200
        query_again_result = json.loads(response.content)

        response = Client().get('/ru/api/segments/train-tariffs/poll/?{}'.format(
            urlencode({
                'pointFrom': 'c{}'.format(piter.id),
                'pointTo': 'c{}'.format(Settlement.MOSCOW_ID),
                'date': '2016-05-15',
                'national_version': 'ru'
            }, doseq=True)
        ))
        assert worker_stub.call_count == 1
        assert response.status_code == 200
        poll_result = response.data

        assert poll_result == query_again_result
        assert not poll_result['querying']

        assert len(poll_result['segments']) == 1

        segment = poll_result['segments'][0]

        assert_that(segment['tariffs'], has_entries({
            'electronicTicket': True,
            'classes': has_entries({
                'compartment': has_entries({
                    'price': {'currency': 'RUB', 'value': 3075.9},
                    'seats': 34, 'severalPrices': True,
                    'orderUrl': contains_string('cls=compartment'),
                    'trainOrderUrl': contains_string('provider=P1'),
                }),
                'platzkarte': has_entries({
                    'price': {'currency': 'RUB', 'value': 1212.8},
                    'seats': 138, 'severalPrices': True,
                    'orderUrl': contains_string('cls=platzkarte'),
                    'trainOrderUrl': contains_string('provider=P1'),
                })
            }),
        }))

        assert_that(segment, has_entries({
            'thread': has_entries(uid=thread.uid, number='020У'),
            'number': '020У',
            'title': thread.title,
            'ufsTitle': 'МОСКВА ОКТ {} С-ПЕТЕР-ГЛ'.format(DASH),
            'canSupplySegments': True,
            'stationFrom': has_entries({
                'id': station_from.id,
                'codes': {'express': 'real_from'}
            }),
            'stationTo': has_entries({
                'id': station_to.id,
                'codes': {'express': 'real_to'}
            }),
            'company': has_entries('ufsTitle', 'ФПК'),
            'provider': 'P1',
        }))

        assert len(trainslist_mock.latest_requests) == 1
        assert trainslist_mock.last_request.parsed_body['Origin'] == 'pseudo_from'
        assert trainslist_mock.last_request.parsed_body['Destination'] == 'real_to'


@replace_now('2016-04-20')
@pytest.mark.usefixtures('worker_cache_stub', 'worker_stub')
@pytest.mark.parametrize(
    'allow_international_routes, expected_train_order_url',
    [
        ('false', 'https://ufs-online.ru/kupit-zhd-bilety/pseudo_from/real_to'),
        ('true', '/order/?toId=c213'),
    ],
)
@pytest.mark.parametrize('protobufs_setting_value', (True, False))
def test_init_and_poll_international_query(trainslist_mock, allow_international_routes, expected_train_order_url,
                                           protobufs_setting_value, protobuf_data_provider):
    with replace_dynamic_setting_key('TRAIN_BACKEND_USE_PROTOBUFS', 'alias', protobufs_setting_value):
        create_www_setting_cache_timeouts()
        create_currency(code='RUR', iso_code='RUB')

        t_train = TransportType.objects.get(pk=TransportType.TRAIN_ID)
        germany = create_country(title='Германия', id=96, code='DE')
        berlin = create_settlement(title='Берлин', country=germany, time_zone=MSK_TIMEZONE)
        pseudo_station_from = create_station(__={'codes': {'express': 'pseudo_from'}}, settlement=berlin,
                                             t_type=t_train, majority=StationMajority.EXPRESS_FAKE_ID)
        station_from = create_station(__={'codes': {'express': 'real_from'}}, settlement=berlin,
                                      t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID)
        station_to = create_station(__={'codes': {'express': 'real_to'}}, settlement=Settlement.MOSCOW_ID,
                                    t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID)

        create_thread(
            number='020У',
            t_type=t_train,
            tz_start_time=time(17, 26),
            schedule_v1=[
                [None, 0, station_from],
                [300, None, station_to],
            ],
            __={'calculate_noderoute': True},
            title_common=build_default_title_common(t_train, [station_from, station_to]),
            title='Берлин - Москва'
        )

        TrainPseudoStationMap.objects.create(number='020У', pseudo_station=pseudo_station_from, station=station_from)
        if protobufs_setting_value:
            for alias_proto in (
                TStationExpressAlias(StationId=station_from.id, Alias='МОСКВА ОКТ'),
                TStationExpressAlias(StationId=station_to.id, Alias='БЕРЛИН-ГЛ')
            ):
                protobuf_data_provider.alias_repo.add(alias_proto.SerializeToString())
        else:
            StationExpressAlias.objects.create(station=station_from, alias='МОСКВА ОКТ')
            StationExpressAlias.objects.create(station=station_to, alias='БЕРЛИН-ГЛ')

        response = Client().get('/ru/api/segments/train-tariffs/?{}'.format(
            urlencode({
                'pointFrom': 'c{}'.format(berlin.id),
                'pointTo': 'c{}'.format(Settlement.MOSCOW_ID),
                'date': '2016-05-15',
                'national_version': 'ru',
                'allow_international_routes': allow_international_routes,
            }, doseq=True)
        ))

        assert response.status_code == 200
        assert response.data == {
            'segments': [],
            'querying': True
        }

        response = Client().get('/ru/api/segments/train-tariffs/?{}'.format(
            urlencode({
                'pointFrom': 'c{}'.format(berlin.id),
                'pointTo': 'c{}'.format(Settlement.MOSCOW_ID),
                'date': '2016-05-15',
                'national_version': 'ru',
                'allow_international_routes': allow_international_routes,
            }, doseq=True)
        ))

        assert response.status_code == 200
        query_again_result = json.loads(response.content)

        response = Client().get('/ru/api/segments/train-tariffs/poll/?{}'.format(
            urlencode({
                'pointFrom': 'c{}'.format(berlin.id),
                'pointTo': 'c{}'.format(Settlement.MOSCOW_ID),
                'date': '2016-05-15',
                'national_version': 'ru',
                'allow_international_routes': allow_international_routes,
            }, doseq=True)
        ))

        assert response.status_code == 200
        poll_result = response.data

        assert poll_result == query_again_result
        assert not poll_result['querying']
        assert_that(poll_result['segments'][0]['tariffs']['classes'], has_entries({
            'compartment': has_entries({'trainOrderUrl': contains_string(expected_train_order_url)}),
            'platzkarte': has_entries({'trainOrderUrl': contains_string(expected_train_order_url)}),
        }))


@replace_now('2016-04-20')
def test_we_dont_make_queries_if_too_many_dates(trainslist_mock):
    piter = create_settlement(title='Питер', country=Country.RUSSIA_ID, time_zone=MSK_TIMEZONE)
    response = Client().get('/ru/api/segments/train-tariffs/?{}'.format(
        urlencode({
            'pointFrom': 'c{}'.format(piter.id),
            'pointTo': 'c{}'.format(Settlement.MOSCOW_ID),
            'date': ['2016-05-17', '2016-05-18', '2016-05-19'],
            'national_version': 'ru'
        }, doseq=True)
    ))

    assert response.status_code == 200
    assert json.loads(response.content) == {
        'segments': [],
        'querying': False,
    }


@replace_now('2016-04-20')
def test_use_railway_tz_only():
    create_www_setting_cache_timeouts()
    create_currency(code='RUR', iso_code='RUB')

    station_from = create_station(__={'codes': {'express': '1111'}}, t_type='train', time_zone='Asia/Yekaterinburg',
                                  settlement=create_settlement(time_zone='Asia/Yekaterinburg'))
    station_to = create_station(__={'codes': {'express': '2222'}}, settlement=Settlement.MOSCOW_ID, t_type='train')

    create_thread(
        __={'calculate_noderoute': True},
        t_type='train',
        schedule_v1=[
            [None, 0, station_from],
            [300, None, station_to],
        ]
    )

    if conf.TRAIN_PURCHASE_FEATURE_REASON_FOR_MISSING_PRICES:
        do_im_query_patch = mock.patch.object(query_result_manager, 'do_im_query',
                                              return_value=ImTariffsResult(None, ImTariffsResult.STATUS_SUCCESS, []))
    else:
        do_im_query_patch = mock.patch.object(query, 'do_im_query',
                                              return_value=ImTariffsResult(None, ImTariffsResult.STATUS_SUCCESS, []))

    with do_im_query_patch as m_do_im_query:
        response = Client().get('/ru/api/segments/train-tariffs/', {
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
            'date': ['2016-05-17'],
            'national_version': 'ru',
            'useRailwayTZ': True
        })

    assert m_do_im_query.call_count == 1
    assert response.status_code == 200

    with do_im_query_patch as m_do_im_query:
        response = Client().get('/ru/api/segments/train-tariffs/', {
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
            'date': ['2016-05-17'],
            'national_version': 'ru'
        })

    assert m_do_im_query.call_count == 2
    assert response.status_code == 200


@replace_now('2000-01-01')
@replace_dynamic_setting('TRAIN_PURCHASE_FEATURE_REASON_FOR_MISSING_PRICES', False)
@mock.patch.object(query, 'do_im_query',
                   return_value=ImTariffsResult(None, ImTariffsResult.STATUS_SUCCESS, []))
@pytest.mark.parametrize('path, query, expected_count', (
    ('/ru/api/segments/train-tariffs/', {}, 4),
    ('/ru/api/segments/train-tariffs/poll/', {}, 0),
))
def test_no_queries_on_poll(m_do_ufs_query, path, query, expected_count):
    station_from = create_station(__={'codes': {'express': '1111'}}, t_type='train')
    station_to = create_station(__={'codes': {'express': '2222'}}, t_type='train')

    response = Client().get(path, dict({
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'startTime': '2000-01-01T00:00:00+00:00',
        'endTime': '2000-01-04T00:00:00+00:00',
        'national_version': 'ru'
    }, **query))

    assert m_do_ufs_query.call_count == expected_count
    assert response.status_code == 200


@replace_now('2000-01-01')
@replace_dynamic_setting('TRAIN_PURCHASE_FEATURE_REASON_FOR_MISSING_PRICES', True)
@mock.patch.object(query_result_manager, 'do_im_query',
                   return_value=ImTariffsResult(None, ImTariffsResult.STATUS_SUCCESS, []))
@pytest.mark.parametrize('path, query, expected_count', (
    ('/ru/api/segments/train-tariffs/', {}, 4),
    ('/ru/api/segments/train-tariffs/poll/', {}, 0),
))
def test_no_queries_on_poll_2(m_do_im_query, path, query, expected_count):
    station_from = create_station(__={'codes': {'express': '1111'}}, t_type='train')
    station_to = create_station(__={'codes': {'express': '2222'}}, t_type='train')

    response = Client().get(path, dict({
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'startTime': '2000-01-01T00:00:00+00:00',
        'endTime': '2000-01-04T00:00:00+00:00',
        'national_version': 'ru'
    }, **query))

    assert m_do_im_query.call_count == expected_count
    assert response.status_code == 200


@pytest.mark.parametrize('path', (
    '/ru/api/segments/train-tariffs/',
    '/ru/api/segments/train-tariffs/poll/',
    '/ru/api/segments/min-tariffs/',
))
def test_invalid_queries(path):
    response = Client().get(path)

    assert response.status_code == 400
    assert_that(response.data, has_entries('errors',
                                           has_entries('national_version', ['Missing data for required field.'])))


def _create_im_response_msk_spb():
    return ImTrainPricingResponseFactory(**{
        'Trains': [{
            'DepartureDateTime': '2017-09-30T17:26:00',
            'ArrivalDateTime': '2017-10-01T02:09:00',
            'TrainNumber': '059А',
            'TrainNumberToGetRoute': '059',
            'DisplayTrainNumber': '058*А',
            'CarGroups': [
                {
                    'CarType': CoachType.PLATZKARTE,
                    'MaxPrice': 1929.2,
                    'MinPrice': 1212.8,
                    'TotalPlaceQuantity': 138,
                },
                {
                    'CarType': CoachType.COMPARTMENT,
                    'MaxPrice': 3708.0,
                    'MinPrice': 3075.9,
                    'TotalPlaceQuantity': 34,
                }
            ],
            'TrainDescription': 'СК ФИРМ',
            'TrainName': 'Волга',
            'OriginName': 'С-ПЕТЕР-ГЛ',
            'DestinationName': 'Н.НОВГОРОД М',
            'DestinationNames': ['Н.НОВГОРОД М'],
            'Provider': 'P1',
        }]
    })


WIZARD_RESPONSE_SPB_MSK_UPDATED = '''
{
  "search_url": "https:\/\/rasp.yandex.ru\/search\/train\/?fromId=c2&toId=c213&when=2017-09-30",
  "minimum_price": {
    "currency": "RUB",
    "value": 1346.21
  },
  "segments": [
    {
      "arrival": {
        "settlement": {
          "key": "c213",
          "title": "Москва"
        },
        "station": {
          "key": "s2000001",
          "title": "Курский вокзал"
        },
        "local_datetime": {
          "timezone": "Europe\/Moscow",
          "value": "2017-10-01T02:10:00+03:00"
        }
      },
      "facilities": null,
      "train": {
        "is_suburban": false,
        "has_dynamic_pricing": true,
        "title": "Санкт-Петербург — Нижний Новгород",
        "display_number": "059А",
        "brand": {
          "is_deluxe": true,
          "id": 146,
          "is_high_speed": false,
          "title": "Волга"
        },
        "two_storey": false,
        "number": "059Я",
        "first_country_code": "RU",
        "last_country_code": "RU",
        "thread_type": "basic",
        "coach_owners": ["ФПК"],
        "provider": "P1",
        "raw_train_name": "Волга"
      },
      "order_url": "https:\/\/rasp.yandex.ru\/order\/?fromId=c2&toId=c213&when=2017-09-30&number=059%D0%90&time=19:15",
      "order_touch_url":
      "https:\/\/t.rasp.yandex.ru\/order\/?fromId=c2&toId=c213&when=2017-09-30&number=059%D0%90&time=19:15",
      "places": {
        "records": [
          {
            "count": 34,
            "max_seats_in_the_same_car": 34,
            "price": {
              "currency": "RUB",
              "value": 3414.25
            },
            "price_details": {
              "fee": 338.35,
              "service_price": 698.0,
              "ticket_price": 3075.90,
              "several_prices": true
            },
            "coach_type": "compartment",
            "service_class": "2Л"
          },
          {
            "count": 138,
            "max_seats_in_the_same_car": 60,
            "price": {
              "currency": "RUB",
              "value": 1346.21
            },
            "price_details":  {
              "fee": 133.41,
              "service_price": 138.2,
              "ticket_price": 1212.80,
              "several_prices": true
            },
            "coach_type": "platzkarte",
            "service_class": "2Л"
          }
        ],
        "electronic_ticket": true,
        "updated_at": {
          "timezone": "UTC",
          "value": "2017-09-30T09:25:58.763418+00:00"
        }
      },
      "minimum_price": {
        "currency": "RUB",
        "value": 1346.21
      },
      "duration": 415.0,
      "departure": {
        "settlement": {
          "key": "c2",
          "title": "Санкт-Петербург"
        },
        "station": {
          "key": "s9602494",
          "title": "Московский вокзал"
        },
        "local_datetime": {
          "timezone": "Europe\/Moscow",
          "value": "2017-09-30T19:15:00+03:00"
        }
      }
    }
  ],
  "found_departure_date": "2017-09-30",
  "path_items": [
    {
      "url": "https:\/\/testing.morda-front.rasp.common.yandex.ru\/search\/train\/?fromId=c2&toId=c213&when=2017-09-30",
      "text": "testing.morda-front.rasp.common.yandex.ru",
      "touch_url": "https:\/\/testing.touch.rasp.common.yandex.ru\/search\/train\/?fromId=c2&toId=c213&when=2017-09-30"
    }
  ],
  "query": {
    "departure_point": {
      "key": "c2",
      "title": "Санкт-Петербург"
    },
    "departure_date": "2017-09-30",
    "order_by": "departure",
    "arrival_point": {
      "key": "c213",
      "title": "Москва"
    },
    "language": "ru"
  },
  "search_touch_url": "https:\/\/t.rasp.yandex.ru\/search\/train\/?fromId=c2&toId=c213&when=2017-09-30"
}
'''


@replace_now('2017-09-01')
@pytest.mark.usefixtures('worker_cache_stub')
@mock.patch.object(yt_dynamic_tariffs_log, 'log')
@pytest.mark.parametrize('force_ufs_order, expected_owner', [
    (True, 'ufs'),
    (False, 'trains'),
])
@pytest.mark.parametrize('reason_for_missing_prices', (True, False))
def test_init_and_poll_and_log_query_im(
    m_log, httpretty, worker_stub, force_ufs_order, expected_owner, reason_for_missing_prices
):
    with replace_dynamic_setting('TRAIN_PURCHASE_FEATURE_REASON_FOR_MISSING_PRICES', reason_for_missing_prices):
        ClientContractsFactory(contracts=[ClientContractFactory(partner_commission_sum=0, partner_commission_sum2=0)])
        mock_im(httpretty, TRAIN_PRICING_ENDPOINT, json=_create_im_response_msk_spb())
        create_www_setting_cache_timeouts()
        create_currency(code='RUR', iso_code='RUB')

        t_train = TransportType.objects.get(pk=TransportType.TRAIN_ID)

        piter = create_settlement(
            title='Питер', country=Country.RUSSIA_ID, time_zone=MSK_TIMEZONE, _geo_id=204,
        )
        create_station(
            title='first', __={'codes': {'express': '2000000'}}, settlement=Settlement.MOSCOW_ID,
            t_type=t_train, majority=StationMajority.EXPRESS_FAKE_ID, country=Country.RUSSIA_ID,
        )
        station_to = create_station(
            title='second', __={'codes': {'express': '2000001'}}, settlement=Settlement.MOSCOW_ID,
            t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID, id=2000001, country=Country.RUSSIA_ID,
        )
        station_from = create_station(
            title='third', __={'codes': {'express': '2004001'}}, settlement=piter,
            t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID, id=9602494, country=Country.RUSSIA_ID,
        )

        # беспересадочная нитка должна игнорироваться в пользу основной
        create_thread(
            number='059А',
            t_type=t_train,
            tz_start_time=time(17, 26),
            schedule_v1=[
                [None, 0, station_from],
                [300, None, station_to],
            ],
            __={'calculate_noderoute': True},
            title_common=build_default_title_common(t_train, [station_from, station_to]),
            title='беспересадочная Финляндия - Москва',
            type='through_train'
        )
        thread = create_thread(
            number='059А',
            t_type=t_train,
            tz_start_time=time(17, 26),
            schedule_v1=[
                [None, 0, station_from],
                [300, None, station_to],
            ],
            __={'calculate_noderoute': True},
            title_common=build_default_title_common(t_train, [station_from, station_to]),
            title='Питер - Москва'
        )
        params = {
            'pointFrom': piter.point_key,
            'pointTo': station_to.point_key,
            'startTime': '2017-09-30T10:00:00+03:00',
            'endTime': '2017-09-30T20:00:00+03:00',
            'partner': 'im',
            'national_version': 'ru',
            'include_price_fee': True,
            'priceExpId': 'someExpId',
            'service': 'someSource',
            'utmSource': 'someUtmSource',
            'yandex_uid': 'someUserUid',
            'forceUfsOrder': force_ufs_order,
            'icookie': 'someCookie',
        }

        headers = {
            'HTTP_X_YA_ICOOKIE': 'notThatCookie',
            'HTTP_X_REQUEST_ID': 'someReqId',
            'HTTP_X_YA_USER_DEVICE': 'some device',
        }

        response = Client().get('/ru/api/segments/train-tariffs/', params, **headers)

        assert worker_stub.call_count == 1
        assert response.status_code == 200
        assert response.data == {
            'segments': [],
            'querying': True
        }
        assert not m_log.call_count

        response = Client().get('/ru/api/segments/train-tariffs/', params, **headers)

        assert worker_stub.call_count == 1
        assert response.status_code == 200
        after_finishing_background_job_result = response.data

        assert m_log.call_count == 2
        decoded_log_0 = {k: v.decode('utf8') for k, v in m_log.call_args_list[0][0][0].items()}
        decoded_log_1 = {k: v.decode('utf8') for k, v in m_log.call_args_list[1][0][0].items()}
        assert_that(decoded_log_0, has_entries({
            'adults': '1',
            'arrival': '2017-10-01 02:09:00+03:00',
            'class_compartment_price': '3414.25 RUB',
            'class_compartment_seats': '34',
            'date_forward': '2017-09-30',
            'departure': '2017-09-30 17:26:00+03:00',
            'duration': '523',
            'fee_percent': '0.11',
            'force_ufs_order': str(force_ufs_order),
            'key': 'train 059А 16',
            'object_from_id': str(station_from.id),
            'object_from_title': station_from.title,
            'object_from_type': 'Station',
            'object_to_id': str(station_to.id),
            'object_to_title': station_to.title,
            'object_to_type': 'Station',
            'partner': 'im',
            'price_exp_id': 'someExpId',
            'route_uid': '059А',
            'service': 'someSource',
            'timestamp': '2017-09-01 00:00:00',
            'timezone': '+0300',
            'tskv_format': 'rasp-tariffs-log',
            'type': 'train',
            'utm_source': 'someUtmSource',
            'yandex_uid': 'someUserUid',
        }))
        assert_that(decoded_log_1, has_entries({
            'adults': '1',
            'arrival': '2017-10-01 02:09:00+03:00',
            'class_platzkarte_price': '1346.21 RUB',
            'class_platzkarte_seats': '138',
            'date_forward': '2017-09-30',
            'departure': '2017-09-30 17:26:00+03:00',
            'duration': '523',
            'fee_percent': '0.11',
            'force_ufs_order': str(force_ufs_order),
            'key': 'train 059А 16',
            'object_from_id': str(station_from.id),
            'object_from_title': station_from.title,
            'object_from_type': 'Station',
            'object_to_id': str(station_to.id),
            'object_to_title': station_to.title,
            'object_to_type': 'Station',
            'partner': 'im',
            'price_exp_id': 'someExpId',
            'route_uid': '059А',
            'service': 'someSource',
            'timestamp': '2017-09-01 00:00:00',
            'timezone': '+0300',
            'tskv_format': 'rasp-tariffs-log',
            'type': 'train',
            'utm_source': 'someUtmSource',
            'yandex_uid': 'someUserUid',
        }))

        response = Client().get('/ru/api/segments/train-tariffs/poll/', params, **headers)
        assert worker_stub.call_count == 1
        assert response.status_code == 200
        poll_result = response.data

        assert poll_result == after_finishing_background_job_result
        assert not poll_result['querying']

        assert len(poll_result['segments']) == 1
        segment = poll_result['segments'][0]

        assert_that(segment['tariffs'], has_entries({
            'electronicTicket': True,
            'classes': has_entries({
                'compartment': has_entries({
                    'price': {'currency': 'RUB', 'value': 3414.25},
                    'seats': 34, 'severalPrices': True,
                    'orderUrl': contains_string('cls=compartment'),
                    'trainOrderUrl':
                        contains_string('coachType=compartment' if not force_ufs_order else 'kupit-zhd-bilety'),
                    'trainOrderUrlOwner': expected_owner,
                }),
                'platzkarte': has_entries({
                    'price': {'currency': 'RUB', 'value': 1346.21},
                    'seats': 138, 'severalPrices': True,
                    'orderUrl': contains_string('cls=platzkarte'),
                    'trainOrderUrl':
                        contains_string('coachType=platzkarte' if not force_ufs_order else 'kupit-zhd-bilety'),
                    'trainOrderUrlOwner': expected_owner,
                })
            }),
        }))

        assert_that(segment, has_entries({
            'thread': has_entries(uid=thread.uid, number='059А'),
            'number': '058А',
            'title': thread.title,
            'ufsTitle': 'С-ПЕТЕР-ГЛ {} Н.НОВГОРОД М'.format(DASH),
            'rawTrainCategory': 'СК ФИРМ',
            'rawTrainName': 'Волга',
            'canSupplySegments': True,
            'isSuburban': False,
            'stationFrom': has_entries({'id': station_from.id, 'ufsTitle': None}),
            'stationTo': has_entries({'id': station_to.id, 'ufsTitle': None}),
            'company': has_entries('ufsTitle', 'ФПК'),
        }))

        order_url = urlparse(segment['tariffs']['classes']['platzkarte']['trainOrderUrl'])
        if force_ufs_order:
            assert order_url.path == '/kupit-zhd-bilety/{}/{}'.format(get_point_express_code(piter),
                                                                      get_point_express_code(station_to))
            assert_that(parse_qs(order_url.query.encode()), has_entries({
                'date': ['30.09.2017'],
                'trainNumber': ['058А'.encode('utf-8')],
                'domain': [ADVERT_DOMAIN],
            }))
        else:
            assert order_url.path == '/order/'
            assert_that(parse_qs(order_url.query.encode()), has_entries({
                'fromId': [piter.point_key],
                'toId': [station_to.point_key],
                'number': ['059А'.encode('utf-8')],
                'when': ['2017-09-30'],
                'time': ['17:26']
            }))

        utc_departure = datetime(2017, 9, 30, 17, 26) - timedelta(hours=3)  # DepartureDateTime
        assert segment['departure'] == isoformat(utc_departure)
        assert segment['arrival'] == isoformat(utc_departure + timedelta(minutes=523))  # TripDuration
        utc_arrival = datetime(2017, 10, 1, 2, 9) - timedelta(hours=3)  # ArrivalDateTime
        assert segment['arrival'] == isoformat(utc_arrival)

        assert_that(httpretty.latest_requests, has_item(has_property('parsed_body', has_entries({
            'Origin': '2004001',
            'Destination': '2000001',
            'CarGrouping': 'DontGroup',
            'DepartureDate': '2017-09-30T00:00:00'
        }))))


@replace_now('2017-09-01')
@pytest.mark.usefixtures('worker_cache_stub')
@replace_dynamic_setting('TRAIN_PURCHASE_WIZARD_CONFIDENCE_MINUTES', 15)
@replace_setting('TRAIN_WIZARD_API_DIRECTION_HOST', 'train-wizard-api.net')
@replace_dynamic_setting('TRAIN_PURCHASE_EXPERIMENTAL_DELTA_FEE', '0.03')
@pytest.mark.parametrize('price_exp_id, expected_fee_percent, expected_compartment_price, expected_platzkarte_price', [
    ('someExpId', 0.0944, 3366.26, 1327.29),
    (None, 0.11, 3414.25, 1346.21),
])
@mock.patch.object(yt_dynamic_tariffs_log, 'log')
@pytest.mark.parametrize('reason_for_missing_prices', (True, False))
def test_init_and_poll_query_with_experiment(m_log, httpretty, worker_stub, price_exp_id, expected_fee_percent,
                                             expected_compartment_price, expected_platzkarte_price,
                                             reason_for_missing_prices):
    with replace_dynamic_setting('TRAIN_PURCHASE_FEATURE_REASON_FOR_MISSING_PRICES', reason_for_missing_prices):
        ClientContractsFactory(contracts=[ClientContractFactory(partner_commission_sum=0, partner_commission_sum2=0)])
        mock_im(httpretty, TRAIN_PRICING_ENDPOINT, json=_create_im_response_msk_spb())
        create_www_setting_cache_timeouts()
        create_currency(code='RUR', iso_code='RUB')

        t_train = TransportType.objects.get(pk=TransportType.TRAIN_ID)

        piter = create_settlement(title='Питер', country=Country.RUSSIA_ID, time_zone=MSK_TIMEZONE)
        create_station(__={'codes': {'express': '2000000'}}, settlement=Settlement.MOSCOW_ID,
                       t_type=t_train, majority=StationMajority.EXPRESS_FAKE_ID)
        station_to = create_station(__={'codes': {'express': '2000001'}}, settlement=Settlement.MOSCOW_ID,
                                    t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID, id=2000001)
        station_from = create_station(__={'codes': {'express': '2004001'}}, settlement=piter,
                                      t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID)

        create_thread(
            number='059А',
            t_type=t_train,
            tz_start_time=time(17, 26),
            schedule_v1=[
                [None, 0, station_from],
                [300, None, station_to],
            ],
            __={'calculate_noderoute': True},
            title_common=build_default_title_common(t_train, [station_from, station_to]),
            title='Питер - Москва'
        )
        params = {
            'pointFrom': piter.point_key,
            'pointTo': station_to.point_key,
            'startTime': '2017-09-30T10:00:00+03:00',
            'endTime': '2017-09-30T20:00:00+03:00',
            'partner': 'im',
            'national_version': 'ru',
            'include_price_fee': True,
            'service': 'someSource',
            'utmSource': 'someUtmSource',
            'yandex_uid': 'someUserUid',
        }

        if price_exp_id:
            params['priceExpId'] = price_exp_id

        response = Client().get('/ru/api/segments/train-tariffs/', params)

        assert worker_stub.call_count == 1
        assert response.status_code == 200
        assert response.data == {
            'segments': [],
            'querying': True
        }

        httpretty.register_uri(
            httpretty.GET,
            'https://train-wizard-api.net/searcher/public-api/open_direction/',
            body=WIZARD_RESPONSE_SPB_MSK_UPDATED
        )
        response = Client().get('/ru/api/segments/train-tariffs/', params)

        assert worker_stub.call_count == 1
        assert response.status_code == 200
        poll_result = response.data
        segment = poll_result['segments'][0]
        assert_that(segment['tariffs'], has_entries({
            'classes': has_entries({
                'compartment': has_entries({
                    'price': {'currency': 'RUB', 'value': expected_compartment_price},
                }),
                'platzkarte': has_entries({
                    'price': {'currency': 'RUB', 'value': expected_platzkarte_price},
                })
            }),
        }))
        assert m_log.call_args_list[0][0][0]['fee_percent'] == str(expected_fee_percent)
        assert m_log.call_args_list[1][0][0]['fee_percent'] == str(expected_fee_percent)


@replace_now('2017-09-01')
@replace_dynamic_setting('TRAIN_PURCHASE_WIZARD_CONFIDENCE_MINUTES', 15)
@replace_setting('TRAIN_WIZARD_API_DIRECTION_HOST', 'train-wizard-api.net')
@pytest.mark.usefixtures('worker_cache_stub')
@pytest.mark.parametrize('reason_for_missing_prices', (True, False))
def test_init_and_poll_query_im_to_msk(httpretty, worker_stub, reason_for_missing_prices):
    with replace_dynamic_setting('TRAIN_PURCHASE_FEATURE_REASON_FOR_MISSING_PRICES', reason_for_missing_prices):
        mock_im(httpretty, TRAIN_PRICING_ENDPOINT, json=_create_im_response_msk_spb())
        create_www_setting_cache_timeouts()
        create_currency(code='RUR', iso_code='RUB')

        t_train = TransportType.objects.get(pk=TransportType.TRAIN_ID)

        piter = create_settlement(title='Питер', country=Country.RUSSIA_ID, time_zone=MSK_TIMEZONE)
        create_station(__={'codes': {'express': '2000000'}}, settlement=Settlement.MOSCOW_ID,
                       t_type=t_train, majority=StationMajority.EXPRESS_FAKE_ID)
        station_to = create_station(__={'codes': {'express': '2000001'}}, settlement=Settlement.MOSCOW_ID,
                                    t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID, id=2000001)
        station_from = create_station(__={'codes': {'express': '2004001'}}, settlement=piter,
                                      t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID)

        create_thread(
            number='059А',
            t_type=t_train,
            tz_start_time=time(17, 26),
            schedule_v1=[
                [None, 0, station_from],
                [300, None, station_to],
            ],
            __={'calculate_noderoute': True},
            title_common=build_default_title_common(t_train, [station_from, station_to]),
            title='Питер - Москва'
        )
        params = {
            'pointFrom': 'c{}'.format(piter.id),
            'pointTo': 'c{}'.format(Settlement.MOSCOW_ID),
            'startTime': '2017-09-30T10:00:00+03:00',
            'endTime': '2017-09-30T20:00:00+03:00',
            'partner': 'im',
            'national_version': 'ru'
        }

        response = Client().get('/ru/api/segments/train-tariffs/', params)

        assert worker_stub.call_count == 1
        assert response.status_code == 200

        httpretty.register_uri(
            httpretty.GET,
            'https://train-wizard-api.net/searcher/public-api/open_direction/',
            body=WIZARD_RESPONSE_SPB_MSK_UPDATED
        )
        response = Client().get('/ru/api/segments/train-tariffs/', params)

        assert worker_stub.call_count == 1
        assert response.status_code == 200
        after_finishing_background_job_result = response.data

        response = Client().get('/ru/api/segments/train-tariffs/poll/', params)
        assert worker_stub.call_count == 1
        assert response.status_code == 200
        poll_result = response.data

        assert poll_result == after_finishing_background_job_result
        assert not poll_result['querying']

        assert len(poll_result['segments']) == 1

        assert_that(httpretty.latest_requests, has_item(has_property('parsed_body', has_entries({
            'Origin': '2004001',
            'Destination': '2000000',
            'CarGrouping': 'DontGroup',
            'DepartureDate': '2017-09-30T00:00:00'
        }))))


WIZARD_RESPONSE_SPB_MSK = '''
{
  "search_url": "https:\/\/rasp.yandex.ru\/search\/train\/?fromId=c2&toId=c213&when=2017-09-30",
  "minimum_price": {
    "currency": "RUB",
    "value": 2166.0
  },
  "segments": [
    {
      "arrival": {
        "settlement": {
          "key": "c213",
          "title": "Москва"
        },
        "station": {
          "key": "s2000001",
          "title": "Курский вокзал"
        },
        "local_datetime": {
          "timezone": "Europe\/Moscow",
          "value": "2017-10-01T02:10:00+03:00"
        }
      },
      "facilities": null,
      "train": {
        "is_suburban": false,
        "has_dynamic_pricing": true,
        "title": "Санкт-Петербург — Нижний Новгород",
        "display_number": "059А",
        "brand": {
          "is_deluxe": true,
          "id": 146,
          "is_high_speed": false,
          "title": "Волга"
        },
        "two_storey": false,
        "number": "059Я",
        "first_country_code": "RU",
        "last_country_code": "RU",
        "thread_type": "basic",
        "coach_owners": ["ФПК"],
        "provider": "P1",
        "raw_train_name": "Волга"
      },
      "order_url": "https:\/\/rasp.yandex.ru\/order\/?fromId=c2&toId=c213&when=2017-09-30&number=059%D0%90&time=19:15",
      "order_touch_url":
      "https:\/\/t.rasp.yandex.ru\/order\/?fromId=c2&toId=c213&when=2017-09-30&number=059%D0%90&time=19:15",
      "places": {
        "records": [
          {
            "count": 16,
            "max_seats_in_the_same_car": 16,
            "price": {
              "currency": "RUB",
              "value": 7914.0
            },
            "price_details": {
              "fee": 414.0,
              "service_price": 500.0,
              "ticket_price": 7500.0,
              "several_prices": true
            },
            "coach_type": "suite",
            "service_class": "2Л"
          },
          {
            "count": 136,
            "max_seats_in_the_same_car": 36,
            "price": {
              "currency": "RUB",
              "value": 3773.0
            },
            "price_details": {
              "fee": 373.0,
              "service_price": 400.0,
              "ticket_price": 3400.0,
              "several_prices": true
            },
            "coach_type": "compartment",
            "service_class": "2Л"
          },
          {
            "count": 317,
            "max_seats_in_the_same_car": 263,
            "price": {
              "currency": "RUB",
              "value": 2166.0
            },
            "price_details":  {
              "fee": 166.0,
              "service_price": 500.0,
              "ticket_price": 2000.0,
              "several_prices": false
            },
            "coach_type": "platzkarte",
            "service_class": "2Л"
          }
        ],
        "electronic_ticket": false,
        "updated_at": {
          "timezone": "UTC",
          "value": "2017-08-30T09:25:58.763418+00:00"
        }
      },
      "minimum_price": {
        "currency": "RUB",
        "value": 2166.0
      },
      "duration": 415.0,
      "departure": {
        "settlement": {
          "key": "c2",
          "title": "Санкт-Петербург"
        },
        "station": {
          "key": "s9602494",
          "title": "Московский вокзал"
        },
        "local_datetime": {
          "timezone": "Europe\/Moscow",
          "value": "2017-09-30T19:15:00+03:00"
        }
      }
    }
  ],
  "found_departure_date": "2017-09-30",
  "path_items": [
    {
      "url": "https:\/\/testing.morda-front.rasp.common.yandex.ru\/search\/train\/?fromId=c2&toId=c213&when=2017-09-30",
      "text": "testing.morda-front.rasp.common.yandex.ru",
      "touch_url": "https:\/\/testing.touch.rasp.common.yandex.ru\/search\/train\/?fromId=c2&toId=c213&when=2017-09-30"
    }
  ],
  "query": {
    "departure_point": {
      "key": "c2",
      "title": "Санкт-Петербург"
    },
    "departure_date": "2017-09-30",
    "order_by": "departure",
    "arrival_point": {
      "key": "c213",
      "title": "Москва"
    },
    "language": "ru"
  },
  "search_touch_url": "https:\/\/t.rasp.yandex.ru\/search\/train\/?fromId=c2&toId=c213&when=2017-09-30"
}
'''


@replace_now('2017-09-02')
@replace_dynamic_setting('TRAIN_PURCHASE_WIZARD_CONFIDENCE_MINUTES', 15)
@replace_setting('TRAIN_WIZARD_API_DIRECTION_HOST', 'train-wizard-api.net')
@replace_setting('TRAIN_WIZARD_API_INDEXER_HOST', 'train-wizard-api.net')
@pytest.mark.usefixtures('worker_cache_stub')
@pytest.mark.parametrize('reason_for_missing_prices', (True, False))
def test_init_and_poll_with_wizard_source(
    httpretty, worker_stub, reason_for_missing_prices
):
    with replace_dynamic_setting('TRAIN_PURCHASE_FEATURE_REASON_FOR_MISSING_PRICES', reason_for_missing_prices):
        ClientContractsFactory(contracts=[ClientContractFactory(partner_commission_sum=0, partner_commission_sum2=0)])
        mock_im(httpretty, TRAIN_PRICING_ENDPOINT, json=_create_im_response_msk_spb())
        create_www_setting_cache_timeouts()
        create_currency(code='RUR', iso_code='RUB')
        httpretty.register_uri(
            httpretty.GET,
            'https://train-wizard-api.net/searcher/public-api/open_direction/',
            body=WIZARD_RESPONSE_SPB_MSK
        )
        httpretty.register_uri(
            httpretty.POST,
            'https://train-wizard-api.net/indexer/public-api/direction/',
            status=200
        )

        t_train = TransportType.objects.get(pk=TransportType.TRAIN_ID)
        piter = create_settlement(
            title='Питер', country=Country.RUSSIA_ID, time_zone=MSK_TIMEZONE, _geo_id=204,
        )
        create_station(
            __={'codes': {'express': '2000000'}}, settlement=Settlement.MOSCOW_ID,
            t_type=t_train, majority=StationMajority.EXPRESS_FAKE_ID,
        )
        station_to = create_station(
            id=2000001, __={'codes': {'express': '2000001'}}, settlement=Settlement.MOSCOW_ID,
            t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID,
        )
        station_from = create_station(
            id=9602494, __={'codes': {'express': '2004001'}}, settlement=piter,
            t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID,
        )
        create_deluxe_train(id=146)
        create_thread(
            number='059А',
            t_type=t_train,
            tz_start_time=time(17, 26),
            schedule_v1=[
                [None, 0, station_from],
                [300, None, station_to],
            ],
            __={'calculate_noderoute': True},
            title_common=build_default_title_common(t_train, [station_from, station_to]),
            title='Питер - Москва'
        )
        params = {
            'pointFrom': piter.point_key,
            'pointTo': station_to.point_key,
            'startTime': '2017-09-30T10:00:00+03:00',
            'endTime': '2017-09-30T20:00:00+03:00',
            'partner': 'im',
            'national_version': 'ru',
            'include_price_fee': True,
            'icookie': 'someCookie',
        }

        response = Client().get('/ru/api/segments/train-tariffs/', params)

        assert worker_stub.call_count == 1
        assert response.status_code == 200
        wizard_result = response.data
        assert wizard_result['querying']
        assert len(wizard_result['segments']) == 1
        segment = wizard_result['segments'][0]

        assert_that(segment['tariffs'], has_entries({
            'classes': has_entries({
                'compartment': has_entries({
                    'price': {'currency': 'RUB', 'value': 3774.00},
                    'seats': 136, 'severalPrices': True,
                    'orderUrl': contains_string('cls=compartment'),
                    'trainOrderUrl': contains_string('coachType=compartment'),
                }),
                'platzkarte': has_entries({
                    'price': {'currency': 'RUB', 'value': 2220.00},
                    'seats': 317,
                    'orderUrl': contains_string('cls=platzkarte'),
                    'trainOrderUrl': contains_string('coachType=platzkarte'),
                })
            }),
        }))

        httpretty.register_uri(
            httpretty.GET,
            'https://train-wizard-api.net/searcher/public-api/open_direction/',
            body=WIZARD_RESPONSE_SPB_MSK_UPDATED
        )
        response = Client().get('/ru/api/segments/train-tariffs/poll/', params)

        assert worker_stub.call_count == 1
        assert response.status_code == 200
        poll_result = response.data
        assert not poll_result['querying']
        assert len(poll_result['segments']) == 1
        segment = poll_result['segments'][0]
        assert_that(segment['tariffs'], has_entries({
            'electronicTicket': True,
            'classes': has_entries({
                'compartment': has_entries({
                    'price': {'currency': 'RUB', 'value': 3414.25},
                    'seats': 34, 'severalPrices': True,
                    'orderUrl': contains_string('cls=compartment'),
                    'trainOrderUrl': string_contains_in_order('provider=P1', 'coachType=compartment'),
                }),
                'platzkarte': has_entries({
                    'price': {'currency': 'RUB', 'value': 1346.21},
                    'seats': 138, 'severalPrices': True,
                    'orderUrl': contains_string('cls=platzkarte'),
                    'trainOrderUrl': (string_contains_in_order('provider=P1', 'coachType=platzkarte')),
                })
            }),
        }))

        assert segment['provider'] == 'P1'


@replace_now('2017-09-01')
def test_train_tariffs_earliest_routed_date():
    piter = create_settlement(title='Питер', country=Country.RUSSIA_ID, time_zone=MSK_TIMEZONE)
    msk = create_settlement(title='Москва', country=Country.RUSSIA_ID, time_zone=MSK_TIMEZONE)

    t_train = TransportType.objects.get(pk=TransportType.TRAIN_ID)

    station_to = create_station(__={'codes': {'express': '2000001'}}, settlement=msk,
                                t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID, id=2000001)
    station_from = create_station(__={'codes': {'express': '2004001'}}, settlement=piter,
                                  t_type=t_train, majority=StationMajority.MAIN_IN_CITY_ID)

    create_thread(
        number='059А',
        t_type=t_train,
        year_days=[date(2017, 9, 1), date(2017, 9, 4), date(2017, 9, 5)],
        tz_start_time=time(17, 26),
        schedule_v1=[
            [None, 0, station_from],
            [300, None, station_to],
        ],
        __={'calculate_noderoute': True},
        title_common=build_default_title_common(t_train, [station_from, station_to]),
        title='Питер - Москва'
    )

    response = Client().get('/ru/api/segments/train-tariffs/earliest_date/?{}'.format(
        urlencode({
            'pointFrom': 'c{}'.format(piter.id),
            'pointTo': 'c{}'.format(msk.id),
        })
    ))

    assert response.status_code == 200
    assert response.data == {
        'date': '2017-09-04',
    }

    response = Client().get('/ru/api/segments/train-tariffs/earliest_date/?{}'.format(
        urlencode({
            'pointFrom': 'c{}'.format(piter.id),
            'pointTo': 'c{}'.format(msk.id),
            'date': '2017-09-05',
        })
    ))

    assert response.status_code == 200
    assert response.data == {
        'date': '2017-09-05',
    }
