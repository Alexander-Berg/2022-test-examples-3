# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import

import ujson
from mock import patch, Mock
from requests import Session
from typing import cast, List  # noqa
from logging import Logger

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.library.python.common.utils import environment

from travel.avia.backend.main.rest.travel_recipes.index import TravelRecipesView, TravelRecipesForm
from travel.avia.backend.main.services.price_index import PriceIndex, FromTo
from travel.avia.backend.repository.country import CountryRepository
from travel.avia.backend.repository.direction import DirectionRepository, Direction
from travel.avia.backend.repository.region import RegionRepository
from travel.avia.backend.repository.settlement import SettlementRepository, SettlementModel
from travel.avia.backend.repository.settlement_big_image import SettlementBigImageRepository


class TravelRecipesTest(TestCase):
    def setUp(self):
        with patch.object(CountryRepository, '_load_db_models') as country_load_models_mock:
            country_load_models_mock.return_value = self._get_countries()
            country_repository = CountryRepository(Mock())
            country_repository.pre_cache()

        with patch.object(RegionRepository, '_load_db_models') as region_load_models_mock:
            region_load_models_mock.return_value = self._get_regions()
            region_repository = RegionRepository(Mock())
            region_repository.pre_cache()

        with patch.object(SettlementRepository, '_load_db_models') as settlement_load_models_mock:
            settlement_load_models_mock.return_value = self._get_settlements()
            settlement_repository = SettlementRepository(Mock(), environment)
            settlement_repository.pre_cache()

        with patch.object(DirectionRepository, '_load_models') as direction_load_models_mock:
            direction_load_models_mock.return_value = self._get_directions()
            direction_repository = DirectionRepository()
            direction_repository.pre_cache()

        with patch.object(SettlementBigImageRepository, '_load_db_models') as images_load_models_mock:
            images_load_models_mock.return_value = self._get_images()
            images_repository = SettlementBigImageRepository(Mock())
            images_repository.pre_cache()

        self._view = TravelRecipesView(
            form=TravelRecipesForm(),
            country_repository=country_repository,
            region_repository=region_repository,
            settlement_repository=settlement_repository,
            direction_repository=direction_repository,
            settlement_big_image_repository=images_repository,
            price_index=PriceIndex(None),
            logger=cast(Logger, Mock()),
        )

    def test_single_min_price(self):
        with patch.object(PriceIndex, 'search') as price_index_mock:
            price_index_mock.return_value = [{
                'forward_date': '2019-07-22',
                'from_id': 495,
                'min_price': {'currency': 'RUR', 'value': 49501},
                'to_id': 343
            }]

            result = self._view._unsafe_process({
                'national_version': 'ru',
                'from_geo_id': 4950,
            })
            response = ujson.loads(result.response[0])

            assert len(response['data']) == 1
            self.assertSequenceEqual(
                response['data'],
                [{
                    'forward_date': '2019-07-22',
                    'to_id': 343,
                    'settlement_to': {
                        'countryId': 225,
                        'phraseFrom': None,
                        'urlTitle': None,
                        'title': None,
                        'phraseIn': None,
                        'iata': 'SVX',
                        'sirena': 'ЕКБ',
                        'phraseTo': None,
                        'id': 343,
                        'geoId': 3430,
                        'latitude': 1,
                        'longitude': 2,
                    },
                    'settlement_from': {
                        'countryId': 225,
                        'phraseFrom': None,
                        'urlTitle': None,
                        'title': None,
                        'phraseIn': None,
                        'iata': 'MOW',
                        'sirena': 'МОС',
                        'phraseTo': None,
                        'id': 495,
                        'geoId': 4950,
                        'latitude': 1,
                        'longitude': 2,
                    },
                    'image': 'http://343',
                    'min_price': {'currency': 'RUR', 'value': 49501},
                    'from_id': 495
                }]
            )

    def test_single_brief(self):
        with patch.object(PriceIndex, 'search') as price_index_mock:
            price_index_mock.return_value = [{
                'forward_date': '2019-07-22',
                'from_id': 495,
                'min_price': {'currency': 'RUR', 'value': 49501},
                'to_id': 343
            }]

            result = self._view._unsafe_process({
                'national_version': 'ru',
                'from_geo_id': 4950,
            })
            response = ujson.loads(result.response[0])

            self.assertSequenceEqual(
                self._prepare_brief(response['data']),
                ['MOW/SVX/2019-07-22/49501.RUR/I']
            )

    def test_svx_then_country_capital(self):
        with patch.object(PriceIndex, 'search') as price_index_mock:
            price_index_mock.return_value = [
                {
                    'forward_date': '2019-07-21',
                    'from_id': 495,
                    'min_price': {'currency': 'RUR', 'value': 4951},
                    'to_id': 21},
                {
                    'forward_date': '2019-07-22',
                    'from_id': 343,
                    'min_price': {'currency': 'RUR', 'value': 34301},
                    'to_id': 20},
            ]

            result = self._view._unsafe_process({
                'national_version': 'ru',
                'from_geo_id': 3430,
            })
            response = ujson.loads(result.response[0])

            args, kwargs = price_index_mock.call_args

            self.assertSequenceEqual(
                kwargs['directions'],
                [
                    FromTo(from_id=343, to_id=20),  # flights from the requested city go first
                    FromTo(from_id=495, to_id=21),  # flights from the nation capital follow
                ]
            )

            self.assertSequenceEqual(
                self._prepare_brief(response['data']),
                [
                    'SVX/c20/2019-07-22/34301.RUR/I',
                    'MOW/LGW/2019-07-21/4951.RUR/I',
                ]
            )

    def test_we_survive_when_price_index_throws_exception(self):
        with patch.object(Session, 'post') as session_post_mock:
            session_post_mock.side_effect = Mock(side_effect=Exception('Simulating price-index exception'))

            result = self._view._unsafe_process({
                'national_version': 'ru',
                'from_geo_id': 3430,
            })
            response = ujson.loads(result.response[0])

            self.assertEqual(session_post_mock.call_count, 1)
            self.assertEqual(len(response['data']), 0)

    def test_cee_then_vgd_country_capital(self):
        with patch.object(PriceIndex, 'search') as price_index_mock:
            price_index_mock.return_value = [
                {
                    'forward_date': '2019-07-21',
                    'from_id': 495,
                    'min_price': {'currency': 'RUR', 'value': 4951},
                    'to_id': 21},
                {
                    'forward_date': '2019-07-23',
                    'from_id': 8202,
                    'min_price': {'currency': 'RUR', 'value': 82021},
                    'to_id': 343},
                {
                    'forward_date': '2019-07-24',
                    'from_id': 8172,
                    'min_price': {'currency': 'RUR', 'value': 81721},
                    'to_id': 495},
                {
                    'forward_date': '2019-07-24',
                    'from_id': 8172,
                    'min_price': {'currency': 'RUR', 'value': 81722},
                    'to_id': 20},
            ]

            result = self._view._unsafe_process({
                'national_version': 'ru',
                'from_geo_id': 82020,
            })
            response = ujson.loads(result.response[0])

            args, kwargs = price_index_mock.call_args

            self.assertSequenceEqual(
                kwargs['directions'],
                [
                    FromTo(from_id=8202, to_id=343),  # flights from the requested city go first
                    FromTo(from_id=8172, to_id=20),   # flight from the region capital follows
                    FromTo(from_id=8172, to_id=495),  # another flight from the region capital follows
                    FromTo(from_id=495, to_id=21),    # flights from the nation capital follow
                ]
            )

            self.assertSequenceEqual(
                self._prepare_brief(response['data']),
                [
                    'CEE/SVX/2019-07-23/82021.RUR/I',
                    'VGD/MOW/2019-07-24/81721.RUR/I',
                    'VGD/c20/2019-07-24/81722.RUR/I',
                    'MOW/LGW/2019-07-21/4951.RUR/I',
                ]
            )

    def test_vozhega_should_be_like_vgd_not_cee(self):
        with patch.object(PriceIndex, 'search') as price_index_mock:
            price_index_mock.return_value = [
                {
                    'forward_date': '2019-07-21',
                    'from_id': 495,
                    'min_price': {'currency': 'RUR', 'value': 4951},
                    'to_id': 8202},
                {
                    'forward_date': '2019-07-24',
                    'from_id': 8172,
                    'min_price': {'currency': 'RUR', 'value': 81721},
                    'to_id': 343},
                {
                    'forward_date': '2019-07-24',
                    'from_id': 8172,
                    'min_price': {'currency': 'RUR', 'value': 81722},
                    'to_id': 20},
            ]

            result = self._view._unsafe_process({
                'national_version': 'ru',
                'from_geo_id': 817440,
            })
            response = ujson.loads(result.response[0])

            args, kwargs = price_index_mock.call_args

            self.assertSequenceEqual(
                kwargs['directions'],
                [
                    FromTo(from_id=8172, to_id=343),  # flights from the region capital go first
                    FromTo(from_id=8172, to_id=20),
                    FromTo(from_id=8172, to_id=495),
                    FromTo(from_id=495, to_id=21),    # flights from the nation capital follow
                ]
            )

            self.assertSequenceEqual(
                self._prepare_brief(response['data']),
                [
                    'VGD/SVX/2019-07-24/81721.RUR/I',
                    'VGD/c20/2019-07-24/81722.RUR/I',
                    'MOW/CEE/2019-07-21/4951.RUR/I',
                ]
            )

    def test_from_msq_in_ru_locale(self):
        # если не хватает направлений / цен с обычных рейсов
        # добиваем вылетами из страны "столицы локали" до 5
        with patch.object(PriceIndex, 'search') as price_index_mock:
            price_index_mock.return_value = [
                {
                    'forward_date': '2019-07-21',
                    'from_id': 153,
                    'min_price': {'currency': 'RUR', 'value': 3000},
                    'to_id': 157
                },
                {
                    'forward_date': '2019-07-21',
                    'from_id': 157,
                    'min_price': {'currency': 'RUR', 'value': 2000},
                    'to_id': 495
                },
                {
                    'forward_date': '2019-07-21',
                    'from_id': 495,
                    'min_price': {'currency': 'RUR', 'value': 1000},
                    'to_id': 343
                },
            ]

            result = self._view._unsafe_process({
                'national_version': 'ru',
                'from_geo_id': 1530,
            })
            response = ujson.loads(result.response[0])

            args, kwargs = price_index_mock.call_args

            self.assertSequenceEqual(
                kwargs['directions'],
                [
                    FromTo(from_id=153, to_id=157),
                    FromTo(from_id=157, to_id=495),
                    FromTo(from_id=495, to_id=343),
                    FromTo(from_id=495, to_id=21),
                ]
            )

            # это нормально, что не на все дирекшенсы есть прайсы
            self.assertSequenceEqual(
                self._prepare_brief(response['data']),
                [
                    'BQT/MSQ/2019-07-21/3000.RUR/I',
                    'MSQ/MOW/2019-07-21/2000.RUR/I',
                    'MOW/SVX/2019-07-21/1000.RUR/I',
                ]
            )

    def test_from_gtw_in_uk_locale(self):
        with patch.object(PriceIndex, 'search') as price_index_mock:
            price_index_mock.return_value = [{
                'forward_date': '2019-07-21',
                'from_id': 20,
                'min_price': {'currency': 'GBP', 'value': 201},
                'to_id': 343
            }]

            result = self._view._unsafe_process({
                'national_version': 'uk',
                'from_geo_id': 210,
            })
            response = ujson.loads(result.response[0])

            args, kwargs = price_index_mock.call_args

            self.assertSequenceEqual(
                kwargs['directions'],
                [
                    FromTo(from_id=20, to_id=343),
                ]
            )

            self.assertSequenceEqual(
                self._prepare_brief(response['data']),
                [
                    'c20/SVX/2019-07-21/201.GBP/I',
                ]
            )

    def test_com_tr(self):
        # для "com.tr" добиваем вылетами из Стамбула
        with patch.object(PriceIndex, 'search') as price_index_mock:
            price_index_mock.return_value = [{
                'forward_date': '2019-07-21',
                'from_id': 11508,
                'min_price': {'currency': 'USD', 'value': 150},
                'to_id': 343
            }]

            result = self._view._unsafe_process({
                'national_version': 'com.tr',
                'from_geo_id': 210,
            })
            response = ujson.loads(result.response[0])

            args, kwargs = price_index_mock.call_args

            self.assertSequenceEqual(
                kwargs['directions'],
                [
                    FromTo(from_id=11508, to_id=343),
                ]
            )

            self.assertSequenceEqual(
                self._prepare_brief(response['data']),
                [
                    'IST/SVX/2019-07-21/150.USD/I',
                ]
            )

    def test_from_gtw_in_es_locale(self):
        result = self._view._unsafe_process({
            'national_version': 'es',
            'from_geo_id': 210,
        })
        response = ujson.loads(result.response[0])
        self.assertEquals(response, {'status': 'ok', 'data': []})

    def test_filter_by_destination(self):
        all_different_destinations = [
            DirectionWithRepr(343, 495, 1, 1, 1, ''),
            DirectionWithRepr(343, 8172, 1, 1, 1, ''),
            DirectionWithRepr(8172, 343, 1, 1, 1, ''),
        ]

        # для направлений с разными destinations, фильтр ничего не фильтрует
        self.assertSequenceEqual(
            self._view._filter_by_destination(all_different_destinations, {}),
            all_different_destinations)

        # добавим две, которые будут повторять что-то из первых трёх
        repeating_destinations = list(all_different_destinations)
        repeating_destinations.append(DirectionWithRepr(21, 8172, 1, 1, 1, ''))
        repeating_destinations.append(DirectionWithRepr(20, 495, 1, 1, 1, ''))

        # фильтр будет отсекать добавленное
        self.assertSequenceEqual(
            self._view._filter_by_destination(repeating_destinations, {}),
            all_different_destinations)

        # если попросим вырезать средний из трёх элементов, должны остаться крайние два
        self.assertSequenceEqual(
            self._view._filter_by_destination(
                all_different_destinations, {all_different_destinations[1].arrival_settlement_id}),
            [all_different_destinations[0], all_different_destinations[2]])

        # результат не зависит от того, просим вырезать из списка с повторами или без
        self.assertSequenceEqual(
            self._view._filter_by_destination(
                repeating_destinations, {all_different_destinations[1].arrival_settlement_id}),
            [all_different_destinations[0], all_different_destinations[2]])

    def test_null_in_output(self):
        with patch.object(SettlementBigImageRepository, 'get_random_default') as get_no_defaults:
            get_no_defaults.return_value = None

            result = self._view._prepare(minprice={'from_id': 495, 'to_id': 8202}, lang='ru')
            response = ujson.dumps(result)
            self.assertTrue('"image":null,' in response)

    def test_model_serialization(self):
        settlement_model = self._view._settlement_repository.get(343)
        self.assertIsInstance(settlement_model, SettlementModel)
        result = ujson.dumps(settlement_model)
        response = ujson.loads(result)
        self.assertDictEqual(response, {
            'country_id': 225,
            'geo_id': 3430,
            'iata': 'SVX',
            'id': 343,
            'is_disputed_territory': False,
            'latitude': 1,
            'longitude': 2,
            'majority_id': 2,
            'pk': 343,
            'point_key': 'c343',
            'pytz': {'zone': 'Europe/Moscow'},
            'region_id': 66,
            'sirena': '\u0415\u041a\u0411',
            'utcoffset': 10800
        })

    @staticmethod
    def _get_countries():
        # type: () -> List[dict]
        return [
            {
                'id': 225,
                'new_L_title_id': 100,
                '_geo_id': 225,
                'code': 'RU',
            },
            {
                'id': 2,
                'new_L_title_id': 200,
                '_geo_id': 20,
                'code': 'UK',
            },
            {
                'id': 149,
                'new_L_title_id': 26,
                '_geo_id': 149,
                'code': 'BY',
            },
            {
                'id': 983,
                'new_L_title_id': 48,
                '_geo_id': 983,
                'code': 'TR',
            },
        ]

    @staticmethod
    def _get_regions():
        # type: () -> List[dict]
        return [
            {
                'id': 1,  # MOW
                'new_L_title_id': 100,
                '_geo_id': 10,
                'country_id': 225,
            },
            {
                'id': 3,  # VGD
                'new_L_title_id': 300,
                '_geo_id': 30,
                'country_id': 225,
            },
            {
                'id': 66,  # SVX
                'new_L_title_id': 6600,
                '_geo_id': 660,
                'country_id': 225,
            },
            {
                'id': 201,  # LON
                'new_L_title_id': 20100,
                '_geo_id': 2010,
                'country_id': 2,
            },
            {
                'id': 21623,  # BQT
                'new_L_title_id': 503374,
                '_geo_id': 216230,
                'country_id': 149,
            }
        ]

    @staticmethod
    def _get_directions():
        # type: () -> List[dict]
        return [
            {
                'departure_settlement_id': 343,
                'arrival_settlement_id': 20,
                'popularity': 10,
                'national_version': 'ru',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 495,
                'arrival_settlement_id': 343,
                'popularity': 9,
                'national_version': 'ru',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 495,
                'arrival_settlement_id': 21,
                'popularity': 9,
                'national_version': 'ru',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 8172,
                'arrival_settlement_id': 343,
                'popularity': 8,
                'national_version': 'ru',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 8202,
                'arrival_settlement_id': 343,
                'popularity': 7,
                'national_version': 'ru',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 8202,
                'arrival_settlement_id': 343,
                'popularity': 7,
                'national_version': 'uk',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 495,
                'arrival_settlement_id': 20,
                'popularity': 6,
                'national_version': 'uk',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 20,
                'arrival_settlement_id': 343,
                'popularity': 5,
                'national_version': 'uk',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 8172,
                'arrival_settlement_id': 20,
                'popularity': 4,
                'national_version': 'uk',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 8172,
                'arrival_settlement_id': 20,
                'popularity': 4,
                'national_version': 'ru',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 8172,
                'arrival_settlement_id': 495,
                'popularity': 3,
                'national_version': 'ru',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 495,
                'arrival_settlement_id': 343,
                'popularity': 2,
                'national_version': 'tr',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 153,
                'arrival_settlement_id': 157,
                'popularity': 2,
                'national_version': 'ru',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 157,
                'arrival_settlement_id': 495,
                'popularity': 3,
                'national_version': 'ru',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 157,
                'arrival_settlement_id': 495,
                'popularity': 2,
                'national_version': 'com.tr',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 11508,
                'arrival_settlement_id': 343,
                'popularity': 2,
                'national_version': 'com.tr',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
        ]

    def _get_settlements(self):
        # type: () -> List[dict]
        return [
            self._create_settlement({
                'id': 495,
                '_geo_id': 4950,
                'iata': 'MOW',
                'sirena_id': 'МОС',

            }),
            self._create_settlement({
                'id': 343,
                '_geo_id': 3430,
                'iata': 'SVX',
                'sirena_id': 'ЕКБ',
                'majority_id': 2,
                'region_id': 66,
            }),
            self._create_settlement({
                'id': 8202,
                '_geo_id': 82020,
                'iata': 'CEE',  # Череповец, не областной центр с аэропортом
                'majority_id': 3,
                'region_id': 3,
            }),
            self._create_settlement({
                'id': 8172,
                '_geo_id': 81720,
                'iata': 'VGD',  # Вологда, областной центр для CEE
                'majority_id': 2,
                'region_id': 3,
            }),
            self._create_settlement({
                'id': 20,  # сеттлмент без iata-кода
                '_geo_id': 200,
                'sirena_id': 'London',
                'majority_id': 1,
                'country_id': 2,
                'region_id': 201,
            }),
            self._create_settlement({
                'id': 21,  # сеттлмент без iata-кода
                '_geo_id': 210,
                'sirena_id': 'Gatwick',
                'iata': 'LGW',
                'majority_id': 2,
                'country_id': 2,
                'region_id': 201,
            }),
            self._create_settlement({
                'id': 81744,  # сеттлмент без аэропорта
                '_geo_id': 817440,
                'sirena_id': 'Vozhega',
                'majority_id': 3,
                'region_id': 3,
            }),
            self._create_settlement({
                'id': 153,
                '_geo_id': 1530,
                'iata': 'BQT',  # Брест, аэропорт извне РФ, с направлениями в локали 'ru'
                'majority_id': 2,
                'region_id': 21623,
                'country_id': 149,
            }),
            self._create_settlement({
                'id': 157,
                '_geo_id': 1570,
                'iata': 'MSQ',  # Минск, столица страны извне РФ, с направлениями в локали 'ru'
                'majority_id': 1,
                'region_id': 21622,
                'country_id': 149,
            }),
            self._create_settlement({
                'id': 11508,
                '_geo_id': 11508,
                'iata': 'IST',  # Стамбул, для проверки костыля com.tr
                'majority_id': 1,
                'region_id': 22287,
                'country_id': 983,
            }),
        ]

    @staticmethod
    def _create_settlement(data):
        # type: (dict) -> dict
        s = {
            'id': 1,
            '_geo_id': 10,
            'new_L_title_id': 1,
            'time_zone': 'Europe/Moscow',
            'country_id': 225,
            'region_id': 1,
            'sirena_id': 'СИР',
            'iata': '',
            '_disputed_territory': False,
            'majority_id': 1,
            'latitude': 1,
            'longitude': 2,
        }
        s.update(data)

        return s

    @staticmethod
    def _get_images():
        # type: () -> List[dict]
        return [
            {
                'id': 495,
                'settlement_id': 495,
                'url2': 'http://495',
            },
            {
                'id': 343,
                'settlement_id': 343,
                'url2': 'http://343',
            },
            {
                'id': 20,
                'settlement_id': 20,
                'url2': 'http://20',
            },
            {
                'id': 999,
                'url2': 'http://default',
            },
        ]

    def _prepare_brief(self, response):
        # type: (list) -> list
        """
        Response в форме короткой строки, позволяет at a glance понять на на отладочных запросах,
        если что-то пошло не так
        """
        return [self._shorten(itinerary) for itinerary in response]

    def _shorten(self, itinerary):
        # type: (dict) -> dict

        settlement_from = itinerary.get('settlement_from')
        settlement_to = itinerary.get('settlement_to')

        if not settlement_from or not settlement_to:
            return None

        image_char = 'I' if itinerary.get('image') is not None else ' '

        forward_date = itinerary.get('forward_date')
        price = itinerary.get('min_price')
        price_text = '{amount}.{currency}'.format(
            amount=price.get('value') or 0,
            currency=price.get('currency') or '') if price else '<no price>'

        return '{from_}/{to}/{date}/{price}/{image}'.format(
            from_=self._short_name(settlement_from),
            to=self._short_name(settlement_to),
            date=forward_date or '<no-date>',
            price=price_text,
            image=image_char)

    @staticmethod
    def _short_name(settlement):
        # type: (dict) -> unicode
        if not settlement:
            return '***'

        if settlement.get('iata'):
            return settlement.get('iata')

        if settlement.get('point_key'):
            return settlement.get('point_key')

        return settlement.get('title') or 'c{}'.format(settlement.get('id'))


class MockImage:
    def __init__(self, link):
        self.url2 = link


class MockResponse:
    def __init__(self, json_data):
        # type: (dict) -> None
        self._json_data = json_data

    def json(self):
        return self._json_data

    @property
    def status_code(self):
        return 200


class DirectionWithRepr(Direction):
    def __init__(self, departure_settlement_id, arrival_settlement_id,
                 direct_flights, connecting_flights,
                 popularity, national_version):
        super(DirectionWithRepr, self).__init__(departure_settlement_id, arrival_settlement_id,
                                                direct_flights, connecting_flights,
                                                popularity, national_version)

    def __repr__(self):
        return '{}-{}'.format(self.departure_settlement_id, self.arrival_settlement_id)
