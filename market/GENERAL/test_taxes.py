#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Currency, Offer, Shop
from core.matcher import NoKey
from core.types.taxes import Vat, Tax, vat_shifter_fabric

VAT_VARIANTS = [NoKey('vat'), 'VAT_18', 'VAT_10', 'VAT_18_118', 'VAT_10_110', 'VAT_0', 'NO_VAT', 'VAT_20', 'VAT_20_120']


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.microseconds_for_disabled_random = 1544184256999999
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
            # TODO: Удалить в https://st.yandex-team.ru/MARKETOUT-22475
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
            vat_shifter=vat_shifter_fabric("20_TO_18"),
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
            vat_shifter=vat_shifter_fabric("20_TO_18"),
        )

    def test_usn_minus_cost_tax(self):
        self.__test_single_tax(
            fesh=3,
            tax='USN_MINUS_COST',
            delivery_vat='VAT_18_118',
            valid_vats=[NoKey('vat'), 'NO_VAT'],
            vat_shifter=vat_shifter_fabric("20_TO_18"),
        )

    def test_envd_tax(self):
        self.__test_single_tax(
            fesh=4,
            tax='ENVD',
            delivery_vat='VAT_10_110',
            valid_vats=[NoKey('vat'), 'NO_VAT'],
            vat_shifter=vat_shifter_fabric("20_TO_18"),
        )

    def test_eshn_tax(self):
        self.__test_single_tax(
            fesh=5,
            tax='ECHN',
            delivery_vat='VAT_0',
            valid_vats=[NoKey('vat'), 'NO_VAT'],
            vat_shifter=vat_shifter_fabric("20_TO_18"),
        )

    def test_psn_tax(self):
        self.__test_single_tax(
            fesh=6,
            tax='PSN',
            delivery_vat='NO_VAT',
            valid_vats=[NoKey('vat'), 'NO_VAT'],
            vat_shifter=vat_shifter_fabric("20_TO_18"),
        )

    def test_osn_tax_flagged(self):
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

    def test_usn_tax_flagged(self):
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


if __name__ == '__main__':
    main()
