#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.svn_data import SvnData
from core.matcher import Contains, NoKey
from core.types.picture import Picture, to_mbo_picture
from core.types import Offer, Shop, Model, MnPlace


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_svn_data = True
        cls.settings.market_access_settings.use_svn_data = True

        cls.index.offers += [
            Offer(title='Футболка обычная', hid=1),
            Offer(title='Футболка Zа Россию', hid=1),
            Offer(title='Футболка с рисунком Z', hid=1),
            Offer(title='Футболка с рисунком "Z V"', hid=1),
            Offer(title='Футболка "Zа Россию"', hid=1),
            Offer(title='Футболка "Спецоперация и мир"', hid=1),
            Offer(title='Футболка «Zа мир, Zа правду, Zа родину»', hid=1),
            Offer(title='Футболка БанZай', hid=1),
            Offer(title='Samsung Galaxy Z Flip'),
            Offer(title='samsung galaxy z fold'),
        ]

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        svn_data = SvnData(access_server=access_server, shade_host_port=shade_host_port, meta_paths=cls.meta_paths)

        svn_data.polit_stop_words += ['Z', 'Zа', 'Спецоперация и мир', 'x y й']

        svn_data.polit_good_words += [
            'z flip',
            'z fold',
        ]

        svn_data.create_version()

    def test_political_stop_words_filter(self):
        """Проверяем, что под флагом market_political_stop_words_filter=1 товыры
        с политической символикой отфильровываются по стоп-словам
        https://st.yandex-team.ru/MARKETOUT-46272
        """

        expected_good_tshorts = {
            "results": [
                {"titles": {"raw": "Футболка обычная"}},
                {"titles": {"raw": "Футболка БанZай"}},
            ],
        }
        # 1. Запрос не содержит стоп-слова, фильтрация включена
        response = self.report.request_json('place=prime&text=Футболка')
        self.assertFragmentIn(response, expected_good_tshorts, allow_different_len=False)

        # 1.1 бестекстовый запрос не содержит стоп-слов - фильтрация включена
        response = self.report.request_json('place=prime&hid=1')
        self.assertFragmentIn(response, expected_good_tshorts, allow_different_len=False)

        # 2. Запрос не содержит стоп-слова, фильтрация включена
        # Товары из списка исключений не отфильтровываются
        response = self.report.request_json('place=prime&text=Samsung+Galaxy')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "titles": {"raw": "Samsung Galaxy Z Flip"},
                    },
                    {
                        "titles": {"raw": "samsung galaxy z fold"},
                    },
                ],
            },
            allow_different_len=False,
        )

        expected_no_filtration = {
            "results": [
                {"titles": {"raw": "Футболка обычная"}},
                {"titles": {"raw": "Футболка Zа Россию"}},
                {"titles": {"raw": "Футболка с рисунком Z"}},
                {"titles": {"raw": "Футболка с рисунком \"Z V\""}},
                {"titles": {"raw": "Футболка \"Zа Россию\""}},
                {"titles": {"raw": "Футболка \"Спецоперация и мир\""}},
                {"titles": {"raw": "Футболка «Zа мир, Zа правду, Zа родину»"}},
                {"titles": {"raw": "Футболка БанZай"}},
                {"titles": {"raw": "Samsung Galaxy Z Flip"}},
                {"titles": {"raw": "samsung galaxy z fold"}},
            ],
        }

        # 3. Запрос содержит стоп-слова, фильтрация выключена
        for text in ['Футболка+Z', 'футболка+z', 'футболка+"Z"']:
            response = self.report.request_json('place=prime&text=' + text)
            self.assertFragmentIn(response, expected_no_filtration, allow_different_len=False)

        # 4. Флаг market_political_stop_words_filter=0 выключает фильтрацию
        response = self.report.request_json(
            'place=prime&text=Футболка+Z&rearr-factors=market_political_stop_words_filter=0'
        )
        self.assertFragmentIn(response, expected_no_filtration, allow_different_len=False)

        # 4. Флаг market_political_stop_words_filter=0 выключает фильтрацию и на бестексте
        response = self.report.request_json('place=prime&hid=1&rearr-factors=market_political_stop_words_filter=0')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"titles": {"raw": "Футболка обычная"}},
                    {"titles": {"raw": "Футболка Zа Россию"}},
                    {"titles": {"raw": "Футболка с рисунком Z"}},
                    {"titles": {"raw": "Футболка с рисунком \"Z V\""}},
                    {"titles": {"raw": "Футболка \"Zа Россию\""}},
                    {"titles": {"raw": "Футболка \"Спецоперация и мир\""}},
                    {"titles": {"raw": "Футболка «Zа мир, Zа правду, Zа родину»"}},
                    {"titles": {"raw": "Футболка БанZай"}},
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_cpa_shop_incut(cls):

        cls.index.shops += [
            Shop(fesh=41, priority_region=213, cpa=Shop.CPA_REAL),
            Shop(fesh=42, priority_region=213, cpa=Shop.CPA_REAL),
        ]
        cls.index.offers += [
            Offer(hyperid=4, hid=44, fesh=42, price=100, fee=500, title='Ставка ниже', cpa=Offer.CPA_REAL),
            Offer(hyperid=4, hid=44, fesh=41, price=100, fee=1000, title='Ставка на Zа выше ', cpa=Offer.CPA_REAL),
        ]

    def test_cpa_shop_incut(self):
        """Проверяем что офферы со стопсловами фильтруются также и в place=cpa_shop_incut"""

        response = self.report.request_json(
            'place=cpa_shop_incut&text=ставка+цена&show-urls=cpa&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0;market_political_stop_words_filter=0'
        )
        self.assertFragmentIn(response, {"results": [{'titles': {'raw': 'Ставка на Zа выше'}}]})

        response = self.report.request_json(
            'place=cpa_shop_incut&text=ставка+цена&show-urls=cpa&debug=1&rearr-factors=market_premium_ads_gallery_default_min_num_doc=0;'
            'market_premium_ads_gallery_shop_incut_logarithm_price=0;'
        )
        self.assertFragmentIn(response, {"results": [{'titles': {'raw': 'Ставка ниже'}}]})

    @classmethod
    def prepare_intents(cls):
        def pic(picid):
            return Picture(picture_id=picid, width=200, height=200, group_id=1000)

        cls.index.models += [
            Model(hid=51, hyperid=51, title="X", proto_picture=to_mbo_picture('Model-X.jpg')),
            Model(hid=52, hyperid=52, title="X Y", proto_picture=to_mbo_picture('Model-X-Y.jpg')),
            Model(hid=53, hyperid=53, title="X Y Й", proto_picture=to_mbo_picture('Model-X-Y-Й.jpg')),
            Model(hid=54, hyperid=54, title="X Y Й 2", proto_picture=to_mbo_picture('Model-X-Y-Й-2.jpg')),
        ]

        cls.index.shops += [Shop(fesh=51, priority_region=213, cpa=Shop.CPA_REAL)]

        cls.index.offers += [
            Offer(
                hid=51,
                fesh=51,
                hyperid=51,
                title="X Model good Offer good",
                picture=pic('model_good_offer_good_'),
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=52,
                fesh=51,
                hyperid=52,
                title="X Model good Offer bad (x y й)",
                picture=pic('model_good_offer_bad__'),
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=53,
                fesh=51,
                hyperid=53,
                title="X Model bad Offer good",
                picture=pic('model_bad_offer_good__'),
                cpa=Offer.CPA_REAL,
            ),
            Offer(
                hid=54,
                fesh=51,
                hyperid=54,
                title="X Model bad Offer bad (x y й)",
                picture=pic('model_bad_offer_bad___'),
                cpa=Offer.CPA_REAL,
            ),
            # офферы хорошие чтобы находились категории
            Offer(hid=51, fesh=51, ts=51, title='X 51', picture=pic('-PU_popular_for_51__iw'), cpa=Offer.CPA_REAL),
            Offer(hid=52, fesh=51, ts=52, title='X 52', picture=pic('-PU_popular_for_52__iw'), cpa=Offer.CPA_REAL),
            Offer(hid=53, fesh=51, ts=53, title='X 53', picture=pic('-PU_popular_for_53__iw'), cpa=Offer.CPA_REAL),
            Offer(hid=54, fesh=51, ts=54, title='X 54', picture=pic('-PU_popular_for_54__iw'), cpa=Offer.CPA_REAL),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 51).respond(0.001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 52).respond(0.001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 53).respond(0.001)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 54).respond(0.001)

    def test_intents_without_filtration(self):
        """Как интенты работали раньше: без фильтрации плохого контента и без использвания офферов с меты"""

        response = self.report.request_json(
            'place=prime&text=X+X&&allow-collapsing=1&use-default-offers=1&additional_entities=intents_pictures&cpa=real'
            '&rearr-factors=market_intents_pictures_from_docs_on_meta=0;market_political_stop_words_filter=0'
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'X'}},
                        {'titles': {'raw': 'X Y'}},
                        {'titles': {'raw': 'X Y Й'}},
                        {'titles': {'raw': 'X Y Й 2'}},
                        {'titles': {'raw': 'X 51'}},
                        {'titles': {'raw': 'X 52'}},
                        {'titles': {'raw': 'X 53'}},
                        {'titles': {'raw': 'X 54'}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=False,
        )

        self.assertFragmentIn(
            response,
            {
                'intents': [
                    {'category': {'hid': 51}, 'pictures': [{'original': {'url': Contains('model_good_offer_good')}}]},
                    {'category': {'hid': 52}, 'pictures': [{'original': {'url': Contains('model_good_offer_bad')}}]},
                    {'category': {'hid': 53}, 'pictures': [{'original': {'url': Contains('model_bad_offer_good')}}]},
                    {'category': {'hid': 54}, 'pictures': [{'original': {'url': Contains('model_bad_offer_bad')}}]},
                ]
            },
        )

    def test_intents_with_filtration(self):
        """Если мы фильтруем офферы только в запросе TOfferPics: фильтруются все документы у которых плохой тайтл или плохой тайтл модели
        В результате у нас остаются некоторые интенты без картинки (она будет отдельно запрошена фронтом)
        """

        response = self.report.request_json(
            'place=prime&text=X+X&&allow-collapsing=1&use-default-offers=1&additional_entities=intents_pictures&cpa=real'
            '&rearr-factors=market_intents_pictures_from_docs_on_meta=0;market_political_stop_words_filter=1'
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'X'}},
                        {'titles': {'raw': 'X 51'}},
                        {'titles': {'raw': 'X 52'}},
                        {'titles': {'raw': 'X 53'}},
                        {'titles': {'raw': 'X 54'}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=False,
        )

        # картинки к нидам взяты из выдачи
        self.assertFragmentIn(
            response,
            {
                'intents': [
                    {'category': {'hid': 51}, 'pictures': [{'original': {'url': Contains('model_good_offer_good')}}]},
                    {'category': {'hid': 52}, 'pictures': NoKey('pictures')},
                    {
                        'category': {'hid': 53},
                        'pictures': [{'original': {'url': Contains('model_bad_offer_good')}}],
                    },  # не можем отфильтровать т.к. не знаем что имя у модели плохое
                    {'category': {'hid': 54}, 'pictures': NoKey('pictures')},
                ]
            },
        )

    def test_intents_with_filtration_and_documents_from_meta(self):
        """Используем картинки не только офферов с базовых но и офферов с меты
        Самые популярные документы с меты теперь передают свои картинки в интенты"""

        response = self.report.request_json(
            'place=prime&text=X+X&&allow-collapsing=1&use-default-offers=1&additional_entities=intents_pictures&cpa=real'
            '&rearr-factors=market_intents_pictures_from_docs_on_meta=1;market_political_stop_words_filter=1'
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'titles': {'raw': 'X'}},
                        {'titles': {'raw': 'X 51'}},
                        {'titles': {'raw': 'X 52'}},
                        {'titles': {'raw': 'X 53'}},
                        {'titles': {'raw': 'X 54'}},
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=False,
        )

        self.assertFragmentIn(
            response,
            {
                'intents': [
                    {'category': {'hid': 51}, 'pictures': [{'original': {'url': Contains('model_good_offer_good')}}]},
                    {'category': {'hid': 52}, 'pictures': [{'original': {'url': Contains('popular_for_52')}}]},
                    {'category': {'hid': 53}, 'pictures': [{'original': {'url': Contains('popular_for_53')}}]},
                    {'category': {'hid': 54}, 'pictures': [{'original': {'url': Contains('popular_for_54')}}]},
                ]
            },
        )


if __name__ == '__main__':
    main()
