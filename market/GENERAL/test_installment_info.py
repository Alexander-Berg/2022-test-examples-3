#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.types import (
    Model,
    MarketSku,
    BlueOffer,
    Offer,
    Shop,
    OfferInstallmentInfo,
    BnplConditionsSettings,
    CreditGlobalRestrictions,
    ShopPaymentMethods,
    PaymentRegionalGroup,
    Payment,
)
from core.matcher import ElementCount, Absent, Regex, NotEmpty
from core.testcase import TestCase, main

IS_METADOC_MSG = {"logicTrace": [Regex("IsMetadocSearchOffers: 1")]}
MIN_BNPL_PRICE = 500
MAX_BNPL_PRICE = 3000
MIN_BANK_PRICE = 1500
MAX_BANK_PRICE = 50000

VIRTUAL_SHOP_ID = 10000


def offer_too_cheap_for_bank(model_id):
    return BlueOffer(
        title='{} offer with both installment, but too cheap for days'.format(model_id),
        price=(MIN_BANK_PRICE - 500),
        installment_info=OfferInstallmentInfo(days=[180], bnpl_available=True),
        download=True,
    )


def sku_too_cheap_for_bank(hyperid, sku, fesh):
    return MarketSku(hyperid=hyperid, sku=sku, fesh=fesh, blue_offers=[offer_too_cheap_for_bank(hyperid)])


def offer_too_expensive_for_bnpl(model_id):
    return BlueOffer(
        title='{} offer with both installment, but too expensive for bnpl'.format(model_id),
        price=(MAX_BNPL_PRICE + 600),
        installment_info=OfferInstallmentInfo(days=[180], bnpl_available=True),
        download=True,
    )


def sku_too_expensive_for_bnpl(hyperid, sku, fesh):
    return MarketSku(hyperid=hyperid, sku=sku, fesh=fesh, blue_offers=[offer_too_expensive_for_bnpl(hyperid)])


