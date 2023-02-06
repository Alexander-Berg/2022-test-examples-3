# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from copy import copy

import mock
import pytest
from django.conf import settings
from django.contrib.contenttypes.models import ContentType


from common.models.geo import (PointSynonym, Settlement, Country, Station, CityMajority, StationMajority, StationType,
                               CodeSystem, ExternalDirection, ExternalDirectionMarker, Region)
from common.models.transport import TransportType

import travel.rasp.suggests_tasks.suggests.generate.titles as titles
from travel.rasp.suggests_tasks.suggests.generate.caches import SynonymsPrecache, StationCodePrecache
from travel.rasp.suggests_tasks.suggests.generate.shared_objects import set_objs
from travel.rasp.suggests_tasks.suggests.generate.titles import (
    SuggestUnload, title_variants, BaseWrapper, UNKNOWN_WEIGHT, SettlementWrapper, CountryWrapper, StationWrapper,
    GeoWeights, titles_by_system_codes, MAX_SETTLEMENT_MAJ, MAX_STATION_MAJ, generate_titles_data, get_titles_data,
    get_sett_one_station, merge_suggests_data, ModelFieldsGetter, stations, settlements, countries,
    save_stations_directions
)
from travel.rasp.suggests_tasks.suggests.text_utils import prepare_title_text, TITLE_LANGS

from common.tester.factories import (
    create_settlement, create_station, create_country, create_region, create_station_code, create_suburban_zone
)
from common.tester.testcase import TestCase


class TestBaseWrapper(TestCase):
    def test_init_and_simple(self):
        obj = [1, 2, 3]
        bs = BaseWrapper(obj)
        assert bs.obj == obj

        bs = BaseWrapper(obj)

        with pytest.raises(NotImplementedError):
            bs.get_synth_weights()

        assert bs.get_ttypes() == []

    def test_get_stat_weights(self):
        settlement = create_settlement(id=1)
        station = create_station(id=1)
        set_objs(**{u'stat_weights': {(u'c', 1): {u'w_sett': u'v_sett'}, (u's', 1): {u'w_station': u'v_station'}}})

        bs = BaseWrapper(settlement)
        assert bs.get_stat_weights() == {u'w_sett': u'v_sett'}

        bs = BaseWrapper(station)
        assert bs.get_stat_weights() == {u'w_station': u'v_station'}

        bs = BaseWrapper(create_country())
        assert bs.get_stat_weights() == {None: {u'all': UNKNOWN_WEIGHT}}

    def test_get_title(self):
        station = create_station(id=1, title=u'station_title', title_uk=u'uk_station_title',
                                 title_en=u'en_station_title')

        bs = BaseWrapper(station)
        assert bs.get_title() == u'station_title'
        assert bs.get_title(lang=u'uk') == u'uk_station_title'
        assert bs.get_title(lang=u'en') == u'en_station_title'

    def test_get_titles_by_lang(self):
        station = create_station(id=1, title=u'station_title', title_uk=u'uk_station_title',
                                 title_tr=u'tr_station_title', title_en=u'en_station_title')

        bs = BaseWrapper(station)
        assert bs.get_titles_by_lang() == {u'ru': u'station_title',
                                           u'uk': u'uk_station_title',
                                           u'en': u'en_station_title'}

    def test_get_titles_synonyms(self):
        settlement = create_settlement(id=1, title=u'settlement_title')

        content_type = ContentType.objects.get_for_model(Settlement)
        point_syns = [PointSynonym.objects.create(title=u'syn_ {}'.format(i), content_type=content_type,  # noqa
                                                  object_id=settlement.id) for i in range(1, 3)]
        synonyms = SynonymsPrecache([Settlement])
        set_objs(**{u'synonyms': synonyms})

        bs = BaseWrapper(settlement)
        assert bs.get_titles_synonyms() == [u'syn_ 1', u'syn_ 2']

    def test_get_base_data(self):
        country = create_country(**{u'title_{}'.format(lang): u'country_{}'.format(lang) for lang in ['ru', 'en']})
        settlement = create_settlement(
            id=1, title=u'settlement_title', country=country,
            **{u'title_{}'.format(lang): u'settlement_{}'.format(lang) for lang in ['ru', 'en']})

        set_objs(**{u'stat_weights': {(u'c', 1): {u'w1': u'v1'}}})

        with mock.patch.object(BaseWrapper, u'get_synth_weights', return_value={u'not': 666}) as m_syn_weights:
            bs = BaseWrapper(settlement)

            assert bs.get_base_data() == {u'full_titles':
                                              {u'ru': {u'default': bs.get_omonim_title(lang=u'ru')},
                                               u'en': {u'default': bs.get_omonim_title(lang=u'en')},
                                               u'uk': {u'default': bs.get_omonim_title(lang=u'uk')}},
                                          u'obj_id': 1,
                                          u'obj_type': u'settlement',
                                          u'point_key': u'c1',
                                          u'titles':
                                              {u'en': settlement.title_en,
                                               u'ru': settlement.title_ru,
                                               u'uk': settlement.title_ru},  # не ставили title_uk, ожидаем фоллбэк на title_ru
                                          u'weights': {u'synth': {u'not': 666}, u'w1': u'v1'},
                                          u't_types': [],
                                          u'country_titles_national':
                                              {u'en': {u'default': country.title_en},
                                               u'ru': {u'default': country.title_ru},
                                               u'uk': {u'default': country.title_ru}},
                                          u'country_id_national': {u'default': country.id}}

            m_syn_weights.assert_called_once_with()

        # проверяем фолбэк yf sett.title при отсутствии русского названия
        settlement.title_ru = ''
        with mock.patch.object(BaseWrapper, u'get_synth_weights', return_value={u'not': 666}) as m_syn_weights:
            bs = BaseWrapper(settlement)

            assert bs.get_base_data()['titles'] == {
                u'en': settlement.title_en,
                u'ru': settlement.title,
                u'uk': settlement.title
            }

            m_syn_weights.assert_called_once_with()

    def test_get_object_forms_by_separators(self):
        bs = BaseWrapper([])

        with mock.patch(u'travel.rasp.suggests_tasks.suggests.generate.titles.title_variants',
                        side_effect=[[(u't1', True), (u't2', False)]]) as m_title_variants:
            objs = list(bs.get_object_forms_by_separators(u'title', {}))

            assert objs == [{u'title': u't1', u'is_prefix': True},
                            {u'title': u't2', u'is_prefix': False}]
            m_title_variants.assert_called_once_with(u'title')

    def test_get_object_forms(self):
        with mock.patch.object(BaseWrapper, u'get_base_data', return_value={u'base': None}) as m_base_data, \
                mock.patch.object(BaseWrapper, u'get_titles_synonyms', return_value=[u'syn1', u'syn2']) as m_titles_syn, \
                mock.patch.object(BaseWrapper, u'get_titles_by_lang', return_value={u'ru': u't_ru', u'uk': u't_uk', u'en': u't_en'}) as m_titles_lang:
            obj = BaseWrapper([])
            obj_forms = list(obj.get_object_forms())
            m_base_data.assert_called_once_with()
            m_titles_syn.assert_called_once_with()
            m_titles_lang.assert_called_once_with()

            check_titles(obj_forms, [u't ru', u'ru',
                                     u't en', u'en',
                                     u't uk', u'uk'], 6, 3, True)


