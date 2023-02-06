#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import ExperimentalShopIncut, MnPlace, Offer, Shop
from core.testcase import TestCase, main
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ["market_money_disable_bids=0"]
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[225]),
            Shop(fesh=2, priority_region=213, regions=[225]),
            Shop(fesh=3, priority_region=213, regions=[225]),
            Shop(fesh=4, priority_region=213, regions=[225]),  # not in experiment
        ]
        cls.index.experimental_shop_incut_shops += [
            ExperimentalShopIncut(1, [1, 2, 3, 4]),
            ExperimentalShopIncut(2, [1, 2, 3, 4]),
            ExperimentalShopIncut(3, [1, 2, 3, 4]),
        ]

        cls.index.offers += [
            Offer(hid=1, bid=10, fesh=1, title='vrezka'),
            Offer(hid=1, bid=10, fesh=1, title='vrezka'),
            Offer(hid=1, bid=10, fesh=1),
            Offer(hid=2, bid=10, fesh=1, title='vrezka'),
            Offer(hid=2, bid=10, fesh=1, title='vrezka'),
            Offer(hid=1, bid=21, fesh=2, title='uno', price=15),
            Offer(hid=1, bid=20, fesh=2, title='dos', price=10),
            Offer(hid=3, fesh=2, title='uno-uno-uno', ts=23),
            Offer(hid=3, fesh=2, title='quatro', ts=24),
            Offer(hid=4, fesh=2, ts=25, title='ohmygodable'),
            Offer(hid=4, fesh=2, ts=26, title='ohmygodable'),
            Offer(hid=1, bid=15, fesh=3, title='vrezka'),
            Offer(hid=1, bid=15, fesh=3),
            Offer(hid=1, bid=100, fesh=4, title='vrezka', ts=41),
        ]

        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.01)
        cls.matrixnet.on_default_place(MnPlace.META_REARRANGE).respond(0.01)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 25).respond(0.01)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 26).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 41).respond(0.1)

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 23).respond(0.02)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 24).respond(0.01)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 23).respond(0.01)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 24).respond(0.02)

        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 1).respond(0.01)
        cls.matrixnet.on_place(MnPlace.CATEGORY_RELEVANCE, 2).respond(0.02)

    def test_hid_only(self):
        '''
        Проверяем наличие врезки при поиске только по категории
        '''
        response = self.report.request_json(
            'place=prime&hid=1&rids=213' '&rearr-factors=market_shop_incut_enable=1;market_shop_incut_min_size=0'
        )
        self.assertFragmentIn(
            response,
            {
                'shopIncut': {
                    'offers': [
                        {
                            'entity': 'offer',
                            'shop': {'id': 2},
                            'titles': {'raw': 'uno'},
                        },
                        {
                            'entity': 'offer',
                            'shop': {'id': 2},
                            'titles': {'raw': 'dos'},
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_text_and_hid(self):
        '''
        Проверяем наличие врезки при поиске по тексту и категории
        '''
        response = self.report.request_json(
            'place=prime&hid=1&rids=213&text=vrezka'
            '&rearr-factors=market_shop_incut_enable=1;market_shop_incut_min_size=0'
        )
        self.assertFragmentIn(
            response,
            {
                'shopIncut': {
                    'offers': [
                        {
                            'entity': 'offer',
                            'shop': {'id': 3},
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_text_and_hid_2(self):
        '''
        Проверяем наличие врезки при поиске только по тексту
        Выбирается самая релевантная категория
        '''
        response = self.report.request_json(
            'place=prime&rids=213&text=vrezka' '&rearr-factors=market_shop_incut_enable=1;market_shop_incut_min_size=0'
        )
        self.assertFragmentIn(
            response,
            {
                'shopIncut': {
                    'offers': [
                        {
                            'entity': 'offer',
                            'shop': {'id': 1},
                            'categories': [{'id': 2}],
                        },
                        {
                            'entity': 'offer',
                            'shop': {'id': 1},
                            'categories': [{'id': 2}],
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_meta_rearrange(self):
        '''
        Проверяем что врезка переранжируется на мете и только под флагом
        '''
        response = self.report.request_json(
            'place=prime&hid=3&rids=213'
            '&rearr-factors=market_shop_incut_enable=1;market_shop_incut_min_size=0;market_textless_meta_formula_type=meta_fml_formula_823709'
        )
        self.assertFragmentIn(
            response,
            {
                'shopIncut': {
                    'offers': [
                        {
                            'entity': 'offer',
                            'shop': {'id': 2},
                            'titles': {'raw': 'uno-uno-uno'},
                        },
                        {
                            'entity': 'offer',
                            'shop': {'id': 2},
                            'titles': {'raw': 'quatro'},
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json(
            'place=prime&hid=3&rids=213'
            '&rearr-factors=market_shop_incut_enable=1;market_shop_incut_min_size=0;'
            'market_shop_incut_enable_rearrange=1;market_textless_meta_formula_type=meta_fml_formula_823709'
        )
        self.assertFragmentIn(
            response,
            {
                'shopIncut': {
                    'offers': [
                        {
                            'entity': 'offer',
                            'shop': {'id': 2},
                            'titles': {'raw': 'quatro'},
                        },
                        {
                            'entity': 'offer',
                            'shop': {'id': 2},
                            'titles': {'raw': 'uno-uno-uno'},
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_formula_threshold(self):
        '''
        Проверяем что флаг управления порогом релевантности
        '''
        response = self.report.request_json(
            'place=prime&hid=4&rids=213&text=ohmygodable'
            '&rearr-factors=market_shop_incut_enable=1;market_shop_incut_min_size=0'
        )
        self.assertFragmentIn(
            response,
            {
                'shopIncut': {
                    'offers': [
                        {
                            'entity': 'offer',
                            'shop': {'id': 2},
                        },
                        {
                            'entity': 'offer',
                            'shop': {'id': 2},
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&hid=4&rids=213&text=ohmygodable'
            '&rearr-factors=market_shop_incut_enable=1;market_shop_incut_min_size=0;market_shop_incut_relevance_formula_threshold=0.015'
        )
        self.assertFragmentIn(
            response,
            {
                'shopIncut': {
                    'offers': [
                        {
                            'entity': 'offer',
                            'shop': {'id': 2},
                        },
                    ]
                }
            },
            allow_different_len=False,
        )

    def test_shop_filter(self):
        '''
        Проверяем отсутствие врезки при фильтрации по магазину
        '''
        response = self.report.request_json(
            'place=prime&hid=1&rids=213&fesh=2' '&rearr-factors=market_shop_incut_enable=1;market_shop_incut_min_size=0'
        )
        self.assertFragmentNotIn(
            response,
            {
                'shopIncut': {},
            },
        )

    def test_sorting(self):
        '''
        Проверяем сортировку врезки при пользовательском поиске
        '''
        response = self.report.request_json(
            'place=prime&hid=1&rids=213&how=aprice'
            '&rearr-factors=market_shop_incut_enable=1;market_shop_incut_min_size=0'
        )
        self.assertFragmentIn(
            response,
            {
                'shopIncut': {
                    'offers': [
                        {
                            'entity': 'offer',
                            'shop': {'id': 2},
                            'titles': {'raw': 'dos'},
                        },
                        {
                            'entity': 'offer',
                            'shop': {'id': 2},
                            'titles': {'raw': 'uno'},
                        },
                    ]
                }
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_click_log(self):
        '''
        Проверяем наличие правильного pp во врезке и цену клика равную мин. ставке
        '''
        _ = self.report.request_json(
            'place=prime&hid=1&rids=213'
            '&show-urls=encrypted&pp=7'
            '&rearr-factors=market_shop_incut_enable=1;market_shop_incut_min_size=0'
        )
        self.click_log.expect(shop_id=2, pp=8, min_bid=1, cp=1)

    def test_min_size(self):
        '''
        Проверяем отсутствие врезки при малом количестве офферов
        '''
        response = self.report.request_json('place=prime&hid=1&rids=213' '&rearr-factors=market_shop_incut_enable=1')
        self.assertFragmentIn(response, {'shopIncut': Absent()})


if __name__ == '__main__':
    main()
