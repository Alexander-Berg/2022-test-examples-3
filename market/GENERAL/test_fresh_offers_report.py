#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Const,
    Currency,
    GLParam,
    MnPlace,
    Model,
    Offer,
    Shop,
)

from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.use_fresh_base_report = True

        cls.emergency_flags.add_flags(report_fresh_base_config_enabled=1)

        cls.fresh_base_index.fresh_offers = [
            Offer(
                title='Fresh offer 1',
                hid=Const.ROOT_HID,
                fesh=123,
                glparams=[
                    GLParam(param_id=2312, value=2),
                ],
                ts=1001,
            ),
            Offer(
                title='New item 2',
                hid=Const.ROOT_HID,
                fesh=123,
                feedid=222,
                offerid=2221,
                ts=22211,
            ),
            Offer(
                title='iphone 11',
                hid=Const.ROOT_HID,
                fesh=123,
                hyperid=777,
                ts=1002,
            ),
            Offer(
                hyperid=8001,
                hid=Const.ROOT_HID,
                fesh=123,
                feedid=333,
                offerid=3331,
                ts=33311,
            ),
            Offer(
                title='With brand new shop',
                hid=Const.ROOT_HID,
                fesh=1234,
                glparams=[
                    GLParam(param_id=2312, value=2),
                ],
                ts=100111,
            ),
        ]

        cls.base_index.offers = [
            Offer(
                title='Old offer 1',
                hid=Const.ROOT_HID,
                fesh=123,
                glparams=[
                    GLParam(param_id=2312, value=1),
                ],
                ts=1003,
            ),
            Offer(
                title='Old item 2',
                hid=Const.ROOT_HID,
                fesh=123,
                feedid=222,  # совпадает с оффером из fresh коллекции
                offerid=2221,  # совпадает с оффером из fresh коллекции
                ts=22212,  # уникальный
            ),
            Offer(
                hyperid=8001,
                hid=Const.ROOT_HID,
                fesh=123,
                feedid=333,
                offerid=3331,
                ts=33312,
            ),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 33311).respond(0.005)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 33312).respond(0.003)

        cls.base_index.models += [
            Model(hyperid=777, hid=Const.ROOT_HID, title='iphone'),
            Model(hyperid=8001, hid=Const.ROOT_HID, title='macbook'),
        ]

        cls.fresh_base_index.shops += [
            Shop(fesh=123, priority_region=213, regions=[225], currency=Currency.RUR),
            Shop(fesh=1234, priority_region=213, regions=[225], currency=Currency.RUR),
        ]
        cls.base_index.shops += [
            Shop(fesh=123, priority_region=213, regions=[225], currency=Currency.RUR),
        ]
        cls.index.shops += [
            Shop(fesh=123, priority_region=213, regions=[225], currency=Currency.RUR),
        ]

    def update_flags(self, **kwargs):
        self.stop_report()
        self.emergency_flags.reset()
        self.emergency_flags.add_flags(**kwargs)
        self.emergency_flags.save()
        self.restart_report()

    def test_prime_search(self):
        """
        С флагом use_external_fresh_offers=0 нет походов в коллекцию SHOP_FRESH,
        а с use_external_fresh_offers=1 – есть, но только на плейсе prime,
        если же use_external_fresh_offers=2, то походы тоже будут, но на всех плейсах.
        """

        # Ищем fresh, коллекция SHOP_FRESH выключена -> не находим ничего
        response = self.report.request_json('place=prime&text=fresh&debug=1&rearr-factors=use_external_fresh_offers=0')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 0,
                },
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response,
            {
                "SHOP_FRESH": {},
            },
        )
        self.assertFragmentIn(
            response,
            {
                "SHOP": {},
            },
        )

        # Ищем offer, коллекция SHOP_FRESH выключена -> находим только old offer
        response = self.report.request_json('place=prime&text=offer&debug=1&rearr-factors=use_external_fresh_offers=0')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {
                                'raw': 'Old offer 1',
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response,
            {
                "SHOP_FRESH": {},
            },
        )
        self.assertFragmentIn(
            response,
            {
                "SHOP": {},
            },
        )

        for exp_status in [1, 2]:
            # Ищем fresh, коллекция SHOP_FRESH работает -> находим только fresh offer
            response = self.report.request_json(
                'place=prime&text=fresh&debug=1&rearr-factors=use_external_fresh_offers={}'.format(exp_status)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'results': [
                            {
                                'entity': 'offer',
                                'titles': {
                                    'raw': 'Fresh offer 1',
                                },
                            },
                        ],
                    },
                },
                allow_different_len=False,
            )
            self.assertFragmentIn(
                response,
                {
                    "SHOP_FRESH": {},
                    "SHOP": {},
                },
            )

            # Ищем offer, коллекция SHOP_FRESH работает -> находим old offer и fresh offer
            response = self.report.request_json(
                'place=prime&text=offer&debug=1&rearr-factors=use_external_fresh_offers={}'.format(exp_status)
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 2,
                        'results': [
                            {
                                'entity': 'offer',
                                'titles': {
                                    'raw': 'Fresh offer 1',
                                },
                            },
                            {
                                'entity': 'offer',
                                'titles': {
                                    'raw': 'Old offer 1',
                                },
                            },
                        ],
                    },
                },
                allow_different_len=False,
            )
            self.assertFragmentIn(
                response,
                {
                    "SHOP_FRESH": {},
                    "SHOP": {},
                },
            )

    def test_fresh_merge(self):
        # Ищем оффер "item", коллекция SHOP_FRESH работает -> находим один "item" из двух дублей по feedId-offerId
        # Приоритет отдаётся офферу из основной коллекции
        # Сниппеты могут соответствовать любому из этих офферов, поскольку ключ у них получается одинаковый
        # Единственный способ различить их в тесте - читать отладочную информацию
        for q, ts, feed_id, offer_id, direct_offers_search in [
            ('place=prime&text=item', '22212', '222', '2221', True),
            ('place=productoffers&hyperid=8001', '33312', '333', '3331', False),
        ]:
            response = self.report.request_json(q + '&debug=1&rearr-factors=use_external_fresh_offers=2')
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'total': 1,
                        'results': [
                            {
                                'entity': 'offer',
                                'debug': {
                                    'properties': {'TS': ts},  # Выбран оффер из основной коллекции
                                },
                            },
                        ],
                    },
                },
                allow_different_len=False,  # Важное условие этого теста!
            )
            if direct_offers_search:
                self.assertFragmentIn(
                    response,
                    {
                        'SHOP_FRESH': {},
                        'SHOP': {},
                    },
                )
                self.assertFragmentIn(
                    response,
                    'offer (feedId, offerId)=({feed_id}, {offer_id}) is not unique'.format(
                        feed_id=feed_id, offer_id=offer_id
                    ),
                )

    def test_fresh_merge_default_offers(self):
        response = self.report.request_json(
            'place=productoffers&hyperid=8001&offers-set=defaultList&rearr-factors=use_external_fresh_offers=2'
            '&debug=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'debug': {
                                'properties': {'TS': '33312'},  # Выбран оффер из базовой-коллекции
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,  # Важное условие этого теста!
        )

    @classmethod
    def prepare_modelinfo(cls):
        cls.fresh_base_index.fresh_offers += [
            Offer(
                hid=Const.ROOT_HID,
                fesh=123,
                glparams=[
                    GLParam(param_id=2312, value=2),
                ],
                hyperid=1,
            ),
        ]

        cls.base_index.offers += [
            Offer(
                hid=Const.ROOT_HID,
                fesh=123,
                glparams=[
                    GLParam(param_id=2312, value=2),
                ],
                hyperid=1,
            ),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=Const.ROOT_HID, title='galaxy'),
        ]

    def test_modelinfo(self):
        for flag, results_count in ((0, 1), (1, 1), (2, 2)):
            response = self.report.request_json(
                'place=modelinfo&hyperid=1&rids=213&use-default-offers=1&rearr-factors=use_external_fresh_offers={flag}'.format(
                    flag=flag
                )
            )
            self.assertFragmentIn(response, {'search': {'results': [{'offers': {'count': results_count}}]}})

    def test_productoffers(self):
        """
        Проверяем, что на других плейсах флаг use_external_fresh_offers работает корректно
        (см. описание этого флага выше в тесте test_prime_search).
        """

        results_with_offer = {
            "results": [
                {
                    'entity': 'offer',
                    'titles': {
                        'raw': 'iphone 11',
                    },
                },
            ],
        }

        # Включаем фреш-коллекцию только для ограниченного числа плейсов
        response = self.report.request_json('place=productoffers&hyperid=777&rearr-factors=use_external_fresh_offers=1')
        self.assertFragmentNotIn(response, results_with_offer)

        # Включаем фреш-коллекцию на всех плейсах
        response = self.report.request_json('place=productoffers&hyperid=777&rearr-factors=use_external_fresh_offers=2')
        self.assertFragmentIn(response, results_with_offer)

    def test_safe_mode(self):
        """
        Проверяем, что при включенном безопасном режиме, походы во fresh-коллекцию отключаются
        """

        # Включаем safe-mode
        self.update_flags(enable_report_safe_mode=1)

        # Ищем fresh, коллекция SHOP_FRESH включена, но активирован safe-mode -> не находим ничего
        response = self.report.request_json('place=prime&text=fresh&debug=1&rearr-factors=use_external_fresh_offers=2')
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 0,
                },
            },
            allow_different_len=False,
        )
        self.assertFragmentNotIn(
            response,
            {
                "SHOP_FRESH": {},
            },
        )
        self.assertFragmentIn(
            response,
            {
                "SHOP": {},
            },
        )

    def test_new_shop(self):
        """
        Проверяем, что добавление нового магазина во фреш-коллекцию не ломает поисковую выдачу.
        Подразумевается, что этот магазин есть только во фреш-индексе, в мета-индексе он отсутствует.
        """

        response = self.report.request_json(
            'place=prime&text=brand+shop&debug=1&rearr-factors=use_external_fresh_offers=1'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 1,
                    'results': [
                        {
                            'entity': 'offer',
                            'titles': {
                                'raw': 'With brand new shop',
                            },
                        },
                    ],
                },
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
