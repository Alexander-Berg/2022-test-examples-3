# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date, time, datetime

import pytest
from hamcrest import assert_that, contains_inanyorder, has_properties, anything, has_entries, empty

from common.models.geo import StationExpressAlias, Country
from common.models.schedule import RThreadType
from common.models.transport import TransportType
from common.tester.factories import create_settlement, create_station, create_thread
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting_key
from common.utils.date import MSK_TZ

from travel.proto.dicts.trains.station_express_alias_pb2 import TStationExpressAlias
from travel.rasp.train_api.tariffs.train.factories.base import create_train_tariffs_query
from travel.rasp.train_api.tariffs.train.factories.im import ImTrainPricingResponseFactory
from travel.rasp.train_api.tariffs.train.im.send_query import (
    get_result_from_partner_and_add_schedule_info, TRAIN_PRICING_ENDPOINT
)
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner


@pytest.mark.dbuser
@replace_now('2020-02-12')
@pytest.mark.parametrize('protobufs_setting_value', (True, False))
def test_through_train_segments(httpretty, protobufs_setting_value, protobuf_data_provider):
    """
    Тестируем ситуацию, когда в поиске два сегмента обычный и безпересадочный.
    Сегмент с ценами приходит один. В итоге должен получиться один сегмент с ценами.
    """
    with replace_dynamic_setting_key('TRAIN_BACKEND_USE_PROTOBUFS', 'alias', protobufs_setting_value):
        create_ru_station = create_station.mutate(
            country=Country.RUSSIA_ID,
            settlement=create_settlement(country=Country.RUSSIA_ID),
            t_type=TransportType.TRAIN_ID,
        )
        start_station = create_ru_station(title='Start station', __={'codes': {'express': 'START_EXPRESS_CODE'}})
        station_from = create_ru_station(title='From station', __={'codes': {'express': 'FROM_EXPRESS_CODE'}})
        station_to = create_ru_station(title='To station', __={'codes': {'express': 'TO_EXPRESS_CODE'}})
        end_1 = create_ru_station(title='End 1 station', __={'codes': {'express': 'END_1_EXPRESS_CODE'}})
        end_2 = create_ru_station(title='End 2 station', __={'codes': {'express': 'END_2_EXPRESS_CODE'}})
        main_thread = create_thread(
            number='015А',
            tz_start_time=time(12, 00),
            t_type=TransportType.TRAIN_ID,
            schedule_v1=[
                [None, 0, start_station],
                [19, 20, station_from],
                [40, 50, station_to],
                [300, None, end_1],
            ],
            __={'calculate_noderoute': True}
        )
        create_thread(
            number='015А',
            tz_start_time=time(12, 00),
            t_type=TransportType.TRAIN_ID,
            type=RThreadType.THROUGH_TRAIN_ID,
            schedule_v1=[
                [None, 0, start_station],
                [19, 20, station_from],
                [50, 60, station_to],
                [400, None, end_2],
            ],
            __={'calculate_noderoute': True}
        )
        if protobufs_setting_value:
            for alias_proto in (
                TStationExpressAlias(StationId=start_station.id, Alias='START'),
                TStationExpressAlias(StationId=end_1.id, Alias='END 1'),
                TStationExpressAlias(StationId=end_2.id, Alias='END 2')
            ):
                protobuf_data_provider.alias_repo.add(alias_proto.SerializeToString())
        else:
            StationExpressAlias.objects.create(station=start_station, alias='START')
            StationExpressAlias.objects.create(station=end_1, alias='END 1')
            StationExpressAlias.objects.create(station=end_2, alias='END 2')
        mock_im(httpretty, TRAIN_PRICING_ENDPOINT, json=ImTrainPricingResponseFactory(
            Trains=[
                {
                    'TrainNumber': '015А',
                    'DisplayTrainNumber': '015А',
                    'TrainNumberToGetRoute': '015А',
                    'DepartureDateTime': datetime(2020, 2, 25, 12, 20),
                    'ArrivalDateTime': datetime(2020, 2, 26, 12, 20),
                    'OriginName': 'START',
                    'DestinationName': 'END 1',
                    'OriginStationCode': 'FROM_EXPRESS_CODE',
                    'DestinationStationCode': 'TO_EXPRESS_CODE',
                },
            ]
        ))

        query = create_train_tariffs_query(TrainPartner.IM, station_from, station_to, date(2020, 2, 25))
        result = get_result_from_partner_and_add_schedule_info(query, include_reason_for_missing_prices=True)

        assert len(result.segments) == 1, 'Должен быть только сегмент с ценами'
        assert_that(result.segments, contains_inanyorder(
            has_properties(
                number='015А',
                start_station=start_station,
                can_supply_segments=True,
                station_from=station_from,
                original_number='015А',
                end_station=end_1,
                station_to=station_to,
                key='train 015А 20200225_1220',
                first_country_code='RU',
                last_country_code='RU',
                tariffs=has_entries(
                    broken_classes=empty(),
                    classes=has_entries(platzkarte=anything()),
                ),
                thread=main_thread,
                departure=MSK_TZ.localize(datetime(2020, 2, 25, 12, 20)),
                arrival=MSK_TZ.localize(datetime(2020, 2, 26, 12, 20)),
            ),
        ))
