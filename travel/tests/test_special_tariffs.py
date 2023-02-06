# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from django.db.models.query import QuerySet

from travel.rasp.library.python.common23.models.tariffs.special_tariffs import SpecialOfferDisplayManager, SpecialOffer
from travel.rasp.library.python.common23.tester.testcase import TestCase


class TestSpecialOfferDisplayManager(TestCase):
    def test_get_queryset(self):
        manager = SpecialOfferDisplayManager()
        manager.model = SpecialOffer
        qs = manager.get_queryset()
        assert isinstance(qs, QuerySet)