def check_titles(values, titles, stop, l_count, lang=False):
    length = len(titles) // l_count
    for i, title in enumerate(titles):
        assert values[i][u'title'] == title

    if lang:
        assert {v[u'lang'] for v in values[:length]} == {u'ru'}
        assert {v[u'lang'] for v in values[length:length * 2]} == {u'en'}
        assert {v[u'lang'] for v in values[length * 2:length * 3]} == {u'uk'}


class TestSuggestUnload(TestCase):
    def test_init(self):
        iterable = [1, 2, 3]
        su = SuggestUnload(iterable)
        assert su.objects_iter == iterable

    def test_get_obj_wrapper(self):
        su = SuggestUnload([])
        settlement = create_settlement()
        assert su.get_obj_wrapper(settlement).__class__ is SettlementWrapper

        country = create_country()
        su.get_obj_wrapper(country)
        assert su.get_obj_wrapper(country).__class__ is CountryWrapper

        station = create_station()
        su.get_obj_wrapper(station)
        assert su.get_obj_wrapper(station).__class__ is StationWrapper

    def test_generate_suggests_data_sett(self):
        settlements = []

        for i, title in zip(range(1, 6), [u'sett_title_1', u'sett_title_2', u'sett_title_3ё']):
            settlements.append(create_settlement(id=i, title=title))

        synonyms = SynonymsPrecache([Settlement])

        set_objs(**{u'stat_weights': {}, u'settlements_ttypes': {i: [TransportType.PLANE_ID] for i in range(1, 6)},
                    u'synonyms': synonyms})

        su = SuggestUnload(settlements)
        su.generate_suggests_data()

        assert len(su.suggests_data) == 3
        items = sorted(su.suggests_data.items(), key=lambda x: x[0][1])

        for obj, values in items[:-1]:
            check_titles(values, [u'{}{}'.format(v, obj[1]) for v in [u'sett title ', u'title ', u''] * 3], 12, 3)

        last_obj = items[-1]
        check_titles(last_obj[1], [u'sett title 3е', u'sett title 3ё', u'title 3е', u'title 3ё', u'3е', u'3ё'] * 3,
                     18, 3)

    def test_generate_suggests_data_multi(self):
        objs = []
        objs.append(create_settlement(id=1, title=u'settlement_title_1'))
        objs.append(create_country(id=1, title=u'country_title_1'))
        objs.append(create_station(id=1, title=u'station_title_1'))

        synonyms = SynonymsPrecache([Settlement, Country, Station])
        station_codes = StationCodePrecache([])

        set_objs(**{u'stat_weights': {},
                    u'sett_one_station': [],
                    u'station_codes': station_codes,
                    u'settlements_ttypes': {i: [TransportType.PLANE_ID] for i in range(1, 6)},
                    u'stations_ttypes': {i: [TransportType.PLANE_ID] for i in range(1, 6)},
                    u'synonyms': synonyms})

        su = SuggestUnload(objs)
        su.generate_suggests_data()

        assert len(su.suggests_data) == 3

        for obj, values in su.suggests_data.items():
            check_titles(values, [u'{} title 1'.format(obj[0]), u'title 1', u'1'] * 3, 9, 3)

    def test_additional_obj_titles(self):
        su = SuggestUnload([])
        titles = list(su.additional_obj_titles(u'a_b_1'))
        assert titles == [u'a_b_1']

        titles = list(su.additional_obj_titles(u'еa_е_b_1е'))
        assert titles == [u'еa_е_b_1е']

        titles = list(su.additional_obj_titles(u'ёa_ё_b_1ё'))
        assert titles == [u'еa_е_b_1е',  u'еa_е_b_1ё',
                          u'еa_ё_b_1ё', u'ёa_ё_b_1ё']


