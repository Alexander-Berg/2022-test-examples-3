from typing import Dict, NamedTuple

from travel.avia.library.python import iata_correction
from travel.avia.library.python.shared_dicts.cache.company_cache import CompanyCache

from travel.avia.flight_status_fetcher.library.flight_number_parser import FlightNumberParser

Company = NamedTuple(
    'Company',
    [
        ('Id', int),
        ('Iata', str),
        ('SirenaId', str),
        ('Icao', str),
    ],
)


class IATACorrector(iata_correction.IATACorrector):
    def __init__(self, company_codes: Dict[str, int]):
        super().__init__()
        self._company_codes = company_codes

    def _get_company_id(self, flight_number):
        for company_code, company_id in self._company_codes.items():
            if company_code in flight_number:
                return company_id
        return None

    def flight_numbers_to_carriers(self, company_codes_with_flight_numbers):
        flight_numbers = self.get_normalized_flight_numbers(company_codes_with_flight_numbers)

        result = {}
        for flight_number in flight_numbers:
            result[flight_number] = self._get_company_id(flight_number)

        return result


def delete_fields_from_dict(dic):
    """check that fields are present, but delete them so to not check their content"""
    fields = ['message_id', 'received_at', 'status_id']
    for field in fields:
        if field not in dic:
            raise AssertionError('expected fields {}'.format(field))
        del dic[field]
    return dic


def mock_flight_number_parser(logger, company_codes: Dict[str, int] = None):
    companies = CompanyCache(logger)
    if company_codes:
        for company_code, company_id in company_codes.items():
            c = Company(Iata=company_code, SirenaId=company_code, Icao=company_code, Id=company_id)
            companies.company_by_id[company_id] = c
            companies.company_by_code[company_code] = c

    return FlightNumberParser(
        companies=companies,
        iata_corrector=IATACorrector(company_codes),
    )
