#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa
import test_bnpl_conditions

from core.testcase import main

from core.types import BlueOffer, BnplConditionsSettings, MarketSku, Payment, PaymentRegionalGroup, ShopPaymentMethods

from test_bnpl_conditions import (
    DEFAULT_DELIVERY_BUCKET_ID,
    PRIME_REQUEST,
    MSK_RIDS,
    SKU_OFFERS_REQUEST,
    VIRTUAL_SHOP_ID,
)


ALLOWED_OFFER_DROPSHIP_NOT_IN_WHITELIST_WHEN_WHITELIST_MODE_DISABLED = BlueOffer(
    price=11100, offerid='Shop4_sku2', waremd5='SkuDROPSHIPAWDvm1Goleg', feedid=4
)

NOT_ALLOWED_OFFER_DROPSHIP_IN_BLACKLIST = BlueOffer(
    price=11100, offerid='Shop4_sku2', waremd5='SkuDROPSHIPNABvm1Goleg', feedid=4
)

ALLOWED_MSKU_DROPSHIP_NOT_IN_WHITELIST_WHEN_WHITELIST_MODE_DISABLED = MarketSku(
    title="ALLOWED MSKU DROPSHIP NOT IN WHITELIST WHEN WHITELIST MODE DISABLED",
    hid=1000,
    hyperid=1000,
    sku='1000',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[ALLOWED_OFFER_DROPSHIP_NOT_IN_WHITELIST_WHEN_WHITELIST_MODE_DISABLED],
)

NOT_ALLOWED_MSKU_DROPSHIP_IN_BLACKLIST = MarketSku(
    title="NOT ALLOWED MSKU DROPSHIP IN BLACKLIST",
    hid=1001,
    hyperid=1001,
    sku='1001',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[NOT_ALLOWED_OFFER_DROPSHIP_IN_BLACKLIST],
)


class T(test_bnpl_conditions.T):
    @classmethod
    def prepare(cls):
        cls.index.bnpl_conditions.settings = BnplConditionsSettings(enable_hids_whitelist=False)
        cls.index.bnpl_conditions.black_hids += [1001]

        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=VIRTUAL_SHOP_ID,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[Payment.PT_PREPAYMENT_CARD],
                    ),
                ],
            ),
        ]

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [
            ALLOWED_MSKU_DROPSHIP_NOT_IN_WHITELIST_WHEN_WHITELIST_MODE_DISABLED,
            NOT_ALLOWED_MSKU_DROPSHIP_IN_BLACKLIST,
        ]

    def test_disabled_white_hids(self):
        '''
        В конфигурации рассрочки отключаем требование наличия категории в белом списке
        '''
        # Проверяем place=prime
        for offer, bnpl_allowed in [
            (ALLOWED_OFFER_DROPSHIP_NOT_IN_WHITELIST_WHEN_WHITELIST_MODE_DISABLED, True),
            (NOT_ALLOWED_OFFER_DROPSHIP_IN_BLACKLIST, False),
        ]:
            response = self.report.request_json(PRIME_REQUEST.format(rid=MSK_RIDS, waremd5=offer.waremd5))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {"entity": "offer", "wareId": offer.waremd5, "yandexBnplInfo": {"enabled": bnpl_allowed}}
                    ]
                },
            )
        # Проверяем place=sku_offers
        for msku, bnpl_allowed in [
            (ALLOWED_MSKU_DROPSHIP_NOT_IN_WHITELIST_WHEN_WHITELIST_MODE_DISABLED, True),
            (NOT_ALLOWED_MSKU_DROPSHIP_IN_BLACKLIST, False),
        ]:
            response = self.report.request_json(SKU_OFFERS_REQUEST.format(rid=MSK_RIDS, msku=msku.sku))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": msku.sku,
                            "offers": {"items": [{"yandexBnplInfo": {"enabled": bnpl_allowed}}]},
                        }
                    ]
                },
            )

    def test_bnpl_info(self):
        '''
        Тест не расчитан под конфиг с enable_hids_whitelist=False
        '''
        pass


if __name__ == '__main__':
    main()
