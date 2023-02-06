#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DeliveryBucket,
    DeliveryOption,
    GLParam,
    MarketSku,
    OfferDimensions,
    Payment,
    PickupBucket,
    PickupOption,
    RegionalDelivery,
    Vat,
)
from core.testcase import main
from core.matcher import Regex
import test_actual_delivery
import unittest


class _Offers(object):
    offer_for_bucket_fallback = BlueOffer(
        price=55,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='blue.offer.bucket.fallback',
        waremd5='SkuFb1Pri55-iLVm1Goleg',
        weight=6,
        dimensions=OfferDimensions(length=6, width=6, height=6),
    )
    offer_for_full_fallback = BlueOffer(
        price=56,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='blue.offer.full.fallback',
        waremd5='SkuFb2Pri56-iLVm1Goleg',
        weight=7,
        dimensions=OfferDimensions(length=7, width=7, height=7),
    )


class T(test_actual_delivery.T):
    @classmethod
    def beforePrepare(cls):
        cls.settings.use_saashub_delivery = True

    @classmethod
    def prepare(cls):
        super(T, cls).prepare()
        cls.index.delivery_buckets_saashub += cls.index.delivery_buckets
        cls.index.pickup_buckets_saashub += cls.index.pickup_buckets
        cls.index.new_pickup_buckets_saashub += cls.index.new_pickup_buckets
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=11001,
                dc_bucket_id=21001,
                fesh=1,
                carriers=[157],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[DeliveryOption(price=23, day_from=10, day_to=20, shop_delivery_price=5)],
                        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]
        cls.index.delivery_buckets_saashub.extend(cls.index.delivery_buckets[-1:])
        cls.index.pickup_buckets += [
            # pickup bucket that not in saashub
            PickupBucket(
                bucket_id=12002,
                dc_bucket_id=22002,
                fesh=1,
                carriers=[123],
                options=[PickupOption(outlet_id=2005, day_from=1, day_to=2, price=5)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=12001,
                dc_bucket_id=22001,
                fesh=1,
                carriers=[123],
                options=[PickupOption(outlet_id=2004, day_from=1, day_to=2, price=5)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # post bucket
            PickupBucket(
                bucket_id=13001,
                dc_bucket_id=23001,
                fesh=1,
                carriers=[201],
                options=[PickupOption(outlet_id=4002, day_from=1, day_to=1, price=6)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]
        cls.index.pickup_buckets_saashub += cls.index.pickup_buckets[-2:]
        cls.index.mskus += [
            MarketSku(
                title="blue offer for fallback",
                hyperid=1,
                sku=12345,
                waremd5='MskuFb1Pri55-LVm1Goleg',
                blue_offers=[
                    _Offers.offer_for_bucket_fallback,
                ],
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[888],
                pickup_buckets=[],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer for force fallback",
                hyperid=1,
                sku=12345,
                waremd5='MskuFb2Pri56-LVm1Goleg',
                blue_offers=[
                    _Offers.offer_for_full_fallback,
                ],
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[11001],
                pickup_buckets=[12001, 13001],
                post_term_delivery=True,
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=6, width=6, height=6, length=6).respond(
            [21001], [22001], [140]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=7, width=7, height=7, length=7).respond(
            [21001], [22002], [23001]
        )

    def test_delivery_calculator_generation_for_warehouse(self):
        """not actual for saashub logic"""
        pass

    def test_unknown_warehouse_id(self):
        """not actual for saashub logic"""
        pass

    # fix this tests in near future
    @unittest.skip("in fix")
    def test_priority(self):
        pass

    @unittest.skip("in fix")
    def test_blue_nearest_outlet(self):
        pass

    @unittest.skip("in fix")
    def test_bucket_numbers_from_delivery_calc(self):
        pass

    @unittest.skip("in fix")
    def test_post_options_without_post_code(self):
        pass

    @unittest.skip("in fix")
    def test_preorder(self):
        pass

    @unittest.skip("in fix")
    def test_preorder_multioffer(self):
        pass

    @unittest.skip("in fix")
    def test_skip_post_options_calc(self):
        pass

    @unittest.skip("in fix")
    def test_weight_threshold(self):
        pass

    @unittest.skip("in fix")
    def test_white_delivery_calculator_trace_log(self):
        pass

    @unittest.skip("in fix")
    def test_white_force_delivery_calc_for_single_offer(self):
        pass

    @unittest.skip("in fix")
    def test_white_delivery_calc(self):
        pass

    @unittest.skip("in fix")
    def test_white_delivery_calc_cost_modifier(self):
        pass

    @unittest.skip("in fix")
    def test_white_delivery_calc_availability_modifier(self):
        pass

    @unittest.skip("in fix")
    def test_white_delivery_calc_time_modifier(self):
        pass

    @unittest.skip("in fix")
    def test_white_delivery_calc_pickup(self):
        pass

    @unittest.skip("in fix")
    def test_white_delivery_calc_pickup_cost_modifier(self):
        pass

    @unittest.skip("in fix")
    def test_white_delivery_calc_pickup_availability_modifier(self):
        pass

    @unittest.skip("in fix")
    def test_white_delivery_calc_pickup_time_modifier(self):
        pass

    @unittest.skip("in fix")
    def test_white_delivery_weight_tariffs(self):
        pass

    @unittest.skip("in fix")
    def test_dsbs_delivery_not_priority_region(self):
        pass

    @unittest.skip("in fix")
    def test_large_size_field(self):
        pass

    def test_delivery_calculator_trace_log(self):
        test_request = "place=actual_delivery&offers-list=Sku2Price55-iLVm1Goleg:1,Sku2Price50-iLVm1Goleg:1"
        self.report.request_json(test_request, headers={'X-Market-Req-ID': "abc123"})
        self.external_services_trace_log.expect(
            target_host=self.delivery_calc.host_and_port(),
            request_id=Regex(r"abc123/\d+"),
            target_module="Delivery Calculator",
            request_method="/feedOffers",
            http_code=200,
            http_method="POST",
            retry_num=0,
            kv_in_weight=17,
            kv_in_length=53,
            kv_in_width=22,
            kv_in_height=16,
            kv_out_courier_buckets="3",
            kv_out_pickup_buckets="4",
            kv_out_post_buckets="7",
            kv_in_feed_id=-1,
            kv_in_program_type=4,
            kv_in_generation_id=2406,
            kv_in_warehouse_id=145,
        )

    def test_fallback_bucket_to_mmap(self):
        """
        Проверяем корректность работы фоллбека для отдельных бакетов
        """
        request = 'place=actual_delivery&offers-list=SkuFb1Pri55-iLVm1Goleg:1&rids=213&force-use-delivery-calc=1&pickup-options=raw&rearr-factors=rty_delivery_cart=2'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "weight": "6",
                "dimensions": ["6", "6", "6"],
                "delivery": {
                    "hasPickup": True,
                    "availableServices": [{"serviceId": 123, "isMarketBranded": True}],
                    "options": [
                        {
                            "serviceId": "157",
                            "price": {"value": "23"},
                        }
                    ],
                    "pickupOptions": [
                        {
                            "serviceId": 123,
                            "outlet": {"id": "2004"},
                            "price": {"value": "5"},
                        }
                    ],
                },
            },
        )

    def test_force_fallback_bucket_to_mmap(self):
        """
        Проверяем корректность работы фоллбека в случае когда все Pickup бакеты из saashub фильтруются
        """
        request = 'place=actual_delivery&offers-list=SkuFb2Pri56-iLVm1Goleg:1&rids=213&force-use-delivery-calc=1&pickup-options=raw&rearr-factors=rty_delivery_cart=2'
        tass_data = self.report.request_tass()
        _ = self.report.request_json(request)
        tass_data_new = self.report.request_tass()
        self.assertEqual(
            tass_data.get('fallback_delivery__cart_forced_dmmm', 0) + int(1),
            tass_data_new.get('fallback_delivery__cart_forced_dmmm', 0),
        )

    def test_delivery_options_delivery_days_unknown(self):
        """not actual for saashub logic"""
        pass


if __name__ == '__main__':
    main()
