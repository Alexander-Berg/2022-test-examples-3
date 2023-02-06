# coding: utf8

from __future__ import unicode_literals

from common.apps.train.models import PlacePriceRules
from common.tester.testcase import TestCase


class TestOwners(TestCase):
    def test_none(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte')

        assert rule.owners == []

    def test_whitespace(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', owner='   ')

        assert rule.owners == []

    def test_single_owner(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', owner='    ФПК ')

        assert rule.owners == ['ФПК']

    def test_multiple_owners(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', owner='  ФПК ,досс,бч, Тверск  ')

        assert rule.owners == ['ФПК', 'ДОСС', 'БЧ', 'ТВЕРСК']


class TestCheckOwner(TestCase):
    def test_no_owners(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte')

        assert not rule.check_owner('ФПК')

    def test_check_fail(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', owner='ФПК, досс')

        assert not rule.check_owner('БЧ')

    def test_check_success(self):
        rule = PlacePriceRules.objects.create(coach_type='platzkarte', owner='ФПК, досс, бч, Тверск')

        assert rule.check_owner('тверск')
