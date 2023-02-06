import logging
import pkgutil

from travel.avia.flight_status_fetcher.settings import app
from travel.avia.flight_status_fetcher.settings.sources import region
from travel.avia.flight_status_fetcher.sources.region_airports import RegionAirportImporter
from travel.avia.flight_status_fetcher.tests import basic_status_asserts
from travel.avia.flight_status_fetcher.tests.test_sources.helpers import (
    delete_fields_from_dict,
    mock_flight_number_parser,
)

logger = logging.getLogger(__name__)


def test_get_expected_time():
    importer = RegionAirportImporter(
        iata='svx',
        retry_settings=app.AIRPORT_IMPORTER_RETRY_SETTINGS,
        flight_number_parser=mock_flight_number_parser(
            logger,
            {
                'U6': 30,
            },
        ),
        logger=logger,
        request_timeout=region.REGION_AIRPORT_TIMEOUT,
        iata_to_url=region.REGION_AIRPORT_IATA,
    )
    xml = pkgutil.get_data('tests', 'test_sources/test_region_airports/response_svx.xml').decode('utf-8')
    statuses = list(importer.build_statuses(xml, 'svx'))

    basic_status_asserts(importer, statuses, 1)

    result0 = delete_fields_from_dict(statuses[0].fields_dict)
    assert result0 == {
        'airline_id': 30,
        'airline_code': 'U6',
        'airport': 'SVX',
        'baggage_carousels': '8',
        'check_in_desks': None,
        'direction': 'arrival',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-09-17',
        'flight_number': '173',
        'gate': None,
        'route_point_from': 'ПЛК',
        'route_point_to': 'ЕКБ',
        'source': 'airport',
        'status': 'delay',
        'terminal': 'A',
        'time_actual': '2020-09-18T00:05:00',
        'time_scheduled': '2020-09-17T21:55:00',
    }


def test_get_multileg_flights():
    importer = RegionAirportImporter(
        iata='kuf',
        retry_settings=app.AIRPORT_IMPORTER_RETRY_SETTINGS,
        flight_number_parser=mock_flight_number_parser(
            logger,
            {
                'IO': 105,
                'WZ': 617,
            },
        ),
        logger=logger,
        request_timeout=region.REGION_AIRPORT_TIMEOUT,
        iata_to_url=region.REGION_AIRPORT_IATA,
    )
    xml = pkgutil.get_data('tests', 'test_sources/test_region_airports/response_kuf.xml').decode('utf-8')
    statuses = list(importer.build_statuses(xml, 'KUF'))

    basic_status_asserts(importer, statuses, 2)

    result0 = delete_fields_from_dict(statuses[0].fields_dict)
    assert result0 == {
        'airline_id': 105,
        'airline_code': 'IO',
        'airport': 'KUF',
        'baggage_carousels': '3',
        'check_in_desks': None,
        'direction': 'arrival',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-09-08',
        'flight_number': '397',
        'gate': None,
        'route_point_from': 'НЖС',
        'route_point_to': 'СКЧ',
        'source': 'airport',
        'status': 'delay',
        'terminal': 'A',
        'time_actual': None,
        'time_scheduled': '2020-09-08T15:20:00',
    }

    result1 = delete_fields_from_dict(statuses[1].fields_dict)
    assert result1 == {
        'airline_id': 617,
        'airline_code': 'WZ',
        'airport': 'KUF',
        'baggage_carousels': None,
        'check_in_desks': None,
        'direction': 'departure',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-09-16',
        'flight_number': '5582',
        'gate': None,
        'route_point_from': 'СКЧ',
        'route_point_to': 'ПЛК',
        'source': 'airport',
        'status': 'delay',
        'terminal': 'A',
        'time_actual': None,
        'time_scheduled': '2020-09-16T20:05:00',
    }


