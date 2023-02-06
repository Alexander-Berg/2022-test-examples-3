# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime
from decimal import Decimal

import pytest
import pytz
from hamcrest import assert_that, has_entries, has_properties, contains_inanyorder
from mock import Mock

from common.models.currency import Price
from common.models.geo import Settlement
from common.tester.factories import create_settlement, create_station
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting, replace_dynamic_setting
from common.utils.date import MSK_TZ
from travel.rasp.library.python.common23.date.environment import now_aware
from travel.rasp.train_api.tariffs.train.base.worker import TrainTariffsResult
from travel.rasp.train_api.tariffs.train.factories.base import create_train_tariffs_query
from travel.rasp.train_api.tariffs.train.wizard.service import get_wizard_tariffs, need_to_update, get_wizard_prices
from travel.rasp.train_api.train_purchase.core.enums import TrainOrderUrlOwner
from travel.rasp.train_api.train_purchase.core.factories import ClientContractFactory, ClientContractsFactory
from travel.rasp.train_api.train_purchase.core.models import TrainPartner

WIZARD_RESPONSE_MSK_EKB_FULL_SEGMENT_AND_SEGMENT_WITHOUT_PRICE = """
{
  "search_url": "https:\/\/rasp.yandex.ru\/search\/train\/?fromId=c213&toId=c54&when=2019-01-15",
  "minimum_price": {
    "currency": "RUB",
    "value": 2681.0
  },
  "segments": [
    {
      "arrival": {
        "settlement": {
          "key": "c54",
          "title": "Екатеринбург"
        },
        "station": {
          "key": "s9607404",
          "title": "Екатеринбург-Пасс."
        },
        "local_datetime": {
          "timezone": "Asia\/Yekaterinburg",
          "value": "2019-01-16T11:16:00+05:00"
        }
      },
      "facilities": null,
      "train": {
        "is_suburban": false,
        "has_dynamic_pricing": true,
        "title": "Москва — Владивосток",
        "display_number": "100ЭЙ",
        "brand": null,
        "two_storey": false,
        "number": "100Э",
        "first_country_code": "RU",
        "last_country_code": "RU",
        "provider": "P1",
        "thread_type": "basic",
        "coach_owners": [
          "ФПК"
        ]
      },
      "order_url": "https:\/\/rasp.yandex.ru\/order\/?fromId=c213&toId=c54&when=2019-01-15&number=100%D0%AD&time=00:35",
      "order_touch_url":
      "https:\/\/t.rasp.yandex.ru\/order\/?fromId=c213&toId=c54&when=2019-01-15&number=100%D0%AD&time=00:35",
      "places": {
        "records": [
          {
            "count": 44,
            "max_seats_in_the_same_car": 32,
            "price": {
              "currency": "RUB",
              "value": 5411.0
            },
            "price_details": {
              "fee": "536.24",
              "several_prices": true,
              "service_price": "156.0",
              "ticket_price": "4874.9"
            },
            "coach_type": "compartment"
          },
          {
            "count": 156,
            "max_seats_in_the_same_car": 156,
            "price": {
              "currency": "RUB",
              "value": 2863.0
            },
            "price_details": {
              "fee": "269.24",
              "several_prices": false,
              "service_price": "145.9",
              "ticket_price": "2593.5"
            },
            "coach_type": "platzkarte"
          }
        ],
        "electronic_ticket": true,
        "updated_at": {
          "timezone": "UTC",
          "value": "2018-12-24T10:18:42.218544+00:00"
        }
      },
      "minimum_price": {
        "currency": "RUB",
        "value": 2863.0
      },
      "duration": 1961.0,
      "departure": {
        "settlement": {
          "key": "c213",
          "title": "Москва"
        },
        "station": {
          "key": "s2000002",
          "title": "Ярославский вокзал"
        },
        "local_datetime": {
          "timezone": "Europe\/Moscow",
          "value": "2019-01-15T00:35:00+03:00"
        }
      }
    },
    {
      "arrival": {
        "settlement": {
          "key": "c54",
          "title": "Екатеринбург"
        },
        "station": {
          "key": "s9607404",
          "title": "Екатеринбург-Пасс."
        },
        "local_datetime": {
          "timezone": "Asia\/Yekaterinburg",
          "value": "2019-01-16T11:16:00+05:00"
        }
      },
      "order_url": "https://travel.ya.ru/trains/order/?fromId=c213&toId=c54&when=2020-05-25&number=016%D0%95&time=16:38",
      "places": {
        "records": null,
        "updated_at": null
      },
      "minimum_price": null,
      "broken_classes": null,
      "departure": {
        "settlement": {
          "key": "c213",
          "title": "Москва"
        },
        "station": {
          "key": "s2000002",
          "title": "Ярославский вокзал"
        },
        "local_datetime": {
          "timezone": "Europe\/Moscow",
          "value": "2019-01-15T00:35:00+03:00"
        }
      },
      "facilities": null,
      "train": {
        "is_suburban": null,
        "has_dynamic_pricing": null,
        "brand": {
          "is_high_speed": false,
          "is_deluxe": true,
          "id": 104,
          "short_title": "фирменный «Урал»",
          "title": "Урал"
        },
        "two_storey": null,
        "number": "016Е",
        "first_country_code": null,
        "display_number": null,
        "title": "Москва — Екатеринбург",
        "thread_type": "basic",
        "last_country_code": null,
        "coach_owners": null,
        "provider": "P1"
      },
      "order_touch_url": "https://travel.ya.ru/trains/order/?fromId=c213&toId=c54&when=2020-05-25&number=016%D0%95&time=16:38",
      "duration": 1541,
      "is_the_fastest": true,
      "is_the_cheapest": false
    }
  ],
  "found_departure_date": "2019-01-15",
  "path_items": [
    {
      "url": "https:\/\/rasp.yandex.ru\/search\/train\/?fromId=c213&toId=c54&when=2019-01-15",
      "text": "testing.morda-front.rasp.common.yandex.ru",
      "touch_url": "https:\/\/t.rasp.yandex.ru\/search\/train\/?fromId=c213&toId=c54&when=2019-01-15"
    }
  ],
  "query": {
    "departure_point": {
      "key": "c213",
      "title": "Москва"
    },
    "departure_date": "2019-01-15",
    "order_by": "departure",
    "arrival_point": {
      "key": "c54",
      "title": "Екатеринбург"
    },
    "language": "ru"
  },
  "search_touch_url": "https:\/\/t.rasp.yandex.ru\/search\/train\/?fromId=c213&toId=c54&when=2019-01-15"
}
"""

