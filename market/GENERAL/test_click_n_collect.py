#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    CategoryRestriction,
    Currency,
    ExchangeRate,
    HyperCategory,
    Model,
    Offer,
    RegionalRestriction,
    Shop,
    WhiteSupplier,
)
from core.types.click_n_collect import Model as CncModel, Offer as CncOffer
from core.testcase import TestCase, main
from core.matcher import Contains, NotEmpty, NoKey, ElementCount
from market.click_n_collect.proto.get_offers_pb2 import TResponse as TGetOffersResponse
from market.click_n_collect.mock.goods.proto.get_stock_pb2 import TResponse


def get_outlet(goods_id='goods_model', outlet_id="outlet1", remain_qty=1000, proto=False):
    if not proto:
        return {
            "remainQty": remain_qty,
            "goodsId": goods_id,
            "price": 5001,
            "location": {
                "identification": {
                    "externalId": "7777777",
                    "name": "Точка C&C №3 для тестового мерчанта на проде",
                    "id": outlet_id,
                },
                "location": {"geo": {"lon": "37.9459172", "lat": "55.796402"}},
                "owner": {"id": "12"},
                "legalInfo": {
                    "plain": "qqq",
                },
                "isActive": True,
                "label": {
                    "caption": "Тестовая точка Goods.ru №3",
                    "imageURL": "www.Goods.ru",
                    "contacts": "+75992342323",
                    "address": "г Москва,  к 2",
                    "schedule": "ежедневно с 5 до 8",
                },
            },
        }
    else:
        return TResponse.TOutlet(
            RemainQty=remain_qty,
            GoodsId=goods_id,
            Price=5001,
            Location=TResponse.TOutlet.TLocation(
                Identification=TResponse.TOutlet.TLocation.TIdentification(
                    ExternalId="7777777", Name="Точка C&C №3 для тестового мерчанта на проде", Id=outlet_id
                ),
                Location=TResponse.TOutlet.TLocation.TGeoLocation(
                    Geo=TResponse.TOutlet.TLocation.TGeoLocation.TGeo(Lon="37.9459172", Lat="55.796402")
                ),
                Label=TResponse.TOutlet.TLocation.TLabel(
                    Caption="Тестовая точка Goods.ru №3",
                    Contacts="+75992342323",
                    Address="г Москва,  к 2",
                    Schedule="ежедневно с 5 до 8",
                ),
                Owner=TResponse.TOutlet.TLocation.TOwner(Id="12"),
                LegalInfo=TResponse.TOutlet.TLocation.TLegalInfo(Plain="qqq"),
                IsActive=True,
            ),
        )


def updated(dct, upd):
    dct.update(upd)
    return dct


def get_OutletsIds(outlet_ids, remains):
    result = TGetOffersResponse.TOutletsInfo()

    for outlet_id, remain_qty in zip(outlet_ids, remains):
        result.OutletIds[outlet_id] = remain_qty

    return result


