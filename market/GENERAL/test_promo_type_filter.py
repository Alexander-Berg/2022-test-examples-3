#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import Absent
from core.types import (
    DynamicMarketSku,
    HyperCategory,
    HyperCategoryType,
    Model,
    ModelGroup,
    Offer,
    Promo,
    PromoPurchase,
    PromoType,
)
from core.types.autogen import b64url_md5
from core.types.offer_promo import PromoBlueCashback

from datetime import datetime


def hyperid(hid, shift):
    return hid * 100 + shift


def promo_key(offer_id):
    return 'xMpCOKC5I4INzFC%08d' % offer_id


def offerid(hid, shift):
    return hid * 1000 + shift


def waremd5(offer_id):
    return b64url_md5('%08d' % offer_id)


def cmagic(offer_id):
    return 'f2dfc75bbd15ae22fbd2e35b%08x' % offer_id


def output_model(id, title):
    return {'entity': 'product', 'titles': {'raw': title}, 'id': id}


def model(hid, offer, output):
    shift = shift_map[offer]
    hyp = hyperid(hid, shift)
    title = 'philips model %d' % hyp
    group_hyperid = None

    if hid == 10 and offer == offers_gift:
        if output:
            return {'entity': 'product', 'titles': {'raw': 'philips group model 1100'}, 'id': 1100}
        group_hyperid = hyperid(hid, 100)
    if output:
        return output_model(hyp, title)

    return Model(hyperid=hyp, title=title, hid=hid, group_hyperid=group_hyperid)


def output_offer(id, title):
    return {
        'entity': 'offer',
        'titles': {'raw': title},
        'shop': {'feed': {'offerId': str(id)}},
    }


def offers_cashback(hid, output):
    shift = shift_map[offers_cashback]
    offer_id = offerid(hid, shift)
    title = 'philips cashback offer'

    if output:
        return output_offer(offer_id, title)

    return [
        Offer(
            title=title,
            fesh=shift,
            hid=hid,
            hyperid=hyperid(hid, shift),
            offerid=offer_id,
            waremd5=waremd5(offer_id),
            cmagic=cmagic(offer_id),
            price=500,
            promo=Promo(
                promo_type=PromoType.BLUE_CASHBACK,
                key='Promo_blue_cashback',
                shop_promo_id='blue_extra_cb_with_dpt_check',
                blue_cashback=PromoBlueCashback(
                    share=0.5,
                    version=6,
                    priority=1,
                ),
            ),
            blue_promo_key=['blue_extra_cb_with_dpt_check'],
        )
    ]


def offers_promocode(hid, output):
    shift = shift_map[offers_promocode]
    offer_id = offerid(hid, shift)
    title = 'philips promocode offer'

    if output:
        return output_offer(offer_id, title)

    return [
        Offer(
            title=title,
            fesh=shift,
            hid=hid,
            hyperid=hyperid(hid, shift),
            offerid=offer_id,
            waremd5=waremd5(offer_id),
            cmagic=cmagic(offer_id),
            price=500,
            promo=Promo(
                promo_type=PromoType.PROMO_CODE,
                start_date=datetime(1980, 1, 1),
                end_date=datetime(2050, 1, 1),
                key=promo_key(offer_id),
                url='http://my.url',
                promo_code="my promo code",
                discount_value=300,
                discount_currency='RUR',
                outlets=[111, 222],
                purchases=[
                    PromoPurchase(offer_id=offer_id),
                ],
            ),
        ),
    ]


def offers_n_plus_m(hid, output):
    shift = shift_map[offers_n_plus_m]
    offer_id = offerid(hid, shift)
    title = 'philips nplusm offer'

    if output:
        return output_offer(offer_id, title)

    return [
        Offer(
            title=title,
            fesh=shift,
            hid=hid,
            hyperid=hyperid(hid, shift),
            offerid=offer_id,
            waremd5=waremd5(offer_id),
            cmagic=cmagic(offer_id),
            promo=Promo(
                promo_type=PromoType.N_PLUS_ONE,
                key=promo_key(offer_id),
                required_quantity=3,
                free_quantity=34,
                purchases=[
                    PromoPurchase(offer_id=offer_id),
                ],
            ),
        )
    ]