def test_title_variants():
    s = u'a_b_1'
    variants = list(title_variants(s))
    assert variants == [(u'a b 1', True), (u'b 1', False), (u'1', False)]

    s = u'a b'
    variants = list(title_variants(s))
    assert variants == [(u'a b', True), (u'b', False)]

    s = u'a  ^ b ! 1 &'
    variants = list(title_variants(s))
    assert variants == [(u'a b 1', True), (u'b 1', False), (u'1', False)]

    s = u'word'
    variants = list(title_variants(s))
    assert variants == [(u'word', True)]


class TestGeoWeights(TestCase):
    fields = {u'base': 1, u'region': 2, u'country': 3, u'settlement': 4}

    def test_init(self):
        gw = GeoWeights(**self.fields)
        for key in self.fields.keys():
            assert getattr(gw, key) == self.fields[key]

    def test_mul(self):
        coef = 10
        gw = GeoWeights(**self.fields)
        old_gw = copy(gw)
        gw * coef
        for v in self.fields.keys():
            assert getattr(gw, v) == getattr(old_gw, v) * coef

    def test_eq(self):
        gw_1 = GeoWeights(**self.fields)
        gw_2 = GeoWeights(**self.fields)
        assert gw_1 == gw_2

        for key in ['base', 'region', 'settlement', 'country']:
            gw_2 = GeoWeights(**self.fields)
            gw_2.base = getattr(gw_1, key) * 2

            assert not (gw_1 == gw_2)


class TestSettlementWrapper(TestCase):
    def test_get_ttypes(self):
        settlements = [create_settlement(id=i, title=u'settlement_title') for i in range(1, 3)]
        t_types = [[TransportType.SUBURBAN_ID], [TransportType.PLANE_ID, TransportType.TRAIN_ID]]
        set_objs(**{u'settlements_ttypes': {i: v for i, v in enumerate(t_types, 1)}})
        for i, settlement in enumerate(settlements):
            sw = SettlementWrapper(settlement)
            assert sw.get_ttypes() == t_types[i]

    def test_majority(self):
        settlement = create_settlement(id=1, big_city=True)
        sw = SettlementWrapper(settlement)
        assert sw.majority() == CityMajority.CAPITAL_ID

        majority = 5
        settlement = create_settlement(id=2, majority=majority)
        sw = SettlementWrapper(settlement)
        assert sw.majority() == majority

    def test_geo_weights(self):
        majority = 10

        CityMajority.objects.create(id=majority)
        settlement = create_settlement(majority=majority)
        sw = SettlementWrapper(settlement)
        assert sw.geo_weights(boost=False) == GeoWeights(**settings.SETT_GEO_WEIGHTS)

        majority = CityMajority.CAPITAL_ID
        settlement = create_settlement(majority=majority)
        sw = SettlementWrapper(settlement)
        assert sw.geo_weights(boost=False) == GeoWeights(**settings.SETT_GEO_WEIGHTS) * MAX_SETTLEMENT_MAJ

        settlement = create_settlement(majority=majority, country=225)
        sw = SettlementWrapper(settlement)
        assert sw.geo_weights(boost=True) == GeoWeights(**settings.SETT_GEO_WEIGHTS) * MAX_SETTLEMENT_MAJ * settings.BOOST.OUR_COUNTRY_CAPITAL

        countries = [create_country(title=c_title) for c_title in [u'Латвия', u'США', u'Германия']]
        with mock.patch(u'travel.rasp.suggests_tasks.suggests.generate.titles.ALL_IMPORTANT_COUNTRIES', countries):
            for country in countries:
                settlement = create_settlement(majority=majority, country=country)
                sw = SettlementWrapper(settlement)
                geo_weights = sw.geo_weights(boost=True)
                assert geo_weights.base == (GeoWeights(**settings.SETT_GEO_WEIGHTS).base
                                            * MAX_SETTLEMENT_MAJ * settings.BOOST.IMPORTANT_COUNTRY_CAPITAL)
                assert geo_weights.settlement == GeoWeights(**settings.SETT_GEO_WEIGHTS).settlement * MAX_SETTLEMENT_MAJ
                assert geo_weights.region == GeoWeights(**settings.SETT_GEO_WEIGHTS).region * MAX_SETTLEMENT_MAJ * settings.BOOST.LARGE_CITIES
                assert geo_weights.country == GeoWeights(**settings.SETT_GEO_WEIGHTS).country * MAX_SETTLEMENT_MAJ * settings.BOOST.LARGE_CITIES

        for majority in [CityMajority.REGION_CAPITAL_ID, CityMajority.POPULATION_MILLION_ID, CityMajority.CAPITAL_ID]:
            settlement = create_settlement(majority=majority)
            sw = SettlementWrapper(settlement)
            geo_weights = sw.geo_weights(boost=True)
            maj = (MAX_SETTLEMENT_MAJ + 1) - majority
            assert geo_weights.base == GeoWeights(**settings.SETT_GEO_WEIGHTS).base * maj
            assert geo_weights.settlement == GeoWeights(**settings.SETT_GEO_WEIGHTS).settlement * maj
            assert geo_weights.region == GeoWeights(**settings.SETT_GEO_WEIGHTS).region * maj * settings.BOOST.LARGE_CITIES
            assert geo_weights.country == GeoWeights(**settings.SETT_GEO_WEIGHTS).country * maj * settings.BOOST.LARGE_CITIES

    def test_get_synth_weights(self):
        with mock.patch.object(SettlementWrapper, u'geo_weights', autospec=True,
                               return_value=GeoWeights(**settings.SETT_GEO_WEIGHTS)) as m_geo_weights:
            settlement = create_settlement()
            sw = SettlementWrapper(settlement)
            assert sw.get_synth_weights() == {None: settings.SETT_GEO_WEIGHTS[u'base']}
            m_geo_weights.assert_called_once_with(sw, boost=True)

            settlement = create_settlement(country=Country.RUSSIA_ID)
            sw = SettlementWrapper(settlement)
            assert sw.get_synth_weights() == {None: settings.SETT_GEO_WEIGHTS[u'base'],
                                              Country.RUSSIA_ID: settings.SETT_GEO_WEIGHTS[u'country']}

            region_geo_id = 1024
            region = create_region(country=Country.RUSSIA_ID, _geo_id=region_geo_id)
            settlement = create_settlement(country=Country.RUSSIA_ID, region=region)
            sw = SettlementWrapper(settlement)
            assert sw.get_synth_weights() == {None: settings.SETT_GEO_WEIGHTS[u'base'],
                                              Country.RUSSIA_ID: settings.SETT_GEO_WEIGHTS[u'country'],
                                              region_geo_id: settings.SETT_GEO_WEIGHTS[u'region']}

            geo_id = 2048
            settlement = create_settlement(country=Country.RUSSIA_ID, region=region, _geo_id=geo_id)
            sw = SettlementWrapper(settlement)
            assert sw.get_synth_weights() == {None: settings.SETT_GEO_WEIGHTS[u'base'],
                                              Country.RUSSIA_ID: settings.SETT_GEO_WEIGHTS[u'country'],
                                              region_geo_id: settings.SETT_GEO_WEIGHTS[u'region'],
                                              geo_id: settings.SETT_GEO_WEIGHTS[u'settlement']}

    def test_get_object_forms(self):
        def get_forms(self):
            for i in [
                {u'lang': u'ru', u'is_prefix': True, u'base': None, u'title': u'title ru'},
                {u'lang': u'uk', u'is_prefix': True, u'base': None, u'title': u'title uk'}
            ]:
                yield i

        def titles_by_system(wrapper, codes):
            for i in [
                {u'comment': u'code system iata', u'lang': u'ru', u'system': u'iata', u'title': u'iata code'},
                {u'comment': u'code system iata', u'lang': u'uk', u'system': u'iata', u'title': u'iata code'}
            ]:
                yield i

        with mock.patch.object(BaseWrapper, u'get_object_forms', autospec=True, side_effect=get_forms) as m_get_obj_forms, \
                mock.patch(u'travel.rasp.suggests_tasks.suggests.generate.titles.titles_by_system_codes', side_effect=titles_by_system) as m_titles:

            settlement = create_settlement(iata=u'iata_code')
            sw = SettlementWrapper(settlement)
            forms = list(sw.get_object_forms())

            m_get_obj_forms.assert_called_once_with(sw)
            m_titles.assert_called_once_with(sw, [[u'iata', u'iata_code']])

            assert forms == list(get_forms(None)) + list(titles_by_system(None, None))


