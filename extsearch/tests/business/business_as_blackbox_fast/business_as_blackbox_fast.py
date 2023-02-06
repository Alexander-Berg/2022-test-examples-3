#!/usr/bin/env python
# -*- coding: utf-8 -*-

from extsearch.geo.base.geobasesearch.tests.business.test_cases.test_cases import BusinessTestcase

import shutil


##
# TEST CASE
#
class FastBusinessTestcase(BusinessTestcase):
    @classmethod
    def BuildBusinessIndex(cls):
        shutil.copytree('index', 'indexer-business/index')


del BusinessTestcase
