#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    Currency,
    DeliveryBucket,
    DeliveryOption,
    DynamicShop,
    ExchangeRate,
    GLParam,
    GLType,
    GpsCoord,
    HyperCategory,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    PromoType,
    Region,
    RegionalDelivery,
    RegionalModel,
    Shop,
)
from core.matcher import Contains, Absent, NoKey, NotEmpty

CONDITION_NEW = 0
CONDITION_LIKE_NEW = 1
CONDITION_USED = 2
CONDITION_UNKNOWN = 1 << 15


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.reqwizard.on_default_request().respond()

        cls.index.regiontree += [Region(rid=213, name='Москва')]

        cls.index.hypertree += [
            HyperCategory(hid=34567, name="Electronics"),
            HyperCategory(hid=12345, name="Electronics2"),
            HyperCategory(hid=123, name="Electronics3"),
            HyperCategory(hid=345, name="Electronics4"),
            HyperCategory(hid=789, name="Electronics5"),
        ]

        cls.index.shops += [
            Shop(fesh=94035, name="technopoint.ru", priority_region=213),
            Shop(fesh=1),
        ]

        cls.index.models += [
            Model(hid=34567, title="Mobile phone", hyperid=1),
            Model(hid=12345, hyperid=2),
            Model(hid=123, hyperid=4),
            Model(hid=345, hyperid=5),
            Model(hid=789, hyperid=6),
        ]

        cls.index.regional_models += [
            RegionalModel(
                hyperid=1, offers=100, price_min=50, price_max=80, price_med=93, rids=[213], cut_price_count=4
            )
        ]

        cls.index.offers += [
            Offer(
                title="Mobile telephone - cutprice",
                hid=34567,
                fesh=94035,
                is_cutprice=True,
                like_new=True,
                condition_reason="вскрытая упаковка",
                hyperid=1,
                price=50,
                glparams=[GLParam(param_id=202, value=1)],
                pickup=False,
            ),  # Уцененный
            Offer(
                title="Mobile telephone - prev.used",
                hid=34567,
                fesh=94035,
                is_cutprice=True,
                previously_used=True,
                condition_reason="следы эксплуатации",
                hyperid=1,
                price=70,
                glparams=[GLParam(param_id=202, value=3)],
                pickup=False,
            ),  # б/у
            Offer(
                title="Mobile telephone - new",
                hid=34567,
                fesh=94035,
                hyperid=1,
                price=100,
                glparams=[GLParam(param_id=202, value=1)],
                pickup=True,
            ),  # новый
            Offer(title="Mobile telephone - new", hid=34567, fesh=94035, hyperid=1, price=90, pickup=True),  # новый
            Offer(
                title="Mobile telephone - cutprice",
                hid=34567,
                fesh=94035,
                is_cutprice=True,
                like_new=True,
                condition_reason="вскрытая упаковка",
                hyperid=1,
                price=60,
                glparams=[GLParam(param_id=202, value=1)],
                pickup=False,
            ),  # Уцененный
            Offer(
                title="Mobile telephone - cutprice II",
                hid=34567,
                fesh=94035,
                is_cutprice=True,
                condition_reason="вскрытая упаковка",
                hyperid=1,
                price=80,
                glparams=[GLParam(param_id=202, value=3)],
                pickup=False,
            ),  # Уцененный
        ]

        cls.index.gltypes = [
            GLType(param_id=202, hid=34567, gltype=GLType.ENUM, values=[1, 2, 3], position=1, cluster_filter=True),
            GLType(param_id=200, hid=34567, gltype=GLType.ENUM, values=[1, 2, 3]),
        ]

        cls.index.offers += [
            Offer(
                title="Mobile telephone - new",
                hid=123,
                hyperid=4,
                is_cutprice=False,
                fesh=1,
                glparams=[GLParam(param_id=200, value=1), GLParam(param_id=202, value=1)],
            ),
            Offer(
                title="Mobile telephone - new",
                hid=123,
                hyperid=4,
                is_cutprice=False,
                fesh=1,
                glparams=[GLParam(param_id=200, value=2), GLParam(param_id=202, value=1)],
            ),
            Offer(
                title="Mobile telephone - cutprice",
                hid=123,
                hyperid=4,
                is_cutprice=True,
                condition_reason="вскрытая упаковка",
                fesh=1,
                glparams=[GLParam(param_id=200, value=3), GLParam(param_id=202, value=2)],
            ),
        ]

        cls.index.offers += [
            Offer(
                title="Mobile telephone - new",
                hid=345,
                hyperid=5,
                is_cutprice=False,
                fesh=1,
                glparams=[GLParam(param_id=200, value=1)],
            ),
            Offer(
                title="Mobile telephone - new",
                hid=345,
                hyperid=5,
                is_cutprice=False,
                fesh=1,
                glparams=[GLParam(param_id=200, value=2)],
            ),
        ]

        cls.index.offers += [
            Offer(
                title="Mobile telephone - cutprice",
                hid=789,
                hyperid=6,
                is_cutprice=True,
                fesh=1,
                glparams=[GLParam(param_id=200, value=1)],
            ),
            Offer(
                title="Mobile telephone - cutprice",
                hid=789,
                hyperid=6,
                is_cutprice=True,
                fesh=1,
                glparams=[GLParam(param_id=200, value=2)],
            ),
        ]

    def test_off_cutprice(self):
        """
        Проверяем, что без флага эксперимента есть только новыe офферa
        """
        response = self.report.request_json("place=prime&hid=34567")
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 2,
                "shops": 1,
                "results": [
                    {
                        "entity": "product",
                        "prices": {},
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                        "condition": NoKey("condition"),
                    },
                    {
                        "entity": "offer",
                        "titles": {"raw": "Mobile telephone - new"},
                        "isCutPrice": False,
                        "condition": NoKey("condition"),
                    },
                ],
            },
            allow_different_len=False,
        )

        """
        Проверяем, что нет уцененного и б/у оффера
        """
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - prev.used",
                        },
                    }
                ]
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                    }
                ]
            },
        )

        """
        Проверяем, что нет фильтра "Состояние товара"
        """
        self.assertFragmentNotIn(
            response,
            {
                "filters": [
                    {
                        "id": "good-state",
                        "name": "Состояние товара",
                    }
                ]
            },
        )

    def test_no_in_parallel(self):
        """
        Проверяем, что уцененных нет в параллельном
        """
        response = self.report.request_bs("place=parallel&text=cutprice")
        self.assertFragmentNotIn(response, {"title": {"__hl": "Cutprice", "raw": True}})

    def test_on_cutprice(self):
        """
        Проверяем, что с флагом эксперимента без фильтров показываются все офферы
        """
        response = self.report.request_json("place=prime&hid=34567&show-cutprice=1")
        self.assertFragmentIn(
            response,
            {
                "shops": 1,
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - prev.used",
                        },
                        "isCutPrice": True,
                        "condition": {
                            "type": "previously-used",
                            "reason": "следы эксплуатации",
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                        "isCutPrice": True,
                        "condition": {
                            "type": "like-new",
                            "reason": "вскрытая упаковка",
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice II",
                        },
                        "isCutPrice": True,
                        "condition": {
                            "type": NoKey("type"),
                            "reason": "вскрытая упаковка",
                        },
                    },
                ],
            },
        )

        """
        Проверяем, что фильтр показывается и установлен верно
        """
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "good-state",
                        "type": "boolean",
                        "name": "Состояние товара",
                        "subType": "",
                        "kind": 2,
                        "hasBoolNo": True,
                        "values": [
                            {
                                "initialFound": 2,
                                "found": 2,
                                "value": "new",
                                "priceMin": {"currency": "RUR", "value": "90"},
                            },
                            {
                                "initialFound": 4,
                                "found": 4,
                                "value": "cutprice",
                                "priceMin": {"currency": "RUR", "value": "50"},
                            },
                        ],
                    }
                ],
            },
        )

    def test_filter_new(self):
        """
        Проверяем, что при включенном фильтре new показываются только новые товары
        """
        response = self.report.request_json("place=prime&hid=34567&show-cutprice=1&rids=213&good-state=new")
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 2,
                "shops": 1,
                "results": [
                    {
                        "entity": "product",
                        "prices": {},
                        "cutprices": NoKey("cutprices"),
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                    },
                ],
            },
            allow_different_len=False,
        )

        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - prev.used",
                        },
                        "isCutPrice": True,
                    }
                ]
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                        "isCutPrice": True,
                    }
                ]
            },
        )

        """
        Проверяем, что фильтр показывается и установлен верно
        """
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "good-state",
                        "type": "boolean",
                        "name": "Состояние товара",
                        "subType": "",
                        "kind": 2,
                        "hasBoolNo": True,
                        "values": [
                            {
                                "initialFound": 3,
                                "checked": True,
                                "found": 3,
                                "value": "new",
                                "priceMin": {"currency": "RUR", "value": "90"},
                            },
                            {
                                "initialFound": 5,
                                "found": 5,
                                "value": "cutprice",
                                "priceMin": {"currency": "RUR", "value": "50"},
                            },
                        ],
                    }
                ],
            },
        )

    def test_filter_cutprice(self):
        """
        Проверяем, что при включенном фильтре cutprice показываются только уценённые товары
        """
        response = self.report.request_json("place=prime&hid=34567&show-cutprice=1&good-state=cutprice")
        self.assertFragmentIn(
            response,
            {
                "shops": 1,
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - prev.used",
                        },
                        "isCutPrice": True,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                        "isCutPrice": True,
                    },
                ],
            },
        )

        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                    }
                ]
            },
        )

        """
        Проверяем, что фильтр показывается и установлен верно
        """
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "good-state",
                        "type": "boolean",
                        "name": "Состояние товара",
                        "subType": "",
                        "kind": 2,
                        "hasBoolNo": True,
                        "values": [
                            {
                                "initialFound": 2,
                                "found": 2,
                                "value": "new",
                                "priceMin": {"currency": "RUR", "value": "90"},
                            },
                            {
                                "initialFound": 4,
                                "checked": True,
                                "found": 4,
                                "value": "cutprice",
                                "priceMin": {
                                    "currency": "RUR",
                                    "value": "50",
                                },
                            },
                        ],
                    }
                ],
            },
        )

    def test_filter_in_prime(self):
        """
        Проверяем, что в prime есть фильтр
        """
        response = self.report.request_json(
            "place=prime&hid=34567&text=phone&allow-collapsing=1&rids=213&show-cutprice=1"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "good-state",
                        "hasBoolNo": True,
                        "values": [
                            {
                                "initialFound": 1,
                                "found": 1,
                                "value": "new",
                                "priceMin": NoKey("priceMin"),
                            },
                            {
                                "initialFound": 1,
                                "found": 1,
                                "value": "cutprice",
                                "priceMin": NoKey("priceMin"),
                            },
                        ],
                    }
                ],
            },
        )

    def test_filter_in_productoffers(self):
        """
        Проверяем, что в productoffers есть фильтр
        """
        response = self.report.request_json("place=productoffers&hid=34567&hyperid=1&show-cutprice=1")
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "good-state",
                        "hasBoolNo": True,
                        "values": [
                            {
                                "initialFound": 2,
                                "found": 2,
                                "value": "new",
                                "priceMin": {
                                    "currency": "RUR",
                                    "value": "90",
                                },
                            },
                            {
                                "initialFound": 4,
                                "found": 4,
                                "value": "cutprice",
                                "priceMin": {
                                    "currency": "RUR",
                                    "value": "50",
                                },
                            },
                        ],
                    }
                ],
            },
        )
        """
        Проверяем положение фильтра good-state
        """
        self.assertFragmentIn(
            response, {"search": {}, "filters": [{"id": "glprice"}, {"id": "good-state"}, {"id": "202"}]}
        )

        """
        Проверяем, что без флага эксперимента нет фильтра в productoffers
        """
        response = self.report.request_json("place=productoffers&hid=34567&hyperid=1")
        self.assertFragmentNotIn(response, {"filters": [{"id": "good-state"}]})

        """
        Проверяем, что если на КМ нет уцененных офферов, то фильтр не показывается
        """
        response = self.report.request_json("place=productoffers&hid=345&hyperid=5&show-cutprice=1")
        self.assertFragmentNotIn(response, {"search": {}, "filters": [{"id": "good-state"}]})

        """
        Проверяем, что фильтр good-state не исчезает при выставленных других фильтрах
        """
        response = self.report.request_json("place=productoffers&hid=123&hyperid=4&glfilter=200:1&show-cutprice=1&")
        self.assertFragmentIn(response, {"search": {}, "filters": [{"id": "good-state"}]})

        response = self.report.request_json("place=productoffers&hid=123&hyperid=4&glfilter=200:2&show-cutprice=1&")
        self.assertFragmentIn(response, {"search": {}, "filters": [{"id": "good-state"}]})

        response = self.report.request_json("place=productoffers&hid=123&hyperid=4&glfilter=200:3&show-cutprice=1&")
        self.assertFragmentIn(response, {"search": {}, "filters": [{"id": "good-state"}]})

    def test_miprime(self):
        """
        Проверяем, что с флагом эксперимента без фильтров показываются все офферы
        """
        response = self.report.request_json("place=miprime&fesh=94035&show-cutprice=1&numdoc=30")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - prev.used",
                        },
                        "isCutPrice": True,
                        "condition": {
                            "type": "previously-used",
                            "reason": "следы эксплуатации",
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                        "isCutPrice": True,
                        "condition": {
                            "type": "like-new",
                            "reason": "вскрытая упаковка",
                        },
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice II",
                        },
                        "isCutPrice": True,
                        "condition": {
                            "type": NoKey("type"),
                            "reason": "вскрытая упаковка",
                        },
                    },
                ]
            },
        )

    def test_min_price(self):
        """
        Проверяем, что если нет уцененных офферов, то minPrice для уцененных в фильтре не возвращается
        """
        response = self.report.request_json("place=productoffers&hyperid=4&hid=123&glfilter=202:1&show-cutprice=1")
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "good-state",
                        "values": [
                            {
                                "value": "cutprice",
                                "priceMin": NoKey("priceMin"),
                            },
                            {
                                "value": "new",
                                "priceMin": {"value": "100"},
                            },
                        ],
                    }
                ],
            },
        )

        # Тоже самое для новых
        response = self.report.request_json("place=productoffers&hyperid=4&hid=123&glfilter=202:3&show-cutprice=1")
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "good-state",
                        "values": [
                            {
                                "value": "cutprice",
                                "priceMin": NoKey("priceMin"),
                            },
                            {
                                "value": "new",
                                "priceMin": NoKey("priceMin"),
                            },
                        ],
                    }
                ],
            },
        )

    def test_defaultoffer(self):
        """
        Проверяем, что без флага эксперимента возвращается новый оффер
        """
        response = self.report.request_json("place=defaultoffer&hyperid=1")
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                    }
                ],
            },
        )

        """
        Проверяем, что в эксперименте без фильтрации возвращаются все офферы
        """
        response = self.report.request_json("place=defaultoffer&hyperid=1&show-cutprice=1")
        self.assertFragmentIn(response, {"total": 6})

        """
        Проверяем, что дефолтный офер - новый, а не уцененный
        """
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                    }
                ]
            },
        )

        """
        Проверяем, что в эксперименте при фильтрации good-state=cutprice возвращается уцененный оффер (не новый)
        """
        response = self.report.request_json("place=defaultoffer&hyperid=1&show-cutprice=1&good-state=cutprice")
        self.assertFragmentIn(response, {"total": 4})
        self.assertFragmentNotIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                    }
                ]
            },
        )

        """
        Проверяем, что в эксперименте при фильтрации good-state=new возвращается новыe офферa
        """
        response = self.report.request_json("place=defaultoffer&hyperid=1&show-cutprice=1&good-state=new")
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                    }
                ],
            },
        )

    def test_default_offer_pp(self):
        """
        Проверяем, что для уцененного товара в дефолтном оффере меняется pp
        при фильтрации по уцененке
        """
        response = self.report.request_json(
            "pp=6&place=productoffers&hyperid=1&show-cutprice=1&offers-set=default,list&show-urls=encrypted&good-state=cutprice"
        )
        #    self.show_log_tskv.expect(hyper_id=1, is_cutprice=1, pp=200).times(1)
        #    self.show_log_tskv.expect(hyper_id=1, is_cutprice=1, pp=211).times(3)
        self.assertFragmentIn(
            response,
            {"entity": "offer", "urls": {"encrypted": Contains("/pp={}/".format(200))}, "benefit": {"type": "default"}},
        )
        self.assertFragmentIn(
            response,
            {"entity": "offer", "urls": {"encrypted": Contains("/pp={}/".format(211))}, "benefit": NoKey("benefit")},
        )

    def test_touch_pp(self):
        """
        Проверяем, что для уцененного товара в таче меняется pp
        """
        response = self.report.request_json(
            "pp=46&place=productoffers&hyperid=6&show-cutprice=1&offers-set=default,list&show-urls=encrypted&touch=1"
        )
        self.assertFragmentIn(
            response,
            {"entity": "offer", "urls": {"encrypted": Contains("/pp={}/".format(614))}, "benefit": NoKey("benefit")},
        )

    def test_touch_grhow_no(self):
        """
        Проверяем, что в таче нет группировки по магазину
        """
        response = self.report.request_json(
            "pp=46&place=productoffers&hyperid=1&show-cutprice=1&offers-set=default,list&show-urls=encrypted&touch=1&grhow=shop"
        )
        self.assertFragmentIn(response, [{"entity": "offer", "shop": {"id": 94035}} for _ in range(1)])

    def test_desc_not_top6_grhow_yes(self):
        """
        Проверяем, что при grcutprice=shop есть группировка, а без - нет.
        """
        response = self.report.request_json("grcutprice=shop&place=productoffers&hyperid=1&show-cutprice=1&grhow=shop")
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "shop": {"id": 94035},
                    "isCutPrice": True,
                },
                {
                    "entity": "offer",
                    "shop": {"id": 94035},
                    "isCutPrice": False,
                },
            ],
            allow_different_len=False,
        )

        response = self.report.request_json(
            "place=productoffers&hyperid=1&show-cutprice=1&show-urls=encrypted&grhow=shop"
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "shop": {"id": 94035},
                    "isCutPrice": True,
                },
                {
                    "entity": "offer",
                    "shop": {"id": 94035},
                    "isCutPrice": True,
                },
                {
                    "entity": "offer",
                    "shop": {"id": 94035},
                    "isCutPrice": True,
                },
                {
                    "entity": "offer",
                    "shop": {"id": 94035},
                    "isCutPrice": True,
                },
                {
                    "entity": "offer",
                    "shop": {"id": 94035},
                    "isCutPrice": False,
                },
            ],
            allow_different_len=False,
        )

    def test_dynamic_statistics(self):
        """
        Проверяем, что с good-state=cutprice в prices и в cutprices статистики по б/у офферам
        """
        response = self.report.request_json("place=prime&hid=34567&show-cutprice=1&good-state=cutprice&rids=213")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "offers": {"count": 4, "cutPriceCount": 4},
                        "prices": {
                            "min": "50",
                            "max": "80",
                            "avg": "65",
                        },
                        "cutprices": {
                            "min": "50",
                            "max": "80",
                            "avg": "65",
                        },
                    }
                ]
            },
        )

        """
        Проверяем, что в таче с good-state=cutprice в cutprices статистика по б/у офферам, а блока prices нет.
        """
        response = self.report.request_json(
            "place=prime&hid=34567&show-cutprice=1&good-state=cutprice&rids=213&touch=1"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "offers": {"count": 4, "cutPriceCount": 4},
                        "prices": NoKey("prices"),
                        "cutprices": {
                            "min": "50",
                            "max": "80",
                            "avg": "65",
                        },
                    }
                ]
            },
        )

        """
        Проверяем, что с good-state=new в prices учитываются только новые офферы, а блока cutprices нет.
        """
        response = self.report.request_json("place=prime&rids=213&hid=34567&show-cutprice=1&good-state=new")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "offers": {"count": 2, "cutPriceCount": 0},
                        "prices": {
                            "min": "90",
                            "max": "100",
                            "avg": "93",  # from region stat
                        },
                        "cutprices": NoKey("cutprices"),
                    }
                ]
            },
        )
        """
        Проверяем, что без фильтра в prices статистики только по новым офферам, а в cutprices только по б/у
        """
        response = self.report.request_json("place=prime&hid=34567&rids=213&show-cutprice=1")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "offers": {"count": 2, "cutPriceCount": 4},
                        "prices": {
                            "min": "90",
                            "max": "100",
                            "avg": "93",  # from region stat
                            "currency": "RUR",
                        },
                        "cutprices": {
                            "min": "50",
                            "max": "80",
                            "avg": "65",
                            "currency": "RUR",
                        },
                    }
                ]
            },
        )

    @classmethod
    def prepare_model_filtering(cls):
        cls.index.shops += [
            Shop(fesh=1001, name="supermag.ru", priority_region=213),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=88),
        ]
        cls.index.models += [
            Model(hid=88, hyperid=30),
            Model(hid=88, hyperid=31),
            Model(hid=88, hyperid=32),
            Model(hid=88, hyperid=33),
            Model(hid=88, hyperid=34),
        ]

        cls.index.regional_models += [
            RegionalModel(
                hyperid=30, offers=100, price_min=50, price_max=80, rids=[213], cut_price_count=4
            ),  # и новые и уценённые
            RegionalModel(
                hyperid=31, offers=100, price_min=50, price_max=80, rids=[213], cut_price_count=0
            ),  # только новые
            RegionalModel(
                hyperid=32, offers=0, price_min=50, price_max=80, rids=[213], cut_price_count=10
            ),  # только уценённые
            RegionalModel(
                hyperid=33, offers=100, price_min=50, price_max=80, rids=[213], cut_price_count=4
            ),  # и новые и уценённые, но оффер только новый
            RegionalModel(
                hyperid=34, offers=100, price_min=50, price_max=80, rids=[213], cut_price_count=4
            ),  # и новые и уценённые, но оффер только уценённый
        ]

        cls.index.offers += [
            Offer(
                title="Mobile telephone 30 - cutprice",
                hid=88,
                is_cutprice=True,
                condition_reason="вскрытая упаковка",
                hyperid=30,
                fesh=1001,
                price=50,
            ),  # уцененный
            Offer(title="Mobile telephone 30 - new", hid=88, hyperid=30, fesh=1001, price=100),  # новый
            Offer(title="Mobile telephone 31 - new", hid=88, hyperid=31, fesh=1001, price=100),  # новый
            Offer(
                title="Mobile telephone 32 - cutprice",
                hid=88,
                is_cutprice=True,
                condition_reason="вскрытая упаковка",
                hyperid=32,
                fesh=1001,
                price=50,
            ),  # уцененный
            Offer(title="Mobile telephone 33 - new", hid=88, hyperid=33, fesh=1001, price=100),  # новый
            Offer(
                title="Mobile telephone 34 - cutprice",
                hid=88,
                is_cutprice=True,
                condition_reason="вскрытая упаковка",
                hyperid=34,
                fesh=1001,
                price=50,
            ),  # уцененный
        ]

    def test_model_filtering(self):
        """
        Проверяем, что, в выдаче есть только модель с cut_price_count > 0
        Модель с cut_price_count = 0 отсутствует
        """
        response = self.report.request_json("place=prime&hid=88&show-cutprice=1&good-state=cutprice&rids=213&numdoc=20")
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 30,
                    "prices": {},
                    "cutprices": {},
                },
                {
                    "entity": "product",
                    "id": 32,
                    # "prices": NoKey("prices"),
                    "cutprices": {},
                },
                {
                    "entity": "product",
                    "id": 33,
                },
                {
                    "entity": "product",
                    "id": 34,
                },
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 31,
                },
            ],
        )

    def test_dynamic_shop(self):
        response = self.report.request_json("place=prime&hid=88&show-cutprice=1&good-state=cutprice&rids=213&numdoc=20")
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {
                        "raw": "Mobile telephone 30 - cutprice",
                    },
                },
            ],
        )

        response = self.report.request_json(
            "place=prime&hid=34567&show-cutprice=1&good-state=cutprice&rids=213&numdoc=20"
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {
                        "raw": "Mobile telephone - cutprice",
                    },
                },
            ],
        )

        self.dynamic.market_dynamic.disabled_cutprice_shops += [DynamicShop(94035)]

        response = self.report.request_json("place=prime&hid=88&show-cutprice=1&good-state=cutprice&rids=213&numdoc=20")
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {
                        "raw": "Mobile telephone 30 - cutprice",
                    },
                },
            ],
        )

        response = self.report.request_json(
            "place=prime&hid=34567&show-cutprice=1&good-state=cutprice&rids=213&numdoc=20"
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "entity": "offer",
                    "titles": {
                        "raw": "Mobile telephone - cutprice",
                    },
                },
            ],
        )

    def test_onstock(self):
        """
        проверяем фильтр "в продаже"
        """

        onstock_req = "place=prime&hid=88&numdoc=20&rids=213&onstock=1"

        # включаем уценённые
        response = self.report.request_json(onstock_req + "&show-cutprice=1")
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 30,
                    "prices": {},
                    "cutprices": {},
                },
                {
                    "entity": "product",
                    "id": 31,
                    "prices": {},
                    "cutprices": NoKey("cutprices"),
                },
                {
                    "entity": "product",
                    "id": 32,
                    "prices": {},
                    "cutprices": {},
                },
                {
                    "entity": "product",
                    "id": 33,
                    "prices": {},
                    "cutprices": NoKey("cutprices"),
                },
                {
                    "entity": "product",
                    "id": 34,
                    "prices": {},
                    "cutprices": {},
                },
            ],
        )

        # не включаем уценённые. не находятся модели с только уценённыеми
        response = self.report.request_json(onstock_req)
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 30,
                },
                {
                    "entity": "product",
                    "id": 31,
                },
                {
                    "entity": "product",
                    "id": 33,
                },
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 32,
                },
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 34,
                },
            ],
        )

        # включаем уценённые, включаем фильтр по уценённые. Не находятся модели без уценённых
        response = self.report.request_json(onstock_req + "&show-cutprice=1&good-state=cutprice")
        self.assertFragmentIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 30,
                },
                {
                    "entity": "product",
                    "id": 32,
                },
                {
                    "entity": "product",
                    "id": 34,
                },
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 31,
                },
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    "entity": "product",
                    "id": 33,
                },
            ],
        )

    def test_show_and_click_log(self):
        """
        Проверяем, что в клик-лог и в шоу-лог записываются флаги
        """
        self.report.request_json("place=prime&hid=34567&show-cutprice=1&good-state=cutprice")
        self.show_log_tskv.expect(
            price=60, condition=CONDITION_LIKE_NEW, promo_type=PromoType.MASK_BY_NAME[PromoType.CUTPRICE]
        )
        self.show_log_tskv.expect(
            price=70, condition=CONDITION_USED, promo_type=PromoType.MASK_BY_NAME[PromoType.CUTPRICE]
        )
        self.show_log_tskv.expect(
            price=80, condition=CONDITION_UNKNOWN, promo_type=PromoType.MASK_BY_NAME[PromoType.CUTPRICE]
        )

        self.click_log.expect(price=60, cond=CONDITION_LIKE_NEW, promo_type=PromoType.MASK_BY_NAME[PromoType.CUTPRICE])
        self.click_log.expect(price=70, cond=CONDITION_USED, promo_type=PromoType.MASK_BY_NAME[PromoType.CUTPRICE])
        self.click_log.expect(price=80, cond=CONDITION_UNKNOWN, promo_type=PromoType.MASK_BY_NAME[PromoType.CUTPRICE])

    @classmethod
    def prepare_top6(cls):
        # Добавляем точно такие же офферы для другой модели - hyperid=2, hid=12345
        cls.index.offers += [
            Offer(
                title="Mobile telephone - cutprice",
                hid=12345,
                fesh=94035,
                is_cutprice=True,
                like_new=True,
                condition_reason="вскрытая упаковка",
                hyperid=2,
                price=50,
                glparams=[GLParam(param_id=202, value=1)],
                pickup=False,
            ),  # Уцененный
            Offer(
                title="Mobile telephone - prev.used",
                hid=12345,
                fesh=94035,
                is_cutprice=True,
                previously_used=True,
                condition_reason="следы эксплуатации",
                hyperid=2,
                price=70,
                glparams=[GLParam(param_id=202, value=3)],
                pickup=False,
            ),  # б/у
            Offer(
                title="Mobile telephone - new",
                hid=12345,
                fesh=94035,
                condition_reason="вскрытая упаковка",
                hyperid=2,
                price=100,
                glparams=[GLParam(param_id=202, value=1)],
                pickup=True,
            ),  # новый
            Offer(
                title="Mobile telephone - cutprice",
                hid=12345,
                fesh=94035,
                is_cutprice=True,
                like_new=True,
                condition_reason="вскрытая упаковка",
                hyperid=2,
                price=60,
                glparams=[GLParam(param_id=202, value=1)],
                pickup=False,
            ),  # Уцененный
            Offer(
                title="Mobile telephone - cutprice",
                hid=12345,
                fesh=94035,
                is_cutprice=True,
                like_new=True,
                condition_reason="вскрытая упаковка",
                hyperid=2,
                price=80,
                glparams=[GLParam(param_id=202, value=3)],
                pickup=False,
            ),  # Уцененный
        ]

    def test_top6_new(self):
        """
        Проверяем, что в productoffers с фильтром good-state=new возвращаются только новые офферы
        """
        response = self.report.request_json(
            "place=productoffers&hid=12345&hyperid=2&offers-set=defaultList,top&good-state=new&show-cutprice=1&pp=6"
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 1,
                "results": [
                    {"titles": {"raw": "Mobile telephone - new"}},  # default offer
                ],
            },
            allow_different_len=False,
        )

        """
        Проверяем, что для top6 подзапрос за new делается с нужным pp=6
        """
        self.show_log_tskv.expect(hyper_id=2, condition=CONDITION_NEW, pp=6)

        """
        Проверяем случай, что если default offer не найден с заданными glfilter'ами, то они выбрасываются и возвращаются все новые офферы
        """

        response = self.report.request_json(
            "place=productoffers&hid=34567&hyperid=1&offers-set=defaultList,top&good-state=new&glfilter=202:2&show-cutprice=1&pp=6"
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 2,
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                    },
                    {"titles": {"raw": "Mobile telephone - new"}},
                    {"titles": {"raw": "Mobile telephone - new"}},  # default offer
                ],
            },
            allow_different_len=False,
        )

        """
        Проверяем, что если есть default offer с заданными glfilters, то возвращаются все нужные офферы
        """
        response = self.report.request_json(
            "place=productoffers&hid=34567&hyperid=1&offers-set=defaultList,top&good-state=new&glfilter=202:1&show-cutprice=1&pp=6"
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 1,
                "results": [
                    {"titles": {"raw": "Mobile telephone - new"}},  # default offer
                ],
            },
            allow_different_len=False,
        )

    def test_top6_cutprice(self):
        """
        Проверяем, что c good-state=cutprice возвращаются только уцененные офферы
        """
        response = self.report.request_json(
            "place=productoffers&hid=12345&hyperid=2&offers-set=defaultList,top&good-state=cutprice&show-cutprice=1&pp=6"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - prev.used",
                        },
                        "isCutPrice": True,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                        "isCutPrice": True,
                    },
                    {"titles": {"raw": "Mobile telephone - cutprice"}},
                    {"titles": {"raw": "Mobile telephone - cutprice"}},
                    {"isCutPrice": True},  # default offer
                ]
            },
            allow_different_len=False,
        )

        """
        Проверяем, что для top6 подзапрос за cutprice делается с нужным pp=211
        """
        self.show_log_tskv.expect(hyper_id=2, condition=CONDITION_LIKE_NEW, pp=211)

        """
        Проверяем случай, что если default offer не найден с заданными glfilter'ами, то они выбрасываются и возвращаются все уцененнные
        """
        response = self.report.request_json(
            "place=productoffers&hid=34567&hyperid=1&offers-set=defaultList,top&good-state=cutprice&glfilter=202:2&show-cutprice=1&pp=6"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - prev.used",
                        },
                        "isCutPrice": True,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                        "isCutPrice": True,
                    },
                    {"titles": {"raw": "Mobile telephone - cutprice"}},
                    {"titles": {"raw": "Mobile telephone - cutprice II"}},
                    {"isCutPrice": True},  # default offer
                ]
            },
            allow_different_len=False,
        )

        """
        Проверяем, что если есть default offer с заданными glfilters, то возвращаются все нужные офферы
        """
        response = self.report.request_json(
            "place=productoffers&hid=34567&hyperid=1&offers-set=defaultList,top&good-state=cutprice&glfilter=202:1&show-cutprice=1&pp=6"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                        "isCutPrice": True,
                    },
                    {"titles": {"raw": "Mobile telephone - cutprice"}},
                    {"titles": {"raw": "Mobile telephone - cutprice"}},  # default offer
                ]
            },
            allow_different_len=False,
        )

    def test_top6_all(self):
        """
        Проверяем, что без good-state возвращаются все офферы
        """
        response = self.report.request_json(
            "place=productoffers&hid=34567&hyperid=1&offers-set=defaultList,top&show-cutprice=1&pp=6"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - prev.used",
                        },
                        "isCutPrice": True,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                        "isCutPrice": True,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                    },
                    {"titles": {"raw": "Mobile telephone - cutprice"}},
                    {"titles": {"raw": "Mobile telephone - cutprice II"}},
                    {"titles": {"raw": "Mobile telephone - new"}},  # default offer
                ]
            },
            allow_different_len=False,
        )

        """
        Проверяем случай, что если default offer не найден с заданными glfilter'ами, то они выбрасываются и возвращаются все офферы
        """
        response = self.report.request_json(
            "place=productoffers&hid=34567&hyperid=1&offers-set=defaultList,top&glfilter=202:2&show-cutprice=1&pp=6"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - prev.used",
                        },
                        "isCutPrice": True,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                        "isCutPrice": True,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                    },
                    {"titles": {"raw": "Mobile telephone - cutprice"}},
                    {"titles": {"raw": "Mobile telephone - cutprice II"}},
                    {"titles": {"raw": "Mobile telephone - new"}},  # default offer
                ]
            },
            allow_different_len=False,
        )

        """
        Проверяем, что если есть default offer с заданными glfilters, то возвращаются все нужные офферы
        """
        response = self.report.request_json(
            "place=productoffers&hid=34567&hyperid=1&offers-set=defaultList,top&glfilter=202:1&show-cutprice=1&pp=6"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                        "isCutPrice": True,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                    },
                    {"titles": {"raw": "Mobile telephone - cutprice"}},
                    {"titles": {"raw": "Mobile telephone - new"}},  # default offer
                ]
            },
            allow_different_len=False,
        )

    def test_top6_all_with_grhow(self):
        """
        Проверяем, что при grhow=shop офферы группируются по магазину и состоянию товара
        """
        response = self.report.request_json(
            "grhow=shop&place=productoffers&hid=34567&hyperid=1&offers-set=defaultList,top&show-cutprice=1&pp=6"
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'offer',
                        'shop': {'id': 94035},
                        'condition': Absent(),  # не уцененный
                        'bundleCount': 2,
                        'benefit': Absent(),
                    },
                    {
                        'entity': 'offer',
                        'shop': {'id': 94035},
                        'condition': NotEmpty(),  # уцененный, bundleCount считается отдельно
                        'bundleCount': 4,
                        'benefit': Absent(),
                    },
                    {
                        'entity': 'offer',
                        'benefit': NotEmpty(),  # премиальный оффер
                        'bundleCount': 2,  # из группы не уцененных
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_top6_touch(self):
        """
        Проверяем, что в productoffers в таче без good-state возвращаются только новые офферы (конда есть и те и те)
        """
        response = self.report.request_json(
            "place=productoffers&hid=12345&hyperid=2&offers-set=defaultList,top&show-cutprice=1&pp=6&touch=1"
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 1,
                "results": [
                    {"titles": {"raw": "Mobile telephone - new"}},  # default offer
                ],
            },
            allow_different_len=False,
        )

        """
        Проверяем, что в productoffers в таче без good-state возвращаются только новые офферы (есть только новые)
        """
        response = self.report.request_json(
            "place=productoffers&hid=345&hyperid=5&offers-set=defaultList,top&show-cutprice=1&pp=6&touch=1"
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 2,
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                    },
                    {"titles": {"raw": "Mobile telephone - new"}},  # default offer
                ],
            },
            allow_different_len=False,
        )

        """
        Проверяем, что в productoffers в таче без good-state возвращаются только уценённые офферы (есть только уценённые)
        """
        response = self.report.request_json(
            "place=productoffers&hid=789&hyperid=6&offers-set=defaultList,top&show-cutprice=1&pp=6&touch=1"
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 2,
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                        "isCutPrice": True,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                        "isCutPrice": True,
                    },
                    {"titles": {"raw": "Mobile telephone - cutprice"}},  # default offer
                ],
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_not_top6(cls):
        # Добавляем новые офферы для проверки вкладки "цены" - hyperid=3, hid=23456
        cls.index.offers += [
            Offer(
                title="Mobile telephone - cutprice",
                hid=23456,
                fesh=94035,
                is_cutprice=True,
                like_new=True,
                condition_reason="вскрытая упаковка",
                hyperid=3,
                price=50,
                glparams=[GLParam(param_id=202, value=1)],
                pickup=False,
            ),
            Offer(
                title="Mobile telephone - prev.used",
                hid=23456,
                fesh=94035,
                is_cutprice=True,
                previously_used=True,
                condition_reason="следы эксплуатации",
                hyperid=3,
                price=70,
                glparams=[GLParam(param_id=202, value=3)],
                pickup=False,
            ),
            Offer(
                title="Mobile telephone - new",
                hid=23456,
                fesh=94035,
                hyperid=3,
                price=100,
                glparams=[GLParam(param_id=202, value=1)],
                pickup=True,
            ),
            Offer(
                title="Mobile telephone - new",
                hid=23456,
                fesh=94035,
                hyperid=3,
                price=90,
                glparams=[GLParam(param_id=202, value=1)],
                pickup=True,
            ),
            Offer(
                title="Mobile telephone - new",
                hid=23456,
                fesh=94035,
                hyperid=3,
                price=80,
                glparams=[GLParam(param_id=202, value=1)],
                pickup=True,
            ),
            Offer(
                title="Mobile telephone - new",
                hid=23456,
                fesh=94035,
                hyperid=3,
                price=70,
                glparams=[GLParam(param_id=202, value=1)],
                pickup=True,
            ),
            Offer(
                title="Mobile telephone - cutprice",
                hid=23456,
                fesh=94035,
                is_cutprice=True,
                like_new=True,
                condition_reason="вскрытая упаковка",
                hyperid=3,
                price=60,
                glparams=[GLParam(param_id=202, value=1)],
                pickup=False,
            ),
            Offer(
                title="Mobile telephone - cutprice",
                hid=23456,
                fesh=94035,
                is_cutprice=True,
                like_new=True,
                condition_reason="вскрытая упаковка",
                hyperid=3,
                price=80,
                glparams=[GLParam(param_id=202, value=3)],
                pickup=False,
            ),
        ]

    def test_not_top6_new(self):
        """
        Проверяем, что в productoffers с фильтром good-state=new возвращаются только новые офферы без default_offer
        """
        response = self.report.request_json(
            "place=productoffers&hid=23456&hyperid=3&good-state=new&show-cutprice=1&pp=21"
        )
        self.assertFragmentIn(
            response,
            {
                "totalOffers": 4,
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - new",
                        },
                        "isCutPrice": False,
                    },
                    {"isCutPrice": False},
                    {"isCutPrice": False},
                    {"isCutPrice": False},
                ],
            },
            allow_different_len=False,
        )

        """
        Проверяем, что с grhow=shop новые офферы схлопываются как и раньше
        """
        response = self.report.request_json(
            "place=productoffers&hid=23456&hyperid=3&good-state=new&show-cutprice=1&pp=21&grhow=shop"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"isCutPrice": False},
                ]
            },
            allow_different_len=False,
        )

        """
        Проверяем, что для не топ6 запрос за new делается с нужным pp=21
        """
        self.show_log_tskv.expect(hyper_id=3, condition=CONDITION_NEW, pp=21)

    def test_not_top6_cutprice(self):
        """
        Проверяем, что c good-state=cutprice возвращаются только уцененные офферы без default_offer
        """
        response = self.report.request_json(
            "place=productoffers&hid=23456&hyperid=3&good-state=cutprice&show-cutprice=1&pp=21"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - prev.used",
                        },
                        "isCutPrice": True,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                        "isCutPrice": True,
                    },
                    {"titles": {"raw": "Mobile telephone - cutprice"}},
                    {"titles": {"raw": "Mobile telephone - cutprice"}},
                ]
            },
            allow_different_len=False,
        )

        """
        Проверяем, что при grhow=shop уцененные офферы не схлопываются
        """
        response = self.report.request_json(
            "place=productoffers&hid=23456&hyperid=3&good-state=cutprice&show-cutprice=1&pp=21&grhow=shop"
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - prev.used",
                        },
                        "isCutPrice": True,
                    },
                    {
                        "entity": "offer",
                        "titles": {
                            "raw": "Mobile telephone - cutprice",
                        },
                        "isCutPrice": True,
                    },
                    {"titles": {"raw": "Mobile telephone - cutprice"}},
                    {"titles": {"raw": "Mobile telephone - cutprice"}},
                ]
            },
            allow_different_len=False,
        )

        """
        Проверяем, что запрос за cutprice офферами делается с pp=214
        """
        self.show_log_tskv.expect(hyper_id=3, condition=CONDITION_LIKE_NEW, pp=214)

    def test_not_top6_all(self):
        """
        Проверяем, что без good-state возвращаются все новые офферы на заданной странице,
        а также первая страница уцененных офферов, без default_offer
        """
        # 3 новых офферов и 3 уцененных
        response = self.report.request_json(
            "place=productoffers&hid=23456&hyperid=3&show-cutprice=1&pp=21&page=1&numdoc=3"
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"isCutPrice": False},
                    {"isCutPrice": False},
                    {"isCutPrice": False},
                    {"isCutPrice": True},
                    {"isCutPrice": True},
                    {"isCutPrice": True},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        # 1 новый оффер со второй страницы и все уцененные офферы с первой страницы
        response = self.report.request_json(
            "place=productoffers&hid=23456&hyperid=3&show-cutprice=1&pp=21&page=2&numdoc=3"
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"isCutPrice": False},
                    {"isCutPrice": True},
                    {"isCutPrice": True},
                    {"isCutPrice": True},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        """
        Проверяем, что новые офферы схлопываются, а уцененные нет
        """
        response = self.report.request_json(
            "place=productoffers&hid=23456&hyperid=3&show-cutprice=1&pp=21&numdoc=3&grhow=shop"
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"isCutPrice": False},
                    {"isCutPrice": True},
                    {"isCutPrice": True},
                    {"isCutPrice": True},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    @classmethod
    def prepare_offer_without_url(cls):
        cls.index.offers += [
            Offer(
                title="Mobile telephone - cutprice",
                hid=777,
                fesh=800,
                is_cutprice=True,
                condition_reason="вскрытая упаковка",
                hyperid=300,
                price=50,
                glparams=[GLParam(param_id=202, value=1)],
                pickup=False,
                has_url=False,
            )
        ]
        cls.index.shops += [Shop(fesh=800, phone="+7222998989")]

    def test_offer_without_url(self):
        """
        Проверяем, что оффер без урла возвращается со ссылкой на телефон
        """
        response = self.report.request_json("place=productoffers&hid=777&hyperid=300&show-cutprice=1&pp=21")
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "titles": {"raw": "Mobile telephone - cutprice"},
                "shop": {"phones": {"raw": "+7222998989", "sanitized": "+7222998989"}},
            },
        )

    def test_hide_phone_disabled(self):
        """
        Проверяем, что оффер без урла не возвращается, если показ телефона выключен
        """
        self.dynamic.market_dynamic.hidden_phone_shops += [DynamicShop(800)]

        response = self.report.request_json("place=productoffers&hid=777&hyperid=300&show-cutprice=1&pp=21")
        self.assertFragmentNotIn(
            response,
            {
                "entity": "offer",
            },
        )

    @classmethod
    def prepare_no_cutprice_on_geo(cls):
        cls.index.hypertree += [
            HyperCategory(hid=730),
        ]
        cls.index.models += [
            Model(hyperid=73, hid=730),
            Model(hyperid=74, hid=730),
        ]
        cls.index.shops += [
            Shop(fesh=7301, priority_region=1, name='Shop_cutprice', pickup_buckets=[5001]),
            Shop(fesh=7302, priority_region=1, name='Shop_nocutprice', pickup_buckets=[5002]),
        ]

        cls.index.outlets += [
            Outlet(point_id=7301, fesh=7301, region=1, gps_coord=GpsCoord(30.0, 50.0)),
            Outlet(point_id=7302, fesh=7302, region=1, gps_coord=GpsCoord(30.1, 50.1)),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=7301,
                carriers=[99],
                options=[PickupOption(outlet_id=7301)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=7302,
                carriers=[99],
                options=[PickupOption(outlet_id=7302)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=73,
                title='maptest is_cp',
                price=500,
                fesh=7301,
                is_cutprice=True,
                condition_reason="вскрытая упаковка",
            ),
            Offer(hyperid=73, title='maptest no_cp', price=600, fesh=7302, is_cutprice=False),
        ]

    def test_no_cutprice_on_geo(self):
        """
        Проверяем, что на карте есть только магазин с нвоым товаром, и нет с уцененным
        """
        response = self.report.request_json(
            'place=geo&geo-location=30.0,50.0&geo_bounds_lb=29.9,49.9&geo_bounds_rt=30.2,50.2&rids=1&text=maptest&show-cutprice=1'
        )

        self.assertFragmentIn(response, [{"entity": "offer", "titles": {"raw": "maptest no_cp"}}])

        self.assertFragmentNotIn(response, [{"entity": "offer", "titles": {"raw": "maptest is_cp"}}])

    @classmethod
    def prepare_default_offer_shop(cls):
        cls.index.hypertree += [
            HyperCategory(hid=740),
        ]

        cls.index.models += [
            Model(hyperid=80, hid=740),
        ]
        cls.index.shops += [
            Shop(fesh=901, priority_region=1, name='Shop_cutprice'),
            Shop(fesh=902, priority_region=1, name='Shop_nocutprice'),
        ]

        cls.index.offers += [
            Offer(
                hyperid=80,
                title='maptest is_cp',
                price=500,
                fesh=901,
                is_cutprice=True,
                condition_reason="вскрытая упаковка",
            ),
            Offer(hyperid=80, title='maptest no_cp', price=600, fesh=902, is_cutprice=False),
        ]

    def test_default_offer(self):
        """
        Проверяем фильтрацию по магазину в дефолтном оффере
        """
        response = self.report.request_json("place=productoffers&offer-set=default&hyperid=80&fesh=901&show-cutprice=1")
        self.assertFragmentIn(response, {"entity": "offer", "shop": {"id": 901}})

    @classmethod
    def prepare_regional_delimiter(cls):
        cls.index.hypertree += [
            HyperCategory(hid=750),
        ]
        cls.index.models += [
            Model(hyperid=90, hid=750),
        ]
        cls.index.shops += [
            Shop(fesh=911, priority_region=1, name='Shop_cutprice'),
            Shop(fesh=912, priority_region=2, name='Shop_nocutprice'),
        ]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=211,
                fesh=912,
                carriers=[1, 3],
                regional_options=[
                    RegionalDelivery(
                        rid=1,
                        options=[
                            DeliveryOption(price=500, day_from=0, day_to=0, order_before=23),
                        ],
                    )
                ],
            )
        ]

        cls.index.offers += [
            Offer(
                hyperid=90,
                title='maptest is_cp',
                price=500,
                fesh=911,
                is_cutprice=True,
                condition_reason="вскрытая упаковка",
            ),
            Offer(
                hyperid=90,
                title='maptest no_cp',
                price=600,
                fesh=912,
                is_cutprice=True,
                condition_reason="вскрытая упаковка",
                delivery_buckets=[211],
            ),
        ]

    def test_regional_delimeter(self):
        """
        проверяем, что черты в уцененных нет
        """
        response = self.report.request_json(
            "place=productoffers&hyperid=90&show-cutprice=1&local-offers-first=1&rids=1&offers-set=list"
        )
        self.assertFragmentNotIn(response, {"entity": "regionalDelimiter"})

    @classmethod
    def prepare_default_offer_without_delivery_options(cls):
        cls.index.hypertree += [
            HyperCategory(hid=745),
        ]

        cls.index.models += [
            Model(hyperid=900, hid=745),
        ]
        cls.index.shops += [
            Shop(fesh=9101, priority_region=1, name='Shop_cutprice'),
            Shop(fesh=9102, priority_region=1, name='Shop_nocutprice'),
        ]

        cls.index.offers += [
            Offer(
                hyperid=900,
                title='maptest is_cp',
                price=500,
                fesh=9101,
                is_cutprice=True,
                condition_reason="вскрытая упаковка",
                delivery_options=[DeliveryOption(day_to=0, price=1000)],
            ),
            Offer(
                hyperid=900, title='maptest no_cp', price=600, fesh=9102, is_cutprice=False, has_delivery_options=False
            ),
        ]

    def test_default_offer_without_delivery_options(self):
        """
        Проверяем фильтрацию по магазину в дефолтном оффере
        """
        response = self.report.request_json("place=productoffers&offer-set=default&hyperid=900&show-cutprice=1")
        self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": "maptest no_cp"}, "shop": {"id": 9102}})

    def test_touch_filter_modelcard(self):
        """
        есть и новые и уценённые, фильтр выставляется в "новые"
        """
        response = self.report.request_json("place=productoffers&hyperid=1&show-cutprice=1&touch=1")

        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "good-state",
                        "name": "Состояние товара",
                        "subType": "radio",
                        "hasBoolNo": NoKey("hasBoolNo"),
                        "values": [
                            {
                                "checked": True,
                                "value": "new",
                            },
                            {
                                "value": "cutprice",
                            },
                        ],
                    }
                ],
            },
        )

        """
            есть только новые, фильтра нет
        """
        response = self.report.request_json("place=productoffers&hyperid=5&show-cutprice=1&touch=1")

        self.assertFragmentNotIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "good-state",
                        "name": "Состояние товара",
                    }
                ],
            },
        )

        """
            есть тлько уценённые, фильтр выставляется в "уценённые"
        """
        response = self.report.request_json("place=productoffers&hyperid=6&show-cutprice=1&touch=1")

        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "good-state",
                        "name": "Состояние товара",
                        "subType": "radio",
                        "hasBoolNo": NoKey("hasBoolNo"),
                        "values": [
                            {
                                "checked": True,
                                "value": "cutprice",
                            }
                        ],
                    }
                ],
            },
        )

        """
            на десктопе - есть hasBoolNo, нет subType, состояние фильтра не выбрано
        """
        response = self.report.request_json("place=productoffers&hyperid=1&show-cutprice=1")
        self.assertFragmentIn(
            response,
            {
                "search": {},
                "filters": [
                    {
                        "id": "good-state",
                        "name": "Состояние товара",
                        "subType": "",
                        "hasBoolNo": True,
                        "values": [
                            {
                                "checked": NoKey("checked"),
                                "value": "new",
                            },
                            {
                                "checked": NoKey("checked"),
                                "value": "cutprice",
                            },
                        ],
                    }
                ],
            },
        )

    @classmethod
    def prepare_many_models(cls):
        cls.index.shops += [
            Shop(fesh=10001, name="supermag.ru", priority_region=213),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=1000),
        ]

        for i in range(100):
            hyperid = 1000 + i
            cls.index.models += [
                Model(hid=1000, hyperid=hyperid),
            ]
            cls.index.regional_models += [
                RegionalModel(
                    hyperid=hyperid, offers=0, rids=[213], cut_price_count=4, cut_price_min=60
                ),  # только уценённые
            ]
            cls.index.offers += [
                Offer(
                    title="Mobile telephone %d - cutprice" % hyperid,
                    hid=1000,
                    is_cutprice=True,
                    condition_reason="вскрытая упаковка",
                    hyperid=hyperid,
                    fesh=10001,
                    price=50,
                ),  # уцененный
                # Offer(title="Mobile telephone %d - new" % hyperid, hid=1000, hyperid=hyperid, fesh=10001, price=50), # уцененный
            ]

    def test_many_models_cutprice(self):
        """
        status quo
        """

        req = "place=prime&hid=1000&rids=213&onstock=1&local-offers-first=0&numdoc=10&show-cutprice=1" + "&touch=1"

        # проверяем, что в первых моделях цена есть (берётся из оффера)
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "prices": NoKey("prices"),
                        "cutprices": {"max": "50"},
                    }
                    for i in range(10)
                ]
            },
            allow_different_len=False,
        )

        # проверяем, что к концу цена есть и берётся из статистики
        response = self.report.request_json(req + "&page=5")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "product",
                        "prices": NoKey("prices"),
                        "cutprices": {"max": "60"},
                    }
                    for i in range(10)
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_auto_discount(cls):
        cls.index.shops += [
            Shop(fesh=20001),
            Shop(fesh=20002),
            Shop(fesh=20003),
            Shop(fesh=20004),
        ]

        cls.index.models += [
            Model(hid=20010, title="Mobile phone", hyperid=20000),
        ]

        cls.index.offers += [
            Offer(
                title="offer1",
                fesh=20001,
                hyperid=20000,
                enable_auto_discounts=1,
                price=100000,
                price_old=150000,
                price_history=120000,
            ),
            Offer(
                title="offer2",
                fesh=20002,
                hyperid=20000,
                enable_auto_discounts=1,
                price=100000,
                price_old=150000,
                price_history=120000,
                is_cutprice=True,
                condition_reason="вскрытая упаковка",
            ),
            Offer(
                title="offer3", fesh=20003, hyperid=20000, enable_auto_discounts=1, price=100000, price_history=120000
            ),
            Offer(
                title="offer4",
                fesh=20004,
                hyperid=20000,
                enable_auto_discounts=1,
                price=100000,
                price_history=120000,
                is_cutprice=True,
                condition_reason="вскрытая упаковка",
            ),
        ]

    def test_auto_discount(self):
        response = self.report.request_json('place=prime&fesh=20001&show-cutprice=1')
        self.assertFragmentIn(
            response, {"titles": {"raw": "offer1"}, "prices": {"value": "100000", "discount": {"percent": 17}}}
        )

        response = self.report.request_json('place=prime&fesh=20002&show-cutprice=1')
        self.assertFragmentIn(
            response, {"titles": {"raw": "offer2"}, "prices": {"value": "100000", "discount": Absent()}}
        )

        response = self.report.request_json('place=prime&fesh=20003&show-cutprice=1')
        self.assertFragmentIn(
            response, {"titles": {"raw": "offer3"}, "prices": {"value": "100000", "discount": {"percent": 17}}}
        )

        response = self.report.request_json('place=prime&fesh=20004&show-cutprice=1')
        self.assertFragmentIn(
            response, {"titles": {"raw": "offer4"}, "prices": {"value": "100000", "discount": Absent()}}
        )

    @classmethod
    def prepare_price_relev(cls):
        cls.index.shops += [Shop(fesh=39, priority_region=213)]

        cls.index.models += [
            Model(hyperid=391, hid=210, title='model with min_price=1000 min_cutprice=600'),
            Model(hyperid=392, hid=210, title='model with min_price=800 min_cutprice=1000'),
            Model(hyperid=393, hid=210, title='model with min_cutprice=2000'),
            Model(hyperid=394, hid=210, title='model with min_price=900'),
        ]

        cls.index.offers += [
            Offer(hyperid=391, fesh=39, price=1000),
            Offer(
                hyperid=391,
                fesh=39,
                price=600,
                is_cutprice=True,
                previously_used=True,
                condition_reason="вскрытая упаковка",
                pickup=False,
            ),
            Offer(hyperid=392, fesh=39, price=800),
            Offer(
                hyperid=392,
                fesh=39,
                price=1000,
                is_cutprice=True,
                previously_used=True,
                condition_reason="вскрытая упаковка",
                pickup=False,
            ),
            Offer(
                hyperid=393,
                fesh=39,
                price=2000,
                is_cutprice=True,
                previously_used=True,
                condition_reason="вскрытая упаковка",
                pickup=False,
            ),
            Offer(hyperid=394, fesh=39, price=900),
            Offer(hid=210, fesh=39, price=500, title='offer with price=500'),
            Offer(
                hid=210,
                fesh=39,
                price=700,
                title='cutprice offer with price=700',
                is_cutprice=True,
                previously_used=True,
                condition_reason="вскрытая упаковка",
                pickup=False,
            ),
        ]

    def test_price_relev__show_cutprice_0(self):
        """
        Модели должны ранжироваться по цене новых офферов.
        Уценка в выдачу не попадает.
        good-state на выдачу не влияет.
        """

        def check(good_state):
            request = 'how=aprice&onstock=1&place=prime&hid=210&rids=213&allow-collapsing=1&debug=da&show-cutprice=0'
            if good_state:
                request += '&good-state=' + good_state
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                [
                    {
                        "titles": {"raw": "offer with price=500"},
                        "debug": {
                            "rank": [{"name": "PRICE", "value": "50000"}],
                            "metaRank": [{"name": "PRICE", "value": "50001"}],
                        },
                    },
                    {
                        "id": 392,
                        "titles": {"raw": "model with min_price=800 min_cutprice=1000"},
                        "debug": {
                            "rank": [{"name": "PRICE", "value": "80000"}],
                            "metaRank": [{"name": "PRICE", "value": "80001"}],
                        },
                    },
                    {
                        "id": 394,
                        "titles": {"raw": "model with min_price=900"},
                        "debug": {
                            "rank": [{"name": "PRICE", "value": "90000"}],
                            "metaRank": [{"name": "PRICE", "value": "90001"}],
                        },
                    },
                    {
                        "id": 391,
                        "titles": {"raw": "model with min_price=1000 min_cutprice=600"},
                        "debug": {
                            "rank": [{"name": "PRICE", "value": "100000"}],
                            "metaRank": [{"name": "PRICE", "value": "100001"}],
                        },
                    },
                ],
                preserve_order=True,
            )

        check(good_state=None)
        check(good_state="cutprice")
        check(good_state="new")

    def test_price_relev__show_cutprice_1(self):
        """
        Модели должны ранжироваться по цене новых офферов, если они есть, иначе по цене уценки.
        """
        response = self.report.request_json(
            'how=aprice&onstock=1&place=prime&hid=210&rids=213&allow-collapsing=1&debug=da' '&show-cutprice=1'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "titles": {"raw": "offer with price=500"},
                    "debug": {
                        "rank": [{"name": "PRICE", "value": "50000"}],
                        "metaRank": [{"name": "PRICE", "value": "50001"}],
                    },
                },
                {
                    "titles": {"raw": "cutprice offer with price=700"},
                    "debug": {
                        "rank": [{"name": "PRICE", "value": "70000"}],
                        "metaRank": [{"name": "PRICE", "value": "70001"}],
                    },
                },
                {
                    "id": 392,
                    "titles": {"raw": "model with min_price=800 min_cutprice=1000"},
                    "debug": {
                        "rank": [{"name": "PRICE", "value": "80000"}],
                        "metaRank": [{"name": "PRICE", "value": "80001"}],
                    },
                },
                {
                    "id": 394,
                    "titles": {"raw": "model with min_price=900"},
                    "debug": {
                        "rank": [{"name": "PRICE", "value": "90000"}],
                        "metaRank": [{"name": "PRICE", "value": "90001"}],
                    },
                },
                {
                    "id": 391,
                    "titles": {"raw": "model with min_price=1000 min_cutprice=600"},
                    "debug": {
                        "rank": [{"name": "PRICE", "value": "100000"}],
                        "metaRank": [{"name": "PRICE", "value": "100001"}],
                    },
                },
                {
                    "id": 393,
                    "titles": {"raw": "model with min_cutprice=2000"},
                    "debug": {
                        "rank": [{"name": "PRICE", "value": "200000"}],
                        "metaRank": [{"name": "PRICE", "value": "200001"}],
                    },
                },
            ],
            preserve_order=True,
        )

    def test_price_relev__good_state_new(self):
        """
        Модели должны ранжироваться по цене новых офферов.
        Уценка в выдачу не попадает.
        """
        response = self.report.request_json(
            'how=aprice&onstock=1&place=prime&hid=210&rids=213&allow-collapsing=1&debug=da'
            '&show-cutprice=1&good-state=new'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "titles": {"raw": "offer with price=500"},
                    "debug": {
                        "rank": [{"name": "PRICE", "value": "50000"}],
                        "metaRank": [{"name": "PRICE", "value": "50001"}],
                    },
                },
                {
                    "id": 392,
                    "titles": {"raw": "model with min_price=800 min_cutprice=1000"},
                    "debug": {
                        "rank": [{"name": "PRICE", "value": "80000"}],
                        "metaRank": [{"name": "PRICE", "value": "80001"}],
                    },
                },
                {
                    "id": 394,
                    "titles": {"raw": "model with min_price=900"},
                    "debug": {
                        "rank": [{"name": "PRICE", "value": "90000"}],
                        "metaRank": [{"name": "PRICE", "value": "90001"}],
                    },
                },
                {
                    "id": 391,
                    "titles": {"raw": "model with min_price=1000 min_cutprice=600"},
                    "debug": {
                        "rank": [{"name": "PRICE", "value": "100000"}],
                        "metaRank": [{"name": "PRICE", "value": "100001"}],
                    },
                },
            ],
            preserve_order=True,
        )

    def test_price_relev__good_state_cutprice(self):
        """
        Модели должны ранжироваться по цене уценки.
        Новые офферы в выдаче отсутствуют.
        """
        response = self.report.request_json(
            'how=aprice&onstock=1&place=prime&hid=210&rids=213&allow-collapsing=1&debug=da'
            '&show-cutprice=1&good-state=cutprice'
        )
        self.assertFragmentIn(
            response,
            [
                {
                    "id": 391,
                    "titles": {"raw": "model with min_price=1000 min_cutprice=600"},
                    "debug": {
                        "rank": [{"name": "PRICE", "value": "60000"}],
                        "metaRank": [{"name": "PRICE", "value": "60001"}],
                    },
                },
                {
                    "titles": {"raw": "cutprice offer with price=700"},
                    "debug": {
                        "rank": [{"name": "PRICE", "value": "70000"}],
                        "metaRank": [{"name": "PRICE", "value": "70001"}],
                    },
                },
                {
                    "id": 392,
                    "titles": {"raw": "model with min_price=800 min_cutprice=1000"},
                    "debug": {
                        "rank": [{"name": "PRICE", "value": "100000"}],
                        "metaRank": [{"name": "PRICE", "value": "100001"}],
                    },
                },
                {
                    "id": 393,
                    "titles": {"raw": "model with min_cutprice=2000"},
                    "debug": {
                        "rank": [{"name": "PRICE", "value": "200000"}],
                        "metaRank": [{"name": "PRICE", "value": "200001"}],
                    },
                },
            ],
            preserve_order=True,
        )

    @classmethod
    def prepare_not_priority_delivery_relevance(cls):
        cls.index.models += [
            Model(hyperid=901, hid=900, title="search me: model with new and cutprice offers"),
            Model(hyperid=902, hid=900, title="search me: model with new offer"),
            Model(hyperid=903, hid=900, title="search me: model with cutprice offer"),
        ]
        cls.index.offers += [
            Offer(hyperid=901, is_cutprice=True),
            Offer(hyperid=901),
            Offer(hyperid=902),
            Offer(hyperid=903, is_cutprice=True),
        ]
        # Нам важно, чтобы у моделей было local_offers=0 (local_offers>0 означает приоритетную доставку).
        # Если не указать rids, то lite добавит к региональным статистикам инф-у об офферах в индексе,
        # и у рег. статистик станет local_offers>0
        cls.index.regional_models += [
            RegionalModel(hyperid=901, local_offers=0, offers=1, cut_price_count=1, rids=[213]),
            RegionalModel(hyperid=902, local_offers=0, offers=1, cut_price_count=0, rids=[213]),
            RegionalModel(hyperid=903, local_offers=0, offers=0, cut_price_count=1, rids=[213]),
        ]

    def test_not_priority_delivery_relevance__show_cutprice_0(self):
        """
        Проверяем, что модельные статистики учитываются правильно в DELIVERY_TYPE
        в базовой и мета-релевантности.
        Модели в индексе не находятся в приоритетном регионе доставки, чтобы доставка высчитывалась,
        исходя из количества офферов моделей.
        Проверяются несхлопнутые модели, так как у схлопнутых моделей будут стоять
        релевантности из офферов.

        Проверяем:
        - при show-cutprice=0 параметр good-state не должен влиять на выдачу.
        - у модели 903 только с уценочным оффером DELIVERY_TYPE=1 (EXISTS), потому что
          при show-cutprice=0 уценка не должна учитываться
        - у остальных документов в выдаче DELIVERY_TYPE=2 (COUNTRY)
        """
        # Запрос текстовый, чтобы не нагрести офферы, по которым схлопнутся модели.
        # local-offers-first=1 нужен для того, чтобы сортировка по delivery_type заработала,
        # иначе всем документам выставится одинаковый delivery_type.
        def check(good_state):
            request = (
                "rids=213&debug=da&place=prime&hid=900&text=search+me&allow-collapsing=1"
                "&show-cutprice=0&local-offer-first=1"
            )
            if good_state:
                request += "&good-state=" + good_state
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "id": 901,
                                "titles": {"raw": "search me: model with new and cutprice offers"},
                                "debug": {
                                    "rank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                    "metaRank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                    "isCollapsed": False,
                                },
                            },
                            {
                                "id": 902,
                                "titles": {"raw": "search me: model with new offer"},
                                "debug": {
                                    "rank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                    "metaRank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                    "isCollapsed": False,
                                },
                            },
                            {
                                "id": 903,
                                "titles": {"raw": "search me: model with cutprice offer"},
                                "debug": {
                                    "rank": [{"name": "DELIVERY_TYPE", "value": "1"}],
                                    "metaRank": [{"name": "DELIVERY_TYPE", "value": "1"}],
                                    "isCollapsed": False,
                                },
                            },
                        ]
                    }
                },
            )

        check(good_state=None)
        check(good_state="new")
        check(good_state="cutprice")

    def test_not_priority_delivery_relevance__show_cutprice_1(self):
        """
        Проверяем, что модельные статистики учитываются правильно в DELIVERY_TYPE
        в базовой и мета-релевантности.
        Модели в индексе не находятся в приоритетном регионе доставки, чтобы доставка высчитывалась,
        исходя из количества офферов моделей.
        Проверяются несхлопнутые модели, так как у схлопнутых моделей будут стоять
        релевантности из офферов.

        Проверяем, что у всех моделей DELIVERY_TYPE=2 (COUNTRY).
        """
        # Запрос текстовый, чтобы не нагрести офферы, по которым схлопнутся модели.
        # local-offers-first=1 нужен для того, чтобы сортировка по delivery_type заработала,
        # иначе всем документам выставится одинаковый delivery_type.
        request = (
            "rids=213&debug=da&place=prime&hid=900&text=search+me&allow-collapsing=1"
            "&show-cutprice=1&local-offer-first=1"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "id": 901,
                            "titles": {"raw": "search me: model with new and cutprice offers"},
                            "debug": {
                                "rank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                "metaRank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                "isCollapsed": False,
                            },
                        },
                        {
                            "id": 902,
                            "titles": {"raw": "search me: model with new offer"},
                            "debug": {
                                "rank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                "metaRank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                "isCollapsed": False,
                            },
                        },
                        {
                            "id": 903,
                            "titles": {"raw": "search me: model with cutprice offer"},
                            "debug": {
                                "rank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                "metaRank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                "isCollapsed": False,
                            },
                        },
                    ]
                }
            },
        )

    def test_not_priority_delivery_relevance__good_state_new(self):
        """
        Проверяем, что модельные статистики учитываются правильно в DELIVERY_TYPE
        в базовой и мета-релевантности.
        Модели в индексе не находятся в приоритетном регионе доставки, чтобы доставка высчитывалась,
        исходя из количества офферов моделей.
        Проверяются несхлопнутые модели, так как у схлопнутых моделей будут стоять
        релевантности из офферов.

        Проверяем, что у всех моделей DELIVERY_TYPE=2 (COUNTRY).
        """
        # Запрос текстовый, чтобы не нагрести офферы, по которым схлопнутся модели.
        # local-offers-first=1 нужен для того, чтобы сортировка по delivery_type заработала.
        # Иначе всем документам выставится одинаковый delivery_type.
        request = (
            "rids=213&debug=da&place=prime&hid=900&text=search+me&allow-collapsing=1"
            "&show-cutprice=1&good-state=new&local-offer-first=1"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "id": 901,
                            "titles": {"raw": "search me: model with new and cutprice offers"},
                            "debug": {
                                "rank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                "metaRank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                "isCollapsed": False,
                            },
                        },
                        {
                            "id": 902,
                            "titles": {"raw": "search me: model with new offer"},
                            "debug": {
                                "rank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                "metaRank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                "isCollapsed": False,
                            },
                        },
                    ]
                }
            },
        )

    def test_not_priority_delivery_relevance__good_state_cutprice(self):
        """
        Проверяем, что модельные статистики учитываются правильно в DELIVERY_TYPE
        в базовой и мета-релевантности.
        Модели в индексе не находятся в приоритетном регионе доставки, чтобы доставка высчитывалась,
        исходя из количества офферов моделей.
        Проверяются несхлопнутые модели, так как у схлопнутых моделей будут стоять
        релевантности из офферов.

        Проверяем, что у всех моделей DELIVERY_TYPE=2 (COUNTRY).
        """
        # Запрос текстовый, чтобы не нагрести офферы, по которым схлопнутся модели.
        # local-offers-first=1 нужен для того, чтобы сортировка по delivery_type заработала.
        # Иначе всем документам выставится одинаковый delivery_type.
        request = (
            "rids=213&debug=da&place=prime&hid=900&text=search+me&allow-collapsing=1"
            "&show-cutprice=1&good-state=cutprice&local-offer-first=1"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "id": 901,
                            "titles": {"raw": "search me: model with new and cutprice offers"},
                            "debug": {
                                "rank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                "metaRank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                "isCollapsed": False,
                            },
                        },
                        {
                            "id": 903,
                            "titles": {"raw": "search me: model with cutprice offer"},
                            "debug": {
                                "rank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                "metaRank": [{"name": "DELIVERY_TYPE", "value": "2"}],
                                "isCollapsed": False,
                            },
                        },
                    ]
                }
            },
        )

    @classmethod
    def prepare_onstock_relevance(cls):
        cls.index.models += [
            Model(hyperid=801, hid=800, title="search me: model with min_cutprice=300"),
            Model(hyperid=802, hid=800, title="search me: model with min_price=550 min_cutprice=500"),
            Model(hyperid=803, hid=800, title="search me: model with min_price=200"),
        ]
        cls.index.offers += [
            Offer(hyperid=801, price=300, is_cutprice=True),
            Offer(hyperid=802, price=500, is_cutprice=True),
            Offer(hyperid=802, price=550),
            Offer(hyperid=803, price=200),
            Offer(hid=800, price=100, title="search me: cutprice offer with price=100", is_cutprice=True),
            Offer(hid=800, price=400, title="search me: offer with price=400"),
        ]

    def test_onstock_relevance__show_cutprice_0(self):
        """
        Проверяется ONSTOCK в базовой и мета-релевантности.
        Сортровка с учётом onstock должна быть корректной.
        Проверяются офферы и несхлопнутые модели, так как у схлопнутых моделей будут стоять
        релевантности из офферов.
        Параметр good-state не должен влиять на выдачу.
        """
        # Запрос текстовый, чтобы не нагрести офферы, по которым схлопнутся модели
        def check(good_state):
            request = "debug=da&place=prime&hid=800&text=search+me&how=aprice&allow-collapsing=1&show-cutprice=0"
            if good_state:
                request += "&good-state=" + good_state
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "id": 803,
                                "titles": {"raw": "search me: model with min_price=200"},
                                "debug": {
                                    "rank": [{"name": "ONSTOCK", "value": "1"}],
                                    "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                                    "isCollapsed": False,
                                },
                            },
                            {
                                "titles": {"raw": "search me: offer with price=400"},
                                "debug": {
                                    "rank": [{"name": "ONSTOCK", "value": "1"}],
                                    "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                                },
                            },
                            {
                                "id": 802,
                                "titles": {"raw": "search me: model with min_price=550 min_cutprice=500"},
                                "debug": {
                                    "rank": [{"name": "ONSTOCK", "value": "1"}],
                                    "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                                    "isCollapsed": False,
                                },
                            },
                            {
                                "id": 801,
                                "titles": {"raw": "search me: model with min_cutprice=300"},
                                "debug": {
                                    "rank": [{"name": "ONSTOCK", "value": "0"}],
                                    "metaRank": [{"name": "ONSTOCK", "value": "0"}],
                                    "isCollapsed": False,
                                },
                            },
                        ]
                    }
                },
                preserve_order=True,
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {"total": 4},
                    "filters": [
                        {
                            "id": "onstock",
                            "values": [
                                {"value": "0", "initialFound": 1, "found": 1},
                                {"value": "1", "initialFound": 3, "found": 3},
                            ],
                        }
                    ],
                },
            )

        check(good_state=None)
        check(good_state="new")
        check(good_state="cutprice")

    def test_onstock_relevance__show_cutprice_1(self):
        """
        Проверяется ONSTOCK в базовой и мета-релевантности.
        Сортровка с учётом onstock должна быть корректной.
        Проверяются офферы и несхлопнутые модели, так как у схлопнутых моделей будут стоять
        релевантности из офферов.
        """
        # Запрос текстовый, чтобы не нагрести офферы, по которым схлопнутся модели
        response = self.report.request_json(
            "debug=da&place=prime&hid=800&text=search+me&how=aprice&allow-collapsing=1" "&show-cutprice=1"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "titles": {"raw": "search me: cutprice offer with price=100"},
                            "debug": {
                                "rank": [{"name": "ONSTOCK", "value": "1"}],
                                "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                            },
                        },
                        {
                            "id": 803,
                            "titles": {"raw": "search me: model with min_price=200"},
                            "debug": {
                                "rank": [{"name": "ONSTOCK", "value": "1"}],
                                "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                                "isCollapsed": False,
                            },
                        },
                        {
                            "id": 801,
                            "titles": {"raw": "search me: model with min_cutprice=300"},
                            "debug": {
                                "rank": [{"name": "ONSTOCK", "value": "1"}],
                                "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                                "isCollapsed": False,
                            },
                        },
                        {
                            "titles": {"raw": "search me: offer with price=400"},
                            "debug": {
                                "rank": [{"name": "ONSTOCK", "value": "1"}],
                                "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                            },
                        },
                        {
                            "id": 802,
                            "titles": {"raw": "search me: model with min_price=550 min_cutprice=500"},
                            "debug": {
                                "rank": [{"name": "ONSTOCK", "value": "1"}],
                                "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                                "isCollapsed": False,
                            },
                        },
                    ]
                }
            },
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 5},
                "filters": [
                    {
                        "id": "onstock",
                        "values": [
                            {"value": "0", "found": NoKey("found")},
                            {"value": "1", "initialFound": 5, "found": 5},
                        ],
                    }
                ],
            },
        )

    def test_onstock_relevance__good_state_new(self):
        """
        Проверяется ONSTOCK в базовой и мета-релевантности.
        Сортровка с учётом onstock должна быть корректной.
        Проверяются офферы и несхлопнутые модели, так как у схлопнутых моделей будут стоять
        релевантности из офферов.
        """
        # Запрос текстовый, чтобы не нагрести офферы, по которым схлопнутся модели
        response = self.report.request_json(
            "debug=da&place=prime&hid=800&text=search+me&how=aprice&allow-collapsing=1"
            "&show-cutprice=1&good-state=new"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "id": 803,
                            "titles": {"raw": "search me: model with min_price=200"},
                            "debug": {
                                "rank": [{"name": "ONSTOCK", "value": "1"}],
                                "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                                "isCollapsed": False,
                            },
                        },
                        {
                            "titles": {"raw": "search me: offer with price=400"},
                            "debug": {
                                "rank": [{"name": "ONSTOCK", "value": "1"}],
                                "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                            },
                        },
                        {
                            "id": 802,
                            "titles": {"raw": "search me: model with min_price=550 min_cutprice=500"},
                            "debug": {
                                "rank": [{"name": "ONSTOCK", "value": "1"}],
                                "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                                "isCollapsed": False,
                            },
                        },
                    ]
                }
            },
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 3},
                "filters": [
                    {
                        "id": "onstock",
                        "values": [
                            {"value": "0", "found": NoKey("found")},
                            {"value": "1", "initialFound": 3, "found": 3},
                        ],
                    }
                ],
            },
        )

    def test_onstock_relevance__good_state_cutprice(self):
        """
        Проверяется ONSTOCK в базовой и мета-релевантности.
        Сортровка с учётом onstock должна быть корректной.
        Проверяются офферы и несхлопнутые модели, так как у схлопнутых моделей будут стоять
        релевантности из офферов.
        """
        # Запрос текстовый, чтобы не нагрести офферы, по которым схлопнутся модели
        response = self.report.request_json(
            "debug=da&place=prime&hid=800&text=search+me&how=aprice&allow-collapsing=1"
            "&show-cutprice=1&good-state=cutprice"
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "titles": {"raw": "search me: cutprice offer with price=100"},
                            "debug": {
                                "rank": [{"name": "ONSTOCK", "value": "1"}],
                                "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                            },
                        },
                        {
                            "id": 801,
                            "titles": {"raw": "search me: model with min_cutprice=300"},
                            "debug": {
                                "rank": [{"name": "ONSTOCK", "value": "1"}],
                                "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                                "isCollapsed": False,
                            },
                        },
                        {
                            "id": 802,
                            "titles": {"raw": "search me: model with min_price=550 min_cutprice=500"},
                            "debug": {
                                "rank": [{"name": "ONSTOCK", "value": "1"}],
                                "metaRank": [{"name": "ONSTOCK", "value": "1"}],
                                "isCollapsed": False,
                            },
                        },
                    ]
                }
            },
            preserve_order=True,
        )
        self.assertFragmentIn(
            response,
            {
                "search": {"total": 3},
                "filters": [
                    {
                        "id": "onstock",
                        "values": [
                            {"value": "0", "found": NoKey("found")},
                            {"value": "1", "initialFound": 3, "found": 3},
                        ],
                    }
                ],
            },
        )

    @classmethod
    def prepare_price_relevance_with_currency_conversion(cls):
        cls.index.currencies += [
            Currency(
                name=Currency.USD,
                exchange_rates=[
                    ExchangeRate(to=Currency.RUR, rate=60.0),
                ],
            ),
        ]
        cls.index.models += [
            Model(hyperid=701, hid=700, title="search me: model with min_price=900"),
            Model(hyperid=702, hid=700, title="search me: model with min_cutprice=600"),
        ]
        cls.index.offers += [
            Offer(hyperid=701, price=900),
            Offer(hyperid=702, price=600, is_cutprice=True),
            Offer(
                hid=700,
                waremd5="ABCDEFGHIJKLMNOPQRSTUQ",
                price=1200,
                is_cutprice=True,
                title="search me: cutprice offer with price=1200",
            ),
        ]

    def test_price_relevance_with_currency_conversion(self):
        """
        Проверка конвертирования цен на базовых у уценённых офферов и несхлопнутых моделей с уценкой.
        """
        response = self.report.request_json(
            'text=search+me&place=prime&hid=700&show-cutprice=1&allow-collapsing=1&how=aprice&currency=USD&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"id": 702, "titles": {"raw": "search me: model with min_cutprice=600"}},
                        {"id": 701, "titles": {"raw": "search me: model with min_price=900"}},
                        {
                            "wareId": "ABCDEFGHIJKLMNOPQRSTUQ",
                            "titles": {"raw": "search me: cutprice offer with price=1200"},
                        },
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"id": 702, "debug": {"rank": [{"name": "PRICE", "value": "1000"}], "isCollapsed": False}},
                        {"id": 701, "debug": {"rank": [{"name": "PRICE", "value": "1500"}], "isCollapsed": False}},
                        {"wareId": "ABCDEFGHIJKLMNOPQRSTUQ", "debug": {"rank": [{"name": "PRICE", "value": "2000"}]}},
                    ]
                }
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