WIZARD_RESPONSE_PRICES_BY_DIRECTION = """[
  {
    "is_suburban": false,
    "has_dynamic_pricing": true,
    "two_storey": false,
    "electronic_ticket": true,
    "number": "100Э",
    "departure_dt": "2019-02-01T00:35:00+03:00",
    "arrival_dt": "2019-02-02T11:16:00+05:00",
    "display_number": "100ЭЙ",
    "order_url": "https:...",
    "places": [
      {
        "coach_type": "compartment",
        "count": 68,
        "max_seats_in_the_same_car": 68,
        "price": {
          "base_value": null,
          "currency": "RUB",
          "sort_value": [
            null,
            5518.03
          ],
          "value": 5518.03
        },
        "price_details": {
          "fee": "546.83",
          "several_prices": false,
          "service_price": "156.0",
          "ticket_price": "4971.2"
        }
      },
      {
        "coach_type": "platzkarte",
        "count": 156,
        "max_seats_in_the_same_car": 156,
        "price": {
          "base_value": null,
          "currency": "RUB",
          "sort_value": [
            null,
            2878.79
          ],
          "value": 2878.79
        },
        "price_details": {
          "fee": "285.29",
          "several_prices": false,
          "service_price": "145.9",
          "ticket_price": "2593.5"
        }
      }
    ],
    "title_dict": {
      "type": "default",
      "title_parts": [
        "c213",
        "c75"
      ]
    },
    "departure_station_id": 2000002,
    "arrival_station_id": 9607404,
    "first_country_code": "RU",
    "last_country_code": "RU",
    "coach_owners": [
      "ФПК"
    ]
  },
  {
    "is_suburban": false,
    "has_dynamic_pricing": true,
    "two_storey": false,
    "electronic_ticket": true,
    "number": "082И",
    "departure_dt": "2019-02-01T13:10:00+03:00",
    "arrival_dt": "2019-02-02T17:59:00+05:00",
    "display_number": "082ИА",
    "order_url": "https:...",
    "places": [
      {
        "coach_type": "compartment",
        "count": 132,
        "max_seats_in_the_same_car": 36,
        "price": {
          "base_value": null,
          "currency": "RUB",
          "sort_value": [
            null,
            5156.51
          ],
          "value": 5156.51
        },
        "price_details": {
          "fee": "511.01",
          "several_prices": true,
          "service_price": "156.0",
          "ticket_price": "4645.5"
        }
      },
      {
        "coach_type": "platzkarte",
        "count": 238,
        "max_seats_in_the_same_car": 152,
        "price": {
          "base_value": null,
          "currency": "RUB",
          "sort_value": [
            null,
            2697.52
          ],
          "value": 2697.52
        },
        "price_details": {
          "fee": "267.32",
          "several_prices": true,
          "service_price": "145.9",
          "ticket_price": "2430.2"
        }
      }
    ],
    "title_dict": {
      "type": "default",
      "title_parts": [
        "c213",
        "c198"
      ]
    },
    "departure_station_id": 2000003,
    "arrival_station_id": 9607404,
    "first_country_code": "RU",
    "last_country_code": "USA",
    "coach_owners": [
      "ФПК"
    ]
  }
]"""


