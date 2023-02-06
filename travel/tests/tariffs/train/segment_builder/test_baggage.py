# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date, time, datetime

import pytest
from hamcrest import assert_that, contains_inanyorder, has_properties, has_entries, empty

from common.models.geo import StationExpressAlias, Country
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
from travel.rasp.train_api.tariffs.train.segment_builder.prices_and_reasons import SOLD_OUT_BROKEN_CLASSES
from travel.rasp.train_api.train_partners.im.factories.utils import mock_im
from travel.rasp.train_api.train_purchase.core.enums import TrainPartner


@pytest.mark.dbuser
@replace_now('2020-02-19')
@pytest.mark.parametrize('protobufs_setting_value', (True, False))
def test_do_not_count_baggage(httpretty, protobufs_setting_value, protobuf_data_provider):
    """
    Цены из ответа ИМ, оставшиеся только на багажные вагоны, должны приводить к обычному SOLD_OUT.
    """
    with replace_dynamic_setting_key('TRAIN_BACKEND_USE_PROTOBUFS', 'alias', protobufs_setting_value):
        create_ru_station = create_station.mutate(
            country=Country.RUSSIA_ID,
            settlement=create_settlement(country=Country.RUSSIA_ID),
            t_type=TransportType.TRAIN_ID,
        )
        station_from = create_ru_station(title='From station', __={'codes': {'express': 'FROM_EXPRESS_CODE'}})
        station_to = create_ru_station(title='To station', __={'codes': {'express': 'TO_EXPRESS_CODE'}})
        thread = create_thread(
            number='030А',
            tz_start_time=time(1, 15),
            t_type=TransportType.TRAIN_ID,
            schedule_v1=[
                [None, 0, station_from],
                [300, None, station_to],
            ],
            __={'calculate_noderoute': True},
        )
        if protobufs_setting_value:
            for alias_proto in (
                TStationExpressAlias(StationId=station_from.id, Alias='IM FROM'),
                TStationExpressAlias(StationId=station_to.id, Alias='IM TO')
            ):
                protobuf_data_provider.alias_repo.add(alias_proto.SerializeToString())
        else:
            StationExpressAlias.objects.create(station=station_from, alias='IM FROM')
            StationExpressAlias.objects.create(station=station_to, alias='IM TO')
        mock_im(httpretty, TRAIN_PRICING_ENDPOINT, json=ImTrainPricingResponseFactory(
            Trains=[
                {
                    "TrainNumber": "030А",
                    "TrainNumberToGetRoute": "030А",
                    "DisplayTrainNumber": "030А",
                    "DepartureDateTime": "2020-02-21T01:15:00",
                    "LocalDepartureDateTime": "2020-02-21T01:15:00",
                    "ArrivalDateTime": "2020-02-21T10:04:00",
                    'OriginName': 'IM FROM',
                    'DestinationName': 'IM TO',
                    'OriginStationCode': 'FROM_EXPRESS_CODE',
                    'DestinationStationCode': 'TO_EXPRESS_CODE',
                    "CarGroups": [
                        {
                            "CarType": "Baggage",
                            "CarTypeName": "БАГАЖ",
                            "PlaceQuantity": 8,
                            "LowerPlaceQuantity": 0,
                            "UpperPlaceQuantity": 0,
                            "LowerSidePlaceQuantity": 0,
                            "UpperSidePlaceQuantity": 0,
                            "MalePlaceQuantity": 0,
                            "FemalePlaceQuantity": 0,
                            "EmptyCabinQuantity": 0,
                            "MixedCabinQuantity": 0,
                            "MinPrice": 0.0,
                            "MaxPrice": 0.0,
                            "IsSaleForbidden": False,
                            "AvailabilityIndication": "ServiceNotAllowed",
                            "ServiceCosts": [0.0],
                            "HasNonRefundableTariff": False,
                            "TotalPlaceQuantity": 8,
                            "PlaceReservationTypes": ["Usual"],
                            "IsTransitDocumentRequired": False,
                        }
                    ],
                }
            ]
        ))

        query = create_train_tariffs_query(TrainPartner.IM, station_from, station_to, date(2020, 2, 21))
        result = get_result_from_partner_and_add_schedule_info(query, include_reason_for_missing_prices=True)

        import logging
        log = logging.getLogger(__name__)
        for s in result.segments:
            log.info('segment %s', s)
        assert len(result.segments) == 2, 'Должены быть два поисковых сегмента, которые отличаются только ключом'
        assert_that(result.segments, contains_inanyorder(
            has_properties(
                number='030А',
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
                departure=MSK_TZ.localize(datetime(2020, 2, 21, 1, 15)),
                arrival=MSK_TZ.localize(datetime(2020, 2, 21, 6, 15)),
                key='train 030А 20200221_0115',
            ),
            has_properties(
                number='030А',
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
                departure=MSK_TZ.localize(datetime(2020, 2, 21, 1, 15)),
                arrival=MSK_TZ.localize(datetime(2020, 2, 21, 6, 15)),
                key='train 029А 20200221_0115',
            ),
        ))
