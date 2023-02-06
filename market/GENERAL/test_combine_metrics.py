#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    DeliveryBucket,
    DeliveryCalcFeedInfo,
    DeliveryOption,
    Offer,
    OfferDimensions,
    Region,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer


PLACE_COMBINE_WITH_RGB = "place=combine&rgb=green_with_blue&use-virt-shop=0"


class T(TestCase):
    @classmethod
    def prepare_filtered_data(cls):
        cls.settings.default_search_experiment_flags += ['enable_cart_split_on_combinator=0']
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        cls.index.shops += [
            Shop(
                fesh=1252,
                datafeed_id=11252,
                priority_region=213,
                name='dsbs партнер',
                cpa=Shop.CPA_REAL,
                client_id=1252,
            ),
            Shop(
                fesh=1254,
                datafeed_id=11254,
                priority_region=512,
                regions=[512],
                name='синий поставщик',
                cpa=Shop.CPA_REAL,
                client_id=1254,
                blue=Shop.BLUE_REAL,
            ),
        ]

        cls.index.mskus += [
            MarketSku(title="мску только для dsbs", hyperid=200333, sku=100450),
            MarketSku(
                title="мску синий + dsbs",
                hyperid=200444,
                sku=200450,
                blue_offers=[
                    BlueOffer(
                        fesh=1254,
                        feedid=11254,
                        offerid='Shop1_sku_00',
                        waremd5='Blue100______________Q',
                        price=10000,
                        delivery_buckets=[4343],
                        weight=1,
                        dimensions=OfferDimensions(length=1, width=1, height=1),
                    )
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title="Протухший dsbs без мску",
                fesh=1252,
                waremd5='DsbsWithoutMsku______g',
                price=100500,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[4240],
            ),
            Offer(
                title="Протухший dsbs с мску",
                hyperid=200333,
                sku=100450,
                fesh=1252,
                waremd5='DsbsWithMsku_________g',
                price=100500,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[4240],
            ),
            Offer(
                title="Протухший dsbs с мску (с байбокс аналогом)",
                hyperid=200444,
                sku=200450,
                fesh=1252,
                waremd5='DsbsWithMsku01_______g',
                price=100500,
                cpa=Offer.CPA_REAL,
                delivery_buckets=[4240],
            ),
        ]

        cls.index.regiontree += [
            Region(rid=213),
            Region(rid=512),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=4240,
                dc_bucket_id=4240,
                fesh=1252,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
            DeliveryBucket(
                bucket_id=4343,
                dc_bucket_id=4343,
                fesh=1254,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=512, options=[DeliveryOption(price=100, day_from=1, day_to=2)])],
            ),
        ]

        cls.index.delivery_calc_feed_info += [DeliveryCalcFeedInfo(feed_id=11254, generation_id=1, warehouse_id=145)]

        cls.delivery_calc.on_request_offer_buckets(weight=1, height=1, length=1, width=1, warehouse_id=145).respond(
            [4343], [], []
        )

    def update_emergency_flags(self, **kwargs):
        self.stop_report()
        self.emergency_flags.reset()
        self.emergency_flags.add_flags(**kwargs)
        self.emergency_flags.save()
        self.restart_report()

    def test_filtered_metrics(self):
        """
        Проверяем, что при протухании офферов заполняются метрики и ничего не ломается
        """
        self.update_emergency_flags(combinator_send_debug_metrics=0)

        tass_data_before = self.report.request_tass()

        request = (
            PLACE_COMBINE_WITH_RGB
            + '&rids=512&offers-list=DsbsWithoutMsku______g:2;cart_item_id:1,\
                                            DsbsWithMsku_________g:1;msku:100450;cart_item_id:2,\
                                            DsbsWithMsku01_______g:1;msku:200450;cart_item_id:3'
        )
        _ = self.report.request_json(request)

        tass_data_after = self.report.request_tass()
        for metric_name, val in [
            ['place_combine_filtered_dsbs_without_msku_dmmm', 1],
            ['place_combine_filtered_with_msku_no_buybox_dmmm', 1],
            ['place_combine_filtered_with_msku_dmmm', 1],
        ]:
            tass_stat_before = tass_data_before.get(metric_name)
            tass_stat_after = tass_data_after.get(metric_name)
            self.assertIn(metric_name, tass_data_after.keys())

            self.assertIsNone(tass_stat_before)
            self.assertEqual(tass_stat_after, val)

        # При повторном запросе старые метрики должны сохраниться
        request = (
            PLACE_COMBINE_WITH_RGB
            + '&rids=512&offers-list=DsbsWithMsku_________g:1;msku:100450;cart_item_id:2,\
                                            DsbsWithMsku01_______g:1;msku:200450;cart_item_id:3'
        )
        _ = self.report.request_json(request)

        tass_data_after = self.report.request_tass()
        for metric_name, val in [
            ['place_combine_filtered_dsbs_without_msku_dmmm', 1],
            ['place_combine_filtered_with_msku_no_buybox_dmmm', 2],
            ['place_combine_filtered_with_msku_dmmm', 2],
        ]:
            tass_stat_after = tass_data_after.get(metric_name)
            self.assertIn(metric_name, tass_data_after.keys())
            self.assertEqual(tass_stat_after, val)

        # Проверяем, что debug-метрики по умолчанию не пишутся
        self.assertNotIn('combinator_send_request_delay_hgram', tass_data_after)

    def test_debug_metrics(self):
        self.update_emergency_flags(combinator_send_debug_metrics=1)

        request = (
            PLACE_COMBINE_WITH_RGB
            + '&rids=512&offers-list=DsbsWithoutMsku______g:2;cart_item_id:1,\
                                            DsbsWithMsku_________g:1;msku:100450;cart_item_id:2,\
                                            DsbsWithMsku01_______g:1;msku:200450;cart_item_id:3'
        )
        _ = self.report.request_json(request)

        tass_data_after = self.report.request_tass()
        self.assertIn('combinator_send_request_delay_hgram', tass_data_after)


if __name__ == '__main__':
    main()
