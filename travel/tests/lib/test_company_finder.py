# coding=utf-8
from collections import defaultdict

from mock import Mock

from travel.avia.library.python.common.lib.company_finder import CompanyFinder
from travel.avia.library.python.common.models.iatacorrection import IataCorrection
from travel.avia.library.python.common.models.schedule import Company
from travel.avia.library.python.tester.testcase import TestCase


class CompanyFinderTest(TestCase):
    def setUp(self):
        self._fake_logger = Mock()

    def create_finder(self, companies, corrections):
        by_iata = defaultdict(list)
        by_sirena = defaultdict(list)
        by_id = {}
        for c in companies:
            by_id[c.id] = c
            by_iata[c.iata].append(c)
            by_sirena[c.sirena_id].append(c)

        corrections_by_iata = defaultdict(list)
        for c in corrections:
            corrections_by_iata[c.code].append(c)

        return CompanyFinder(
            company_by_id=by_id,
            companies_by_iata=dict(by_iata),
            companies_by_sirena=dict(by_sirena),
            corrections_by_iata=dict(corrections_by_iata),
            companies_by_icao={},  # TODO: write tests with using this parameter
            companies_by_icao_ru={},  # TODO: write tests with using this parameter
            logger=self._fake_logger
        )

    def test_find_by_iata_without_correction(self):
        company = Company(
            id=1,
            iata='SU'
        )
        another_company = Company(
            id=2,
            iata='FU'
        )

        finder = self.create_finder(
            [company, another_company],
            {}
        )

        actual = finder.find('SU', None)
        assert actual.iata == 'SU'
        actual = finder.find('FU', None)
        assert actual.iata == 'FU'

        actual = finder.find('SU', 'FU 123')
        assert actual.iata == 'SU'
        actual = finder.find('SU', 'SU 123')
        assert actual.iata == 'SU'

        actual = finder.find('Unknown', None)
        assert actual is None

    def test_find_by_sirena_without_correction(self):
        company = Company(
            id=1,
            iata='SU',
            sirena_id='WITH_IATA'
        )
        another_company = Company(
            id=2,
            sirena_id='WITHOUT_IATA'
        )

        finder = self.create_finder(
            [company, another_company],
            {}
        )

        actual = finder.find('SU', None)
        assert actual.iata == 'SU'
        actual = finder.find('WITH_IATA', None)
        assert actual.iata == 'SU'

        actual = finder.find('WITHOUT_IATA', None)
        assert actual.id == another_company.id

    def test_find_by_iata_with_correction(self):
        company = Company(
            id=1,
            iata='SU'
        )
        another_company = Company(
            id=2,
            iata='FU'
        )

        finder = self.create_finder(
            [company, another_company],
            [
                IataCorrection(
                    code='SU',
                    number='^\d\d$',
                    company_id=another_company.id
                ),
                IataCorrection(
                    code='SU',
                    number='^\d\d\d$',
                    company_id=another_company.id
                )
            ]
        )

        actual = finder.find('SU', None)
        assert actual.iata == 'SU'
        actual = finder.find('FU', None)
        assert actual.iata == 'FU'

        actual = finder.find('SU', 'XX 12')
        assert actual.iata == 'FU'
        actual = finder.find('SU', 'XX 123')
        assert actual.iata == 'FU'
        actual = finder.find('SU', 'XX 1234')
        assert actual.iata == 'SU'

        actual = finder.find('Unknown', None)
        assert actual is None

    def test_find_by_sirena_with_correction(self):
        company = Company(
            id=1,
            iata='SU',
            sirena_id='WITH_SIRENA'
        )
        another_company = Company(
            id=2,
            iata='FU'
        )

        finder = self.create_finder(
            [company, another_company],
            [
                IataCorrection(
                    code='SU',
                    number='^\d\d$',
                    company_id=another_company.id
                ),
                IataCorrection(
                    code='SU',
                    number='^\d\d\d$',
                    company_id=another_company.id
                )
            ]
        )

        actual = finder.find('SU', None)
        assert actual.iata == 'SU'
        actual = finder.find('FU', None)
        assert actual.iata == 'FU'

        actual = finder.find('SU', 'XX 12')
        assert actual.iata == 'FU'
        actual = finder.find('SU', 'XX 123')
        assert actual.iata == 'FU'
        actual = finder.find('SU', 'XX 1234')
        assert actual.iata == 'SU'

        actual = finder.find('WITH_SIRENA', 'XX 12')
        assert actual.iata == 'FU'
        actual = finder.find('WITH_SIRENA', 'XX 123')
        assert actual.iata == 'FU'
        actual = finder.find('WITH_SIRENA', 'XX 1234')
        assert actual.iata == 'SU'

        actual = finder.find('Unknown', None)
        assert actual is None

    def test_find_by_duplicate_iata(self):
        company = Company(
            id=1,
            iata='SU'
        )
        another_company = Company(
            id=2,
            iata='SU'
        )

        finder = self.create_finder(
            [company, another_company],
            []
        )

        actual = finder.find('SU', None)
        assert actual.id == company.id

    def test_find_by_duplicate_sirena(self):
        company = Company(
            id=1,
            iata='SU',
            sirena_id='WITH_IATA'
        )
        another_company = Company(
            id=2,
            iata='FU',
            sirena_id='WITH_IATA'
        )

        finder = self.create_finder(
            [company, another_company],
            {}
        )

        actual = finder.find('SU', None)
        assert actual.iata == 'SU'
        actual = finder.find('FU', None)
        assert actual.iata == 'FU'
        actual = finder.find('WITH_IATA', None)
        assert actual.id == company.id
