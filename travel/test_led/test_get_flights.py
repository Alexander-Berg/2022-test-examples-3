import logging

from travel.avia.flight_status_fetcher.settings.sources.led import LED_RETRY_SETTINGS, LED_TIMEOUT
from travel.avia.flight_status_fetcher.sources.led import LEDImporter
from travel.avia.flight_status_fetcher.tests import basic_status_asserts
from travel.avia.flight_status_fetcher.tests.test_sources.helpers import (
    delete_fields_from_dict,
    mock_flight_number_parser,
)

logger = logging.getLogger(__name__)

arrival_xml = '''
<Flights>
    <row>
        <OA_ID>1903032</OA_ID>
        <OA_OD_ID>1903061</OA_OD_ID>
        <OA_FLIGHT_NUMBER>SU  034</OA_FLIGHT_NUMBER>
        <OA_STATUS_EN>Arrived</OA_STATUS_EN>
        <OA_STATUS_RU>Прибыл</OA_STATUS_RU>
        <OA_RFS_CODE>ONB</OA_RFS_CODE>
        <OA_RDI_CODE>P</OA_RDI_CODE>
        <OA_RAL_CODE>SU</OA_RAL_CODE>
        <OA_RAL_NAME_EN>Aeroflot</OA_RAL_NAME_EN>
        <OA_RAL_NAME_RUS>Аэрофлот</OA_RAL_NAME_RUS>
        <OA_RACT_CODE>32A</OA_RACT_CODE>
        <OA_RACT_ICAO_CODE>A320</OA_RACT_ICAO_CODE>
        <OA_RAC_CODE>VPBII</OA_RAC_CODE>
        <OA_RAP_CODE_ORIGIN>SVO</OA_RAP_CODE_ORIGIN>
        <OA_RAP_ORIGIN_NAME_EN>Moscow (SVO)</OA_RAP_ORIGIN_NAME_EN>
        <OA_RAP_ORIGIN_NAME_RU>Москва (SVO)</OA_RAP_ORIGIN_NAME_RU>
        <OA_RAP_ORIGIN_NAME_CN>莫斯科 (SVO)</OA_RAP_ORIGIN_NAME_CN>
        <OA_RAP_CODE_ORIGIN_ATD>2020-01-27T22:46:00.000</OA_RAP_CODE_ORIGIN_ATD>
        <OA_RAP_CODE_PREVIOUS>SVO</OA_RAP_CODE_PREVIOUS>
        <OA_RAP_PREVIOUS_NAME_EN>Moscow (SVO)</OA_RAP_PREVIOUS_NAME_EN>
        <OA_RAP_PREVIOUS_NAME_RU>Москва (SVO)</OA_RAP_PREVIOUS_NAME_RU>
        <OA_RAP_PREVIOUS_NAME_CN>莫斯科 (SVO)</OA_RAP_PREVIOUS_NAME_CN>
        <OA_RAP_CODE_PREVIOUS_ATD>2020-01-27T22:46:00.000</OA_RAP_CODE_PREVIOUS_ATD>
        <OA_STA>2020-01-28T00:15:00.000</OA_STA>
        <OA_ETA>2020-01-27T23:51:00.000</OA_ETA>
        <OA_ATA>2020-01-27T23:47:00.000</OA_ATA>
        <OA_ONBLOCK>2020-01-27T23:51:00.000</OA_ONBLOCK>
        <OA_RSP_CODE>109B</OA_RSP_CODE>
        <OA_RTRM_CODE>1</OA_RTRM_CODE>
        <OA_RST_CODE>J</OA_RST_CODE>
        <OA_RCT_CODE>D</OA_RCT_CODE>
        <OA_PAX_TOTAL>65</OA_PAX_TOTAL>
    </row>
</Flights>
'''
departure_xml = '''
<Flights>
    <row>
        <OD_ID>1902841</OD_ID>
        <OD_OA_ID>1902105</OD_OA_ID>
        <OD_FLIGHT_NUMBER>FV 6581</OD_FLIGHT_NUMBER>
        <OD_STATUS_EN>Departed</OD_STATUS_EN>
        <OD_STATUS_RU>Отправлен</OD_STATUS_RU>
        <OD_RFS_CODE>OFF</OD_RFS_CODE>
        <OD_RDI_CODE>P</OD_RDI_CODE>
        <OD_RAL_CODE>FV</OD_RAL_CODE>
        <OD_RAL_NAME_EN>Rossiya</OD_RAL_NAME_EN>
        <OD_RAL_NAME_RUS>Россия</OD_RAL_NAME_RUS>
        <OD_RACT_CODE>319</OD_RACT_CODE>
        <OD_RACT_ICAO_CODE>A319</OD_RACT_ICAO_CODE>
        <OD_RAC_CODE>VPBNJ</OD_RAC_CODE>
        <OD_RAP_CODE_NEXT>PEE</OD_RAP_CODE_NEXT>
        <OD_RAP_NEXT_NAME_EN>Perm</OD_RAP_NEXT_NAME_EN>
        <OD_RAP_NEXT_NAME_RU>Пермь</OD_RAP_NEXT_NAME_RU>
        <OD_RAP_NEXT_NAME_CN>彼尔姆</OD_RAP_NEXT_NAME_CN>
        <OD_RAP_NEXT_RCO_CODE>RU</OD_RAP_NEXT_RCO_CODE>
        <OD_RAP_NEXT_RIAR_CODE>EUR</OD_RAP_NEXT_RIAR_CODE>
        <OD_RAP_CODE_DESTINATION>PEE</OD_RAP_CODE_DESTINATION>
        <OD_RAP_DESTINATION_NAME_EN>Perm</OD_RAP_DESTINATION_NAME_EN>
        <OD_RAP_DESTINATION_NAME_RU>Пермь</OD_RAP_DESTINATION_NAME_RU>
        <OD_RAP_DESTINATION_NAME_CN>彼尔姆</OD_RAP_DESTINATION_NAME_CN>
        <OD_RAP_DESTINATION_RCO_CODE>RU</OD_RAP_DESTINATION_RCO_CODE>
        <OD_RAP_DESTINATION_RIAR_CODE>EUR</OD_RAP_DESTINATION_RIAR_CODE>
        <OD_STD>2020-01-28T00:05:00.000</OD_STD>
        <OD_ETD>2020-01-28T00:05:00.000</OD_ETD>
        <OD_OFFBLOCK>2020-01-28T00:24:00.000</OD_OFFBLOCK>
        <OD_ATD>2020-01-28T00:30:00.000</OD_ATD>
        <OD_RSP_CODE>133</OD_RSP_CODE>
        <OD_RTRM_CODE>1</OD_RTRM_CODE>
        <OD_RST_CODE>J</OD_RST_CODE>
        <OD_RCT_CODE>D</OD_RCT_CODE>
        <OD_PAX_TOTAL>50</OD_PAX_TOTAL>
        <OD_FLIGHT_NUMBER_K1>SU 6581</OD_FLIGHT_NUMBER_K1>
        <OD_COUNTERS>A220-A224</OD_COUNTERS>
        <OD_COUNTER_BEGIN_PLAN>2020-01-27T22:05:00.000</OD_COUNTER_BEGIN_PLAN>
        <OD_COUNTER_END_PLAN>2020-01-27T23:25:00.000</OD_COUNTER_END_PLAN>
        <OD_COUNTER_BEGIN_ACTUAL>2020-01-27T04:17:00.000</OD_COUNTER_BEGIN_ACTUAL>
        <OD_COUNTER_END_ACTUAL>2020-01-27T23:25:00.000</OD_COUNTER_END_ACTUAL>
        <OD_GATES>D22</OD_GATES>
        <OD_BOARDING_BEGIN_PLAN>2020-01-27T23:25:00.000</OD_BOARDING_BEGIN_PLAN>
        <OD_BOARDING_END_PLAN>2020-01-27T23:45:00.000</OD_BOARDING_END_PLAN>
        <OD_BOARDING_BEGIN_ACTUAL>2020-01-27T23:31:00.000</OD_BOARDING_BEGIN_ACTUAL>
        <OD_BOARDING_BOARDING_ACTUAL>2020-01-27T23:31:00.000</OD_BOARDING_BOARDING_ACTUAL>
        <OD_BOARDING_BOARDING_SECOND>2020-01-27T23:41:00.000</OD_BOARDING_BOARDING_SECOND>
        <OD_BOARDING_END_ACTUAL>2020-01-27T23:55:00.000</OD_BOARDING_END_ACTUAL>
    </row>
</Flights>
'''