@pytest.mark.dbuser
@pytest.mark.mongouser
@replace_setting('TRAIN_WIZARD_API_DIRECTION_HOST', 'train-wizard-api.net')
@replace_dynamic_setting('TRAIN_PURCHASE_WIZARD_CONFIDENCE_MINUTES', 24 * 60)
@pytest.mark.parametrize('now, expected_status', (
    (datetime(2018, 12, 25), TrainTariffsResult.STATUS_SUCCESS),
    (datetime(2018, 12, 26), TrainTariffsResult.STATUS_PENDING),
))
def test_get_wizard_tariffs(httpretty, now, expected_status):
    ekb_tz = pytz.timezone('Asia/Yekaterinburg')
    ClientContractsFactory(contracts=[ClientContractFactory(
        partner_commission_sum=Decimal('10.0'), partner_commission_sum2=Decimal('10.0')
    )])
    httpretty.register_uri(
        httpretty.GET,
        'https://train-wizard-api.net/searcher/public-api/open_direction/',
        body=WIZARD_RESPONSE_MSK_EKB_FULL_SEGMENT_AND_SEGMENT_WITHOUT_PRICE
    )
    msk = Settlement.objects.get(id=213)
    st_from = create_station(id=2000002, title='Ярославский вокзал', settlement=msk,
                             __=dict(codes={'express': '2004001'}))
    ekb = create_settlement(id=54, title='Екатеринбург', time_zone=str(ekb_tz))
    st_to = create_station(id=9607404, title='Екатеринбург-Пасс.', settlement=ekb,
                           __=dict(codes={'express': '2000001'}))

    train_query = create_train_tariffs_query(TrainPartner.IM, st_from, st_to, departure_date=date(2019, 1, 15))
    with replace_now(now):
        result = get_wizard_tariffs(train_query)

    assert len(httpretty.latest_requests)
    assert result.status == expected_status
    assert len(result.segments) == 1
    assert_that(result.segments[0], has_properties(
        coach_owners=['ФПК'],
        number='100ЭЙ',
        original_number='100Э',
        is_suburban=False,
        has_dynamic_pricing=True,
        two_storey=False,
        departure=MSK_TZ.localize(datetime(2019, 1, 15, 0, 35)),
        arrival=ekb_tz.localize(datetime(2019, 1, 16, 11, 16)),
        railway_departure=MSK_TZ.localize(datetime(2019, 1, 15, 0, 35)),
        railway_arrival=MSK_TZ.localize(datetime(2019, 1, 16, 9, 16)),
        tariffs=has_entries(
            classes=has_entries(
                compartment=has_properties(
                    ticket_price=Price(Decimal('4874.9'), 'RUB'),
                    service_price=Price(Decimal('156.0'), 'RUB'),
                    several_prices=True,
                ),
                platzkarte=has_properties(
                    ticket_price=Price(Decimal('2593.5'), 'RUB'),
                    service_price=Price(Decimal('145.9'), 'RUB'),
                    several_prices=False,
                ),
            )
        ),
        updated_at=pytz.UTC.localize(datetime(2018, 12, 24, 10, 18, 42)),
    ))


