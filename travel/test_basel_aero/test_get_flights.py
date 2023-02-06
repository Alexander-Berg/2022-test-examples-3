import logging
import pkgutil

from travel.avia.flight_status_fetcher.sources.basel import BaselImporter
from travel.avia.flight_status_fetcher.tests import basic_status_asserts
from travel.avia.flight_status_fetcher.tests.test_sources.helpers import (
    delete_fields_from_dict,
    mock_flight_number_parser,
)

logger = logging.getLogger(__name__)


def test_get_basel_flights():
    importer = BaselImporter(
        iata='aer',
        wsdl_client=None,
        flight_number_parser=mock_flight_number_parser(
            logger,
            {
                'ЮТ': 1,
                'С7': 2,
                'А4': 3,
                'КЛ': 4,
                'FV': 5,
                'WDL': 6,
                'UK': 7,
            },
        ),
        logger=logger,
    )
    xml = pkgutil.get_data('tests', 'test_sources/test_basel_aero/response.xml').decode('utf-8')
    flights = list(importer.build_statuses(xml, 'aer'))

    basic_status_asserts(importer, flights, 7, company_not_found=1)

    assert delete_fields_from_dict(flights[0].fields_dict) == {
        'airline_id': 1,
        'airline_code': 'ЮТ',
        'airport': 'AER',
        'baggage_carousels': None,
        'check_in_desks': None,
        'direction': 'arrival',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-05-05',
        'flight_number': '249',
        'gate': None,
        'route_point_from': 'VKO',
        'route_point_to': 'AER',
        'source': 'airport',
        'status': 'cancelled',
        'terminal': '',
        'time_actual': None,
        'time_scheduled': '2020-05-05T12:15:00',
    }

    assert delete_fields_from_dict(flights[1].fields_dict) == {
        'airline_id': 2,
        'airport': 'AER',
        'airline_code': 'С7',
        'baggage_carousels': None,
        'check_in_desks': '7-8',
        'direction': 'departure',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-05-05',
        'flight_number': '5104',
        'gate': '9',
        'route_point_from': 'AER',
        'route_point_to': 'OVB',
        'source': 'airport',
        'status': 'departed',
        'terminal': '1',
        'time_actual': '2020-05-05T13:25:00',
        'time_scheduled': '2020-05-05T13:25:00',
    }

    assert delete_fields_from_dict(flights[2].fields_dict) == {
        'airline_id': 3,
        'airport': 'AER',
        'airline_code': 'А4',
        'baggage_carousels': None,
        'check_in_desks': None,
        'direction': 'arrival',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-05-07',
        'flight_number': '223',
        'gate': None,
        'route_point_from': 'ROV',
        'route_point_to': 'AER',
        'source': 'airport',
        'status': 'wait',
        'terminal': '',
        'time_actual': None,
        'time_scheduled': '2020-05-07T10:50:00',
    }

    assert delete_fields_from_dict(flights[3].fields_dict) == {
        'airline_id': 4,
        'airport': 'AER',
        'airline_code': 'КЛ',
        'baggage_carousels': None,
        'check_in_desks': None,
        'direction': 'arrival',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-08-04',
        'flight_number': '225',
        'gate': None,
        'route_point_from': 'SVO',
        'route_point_to': 'AER',
        'source': 'airport',
        'status': 'cancelled',
        'terminal': '',
        'time_actual': None,
        'time_scheduled': '2020-08-04T18:30:00',
    }

    assert delete_fields_from_dict(flights[4].fields_dict) == {
        'airline_id': 5,
        'airport': 'AER',
        'airline_code': 'FV',
        'baggage_carousels': None,
        'check_in_desks': None,
        'direction': 'departure',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-06-22',
        'flight_number': '6530',
        'gate': None,
        'route_point_from': 'AER',
        'route_point_to': 'UFA',
        'source': 'airport',
        'status': 'wait',
        'terminal': '',
        'time_actual': '2020-06-22T03:55:00',
        'time_scheduled': '2020-06-22T02:55:00',
    }

    assert delete_fields_from_dict(flights[5].fields_dict) == {
        'airline_id': 6,
        'airport': 'AER',
        'airline_code': 'WDL',
        'baggage_carousels': None,
        'check_in_desks': None,
        'direction': 'departure',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-09-14',
        'flight_number': '116F',
        'gate': None,
        'route_point_from': 'AER',
        'route_point_to': 'CGN',
        'source': 'airport',
        'status': 'departed',
        'terminal': '',
        'time_actual': '2020-09-14T14:53:00',
        'time_scheduled': '2020-09-14T13:30:00',
    }

    assert delete_fields_from_dict(flights[6].fields_dict) == {
        'airline_id': 7,
        'airport': 'AER',
        'airline_code': 'UK',
        'baggage_carousels': None,
        'check_in_desks': None,
        'direction': 'departure',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-12-31',
        'flight_number': '29501',
        'gate': None,
        'route_point_from': 'AER',
        'route_point_to': 'NCU',
        'source': 'airport',
        'status': 'wait',
        'terminal': '',
        'time_actual': '2020-12-31T13:00:00',
        'time_scheduled': '2020-12-31T13:00:00',
    }