ourResp = {
    "data": [
        updated(
            get_outlet(goods_id='goods_model', outlet_id="1"),
            {
                "location": {
                    "legalInfo": {"ogrn": "1238", "jur_address": "address2", "jur_name": "name2"},
                    "show_uid": "04884192001117778888800004",
                    "feed_id": "6155",
                    "offer_id": "3333",
                    "market_price": 100,
                    "fake_url": Contains("shop_id=627953"),
                }
            },
        ),
        updated(
            get_outlet(goods_id='goods_model2', outlet_id="3"),
            {
                "location": {
                    "legalInfo": {"ogrn": "1238", "jur_address": "address2", "jur_name": "name2"},
                    "show_uid": "04884192001117778888800002",
                    "feed_id": "6155",
                    "offer_id": "0000",
                    "market_price": 100,
                    "fake_url": Contains("shop_id=627953"),
                }
            },
        ),
        updated(
            get_outlet(goods_id='goods_model', outlet_id="2"),
            {
                "location": {
                    "legalInfo": {"ogrn": "1235", "jur_address": "address1", "jur_name": "name1"},
                    "show_uid": "04884192001117778888800001",
                    "feed_id": "6155",
                    "offer_id": "1111",
                    "market_price": 100,
                    "fake_url": Contains("shop_id=627953"),
                }
            },
        ),
    ],
    "success": True,
}


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.disable_randx_randomize()
        cls.index.fixed_index_generation = '19700101_0300'

        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        cls.index.currencies += [Currency(name=Currency.UE, exchange_rates=[ExchangeRate(fr=Currency.RUR, rate=0.5)])]

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='medicine',
                hids=[905901],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=False,
                        display_only_matched_offers=False,
                        delivery=True,
                        rids=[213],
                    ),
                ],
            )
        ]
        cls.index.hypertree += [
            HyperCategory(hid=905901, name='Лекарства'),
        ]

        # MARKETOUT-30771
        cls.index.offers += [
            Offer(
                title='nakovalnya #1',
                hyperid=17003,
                hid=5557,
                fesh=627953,
                click_n_collect_id='goods_model',
                mp_supl_ogrn="1235",
                offerid=1111,
                feedid=6155,
                randx=4,
            ),
            Offer(
                title='nakovalnya #2',
                hyperid=17003,
                hid=5557,
                fesh=627953,
                click_n_collect_id='goods_model',
                mp_supl_ogrn="1238",
                offerid=2222,
                feedid=6155,
                randx=2,
            ),
            Offer(
                title='nakovalnya #3',
                hyperid=17003,
                hid=5557,
                fesh=627953,
                click_n_collect_id='goods_model2',
                mp_supl_ogrn="1238",
                offerid="0000",
                feedid=6155,
                randx=3,
            ),
            Offer(
                title='nakovalnya #2',
                hyperid=17003,
                hid=5557,
                fesh=627953,
                click_n_collect_id='goods_model',
                mp_supl_ogrn="1238",
                offerid=3333,
                feedid=6155,
                randx=2,
            ),
        ]

        cls.index.click_n_collect_model_info += [
            CncModel(
                model_id=17003,
                offers=[
                    CncOffer(click_n_collect_id='goods_model2', feed_id=6155, offer_id='0000', outlets=[3]),
                    CncOffer(click_n_collect_id='goods_model', feed_id=6155, offer_id='1111', outlets=[2]),
                    CncOffer(click_n_collect_id='goods_model', feed_id=6155, offer_id='2222', outlets=[1]),
                    CncOffer(click_n_collect_id='goods_model', feed_id=6155, offer_id='3333', outlets=[1]),
                ],
            ),
            CncModel(
                model_id=170071,
                offers=[CncOffer(click_n_collect_id='goods_model1', feed_id=1, offer_id='1', outlets=[1])],
            ),
            CncModel(
                model_id=17008,
                offers=[CncOffer(click_n_collect_id='goods_model2', feed_id=1, offer_id='1', outlets=[4])],
            ),
        ]

        cls.index.white_suppliers += [
            WhiteSupplier(ogrn="1235", jur_name="name1", jur_address="address1"),
            WhiteSupplier(ogrn="1238", jur_name="name2", jur_address="address2"),
            WhiteSupplier(ogrn="125100", jur_name="name3", jur_address="address3"),
        ]

        cls.index.offers += [
            Offer(
                title='nakovalnya #3',
                hyperid=17005,
                hid=5559,
                fesh=627953,
                click_n_collect_id='goods_model',
                alcohol=True,
            ),
            Offer(title='nakovalnya #4', hyperid=17006, hid=905901, fesh=627953, click_n_collect_id='goods_model'),
        ]

        cls.index.shops += [
            Shop(
                fesh=627953,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
            )
        ]

        goodsRespProto = TGetOffersResponse(
            Success=True,
            Data=[
                get_outlet(goods_id='goods_model', outlet_id="1", proto=True),
                get_outlet(goods_id='goods_model', outlet_id="2", proto=True),
                get_outlet(goods_id='goods_model2', outlet_id="3", proto=True),
            ],
        )

        cls.click_n_collect.on_request(
            clickNCollectIds=['goods_model', 'goods_model2'],
            userInfo={'ip': '127.0.0.1'},
        ).respond(goodsRespProto.SerializeToString())
        cls.click_n_collect.on_request(
            clickNCollectIds=['goods_model2', 'goods_model'],
            userInfo={'ip': '127.0.0.1'},
        ).respond(goodsRespProto.SerializeToString())

        userInfo = {'ip': '127.0.0.1', 'yandexUid': '1', 'yandexGid': 2, 'yp': 'iamyp'}

        goodsRespWithUser = goodsRespProto
        goodsRespWithUser.User.Location.Latitude = 30
        goodsRespWithUser.User.Location.Longitude = 40

        cls.click_n_collect.on_request(clickNCollectIds=['goods_model', 'goods_model2'], userInfo=userInfo).respond(
            goodsRespWithUser.SerializeToString()
        )

    def test_click_n_collect(self):
        headers = {'X-Market-Req-ID': "1"}
        req = 'place=productoffers&hyperid=17003&rearr-factors=market_click_and_collect_mode=1;drop_goods_offers=0&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0'
        response = self.report.request_json(req, headers=headers)
        self.assertFragmentIn(
            response,
            {
                "clickNCollectInfo": ourResp,
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(response, {'entity': 'offer'})
        req = 'place=productoffers&hyperid=17003&rearr-factors=market_click_and_collect_mode=1'
        response = self.report.request_json(req, headers=headers)
        self.assertFragmentNotIn(response, {"clickNCollectInfo": {}})
        self.assertFragmentNotIn(response, {'entity': 'offer'})

        req = 'place=productoffers&hyperid=17003'
        response = self.report.request_json(req, headers=headers)
        self.assertFragmentNotIn(response, {"clickNCollectInfo": {}})
        self.assertFragmentNotIn(response, {'entity': 'offer'})

        # alco
        req = 'place=productoffers&hyperid=17005&rearr-factors=market_click_and_collect_mode=2;drop_goods_offers=0&show-alcohol=1'
        response = self.report.request_json(req, headers=headers)
        self.assertFragmentNotIn(response, {"clickNCollectInfo": {}})
        self.assertFragmentNotIn(response, {'entity': 'offer'})

        # drugs
        req = 'place=productoffers&hyperid=17006&rearr-factors=market_click_and_collect_mode=2;drop_goods_offers=0&rids=213'
        response = self.report.request_json(req, headers=headers)
        self.assertFragmentNotIn(response, {"clickNCollectInfo": {}})
        self.assertFragmentNotIn(response, {'entity': 'offer'})

    @classmethod
    def prepare_click_n_collect_on_prime(cls):
        cls.index.hypertree += [
            HyperCategory(hid=5560, name='Anvils'),
            HyperCategory(hid=5561, name='Furnaces'),
        ]

        cls.index.models += [
            Model(hyperid=17007, hid=5560, title='Anvil No. 3.5 with click-n-collect'),
            Model(hyperid=170071, hid=5560, title='Anvil No. 3.6 with click-n-collect'),
            Model(hyperid=17008, hid=5560, title='Anvil No. 3.5 out of stock'),
            Model(hyperid=17009, hid=5560, title='Anvil No. 3.5'),
            Model(hyperid=17010, hid=5561, title='Not furnaces No. 1'),
        ]

        cls.index.offers += [
            Offer(title='Anvil No. 3.5 with click-n-collect offer', hyperid=17007, click_n_collect_id='goods_model1'),
            Offer(title='Anvil No. 3.5 out of stock offer', hyperid=17008, click_n_collect_id='goods_model2'),
            Offer(title='Anvil No. 3.5 offer', hyperid=17009),
        ]

        cls.index.click_n_collect_model_info += [
            CncModel(
                model_id=17007,
                offers=[CncOffer(click_n_collect_id='goods_model1', feed_id=1, offer_id='1', outlets=[1, 2])],
            ),
            CncModel(
                model_id=170071,
                offers=[CncOffer(click_n_collect_id='goods_model1', feed_id=1, offer_id='1', outlets=[1])],
            ),
            CncModel(
                model_id=17008,
                offers=[CncOffer(click_n_collect_id='goods_model2', feed_id=1, offer_id='1', outlets=[4])],
            ),
        ]

        goodsRespProto = TGetOffersResponse(
            Success=True,
            Data=[
                get_outlet(goods_id='goods_model1', outlet_id="1", remain_qty=10, proto=True),
                get_outlet(goods_id='goods_model1', outlet_id="2", remain_qty=100, proto=True),
                get_outlet(
                    goods_id='goods_model1', outlet_id="3", remain_qty=100, proto=True
                ),  # this outlet will not match since it is not in click_n_collect_model_info
                get_outlet(goods_id='goods_model2', outlet_id="4", remain_qty=0, proto=True),
            ],
        )
        cls.click_n_collect.on_request(
            clickNCollectIds=['goods_model2', 'goods_model1'],
        ).respond(goodsRespProto.SerializeToString())
        cls.click_n_collect.on_request(
            clickNCollectIds=['goods_model1', 'goods_model2'],
        ).respond(goodsRespProto.SerializeToString())

    @classmethod
    def prepare_click_n_collect_on_prime_with_light_answer(cls):
        cls.index.hypertree += [
            HyperCategory(hid=5590, name='Workbenches'),
            HyperCategory(hid=5591, name='Tables'),
        ]

        cls.index.models += [
            Model(hyperid=19007, hid=5590, title='Workbench No. 3.5 with click-n-collect'),
            Model(hyperid=190071, hid=5590, title='Workbench No. 3.6 with click-n-collect'),
            Model(hyperid=19008, hid=5590, title='Workbench No. 3.5 out of stock'),
            Model(hyperid=19009, hid=5590, title='Workbench No. 3.5'),
            Model(hyperid=19010, hid=5591, title='Not tables No. 1'),
        ]

        cls.index.offers += [
            Offer(
                title='Workbench No. 3.5 with click-n-collect offer',
                hyperid=19007,
                click_n_collect_id='light_goods_model1',
            ),
            Offer(title='Workbench No. 3.5 out of stock offer', hyperid=19008, click_n_collect_id='light_goods_model2'),
            Offer(title='Workbench No. 3.5 offer', hyperid=19009),
        ]

        cls.index.click_n_collect_model_info += [
            CncModel(
                model_id=19007,
                offers=[CncOffer(click_n_collect_id='light_goods_model1', feed_id=1, offer_id='1', outlets=[1, 2])],
            ),
            CncModel(
                model_id=190071,
                offers=[CncOffer(click_n_collect_id='light_goods_model1', feed_id=1, offer_id='1', outlets=[1])],
            ),
            CncModel(
                model_id=19008,
                offers=[CncOffer(click_n_collect_id='light_goods_model2', feed_id=1, offer_id='1', outlets=[2, 3, 4])],
            ),
        ]

        goodsId2OutletsIds = {}
        goodsId2OutletsIds['light_goods_model1'] = get_OutletsIds(outlet_ids=['1', '2', '3'], remains=[10, 0, 100])
        goodsId2OutletsIds['light_goods_model2'] = get_OutletsIds(outlet_ids=['3', '4'], remains=[20, 200])

        lightGoodsRespProto = TGetOffersResponse(Success=True, GoodsId2OutletsIds=goodsId2OutletsIds)

        cls.click_n_collect.on_request(
            clickNCollectIds=['light_goods_model2', 'light_goods_model1'], isStatisticsRequest=True
        ).respond(lightGoodsRespProto.SerializeToString())
        cls.click_n_collect.on_request(
            clickNCollectIds=['light_goods_model1', 'light_goods_model2'], isStatisticsRequest=True
        ).respond(lightGoodsRespProto.SerializeToString())

    def test_click_n_collect_on_prime(self):
        """Check that 'clickNCollect' block appears on 'product' entity on prime output"""

        headers = {'X-Market-Req-ID': "1"}
        flag = '&rearr-factors=market_click_and_collect_on_prime=1'
        # with flag we have hasClickNCollect in output
        for request in ['&hid=5560', '&text=Anvil']:
            response = self.report.request_json('place=prime' + request + flag, headers=headers)
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': 17007,
                    'clickNCollect': {'outlets': 2},
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': 170071,
                    'clickNCollect': {'outlets': 1},
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': 17008,
                    'clickNCollect': NoKey('clickNCollect'),  # no stock - no block
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': 17009,
                    'clickNCollect': NoKey('clickNCollect'),  # no record in click_n_collect_info.mmap = no block
                },
            )

        # no flag - no output
        for request in ['&hid=5560', '&text=Anvil']:
            response = self.report.request_json('place=prime' + request, headers=headers)
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': 17007,
                    'clickNCollect': NoKey('clickNCollect'),
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': 17008,
                    'clickNCollect': NoKey('clickNCollect'),
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': 17009,
                    'clickNCollect': NoKey('clickNCollect'),
                },
            )

    def test_click_n_collect_on_prime_with_light_request(self):
        headers = {'X-Market-Req-ID': "1"}
        flag = '&rearr-factors=market_click_and_collect_on_prime=1;market_click_and_collect_use_fast_light_request=1'
        # with flag we have hasClickNCollect in output
        for request in ['&hid=5590', '&text=Workbench']:
            response = self.report.request_json('place=prime' + request + flag, headers=headers)
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': 19007,
                    'clickNCollect': {'outlets': 1},
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': 190071,
                    'clickNCollect': {'outlets': 1},
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': 19008,
                    'clickNCollect': {'outlets': 2},
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': 19009,
                    'clickNCollect': NoKey('clickNCollect'),  # no record in click_n_collect_info.mmap = no block
                },
            )

    @classmethod
    def prepare_click_n_collect_on_prime_region(cls):
        cls.index.hypertree += [HyperCategory(hid=9999, name='Plumbus')]
        cls.index.models += [Model(hyperid=9999, hid=9999, title='All-purpose Plumbus')]
        cls.index.click_n_collect_model_info += [
            CncModel(
                model_id=9999,
                offers=[CncOffer(click_n_collect_id='goods_model9999', feed_id=1, offer_id='9999', outlets=[1, 2])],
            ),
        ]
        cls.index.offers += [
            Offer(title='All-purpose Plumbus offer', hyperid=9999, click_n_collect_id='goods_model9999'),
        ]

        # when no region specified
        cls.click_n_collect.on_request(clickNCollectIds=['goods_model9999'],).respond(
            TGetOffersResponse(
                Success=True,
                Data=[
                    get_outlet(goods_id='goods_model9999', outlet_id="1", remain_qty=10, proto=True),
                    get_outlet(goods_id='goods_model9999', outlet_id="2", remain_qty=10, proto=True),
                ],
            ).SerializeToString()
        )

        # when requesting 213 region
        cls.click_n_collect.on_request(
            clickNCollectIds=['goods_model9999'], filters={'geo': {'includeIds': [213]}}
        ).respond(
            TGetOffersResponse(
                Success=True,
                Data=[
                    get_outlet(goods_id='goods_model9999', outlet_id="1", remain_qty=10, proto=True),
                ],
            ).SerializeToString()
        )

    def test_click_n_collect_on_prime_region(self):
        """Test that under market_click_and_collect_prime_region_filter flag we go to the click_n_collect back with
        geo filters"""
        headers = {'X-Market-Req-ID': "1"}

        # no filtering - we have 2 outlets
        flag = '&rearr-factors=market_click_and_collect_on_prime=1'
        response = self.report.request_json('place=prime&hid=9999' + flag, headers=headers)
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 9999,
                'clickNCollect': {'outlets': 2},
            },
        )

        # with filter - only 1
        flag = '&rearr-factors=market_click_and_collect_on_prime=1;market_click_and_collect_prime_region_filter=1'
        response = self.report.request_json('place=prime&rids=213&hid=9999' + flag, headers=headers)
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'id': 9999,
                'clickNCollect': {'outlets': 1},
            },
        )

    def test_click_n_collect_filter(self):
        headers = {'X-Market-Req-ID': "1"}
        base_request = 'place=prime&click-n-collect=1'
        flag = '&rearr-factors=market_click_and_collect_on_prime=1'

        for request in ['&hid=5560', '&text=Anvil']:
            # check that click-n-collect-models=1 filters out non click-n-collect offers
            response = self.report.request_json(base_request + request, headers=headers)
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': 17007,
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'id': 17008,
                },
            )
            self.assertFragmentNotIn(
                response,
                {
                    'entity': 'product',
                    'id': 17009,
                },
            )

            # check that filter block appears with flag
            response = self.report.request_json(base_request + request + flag, headers=headers)
            self.assertFragmentIn(
                response,
                {
                    'filters': [
                        {
                            'id': 'click-n-collect',
                            'type': 'boolean',
                            'values': [
                                {
                                    'checked': True,
                                    'found': 3,  # this filter counts only models, so we have only 3 models
                                    # that fit filters above
                                    'value': '1',
                                },
                                {
                                    'found': 2,  # in total with request above we can found 5 docs: 3 models & 2
                                    # offers which means that we have 2 docs without click-n-collect:
                                    # 2 offers
                                    'value': '0',
                                },
                            ],
                        }
                    ]
                },
                allow_different_len=True,
            )

            # and disappears without
            response = self.report.request_json(base_request + request, headers=headers)
            self.assertFragmentNotIn(
                response,
                {
                    'filters': [
                        {
                            'id': 'click-n-collect',
                        }
                    ]
                },
            )

        # check that if request don't find initially click-n-collect docs we will not show filter block
        response = self.report.request_json(base_request + '&hid=5561' + flag, headers=headers)
        self.assertFragmentNotIn(
            response,
            {
                'filters': [
                    {
                        'id': 'click-n-collect',
                    }
                ]
            },
        )

    def test_hide_parallel(self):
        response = self.report.request_bs('place=parallel&text=nakovalnya&rids=213')
        self.assertFragmentNotIn(response, {"market_offers_wizard": NotEmpty()})

        response = self.report.request_bs('place=parallel&text=nakovalnya&rids=213&rearr-factors=drop_goods_offers=0')
        self.assertFragmentIn(response, {"market_offers_wizard": NotEmpty()})

    def test_laas(self):
        headers = {'X-Market-Req-ID': "1"}
        req = 'place=productoffers&hyperid=17003&rearr-factors=market_click_and_collect_mode=1;drop_goods_offers=0&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0'

        response = self.report.request_json(req + '&ip=127.0.0.1&yandexuid=1&yandexgid=2&yp=iamyp', headers=headers)
        ourRespWithUser = ourResp
        ourRespWithUser['user'] = {'location': {'latitude': 30, 'longitude': 40}}
        self.assertFragmentIn(
            response,
            {
                "clickNCollectInfo": ourRespWithUser,
            },
            allow_different_len=False,
        )

    def test_click_n_collect_url(self):
        headers = {'X-Market-Req-ID': "1"}
        request = 'place=productoffers&hyperid=17003&rearr-factors=market_click_and_collect_mode=1;drop_goods_offers=0&show-urls=external&rearr-factors=use_offer_type_priority_as_main_factor_in_top=0'
        response = self.report.request_json(request, headers=headers)

        self.assertFragmentIn(
            response,
            {
                "clickNCollectInfo": ourResp,
            },
            allow_different_len=False,
        )

        # currencies = Currencies(self.index.currencies)
        # exchange_rate = currencies.get_exchange_rate('RUR', 'UE')
        # comission = 0.015
        # offer_price = 100
        # click_price = int(offer_price * exchange_rate * comission * 100)
        # питон и плюсы округляют в инт по-разному, поэтому в тесте зададим явно
        click_price = 4
        self.assertFragmentIn(
            response,
            {
                'fake_url': Contains('url=https%3A%2F%2Fgoods.ru'),
            },
        )
        self.assertFragmentIn(
            response,
            {
                'fake_url': Contains('cp=%s' % str(click_price)),
            },
        )
        self.assertFragmentIn(
            response,
            {
                'fake_url': Contains('shop_id=627953'),
            },
        )

        # проверим, что если передаем флажок market_click_and_collect_disable_click_on_order , то кнк клик-урлов нет
        request = 'place=productoffers&hyperid=17003&rearr-factors=market_click_and_collect_mode=1;drop_goods_offers=0;market_click_and_collect_disable_click_on_order=1&show-urls=external'
        response = self.report.request_json(request, headers=headers)

        self.assertFragmentNotIn(
            response,
            {
                'fake_url': Contains('url=https%3A%2F%2Fgoods.ru'),
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'fake_url': Contains('cp=%s' % str(click_price)),
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'fake_url': Contains('shop_id=627953'),
            },
        )
        self.assertFragmentIn(
            response,
            {
                'clickNCollectInfo': {
                    'data': ElementCount(3),
                }
            },
        )


if __name__ == '__main__':
    main()