def test_titles_by_system_codes():
    wrapper = mock.Mock()
    wrapper.get_base_data = mock.Mock(return_value={u'data': None})
    codes = [(u'iata', u'iata_code'), (u'sirena', u'sirena_code')]

    titles_by_system = []
    for lang in [u'ru', u'uk', u'en']:
        titles_by_system.append({
            u'comment': u'code system iata',
            u'data': None,
            u'lang': lang,
            u'system': u'iata',
            u'title': u'iata code'
        })

    for lang in [u'ru', u'uk']:
        titles_by_system.append({
            u'comment': u'code system sirena',
            u'data': None,
            u'lang': lang,
            u'system': u'sirena',
            u'title': u'sirena code'
        })

    assert list(titles_by_system_codes(wrapper, codes)) == titles_by_system


class TestCountryWrapper(TestCase):
    def test_get_synth_weights(self):
        country_groups = [
            [create_country(title=u'США'), create_country(title=u'Канада', _geo_id=1001)],
            [create_country(title=u'Германия'), create_country(title=u'Великобритания', _geo_id=1002)],
            [create_country(title=u'Эстония'), create_country(title=u'Молдавия', _geo_id=1003)],
            [create_country(title=u'Турция'), create_country(title=u'Украина', _geo_id=1004)]
        ]
        country_majority = {
            country.title: maj
            for maj, countries in enumerate(country_groups, start=2)
            for country in countries
        }

        with mock.patch(u'travel.rasp.suggests_tasks.suggests.generate.titles.COUNTRY_MAJORITY', country_majority):
            for i, country_group in enumerate(country_groups, 2):
                cw = CountryWrapper(country_group[0])
                assert cw.get_synth_weights() == {None: i}

                cw = CountryWrapper(country_group[1])
                assert cw.get_synth_weights() == {None: i, country_group[1]._geo_id: i * 2}

        cw = CountryWrapper(create_country())
        assert cw.get_synth_weights() == {None: 1}

        cw = CountryWrapper(create_country(_geo_id=1005))
        assert cw.get_synth_weights() == {None: 1, 1005: 2}