def offers_gift(hid, output):
    shift = shift_map[offers_gift]
    offer_id = offerid(hid, shift)
    title = 'philips offer with gift'

    if output:
        return output_offer(offer_id, title)

    return [
        Offer(
            title=title,
            fesh=shift,
            hid=hid,
            hyperid=hyperid(hid, shift),
            offerid=offer_id,
            waremd5=waremd5(offer_id),
            cmagic=cmagic(offer_id),
            promo=Promo(
                promo_type=PromoType.GIFT_WITH_PURCHASE,
                start_date=datetime(1980, 1, 1),
                end_date=datetime(2050, 1, 1),
                key=promo_key(offer_id),
                url='http://my.url',
                required_quantity=2,
                feed_id=hid,
                gift_offers=[1, 2],
                gift_gifts=[3, 4],
            ),
        )
    ]


def offers_flash_discount(hid, output):
    shift = shift_map[offers_flash_discount]
    offer_id = offerid(hid, shift)
    title = 'philips flashdiscount offer'

    if output:
        return output_offer(offer_id, title)

    return [
        Offer(
            title=title,
            fesh=shift,
            hid=hid,
            hyperid=hyperid(hid, shift),
            offerid=offer_id,
            waremd5=waremd5(offer_id),
            cmagic=cmagic(offer_id),
            price=150,
            price_old=200,
            promo_price=49,
            promo=Promo(
                promo_type=PromoType.FLASH_DISCOUNT,
                start_date=datetime(1985, 6, 20),
                end_date=datetime(1985, 6, 26),
                key=promo_key(offer_id),
                url='http://my.url',
            ),
        ),
    ]


def offers_discount(hid, output):
    shift = shift_map[offers_discount]
    offer_id = offerid(hid, shift)
    title = 'philips discount offer'

    if output:
        return output_offer(offer_id, title)

    return [
        Offer(
            title=title,
            fesh=shift,
            hid=hid,
            hyperid=hyperid(hid, shift),
            offerid=offer_id,
            waremd5=waremd5(offer_id),
            cmagic=cmagic(offer_id),
            price=150,
            price_old=200,
        ),
    ]


def offers_simple(hid, output):
    shift = shift_map[offers_simple]
    offer_id = offerid(hid, shift)
    title = 'philips simple offer'

    if output:
        return output_offer(offer_id, title)

    return [
        Offer(
            title=title,
            fesh=shift,
            hid=hid,
            hyperid=hyperid(hid, shift),
            offerid=offer_id,
            waremd5=waremd5(offer_id),
            cmagic=cmagic(offer_id),
            price=150,
        ),
    ]


shift_map = {
    offers_promocode: 10,
    offers_n_plus_m: 20,
    offers_gift: 30,
    offers_flash_discount: 40,
    offers_discount: 50,
    offers_simple: 60,
    offers_cashback: 70,
}


def response_filter(inp, models_and_offers=True):
    ret = {
        'filters': [
            {"id": "glprice"},
            {
                "id": "promo-type",
                'values': [
                    {
                        'id': 'discount',
                        "found": 0,
                    },
                    {
                        'id': 'promo-code',
                        "found": 0,
                    },
                    {
                        'id': 'gift-with-purchase',
                        "found": 0,
                    },
                    {
                        'id': 'n-plus-m',
                        "found": 0,
                    },
                ],
            },
        ]
    }

    add = 1
    if models_and_offers:
        ret['filters'].append({"id": "manufacturer_warranty"})
        add = 2
    else:
        ret['filters'].append({"id": "onstock"})

    for i in inp:
        if i == offers_discount or i == offers_flash_discount:
            ret['filters'][1]['values'][0]['found'] += add
        elif i == offers_promocode:
            ret['filters'][1]['values'][1]['found'] = add
        elif i == offers_gift:
            ret['filters'][1]['values'][2]['found'] = add
        elif i == offers_n_plus_m:
            ret['filters'][1]['values'][3]['found'] = add

    return ret


