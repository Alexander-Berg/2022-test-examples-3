#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import LikeUrl, NoKey
from core.types import (
    CategoryRestriction,
    Disclaimer,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Opinion,
    RegionalRestriction,
    YamarecPlaceReasonsToBuy,
)
from core.testcase import TestCase, main
from core.blackbox import BlackboxUser


ITEMS_COUNT = 25
DEFAULT_PARALLEL_TESTS_REARR = ";parallel_smm=1.0;ext_snippet=1;no_snippet_arc=1"


class T(TestCase):
    @staticmethod
    def get_waremd5(n):
        return "AAAAAAAA{:02}AAAAAAAAAAAA".format(n)

    @staticmethod
    def get_modelid(n):
        return str(100 + n)

    @classmethod
    def prepare_parallel_sliders(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.navtree += [NavCategory(nid=322780, hid=32278)]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='medicine',
                hids=[32278],
                regional_restrictions=[
                    RegionalRestriction(
                        disclaimers=[
                            Disclaimer(name='medicine', text='Есть противопоказания, посоветуйтесь с врачом'),
                        ]
                    ),
                ],
            )
        ]

        for i in range(1, ITEMS_COUNT + 1):
            offer_ts = 10000 + i
            hyperid = 100 + i
            model_ts = hyperid * 10

            cls.index.offers += [
                Offer(
                    title='carousel offer {0}'.format(i),
                    ts=offer_ts,
                    hyperid=hyperid,
                    price=1000 + i,
                    url="http://slider.ru/offer?id={0}".format(i),
                    waremd5=T.get_waremd5(i),
                    fesh=i,
                ),
            ]
            cls.index.models += [
                Model(
                    hyperid=hyperid,
                    hid=32278,
                    title="carousel model {0}".format(i),
                    ts=model_ts,
                    opinion=Opinion(rating=4.5, rating_count=10, total_count=12, precise_rating=4.73),
                    new=True,
                ),
            ]

            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, offer_ts).respond(0.99 - 0.001 * i)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, model_ts).respond(0.99 - 0.001 * i)

        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy()
            .new_partition()
            .add(
                hyperid=101,
                reasons=[
                    {
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1001",
                        "text": "feedback 1",
                        "anonymous": "false",
                    },
                    {
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1002",
                        "text": "feedback 2",
                    },
                    {
                        "id": "alisa_lives_here",
                        "type": "specsFactor",
                        "value": 1,
                    },
                ],
            )
            .add(
                hyperid=102,
                reasons=[
                    {
                        "id": "compatible_with_alisa",
                        "type": "specsFactor",
                        "value": 1,
                    }
                ],
            )
        ]

        cls.blackbox.on_request(uids=['1001', '1002']).respond(
            [
                BlackboxUser(uid='1001', name='name 1001', avatar='avatar_1001'),
                BlackboxUser(uid='1002', name='name 1002', avatar='avatar_1002'),
            ]
        )

    def test_parallel_sliders(self):
        """Тестируем базу для каруселей в колдунщиках.
        На текущем этапе пока что включение карусели не имеет под собой ничего содержательного,
        работает как старый параллельный, оставляя только один колдунщик в выдаче.
        Пока что это лишь база для оттачивания выдачи.

        https://st.yandex-team.ru/MARKETOUT-31681
        """

        # 1. В обычной выдаче на параллельном есть все колдунщики
        response = self.report.request_bs_pb('place=parallel&text=carousel')
        self.assertFragmentIn(response, {"market_offers_wizard": {}})
        self.assertFragmentIn(response, {"market_implicit_model": {}})
        self.assertFragmentIn(response, {"market_model": {}})
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "showcase": {
                        "items": [
                            {
                                "title": {"text": {"__hl": {"text": "carousel offer 1", "raw": True}}},
                                "offerId": T.get_waremd5(1),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "carousel offer 2", "raw": True}}},
                                "offerId": T.get_waremd5(2),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "carousel offer 3", "raw": True}}},
                                "offerId": T.get_waremd5(3),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "carousel offer 4", "raw": True}}},
                                "offerId": T.get_waremd5(4),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "carousel offer 5", "raw": True}}},
                                "offerId": T.get_waremd5(5),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "carousel offer 6", "raw": True}}},
                                "offerId": T.get_waremd5(6),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "carousel offer 7", "raw": True}}},
                                "offerId": T.get_waremd5(7),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "carousel offer 8", "raw": True}}},
                                "offerId": T.get_waremd5(8),
                            },
                            {
                                "title": {"text": {"__hl": {"text": "carousel offer 9", "raw": True}}},
                                "offerId": T.get_waremd5(9),
                            },
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "showcase": {
                        "items": [
                            {"title": {"text": {"__hl": {"text": "carousel model 1", "raw": True}}}, "modelId": "101"},
                            {"title": {"text": {"__hl": {"text": "carousel model 2", "raw": True}}}, "modelId": "102"},
                            {"title": {"text": {"__hl": {"text": "carousel model 3", "raw": True}}}, "modelId": "103"},
                            {"title": {"text": {"__hl": {"text": "carousel model 4", "raw": True}}}, "modelId": "104"},
                            {"title": {"text": {"__hl": {"text": "carousel model 5", "raw": True}}}, "modelId": "105"},
                            {"title": {"text": {"__hl": {"text": "carousel model 6", "raw": True}}}, "modelId": "106"},
                        ]
                    }
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # 2. C &slider-type=offers в выдаче появляется офферная карусель
        for device in ('desktop', 'touch'):
            rearr = 'device={0};market_parallel_show_warning_text_for_offers_wizard=1;'.format(device)
            if device == 'touch':
                rearr += ';offers_touch=1'

            response = self.report.request_parallel_data(
                'place=parallel_slider&text=carousel&slider-type=offers&numdoc=7&rearr-factors={0}'.format(rearr)
            )
            self.assertFragmentIn(
                response,
                [
                    {"titleText": "carousel offer 1", "offerId": T.get_waremd5(1)},
                    {"titleText": "carousel offer 2", "offerId": T.get_waremd5(2)},
                    {"titleText": "carousel offer 3", "offerId": T.get_waremd5(3)},
                    {"titleText": "carousel offer 4", "offerId": T.get_waremd5(4)},
                    {"titleText": "carousel offer 5", "offerId": T.get_waremd5(5)},
                    {"titleText": "carousel offer 6", "offerId": T.get_waremd5(6)},
                    {"titleText": "carousel offer 7", "offerId": T.get_waremd5(7)},
                ],
                preserve_order=True,
                allow_different_len=False,
            )

            desktop_url = (
                "//market.yandex.ru/search?cvredirect=0&rs=eJwzUvCS4xJzhAIDQ0ckIMGkwKDBAAB8GQay&text=carousel&lr=0&utm_medium=cpc&utm_referrer=wizards&"
                "utm_medium=cpc&utm_referrer=wizards&clid=545"
            )
            touch_url = (
                "//market.yandex.ru/search?cvredirect=0&rs=eJwzUvCS4xJzhAIDQ0ckIMGkwKDBAAB8GQay&text=carousel&lr=0&utm_medium=cpc&utm_referrer=wizards&"
                "utm_medium=cpc&utm_referrer=wizards&clid=708"
            )
            shop_desktop_url = "//market.yandex.ru/shop--shop-1/1/reviews?cmid=AAAAAAAA01AAAAAAAAAAAA&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=545"
            shop_touch_url = "//m.market.yandex.ru/grades-shop.xml?shop_id=1&cmid=AAAAAAAA01AAAAAAAAAAAA&lr=0&utm_medium=cpc&utm_referrer=wizards&clid=708"
            self.assertFragmentIn(
                response,
                {
                    "titleText": "carousel offer 1",
                    "url": LikeUrl.of(touch_url if device == 'touch' else desktop_url),
                    "image": "//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100",
                    "imageHd": "//avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100",
                    "priceValue": "1001",
                    "priceCurrency": "RUR",
                    "ratingValue": NoKey("ratingValue"),
                    "disclaimer": "Есть противопоказания, посоветуйтесь с врачом",
                    "offerId": "AAAAAAAA01AAAAAAAAAAAA",
                    "shopName": "SHOP-1",
                    "shopUrl": LikeUrl.of(shop_touch_url if device == 'touch' else shop_desktop_url),
                },
            )

        # 3. C &slider-type=models в выдаче появляется модельная карусель
        for device in ('desktop', 'touch'):
            response = self.report.request_parallel_data(
                'place=parallel_slider&text=carousel&slider-type=models&numdoc=5&rearr-factors=device={0};market_parallel_allow_new_models=1'.format(
                    device
                )
            )
            self.assertFragmentIn(
                response,
                [
                    {"titleText": "carousel model 1", "modelId": "101"},
                    {"titleText": "carousel model 2", "modelId": "102"},
                    {"titleText": "carousel model 3", "modelId": "103"},
                    {"titleText": "carousel model 4", "modelId": "104"},
                    {"titleText": "carousel model 5", "modelId": "105"},
                ],
                preserve_order=True,
                allow_different_len=False,
            )

            desktop_url = "//market.yandex.ru/product--carousel-model-1/101?hid=32278&nid=322780&text=carousel&clid=698&lr=0&utm_medium=cpc&utm_referrer=wizards"
            touch_url = "//m.market.yandex.ru/product--carousel-model-1/101?hid=32278&nid=322780&text=carousel&clid=721&lr=0&utm_medium=cpc&utm_referrer=wizards"

            self.assertFragmentIn(
                response,
                {
                    "titleText": "carousel model 1",
                    "url": LikeUrl.of(touch_url if device == 'touch' else desktop_url, ignore_len=False),
                    "image": "//mdata.yandex.net/i?path=b0130135356_img_id2520674011472212068.jpg&size=2",
                    "imageHd": "//mdata.yandex.net/i?path=b0130135356_img_id2520674011472212068.jpg&size=5",
                    "priceFromValue": "1001",
                    "priceCurrency": "RUR",
                    "ratingValue": 4.73,
                    "isNew": True,
                    "modelId": "101",
                },
            )

        # 4. C неправильным &slider-type в выдаче ничего нет
        response = self.report.request_parallel_data('place=parallel_slider&text=carousel&slider-type=ololo')
        self.assertFragmentNotIn(response, [])

    def test_the_end_mark_in_wizards(self):
        """Если за выдачей колдунщика нет хвоста, то должна быть метка isTheEnd.

        Для того, чтобы определить, есть ли хвост, нужно запрашивать дополнительные офферы/модели,
        т.к. сейчас запрос делается ровно под выдачу. Их количество задаётся настройками
        market_parallel_slider_extra_models и market_parallel_slider_extra_offers.

        https://st.yandex-team.ru/MARKETOUT-expected_list
        """

        # Если в выдаче оказались все офферы и хвоста нет, то должна быть метка "isTheEnd": "1".
        # В противном случае, если хвост есть, этой метки быть не должно.

        for models_count in range(1, ITEMS_COUNT + 3):
            response = self.report.request_bs_pb(
                'place=parallel&text=carousel&rearr-factors=market_implicit_model_wizard_models_right_incut_count={0};market_parallel_slider_extra_models=1'.format(
                    models_count
                )
            )
            self.assertFragmentIn(
                response,
                {"market_implicit_model": {"isTheEnd": NoKey("isTheEnd") if models_count < ITEMS_COUNT else "1"}},
            )

        for offers_count in range(1, ITEMS_COUNT + 3):
            response = self.report.request_bs_pb(
                'place=parallel&text=carousel&rearr-factors=market_offers_wizard_offers_incut_count={0};market_parallel_slider_extra_offers=1'.format(
                    offers_count
                )
            )
            self.assertFragmentIn(
                response,
                {"market_offers_wizard": {"isTheEnd": NoKey("isTheEnd") if offers_count < ITEMS_COUNT else "1"}},
            )

    def test_models_slider_reviews_data(self):
        """Модельная карусель: информация об отзывах, превью отзыва.

        https://st.yandex-team.ru/MARKETOUT-32922
        """

        for device in ('desktop', 'touch'):
            response = self.report.request_parallel_data(
                'place=parallel_slider&text=carousel&slider-type=models&numdoc=1&rearr-factors=device={0};market_implicit_model_wizard_author_info=1'.format(
                    device
                )
            )

            reviews_desktop_url = "//market.yandex.ru/product--carousel-model-1/101/reviews?lr=0&text=carousel&clid=698"
            reviews_touch_url = "//m.market.yandex.ru/product--carousel-model-1/101/reviews?lr=0&text=carousel&clid=721"

            self.assertFragmentIn(
                response,
                {
                    "titleText": "carousel model 1",
                    "reviews": {
                        "url": LikeUrl.of(reviews_touch_url if device == 'touch' else reviews_desktop_url),
                        "count": 12,
                        "authorAvatars": ["avatar_1001", "avatar_1002"],
                    },
                    "reviewPreview": {
                        "text": "feedback 1",
                        "anonymous": False,
                        "authorName": "name 1001",
                        "authorAvatar": "avatar_1001",
                    },
                },
            )

    def test_models_slider_alisa_data(self):
        """Модельная карусель: информация о совместимости с Алисой.

        https://st.yandex-team.ru/MARKETOUT-33230
        """

        response = self.report.request_parallel_data('place=parallel_slider&text=carousel&slider-type=models&numdoc=3')
        self.assertFragmentIn(
            response,
            [
                {"titleText": "carousel model 1", "withAlice": "alisa_lives_here"},
                {"titleText": "carousel model 2", "withAlice": "compatible_with_alisa"},
                {"titleText": "carousel model 3", "withAlice": NoKey("withAlice")},
            ],
        )

    def test_model_rating_in_offers_slider(self):
        """Под флагом market_offers_wizard_model_rating=1 у офферов должен отображаться рейтинг модели
        https://st.yandex-team.ru/MARKETOUT-34448
        """
        response = self.report.request_parallel_data(
            'place=parallel_slider&text=carousel&slider-type=offers&numdoc=3&rearr-factors=market_offers_wizard_model_rating=1'
        )

        self.assertFragmentIn(
            response,
            {
                "titleText": "carousel offer 1",
                "ratingValue": 4.73,
            },
        )

    def test_models_slider_max_length(self):
        """Проверяем ограничение на длину бесконечной модельной карусели.
        Флаг market_parallel_slider_models_max_count.

        https://st.yandex-team.ru/MARKETOUT-34615
        """

        def do_test(requested_count, ignored_list, expected_list, is_the_end, max_count, ignore_extra):
            request = 'place=parallel_slider&text=carousel&slider-type=models&numdoc={0}'.format(requested_count)
            for ignored_doc in ignored_list:
                request += '&slider-ignore-doc={0}'.format(T.get_modelid(ignored_doc))

            if ignore_extra:
                # Документ, которого не будет в выдаче
                request += '&slider-ignore-doc=1234567'

            # Открутка мета-фильтра, чтобы протестировать корректоность с одной моделью в результате
            request += '&rearr-factors=market_implicit_model_wizard_meta_threshold=0.0'

            # https://st.yandex-team.ru/MARKETOUT-34790
            request += ';market_parallel_slider_extra_models=1'

            if max_count is not None:
                request += ';market_parallel_slider_models_max_count={0}'.format(max_count)

            response = self.report.request_parallel_data(request)
            if len(expected_list) > 0:
                self.assertFragmentIn(
                    response,
                    [{"titleText": "carousel model {}".format(result_doc)} for result_doc in expected_list],
                    preserve_order=True,
                    allow_different_len=False,
                )
                self.assertEqual(response.get_is_the_end(), is_the_end)
            else:
                self.assertFragmentNotIn(response, [])

        # 1. Все игнорируемые документы находятся

        # 1.1 Крайний случай: все найденные документы кончились
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24],
            is_the_end=False,
            max_count=None,
            ignore_extra=False,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24],
            is_the_end=False,
            max_count=25,
            ignore_extra=False,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24],
            is_the_end=True,
            max_count=24,
            ignore_extra=False,
        )

        # 1.2 Средний случай
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14, 15],
            is_the_end=True,
            max_count=15,
            ignore_extra=False,
        )

        # 1.3 Крайний случай: в карусель уже нечего отдать
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8],
            is_the_end=True,
            max_count=8,
            ignore_extra=False,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7],
            is_the_end=True,
            max_count=7,
            ignore_extra=False,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[],
            is_the_end=True,
            max_count=6,
            ignore_extra=False,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[],
            is_the_end=True,
            max_count=5,
            ignore_extra=False,
        )

        # 2. Один из игнорируемых документов на дозапрос не находится, но т.к. он был показан
        # Нужно учесть его в общем ограничении, поэтому is_the_end=True возникает раньше

        # 2.1 Крайний случай: все найденные документы кончились
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24],
            is_the_end=False,
            max_count=None,
            ignore_extra=True,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24],
            is_the_end=True,
            max_count=25,
            ignore_extra=True,
        )  # is_the_end=False
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23],
            is_the_end=True,
            max_count=24,
            ignore_extra=True,
        )

        # 2.2 Средний случай
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14],
            is_the_end=True,
            max_count=15,
            ignore_extra=True,
        )

        # 2.3 Крайний случай: в карусель уже нечего отдать
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7],
            is_the_end=True,
            max_count=8,
            ignore_extra=True,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[],
            is_the_end=True,
            max_count=7,
            ignore_extra=True,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[],
            is_the_end=True,
            max_count=6,
            ignore_extra=True,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[],
            is_the_end=True,
            max_count=5,
            ignore_extra=True,
        )

    def test_offers_slider_max_length(self):
        """Проверяем ограничение на длину бесконечной офферной карусели.
        Флаг market_parallel_slider_offers_max_count.

        https://st.yandex-team.ru/MARKETOUT-34615
        """

        def do_test(requested_count, ignored_list, expected_list, is_the_end, max_count, ignore_extra=False):
            request = 'place=parallel_slider&text=carousel&slider-type=offers&numdoc={0}'.format(requested_count)
            for ignored_doc in ignored_list:
                ignored_waremd5 = T.get_waremd5(ignored_doc)
                request += '&slider-ignore-doc={0}'.format(ignored_waremd5)

            if ignore_extra:
                # Документ, которого не будет в выдаче
                request += '&slider-ignore-doc=waremd5_123'

            # https://st.yandex-team.ru/MARKETOUT-34790
            request += '&rearr-factors=market_parallel_slider_extra_offers=1'

            if max_count is not None:
                request += ';market_parallel_slider_offers_max_count={0}'.format(max_count)

            response = self.report.request_parallel_data(request)
            if len(expected_list) > 0:
                self.assertFragmentIn(
                    response,
                    [{"titleText": "carousel offer {}".format(result_doc)} for result_doc in expected_list],
                    preserve_order=True,
                    allow_different_len=False,
                )
                self.assertEqual(response.get_is_the_end(), is_the_end)
            else:
                self.assertFragmentNotIn(response, [])

        # 1. Все игнорируемые документы находятся

        # 1.1 Крайний случай: все найденные документы кончились
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24],
            is_the_end=False,
            max_count=None,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24],
            is_the_end=False,
            max_count=25,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24],
            is_the_end=True,
            max_count=24,
        )

        # 1.2 Средний случай
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14, 15],
            is_the_end=True,
            max_count=15,
        )

        # 1.3 Крайний случай: в карусель уже нечего отдать
        do_test(requested_count=18, ignored_list=[1, 2, 3, 4, 5, 6], expected_list=[7, 8], is_the_end=True, max_count=8)
        do_test(requested_count=18, ignored_list=[1, 2, 3, 4, 5, 6], expected_list=[7], is_the_end=True, max_count=7)
        do_test(requested_count=18, ignored_list=[1, 2, 3, 4, 5, 6], expected_list=[], is_the_end=True, max_count=6)
        do_test(requested_count=18, ignored_list=[1, 2, 3, 4, 5, 6], expected_list=[], is_the_end=True, max_count=5)

        # 2. Один из игнорируемых документов на дозапрос не находится, но т.к. он был показан
        # Нужно учесть его в общем ограничении, поэтому is_the_end=True возникает раньше

        # 2.1 Крайний случай: все найденные документы кончились
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24],
            is_the_end=False,
            max_count=None,
            ignore_extra=True,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24],
            is_the_end=True,
            max_count=25,
            ignore_extra=True,
        )  # is_the_end=False
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23],
            is_the_end=True,
            max_count=24,
            ignore_extra=True,
        )

        # 2.2 Средний случай
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7, 8, 9, 10, 11, 12, 13, 14],
            is_the_end=True,
            max_count=15,
            ignore_extra=True,
        )

        # 2.3 Крайний случай: в карусель уже нечего отдать
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[7],
            is_the_end=True,
            max_count=8,
            ignore_extra=True,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[],
            is_the_end=True,
            max_count=7,
            ignore_extra=True,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[],
            is_the_end=True,
            max_count=6,
            ignore_extra=True,
        )
        do_test(
            requested_count=18,
            ignored_list=[1, 2, 3, 4, 5, 6],
            expected_list=[],
            is_the_end=True,
            max_count=5,
            ignore_extra=True,
        )

    def test_requery_url(self):
        """Генерируем урлы для дозапроса каруселей в колдунщиках
        https://st.yandex-team.ru/MARKETOUT-34872
        """

        request = (
            'place=parallel&text=carousel&rearr-factors=market_implicit_model_wizard_models_right_incut_count={0};'
            'market_parallel_slider_extra_models=1;market_parallel_prime_slider_request_count=5'
        )
        for models_count in range(1, ITEMS_COUNT + 3):
            response = self.report.request_bs_pb(request.format(models_count))
            docs_to_ignore = ','.join([T.get_modelid(i) for i in range(1, models_count + 1)])

            not_the_end = models_count < ITEMS_COUNT
            requery_url = (
                "/search/report_market?market_slider_type=prime_models&market_slider_numdoc=5&text=carousel&lr=0"
                "&market_slider_ignore_doc={0}".format(docs_to_ignore)
            )

            self.assertFragmentIn(
                response,
                {
                    "market_implicit_model": {
                        "requeryUrl": requery_url if not_the_end else NoKey("requeryUrl"),
                    }
                },
            )

        for offers_count in range(1, ITEMS_COUNT + 3):
            response = self.report.request_bs_pb(
                'place=parallel&text=carousel&rearr-factors=market_offers_wizard_offers_incut_count={0};market_parallel_slider_extra_offers=1;market_parallel_prime_slider_request_count=5'.format(
                    offers_count
                )
            )
            docs_to_ignore = ','.join([T.get_waremd5(i) for i in range(1, offers_count + 1)])

            not_the_end = offers_count < ITEMS_COUNT
            requery_url = (
                "/search/report_market?market_slider_type=prime_offers&market_slider_numdoc=5&text=carousel&lr=0"
                "&market_slider_ignore_doc={0}".format(docs_to_ignore)
            )

            self.assertFragmentIn(
                response,
                {
                    "market_offers_wizard": {
                        "requeryUrl": requery_url if not_the_end else NoKey("requeryUrl"),
                    }
                },
            )

    def test_parallel_prime_requery_url(self):
        '''Проверяем формирвание url дозапроса в parallel_slider и parallel_prime
        https://st.yandex-team.ru/MARKETOUT-35830
        '''

        models_ignore = "&market_slider_ignore_doc=101,102,103,104,105,106"
        offers_ignore = (
            "&market_slider_ignore_doc=AAAAAAAA01AAAAAAAAAAAA,AAAAAAAA02AAAAAAAAAAAA,AAAAAAAA03AAAAAAAAAAAA,"
            "AAAAAAAA04AAAAAAAAAAAA,AAAAAAAA05AAAAAAAAAAAA,AAAAAAAA06AAAAAAAAAAAA,AAAAAAAA07AAAAAAAAAAAA,AAAAAAAA08AAAAAAAAAAAA,AAAAAAAA09AAAAAAAAAAAA"
        )
        # Проверяем формирование url дозапроса в place=parallel под флагами market_parallel_slider_offers_request_count и market_parallel_slider_models_request_count
        response = self.report.request_bs_pb(
            'place=parallel&text=carousel&rearr-factors=market_parallel_prime_slider_request_count=2'
        )

        offerRequeryUrl = (
            "/search/report_market?market_slider_type=prime_offers&market_slider_numdoc=2&text=carousel&lr=0"
            + offers_ignore
        )
        modelRequeryUrl = (
            "/search/report_market?market_slider_type=prime_models&market_slider_numdoc=2&text=carousel&lr=0"
            + models_ignore
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "requeryUrl": offerRequeryUrl,
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "requeryUrl": modelRequeryUrl,
                }
            },
        )

        # Проверяем формирование url дозапроса в place=parallel_prime под флагом market_parallel_prime_slider_request_count
        response = self.report.request_bs_pb(
            'place=parallel&text=carousel&rearr-factors=market_parallel_prime_slider_request_count=3'
        )
        offerPrimeRequeryUrl = (
            "/search/report_market?market_slider_type=prime_offers&market_slider_numdoc=3&text=carousel&lr=0"
            + offers_ignore
        )
        modelPrimeRequeryUrl = (
            "/search/report_market?market_slider_type=prime_models&market_slider_numdoc=3&text=carousel&lr=0"
            + models_ignore
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "requeryUrl": offerPrimeRequeryUrl,
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "requeryUrl": modelPrimeRequeryUrl,
                }
            },
        )

        # Проверяем приоритет при одновременном использовании market_parallel_slider_offers_request_count и market_parallel_prime_slider_request_count
        response = self.report.request_bs_pb(
            'place=parallel&text=carousel&rearr-factors=market_parallel_prime_slider_request_count=3'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "requeryUrl": offerPrimeRequeryUrl,
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "requeryUrl": modelPrimeRequeryUrl,
                }
            },
        )

        # Проверяем приоритет при одновременном использовании market_parallel_slider_models_request_count и market_parallel_prime_slider_request_count
        response = self.report.request_bs_pb(
            'place=parallel&text=carousel&rearr-factors=market_parallel_prime_slider_request_count=3'
        )
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": {
                    "requeryUrl": offerPrimeRequeryUrl,
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "market_implicit_model": {
                    "requeryUrl": modelPrimeRequeryUrl,
                }
            },
        )

        # Симулируем дозапрос в parallel_prime и проверяем, что возвращается правильный url дозапроса
        query = (
            "place=parallel_prime&slider-type=prime_models&numdoc=2&text=carousel&lr=0"
            "&slider-ignore-doc=101,102"
            "&rearr-factors=market_parallel_data_new_format=1;market_implicit_model_wizard_author_info=1;market_implicit_model_wizard_random_avatar_count=0;market_parallel_allow_new_models=1"
        )
        response = self.report.request_parallel_data(query)
        requeryUrl = "/search/report_market?market_slider_type=prime_models&market_slider_numdoc=2&text=carousel&lr=0&market_slider_ignore_doc=101,102,103,104"
        self.assertEqual(response.get_requery_url(), requeryUrl)


if __name__ == '__main__':
    main()