class TestStationWrapper(TestCase):
    test_base_data = {
        u'full_title': u'',
        u'obj_id': 1
    }

    def test_get_ttypes(self):
        stations = [create_station(id=i, title=u'station_title') for i in range(1, 3)]
        t_types = [[TransportType.SUBURBAN_ID], [TransportType.PLANE_ID, TransportType.TRAIN_ID]]
        set_objs(**{u'stations_ttypes': {i: v for i, v in enumerate(t_types, 1)}})
        for i, station in enumerate(stations):
            sw = StationWrapper(station)
            assert sw.get_ttypes() == t_types[i]

    def test_maj_coef(self):
        with pytest.raises(AssertionError):
            majority = StationMajority.objects.create(id=-10)
            station = create_station(majority=majority)
            sw = StationWrapper(station)
            sw.maj_coeff()

        for majority, replace_majority in [[StationMajority.MAIN_IN_CITY_ID] * 2,
                                           [StationMajority.IN_TABLO_ID] * 2,
                                           [StationMajority.MAX_ID, MAX_STATION_MAJ]]:
            station = create_station(majority=majority)
            sw = StationWrapper(station)
            maj = (MAX_STATION_MAJ + 1) - replace_majority
            coef_norm_len = settings.STATION_NORM_COEF[1] - settings.STATION_NORM_COEF[0]
            coef_len = float(MAX_STATION_MAJ - 1)

            assert sw.maj_coeff() == settings.STATION_NORM_COEF[0] + coef_norm_len / coef_len * (maj - 1)

    def test_get_synth_weights(self):
        sett_wrapper_call = mock.Mock()
        sett_wrapper_call.geo_weights = mock.Mock(side_effect=[copy(GeoWeights(**settings.SETT_GEO_WEIGHTS)) for i in range(5)])

        with mock.patch(u'travel.rasp.suggests_tasks.suggests.generate.titles.SettlementWrapper', return_value=sett_wrapper_call) as m_sett_wrapper:
            settlement = create_settlement()
            sw = StationWrapper(create_station(settlement=settlement))
            assert sw.get_synth_weights() == {None: settings.SETT_GEO_WEIGHTS[u'base'] * sw.maj_coeff()}
            m_sett_wrapper.assert_called_once_with(settlement)
            sett_wrapper_call.geo_weights.assert_called_once_with(boost=False)

            sw = StationWrapper(create_station(settlement=settlement, station_type=StationType.AIRPORT_ID))
            assert sw.get_synth_weights() == {None: settings.SETT_GEO_WEIGHTS[u'base'] * sw.maj_coeff()}
            sett_wrapper_call.geo_weights.assert_called_with(boost=True)

            settlement = create_settlement(_geo_id=1001)
            sw = StationWrapper(create_station(settlement=settlement))
            assert sw.get_synth_weights() == {None: settings.SETT_GEO_WEIGHTS[u'base'] * sw.maj_coeff(),
                                              settlement._geo_id: settings.SETT_GEO_WEIGHTS[u'settlement'] * sw.maj_coeff()}

            country = create_country(_geo_id=1002)
            sw = StationWrapper(create_station(settlement=settlement, country=country))
            assert sw.get_synth_weights() == {None: settings.SETT_GEO_WEIGHTS[u'base'] * sw.maj_coeff(),
                                              settlement._geo_id: settings.SETT_GEO_WEIGHTS[u'settlement'] * sw.maj_coeff(),
                                              country._geo_id: settings.SETT_GEO_WEIGHTS[u'country'] * sw.maj_coeff()}

            region = create_region(_geo_id=1003, country=country)
            sw = StationWrapper(create_station(settlement=settlement, region=region))
            assert sw.get_synth_weights() == {None: settings.SETT_GEO_WEIGHTS[u'base'] * sw.maj_coeff(),
                                              settlement._geo_id: settings.SETT_GEO_WEIGHTS[u'settlement'] * sw.maj_coeff(),
                                              country._geo_id: settings.SETT_GEO_WEIGHTS[u'country'] * sw.maj_coeff(),
                                              region._geo_id: settings.SETT_GEO_WEIGHTS[u'region'] * sw.maj_coeff()}

    def test_get_base_data(self):
        base_data = copy(self.test_base_data)

        with mock.patch.object(BaseWrapper, u'get_base_data', autospec=True, return_value=copy(base_data)):
            region = create_region(title_en=u'region1')
            settlement = create_settlement()
            station = create_station(
                t_type=TransportType.BUS_ID, country=225, slug=u'station', popular_title_ru=u'st1',
                region=region, settlement=settlement,
            )
            titles.stations_directions = {station.id: ExternalDirection(full_title_ru=u'ru_title',
                                                                        full_title_uk=u'uk_title')}
            sw = StationWrapper(station)
            base_data.update(
                {
                    u'codes': {u'iata': None, u'sirena': None},
                    u'disputed_territory': False,
                    u'majority_id': 1,
                    u'region_id': region.id,
                    u't_type_code': station.t_type.code,
                    u'zone_id': None,
                    u't_type': station.t_type.id,
                    u'suburban_directions': {u'en': u'', u'ru': u'ru_title', u'uk': u'uk_title'},
                    u'slug': u'station',
                    u'popular_titles': {u'ru': u'st1', u'uk': u'st1'},
                    u'station_types': {u'ru': u'автобусная остановка', u'en': u'bus stop', u'uk': u'автобусна зупинка'},
                    u'region_titles': {u'ru': region.title, u'en': u'region1', u'uk': region.title},
                    u'settlement_titles': {u'ru': settlement.title, u'en': settlement.title, u'uk': settlement.title},
                })

            assert sw.get_base_data() == base_data

    def test_get_object_forms(self):
        set_objs(**{u'sett_one_station': [5]})
        with mock.patch.object(StationWrapper, u'forms_for_not_same', autospec=True) as m_not_same, \
             mock.patch.object(StationWrapper, u'forms_for_same', autospec=True) as m_same, \
             mock.patch.object(StationWrapper, u'forms_by_systems_codes', autospec=True) as m_system_codes, \
             mock.patch.object(StationWrapper, u'forms_with_popular_title', autospec=True) as m_popular, \
             mock.patch.object(StationWrapper, u'get_base_data', autospec=True,
                               return_value=self.test_base_data) as m_get_base_data:
            sw = StationWrapper(create_station())
            forms_not_same = list(sw.get_object_forms())  # noqa
            m_not_same.assert_called_once_with(sw, self.test_base_data)
            m_popular.assert_called_once_with(sw, self.test_base_data)
            m_system_codes.assert_called_once_with(sw)
            m_get_base_data.assert_called_with(sw)
            assert m_same.call_count == 0

            settlement = create_settlement(id=5)
            sw = StationWrapper(create_station(settlement=settlement))
            forms_same = list(sw.get_object_forms())  # noqa
            m_same.assert_called_once_with(sw, self.test_base_data)
            assert m_popular.call_count == 2
            assert m_not_same.call_count == 1
            assert m_get_base_data.call_count == 2

    def test_forms_for_not_same(self):
        def get_forms(self):
            for i in [
                {u'lang': u'ru', u'is_prefix': True, u'base': None, u'title': u'title ru'},
                {u'lang': u'uk', u'is_prefix': True, u'base': None, u'title': u'title uk'}
            ]:
                yield i

        def titles_prefix(station):
            titles_with_prefix = []
            for lang in TITLE_LANGS:
                titles_with_prefix.append({
                    u'comment': u'important station',
                    u'lang': lang,
                    u'title': prepare_title_text(u'{} {}'.format(station.station_type.L_name(lang=lang), station.title))
                })
            for title_form in titles_with_prefix:
                title_form.update(self.test_base_data)
            return titles_with_prefix

        with mock.patch.object(BaseWrapper, u'get_object_forms', autospec=True, side_effect=get_forms) as m_get_obj_forms:
            station = create_station()
            sw = StationWrapper(station)
            forms = list(sw.forms_for_not_same(self.test_base_data))
            m_get_obj_forms.assert_called_once_with(sw)
            assert forms == list(get_forms(None))

            for st_type in [StationType.AIRPORT_ID, StationType.BUS_STATION_ID, StationType.TRAIN_STATION_ID]:
                for maj in [StationMajority.MAIN_IN_CITY_ID, StationMajority.IN_TABLO_ID]:
                    station = create_station(majority=maj, station_type=st_type)
                    sw = StationWrapper(station)
                    forms = list(sw.forms_for_not_same(self.test_base_data))
                    expected_forms = list(get_forms(None)) + titles_prefix(station)
                    for el in forms:
                        assert el in expected_forms
                    assert len(forms) == len(expected_forms)

    def test_forms_for_same(self):
        def titles_prefix(station_type, station):
            titles_with_prefix = []
            for lang in TITLE_LANGS:
                titles_with_prefix.append({
                    u'comment': u'same_as_settlement prefix; {}'.format(lang),
                    u'lang': lang,
                    u'title': prepare_title_text(u'{} {}'.format(station_type.L_name(lang=lang), station.title))
                })
            for title_form in titles_with_prefix:
                title_form.update(self.test_base_data)
            return titles_with_prefix

        for t_type, station_types in StationWrapper.STATION_TYPE_BY_TRANSPORT_TYPE.items():
            station = create_station(t_type=t_type)
            sw = StationWrapper(station)
            forms = list(sw.forms_for_same(self.test_base_data))
            expected_forms = []
            for station_type_id in station_types:
                station_type = StationType.objects.get(id=station_type_id)
                expected_forms += titles_prefix(station_type, station)

            for el in forms:
                assert el in expected_forms
            assert len(forms) == len(expected_forms)

    def test_forms_with_popular_title(self):
        station = create_station(popular_title=u'popular')
        sw = StationWrapper(station)
        base_data = sw.get_base_data()

        def popular_titles(station):
            popular_prefix_titles = []
            for lang in TITLE_LANGS:
                popular_prefix_titles.append({
                    u'comment': u'popular name',
                    u'lang': lang,
                    u'title': prepare_title_text(station.popular_title)
                })
            for title_form in popular_prefix_titles:
                title_form.update(base_data)
            return popular_prefix_titles

        forms = list(sw.forms_with_popular_title(base_data))
        expected_forms = popular_titles(station)

        for el in forms:
            assert el in expected_forms
        assert len(forms) == len(expected_forms)

        station = create_station()
        sw = StationWrapper(station)
        assert list(sw.forms_with_popular_title(sw.get_base_data())) == []

    def test_forms_by_systems_codes(self):
        code_systems = list(CodeSystem.objects.all()[:2])
        stations = [create_station() for i in range(len(code_systems))]
        codes = [create_station_code(code='code{}'.format(i), system=code_systems[i], station=station)
                 for i, station in enumerate(stations)]
        station_codes = StationCodePrecache([system.code for system in code_systems])
        set_objs(**{u'station_codes': station_codes})

        with mock.patch.object(StationWrapper, u'get_base_data', autospec=True,
                               return_value=self.test_base_data) as m_get_base_data:
            for i, station in enumerate(stations):
                sw = StationWrapper(station)
                forms = list(sw.forms_by_systems_codes())
                assert forms == list(titles_by_system_codes(sw, [(code_systems[i].code, codes[i].code)]))
                m_get_base_data.assert_called_with(sw)


