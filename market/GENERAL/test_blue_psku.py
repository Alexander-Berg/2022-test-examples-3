#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DeliveryBucket,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    Offer,
    OfferDimensions,
    Outlet,
    PickupBucket,
    PickupOption,
    Region,
    Shop,
    Vendor,
)
from core.testcase import TestCase, main
from core.matcher import Absent, NotEmpty


SPECIAL_PSKU_VENDOR = 16644882


class T(TestCase):
    @classmethod
    def prepare_demand_prediction(cls):
        cls.index.shops += [
            Shop(fesh=1, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE, priority_region=213),
            Shop(fesh=2001, datafeed_id=2001, blue='REAL', priority_region=213),
        ]
        cls.index.regiontree += [
            Region(rid=213),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=90569,
                children=[
                    HyperCategory(hid=1, output_type=HyperCategoryType.GURU),
                ],
            )
        ]

        cls.index.models += [
            Model(hyperid=1001, title='pskuModel', is_pmodel=True, hid=1, vendor_id=1),
            Model(hyperid=1002, title='mskuModel', hid=1),
        ]

        cls.index.outlets += [
            Outlet(fesh=2001, region=213, point_type=Outlet.FOR_PICKUP, point_id=1001),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=2001,
                carriers=[101],
                options=[PickupOption(outlet_id=1001)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.mskus += [
            # test_msku_by_partner
            MarketSku(
                hyperid=1001,
                vendor_id=1,
                forbidden_market_mask=Offer.IS_PSKU,
                sku=1,
                pickup_buckets=[5001],
                blue_offers=[
                    BlueOffer(
                        price=100,
                        vendor_id=1,
                        forbidden_market_mask=Offer.IS_PSKU,
                        offerid='1',
                        feedid=2001,
                        waremd5='_____________psku1001g',
                        weight=5,
                        dimensions=OfferDimensions(length=20, width=30, height=10),
                    ),
                ],
            ),
            # test_msku_by_market
            MarketSku(
                hyperid=1002,
                sku=2,
                pickup_buckets=[5001],
                blue_offers=[
                    BlueOffer(
                        price=100,
                        offerid='2',
                        feedid=2001,
                        waremd5='_____________msku1002g',
                        weight=5,
                        dimensions=OfferDimensions(length=20, width=30, height=10),
                    ),
                ],
            ),
        ]

        cls.index.vendors += [
            Vendor(
                vendor_id=1,
                name='goodVendor',
                website='www.good.com',
                webpage_recommended_shops='http://www.good.com/ru/brandshops/',
                description='VendorDescription',
                logos=[],
            ),
            Vendor(
                vendor_id=SPECIAL_PSKU_VENDOR,
                name='fakeVendor',
                website='www.fake.com',
                webpage_recommended_shops='http://www.fake.com/ru/brandshops/',
                description='FakeVendorDescription',
                logos=[],
            ),
        ]

    def test_output_info_prime(self):
        response = self.report.request_json("place=prime&hid=1&rgb=blue")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "modelCreator": "partner",
                        "offers": {
                            "items": [
                                {
                                    "marketSkuCreator": "partner",
                                    "model": {"id": 1001},
                                }
                            ]
                        },
                    }
                ]
            },
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "modelCreator": "market",
                        "offers": {
                            "items": [
                                {
                                    "marketSkuCreator": "market",
                                    "model": {"id": 1002},
                                }
                            ]
                        },
                    }
                ]
            },
        )

    def test_output_info_sku_offers(self):
        response = self.report.request_json("place=sku_offers&market-sku=1,2")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "marketSkuCreator": "partner",
                        "offers": {
                            "items": [
                                {
                                    "marketSkuCreator": "partner",
                                    "model": {"id": 1001},
                                }
                            ]
                        },
                    }
                ]
            },
        )

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "marketSkuCreator": "market",
                        "offers": {
                            "items": [
                                {
                                    "marketSkuCreator": "market",
                                    "model": {"id": 1002},
                                }
                            ]
                        },
                    }
                ]
            },
        )

    def test_show_models_specs(self):
        # для pmodel описание модели не должно рендериться (https://st.yandex-team.ru/MARKETOUT-25334)
        response = self.report.request_json(
            "place=sku_offers&market-sku=1,2&show-models-specs=msku-full&show-models-specs=full&show-models=1"
        )
        self.assertFragmentIn(
            response,
            {
                "product": {
                    "id": 1001,
                    "specs": Absent(),
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                "product": {
                    "id": 1002,
                    "specs": NotEmpty(),
                }
            },
        )

        response = self.report.request_json("place=prime&hid=1&rgb=blue&show-models-specs=full")
        self.assertFragmentIn(
            response,
            {
                "id": 1001,
                "specs": Absent(),
            },
        )

        self.assertFragmentIn(
            response,
            {
                "id": 1002,
                "specs": NotEmpty(),
            },
        )

    @classmethod
    def prepare_psku_jump_table(cls):
        cls.index.models += [
            Model(hyperid=2001, is_pmodel=True, hid=2),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=2001,
                forbidden_market_mask=Offer.IS_PSKU,
                sku=101,
                title="МСКУ 101",
                blue_offers=[
                    BlueOffer(price=100, offerid='101', feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=2001,
                forbidden_market_mask=Offer.IS_PSKU,
                sku=102,
                title="Б МСКУ 102",
                blue_offers=[
                    BlueOffer(price=100, offerid='102', feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=2001,
                forbidden_market_mask=Offer.IS_PSKU,
                sku=103,
                title="МСКУ 103",
                blue_offers=[
                    # Ску вне продажи
                ],
            ),
        ]

    def test_psku_jump_table(self):
        """
        Проверяем, что вместо обычной карты переходов
        для ПСКУ выводится карта переходов по заголовкам, но в виде фильтров
        """
        response = self.report.request_json("place=sku_offers&rgb=blue&market-sku=101")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "101",
                        "filters": [
                            {
                                "id": "modifications",
                                "name": "Модификации",
                                "type": "enum",
                                "subType": "text_extended",
                                "values": [
                                    {
                                        "id": "102",
                                        "marketSku": "102",
                                        "value": "Б МСКУ 102",  # Заголовки отсортированы в алфавитном порядке
                                        "slug": "b-msku-102",
                                        "found": 1,
                                    },
                                    {
                                        "id": "101",
                                        "marketSku": "101",
                                        "value": "МСКУ 101",
                                        "slug": "msku-101",
                                        "found": 1,
                                        "checked": True,
                                    },
                                ],
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json("place=sku_offers&rgb=blue&market-sku=102")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "102",
                        "filters": [
                            {
                                "id": "modifications",
                                "name": "Модификации",
                                "type": "enum",
                                "subType": "text_extended",
                                "values": [
                                    {
                                        "id": "102",
                                        "marketSku": "102",
                                        "value": "Б МСКУ 102",
                                        "slug": "b-msku-102",
                                        "found": 1,
                                        "checked": True,
                                    },
                                    {
                                        "id": "101",
                                        "marketSku": "101",
                                        "value": "МСКУ 101",
                                        "slug": "msku-101",
                                        "found": 1,
                                    },
                                ],
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

        response = self.report.request_json("place=sku_offers&rgb=blue&market-sku=103")
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "103",
                        "filters": [
                            {
                                "id": "modifications",
                                "name": "Модификации",
                                "type": "enum",
                                "subType": "text_extended",
                                "values": [
                                    {
                                        "id": "102",
                                        "marketSku": "102",
                                        "value": "Б МСКУ 102",
                                        "slug": "b-msku-102",
                                        "found": 1,
                                    },
                                    {
                                        "id": "101",
                                        "marketSku": "101",
                                        "value": "МСКУ 101",
                                        "slug": "msku-101",
                                        "found": 1,
                                    },
                                    {
                                        "id": "103",
                                        "marketSku": "103",
                                        "value": "МСКУ 103",
                                        "slug": "msku-103",
                                        "found": 0,
                                        "checked": True,
                                    },
                                ],
                            }
                        ],
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def __request_show_psku(self):
        '''
        запросы, которые не приводят к скрытию psku.
        '''
        baseFlag = "&show-partner-documents={}"
        req = []
        req.append("{reqBase}")  # дефотное поведение - скрывать psku для всех
        req.append("{reqBase}" + baseFlag.format("true"))  # show-partner-documents=true позволяет выводить psku
        req.append(
            "{reqBase}" + "&alice=1" + baseFlag.format("true")
        )  # show-partner-documents=true позволяет выводить psku для алисы
        return req

    def __request_hide_psku(self):
        '''
        запросы, которые приводят к скрытию psku.
        '''
        baseFlag = "&show-partner-documents={}"
        req = []
        req.append("{reqBase}" + "&alice=1")  # дефотное поведение для алисы - скрывать psku для всех
        req.append("{reqBase}" + baseFlag.format("false"))  # show-partner-documents=false позволяет заблокировать psku
        return req

    def test_show_psku_sku_offers_blue(self):
        """
        Проверяем, что для синего маркета для плейса sku_offers не происходит скрытия psku
        для запросов в описанных в __request_show_psku
        """
        for req in self.__request_show_psku():
            response = self.report.request_json(req.format(reqBase="place=sku_offers&rgb=blue&market-sku=1,2"))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "_____________msku1002g",
                                    }
                                ]
                            }
                        },
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "_____________psku1001g",
                                    }
                                ]
                            }
                        },
                    ]
                },
                allow_different_len=False,
            )

    def test_show_psku_sku_offers_green(self):
        """
        Проверяем, что белого маркета для плейса sku_offers не происходит скрытия psku
        для запросов в описанных в __request_show_psku
        """
        for req in self.__request_show_psku():
            response = self.report.request_json(req.format(reqBase="place=sku_offers&rgb=green&market-sku=1,2"))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "_____________msku1002g",
                                    }
                                ]
                            }
                        },
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "_____________psku1001g",
                                    }
                                ]
                            }
                        },
                    ]
                },
                allow_different_len=False,
            )

    def test_hide_psku_sku_offers_blue(self):
        """
        Проверяем, что для плейса sku_offers происходит скрытие psku для запросов в описанных в __request_hide_psku
        """
        for req in self.__request_hide_psku():
            response = self.report.request_json(req.format(reqBase="place=sku_offers&rgb=blue&market-sku=1,2"))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "offers": {
                                    "items": [
                                        {
                                            "wareId": "_____________msku1002g",
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

    def test_hide_psku_sku_offers_green(self):
        """
        Проверяем, что для белого маркета для плейса sku_offers происходит скрытие psku
        для запросов в описанных в __request_hide_psku
        """
        for req in self.__request_hide_psku():
            response = self.report.request_json(req.format(reqBase="place=sku_offers&rgb=green&market-sku=1,2"))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "offers": {
                                    "items": [
                                        {
                                            "wareId": "_____________msku1002g",
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

    def test_show_psku_prime_blue(self):
        """
        Проверяем, что для синего маркета для плейса prime не происходит скрытия psku
        для запросов в описанных в __request_show_psku
        """
        for req in self.__request_show_psku():
            response = self.report.request_json(req.format(reqBase="place=prime&hid=1&rgb=blue"))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "_____________msku1002g",
                                    }
                                ]
                            }
                        },
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "_____________psku1001g",
                                    }
                                ]
                            }
                        },
                    ]
                },
                allow_different_len=False,
            )

    def test_show_psku_prime_green(self):
        """
        Проверяем, что для белого маркета для плейса prime не происходит скрытия psku
        для запросов в описанных в __request_show_psku
        """
        for req in self.__request_show_psku():
            response = self.report.request_json(req.format(reqBase="place=prime&hid=1&rgb=green"))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": "_____________msku1002g",
                        },
                        {
                            "entity": "offer",
                            "wareId": "_____________psku1001g",
                        },
                        {
                            "entity": "product",
                            "id": 1002,
                        },
                        {
                            "entity": "product",
                            "id": 1001,
                        },
                    ]
                },
                allow_different_len=False,
            )

    def test_hide_psku_prime_blue(self):
        """
        Проверяем, что для синего маркета для плейса prime происходит скрытие psku
        для запросов в описанных в __request_hide_psku
        """
        for req in self.__request_hide_psku():
            response = self.report.request_json(req.format(reqBase="place=prime&hid=1&rgb=blue"))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "offers": {
                                    "items": [
                                        {
                                            "wareId": "_____________msku1002g",
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

    def test_hide_psku_prime_green(self):
        """
        Проверяем, что для белого маркета для плейса prime происходит скрытие psku
        для запросов в описанных в __request_hide_psku
        """
        for req in self.__request_hide_psku():
            response = self.report.request_json(req.format(reqBase="place=prime&hid=1&rgb=green"))
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "offer",
                                "wareId": "_____________msku1002g",
                            },
                            {
                                "entity": "product",
                                "id": 1002,
                            },
                        ]
                    }
                },
                allow_different_len=False,
            )

    def test_show_psku_offerinfo(self):
        """
        Проверяем, что для плейса offerinfo не происходит скрытия psku для запросов в описанных в __request_show_psku
        """
        for req in self.__request_show_psku():
            response = self.report.request_json(
                req.format(reqBase="place=offerinfo&regset=2&rids=213&offerid=_____________psku1001g")
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "wareId": "_____________psku1001g",
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )
            response = self.report.request_json(
                req.format(reqBase="place=offerinfo&regset=2&rids=213&offerid=_____________msku1002g")
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "wareId": "_____________msku1002g",
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )

    def test_hide_psku_offerinfo(self):
        """
        Проверяем, что для плейса offerinfo происходит скрытие psku для запросов в описанных в __request_hide_psku
        """
        for req in self.__request_hide_psku():
            response = self.report.request_json(
                req.format(reqBase="place=offerinfo&regset=2&rids=213&offerid=_____________msku1002g")
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "wareId": "_____________msku1002g",
                            }
                        ]
                    }
                },
                allow_different_len=False,
            )
            response = self.report.request_json(
                req.format(reqBase="place=offerinfo&regset=2&rids=213&offerid=_____________psku1001g")
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "total": 0,
                        "totalOffers": 0,
                    },
                },
                allow_different_len=False,
            )

    def test_show_psku_actual_delivery(self):
        """
        Проверяем, что для плейса actual_delivery не происходит скрытия psku для запросов в описанных в __request_show_psku
        """
        for req in self.__request_show_psku():
            response = self.report.request_json(
                req.format(reqBase="place=actual_delivery&offers-list=_____________psku1001g:1")
            )
            self.assertFragmentIn(
                response,
                {
                    "offers": [
                        {
                            "wareId": "_____________psku1001g",
                        }
                    ]
                },
                allow_different_len=False,
            )  # rgb=blue
            response = self.report.request_json(
                req.format(reqBase="place=actual_delivery&offers-list=_____________msku1002g:1")
            )
            self.assertFragmentIn(
                response,
                {
                    "offers": [
                        {
                            "wareId": "_____________msku1002g",
                        }
                    ]
                },
                allow_different_len=False,
            )

    def test_hide_psku_actual_delivery(self):
        """
        Проверяем, что для плейса actual_delivery происходит скрытие psku для запросов в описанных в __request_hide_psku
        """
        for req in self.__request_hide_psku():
            response = self.report.request_json(
                req.format(reqBase="place=actual_delivery&offers-list=_____________psku1001g:1")
            )
            self.assertFragmentIn(
                response,
                {
                    "offerProblems": [{"wareId": "_____________psku1001g", "problems": ["NONEXISTENT_OFFER"]}],
                },
                allow_different_len=False,
            )
            response = self.report.request_json(
                req.format(reqBase="place=actual_delivery&offers-list=_____________msku1002g:1")
            )
            self.assertFragmentIn(
                response,
                {
                    "offers": [
                        {
                            "model": {"id": 1002},
                            "wareId": "_____________msku1002g",
                        }
                    ]
                },
                allow_different_len=False,
            )

    def test_show_psku_modelinfo(self):
        """
        Проверяем, что для плейса modelinfo не происходит скрытия pmodel для запросов в описанных в __request_show_psku
        """
        for req in self.__request_show_psku():
            response = self.report.request_json(
                req.format(reqBase="place=modelinfo&rgb=blue&rids=213&hyperid=1001,1002")
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "product",
                                "id": 1001,
                            },
                            {
                                "entity": "product",
                                "id": 1002,
                            },
                        ]
                    },
                },
                allow_different_len=False,
            )

    def test_hide_psku_modelinfo(self):
        """
        Проверяем, что для плейса modelinfo происходит скрытие pmodel для запросов в описанных в __request_hide_psku
        Обычная модель отображается всегда
        """
        for req in self.__request_hide_psku():
            response = self.report.request_json(
                req.format(reqBase="place=modelinfo&rgb=blue&rids=213&hyperid=1001,1002")
            )
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "product",
                                "id": 1002,
                            }
                        ]
                    },
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_vendor_info(cls):
        cls.index.models += [
            Model(hyperid=1003, is_pmodel=True, hid=2, vendor_id=SPECIAL_PSKU_VENDOR),
        ]

        cls.index.mskus += [
            # test_msku_by_partner
            MarketSku(
                hyperid=1003,
                vendor_id=SPECIAL_PSKU_VENDOR,
                forbidden_market_mask=Offer.IS_PSKU,
                sku=3,
                blue_offers=[
                    BlueOffer(
                        price=100,
                        vendor_id=SPECIAL_PSKU_VENDOR,
                        offerid='3',
                        feedid=2003,
                        waremd5='_____________psku1003g',
                        weight=5,
                        dimensions=OfferDimensions(length=20, width=30, height=10),
                    ),
                ],
            ),
        ]

    def test_vendor_info(self):
        response = self.report.request_json('place=sku_offers&market-sku=1&rgb=blue&show-partner-documents=true')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "vendor": {
                            "name": "goodVendor",
                            "slug": "goodvendor",
                        },
                        "offers": {
                            "items": [
                                {
                                    "vendor": {
                                        "name": "goodVendor",
                                        "slug": "goodvendor",
                                    },
                                }
                            ]
                        },
                    }
                ]
            },
            allow_different_len=False,
        )
        response = self.report.request_json('place=sku_offers&market-sku=3&rgb=blue&show-partner-documents=true')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "vendor": Absent(),
                        "offers": {
                            "items": [
                                {
                                    "vendor": Absent(),
                                }
                            ]
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_product_info(self):
        '''
        Проверяем выводится ли информация по продукту для psku оффера для базового поведения.
        (необходимо, так как информация запрашивается от плейса не входящего в список базового доступа.
        '''
        response = self.report.request_json('place=sku_offers&market-sku=1&rgb=blue&show-models=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "product": {
                            "entity": "product",
                            "titles": {
                                "raw": "pskuModel",
                            },
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_filter_info(cls):
        cls.index.models += [
            Model(hyperid=1004, is_pmodel=True, hid=3),
        ]

        cls.index.mskus += [
            # test_msku_by_partner
            MarketSku(
                hyperid=1004,
                forbidden_market_mask=Offer.IS_PSKU,
                sku=4,
                blue_offers=[
                    BlueOffer(
                        price=100,
                        offerid='4',
                        feedid=2004,
                        waremd5='_____________psku1004g',
                        weight=5,
                        dimensions=OfferDimensions(length=20, width=30, height=10),
                    ),
                ],
            ),
            MarketSku(hyperid=1004, forbidden_market_mask=Offer.IS_PSKU, sku=5, blue_offers=[]),
            MarketSku(hyperid=1004, forbidden_market_mask=Offer.IS_PSKU, sku=6, blue_offers=[]),
        ]

    def test_filter_info(self):
        '''
        Проверяем, что при вызове запроса, для которого filter содержит единственное значение - он не выводил его,
        а обнулял поле filter
        '''
        response = self.report.request_json('place=sku_offers&market-sku=1&rgb=blue&show-models=1')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "1",
                        "filters": Absent(),
                    }
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=sku_offers&market-sku=5&rgb=blue&show-models=1&show-partner-documents=true'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "filters": [
                            {
                                "id": "modifications",
                                "values": [
                                    {
                                        "id": "5",
                                    },
                                    {
                                        "id": "4",
                                    },
                                ],
                            }
                        ]
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_psku_in_recommendations_hardcode(cls):
        cls.index.models += [
            Model(hyperid=168553000, is_pmodel=True, hid=2612000),
            Model(hyperid=175941400, hid=2612000),
            Model(hyperid=178503626, is_pmodel=True, hid=2612000),
            Model(hyperid=180423140, is_pmodel=True, hid=2612000),
            Model(hyperid=417037143, hid=2612000),
            Model(hyperid=183694968, is_pmodel=True, hid=2612000),
            Model(hyperid=169117444, is_pmodel=True, hid=2612000),
            Model(hyperid=1759299345, hid=2612000),
            Model(hyperid=1711235661, hid=2612000),
            Model(hyperid=1759297656, hid=2612000),
            Model(hyperid=1711072058, hid=2612000),
            Model(hyperid=12259971, hid=2612000),
            Model(hyperid=1712313026, hid=2612000),
            Model(hyperid=71296620, hid=2612000),
        ]

        cls.index.mskus += [
            # test_msku_by_partner
            MarketSku(
                hyperid=168553000,
                sku=1685530001,
                forbidden_market_mask=Offer.IS_PSKU,
                blue_offers=[
                    BlueOffer(price=100, forbidden_market_mask=Offer.IS_PSKU, feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=175941400,
                sku=1759414001,
                blue_offers=[
                    BlueOffer(price=100, feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=178503626,
                sku=1785036261,
                forbidden_market_mask=Offer.IS_PSKU,
                blue_offers=[
                    BlueOffer(price=100, forbidden_market_mask=Offer.IS_PSKU, feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=180423140,
                sku=1804231401,
                forbidden_market_mask=Offer.IS_PSKU,
                blue_offers=[
                    BlueOffer(price=100, forbidden_market_mask=Offer.IS_PSKU, feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=417037143,
                sku=4170371431,
                blue_offers=[
                    BlueOffer(price=100, feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=183694968,
                sku=1836949681,
                forbidden_market_mask=Offer.IS_PSKU,
                blue_offers=[
                    BlueOffer(price=100, forbidden_market_mask=Offer.IS_PSKU, feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=169117444,
                sku=1691174441,
                forbidden_market_mask=Offer.IS_PSKU,
                blue_offers=[
                    BlueOffer(price=100, forbidden_market_mask=Offer.IS_PSKU, feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=1759299345,
                sku=100126175475,
                blue_offers=[
                    BlueOffer(price=100, feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=1711235661,
                sku=100256621468,
                blue_offers=[
                    BlueOffer(price=100, feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=1759297656,
                sku=100126174899,
                blue_offers=[
                    BlueOffer(price=100, feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=1711072058,
                sku=100256621395,
                blue_offers=[
                    BlueOffer(price=100, feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=12259971,
                sku=100131944741,
                blue_offers=[
                    BlueOffer(price=100, feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=1712313026,
                sku=100256586203,
                blue_offers=[
                    BlueOffer(price=100, feedid=2001),
                ],
            ),
            MarketSku(
                hyperid=71296620,
                sku=100256621508,
                blue_offers=[
                    BlueOffer(price=100, feedid=2001),
                ],
            ),
        ]

    def test_psku_in_recommendations_hardcode(self):
        '''
        Проверяем, что при запросах с нужным puid плейсы рекомендаций
        отдают хардкод
        '''
        person_models = {
            '907159475': [
                {'msku': 1685530001, 'hyperid': 168553000},
                {'msku': 1759414001, 'hyperid': 175941400},
                {'msku': 1785036261, 'hyperid': 178503626},
                {'msku': 1804231401, 'hyperid': 180423140},
                {'msku': 4170371431, 'hyperid': 417037143},
                {'msku': 1836949681, 'hyperid': 183694968},
                {'msku': 1691174441, 'hyperid': 169117444},
            ],
            '4032805824': [
                {'msku': 100126175475, 'hyperid': 1759299345},
                {'msku': 100256621468, 'hyperid': 1711235661},
                {'msku': 100126174899, 'hyperid': 1759297656},
            ],
            '993730378': [
                {'msku': 100126175475, 'hyperid': 1759299345},
                {'msku': 100256621468, 'hyperid': 1711235661},
                {'msku': 100126174899, 'hyperid': 1759297656},
            ],
            '1016530154': [
                {'msku': 100256621395, 'hyperid': 1711072058},
                {'msku': 100131944741, 'hyperid': 12259971},
                {'msku': 100256586203, 'hyperid': 1712313026},
                {'msku': 100256621508, 'hyperid': 71296620},
            ],
            '4032806878': [
                {'msku': 100256621395, 'hyperid': 1711072058},
                {'msku': 100131944741, 'hyperid': 12259971},
                {'msku': 100256586203, 'hyperid': 1712313026},
                {'msku': 100256621508, 'hyperid': 71296620},
            ],
        }
        for place in [
            'also_viewed',
            'attractive_models',
            'blue_attractive_models',
            'blue_omm_findings',
            'commonly_purchased',
            'omm_findings',
            'omm_market',
            'deals',
            'popular_products',
            'product_accessories',
            'products_by_history',
        ]:
            for person in person_models:
                response = self.report.request_json('place={}&puid={}&rgb=blue'.format(place, person))
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "id": model['hyperid'],
                                "offers": {"items": [{"marketSku": str(model['msku'])}]},
                            }
                            for model in person_models[person]
                        ]
                    },
                    allow_different_len=False,
                )


if __name__ == '__main__':
    main()
