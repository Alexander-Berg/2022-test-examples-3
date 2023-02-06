# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from builtins import range
from travel.rasp.library.python.common23.models.core.schedule.train_schedule_plan import TrainSchedulePlan
from travel.rasp.library.python.common23.tester.factories import create_thread
from travel.rasp.library.python.common23.tester.testcase import TestCase
from travel.rasp.library.python.common23.date import environment


class TestTrainSchedulePlan(TestCase):
    def test_add_to_threads_empty(self):
        threads = [create_thread() for _ in range(3)]
        assert TrainSchedulePlan.add_to_threads(threads, environment.now()) == (None, None)
