# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date, time, datetime

from mock import Mock
import pytest
from hamcrest import assert_that, contains_inanyorder, has_properties, anything, has_entries, empty, contains

from common.models.geo import StationExpressAlias, Country
from common.models.transport import TransportType
from common.tester.factories import create_settlement, create_station, create_thread, create_company
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting_key
from common.utils.date import MSK_TZ
from route_search.models import RThreadSegment
from travel.proto.dicts.trains.station_express_alias_pb2 import TStationExpressAlias
from travel.rasp.train_api.tariffs.train.factories.base import create_train_tariffs_query
from travel.rasp.train_api.tariffs.train.factories.im import ImTrainPricingResponseFactory
from travel.rasp.train_api.tariffs.train.im.send_query import (
    get_result_from_partner_and_add_schedule_info, TRAIN_PRICING_ENDPOINT
)
from travel.rasp.train_api.tariffs.train.segment_builder.prices_and_reasons import (
    make_no_price_segments_from_search_segment, SOLD_OUT_BROKEN_CLASSES
)
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner


@pytest.mark.dbuser
@replace_now('2020-02-11')
@pytest.mark.parametrize('protobufs_setting_value', (True, False))
def test_one_full_and_one_only_price_and_one_only_search_segments(httpretty, protobufs_setting_value, protobuf_data_provider):
    """
    Тестируем создание трех разных сегментов. Сегмент только с ценой, сегмент только из поиска и полный сегмент.
    100Ц - start_100, from, to, end_100 - только цена
    200В - start_200, from, to, end_200 - и в поиске и есть цена
    300П - start_300, from, to, end_300 - только в поиске
    """
    with replace_dynamic_setting_key('TRAIN_BACKEND_USE_PROTOBUFS', 'alias', protobufs_setting_value):
        create_ru_station = create_station.mutate(
            country=Country.RUSSIA_ID,
            settlement=create_settlement(country=Country.RUSSIA_ID),
            t_type=TransportType.TRAIN_ID,
        )
        station_from = create_ru_station(title='From station', __={'codes': {'express': 'FROM_EXPRESS_CODE'}})
        station_to = create_ru_station(title='To station', __={'codes': {'express': 'TO_EXPRESS_CODE'}})
        start_100 = create_ru_station(title='Start 100 station', __={'codes': {'express': 'START_100_EXPRESS_CODE'}})
        end_100 = create_ru_station(title='End 100 station', __={'codes': {'express': 'END_100_EXPRESS_CODE'}})
        start_200 = create_ru_station(title='Start 200 station', __={'codes': {'express': 'START_200_EXPRESS_CODE'}})
        end_200 = create_ru_station(title='End 200 station', __={'codes': {'express': 'END_200_EXPRESS_CODE'}})
        start_300 = create_ru_station(title='Start 300 station', __={'codes': {'express': 'START_300_EXPRESS_CODE'}})
        end_300 = create_ru_station(title='End 300 station', __={'codes': {'express': 'END_300_EXPRESS_CODE'}})
        company = create_company(short_title='Расп Перевозчик', title='Длинное название перевозчика расписаний')
        thread_200 = create_thread(
            number='200В',
            tz_start_time=time(12, 00),
            t_type=TransportType.TRAIN_ID,
            schedule_v1=[
                [None, 0, start_200],
                [19, 20, station_from],
                [40, 50, station_to],
                [300, None, end_200],
            ],
            __={'calculate_noderoute': True},
            company=company,
        )
        thread_300 = create_thread(
            number='300П',
            tz_start_time=time(13, 00),
            t_type=TransportType.TRAIN_ID,
            schedule_v1=[
                [None, 0, start_300],
                [29, 30, station_from],
                [50, 60, station_to],
                [300, None, end_300],
            ],
            __={'calculate_noderoute': True},
            company=company,
        )
        if protobufs_setting_value:
            for alias_proto in (
                TStationExpressAlias(StationId=start_100.id, Alias='START 100'),
                TStationExpressAlias(StationId=end_100.id, Alias='END 100'),
                TStationExpressAlias(StationId=start_200.id, Alias='START 200'),
                TStationExpressAlias(StationId=end_200.id, Alias='END 200')
            ):
                protobuf_data_provider.alias_repo.add(alias_proto.SerializeToString())
        else:
            StationExpressAlias.objects.create(station=start_100, alias='START 100')
            StationExpressAlias.objects.create(station=end_100, alias='END 100')
            StationExpressAlias.objects.create(station=start_200, alias='START 200')
            StationExpressAlias.objects.create(station=end_200, alias='END 200')
        mock_im(httpretty, TRAIN_PRICING_ENDPOINT, json=ImTrainPricingResponseFactory(
            Trains=[
                {
                    'TrainNumber': '100Ц',
                    'DisplayTrainNumber': '100Ц',
                    'TrainNumberToGetRoute': '100Ц',
                    'DepartureDateTime': datetime(2020, 2, 25, 12, 10),
                    'ArrivalDateTime': datetime(2020, 2, 26, 12, 10),
                    'OriginName': 'START 100',
                    'DestinationName': 'END 100',
                    'OriginStationCode': 'FROM_EXPRESS_CODE',
                    'DestinationStationCode': 'TO_EXPRESS_CODE',
                    'Carriers': ['Перевозчик от ИМ'],
                },
                {
                    'TrainNumber': '200В',
                    'DisplayTrainNumber': '200В',
                    'TrainNumberToGetRoute': '200В',
                    'DepartureDateTime': datetime(2020, 2, 25, 12, 20),
                    'ArrivalDateTime': datetime(2020, 2, 26, 12, 20),
                    'OriginName': 'START 200',
                    'DestinationName': 'END 200',
                    'OriginStationCode': 'FROM_EXPRESS_CODE',
                    'DestinationStationCode': 'TO_EXPRESS_CODE',
                    'Carriers': ['Перевозчик от ИМ'],
                },
            ]
        ))

        query = create_train_tariffs_query(TrainPartner.IM, station_from, station_to, date(2020, 2, 25))
        result = get_result_from_partner_and_add_schedule_info(query, include_reason_for_missing_prices=True)

        assert len(result.segments) == 4, ('Должно быть: '
                                           '1 сегмент только с ценой; '
                                           '1 сегмент с ценой и ниткой; '
                                           '2 SOLD_OUT - один с ключом 300* и один с ключом 299*.')
        assert_that(result.segments, contains_inanyorder(
            has_properties(
                number='100Ц',
                start_station=start_100,
                can_supply_segments=True,
                station_from=station_from,
                original_number='100Ц',
                end_station=end_100,
                station_to=station_to,
                key='train 100Ц 20200225_1210',
                first_country_code='RU',
                last_country_code='RU',
                tariffs=has_entries(
                    broken_classes=empty(),
                    classes=has_entries(platzkarte=anything()),
                ),
                thread=None,
                departure=MSK_TZ.localize(datetime(2020, 2, 25, 12, 10)),
                arrival=MSK_TZ.localize(datetime(2020, 2, 26, 12, 10)),
                coach_owners=contains('Перевозчик от ИМ'),
            ),
            has_properties(
                number='200В',
                start_station=start_200,
                can_supply_segments=True,
                station_from=station_from,
                end_station=end_200,
                station_to=station_to,
                key='train 200В 20200225_1220',
                thread=thread_200,
                first_country_code='RU',
                last_country_code='RU',
                tariffs=has_entries(
                    broken_classes=empty(),
                    classes=has_entries(platzkarte=anything()),
                ),
                departure=MSK_TZ.localize(datetime(2020, 2, 25, 12, 20)),
                arrival=MSK_TZ.localize(datetime(2020, 2, 26, 12, 20)),
                coach_owners=contains('Расп Перевозчик'),
            ),
            has_properties(
                end_station=end_300,
                can_supply_segments=False,
                station_from=station_from,
                start_station=start_300,
                number='300П',
                station_to=station_to,
                key='train 299П 20200225_1330',
                thread=thread_300,
                first_country_code='RU',
                last_country_code='RU',
                tariffs=has_entries(
                    broken_classes=SOLD_OUT_BROKEN_CLASSES,
                    classes=empty(),
                ),
                departure=MSK_TZ.localize(datetime(2020, 2, 25, 13, 30)),
                arrival=MSK_TZ.localize(datetime(2020, 2, 25, 13, 50)),
                coach_owners=contains('Расп Перевозчик'),
            ),
            has_properties(
                end_station=end_300,
                can_supply_segments=False,
                station_from=station_from,
                start_station=start_300,
                number='300П',
                station_to=station_to,
                key='train 300П 20200225_1330',
                thread=thread_300,
                first_country_code='RU',
                last_country_code='RU',
                tariffs=has_entries(
                    broken_classes=SOLD_OUT_BROKEN_CLASSES,
                    classes=empty(),
                ),
                departure=MSK_TZ.localize(datetime(2020, 2, 25, 13, 30)),
                arrival=MSK_TZ.localize(datetime(2020, 2, 25, 13, 50)),
                coach_owners=contains('Расп Перевозчик'),
            ),
        ))


@pytest.mark.dbuser
def test_make_no_price_segments_from_search_segment__rtstation_train_number():
    search_segment = RThreadSegment()
    search_segment.rtstation_from = Mock(train_number=3)
    search_segment.rtstation_to = Mock(train_number=2)
    search_segment.number = 1
    search_segment.train_keys = ['200']
    search_segment.thread = Mock(number='200', t_model=None)
    segments = make_no_price_segments_from_search_segment(search_segment)
    no_price_segment = segments[0]
    assert no_price_segment.number == search_segment.rtstation_from.train_number
