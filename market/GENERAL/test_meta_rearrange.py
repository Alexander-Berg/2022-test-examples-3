#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from unittest import skip

from core.testcase import TestCase, main
from core.types import BlueOffer, Currency, MarketSku, MnPlace, Model, Offer, Picture, Region, Shop, Tax, Vat
from core.dj import DjModel
from core.matcher import NotEmpty, Contains


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        # RANDX randomizing is disabled because these tests don't work with it. See MARKETOUT-21319
        cls.disable_randx_randomize()
        cls.settings.set_default_reqid = False

    @classmethod
    def prepare_head_rearrange(cls):
        """
        MARKETOUT-10395
        подготовка данных для проверки переранжирования головной части выдачи на мете
        """

        # добавляем 8 офферов от 8 разных магазинов (в названии должно быть одно и то же слово)
        # выставляем им значение matrix net для базового поиска 8, 7 ... 1
        # и 1 ... 7, 8 для переранжирования на мете
        for i in range(1, 9):
            pic = Picture(width=100, height=100, group_id=1234)
            cls.index.offers += [
                Offer(
                    title='goods ' + str(i),
                    fesh=i,
                    ts=i,
                    picture=pic,
                    hid=123,
                    price=100 * i,
                    vendor_id=i,
                    cpa=Offer.CPA_REAL if i % 2 else Offer.CPA_NO,
                ),
            ]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, i).respond(9 - i)
            cls.matrixnet.on_place(MnPlace.META_REARRANGE, i).respond(i)

    def test_head_rearrange_MARKETOUT_29780(self):
        """
        MARKETOUT-10395
        подготовка данных для проверки переранжирования головной части выдачи на мете
        """

        # будем запрашивать 2 документа на страницу, чтобы уменьшить размер head
        # при таких запросах в head попадут документы от 5 магазинов
        # (удвоенное количество документов на странице плюс один).

        # в соответствии с релевантностью вычисленной на базовых
        # офферы должны были бы располагаться в порядке 1,2,3,4,5,6,7,8
        # офферы 1-5 попавшие в head будут переранжированы в соответствии
        # с формулой релевантности на мете, т.е. будут располагаться
        # в порядке 5,4,3,2,1

        # Вся выдача из 8 документов распадается на 4 страницы
        # и для полной проверки необходимо сделать 4 запроса
        # Делаем 4 запроса к place prime со словом из названия офферов, количеством документов равным 2
        # и флагом rearr-factors=market_meta_formula_type=TESTALGO_trivial
        # (флаг формулы может быть любым т.к. для теста значения замоканы),
        # котрые различаются только номером страницы

        # проверяем, что на прервой странице стоят 5 и 4 офферы (порядок важен)
        # первая половина переранижированной (развернутой) головы выдачи
        rearr = '&rearr-factors=market_new_cpm_iterator=0'
        request = 'place=prime&text=goods&numdoc=2' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "goods 5"}},
                {"titles": {"raw": "goods 4"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем, что на второй странице стоят 3 и 2 офферы (порядок важен)
        # вторая половина переранижированной (развернутой) головы выдачи
        request = 'place=prime&text=goods&numdoc=2&page=2' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "goods 3"}},
                {"titles": {"raw": "goods 2"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем, что на третьей странице стоят 1 и 6 офферы (порядок важен)
        # конец переранжированной выдачи (развернутой) и начало не тронутого хвоста выдачи
        request = 'place=prime&text=goods&numdoc=2&page=3' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "goods 1"}},
                {"titles": {"raw": "goods 6"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        # проверяем, что на третьей странице стоят 7 и 8 офферы (порядок важен)
        # конец не тронутого хвоста выдачи
        request = 'place=prime&text=goods&numdoc=2&page=4' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "goods 7"}},
                {"titles": {"raw": "goods 8"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_head_rearrange_textless_by_suggest_text_MARKETOUT_29780(self):
        """
        MARKETOUT-19263
        Проверяем что выдача правильно ранжируется на мете для бестекстового запроса по параметру suggest_text.
        """
        rearr_factors = 'market_textless_meta_formula_type=TESTALGO_trivial;market_new_cpm_iterator=0'
        request = (
            'place=prime&text=&suggest_text=goods&hid=123&numdoc=2&rearr-factors={rearr_factors}'
            '&page={page}'.format(rearr_factors=rearr_factors, page='{page}')
        )

        response = self.report.request_json(request.format(page=1))
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': 'goods 5'}},
                {'titles': {'raw': 'goods 4'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(request.format(page=2))
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': 'goods 3'}},
                {'titles': {'raw': 'goods 2'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(request.format(page=3))
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': 'goods 1'}},
                {'titles': {'raw': 'goods 6'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(request.format(page=4))
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': 'goods 7'}},
                {'titles': {'raw': 'goods 8'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_head_rearrange_textless_by_hid_MARKETOUT_29780(self):
        """
        MARKETOUT-19263
        Проверяем что выдача правильно ранжируется на мете для бестекстового запроса по параметру hid.
        """
        rearr_factors = 'market_textless_meta_formula_type=TESTALGO_trivial;market_new_cpm_iterator=0'
        request = (
            'place=prime&text=&suggest_text=&hid=123&numdoc=2&rearr-factors={rearr_factors}'
            '&page={page}'.format(rearr_factors=rearr_factors, page='{page}')
        )

        response = self.report.request_json(request.format(page=1))
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': 'goods 5'}},
                {'titles': {'raw': 'goods 4'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(request.format(page=2))
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': 'goods 3'}},
                {'titles': {'raw': 'goods 2'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(request.format(page=3))
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': 'goods 1'}},
                {'titles': {'raw': 'goods 6'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(request.format(page=4))
        self.assertFragmentIn(
            response,
            [
                {'titles': {'raw': 'goods 7'}},
                {'titles': {'raw': 'goods 8'}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_disable_rearrange(self):
        """
        MARKETOUT-17376
        Проверка выключения переранжирования через флаг обратного эксперимента
        """

        # по умолчанию переранжирование на мете включено:
        # документы идут в обратном, переранжированом порядке
        response = self.report.request_json('place=prime&text=goods')
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "goods 8"}},
                {"titles": {"raw": "goods 7"}},
                {"titles": {"raw": "goods 6"}},
                {"titles": {"raw": "goods 5"}},
                {"titles": {"raw": "goods 4"}},
                {"titles": {"raw": "goods 3"}},
                {"titles": {"raw": "goods 2"}},
                {"titles": {"raw": "goods 1"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        # при явном задании выключения через флаг, переранжирования нет:
        # документы идут в прямом, не переранжированном порядке
        response = self.report.request_json(
            'place=prime&text=goods' '&rearr-factors=' 'market_enable_meta_head_rearrange=0'
        )
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "goods 1"}},
                {"titles": {"raw": "goods 2"}},
                {"titles": {"raw": "goods 3"}},
                {"titles": {"raw": "goods 4"}},
                {"titles": {"raw": "goods 5"}},
                {"titles": {"raw": "goods 6"}},
                {"titles": {"raw": "goods 7"}},
                {"titles": {"raw": "goods 8"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_rearrange_for_content_api_requests(cls):
        """Проверка включения/выключения переранжирования при запросе от content api
        MARKETOUT-17376, MARKETOUT-18186
        """
        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.01)

        blue_shop = Shop(
            fesh=300,
            datafeed_id=4,
            priority_region=213,
            fulfillment_virtual=True,
            virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            currency=Currency.RUR,
            tax_system=Tax.OSN,
            supplier_type=Shop.FIRST_PARTY,
        )
        supplier_shop = Shop(
            fesh=200,
            datafeed_id=5,
            priority_region=213,
            currency=Currency.RUR,
            tax_system=Tax.OSN,
            supplier_type=Shop.FIRST_PARTY,
            blue=Shop.BLUE_REAL,
        )
        cls.index.shops += [blue_shop, supplier_shop]

        for i in range(11, 19):
            green_shop = Shop(fesh=400 + i, cpa=Shop.CPA_REAL)
            cls.index.shops += [green_shop]

            cls.index.models += [
                Model(hyperid=i, hid=1, title="api model " + str(i), ts=i),
            ]
            cls.index.offers += [
                Offer(title='api offer ' + str(i), hyperid=i, fesh=green_shop.fesh),
            ]
            cls.index.mskus += [
                MarketSku(
                    title='blue api offer ' + str(i),
                    hyperid=i,
                    sku=1100 + i,
                    ts=1100 + i,
                    blue_offers=[BlueOffer(ts=1200 + i, vat=Vat.NO_VAT, feedid=supplier_shop.datafeed_id)],
                ),
            ]
            # for models
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, i).respond(20 - i)
            cls.matrixnet.on_place(MnPlace.META_REARRANGE, i).respond(i)

            # for market skus
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1100 + i).respond(20 - i)
            cls.matrixnet.on_place(MnPlace.META_REARRANGE, 1100 + i).respond(i)

            # for blue offers
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1200 + i).respond(20 - i)
            cls.matrixnet.on_place(MnPlace.META_REARRANGE, 1200 + i).respond(i)

        cls.dj.on_request(yandexuid=258907, exp='dj_formula_1').respond(
            [
                DjModel(id=i)
                for i in sorted(range(11, 19), key=lambda i: (i % 2) * 100 + i)  # сначала четные потом нечетные
            ]
        )

    def test_rearrange_for_content_api_requests(self):
        """Проверка включения/выключения переранжирования при запросе от content api
        MARKETOUT-17376, MARKETOUT-18186
        Переранжирование выключено на запросах от виджетов и советника и включено на всех остальных
        """

        ordered_by_baseformula = [
            {"titles": {"raw": "api model 11"}},
            {"titles": {"raw": "api model 12"}},
            {"titles": {"raw": "api model 13"}},
            {"titles": {"raw": "api model 14"}},
            {"titles": {"raw": "api model 15"}},
            {"titles": {"raw": "api model 16"}},
            {"titles": {"raw": "api model 17"}},
            {"titles": {"raw": "api model 18"}},
        ]

        ordered_by_metaformula = [
            {"titles": {"raw": "api model 18"}},
            {"titles": {"raw": "api model 17"}},
            {"titles": {"raw": "api model 16"}},
            {"titles": {"raw": "api model 15"}},
            {"titles": {"raw": "api model 14"}},
            {"titles": {"raw": "api model 13"}},
            {"titles": {"raw": "api model 12"}},
            {"titles": {"raw": "api model 11"}},
        ]

        # при не синих запросах от виджета или советника переранжирование отключается
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&text=api&api=content&client=widget&debug=da'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, ordered_by_baseformula, preserve_order=True, allow_different_len=False)
        self.assertFragmentIn(
            response, {'logicTrace': [Contains('No rearrange on meta because: hard request from content api;')]}
        )
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&text=api&api=content&client=sovetnik&debug=da'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, ordered_by_baseformula, preserve_order=True, allow_different_len=False)
        self.assertFragmentIn(
            response, {'logicTrace': [Contains('No rearrange on meta because: hard request from content api;')]}
        )

        # для остальных клиентов контентного апи переранжирование на мете включено
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&text=api&api=content&debug=da' '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, ordered_by_metaformula, preserve_order=True, allow_different_len=False)

        # В синем маркете по-умолчанию переранжирование выключено. Используется plain итератор
        # Проверяем, что при запросе через API и напрямую ответ одинаковый.

        # запрос через API к синему
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&text=api&rgb=blue&api=content&client=widget&debug=da'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, ordered_by_baseformula, preserve_order=True, allow_different_len=False)
        self.assertFragmentIn(
            response, {'logicTrace': [Contains('No rearrange on meta because: hard request from content api;')]}
        )

        # запрос НЕ через API к синему
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&text=api&rgb=blue&debug=da' '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, ordered_by_baseformula, preserve_order=True, allow_different_len=False)
        self.assertFragmentIn(
            response, {'logicTrace': [Contains('No rearrange on meta because: algorithm was not selected;')]}
        )

    def test_no_rarrange_on_user_sortings(self):
        """При пользовательских сортировках нет переранжирования по мета-формуле"""

        rearr = '&rearr-factors=market_dj_exp_rearrange_on_prime=dj_formula_1'
        blue_rearr = '&rearr-factors=market_blue_meta_formula_type=TESTALGO_trivial'
        for sort in ['aprice', 'rorp', 'discount_p', 'quality', 'promo_quality', 'opinions']:
            for color in ['&rgb=blue' + blue_rearr, '&rgb=green']:
                response = self.report.request_json(
                    'place=prime&allow-collapsing=1&text=api&how={}&debug=da'.format(sort) + color + rearr
                )
                self.assertFragmentIn(
                    response, {'logicTrace': [Contains('No rearrange on meta because: request has user sorting;')]}
                )
                self.assertFragmentIn(response, {'trace': {'fullFormulaInfo': [{'tag': 'Default'}]}})
                self.assertFragmentNotIn(response, {'trace': {'fullFormulaInfo': [{'tag': 'Meta'}]}})

    def test_rearrange_for_blue_market(self):
        """Проверка переранжирования на синем маркете
        Переранжирование включается только при явном указании флага
        """

        ordered_by_baseformula = [
            {"titles": {"raw": "api model 11"}},
            {"titles": {"raw": "api model 12"}},
            {"titles": {"raw": "api model 13"}},
            {"titles": {"raw": "api model 14"}},
            {"titles": {"raw": "api model 15"}},
            {"titles": {"raw": "api model 16"}},
            {"titles": {"raw": "api model 17"}},
            {"titles": {"raw": "api model 18"}},
        ]

        ordered_by_metaformula = [
            {"titles": {"raw": "api model 18"}},
            {"titles": {"raw": "api model 17"}},
            {"titles": {"raw": "api model 16"}},
            {"titles": {"raw": "api model 15"}},
            {"titles": {"raw": "api model 14"}},
            {"titles": {"raw": "api model 13"}},
            {"titles": {"raw": "api model 12"}},
            {"titles": {"raw": "api model 11"}},
        ]

        # по-умолчанию в синем маркете переранжирование выключено
        response = self.report.request_json('place=prime&allow-collapsing=1&text=blue+offer&rgb=blue&debug=da')
        self.assertFragmentIn(response, ordered_by_baseformula, preserve_order=True, allow_different_len=False)
        self.assertFragmentIn(
            response, {'logicTrace': [Contains('No rearrange on meta because: algorithm was not selected;')]}
        )

        # явное указание флага market_blue_meta_formula_type включает переранжирование на мете
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&text=blue+offer&rgb=blue&debug=da'
            '&rearr-factors=market_blue_meta_formula_type=TESTALGO_trivial'
        )
        self.assertFragmentIn(response, ordered_by_metaformula, preserve_order=True, allow_different_len=False)
        self.assertFragmentIn(response, {'logicTrace': [Contains('Meta formula: TESTALGO_trivial')]})

    def test_rearrange_by_dj(self):
        """Проверка переранжирования бестекста dj-ем
        https://st.yandex-team.ru/MARKETOUT-40990
        """

        _ = [
            {"titles": {"raw": "api model 11"}},
            {"titles": {"raw": "api model 12"}},
            {"titles": {"raw": "api model 13"}},
            {"titles": {"raw": "api model 14"}},
            {"titles": {"raw": "api model 15"}},
            {"titles": {"raw": "api model 16"}},
            {"titles": {"raw": "api model 17"}},
            {"titles": {"raw": "api model 18"}},
        ]

        ordered_by_metaformula = [
            {"titles": {"raw": "api model 18"}},
            {"titles": {"raw": "api model 17"}},
            {"titles": {"raw": "api model 16"}},
            {"titles": {"raw": "api model 15"}},
            {"titles": {"raw": "api model 14"}},
            {"titles": {"raw": "api model 13"}},
            {"titles": {"raw": "api model 12"}},
            {"titles": {"raw": "api model 11"}},
        ]

        ordered_by_dj = [
            {"titles": {"raw": "api model 12"}},
            {"titles": {"raw": "api model 14"}},
            {"titles": {"raw": "api model 16"}},
            {"titles": {"raw": "api model 18"}},
            {"titles": {"raw": "api model 11"}},
            {"titles": {"raw": "api model 13"}},
            {"titles": {"raw": "api model 15"}},
            {"titles": {"raw": "api model 17"}},
        ]

        # вызываем dj на бестексте
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&hid=1&yandexuid=258907&debug=da'
            '&rearr-factors=market_dj_exp_rearrange_on_prime=dj_formula_1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, ordered_by_dj, preserve_order=True, allow_different_len=False)
        self.assertFragmentIn(response, {'logicTrace': [Contains('Meta formula for Dj: dj_formula_1')]})

        # на текстовом поиске dj не вызывается, переранжирование метаформулой
        response = self.report.request_json(
            'place=prime&allow-collapsing=1&text=api&yandexuid=258907&debug=da'
            '&rearr-factors=market_dj_exp_rearrange_on_prime=dj_formula_1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, ordered_by_metaformula, preserve_order=True, allow_different_len=False)
        self.assertFragmentNotIn(response, {'logicTrace': [Contains('Meta formula for Dj: dj_formula_1')]})
        self.assertFragmentIn(response, {'logicTrace': [Contains('Meta formula:')]})

    @classmethod
    def prepare_many_models(cls):
        for i in range(1, 150):
            hyperid = fesh = model_ts = 238800 + i
            offer1_ts = 248800 + i
            offer2_ts = 258800 + i
            shop = Shop(fesh=fesh + i, cpa=Shop.CPA_REAL)
            cls.index.shops += [shop]

            cls.index.models += [
                Model(hyperid=hyperid, hid=2388, title="lotalot model " + str(i), ts=model_ts),
            ]
            cls.index.offers += [
                Offer(title='lotalot offer of model ' + str(i), hid=2388, hyperid=hyperid, fesh=fesh, ts=offer1_ts),
                Offer(title='other lotalot offer', hid=2388, fesh=fesh, ts=offer2_ts),
            ]

            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, model_ts).respond(0.5 - i * 0.001)
            cls.matrixnet.on_place(MnPlace.META_REARRANGE, model_ts).respond(0.5 - i * 0.001)

            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, offer1_ts).respond(0.2 - i * 0.001)
            cls.matrixnet.on_place(MnPlace.META_REARRANGE, offer1_ts).respond(0.2 + i * 0.001)

            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, offer2_ts).respond(0.3 - i * 0.001)
            cls.matrixnet.on_place(MnPlace.META_REARRANGE, offer2_ts).respond(0.3 - i * 0.001)

    def test_market_min_models_to_request_MARKETOUT_29780(self):
        """Флаг market_min_models_to_request влияет на количество запрашиваемых моделей в группировке yg
        а также на количество моделей из этой группировки которые попадут в head в cpm-итераторе
        Если market_min_models_to_request не задан используется ограничение в 100 моделей в топе
        """

        # минимальное количество моделей в группировке yg определяется требованиями к обновлению статистик
        # TODO: надо это поправить - все равно мы дозапрашиваем все нужные модели для схлопывания
        response = self.report.request_json(
            'place=prime&text=lotalot&allow-collapsing=1&debug=da'
            '&rearr-factors=market_min_models_to_request=10;market_new_cpm_iterator=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {"g": ["1.hyper_ts.60.1.-1", "1.yg.1.60.-1", "1.ygg.1.60.-1", "4.dsrcid.20.1.120"]},
            allow_different_len=False,
        )

        logicTrace10 = {
            "logicTrace": [
                Contains(
                    "DocRangeHead market.yandex.ru/product/238801 g:yg: ModeId:238801 WareMd5:None Hid: 2388 Metadoc: None RESULT: ACCEPTED: 1"
                ),
                Contains(
                    "DocRangeHead market.yandex.ru/product/238802 g:yg: ModeId:238802 WareMd5:None Hid: 2388 Metadoc: None RESULT: ACCEPTED: 2"
                ),
                Contains(
                    "DocRangeHead market.yandex.ru/product/238803 g:yg: ModeId:238803 WareMd5:None Hid: 2388 Metadoc: None RESULT: ACCEPTED: 3"
                ),
                Contains(
                    "DocRangeHead market.yandex.ru/product/238804 g:yg: ModeId:238804 WareMd5:None Hid: 2388 Metadoc: None RESULT: ACCEPTED: 4"
                ),
                Contains(
                    "DocRangeHead market.yandex.ru/product/238805 g:yg: ModeId:238805 WareMd5:None Hid: 2388 Metadoc: None RESULT: ACCEPTED: 5"
                ),
                Contains(
                    "DocRangeHead market.yandex.ru/product/238806 g:yg: ModeId:238806 WareMd5:None Hid: 2388 Metadoc: None RESULT: ACCEPTED: 6"
                ),
                Contains(
                    "DocRangeHead market.yandex.ru/product/238807 g:yg: ModeId:238807 WareMd5:None Hid: 2388 Metadoc: None RESULT: ACCEPTED: 7"
                ),
                Contains(
                    "DocRangeHead market.yandex.ru/product/238808 g:yg: ModeId:238808 WareMd5:None Hid: 2388 Metadoc: None RESULT: ACCEPTED: 8"
                ),
                Contains(
                    "DocRangeHead market.yandex.ru/product/238809 g:yg: ModeId:238809 WareMd5:None Hid: 2388 Metadoc: None RESULT: ACCEPTED: 9"
                ),
                Contains(
                    "DocRangeHead market.yandex.ru/product/238810 g:yg: ModeId:238810 WareMd5:None Hid: 2388 Metadoc: None RESULT: ACCEPTED: 10"
                ),
                Contains("DocRangeNonLocalHead", "g:dsrcid", "ModeId:0", "RESULT: ACCEPTED: 11"),
            ]
        }

        # из группировки yg добавляются только 10 моделей в head остальные добираются потом из g:hyper_ts
        self.assertFragmentIn(response, logicTrace10, preserve_order=True)

        # на количество запрашиваемых моделей может влиять numdoc и page
        response = self.report.request_json(
            'place=prime&text=lotalot&allow-collapsing=1&debug=da'
            '&rearr-factors=market_min_models_to_request=10;market_new_cpm_iterator=0&numdoc=20&page=4'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {"g": ["1.hyper_ts.81.1.-1", "1.yg.1.81.-1", "1.ygg.1.81.-1", "4.dsrcid.40.1.120"]},
            allow_different_len=False,
        )

        # в head попадут все равно только первые 10 моделей
        self.assertFragmentIn(response, logicTrace10, preserve_order=True)

        # если market_min_models_to_request не задан используется ограничение в 100 моделей в топе
        response = self.report.request_json(
            'place=prime&text=lotalot&allow-collapsing=1&debug=da&numdoc=20&page=10'
            '&rearr-factors=market_new_cpm_iterator=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {"g": ["1.hyper_ts.201.1.-1", "1.yg.1.201.-1", "1.ygg.1.201.-1", "4.dsrcid.40.1.120"]},
            allow_different_len=False,
        )

        logicTrace100 = {
            "logicTrace": [
                Contains(
                    "DocRangeHead market.yandex.ru/product/238801 g:yg: ModeId:238801 WareMd5:None Hid: 2388 Metadoc: None RESULT: ACCEPTED: 1"
                ),
                Contains(
                    "DocRangeHead market.yandex.ru/product/238802 g:yg: ModeId:238802 WareMd5:None Hid: 2388 Metadoc: None RESULT: ACCEPTED: 2"
                ),
                Contains(
                    "DocRangeHead market.yandex.ru/product/238899 g:yg: ModeId:238899 WareMd5:None Hid: 2388 Metadoc: None RESULT: ACCEPTED: 99"
                ),
                Contains(
                    "DocRangeHead market.yandex.ru/product/238900 g:yg: ModeId:238900 WareMd5:None Hid: 2388 Metadoc: None RESULT: ACCEPTED: 100"
                ),
                Contains("DocRangeNonLocalHead", "g:dsrcid", "ModeId:0", "RESULT: ACCEPTED: 101"),
            ]
        }

        self.assertFragmentIn(response, logicTrace100, preserve_order=True)

    def test_reserve_pages(self):
        # minimumItemsToRequest=60
        response = self.report.request_json(
            'place=prime&text=lotalot&allow-collapsing=1&debug=da&numdoc=10&page=1'
            '&rearr-factors=market_min_models_to_request=0;market_new_cpm_iterator=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"g": ["1.hyper_ts.60.1.-1"]})

        # для page=5 все еще достаточно 60 документов
        response = self.report.request_json(
            'place=prime&text=lotalot&allow-collapsing=1&debug=da&numdoc=10&page=5'
            '&rearr-factors=market_min_models_to_request=0;market_new_cpm_iterator=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"g": ["1.hyper_ts.60.1.-1"]})

        # для page=6 нужен +1 документ, что бы понимать, существует ли следующая страница
        response = self.report.request_json(
            'place=prime&text=lotalot&allow-collapsing=1&debug=da&numdoc=10&page=6'
            '&rearr-factors=market_min_models_to_request=0;market_new_cpm_iterator=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"g": ["1.hyper_ts.61.1.-1"]})

        # для page=7 нужно уже 71 документов
        response = self.report.request_json(
            'place=prime&text=lotalot&allow-collapsing=1&debug=da&numdoc=10&page=7'
            '&rearr-factors=market_min_models_to_request=0;market_new_cpm_iterator=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"g": ["1.hyper_ts.71.1.-1"]})

        # для page=7 c how=aprice нужно уже 70+60+1 документов
        response = self.report.request_json(
            'place=prime&text=lotalot&allow-collapsing=1&debug=da&numdoc=10&page=7&how=aprice'
            '&rearr-factors=market_min_models_to_request=0;market_new_cpm_iterator=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(response, {"g": ["1.hyper_ts.131.1.-1"]})

        # и при этом все они попадают в ранжирование на мете
        logicTrace100 = {
            "logicTrace": [
                # при этом все 130 документов попадают в переранжирование на мете
                Contains("Apply doc :", "RESULT: ACCEPTED: 1"),
                Contains("Apply doc :", "RESULT: ACCEPTED: 2"),
                Contains("Apply doc :", "RESULT: ACCEPTED: 99"),
                Contains("Apply doc :", "RESULT: ACCEPTED: 130"),
                Contains("Apply doc :", "RESULT: ACCEPTED: 131"),
                # при этом ставки аукциона рассчитываются только для тех что отображаются (0-69)
                Contains("Flush doc :", "auctioned: 0 POSITION: 0"),
                Contains("Flush doc :", "auctioned: 1 POSITION: 69"),
                Contains("Flush doc :", "auctioned: -1 POSITION: 70"),  # уже не участвует в аукционе совсем
            ]
        }
        self.assertFragmentIn(response, logicTrace100, preserve_order=True, allow_different_len=True)

    @classmethod
    def prepare_head_rearrange_and_autobroker(cls):
        """
        MARKETOUT-10395
        подготовка данных для проверки работы автоброкера при переранжировании головной части выдачи на мете
        """

        # добавляем 4 оффера от 4х разных магазинов в регионе 213,
        # выставляем им значение matrix net для базового поиска 1.0, 0.9, 0.8, 0.7 и
        # 0.7, 0.8, 0.9, 1.0, для переранижирования на мете
        # и ставки 1000, 2000, 3000 и 4000
        for i in range(1, 5):
            id_ = 100 + i
            cls.index.offers += [Offer(title='asset ' + str(i), fesh=id_, bid=1000 * i, ts=id_)]
            cls.index.shops += [Shop(fesh=id_, priority_region=213)]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, id_).respond(1.0 - (i - 1) * 0.1)
            cls.matrixnet.on_place(MnPlace.META_REARRANGE, id_).respond(0.6 + 0.1 * i)

        # Добавляем оферы в tail без значений meta
        cls.index.shops += [Shop(fesh=105, priority_region=213)]
        # head
        cls.index.offers += [Offer(title='asset 5', fesh=105, bid=1000 * 5, ts=105)]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 105).respond(0.85)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 105).respond(0.85)
        # tail
        for i in range(6, 9):
            id_ = 100 + i
            cls.index.offers += [Offer(title='asset ' + str(i), fesh=105, bid=1000 * i, ts=id_)]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, id_).respond(0.8 - 0.01 * i)
            cls.matrixnet.on_place(MnPlace.META_REARRANGE, id_).respond(0.9 + 0.01 * i)

    def test_head_rearrange_and_autobroker_MARKETOUT_29780(self):
        """
        MARKETOUT-10395
        проверка работы автоброкера при переранжировании головной части выдачи на мете
        """

        # проверяем, что по запросу к place prime в регионе 213 с флагами
        # show-urls=external и rearr-factors=market_meta_formula_type=TESTALGO_trivial
        # в выдаче оферы идут в следующем порядке: 4, 3, 5, 2, 1, 6, 7, 8 (по значению меты)
        # а в show_log есть записи о цене кликов

        request = (
            'place=prime&rids=213&text=asset&show-urls=external'
            '&rearr-factors=market_meta_formula_type=TESTALGO_trivial;market_new_cpm_iterator=0'
        )
        response = self.report.request_json(request + '&numdoc=10')
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "asset 4"}},  # 1.0
                {"titles": {"raw": "asset 3"}},  # 0.9
                {"titles": {"raw": "asset 5"}},  # 0.85
                {"titles": {"raw": "asset 2"}},  # 0.8
                {"titles": {"raw": "asset 1"}},  # 0.7
                {"titles": {"raw": "asset 6"}},  # tail: 0.74 (значение взялось с базовой формулы)
                {"titles": {"raw": "asset 7"}},  # tail: 0.73 (значение взялось с базовой формулы)
                {"titles": {"raw": "asset 8"}},  # tail: 0.72 (значение взялось с базовой формулы)
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        self.show_log.expect(title='asset 4', click_price=219)
        self.show_log.expect(title='asset 3', click_price=291)
        self.show_log.expect(title='asset 5', click_price=284)
        self.show_log.expect(title='asset 2', click_price=189)
        self.show_log.expect(title='asset 1', click_price=1)
        self.show_log.expect(title='asset 6', click_price=443)
        self.show_log.expect(title='asset 7', click_price=442)
        self.show_log.expect(title='asset 8', click_price=1)

    def test_head_rearrange_and_autobroker_tail_doc_MARKETOUT_29780(self):
        """
        MARKETOUT-17531
        Проверяем, что для последнего документа на странице
        правильно считается ставка, а не подставляется bid.
        """
        request = (
            'place=prime&rids=213&text=asset&show-urls=external'
            '&rearr-factors=market_meta_formula_type=TESTALGO_trivial;market_new_cpm_iterator=0'
        )
        # click_price у документов не меняется при изменении количества документов
        response = self.report.request_json(request + '&numdoc=2')
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "asset 4"}},
                {"titles": {"raw": "asset 3"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )
        response = self.report.request_json(request + '&numdoc=3')
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "asset 4"}},
                {"titles": {"raw": "asset 3"}},
                {"titles": {"raw": "asset 5"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )
        response = self.report.request_json(request + '&numdoc=8')
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "asset 4"}},
                {"titles": {"raw": "asset 3"}},
                {"titles": {"raw": "asset 5"}},
                {"titles": {"raw": "asset 2"}},
                {"titles": {"raw": "asset 1"}},
                {"titles": {"raw": "asset 6"}},
                {"titles": {"raw": "asset 7"}},
                {"titles": {"raw": "asset 8"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        self.show_log.expect(title='asset 4', click_price=219).times(3)
        self.show_log.expect(title='asset 3', click_price=291).times(3)
        self.show_log.expect(title='asset 5', click_price=284).times(2)
        self.show_log.expect(title='asset 2', click_price=189).once()
        self.show_log.expect(title='asset 1', click_price=1).once()
        self.show_log.expect(title='asset 6', click_price=443).once()
        self.show_log.expect(title='asset 7', click_price=442).once()
        self.show_log.expect(title='asset 8', click_price=1).once()

    @classmethod
    def prepare_head_rearrange_with_delivery(cls):
        """
        MARKETOUT-12708
        Подготовка данных для теста переранижирования на мете с учетом доставки
        Нужно создать три магазина, два из одного региона, третий из другого региона
        И 4 оффера два от первого магазина с обязательно с одной картинкой,
        и по одному офферу для двух оставшихся магазинов.
        У всех офферов в названии должно быть одно и то же слово (delivery)
        """
        cls.index.regiontree += [Region(rid=300), Region(rid=301)]
        cls.index.shops += [
            Shop(fesh=310, priority_region=300, regions=[300, 301]),
            Shop(fesh=311, priority_region=300, regions=[300, 301]),
            Shop(fesh=312, priority_region=301, regions=[301, 300]),
        ]

        picture = Picture(width=100, height=100, group_id=1234)

        cls.index.offers += [
            Offer(title='delivery 1', ts=320, fesh=310, picture=picture),
            Offer(title='delivery 2', ts=321, fesh=310, picture=picture),
            Offer(title='delivery 3', ts=322, fesh=311),
            Offer(title='delivery 4', ts=324, fesh=312),
        ]

        for i in range(4):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 320 + i).respond(1.0 - 0.1 * i)
            cls.matrixnet.on_place(MnPlace.META_REARRANGE, 320 + i).respond(1.0 - 0.1 * i)

    def test_head_rearrange_with_delivery_MARKETOUT_29780(self):
        """
        MARKETOUT-12708
        Задаем запрос в place prime с указанием региона 300 (там, где два магазина)
        со включенным переранжированием (rearr-factors=market_meta_formula_type=TESTALGO_trivial)
        и текстом из названия офферов (delivery)

        Ожидаем увидеть офферы в следующем порядке: 1, 3, 2, 4.
        Оффер 2 был отфильтрован как дубликат и вытеснен в конец.
        В случае ошибки сортировки по достаке офферы будут идти в порядке 1, 2, 3, 4
        (офферы 1, 2, 3 будут браться из local tail и фильтр дубликата по картинке не применится)
        """
        response = self.report.request_json(
            'place=prime&rids=300&text=delivery'
            '&rearr-factors=market_meta_formula_type=TESTALGO_trivial;market_new_cpm_iterator=0'
        )
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "delivery 1"}},
                {"titles": {"raw": "delivery 3"}},
                {"titles": {"raw": "delivery 2"}},
                {"titles": {"raw": "delivery 4"}},
            ],
            preserve_order=True,
        )

    def test_head_rearrange_debug(self):
        """
        Проверка отладочного вывода информации о переранжировании на мете
        Проверяем, что выводится занчение формулы, ее имя и факторы
        """
        response = self.report.request_json(
            'place=prime&text=goods&debug=da&&rearr-factors=market_meta_formula_type=TESTALGO_trivial'
        )

        self.assertFragmentIn(
            response,
            {
                "debug": {
                    "fullFormulaInfo": [{"tag": "Meta", "value": NotEmpty(), "name": "TESTALGO_trivial"}],
                    "factors": NotEmpty(),
                }
            },
        )

    def test_default_meta_formulas_text(self):
        """
        По умолчанию на текстовом поиске используется метаформула meta_fml_formula_859074
        Формулу можно задать флагом market_meta_formula_type=formula_name

        """

        def meta_formula(formula):
            return {"debug": {"fullFormulaInfo": [{"tag": "Meta", "value": NotEmpty(), "name": formula}]}}

        response = self.report.request_json('place=prime&text=goods&hid=123&debug=da&')
        self.assertFragmentIn(response, meta_formula("meta_fml_formula_859074"))

        response = self.report.request_json(
            'place=prime&text=goods&hid=123&debug=da&rearr-factors=market_meta_formula_type=TESTALGO_trivial'
        )
        self.assertFragmentIn(response, meta_formula("TESTALGO_trivial"))

        response = self.report.request_json('place=prime&text=goods&hid=123&debug=da&client=ANDROID')
        self.assertFragmentIn(response, meta_formula("meta_fml_formula_859074"))

        response = self.report.request_json(
            'place=prime&text=goods&hid=123&debug=da&client=ANDROID&rearr-factors=market_meta_formula_type=TESTALGO_trivial'
        )
        self.assertFragmentIn(response, meta_formula("TESTALGO_trivial"))

    def test_default_meta_formulas_textless(self):
        """
        По умолчанию на бестексте используется метаформула base___common_851476_0_3__851442_0_7__fashion_859291_0_6__859187_0_4
        Формулу можно задать флагом market_textless_meta_formula_type=formula_name

        """

        def meta_formula(formula):
            return {"debug": {"fullFormulaInfo": [{"tag": "Meta", "value": NotEmpty(), "name": formula}]}}

        response = self.report.request_json('place=prime&hid=123&debug=da')
        self.assertFragmentIn(
            response, meta_formula("base___common_851476_0_3__851442_0_7__fashion_859291_0_6__859187_0_4")
        )

        response = self.report.request_json(
            'place=prime&hid=123&debug=da&rearr-factors=market_textless_meta_formula_type=TESTALGO_trivial'
        )
        self.assertFragmentIn(response, meta_formula("TESTALGO_trivial"))

        response = self.report.request_json('place=prime&hid=123&debug=da&client=ANDROID')
        self.assertFragmentIn(
            response, meta_formula("base___common_851476_0_3__851442_0_7__fashion_859291_0_6__859187_0_4")
        )

        response = self.report.request_json(
            'place=prime&hid=123&debug=da&client=ANDROID&rearr-factors=market_textless_meta_formula_type=TESTALGO_trivial'
        )
        self.assertFragmentIn(response, meta_formula("TESTALGO_trivial"))

    def test_default_formulas_on_app_2(self):
        formula_info = {
            "debug": {"fullFormulaInfo": [{"tag": "Meta", "value": NotEmpty(), "name": "meta_fml_formula_859074"}]}
        }

        response = self.report.request_json('place=prime&text=goods&hid=123&debug=da&api=content')
        self.assertFragmentIn(response, formula_info)

        response = self.report.request_json(
            'place=prime&text=goods&hid=123&debug=da&api=content&rearr-factors=market_textless_meta_formula_type=TESTALGO_trivial'
        )
        self.assertFragmentIn(response, formula_info)

        response = self.report.request_json('place=prime&hid=123&debug=da&api=content')
        self.assertFragmentNotIn(response, formula_info)

        response = self.report.request_json(
            'place=prime&hid=123&debug=da&api=content&rearr-factors=market_textless_meta_formula_type=TESTALGO_trivial'
        )
        self.assertFragmentIn(
            response, {"debug": {"fullFormulaInfo": [{"tag": "Meta", "value": NotEmpty(), "name": "TESTALGO_trivial"}]}}
        )

    def test_meta_formulas_on_blue_market(self):
        """Переранжирование на мете на синем маркете включается только при явном указании флагов
        market_blue_meta_formula_type - для переранжирования на текстовых запросах
        market_textless_meta_formula_type - для переранжирования на бестексте
        """

        formula_info = {
            "debug": {"fullFormulaInfo": [{"tag": "Meta", "value": NotEmpty(), "name": "TESTALGO_trivial"}]}
        }

        response = self.report.request_json(
            'place=prime&text=blue&debug=da&rgb=blue&rearr-factors=market_blue_meta_formula_type=TESTALGO_trivial'
        )
        self.assertFragmentIn(response, formula_info)

        response = self.report.request_json('place=prime&text=blue&debug=da&rgb=blue')
        self.assertFragmentNotIn(response, formula_info)

        response = self.report.request_json(
            'place=prime&hid=1&debug=da&rgb=blue&rearr-factors=market_blue_textless_meta_formula_type=TESTALGO_trivial'
        )
        self.assertFragmentIn(response, formula_info)

        response = self.report.request_json('place=prime&hid=123&debug=da')
        self.assertFragmentNotIn(response, formula_info)

        response = self.report.request_json(
            'place=prime&hid=123&debug=da&rearr-factors=market_textless_meta_formula_type=TESTALGO_trivial'
        )
        self.assertFragmentIn(
            response, {"debug": {"fullFormulaInfo": [{"tag": "Meta", "value": NotEmpty(), "name": "TESTALGO_trivial"}]}}
        )

    def test_default_formulas_on_goods(self):
        """Переранжирование на мете на Товарной Вертикали"""

        formula_info = {
            "debug": {"fullFormulaInfo": [{"tag": "Meta", "value": NotEmpty(), "name": "meta_fml_formula_861815"}]}
        }

        response = self.report.request_json('place=prime&text=goods&hid=123&debug=da&client=products&')
        self.assertFragmentIn(response, formula_info)

        response = self.report.request_json(
            'place=prime&text=goods&hid=123&debug=da&rearr-factors=market_textless_meta_formula_type=TESTALGO_trivial'
            + '&client=products'
        )
        self.assertFragmentIn(response, formula_info)

        response = self.report.request_json('place=prime&hid=123&debug=da&client=products')
        self.assertFragmentNotIn(response, formula_info)

        response = self.report.request_json(
            'place=prime&hid=123&debug=da&rearr-factors=market_textless_meta_formula_type=TESTALGO_trivial'
            + '&client=products'
        )
        self.assertFragmentIn(
            response, {"debug": {"fullFormulaInfo": [{"tag": "Meta", "value": NotEmpty(), "name": "TESTALGO_trivial"}]}}
        )

    @skip('deleted old booster')
    def test_boost_with_rearrange_textless(self):
        rearr = (
            '&rearr-factors=market_new_cpm_iterator=0;market_boost_allowed_hids=123;market_boost_brands=1;market_boost_brands_coef_textless=10;'
            'market_textless_meta_formula_type=TESTALGO_trivial;market_enable_textless_meta_boost=0'
        )
        request = 'place=prime&hid=123&numdoc=2' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "goods 1"}},
                {"titles": {"raw": "goods 5"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        request = 'place=prime&hid=123&numdoc=2&page=2' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "goods 4"}},
                {"titles": {"raw": "goods 3"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        request = 'place=prime&hid=123&numdoc=2&page=3' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "goods 2"}},
                {"titles": {"raw": "goods 6"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        request = 'place=prime&hid=123&numdoc=2&page=4' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "goods 7"}},
                {"titles": {"raw": "goods 8"}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        rearr = '&rearr-factors=market_new_cpm_iterator=0;market_textless_meta_formula_type=TESTALGO_trivial;market_boost_cpa_offers_mnvalue_coef_text=10;market_boost_cpa_offers_mnvalue_coef_textless=10'  # noqa
        request = 'place=prime&hid=123&numdoc=2&debug=da' + rearr + ';market_enable_textless_meta_boost=0'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "goods 7"}, "debug": {"metaProperties": {"BOOST_MULTIPLIER": "1"}}},
                {"titles": {"raw": "goods 5"}, "debug": {"metaProperties": {"BOOST_MULTIPLIER": "1"}}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        request = 'place=prime&text=goods&numdoc=2&debug=da' + rearr + ';market_enable_textless_meta_boost=0'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "goods 7"}, "debug": {"metaProperties": {"BOOST_MULTIPLIER": "10"}}},
                {"titles": {"raw": "goods 5"}, "debug": {"metaProperties": {"BOOST_MULTIPLIER": "10"}}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        request = 'place=prime&hid=123&numdoc=2&debug=da' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "goods 7"}, "debug": {"metaProperties": {"BOOST_MULTIPLIER": "10"}}},
                {"titles": {"raw": "goods 5"}, "debug": {"metaProperties": {"BOOST_MULTIPLIER": "10"}}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        request = 'place=prime&text=goods&numdoc=2&debug=da' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "goods 7"}, "debug": {"metaProperties": {"BOOST_MULTIPLIER": "10"}}},
                {"titles": {"raw": "goods 5"}, "debug": {"metaProperties": {"BOOST_MULTIPLIER": "10"}}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_formula_value_in_meta_properties(self):
        rearr = '&rearr-factors=market_new_cpm_iterator=0;market_textless_meta_formula_type=TESTALGO_trivial;'
        request = 'place=prime&hid=123&numdoc=2&debug=da' + rearr
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {"debug": {"metaProperties": {"FORMULA_NAME": "TESTALGO_trivial", "FORMULA_VALUE": "5"}}},
                {"debug": {"metaProperties": {"FORMULA_NAME": "TESTALGO_trivial", "FORMULA_VALUE": "4"}}},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    def test_no_meta_rearrange_on_nosearchresults(self):
        """Не делаем переранжирование на мете если не собираемся выводить результаты"""
        response = self.report.request_json(
            'place=prime&text=goods&debug=da' '&rearr-factors=market_optimize_no_search_results=1'
        )
        self.assertFragmentIn(response, 'GetHeadRearrangementCount(): Meta formula:')

        response = self.report.request_json(
            'place=prime&text=goods&debug=da&nosearchresults=1' '&rearr-factors=market_optimize_no_search_results=1'
        )
        self.assertFragmentIn(response, 'No rearrange on meta because: optimize for nosearchresults=1')


if __name__ == '__main__':
    main()
