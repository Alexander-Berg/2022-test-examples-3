#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DeliveryBucket,
    DeliveryOption,
    MarketSku,
    Model,
    Offer,
    Payment,
    Region,
    RegionalDelivery,
    Shop,
)
from core.matcher import Contains, NoKey


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.regiontree += [
            Region(rid=213, name="Москва", region_type=Region.CITY),
            Region(rid=2, name="Питер", region_type=Region.CITY),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='ВиртуальныйМагазинНаБеру',
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=1111,
                datafeed_id=1111,
                priority_region=213,
                regions=[225],
                name="Беру!",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(fesh=2, name="БелыйМагазин", priority_region=213, regions=[225]),
            Shop(fesh=3, name="БелыйМагазинРазмещаюсьНаБеру", priority_region=213, regions=[225], cpa=Shop.CPA_REAL),
        ]

        cls.settings.lms_autogenerate = True

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=225,
                        options=[DeliveryOption(price=150, day_from=1, day_to=2)],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    ),
                ],
            )
        ]

        cls.index.models += [
            Model(title="Майский отпуск во время короновируса", hid=1, hyperid=1),
        ]

        cls.index.mskus += [
            MarketSku(
                title="Путевка на Дачу",
                descr="Прекрасная идея для майского отпуска",
                hyperid=1,
                hid=1,
                sku=1000,
                delivery_buckets=[1234],
                blue_offers=[BlueOffer(price=1300, feedid=1111, waremd5='mPlY7hu1A-H7qckFdOy9uA')],
            ),
        ]

        cls.index.offers += [
            # cpc-оффер
            Offer(
                fesh=2,
                hyperid=1,
                hid=1,
                title="Незабываемое путешествие к Холодильной Гренландии",
                waremd5='ElmBfP4iZLUuvaCuokXbYw',
                cpa=Offer.CPA_NO,
            ),
            # cpa-оффер
            Offer(
                fesh=3,
                hyperid=1,
                hid=1,
                sku=1001,
                title="Купание в ванне с ненастоящими фламинго",
                price=250,
                waremd5='LTMhuqFfD0-gwZKFmLYdDw',
                cpa=Offer.CPA_REAL,
            ),
        ]

    def test_beru_order_url_format(self):
        """Урл beruOrder имеет формат
        https://checkout.market.yandex.ru/checkout?cartItems=<ware_md5>:1:<feeshow>::true&cartItemsSchema=offerId:count:feeShow:bundleId:isPrimaryInBundle&lr=<rids>
        (хост чекаута может меняться в зависимости от rgb=blue или задаваться параметром)
        параметр beru-order-params позволяет задать дополнительные cgi параметры к урлу чекаутера

        """

        response = self.report.request_json(
            "place=productoffers&hyperid=1&rids=213&pp=18&show-urls=external,beruOrder"
            "&beru-order-params=purchase-referrer%3Dberu_in_yamarket%26clid%3D123%26vid%3D456"
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "mPlY7hu1A-H7qckFdOy9uA",
                "cpa": "real",
                "urls": {
                    "beruOrder": Contains(
                        "/redir/dtype=cpa/",
                        "data=url=https%3A%2F%2Fcheckout.market.yandex.ru%2Fcheckout%3FcartItems%3DmPlY7hu1A-H7qckFdOy9uA",
                        "lr%3D213",
                        "purchase-referrer%3Dberu_in_yamarket",
                        "clid%3D123",
                        "vid%3D456",
                    )
                },
            },
        )

        response = self.report.request_json("place=productoffers&hyperid=1&rids=213&pp=18&show-urls=external,beruOrder")
        self.assertFragmentIn(
            response,
            {
                "wareId": "mPlY7hu1A-H7qckFdOy9uA",
                "cpa": "real",
                "offerColor": "blue",
                "urls": {
                    "beruOrder": Contains(
                        "/redir/dtype=cpa/",
                        "data=url=https%3A%2F%2Fcheckout.market.yandex.ru%2Fcheckout%3FcartItems%3DmPlY7hu1A-H7qckFdOy9uA",
                        "lr%3D213",
                    )
                },
            },
        )

        self.assertFragmentIn(
            response,
            {
                "wareId": "LTMhuqFfD0-gwZKFmLYdDw",
                "cpa": "real",
                "offerColor": "white",
                "urls": {
                    "beruOrder": Contains(
                        "/redir/dtype=cpa/",
                        "data=url=https%3A%2F%2Fcheckout.market.yandex.ru%2Fcheckout%3FcartItems%3DLTMhuqFfD0-gwZKFmLYdDw",
                        "lr%3D213",
                    )
                },
            },
        )

        # для белого cpc оффера в beruOrder отсутствует
        self.assertFragmentIn(
            response,
            {
                "wareId": "ElmBfP4iZLUuvaCuokXbYw",
                "cpa": NoKey("cpa"),
                "offerColor": "white",
                "urls": {"beruOrder": NoKey("beruOrder")},
            },
        )


if __name__ == '__main__':
    main()
