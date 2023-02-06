import logging
import pkgutil
import pytest

from mock import patch

from travel.avia.flight_status_fetcher.sources.sever_aero import SeverAero
from travel.avia.flight_status_fetcher.tests import avia_backend_mock
from travel.avia.flight_status_fetcher.tests.test_sources.helpers import delete_fields_from_dict


@pytest.fixture()
def sever_aero():
    _sever_aero = SeverAero(
        avia_backend_mock.AviaBackendMock(),
        pkgutil.get_data('tests', 'resources/subjects.xml').decode('utf-8'),
        pkgutil.get_data('tests', 'resources/timetable.xml').decode('utf-8'),
    )
    return _sever_aero


def test_get_iata_or_sirena_code_by_name_ru(sever_aero: SeverAero):
    assert sever_aero.get_iata_or_sirena_code_by_name_ru('Усть-Нера') == 'USR'


def test_get_iata_or_sirena_code_by_name_ru_via_sirena_name(sever_aero: SeverAero):
    assert sever_aero.get_iata_or_sirena_code_by_name_ru('Якутск') == 'YKS'
    assert sever_aero.get_iata_or_sirena_code_by_name_ru('Батагай') == 'BTG'


def test_get_iata_or_sirena_code_by_internal_code(sever_aero: SeverAero):
    assert sever_aero.get_iata_or_sirena_code_by_internal_code('000000079') == 'USR'


def test_statuses(sever_aero: SeverAero):
    logger = logging.getLogger('travel.avia.flight_status_fetcher.sources.sever_aero')
    with patch.object(logger, 'error') as mock_error:
        result = list(sever_aero._get_statuses())
        mock_error.assert_not_called()

    assert len(result) == 104

    expected_flight = {
        'airline_id': 16,
        'airline_code': 'ЯК',
        'airport': 'BTG',
        'baggage_carousels': None,
        'check_in_desks': None,
        'direction': 'arrival',
        'diverted': False,
        'diverted_airport_iata': None,
        'diverted_airport_code': None,
        'flight_date': '2019-04-29',
        'flight_number': '415',
        'gate': None,
        'route_point_from': 'YKS',
        'route_point_to': 'BTG',
        'source': 'airport',
        'status': 'arrived',
        'terminal': None,
        'time_actual': '2019-04-29T10:38:00',
        'time_scheduled': '2019-04-29T09:55:00',
    }

    flight = delete_fields_from_dict(result[0].fields_dict)
    shared_flight = delete_fields_from_dict(result[1].fields_dict)

    assert flight == expected_flight

    expected_flight['flight_number'] = '423'
    assert shared_flight == expected_flight
