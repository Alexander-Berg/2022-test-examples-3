# coding: utf8

from __future__ import unicode_literals

import json

import mock
from django.test import Client

from common.models.geo import Country
from common.models_utils.geo import Geobase_L_title
from common.tester.factories import create_country
from common.tester.skippers import not_macos
from common.tester.testcase import TestCase

not_macos_bad_locale = not_macos(reason='''
    Can't run on MacOS. Даный тест зависит от правильной работы locale collation для русских/украинских символов.
    В macos локаль ru_RU.UTF-8 использует ASCII-collation (см. /usr/share/locale/ru_RU.UTF-8), поэтому тест пройти не может.''')


class TestCountries(TestCase):

    def setUp(self):
        super(TestCountries, self).setUp()
        russia = Country.objects.get(id=Country.RUSSIA_ID)
        russia.title_uk = 'Росія'
        russia.code = 'RU'
        russia.code3 = 'RUS'
        russia._geo_id = Country.RUSSIA_ID
        russia.save()

        create_country(id=Country.UKRAINE_ID, code='UA', code3='UKR', title='Украина', title_uk='Україна',
                       domain_zone='ua', _geo_id=Country.UKRAINE_ID)
        create_country(id=Country.BELARUS_ID, code='BY', code3='BLR', title='Белоруссия', title_uk='Білорусія',
                       domain_zone='by', _geo_id=Country.BELARUS_ID)
        create_country(id=Country.KAZAKHSTAN_ID, code='KZ', code3='KAZ', title='Казахстан', title_uk='Казахстан',
                       domain_zone='kz', _geo_id=Country.KAZAKHSTAN_ID)
        create_country(id=Country.LITVA_ID, code='LT', code3='LTU', title='Литва', title_uk='Литва',
                       _geo_id=Country.LITVA_ID)
        create_country(id=171, code='UZ', code3='UZB', title='Узбекистан', title_uk='Узбекистан',
                       _geo_id=171)
        create_country(id=181, code='IL', code3='ISR', title='Израиль', title_uk='Ізраїль',
                       _geo_id=181)
        create_country(id=1056, code='EG', code3='EGY', title='Египет', title_uk='Єгипет',
                       _geo_id=1056)
        create_country(id=94, code='BR', code3='BRA', title='Бразилия', title_uk='Бразилія',
                       _geo_id=94)
        create_country(id=20785, code='EC', code3='ECU', title='Эквадор', title_uk='Еквадор',
                       _geo_id=20785)
        create_country(id=10535, code='JO', code3='JOR', title='Иордания', title_uk='Йорданія',
                       _geo_id=10535)
        create_country(id=95, code='CA', code3='CAN', title='Канада', title_uk='Канада',
                       _geo_id=95)

    @not_macos_bad_locale
    # если у страны указан geo_id, L_title идет в геобазу за названием
    # мокаем это обращение, чтобы не сбивать параметры CountryFactory
    @mock.patch.object(Geobase_L_title, 'get_geobase_linguistics', autospec=True, return_value=None)
    def test_ua_uk(self, m_):
        response = Client().get('/uk/countries/?national_version=ua')

        assert response.status_code == 200
        data = json.loads(response.content)
        assert data == {'countries': [
            {'id': Country.UKRAINE_ID, 'title': 'Україна', 'code2': 'UA', 'code3': 'UKR',
             'geoId': Country.UKRAINE_ID},
            {'id': Country.RUSSIA_ID, 'title': 'Росія', 'code2': 'RU', 'code3': 'RUS',
             'geoId': Country.RUSSIA_ID},
            {'id': Country.BELARUS_ID, 'title': 'Білорусія', 'code2': 'BY', 'code3': 'BLR',
             'geoId': Country.BELARUS_ID},
            {'id': Country.KAZAKHSTAN_ID, 'title': 'Казахстан', 'code2': 'KZ', 'code3': 'KAZ',
             'geoId': Country.KAZAKHSTAN_ID},
            {'id': Country.LITVA_ID, 'title': 'Литва', 'code2': 'LT', 'code3': 'LTU',
             'geoId': Country.LITVA_ID},
            {'id': 171, 'title': 'Узбекистан', 'code2': 'UZ', 'code3': 'UZB',
             'geoId': 171},
            {'id': 94, 'title': 'Бразилія', 'code2': 'BR', 'code3': 'BRA',
             'geoId': 94},
            {'id': 20785, 'title': 'Еквадор', 'code2': 'EC', 'code3': 'ECU',
             'geoId': 20785},
            {'id': 1056, 'title': 'Єгипет', 'code2': 'EG', 'code3': 'EGY',
             'geoId': 1056},
            {'id': 181, 'title': 'Ізраїль', 'code2': 'IL', 'code3': 'ISR',
             'geoId': 181},
            {'id': 10535, 'title': 'Йорданія', 'code2': 'JO', 'code3': 'JOR',
             'geoId': 10535},
            {'id': 95, 'title': 'Канада', 'code2': 'CA', 'code3': 'CAN',
             'geoId': 95},
        ]}

    @not_macos_bad_locale
    @mock.patch.object(Geobase_L_title, 'get_geobase_linguistics', autospec=True, return_value=None)
    def test_ua_ru(self, m_):
        response = Client().get('/ru/countries/?national_version=ua')

        assert response.status_code == 200
        data = json.loads(response.content)
        assert data == {'countries': [
            {'id': Country.UKRAINE_ID, 'title': 'Украина', 'code2': 'UA', 'code3': 'UKR',
             'geoId': Country.UKRAINE_ID},
            {'id': Country.RUSSIA_ID, 'title': 'Россия', 'code2': 'RU', 'code3': 'RUS',
             'geoId': Country.RUSSIA_ID},
            {'id': Country.BELARUS_ID, 'title': 'Белоруссия', 'code2': 'BY', 'code3': 'BLR',
             'geoId': Country.BELARUS_ID},
            {'id': Country.KAZAKHSTAN_ID, 'title': 'Казахстан', 'code2': 'KZ', 'code3': 'KAZ',
             'geoId': Country.KAZAKHSTAN_ID},
            {'id': Country.LITVA_ID, 'title': 'Литва', 'code2': 'LT', 'code3': 'LTU',
             'geoId': Country.LITVA_ID},
            {'id': 171, 'title': 'Узбекистан', 'code2': 'UZ', 'code3': 'UZB',
             'geoId': 171},
            {'id': 94, 'title': 'Бразилия', 'code2': 'BR', 'code3': 'BRA',
             'geoId': 94},
            {'id': 1056, 'title': 'Египет', 'code2': 'EG', 'code3': 'EGY',
             'geoId': 1056},
            {'id': 181, 'title': 'Израиль', 'code2': 'IL', 'code3': 'ISR',
             'geoId': 181},
            {'id': 10535, 'title': 'Иордания', 'code2': 'JO', 'code3': 'JOR',
             'geoId': 10535},
            {'id': 95, 'title': 'Канада', 'code2': 'CA', 'code3': 'CAN',
             'geoId': 95},
            {'id': 20785, 'title': 'Эквадор', 'code2': 'EC', 'code3': 'ECU',
             'geoId': 20785},
        ]}

    @not_macos_bad_locale
    @mock.patch.object(Geobase_L_title, 'get_geobase_linguistics', autospec=True, return_value=None)
    def test_ru_ru(self, m_):
        response = Client().get('/ru/countries/?national_version=ru')

        assert response.status_code == 200
        data = json.loads(response.content)
        assert data == {'countries': [
            {'id': Country.RUSSIA_ID, 'title': 'Россия', 'code2': 'RU', 'code3': 'RUS',
             'geoId': Country.RUSSIA_ID},
            {'id': Country.UKRAINE_ID, 'title': 'Украина', 'code2': 'UA', 'code3': 'UKR',
             'geoId': Country.UKRAINE_ID},
            {'id': Country.BELARUS_ID, 'title': 'Белоруссия', 'code2': 'BY', 'code3': 'BLR',
             'geoId': Country.BELARUS_ID},
            {'id': Country.KAZAKHSTAN_ID, 'title': 'Казахстан', 'code2': 'KZ', 'code3': 'KAZ',
             'geoId': Country.KAZAKHSTAN_ID},
            {'id': Country.LITVA_ID, 'title': 'Литва', 'code2': 'LT', 'code3': 'LTU',
             'geoId': Country.LITVA_ID},
            {'id': 171, 'title': 'Узбекистан', 'code2': 'UZ', 'code3': 'UZB',
             'geoId': 171},
            {'id': 94, 'title': 'Бразилия', 'code2': 'BR', 'code3': 'BRA',
             'geoId': 94},
            {'id': 1056, 'title': 'Египет', 'code2': 'EG', 'code3': 'EGY',
             'geoId': 1056},
            {'id': 181, 'title': 'Израиль', 'code2': 'IL', 'code3': 'ISR',
             'geoId': 181},
            {'id': 10535, 'title': 'Иордания', 'code2': 'JO', 'code3': 'JOR',
             'geoId': 10535},
            {'id': 95, 'title': 'Канада', 'code2': 'CA', 'code3': 'CAN',
             'geoId': 95},
            {'id': 20785, 'title': 'Эквадор', 'code2': 'EC', 'code3': 'ECU',
             'geoId': 20785},
        ]}
