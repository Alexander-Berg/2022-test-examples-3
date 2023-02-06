#!/usr/bin/env python
# -*- encoding: utf-8 -*-

from __future__ import absolute_import

import sys

from travel.avia.stat_admin.tester import runner

if __name__ == '__main__':

    args = sys.argv[1:]
    if not args:
        # если добавить --stat-reuse-db, то будет работать быстрее
        args = '-s -vv --tb=native'

    runner.run({}, args)