def test_get_led_flights():
    importer = LEDImporter(
        'host',
        'user',
        'password',
        'arrival-data',
        'departure-data',
        LED_RETRY_SETTINGS,
        flight_number_parser=mock_flight_number_parser(
            logger,
            {
                'FV': 53,
                'SU': 26,
            },
        ),
        logger=logger,
        timeout=LED_TIMEOUT,
    )

    flights = list(importer._build_flights_from_strings(arrival_xml, departure_xml))

    basic_status_asserts(importer, flights, 3)

    results = {
        '{}.{}'.format(
            item.fields_dict['airline_id'],
            item.fields_dict['flight_number'],
        ): item.fields_dict
        for item in flights
    }

    result0 = delete_fields_from_dict(results['53.6581'])
    assert result0 == {
        'airline_id': 53,
        'airline_code': 'FV',
        'airport': 'LED',
        'baggage_carousels': None,
        'check_in_desks': '220-224',
        'direction': 'departure',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-01-28',
        'flight_number': '6581',
        'gate': 'D22',
        'route_point_from': 'LED',
        'route_point_to': 'PEE',
        'source': 'airport',
        'status': 'departed',
        'terminal': None,
        'time_actual': '2020-01-28T00:30:00',
        'time_scheduled': '2020-01-28T00:05:00',
    }

    result1 = delete_fields_from_dict(results['26.6581'])
    assert result1 == {
        'airline_id': 26,
        'airline_code': 'SU',
        'airport': 'LED',
        'baggage_carousels': None,
        'check_in_desks': '220-224',
        'direction': 'departure',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-01-28',
        'flight_number': '6581',
        'gate': 'D22',
        'route_point_from': 'LED',
        'route_point_to': 'PEE',
        'source': 'airport',
        'status': 'departed',
        'terminal': None,
        'time_actual': '2020-01-28T00:30:00',
        'time_scheduled': '2020-01-28T00:05:00',
    }

    result2 = delete_fields_from_dict(results['26.34'])
    assert result2 == {
        'airline_id': 26,
        'airline_code': 'SU',
        'airport': 'LED',
        'baggage_carousels': None,
        'check_in_desks': None,
        'direction': 'arrival',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-01-28',
        'flight_number': '34',
        'gate': None,
        'route_point_from': 'SVO',
        'route_point_to': 'LED',
        'source': 'airport',
        'status': 'arrived',
        'terminal': None,
        'time_actual': '2020-01-27T23:47:00',
        'time_scheduled': '2020-01-28T00:15:00',
    }
