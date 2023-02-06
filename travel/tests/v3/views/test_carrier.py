# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from common.models.geo import Settlement, CompanyOffice
from common.models.schedule import Company
from travel.rasp.api_public.tests.v3 import ApiTestCase


class TestCarrier(ApiTestCase):
    def test_valid(self):
        company = Company.objects.create(
            id=680,
            title='Company1',
            sirena_id='111',
            iata='222',
            icao='333',
            address='some_address',
            url='http://my-url.ru',
            email='company1@ema.il',
            phone='123',
            contact_info='call me!',
            logo='logo.jpg',
        )

        offices_number = 4
        for i in range(offices_number):
            CompanyOffice.objects.create(company=company, settlement_id=Settlement.MOSCOW_ID)

        expected_result = {
            'code': company.id,
            'title': company.title,
            'codes': {
                'sirena': company.sirena_id,
                'iata': company.iata,
                'icao': company.icao,
            },
            'address': company.address,
            'email': company.email,
            'url': company.url,
            'contacts': company.contact_info,
            'logo': company.logo,
            'phone': company.phone,
        }

        query = {'code': '680'}
        result = self.api_get_json('carrier', query)
        carrier = result['carrier']
        offices = carrier.pop('offices', None)
        assert carrier == expected_result
        assert len(offices) == offices_number
