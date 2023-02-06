# -*- coding: utf-8 -*-

from travel.avia.library.python.tester import transaction_context
from travel.avia.library.python.tester import factories
from travel.avia.library.python.tester.testcase import TestCase


class TestFactories(TestCase):
    def create_and_rollback(self, creation_function):
        atomic = transaction_context.enter_atomic()

        creation_function()

        transaction_context.rollback_atomic(atomic)

    def test_creation(self):
        self.create_and_rollback(factories.create_route)
        self.create_and_rollback(factories.create_thread)
        self.create_and_rollback(factories.create_station)
