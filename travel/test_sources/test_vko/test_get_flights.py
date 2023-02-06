import logging

from travel.avia.flight_status_fetcher.settings import app
from travel.avia.flight_status_fetcher.settings.sources import vko
from travel.avia.flight_status_fetcher.sources.vko import VKOImporter
from travel.avia.flight_status_fetcher.tests import basic_status_asserts
from travel.avia.flight_status_fetcher.tests.test_sources.helpers import delete_fields_from_dict
from travel.avia.flight_status_fetcher.tests.test_sources.helpers import mock_flight_number_parser

logger = logging.getLogger(__name__)


xml_test = '''
<ROWSET>
  <ROW num="1">
    <SYSTIMESTAMP>28.01.2020 19:27:01</SYSTIMESTAMP>
    <FLT_ID>100153306</FLT_ID>
    <FLIGHT_NUMBER_FOR_PASS>ФВ 6023</FLIGHT_NUMBER_FOR_PASS>
    <BOUND>1</BOUND>
    <AIRLINE_VNT>ФВ</AIRLINE_VNT>
    <AIRLINE_IATA>FV</AIRLINE_IATA>
    <AIRLINE_NAME_RUS>РОССИЯ</AIRLINE_NAME_RUS>
    <AIRLINE_NAME_ENG>"Rossiya Airlines" JSC</AIRLINE_NAME_ENG>
    <ST>28.01.2020 21:20:00</ST>
    <ET_FOR_PASS>28.01.2020 21:20:00</ET_FOR_PASS>
    <AC_TYPE_NAME_RUS>Аэробус А319</AC_TYPE_NAME_RUS>
    <AC_TYPE_NAME_ENG>Airbus Industrie A319</AC_TYPE_NAME_ENG>
    <AC_REG>VPBIV</AC_REG>
    <TERM>А</TERM>
    <HANDLING_TYPE_FOR_PASS>Пассажирский</HANDLING_TYPE_FOR_PASS>
    <CATEGORY>В</CATEGORY>
    <BELT>А05</BELT>
    <FLS_IATA>LED</FLS_IATA>
    <FLS_FULL_NAME_RUS>САНКТ-ПЕТЕРБУРГ(ПУЛКОВО)</FLS_FULL_NAME_RUS>
    <FLS_FULL_NAME_ENG>St Petersburg(Pulkovo)</FLS_FULL_NAME_ENG>
    <FLS_ST>28.01.2020 19:45:00</FLS_ST>
    <FLS_ET>28.01.2020 19:45:00</FLS_ET>
    <CHECKIN_LIST_ACTUAL_FOR_PASS>126,127A,127B,128,140</CHECKIN_LIST_ACTUAL_FOR_PASS>
  </ROW>
</ROWSET>
'''


def test_get_vko_flights():
    importer = VKOImporter(
        'no-data-url',
        'no-file',
        'no-user',
        'no-password',
        logger=logger,
        flight_number_parser=mock_flight_number_parser(
            logger,
            {
                'FV': 53,
            },
        ),
        retry_settings=app.AIRPORT_IMPORTER_RETRY_SETTINGS,
        request_timeout=vko.VKO_REQUEST_TIMEOUT,
    )
    flights = list(importer.collect_statuses(xml_test).statuses)

    basic_status_asserts(importer, flights, 1)

    result = delete_fields_from_dict(flights[0].fields_dict)
    assert result == {
        'airline_id': 53,
        'airline_code': 'FV',
        'airport': 'VKO',
        'baggage_carousels': 'А05',
        'check_in_desks': '126-128,140',
        'direction': 'arrival',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-01-28',
        'flight_number': '6023',
        'gate': None,
        'route_point_from': 'LED',
        'route_point_to': 'VKO',
        'source': 'airport',
        'status': 'no-data',
        'terminal': 'А',
        'time_actual': '2020-01-28T21:20:00',
        'time_scheduled': '2020-01-28T21:20:00',
    }
