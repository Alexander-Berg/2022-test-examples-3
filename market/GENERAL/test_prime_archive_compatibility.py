#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import main
import test_prime


class T(test_prime.T):
    @classmethod
    def beforePrepare(cls):
        cls.settings.force_archive_mode = 'IndexOnly'


if __name__ == '__main__':
    main()
