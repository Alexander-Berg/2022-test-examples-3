#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, Currency, DeliveryOption, MarketSku, Model, Offer, Shop
from core.matcher import NoKey
from core.types.taxes import Vat, Tax, vat_shifter_fabric


VAT_VARIANTS = [NoKey('vat'), 'VAT_18', 'VAT_10', 'VAT_18_118', 'VAT_10_110', 'VAT_0', 'NO_VAT', 'VAT_20', 'VAT_20_120']

FOOD_CATEGORY = 91307
OTHER_CATEGORY = 534636


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.microseconds_for_disabled_random = 1546290000000000
        cls.index.shops += [
            Shop(
                fesh=1,
                priority_region=213,
                name='test_shop_with_tax_OSN',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
            ),
            Shop(
                fesh=2,
                priority_region=213,
                name='test_shop_with_tax_USN',
                currency=Currency.RUR,
                tax_system=Tax.USN,
                delivery_vat=Vat.VAT_10,
            ),
            Shop(
                fesh=3,
                priority_region=213,
                name='test_shop_with_tax_USN_MINUS',
                currency=Currency.RUR,
                tax_system=Tax.USN_MINUS_COST,
                delivery_vat=Vat.VAT_18_118,
            ),
            Shop(
                fesh=4,
                priority_region=213,
                name='test_shop_with_tax_ENVD',
                currency=Currency.RUR,
                tax_system=Tax.ENVD,
                delivery_vat=Vat.VAT_10_110,
            ),
            Shop(
                fesh=5,
                priority_region=213,
                name='test_shop_with_tax_ECHN',
                currency=Currency.RUR,
                tax_system=Tax.ECHN,
                delivery_vat=Vat.VAT_0,
            ),
            Shop(
                fesh=6,
                priority_region=213,
                name='test_shop_with_tax_PSN',
                currency=Currency.RUR,
                tax_system=Tax.PSN,
                delivery_vat=Vat.NO_VAT,
            ),
            Shop(
                fesh=7,
                priority_region=213,
                name='test_shop_without_tax',
                currency=Currency.RUR,
                delivery_vat=Vat.VAT_18,
            ),
            Shop(
                fesh=8,
                priority_region=213,
                name='test_shop_without_tax',
                currency=Currency.RUR,
                delivery_vat=Vat.VAT_18,
            ),
            Shop(
                fesh=9,
                priority_region=213,
                name='test_shop_without_tax',
                currency=Currency.RUR,
                delivery_vat=Vat.VAT_18,
            ),
        ]

        for shop_id in range(1, 10):
            cls.index.offers += [
                Offer(title="", fesh=shop_id),
                Offer(title=str(Vat(Vat.VAT_18)), fesh=shop_id, vat=Vat.VAT_18),
                Offer(title=str(Vat(Vat.VAT_10)), fesh=shop_id, vat=Vat.VAT_10),
                Offer(title=str(Vat(Vat.VAT_18_118)), fesh=shop_id, vat=Vat.VAT_18_118),
                Offer(title=str(Vat(Vat.VAT_10_110)), fesh=shop_id, vat=Vat.VAT_10_110),
                Offer(title=str(Vat(Vat.VAT_0)), fesh=shop_id, vat=Vat.VAT_0),
                Offer(title=str(Vat(Vat.NO_VAT)), fesh=shop_id, vat=Vat.NO_VAT),
                Offer(title=str(Vat(Vat.VAT_20)), fesh=shop_id, vat=Vat.VAT_20),
                Offer(title=str(Vat(Vat.VAT_20_120)), fesh=shop_id, vat=Vat.VAT_20_120),
            ]

    def __test_single_tax(self, fesh, tax, delivery_vat, valid_vats, vat_shifter):
        request_string = 'place=prime&fesh={}'.format(fesh)

        response = self.report.request_json(request_string)

        result_array = []
        for offer_vat in VAT_VARIANTS:
            if offer_vat in valid_vats:
                result_array.append(
                    {
                        'titles': {
                            'raw': offer_vat if isinstance(offer_vat, str) else '',
                        },
                        'entity': 'offer',
                        'shop': {'id': fesh, 'taxSystem': tax, 'deliveryVat': vat_shifter(delivery_vat)},
                        'vat': vat_shifter(offer_vat),
                    }
                )
            else:
                result_array.append(
                    {
                        'titles': {
                            'raw': offer_vat if isinstance(offer_vat, str) else '',
                        },
                        'entity': 'offer',
                        'shop': {'id': fesh, 'taxSystem': tax, 'deliveryVat': vat_shifter(delivery_vat)},
                        'vat': NoKey('vat'),
                    }
                )
        self.assertFragmentIn(response, {'results': result_array}, allow_different_len=False)

    def test_osn_tax(self):
        '''
        Что тестируем: проверяем проброс значений НДС для Основной Системы Налогообложения.
        Для этой СНО доступны все НДС, так что сообщений об ошибках быть не должно.
        '''
        self.__test_single_tax(
            fesh=1,
            tax='OSN',
            delivery_vat=NoKey('delivery_vat'),
            valid_vats=[
                NoKey('vat'),
                'VAT_18',
                'VAT_10',
                'VAT_18_118',
                'VAT_10_110',
                'VAT_0',
                'NO_VAT',
                'VAT_20',
                'VAT_20_120',
            ],
            vat_shifter=vat_shifter_fabric("18_TO_20"),
        )

    def test_usn_tax(self):
        '''
        Что тестируем: в этом и следующих тестах проверяем проброс значений НДС для других систем налогообложения.
        Для этой СНО доступна только нет НДС и пустая запись (НДС не указан), остальные ставки должны генерировать ошибку
        '''
        self.__test_single_tax(
            fesh=2,
            tax='USN',
            delivery_vat='VAT_10',
            valid_vats=[NoKey('vat'), 'NO_VAT'],
            vat_shifter=vat_shifter_fabric("18_TO_20"),
        )

    def test_usn_minus_cost_tax(self):
        self.__test_single_tax(
            fesh=3,
            tax='USN_MINUS_COST',
            delivery_vat='VAT_18_118',
            valid_vats=[NoKey('vat'), 'NO_VAT'],
            vat_shifter=vat_shifter_fabric("18_TO_20"),
        )

    def test_envd_tax(self):
        self.__test_single_tax(
            fesh=4,
            tax='ENVD',
            delivery_vat='VAT_10_110',
            valid_vats=[NoKey('vat'), 'NO_VAT'],
            vat_shifter=vat_shifter_fabric("18_TO_20"),
        )

    def test_eshn_tax(self):
        self.__test_single_tax(
            fesh=5,
            tax='ECHN',
            delivery_vat='VAT_0',
            valid_vats=[NoKey('vat'), 'NO_VAT'],
            vat_shifter=vat_shifter_fabric("18_TO_20"),
        )

    def test_psn_tax(self):
        self.__test_single_tax(
            fesh=6,
            tax='PSN',
            delivery_vat='NO_VAT',
            valid_vats=[NoKey('vat'), 'NO_VAT'],
            vat_shifter=vat_shifter_fabric("18_TO_20"),
        )

    def test_osn_tax_flagged(self):
        '''
        Что тестируем: проверяем проброс значений НДС для Основной Системы Налогообложения.
        Для этой СНО доступны все НДС, так что сообщений об ошибках быть не должно.
        В данном тесте также проверяем, что невозможно получить НДС в 18 % с первого января (даже используя флаги)
        '''
        self.__test_single_tax(
            fesh=1,
            tax='OSN',
            delivery_vat=NoKey('delivery_vat'),
            valid_vats=[
                NoKey('vat'),
                'VAT_18',
                'VAT_10',
                'VAT_18_118',
                'VAT_10_110',
                'VAT_0',
                'NO_VAT',
                'VAT_20',
                'VAT_20_120',
            ],
            vat_shifter=vat_shifter_fabric("18_TO_20"),
        )

    def test_usn_tax_flagged(self):
        '''
        Что тестируем: в этом и следующих тестах проверяем проброс значений НДС для других систем налогообложения.
        Для этой СНО доступна только нет НДС и пустая запись (НДС не указан), остальные ставки должны генерировать ошибку
        В данном тесте также проверяем, что невозможно получить НДС в 18 % с первого января (даже используя флаги)
        '''
        self.__test_single_tax(
            fesh=2,
            tax='USN',
            delivery_vat='VAT_10',
            valid_vats=[NoKey('vat'), 'NO_VAT'],
            vat_shifter=vat_shifter_fabric("18_TO_20"),
        )

    def test_usn_minus_cost_tax_flagged(self):
        self.__test_single_tax(
            fesh=3,
            tax='USN_MINUS_COST',
            delivery_vat='VAT_18_118',
            valid_vats=[NoKey('vat'), 'NO_VAT'],
            vat_shifter=vat_shifter_fabric("18_TO_20"),
        )

    def test_envd_tax_flagged(self):
        self.__test_single_tax(
            fesh=4,
            tax='ENVD',
            delivery_vat='VAT_10_110',
            valid_vats=[NoKey('vat'), 'NO_VAT'],
            vat_shifter=vat_shifter_fabric("18_TO_20"),
        )

    def test_eshn_tax_flagged(self):
        self.__test_single_tax(
            fesh=5,
            tax='ECHN',
            delivery_vat='VAT_0',
            valid_vats=[NoKey('vat'), 'NO_VAT'],
            vat_shifter=vat_shifter_fabric("18_TO_20"),
        )

    def test_psn_tax_flagged(self):
        self.__test_single_tax(
            fesh=6,
            tax='PSN',
            delivery_vat='NO_VAT',
            valid_vats=[NoKey('vat'), 'NO_VAT'],
            vat_shifter=vat_shifter_fabric("18_TO_20"),
        )

    OSN_TAX_SYSTEM_SHOP_ID = 11
    USN_TAX_SYSTEM_SHOP_ID = 12
    WHITE_CPA_SHOP_ID = 13

    GOOD_VAT_OFFER_OSN = BlueOffer(
        price=9000,
        price_old=17,
        vat=Vat.VAT_10,
        feedid=OSN_TAX_SYSTEM_SHOP_ID,
        offerid='good_vat_offer_osn',
        waremd5='GoodVatOSN-IiLVm1Goleg',
    )
    BAD_VAT_OFFER = BlueOffer(
        price=9,
        price_old=17,
        vat=Vat.VAT_10,
        feedid=USN_TAX_SYSTEM_SHOP_ID,
        offerid='bad_vat_offer',
        waremd5='BadVat--e9-IiLVm1Goleg',
    )

    GOOD_VAT_OFFER_USN = BlueOffer(
        price=9000,
        price_old=17,
        vat=Vat.NO_VAT,
        feedid=USN_TAX_SYSTEM_SHOP_ID,
        offerid='good_vat_offer_usn',
        waremd5='GoodVatUSN-IiLVm1Goleg',
    )

    GOOD_VAT_WHITE_CPA_OFFER = Offer(
        fesh=WHITE_CPA_SHOP_ID,
        vat=Vat.NO_VAT,
        waremd5='GoodVat-White-Vm1Goleg',
        cpa=Offer.CPA_REAL,
        delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
    )
    BAD_VAT_WHITE_CPA_OFFER = Offer(
        fesh=WHITE_CPA_SHOP_ID,
        vat=Vat.VAT_18,
        waremd5='BadVat-White--Vm1Goleg',
        cpa=Offer.CPA_REAL,
        delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
    )
    BAD_VAT_WHITE_CPA_EDA_OFFER = Offer(
        fesh=WHITE_CPA_SHOP_ID,
        vat=Vat.VAT_18,
        waremd5='BadVatWhiteEdaVm1Goleg',
        cpa=Offer.CPA_REAL,
        delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
        is_eda=True,
        hyperid=101,
    )
    BAD_VAT_WHITE_CPA_LAVKA_OFFER = Offer(
        fesh=WHITE_CPA_SHOP_ID,
        vat=Vat.VAT_18,
        waremd5='BadVat-Lavka--Vm1Goleg',
        cpa=Offer.CPA_REAL,
        delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
        is_lavka=True,
        hyperid=101,
    )

    @classmethod
    def prepare_vat_for_cpa(cls):
        cls.index.shops += [
            Shop(
                fesh=cls.OSN_TAX_SYSTEM_SHOP_ID,
                datafeed_id=cls.OSN_TAX_SYSTEM_SHOP_ID,
                priority_region=2,
                name='osn_blue_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=cls.USN_TAX_SYSTEM_SHOP_ID,
                datafeed_id=cls.USN_TAX_SYSTEM_SHOP_ID,
                priority_region=2,
                name='usn_blue_shop',
                currency=Currency.RUR,
                tax_system=Tax.USN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=cls.WHITE_CPA_SHOP_ID,
                priority_region=213,
                name='test_shop_without_tax',
                currency=Currency.RUR,
                tax_system=Tax.USN,
                cpa=Shop.CPA_REAL,
            ),
        ]
        cls.index.models += [
            Model(hyperid=1, hid=OTHER_CATEGORY, title='hyperid_1'),
            Model(hyperid=101, hid=FOOD_CATEGORY, title='hyperid_101'),
        ]
        cls.index.mskus += [
            MarketSku(
                sku=1,
                hyperid=1,
                blue_offers=[
                    cls.GOOD_VAT_OFFER_OSN,
                    cls.BAD_VAT_OFFER,
                    cls.GOOD_VAT_OFFER_USN,
                ],
            ),
        ]
        cls.index.offers += [
            cls.GOOD_VAT_WHITE_CPA_OFFER,
            cls.BAD_VAT_WHITE_CPA_OFFER,
            cls.BAD_VAT_WHITE_CPA_EDA_OFFER,
            cls.BAD_VAT_WHITE_CPA_LAVKA_OFFER,
        ]

    def test_vat_for_cpa_good(self):
        '''
        Проверяем, что для cpa оферов правильный НДС обязателен. СРА офер без НДС скрывается
        '''
        request = 'place=offerinfo&rids=213&regset=1&show-urls=&offerid={}'
        for offer in [self.GOOD_VAT_OFFER_OSN, self.GOOD_VAT_OFFER_USN, self.GOOD_VAT_WHITE_CPA_OFFER]:
            response = self.report.request_json(request.format(offer.waremd5))
            self.assertFragmentIn(
                response, {'wareId': offer.waremd5, 'vat': str(Vat(offer.vat))}, allow_different_len=False
            )

    def test_vat_for_cpa_bad(self):
        '''
        СРА офер без правильного НДС скрывается
        '''
        request = 'place=offerinfo&rids=2&regset=1&show-urls=&offerid={}'
        for offer in [self.BAD_VAT_OFFER, self.BAD_VAT_WHITE_CPA_OFFER]:
            response = self.report.request_json(request.format(offer.waremd5))
            self.assertFragmentNotIn(
                response,
                {
                    'wareId': offer.waremd5,
                },
            )

    def test_bad_vat_for_cpa_eda_lavka(self):
        '''
        СРА офер еды/лавки без правильного НДС не скрывается в случае если клиент еда/лавка,
        во всех остальных случаях скрывается
        '''
        request = 'place=offerinfo&rids=2&regset=1&show-urls=&offerid={}&enable-foodtech-offers=eda_retail,eda_restaurants,lavka'
        for offer in [self.BAD_VAT_WHITE_CPA_EDA_OFFER, self.BAD_VAT_WHITE_CPA_LAVKA_OFFER]:
            # проверяем что офер еды/лавки не скрывается если client=eats/lavka
            for flag in ['&client=eats', '&client=lavka']:
                response = self.report.request_json(request.format(offer.waremd5) + flag)
                self.assertFragmentIn(
                    response,
                    {
                        'wareId': offer.waremd5,
                    },
                )

            # проверяем что в остальных случаях офер еды/лавки скрывается
            for flag in ['&client=some_other_client', '']:
                response = self.report.request_json(request.format(offer.waremd5) + flag)
                self.assertFragmentNotIn(
                    response,
                    {
                        'wareId': offer.waremd5,
                    },
                )


if __name__ == '__main__':
    main()
