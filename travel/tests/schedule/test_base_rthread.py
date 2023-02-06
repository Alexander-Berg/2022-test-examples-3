# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from future import standard_library
standard_library.install_aliases()
from travel.rasp.library.python.common23.tester.factories import create_thread
from travel.rasp.library.python.common23.tester.testcase import TestCase


class TestBaseRThread(TestCase):
    def test_path_cached(self):
        thread = create_thread(
            schedule_v1=[
                [None, 10],
                [20, 30],
                [40, None]
            ],
        )

        path = list(thread.path)

        with self.assertNumQueries(1):
            assert path == thread.path_cached
            assert path == thread.path_cached
            assert path == thread.path_cached
