# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.test_utils import TestCase
from search.martylib.yt_utils.manager import YtManager


class TestYtManager(TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manager = YtManager()
        cls.manager.set_project_path('default')

    def test_project_path_override_normal(self):
        self.assertEqual(self.manager.project_path, 'default')

        with self.manager.override_project_path('foo'):
            self.assertEqual(self.manager.project_path, 'foo')

            with self.manager.override_project_path('bar'):
                self.assertEqual(self.manager.project_path, 'bar')

            self.assertEqual(self.manager.project_path, 'foo')

        self.assertEqual(self.manager.project_path, 'default')

    def test_project_path_override_with_exception(self):
        self.assertEqual(self.manager.project_path, 'default')

        try:
            with self.manager.override_project_path('foo'):
                self.assertEqual(self.manager.project_path, 'foo')
                raise RuntimeError
        except RuntimeError:
            pass

        self.assertEqual(self.manager.project_path, 'default')
