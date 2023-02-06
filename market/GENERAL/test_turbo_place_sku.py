#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import NotEmptyList, EmptyList, NotEmpty, Wildcard, LikeUrl
from core.testcase import TestCase, main
from core.types import (
    Region,
    Model,
    GLParam,
    ParameterValue,
    ImagePickerData,
    MarketSku,
    BlueOffer,
    Vat,
    Vendor,
    ClickType,
    MarketSkuTransition,
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                    Region(rid=2, name='Санкт-Петербург'),
                ],
            )
        ]

        cls.index.vendors += [Vendor(vendor_id=1, name='Cisco')]

        cls.index.models += [
            Model(
                hyperid=1,
                hid=1,
                title='Cisco phone',
                vendor_id=1,
                glparams=[
                    GLParam(param_id=101, value=1),
                    GLParam(param_id=102, value=1),
                    GLParam(param_id=103, value=1),
                    GLParam(param_id=104, value=1),
                ],
                parameter_value_links=[
                    ParameterValue(
                        param_id=201,
                        option_id=3,
                        picture=ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_model1_201_3/orig',
                            namespace='get-mpic',
                            group_id='466729',
                            image_name='img_model1_201_3',
                        ),
                    ),
                    ParameterValue(
                        param_id=201,
                        option_id=2,
                        picture=ImagePickerData(
                            url='//avatars.mds.yandex.net/get-mpic/466729/img_model1_201_2/orig',
                            namespace='get-mpic',
                            group_id='466729',
                            image_name='img_model1_201_2',
                        ),
                    ),
                ],
            ),
            Model(hyperid=2, hid=1, title='Samsung Galaxy S10'),
            Model(hyperid=3, hid=1, title='Apple iPhone Xr'),
            Model(hyperid=4, hid=1, title='Old Mobile Phone'),
        ]

        cls.index.mskus += [
            MarketSku(
                title='Cisco phone Green',
                hyperid=1,
                vendor_id=1,
                sku=1,
                blue_offers=[
                    BlueOffer(
                        price=100,
                        feedid=3,
                        offerid='blue.offer.1.1',
                        waremd5='xMpCOKC5I4INzFCab3WEmQ',
                    ),
                    BlueOffer(price=110, feedid=3, offerid='blue.offer.1.2'),
                ],
                descr='Cisco phone Green description',
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                randx=1,
            ),
            MarketSku(
                title='Cisco phone Red',
                hyperid=1,
                sku=2,
                blue_offers=[
                    BlueOffer(price=5, vat=Vat.VAT_10, feedid=3, offerid='blue.offer.2.1'),
                ],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                randx=4,
            ),
            MarketSku(
                title='Samsung Galaxy S10 Black',
                hyperid=2,
                sku=3,
                blue_offers=[
                    BlueOffer(price=11, vat=Vat.VAT_10, feedid=3, offerid='blue.offer.3.1'),
                ],
            ),
            MarketSku(
                title='Apple iPhone Xr Orange',
                hyperid=3,
                sku=4,
                blue_offers=[
                    BlueOffer(price=11, vat=Vat.VAT_10, feedid=3, offerid='blue.offer.4.1'),
                ],
            ),
            MarketSku(
                title='Apple iPhone 7 Gray',
                sku=5,
                blue_offers=[
                    BlueOffer(price=11, vat=Vat.VAT_10),
                ],
            ),
            MarketSku(
                title='Old mobile phone sku',
                hyperid=4,
                sku=6,
                blue_offers=[],
            ),
        ]

        cls.index.msku_transitions += [
            MarketSkuTransition(src_id=100500, dst_id=6),
        ]

    def test_get_market_sku(self):
        response = self.report.request_json('place=turbo&market-sku=1&rids=0&rearr-factors=market_nordstream=0')
        offer_url = 'market.yandex.ru/bundle/1?schema=type,objId,count&data=offer,xMpCOKC5I4INzFCab3WEmQ,1&fromTurbo=1&clid=926&lr=0'
        self.assertFragmentIn(
            response,
            {
                'knownThumbnails': NotEmptyList(),
                'result': {
                    'entity': 'sku',
                    'id': '1',
                    'modelId': 1,
                    'titles': {
                        'raw': 'Cisco phone Green',
                    },
                    'vendor': {
                        'entity': 'vendor',
                        'id': 1,
                        'name': 'Cisco',
                        'slug': 'cisco',
                    },
                    'product': {
                        'entity': 'product',
                        'type': 'model',
                        'titles': {
                            'raw': 'Cisco phone',
                        },
                        'offers': {
                            'count': 3,
                            'cutPriceCount': 0,
                        },
                        'specs': {},
                    },
                    'slug': 'cisco-phone-green',
                    'description': 'Cisco phone Green description',
                    'navnodes': NotEmptyList(),
                    'categories': NotEmptyList(),
                    'pictures': NotEmptyList(),
                    'formattedDescription': {
                        'shortPlain': 'Cisco phone Green description',
                        'fullPlain': 'Cisco phone Green description',
                        'shortHtml': 'Cisco phone Green description',
                        'fullHtml': 'Cisco phone Green description',
                    },
                    'offers': {
                        'items': [
                            {
                                'entity': 'offer',
                                'titles': {
                                    'raw': 'Cisco phone Green',
                                },
                                'slug': 'cisco-phone-green',
                                'pictures': NotEmptyList(),
                                'delivery': {
                                    'shopPriorityRegion': {
                                        'entity': 'region',
                                    },
                                    'shopPriorityCountry': {
                                        'entity': 'region',
                                    },
                                    'isPriorityRegion': False,
                                    'isCountrywide': False,
                                    'isAvailable': True,
                                    'hasPickup': True,
                                    'hasLocalStore': True,
                                    'hasPost': False,
                                    'isFree': False,
                                    'isDownloadable': False,
                                    'inStock': True,
                                    'postAvailable': True,
                                    'options': [],
                                    'deliveryPartnerTypes': [],
                                },
                                'shop': {
                                    'entity': 'shop',
                                },
                                'wareId': NotEmpty(),
                                'cpa': 'real',
                                'sku': '1',
                                'ownMarketPlace': True,
                                'prices': {
                                    'currency': 'RUR',
                                    'value': '100',
                                },
                                'benefit': {
                                    'type': 'default',
                                },
                                'isFulfillment': True,
                                'supplier': {
                                    'entity': 'shop',
                                },
                                'realShop': {
                                    'entity': 'shop',
                                },
                                'urls': {
                                    'direct': LikeUrl.of(offer_url),
                                },
                            },
                        ],
                    },
                },
            },
        )

    def test_get_invalid_sku(self):
        """Ожидается пустой ответ, если запрошен несуществующий sku"""
        response = self.report.request_json('place=turbo&market-sku=100500&rids=0')
        self.assertFragmentIn(response, {'result': {}})

    def test_get_invalid_sku_with_transitions(self):
        """
        Если задан несуществующий sku, но есть MarketSkuTransition из него
        и запрос с &with-rebuilt-model=1 в синий, то должен вернуться sku из транзишна
        """
        response = self.report.request_json('place=turbo&market-sku=100500&rids=0&with-rebuilt-model=1')
        self.assertFragmentIn(
            response,
            {
                'knownThumbnails': NotEmptyList(),
                'result': {
                    'entity': 'sku',
                    'id': '6',
                    'modelId': 4,
                    'titles': {
                        'raw': 'Old mobile phone sku',
                    },
                    'offers': {
                        'items': EmptyList(),
                    },
                },
            },
        )

    def test_get_market_sku_without_recommendations(self):
        """Если для sku нет рекомендаций, должен приходить пустой список"""
        response = self.report.request_json('place=turbo&market-sku=3&rids=0')
        self.assertFragmentIn(
            response,
            {
                'knownThumbnails': NotEmptyList(),
                'result': {
                    'entity': 'sku',
                    'id': '3',
                    'modelId': 2,
                    'titles': {
                        'raw': 'Samsung Galaxy S10 Black',
                    },
                },
            },
        )

    def test_no_offers(self):
        """Если у sku нет оферов, должен выводиться валидный json"""
        response = self.report.request_json('place=turbo&market-sku=6&rids=0')
        self.assertFragmentIn(
            response,
            {
                'knownThumbnails': NotEmptyList(),
                'result': {
                    'entity': 'sku',
                    'id': '6',
                    'modelId': 4,
                    'titles': {
                        'raw': 'Old mobile phone sku',
                    },
                    'offers': {
                        'items': EmptyList(),
                    },
                },
            },
        )

    def test_cpa_urls_with_redirect(self):
        """Проверяем, что для данного плейса возвращаются CPA URL'ы с редиректом - такие же как encrypted"""
        response = self.report.request_json('place=turbo&market-sku=1&show-urls=external,cpa&rids=0')
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offers': {
                        'items': [
                            {
                                'entity': 'offer',
                                'urls': {'encrypted': Wildcard('/redir/*'), 'cpa': Wildcard('/redir/*')},
                            }
                        ],
                    }
                }
            },
        )
        self.click_log.expect(
            ClickType.EXTERNAL, click_url=Wildcard('/redir/*'), data_url=Wildcard('market.yandex.ru%2Fbundle*')
        )
        self.click_log.expect(
            ClickType.CPA, click_url=Wildcard('/redir/*'), data_url=Wildcard('market.yandex.ru%2Fbundle*')
        )

    def test_clid_from_clid_cgi_param(self):
        response = self.report.request_json('place=turbo&market-sku=1&rids=0&clid=123')
        offer_url = 'market.yandex.ru/bundle/1?schema=type,objId,count&data=offer,xMpCOKC5I4INzFCab3WEmQ,1&fromTurbo=1&clid=123&lr=0'
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offers': {
                        'items': [
                            {
                                'urls': {
                                    'direct': LikeUrl.of(offer_url),
                                }
                            }
                        ],
                    },
                },
            },
        )

    def test_clid_from_pof_cgi_param(self):
        response = self.report.request_json('place=turbo&market-sku=1&rids=0&pof={"clid":["234"]}')
        offer_url = 'market.yandex.ru/bundle/1?schema=type,objId,count&data=offer,xMpCOKC5I4INzFCab3WEmQ,1&fromTurbo=1&clid=234&lr=0'
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offers': {
                        'items': [
                            {
                                'urls': {
                                    'direct': LikeUrl.of(offer_url),
                                }
                            }
                        ],
                    },
                },
            },
        )

    def test_purchase_referrer(self):
        response = self.report.request_json(
            'place=turbo&market-sku=1&rids=0&rearr-factors=market_purchase_referrer=beru_in_portal'
        )
        offer_url = 'market.yandex.ru/bundle/1?schema=type,objId,count&data=offer,xMpCOKC5I4INzFCab3WEmQ,1&fromTurbo=1&clid=926&lr=0&purchase-referrer=beru_in_portal'
        self.assertFragmentIn(
            response,
            {
                'result': {
                    'offers': {
                        'items': [
                            {
                                'urls': {
                                    'direct': LikeUrl.of(offer_url),
                                }
                            }
                        ],
                    },
                },
            },
        )


if __name__ == '__main__':
    main()
