#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import LikeUrl, Contains, ElementCount
from core.types import (
    CategoryRestriction,
    Currency,
    Disclaimer,
    HyperCategory,
    HyperCategoryType,
    MnPlace,
    Model,
    NavCategory,
    Offer,
    Opinion,
    RegionalRestriction,
    Shop,
    YamarecPlaceReasonsToBuy,
)
from core.blackbox import BlackboxUser
from core.testcase import TestCase, main


class T(TestCase):
    @staticmethod
    def get_waremd5(n):
        return "BBBBBBBB{:02}BBBBBBBBBBBA".format(n)

    @classmethod
    def prepare_parallel_prime(cls):
        cls.settings.default_search_experiment_flags += ['market_new_cpm_iterator=0']
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.navtree += [NavCategory(nid=346760, hid=34676)]
        cls.index.category_restrictions += [
            CategoryRestriction(
                name='medicine',
                hids=[34676],
                regional_restrictions=[
                    RegionalRestriction(
                        disclaimers=[
                            Disclaimer(name='medicine', text='Есть противопоказания, посоветуйтесь с врачом'),
                        ]
                    ),
                ],
            )
        ]

        docs_count = 20
        for i in range(1, docs_count + 1):
            offer_ts = 20000 + i
            hyperid = 200 + i
            model_ts = hyperid * 10
            waremd5 = T.get_waremd5(i)

            if i % 2 == 0:
                # в случае четных i чередуем:
                # * офферы, которые ищутся по [roundabout], с моделями, которые по [roundabout] не ищутся
                # * офферы без моделей
                if i % 4 == 0:
                    cls.index.models += [
                        Model(hyperid=hyperid, hid=34676, title="tuobadnuor model {0}".format(i), ts=model_ts),
                    ]
                    cls.index.offers += [
                        Offer(
                            title='roundabout offer {0} (with not-roundabout model)'.format(i),
                            ts=offer_ts,
                            hyperid=hyperid,
                            price=2000 + i,
                            url="http://roundabouts.ru/offer?id={0}".format(i),
                            fesh=i,
                        ),
                    ]
                else:
                    cls.index.offers += [
                        Offer(
                            title='roundabout offer {0} (w/o model)'.format(i),
                            ts=offer_ts,
                            price=2000 + i,
                            url="http://roundabouts.ru/offer?id={0}".format(i),
                            fesh=i,
                        ),
                    ]
            else:
                # для каждого нечетного i делаем связанные модель и оффер, которые ищутся по [roundabout]
                cls.index.models += [
                    Model(
                        hyperid=hyperid,
                        title="roundabout model {0}".format(i),
                        ts=model_ts,
                        hid=34676,
                        opinion=Opinion(rating=4.5, rating_count=10, total_count=12, precise_rating=4.37),
                        new=True,
                    ),
                ]
                cls.index.offers += [
                    Offer(
                        title='roundabout offer {0}'.format(i),
                        ts=offer_ts,
                        hyperid=hyperid,
                        price=2000 + i,
                        price_old=4000 + i,
                        url="http://roundabouts.ru/offer?id={0}".format(i),
                        fesh=i,
                        waremd5=waremd5,
                    ),
                ]

            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, model_ts).respond(0.99 - 0.001 * i)
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, offer_ts).respond(0.99 - 0.001 * i)

    def test_allow_collapsing_and_entities_for_parallel_prime(self):
        """Фиксируем поведение параметров &allow-collapsing= и &entities= на place=prime,
        на которые будем завязываться в каруселях.

        https://st.yandex-team.ru/MARKETOUT-34676
        https://st.yandex-team.ru/MARKETOUT-34678
        """

        def do_test(params, result):
            response = self.report.request_parallel_data(
                'place=parallel_prime&text=roundabout&numdoc=40&rearr-factors=market_metadoc_search=no' + params
            )
            self.assertFragmentIn(
                response, [{"titleText": title} for title in result], preserve_order=True, allow_different_len=False
            )

        # 1. Только модели
        # (на первый взгляд поведение не отличается от &allow-collapsing=0)
        # После https://st.yandex-team.ru/MARKETOUT-35875 эти параметры в place=parallel_prime автоматически проставляются по &slider-type=prime_models
        do_test(
            '&allow-collapsing=1&entities=product',
            [
                'roundabout model 1',
                'roundabout model 3',
                'roundabout model 5',
                'roundabout model 7',
                'roundabout model 9',
                'roundabout model 11',
                'roundabout model 13',
                'roundabout model 15',
                'roundabout model 17',
                'roundabout model 19',
            ],
        )

        # 2. Только офферы
        # (с &allow-collapsing=1 офферы с моделями схлопнутся в модели)
        # После https://st.yandex-team.ru/MARKETOUT-35875 эти параметры в place=parallel_prime автоматически проставляются по &slider-type=prime_offers
        do_test(
            '&allow-collapsing=0&entities=offer',
            [
                'roundabout offer 1',
                'roundabout offer 2 (w/o model)',
                'roundabout offer 3',
                'roundabout offer 4 (with not-roundabout model)',
                'roundabout offer 5',
                'roundabout offer 6 (w/o model)',
                'roundabout offer 7',
                'roundabout offer 8 (with not-roundabout model)',
                'roundabout offer 9',
                'roundabout offer 10 (w/o model)',
                'roundabout offer 11',
                'roundabout offer 12 (with not-roundabout model)',
                'roundabout offer 13',
                'roundabout offer 14 (w/o model)',
                'roundabout offer 15',
                'roundabout offer 16 (with not-roundabout model)',
                'roundabout offer 17',
                'roundabout offer 18 (w/o model)',
                'roundabout offer 19',
                'roundabout offer 20 (with not-roundabout model)',
            ],
        )

        # 3. Модели и офферы
        # (с &allow-collapsing=0 вместо tuobadnuor-моделей будут соотвествующие офферы)
        do_test(
            '&allow-collapsing=1',
            [
                'roundabout model 1',
                'roundabout model 3',
                'roundabout model 5',
                'roundabout model 7',
                'roundabout model 9',
                'roundabout model 11',
                'roundabout model 13',
                'roundabout model 15',
                'roundabout model 17',
                'roundabout model 19',
                'roundabout offer 2 (w/o model)',
                'tuobadnuor model 4',
                'roundabout offer 6 (w/o model)',
                'tuobadnuor model 8',
                'roundabout offer 10 (w/o model)',
                'tuobadnuor model 12',
                'roundabout offer 14 (w/o model)',
                'tuobadnuor model 16',
                'roundabout offer 18 (w/o model)',
                'tuobadnuor model 20',
            ],
        )

    def test_parallel_prime_model(self):
        """Проверка выдачи модели на place=parallel_prime

        https://st.yandex-team.ru/MARKETOUT-34676
        """

        response = self.report.request_parallel_data(
            'place=parallel_prime&text=roundabout&numdoc=40&slider-type=prime_models&rearr-factors=device=desktop;market_parallel_allow_new_models=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "titleText": "roundabout model 1",
                    "url": LikeUrl.of("//market.yandex.ru/product/201?hid=34676&nid=346760", ignore_len=False),
                    "image": "//mdata.yandex.net/i?path=b0130135356_img_id2520674011472212068.jpg&size=2",
                    "imageHd": "//mdata.yandex.net/i?path=b0130135356_img_id2520674011472212068.jpg&size=5",
                    "priceFromValue": "2001",
                    "priceCurrency": "RUR",
                    "ratingValue": 4.37,
                    "isNew": True,
                    "modelId": "201",
                }
            ],
        )

    def test_parallel_prime_offer(self):
        """Проверка выдачи оффера на place=parallel_prime

        https://st.yandex-team.ru/MARKETOUT-34676
        """

        response = self.report.request_parallel_data(
            'place=parallel_prime&text=roundabout&numdoc=40&slider-type=prime_offers&rearr-factors=market_slider_parallel_prime_enable_rating_for_offers=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "titleText": 'roundabout offer 1',
                    "url": "//market.yandex.ru/search?cvredirect=0&lr=0&rs=eJwzUvCS4xJzggIDQycEcJRgVmDQYAAAfXEGxg%2C%2C&text=roundabout&clid=545",
                    "image": "http://avatars.mdst.yandex.net/get-marketpic/1/market_iyC3nHslqLtqZJLygVAHeA/100x100",
                    "imageHd": "",
                    "priceValue": "2001",
                    "priceCurrency": "RUR",
                    "offerId": T.get_waremd5(1),
                    "shopName": "SHOP-1",
                    "shopUrl": "//market.yandex.ru/shop--shop-1/1/reviews?clid=545&cmid=BBBBBBBB01BBBBBBBBBBBA&lr=0&utm_medium=cpc&utm_referrer=wizards",
                    "oldPrice": "4001",
                    "discountPercent": "50",
                    "ratingValue": "4.37",
                }
            ],
        )

    def test_models_is_the_end(self):
        """Проверяем валидность метки конца в модельной карусели через основной поиск
        https://st.yandex-team.ru/MARKETOUT-35617
        """

        def do_test(numdoc, result_size, is_the_end):
            result = [
                'roundabout model 1',
                'roundabout model 3',
                'roundabout model 5',
                'roundabout model 7',
                'roundabout model 9',
                'roundabout model 11',
                'roundabout model 13',
                'roundabout model 15',
                'roundabout model 17',
                'roundabout model 19',
            ]

            response = self.report.request_parallel_data(
                'place=parallel_prime&text=roundabout&numdoc={0}&slider-type=prime_models'.format(numdoc)
            )
            self.assertFragmentIn(
                response,
                [{"titleText": title} for title in result[:result_size]],
                preserve_order=True,
                allow_different_len=False,
            )
            self.assertEqual(len(response.get_parallel_data()), result_size)
            self.assertEqual(response.get_is_the_end(), is_the_end)

        # Всего у нас есть 10 документов. При запросе < 10 документов в выдаче isTheEnd=False,
        # Начиная с 10 isTheEnd=True
        do_test(8, 8, False)
        do_test(9, 9, False)
        do_test(10, 10, True)
        do_test(11, 10, True)

    def test_models_ignore_docs(self):
        """Проверяем валидность метки конца в модельной карусели через основной поиск
        с игнором документов.
        https://st.yandex-team.ru/MARKETOUT-35617
        """

        def do_test(numdoc, result_size, is_the_end):
            result = [
                'roundabout model 1',
                'roundabout model 5',
                'roundabout model 7',
                'roundabout model 9',
                'roundabout model 11',
                'roundabout model 13',
                'roundabout model 15',
                'roundabout model 17',
            ]

            response = self.report.request_parallel_data(
                'place=parallel_prime&text=roundabout&numdoc={0}&slider-type=prime_models&slider-ignore-doc=203,219'.format(
                    numdoc
                )
            )
            self.assertFragmentIn(
                response,
                [{"titleText": title} for title in result[:result_size]],
                preserve_order=True,
                allow_different_len=False,
            )
            self.assertEqual(len(response.get_parallel_data()), result_size)
            self.assertEqual(response.get_is_the_end(), is_the_end)

        # Всего у нас есть 10 - 2 = 8 документов. При запросе < 8 документов в выдаче isTheEnd=False,
        # Начиная с 8 isTheEnd=True
        do_test(6, 6, False)
        do_test(7, 7, False)
        do_test(8, 8, True)
        do_test(9, 8, True)

    def test_offers_is_the_end(self):
        """Проверяем валидность метки конца в офферной карусели через основной поиск
        https://st.yandex-team.ru/MARKETOUT-35617
        """

        def do_test(numdoc, result_size, is_the_end):
            result = [
                'roundabout offer 1',
                'roundabout offer 2 (w/o model)',
                'roundabout offer 3',
                'roundabout offer 4 (with not-roundabout model)',
                'roundabout offer 5',
                'roundabout offer 6 (w/o model)',
                'roundabout offer 7',
                'roundabout offer 8 (with not-roundabout model)',
                'roundabout offer 9',
                'roundabout offer 10 (w/o model)',
                'roundabout offer 11',
                'roundabout offer 12 (with not-roundabout model)',
                'roundabout offer 13',
                'roundabout offer 14 (w/o model)',
                'roundabout offer 15',
                'roundabout offer 16 (with not-roundabout model)',
                'roundabout offer 17',
                'roundabout offer 18 (w/o model)',
                'roundabout offer 19',
                'roundabout offer 20 (with not-roundabout model)',
            ]

            response = self.report.request_parallel_data(
                'place=parallel_prime&text=roundabout&numdoc={0}&slider-type=prime_offers'.format(numdoc)
            )
            self.assertFragmentIn(
                response,
                [{"titleText": title} for title in result[:result_size]],
                preserve_order=True,
                allow_different_len=False,
            )
            self.assertEqual(len(response.get_parallel_data()), result_size)
            self.assertEqual(response.get_is_the_end(), is_the_end)

        # Всего у нас есть 20 документов. При запросе < 20 документов в выдаче isTheEnd=False,
        # Начиная с 20 isTheEnd=True
        do_test(18, 18, False)
        do_test(19, 19, False)
        do_test(20, 20, True)
        do_test(21, 20, True)

    def test_offers_ignore_docs(self):
        """Проверяем валидность метки конца в офферной карусели через основной поиск
        с игнором документов.
        https://st.yandex-team.ru/MARKETOUT-35617
        """

        def do_test(numdoc, result_size, is_the_end):
            result = [
                'roundabout offer 2 (w/o model)',
                'roundabout offer 3',
                'roundabout offer 4 (with not-roundabout model)',
                'roundabout offer 5',
                'roundabout offer 6 (w/o model)',
                'roundabout offer 7',
                'roundabout offer 8 (with not-roundabout model)',
                'roundabout offer 9',
                'roundabout offer 10 (w/o model)',
                'roundabout offer 11',
                'roundabout offer 12 (with not-roundabout model)',
                'roundabout offer 13',
                'roundabout offer 14 (w/o model)',
                'roundabout offer 16 (with not-roundabout model)',
                'roundabout offer 17',
                'roundabout offer 18 (w/o model)',
                'roundabout offer 19',
                'roundabout offer 20 (with not-roundabout model)',
            ]

            response = self.report.request_parallel_data(
                'place=parallel_prime&text=roundabout&numdoc={0}&slider-type=prime_offers&slider-ignore-doc={1},{2}'.format(
                    numdoc, T.get_waremd5(1), T.get_waremd5(15)
                )
            )
            self.assertFragmentIn(
                response,
                [{"titleText": title} for title in result[:result_size]],
                preserve_order=True,
                allow_different_len=False,
            )
            self.assertEqual(len(response.get_parallel_data()), result_size)
            self.assertEqual(response.get_is_the_end(), is_the_end)

        # Всего у нас есть 20 - 2 = 18 документов. При запросе < 18 документов в выдаче isTheEnd=False,
        # Начиная с 18 isTheEnd=True
        do_test(16, 16, False)
        do_test(17, 17, False)
        do_test(18, 18, True)
        do_test(19, 18, True)

    @classmethod
    def prepare_restriction_filter(cls):
        cls.index.category_restrictions += [
            CategoryRestriction(
                name='adult',
                hids=[6290267],
                regional_restrictions=[
                    RegionalRestriction(),
                ],
            ),
            CategoryRestriction(
                name='ask_18',
                hids=[6290267],
                regional_restrictions=[
                    RegionalRestriction(),
                ],
            ),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=6290267, name='adult category'),
        ]
        cls.index.models += [
            Model(hyperid=50, hid=6290267, title='Restricted model', ts=50),
        ]
        cls.index.offers += [
            Offer(hyperid=50, hid=6290267, title='Restricted offer', price=100),
        ]

    def test_restriction_filter(self):
        """Фильтрация документов по категориям для parallel_prime
        https://st.yandex-team.ru/MARKETOUT-36811
        """
        request = 'place=parallel_prime&text=restricted&numdoc=40'

        response = self.report.request_parallel_data(request + "&slider-type=prime_offers")
        self.assertFragmentNotIn(
            response,
            {
                "titleText": "Restricted offer",
            },
        )

        response = self.report.request_parallel_data(request + "&slider-type=prime_models")
        self.assertFragmentNotIn(
            response,
            {
                "titleText": "Restricted model",
            },
        )

    @classmethod
    def prepare_parallel_prime_reviews(cls):
        cls.index.models += [
            Model(
                hyperid=101,
                hid=30,
                title="panasonic tumix 5000",
                opinion=Opinion(rating=4.5, rating_count=10, total_count=12, precise_rating=4.46),
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=101, fesh=39, price=3000),
        ]

    def test_parallel_prime_reviews(self):
        '''Отзывы от parallel_prime
        https://st.yandex-team.ru/MARKETOUT-37728
        '''

        # Проверяем на десктопе
        response = self.report.request_parallel_data(
            "place=parallel_prime&text=panasonic+tumix&numdoc=5&slider-type=prime_models&rearr-factors=market_implicit_model_wizard_random_avatar_count=2"
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "reviews": {
                        "count": "12",
                        "url": LikeUrl.of(
                            "//market.yandex.ru/product--panasonic-tumix-5000/101/reviews?clid=721&lr=0&text=panasonic%20tumix&utm_medium=cpc&utm_referrer=wizards"
                        ),
                        "authorAvatars": ElementCount(2),
                    }
                }
            ],
        )

        # Проверяем на таче
        response = self.report.request_parallel_data(
            "place=parallel_prime&text=panasonic+tumix&numdoc=5&slider-type=prime_models&touch=1&rearr-factors=market_implicit_model_wizard_random_avatar_count=3"
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "reviews": {
                        "count": "12",
                        "url": LikeUrl.of(
                            "//m.market.yandex.ru/product--panasonic-tumix-5000/101/reviews?clid=698&lr=0&text=panasonic%20tumix&utm_medium=cpc&utm_referrer=wizards"
                        ),
                        "authorAvatars": ElementCount(3),
                    }
                }
            ],
        )

    @classmethod
    def prepare_parallel_prime_author_avatars(cls):
        cls.index.models += [
            Model(hyperid=1011, ts=101, title="bull", opinion=Opinion(total_count=10)),
        ]
        cls.index.offers += [
            Offer(hyperid=1011),
        ]
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101).respond(0.9)
        cls.index.yamarec_places += [
            YamarecPlaceReasonsToBuy()
            .new_partition("split=1")
            .add(
                hyperid=1011,
                reasons=[
                    {
                        "id": "positive_feedback",
                        "type": "consumerFactor",
                        "author_puid": "1001",
                        "text": "Уникальный аппарат, который уверенно смотрит в будущее",
                    }
                ],
            )
        ]
        cls.blackbox.on_request(uids=['1001']).respond(
            [
                BlackboxUser(uid='1001', name='name 1001', avatar='avatar_1001'),
            ]
        )

    def test_parallel_prime_author_avatars(self):
        '''Проверяем аватарки в ответе parallel_prime
        https://st.yandex-team.ru/MARKETOUT-38066
        '''
        response = self.report.request_parallel_data(
            "place=parallel_prime&text=bull&numdoc=5&slider-type=prime_models&rearr-factors=split=1;market_implicit_model_wizard_author_info=1"
        )
        self.assertFragmentIn(response, [{"reviews": {"authorAvatars": ElementCount(1)}}])

    def test_parallel_prime_offer_disclaimer(self):
        """Показ disclaimer под флагом market_parallel_show_warning_text_for_offers_wizard
        https://st.yandex-team.ru/MARKETOUT-37840
        """

        response = self.report.request_parallel_data(
            'place=parallel_prime&text=roundabout&numdoc=40&slider-type=prime_offers'
        )
        self.assertFragmentNotIn(
            response,
            [{"titleText": 'roundabout offer 1', "disclaimer": "Есть противопоказания, посоветуйтесь с врачом"}],
        )
        response = self.report.request_parallel_data(
            'place=parallel_prime&text=roundabout&numdoc=40&slider-type=prime_offers&rearr-factors=market_parallel_show_warning_text_for_offers_wizard=1'
        )
        self.assertFragmentIn(
            response,
            [{"titleText": 'roundabout offer 1', "disclaimer": "Есть противопоказания, посоветуйтесь с врачом"}],
        )

    @classmethod
    def prepare_filter_out_models_without_price(cls):
        cls.index.models += [
            Model(hyperid=393, hid=210, title='model with min_cutprice=2000'),
            Model(hyperid=394, hid=210, title='model without price'),
            Model(hyperid=395, hid=210, title='model price'),
        ]

        cls.index.offers += [
            Offer(
                hyperid=393,
                fesh=39,
                price=2000,
                is_cutprice=True,
            ),
            Offer(hyperid=395, fesh=39, price=2000, is_cutprice=False, pickup=False),
        ]

    def test_filter_out_models_without_price(self):
        """Проверяем, что модели без цены не показываются
        https://st.yandex-team.ru/MARKETOUT-37915
        """

        response = self.report.request_parallel_data(
            'place=parallel_prime&text=models+min_cutprice=2000&bsformat=1&pp=18&numdoc=20&slider-type=prime_models'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "titleText": "model price",
                    "modelId": "395",
                }
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "titleText": "model with min_cutprice=2000",
                    "modelId": "393",
                }
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "titleText": "model without price",
                    "modelId": "394",
                }
            ],
        )

    def test_change_url_to_external(self):
        """Проверяем, что под market_slider_parallel_prime_set_external_url урлы ведут в магазины
        https://st.yandex-team.ru/MARKETOUT-39517
        """

        response = self.report.request_parallel_data(
            'place=parallel_prime&text=roundabout&numdoc=40&slider-type=prime_offers&rearr-factors=market_slider_parallel_prime_set_external_url=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "titleText": 'roundabout offer 1',
                    "url": Contains("//market-click2.yandex.ru/redir/dtype=market"),
                }
            ],
        )

    @classmethod
    def prepare_groupings(cls):
        cls.index.offers += [
            Offer(hyperid=500, fesh=39, price=2000, title="africa 1", ts=350),
            Offer(hyperid=500, fesh=39, price=2000, title="africa 2", ts=351),
            Offer(hyperid=500, fesh=39, price=2000, title="africa 3", ts=352),
            Offer(hyperid=501, fesh=41, price=2100, title="africa 4", ts=353),
            Offer(hyperid=501, fesh=41, price=1000, title="africa 5", ts=354),
            Offer(hyperid=501, fesh=41, price=1100, title="africa 6", ts=355),
            Offer(hyperid=501, fesh=42, price=1100, title="africa 7", ts=356),
            Offer(hyperid=501, fesh=42, price=1100, title="africa 8", ts=357),
            Offer(hyperid=501, fesh=42, price=1100, title="africa 9", ts=358),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 350).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 351).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 352).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 353).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 354).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 355).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 356).respond(0.3)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 357).respond(0.2)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 358).respond(0.1)

    def test_groupings(self):
        """Проверяем, что под market_set_prime_grouping_to_shop_id отдаем по дному офферу на магазин
        Проверяем, что под market_set_prime_grouping_to_hyper_ts группируем офферы по модели
         https://st.yandex-team.ru/MARKETOUT-39819
        """

        # Проверяем флаг market_set_prime_grouping_to_shop_id
        response = self.report.request_parallel_data(
            'place=parallel_prime&text=africa&numdoc=10&slider-type=prime_offers&rearr-factors=market_set_prime_grouping_to_shop_id=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "titleText": 'africa 1',
                },
                {
                    "titleText": 'africa 4',
                },
                {
                    "titleText": 'africa 7',
                },
            ],
        )

        self.assertFragmentNotIn(
            response,
            [
                {
                    "titleText": 'africa 2',
                }
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "titleText": 'africa 3',
                }
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "titleText": 'africa 5',
                }
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "titleText": 'africa 6',
                }
            ],
        )

        # Проверяем флаг market_set_prime_grouping_to_hyper_ts
        response = self.report.request_parallel_data(
            'place=parallel_prime&text=africa&numdoc=10&slider-type=prime_offers&rearr-factors=market_set_prime_grouping_to_hyper_ts=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "titleText": 'africa 1',
                },
                {
                    "titleText": 'africa 4',
                },
                {
                    "titleText": 'africa 7',
                },
            ],
        )

        self.assertFragmentNotIn(
            response,
            [
                {
                    "titleText": 'africa 2',
                }
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "titleText": 'africa 3',
                }
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "titleText": 'africa 5',
                }
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "titleText": 'africa 6',
                }
            ],
        )

    def test_parallel_prime_shop_id_filter(self):
        """Проверка фильтрации по магазину выдачи оффера на place=parallel_prime
        https://st.yandex-team.ru/MARKETOUT-39681
        """
        request = 'place=parallel_prime&text=roundabout&numdoc=5&slider-type=prime_offers'

        # С параметром &fesh=3 в выдачи офферы только от магазина SHOP-3
        response = self.report.request_parallel_data(request + '&fesh=3')
        self.assertFragmentIn(
            response,
            [{"titleText": 'roundabout offer 3', "shopName": "SHOP-3"}],
            preserve_order=True,
            allow_different_len=False,
        )

        # Без параметра &fesh в выдачи офферы всех магазинов
        response = self.report.request_parallel_data(request)
        self.assertFragmentIn(
            response,
            [
                {"titleText": 'roundabout offer 1', "shopName": "SHOP-1"},
                {"titleText": 'roundabout offer 2 (w/o model)', "shopName": "SHOP-2"},
                {"titleText": 'roundabout offer 3', "shopName": "SHOP-3"},
                {"titleText": 'roundabout offer 4 (with not-roundabout model)', "shopName": "SHOP-4"},
                {"titleText": 'roundabout offer 5', "shopName": "SHOP-5"},
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_uniq_by_models(cls):

        cls.index.models += [
            Model(hyperid=100100),
            Model(hyperid=100101),
        ]

        cls.index.offers += [
            Offer(hyperid=100100, fesh=39, price=2000, title="island 1", waremd5='12222222222222gggggggg', ts=8495),
            Offer(hyperid=100100, fesh=39, price=2001, title="island 2", waremd5='22222222222222gggggggg', ts=8496),
            Offer(hyperid=100101, fesh=39, price=2001, title="island 3", waremd5='32222222222222gggggggg', ts=8497),
            Offer(hyperid=100100, fesh=41, price=2001, title="island 4", waremd5='42222222222222gggggggg', ts=8498),
            Offer(hyperid=100101, fesh=41, price=2001, title="island 5", waremd5='52222222222222gggggggg', ts=8499),
            Offer(hyperid=100100, fesh=42, price=2001, title="island 6", waremd5='62222222222222gggggggg', ts=8501),
            Offer(hyperid=100102, fesh=42, price=2001, title="island 7", waremd5='72222222222222gggggggg', ts=8502),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8495).respond(0.9)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8496).respond(0.8)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8497).respond(0.7)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8498).respond(0.6)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8499).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8501).respond(0.4)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 8502).respond(0.3)

    def test_uniq_by_models(self):
        """Проверка уникализации выдачи по модели
        https://st.yandex-team.ru/MARKETOUT-40422
        """
        request = 'place=parallel_prime&text=island&numdoc=10&slider-type=prime_offers'
        response = self.report.request_parallel_data(request)
        self.assertFragmentIn(
            response,
            [
                {
                    "titleText": 'island 1',
                },
                {
                    "titleText": 'island 2',
                },
                {
                    "titleText": 'island 3',
                },
                {
                    "titleText": 'island 4',
                },
                {
                    "titleText": 'island 5',
                },
                {
                    "titleText": 'island 6',
                },
                {
                    "titleText": 'island 7',
                },
            ],
        )

        response = self.report.request_parallel_data(request + "&rearr-factors=market_set_uniq_attr_to_hyper_ts=1")
        self.assertFragmentIn(
            response,
            [
                {
                    "titleText": 'island 1',
                },
                {
                    "titleText": 'island 3',
                },
                {
                    "titleText": 'island 7',
                },
            ],
        )

        self.assertFragmentNotIn(
            response,
            [
                {
                    "titleText": 'island 2',
                },
                {
                    "titleText": 'island 4',
                },
                {
                    "titleText": 'island 5',
                },
                {
                    "titleText": 'island 6',
                },
            ],
        )

    @classmethod
    def prepare_filter_offers_without_urls(cls):
        cls.index.hypertree += [HyperCategory(hid=10, output_type=HyperCategoryType.GURU)]
        cls.index.navtree += [NavCategory(nid=10, hid=10)]
        cls.index.models += [Model(hyperid=2000, title="DSBS model w/o url 0", hid=10)]

        cls.index.shops += [
            Shop(
                fesh=431782,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                priority_region=213,
                name='Яндекс.Маркет',
            )
        ]

        for i in range(3):
            shop_id = 40707 + i
            cls.index.shops += [
                Shop(
                    fesh=shop_id,
                    name='DSBS shop no urls {}'.format(shop_id),
                    cpa=Shop.CPA_REAL,
                )
            ]

            cls.index.offers += [
                Offer(
                    title="nemoloko dsbs w/o url {}".format(i),
                    cpa=Offer.CPA_REAL,
                    is_cpc=False,
                    has_url=False,
                    fesh=shop_id,
                    waremd5='M4Ss6aSMI9aJ80cjbHq0mg' if i == 0 else None,
                    hyperid=2000 if i == 0 else None,
                    ts=1000 + i,
                ),
            ]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000 + i).respond(0.9 - 0.01 * i)

            cls.index.shops += [
                Shop(
                    fesh=shop_id + 100,
                    name='DSBS shop with urls {}'.format(shop_id + 100),
                    cpa=Shop.CPA_REAL,
                )
            ]

            cls.index.offers += [
                Offer(
                    title="nemoloko dsbs with url {}".format(i),
                    cpa=Offer.CPA_REAL,
                    fesh=shop_id + 100,
                    url="http://nemolokoseller.ru/offer?id={0}".format(i),
                    ts=1100 + i,
                ),
            ]
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1100 + i).respond(0.8 - 0.01 * i)

    def test_filter_offers_without_urls(self):
        """Проверка фильтрации оферов без урлов
        https://st.yandex-team.ru/MARKETOUT-40707
        """
        # нет фильтрации, найдём все офера
        request = 'place=parallel_prime&text=nemoloko&numdoc=6&slider-type=prime_offers'
        response = self.report.request_parallel_data(request)
        res_with_urls = [{'titleText': 'nemoloko dsbs with url {}'.format(i)} for i in range(3)]
        res_without_urls = [{'titleText': 'nemoloko dsbs w/o url {}'.format(i)} for i in range(3)]
        self.assertFragmentIn(response, res_with_urls + res_without_urls)

        # включаем фильтрацию в place=prime
        rearr = ['market_disable_cpc_without_links_on_prime=1', 'market_slider_parallel_prime_set_external_url=1']
        request = 'place=parallel_prime&text=nemoloko&numdoc=6&slider-type=prime_offers&rearr-factors={}'.format(
            ';'.join(rearr)
        )
        response = self.report.request_parallel_data(request)
        self.assertFragmentNotIn(response, res_without_urls)

    def test_dsbs_url_type(self):
        """Проверяем, что флаг market_offers_wizard_incut_dsbs_url_type задает тип ссылки
        для DSBS офферов без ссылки в магазин
        https://st.yandex-team.ru/MARKETOUT-41633
        """
        request = (
            'place=parallel_prime&text=nemoloko&numdoc=2&slider-type=prime_offers'
            '&rearr-factors=market_slider_parallel_prime_set_external_url=1;'
            'market_max_offers_per_shop_count=1;'
        )

        # Под флагом market_offers_wizard_incut_dsbs_url_type=OfferCard ведем на КО
        response = self.report.request_parallel_data(request + 'market_offers_wizard_incut_dsbs_url_type=OfferCard')
        self.assertFragmentIn(
            response,
            [
                {
                    "titleText": "nemoloko dsbs w/o url 0",
                    "url": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                },
                {
                    "titleText": "nemoloko dsbs w/o url 1",
                    "url": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                },
            ],
            preserve_order=True,
            allow_different_len=False,
        )

        # Под флагом market_offers_wizard_incut_dsbs_url_type=ModelCard ведем на КМ
        # Оффер без модели ведет на КО
        response = self.report.request_parallel_data(request + 'market_offers_wizard_incut_dsbs_url_type=ModelCard')
        self.assertFragmentIn(
            response,
            [
                {
                    "titleText": "nemoloko dsbs w/o url 0",
                    "shopName": "Яндекс.Маркет",
                    "url": LikeUrl.of(
                        "//market.yandex.ru/product/2000?do-waremd5=M4Ss6aSMI9aJ80cjbHq0mg&hid=10&lr=0&nid=10&clid=545"
                    ),
                },
                {
                    "titleText": "nemoloko dsbs w/o url 1",
                    "shopName": "Яндекс.Маркет",
                    "url": Contains("//market-click2.yandex.ru/redir/dtype=offercard/"),
                },
            ],
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_round_old_price(cls):
        cls.index.shops += [
            Shop(
                fesh=214,
                currency=Currency.RUR,
            ),
        ]

        cls.index.offers += [
            Offer(fesh=214, price=2000, title="ocean", price_old=4000.21),
        ]

    def test_round_old_price(self):
        """
        https://st.yandex-team.ru/MARKETOUT-40791
        Убираем копейки из oldPrice
        """
        request = 'place=parallel_prime&text=ocean&numdoc=10&slider-type=prime_offers'
        response = self.report.request_parallel_data(request)

        self.assertFragmentIn(
            response,
            [
                {
                    "titleText": 'ocean',
                    "oldPrice": "4000",
                }
            ],
        )


if __name__ == '__main__':
    main()
