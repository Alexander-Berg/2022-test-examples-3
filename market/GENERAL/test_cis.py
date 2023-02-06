#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, Currency, MarketSku, Model, Shop, Tax
from core.testcase import TestCase, main
from core.matcher import Absent, EmptyList


MARKET_CATEGORY_ID = 1

SUPPLIER_SHOP_ID = 1
VIRTUAL_SHOP_ID = 2

SUPPLIER_FEED_ID = 1
VIRTUAL_SHOP_FEED_ID = 2

# Models
MARKET_MODEL = Model(hyperid=11, title="Обычная модель", hid=MARKET_CATEGORY_ID)

ANOTHER_MARKET_MODEL = Model(hyperid=12, title="Обычная модель (есть NO_CIS)", hid=MARKET_CATEGORY_ID)

# Blue offers
COMMON_OFFER = BlueOffer(
    price=10000,
    offerid='Shop1_sku01',
    waremd5='Sku01Price10k-vm1Goleg',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    title='Просто оффер',
)

CIS_OFFER = BlueOffer(
    price=12000,
    offerid='Shop1_sku02',
    waremd5='Sku03Price12k-vm1Goleg',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    title='Оффер CIS',
)

NO_CIS_OFFER = BlueOffer(
    price=11000,
    offerid='Shop1_sku02',
    waremd5='Sku02Price11k-vm1Goleg',
    supplier_id=SUPPLIER_SHOP_ID,
    feedid=SUPPLIER_FEED_ID,
    is_no_cis=True,
    title='Оффер без CIS',
)

MARKET_SKU = MarketSku(
    title="Тестовый синий MSKU",
    hid=MARKET_CATEGORY_ID,
    hyperid=MARKET_MODEL.hyper,
    sku=1,
    blue_offers=[
        COMMON_OFFER,
    ],
)

ANOTHER_MARKET_SKU = MarketSku(
    title="Тестовый синий MSKU (NO_CIS)",
    hid=MARKET_CATEGORY_ID,
    hyperid=ANOTHER_MARKET_MODEL.hyper,
    sku=2,
    blue_offers=[NO_CIS_OFFER, CIS_OFFER],
)

REQUEST_BASE = (
    'place=prime' '&use-default-offers=1' '&onstock=1' '&allow-collapsing=1' '&hid={hid}' '&pp=18' '&rgb={color}'
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'blue-main'
        cls.settings.lms_autogenerate = True

    @classmethod
    def prepare_shops(cls):
        cls.index.shops += [
            Shop(
                fesh=SUPPLIER_SHOP_ID,
                datafeed_id=SUPPLIER_FEED_ID,
                name="Тестовый поставщик",
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=VIRTUAL_SHOP_ID,
                datafeed_id=VIRTUAL_SHOP_FEED_ID,
                name="Тестовый виртуальный поставщик",
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
        ]

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [MARKET_SKU, ANOTHER_MARKET_SKU]

    @staticmethod
    def __get_response_fragment(model_offer_list=None):
        return (
            {'results': EmptyList()}
            if model_offer_list is None
            else {
                'results': [
                    {
                        'entity': 'product',
                        'id': model.hyper,
                        'offers': {
                            'items': [{'entity': 'offer', 'wareId': offer.waremd5}] if offer is not None else Absent()
                        },
                    }
                    for model, offer in model_offer_list
                ]
            }
        )

    def test_no_cis(self):
        """
        Проверяем, что оффер с NO_CIS скрыт по-умоланию,
        и показывается под флагами client=partnerinterface&pi-from=sandbox
        Удостоверяемся, что на параллельном оффер так же скрывается
        """
        for color in ['blue', 'white']:
            request = REQUEST_BASE.format(hid=MARKET_CATEGORY_ID, color=color)
            # проверяем, что в ответе есть офферы с CIS и обе модели
            model_offer_list = [(MARKET_MODEL, COMMON_OFFER), (ANOTHER_MARKET_MODEL, CIS_OFFER)]

            response = self.report.request_json(request)
            self.assertFragmentIn(response, T.__get_response_fragment(model_offer_list), allow_different_len=False)

            # проверяем, что в ответе нет оффера без CIS
            model_offer_list = [(ANOTHER_MARKET_MODEL, NO_CIS_OFFER)]

            self.assertFragmentNotIn(response, T.__get_response_fragment(model_offer_list))

            # проверяем, что фильтр по CIS отключается
            model_offer_list = [(MARKET_MODEL, COMMON_OFFER), (ANOTHER_MARKET_MODEL, NO_CIS_OFFER)]
            response = self.report.request_json(request + '&client=partnerinterface&pi-from=sandbox')
            self.assertFragmentIn(response, T.__get_response_fragment(model_offer_list), allow_different_len=False)

        response = self.report.request_bs('place=parallel&text=оффер+без+cis')
        # проверяем что parallel не выдаёт оффер без CIS
        self.assertFragmentNotIn(response, {'offers': [{'ware_md5': 'Sku02Price11k-vm1Goleg'}]})


if __name__ == '__main__':
    main()
