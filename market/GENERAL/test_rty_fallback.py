#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import time
from datetime import timedelta
from core.types import Offer, RtyOffer
from core.testcase import TestCase, main

PRICES_GENERATION_INTERVAL = 5


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.rty_qpipe = True
        cls.index.creation_time = int(time.time()) // 60 * 60
        cls.index.prices_generation_ts = cls.index.creation_time - cls._timestamp_delta(
            hours=PRICES_GENERATION_INTERVAL
        )
        cls.index.offers += [
            Offer(title='brick', fesh=39, feedid=26, offerid='aaa', price=400),
            Offer(title='beam', fesh=39, feedid=26, offerid='bbb', price=800),
        ]

    def _get_fallbacks_count(self, rty_fallback_signal):
        tass_data = self.base_search_client.request_tass(store_request=False)
        signal_name = 'rty_fallback_count_dmmm' if rty_fallback_signal else 'qprice_fallback_count_dmmm'
        return tass_data.get(signal_name, 0)

    def _check_price(self, text, price, fallback_expected, rty_fallback_interval=None, rty_fallback_signal=False):
        fallbacks_count = self._get_fallbacks_count(rty_fallback_signal)
        request = 'place=prime&text={}&rearr-factors=rty_qpipe=1'.format(text)
        if rty_fallback_interval:
            request = '{};rty_fallback_interval={}'.format(request, rty_fallback_interval)
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {'prices': {'currency': 'RUR', 'value': str(price)}})
        new_fallbacks_count = self._get_fallbacks_count(rty_fallback_signal)
        if fallback_expected:
            self.assertGreater(new_fallbacks_count, fallbacks_count, 'was not fallback than it is needed')
        else:
            self.assertEquals(
                fallbacks_count, self._get_fallbacks_count(rty_fallback_signal), 'was fallback than it is not needed'
            )

    @staticmethod
    def _timestamp_delta(**kwargs):
        return int(timedelta(**kwargs).total_seconds())

    def test_fallback(self):
        """Проверяем работу фолбека в случае, когда в rty-индексе документа нет"""

        # проверяем, что оффер берется из большого поколения, т.к. в rty его еще нет
        self._check_price('brick', 400, fallback_expected=True)

        # добавляем его в rty, но с меньшим таймстемпом, нежели у поколения, но с той же ценой
        self.rty.offers += [
            RtyOffer(
                feedid=26,
                offerid='aaa',
                price=400,
                modification_time=self.index.creation_time - self._timestamp_delta(seconds=1000),
            )
        ]
        # проверяем, что оффер берется из rty
        self._check_price('brick', 400, fallback_expected=False)

        # добавляем его в rty, но с бОльшим таймстемпом, нежели у поколения, но с той же ценой
        self.rty.offers += [
            RtyOffer(
                feedid=26,
                offerid='aaa',
                price=400,
                modification_time=self.index.creation_time + self._timestamp_delta(seconds=1000),
            )
        ]
        # проверяем, что оффер берется из rty
        self._check_price('brick', 400, fallback_expected=False)

        # добавляем его в rty, но с ценой в 6 раз больше
        self.rty.offers += [
            RtyOffer(
                feedid=26,
                offerid='aaa',
                price=2400,
                modification_time=self.index.creation_time + self._timestamp_delta(seconds=4000),
            )
        ]
        # проверяем, что оффер берется из rty
        self._check_price('brick', 2400, fallback_expected=False)

        # добавляем его в rty, но с ценой в 6 раз меньше
        self.rty.offers += [
            RtyOffer(
                feedid=26,
                offerid='aaa',
                price=66,
                modification_time=self.index.creation_time + self._timestamp_delta(seconds=5000),
            )
        ]
        # проверяем, что оффер берется из rty
        self._check_price('brick', 66, fallback_expected=False)

    def test_fallback_interval(self):
        """Проверяем работу фолбека RTY на поколение через заданный интервал с учетом возраста данных в поколении:
        если в RTY документ старше, чем min(prices_generation_ts, creation_time - rty_fallback_interval), то
        данные бурется из поколения.
        """
        rty_fallback_interval_hours = PRICES_GENERATION_INTERVAL + 1

        # оффер берется из большого поколения, т.к. в rty его еще нет
        self._check_price(
            'beam', 800, fallback_expected=True, rty_fallback_interval='{}h'.format(rty_fallback_interval_hours)
        )

        # оффер берется из поколения, т.к. документ RTY не попадает в минимальный интервал
        self.rty.offers += [
            RtyOffer(
                feedid=26,
                offerid='aaa',
                price=900,
                modification_time=self.index.creation_time
                - self._timestamp_delta(hours=rty_fallback_interval_hours, seconds=1),
            )
        ]
        self._check_price(
            'brick',
            400,
            fallback_expected=True,
            rty_fallback_interval='{}h'.format(rty_fallback_interval_hours),
            rty_fallback_signal=True,
        )

        # оффер берется из RTY, т.к. документ в RTY попадает в инетрвал
        self.rty.offers += [
            RtyOffer(
                feedid=26,
                offerid='aaa',
                price=901,
                modification_time=self.index.creation_time
                - self._timestamp_delta(hours=rty_fallback_interval_hours)
                + self._timestamp_delta(seconds=1),
            )
        ]
        self._check_price(
            'brick',
            901,
            fallback_expected=False,
            rty_fallback_interval='{}h'.format(rty_fallback_interval_hours),
            rty_fallback_signal=True,
        )

        rty_fallback_interval_hours = PRICES_GENERATION_INTERVAL - 1

        # оффер берется из поколения, т.к. документ RTY не попадает в инетрвал по возрасту цен
        self.rty.offers += [
            RtyOffer(
                feedid=26,
                offerid='aaa',
                price=902,
                modification_time=self.index.prices_generation_ts - self._timestamp_delta(seconds=1),
            )
        ]
        self._check_price(
            'brick',
            400,
            fallback_expected=True,
            rty_fallback_interval='{}h'.format(rty_fallback_interval_hours),
            rty_fallback_signal=True,
        )

        # оффер берется из RTY, т.к. документ в RTY попадает в инетрвал
        self.rty.offers += [
            RtyOffer(
                feedid=26,
                offerid='aaa',
                price=903,
                modification_time=self.index.prices_generation_ts + self._timestamp_delta(seconds=1),
            )
        ]
        self._check_price(
            'brick',
            903,
            fallback_expected=False,
            rty_fallback_interval='{}h'.format(rty_fallback_interval_hours),
            rty_fallback_signal=True,
        )

    @classmethod
    def prepare_price_fallback(cls):
        cls.index.offers += [
            Offer(
                title='cube',
                fesh=39,
                feedid=27,
                offerid='cub',
                price=100,
                price_timestamp=cls.index.creation_time - cls._timestamp_delta(hours=1),
            ),
        ]

    def test_price_fallback(self):
        """Проверяем фолбек, когда для оффера в поколении указана цена,
        отличная от времени создания индекса (т.е. приехала в доп. файле).
        """

        # Добавляем оффер в rty, mtime меньше, чем ts цены оффера в поколении.
        self.rty.offers += [
            RtyOffer(
                feedid=27,
                offerid='cub',
                price=200,
                modification_time=self.index.creation_time - self._timestamp_delta(hours=2),
            )
        ]
        # Проверяем, что оффер берется из поколения
        self._check_price('cube', 100, fallback_expected=True)

        # Добавляем оффер в rty с бОльшим ts, чем у цены оффера в поколении, но
        # меньшим, чем время создания поколения.
        self.rty.offers += [
            RtyOffer(
                feedid=27,
                offerid='cub',
                price=400,
                modification_time=self.index.creation_time
                - self._timestamp_delta(hours=1)
                + self._timestamp_delta(seconds=1),
            )
        ]
        # Проверяем, что оффер берется из rty
        self._check_price('cube', 400, fallback_expected=False)


if __name__ == '__main__':
    main()
