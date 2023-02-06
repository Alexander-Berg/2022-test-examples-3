# -*- coding: utf-8 -*-

__author__ = 'denkoren'

import unittest

from search.pumpkin.yalite_service.libyalite.services.images_search import ImagesSearch

import test_yaLiteSearchService


class TestWebSearch(test_yaLiteSearchService.TestYaLiteSearchService):
    ServiceClass = ImagesSearch

    @classmethod
    def setUpClass(cls):
        print "Testing ImagesSearch class:"

    @unittest.skip("Skipping test_data method testing for ImagesSearch service.")
    def test__test_data(self):
        pass
