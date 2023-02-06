# -*- coding: utf-8 -*-
from travel.avia.library.python.common.models.schedule import TrainSchedulePlan
from travel.avia.library.python.common.utils import environment
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.factories import create_thread


class TestTrainSchedulePlan(TestCase):
    def test_add_to_threads_empty(self):
        threads = [create_thread() for _ in range(3)]
        assert TrainSchedulePlan.add_to_threads(threads, environment.now()) == (None, None)
