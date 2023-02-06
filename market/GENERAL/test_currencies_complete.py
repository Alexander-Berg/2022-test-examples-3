#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Currency, ExchangeRate


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    @classmethod
    def prepare_all_currencies(cls):
        cls.index.currencies += [
            Currency(
                name=Currency.USD,
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=0.016998),
                    ExchangeRate(fr=Currency.EUR, rate=1.055075),
                    ExchangeRate(fr=Currency.UAH, rate=0.037163),
                    ExchangeRate(to=Currency.BYR, rate=19126.0),
                    ExchangeRate(fr=Currency.KZT, rate=0.003156),
                ],
            ),
            Currency(
                name=Currency.EUR,
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=0.016131),
                    ExchangeRate(fr=Currency.USD, rate=0.9478),
                    ExchangeRate(fr=Currency.UAH, rate=0.035222),
                    ExchangeRate(to=Currency.BYR, rate=20153.0),
                    ExchangeRate(fr=Currency.KZT, rate=0.00299),
                ],
            ),
            Currency(
                name=Currency.UAH,
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=0.45738),
                    ExchangeRate(fr=Currency.USD, rate=26.9086),
                    ExchangeRate(fr=Currency.EUR, rate=28.3913),
                    ExchangeRate(fr=Currency.BYR, rate=0.00141),
                    ExchangeRate(fr=Currency.KZT, rate=0.084936),
                ],
            ),
            Currency(
                name=Currency.BYR,
                alias=Currency.BYN,
                rate_to_primary=10000.0,
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=325.27),
                    ExchangeRate(fr=Currency.USD, rate=19126.0),
                    ExchangeRate(fr=Currency.EUR, rate=20153.0),
                    ExchangeRate(fr=Currency.UAH, rate=711.4),
                    ExchangeRate(fr=Currency.KZT, rate=60.406),
                ],
            ),
            Currency(
                name=Currency.KZT,
                exchange_rates=[
                    ExchangeRate(fr=Currency.RUR, rate=5.38),
                    ExchangeRate(fr=Currency.USD, rate=316.81),
                    ExchangeRate(fr=Currency.EUR, rate=334.46),
                    ExchangeRate(fr=Currency.UAH, rate=11.79),
                    ExchangeRate(fr=Currency.BYR, rate=0.0166),
                ],
            ),
        ]

    def test_simple(self):
        """
        Ничего не проверяем в выдаче, проверяем только собственно старт репорта на сгенерированном currency_rates.xml
        """
        _ = self.report.request_json('place=currency_convert&currency-from=RUB&currency-to=RUB&currency-value=1')


if __name__ == '__main__':
    main()
