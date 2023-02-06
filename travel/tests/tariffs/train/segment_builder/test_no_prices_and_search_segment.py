# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date, time, datetime

import pytest
from hamcrest import assert_that, has_properties, has_entries, empty, contains_inanyorder

from common.models.geo import Country
from common.models.transport import TransportType
from common.tester.factories import create_settlement, create_station, create_thread
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting_key
from common.utils.date import MSK_TZ

from travel.rasp.train_api.tariffs.train.factories.base import create_train_tariffs_query
from travel.rasp.train_api.tariffs.train.im.send_query import (
    get_result_from_partner_and_add_schedule_info, TRAIN_PRICING_ENDPOINT
)
from travel.rasp.train_api.tariffs.train.segment_builder.prices_and_reasons import SOLD_OUT_BROKEN_CLASSES
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner


@pytest.mark.dbuser
@replace_now('2020-02-17')
@pytest.mark.parametrize('empty_error_response', (
    {'Code': 311, 'Message': 'На заданном направлении (или поезде) мест нет', 'MessageParams': []},
    {'Code': 310, 'Message': 'В запрашиваемую дату поездов нет', 'MessageParams': []}
))
@pytest.mark.parametrize('protobufs_setting_value', (True, False))
def test_one_full_and_one_only_price_and_one_only_search_segments(httpretty, empty_error_response, protobufs_setting_value):
    with replace_dynamic_setting_key('TRAIN_BACKEND_USE_PROTOBUFS', 'alias', protobufs_setting_value):
        create_ru_station = create_station.mutate(
            country=Country.RUSSIA_ID,
            settlement=create_settlement(country=Country.RUSSIA_ID),
            t_type=TransportType.TRAIN_ID,
        )
        station_from = create_ru_station(title='From station', __={'codes': {'express': 'FROM_EXPRESS_CODE'}})
        station_to = create_ru_station(title='To station', __={'codes': {'express': 'TO_EXPRESS_CODE'}})
        thread = create_thread(
            number='132Ы',
            tz_start_time=time(12, 00),
            t_type=TransportType.TRAIN_ID,
            schedule_v1=[
                [None, 0, station_from],
                [40, None, station_to],
            ],
            __={'calculate_noderoute': True}
        )
        mock_im(httpretty, TRAIN_PRICING_ENDPOINT, status=500, json=empty_error_response)

        query = create_train_tariffs_query(TrainPartner.IM, station_from, station_to, date(2020, 2, 25))
        result = get_result_from_partner_and_add_schedule_info(query, include_reason_for_missing_prices=True)

        assert len(result.segments) == 2, 'Должены быть два поисковых сегмента, которые отличаются только ключом'
        assert_that(result.segments, contains_inanyorder(
            has_properties(
                number='132Ы',
                can_supply_segments=False,
                station_from=station_from,
                station_to=station_to,
                start_station=station_from,
                end_station=station_to,
                thread=thread,
                first_country_code='RU',
                last_country_code='RU',
                tariffs=has_entries(
                    broken_classes=SOLD_OUT_BROKEN_CLASSES,
                    classes=empty(),
                ),
                departure=MSK_TZ.localize(datetime(2020, 2, 25, 12, 0)),
                arrival=MSK_TZ.localize(datetime(2020, 2, 25, 12, 40)),
                key='train 132Ы 20200225_1200',
            ),
            has_properties(
                number='132Ы',
                can_supply_segments=False,
                station_from=station_from,
                station_to=station_to,
                start_station=station_from,
                end_station=station_to,
                thread=thread,
                first_country_code='RU',
                last_country_code='RU',
                tariffs=has_entries(
                    broken_classes=SOLD_OUT_BROKEN_CLASSES,
                    classes=empty(),
                ),
                departure=MSK_TZ.localize(datetime(2020, 2, 25, 12, 0)),
                arrival=MSK_TZ.localize(datetime(2020, 2, 25, 12, 40)),
                key='train 131Ы 20200225_1200',
            ),
        ))
