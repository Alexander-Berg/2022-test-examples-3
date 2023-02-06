#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main
from core.types import Offer, Shop, DynamicMarketSku, Region
from core.bots import BOT_USER_AGENTS
from core.matcher import NotEmpty, Contains


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += [
            "market_money_disable_bids=0",
            "market_report_blender_premium_ios_text_redirect=0",
        ]
        cls.settings.memcache_enabled = True
        cls.index.shops += [
            Shop(fesh=10, cpa=Shop.CPA_REAL),
        ]
        cls.index.offers += [
            Offer(title='offer visible', fesh=10, cpa=Offer.CPA_REAL),
            Offer(title='offer visible', fesh=10),
            Offer(title='offer hidden by offer id', fesh=10, offerid='offer_id', hid=11),
        ]

    def test_bot_cache(self):
        """Проверяем что работает флаг эксперимента bot_cache"""
        cached_items_before = self.__get_cached_items()

        bot_agent = BOT_USER_AGENTS[0]

        # делаем запрос и сохраняем его в кэше
        original_response = self.report.request_json(
            'place=prime&fesh=10&rearr-factors=bot_cache=8', headers={"User-Agent": bot_agent}
        )
        # отключаем офер чтобы поменять выдачу
        self.dynamic.market_dynamic.disabled_market_sku = [
            DynamicMarketSku(supplier_id=10, shop_sku='offer_id'),
        ]

        # делаем то же запрос как пользователь (не бот), ответ должен быть другой
        response_for_customer = self.report.request_json('place=prime&fesh=10&rearr-factors=bot_cache=8')
        self.assertNotEqual(str(original_response), str(response_for_customer))

        for user_agent in BOT_USER_AGENTS:
            bot_headers = {"User-Agent": bot_agent}

            # делаем то же запрос, он должен быть скопирован из кэша
            response_from_cache = self.report.request_json(
                'place=prime&fesh=10&rearr-factors=bot_cache=8', headers=bot_headers
            )
            self.assertEqual(str(original_response), str(response_from_cache))

            # делаем то же запрос, но с выключеным кешированием
            response_without_cache = self.report.request_json(
                'place=prime&fesh=10&rearr-factors=bot_cache=0', headers=bot_headers
            )
            self.assertNotEqual(str(original_response), str(response_without_cache))
            self.assertEqual(str(response_for_customer), str(response_without_cache))

        # и запрос без реара, кэш не будет использован и выдача должна быть другой
        response_with_disabled_offer = self.report.request_json('place=prime&fesh=10')
        self.assertNotEqual(str(original_response), str(response_with_disabled_offer))

        # статистика должна показать что в кэше стало на один элемент больше
        cached_items_after = self.__get_cached_items()
        self.assertEqual(cached_items_before + 1, cached_items_after)

    def __get_cached_items(self):
        memcached_client = self.memcached.get_client()
        return int(memcached_client.get_stats()[0][1]['curr_items'])

    def test_prime_empty_cache(self):
        """
        Проверяем кеширование пустых запросов на прайме и попадание запрос к memcached в лог
        """
        cached_items_before = self.__get_cached_items()

        # Отключаем оффер, чтобы был пустой ответ
        self.dynamic.market_dynamic.disabled_market_sku = [
            DynamicMarketSku(supplier_id=10, shop_sku='offer_id'),
        ]

        # Проверяем, что ответ на этот запрос мы закешироввали
        original_response = self.report.request_json(
            'place=prime&hid=11&rearr-factors=market_empty_response_cache_ttl=0.25&reqid=cb8f7a995444ab4dc3215cad33fcd6bb'
        )
        self.assertEqual(cached_items_before + 1, self.__get_cached_items())

        cache_info = {
            "debug": {
                "cache": [
                    {
                        "key": "bsformat=2&hid=11&place=prime&pp=18"
                        "&rearr-factors=market_empty_response_cache_ttl%3D0.25;"
                        "parallel_smm%3D1.0;ext_snippet%3D1;no_snippet_arc%3D1;market_enable_sins_offers_wizard%3D1"
                        "&show-urls=external%2Ccpa&timeout=10000000&waitall=da",
                        "savedAt": NotEmpty(),
                        "savedFromReqId": "cb8f7a995444ab4dc3215cad33fcd6bb",
                        "ttl": "900s",
                        "type": "prime_empty_responses",
                    }
                ]
            }
        }

        self.dynamic.market_dynamic.disabled_market_sku = []
        response_from_cache = self.report.request_json(
            'place=prime&hid=11&rearr-factors=market_empty_response_cache_ttl=0.25'
        )
        self.assertFragmentIn(
            response_from_cache, original_response.root, allow_different_len=False, preserve_order=True
        )
        self.assertFragmentIn(response_from_cache, cache_info, allow_different_len=False, preserve_order=True)

        # Этот ответ мы должны взять не из кеша
        response_with_offer = self.report.request_json(
            'place=prime&hid=11&rearr-factors=market_empty_response_cache_ttl=0'
        )
        self.assertNotEqual(response_with_offer.text, response_from_cache.text)
        self.assertNotEqual(original_response.text, response_with_offer.text)
        self.assertFragmentNotIn(response_with_offer, {"debug": {"cache": NotEmpty()}})

        self.assertEqual(cached_items_before + 1, self.__get_cached_items())

        self.external_services_log.expect(service='memcached_prime_empty_responses').times(3)
        self.external_services_log.expect(service='memcached_set_prime_empty_responses').times(1)

    @classmethod
    def prepare_for_test_redirect_cache(cls):

        cls.index.regiontree += [Region(rid=213, name='Москва'), Region(rid=2, name='Питер')]
        cls.index.shops += [
            Shop(fesh=15, regions=[225], cpa=Shop.CPA_REAL),
        ]
        cls.index.offers += [Offer(hid=12, fesh=15, title="redirect cache", cpa=Offer.CPA_REAL)]

    def test_redirect_cache(self):
        """
        в кеше сохраняются только отдельные rearr-флаги (в том числе их значения по умолчанию в стопкране)
        а также основные дефолты (формулы, пороги) забитые в __defaults_hash__
        любые другие не влияют на ключ кеша
        :return:
        """

        redirect = {
            "redirect": {
                "params": {"text": ["redirect cache"], "hid": ["12"], "rt": ["9"], "rs": ["eJwzYqli4uABAANQAMc,"]},
                "target": "search",
            }
        }
        cache_exist = {"debug": {"cache": NotEmpty()}}

        def cache_info_from(reqid):
            return {
                "debug": {
                    "cache": [
                        {
                            "type": "redirects",
                            "key": Contains(
                                "rearr-factors=market_category_redirect_treshold%3D-3;"
                                "market_relevance_formula_threshold%3D-1;__defaults_hash__%3D",
                                "&text=redirect+cache",
                            ),
                            "savedFromReqId": reqid,
                            "savedAt": NotEmpty(),
                            "ttl": "3600s",
                        }
                    ]
                }
            }

        cache_info = cache_info_from("d1fd58282fa65212c2be3fd33ddd0500")
        cache_info_updated = cache_info_from("2b606b626b4afa710f378b6c6825adc")

        cached_items_before = self.__get_cached_items()

        response = self.report.request_json(
            'place=prime&text=redirect+cache&rids=213&pp=18&cvredirect=1&reqid=d1fd58282fa65212c2be3fd33ddd0500'
            '&rearr-factors=market_redirect_cache_ttl=1;market_some_other_rearr=1'
        )

        self.assertFragmentIn(response, redirect, allow_different_len=False)
        self.assertFragmentNotIn(response, cache_exist)
        self.assertEqual(cached_items_before + 1, self.__get_cached_items())

        response = self.report.request_json(
            'place=prime&text=redirect+cache&rids=213&pp=18&cvredirect=1&reqid=619be8a28ff09a30ca93d1f9'
            '&rearr-factors=market_redirect_cache_ttl=1;market_some_other_rearr=2;market_some_other_rearr=1'
        )
        self.assertFragmentIn(response, redirect, allow_different_len=False)
        self.assertFragmentIn(response, cache_info, allow_different_len=False)

        # изменение региона не влияет на редирект
        response = self.report.request_json(
            'place=prime&text=redirect+cache&rids=2&pp=18&cvredirect=1&reqid=619be8a28ff09a30ca93d1f9'
            '&rearr-factors=market_redirect_cache_ttl=1;market_some_other_rearr=1'
        )
        self.assertFragmentIn(response, redirect, allow_different_len=False)
        self.assertFragmentIn(response, cache_info, allow_different_len=False)

        # debug=da тоже не влияет на редирект
        response = self.report.request_json(
            'place=prime&text=redirect+cache&rids=2&pp=18&debug=da&cvredirect=1&reqid=619be8a28ff09a30ca93d1f9'
            '&rearr-factors=market_redirect_cache_ttl=1;market_some_other_rearr=1'
        )
        self.assertFragmentIn(response, redirect, allow_different_len=False)
        self.assertFragmentIn(response, cache_info, allow_different_len=False)

        # nocache=1 отключает кеш в запросе
        response = self.report.request_json(
            'place=prime&text=redirect+cache&rids=2&pp=18&nocache=1&cvredirect=1&reqid=619be8a28ff09a30ca93d1f9'
            '&rearr-factors=market_redirect_cache_ttl=1;market_some_other_rearr=1'
        )
        self.assertFragmentIn(response, redirect, allow_different_len=False)
        self.assertFragmentNotIn(response, cache_exist)

        # флагом market_redirect_cache_force_update=1 редирект не берется из кеша но записывается в него
        cached_items_before = self.__get_cached_items()
        response = self.report.request_json(
            'place=prime&text=redirect+cache&rids=2&pp=18&cvredirect=1&reqid=2b606b626b4afa710f378b6c6825adc'
            '&rearr-factors=market_redirect_cache_ttl=1;market_some_other_rearr=1;market_redirect_cache_force_update=1'
        )
        self.assertFragmentIn(response, redirect, allow_different_len=False)
        self.assertFragmentNotIn(response, cache_exist)
        # мы записали данные по тому же ключу, поэтому количество закешированных элементов не увеличилось
        self.assertEqual(cached_items_before, self.__get_cached_items())

        response = self.report.request_json(
            'place=prime&text=redirect+cache&rids=213&pp=18&cvredirect=1&reqid=619be8a28ff09a30ca93d1f9'
            '&rearr-factors=market_redirect_cache_ttl=1;market_some_other_rearr=2;market_some_other_rearr=1'
        )
        self.assertFragmentIn(response, redirect, allow_different_len=False)
        self.assertFragmentIn(response, cache_info_updated, allow_different_len=False)

    def test_only_cached(self):
        """
        &only-cached=1 возвращает очень бысто закешированный редирект если он есть иначе пустой ответ
        """
        redirect = {
            "redirect": {
                "params": {
                    "text": ["redirect cache from cache"],
                    "hid": ["12"],
                    "rt": ["9"],
                    "rs": ["eJwzYqli4uABAANQAMc,"],
                },
                "target": "search",
            }
        }
        cache_exist = {"debug": {"cache": NotEmpty()}}

        cached_items_before = self.__get_cached_items()

        # &only-cached=1 но запрос [redirect cache from cache] еще не закеширован - ответ будет пустым
        response = self.report.request_json(
            'place=prime&text=redirect+cache+from+cache&rids=213&pp=18&cvredirect=1&reqid=2570922881624645574'
            '&rearr-factors=market_redirect_cache_ttl=1&only-cached=1'
        )
        self.assertEqual(str(response), "{}")
        self.assertEqual(cached_items_before, self.__get_cached_items())

        # задаем запрос - он закешируется
        response = self.report.request_json(
            'place=prime&text=redirect+cache+from+cache&rids=213&pp=18&cvredirect=1&reqid=4b65c0628cf54a4ec60f551b86dc0500'
            '&rearr-factors=market_redirect_cache_ttl=1;market_some_other_rearr=1;market_report_blender_premium_ios_text_redirect=0'
        )
        self.assertFragmentIn(response, redirect)
        self.assertFragmentNotIn(response, cache_exist)
        self.assertEqual(cached_items_before + 1, self.__get_cached_items())

        # &only-cached=1 возвращает закешированный редирект
        response = self.report.request_json(
            'place=prime&text=redirect+cache+from+cache&rids=213&pp=18&cvredirect=1&reqid=06c8dec590610ddb94020e2236df0500'
            '&rearr-factors=market_redirect_cache_ttl=1&only-cached=1'
        )
        self.assertFragmentIn(response, redirect)
        self.assertFragmentIn(response, cache_exist)


if __name__ == '__main__':
    main()
