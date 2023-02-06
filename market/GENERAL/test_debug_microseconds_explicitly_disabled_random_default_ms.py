#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        """
        Явно указываем DisableRandom
        Не указываем таймстамп
        """
        cls.settings.disable_random = 1

    def test_timestamp_is_in_present(self):
        """
        При незаданном DisableRandom проверяем, что в Репорте живёт день рождения yuraaka
        """
        response = self.report.request_json('place=prime&debug=1&text=test')
        self.assertFragmentIn(
            response, {'debug': {'brief': {'timestamp': 488419200111000}}}  # дефолтное значение в LITE
        )


if __name__ == '__main__':
    main()
