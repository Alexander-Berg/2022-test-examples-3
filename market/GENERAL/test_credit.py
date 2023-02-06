#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    CreditTemplate,
    CreditTemplateOrganizationType,
    DeliveryBucket,
    DynamicShop,
    GpsCoord,
    HyperCategory,
    HyperCategoryType,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
)
from core.matcher import NotEmpty
from core.matcher import NoKey
from core.types.offer_features import Features
from unittest import skip


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(
                rid=1,
                name='MO',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(
                        rid=213,
                        name='Москва',
                        children=[
                            Region(rid=1010, name='VAO'),
                        ],
                    ),
                ],
            ),
            Region(rid=75, name='Gorod2', children=[Region(rid=10, name='derevnya')]),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=198119,
                name="Electronics",
                children=[
                    HyperCategory(hid=91491, name="Mobile telephones", output_type=HyperCategoryType.GURU),
                    HyperCategory(hid=9000, name="Mobile telephones 2"),
                    HyperCategory(hid=98000, name="Credit test offers", output_type=HyperCategoryType.GURU),
                ],
            ),
            HyperCategory(hid=10470548, name="category_bad"),
        ]

        cls.index.shops += [
            Shop(fesh=123456, name="credit_shop_1", regions=[213, 75, 11], priority_region=213, pickup_buckets=[5001]),
            Shop(fesh=1234567, name="credit_shop_3", regions=[213, 75, 11], priority_region=213, pickup_buckets=[5001]),
            Shop(fesh=654321, name="credit_shop_2", regions=[75, 213], priority_region=75),
            Shop(fesh=100500, name="shop_with_mfo_credit", regions=[213], priority_region=213),
            Shop(fesh=10000, name="no_credit_shop", regions=[213, 75], priority_region=213, pickup_buckets=[5002]),
            Shop(fesh=999, name="blocked_credit_shop", regions=[213], priority_region=213),
            Shop(fesh=998, name="no blocked shop without credit", regions=[213], priority_region=213),
            Shop(fesh=991, regions=[213], priority_region=213),
        ]

        cls.index.models += [
            Model(hid=91491, hyperid=1, title='Model with creditt'),
            Model(hid=91491, hyperid=2, title='Model without creditt'),
        ]

        cls.index.credit_templates += [
            CreditTemplate(template_id=1, bank="Sber", url="sber.ru", term=24, rate=5.0),
            CreditTemplate(template_id=2, bank="Tinkoff", url="tinkoff.ru", term=24, rate=2, max_price=8000),
            CreditTemplate(template_id=3, bank="Sber", url="sber.ru", term=36, rate=0),
            CreditTemplate(template_id=4, bank="Sber", url="sber.ru", term=36, rate=0, min_price=2000, max_price=4000),
            CreditTemplate(template_id=5, bank="Sber", url="sber.ru", term=24, rate=5, min_price=2000, max_price=4000),
            CreditTemplate(template_id=6, bank="Sber", url="sber.ru", term=24, rate=5, min_price=2000),
            CreditTemplate(template_id=7, bank="", url="mfo.ru", term=12, rate=10, min_price=1000),
        ]

        cls.index.offers += [
            Offer(title='Offer with creditt 1', offerid=101, fesh=1234567, price=3000, credit_template_id=1, hyperid=1),
            Offer(
                title='Offer with creditt 2',
                offerid=202,
                fesh=654321,
                price=7000,
                credit_template_id=2,
                hid=91491,
                hyperid=1,
                waremd5='Fsqr4FtT6YqkXmpJHfIb7w',
            ),
            Offer(title='Offer without creditt 1', offerid=301, fesh=10000, price=9500, hyperid=2),
            Offer(
                title='Offer with installment',
                offerid=102,
                fesh=123456,
                price=4000,
                credit_template_id=3,
                hyperid=1,
                hid=91491,
            ),
            Offer(
                title='Offer from blocked shop',
                offerid=401,
                fesh=999,
                price=3000,
                credit_template_id=1,
                waremd5='RcSMzi4tf73qGvxRx8atJg',
            ),
            Offer(title='Offer from no blocked shop', offerid=402, fesh=998, price=3000),
            Offer(
                title='Offer with credit template and bad price', offerid=501, fesh=998, price=900, credit_template_id=1
            ),
            Offer(
                title='Offer with credit and default template', offerid=601, fesh=991, price=2000, credit_template_id=5
            ),
            Offer(
                title='Offer without credit and default template',
                offerid=602,
                fesh=991,
                price=1500,
                credit_template_id=5,
            ),
            Offer(
                title='Offer with installment and default template',
                offerid=603,
                fesh=991,
                price=4000,
                credit_template_id=4,
            ),
            Offer(
                title='Offer without installment and default template',
                offerid=604,
                fesh=991,
                price=7000,
                credit_template_id=4,
            ),
            Offer(
                title='Offer with credit and default template 2',
                offerid=605,
                fesh=991,
                price=10000,
                credit_template_id=6,
            ),
            Offer(
                title='Offer without credit and default template 2',
                offerid=606,
                fesh=991,
                price=1500,
                credit_template_id=6,
            ),
            Offer(
                title='Offer with mfo creditt 7', offerid=102, fesh=100500, price=1500, credit_template_id=7, hyperid=1
            ),
        ]

    @skip('white credits will be deleted soon')
    def test_default_templates(self):
        """
        Проверяем, что если цена оффера не удовлетворяет ограничениям в шаблоне, то считаем, что кредита у него нет
        """
        rearr = 'rearr-factors=market_calculate_credits=1;market_return_credits=1'
        response = self.report.request_json('place=prime&fesh=991&rids=213&{}&debug=da'.format(rearr))
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": 'Offer with credit and default template'},
                    "creditInfo": NotEmpty(),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": 'Offer without credit and default template'},
                    "creditInfo": NoKey("creditInfo"),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": 'Offer with installment and default template'},
                    "creditInfo": NotEmpty(),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": 'Offer without installment and default template'},
                    "creditInfo": NoKey("creditInfo"),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": 'Offer with credit and default template 2'},
                    "creditInfo": NotEmpty(),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": 'Offer without credit and default template 2'},
                    "creditInfo": NoKey("creditInfo"),
                },
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "name": "Покупка в кредит",
                "values": [
                    {
                        "found": 3,
                        "value": "credit",
                    },
                    {
                        "value": "installment",
                        "found": 1,
                    },
                ],
            },
        )

        # С фильтром по кредиту
        response = self.report.request_json(
            'place=prime&fesh=991&rids=213&{}&debug=da&credit-type=credit'.format(rearr)
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": 'Offer with credit and default template'},
                    "creditInfo": NotEmpty(),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": 'Offer with installment and default template'},
                    "creditInfo": NotEmpty(),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": 'Offer with credit and default template 2'},
                    "creditInfo": NotEmpty(),
                },
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "name": "Покупка в кредит",
                "values": [
                    {
                        "found": 3,
                        "value": "credit",
                    },
                    {
                        "value": "installment",
                        "found": 1,
                    },
                ],
            },
        )

        # С фильтром по рассрочке
        response = self.report.request_json(
            'place=prime&fesh=991&rids=213&{}&debug=da&credit-type=installment'.format(rearr)
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {"raw": 'Offer with installment and default template'},
                    "creditInfo": NotEmpty(),
                },
            ],
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "name": "Покупка в кредит",
                "values": [
                    {
                        "found": 3,
                        "value": "credit",
                    },
                    {
                        "value": "installment",
                        "found": 1,
                    },
                ],
            },
        )

    @skip('white credits will be deleted soon')
    def test_dynamic_credit_filter_and_bad_price(self):
        """
        Проверяем работу отключения кредитов у магазинов из динамического фильтра
        и проверку цены на превышение минимального порога
        """
        self.dynamic.market_dynamic.disabled_credit_shops.clear()
        '''
        Проверяем без фильтрации по кредиту
        '''
        response = self.report.request_json(
            "place=prime&fesh=999&fesh=998&rids=213&debug=da"
            "&rearr-factors=market_calculate_credits=1;market_return_credits=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "Offer from blocked shop"}, "creditInfo": NotEmpty()},
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer from no blocked shop"},
                        "creditInfo": NoKey("creditInfo"),
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer with credit template and bad price"},
                        "creditInfo": NoKey("creditInfo"),
                    },
                ]
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "name": "Покупка в кредит",
                "values": [
                    {
                        "found": 1,
                        "value": "credit",
                    },
                    {
                        "value": "installment",
                        "found": 0,
                    },
                ],
            },
        )

        self.click_log.expect(feat=Features.MASK_BY_NAME[Features.HAS_CREDIT], ware_md5='RcSMzi4tf73qGvxRx8atJg')

        self.show_log.expect(feat=Features.MASK_BY_NAME[Features.HAS_CREDIT], ware_md5='RcSMzi4tf73qGvxRx8atJg')

        '''
        Проверяем с фильтром
        '''
        response = self.report.request_json(
            "place=prime&fesh=999&fesh=998&rids=213&credit-type=credit"
            "&rearr-factors=market_calculate_credits=1;market_return_credits=1;"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "titles": {"raw": "Offer from blocked shop"}, "creditInfo": NotEmpty()},
                ]
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "name": "Покупка в кредит",
                "values": [
                    {
                        "checked": True,
                        "found": 1,
                        "value": "credit",
                    },
                    {
                        "value": "installment",
                        "found": 0,
                    },
                ],
            },
        )

        self.click_log.expect(feat=Features.MASK_BY_NAME[Features.HAS_CREDIT], ware_md5='RcSMzi4tf73qGvxRx8atJg')

        self.show_log.expect(feat=Features.MASK_BY_NAME[Features.HAS_CREDIT], ware_md5='RcSMzi4tf73qGvxRx8atJg')

        self.dynamic.market_dynamic.disabled_credit_shops += [DynamicShop(999)]

        response = self.report.request_json(
            "place=prime&fesh=999&fesh=998&rids=213&rearr-factors=market_calculate_credits=1;market_return_credits=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer from no blocked shop"},
                        "creditInfo": NoKey("creditInfo"),
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer from blocked shop"},
                        "creditInfo": NoKey("creditInfo"),
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer with credit template and bad price"},
                        "creditInfo": NoKey("creditInfo"),
                    },
                ]
            },
            allow_different_len=False,
        )

        self.assertFragmentNotIn(
            response,
            {
                "name": "Покупка в кредит",
            },
        )

    @skip('white credits will be deleted soon')
    def test_filter_prime(self):
        """
        Check filter "credit-type=credit" on place=prime
        """
        response = self.report.request_json(
            "place=prime&hid=91491&rids=213&rearr-factors=market_calculate_credits=1;market_return_credits=1&credit-type=credit&debug=da&allow-collapsing=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "titles": {"raw": "Model with creditt"},
                    },
                ],
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "search": {"results": []},
                "filters": [
                    {
                        "name": "Покупка в кредит",
                        "values": [
                            {
                                "checked": True,
                                "found": 3,
                                "value": "credit",
                            },
                            {
                                "value": "installment",
                                "found": 1,
                            },
                        ],
                    }
                ],
            },
        )

    @skip('white credits will be deleted soon')
    def test_filter_prime_2(self):
        '''
        Проверяем, что оффера из магазинов, у которых локальный регион не является регионом пользователя или его родителем, отфильтровались
        '''
        response = self.report.request_json(
            "place=prime&hid=91491&rids=75&rearr-factors=market_return_credits=1;market_calculate_credits=1&credit-type=credit&allow-collapsing=0"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer with creditt 2"},
                        "creditInfo": {  # Check return credit info for offer
                            "url": "tinkoff.ru",
                            "bank": "Tinkoff",
                            "term": 24,
                            "rate": 2,
                            "monthlyPayment": {"value": "298", "currency": "RUR"},
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                "search": {"results": []},
                "filters": [
                    {
                        "name": "Покупка в кредит",
                        "values": [
                            {
                                "checked": True,
                                "found": 1,
                                "value": "credit",
                            },
                            {
                                "value": "installment",
                                "found": 0,
                            },
                        ],
                    }
                ],
            },
        )

        self.click_log.expect(feat=Features.MASK_BY_NAME[Features.HAS_CREDIT], ware_md5='Fsqr4FtT6YqkXmpJHfIb7w')

        self.show_log.expect(feat=Features.MASK_BY_NAME[Features.HAS_CREDIT], ware_md5='Fsqr4FtT6YqkXmpJHfIb7w')

        '''
        Проверяем, что если локальный регион магазина - родитель региона пользователя, то там тоже есть кредит
        '''
        response = self.report.request_json(
            "place=prime&hid=91491&rids=10&rearr-factors=market_return_credits=1;market_calculate_credits=1&credit-type=credit&allow-collapsing=0"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer with creditt 2"},
                        "creditInfo": {  # Check return credit info for offer
                            "url": "tinkoff.ru",
                            "bank": "Tinkoff",
                            "term": 24,
                            "rate": 2,
                            "monthlyPayment": {"value": "298", "currency": "RUR"},
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

        self.click_log.expect(feat=Features.MASK_BY_NAME[Features.HAS_CREDIT], ware_md5='Fsqr4FtT6YqkXmpJHfIb7w')

        self.show_log.expect(feat=Features.MASK_BY_NAME[Features.HAS_CREDIT], ware_md5='Fsqr4FtT6YqkXmpJHfIb7w')

    @skip('white credits will be deleted soon')
    def test_prime_without_filter(self):
        """
        Check results without filter "credit-type"
        """
        response = self.report.request_json(
            "place=prime&text=creditt&rids=213&rearr-factors=market_return_credits=1;market_calculate_credits=1&debug=da"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer with creditt 1"},
                        "creditInfo": {
                            "rate": 5,
                            "term": 24,
                            "url": "sber.ru",
                            "bank": "Sber",
                            "monthlyPayment": {"currency": "RUR", "value": "132", "isDeliveryIncluded": False},
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer with mfo creditt 7"},
                        "creditInfo": {"rate": 10, "term": 12, "url": "mfo.ru", "bank": ""},
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer without creditt 1"},
                        "creditInfo": NoKey("creditInfo"),
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer with creditt 2"},
                        "creditInfo": NoKey("creditInfo"),
                    },
                    {
                        "entity": "product",
                        "titles": {"raw": "Model with creditt"},
                    },
                    {
                        "entity": "product",
                        "titles": {"raw": "Model without creditt"},
                    },
                    {"entity": "regionalDelimiter"},
                ],
            },
            allow_different_len=False,
        )

    @skip('white credits will be deleted soon')
    def test_filter_prime_no_exp(self):
        """
        Check without experiment flag: market_return_credits, but with market_calculate_credits
        """
        rearr = "rearr-factors=market_calculate_credits=1;market_return_credits=0"
        response = self.report.request_json(
            "place=prime&text=creditt&rids=213&credit-type=credit&allow-collapsing=0&{}".format(rearr)
        )
        all_docs_213 = {
            "results": [
                {
                    "entity": "offer",
                    "titles": {"raw": "Offer with creditt 1"},
                    "creditInfo": NoKey("creditInfo"),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Offer with creditt 2"},
                    "creditInfo": NoKey("creditInfo"),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Offer with mfo creditt 7"},
                    "creditInfo": NoKey("creditInfo"),
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Offer without creditt 1"},
                    "creditInfo": NoKey("creditInfo"),
                },
                {"entity": "product", "titles": {"raw": "Model with creditt"}},
                {"entity": "product", "titles": {"raw": "Model without creditt"}},
                {"entity": "regionalDelimiter"},
            ],
        }
        self.assertFragmentIn(response, all_docs_213, allow_different_len=False)

        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "name": "Покупка в кредит",
                    }
                ]
            },
        )

        response = self.report.request_json("place=prime&text=creditt&rids=213&{}".format(rearr))
        self.assertFragmentIn(response, all_docs_213, allow_different_len=False)

        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "name": "Покупка в кредит",
                    }
                ]
            },
        )

    @classmethod
    def prepare_credit_redirect(cls):
        cls.index.hypertree += [
            HyperCategory(
                hid=198118,
                name="Applience",
                children=[
                    HyperCategory(hid=90584, name="Wash machine"),
                ],
            ),
        ]

        cls.index.models += [
            Model(hid=90584, hyperid=3, title='Model with'),
        ]

        cls.index.offers += [
            Offer(
                title='iphone 6', offerid=1002, fesh=123456, hyperid=3, price=3000, credit_template_id=1
            ),  # with credit
            Offer(
                title='iphone 8',
                offerid=1003,
                fesh=1234567,
                hyperid=3,
                price=3000,
                credit_template_id=3,
                waremd5='zvDT8K9GzgSctLiDQHrPxw',
            ),  # with installment
            Offer(title='iphone 7', offerid=2001, fesh=10000, hyperid=3, price=3200),  # without credit
        ]

    @skip('white credits will be deleted soon')
    def test_credit_redirect(self):
        """
        Проверяем, что при наличии в запросе слова "кредит" происходит кредитный редирект.
        Проставляется фильтр и слово "кредит" подсвечивается.
        """

        text = 'iphone в кредит'
        hid = '90584'
        rearr = 'rearr-factors=market_return_credits=1;market_calculate_credits=1'
        request = 'cvredirect=1&place=prime&text={}&{}&rids=213'.format(text, rearr)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "hid": [hid],
                        "credit-type": ["credit"],
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]
        creditFilter = "credit-type=credit"

        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}&{}&rids=213'.format(text, hid, rs, creditFilter, rearr)
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 2,
                    "isParametricSearch": True,
                    "results": [
                        {
                            "entity": "offer",
                            "titles": {"raw": "iphone 6"},
                            "creditInfo": NotEmpty(),
                        },
                        {"entity": "offer", "titles": {"raw": "iphone 8"}, "creditInfo": NotEmpty()},
                    ],
                },
                "query": {"highlighted": [{"value": "iphone в "}, {"value": "кредит", "highlight": True}]},
                "filters": [
                    {
                        "name": "Покупка в кредит",
                        "isParametric": True,
                        "values": [
                            {
                                "value": "credit",
                                "checked": True,
                                "found": 2,
                            },
                            {"value": "installment", "found": 1},
                        ],
                    }
                ],
            },
        )

    @skip('white credits will be deleted soon')
    def test_installment_redirect(self):
        """
        Проверяем, что при наличие в запросе слова "рассрочку" происходит редирект.
        Проставляется фильтр и слово "рассрочку" подсвечивается, не смотря на то,
        что есть категорийный редирект.
        """
        text = 'iphone в рассрочку'
        hid = '90584'
        rearr = 'rearr-factors=market_return_credits=1;market_calculate_credits=1'
        request = 'cvredirect=1&place=prime&text={}&{}&rids=213'.format(text, rearr)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "hid": [hid],
                        "credit-type": ["installment"],
                    },
                }
            },
        )

        rs = response.root['redirect']['params']['rs'][0]
        installment = "credit-type=installment"
        response = self.report.request_json(
            'place=prime&text={}&hid={}&rs={}&{}&{}&rids=213'.format(text, hid, rs, installment, rearr)
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,
                    "isParametricSearch": True,
                    "results": [{"entity": "offer", "titles": {"raw": "iphone 8"}, "creditInfo": NotEmpty()}],
                },
                "query": {"highlighted": [{"value": "iphone в "}, {"value": "рассрочку", "highlight": True}]},
                "filters": [
                    {
                        "name": "Покупка в кредит",
                        "isParametric": True,
                        "values": [
                            {
                                "checked": True,
                                "found": 1,
                                "value": "installment",
                            },
                            {"value": "credit", "checked": NoKey("checked")},
                        ],
                    }
                ],
            },
        )

        self.click_log.expect(
            feat=Features.feature_list_to_mask([Features.HAS_CREDIT, Features.HAS_INSTALLMENT]),
            ware_md5='zvDT8K9GzgSctLiDQHrPxw',
        )

        self.show_log.expect(
            feat=Features.feature_list_to_mask([Features.HAS_CREDIT, Features.HAS_INSTALLMENT]),
            ware_md5='zvDT8K9GzgSctLiDQHrPxw',
        )

    @skip('white credits will be deleted soon')
    def test_no_redirect_without_exp(self):
        """
        Проверяем, что без флага эксперимента нет кредитного редиректа
        """

        text = 'iphone в кредит'
        hid = '90584'
        rearr = 'rearr-factors=market_calculate_credits=0;market_return_credits=0'
        request = 'cvredirect=1&place=prime&text={}&rids=213&{}'.format(text, rearr)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "hid": [hid],
                        "credit-type": NoKey("credit-type"),
                    },
                }
            },
        )

    @skip('white credits will be deleted soon')
    def test_only_credit_redirect(self):
        """
        Проверяем, что без категорийного редиректа также есть кредитный редирект.
        """
        text = 'iphone в кредит'
        _ = '90584'
        rearr = 'rearr-factors=market_return_credits=1;market_calculate_credits=1'
        request = 'cvredirect=1&place=prime&text={}&{}&rids=213'.format(text, rearr)
        request += '&rearr-factors=market_redirect_to_alone_category=0;'
        request += 'market_category_redirect_treshold=3'  # чтобы не было категорийного редиректа
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "was_redir": ["1"],
                        "hid": NoKey("hid"),
                        "credit-type": ["credit"],
                    },
                }
            },
        )

    @skip('white credits will be deleted soon')
    def test_no_redirect_without_word(self):
        """
        Проверяем, что без соответствующего слова в запросе нет кредитного редиректа,
        когда нет категорийного редиректа
        """
        text = 'iphone'
        rearr = 'rearr-factors=market_return_credits=1;market_calculate_credits=1'
        request = 'cvredirect=1&place=prime&text={}&{}&rids=213'.format(text, rearr)
        request += '&rearr-factors=market_redirect_to_alone_category=0;'
        request += 'market_category_redirect_treshold=3'  # чтобы не было категорийного редиректа
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"redirect": NoKey("redirect")})

        """
        Проверяем, что без соответствующего слова в запросе нет кредитного редиректа,
        когда есть категорийный редирект
        """
        text = 'iphone'
        request = 'cvredirect=1&place=prime&text={}&{}&rids=213'.format(text, rearr)
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"redirect": NoKey("redirect")})
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "target": "search",
                    "params": {
                        "hid": ['90584'],
                        "credit-type": NoKey("credit-type"),
                    },
                }
            },
        )

    @classmethod
    def prepare_product_offers(cls):
        cls.index.models += [
            Model(hid=9000, hyperid=4),
        ]

        cls.index.offers += [
            Offer(title='Offer with cred 1-1', offerid=401, fesh=123456, hyperid=4, price=3000, credit_template_id=1),
            Offer(
                title='Offer with installment 1-2',
                offerid=402,
                fesh=1234567,
                hyperid=4,
                price=3000,
                credit_template_id=3,
            ),
            Offer(title='Offer without cred 2-1', offerid=501, fesh=10000, hyperid=4, price=3100),  # without credit
        ]

    @skip('white credits will be deleted soon')
    def test_filter_productoffers(self):

        all_offers = {
            "results": [
                {
                    "entity": "offer",
                    "titles": {"raw": "Offer with cred 1-1"},
                    "creditInfo": {
                        "rate": 5,
                        "term": 24,
                        "url": "sber.ru",
                        "bank": "Sber",
                        "monthlyPayment": {"currency": "RUR", "value": "132", "isDeliveryIncluded": False},
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Offer with installment 1-2"},
                    "creditInfo": {
                        "rate": 0,
                        "term": 36,
                        "url": "sber.ru",
                        "bank": "Sber",
                        "monthlyPayment": {"currency": "RUR", "value": "83", "isDeliveryIncluded": False},
                    },
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Offer without cred 2-1"},
                },
            ],
        }

        installment_offers = [
            {
                "entity": "offer",
                "titles": {"raw": "Offer with installment 1-2"},
                "creditInfo": {
                    "rate": 0,
                    "term": 36,
                    "url": "sber.ru",
                    "bank": "Sber",
                    "monthlyPayment": {"currency": "RUR", "value": "83", "isDeliveryIncluded": False},
                },
            },
        ]

        credit_offers = installment_offers + [
            {
                "entity": "offer",
                "titles": {"raw": "Offer with cred 1-1"},
                "creditInfo": {
                    "rate": 5,
                    "term": 24,
                    "url": "sber.ru",
                    "bank": "Sber",
                    "monthlyPayment": {"currency": "RUR", "value": "132", "isDeliveryIncluded": False},
                },
            },
        ]

        """
        Check filter "credit-type=credit" on place=productoffers in top6. Must have all offers (relax filters)
        """
        relax_filters = "relax-filters=true"
        rearr = "rearr-factors=market_return_credits=1;market_calculate_credits=1"
        top6 = "offers-set=top"
        response = self.report.request_json(
            "place=productoffers&hyperid=4&rids=213&credit-type=credit&{}&{}&{}".format(relax_filters, rearr, top6)
        )
        self.assertFragmentIn(response, all_offers, allow_different_len=False)

        response = self.report.request_json(
            "place=productoffers&hyperid=4&rids=213&credit-type=installment&{}&{}&{}".format(relax_filters, rearr, top6)
        )
        self.assertFragmentIn(response, all_offers, allow_different_len=False)

        """
        Check filter "credit-type=credit" on place=productoffers in prices - checked filter
        """
        response = self.report.request_json(
            "place=productoffers&hyperid=4&rids=213&{}&credit-type=credit".format(rearr)
        )

        self.assertFragmentIn(response, credit_offers, allow_different_len=False)

        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "name": "Покупка в кредит",
                        "values": [
                            {
                                "checked": True,
                                "found": 2,
                                "value": "credit",
                            },
                            {
                                "value": "installment",
                                "found": 1,
                            },
                        ],
                    }
                ],
            },
        )

        """
        Check filter "credit-type=installment" on place=productoffers in prices - checked filter
        """
        response = self.report.request_json(
            "place=productoffers&hyperid=4&rids=213&{}&credit-type=installment".format(rearr)
        )

        self.assertFragmentIn(response, installment_offers, allow_different_len=False)

        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "name": "Покупка в кредит",
                        "values": [
                            {"found": 2, "value": "credit", "checked": NoKey("checked")},
                            {
                                "value": "installment",
                                "found": 1,
                                "checked": True,
                            },
                        ],
                    }
                ],
            },
        )

        """
        Check filter "credit-type" on place=productoffers in prices - didn't check filter
        """
        response = self.report.request_json("place=productoffers&hyperid=4&rids=213&{}".format(rearr))

        self.assertFragmentIn(response, all_offers, allow_different_len=False)

        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "name": "Покупка в кредит",
                        "values": [
                            {
                                "checked": NoKey("checked"),
                                "found": 2,
                                "value": "credit",
                            },
                            {
                                "found": 1,
                                "value": "installment",
                                "checked": NoKey("checked"),
                            },
                        ],
                    }
                ],
            },
        )

        """
        Check filter "credit-type=credit" filter for default Offer
        """
        response = self.report.request_json(
            "place=defaultoffer&credit-type=credit&hyperid=4&rids=213&rearr-factors=market_return_credits=1;market_calculate_credits=1"
        )
        self.assertFragmentIn(response, {"entity": "offer", "creditInfo": NotEmpty()}, allow_different_len=False)

    @classmethod
    def prepare_no_credit_offers(cls):
        cls.index.models += [
            Model(hid=9000, hyperid=5),
        ]

        cls.index.offers += [
            Offer(title='Offer without cred 3-1', offerid=502, fesh=10000, hyperid=5, price=3100),  # without credit
        ]

    @skip('white credits will be deleted soon')
    def test_no_credit_offers(self):
        response = self.report.request_json(
            "place=productoffers&hyperid=5&rids=213&rearr-factors=market_return_credits=1;market_calculate_credits=1"
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer without cred 3-1"},
                    },
                ],
            },
            allow_different_len=False,
        )

        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "name": "Покупка в кредит",
                    }
                ]
            },
        )

    @classmethod
    def prepare_geo_relax_credit_filter(cls):
        cls.index.outlets += [
            Outlet(point_id=1, fesh=123456, region=213, gps_coord=GpsCoord(37.1, 55.1)),
            Outlet(point_id=2, fesh=10000, region=213, gps_coord=GpsCoord(37.2, 55.2)),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=123456,
                carriers=[99],
                options=[PickupOption(outlet_id=1)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=10000,
                carriers=[99],
                options=[PickupOption(outlet_id=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    @skip('white credits will be deleted soon')
    def test_geo_relax_credit_filter(self):
        """
        Check relax credit filter in place=geo
        """

        all_offers = {
            "results": [
                {
                    "entity": "offer",
                    "titles": {"raw": "Offer with cred 1-1"},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Offer without cred 2-1"},
                },
                {
                    "entity": "offer",
                    "titles": {"raw": "Offer with installment 1-2"},
                },
            ],
        }

        geo_info = 'geo-location=37.15,55.15&geo_bounds_lb=37.0,55.0&geo_bounds_rt=37.3,55.3'
        relax_filters = "relax-filters=true"
        rearr = "rearr-factors=market_return_credits=1;market_calculate_credits=1"
        credit_filter = "credit-type=credit"
        response = self.report.request_json(
            'place=geo&hyperid=4&rids=213&{}&{}&{}&{}'.format(geo_info, relax_filters, rearr, credit_filter)
        )

        self.assertFragmentIn(response, all_offers, allow_different_len=False)

    @skip('white credits will be deleted soon')
    def test_installment(self):
        rearr = "rearr-factors=market_return_credits=1;market_calculate_credits=1"
        response = self.report.request_json('place=prime&text=installment&fesh=123456&{}&rids=213'.format(rearr))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {"raw": "Offer with installment"},
                        "creditInfo": {"term": 36, "rate": 0, "monthlyPayment": {"currency": "RUR", "value": "111"}},
                    }
                ]
            },
        )

    @classmethod
    def prepare_credit_template_id_filter(cls):
        cls.index.shops += [
            Shop(fesh=393930),
            Shop(fesh=393931),
        ]

        cls.index.credit_templates += [
            CreditTemplate(template_id=300, bank="Sber", url="sber.ru", term=24, rate=5.0),
            CreditTemplate(template_id=301, bank="Sber", url="sber.ru", term=24, rate=5.0),
        ]

        cls.index.offers += [
            Offer(title='300 credit template id', fesh=393930, price=3000, credit_template_id=300),
            Offer(title='300 credit template id 2', fesh=393930, price=1000, credit_template_id=300),
            Offer(title='301 credit template id', fesh=393931, price=3000, credit_template_id=301),
        ]

    @skip('white credits will be deleted soon')
    def test_credit_template_id_filter(self):
        """
        Проверяем, что остаются только оффера с кредитом, у которых привязан конкретных шаблон
        """
        response = self.report.request_json("place=prime&fesh=393930&fesh=393931&credit-template-id=300")
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "300 credit template id"}},
                {"titles": {"raw": "300 credit template id 2"}},
            ],
            allow_different_len=False,
        )

        response = self.report.request_json("place=prime&fesh=393930&fesh=393931&credit-template-id=301")
        self.assertFragmentIn(
            response,
            [
                {"titles": {"raw": "301 credit template id"}},
            ],
            allow_different_len=False,
        )

    @classmethod
    def prepare_yandex_cassa_formula(cls):
        cls.index.shops += [
            Shop(fesh=400000, regions=[213], priority_region=213),
        ]

        cls.index.credit_templates += [
            CreditTemplate(
                template_id=400,
                bank="Yandex Kassa",
                url="kassa.yandex.ru",
                organization_type=CreditTemplateOrganizationType.YANDEX_KASSA,
                term=12,
                rate=1.9,
            ),
        ]

        cls.index.offers += [
            Offer(title='400 credit template id', fesh=400000, price=3000, credit_template_id=400),
        ]

    @skip('white credits will be deleted soon')
    def test_yandex_cassa_formula(self):
        """Test that in case of Yandex.Kassa template monthly payment calculated using
        separate formula from https://st.yandex-team.ru/MARKETBTOB-729#5ec461a31d21c53ee38a3ca6
        """

        # using formula we should get 3000*(1/12+0.039) == 3000*0.122333 == 367
        rearr = '&rearr-factors=market_calculate_credits=1;market_return_credits=1'
        response = self.report.request_json("place=prime&fesh=400000&rids=213" + rearr)
        self.assertFragmentIn(
            response,
            [
                {
                    "titles": {"raw": "400 credit template id"},
                    "creditInfo": {
                        "url": "kassa.yandex.ru",
                        "bank": "Yandex Kassa",
                        "term": 12,
                        "rate": 1.9,  # rate doesn't matter
                        "monthlyPayment": {"value": "367", "currency": "RUR"},
                    },
                },
            ],
            allow_different_len=False,
        )

        # try custom rate
        # using formula we should get 3000*(1/12+0.05) == 3000*0.133333 == 400
        custom_rate = ';market_yandex_kassa_rate=0.05'
        response = self.report.request_json("place=prime&fesh=400000&rids=213" + rearr + custom_rate)
        self.assertFragmentIn(
            response,
            [
                {
                    "titles": {"raw": "400 credit template id"},
                    "creditInfo": {
                        "url": "kassa.yandex.ru",
                        "bank": "Yandex Kassa",
                        "term": 12,
                        "rate": 1.9,  # rate doesn't matter
                        "monthlyPayment": {"value": "400", "currency": "RUR"},
                    },
                },
            ],
        )

    @classmethod
    def prepare_payment_round_ceil(cls):
        cls.index.shops += [
            Shop(fesh=500000, regions=[213], priority_region=213),
        ]

        cls.index.credit_templates += [
            CreditTemplate(
                template_id=500,
                bank="MaybeABank",
                url="maybeabank.ru",
                organization_type=CreditTemplateOrganizationType.BANK,
                term=12,
                rate=1.9,
            ),
            CreditTemplate(
                template_id=501,
                bank="Yandex Kassa",
                url="kassa.yandex.ru",
                organization_type=CreditTemplateOrganizationType.YANDEX_KASSA,
                term=12,
                rate=1.9,
            ),
        ]

        cls.index.offers += [
            Offer(title='500 credit template id', fesh=500000, price=3001, credit_template_id=500),
            Offer(title='501 credit template id', fesh=500000, price=3001, credit_template_id=501),
        ]

    @skip('white credits will be deleted soon')
    def test_payment_round_ceil(self):
        """Check that monthly prices rounds up away from zero"""

        # using formula we should get
        rearr = '&rearr-factors=market_calculate_credits=1;market_return_credits=1'
        response = self.report.request_json("place=prime&fesh=500000&rids=213" + rearr)
        self.assertFragmentIn(
            response,
            [
                {
                    "titles": {"raw": "500 credit template id"},
                    "creditInfo": {
                        "url": "maybeabank.ru",
                        "bank": "MaybeABank",
                        "term": 12,
                        "rate": 1.9,
                        "monthlyPayment": {
                            "value": "253",  # 3001*(0.019/12 + 0.019/12/((0.019/12+1)^12-1)) ~ 252.6
                            "currency": "RUR",
                        },
                    },
                },
                {
                    "titles": {"raw": "501 credit template id"},
                    "creditInfo": {
                        "url": "kassa.yandex.ru",
                        "bank": "Yandex Kassa",
                        "term": 12,
                        "rate": 1.9,  # since rate hard-coded this value does not matter
                        "monthlyPayment": {"value": "368", "currency": "RUR"},  # 3001*(1/12+0.039) ~ 367.12
                    },
                },
            ],
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
