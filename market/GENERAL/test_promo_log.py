#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa
from datetime import datetime

from core.testcase import (
    main,
    TestCase,
)
from core.types import Model, MnPlace, Promo, PromoType, Shop
from core.types.offer_promo import PromoBlueCashback
from core.types.sku import (
    BlueOffer,
    MarketSku,
)
from market.pylibrary.const.offer_promo import MechanicsPaymentType


DEFAULT_FEED_ID = 777
DEFAULT_SHOP_ID = 7770


def build_ware_md5(offerid):
    insert = offerid.ljust(14, "_")
    return "Sku{}Goleg".format(insert)


class _Shops(object):
    shop_pr = Shop(
        fesh=DEFAULT_SHOP_ID,
        datafeed_id=DEFAULT_FEED_ID,
        priority_region=213,
        supplier_type=Shop.THIRD_PARTY,
        blue=Shop.BLUE_REAL,
    )


def create_blue_offer(offerid, price, ts, randx, promo=None, fesh=DEFAULT_SHOP_ID, feed=DEFAULT_FEED_ID):
    params = dict(
        waremd5=build_ware_md5(offerid),
        price=price,
        fesh=fesh,
        feedid=feed,
        ts=ts,
        randx=randx,
        promo=promo,
        offerid='shop_sku_{}'.format(offerid),
    )

    return BlueOffer(**params)


class _Offers(object):
    offers = [create_blue_offer('offer{}'.format(i), ts=100 + i, randx=10 - i, price=100) for i in range(10)]


class _Models(object):
    models = [Model(hyperid=400 + i, ts=400 + i, hid=1000) for i in range(10)]


class _Mskus(object):
    mskus = [
        MarketSku(
            sku=100900 + i,
            blue_offers=[_Offers.offers[i]],
            hyperid=400 + i,
            ts=10 + i,
            randx=10 - i,
            title="покрышка с острыми шипами",
        )
        for i in range(10)
    ]


OLD_DATETIME = datetime(1971, 1, 1)


class _DirectPromos(object):
    promo_blue_promocode_pr_inactive = Promo(
        promo_type=PromoType.PROMO_CODE,
        promo_code='promocode_1_text',
        discount_value=25,
        discount_currency='RUR',
        key='promocode_pr',
        url='promocode_pr.ru',
        mechanics_payment_type=MechanicsPaymentType.CPA,
        end_date=OLD_DATETIME,
    )

    promo_blue_promocode_pr_active = Promo(
        promo_type=PromoType.PROMO_CODE,
        promo_code='promocode_1_active',
        discount_value=25,
        discount_currency='RUR',
        key='promocode_pr_active',
        url='promocode_pr_active.ru',
        mechanics_payment_type=MechanicsPaymentType.CPA,
    )

    promo_cheapest_active = Promo(
        promo_type=PromoType.CHEAPEST_AS_GIFT,
        key='cheapest_pr_active',
    )

    promo_cashback_pr_active = Promo(
        promo_type=PromoType.BLUE_CASHBACK,
        key='cashback_pr_active',
        blue_cashback=PromoBlueCashback(
            share=0.02,
            version=10,
            priority=3,
        ),
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.disable_randx_randomize()

        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']

        cls.index.shops += [
            _Shops.shop_pr,
        ]

        cls.index.models += _Models.models

        cls.index.mskus += _Mskus.mskus

        active_promos = [_DirectPromos.promo_blue_promocode_pr_active for i in range(10)]

        inactive_promos = [_DirectPromos.promo_blue_promocode_pr_inactive for i in range(10)]

        cashback_promos = [_DirectPromos.promo_cashback_pr_active for i in range(10)]

        cheapest_promos = [_DirectPromos.promo_cheapest_active for i in range(10)]

        cls.index.promos += active_promos
        cls.index.promos += inactive_promos
        cls.index.promos += cashback_promos
        cls.index.promos += cheapest_promos

        for i in range(10):
            _Offers.offers[i].promo = [inactive_promos[i], active_promos[i], cashback_promos[i], cheapest_promos[i]]

        for i in range(10):
            cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 400 + i).respond(0.9 - 0.05 * i)
            cls.matrixnet.on_place(MnPlace.META_REARRANGE, 400 + i).respond(0.9 - 0.05 * i)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 440).respond(0.02)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 440).respond(0.02)

    def build_request(self, place, rearr_flags):
        request = "place={place}&rgb=blue&numdoc=100&regset=0&pp=18&yandexuid=1".format(place=place)
        request += '&perks=yandex_cashback'  # для кэшбека
        request += '&rearr-factors={}'.format(
            ";".join("{}={}".format(key, val) for key, val in rearr_flags.iteritems())
        )
        request += "&text=покрышка с острыми"
        return request

    def ask_report_and_check_promo_log_line_number(self, offers, promo_log_offers_to_write=None):
        # form rearr-flags
        flags = {
            "market_enable_multipromo": 1,
            "market_promo_quantity_limit": 999,
            "market_execution_stats_log": 1,
        }
        if promo_log_offers_to_write is not None:
            flags["market_promo_log_max_offers_per_request"] = promo_log_offers_to_write

        # build request and ask report
        req = self.build_request("prime", flags)
        self.report.request_json(req)

        # check offer number in log
        expected_offers = promo_log_offers_to_write if promo_log_offers_to_write is not None else 3
        self.promo_log_tskv.expect(reason='DeclinedByTimeItsOver').times(expected_offers)
        self.promo_log_tskv.expect(reason='Active').times(expected_offers)

        for i in range(len(offers)):
            if i < expected_offers:
                # 3 промо в логах на каждом оффере (промокод активный, промокод неактивный, 2=3)
                # Кешбек не логируем
                self.promo_log_tskv.expect(feed_id=DEFAULT_FEED_ID, offer_id=offers[i].offerid).times(3)
            else:
                self.promo_log_tskv.expect(feed_id=DEFAULT_FEED_ID, offer_id=offers[i].offerid).times(0)

    def test_promo_log_is_off(self):
        """
        Проверяем, что количество записей в логе правильное: при promo_log_offers_to_write=0 лог выключен и ничего не пишем
        """

        self.ask_report_and_check_promo_log_line_number(_Offers.offers, promo_log_offers_to_write=0)

    def test_promo_log_is_default(self):
        """
        Проверяем, что количество записей в логе правильное:
        при не заданном значении promo_log_offers_to_write пишется три оффера (но все промки для каждого оффера) на запрос
        """

        self.ask_report_and_check_promo_log_line_number(_Offers.offers)  # 3 is default for now

    def test_promo_log_is_custom_equal_1(self):
        """
        Проверяем, что количество записей в логе правильное:
        при заданном значении promo_log_offers_to_write=1 пишется один оффер (но все промки для оффера) на запрос
        """

        self.ask_report_and_check_promo_log_line_number(_Offers.offers, promo_log_offers_to_write=1)

    def test_promo_log_is_custom_equal_2(self):
        """
        Проверяем, что количество записей в логе правильное:
        при заданном значении promo_log_offers_to_write=2 пишется два оффера (но все промки для каждого оффера) на запрос
        """

        self.ask_report_and_check_promo_log_line_number(_Offers.offers, promo_log_offers_to_write=2)

    def test_promo_log_more_then_default(self):
        """
        Проверяем, что количество записей в логе правильное:
        при заданном значении promo_log_offers_to_write=5 пишется пять офферов (но все промки для каждого оффера) на запрос
        """

        self.ask_report_and_check_promo_log_line_number(_Offers.offers, promo_log_offers_to_write=5)


if __name__ == '__main__':
    main()