def test_get_rov_flights():
    importer = RegionAirportImporter(
        iata='rov',
        retry_settings=app.AIRPORT_IMPORTER_RETRY_SETTINGS,
        flight_number_parser=mock_flight_number_parser(
            logger,
            {
                'A4': 1,
                'SU': 26,
                'FV': 53,
            },
        ),
        logger=logger,
        request_timeout=region.REGION_AIRPORT_TIMEOUT,
        iata_to_url=region.REGION_AIRPORT_IATA,
    )
    xml = pkgutil.get_data('tests', 'test_sources/test_region_airports/response_rov.xml').decode('utf-8')
    statuses = list(importer.build_statuses(xml, 'ROV'))

    basic_status_asserts(importer, statuses, 4)

    result = delete_fields_from_dict(statuses[0].fields_dict)
    assert result == {
        'airline_id': 1,
        'airline_code': 'A4',
        'airport': 'ROV',
        'baggage_carousels': None,
        'check_in_desks': None,
        'direction': 'arrival',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-04-18',
        'flight_number': '208',
        'gate': None,
        'route_point_from': 'СКЧ',
        'route_point_to': 'РОВ',
        'source': 'airport',
        'status': 'delay',
        'terminal': 'A',
        'time_actual': None,
        'time_scheduled': '2020-04-18T19:50:00',
    }

    result = delete_fields_from_dict(statuses[1].fields_dict)
    assert result == {
        'airline_id': 26,
        'airline_code': 'SU',
        'airport': 'ROV',
        'baggage_carousels': '3',
        'check_in_desks': None,
        'direction': 'arrival',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-04-16',
        'flight_number': '6371',
        'gate': None,
        'route_point_from': 'ПЛК',
        'route_point_to': 'РОВ',
        'source': 'airport',
        'status': 'arrived',
        'terminal': 'A',
        'time_actual': '2020-04-16T10:38:00',
        'time_scheduled': '2020-04-16T11:10:00',
    }

    result = delete_fields_from_dict(statuses[2].fields_dict)
    assert result == {
        'airline_id': 53,
        'airline_code': 'FV',
        'airport': 'ROV',
        'baggage_carousels': None,
        'check_in_desks': '17',
        'direction': 'departure',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-04-16',
        'flight_number': '6372',
        'gate': '12',
        'route_point_from': 'РОВ',
        'route_point_to': 'ПЛК',
        'source': 'airport',
        'status': 'departed',
        'terminal': 'A',
        'time_actual': '2020-04-16T12:07:00',
        'time_scheduled': '2020-04-16T12:05:00',
    }

    result = delete_fields_from_dict(statuses[3].fields_dict)
    assert result == {
        'airline_id': 26,
        'airline_code': 'SU',
        'airport': 'ROV',
        'baggage_carousels': None,
        'check_in_desks': '17',
        'direction': 'departure',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-04-17',
        'flight_number': '6372',
        'gate': None,
        'route_point_from': 'РОВ',
        'route_point_to': 'ПЛК',
        'source': 'airport',
        'status': 'delay',
        'terminal': 'A',
        'time_actual': None,
        'time_scheduled': '2020-04-17T12:05:00',
    }


def test_normalized_flights():
    importer = RegionAirportImporter(
        iata='gsv',
        retry_settings=app.AIRPORT_IMPORTER_RETRY_SETTINGS,
        flight_number_parser=mock_flight_number_parser(
            logger,
            {
                'EO': 196,
                'N4': 2543,
            },
        ),
        logger=logger,
        request_timeout=region.REGION_AIRPORT_TIMEOUT,
        iata_to_url=region.REGION_AIRPORT_IATA,
    )
    xml = pkgutil.get_data('tests', 'test_sources/test_region_airports/response_gsv.xml').decode('utf-8')
    statuses = list(importer.build_statuses(xml, 'GSV'))

    basic_status_asserts(importer, statuses, 2)

    expected = {
        'airline_id': 196,
        'airline_code': 'EO',
        'airport': 'GSV',
        'baggage_carousels': None,
        'check_in_desks': '9',
        'direction': 'departure',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2020-10-26',
        'flight_number': '54',
        'gate': '5',
        'route_point_from': 'ГСВ',
        'route_point_to': 'МРВ',
        'source': 'airport',
        'status': 'departed',
        'terminal': 'A',
        'time_actual': '2020-10-26T18:45:00',
        'time_scheduled': '2020-10-26T18:45:00',
    }

    assert delete_fields_from_dict(statuses[0].fields_dict) == expected

    expected['airline_code'] = 'N4'
    expected['airline_id'] = 2543
    assert delete_fields_from_dict(statuses[1].fields_dict) == expected


def test_nux_flights():
    importer = RegionAirportImporter(
        iata='nux',
        retry_settings=app.AIRPORT_IMPORTER_RETRY_SETTINGS,
        flight_number_parser=mock_flight_number_parser(
            logger,
            {
                'UT': 1,
            },
        ),
        logger=logger,
        request_timeout=region.REGION_AIRPORT_TIMEOUT,
        iata_to_url=region.REGION_AIRPORT_IATA,
    )
    xml = pkgutil.get_data('tests', 'test_sources/test_region_airports/response_nux.xml').decode('utf-8')
    statuses = list(importer.build_statuses(xml, 'NUX'))

    basic_status_asserts(importer, statuses, 1)


def test_goj_flights():
    importer = RegionAirportImporter(
        iata='goj',
        retry_settings=app.AIRPORT_IMPORTER_RETRY_SETTINGS,
        flight_number_parser=mock_flight_number_parser(
            logger,
            {
                'FV': 1,
            },
        ),
        logger=logger,
        request_timeout=region.REGION_AIRPORT_TIMEOUT,
        iata_to_url=region.REGION_AIRPORT_IATA,
    )
    xml = pkgutil.get_data('tests', 'test_sources/test_region_airports/response_goj.xml').decode('utf-8')
    statuses = list(importer.build_statuses(xml, 'GOJ'))

    basic_status_asserts(importer, statuses, 1)
