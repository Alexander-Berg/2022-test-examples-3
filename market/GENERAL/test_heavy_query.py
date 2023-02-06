#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main


# https://st.yandex-team.ru/MARKETOUT-20018
class T(TestCase):

    # https://st.yandex-team.ru/MARKETOUT-21787

    def test_rgb_only_fail(self):
        for arg in ["green", "red", "blue", "blue_with_green"]:
            response = self.report.request_json(
                'place=prime&pp=18&rids=0&rgb={}&rearr-factors=market_metadoc_search=no'.format(arg), strict=False
            )
            self.assertFragmentIn(response, {"error": {"code": "EMPTY_REQUEST", "message": "Request is empty"}})


if __name__ == '__main__':
    main()