def test_merge_suggest_data():
    dict_to = {}
    data_1 = {'key_1': ['value_1_1'],
              'key_2': ['value_2_1']}
    data_2 = {'key_1': ['value_1_2'],
              'key_2': ['value_2_2']}

    merge_suggests_data(dict_to, data_1)
    assert dict_to == data_1

    merge_suggests_data(dict_to, data_2)
    assert dict_to == {'key_1': ['value_1_1', 'value_1_2'],
                       'key_2': ['value_2_1', 'value_2_2']}


class TestSettOneStation(TestCase):
    def test_get_sett_one_station(self):
        settlements = [create_settlement(id=i, title='title_{}'.format(i)) for i in [1, 2, 3]]
        stations = [create_station(id=i, title='title_{}'.format(sett_id), settlement=settlements[sett_id - 1])  # noqa
                    for i, sett_id in [(1, 1), (2, 2), (3, 2), (4, 3)]]
        one_stations = get_sett_one_station()

        assert len(one_stations) == 2
        assert one_stations[0].title == 'title_1' and one_stations[1].title == 'title_3'


def test_get_titles_data():
    worker_id = 0
    model_ids = [(Station, [1, 2]), (Country, [3])]
    with mock.patch('travel.rasp.suggests_tasks.suggests.generate.titles.SuggestUnload') as m_sugg_unload:
        instance = m_sugg_unload.return_value
        instance.suggests_data = mock.Mock()
        m_sugg_unload.generate_suggests_data = mock.Mock()

        result_worker_id, suggest_data = get_titles_data((worker_id, model_ids))
        assert instance.generate_suggests_data.call_count == 1
        assert result_worker_id == worker_id
        assert suggest_data == instance.suggests_data


