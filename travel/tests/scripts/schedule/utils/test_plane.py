# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.db import connection
from django.test.utils import CaptureQueriesContext

from common.models.geo import Country
from common.tester.factories import create_company, create_country, create_station
from travel.rasp.admin.importinfo.models import IataCorrection
from travel.rasp.admin.scripts.schedule.utils.plane import AviaCompanyFinder


@pytest.mark.dbuser
class TestAviaCompanyFinder(object):
    def check_search(self, company, code, flight_number=None, stations=None):
        finder = AviaCompanyFinder()
        with CaptureQueriesContext(connection) as queries:
            assert finder.get_company(code, flight_number, stations) == company
        assert queries
        with CaptureQueriesContext(connection) as queries:
            assert finder.get_company(code, flight_number, stations) == company
        assert not queries

    def test_find_nothing(self):
        self.check_search(None, code='AAA')

    def test_find_by_iata(self):
        company = create_company(iata='AAA')
        self.check_search(company, code='AAA')

    def test_find_by_sirena(self):
        company = create_company(sirena_id='AAA')
        self.check_search(company, code='AAA')

    def test_find_by_icao(self):
        company = create_company(icao='AAA')
        self.check_search(company, code='AAA')

    def test_find_by_icao_ru(self):
        company = create_company(icao_ru='ФФФ')
        self.check_search(company, code='ФФФ')

    def test_find_by_correction_only(self):
        create_company(iata='BBB')
        company = create_company(iata='CCC')
        IataCorrection.objects.create(code='AAA', company=company, number='.*')
        self.check_search(company, code='AAA', flight_number='123')

    def test_find_by_correction_only_no_number(self):
        create_company(iata='BBB')
        company = create_company(iata='CCC')
        IataCorrection.objects.create(code='AAA', company=company, number='.*')
        self.check_search(None, code='AAA')

    def test_correction_priority(self):
        company_aaa = create_company(iata='AAA')
        company_ccc = create_company(iata='CCC')
        IataCorrection.objects.create(code='AAA', company=company_ccc, number='12.*')
        self.check_search(company_aaa, code='AAA')
        self.check_search(company_aaa, code='AAA', flight_number='234')
        self.check_search(company_ccc, code='AAA', flight_number='123')

    def test_correct_by_stations(self):
        country_ru = Country.objects.get(pk=Country.RUSSIA_ID)
        country_ua = create_country()
        company_first = create_company(id=1, iata='AAA')
        company_ru = create_company(iata='AAA', country=country_ru)
        company_ua = create_company(iata='AAA', country=country_ua)

        self.check_search(company_first, code='AAA')
        self.check_search(company_ru, code='AAA', stations=[create_station(country=country_ru)])
        self.check_search(company_ua, code='AAA', stations=[create_station(country=country_ua)])
        self.check_search(company_first, code='AAA', stations=[create_station(country=create_country())])
