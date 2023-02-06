#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, MarketSku, Model, Offer, Shop
from core.matcher import Absent, LikeUrl, NotEmpty

PLATFORM = "&platform=touch"


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.settings.disable_url_encryption = False

        cls.index.shops += [
            Shop(fesh=1),
            Shop(fesh=375159),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=1, title='iphone'),
            Model(hyperid=2, hid=1, title='blue iphone'),
            Model(hyperid=3, hid=1, title='test shop excluding'),
        ]

        cls.index.offers += [
            Offer(
                title='iphone offer 1',
                hyperid=1,
                url='https://test.shop.ru/product/1',
                fesh=1,
                waremd5='6q6T5v-BnLr2Z91X30SCZw',
                vendor_id=101,
            ),
            Offer(
                title='iphone offer 2',
                hyperid=1,
                url='https://test.shop.ru/product/2',
                fesh=1,
                waremd5='nTOjMgNUtwYTaB-M304r5Q',
                vendor_id=101,
            ),
            Offer(
                title='shop should be excluded',
                hyperid=3,
                url='https://375159.test.shop.ru/product/3',
                fesh=375159,
                waremd5='eQMntPCMIGxaqxF894EmKg',
                vendor_id=101,
            ),
        ]

        turbo_urls = dict()
        for i in range(150):
            url = 'https://many.offers.test.shop.ru/product/{}'.format(i)
            cls.index.offers.append(
                Offer(
                    title='TurboOffer {}'.format(i),
                    hyperid=5,
                    url=url,
                    fesh=2,
                    vendor_id=103,
                ),
            )
            turbo_urls[url] = '/turbo?text={}'.format(url)

        cls.index.mskus += [
            MarketSku(
                hyperid=2,
                sku=2,
                blue_offers=[
                    BlueOffer(price=200, feedid=100, waremd5='DNYO-1V4zZZ8PCOJTzBYAA', fesh=1, vendor_id=102),
                ],
            ),
        ]

        turbo_urls.update(
            {
                'https://beru.ru/product/2?offerid=DNYO-1V4zZZ8PCOJTzBYAA': '/turbo?text=https://beru.ru/product/2',
                'https://test.shop.ru/product/1': '/turbo?text=https://test.shop.ru/product/1?clid=928',
                'https://375159.test.shop.ru/product/3': '/turbo?text=https://375159.test.shop.ru/product/3',
            }
        )
        cls.turbo.fill_urls(turbo_urls)

    def test_blue_exp(self):
        exp_response = {
            'sku': '2',
            'urls': {
                'directTurbo': LikeUrl.of(
                    'https://hamster.yandex.ru/turbo?text=https%3A%2F%2Fpokupki.market.yandex.ru%2Fproduct%2F2%3Fofferid%3DDNYO-1V4zZZ8PCOJTzBYAA&clid=928'
                ),
            },
        }
        without_exp_response = {
            'sku': '2',
            'urls': {
                'directTurbo': Absent(),
            },
        }
        exps = (
            ('rearr-factors=market_use_turbo_offers=1;market_shops_turbo_offers=1', exp_response),
            ('rearr-factors=market_use_turbo_offers=0', without_exp_response),
            ('rearr-factors=market_use_turbo_offers=1', exp_response),
        )
        requests = (
            'place=prime&hyperid=2&{}&show-urls=directTurbo&rearr-factors=market_metadoc_search=no',
            'place=offerinfo&offerid=DNYO-1V4zZZ8PCOJTzBYAA&show-urls=directTurbo&rids=0&regset=1&{}',
            'place=productoffers&hyperid=2&show-urls=directTurbo&rids=0&{}',
            'place=vendor_offers_models&hid=1&vendor_id=102&{}&show-urls=directTurbo',
            'place=defaultoffer&pp=18&hyperid=2&bsformat=2&{}&show-urls=directTurbo',
        )
        for request in requests:
            for exp, resp in exps:
                response = self.report.request_json(request.format(exp) + PLATFORM)
                self.assertFragmentIn(response, resp)

    def test_shops_exp(self):
        exp_response = [
            {
                'titles': {
                    'raw': 'iphone offer 1',
                },
                'urls': {
                    'directTurbo': LikeUrl.of(
                        'https://hamster.yandex.ru/turbo?text=https://test.shop.ru/product/1?clid=928'
                    ),
                },
            },
            {
                'titles': {
                    'raw': 'iphone offer 2',
                },
                'urls': {
                    'directTurbo': Absent(),
                },
            },
        ]
        without_exp_response = [
            {
                'titles': {
                    'raw': 'iphone offer 1',
                },
                'urls': {
                    'directTurbo': Absent(),
                },
            },
            {
                'titles': {
                    'raw': 'iphone offer 2',
                },
                'urls': {
                    'directTurbo': Absent(),
                },
            },
        ]
        exps = (
            ('rearr-factors=market_use_turbo_offers=1', exp_response),
            ('rearr-factors=market_use_turbo_offers=0;market_shops_turbo_offers=0', without_exp_response),
            ('rearr-factors=market_use_turbo_offers=1;market_shops_turbo_offers=0', without_exp_response),
        )
        requests = (
            'place=prime&hyperid=1&{}&show-urls=directTurbo',
            'place=offerinfo&offerid=6q6T5v-BnLr2Z91X30SCZw,nTOjMgNUtwYTaB-M304r5Q&show-urls=directTurbo&rids=0&regset=1&{}',
            'place=productoffers&hyperid=1&show-urls=directTurbo&rids=0&{}',
            'place=vendor_offers_models&hid=1&vendor_id=101&{}&show-urls=directTurbo',
        )
        for request in requests:
            for exp, resp in exps:
                response = self.report.request_json(request.format(exp) + PLATFORM)
                self.assertFragmentIn(response, resp)

    def test_exclude_shops_from_exp(self):
        response = self.report.request_json(
            'place=offerinfo&offerid=eQMntPCMIGxaqxF894EmKg&show-urls=directTurbo&rids=0&regset=1&rearr-factors=market_use_turbo_offers=1;market_shops_turbo_offers=1'
            + PLATFORM
        )
        self.assertFragmentIn(
            response,
            {
                'titles': {
                    'raw': 'shop should be excluded',
                },
                'urls': {
                    'directTurbo': Absent(),
                },
            },
        )

    def test_blue_and_white_offers(self):
        requests = (
            'place=prime&hyperid=1,2&rearr-factors=market_use_turbo_offers=1;market_shops_turbo_offers=1&show-urls=directTurbo&rearr-factors=market_metadoc_search=no',
            'place=offerinfo&offerid=6q6T5v-BnLr2Z91X30SCZw,DNYO-1V4zZZ8PCOJTzBYAA&show-urls=directTurbo&rids=0&regset=1&rearr-factors=market_use_turbo_offers=1;market_shops_turbo_offers=1',
            'place=productoffers&hyperid=1,2&show-urls=directTurbo&rids=0&rearr-factors=market_use_turbo_offers=1;market_shops_turbo_offers=1',
        )
        expected_response = [
            {
                'sku': '2',
                'urls': {
                    'directTurbo': LikeUrl.of(
                        'https://hamster.yandex.ru/turbo?text=https%3A%2F%2Fpokupki.market.yandex.ru%2Fproduct%2F2%3Fofferid%3DDNYO-1V4zZZ8PCOJTzBYAA&clid=928'
                    ),
                },
            },
            {
                'titles': {
                    'raw': 'iphone offer 1',
                },
                'urls': {
                    'directTurbo': LikeUrl.of(
                        'https://hamster.yandex.ru/turbo?text=https://test.shop.ru/product/1?clid=928'
                    ),
                },
            },
        ]
        for request in requests:
            response = self.report.request_json(request + PLATFORM)
            self.assertFragmentIn(response, expected_response)

    def test_text_in_turbo_url(self):
        """Проверяем, что в заэнкрипченный турбо урл добавляется корректный text= снаружи"""
        response = self.report.request_json(
            'place=offerinfo&offerid=DNYO-1V4zZZ8PCOJTzBYAA&show-urls=encryptedTurbo&rids=0&regset=1&rearr-factors=market_use_turbo_offers=1;market_shops_turbo_offers=1'
            + PLATFORM
        )

        # Нужно сформировать LikeUrl без path (так как ссылка шифрованная)
        # Если пользоваться стандартным LikeUrl.of, то в path попадает пустая строка и сравнение падает
        self.assertFragmentIn(
            response,
            {
                'encryptedTurbo': LikeUrl(
                    url_params={'text': ['https://pokupki.market.yandex.ru/product/2?offerid=DNYO-1V4zZZ8PCOJTzBYAA']}
                ),
            },
        )

    def test_turbo_exp_without_blue(self):
        """Проверяем, что можно запустить отдельно эксперимент на все магазины, кроме синего"""
        requests = (
            'place=prime&hyperid=1,2&rearr-factors=market_use_turbo_offers=0;market_shops_turbo_offers=1&show-urls=directTurbo&rearr-factors=market_metadoc_search=no',
            'place=offerinfo&offerid=6q6T5v-BnLr2Z91X30SCZw,DNYO-1V4zZZ8PCOJTzBYAA&show-urls=directTurbo&rids=0&regset=1&rearr-factors=market_use_turbo_offers=0;market_shops_turbo_offers=1',
            'place=productoffers&hyperid=1,2&show-urls=directTurbo&rids=0&rearr-factors=market_use_turbo_offers=0;market_shops_turbo_offers=1',
        )
        expected_response = [
            {
                'sku': '2',
                'urls': {
                    'directTurbo': Absent(),
                },
            },
            {
                'titles': {
                    'raw': 'iphone offer 1',
                },
                'urls': {
                    'directTurbo': LikeUrl.of(
                        'https://hamster.yandex.ru/turbo?text=https://test.shop.ru/product/1?clid=928'
                    ),
                },
            },
        ]
        for request in requests:
            response = self.report.request_json(request + PLATFORM)
            self.assertFragmentIn(response, expected_response)

    def test_many_urls(self):
        """Проверяем, что количество урлов, запрашиваемых из турбо API не превысит 100"""
        response = self.report.request_json(
            'place=prime&text=TurboOffer&numdoc=500&debug=1&rearr-factors=market_use_turbo_offers=1;market_shops_turbo_offers=1&show-urls=directTurbo'
            + PLATFORM
        )
        self.assertNotIn('Urls count exceeded the limit of {}'.format(self.turbo.max_turbo_urls_count), str(response))

        self.assertEqual(response.count({'directTurbo': NotEmpty()}), self.turbo.max_turbo_urls_count)

    def __get_cached_items(self):
        memcached_client = self.memcached.get_client()
        return int(memcached_client.get_stats()[0][1]['curr_items'])

    @classmethod
    def prepare_shiny_caching(cls):
        cls.settings.memcache_enabled = True

    def test_shiny_caching(self):
        """Тест глобального кэширования"""

        cached_items_before = self.__get_cached_items()

        exp_response = [
            {
                'titles': {
                    'raw': 'iphone offer 1',
                },
                'urls': {
                    'directTurbo': LikeUrl.of(
                        'https://hamster.yandex.ru/turbo?text=https://test.shop.ru/product/1?clid=928'
                    ),
                },
            },
            {
                'titles': {
                    'raw': 'iphone offer 2',
                },
                'urls': {
                    'directTurbo': Absent(),
                },
            },
        ]

        exp = 'rearr-factors=market_use_turbo_offers=1;turbo_client_memcached_ttl_min=1'

        requests = (
            'place=prime&hyperid=1&{}&show-urls=directTurbo',
            'place=offerinfo&offerid=6q6T5v-BnLr2Z91X30SCZw,nTOjMgNUtwYTaB-M304r5Q&show-urls=directTurbo&rids=0&regset=1&{}',
            'place=productoffers&hyperid=1&show-urls=directTurbo&rids=0&{}',
            'place=vendor_offers_models&hid=1&vendor_id=101&{}&show-urls=directTurbo',
        )

        for request in requests:
            response = self.report.request_json(request.format(exp) + PLATFORM)
            self.assertFragmentIn(response, exp_response)

        # на таких запросах ровно два уникальных в кэше
        self.assertEqual(cached_items_before + 2, self.__get_cached_items())

        for request in requests:
            response = self.report.request_json(request.format(exp) + PLATFORM)
            self.assertFragmentIn(response, exp_response)

        self.external_services_log.expect(service='memcached_turbo_client', http_code=204).times(2)
        self.external_services_log.expect(service='memcached_set_turbo_client').times(2)
        self.external_services_log.expect(service='memcached_turbo_client', http_code=200).times(6)


if __name__ == '__main__':
    main()
