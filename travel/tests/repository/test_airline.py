# coding=utf-8
from __future__ import absolute_import

from mock import Mock
from typing import cast

from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.backend.repository.airlines import AirlineRepository, AirlineModel
from travel.avia.backend.repository.helpers import NationalBox
from travel.avia.backend.repository.translations import TranslatedTitleRepository
from travel.avia.library.python.tester.factories import create_company, create_aviacompany
from travel.avia.library.python.tester.testcase import TestCase


class TestAirlineRepository(TestCase):
    def setUp(self):
        self._fake_translated_title_repository = Mock(TranslatedTitleRepository.__new__(TranslatedTitleRepository))
        self._airline_repository = AirlineRepository(
            translated_title_repository=cast(TranslatedTitleRepository, self._fake_translated_title_repository)
        )

    def test_repository(self):
        airline = create_company(
            title='some_airline',
            slug='some_slug',
            sirena_id=u'АУ',
            iata='',
            t_type_id=TransportType.PLANE_ID,
            popular_score_for_ru=1,
            popular_score_for_ua=2,
            popular_score_for_kz=3,
            popular_score_for_com=4,
            popular_score_for_tr=5
        )
        create_aviacompany(
            rasp_company=airline,
            iata='ZZ',
            carryon_width=1,
            carryon_height=2,
            carryon_length=3,
            carryon_dimensions_sum=6,
            baggage_width=1,
            baggage_height=2,
            baggage_length=3,
            baggage_dimensions_sum=6,
        )
        expected = AirlineModel(
            translated_title_repository=cast(TranslatedTitleRepository, self._fake_translated_title_repository),
            pk=airline.id,
            title_id=airline.new_L_title_id,
            popular_score_by_national_version=NationalBox({
                'ru': 1, 'ua': 2, 'kz': 3,
                'com': 4, 'tr': 5
            }),
            slug='some_slug',
            sirena=u'АУ',
            iata='ZZ',
            icao='ZZZ',
            icao_ru=u'АБВ',
            tariff={},
            seo_description_key='',
            alliance_id=None,
            baggage_rules='',
            baggage_rules_url='',
            logo='',
            logo_bgcolor='',
            registration_url='',
            url='',
            registration_url_locals=None,
            registration_phone='',
            registration_phone_locals=None,
            hidden=False,
            carryon_width=1,
            carryon_height=2,
            carryon_length=3,
            carryon_dimensions_sum=6,
            baggage_width=1,
            baggage_height=2,
            baggage_length=3,
            baggage_dimensions_sum=6,
        )

        self._airline_repository.pre_cache()
        airlines_models = self._airline_repository.get_all()
        actual_airline = self._airline_repository.get(airline.id)

        assert [expected] == airlines_models
        assert expected == actual_airline
        assert expected.iata == actual_airline.iata

    @staticmethod
    def create_some_company(**kwargs):
        mandatory_fields = {
            'title': 'some_airline',
            'slug': 'some_slug',
            'sirena_id': u'АУ',
            'iata': '',
            't_type_id': TransportType.PLANE_ID,
        }
        mandatory_fields.update(kwargs)
        return create_company(mandatory_fields)

    def assert_registration_field_is_expected(self, registration_field, registration_field_local, field_name,
                                              field_lang, test_lang, expected):
        kwargs = {
            'registration_{}'.format(field_name): registration_field,
        }
        if field_lang:
            kwargs['registration_{}_{}'.format(field_name, field_lang)] = registration_field_local
        company = self.create_some_company(**kwargs)
        self._airline_repository.pre_cache()
        actual_airline = self._airline_repository.get(company.id)
        assert expected == getattr(actual_airline, 'get_registration_{}'.format(field_name))(test_lang)

    def assert_registration_field_ru_is_local_when_specified(self, field_name):
        self.assert_registration_field_is_expected(
            registration_field='some_field',
            registration_field_local='some_field_local',
            field_name=field_name,
            field_lang='ru',
            test_lang='ru',
            expected='some_field_local',
        )

    def test_registration_url_ru_is_local_when_specified(self):
        self.assert_registration_field_ru_is_local_when_specified('url')

    def test_registration_phone_ru_is_local_when_specified(self):
        self.assert_registration_field_ru_is_local_when_specified('phone')

    def assert_fallback_to_registration_field_if_no_local(self, field_name):
        self.assert_registration_field_is_expected(
            registration_field='some_field',
            registration_field_local=None,
            field_name=field_name,
            field_lang='ru',
            test_lang='ru',
            expected='some_field',
        )

    def test_fallback_to_registration_url_if_no_local(self):
        self.assert_fallback_to_registration_field_if_no_local('url')

    def test_fallback_to_registration_phone_if_no_local(self):
        self.assert_fallback_to_registration_field_if_no_local('phone')

    def assert_return_registration_field_if_lang_not_specified(self, field_name):
        self.assert_registration_field_is_expected(
            registration_field='some_field',
            registration_field_local='some_field_local',
            field_name=field_name,
            field_lang='ru',
            test_lang=None,
            expected='some_field',
        )

    def test_return_registration_url_if_lang_not_specified(self):
        self.assert_return_registration_field_if_lang_not_specified('url')

    def test_return_registration_phone_if_lang_not_specified(self):
        self.assert_return_registration_field_if_lang_not_specified('phone')

    def assert_return_none_if_no_field_found(self, field_name):
        self.assert_registration_field_is_expected(
            registration_field='',
            registration_field_local='',
            field_name=field_name,
            field_lang='ru',
            test_lang='ru',
            expected=None,
        )

    def test_return_none_if_no_url_found(self):
        self.assert_return_none_if_no_field_found('url')

    def test_return_none_if_no_phone_found(self):
        self.assert_return_none_if_no_field_found('phone')
