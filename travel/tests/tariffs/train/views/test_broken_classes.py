# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

import pytest
import pytz
from django.core.urlresolvers import reverse
from django.test import Client

from common.models.geo import Settlement
from common.tester.factories import create_station, create_settlement
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting, replace_setting
from travel.rasp.train_api.train_purchase.core.factories import ClientContractsFactory, ClientContractFactory


WIZARD_OPEN_DIRECTION_RESPONSE = """
{
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
          "timezone": "Asia/Yekaterinburg",
          "value": "2020-01-16T11:35:00+05:00"
        }
      },
      "order_url": "https://example.com/",
      "places": {
        "records": [
          {
            "count": 19,
            "max_seats_in_the_same_car": 18,
            "price": {
              "currency": "RUB",
              "value": 3910
            },
            "price_details": {
              "fee": "387.48",
              "several_prices": true,
              "service_price": "161.0",
              "ticket_price": "3522.5"
            },
            "coach_type": "compartment"
          }
        ],
        "electronic_ticket": true,
        "updated_at": {
          "timezone": "UTC",
          "value": "2019-12-17T14:18:45.034993+00:00"
        }
      },
      "minimum_price": {
        "currency": "RUB",
        "value": 3025
      },
      "broken_classes": {
        "platzkarte": [
          9,
          10
        ]
      },
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
          "timezone": "Europe/Moscow",
          "value": "2020-01-15T00:35:00+03:00"
        }
      },
      "facilities": [],
      "train": {
        "is_suburban": false,
        "has_dynamic_pricing": false,
        "brand": null,
        "two_storey": false,
        "number": "100Э",
        "first_country_code": "RU",
        "display_number": "100Э",
        "title": "Москва — Владивосток",
        "thread_type": "basic",
        "last_country_code": "RU",
        "provider": "P1",
        "coach_owners": [
          "ФПК"
        ]
      },
      "order_touch_url": "https://example.com/",
      "duration": 1980,
      "is_the_fastest": false,
      "is_the_cheapest": false
    }
  ]
}
""".strip()


@pytest.mark.dbuser
@pytest.mark.usefixtures('worker_cache_stub')
@pytest.mark.usefixtures('worker_stub')
@replace_now('2019-11-18 00:00:00')
@replace_setting('TRAIN_WIZARD_API_DIRECTION_HOST', 'example.com')
@replace_dynamic_setting('TRAIN_PURCHASE_FEATURE_REASON_FOR_MISSING_PRICES', True)
def test_broken_classes_in_response(httpretty):
    ekb_tz = pytz.timezone('Asia/Yekaterinburg')
    ClientContractsFactory(contracts=[ClientContractFactory(
        partner_commission_sum=Decimal('10.0'), partner_commission_sum2=Decimal('10.0')
    )])
    msk = Settlement.objects.get(id=213)
    st_from = create_station(id=2000002, title='Ярославский вокзал', settlement=msk,
                             __=dict(codes={'express': '2004001'}))
    ekb = create_settlement(id=54, title='Екатеринбург', time_zone=str(ekb_tz))
    st_to = create_station(id=9607404, title='Екатеринбург-Пасс.', settlement=ekb,
                           __=dict(codes={'express': '2000001'}))

    httpretty.register_uri(
        httpretty.GET,
        'https://example.com/searcher/public-api/open_direction/',
        body=WIZARD_OPEN_DIRECTION_RESPONSE
    )

    request_params = {
        'pointFrom': st_from.point_key,
        'pointTo': st_to.point_key,
        'date': '2020-01-15',
        'national_version': 'ru',
        'partner': 'im',
    }
    response = Client().get(reverse('train_tariffs_poll'), request_params)

    assert response.status_code == 200
    assert len(httpretty.latest_requests) == 1
    segments = response.data['segments']
    assert len(segments) == 1
    assert 'compartment' in segments[0]['tariffs']['classes']
    assert segments[0]['tariffs']['brokenClasses'] == {'platzkarte': [9, 10]}
