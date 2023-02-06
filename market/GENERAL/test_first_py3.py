#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        pass

    def test_logbroker_got_log(self):  # timeout otherwise
        pass


if __name__ == '__main__':
    main()
