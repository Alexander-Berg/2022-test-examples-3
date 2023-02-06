# coding: utf-8

import os
import sys
import unittest
from importlib import import_module

from django.test.testcases import TestCase as DjangoTestCase
from travel.avia.admin.lib.fileutils import get_project_relative_path

from travel.avia.admin.tests.lib.unittests import UnicodeAssertionError
from travel.avia.library.python.tester import yaml_fixtures


unittest.TestCase.failureException = UnicodeAssertionError


class TestCase(DjangoTestCase):
    class_fixtures = None
    _parent_loaded_fixtures = {}

    @classmethod
    def load_fixtures(cls, fixtures, loaded_fixtures=None):
        loaded_fixtures = loaded_fixtures or {}
        module = sys.modules[cls.__module__]

        return yaml_fixtures.load_fixtures(fixtures, module, loaded_fixtures)

    @classmethod
    def get_abs_fixture_name(cls, fname):
        if ':' in fname:
            return fname

        basepath = os.path.abspath(os.path.dirname(
            sys.modules[cls.__module__].__file__
        ))
        basepath = get_project_relative_path(os.path.join(basepath, 'fixtures'))
        module = basepath.replace('/', '.')
        return '{}:{}'.format(module, fname)

    def get_resource_names(self, fixture_name):
        if ':' in fixture_name:
            module, filename = fixture_name.split(':')
            resource_names = [
                '{module}.fixtures.{filename}'.format(module=module, filename=filename),
                '{module}.{filename}'.format(module=module, filename=filename)
            ]
        else:
            resource_names = [fixture_name]

        return resource_names

    def get_obj_from_fixture(self, fname, model, code):
        for name in self.get_resource_names(fname):
            fixture = self.loaded_fixtures.get(name)
            if fixture:
                return self.loaded_fixtures[name][model][code]

        abs_name = self.get_abs_fixture_name(fname)

        return self.loaded_fixtures[abs_name][model][code]

    @classmethod
    def setUpTestData(cls):
        if cls.fixtures:
            loaded_fixtures = yaml_fixtures.copy_fixtures(yaml_fixtures.fixtures_stack[-1])
            module = import_module(cls.__module__)

            loaded_fixtures = yaml_fixtures.load_fixtures(cls.fixtures, module, loaded_fixtures)

            yaml_fixtures.fixtures_stack.append(loaded_fixtures)

            cls.loaded_fixtures = loaded_fixtures

        from django.contrib.sites.models import Site
        Site.objects.clear_cache()

    @classmethod
    def tearDownClass(cls):
        if cls.fixtures:
            yaml_fixtures.fixtures_stack.pop()

            cls.loaded_fixtures = yaml_fixtures.fixtures_stack[-1]

        cls._rollback_atomics(cls.cls_atomics)

        super(DjangoTestCase, cls).tearDownClass()