def sku_offer_without_installment_info(hyperid, sku, fesh):
    return MarketSku(
        hyperid=hyperid,
        sku=sku,
        fesh=fesh,
        blue_offers=[BlueOffer(title='{} offer without installment info'.format(hyperid), download=True)],
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.index.shops += [
            Shop(
                fesh=VIRTUAL_SHOP_ID,
                datafeed_id=VIRTUAL_SHOP_ID,
                priority_region=213,
                name='Виртуальный магазин',
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
        ]
        for fesh in [VIRTUAL_SHOP_ID, 1, 2, 3, 4, 5]:
            cls.index.shops += [Shop(fesh=fesh, priority_region=213, cpa=Shop.CPA_REAL)]
            cls.index.shops_payment_methods += [
                ShopPaymentMethods(
                    fesh=fesh,
                    payment_groups=[
                        PaymentRegionalGroup(
                            included_regions=[213],
                            payment_methods=[Payment.PT_CASH_ON_DELIVERY if fesh == 2 else Payment.PT_PREPAYMENT_CARD],
                        )
                    ],
                )
            ]

        for hyperid in range(100, 2000, 100):
            cls.index.models += [Model(hyperid=hyperid, hid=1000)]

        cls.index.bnpl_conditions.settings = BnplConditionsSettings(min_price=MIN_BNPL_PRICE, max_price=MAX_BNPL_PRICE)
        cls.index.credit_plans_container.global_restrictions = CreditGlobalRestrictions(
            min_price=MIN_BANK_PRICE, max_price=MAX_BANK_PRICE
        )

        cls.index.mskus += [
            MarketSku(
                hyperid=100,
                sku=1001,
                fesh=1,
                blue_offers=[
                    BlueOffer(
                        title='100 offer with days and bnpl',
                        price=2400,
                        installment_info=OfferInstallmentInfo(days=[180, 360, 0, 510], bnpl_available=True),
                        download=True,
                    )
                ],
            ),
            MarketSku(
                hyperid=100,
                sku=1002,
                fesh=2,
                blue_offers=[
                    BlueOffer(
                        title='100 offer without days and bnpl',
                        installment_info=OfferInstallmentInfo(days=[], bnpl_available=False),
                        download=True,
                    )
                ],
            ),
            sku_offer_without_installment_info(hyperid=100, sku=1003, fesh=3),
            sku_too_cheap_for_bank(hyperid=100, sku=1004, fesh=4),
            sku_too_expensive_for_bnpl(hyperid=100, sku=1005, fesh=5),
            MarketSku(
                hyperid=200,
                sku=2001,
                fesh=1,
                blue_offers=[
                    BlueOffer(
                        title='200 offer with only days',
                        price=3500,
                        installment_info=OfferInstallmentInfo(days=[180], bnpl_available=False),
                        download=True,
                    )
                ],
            ),
            MarketSku(
                hyperid=300,
                sku=3001,
                fesh=1,
                blue_offers=[
                    BlueOffer(
                        title='300 offer with only bnpl',
                        price=2500,
                        installment_info=OfferInstallmentInfo(days=[], bnpl_available=True),
                        download=True,
                    )
                ],
            ),
            sku_offer_without_installment_info(hyperid=400, sku=4001, fesh=1),
            sku_too_cheap_for_bank(hyperid=500, sku=5001, fesh=1),
            sku_too_expensive_for_bnpl(hyperid=600, sku=6001, fesh=1),
            MarketSku(
                hyperid=700,
                sku=7001,
                fesh=1,
                blue_offers=[
                    BlueOffer(
                        title='700 offer with both installment, but too expensive for both',
                        price=100000,
                        installment_info=OfferInstallmentInfo(days=[180], bnpl_available=False),
                        download=True,
                    )
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                hyperid=800,
                fesh=2,
                title='800 white offer without prepayment',
                price=2400,
                installment_info=OfferInstallmentInfo(days=[180], bnpl_available=False),
            ),
            Offer(
                hyperid=800,
                fesh=3,
                title='800 white offer with prepayment',
                price=2400,
                installment_info=OfferInstallmentInfo(days=[180], bnpl_available=False),
            ),
        ]

    def _term_to_json(self, term, monthly_payment):
        return {
            'term': term,
            'monthlyPayment': {
                'currency': 'RUR',
                'value': str(monthly_payment),
            },
        }

    def _gen_request(self, base_req, enable_installments, show_installments, enable_installments_filters=None):
        req = base_req + '&rearr-factors=market_nordstream_bypass=1'
        if enable_installments is not None:
            req += ';enable_installments={}'.format(enable_installments)
        if enable_installments_filters is not None:
            req += ';enable_installments_filters={}'.format(enable_installments_filters)
        if show_installments is not None:
            req += '&show-installments={}'.format(show_installments)
        return req

    def test_productoffers(self):
        for enable_installments in (None, '0', '1'):
            # от enable_installments_filters не должно зависеть показывать ли инфу о рассрочке
            for enable_installments_filters in (None, '0', '1'):
                for show_installments in (None, '0', '1'):
                    req = self._gen_request(
                        'place=productoffers&hyperid=100&rids=213',
                        enable_installments,
                        show_installments,
                        enable_installments_filters,
                    )
                    response = self.report.request_json(req)
                    show_installment_info = enable_installments == '1' and show_installments == '1'
                    self.assertFragmentIn(
                        response,
                        [
                            {  # заданы и месяца, и bnplAvailable = True
                                'slug': '100-offer-with-days-and-bnpl',
                                'installmentInfo': {
                                    'terms': [
                                        self._term_to_json(6, 400),
                                        self._term_to_json(12, 200),
                                        #  ноль дней пропустили
                                        self._term_to_json(17, 142),  # округление 141.17 в потолок
                                    ],
                                    'bnplAvailable': True,
                                }
                                if show_installment_info
                                else Absent(),
                            },
                            {  # задан только bnplAvailable = False
                                'slug': '100-offer-without-days-and-bnpl',
                                'installmentInfo': {
                                    'terms': ElementCount(0),
                                    'bnplAvailable': False,
                                }
                                if show_installment_info
                                else Absent(),
                            },
                            {  # рассрочка не задана
                                'slug': '100-offer-without-installment-info',
                                'installmentInfo': Absent(),
                            },
                            {  # заданы обе рассрочки, но банковская откидывается из-за дешевой цены
                                'slug': '100-offer-with-both-installment-but-too-cheap-for-days',
                                'installmentInfo': {
                                    'terms': ElementCount(0),
                                    'bnplAvailable': True,
                                }
                                if show_installment_info
                                else Absent(),
                            },
                            {  # заданы обе рассрочки, но bnpl откидывается из-за дорогой цены
                                'slug': '100-offer-with-both-installment-but-too-expensive-for-bnpl',
                                'installmentInfo': {
                                    'terms': [
                                        self._term_to_json(6, 600),
                                    ],
                                    'bnplAvailable': False,
                                }
                                if show_installment_info
                                else Absent(),
                            },
                        ],
                        allow_different_len=False,
                    )

                    # проверка скрытия рассрочки для офферов без предоплаты
                    req = self._gen_request(
                        'place=productoffers&hyperid=800&rids=213',
                        enable_installments,
                        show_installments,
                        enable_installments_filters,
                    )
                    response = self.report.request_json(req)
                    self.assertFragmentIn(
                        response,
                        [
                            {
                                'slug': '800-white-offer-without-prepayment',
                                'installmentInfo': Absent(),
                            },
                            {
                                'slug': '800-white-offer-with-prepayment',
                                'installmentInfo': {
                                    'terms': [
                                        self._term_to_json(6, 400),
                                    ],
                                    'bnplAvailable': False,
                                }
                                if show_installment_info
                                else Absent(),
                            },
                        ],
                        allow_different_len=False,
                    )

                    # проверка отфильтровывания офферов без предоплаты
                    response = self.report.request_json(req + '&has-installment=1')
                    if enable_installments_filters == '1':
                        self.assertFragmentNotIn(response, {'slug': '800-white-offer-without-prepayment'})
                    elif enable_installments_filters == '0':
                        self.assertFragmentIn(response, {'slug': '800-white-offer-without-prepayment'})

    def test_not_checked_bool_filter(self):
        for enable_installments in (None, '0', '1'):
            for enable_installments_filters in (None, '0', '1'):
                for show_installments in (None, '0', '1'):
                    req = self._gen_request(
                        'debug=1&place=prime&hid=1000&allow-collapsing=1&onstock=1',
                        enable_installments,
                        show_installments,
                        enable_installments_filters,
                    )
                    response = self.report.request_json(req)
                    use_filter = (
                        enable_installments == '1' and enable_installments_filters != '0' and show_installments == '1'
                    ) or (enable_installments_filters == '1')
                    if not use_filter:
                        self.assertFragmentNotIn(response, {'filters': [{'id': 'has-installments'}]})
                    else:
                        self.assertFragmentIn(
                            response,
                            {
                                'id': 'has-installment',
                                'name': 'Можно оплатить частями',
                                'type': 'boolean',
                                'values': [
                                    {
                                        'found': 9,  # должно быть 8, но без включенного фильтра не проверяются все условия на рассрочку
                                        'initialFound': 1,
                                        'value': '1',
                                        'checked': Absent(),
                                    },
                                ],
                            },
                            allow_different_len=False,
                        )
                    self.assertFragmentIn(response, IS_METADOC_MSG)
                    self.assertFragmentNotIn(response, {'id': 'with-installments'})

    def test_bool_filter(self):
        base_req = 'debug=1&place=prime&hid=1000&allow-collapsing=1&onstock=1&platform=desktop&rids=213'
        req = self._gen_request(base_req, '1', '1')
        response = self.report.request_json(req)
        # под ограничения по bnpl цене подходят 1001, 1004, 3001, 5001
        # под ограничения по банковской цене подходят 1001, 1005, 2001, 6001
        self.assertFragmentIn(
            response,
            {
                'id': 'has-installment',
                'name': 'Можно оплатить частями',
                'type': 'boolean',
                'values': [
                    {
                        'found': 9,  # (1001, 1004, 3001, 5001 + 2 white offers) | (1001, 1005, 2001, 6001)
                        'initialFound': 1,
                        'value': '1',
                        'checked': Absent(),
                    },
                ],
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(response, {'id': 'with-installments'})
        self.assertFragmentNotIn(response, {'BAD_PRICE_FOR_INSTALLMENT': NotEmpty()})
        self.assertFragmentNotIn(response, {'NO_PREPAYMENT': NotEmpty()})
        self.assertFragmentIn(response, IS_METADOC_MSG)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'product', 'id': 100},
                    {'entity': 'product', 'id': 200},
                    {'entity': 'product', 'id': 300},
                    {'entity': 'product', 'id': 400},
                    {'entity': 'product', 'id': 500},
                    {'entity': 'product', 'id': 600},
                    {'entity': 'product', 'id': 700},
                    {'entity': 'product', 'id': 800},
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(req + '&has-installment=1')
        self.assertFragmentIn(
            response,
            {
                'id': 'has-installment',
                'values': [
                    {
                        'found': 8,
                        'value': '1',
                        'checked': True,
                    }
                ],
            },
            allow_different_len=False,
        )
        # оффер для скю 7001 слишком дорогой для обеих рассрочек, а оффер для модели 800 не имеет предоплаты
        self.assertFragmentIn(
            response,
            {
                'BAD_PRICE_FOR_INSTALLMENT': 1,
                'NO_PREPAYMENT': 1,
            },
        )
        self.assertFragmentNotIn(response, IS_METADOC_MSG)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {'entity': 'product', 'id': 100},
                    {'entity': 'product', 'id': 200},
                    {'entity': 'product', 'id': 300},
                    {'entity': 'product', 'id': 500},
                    {'entity': 'product', 'id': 600},
                    {'entity': 'product', 'id': 800},
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
