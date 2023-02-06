import pytest
import requests_mock

from travel.avia.library.python.iata_correction import IATACorrector


@pytest.mark.parametrize('test_input,expected', [
    ('SU 100', 26),
    ('SU100', 26),
    (('SU', '100'), 26),
    ('XX 100', None),
    (('XX', '100'), None),
    ('XX100', None),
    (('', '100'), None),
    ('100', None),
])
def test_flight_number_to_carrier(test_input, expected):
    iata_corrector = IATACorrector(shared_flights_api_base_url='http://example.com')

    with requests_mock.Mocker() as req_mock:
        _init_requests(req_mock, iata_corrector._shared_flights_base_url + '/flight-numbers-to-carriers', test_input)

        company_id = iata_corrector.flight_number_to_carrier(test_input)
        assert company_id == expected


def test_flight_numbers_to_carriers():
    iata_corrector = IATACorrector(shared_flights_api_base_url='http://example.com')

    test_input = [
        'SU 100',
        'SU100',
        ('SU', '100'),
        'XX100',
        ('XX', '100'),
        '100',
        ('', '100'),
    ]
    expected = {
        'SU 100': 26,
        ' SU100': 26,
        ' XX100': None,
        'XX 100': None,
        ' 100': None,
    }

    with requests_mock.Mocker() as req_mock:

        _init_requests(req_mock, iata_corrector._shared_flights_base_url + '/flight-numbers-to-carriers', test_input)

        result = iata_corrector.flight_numbers_to_carriers([
            'SU 100',
        ])
        assert result == expected


def _init_requests(mock, url, test_input):
    company_ids = {
        'SU': 26,
    }

    def get_company_id(f):
        for company_code in company_ids:
            if company_code in f:
                return company_ids[company_code]
        return None

    if not isinstance(test_input, list):
        test_input = [test_input]

    result = {}
    for flight_number in test_input:
        flight_number = IATACorrector._flight_number_to_string(flight_number)
        result[flight_number] = get_company_id(flight_number)

    mock.post(url, json=result)
