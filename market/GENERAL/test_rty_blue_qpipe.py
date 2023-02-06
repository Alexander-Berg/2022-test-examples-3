#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import NotEmpty
from core.testcase import (
    TestCase,
    main,
)
from core.types.sku import (
    MarketSku,
    BlueOffer,
)
from core.types.taxes import (
    Vat,
    Tax,
)
from core.types import Currency, RtyOffer, Shop


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True
        cls.settings.report_subrole = 'blue-main'
        cls.index.shops += [
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=2,
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="BlueOfferSkuOne",
                hyperid=1,
                sku=1,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[
                    BlueOffer(
                        price=5,
                        price_old=8,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='shop_sku_322',
                        waremd5='Sku1Price5-IiLVm1Goleg',
                        randx=1,
                    ),
                ],
            ),
            MarketSku(
                title="BlueOfferSkuTwo",
                hyperid=1,
                sku=2,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[
                    BlueOffer(
                        price=9,
                        price_old=17,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='shop_sku_223',
                        waremd5='Sku2Price9-IiLVm1Goleg',
                        randx=1,
                    ),
                ],
            ),
        ]
        cls.index.creation_time = 120

    def _check_offer(self, text, price, vat=None, pricefrom=False, rgb=None):
        request = 'place=prime&text={}&rearr-factors=rty_qpipe=1'.format(text)
        if rgb:
            request += '&rgb={}'.format(rgb)
        response = self.report.request_json(request)

        price_value_field = 'value' if not pricefrom else 'min'
        self.assertFragmentIn(response, {'prices': {'currency': 'RUR', price_value_field: str(price)}})

        if vat:
            self.assertFragmentIn(response, {'vat': str(Vat(vat))})
        else:
            self.assertFragmentNotIn(response, {'vat': NotEmpty()})

    def test_blue_qfields(self):
        """
        Отдельно проверяем корректную работу синих полей быстрого пайплайна.
        """
        # Проверяем, что данные нормально читаются из поколения, когда в rty этих оферов ещё нет
        self._check_offer('BlueOfferSkuOne', 5, vat=Vat.VAT_10, rgb='blue')
        self._check_offer('BlueOfferSkuTwo', 9, vat=Vat.VAT_10, rgb='blue')

        # Делаем изменения через rty
        self.rty.offers += [
            RtyOffer(feedid=3, offerid='shop_sku_322', price=10),
            # Для синего Rty выключено чтение markup полей, поэтому они не изменятся
            RtyOffer(
                feedid=3,
                offerid='shop_sku_223',
                price=19,
                modification_time=121,
            ),
        ]

        # Проверяем изменения через rty
        self._check_offer('BlueOfferSkuOne', 10, vat=Vat.VAT_10, rgb='blue')
        self._check_offer('BlueOfferSkuTwo', 19, vat=Vat.VAT_10, rgb='blue')


if __name__ == '__main__':
    main()
