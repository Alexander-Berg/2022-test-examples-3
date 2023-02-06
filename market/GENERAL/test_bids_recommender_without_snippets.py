#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import main
import test_bids_recommender


class T(test_bids_recommender.T):
    @classmethod
    def beforePrepare(cls):
        cls.settings.disable_snippet_request = True


if __name__ == '__main__':
    main()
