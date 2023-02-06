#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    HyperCategory,
    HyperCategoryType,
    NavCategory,
    Region,
    Shop,
    GLType,
    GLParam,
    Offer,
    RedirectWhiteListRecord,
    FormalizedParam,
)
from core.testcase import TestCase, main
from core.matcher import LikeUrl, NoKey


class T(TestCase):
    """Тестируем как работает редирект со сменой региона
    Например если пользователь находится в Питере и пишет запрос [купить джинсы в Москве]
    То редирект должен содержать параметр lr=213 чтобы сменить регион пользователя
    Редирект может вести на search (мультикатегорийную выдачу) c cvredirect=0
    Или на категорию, или на категорию с фильтрами и т.п.

    Т.к. редиректы предполагается кешировать то редирект не может содержать cvredirect=1 (или 2)
    иначе если пользователь находится в Питере и пишет запрос [купить джинсы в Москве]
    и получает редирект с lr=213 и cvredirect=1 то далее делая повторный запрос из Москвы уже
    он снова получит точно такой же ответ с lr=213 и cvredirect=1
    что приведет к бесконечному редиректу

    """

    @classmethod
    def prepare(cls):

        cls.index.hypertree += [
            HyperCategory(hid=100, output_type=HyperCategoryType.GURU, visual=False, name="Что есть в Армении"),
            HyperCategory(hid=200, output_type=HyperCategoryType.GURU, visual=False, name="Что можно есть в Армении"),
        ]
        cls.index.navtree += [
            NavCategory(nid=100, hid=100),
            NavCategory(nid=200, hid=200),
        ]

        cls.index.regiontree += [
            Region(rid=213, name='Москва'),
            Region(rid=10262, name='Ереван'),
            Region(rid=3, name='Греция'),  # тут всё есть
        ]

        cls.index.shops += [
            Shop(fesh=10, regions=[213, 3], cpa=Shop.CPA_REAL),
            Shop(fesh=20, regions=[10262, 3], cpa=Shop.CPA_REAL),
        ]

        cls.index.gltypes += [
            GLType(param_id=1001, hid=100, gltype=GLType.BOOL),
        ]

        cls.index.offers += [
            Offer(fesh=10, hid=100, title='В России улитка, а в Армении хохуч', cpa=Offer.CPA_REAL),
            Offer(fesh=10, hid=100, title='В России кошка, а в Армении коту', cpa=Offer.CPA_REAL),
            Offer(
                fesh=10,
                hid=100,
                title='В Москве - дорого, а в Армении есть ресторан Таверна Еревана. Нас там бесплатно покормили :)',
                cpa=Offer.CPA_REAL,
                glparams=[GLParam(param_id=1001, value=1)],
            ),
            Offer(fesh=20, hid=200, title='В Армении есть драмы, а в Москве нет', cpa=Offer.CPA_REAL),
            Offer(fesh=20, hid=200, title='В Арменнии можно есть клубнику', cpa=Offer.CPA_REAL),
            Offer(fesh=20, hid=200, title='В Арменнии можно есть толму', cpa=Offer.CPA_REAL),
        ]

        cls.reqwizard.on_request('что есть в Ереване').respond(found_cities=[10262], non_region_query='что есть')
        cls.reqwizard.on_request('где есть в Москве').respond(found_cities=[213], non_region_query='где есть')
        cls.reqwizard.on_request('что есть в Греции').respond(found_cities=[3], non_region_query='что есть')
        cls.reqwizard.on_request('что можно съесть в Греции').respond(
            found_cities=[3],
            non_region_query='что можно съесть',
            qtree='cHic1VS_ixNREJ552YTncy8sF8S4cJgLFosgBKtgE7nqsNDjGo9gcQSRlJLquMaYQ-5X9Kws1MpDsVFyp4GIl4DYiCC8B9Y2Fv4Lls57-7KbjVGUOwtThNn58c3MN7ufuCRcPuVl81D'
            'AgJVgGnwowlk4DxdcDh6QHwIowcX0fHoBlmAZ6ngf4SHCE4R9hD4C_T4gSLzuP2LiqgjLOJVpuIxaVy058LEQoaZGUGEeNGq9_Z0PUW3FGHYJys5cFzl64NuMIgRYwoVdtoTLrI6NWW'
            'EDeY2NZbaIa1WoZagCglwdVrBxOUpBm-LcffnuYxWfV_FpzfWYn5Fd-UoOglSN0xOTB8YCn6k1soSX8h1db_EmtGwnW64yjk0EWs5_dlzcGGPHVbfVluzSf0u1NUdYsCRlJpD0-MGXF'
            '2xIU6L0Z7Jwbk8YshJ5MWXmkI2vx0Q2jMvXsqMzzCJQhkXcrLK997S7Qyzs0-5ZAuNqQ3ZG-VE7ZE3irWssJOtNZCW5DOvvRRW9YfT3tTZP194ZRs0UgnyOWjOILk2d0Sv9d5MLmtyh'
            'ieKqjvECecenD7sPJsX_aovR_qF3-5AbcfpOmGolUMIuG6o9jBtEMzl9Qd3I-zaxedixF1mDCKllkeJbgO2ZpUyuNv_BvU1n2TuCO5vvKvbSTP3E3mYH2f_lDoMjuXZobx_mynYKtG-'
            'NUUhi6GDstkPljO0d-3782c1jNRbkc7ScRfu1g5zFDN-ukb5Gpc-IhAjmtQhqiYuFmq9ALNTFKX7La2KeFVJBuqTVUAe28LRwtHhPn-To5_ipE592b1ZmAJqVApzT2htmMI9FGbPXrlRmPn9bjTNcm-HwbEP3-gGBWuLs',
        )

        cls.index.redirect_whitelist_records += [
            RedirectWhiteListRecord(query='что съесть', url='/catalog/200/list?hid=200&suggest=1'),
        ]

        cls.formalizer.on_default_request().respond()
        cls.formalizer.on_request(hid=100, query="где есть в Москве").respond(
            formalized_params=[FormalizedParam(param_id=1001, value=1, is_numeric=False, value_positions=(0, 8))]
        )

    def test_region_redirect(self):
        """Редирект только со сменой региона
        В данном примере после смены региона мы получаем мультикатегорийную поисковую выдачу
        повторного редиректа не происходит
        """

        # редирект в категорию не произойдет из-за большого порога
        r = '&rearr-factors=market_category_redirect_treshold=100;market_single_redirect=0'

        # запрос [что есть в Греции] в Москве rids=213 отдает редирект со сменой региона и с cvredirect=2
        # чтобы мы дальше сделали повторный редирект если это возможно
        response = self.report.request_json('place=prime&text=что есть в Греции&rids=213&cvredirect=1' + r)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["13"],
                        "text": ["что есть в Греции"],
                        "lr": ["3"],
                        "cvredirect": ["2"],
                        "hid": NoKey("hid"),
                    },
                    "target": "search",
                }
            },
        )

        # запрос [что есть в Греции] в Греции rids=3 отдает поисковую выдачу а не редирект
        # т.к. регион пользователя совпадает с регионом запроса (в новой логике это меняется)
        response = self.report.request_json('place=prime&text=что есть в Греции&rids=3&cvredirect=1' + r)
        self.assertFragmentIn(response, {'search': {'total': 6}})

        # после смены региона мы увидим поисковую выдачу
        response = self.report.request_json('place=prime&text=что есть в Греции&rids=3&cvredirect=1' + r)
        self.assertFragmentIn(response, {'search': {'total': 6}})

    def test_region_redirect_single_redirect(self):
        """Редирект только со сменой региона"""

        # редирект в категорию не произойдет из-за большого порога
        r = '&rearr-factors=market_category_redirect_treshold=100;market_single_redirect=1'

        redirect = {
            "redirect": {
                "params": {
                    "was_redir": ["1"],
                    "rt": ["13"],
                    "text": ["что есть в Греции"],
                    "lr": ["3"],
                    "cvredirect": NoKey(
                        "cvredirect"
                    ),  # не возвращаем cvredirect т.к. мы уже знаем что будет поисковая выдача
                    "hid": NoKey("hid"),
                },
                "target": "search",
            }
        }

        # запрос [что есть в Греции] в Москве rids=213 отдает редирект со сменой региона но без cvredirect=2
        # (т.к. мы уже знаем что конечный результат - поисковая выдача)
        response = self.report.request_json('place=prime&text=что есть в Греции&rids=213&cvredirect=1' + r)
        self.assertFragmentIn(response, redirect)

        # запрос [что есть в Греции] в Греции rids=3 отдает поисковую выдачу а не редирект (без кеша)
        # т.к. регион пользователя совпадает с регионом запроса
        # однако из-за региононезависимого кеширования точно такой же ответ как в москве может быть отдан из кеша и для запроса из Греции
        # но это ничего не сломает, т.к. "вечный" редирект невозможен потому что в ответе был cvredirect=0
        response = self.report.request_json('place=prime&text=что есть в Греции&rids=3&cvredirect=1' + r)
        self.assertFragmentIn(response, {'search': {'total': 6}})

        # после смены региона мы увидим поисковую выдачу т.к. редирект ведет на cvredirect=0
        response = self.report.request_json('place=prime&text=что есть в Греции&rids=3' + r)
        self.assertFragmentIn(response, {'search': {'total': 6}})

    def test_region_redirect_with_white_list(self):
        """Редирект через вайт-лист [что съесть]"""

        r = '&rearr-factors=market_single_redirect=0'

        # первый запрос отдает редирект на смену региона
        response = self.report.request_json('place=prime&text=что можно съесть в Греции&rids=213&cvredirect=1' + r)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "was_redir": ["1"],
                        "rt": ["13"],
                        "text": ["что можно съесть в Греции"],
                        "lr": ["3"],
                        "cvredirect": ["2"],  # означает что надо сходить еще раз за редиректом
                    },
                    "target": "search",
                }
            },
        )

        # повторный запрос отдает еще один редирект на вайт лист
        response = self.report.request_json('place=prime&text=что можно съесть в Греции&rids=3&cvredirect=1' + r)
        self.assertFragmentIn(
            response, {'redirect': {'url': LikeUrl.of('/catalog/200/list?hid=200&suggest=1&was_redir=1&rt=10&nid=200')}}
        )

    def test_region_redirect_with_white_list_single_redirect(self):
        """Редирект через вайт-лист [что съесть]"""

        r = '&rearr-factors=market_single_redirect=1'

        # запрос сразу отдает отдает редирект по вайтлисту с параметром lr=3
        response = self.report.request_json('place=prime&text=что можно съесть в Греции&rids=213&cvredirect=1' + r)
        self.assertFragmentIn(
            response,
            {'redirect': {'url': LikeUrl.of('/catalog/200/list?hid=200&suggest=1&was_redir=1&rt=10&lr=3&nid=200')}},
        )

    def test_region_redirect_with_category(self):
        """Редирект в категорию (выполняется после поиска)"""

        r = '&rearr-factors=market_single_redirect=0'

        # сначала поиск отдает редирект на новый регион
        response = self.report.request_json('place=prime&text=что есть в Ереване&rids=213&cvredirect=1' + r)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "rt": ["13"],
                        "text": ["что есть в Ереване"],
                        "lr": ["10262"],
                        "cvredirect": ["2"],
                        "hid": NoKey("hid"),
                    },
                    "target": "search",
                }
            },
            allow_different_len=False,
        )

        # повторный запрос с другим регионом возвращает редирект на категорию
        response = self.report.request_json('place=prime&text=что есть в Ереване&rids=10262&cvredirect=1' + r)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "rt": ["9"],
                        "text": ["что есть в Ереване"],
                        "hid": ["200"],
                        "cvredirect": NoKey("cvredirect"),
                        "lr": NoKey("lr"),
                    }
                }
            },
        )

    def test_region_redirect_with_category_single_redirect(self):
        """Редирект в категорию (выполняется после поиска)"""

        # при поиске в любом регионе мы получим одинаковый результат
        # редирект на категорию + параметр lr=10262
        # (результат должен быть одинаковым для разных регионов и он будет закеширован если включить кеш)
        redirect = {
            "redirect": {
                "params": {
                    "rt": ["9"],
                    "text": ["что есть в Ереване"],
                    "hid": ["200"],
                    "cvredirect": NoKey("cvredirect"),  # повторный редирект не нужен
                    "lr": ["10262"],
                }
            }
        }

        r = '&rearr-factors=market_single_redirect=1'

        # если бы мы искали в Ереване то получили бы редирект в категорию
        response = self.report.request_json('place=prime&text=что есть в Ереване&rids=10262&cvredirect=1' + r)
        self.assertFragmentIn(response, redirect)

        # Поиск из региона rids=213 возвращает тот же самый редирект
        response = self.report.request_json('place=prime&text=что есть в Ереване&rids=213&cvredirect=1' + r)
        self.assertFragmentIn(response, redirect)

    def test_region_redirect_with_parametric(self):
        # Пользватель в Греции спрашивает выдачу в Мск - получает редирект с lr=213 и cvredirect=2

        r = '&rearr-factors=market_single_redirect=0'

        response = self.report.request_json('place=prime&text=где есть в Москве&rids=3&cvredirect=1' + r)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {"rt": ["13"], "text": ["где есть в Москве"], "lr": ["213"], "cvredirect": ["2"]},
                    "target": "search",
                }
            },
        )

        # повторный запрос в Мск возвращает редирект на hid+glfilter
        response = self.report.request_json('place=prime&text=где есть в Москве&rids=213&cvredirect=1' + r)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "text": ["где есть в Москве"],
                        "hid": ["100"],
                        "glfilter": ["1001:1"],
                        "rt": ["11"],
                        "lr": NoKey("lr"),
                        "cvredirect": NoKey("cvredirect"),
                    },
                    "target": "search",
                }
            },
        )

    def test_region_redirect_with_parametric_single_redirect(self):
        """Параметрический редирект - так же как и категорийный отдает сразу редирект на категорию + фильтры + lr"""

        r = '&rearr-factors=market_single_redirect=1'

        redirect = {
            "redirect": {
                "params": {
                    "text": ["где есть в Москве"],
                    "hid": ["100"],
                    "glfilter": ["1001:1"],
                    "rt": ["11"],
                    "lr": ["213"],
                    "cvredirect": NoKey("cvredirect"),
                },
                "target": "search",
            }
        }
        # Пользватель в Греции спрашивает выдачу в Мск - получает редирект с lr=213 и cvredirect=0 (отсутствует)
        response = self.report.request_json('place=prime&text=где есть в Москве&rids=3&cvredirect=1' + r)
        self.assertFragmentIn(response, redirect)

        # Пользватель в Москве спрашивает выдачу в Мск - и также получает редирект с lr=213 и cvredirect=2
        response = self.report.request_json('place=prime&text=где есть в Москве&rids=213&cvredirect=1' + r)
        self.assertFragmentIn(response, redirect)


if __name__ == '__main__':
    main()
