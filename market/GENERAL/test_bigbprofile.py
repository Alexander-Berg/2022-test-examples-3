#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    HyperCategory,
    HyperCategoryType,
    Model,
    NavCategory,
    Offer,
    Shop,
    YamarecFeaturePartition,
    YamarecPlace,
)
from core.matcher import Contains
from core.testcase import main
from core.bigb import ModelLastSeenEvent, MarketModelLastTimeCounter, BeruModelViewLastTimeCounter
from simple_testcase import SimpleTestCase
from core.crypta import CryptaFeature, CryptaName

personal_categories = 'debug=1&place=personal_categories&yandexuid=400&rearr-factors=recom_history_from_bigb_only=1;recom_history_from_bigb_include_beru=1'
expected_fragment = {
    'search': {
        'total': 2,
        'results': [
            {'link': {'params': {'hid': '201'}}},
            {'link': {'params': {'hid': '202'}}},
        ],
    }
}


class T(SimpleTestCase):
    '''
    Test some places that call Big B internally.
    '''

    @classmethod
    def prepare(cls):
        '''
        hyperid=1..
        hid=2..
        vendor_id=3..
        yandexuid=4..
        region=5..
        fesh=6..
        price=7..
        '''

        cls.index.models += [
            Model(hyperid=100, hid=200, vendor_id=300),
            Model(hyperid=101, hid=200, vendor_id=301),
            Model(hyperid=102, hid=200, vendor_id=302),
            Model(hyperid=103, hid=201, vendor_id=300),
            Model(hyperid=104, hid=201, vendor_id=301),
            Model(hyperid=105, hid=201, vendor_id=302),
            Model(hyperid=106, hid=202, vendor_id=303),
            Model(hyperid=107, hid=202, vendor_id=303),
            Model(hyperid=108, hid=202, vendor_id=303),
            Model(hyperid=109, hid=202, vendor_id=303),
        ]

        cls.index.offers += [
            Offer(hyperid=100, fesh=600, discount=0),
            Offer(hyperid=101, fesh=600, discount=35),
            Offer(hyperid=102, fesh=600, discount=40),
            Offer(hyperid=103, fesh=600, discount=35),
            Offer(hyperid=104, fesh=600, discount=40),
            Offer(hyperid=107, fesh=600, discount=90),
        ]

        cls.index.shops += [
            Shop(fesh=600, priority_region=500),
            Shop(fesh=601, priority_region=500),
            Shop(fesh=602, priority_region=501),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=203,
                children=[HyperCategory(hid=child, output_type=HyperCategoryType.GURU) for child in range(200, 203)],
            ),
        ]

        cls.index.navtree += [
            NavCategory(hid=200),
            NavCategory(hid=201),
            NavCategory(hid=202),
            NavCategory(hid=203),
        ]

        cls.recommender.on_request_models_of_interest(user_id="yandexuid:400", item_count=1000).respond(
            {"models": ['100']}
        )

        cls.bigb.on_request(yandexuid='400', client='merch-machine').respond(
            counters=[
                MarketModelLastTimeCounter(
                    model_view_events=[
                        ModelLastSeenEvent(model_id=103, timestamp=0),
                    ]
                ),
                BeruModelViewLastTimeCounter(
                    model_view_events=[
                        ModelLastSeenEvent(model_id=106, timestamp=1),
                    ]
                ),
            ]
        )

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.CATEGORY_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        feature_names=['category_id', 'position'],
                        feature_keys=['category_id'],
                        features=[],
                    ),
                ],
            )
        ]

        cls.crypta.on_request_profile(yandexuid=400).respond(
            features=[CryptaFeature(name=CryptaName.GENDER_MALE, value=100)]
        )
        cls.crypta.on_default_request().respond(features=[])

    def test_shiny_log(self):
        '''
        Тест попадания нового клиента в логи
        '''

        response = self.report.request_json(personal_categories)
        self.assertFragmentIn(response, expected_fragment)

        self.external_services_log.expect(service='bigb_profile').times(1)
        self.external_services_trace_log.expect(target_module='Bigb Profile').times(1)

    def __get_cached_items(self):
        memcached_client = self.memcached.get_client()
        return int(memcached_client.get_stats()[0][1]['curr_items'])

    @classmethod
    def prepare_shiny_caching(cls):
        cls.settings.memcache_enabled = True

    def test_shiny_caching(self):
        '''
        Тест глобального кэширования
        '''

        cached_items_before = self.__get_cached_items()

        # Без кэширования
        response = self.report.request_json(personal_categories)
        self.assertFragmentIn(response, expected_fragment)

        response = self.report.request_json(personal_categories + ';bigb_shiny_client_memcached_ttl_min=1')
        self.assertFragmentIn(response, expected_fragment)

        self.assertEqual(cached_items_before + 1, self.__get_cached_items())

        response = self.report.request_json(personal_categories + ';bigb_shiny_client_memcached_ttl_min=1')
        self.assertFragmentIn(response, expected_fragment)

        # Сейчас только запрос memcached попадает в external_services_log.
        self.external_services_log.expect(service='memcached_bigb_profile', http_code=204).once()
        self.external_services_log.expect(service='memcached_set_bigb_profile', http_code=200).once()
        self.external_services_log.expect(service='memcached_bigb_profile', http_code=200).once()

    def test_empty_response(self):
        '''
        Тест отсутствия ошибки при пустом запросе (без всех ID)
        '''

        response = self.report.request_json(
            'debug=1&place=personal_categories&rearr-factors=recom_history_from_bigb_only=1;recom_history_from_bigb_include_beru=1'
        )
        self.assertFragmentIn(
            response, {"debug": {"report": {"logicTrace": [Contains('WARNING not sending empty request')]}}}
        )
        self.error_log.expect(code=3755).never()


if __name__ == '__main__':
    main()