def test_generate_titles_data():
    model_ids = [(Station, [1, 2]), (Country, [3])]
    title_forms = [[
        (0, {(Station, 1): [{'title': 'title_1', 'id': 1, 'slug': 'title-1'}]}),
        (1, {(Settlement, 2): [{'title': 'title_2', 'id': 2, 'slug': 'title-2'}]})
    ]]
    objects_data = [{'title': '{} object'.format(ob_id)} for model, ids in model_ids for ob_id in ids]

    def get_titles((worker_id, model_ids)):
        return worker_id, objects_data

    with mock.patch('travel.rasp.suggests_tasks.suggests.generate.titles.get_titles_data', side_effect=get_titles) as m_get_titles, \
         mock.patch('travel.rasp.suggests_tasks.suggests.generate.titles.get_sett_one_station', autospec=True) as m_get_one_st, \
         mock.patch('travel.rasp.suggests_tasks.suggests.generate.titles.generate_parallel', autospec=True, side_effect=title_forms) as m_gen_par, \
         mock.patch('travel.rasp.suggests_tasks.suggests.generate.titles.merge_suggests_data', autospec=True, side_effect=merge_suggests_data) as m_merge_data, \
         mock.patch('travel.rasp.suggests_tasks.suggests.generate.titles.save_stations_directions', autospec=True) as m_save_dir:
        titles_data = generate_titles_data(model_ids, pool_size=1)
        m_get_one_st.assert_called_once_with()
        m_get_titles.assert_called_once_with((0, model_ids))
        m_save_dir.assert_called_once_with()
        assert titles_data == objects_data

        titles_data = generate_titles_data(model_ids, pool_size=2)
        m_gen_par.assert_called_once_with(m_get_titles, {0: [(Station, [1]), (Country, [3])],
                                                         1: [(Station, [2]), (Country, [])]}, 2)
        assert m_merge_data.call_count == 2
        assert m_merge_data.call_args_list[0][0][1] == {(Station, 1): [{'title': 'title_1', 'id': 1, 'slug': 'title-1'}]}
        assert m_merge_data.call_args_list[1][0][1] == {(Settlement, 2): [{'title': 'title_2', 'id': 2, 'slug': 'title-2'}]}
        assert titles_data == {(Station, 1): [{'title': 'title_1', 'id': 1, 'slug': 'title-1'}],
                               (Settlement, 2): [{'title': 'title_2', 'id': 2, 'slug': 'title-2'}]}


