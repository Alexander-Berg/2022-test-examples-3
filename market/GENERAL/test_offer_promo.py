#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import DynamicPromo, Model, Offer, Promo, PromoOffer, PromoType, Region, RegionalModel, Shop, VCluster
from core.matcher import NoKey, Absent, NotEmpty
from unittest import skip

from datetime import datetime


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.shops += [
            Shop(fesh=11, priority_region=213),
            Shop(fesh=12, priority_region=214, regions=[213], cpa=Shop.CPA_REAL),
        ]

        cls.index.regiontree += [
            Region(rid=213, name='Москва', region_type=Region.FEDERATIVE_SUBJECT),
            Region(rid=214, name='Республика Пепястан', region_type=Region.FEDERATIVE_SUBJECT),
        ]

        cls.index.offers += [
            Offer(
                title='promo 1',
                fesh=11,
                hyperid=101,
                waremd5='X9_iwgA17yd3FCI0LRhC_w',
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='xMpCOKC5I4INzFCab3WEmw',
                    required_quantity=3,
                    free_quantity=34,
                ),
            ),
            Offer(
                title='promo 2',
                fesh=11,
                hyperid=101,
                waremd5='WVe1mzsz2_ZcX2auywOU9w',
                promo=Promo(
                    start_date=datetime(1980, 1, 1),
                    promo_type=PromoType.GIFT_WITH_PURCHASE,
                    key='yB5yjZ1ML2NvBn-JzBSGLA',
                ),
            ),
            Offer(
                title='promo 4',
                fesh=11,
                hyperid=101,
                waremd5='hUElnbhS0nTF3o1F_CnqIQ',
                promo=Promo(
                    end_date=datetime(2050, 1, 1),
                    promo_type=PromoType.SECOND_OFFER_DISCOUNT,
                    key='qH_2eaLz5x2RgaZ7dUISLA',
                ),
            ),
            Offer(
                title='promo 5',
                fesh=11,
                hyperid=101,
                waremd5='HueLNBHs0nTF3o1f_cNQIQ',
                promo=Promo(promo_type=PromoType.SECOND_OFFER_FOR_FIXED_PRICE, key='Qh_2EAlZ5x2rGAZ7DuISlg'),
            ),
            Offer(title='no promo', fesh=11, hyperid=101, waremd5='RZ-TlPC81noIoyK1bbIN0w'),
            Offer(title='hyper_without_promos', fesh=11, hyperid=102),
            Offer(
                title='market_promo',
                fesh=12,
                hyperid=103,
                waremd5='26_iwgA17yd3FCI0LRh4_w',
                feedid=4242,
                offerid='424242',
                cpa=Offer.CPA_REAL,
                promo=Promo(
                    promo_type=PromoType.MARKET_MODEL_FOR_FIXED_PRICE,
                    key='xMpCOKC554INzFCab3WE2w',
                    active_regions=[214],
                    user_price=42.93,
                    products=[
                        PromoOffer(
                            md5='xMpCOKC554INzFCab3WE2w',
                            subsidy=42.42,
                            free_quantity=1,
                            offer_id='424242',
                            feed_id=4242,
                            model_id=103,
                            stock=50,
                            initial_stock=60,
                        )
                    ],
                ),
            ),
            Offer(
                title='region',
                fesh=12,
                hyperid=103,
                waremd5='28_iwgA17yd3FCI0LRh4_w',
                feedid=4242,
                offerid='424242',
                cpa=Offer.CPA_REAL,
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE,
                    key='MjhfaXdnQTE3eWQzRkNJMA',
                    active_regions=[214],
                    user_price=42.93,
                ),
            ),
        ]

    def test_promo_gift_factor(self):
        response = self.report.request_json('place=prime&text=promo&debug=da')
        self.assertFragmentIn(
            response,
            {
                'wareId': 'WVe1mzsz2_ZcX2auywOU9w',
                "debug": {
                    "factors": {
                        "GIFTS_WITH_PURCHASE_PROMO_TYPE": "1",
                        "N_PLUS_M_PROMO_TYPE": NoKey("N_PLUS_M_PROMO_TYPE"),
                        "PROMO_CODE_PROMO_TYPE": NoKey("PROMO_CODE_PROMO_TYPE"),
                    }
                },
            },
        )

    def test_n_plus_m_factor(self):
        response = self.report.request_json('place=prime&text=promo&debug=da')
        self.assertFragmentIn(
            response,
            {
                'wareId': 'X9_iwgA17yd3FCI0LRhC_w',
                "debug": {
                    "factors": {
                        "GIFTS_WITH_PURCHASE_PROMO_TYPE": NoKey("GIFTS_WITH_PURCHASE_PROMO_TYPE"),
                        "N_PLUS_M_PROMO_TYPE": "1",
                        "PROMO_CODE_PROMO_TYPE": NoKey("PROMO_CODE_PROMO_TYPE"),
                    }
                },
            },
        )

    def test_no_promo_factor(self):
        response = self.report.request_json('place=prime&text=promo&debug=da')
        self.assertFragmentIn(
            response,
            {
                'wareId': 'RZ-TlPC81noIoyK1bbIN0w',
                "debug": {
                    "factors": {
                        "GIFTS_WITH_PURCHASE_PROMO_TYPE": NoKey("GIFTS_WITH_PURCHASE_PROMO_TYPE"),
                        "N_PLUS_M_PROMO_TYPE": NoKey("N_PLUS_M_PROMO_TYPE"),
                        "PROMO_CODE_PROMO_TYPE": NoKey("PROMO_CODE_PROMO_TYPE"),
                    }
                },
            },
        )

    def test_offer_promo_info(self):
        # prime and productoffers
        urls = ['place=productoffers&hyperid=101', 'place=prime&text=promo']
        for url in urls:
            response = self.report.request_json(url)

            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': 'X9_iwgA17yd3FCI0LRhC_w',
                        'promos': [
                            {
                                'type': PromoType.N_PLUS_ONE,
                                'key': 'xMpCOKC5I4INzFCab3WEmw',
                                'startDate': '1980-01-01T00:00:00Z',
                                'endDate': '2050-01-01T00:00:00Z',
                                'parameters': {
                                    'requiredQuantity': 3,
                                    'freeQuantity': 34,
                                },
                            }
                        ],
                    }
                ],
            )
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': 'WVe1mzsz2_ZcX2auywOU9w',
                        'promos': [
                            {
                                'startDate': NotEmpty(),
                                'endDate': Absent(),
                                'type': PromoType.GIFT_WITH_PURCHASE,
                                'key': 'yB5yjZ1ML2NvBn-JzBSGLA',
                                'parameters': Absent(),
                            }
                        ],
                    }
                ],
            )
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': 'hUElnbhS0nTF3o1F_CnqIQ',
                        'promos': [
                            {
                                'startDate': Absent(),
                                'endDate': NotEmpty(),
                                'type': PromoType.SECOND_OFFER_DISCOUNT,
                                'key': 'qH_2eaLz5x2RgaZ7dUISLA',
                                'parameters': Absent(),
                            }
                        ],
                    }
                ],
            )
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': 'HueLNBHs0nTF3o1f_cNQIQ',
                        'promos': [
                            {
                                'startDate': Absent(),
                                'endDate': Absent(),
                                'type': PromoType.SECOND_OFFER_FOR_FIXED_PRICE,
                                'key': 'Qh_2EAlZ5x2rGAZ7DuISlg',
                                'extra': Absent(),
                                'parameters': Absent(),
                            }
                        ],
                    }
                ],
            )
            self.assertFragmentIn(
                response,
                [
                    {
                        'entity': 'offer',
                        'wareId': 'RZ-TlPC81noIoyK1bbIN0w',
                        'promos': Absent(),
                    }
                ],
            )

        # offerinfo
        url_fmt = 'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid={}'

        response = self.report.request_json(url_fmt.format('X9_iwgA17yd3FCI0LRhC_w'))
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'X9_iwgA17yd3FCI0LRhC_w',
                'promos': [
                    {
                        'type': PromoType.N_PLUS_ONE,
                        'key': 'xMpCOKC5I4INzFCab3WEmw',
                        'parameters': {
                            'requiredQuantity': 3,
                            'freeQuantity': 34,
                        },
                    }
                ],
            },
        )

        response = self.report.request_json(url_fmt.format('WVe1mzsz2_ZcX2auywOU9w'))
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'WVe1mzsz2_ZcX2auywOU9w',
                'promos': [
                    {
                        'type': PromoType.GIFT_WITH_PURCHASE,
                        'key': 'yB5yjZ1ML2NvBn-JzBSGLA',
                        'parameters': Absent(),
                    }
                ],
            },
        )

        response = self.report.request_json(url_fmt.format('hUElnbhS0nTF3o1F_CnqIQ'))
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'hUElnbhS0nTF3o1F_CnqIQ',
                'promos': [
                    {
                        'type': PromoType.SECOND_OFFER_DISCOUNT,
                        'key': 'qH_2eaLz5x2RgaZ7dUISLA',
                        'parameters': Absent(),
                    }
                ],
            },
        )

        response = self.report.request_json(url_fmt.format('HueLNBHs0nTF3o1f_cNQIQ'))
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'HueLNBHs0nTF3o1f_cNQIQ',
                'promos': [
                    {
                        'type': PromoType.SECOND_OFFER_FOR_FIXED_PRICE,
                        'key': 'Qh_2EAlZ5x2rGAZ7DuISlg',
                        'extra': NoKey('extra'),
                        'parameters': Absent(),
                    }
                ],
            },
        )

        response = self.report.request_json(url_fmt.format('RZ-TlPC81noIoyK1bbIN0w'))
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'RZ-TlPC81noIoyK1bbIN0w',
                'promos': Absent(),
            },
        )

    def test_offer_promo_info_same_region(self):
        """
        Вызываем плейсы prime, productoffers и offerinfo устанавливая регион пользователя равным региону промо-акции оффера
        и отфильтровывая интересующий нас оффер либо по модели (для productoffers), либо по тексту (для prime),
        либо по идентификатору (для offerinfo).

        Проверяем, что информация об оффере в выдаче, содержит в себе информацию об акции.
        """
        urls = [
            'place=productoffers&hyperid=103',
            'place=prime&text=region',
            'place=offerinfo&show-urls=&regset=1&pp=42&offerid=28_iwgA17yd3FCI0LRh4_w',
        ]
        for url in urls:
            url += "&rids=214"
            response = self.report.request_json(url)
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': '28_iwgA17yd3FCI0LRh4_w',
                    'promos': [
                        {
                            'type': PromoType.N_PLUS_ONE,
                            'key': 'MjhfaXdnQTE3eWQzRkNJMA',
                        }
                    ],
                },
            )

    @skip('see MARKETINDEXER-12793')
    def test_offer_promo_dynamic_end(self):
        """
        Сообщаем через быстрый пайплайн от Market Loyalty о полном завершении акции.

        Вызываем плейсы prime, productoffers и offerinfo устанавливая регион пользователя равным региону промо-акции оффера
        и отфильтровывая интересующий нас оффер либо по модели (для productoffers), либо по тексту (для prime),
        либо по идентификатору (для offerinfo).

        Проверяем, что информация об оффере в выдаче, но не содержит в себе информацию об акции.
        """
        # TODO : (a-anikina) redo this test for new dynamic promos when report is ready
        # self.dynamic.loyalty_promo_end += [
        #     DynamicLoyaltyPromoEnd(promo_key='xMpCOKC554INzFCab3WE2w')
        # ]

        urls = [
            'place=productoffers&hyperid=103',
            'place=prime&text=market_promo',
            'place=offerinfo&show-urls=&regset=1&pp=42&offerid=26_iwgA17yd3FCI0LRh4_w',
        ]
        for url in urls:
            url += "&rids=214"
            response = self.report.request_json(url)
            self.assertFragmentIn(response, {'entity': 'offer', 'wareId': '26_iwgA17yd3FCI0LRh4_w'})
            self.assertFragmentNotIn(
                response,
                {'promos': [{'type': PromoType.MARKET_MODEL_FOR_FIXED_PRICE, 'key': 'xMpCOKC554INzFCab3WE2w'}]},
            )

    def test_offer_promo_on_prime(self):
        """
        Делаем запрос к плейсу prime, и проверяем, что офферы с промо-акциями имеют раздел с информацией о промо-акции.
        Проверяем правильность информации. Для оффера без промо-акции проверяем, что информация о промо-акции отсутствует.
        """
        response = self.report.request_json('place=prime&text=promo')

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "X9_iwgA17yd3FCI0LRhC_w",
                "promos": [
                    {
                        "type": PromoType.N_PLUS_ONE,
                        "key": "xMpCOKC5I4INzFCab3WEmw",
                        "parameters": {"requiredQuantity": 3, "freeQuantity": 34},
                    }
                ],
            },
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "WVe1mzsz2_ZcX2auywOU9w",
                "promos": [
                    {
                        "type": PromoType.GIFT_WITH_PURCHASE,
                        "key": "yB5yjZ1ML2NvBn-JzBSGLA",
                    }
                ],
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                "entity": "offer",
                "wareId": "WVe1mzsz2_ZcX2auywOU9w",
                "promos": [
                    {
                        "parameters": {
                            'freeQuantity': NotEmpty(),
                        }
                    }
                ],
            },
        )

        self.assertFragmentNotIn(
            response, {"entity": "offer", "wareId": "owH81q5N8gG4NyrtNLDL7w", "promos": [{"parameters": {}}]}
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "hUElnbhS0nTF3o1F_CnqIQ",
                "promos": [
                    {
                        "type": PromoType.SECOND_OFFER_DISCOUNT,
                        "key": "qH_2eaLz5x2RgaZ7dUISLA",
                    }
                ],
            },
        )
        self.assertFragmentNotIn(
            response, {"entity": "offer", "wareId": "hUElnbhS0nTF3o1F_CnqIQ", "promos": [{"parameters": {}}]}
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "HueLNBHs0nTF3o1f_cNQIQ",
                "promos": [
                    {
                        "type": PromoType.SECOND_OFFER_FOR_FIXED_PRICE,
                        "key": "Qh_2EAlZ5x2rGAZ7DuISlg",
                    }
                ],
            },
        )
        self.assertFragmentNotIn(
            response, {"entity": "offer", "wareId": "HueLNBHs0nTF3o1f_cNQIQ", "promos": [{"parameters": {}}]}
        )

        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "RZ-TlPC81noIoyK1bbIN0w",
            },
        )
        self.assertFragmentNotIn(response, {"entity": "offer", "wareId": "RZ-TlPC81noIoyK1bbIN0w", "promos": [{}]})

    def test_offer_promo_info_same_region_on_prime(self):
        """
        Вызываем плейс prime устанавливая регион пользователя равным региону промо-акции оффера
        и отфильтровывая интересующий нас оффер по тексту.

        Проверяем, что информация об оффере в выдаче, содержит в себе информацию об акции.
        """
        response = self.report.request_json('place=prime&text=region&rids=214')
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': '28_iwgA17yd3FCI0LRh4_w',
                'promos': [
                    {
                        'type': PromoType.N_PLUS_ONE,
                        'key': 'MjhfaXdnQTE3eWQzRkNJMA',
                    }
                ],
            },
        )

    def test_offer_promo_filter(self):
        # prime and productoffers
        urls = ['place=prime&text=promo', 'place=productoffers&hyperid=101']
        for url in urls:
            response = self.report.request_json(url + '&promo-type={}'.format(PromoType.N_PLUS_ONE))
            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': Absent(),
                },
            )

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.GIFT_WITH_PURCHASE,
                        }
                    ],
                },
            )

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.SECOND_OFFER_DISCOUNT,
                        }
                    ],
                },
            )

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.SECOND_OFFER_FOR_FIXED_PRICE,
                        }
                    ],
                },
            )

            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': 'X9_iwgA17yd3FCI0LRhC_w',
                    'promos': [
                        {
                            'type': PromoType.N_PLUS_ONE,
                        }
                    ],
                },
            )

            response = self.report.request_json(url + '&promo-type={}'.format(PromoType.GIFT_WITH_PURCHASE))

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': Absent(),
                },
            )

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.N_PLUS_ONE,
                        }
                    ],
                },
            )

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.SECOND_OFFER_DISCOUNT,
                        }
                    ],
                },
            )

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.SECOND_OFFER_FOR_FIXED_PRICE,
                        }
                    ],
                },
            )

            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.GIFT_WITH_PURCHASE,
                        }
                    ],
                },
            )

            response = self.report.request_json(url + '&promo-type={}'.format(PromoType.SECOND_OFFER_DISCOUNT))

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': Absent(),
                },
            )

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.GIFT_WITH_PURCHASE,
                        }
                    ],
                },
            )

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.N_PLUS_ONE,
                        }
                    ],
                },
            )

            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.SECOND_OFFER_DISCOUNT,
                        }
                    ],
                },
            )

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.SECOND_OFFER_FOR_FIXED_PRICE,
                        }
                    ],
                },
            )

            response = self.report.request_json(url + '&promo-type={}'.format(PromoType.SECOND_OFFER_FOR_FIXED_PRICE))

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': NoKey('promos'),
                },
            )

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.N_PLUS_ONE,
                        }
                    ],
                },
            )

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.GIFT_WITH_PURCHASE,
                        }
                    ],
                },
            )

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.SECOND_OFFER_DISCOUNT,
                        }
                    ],
                },
            )

            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.SECOND_OFFER_FOR_FIXED_PRICE,
                        }
                    ],
                },
            )

            response = self.report.request_json(url + '&promo-type={}'.format(PromoType.ALL))

            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'promos': NoKey('promos'),
                },
            )

            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.N_PLUS_ONE,
                        }
                    ],
                },
            )

            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.GIFT_WITH_PURCHASE,
                        }
                    ],
                },
            )

            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.SECOND_OFFER_DISCOUNT,
                        }
                    ],
                },
            )

            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'promos': [
                        {
                            'type': PromoType.SECOND_OFFER_FOR_FIXED_PRICE,
                        }
                    ],
                },
            )

    def test_offer_promo_filter_same_region(self):
        """
        Вызываем плейсы prime и productoffers устанавливая регион пользователя равным региону промо-акции оффера
        и отфильтровывая интересующий нас оффер либо по модели (для productoffers), либо по тексту (для prime).
        Кроме этого устанавливаем фильтр по типу промо-акций, который мы и хотим проверить.

        Проверяем, что в выдаче содержится оффер с указанным в фильтре типом.
        """

        urls = ['place=prime&text=region', 'place=productoffers&hyperid=103']
        for url in urls:
            response = self.report.request_json(url + '&rids=214&promo-type={}'.format(PromoType.N_PLUS_ONE))
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': '28_iwgA17yd3FCI0LRh4_w',
                    'promos': [
                        {
                            'type': PromoType.N_PLUS_ONE,
                        }
                    ],
                },
            )

    def test_offer_promo_filter_on_prime(self):
        """
        Делаем запрос к плейсу prime с помощью поисковой строки выбирая набор офферов
        с различными типами промо-акций (и один без промо-акции).
        Запрос содержит фильтр по промо-акции n-plus-one.

        Проверяем, что в выдаче есть только оффер с промоакцией n-plus-one
        """

        response = self.report.request_json('place=prime&text=promo&promo-type={}'.format(PromoType.N_PLUS_ONE))
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "X9_iwgA17yd3FCI0LRhC_w",
                "promos": [
                    {
                        "type": PromoType.N_PLUS_ONE,
                    }
                ],
            },
        )
        self.assertFragmentNotIn(response, {"entity": "offer", "promos": [{"type": PromoType.GIFT_WITH_PURCHASE}]})
        self.assertFragmentNotIn(response, {"entity": "offer", "promos": [{"type": PromoType.SECOND_OFFER_DISCOUNT}]})
        self.assertFragmentNotIn(
            response, {"entity": "offer", "promos": [{"type": PromoType.SECOND_OFFER_FOR_FIXED_PRICE}]}
        )

    @classmethod
    def prepare_discount_filter_for_offers(cls):
        """
        Создаём два оффера в одной модели и с именем содержащим слово "discount". Один оффер со скидкой, а другой без.
        """

        cls.index.offers += [
            Offer(title='discount', fesh=11, hyperid=102, discount=50),
            Offer(title='no discount', fesh=11, hyperid=102),
        ]

    def test_discount_filter(self):
        """
        Делаем запрос с фильтром по скидке, и проверяем, что в выдаче присутствует оффер со скидкой и отсутствует оффер без скидки.

        Выполняем эти проверки для place=prime и place=productoffers
        """

        urls = ['place=prime&text=discount', 'place=productoffers&hyperid=102']
        for url in urls:
            response = self.report.request_json(url + '&filter-discount-only=1')
            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'prices': {
                        'discount': {
                            'percent': 0,
                        }
                    },
                },
            )

            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'prices': {
                        'discount': {
                            'percent': 50,
                        }
                    },
                },
            )

    def test_discount_filter_is_off(self):
        """
        Делаем запрос без фильтра по скидке, и проверяем, что в выдаче присутствуют оба оффера.

        Выполняем эти проверки для place=prime и place=productoffers
        """

        urls = ['place=prime&text=discount', 'place=productoffers&hyperid=102']
        for url in urls:
            response = self.report.request_json(url + '')
            self.assertFragmentIn(response, {'entity': 'offer', 'prices': {'discount': Absent()}})

            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'prices': {
                        'discount': {
                            'percent': 50,
                        }
                    },
                },
            )

    @classmethod
    def prepare_discount_filter_for_models(cls):
        """
        1. Создаём два оффера в одной модели. Один со скидкой, а второй без.
        2. Создаём один оффер без скидки во второй модели.
        Оба оффера сожержат слово "discount".
        """

        cls.index.models += [
            Model(hyperid=200, title='discountmodel'),
            Model(hyperid=201, title='no discountmodel'),
        ]

        cls.index.offers += [
            Offer(
                fesh=11,
                hyperid=200,
                discount=50,
                price=50,
                title="discountmodel_offer1",
            ),
            Offer(
                fesh=11,
                hyperid=200,
                price=100,
                title="discountmodel_offer2",
            ),
            Offer(
                fesh=11,
                hyperid=201,
                price=100,
                title="no discountmodel_offer2",
            ),
        ]

    def test_discount_filter_for_models(self):
        """
        Делаем запрос с фильтром по скидке для place=prime

        Проверяем, что в выдаче присутствует модель со скидкой и отсутствует модель без скидки.
        """

        response = self.report.request_json('place=prime&text=discountmodel&filter-discount-only=1&allow-collapsing=1')

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'type': 'model',
                'id': 201,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'type': 'model',
                'id': 200,
            },
        )

    def test_discount_filter_for_models_is_off(self):
        """
        Делаем запрос без фильтра по скидке для place=prime

        Проверяем, что в выдаче присутствуют обе модели.
        """

        response = self.report.request_json('place=prime&text=discountmodel')
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'type': 'model',
                'id': 201,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'type': 'model',
                'id': 200,
            },
        )

    def test_dont_use_discounts_for_models_experiment_with_discount_filter(self):
        """
        Проверяем работу эксперимента по отключению обработки скидок для моделей
        Делаем запрос с фильтром по скидке и установленным флагом эксперимента для плейсов prime

        Проверяем, что в выдаче ОТсутствуют обе модели.
        """

        response = self.report.request_json(
            'place=prime&text=discountmodel&filter-discount-only=1&rearr-factors=market_dont_use_discounts_for_models=1'
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'type': 'model',
                'id': 201,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'type': 'model',
                'id': 200,
            },
        )

    def test_dont_use_discounts_for_models_experiment_without_discount_filter(self):
        """
        Проверяем работу эксперимента по отключению обработки скидок для моделей
        Делаем запрос с установленным флагом эксперимента для плейсов prime

        Проверяем, что в выдаче ПРИсутствуют обе модели.
        Проверяем, что у моделей отсутствует информация по скидкам, хотя у одной из них есть скидка.
        Проверяем, что в статистике по фильтру указано нулевое количество объектов проходящих фильтр.
        Проверяем, что из карточки убирано количество оферов со скидкой
        """

        response = self.report.request_json(
            'place=prime&text=discountmodel&rearr-factors=market_dont_use_discounts_for_models=1'
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'type': 'model',
                'id': 201,
                'prices': {'discount': Absent()},
                'offers': {
                    # 'count': 1, see MARKETOUT-15201
                    'discountCount': Absent()
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'type': 'model',
                'id': 200,
                'prices': {'discount': Absent()},
                'offers': {
                    # 'count': 2, see MARKETOUT-15201'
                    'discountCount': Absent()
                },
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'id': 'filter-discount-only',
            },
        )

    def test_dont_use_discounts_for_models_experiment_for_modelinfo(self):
        """
        Проверяем работу эксперимента по отключению обработки скидок для моделей
        Делаем запрос с установленным флагом эксперимента для плейсов modelinfo
        с фильтром по модели со скидкой

        Проверяем, что у модели отсутствует информация по скидкам.
        """

        response = self.report.request_json(
            'place=modelinfo&rearr-factors=market_dont_use_discounts_for_models=1&hyperid=200&rids=213'
        )

        self.assertFragmentIn(
            response, {'entity': 'product', 'type': 'model', 'id': 200, 'prices': {'discount': Absent()}}
        )

    @classmethod
    def prepare_filter_discount_only_for_clusters(cls):
        """
        1. Создаём два оффера в одном кластере. Один со скидкой, а второй без.
        2. Создаём один оффер без скидки во втором кластере.
        Оба оффера сожержат слово "discount".
        """

        cls.index.vclusters += [
            VCluster(vclusterid=1000000001, title='discountcluster'),
            VCluster(vclusterid=1000000002, title='no discountcluster'),
        ]

        cls.index.offers += [
            Offer(
                fesh=11,
                vclusterid=1000000001,
                discount=50,
                price=50,
                title='discountcluster_offer1',
            ),
            Offer(
                fesh=11,
                vclusterid=1000000001,
                price=100,
                title='discountcluster_offer2',
            ),
            Offer(
                fesh=11,
                vclusterid=1000000002,
                price=100,
                title='no discountcluster_offer3',
            ),
        ]

    def test_offer_discount_filter_for_clusters(self):
        """
        Делаем запрос с фильтром по скидке, и проверяем, что в выдаче присутствует кластер со скидкой и отсутствует кластер без скидки.

        Выполняем проверку для place=prime
        """

        response = self.report.request_json(
            'place=prime&text=discountcluster&filter-discount-only=1&allow-collapsing=1'
        )
        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'type': 'cluster',
                'id': 1000000002,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'type': 'cluster',
                'id': 1000000001,
            },
        )

    def test_offer_discount_filter_for_clusters_is_off(self):
        """
        Делаем запрос без фильтра по скидке, и проверяем, что в выдаче присутствуют оба кластера.

        Выполняем проверку для place=prime
        """

        response = self.report.request_json('place=prime&text=discountcluster')
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'type': 'cluster',
                'id': 1000000002,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'type': 'cluster',
                'id': 1000000001,
            },
        )

    @classmethod
    def prepare_offer_promo_filter_for_models(cls):
        cls.index.models += [
            Model(title="model 1", hyperid=301),
            Model(title="model 2", hyperid=302),
            Model(title="model 3", hyperid=303),
            Model(title="model 4", hyperid=304),
            Model(title="model 5", hyperid=305),
            Model(title="model 6", hyperid=306),
            Model(title="model 7", hyperid=307),
        ]

        cls.index.offers += [
            Offer(fesh=11, hyperid=301, promo=Promo(promo_type=PromoType.N_PLUS_ONE, key='xMpCOKC5I4INzFCab3WEm1')),
            Offer(
                fesh=11, hyperid=302, promo=Promo(promo_type=PromoType.GIFT_WITH_PURCHASE, key='yB5yjZ1ML2NvBn-JzBSGL2')
            ),
            Offer(
                fesh=11,
                hyperid=304,
                promo=Promo(promo_type=PromoType.SECOND_OFFER_DISCOUNT, key='qH_2eaLz5x2RgaZ7dUISL4'),
            ),
            Offer(
                fesh=11,
                hyperid=307,
                promo=Promo(promo_type=PromoType.SECOND_OFFER_FOR_FIXED_PRICE, key='wH_2eaLz5x2RgaZ7dUISLw'),
            ),
            Offer(fesh=11, hyperid=305),
            Offer(
                fesh=11,
                hyperid=306,
                promo=Promo(promo_type=PromoType.SECOND_OFFER_DISCOUNT, key='11_2eaLrrx2RgaZ7dUISLA'),
            ),
            Offer(
                fesh=11, hyperid=306, promo=Promo(promo_type=PromoType.GIFT_WITH_PURCHASE, key='225yjZ1ML2Nttn-JzBSGLA')
            ),
        ]

    def test_offer_promo_filter_for_models_n_plus_one(self):
        response = self.report.request_json('place=prime&text=model&promo-type={}'.format(PromoType.N_PLUS_ONE))

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 301,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 302,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 303,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 304,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 305,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 306,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 307,
            },
        )

    def test_offer_promo_filter_for_models_gift_with_purchase(self):
        response = self.report.request_json('place=prime&text=model&promo-type={}'.format(PromoType.GIFT_WITH_PURCHASE))

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 301,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 302,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 303,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 304,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 305,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 306,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 307,
            },
        )

    def test_offer_promo_filter_for_models_second_offer_discount(self):
        response = self.report.request_json(
            'place=prime&text=model&promo-type={}'.format(PromoType.SECOND_OFFER_DISCOUNT)
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 301,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 302,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 303,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 304,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 305,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 306,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 307,
            },
        )

    def test_offer_promo_filter_for_models_second_offer_for_fixed_price(self):
        response = self.report.request_json(
            'place=prime&text=model&promo-type={}'.format(PromoType.SECOND_OFFER_FOR_FIXED_PRICE)
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 301,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 302,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 303,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 304,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 305,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 306,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 307,
            },
        )

    @skip('see MARKETOUT-15201')
    def test_model_has_promo_count(self):
        """
        Проверяем, что на плейсе prime, для модели выводятся данные о количестве акций.
        Для этого запрашиваем поиск по тексту, который содержится в модели с двумя акциями, и проверяем,
        что счётчик равен двум. Затем то же самое делаем для модели с одной акцией
        """
        response = self.report.request_json('place=prime&text=model%206')

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'promos': [
                    {
                        'count': 2,
                    }
                ],
            },
        )

        response = self.report.request_json('place=prime&text=model%204')

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'promos': [
                    {
                        'count': 1,
                    }
                ],
            },
        )

    @skip('see MARKETOUT-15201')
    def test_model_has_no_promo_info_for_zero_promo_count(self):
        """
        Проверяем, что для моделей с нулевым количеством акций, секция promo не выводится
        Для этого запрашиваем поиск по тексту, который содержится в модели без акций, и проверяем,
        что счётчик равен нулю.
        """
        response = self.report.request_json('place=prime&text=model%205')

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'promos': Absent(),
            },
        )

    @classmethod
    def prepare_offer_promo_filter_for_clusters(cls):
        """
        Создаём набор кластеров: по кластеру на каждый из типов акции, кластер без акций и кластер с двумя разными акциями.
        Кластера сожержат слово "cluster".
        """

        cls.index.vclusters += [
            VCluster(vclusterid=1100000001, title='cluster 1'),
            VCluster(vclusterid=1100000002, title='cluster 2'),
            VCluster(vclusterid=1100000003, title='cluster 3'),
            VCluster(vclusterid=1100000004, title='cluster 4'),
            VCluster(vclusterid=1100000005, title='cluster 5'),
            VCluster(vclusterid=1100000006, title='cluster 6'),
        ]

        cls.index.offers += [
            Offer(
                fesh=11,
                vclusterid=1100000001,
                promo=Promo(promo_type=PromoType.N_PLUS_ONE, key='xMpCOKC5I4INzFCab3WEm2'),
            ),
            Offer(
                fesh=11,
                vclusterid=1100000002,
                promo=Promo(promo_type=PromoType.GIFT_WITH_PURCHASE, key='yB5yjZ1ML2NvBn-JzBSGL3'),
            ),
            Offer(
                fesh=11,
                vclusterid=1100000004,
                promo=Promo(promo_type=PromoType.SECOND_OFFER_DISCOUNT, key='qH_2eaLz5x2RgaZ7dUISL5'),
            ),
            Offer(
                fesh=11,
                vclusterid=1100000007,
                promo=Promo(promo_type=PromoType.SECOND_OFFER_FOR_FIXED_PRICE, key='qH_2eaLz5x2RgaZ7dUISL6'),
            ),
            Offer(fesh=11, vclusterid=1100000005),
            Offer(
                fesh=11,
                vclusterid=1100000006,
                promo=Promo(promo_type=PromoType.SECOND_OFFER_DISCOUNT, key='11_2eaLrrx2RgaZ7dUISL7'),
            ),
            Offer(
                fesh=11,
                vclusterid=1100000006,
                promo=Promo(promo_type=PromoType.GIFT_WITH_PURCHASE, key='225yjZ1ML2Nttn-JzBSGL8'),
            ),
        ]

    @classmethod
    def prepare_promo_blocking(cls):
        """
        Создаём офферы для тестирования быстрой блокировки. Один имеет промо-акцию без блокировок, второй
        с блокировкой по промо, третий с блокировкой по главному офферу. Еще два оффера имеют общую промо-акцию,
        но один из них в ней заблокирован, а второй нет. Офферы содержат в имени слово
        blocking и имеют отдельный набор hyperid для тестирования быстрой блокировки.
        """

        cls.index.promos += [
            Promo(promo_type=PromoType.SECOND_OFFER_FOR_FIXED_PRICE, key='Qh_2EAlZ5x2rGAZ7Ddddww'),
            Promo(promo_type=PromoType.SECOND_OFFER_FOR_FIXED_PRICE, key='HcccNBHs0nTF3o1f_cNbbQ'),
        ]

        cls.index.offers += [
            Offer(
                title='blockingnone',
                hyperid=104,
                waremd5='HaaaNBHs0nTF3o1f_cNbbQ',
                promo=Promo(promo_type=PromoType.SECOND_OFFER_FOR_FIXED_PRICE, key='Qh_2EAlZ5x2rGAZ7Daaalg'),
            ),
            Offer(
                title='blockingbypromo',
                hyperid=105,
                waremd5='HbbbNBHs0nTF3o1f_cNccQ',
                promo=Promo(promo_type=PromoType.SECOND_OFFER_FOR_FIXED_PRICE, key='Qh_2EAlZ5x2rGAZ7Deeelg'),
            ),
            Offer(
                title='blockingbymainoffer',
                hyperid=106,
                waremd5='HcccNBHs0nTF3o1f_cNddQ',
                promo=Promo(promo_type=PromoType.SECOND_OFFER_FOR_FIXED_PRICE, key='Qh_2EAlZ5x2rGAZ7Ddddlg'),
            ),
            Offer(
                title='blockingtwomainoffers 1',
                hyperid=107,
                waremd5='HdddNBHs0nTF3o1f_cNeeQ',
                promo_key='HcccNBHs0nTF3o1f_cNbbQ',
            ),
            Offer(
                title='blockingtwomainoffers 2',
                hyperid=107,
                waremd5='HeeeNBHs0nTF3o1f_cNffQ',
                promo_key='HcccNBHs0nTF3o1f_cNbbQ',
            ),
        ]

    def test_offer_promo_filter_blocking_none(self):
        """
        Вызываем плейсы prime и productoffers. Тестируем одиночный оффер без блокировки
        Отфильтровываем интересующий нас оффер либо по модели (для productoffers), либо по тексту (для prime).
        Кроме этого устанавливаем фильтр по типу промо-акций, который мы и хотим проверить.

        Проверяем, что в выдаче содержится данный оффер.
        """

        urls = ['place=prime&text=blockingnone', 'place=productoffers&hyperid=104']
        for url in urls:
            response = self.report.request_json(url + '&promo-type={}'.format(PromoType.SECOND_OFFER_FOR_FIXED_PRICE))
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': 'HaaaNBHs0nTF3o1f_cNbbQ',
                },
            )

    def test_offer_promo_filter_blocking_by_promo(self):
        """
        Вызываем плейсы prime и productoffers. Тестируем блокировку промо-акции целиком.
        Этот кейс должен отличаться от кейса test_offer_promo_filter_blocking_none только
        наличием блокировки у промо-акции оффера и проверкой.
        Отфильтровываем интересующий нас оффер либо по модели (для productoffers), либо по тексту (для prime).
        Кроме этого устанавливаем фильтр по типу промо-акции.

        Проверяем, что в выдаче не содержится офферов.
        """

        self.dynamic.market_dynamic.disabled_promos += [DynamicPromo(promo_key='Qh_2EAlZ5x2rGAZ7Deeelg')]

        urls = ['place=prime&text=blockingbypromo', 'place=productoffers&hyperid=105']
        for url in urls:
            response = self.report.request_json(url + '&promo-type={}'.format(PromoType.SECOND_OFFER_FOR_FIXED_PRICE))
            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                },
            )

    def test_offer_promo_filter_blocking_by_main_offer(self):
        """
        Вызываем плейсы prime и productoffers. Тестируем блокировку главного оффера.
        Этот кейс должен отличаться от кейса test_offer_promo_filter_blocking_none только
        наличием блокировки у промо-акции и проверкой.
        Отфильтровываем интересующий нас оффер либо по модели (для productoffers), либо по тексту (для prime).
        Кроме этого устанавливаем фильтр по типу промо-акции.

        Проверяем, что в выдаче не содержится офферов.
        """

        self.dynamic.market_dynamic.disabled_promos += [
            DynamicPromo(promo_key='Qh_2EAlZ5x2rGAZ7Ddddlg', offer_ids=['HcccNBHs0nTF3o1f_cNddQ'])
        ]

        urls = ['place=prime&text=blockingbymainoffer', 'place=productoffers&hyperid=106']
        for url in urls:
            response = self.report.request_json(url + '&promo-type={}'.format(PromoType.SECOND_OFFER_FOR_FIXED_PRICE))
            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                },
            )

    def test_offer_promo_filter_blocking_two_main_offers(self):
        """
        Вызываем плейсы prime и productoffers. Тестируем блокировку одного главного оффера из двух.
        Отфильтровываем интересующие нас офферы либо по модели (для productoffers), либо по тексту (для prime).
        Кроме этого устанавливаем фильтр по типу промо-акции.

        Проверяем, что в выдаче содержится только незаблокированный оффер.
        """

        self.dynamic.market_dynamic.disabled_promos += [
            DynamicPromo(promo_key='Qh_2EAlZ5x2rGAZ7Ddddww', offer_ids=['HcccNBHs0nTF3o1f_cNbbQ']),
            DynamicPromo(promo_key='HcccNBHs0nTF3o1f_cNbbQ', offer_ids=['HdddNBHs0nTF3o1f_cNeeQ']),
        ]

        urls = ['place=prime&text=blockingtwomainoffers', 'place=productoffers&hyperid=107']
        for url in urls:
            response = self.report.request_json(url + '&promo-type={}'.format(PromoType.SECOND_OFFER_FOR_FIXED_PRICE))
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': 'HeeeNBHs0nTF3o1f_cNffQ',
                },
            )
            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': 'HdddNBHs0nTF3o1f_cNeeQ',
                },
            )

    def test_offer_promo_info_blocking_none(self):
        """
        Вызываем плейсы prime и productoffers. Тестируем одиночный оффер без блокировки
        Отфильтровываем интересующий нас оффер либо по модели (для productoffers), либо по тексту (для prime).

        Проверяем, что в выдаче содержится информация о промо-акции оффера.
        """

        urls = [
            'place=prime&text=blockingnone',
            'place=productoffers&hyperid=104',
            'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid=HaaaNBHs0nTF3o1f_cNbbQ',
        ]
        for url in urls:
            response = self.report.request_json(url)
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': 'HaaaNBHs0nTF3o1f_cNbbQ',
                    'promos': [
                        {
                            'type': PromoType.SECOND_OFFER_FOR_FIXED_PRICE,
                            'key': 'Qh_2EAlZ5x2rGAZ7Daaalg',
                        }
                    ],
                },
            )

    def test_offer_promo_info_blocking_by_promo(self):
        """
        Вызываем плейсы prime и productoffers. Тестируем одиночный оффер с блокировкой по промо-акции
        Отфильтровываем интересующий нас оффер либо по модели (для productoffers), либо по тексту (для prime).

        Проверяем, что в выдаче НЕ содержится информация о промо-акции оффера.
        """

        self.dynamic.market_dynamic.disabled_promos += [DynamicPromo(promo_key='Qh_2EAlZ5x2rGAZ7Deeelg')]

        urls = [
            'place=prime&text=blockingbypromo',
            'place=productoffers&hyperid=105',
            'place=offerinfo&rids=213&show-urls=&regset=1&pp=42&offerid=HbbbNBHs0nTF3o1f_cNccQ',
        ]
        for url in urls:
            response = self.report.request_json(url)
            self.assertFragmentIn(response, {'entity': 'offer', 'wareId': 'HbbbNBHs0nTF3o1f_cNccQ', 'promos': Absent()})

    def test_offer_promo_blocking_none_on_prime(self):
        """
        Делаем запрос к плейсу prime, выбирая с помощью поисковой строки оффер без блокировок

        Проверяем, что оффер имеет раздел с информацией о промо-акции.
        """
        response = self.report.request_json('place=prime&text=blockingnone')
        self.assertFragmentIn(response, {"entity": "offer", "wareId": "HaaaNBHs0nTF3o1f_cNbbQ", "promos": []})

    def test_offer_promo_blocking_by_promo_on_prime(self):
        """
        Делаем запрос к плейсу prime, выбирая с помощью поисковой строки оффер с блокировкой

        Проверяем, что оффер в выдаче есть и что у него нет раздела с информацией о промо-акции.
        """
        self.dynamic.market_dynamic.disabled_promos += [DynamicPromo(promo_key='Qh_2EAlZ5x2rGAZ7Deeelg')]

        response = self.report.request_json('place=prime&text=blockingbypromo')
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": "HbbbNBHs0nTF3o1f_cNccQ",
            },
        )
        self.assertFragmentNotIn(response, {"entity": "offer", "wareId": "HbbbNBHs0nTF3o1f_cNccQ", "promos": []})

    def test_offer_promo_blocking_by_promo_on_prime_is_off(self):
        """
        Делаем запрос к плейсу prime, выбирая с помощью поисковой строки оффер с блокировкой и добавляя параметр выключения
        динамических фильтров

        Проверяем, что оффер имеет раздел с информацией о промо-акции.
        """
        self.dynamic.market_dynamic.disabled_promos += [DynamicPromo(promo_key='Qh_2EAlZ5x2rGAZ7Deeelg')]

        response = self.report.request_json('place=prime&text=blockingbypromo&dynamic-filters=no')
        self.assertFragmentIn(response, {"entity": "offer", "wareId": "HbbbNBHs0nTF3o1f_cNccQ", "promos": []})

    @classmethod
    def prepare_model_promo_region(cls):
        cls.index.regional_models += [
            RegionalModel(hyperid=210, offers=100, rids=[213], promo_count=1, promo_types=[PromoType.N_PLUS_ONE]),
            RegionalModel(hyperid=211, offers=100, rids=[214], promo_count=2, promo_types=[PromoType.N_PLUS_ONE]),
            RegionalModel(
                hyperid=213,
                offers=100,
                rids=[213, 214],
                promo_count=4,
                white_promo_count=5,
                promo_types=[PromoType.N_PLUS_ONE],
            ),
        ]

        cls.index.models += [
            Model(hyperid=210, title='modelregion'),
            Model(hyperid=211, title='modelregion'),
            Model(hyperid=212, title='modelregion'),
            Model(hyperid=213, title='modelregion'),
        ]

        # for onstock
        cls.index.offers += [
            Offer(
                title='onstock 1',
                fesh=11,
                hyperid=210,
                waremd5='X9_qqqA17yd3FCI0LRhC_w',
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='qqqCOKC5I4INzFCab3WEmw',
                    required_quantity=3,
                    free_quantity=34,
                ),
            ),
            Offer(
                title='onstock 2',
                fesh=12,
                hyperid=213,
                waremd5='X9_zzzA17yd3FCI0LRhC_w',
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='zzzCOKC5I4INzFCab3WEmw',
                    required_quantity=3,
                    free_quantity=34,
                ),
            ),
            Offer(
                title='onstock 3',
                fesh=12,
                hyperid=211,
                waremd5='X9_xqxA17yd3FCI0LRhC_w',
                promo=Promo(
                    promo_type=PromoType.N_PLUS_ONE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='xqxCOKC5I4INzFCab3WEmw',
                    required_quantity=3,
                    free_quantity=34,
                ),
            ),
        ]

    @skip('see MARKETOUT-15201')
    def test_model_promo_info_region(self):
        """
        Вызываем плейс prime устанавливая регион пользователя равным одному из двух регионов, для которых у модели есть статистика по
        промо-акциям. Отфильтровываем интересующую нас модель по тексту.

        Проверяем, что информация о моделя в выдаче, содержит в себе информацию об акции в соответствии с регионом.
        """
        response = self.report.request_json('place=prime&text=modelregion&rids=213')

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 210,
                'promos': [
                    {
                        'count': 1,
                    }
                ],
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 211,
                'promos': [
                    {
                        'count': 2,
                    }
                ],
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 213,
                'promo': {'count': 4, 'whitePromoCount': 5},
            },
        )

        response = self.report.request_json('place=prime&text=modelregion&rids=214')

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 210,
                'promos': [
                    {
                        'count': 1,
                    }
                ],
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 211,
                'promos': [
                    {
                        'count': 2,
                    }
                ],
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 213,
                'promo': {
                    'count': 4,
                    'whitePromoCount': 5,
                },
            },
        )

    def test_remove_promo_flag_set_whitepromo_zero(self):
        response = self.report.request_json(
            'place=prime&text=modelregion&rids=214&rearr-factors=market_remove_promos=1'
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 213,
                'promo': {
                    'whitePromoCount': 0,
                },
            },
        )

    def test_model_promo_filter_region(self):
        """
        Вызываем плейс prime устанавливая регион пользователя равным одному из двух регионов, для которых у модели есть статистика по
        промо-акциям. Отфильтровываем интересующую нас модель по тексту. Дополнительно ставим фильтр по типу промо-акции

        Проверяем, что информация в выдаче есть только те модели, регион которых соответствует параметру rids.
        """
        response = self.report.request_json(
            'place=prime&text=modelregion&rids=213&promo-type={}'.format(PromoType.N_PLUS_ONE)
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 210,
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 211,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 213,
            },
        )

        response = self.report.request_json(
            'place=prime&text=modelregion&rids=214&promo-type={}'.format(PromoType.N_PLUS_ONE)
        )

        self.assertFragmentNotIn(
            response,
            {
                'entity': 'product',
                'id': 210,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 211,
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 213,
            },
        )

    def test_offer_promo_clicks(self):
        """
        Запрашиваем на прайме все офферы с разными видами промо-акций.
        В запрос добавляем параметр, который заставляет репорт выводить клики.

        Проверяем, что в логе кликов появилась правильная информация о типах акций, а для оффера без акции её нет.
        """
        _ = self.report.request_json('place=prime&text=promo&show-urls=offercard')

        self.click_log.expect(
            clicktype=8, ware_md5='X9_iwgA17yd3FCI0LRhC_w', promo_type=PromoType.MASK_BY_NAME[PromoType.N_PLUS_ONE]
        )
        self.click_log.expect(
            clicktype=8,
            ware_md5='WVe1mzsz2_ZcX2auywOU9w',
            promo_type=PromoType.MASK_BY_NAME[PromoType.GIFT_WITH_PURCHASE],
        )
        self.click_log.expect(
            clicktype=8,
            ware_md5='hUElnbhS0nTF3o1F_CnqIQ',
            promo_type=PromoType.MASK_BY_NAME[PromoType.SECOND_OFFER_DISCOUNT],
        )
        self.click_log.expect(
            clicktype=8,
            ware_md5='HueLNBHs0nTF3o1f_cNQIQ',
            promo_type=PromoType.MASK_BY_NAME[PromoType.SECOND_OFFER_FOR_FIXED_PRICE],
        )
        self.click_log.expect(clicktype=8, ware_md5='RZ-TlPC81noIoyK1bbIN0w', promo_type=0)

    def test_offer_promo_clicks_reverse_exp(self):
        """
        Запрашиваем на прайме все офферы с разными видами промо-акций.
        В запрос добавляем параметр, который заставляет репорт выводить клики.

        Проверяем, что в логе кликов появилась правильная информация о типах акций, а для оффера без акции её нет.
        """
        _ = self.report.request_json('place=prime&text=promo&show-urls=offercard&rearr-factors=market_remove_promos=1')

        self.click_log.expect(
            clicktype=8, ware_md5='X9_iwgA17yd3FCI0LRhC_w', promo_type=PromoType.MASK_BY_NAME[PromoType.N_PLUS_ONE]
        )
        self.click_log.expect(
            clicktype=8,
            ware_md5='WVe1mzsz2_ZcX2auywOU9w',
            promo_type=PromoType.MASK_BY_NAME[PromoType.GIFT_WITH_PURCHASE],
        )
        self.click_log.expect(
            clicktype=8,
            ware_md5='hUElnbhS0nTF3o1F_CnqIQ',
            promo_type=PromoType.MASK_BY_NAME[PromoType.SECOND_OFFER_DISCOUNT],
        )
        self.click_log.expect(
            clicktype=8,
            ware_md5='HueLNBHs0nTF3o1f_cNQIQ',
            promo_type=PromoType.MASK_BY_NAME[PromoType.SECOND_OFFER_FOR_FIXED_PRICE],
        )
        self.click_log.expect(clicktype=8, ware_md5='RZ-TlPC81noIoyK1bbIN0w', promo_type=0)

    @classmethod
    def prepare_promo_activation_filter(cls):
        cls.index.shops += [
            Shop(fesh=13, priority_region=213, promo_cpc_status='real'),
            Shop(fesh=14, priority_region=213, promo_cpc_status='off'),
            Shop(fesh=15, priority_region=213, promo_cpc_status='sandbox'),
            Shop(fesh=16, priority_region=213, promo_cpc_status='no'),  # same as 'off'
        ]
        cls.index.offers += [
            Offer(
                title='activation real',
                fesh=13,
                waremd5='AA_11gA17yd3FCI0LR221w',
                promo=Promo(promo_type=PromoType.N_PLUS_ONE, key='xMpCOKC6I4INzFCab3WEmw'),
            ),
            Offer(
                title='activation off',
                fesh=14,
                waremd5='AA335gA17yd3FCI0LR221w',
                promo=Promo(promo_type=PromoType.N_PLUS_ONE, key='xMpCOKC7I4INzFCab3WEmw'),
            ),
            Offer(
                title='activation no',
                fesh=14,
                waremd5='AB335gA17yd3FCI0LR221w',
                promo=Promo(promo_type=PromoType.N_PLUS_ONE, key='yMpCOKC7I4INzFCab3WEmw'),
            ),
            Offer(
                title='activation sandbox',
                fesh=15,
                waremd5='22911gA17yd3FCI0LR221w',
                promo=Promo(promo_type=PromoType.N_PLUS_ONE, sandbox=True, key='xMpCOKC8I4INzFCab3WEmw'),
            ),
            Offer(
                title='bad activation sandbox',
                fesh=15,
                waremd5='33911gA17yd3FCI0LR221w',
                promo=Promo(promo_type=PromoType.N_PLUS_ONE, key='XMpCOKC8I4INzFCab3WEmw'),
            ),
            Offer(
                title='bad activation sandbox',
                fesh=14,
                waremd5='44911gA17yd3FCI0LR221w',
                promo=Promo(promo_type=PromoType.N_PLUS_ONE, sandbox=True, key='XXpCOKC8I4INzFCab3WEmw'),
            ),
        ]

    def check_that_promo_info_is_present(self, response, waremd5):
        self.assertFragmentIn(
            response, {"entity": "offer", "wareId": waremd5, "promos": [{"type": PromoType.N_PLUS_ONE}]}
        )

    def check_that_promo_info_is_absent(self, response, waremd5):
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "wareId": waremd5,
            },
        )
        self.assertFragmentNotIn(response, {"entity": "offer", "wareId": waremd5, "promos": []})

    def test_offer_promo_on_prime_promo_activation_status_default(self):
        """
        Делаем запрос к плейсу prime, выбирая набор офферов с различными статусами активации с помощью поисковой строки
        Проверяем, что оффер со статусом 'real' имеет раздел с информацией о промо-акции, а остальные нет.
        """
        response = self.report.request_json('place=prime&text=activation')
        self.check_that_promo_info_is_present(response, 'AA_11gA17yd3FCI0LR221w')
        self.check_that_promo_info_is_absent(response, 'AA335gA17yd3FCI0LR221w')
        self.check_that_promo_info_is_absent(response, 'AB335gA17yd3FCI0LR221w')
        self.check_that_promo_info_is_absent(response, '22911gA17yd3FCI0LR221w')
        self.check_that_promo_info_is_absent(response, '33911gA17yd3FCI0LR221w')
        self.check_that_promo_info_is_absent(response, '44911gA17yd3FCI0LR221w')


if __name__ == '__main__':
    main()