@replace_now('2019-01-29 12:00:00')
@replace_dynamic_setting('TRAIN_PURCHASE_WIZARD_CONFIDENCE_MINUTES', 24 * 60)
@pytest.mark.parametrize('segments, expected', (
    ([], True),
    ([Mock(updated_at=None)], True),
    ([Mock(updated_at=MSK_TZ.localize(datetime(2018, 12, 24))), Mock(updated_at=now_aware())], True),
    ([Mock(updated_at=MSK_TZ.localize(datetime(2019, 1, 29))), Mock(updated_at=now_aware())], False),
    ([Mock(updated_at=now_aware())], False),
))
def test_need_to_update(segments, expected):
    assert need_to_update(segments) is expected


@replace_now('2019-01-29 12:00:00')
@pytest.mark.dbuser
@pytest.mark.mongouser
@replace_setting('TRAIN_WIZARD_API_DIRECTION_HOST', 'train-wizard-api.net')
def test_get_wizard_prices(httpretty):
    ekb_tz = pytz.timezone('Asia/Yekaterinburg')
    ClientContractsFactory(contracts=[ClientContractFactory(
        partner_commission_sum=Decimal('10.0'), partner_commission_sum2=Decimal('10.0')
    )])
    httpretty.register_uri(
        httpretty.GET,
        'https://train-wizard-api.net/searcher/public-api/prices_by_directions/',
        body=WIZARD_RESPONSE_PRICES_BY_DIRECTION
    )
    msk = Settlement.objects.get(id=213)
    st_from = create_station(title='Ярославский вокзал', settlement=msk,
                             __=dict(codes={'express': '2004001'}))
    ekb = create_settlement(id=54, title='Екатеринбург', time_zone=str(ekb_tz))
    st_to = create_station(title='Екатеринбург-Пасс.', settlement=ekb,
                           __=dict(codes={'express': '2000001'}))

    query = dict(
        partner=TrainPartner.IM,
        point_from=st_from,
        point_to=st_to,
        experiment=False,
        national_version='ru'
    )
    result = get_wizard_prices(query)

    assert len(httpretty.latest_requests) == 1
    assert len(result) == 2
    assert_that(result, contains_inanyorder(
        has_properties(
            coach_owners=['ФПК'],
            display_number='100ЭЙ',
            original_number='100Э',
            is_suburban=False,
            has_dynamic_pricing=True,
            two_storey=False,
            departure_station_id=2000002,
            arrival_station_id=9607404,
            departure=MSK_TZ.localize(datetime(2019, 2, 1, 0, 35)),
            arrival=ekb_tz.localize(datetime(2019, 2, 2, 11, 16)),
            tariffs=has_entries(
                electronic_ticket=True,
                classes=has_entries(
                    compartment=has_properties(
                        price=Price(Decimal('5518.03'), 'RUB'),
                        ticket_price=Price(Decimal('4971.2'), 'RUB'),
                        service_price=Price(Decimal('156.0'), 'RUB'),
                        several_prices=False,
                        train_order_url_owner=TrainOrderUrlOwner.TRAINS,
                    ),
                    platzkarte=has_properties(
                        price=Price(Decimal('2878.79'), 'RUB'),
                        ticket_price=Price(Decimal('2593.5'), 'RUB'),
                        service_price=Price(Decimal('145.9'), 'RUB'),
                        several_prices=False,
                        train_order_url_owner=TrainOrderUrlOwner.TRAINS,
                    ),
                )
            ),
        ),
        has_properties(
            coach_owners=['ФПК'],
            display_number='082ИА',
            original_number='082И',
            is_suburban=False,
            has_dynamic_pricing=True,
            two_storey=False,
            departure_station_id=2000003,
            arrival_station_id=9607404,
            departure=MSK_TZ.localize(datetime(2019, 2, 1, 13, 10)),
            arrival=ekb_tz.localize(datetime(2019, 2, 2, 17, 59)),
            tariffs=has_entries(
                electronic_ticket=True,
                classes=has_entries(
                    compartment=has_properties(
                        price=Price(Decimal('5156.51'), 'RUB'),
                        ticket_price=Price(Decimal('4645.5'), 'RUB'),
                        service_price=Price(Decimal('156.0'), 'RUB'),
                        several_prices=True,
                        train_order_url_owner=TrainOrderUrlOwner.UFS,
                    ),
                    platzkarte=has_properties(
                        price=Price(Decimal('2697.52'), 'RUB'),
                        ticket_price=Price(Decimal('2430.2'), 'RUB'),
                        service_price=Price(Decimal('145.9'), 'RUB'),
                        several_prices=True,
                        train_order_url_owner=TrainOrderUrlOwner.UFS,
                    ),
                )
            ),
        )
    ))
