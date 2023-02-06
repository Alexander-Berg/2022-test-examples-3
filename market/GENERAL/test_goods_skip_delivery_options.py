#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main

from core.matcher import Contains, Not


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.disable_check_empty_output()
        cls.settings.report_subrole = 'goods'

    def test_skip_delivery_options(self):
        """
        Проверяем, что на базовый отправляются delivery опции для goods
        в debug->report->how->args
        """
        for (place, relevance) in zip(
            ["prime", "parallel", "parallel_prime", "productoffers"], ["main", "parallel", "main", "main"]
        ):
            request = 'place=' + place + '&text=offers&debug=da'

            if place == "productoffers":
                request += "&market-sku=1"
            elif place == "parallel_prime":
                request += "&numdoc=1&hr=json"

            response = self.report.request_json(request)

            # проверяем что все вхождения с нужным relevance содержат флаги на скип доставки
            self.assertFragmentNotIn(
                response,
                {
                    'how': [
                        {
                            'args': Not(
                                Contains(
                                    'enable_blue_check_deliverability_for_buybox: false',
                                    'skip_delivery_info_calculation: true',
                                    'skip_delivery_info_calculation_for_dsbs: true',
                                )
                            ),
                            'relevance': relevance,
                        }
                    ],
                },
            )


if __name__ == '__main__':
    main()
