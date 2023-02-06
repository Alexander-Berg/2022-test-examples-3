#!/usr/bin/env python
# -*- coding: utf-8 -*-

import __classic_import     # noqa
import market.report.cache_filler.mt.env as env


class T(env.CacheFillerSuite):
    def test_json(self):
        response = self.cache_filler.request_json('fill_cache?request=request')
        self.assertFragmentIn(response, {'status': 'ok'})


if __name__ == '__main__':
    env.main()
