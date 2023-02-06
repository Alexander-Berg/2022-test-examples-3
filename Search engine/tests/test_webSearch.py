# -*- coding: utf-8 -*-

__author__ = 'denkoren'

from search.pumpkin.yalite_service.libyalite.services.web_search import WebSearch

import test_yaLiteSearchService


class TestWebSearch(test_yaLiteSearchService.TestYaLiteSearchService):
    ServiceClass = WebSearch

    @classmethod
    def setUpClass(cls):
        print "Testing WebSearch class:"
