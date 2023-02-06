#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import Greater
from unittest import skip


class T(TestCase):
    @classmethod
    def prepare(cls):
        """
        Явно отключаем DisableRandom
        """
        cls.settings.disable_random = 0

    @skip("flaky test")
    def test_timestamp_is_in_present(self):
        """
        При отключенном DisableRandom проверяем, что в Репорте живёт дата в настоящем
        Т.е. точно больше чем 03 ноября 2018 года
        """
        response = self.report.request_json('place=prime&debug=1&text=test')
        self.assertFragmentIn(
            response, {'debug': {'brief': {'timestamp': Greater(1541234567890123)}}}  # 03 Nov 2018 08:42:47.890123
        )


if __name__ == '__main__':
    main()
