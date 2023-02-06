#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        """
        Не указываем значение для disable_random
        Но указываем кастомный таймстамп
        """
        # cls.settings.disable_random = ?
        cls.settings.microseconds_for_disabled_random = 1234567890123456  # 13 Feb 2009 23:31:30.123456

    def test_timestamp_is_in_present(self):
        """
        При незаданном DisableRandom проверяем, что в Репорте живёт дата, которую мы задали
        """
        response = self.report.request_json('place=prime&debug=1&text=test')
        self.assertFragmentIn(
            response, {'debug': {'brief': {'timestamp': 1234567890123456}}}  # явно заданное нами значение
        )


if __name__ == '__main__':
    main()
