#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import Model, Offer, Region, Shop
from core.testcase import TestCase, main
from core.matcher import Contains, NoKey

import json


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.offers += [Offer(title="kijanka")]
        cls.index.regiontree += [
            # Регионы по-умолчанию приматчены к России
            Region(rid=213, name='Москва'),
            Region(rid=2, name='Санкт-Петербург'),
            Region(rid=111, name='Сухой лог'),
            Region(rid=127, name='Швеция', region_type=Region.COUNTRY, children=[Region(rid=10519, name='Стокгольм')]),
            Region(rid=187, name='Украина', region_type=Region.COUNTRY, children=[Region(rid=143, name='Киев')]),
        ]
        cls.reqwizard.on_default_request().respond()
        cls.reqwizard.on_request('kijanka v moskve').respond(found_cities=[213])
        cls.reqwizard.on_request('kijanka v pitere').respond(found_cities=[2])
        cls.reqwizard.on_request('kijanka v kieve').respond(found_cities=[143])
        cls.reqwizard.on_request('kijanka v stokholme').respond(found_cities=[10519])

        cls.index.shops += [
            Shop(fesh=234, priority_region=2),
            Shop(fesh=235, priority_region=2),
            Shop(fesh=236, priority_region=2),
            Shop(fesh=237, priority_region=2),
            Shop(fesh=238, priority_region=2),
        ]

        cls.index.models += [
            Model(title="shina nokian", hyperid=333, hid=33),
        ]

        cls.index.offers += [
            Offer(title="shina", fesh=234, hyperid=333),
            Offer(title="shina", fesh=235, hyperid=333),
            Offer(title="shina", fesh=236, hyperid=333),
            Offer(title="shina", fesh=237, hyperid=333),
            Offer(title="shina", fesh=238, hyperid=333),
        ]

        '''
            MARKETOUT-11076 Не делать региональный редирект для однословных запросов
            Для реквизарда создаются пары запросов (только регион и регион + какое-то слово), возвращающие коды региона:
             1. с одним регионом moskva (213)
             2. с составным регионом sukhoy log (111)
             3. с двумя регионами moskva piter (213, 2)

             non_region_query - поле в результате реквизарда, в котором возвращаются все слова, которые не относятся к регионам.
             Если все слова относятся к регионам, то они все возвращаются в этом поле (такая логика реквизарда).
        '''
        cls.reqwizard.on_request('moskva').respond(found_cities=[213], non_region_query='moskva')
        cls.reqwizard.on_request('moskva 2016').respond(found_cities=[213], non_region_query='2016')

        cls.reqwizard.on_request('sukhoy log').respond(found_cities=[111], non_region_query='sukhoy log')
        cls.reqwizard.on_request('sukhoy log wall').respond(found_cities=[111], non_region_query='wall')

        cls.reqwizard.on_request('moskva piter').respond(found_cities=[213, 2], non_region_query='moskva piter')
        cls.reqwizard.on_request('moskva piter summit').respond(found_cities=[213, 2], non_region_query='summit')

        # Запрос с верхним регистром - реквизард возвращает в нижнем.
        cls.reqwizard.on_request('Moskva').respond(found_cities=[213], non_region_query='moskva')
        cls.reqwizard.on_request('Moskva 2016').respond(found_cities=[213], non_region_query='2016')

        # После вырезания региона остается очень короткий запрос
        cls.reqwizard.on_request('moskva 7').respond(found_cities=[213], non_region_query='7')

        # Город с дефисом
        cls.reqwizard.on_request('Санкт-Петербург').respond(found_cities=[2], non_region_query='санкт-петербург')
        cls.reqwizard.on_request('Санкт-Петербург 2016').respond(found_cities=[2], non_region_query='2016')
        cls.reqwizard.on_request('Санкт Петербург').respond(found_cities=[2], non_region_query='санкт петербург')
        cls.reqwizard.on_request('Санкт Петербург 2016').respond(found_cities=[2], non_region_query='2016')

        cls.settings.ignore_qtree_decoding_failed_in_error_log = True

    def test_request_region(self):
        response = self.report.request_json('place=prime&rids=213&text=kijanka+v+pitere&cvredirect=1')
        self.assertFragmentIn(response, {"redirect": {"params": {"rt": ["13"], "lr": ["2"]}}})

        self.error_log.ignore(code=3021)

    def test_no_request_region_with_no_redirect(self):
        """Не происходит редирект если выставлен cvredirect=0"""
        response = self.report.request_json('place=prime&rids=213&text=kijanka+v+pitere&cvredirect=0')
        self.assertFragmentNotIn(response, {"redirect": {}})

        self.error_log.ignore(code=3021)

    def test_no_request_region_with_no_redirect_2(self):
        """Не происходит редирект если регион в запросе совпадает с регионом пользователя
        UPD: такой редирект возможен если отдается редирект из кеша
        """
        response = self.report.request_json('place=prime&rids=213&text=kijanka+v+moskve&cvredirect=1')
        self.assertFragmentNotIn(response, {"redirect": {}})

        self.error_log.ignore(code=3021)

    def test_request_region_foreign_country(self):
        """
        MARKETOUT-10804
        Временный костыль: проверяем, что регион в запросе из страны присутствия Маркета, иначе нет редиректа
        Будет удален в MARKETOUT-10900
        """
        prime_queries = [
            'place=prime&rids=213&text=kijanka+v+stokholme&cvredirect=0',
            'place=prime&rids=213&text=kijanka+v+stokholme&cvredirect=1',
        ]

        for query in prime_queries:
            response = self.report.request_json(query)
            self.assertFragmentNotIn(response, {"redirect": {}})

        self.error_log.ignore(code=3021)

    def test_request_region_foreign_country_ukraine(self):
        """
        MARKETOUT-10804
        Будет удален в MARKETOUT-10900
        Отдельно убедимся, что редирект будет не только в российских городах
        """
        response = self.report.request_json('place=prime&rids=213&text=kijanka+v+kieve&cvredirect=1')
        self.assertFragmentIn(response, {"redirect": {"params": {"lr": ["143"]}}})

        self.error_log.ignore(code=3021)

    def test_wizards_request_region_incut(self, not_fail_when_qtree4market_is_absent_exp=False):
        piter_markup = {
            "Market": {
                "FoundCities": [2],
            }
        }

        if not not_fail_when_qtree4market_is_absent_exp:
            piter_markup["Market"]["qtree4market"] = ""

        rearr = ''
        if not_fail_when_qtree4market_is_absent_exp:
            rearr = 'fail_when_qtree4market_is_absent=0'

        # запрос из Москвы с найденным регионом Санкт-Петербург
        # определение региона в запросе включено по умолчанию
        response = self.report.request_bs(
            'place=parallel&text=shina+nokian&rids=213'
            '&rearr-factors=market_parallel_wizard=1;{1}'
            '&wizard-rules={0}'.format(json.dumps(piter_markup), rearr)
        )
        # проверяем наличие врезки с офферами из Санкт-Петербурга
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "url": Contains("lr=2"),
                    }
                ]
            },
        )

        self.error_log.ignore(code=3021)

    def test_wizards_request_region_incut__fail_when_qtree4market_is_absent_exp(self):
        '''Проверяем действите обратного эксперимента fail_when_qtree4market_is_absent=0 см. MARKETOUT-17550'''
        return self.test_wizards_request_region_incut(not_fail_when_qtree4market_is_absent_exp=True)

    ''' Вспомогательный метод для тестирования редиректа по регионам: '''

    def region_redirect_helper(self, request1, request2, result_region):
        '''Тест запроса, состоящего только из региона для place=prime.'''
        response = self.report.request_json('place=prime&text={0}&rids=143&cvredirect=1'.format(request1))
        self.assertFragmentIn(response, {"redirect": {"params": {"lr": [result_region]}}})

        ''' Тест запроса, состоящего из региона и какого-то слова для place=prime. Должен быть редирект на заданный регион '''
        response = self.report.request_json('place=prime&text={0}&rids=143&cvredirect=1'.format(request2))
        self.assertFragmentIn(
            response,
            {"redirect": {"params": {"lr": [result_region], "cvredirect": NoKey("cvredirect")}}},
        )

    def test_reqwizard_request_single_word_region(self):
        '''
        MARKETOUT-11076 Не делать региональный редирект для однословных запросов

        Для одиночного слова moskva не будет редиректа на регион
        Для запроса из двух слов moskva 2016 будет выведен редирект на регион Москва (213)
        '''
        self.region_redirect_helper('moskva', 'moskva+2016', "213")

    def test_reqwizard_request_two_words_region(self):
        '''
        MARKETOUT-11076 Не делать региональный редирект для однословных запросов

        Для составного региона sukhoy log не будет редиректа на регион
        Для запроса, содержащего дополнительное слово sukhoy log wall будет выведен редирект на регион sukhoy log (111)
        '''
        self.region_redirect_helper('sukhoy+log', 'sukhoy+log+wall', "111")

    def test_reqwizard_request_two_regions(self):
        '''
        MARKETOUT-11076 Не делать региональный редирект для однословных запросов (запросов, содержащих только регионы)

        Для запроса, содержащего два региона moskva и piter не будет редиректа на регион
        Для запроса, содержащего дополнительное слово summit будет выведен редирект на регион piter (2)
        '''
        self.region_redirect_helper('moskva+piter', 'moskva+piter+summit', "2")

    def test_reqwizard_request_parallel(self):
        '''Тест запроса, состоящего из региона и какого-то слова для place=mainreport. Должен быть редирект на заданный регион'''
        # запрос из Москвы с найденным регионом Санкт-Петербург
        # определение региона в запросе включено по умолчанию
        _ = self.report.request_bs('place=parallel&text=moskva+piter&rids=213&rearr-factors=market_parallel_wizard=1')

    def test_reqwizard_request_upper_case_region(self):
        """Проверяем отсутствие редиректа в регион
        по региональному имени с верхним регистром.
        https://st.yandex-team.ru/MARKETOUT-16063
        """
        self.region_redirect_helper('Moskva', 'Moskva+2016', "213")
        self.region_redirect_helper('Санкт-Петербург', 'Санкт-Петербург+2016', "2")
        self.region_redirect_helper('Санкт+Петербург', 'Санкт+Петербург+2016', "2")

    def test_reqwizard_request_short_query_with_region(self):
        """Проверяем отсутствие редиректа в регион
        по региональному имени и слишком короткому остатку (1 символ) - иначе "банан".
        https://st.yandex-team.ru/MARKETOUT-16063
        """
        response = self.report.request_json('place=prime&text=moskva+7&rids=143&cvredirect=1')
        self.assertFragmentNotIn(
            response,
            {
                "redirect": {
                    "params": {
                        "lr": [],
                    }
                }
            },
        )

    def test_region_choice_logic_parallel(self):
        """
        https://st.yandex-team.ru/MARKETOUT-34439

        Проверяем, что берется последний регион из ответа req
        """
        region_markup = {
            "Market": {
                "FoundCities": [2, 111, 143],
            }
        }
        region_markup["Market"]["qtree4market"] = ""

        # Проверяем, что берется последний регион из "FoundCities"
        response = self.report.request_bs(
            'place=parallel&text=shina+nokian&rids=213'
            '&rearr-factors=market_parallel_wizard=1'
            '&wizard-rules={0}'.format(json.dumps(region_markup))
        )
        print(
            'place=parallel&text=shina+nokian&rids=213'
            '&rearr-factors=market_parallel_wizard=1'
            '&wizard-rules={0}'.format(json.dumps(region_markup))
        )

        # Проверяем наличие врезки с офферами из Киева (rid = 143)
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "url": Contains("lr=143"),
                    }
                ]
            },
        )

        # Проверяем для другого порядка регионов
        region_markup["Market"]["FoundCities"] = [2, 143, 111]

        response = self.report.request_bs(
            'place=parallel&text=shina+nokian&rids=213'
            '&rearr-factors=market_parallel_wizard=1'
            '&wizard-rules={0}'.format(json.dumps(region_markup))
        )

        # Проверяем наличие врезки с офферами из города Сухой Лог (rid = 111)
        self.assertFragmentIn(
            response,
            {
                "market_model": [
                    {
                        "url": Contains("lr=111"),
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
