#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    ClickType,
    MnPlace,
    Model,
    Offer,
    Shop,
)
from core.types.vendor_reserve_prices import HidToRP
from core.testcase import TestCase, main
from core.matcher import Absent, ElementCount
from core.blender_bundles import create_blender_bundles, get_supported_incuts_cgi


class BlenderConstVendorIncut:
    BUNDLE = '''
{
    "incut_places": ["Top"],
    "incut_positions": [1],
    "incut_viewtypes": ["GalleryWithBanner", "VendorGallery"],
    "incut_ids": ["vendor_incut_with_banner", "vendor_incut"],
    "result_scores": [
        {
            "incut_place": "Top",
            "row_position": 1,
            "incut_viewtype": "GalleryWithBanner",
            "incut_id": "vendor_incut_with_banner",
            "score": 0.75
        },
        {
            "incut_place": "Top",
            "row_position": 1,
            "incut_viewtype": "VendorGallery",
            "incut_id": "vendor_incut",
            "score": 0.74
        }
    ],
    "calculator_type": "ConstPosition"
}
'''


class BlenderBundlesConfig:
    BUNDLES_CONFIG = """
{
    "INCLID_VENDOR_INCUT" : {
        "client == frontend && platform == desktop" : {
            "bundle_name": "const_vendor_incut.json"
        },
        "client == frontend && platform == touch" : {
            "bundle_name": "const_vendor_incut.json"
        }
    }
}
"""


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += [
            'market_vendor_incut_hide_undeliverable_models=0;'
            'market_vendor_incut_with_CPA_offers_only=0;'
            'market_vendor_incut_min_size=1;'
            'market_vendor_incut_reserve_price_disabled=0;'
        ]
        cls.settings.formulas_path = create_blender_bundles(
            cls.meta_paths.testroot,
            BlenderBundlesConfig.BUNDLES_CONFIG,
            {"const_vendor_incut.json": BlenderConstVendorIncut.BUNDLE},
        )

    @classmethod
    def prepare_reserve_price_works(cls):
        cls.index.shops += [
            Shop(fesh=10, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.models += [
            Model(hid=1, hyperid=1, ts=1, vendor_id=1),
            Model(hid=1, hyperid=2, ts=2, vbid=40, vendor_id=2, datasource_id=1),
            Model(hid=1, hyperid=3, ts=3, vbid=60, vendor_id=3, datasource_id=1),
            Model(hid=1, hyperid=4, ts=4, vbid=80, vendor_id=4, datasource_id=2),
        ]

        cls.index.offers += [
            Offer(
                hid=1,
                title='Nordic Carved Armor vesion {}'.format(i),
                waremd5='VkjX-0weqaaf1QfMrNfQl{}'.format(i),
                hyperid=i + 1,
                fesh=10,
                ts=100 + i,
                cpa=Offer.CPA_REAL,
            )
            for i in range(4)
        ]

        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.02)

        for i in range(4):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, i).respond(0.9 - 0.1 * i)

        cls.index.vendor_reserve_prices += [HidToRP(hid=1, rp=50)]

    def test_reserve_price_works(self):
        '''
        Проверяем, что две модельки отфильтровались на базовом так как ставка меньше рп
        '''
        response = self.report.request_json('place=vendor_incut&hid=1&pp=18&show-urls=productVendorBid&debug=1')

        self.assertFragmentIn(response, {"filters": {"VENDOR_BID_LESS_THAN_RESERVE_PRICE": 2}})
        self.assertFragmentIn(response, {"results": ElementCount(1)}, allow_different_len=False)

    @classmethod
    def prepare_autobroker_click_price_is_less_then_reserve_price(cls):
        cls.index.shops += [
            Shop(fesh=11, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.models += [
            Model(hid=2, hyperid=5, ts=5, vbid=200, vendor_id=5, datasource_id=3, title='Daedric Armor from  Sanguine'),
            Model(
                hid=2, hyperid=6, ts=6, vbid=40, vendor_id=5, datasource_id=3, title='Daedric Armor from  Sheogorath'
            ),
            Model(hid=2, hyperid=7, ts=7, vbid=60, vendor_id=6, datasource_id=4),
            Model(hid=2, hyperid=8, ts=8, vbid=60, vendor_id=6, datasource_id=4),
        ]

        cls.index.offers += [
            Offer(
                hid=2,
                title='Daedric Armor vesion {}'.format(i),
                waremd5='VkjX-0weKaaf1QfMrNfQl{}'.format(i),
                hyperid=i + 4,
                fesh=11,
                ts=110 + i,
                cpa=Offer.CPA_REAL,
            )
            for i in range(1, 5)
        ]

        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.02)

        cls.index.vendor_reserve_prices += [HidToRP(hid=2, rp=30)]

    def test_autobroker_click_price_is_less_then_reserve_price(self):
        '''
        Проверяем, что работает автоброкер в простом случае, когда выиграл вендор у которого одна цена клика больше РП а вторая меньше и подперта РП
        '''
        response = self.report.request_json('place=vendor_incut&hid=2&pp=18&show-urls=productVendorBid&debug=1')

        self.assertFragmentIn(response, {"filters": {"VENDOR_BID_LESS_THAN_RESERVE_PRICE": Absent()}})
        self.assertFragmentIn(response, {"results": ElementCount(2)})

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=200, vendor_price=100, brand_id=5, hyper_id=5
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=40, vendor_price=30, brand_id=5, hyper_id=6
        )

    @classmethod
    def prepare_autobroker_only_one_vendor(cls):
        cls.index.shops += [
            Shop(fesh=12, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.models += [
            Model(hid=3, hyperid=9, ts=9, vbid=200, vendor_id=7, datasource_id=4),
            Model(hid=3, hyperid=10, ts=10, vbid=40, vendor_id=7, datasource_id=4),
        ]

        cls.index.offers += [
            Offer(
                hid=3,
                title='Ebony Mail vesion {}'.format(i),
                waremd5='VkjX-0weAaaf1QfMrNfQl{}'.format(i),
                hyperid=i + 8,
                fesh=12,
                ts=120 + i,
                cpa=Offer.CPA_REAL,
            )
            for i in range(1, 3)
        ]

        cls.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.02)

        cls.index.vendor_reserve_prices += [HidToRP(hid=3, rp=30)]

    def test_autobroker_only_one_vendor(self):
        '''
        Проверяем, что если вендор всего один, то его ставки амнистируются до резерв прайс
        '''
        response = self.report.request_json('place=vendor_incut&hid=3&pp=18&show-urls=productVendorBid&debug=1')

        self.assertFragmentIn(response, {"filters": {"VENDOR_BID_LESS_THAN_RESERVE_PRICE": Absent()}})
        self.assertFragmentIn(response, {"results": ElementCount(2)}, allow_different_len=False)

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=200, vendor_price=30, brand_id=7, hyper_id=9
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=40, vendor_price=30, brand_id=7, hyper_id=10
        )

    @classmethod
    def prepare_reserve_price_disabled_by_rearr_flag(cls):
        cls.index.models += [
            Model(hid=4, hyperid=11, ts=11, vendor_id=8),
            Model(hid=4, hyperid=12, ts=12, vbid=40, vendor_id=9),
        ]

        cls.index.vendor_reserve_prices += [HidToRP(hid=4, rp=50)]

    def test_reserve_price_disabled_by_rearr_flag(self):
        '''
        Проверяем, что использование резерв прайс можно выключить с помощью реарр флага: не выкинулись модели и автоброкер амнистировал до мин бида!!!!
        '''

        response = self.report.request_json(
            'place=vendor_incut&hid=4&pp=18&rearr-factors=market_vendor_incut_reserve_price_disabled=1&show-urls=productVendorBid&debug=1'
        )

        ''' первое - модели не отфильтровались '''
        self.assertFragmentIn(response, {"filters": {"VENDOR_BID_LESS_THAN_RESERVE_PRICE": Absent()}})

        '''второе - автоброкер'''
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=40, vendor_price=1, brand_id=9, hyper_id=12
        )

    class CgiParams(dict):
        def raw(self, separator='&'):
            if len(self):
                return separator.join("{}={}".format(str(k), str(v)) for (k, v) in self.iteritems())
            return ""

    class RearrFlags(CgiParams):
        def __init__(self, *args, **kwargs):
            super(T.RearrFlags, self).__init__(*args, **kwargs)

        def raw(self):
            if len(self):
                return 'rearr-factors={}'.format(super(T.RearrFlags, self).raw(';'))
            return str()

    @staticmethod
    def create_request(parameters, rearr):
        return '{}{}'.format(parameters.raw(), '&{}'.format(rearr.raw()) if len(rearr) else '')

    def test_reserve_price_in_blender(self):
        '''
        Проверяем, что резерв прайсы работают из-под блендера, блендер тут не причем, но просто на всякий случай, вдруг в блендере какая черная магия творится
        '''
        params = self.CgiParams(
            {
                'place': 'blender',
                'use-default-offers': 1,
                'debug': 'da',
                'allow-collapsing': 1,
                'pp': 18,
                'show-urls': 'productVendorBid',
                'hid': 2,
                'client': 'frontend',
                'platform': 'desktop',
                'show-urls': 'productVendorBid',
                'supported-incuts': get_supported_incuts_cgi(),
            }
        )

        rearr_factors = self.RearrFlags(
            {
                'market_report_blender_vendor_incut_enable': 1,  # разрешение работы вендорской врезки
                'market_blender_use_bundles_config': 1,
                "market_blender_media_adv_incut_enabled": 0,
            }
        )

        response = self.report.request_json(self.create_request(params, rearr_factors))

        self.assertFragmentIn(
            response,
            {
                "incuts": {
                    "results": [
                        {
                            'incutId': 'vendor_incut',
                            "entity": "searchIncut",
                            "items": ElementCount(2),
                        },
                    ],
                },
            },
            preserve_order=True,
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=200, vendor_price=100, brand_id=5, hyper_id=5
        )
        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=40, vendor_price=30, brand_id=5, hyper_id=6
        )

    @classmethod
    def prepare_reserve_price_multiplier(cls):
        cls.index.shops += [
            Shop(fesh=12, priority_region=213, regions=[225], cpa=Shop.CPA_REAL, cpc=Shop.CPC_NO),
        ]

        cls.index.models += [
            Model(hid=5, hyperid=13, ts=13, vbid=800, vendor_id=10, datasource_id=5),
            Model(hid=5, hyperid=14, ts=14, vbid=400, vendor_id=10, datasource_id=5),
            Model(hid=5, hyperid=15, ts=15, vbid=200, vendor_id=11, datasource_id=6),
            Model(hid=5, hyperid=16, ts=16, vbid=100, vendor_id=11, datasource_id=6),
        ]

        cls.index.offers += [
            Offer(
                hid=5,
                title='Daedric Armor vesion {}'.format(i),
                waremd5='VkyX-0weKaaf1QfMrNfQl{}'.format(i),
                hyperid=i + 12,
                fesh=11,
                ts=120 + i,
                cpa=Offer.CPA_REAL,
            )
            for i in range(1, 5)
        ]

        cls.index.vendor_reserve_prices += [HidToRP(hid=5, rp=300)]

    def test_reserve_price_multiplier_is_1(self):
        '''
        Проверяем, что при дефолтном множителе выиграет вендор 10 с амнистией до резерв прайс, так как второй отфильтруется
        '''

        response = self.report.request_json(
            'place=vendor_incut&hid=5&pp=18&rearr-factors=&show-urls=productVendorBid&debug=1'
        )

        self.assertFragmentIn(response, {"filters": {"VENDOR_BID_LESS_THAN_RESERVE_PRICE": 2}})

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=300, brand_id=10, hyper_id=13
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=400, vendor_price=300, brand_id=10, hyper_id=14
        )

    def test_reserve_price_multiplier_is_05(self):
        '''
        Проверяем, что при множителе 0.5 выиграет вендор 10 с амнистией до резерв прайс == 150, так как 400 * (1/6) =67, 800 * (1/6) = 133 что меньше РП
        '''

        response = self.report.request_json(
            'place=vendor_incut&hid=5&pp=18&rearr-factors=market_vendor_incut_reserve_price_multiplier=0.5&show-urls=productVendorBid&debug=1'
        )

        self.assertFragmentIn(response, {"filters": {"VENDOR_BID_LESS_THAN_RESERVE_PRICE": 1}})

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=150, brand_id=10, hyper_id=13
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=400, vendor_price=150, brand_id=10, hyper_id=14
        )

    def test_reserve_price_multiplier_is_04(self):
        '''
        Проверяем, что при множителе 0.4 выиграет вендор 10
              с амнистией до резерв прайс == 120 для модели 13, так как 400 * (1/6) =67 что меньше РП,
            и с амнистией до              == 133 для модели 14, так как 800 * (1/6) = 133 что больше РП
        '''

        response = self.report.request_json(
            'place=vendor_incut&hid=5&pp=18&rearr-factors=market_vendor_incut_reserve_price_multiplier=0.4&show-urls=productVendorBid&debug=1'
        )

        self.assertFragmentIn(response, {"filters": {"VENDOR_BID_LESS_THAN_RESERVE_PRICE": 1}})

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=800, vendor_price=134, brand_id=10, hyper_id=13
        )

        self.click_log.expect(
            clicktype=ClickType.EXTERNAL, dtype='modelcard', vc_bid=400, vendor_price=120, brand_id=10, hyper_id=14
        )


if __name__ == '__main__':
    main()