cases = [
    (10, [offers_simple, offers_promocode, offers_n_plus_m, offers_gift, offers_flash_discount, offers_discount]),
    (11, [offers_promocode, offers_n_plus_m, offers_gift, offers_flash_discount, offers_discount]),
    (12, [offers_simple, offers_promocode, offers_n_plus_m, offers_gift, offers_flash_discount]),
    (13, [offers_simple, offers_promocode, offers_n_plus_m, offers_gift, offers_discount]),
    (14, [offers_simple, offers_promocode, offers_n_plus_m, offers_gift]),
    (15, [offers_simple]),
    (16, [offers_simple, offers_promocode, offers_flash_discount, offers_discount]),
    (17, [offers_gift]),
    (18, [offers_promocode, offers_cashback]),
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']
        cls.settings.default_search_experiment_flags += ['market_enable_common_promo_type_filter=0']
        cls.index.hypertree += [HyperCategory(hid=42, uniq_name='Тракторы')]

        for hid, v in cases:
            for f in v:
                cls.index.offers += f(hid, False)
                cls.index.models += [model(hid, f, False)]
            cls.index.hypertree += [
                HyperCategory(hid=hid, output_type=HyperCategoryType.GURU),
            ]

        cls.index.model_groups += [ModelGroup(hyperid=hyperid(10, 100), title='philips group model 1100', hid=10)]

    def test_output_filter(self):

        response = self.report.request_json('place=prime&allow-collapsing=1&entities=product&hid=10&debug=da')
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    # {"id": "glprice"}, # почему нет?
                    {"id": "onstock"},
                    {
                        "id": "promo-type",
                        'values': [
                            {'id': 'discount', "found": 4},
                            {'id': 'promo-code', "found": 2},
                            {'id': 'gift-with-purchase', "found": 2},
                            {'id': 'n-plus-m', "found": 2},
                        ],
                    },
                ]
            },
            allow_different_len=True,
            preserve_order=False,
        )
        self.assertFragmentIn(response, 'Make additional request for glfilters')

        response = self.report.request_json('place=prime&allow-collapsing=1&entities=offer&hid=10&debug=da')
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {"id": "glprice"},
                    {"id": "manufacturer_warranty"},
                    {"id": "onstock"},
                    {
                        "id": "promo-type",
                        'values': [
                            {'id': 'discount', "found": 4},
                            {'id': 'promo-code', "found": 2},
                            {'id': 'gift-with-purchase', "found": 2},
                            {'id': 'n-plus-m', "found": 2},
                        ],
                    },
                ]
            },
            allow_different_len=True,
            preserve_order=False,
        )
        self.assertFragmentIn(response, 'Make additional request for glfilters')

        response = self.report.request_json('place=prime&allow-collapsing=1&hid=10&debug=da')
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {"id": "glprice"},
                    {"id": "manufacturer_warranty"},
                    {"id": "onstock"},
                    {
                        "id": "promo-type",
                        'values': [
                            {'id': 'discount', "found": 6},
                            {'id': 'promo-code', "found": 3},
                            {'id': 'gift-with-purchase', "found": 3},
                            {'id': 'n-plus-m', "found": 3},
                        ],
                    },
                ]
            },
            allow_different_len=True,
            preserve_order=False,
        )
        self.assertFragmentIn(response, 'Make additional request for glfilters')

        for promo_type in ['discount', 'gift-with-purchase', 'n-plus-m', 'market']:
            response = self.report.request_json(
                'place=prime&allow-collapsing=1&hid=10&promo-type={}&debug=da'.format(promo_type)
            )
            self.assertFragmentIn(
                response,
                {
                    'filters': [
                        {"id": "glprice"},
                        {"id": "manufacturer_warranty"},
                        {"id": "onstock"},
                        {
                            "id": "promo-type",
                            'values': [
                                {'id': 'discount', "found": 4},
                                {'id': 'promo-code', "found": 2},
                                {'id': 'gift-with-purchase', "found": 2},
                                {'id': 'n-plus-m', "found": 2},
                            ],
                        },
                    ]
                },
                allow_different_len=True,
                preserve_order=False,
            )
            self.assertFragmentNotIn(response, {'filters': [{"id": "filter-promo-or-discount"}]})
            # оффероспецифичные фильтры делают ненужным отдельный дозапрос
            self.assertFragmentNotIn(response, 'Make additional request for glfilters')

        response = self.report.request_json('place=prime&allow-collapsing=1&hid=10&promo-type=promo-code&debug=da')
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {"id": "glprice"},
                    {"id": "manufacturer_warranty"},
                    {"id": "onstock"},
                    {
                        "id": "promo-type",
                        'values': [
                            {'id': 'discount', "found": 2},
                            {'id': 'promo-code', "found": 2},
                            {'id': 'gift-with-purchase', "found": 1},
                            {'id': 'n-plus-m', "found": 1},
                        ],
                    },
                ]
            },
            allow_different_len=True,
            preserve_order=False,
        )
        self.assertFragmentNotIn(response, {'filters': [{"id": "filter-promo-or-discount"}]})
        # оффероспецифичные фильтры делают ненужным отдельный дозапрос
        self.assertFragmentNotIn(response, 'Make additional request for glfilters')

    def test_output_no_filter(self):
        '''
        проверяем, что под экспериментом выводится старый фильтр
        а если market_remove_promos, не выводятся оба
        '''
        for k, v in cases:
            response = self.report.request_json(
                'place=prime&hid=%d&rearr-factors=market_do_not_split_promo_filter=1' % k
            )
            self.assertFragmentNotIn(response, {'filters': [{"id": "promo-type"}]})
            if k == 15:  # no promo offers
                self.assertFragmentNotIn(response, {'filters': [{"id": "filter-promo-or-discount"}]})
            else:
                self.assertFragmentIn(response, {'filters': [{"id": "filter-promo-or-discount"}]})

            response = self.report.request_json('place=prime&hid=%d&rearr-factors=market_remove_promos=1' % k)
            self.assertFragmentNotIn(response, {'filters': [{"id": "promo-type"}]})
            self.assertFragmentNotIn(response, {'filters': [{"id": "filter-promo-or-discount"}]})

    def check_filter_(self, hid, url, in_lst, not_in_lst, mode):
        if mode == 1:
            url += '&entities=product'
        elif mode == 2:
            url += '&entities=offer'

        response = self.report.request_json(url)
        for f in in_lst:
            if mode != 1:
                self.assertFragmentIn(response, f(hid, True))
            else:
                self.assertFragmentNotIn(response, f(hid, True))

            if mode != 2:
                self.assertFragmentIn(response, model(hid, f, True))
            else:
                self.assertFragmentNotIn(response, model(hid, f, True))

        for f in not_in_lst:
            self.assertFragmentNotIn(response, f(10, True))
            self.assertFragmentNotIn(response, model(hid, f, True))

    def check_filter(self, hid, url, in_lst, not_in_lst):
        self.check_filter_(hid, url, in_lst, not_in_lst, 0)
        self.check_filter_(hid, url, in_lst, not_in_lst, 1)
        self.check_filter_(hid, url, in_lst, not_in_lst, 2)

    def test_filter(self):
        '''
        проверяем, что через фильтр promo-type проходят только нужные офферы/модели
        и что filter-promo-or-discount при этом игнорируется
        '''
        for url in (
            'place=prime&hid=10&numdoc=20',
            'place=prime&hid=10&numdoc=20&promo-type=',
        ):
            self.check_filter(
                10,
                url,
                [offers_simple, offers_promocode, offers_n_plus_m, offers_gift, offers_flash_discount, offers_discount],
                [],
            )

        for url in (
            'place=prime&hid=10&filter-promo-or-discount=1',
            'place=prime&hid=10&promo-type=market',
            'place=prime&hid=10&filter-promo-or-discount=1&promo-type=market',
        ):
            self.check_filter(
                10,
                url,
                [offers_promocode, offers_n_plus_m, offers_gift, offers_flash_discount, offers_discount],
                [offers_simple],
            )

        for url in (
            'place=prime&hid=10&filter-promo-or-discount=1&promo-type=n-plus-m',
            'place=prime&hid=10&promo-type=n-plus-m',
        ):
            self.check_filter(
                10,
                url,
                [offers_n_plus_m],
                [offers_simple, offers_promocode, offers_gift, offers_flash_discount, offers_discount],
            )

        for url in (
            'place=prime&hid=10&filter-promo-or-discount=1&promo-type=flash-discount',
            'place=prime&hid=10&promo-type=flash-discount',
        ):
            self.check_filter(
                10,
                url,
                [offers_flash_discount],
                [offers_simple, offers_promocode, offers_n_plus_m, offers_gift, offers_discount],
            )

        for url in (
            'place=prime&hid=10&filter-promo-or-discount=1&promo-type=discount',
            'place=prime&hid=10&promo-type=discount',
        ):
            self.check_filter(
                10,
                url,
                [offers_discount, offers_flash_discount],  # 'flash discount' is discount
                [offers_simple, offers_promocode, offers_n_plus_m, offers_gift],
            )

        for url in (
            'place=prime&hid=10&filter-promo-or-discount=1&promo-type=discount,gift-with-purchase,promo-code',
            'place=prime&hid=10&promo-type=discount,gift-with-purchase,promo-code',
        ):
            self.check_filter(
                10,
                url,
                [offers_discount, offers_flash_discount, offers_promocode, offers_gift],
                [offers_simple, offers_n_plus_m],
            )

    def test_filter_checked(self):
        '''
        проверяем, что флажок выставлен только у выбранных фильтров и только у тех, для которых есть офферы
        '''
        # [offers_simple, offers_promocode, offers_flash_discount, offers_discount]
        response = self.report.request_json('place=prime&hid=16&promo-type=n-plus-m,promo-code')

        # selected && exist
        self.assertFragmentIn(
            response,
            {
                'id': 'promo-code',
                'found': 2,
                "checked": True,
            },
        )

        # exist but not selected
        self.assertFragmentIn(
            response,
            {
                'id': 'discount',
                "found": 4,
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'id': 'discount',
                "checked": True,
            },
        )

        # selected but not exist
        self.assertFragmentIn(
            response,
            {
                'id': 'n-plus-m',
                "found": 0,
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'id': 'n-plus-m',
                "checked": True,
            },
        )

        # not selected and not exist
        self.assertFragmentIn(
            response,
            {
                'id': 'gift-with-purchase',
                "found": 0,
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'id': 'gift-with-purchase',
                "checked": True,
            },
        )

        response = self.report.request_json('place=prime&hid=16&promo-type=market&touch=1')
        # selected && exist
        self.assertFragmentIn(
            response,
            {
                'id': 'promo-code',
                'found': 2,
                "checked": True,
            },
        )

        # selected && exist
        self.assertFragmentIn(
            response,
            {
                'id': 'discount',
                'found': 4,
                "checked": True,
            },
        )

        # selected but not exist
        self.assertFragmentIn(
            response,
            {
                'id': 'n-plus-m',
                "found": 0,
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'id': 'n-plus-m',
                "checked": True,
            },
        )

        # selected but not exist
        self.assertFragmentIn(
            response,
            {
                'id': 'gift-with-purchase',
                "found": 0,
            },
        )
        self.assertFragmentNotIn(
            response,
            {
                'id': 'gift-with-purchase',
                "checked": True,
            },
        )

    def test_force_promo_offer(self):
        url = 'place=prime&numdoc=20&hid=10&text=philips&promo-type=market'
        # постой слeчай
        response = self.report.request_json(url)
        self.assertFragmentIn(response, offers_promocode(10, True))  # проверяем, что в выдаче есть оффер
        self.assertFragmentIn(response, model(10, offers_promocode, True))  # и модель к нему
        self.assertFragmentIn(response, offers_discount(10, True))  # другой промо оффер
        self.assertFragmentIn(response, model(10, offers_discount, True))  # и модель к нему

        # запрещаем offers_promocode
        shift = shift_map[offers_promocode]
        self.dynamic.market_dynamic.disabled_market_sku += [
            DynamicMarketSku(supplier_id=shift, shop_sku=str(offerid(10, shift))),
        ]
        response = self.report.request_json(url)
        self.assertFragmentNotIn(response, offers_promocode(10, True))  # оффера больше нет
        self.assertFragmentNotIn(response, model(10, offers_promocode, True))  # модели больше нет - она не onstock
        self.assertFragmentIn(response, offers_discount(10, True))  # другой промо оффер остался
        self.assertFragmentIn(response, model(10, offers_discount, True))  # и модель осталась

        # офферов больше нет в выдаче, потому что коллапсинг
        response = self.report.request_json(url + '&allow-collapsing=1')
        self.assertFragmentNotIn(response, offers_promocode(10, True))
        self.assertFragmentNotIn(response, offers_discount(10, True))
        self.assertFragmentNotIn(response, model(10, offers_promocode, True))  # модели больше нет - она не onstock
        self.assertFragmentIn(response, model(10, offers_discount, True))

        # пропускаем через фильтр только офферы
        response = self.report.request_json(url + '&force-promo-offer-search=1')
        self.assertFragmentNotIn(response, offers_promocode(10, True))  # этот забанен
        self.assertFragmentIn(response, offers_discount(10, True))
        self.assertFragmentNotIn(response, model(10, offers_promocode, True))  # моделей нет
        self.assertFragmentNotIn(response, model(10, offers_discount, True))  # моделей нет

        # а теперь фильтрацию проходят только офферы
        # но потом - коллапсинг
        response = self.report.request_json(url + '&allow-collapsing=1&force-promo-offer-search=1')
        self.assertFragmentNotIn(response, offers_promocode(10, True))  # офферов нет
        self.assertFragmentNotIn(response, offers_discount(10, True))  # офферов нет
        self.assertFragmentNotIn(response, model(10, offers_promocode, True))  # оффер для этой модели забанен
        self.assertFragmentIn(response, model(10, offers_discount, True))  # другая модель осталась

    def test_skip_group_model(self):
        url = 'place=prime&numdoc=20&hid=10&text=philips&promo-type=market'
        # постой слeчай
        response = self.report.request_json(url)
        self.assertFragmentIn(response, offers_gift(10, True))  # проверяем, что в выдаче есть оффер
        self.assertFragmentIn(response, model(10, offers_gift, True))  # и модель к нему
        self.assertFragmentIn(response, offers_promocode(10, True))  # другой промооффер
        self.assertFragmentIn(response, model(10, offers_promocode, True))  # и модель к нему

        # запрещаем групповые
        response = self.report.request_json(url + '&skip-group-model-offers=1')
        self.assertFragmentNotIn(response, offers_gift(10, True))  # оффера больше нет
        self.assertFragmentNotIn(response, model(10, offers_gift, True))  # модели - тоже
        self.assertFragmentIn(response, offers_promocode(10, True))  # другой промо оффер остался
        self.assertFragmentIn(response, model(10, offers_promocode, True))  # и модель осталась

    def test_blue_cashback_filter_in_promotype(self):
        # Проверяем отображение кешбэка в фильтре "promo-type"
        # фильтрация уже проверялась подробнее в test_blue_promo_blue_cashback
        TEST_HID = 18
        url = 'place=prime&numdoc=20&hid={}&text=philips&perks=yandex_cashback'.format(TEST_HID)
        rearr = '&rearr-factors=market_enable_cashback_in_promo_type_filter={}'

        fragment = {
            'id': 'blue-cashback',
            'checked': Absent(),
            'found': 2,
        }

        # новое значение выводим только при market_enable_cashback_in_promo_type_filter = 1
        for flag, show in [(None, True), (0, False), (1, True)]:
            request = url
            if flag is not None:
                request += rearr.format(flag)
            response = self.report.request_json(request)
            if show:
                self.assertFragmentIn(response, fragment)
            else:
                self.assertFragmentNotIn(response, fragment)

        # быстро проверим фильтрацию
        url += rearr.format(1)
        request = url + "&promo-type=blue-cashback"
        response = self.report.request_json(request)

        # если фильтрация включена - проверяем checked
        fragment['checked'] = True
        self.assertFragmentIn(response, fragment)

        # оффер с кешбэком должен быть в выдаче (вместе с моделью)
        self.assertFragmentIn(response, offers_cashback(TEST_HID, True))
        self.assertFragmentIn(response, model(TEST_HID, offers_cashback, True))

        # оффер без кешбэка будет отфильтрован (вместе с моделью)
        self.assertFragmentNotIn(response, offers_promocode(TEST_HID, True))
        self.assertFragmentNotIn(response, model(TEST_HID, offers_promocode, True))

    def test_hide_blue_cashback_filter(self):
        # Проверяем, что по параметру hide_plus_subscriptions
        # скрывается кешбек в фильтре по промотипу
        TEST_HID = 18
        url = 'place=prime&numdoc=20&hid={}&text=philips&perks=yandex_cashback'.format(TEST_HID)
        param = '&hide_plus_subscriptions={}'

        fragment = {
            'id': 'blue-cashback',
            'checked': Absent(),
            'found': 2,
        }
        for should_hide in (0, 1):
            request = url + param.format(should_hide)
            response = self.report.request_json(request)
            if should_hide:
                self.assertFragmentNotIn(response, fragment)
            else:
                self.assertFragmentIn(response, fragment)


if __name__ == '__main__':
    main()
