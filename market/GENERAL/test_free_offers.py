#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import ClickType, DeliveryBucket, Offer, Outlet, PickupBucket, PickupOption, Shop
from core.testcase import TestCase, main


class T(TestCase):
    '''Тестируем, что магазины с тарифами FREE и FIX ничего не платят'''

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['market_filter_offers_with_model_without_sku=0']

        # online shop with free tariff and offline shop with fix tarif
        cls.index.shops += [
            Shop(
                fesh=100,
                priority_region=213,
                regions=[225],
                name='Good Internet Shop with free delivery',
                online=True,
                tariff="FREE",
            ),
            Shop(
                fesh=200, priority_region=213, regions=[225], name='Stone Shop Hangestone', online=False, tariff="FIX"
            ),
        ]

        cls.index.outlets += [
            Outlet(point_id=1, fesh=100, region=213, point_type=Outlet.FOR_PICKUP),
            Outlet(point_id=2, fesh=200, region=213, point_type=Outlet.FOR_STORE),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5100,
                fesh=100,
                carriers=[99],
                options=[PickupOption(outlet_id=1)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5200,
                fesh=200,
                carriers=[99],
                options=[PickupOption(outlet_id=2)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.offers += [
            Offer(hyperid=1000, fesh=100, title='free ukraine snusmumrik', pickup_buckets=[5100]),
            Offer(
                hyperid=1000,
                fesh=200,
                title='free offline snusmumrik',
                has_delivery_options=False,
                store=True,
                pickup_buckets=[5200],
            ),
        ]

    def test_search_offers_of_free_tariff_shop(self):
        '''делаем запрос с geo=0 (значение по умолчанию)
        При данном параметре оффлайн магазины отфильтровываются
        Оставшийся онлайн магазин показывается бесплатно
        т.е. клики на них записываются с bid=0
        '''

        for place, pp in [('defaultoffer', '200')]:
            response = self.report.request_xml(
                'place={}&pp={}&hyperid=1000&rids=213&show-urls=external'.format(place, pp)
            )
            self.assertFragmentIn(
                response,
                '''
                    <offer>
                      <raw-title>free ukraine snusmumrik</raw-title>
                    </offer>
                ''',
            )
            self.assertEqual(response.count('<offer/>'), 1)

        # проверка для prime, productoffers
        for place in ['prime', 'productoffers']:
            response = self.report.request_json('place={}&hyperid=1000&rids=213&show-urls=external'.format(place))
            self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": "free ukraine snusmumrik"}})
            self.assertEqual(response.count({"entity": "offer"}), 1)

        self.click_log.expect(ClickType.EXTERNAL, cp=0, cb=0).times(3)
        self.show_log.expect(click_price=0, bid=0).times(3)

    def test_search_offers_of_free_tariff_and_offline_shop(self):
        '''Делаем запрос с geo=1
        Находятся офферы из двух магазинов - онлайн и оффлайн - оба показываются бесплатно
        prime, productoffers вообще не поддерживают параметра geo - это нормально?
        '''
        response = self.report.request_json('place=geo&hyperid=1000&rids=213&geo=1&show-urls=external')
        self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": "free ukraine snusmumrik"}})
        self.assertFragmentIn(response, {"entity": "offer", "titles": {"raw": "free offline snusmumrik"}})

        # Both offline and free shops should get zero bid clicks
        self.click_log.expect(ClickType.EXTERNAL, cp=0, cb=0).times(2)
        self.show_log.expect(click_price=0, bid=0).times(2)


if __name__ == '__main__':
    main()
