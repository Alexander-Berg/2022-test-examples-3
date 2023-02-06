#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    Model,
    NavCategory,
    Offer,
    Picture,
    ReportState,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import LikeUrl, NoKey
from core.types.autogen import b64url_md5
from core.logs import ErrorCodes


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.hypertree += [HyperCategory(hid=1, output_type=HyperCategoryType.GURU)]

        cls.index.navtree += [NavCategory(nid=1111, hid=1)]

        cls.index.models += [
            Model(hyperid=101, title='Яндекс.Поиск', hid=1),
            Model(hyperid=102, title='Яндекс.Маркет', hid=1),
            Model(hyperid=103, title='Яндекс.Такси', hid=1),
        ]

        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[213, 2], name='ВМК МГУ'),
            Shop(fesh=2, priority_region=2, regions=[213, 2], name='МехМат СПБГУ'),
            Shop(fesh=3, priority_region=121642, regions=[213, 2], name='Innopolis University'),
        ]

        def pic():
            return Picture(width=100, height=100, group_id=1)

        cls.index.offers += [
            Offer(
                hyperid=101,
                fesh=1,
                waremd5='7l9_KKmh-P6NJR6MDlCj1Q',
                title='Разработчик-стажер из Поиска',
                price=50000,
                randx=1,
                picture=pic(),
            ),
            Offer(
                hyperid=101,
                fesh=2,
                waremd5='q-WVXRCCwqh9g6N-nCp5Vw',
                title='Питерский разработчик из Поиска',
                price=90000,
                randx=2,
                picture=pic(),
            ),
            Offer(
                hyperid=102,
                fesh=1,
                waremd5='Gb7PlRtBgDuGsueK7Gnm9g',
                title='Старший разработчик Яндекс.Маркета',
                price=1005000,
                randx=3,
                picture=pic(),
            ),
            Offer(
                hyperid=102,
                fesh=2,
                waremd5='LkE-wxhQeu5RNmNW0gkYug',
                title='Разработчик Яндекс.Маркета из Питера',
                price=120000,
                randx=4,
                picture=pic(),
            ),
            Offer(
                hyperid=103,
                fesh=1,
                waremd5='U1S5zIgpekEel595IqMvYQ',
                title='Личный разработчик Тиграна в Яндекс.Такси',
                price=150000,
                randx=5,
                picture=pic(),
            ),
            Offer(
                hyperid=103,
                fesh=3,
                waremd5='kiKmN4sf1yn02NQ-Xexe0w',
                title='Разработчик-тестировщик беспилотного такси в Иннополисе',
                price=85000,
                randx=6,
                picture=pic(),
            ),
        ]

    class Rs(object):
        def __init__(self):
            self.__state = ReportState.create()

        def nail(self, wareid, shop_cp=1, vendor_cp=0, fee=0):
            doc = self.__state.search_state.nailed_docs.add()
            doc.ware_id = wareid
            doc.shop_click_price = shop_cp
            doc.vendor_click_price = vendor_cp
            doc.fee = fee
            return self

        def nail_model(self, model_id):
            doc = self.__state.search_state.nailed_docs.add()
            doc.model_id = model_id
            return self

        def nail_recom_model(self, model_id, wareid):
            doc = self.__state.search_state.nailed_docs.add()
            doc.ware_id = wareid
            doc.model_id = model_id
            return self

        def set_use_nailed_docs(self, value):
            self.__state.search_state.use_nailed_docs = value
            return self

        def set_nailed_docs_from_recom_morda(self, value):
            self.__state.search_state.nailed_docs_from_recom_morda = value
            return self

        def to_str(self):
            return ReportState.serialize(self.__state)

    def test_nailed_offer_at_top_by_default(self):
        '''Приколоченные офферы находятся на первых позициях
        Цена клика для них задана в rs
        Приколоченные офферы не влияют на появление (схлопывание) их модели от других офферов
        Приколоченные офферы не появляются в выдаче повторно при отсутствии схлопывания
        '''

        rs = (
            T.Rs()
            .nail('q-WVXRCCwqh9g6N-nCp5Vw', shop_cp=500, fee=23)
            .nail('U1S5zIgpekEel595IqMvYQ', shop_cp=2500, vendor_cp=2, fee=28)
            .to_str()
        )

        # дефолтная сортировка с local-offers-first=0
        response = self.report.request_json(
            'place=prime&text=разработчик&rids=213&local-offers-first=0&allow-collapsing=1&debug=da&rs={}'.format(rs)
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 5,
                    'totalOffers': 2,
                    'results': [
                        {'entity': 'offer', 'wareId': 'q-WVXRCCwqh9g6N-nCp5Vw', 'isPinned': True},
                        {'entity': 'offer', 'wareId': 'U1S5zIgpekEel595IqMvYQ', 'isPinned': True},
                        {'entity': 'product', 'id': 103},
                        {'entity': 'product', 'id': 102},
                        {'entity': 'product', 'id': 101},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'q-WVXRCCwqh9g6N-nCp5Vw',
                'debug': {
                    'sale': {'clickPrice': 500, 'brokeredClickPrice': 500, 'vendorClickPrice': 0, 'brokeredFee': 0}
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'U1S5zIgpekEel595IqMvYQ',
                'debug': {
                    'sale': {
                        'clickPrice': 2500,
                        'brokeredClickPrice': 2502,  # 25+2
                        'vendorClickPrice': 0,
                        'brokeredFee': 0,
                    }
                },
            },
        )

        # дефолтная сортировка с local-offers-first=1
        # приколоченные офферы игнорируются при определении местоположения черты "Доставка из других регионов"
        response = self.report.request_json(
            'place=prime&text=разработчик&rids=213&local-offers-first=1&allow-collapsing=1&debug=da&rs={}'.format(rs)
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'offer', 'wareId': 'q-WVXRCCwqh9g6N-nCp5Vw', 'isPinned': True},
                        {'entity': 'offer', 'wareId': 'U1S5zIgpekEel595IqMvYQ', 'isPinned': True},
                        {'entity': 'product', 'id': 103},
                        {'entity': 'product', 'id': 102},
                        {'entity': 'product', 'id': 101},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # при отключенном схлапывании
        # черта Доставка из других регионов игнорирует первый приколоченный оффер (хотя он из другого региона)
        # приколоченный оффер не дублируется в выдаче, даже несмотря на то, что он и сам по себе нашелся
        response = self.report.request_json(
            'place=prime&text=разработчик&rids=213&local-offers-first=1&allow-collapsing=0&debug=da&rs={}'.format(rs)
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 6,
                    'totalOffers': 6,
                    'results': [
                        {
                            'titles': {'raw': 'Питерский разработчик из Поиска'},
                            'wareId': 'q-WVXRCCwqh9g6N-nCp5Vw',
                            'isPinned': True,
                        },
                        {
                            'titles': {'raw': 'Личный разработчик Тиграна в Яндекс.Такси'},
                            'wareId': 'U1S5zIgpekEel595IqMvYQ',
                            'isPinned': True,
                        },
                        {'titles': {'raw': 'Старший разработчик Яндекс.Маркета'}},
                        {'titles': {'raw': 'Разработчик-стажер из Поиска'}},
                        {'entity': 'regionalDelimiter'},
                        {'titles': {'raw': 'Разработчик-тестировщик беспилотного такси в Иннополисе'}},
                        {'titles': {'raw': 'Разработчик Яндекс.Маркета из Питера'}},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.error_log.expect(code=ErrorCodes.BROKERED_FEE_IS_HIGHER_THEN_SHOP_FEE).times(6)
        self.error_log.expect(code=ErrorCodes.BROKERED_VENDOR_CLICK_PRICE_IS_HIGHER_THEN_VENDOR_BID).times(3)

    def test_fair_price_sorting(self):
        '''На пользовательских сортировках (например на сортировке по цене)
        офферы замешиваются в выдачу честно
        Цена клика высчитывается стандартно (не из rs)
        Черта Доставка из других регионов учитывает региональность всех офферов
        '''

        rs = (
            T.Rs()
            .nail('q-WVXRCCwqh9g6N-nCp5Vw', shop_cp=5, fee=23)
            .nail('U1S5zIgpekEel595IqMvYQ', shop_cp=25, vendor_cp=2, fee=28)
            .to_str()
        )

        response = self.report.request_json(
            'place=prime&text=разработчик&rids=213&local-offers-first=0&how=aprice&allow-collapsing=1&debug=da&rs={}'.format(
                rs
            )
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'id': 101, 'prices': {'min': '50000', 'max': '90000'}},
                        {'id': 103, 'prices': {'min': '85000', 'max': '150000'}},
                        {'wareId': 'q-WVXRCCwqh9g6N-nCp5Vw', 'prices': {'value': '90000'}},
                        {'id': 102, 'prices': {'min': '120000', 'max': '1005000'}},
                        {'wareId': 'U1S5zIgpekEel595IqMvYQ', 'prices': {'value': '150000'}},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=prime&text=разработчик&rids=213&local-offers-first=1&allow-collapsing=0&how=aprice&debug=da&rs={}'.format(
                rs
            )
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 6,
                    'totalOffers': 6,
                    'results': [
                        {'titles': {'raw': 'Разработчик-стажер из Поиска'}, 'prices': {'value': '50000'}},
                        {
                            'titles': {'raw': 'Личный разработчик Тиграна в Яндекс.Такси'},
                            'prices': {'value': '150000'},
                            'wareId': 'U1S5zIgpekEel595IqMvYQ',
                        },
                        {'titles': {'raw': 'Старший разработчик Яндекс.Маркета'}, 'prices': {'value': '1005000'}},
                        {'entity': 'regionalDelimiter'},
                        {
                            'titles': {'raw': 'Разработчик-тестировщик беспилотного такси в Иннополисе'},
                            'prices': {'value': '85000'},
                        },
                        {
                            'titles': {'raw': 'Питерский разработчик из Поиска'},
                            'prices': {'value': '90000'},
                            'wareId': 'q-WVXRCCwqh9g6N-nCp5Vw',
                        },
                        {'titles': {'raw': 'Разработчик Яндекс.Маркета из Питера'}, 'prices': {'value': '120000'}},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'U1S5zIgpekEel595IqMvYQ',
                'debug': {'sale': {'bidType': 'minbid', 'minBid': 90, 'brokeredClickPrice': 90}},
            },
        )

    def test_nailed_in_logs_by_defult(self):
        '''Проверяем что в лог записывается nailed=1 для приколоченных документов на дефолтной сортировке'''
        rs = T.Rs().nail('q-WVXRCCwqh9g6N-nCp5Vw', shop_cp=5, fee=23).to_str()

        self.report.request_json(
            'place=prime&text=разработчик&rids=213&local-offers-first=1&allow-collapsing=0&debug=da&rs={}'.format(rs)
        )
        self.error_log.expect(code=ErrorCodes.BROKERED_FEE_IS_HIGHER_THEN_SHOP_FEE)

        self.show_log.expect(ware_md5='q-WVXRCCwqh9g6N-nCp5Vw', nailed=1).once()
        self.show_log.expect(ware_md5='LkE-wxhQeu5RNmNW0gkYug', nailed=0).once()

        self.feature_log.expect(ware_md5='q-WVXRCCwqh9g6N-nCp5Vw', nailed=1).once()
        self.feature_log.expect(ware_md5='LkE-wxhQeu5RNmNW0gkYug', nailed=0).once()

    def test_nailed_in_logs_by_user_sorting(self):
        '''Проверяем что в лог записывается nailed=1 для приколоченных документов на пользовательских сортировках'''
        rs = T.Rs().nail('q-WVXRCCwqh9g6N-nCp5Vw', shop_cp=5, fee=23).to_str()

        self.report.request_json(
            'place=prime&text=разработчик&rids=213&local-offers-first=1&allow-collapsing=0&how=aprice&debug=da&rs={}'.format(
                rs
            )
        )

        self.show_log.expect(ware_md5='q-WVXRCCwqh9g6N-nCp5Vw', nailed=1).once()
        self.show_log.expect(ware_md5='LkE-wxhQeu5RNmNW0gkYug', nailed=0).once()

        self.feature_log.expect(ware_md5='q-WVXRCCwqh9g6N-nCp5Vw', nailed=1).once()
        self.feature_log.expect(ware_md5='LkE-wxhQeu5RNmNW0gkYug', nailed=0).once()

    def test_link_to_search_with_nailed_document(self):
        '''Проверяем что в офферной врезке offercardUrl содержит ссылки с rs ведущие на поиск
        market_adg_offer_url_type=NailedInSearch - ссылка на search с сvredirect=0
        market_adg_offer_url_type=NailedInCatalog - ссылка на catalog/xxx/list
        '''

        # market_check_offers_incut_size=0 отключает проверку на хотя бы 4 офера
        request = 'place=parallel&text=разработчик&rids=213&rearr-factors=market_check_offers_incut_size=0;'

        response = self.report.request_bs(request + 'market_adg_offer_url_type=NailedInSearch;')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {
                                            "__hl": {"text": "Личный разработчик Тиграна в Яндекс.Такси", "raw": True}
                                        },
                                        "offercardUrl": LikeUrl.of(
                                            '//market.yandex.ru/search?rs=eJwzUvCS4xILNQw2rfJML0jNdk3NMbU09Sz0LYsMlIhSYNBgAACeKwjt&text=разработчик&cvredirect=0&clid=913'
                                        ),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {
                                            "__hl": {
                                                "text": "Разработчик-тестировщик беспилотного такси в Иннополисе",
                                                "raw": True,
                                            }
                                        },
                                        "offercardUrl": LikeUrl.of(
                                            '//market.yandex.ru/search?rs=eJwzUvCS4xLLzvTO9TMpTjOszDMw8gvUjUitSDUol4hSYNBgAAChcwkD&text=разработчик&cvredirect=0&clid=913'
                                        ),
                                    }
                                },
                            ]
                        }
                    }
                ]
            },
        )

        response = self.report.request_bs(request + 'market_adg_offer_url_type=NailedInCatalog;')
        self.assertFragmentIn(
            response,
            {
                "market_offers_wizard": [
                    {
                        "showcase": {
                            "items": [
                                {
                                    "title": {
                                        "text": {
                                            "__hl": {"text": "Личный разработчик Тиграна в Яндекс.Такси", "raw": True}
                                        },
                                        "offercardUrl": LikeUrl.of(
                                            '//market.yandex.ru/catalog/1111/list?hid=1&rs=eJwzUvCS4xILNQw2rfJML0jNdk3NMbU09Sz0LYsMlIhSYNBgAACeKwjt&text=разработчик&clid=913'
                                        ),
                                    }
                                },
                                {
                                    "title": {
                                        "text": {
                                            "__hl": {
                                                "text": "Разработчик-тестировщик беспилотного такси в Иннополисе",
                                                "raw": True,
                                            }
                                        },
                                        "offercardUrl": LikeUrl.of(
                                            '//market.yandex.ru/catalog/1111/list?hid=1&rs=eJwzUvCS4xLLzvTO9TMpTjOszDMw8gvUjUitSDUol4hSYNBgAAChcwkD&text=разработчик&clid=913'
                                        ),
                                    }
                                },
                            ]
                        }
                    }
                ]
            },
        )

        # тот же rs при поиске возвращает прибитый документ
        # для Личного разработчика прибивается Личный разработчик Тиграна в Я.Такси
        response = self.report.request_json(
            'place=prime&text=разработчик&rids=213&cvredirect=0&clid=913&allow-collapsing=1&local-offers-first=0'
            + '&rs=eJwzUvCS4xILNQw2rfJML0jNdk3NMbU09Sz0LYsMlIhSYNBgAACeKwjt'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {'raw': 'Личный разработчик Тиграна в Яндекс.Такси'},
                            'wareId': 'U1S5zIgpekEel595IqMvYQ',
                            'isPinned': True,
                        },
                        {'entity': 'product', 'id': 103},
                        {'entity': 'product', 'id': 102},
                        {'entity': 'product', 'id': 101},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_nailed_models_at_top_by_default(self):
        """Приколоченные модели находятся на первых позициях
        Приколоченные модели не появляются в выдаче повторно
        """
        rs = T.Rs().nail_model('101').nail_model('103').to_str()

        # Приколоченные модели находятся на первых позициях и не дублируются в выдаче
        response = self.report.request_json(
            'place=prime&text=Яндекс&rids=213&rearr-factors=market_metadoc_search=no&rs={}'.format(rs)
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 6,
                    'totalOffers': 3,
                    'totalModels': 3,
                    'results': [
                        {'entity': 'product', 'id': 101, 'isPinned': True},  # приколоченная модель
                        {'entity': 'product', 'id': 103, 'isPinned': True},  # приколоченная модель
                        {'entity': 'product', 'id': 102},
                        {'titles': {'raw': 'Личный разработчик Тиграна в Яндекс.Такси'}},
                        {'titles': {'raw': 'Старший разработчик Яндекс.Маркета'}},
                        {'entity': 'regionalDelimiter'},
                        {'titles': {'raw': 'Разработчик Яндекс.Маркета из Питера'}},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # По запросу "some request" документы не находятся, в выдаче только приколоченные модели
        response = self.report.request_json(
            'place=prime&text=some+request&rids=213&rearr-factors=market_metadoc_search=no&rs={}'.format(rs)
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'totalOffers': 0,
                    'totalModels': 2,
                    'results': [
                        {'entity': 'product', 'id': 101, 'isPinned': True},
                        {'entity': 'product', 'id': 103, 'isPinned': True},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Без приколоченных моделей по запросу "some request" получаем пустую выдачу
        response = self.report.request_json(
            'place=prime&text=some+request&rids=213&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {'search': {'total': 0, 'totalOffers': 0, 'totalModels': 0, 'results': []}},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_nailed_in_logs_by_default(self):
        """Проверяем что в лог записывается nailed=1 для приколоченных моделей на дефолтной сортировке"""
        rs = T.Rs().nail_model('101').to_str()

        self.report.request_json('place=prime&text=Яндекс&rids=213&reqid=1&rs={}'.format(rs))

        self.show_log.expect(reqid=1, hyper_id=101, nailed=1).once()
        self.show_log.expect(reqid=1, hyper_id=102, nailed=0).times(3)  # 1 модель + 2 оффера

        self.feature_log.expect(req_id=1, model_id=101, nailed=1).once()
        self.feature_log.expect(req_id=1, model_id=102, nailed=0).times(3)  # 1 модель + 2 оффера

    def test_nailed_docs_if_empty_serp(self):
        """Проверяем что при наличии в параметре &rs флага use_nailed_docs=false
        приколоченные документы отображаются только после перезапроса за пустым серпом
        https://st.yandex-team.ru/MARKETOUT-23154
        """
        rs = (
            T.Rs()
            .nail_model('101')
            .nail_model('103')
            .nail('q-WVXRCCwqh9g6N-nCp5Vw', shop_cp=5, fee=23)
            .set_use_nailed_docs(False)
            .to_str()
        )

        # По запросу находятся документы, приколоченные документы из параметра &rs не отображаются
        response = self.report.request_json(
            'place=prime&text=стажер&rids=213&clid=698&rearr-factors=market_metadoc_search=no&rs={}'.format(rs)
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'totalOffers': 1,
                    'totalModels': 0,
                    'results': [
                        {'entity': 'offer', 'wareId': '7l9_KKmh-P6NJR6MDlCj1Q'},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # По запросу не находятся документы, отображаются приколоченные документы из параметра &rs
        response = self.report.request_json(
            'place=prime&text=some+request&rids=213&clid=698&rearr-factors=market_metadoc_search=no&rs={}'.format(rs)
        )
        self.error_log.expect(code=ErrorCodes.BROKERED_FEE_IS_HIGHER_THEN_SHOP_FEE)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'totalOffers': 1,
                    'totalModels': 2,
                    'results': [
                        {'entity': 'product', 'id': 101, 'isPinned': True},
                        {'entity': 'product', 'id': 103, 'isPinned': True},
                        {'entity': 'offer', 'wareId': 'q-WVXRCCwqh9g6N-nCp5Vw'},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_nailed_docs_if_empty_serp_2(self):
        """
        проверяем, что place=blender отработает так же как и прайм в этом случае
        """
        rs = (
            T.Rs()
            .nail_model('101')
            .nail_model('103')
            .nail('q-WVXRCCwqh9g6N-nCp5Vw', shop_cp=5, fee=23)
            .set_use_nailed_docs(False)
            .to_str()
        )

        # По запросу находятся документы, приколоченные документы из параметра &rs не отображаются
        response = self.report.request_json(
            'place=blender&text=стажер&rids=213&clid=698&rearr-factors=market_metadoc_search=no&rs={}'.format(rs)
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'totalOffers': 1,
                    'totalModels': 0,
                    'results': [
                        {'entity': 'offer', 'wareId': '7l9_KKmh-P6NJR6MDlCj1Q'},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # По запросу не находятся документы, отображаются приколоченные документы из параметра &rs
        response = self.report.request_json(
            'place=blender&text=some+request&rids=213&clid=698&rearr-factors=market_metadoc_search=no&rs={}'.format(rs)
        )
        self.error_log.expect(code=ErrorCodes.BROKERED_FEE_IS_HIGHER_THEN_SHOP_FEE)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'totalOffers': 1,
                    'totalModels': 2,
                    'results': [
                        {'entity': 'product', 'id': 101, 'isPinned': True},
                        {'entity': 'product', 'id': 103, 'isPinned': True},
                        {'entity': 'offer', 'wareId': 'q-WVXRCCwqh9g6N-nCp5Vw'},
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_nailed_models_on_blue(cls):
        MAX_HYPER_ID = 230
        hyperids = list(range(200, MAX_HYPER_ID + 1))

        cls.index.hypertree += [HyperCategory(hid=2, output_type=HyperCategoryType.GURU)]

        cls.index.navtree += [NavCategory(nid=2222, hid=2)]

        cls.index.models += [Model(hyperid=hyperid, hid=2) for hyperid in hyperids]

        def pic():
            return Picture(width=100, height=100, group_id=1)

        for i in range(50):
            cls.index.offers += [
                Offer(
                    hyperid=MAX_HYPER_ID,
                    waremd5=b64url_md5('white-{}-{}'.format(hyperid, i)),
                    randx=i + 100,
                    picture=pic(),
                ),
            ]

        cls.index.mskus += [
            MarketSku(
                title='sku{}'.format(hyperid),
                hid=2,
                hyperid=hyperid,
                sku=hyperid * 10,
                blue_offers=[BlueOffer(waremd5=b64url_md5('blue-{}'.format(hyperid)), randx=i, price=1000 - i)],
            )
            for i, hyperid in enumerate(hyperids)
        ]

    def test_nailed_models_hide_on_sort(self):
        """
        Опция nailed_docs_from_recom_morda в ReportState позволяет не прибивать документы,
        если задана пользовательская сортировка.
        """

        request = 'place=prime&nid=2222&rgb=blue&rs={rs}&how=aprice&numdoc=4'

        rs = T.Rs().nail_model('201').nail_model('220').to_str()
        response = self.report.request_json(request.format(rs=rs))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'id': 201, 'prices': {'min': '999', 'max': '999'}, 'isPinned': True},
                        {'id': 220, 'prices': {'min': '980', 'max': '980'}, 'isPinned': True},
                        {'id': 230, 'prices': {'min': '970', 'max': '970'}, 'isPinned': NoKey('isPinned')},
                        {'id': 229, 'prices': {'min': '971', 'max': '971'}, 'isPinned': NoKey('isPinned')},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        rs = T.Rs().nail_model('201').nail_model('220').set_nailed_docs_from_recom_morda(True).to_str()
        response = self.report.request_json(request.format(rs=rs))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'id': 230, 'prices': {'min': '970', 'max': '970'}, 'isPinned': NoKey('isPinned')},
                        {'id': 229, 'prices': {'min': '971', 'max': '971'}, 'isPinned': NoKey('isPinned')},
                        {'id': 228, 'prices': {'min': '972', 'max': '972'}, 'isPinned': NoKey('isPinned')},
                        {'id': 227, 'prices': {'min': '973', 'max': '973'}, 'isPinned': NoKey('isPinned')},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    @classmethod
    def prepare_remove_nailed_model_without_nailed_offer(cls):
        cls.index.hypertree += [HyperCategory(hid=41, output_type=HyperCategoryType.GURU)]

        cls.index.navtree += [NavCategory(nid=17, hid=41)]

        cls.index.models += [
            Model(hyperid=12000, hid=41),
            Model(hyperid=21000, hid=41),
        ]

        cls.index.mskus += [
            MarketSku(
                title='sku{}'.format(210000),
                hid=41,
                hyperid=21000,
                sku=210000,
                blue_offers=[BlueOffer(waremd5=b64url_md5('blue-{}'.format(210000)), randx=152, price=1000)],
            ),
        ]

    def test_remove_nailed_model_without_nailed_offer(self):
        """
        У запиненной модели оффера нет в индексе
        """
        rs = (
            T.Rs()
            .nail_recom_model(str(12000), b64url_md5('blue-{}'.format(120000)))
            .set_nailed_docs_from_recom_morda(True)
            .to_str()
        )

        response = self.report.request_json(
            'place=prime&nid=17&rgb=blue&rs={rs}&numdoc={ndoc}&debug=1'.format(rs=rs, ndoc=10)
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'id': 21000,
                            'offers': {'items': [{'wareId': b64url_md5('blue-{}'.format(210000))}]},
                            'isPinned': NoKey('isPinned'),
                        }
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_recom_morda_accept_nailed_doc_without_ware_id(self):
        """
        С морды может быть запин моделей без офферов
        """
        rs = T.Rs().nail_model('101').nail_model('103').set_nailed_docs_from_recom_morda(True).to_str()

        request = 'place=prime&nid=2222&rgb=blue&rs={rs}&numdoc={ndoc}&debug=1&rearr-factors=recom_accept_nailed_doc_without_ware_id={nail_without_ware_id}'

        response = self.report.request_json(request.format(rs=rs, ndoc=10, nail_without_ware_id=True))

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'product', 'id': 101, 'isPinned': True},  # приколоченная модель
                        {'entity': 'product', 'id': 103, 'isPinned': True},  # приколоченная модель
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=True,
        )

        response = self.report.request_json(request.format(rs=rs, ndoc=10, nail_without_ware_id=False))

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {'entity': 'product', 'isPinned': NoKey('isPinned')},
                    ]
                }
            },
            preserve_order=True,
            allow_different_len=True,
        )


if __name__ == '__main__':
    main()
