#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

import time

from core.testcase import TestCase, main

# Тест предназначен для ручной проверки процедуры оффлайн тестирования
# экспериментов. Без специально установленных реарров всегда
# возвращает SUCCESS.


TIMEOUT = 700

TEST_FAIL_REARR = 'test_fail'
TEST_TIMEOUT_REARR = 'test_timeout'
TEST_REPORT_COREDUMP_REARR = 'test_coredump'


class T(TestCase):
    EXP = {}

    @classmethod
    def prepare(cls):
        for exp in cls.settings.default_search_experiment_flags:
            parts = exp.split('=', 1)
            value = None
            if len(parts) > 1:
                value = int(parts[1]) if parts[1].isdigit() else parts[1]
            cls.EXP[parts[0]] = value

    def test(self):
        if self.EXP.get(TEST_FAIL_REARR, False):
            assert False
        elif self.EXP.get(TEST_TIMEOUT_REARR, False):
            start_time = time.time()
            while True:
                time.sleep(0.1)
                if time.time() >= start_time + TIMEOUT:
                    break
        elif self.EXP.get(TEST_REPORT_COREDUMP_REARR, False):
            self.server.stop()


if __name__ == '__main__':
    main()
