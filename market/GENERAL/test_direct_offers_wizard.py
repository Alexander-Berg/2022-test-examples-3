#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import DynamicShop, Offer, Region, Shop
from core.testcase import TestCase, main

from core.matcher import Contains, LikeUrl, NoKey, NotEmpty
from adv.direct.proto.banner_resources.banner_feed_info_pb2 import FeedInfo
from core.matcher import NotEmptyList
from unittest import skip
import base64

# Regions
MOSCOW_REGION_RIDS = 1
CFD_RIDS = 3
MOSCOW_RIDS = 213


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(
                rid=CFD_RIDS,
                name="Центральный федеральный округ",
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=MOSCOW_REGION_RIDS,
                        name="Московская область",
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(
                                rid=MOSCOW_RIDS,
                                name="Москва",
                            )
                        ],
                    )
                ],
            )
        ]
        shop_count = 10
        offers_per_shop = 10

        # проверяем, что офферы Директа из магазина без CPA/CPC будут показаны на выдаче
        cls.index.shops += [Shop(fesh=1, cpc=Shop.CPC_NO, cpa=Shop.CPA_NO)]

        # add direct offers from external shops
        for shop_id in range(1, shop_count + 1):
            for id in range(1, offers_per_shop + 1):
                cls.index.offers += [
                    Offer(
                        fesh=shop_id,
                        offerid=shop_id * 10 + id,
                        price=id * shop_id,
                        title='Direct offer {} in shop {}'.format(id, shop_id),
                        is_direct=True,
                        url='http://direct_shop_{}.ru/offers?id={}'.format(shop_id, id),
                        direct_moderation_flags='1,2,3',
                        business_id=999,
                        direct_feed_id=300 + shop_id * 10 + id,
                        direct_filter_id=[400 + shop_id * 10 + id],
                    )
                ]

        # add market offers
        for id in range(offers_per_shop):
            cls.index.offers += [Offer(fesh=123, offerid=100 + id, price=555, title='market offer {}'.format(id))]

    @skip('no wizards')
    def test_direct_offers_wizard(self):
        """https://st.yandex-team.ru/MARKETOUT-38517"""

        request = 'place=parallel&text=offer'
        response = self.report.request_bs_pb(request)

        # test no effect of market wizard
        self.assertFragmentIn(
            response,
            {
                'market_direct_offers_wizard_0': NoKey('market_direct_offers_wizard_0'),
            },
        )

        request = 'place=parallel&text=offer&rearr-factors=market_enable_direct_offers_wizard=1'
        response = self.report.request_bs_pb(request)

        # no market offers for new search
        self.assertFragmentNotIn(
            response,
            {
                'market_direct_offers_wizard_0': {
                    'title': {'text': {'__hl': {'text': 'market offer 0'}}},
                    'price': {
                        'currency': 'RUR',
                        'priceMin': '555',
                    },
                }
            },
        )

        # test docs format
        shop_url = 'direct_shop_1.ru'
        self.assertFragmentIn(
            response,
            {
                'market_direct_offers_wizard_0': {
                    # fixme: показывает тайтл второго офера
                    # 'title': {'__hl': {'text': 'Direct offer 1 in shop 1'}},
                    'url': Contains(shop_url),
                    'urlTouch': Contains(shop_url),
                    'greenUrl': [
                        {
                            'text': 'SHOP-1',
                            'url': Contains(shop_url),
                            'urlTouch': Contains(shop_url),
                        }
                    ],
                    'offer_count': 10,
                    'shopId': 1,
                    'showcase': {
                        'items': [
                            {
                                'title': {'text': {'__hl': {'text': 'Direct offer 1 in shop 1', 'raw': True}}},
                                'price': {
                                    'currency': 'RUR',
                                    'priceMin': '1',
                                },
                                'moderation_flags': [1, 2, 3],
                                'thumb': {
                                    'url': LikeUrl.of('http://{}/offers?id=1'.format(shop_url)),
                                    'urlTouch': LikeUrl.of('http://{}/offers?id=1'.format(shop_url)),
                                },
                            },
                        ]
                    },
                    "feed_info": {
                        "direct_feed_id": NotEmpty(),
                        "market_business_id": 999,
                        "market_shop_id": 1,
                    },
                }
            },
        )

    @classmethod
    def prepare_feed_info_search(cls):
        for i in range(10):
            # в рамках одного магазина должны быть одинаковые фиды и фильтры
            # 999 для проверки, что правильно используем массив фильтров
            cls.index.offers += [
                Offer(title='Iphone offer with filter 100', is_direct=True, direct_filter_id=[100, 999], fesh=600),
                Offer(title='Iphone offer with filter 101', is_direct=True, direct_filter_id=[101, 999], fesh=601),
                Offer(title='Iphone offer with filter 102', is_direct=True, direct_filter_id=[102, 999], fesh=602),
                Offer(title='Iphone offer with filter 103', is_direct=True, direct_filter_id=[103], fesh=603),
                Offer(title='Iphone offer with filter 104', is_direct=True, direct_filter_id=[104], fesh=604),
                Offer(title='Iphone offer with filter 105', is_direct=True, direct_filter_id=[105], fesh=605),
                Offer(title='Iphone offer with feed 200', is_direct=True, direct_feed_id=200, fesh=700),
                Offer(title='Iphone offer with feed 201', is_direct=True, direct_feed_id=201, fesh=701),
                Offer(title='Iphone offer with feed 202', is_direct=True, direct_feed_id=202, fesh=702),
                Offer(title='Iphone offer with feed 203', is_direct=True, direct_feed_id=203, fesh=703),
                Offer(title='Iphone offer with feed 204', is_direct=True, direct_feed_id=204, fesh=704),
                Offer(title='Iphone offer with feed 205', is_direct=True, direct_feed_id=205, fesh=705),
                Offer(title='Iphone offer with filter 666', is_direct=True, direct_filter_id=[666, 777, 888], fesh=600),
                Offer(title='Iphone offer with filter 777', is_direct=True, direct_filter_id=[666, 777, 888], fesh=600),
                Offer(title='Iphone offer with filter 888', is_direct=True, direct_filter_id=[666, 777, 888], fesh=600),
            ]

        for i in range(6):
            cls.index.shops += [Shop(fesh=600 + i, priority_region=213), Shop(fesh=700 + i, priority_region=213)]

    def test_feed_info_search(self):
        '''
        Проверяем как работает поиск по feedId и filterId, полученных из кэша через cgi cache-feed-info и эксп флага market_direct_feed_white_list соответсвенно
        https://st.yandex-team.ru/MARKETOUT-39160
        '''

        def getFeedInfo(x):
            feed = FeedInfo()
            feed.market_business_id = x
            feed.market_shop_id = x
            feed.market_feed_id = x
            feed.direct_feed_id = x
            feed.filter_id = x

            return feed

        '''
        Проверяем для случая, когда из кэша пришло больше 5 feedInfo и в white-листе тоже больше 5 feedId
        В таком случаем будем использовать 5 filterId и 5 feedId
        '''
        cacheCgis = ""
        for value in range(100, 106):
            info = getFeedInfo(value)
            cacheCgis += "&cache-feed-info=" + base64.b64encode(info.SerializeToString())

        # проверяем, что офферы Директа из магазина, отключенного динамиком будут показаны на выдаче
        self.dynamic.market_dynamic.disabled_cpc_shops += [DynamicShop(600)]
        self.dynamic.market_dynamic.disabled_cpa_shops += [DynamicShop(600)]

        request = (
            'place=parallel&text=offer&debug=0&rids=213&rearr-factors=market_enable_direct_offers_wizard=1'
            + cacheCgis
            + '&rearr-factors=market_direct_feed_white_list=200:213,201:213,202:213,203:213,204:213,205:213'
        )
        response = self.report.request_bs(request)
        self.assertFragmentIn(response, "Iphone offer with filter 100")
        self.assertFragmentIn(response, "Iphone offer with filter 101")
        self.assertFragmentIn(response, "Iphone offer with filter 102")
        self.assertFragmentIn(response, "Iphone offer with filter 103")
        self.assertFragmentIn(response, "Iphone offer with filter 104")
        self.assertFragmentNotIn(response, "Iphone offer with filter 105")  # нету
        self.assertFragmentIn(response, "Iphone offer with feed 200")
        self.assertFragmentIn(response, "Iphone offer with feed 201")
        self.assertFragmentIn(response, "Iphone offer with feed 202")
        self.assertFragmentIn(response, "Iphone offer with feed 203")
        self.assertFragmentIn(response, "Iphone offer with feed 204")
        self.assertFragmentNotIn(response, "Iphone offer with feed 205")  # нету

        '''
        Проверяем для случая, когда из кэша пришло 4 feedInfo и в white-листе 6 feedId
        В таком случаем квота перетекает из filterId в feedId и мы будем использовать 4 filterId и 6 feedId
        '''
        cacheCgis = ""
        for value in range(100, 104):
            info = getFeedInfo(value)
            cacheCgis += "&cache-feed-info=" + base64.b64encode(info.SerializeToString())

        request = (
            'place=parallel&text=offer&debug=0&rids=213&rearr-factors=market_enable_direct_offers_wizard=1'
            + cacheCgis
            + '&rearr-factors=market_direct_feed_white_list=200:213,201:213,202:213,203:213,204:213,205:213'
        )
        response = self.report.request_bs(request)
        self.assertFragmentIn(response, "Iphone offer with filter 100")
        self.assertFragmentIn(response, "Iphone offer with filter 101")
        self.assertFragmentIn(response, "Iphone offer with filter 102")
        self.assertFragmentIn(response, "Iphone offer with filter 103")
        self.assertFragmentNotIn(response, "Iphone offer with filter 104")  # нету
        self.assertFragmentIn(response, "Iphone offer with feed 200")
        self.assertFragmentIn(response, "Iphone offer with feed 201")
        self.assertFragmentIn(response, "Iphone offer with feed 202")
        self.assertFragmentIn(response, "Iphone offer with feed 203")
        self.assertFragmentIn(response, "Iphone offer with feed 204")
        self.assertFragmentIn(response, "Iphone offer with feed 205")

        '''
        Проверяем для случая, когда из кэша пришло 6 feedInfo и в white-листе 4 feedId
        В таком случаем квота перетекает из feedId в filterId и мы будем использовать 6 filterId и 4 feedId
        '''
        cacheCgis = ""
        for value in range(100, 106):
            info = getFeedInfo(value)
            cacheCgis += "&cache-feed-info=" + base64.b64encode(info.SerializeToString())

        request = (
            'place=parallel&text=offer&debug=0&rids=213&rearr-factors=market_enable_direct_offers_wizard=1'
            + cacheCgis
            + '&rearr-factors=market_direct_feed_white_list=200:213,201:213,202:213,203:213'
        )
        response = self.report.request_bs(request)
        self.assertFragmentIn(response, "Iphone offer with filter 100")
        self.assertFragmentIn(response, "Iphone offer with filter 101")
        self.assertFragmentIn(response, "Iphone offer with filter 102")
        self.assertFragmentIn(response, "Iphone offer with filter 103")
        self.assertFragmentIn(response, "Iphone offer with filter 104")
        self.assertFragmentIn(response, "Iphone offer with filter 105")
        self.assertFragmentIn(response, "Iphone offer with feed 200")
        self.assertFragmentIn(response, "Iphone offer with feed 201")
        self.assertFragmentIn(response, "Iphone offer with feed 202")
        self.assertFragmentIn(response, "Iphone offer with feed 203")
        self.assertFragmentNotIn(response, "Iphone offer with feed 204")  # нету

        '''
        Проверяем для случая, когда из кэша пришло 3 feedInfo и в white-листе 3 feedId
        В таком случаем мы будем использовать 3 filterId и 3 feedId
        '''
        cacheCgis = ""
        for value in range(100, 103):
            info = getFeedInfo(value)
            cacheCgis += "&cache-feed-info=" + base64.b64encode(info.SerializeToString())

        request = (
            'place=parallel&text=offer&debug=0&rids=213&rearr-factors=market_enable_direct_offers_wizard=1'
            + cacheCgis
            + '&rearr-factors=market_direct_feed_white_list=200:213,201:213,202:213'
        )
        response = self.report.request_bs(request)
        self.assertFragmentIn(response, "Iphone offer with filter 100")
        self.assertFragmentIn(response, "Iphone offer with filter 101")
        self.assertFragmentIn(response, "Iphone offer with filter 102")
        self.assertFragmentNotIn(response, "Iphone offer with filter 103")  # нету
        self.assertFragmentIn(response, "Iphone offer with feed 200")
        self.assertFragmentIn(response, "Iphone offer with feed 201")
        self.assertFragmentIn(response, "Iphone offer with feed 202")
        self.assertFragmentNotIn(response, "Iphone offer with feed 203")  # нету

        '''
        Проверим feed_info для ответа по кэшу и для ответа по белому списку
        '''
        # ответ из кэша
        self.assertFragmentIn(
            response,
            {
                "feed_info": {
                    "direct_feed_id": 100,
                    "filter_id": 100,
                    "market_business_id": 100,
                    "market_shop_id": 100,
                },
                "is_white_list": NoKey("is_white_list"),
            },
        )
        # ответ из белого списка
        self.assertFragmentIn(
            response,
            {
                "feed_info": {"direct_feed_id": 200, "market_business_id": 700, "market_shop_id": 700},
                "is_white_list": "1",
            },
        )
        '''
        Проверим, что при 2х фильтрах с одного магазина, получим 2 галерейки
        '''
        # максимум 2 фильтра на 1 фид, остальные выкинем
        infos = [getFeedInfo(666) for i in range(3)]
        infos[1].filter_id = 777
        infos[2].filter_id = 888
        serialized_infos = [base64.b64encode(inf.SerializeToString()) for inf in infos]
        cacheCgis = "&cache-feed-info=" + ','.join(serialized_infos)

        request = (
            'place=parallel&text=offer&debug=0&rids=213&rearr-factors=market_enable_direct_offers_wizard=1' + cacheCgis
        )
        response = self.report.request_bs(request)
        self.assertFragmentIn(response, {"feed_info": {"filter_id": 666}})
        self.assertFragmentIn(response, {"feed_info": {"filter_id": 777}})
        self.assertFragmentNotIn(response, {"feed_info": {"filter_id": 888}})

    @classmethod
    def prepare_ignore_region(cls):

        # Магазин без региона
        cls.index.shops += [
            Shop(fesh=350),
        ]

        cls.index.offers += [
            Offer(title='free offer 1', is_direct=True, fesh=350),
            Offer(title='free offer 2', is_direct=True, fesh=350),
            Offer(title='free offer 3', is_direct=True, fesh=350),
            Offer(title='free offer 4', is_direct=True, fesh=350),
            Offer(title='free offer 5', is_direct=True, fesh=350),
        ]

    def test_ignore_region(self):
        '''Проверяем, что под флагом market_direct_offers_ignore_region находятся оффера из магазина, у которого нету региона и direct_offers колдунщик строится
        https://st.yandex-team.ru/MARKETOUT-40239
        '''

        # проверяем, что без флага market_direct_offers_ignore_region оффера не находятся и direct_offers колдунщик не строится
        request = 'place=parallel&text=free&rearr-factors=market_enable_direct_offers_wizard=1&rids=213'
        response = self.report.request_bs(request)
        self.assertFragmentNotIn(response, {"market_direct_offers_wizard_0": NotEmptyList()})

        # проверяем, что под флагом market_direct_offers_ignore_region оффера находятся и direct_offers колдунщик строится
        request = 'place=parallel&text=free&rearr-factors=market_enable_direct_offers_wizard=1&rids=213&rearr-factors=market_direct_offers_ignore_region=1'
        response = self.report.request_bs(request)
        self.assertFragmentIn(response, {"market_direct_offers_wizard_0": NotEmptyList()})


if __name__ == '__main__':
    main()