class TestModelFieldsGetter(TestCase):
    def test_init(self):
        fields = ['id', 'title', 'majority']
        mfg = ModelFieldsGetter(Station, fields)

        assert mfg.model == Station
        assert mfg.fields == fields

    def test_get(self):
        fields = ['id', 'title', 'majority']
        for (st_id, title, maj) in [(1, 'title_1', 1),
                                    (2, 'title_1', 2),
                                    (3, 'title_1', 3)]:
            create_station(id=st_id, title=title, majority=maj)

        data = {'majority': StationMajority.objects.get(id=StationMajority.MAIN_IN_CITY_ID), 'id': 1, 'title': u'title_1'}
        with mock.patch.object(ModelFieldsGetter, 'foreign_keys_to_objects', autospec=True,
                               side_effect=ModelFieldsGetter.foreign_keys_to_objects) as m_foreign_keys:
            mfg = ModelFieldsGetter(Station, fields)
            result = list(mfg.get(pk=1))
            m_foreign_keys.assert_called_once_with(mfg, data)
            assert len(result) == 1 and result[0].id == 1
            assert result[0] == Station(**data)

    def test_foreign_keys_to_objects(self):
        fields = ['id', 'title', 'majority']
        objs_data = {'majority': 1, 'id': 1, 'title': None}
        expected_data = {'majority': StationMajority.objects.get(id=StationMajority.MAIN_IN_CITY_ID), 'id': 1, 'title': None}
        mfg = ModelFieldsGetter(Station, fields)
        mfg.foreign_keys_to_objects(objs_data)

        assert isinstance(objs_data['majority'], StationMajority)
        assert objs_data == expected_data


class TestGetObjects(TestCase):
    def test_countries(self):
        create_country(id=2)
        fields = ['title_ru', 'title_en', 'title_uk', 'title', '_geo_id', 'id']
        with mock.patch('travel.rasp.suggests_tasks.suggests.generate.titles.ModelFieldsGetter.__init__', autospec=True,
                        side_effect=ModelFieldsGetter.__init__) as m_init, \
             mock.patch.object(ModelFieldsGetter, 'get', autospec=True, side_effect=ModelFieldsGetter.get) as m_get:
            kwargs = {'id': 2}
            countries_list = list(countries(**kwargs))
            assert m_init.call_count == 1
            call_args = m_init.call_args_list[0]
            assert call_args[0][1] == Country
            assert call_args[1] == {'fields': fields}
            m_get.assert_called_once_with(call_args[0][0], **kwargs)
            assert countries_list[0] == Country(**kwargs)

    def test_settlement(self):
        create_settlement(id=2)
        fields = ['title_{}'.format(lang) for lang in TITLE_LANGS] + [
            'title', 'majority', 'country', 'region', '_geo_id', 'id', 'district',
            'big_city', 'suburban_zone', '_disputed_territory', 'sirena_id', 'iata', 'slug'
        ]
        with mock.patch('travel.rasp.suggests_tasks.suggests.generate.titles.ModelFieldsGetter.__init__', autospec=True,
                        side_effect=ModelFieldsGetter.__init__) as m_init, \
             mock.patch.object(ModelFieldsGetter, 'get', autospec=True, side_effect=ModelFieldsGetter.get) as m_get:
            kwargs = {'id': 2}
            settlements_list = list(settlements(**kwargs))
            assert m_init.call_count == 1
            call_args = m_init.call_args_list[0]
            assert call_args[0][1] == Settlement
            assert call_args[1] == {'fields': fields}
            kwargs['hidden'] = 0
            m_get.assert_called_once_with(call_args[0][0], **kwargs)
            assert settlements_list[0] == Settlement(**kwargs)

    def test_station(self):
        create_station(id=2)
        fields = (['title_{}'.format(lang) for lang in TITLE_LANGS] +
                  ['popular_title_{}'.format(lang) for lang in TITLE_LANGS] +
                  ['title', 'id', 'type_choices', 'sirena_id',
                   'majority', 'station_type', 't_type',
                   'settlement', 'country', 'region', 'suburban_zone', 'district', 'slug'])
        with mock.patch('travel.rasp.suggests_tasks.suggests.generate.titles.ModelFieldsGetter.__init__', autospec=True,
                        side_effect=ModelFieldsGetter.__init__) as m_init, \
                mock.patch.object(ModelFieldsGetter, 'get', autospec=True, side_effect=ModelFieldsGetter.get) as m_get:
            kwargs = {'id': 2}
            stations_list = list(stations(**kwargs))
            assert m_init.call_count == 1
            call_args = m_init.call_args_list[0]
            assert call_args[0][1] == Station
            assert call_args[1] == {'fields': fields}
            kwargs['hidden'] = 0
            m_get.assert_called_once_with(call_args[0][0], **kwargs)
            assert stations_list[0] == Station(**kwargs)


class TestSaveDirections(TestCase):
    def test_save_stations_directions(self):
        region = Region.objects.create(country_id=Country.RUSSIA_ID, title=u'region')
        s_zone = create_suburban_zone(settlement_id=Settlement.MOSCOW_ID)

        stations = [
            create_station(title=u'same', region=region, suburban_zone=s_zone,
                           t_type=TransportType.objects.get(id=TransportType.SUBURBAN_ID)),
            create_station(title=u'not same'),
            create_station(title=u'same', region=region, suburban_zone=s_zone,
                           t_type=TransportType.objects.get(id=TransportType.TRAIN_ID))]

        ext_dirs = [
            ExternalDirection.objects.create(id=1, code=1, full_title=u'same', title=u'same', suburban_zone=s_zone),
            ExternalDirection.objects.create(id=2, code=2, full_title=u'not same', title=u'same'),
            ExternalDirection.objects.create(id=3, code=3, full_title=u'same', title=u'same', suburban_zone=s_zone)]

        for i in range(3):
            ExternalDirectionMarker.objects.create(station=stations[i], external_direction=ext_dirs[i], order=0)

        save_stations_directions()
        assert titles.stations_directions == {stations[0].id: ext_dirs[0],
                                              stations[2].id: ext_dirs[2]}
