# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.library.python.common23.tester import factories
from travel.rasp.library.python.common23.tester import transaction_context
from travel.rasp.library.python.common23.tester.testcase import TestCase


class TestFactories(TestCase):
    def create_and_rollback(self, creation_function):
        atomic = transaction_context.enter_atomic()

        creation_function()

        transaction_context.rollback_atomic(atomic)

    def test_creation(self):
        self.create_and_rollback(factories.create_route)
        self.create_and_rollback(factories.create_thread)
        self.create_and_rollback(factories.create_station)
