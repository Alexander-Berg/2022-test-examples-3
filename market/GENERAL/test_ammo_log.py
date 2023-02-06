#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import os
from core.types import (
    FormalizedParam,
    GLType,
    HyperCategory,
    Offer,
    Shop,
    Suggestion,
    YamarecFeaturePartition,
    YamarecPlace,
)
from core.testcase import TestCase, main
from core.logs import parse_tskv
from core.crypta import CryptaFeature, CryptaName

from market.pylibrary.lite.vars import touch


class T(TestCase):
    _ammo_log = None

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        log_path = os.path.join(cls.meta_paths.logs, 'ammo.log')
        touch(log_path)
        cls._ammo_log = open(log_path)

        cls.index.shops += [Shop(fesh=10)]

        cls.index.hypertree += [HyperCategory(hid=20)]

        cls.index.offers += [Offer(fesh=10, hid=20, hyperid=30, title='white iphone')]

        cls.index.gltypes += [
            GLType(param_id=40, hid=20, gltype=GLType.ENUM, values=[1, 2]),
        ]

        # Test handling of custom WizClient in report config
        cls.reqwizard.wizclient = 'custom_wizclient'

        cls.reqwizard.on_request('ignore').respond(remove_query=True)
        cls.reqwizard.on_default_request().respond()
        cls.reqwizard.on_request('reqwizard 404').return_code(404)
        cls.reqwizard.on_request('shini+na+skoda').respond(tires_mark='Skoda')

        cls.formalizer.on_request(hid=20, query='white+iphone').respond(
            formalized_params=[FormalizedParam(param_id=40, value=1, is_numeric=False, value_positions=(0, 5))]
        )
        cls.formalizer.on_default_request().respond()

        cls.speller.on_request(text='ifone').respond(originalText='i<fix>f</fix>one', fixedText='i<fix>ph</fix>one')
        cls.speller.on_default_request().respond()

        cls.suggester.on_request(part='phones').respond(
            suggestions=[Suggestion(part='mobile phones', url='/catalog/11111/list?suggest=1')]
        )
        cls.suggester.on_default_request().respond()

        cls.index.yamarec_places += [
            YamarecPlace(
                name=YamarecPlace.Name.ARTICLE_GENERIC,
                kind=YamarecPlace.Type.FORMULA,
                partitions=[
                    YamarecFeaturePartition(
                        formula_id=50,
                        splits=['*'],
                        feature_keys=['article_id', 'category_id'],
                        feature_names=['article_id', 'category_id', 'goodness'],
                        features=[[52, 20, 1]],
                    )
                ],
            )
        ]

        cls.crypta.on_request_profile(yandexuid=1).respond(
            features=[CryptaFeature(name=CryptaName.GENDER_MALE, value=100)]
        )
        cls.crypta.on_default_request().respond(features=[])

    def logged_request(self, response_type, url):
        response = self.report.request(response_type, url)
        self.assertFragmentIn(self.report.request_xml('admin_action=flushlogs'), '<status>Logs flushed ok</status>')
        return response, self._ammo_log.readline()

    def test_reqwizard_mock(self):
        had_requests = self.reqwizard.counter.overall()
        response, _ = self.logged_request('json', 'place=prime&hid=20&text=ignore')
        self.assertFragmentIn(response, {"search": {"results": [{"entity": "offer", "model": {"id": 30}}]}})

        response, _ = self.logged_request('json', 'place=prime&hid=20&text=nokia')
        self.assertFragmentNotIn(response, {"search": {"results": [{"entity": "offer", "model": {"id": 30}}]}})
        self.assertEqual(self.reqwizard.counter.overall(), had_requests + 1)

        self.error_log.ignore(code=3021)

    def test_reqwizard_duplicates(self):
        had_requests = self.reqwizard.counter.overall()
        self.logged_request('json', 'place=prime&hid=20&text=nokia&askreqwizard=1')
        self.assertEqual(self.reqwizard.counter.overall(), had_requests + 1)
        self.error_log.ignore(code=3021)

    def check_service_log(self, url, response_type, service):
        had_requests = service.counter.overall()
        name = service.name() + '-history'
        response, log = self.logged_request(response_type, url)
        tskv = parse_tskv(log)
        self.assertTrue(name in tskv)
        self.assertEqual(service.counter.overall(), had_requests + 1)
        had_requests = service.counter.overall()
        url_with_param = url + '&{}={}'.format(name, tskv[name])
        response_with_param, _ = self.logged_request(response_type, url_with_param)
        self.assertEqual(str(response), str(response_with_param))
        self.assertEqual(service.counter.overall(), had_requests)
        self.error_log.ignore(code=3021)

    def test_formalizer(self):
        # https://st.yandex-team.ru/MARKETOUT-25652
        self.check_service_log('place=prime&text=white+iphone&cvredirect=1', 'json', self.formalizer)

    def test_speller(self):
        self.check_service_log('place=prime&text=ifone', 'json', self.speller)

    def test_suggest(self):
        self.check_service_log('place=prime&text=phones&cvredirect=1', 'json', self.suggester)

    def test_reqwizard_network_error(self):
        url = 'place=prime&text=reqwizard+404'
        self.logged_request('json', url)
        self.error_log.expect(code=3665)
        # FIXME: uncomment after MARKETOUT-10940
        # self.error_log.expect(code=3021)

    def test_reqwizard_parsing_error(self):
        url = 'place=prime&text=iphone'
        self.logged_request('json', url)
        # FIXME: uncomment after MARKETOUT-10940
        # self.error_log.expect(code=3021)

    def test_reqwizard_access_log(self):
        url = 'place=prime&text=iphone'
        response, _ = self.logged_request('json', url)
        # self.access_log.expect(req_wiz_time=GreaterEq(0), req_wiz_count=1)
        self.error_log.ignore(code=3021)

    def test_wizards_in_log(self):
        url_with_wizard = 'place=parallel&text=shini+na+skoda&rearr-factors=market_parallel_wizard=1&askreqwizard=1'
        _, log_line_with_wizard = self.logged_request('bs', url_with_wizard)
        tskv = parse_tskv(log_line_with_wizard)
        self.assertTrue('wizards' in tskv and tskv['wizards'] == 'market_tires')

        url_wo_wizard = 'place=parallel&text=koti+na+more&rearr-factors=market_parallel_wizard=1&askreqwizard=1'
        _, log_line_wo_wizard = self.logged_request('bs', url_wo_wizard)
        tskv = parse_tskv(log_line_wo_wizard)
        self.assertFalse('wizards' in tskv)

        self.error_log.ignore(code=3021)


if __name__ == '__main__':
    main()
