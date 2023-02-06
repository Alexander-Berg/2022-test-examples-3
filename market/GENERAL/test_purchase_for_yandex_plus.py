#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    CreditPlan,
    DeliveryBucket,
    GLParam,
    GLType,
    GLValue,
    MarketSku,
    Model,
    OfferDimensions,
    Region,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main
from core.types.cms_promo import CmsPromo
from core.matcher import Absent, ElementCount, NotEmpty, EqualToOneOf
from core.logs import ErrorCodes

from unittest import skip


PURCHASE_FOR_YANDEX_PLUS = 'purchase_for_yandex_plus'


class T(TestCase):
    @classmethod
    def prepare(cls):
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        cls.index.regiontree += [Region(rid=213, name="Москва")]

        cls.index.gltypes = [
            GLType(
                param_id=100,
                hid=1,
                gltype=GLType.ENUM,
                name='Size',
                cluster_filter=True,
                model_filter_index=1,
                values=[GLValue(value_id=1001, text='Big'), GLValue(value_id=1002, text='Small')],
            )
        ]
        cls.index.models += [
            # Эксклюзивная модель стола (сама карточка не секретная, только офферы)
            Model(hyperid=1, hid=1, title="model Table"),
            # Эксклюзивная модель стула
            Model(hyperid=2, hid=1, title="model Chair"),
            # Простая модель шкафа
            Model(hyperid=3, hid=1, title="model Wardrobe"),
            # Простая модель из другой категории
            Model(hyperid=4, hid=2, title="Pencil"),
        ]
        cls.index.shops += [
            Shop(
                fesh=513,
                name='blue_shop_1',
                priority_region=213,
                supplier_type=Shop.FIRST_PARTY,
                datafeed_id=3,
                fulfillment_program=True,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
        ]
        cls.index.mskus += [
            MarketSku(
                title='Rainbow table big',
                hyperid=1,
                sku=10,
                glparams=[GLParam(param_id=100, value=1001)],
                blue_offers=[
                    BlueOffer(
                        waremd5='VApGSN_T1j3gFcCnU400Sg',
                        title='offer rainbow table big',
                        price=500,
                        cms_promo_literal=PURCHASE_FOR_YANDEX_PLUS,
                        feedid=3,
                        delivery_buckets=[4321],
                        weight=1,
                        dimensions=OfferDimensions(length=1, width=1, height=1),
                    ),
                ],
            ),
            MarketSku(
                title='Rainbow table small',
                hyperid=1,
                sku=11,
                glparams=[GLParam(param_id=100, value=1002)],
                blue_offers=[
                    BlueOffer(
                        waremd5='cyMvBACGeV54y1vTCtqaCg',
                        title='offer rainbow table small',
                        price=250,
                        cms_promo_literal=PURCHASE_FOR_YANDEX_PLUS,
                    )
                ],
            ),
            MarketSku(
                title='Rainbow chair',
                hyperid=2,
                sku=21,
                blue_offers=[
                    BlueOffer(
                        waremd5='SiYW-xkUJF-0zig1uUabXg',
                        title='offer rainbow chair',
                        price=150,
                        cms_promo_literal=PURCHASE_FOR_YANDEX_PLUS,
                    )
                ],
            ),
            MarketSku(
                title='Black wardrobe',
                hyperid=3,
                sku=31,
                blue_offers=[BlueOffer(waremd5='i59N_eKSxcRdD6l6mjm71A', title='offer black wardrobe', price=350)],
                ungrouped_model_blue=31,
            ),
            MarketSku(
                title='Simple pencil',
                hyperid=4,
                sku=40,
                blue_offers=[BlueOffer(waremd5='Z-F8-68KcXEFN5223hAvVg', price=50)],
            ),
        ]

        cls.index.cms_promos += [CmsPromo(promo_id=PURCHASE_FOR_YANDEX_PLUS, available_mskus=(10, 11, 21))]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=4321, dc_bucket_id=4321, fesh=123, carriers=[99], regional_options=[RegionalDelivery(rid=213)]
            ),
        ]
        cls.delivery_calc.on_request_offer_buckets(weight=1, height=1, length=1, width=1).respond([4321], [], [])

    def _gen_prime_request(self, onstock=None, allow_collapsing=None, allow_ungrouping=None, use_do=None):
        request = 'place=prime&hid=1&debug=1'
        if onstock is not None:
            request += '&onstock=1'
        if use_do is not None:
            request += '&use-default-offers=1'
        if allow_collapsing is not None:
            request += '&allow-collapsing=1'
        if allow_ungrouping:
            request += '&allow-collapsing=1&allow-ungrouping=1'
        return request

    def check_debug_info(self, result):
        self.assertFragmentIn(
            result,
            {
                'filters': {
                    # два оффера из скю 11 и один из скю 21
                    'PURCHASE_FOR_YANDEX_PLUS': 3,
                },
            },
        )

    def test_prime_without_collapsing(self):
        '''Проверяем прайм без схлопывания, должны отфильтровываться оффера "за баллы"'''

        result = self.report.request_json(self._gen_prime_request() + '&rearr-factors=market_metadoc_search=no')
        self.check_debug_info(result)
        self.assertFragmentIn(
            result,
            {
                'total': 4,
                'results': [
                    {
                        'entity': 'product',
                        'id': 1,
                        'offers': {'count': 0},  # у этой модели только эксклюзивные оффера, и они отбросились
                    },
                    {
                        'entity': 'product',
                        'id': 2,
                        'offers': {'count': 0},  # у этой модели только эксклюзивный оффер, и он отбросился
                    },
                    {
                        # обычная модель с обычным оффером
                        'entity': 'product',
                        'id': 3,
                        'offers': {'count': 1},
                    },
                    {
                        'entity': 'offer',
                        'model': {'id': 3},
                        'marketSku': '31',
                        'wareId': 'i59N_eKSxcRdD6l6mjm71A',
                    },
                    # оффера для эксклюзивных скю 11 и 21 отфильтровались
                ],
            },
            allow_different_len=False,
        )

        # c onstock=1 модели 1 и 2 отбросятся (хотя эксклюзивные оффера есть)
        result = self.report.request_json(
            self._gen_prime_request(onstock=True, use_do=True) + '&rearr-factors=market_metadoc_search=no'
        )
        self.check_debug_info(result)
        self.assertFragmentIn(
            result,
            {
                'total': 2,
                'results': [
                    {
                        'entity': 'product',
                        'id': 3,
                        'offers': {
                            'count': 1,
                            'items': [
                                {
                                    'sku': '31',
                                    'wareId': 'i59N_eKSxcRdD6l6mjm71A',
                                }
                            ],
                        },
                    },
                    {
                        'entity': 'offer',
                        'model': {'id': 3},
                        'marketSku': '31',
                        'wareId': 'i59N_eKSxcRdD6l6mjm71A',
                    },
                    # оффера для эксклюзивных скю 11 и 21 отфильтровались
                ],
            },
            allow_different_len=False,
        )

    def test_prime_with_collapsing(self):
        '''Проверяем прайм со схлопыванием, должны отфильтровываться оффера "за баллы"'''

        result = self.report.request_json(
            self._gen_prime_request(allow_collapsing=True) + '&rearr-factors=market_metadoc_search=no'
        )
        self.check_debug_info(result)
        # должны быть только документы-модели
        self.assertFragmentIn(
            result,
            {
                'total': 3,
                'results': [
                    {
                        'entity': 'product',
                        'id': 1,
                        'offers': {'count': 0},  # оффер из эксклюзивного скю отбросился
                    },
                    {
                        'entity': 'product',
                        'id': 2,
                        'offers': {'count': 0},  # оффер из эксклюзивного скю отбросился
                    },
                    {
                        'entity': 'product',
                        'id': 3,
                        'offers': {'count': 1},
                    },
                ],
            },
            allow_different_len=False,
        )

        # c onstock=1 модели 1 и 2 отбросятся (хотя эксклюзивные оффера есть)
        result = self.report.request_json(
            self._gen_prime_request(onstock=True, allow_collapsing=True, use_do=True)
            + '&rearr-factors=market_metadoc_search=no'
        )
        self.check_debug_info(result)
        self.assertFragmentIn(
            result,
            {
                'total': 1,
                'results': [
                    {
                        'entity': 'product',
                        'id': 3,
                        'offers': {
                            'count': 1,
                            'items': [
                                {
                                    'sku': '31',
                                    'wareId': 'i59N_eKSxcRdD6l6mjm71A',
                                }
                            ],
                        },
                    },
                ],
            },
            allow_different_len=False,
        )

    def test_prime_with_ungrouping(self):
        '''Проверяем прайм с разгруппированием. При нем на выдаче и модели и скю.'''

        result = self.report.request_json(
            self._gen_prime_request(allow_ungrouping=True) + '&rearr-factors=market_metadoc_search=no'
        )
        self.check_debug_info(result)
        # Без onstock=1 и ДО показываются недублирующиеся документы на выдаче
        self.assertFragmentIn(
            result,
            {
                'total': 3,
                'results': [
                    {
                        # модель 1, у ее скю 10 и 11 отбросятся эксклюзивные офферы
                        'entity': 'product',
                        'id': 1,
                        'offers': {
                            'count': 0,
                        },
                        # Без ДО не завяжешься на skuStats, поэтому проверяем тип объекта по отладочной инфе
                        'debug': {'properties': {'IS_MODEL': '1', 'MODEL_TYPE': '3', 'MODEL_ID': '1'}},
                    },
                    {
                        # модель 2, у ее единственного скю 21 отбросися эксклюзивный оффер
                        'entity': 'product',
                        'id': 2,
                        'offers': {
                            'count': 0,
                        },
                        'debug': {'properties': {'IS_MODEL': '1', 'MODEL_TYPE': '3', 'MODEL_ID': '2'}},
                    },
                    {
                        # скю 31 или модель 3
                        'entity': 'product',
                        'id': 3,
                        'offers': {'count': 1},  # это относится к модели, совпадает у всех скю одной модели
                    },
                ],
            },
            allow_different_len=False,
        )

        result = self.report.request_json(
            self._gen_prime_request(allow_ungrouping=True) + '&rearr-factors=market_metadoc_search=skus'
        )
        self.check_debug_info(result)
        # Без onstock=1 и ДО показываются недублирующиеся документы на выдаче (в данном случае модели и скухи)
        self.assertFragmentIn(
            result,
            {
                'total': 4,
                'results': [
                    {
                        # модель 1, у ее скю 10 и 11 отбросятся эксклюзивные офферы
                        'entity': 'product',
                        'id': 1,
                        'offers': {
                            'count': 0,
                        },
                        # Без ДО не завяжешься на skuStats, поэтому проверяем тип объекта по отладочной инфе
                        'debug': {'properties': {'IS_MODEL': '1', 'MODEL_TYPE': '3', 'MODEL_ID': '1'}},
                    },
                    {
                        # модель 2, у ее единственного скю 21 отбросися эксклюзивный оффер
                        'entity': 'product',
                        'id': 2,
                        'offers': {
                            'count': 0,
                        },
                        'debug': {'properties': {'IS_MODEL': '1', 'MODEL_TYPE': '3', 'MODEL_ID': '2'}},
                    },
                    {
                        # модель 3
                        'entity': 'product',
                        'id': 3,
                        'offers': {'count': 1},  # это относится к модели, совпадает у всех скю одной модели
                        'debug': {'properties': {'IS_MODEL': '1', 'MODEL_TYPE': '3', 'MODEL_ID': '3'}},
                    },
                    {
                        'entity': 'sku',
                        'id': '31',
                        'offers': {
                            # 'count': 1 - не приходит, т.к. не запрашивали ДО для скух
                        },
                        'debug': {'properties': {'IS_MODEL': '0', 'MODEL_TYPE': '2', 'MODEL_ID': '3'}},
                    },
                ],
            },
            allow_different_len=False,
        )

        result = self.report.request_json(
            self._gen_prime_request(onstock=True, allow_ungrouping=True) + '&rearr-factors=market_metadoc_search=skus'
        )
        self.check_debug_info(result)
        # у моделей 1 и 2 нет не эксклюзивных офферов, c onstock=1 они пропадут с выдачи
        self.assertFragmentIn(
            result,
            {
                'total': 2,
                'results': [
                    {
                        'entity': 'sku',
                        'id': '31',
                        # 'offers': {'count': 1},
                        'debug': {
                            'properties': {
                                'IS_MODEL': '0',
                                'MODEL_TYPE': '2',
                            }
                        },
                    },
                    {
                        'entity': 'product',
                        'id': 3,
                        'offers': {'count': 1},
                        'debug': {
                            'properties': {
                                'IS_MODEL': '1',
                                'MODEL_TYPE': '3',
                            }
                        },
                    },
                ],
            },
            allow_different_len=False,
        )

        # а с onstock=1 и use-default-offers=1 остается только скю с оффером
        result = self.report.request_json(
            self._gen_prime_request(onstock=True, allow_ungrouping=True, use_do=True)
            + '&rearr-factors=market_metadoc_search=no'
        )
        self.check_debug_info(result)
        self.assertFragmentIn(
            result,
            {
                'total': 1,
                'results': [
                    {
                        'entity': 'product',
                        'id': 3,
                        'offers': {'count': 1, 'items': [{'sku': '31', 'wareId': 'i59N_eKSxcRdD6l6mjm71A'}]},
                        'skuStats': {'totalCount': 1},
                    },
                ],
            },
            allow_different_len=False,
        )

    def test_parallel(self):
        '''Проверяем модельные и офферные колдунщики'''

        response = self.report.request_bs('place=parallel&text=model&debug=1&onstock=1')
        # Модели 1 и 2 тоже показываются (видно onstock=1 игнорится).
        # Но модельные колдунщики сейчас не используются, поэтому можно забить.
        self.assertFragmentIn(
            response,
            [
                {'model_id': 1},
                {'model_id': 2},
                {'model_id': 3},
            ],
            allow_different_len=False,
        )

        response = self.report.request_bs('place=parallel&text=offer')
        # остался только оффер для обычного скю 31
        self.assertFragmentIn(
            response,
            {'market_offers_wizard': [{'showcase': {'items': [{'skuId': '31', 'offerId': 'i59N_eKSxcRdD6l6mjm71A'}]}}]},
            allow_different_len=False,
        )

    def test_offerinfo(self):
        '''Плейс offerinfo должен показывать оффер, если не включен флаг disable_purchase_for_yandex_plus=1'''

        place_request = 'place=offerinfo&pp=18&rids=0&show-urls=external&regset=2'
        # Оффер из эксклюзивной скю
        response = self.report.request_json(place_request + '&offerid=cyMvBACGeV54y1vTCtqaCg')
        self.assertFragmentIn(
            response,
            {
                'total': 1,
                'results': [
                    {
                        'wareId': 'cyMvBACGeV54y1vTCtqaCg',
                        'prices': {'value': '250', 'rawValue': '250', 'currency': 'RUR'},
                        'payByYaPlus': {
                            # price - 1, потому что пользователь заплатит один рубль
                            'price': '249'  # а значение из документа выводится как яндекс.плюс баллы
                        },
                    }
                ],
            },
            allow_different_len=False,
        )

        # c rearr-factors=disable_purchase_for_yandex_plus=1 не должен показываться
        req = place_request + '&offerid=cyMvBACGeV54y1vTCtqaCg&debug=1&rearr-factors=disable_purchase_for_yandex_plus=1'
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            {
                'search': {'total': 0, 'results': ElementCount(0)},
                'debug': {'brief': {'filters': {'PURCHASE_FOR_YANDEX_PLUS': 1}}},
            },
            allow_different_len=False,
        )

        # Оффер не из эксклюзивной скю. Должен показываться не зависимо от disable_purchase_for_yandex_plus и всегда без payByYaPlus
        for disable_purchase in ('0', '1'):
            req = (
                place_request
                + '&offerid=i59N_eKSxcRdD6l6mjm71A&debug=1&rearr-factors=disable_purchase_for_yandex_plus={}'.format(
                    disable_purchase
                )
            )
            response = self.report.request_json(req)
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'results': [
                            {'wareId': 'i59N_eKSxcRdD6l6mjm71A', 'prices': {'value': '350'}, 'payByYaPlus': Absent()}
                        ],
                    },
                    'debug': {'brief': {'filters': ElementCount(0)}},
                },
                allow_different_len=False,
            )

    def test_filter_by_cms_promo(self):
        '''Проверяем, что при filter-by-cms-promo=purchase_for_yandex_plus не скрываются оффера и скю за баллы'''

        request = 'place=prime&use-default-offers=1&allow-collapsing=1&rearr-factors=market_metadoc_search=no'

        # без filter-by-cms-promo невалидный запрос, т.к нет и хида и текстового запроса
        response = self.report.request_json(request, strict=False)
        self.assertFragmentIn(response, {'error': {'code': 'EMPTY_REQUEST'}})

        request_with_filter = request + '&filter-by-cms-promo={}'.format(PURCHASE_FOR_YANDEX_PLUS)
        response = self.report.request_json(request_with_filter)
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'product',
                    'id': 1,
                    'slug': 'model-table',
                    'offers': {
                        'count': 2,
                        'items': [
                            {
                                'marketSku': EqualToOneOf('11', '12'),
                                'wareId': EqualToOneOf('cyMvBACGeV54y1vTCtqaCg', 'VApGSN_T1j3gFcCnU400Sg'),
                                'payByYaPlus': EqualToOneOf({'price': '249'}, {'price': '499'}),
                            }
                        ],
                    },
                },
                {
                    'entity': 'product',
                    'id': 2,
                    'slug': 'model-chair',
                    'offers': {
                        'count': 1,
                        'items': [
                            {
                                'marketSku': '21',
                                'slug': 'offer-rainbow-chair',
                                'wareId': 'SiYW-xkUJF-0zig1uUabXg',
                                'payByYaPlus': {'price': '149'},
                            }
                        ],
                    },
                },
            ],
            allow_different_len=False,
        )

        # c disable_purchase_for_yandex_plus не будет ошибки пустого запроса, но и ничего не найдет
        response = self.report.request_json(
            request_with_filter + '&rearr-factors=disable_purchase_for_yandex_plus=1&debug=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {'total': 0, 'results': ElementCount(0)},
                'debug': {'brief': {'filters': {'PURCHASE_FOR_YANDEX_PLUS': 3}}},  # все эксклюзивные оффера
            },
        )

    def test_product_offers(self):
        # запрос эксклюзивной модели без конкретного скю
        model_request = 'place=productoffers&hyperid=1&hid=1'
        response = self.report.request_json(model_request)
        self.assertFragmentIn(
            response,
            {
                'totalOffers': 2,
                'results': [
                    {'marketSku': '10'},
                    {'marketSku': '11'},
                ],
            },
            allow_different_len=False,
        )

        sku_request = model_request + '&market-sku=11&offers-set=defaultList'
        response = self.report.request_json(sku_request)
        self.assertFragmentIn(
            response,
            {
                'totalOffers': 1,
                'results': [
                    {
                        'marketSku': '11',
                        'wareId': 'cyMvBACGeV54y1vTCtqaCg',
                        'prices': {'value': '250'},
                        # price - 1, потому что пользователь заплатит один рубль
                        'payByYaPlus': {'price': '249'},
                        'benefit': {'type': NotEmpty()},  # без этого будет надпись "Нет в продаже"
                    }
                ],
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                'search': {},  # нужны фильтры в корне, а не в results[0]
                'filters': [
                    {
                        'id': '100',
                        'name': 'Size',
                        'values': [
                            {
                                'id': '1001',
                                'marketSku': '10',
                                'value': 'Big',
                                'found': 1,
                            },
                            {
                                'id': '1002',
                                'marketSku': '11',
                                'value': 'Small',
                                'found': 1,
                            },
                        ],
                    }
                ],
            },
        )

        # C disable_purchase_for_yandex_plus перестаем показывать
        response = self.report.request_json(sku_request + '&rearr-factors=disable_purchase_for_yandex_plus=1')
        self.assertFragmentIn(
            response,
            {
                'totalOffers': 0,
                'results': ElementCount(0),
            },
        )

    def test_combine(self):
        req = 'place=combine&rgb=green_with_blue&use-virt-shop=0&rids=0&offers-list=VApGSN_T1j3gFcCnU400Sg:1;msku:10'
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            {
                'total': 1,
                'offers': {
                    'items': [
                        {
                            'wareId': 'VApGSN_T1j3gFcCnU400Sg',
                            'prices': {'value': '500'},
                            'payByYaPlus': {'price': '499'},
                        }
                    ]
                },
                'results': [
                    {
                        'buckets': [
                            {'offers': [{'wareId': 'VApGSN_T1j3gFcCnU400Sg', 'replacedId': 'VApGSN_T1j3gFcCnU400Sg'}]}
                        ]
                    }
                ],
            },
        )

        # C disable_purchase_for_yandex_plus перестаем показывать
        response = self.report.request_json(req + '&rearr-factors=disable_purchase_for_yandex_plus=1')
        self.assertFragmentIn(
            response,
            {
                'total': 0,
                'offers': {'items': ElementCount(0)},
                'results': [
                    {
                        'buckets': [
                            {
                                'offers': [
                                    {
                                        'wareId': '',
                                        'replacedId': 'VApGSN_T1j3gFcCnU400Sg',
                                        'reason': 'PURCHASE_FOR_YANDEX_PLUS',
                                    }
                                ]
                            }
                        ]
                    }
                ],
            },
        )

    def test_actual_delivery(self):
        req = 'place=actual_delivery&rids=0&offers-list=VApGSN_T1j3gFcCnU400Sg:1&&force-use-delivery-calc=1'
        response = self.report.request_json(req)
        self.assertFragmentIn(
            response,
            {
                'total': 1,
                'results': [
                    {
                        'offers': [
                            {
                                'wareId': 'VApGSN_T1j3gFcCnU400Sg',
                                'prices': {'value': '500'},
                                'payByYaPlus': {'price': '499'},
                            }
                        ]
                    }
                ],
            },
        )

        # C disable_purchase_for_yandex_plus перестаем показывать
        response = self.report.request_json(req + '&rearr-factors=disable_purchase_for_yandex_plus=1')
        self.assertFragmentIn(response, {'total': 0, 'results': ElementCount(0)})

    @classmethod
    def prepare_credif_info(cls):
        cls.index.credit_plans_container.credit_plans = [
            CreditPlan(
                plan_id='ABC', bank='Райффайзен банк', term=24, rate=12, initial_payment_percent=0, min_price=300
            )
        ]

    def test_credit_info(self):
        '''Проверяем, что плейс credict_info всегда нормально работает с офферами за баллы.'''

        def gen_request(offer_id, price, disable_purchase):
            req = 'place=credit_info&rids=213&currency=RUR&show-credits=1&rearr-factors=show_credits_on_white=1;'
            req += 'disable_purchase_for_yandex_plus={}'.format(disable_purchase)
            return req + '&offers-list={offer_id}:1;hid:1;p:{price}&total-price={price}'.format(
                offer_id=offer_id, price=price
            )

        # для недешевого оффера всегда покажется creditInfo
        for disable_purchase in (0, 1):
            result = self.report.request_json(gen_request('VApGSN_T1j3gFcCnU400Sg', 500, disable_purchase))
            self.assertFragmentIn(
                result,
                {
                    'creditInfo': {'bestOptionId': 'ABC'},
                    'creditDenial': Absent(),
                },
            )

        # для слишком дешевого оффера кредита никогда не будет
        for disable_purchase in (0, 1):
            result = self.report.request_json(gen_request('cyMvBACGeV54y1vTCtqaCg', 250, disable_purchase))
            self.assertFragmentIn(
                result,
                {
                    'creditInfo': Absent(),
                    'creditDenial': {
                        'maxMinPrice': {'value': '300'},
                        'reason': 'NO_AVAILABLE_CREDIT_PLAN',
                    },
                },
            )

    def test_delivery_route(self):
        req = 'place=delivery_route&pp=18&rids=0&offers-list=VApGSN_T1j3gFcCnU400Sg:1&delivery-type=courier'
        result = self.report.request_json(req)
        self.assertFragmentIn(
            result,
            {
                'total': 1,
                'results': [
                    {
                        'offers': [
                            {
                                'marketSku': '10',
                                'wareId': 'VApGSN_T1j3gFcCnU400Sg',
                                'prices': {'value': '500'},
                                'payByYaPlus': {'price': '499'},
                            }
                        ]
                    }
                ],
            },
        )
        self.error_log.expect(code=ErrorCodes.COMBINATOR_ROUTE_UNAVAILABLE)

        # с disable_purchase_for_yandex_plus=1 оффер не находится
        result = self.report.request_json(req + '&rearr-factors=disable_purchase_for_yandex_plus=1&debug=1')
        self.assertFragmentIn(
            result,
            {
                'search': {'total': 0, 'results': ElementCount(0)},
                'debug': {'brief': {'filters': {'PURCHASE_FOR_YANDEX_PLUS': 1}}},
            },
        )
        self.error_log.expect(code=ErrorCodes.ACD_NONEXISTENT_OFFER)

    @skip('payByYaPlus не поддержано на фронте, лучше "нет в наличии"')
    def test_modelinfo(self):
        req = 'place=modelinfo&hyperid=1&rids=0'
        result = self.report.request_json(req)
        self.assertFragmentIn(
            result,
            {
                'entity': 'product',
                'id': 1,
                'offers': {'count': 2},
                'prices': {'min': '250', 'max': '500'},
                'skuStats': {'totalCount': 2},
            },
        )

        # с disable_purchase_for_yandex_plus=1 оффера не находятся
        result = self.report.request_json(req + '&rearr-factors=disable_purchase_for_yandex_plus=1&debug=1')
        self.assertFragmentIn(
            result,
            {
                'entity': 'product',
                'id': 1,
                'offers': {'count': 0},
                'prices': Absent(),
                'skuStats': {'totalCount': 0},
            },
        )

    @skip('payByYaPlus не поддержано на фронте, лучше "нет в наличии"')
    def test_sku_offers(self):
        req = 'place=sku_offers&market-sku=10&rids=0'
        result = self.report.request_json(req)
        self.assertFragmentIn(
            result,
            {
                'totalOffers': 1,
                'results': [
                    {
                        'entity': 'sku',
                        'id': '10',
                        'offers': {
                            'items': [
                                {
                                    'wareId': 'VApGSN_T1j3gFcCnU400Sg',
                                    'prices': {'value': '500'},
                                    'payByYaPlus': {'price': '499'},
                                },
                            ]
                        },
                    }
                ],
            },
        )

        # с disable_purchase_for_yandex_plus=1 оффер не находится
        result = self.report.request_json(req + '&rearr-factors=disable_purchase_for_yandex_plus=1&debug=1')
        self.assertFragmentIn(
            result,
            {
                'totalOffers': 0,
                'results': [
                    {
                        'entity': 'sku',
                        'id': '10',
                        'offers': {'items': Absent()},
                    }
                ],
            },
        )


if __name__ == '__main__':
    main()
