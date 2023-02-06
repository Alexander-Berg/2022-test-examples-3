# -*- coding: utf-8 -*-

import os
import sys
import traceback
from importlib import import_module

import pytest
from django.apps import apps
from django.db import connection, transaction
from django.conf import settings

from travel.avia.library.python.common.utils.fileutils import get_project_relative_path

from travel.avia.library.python.tester import transaction_context
from travel.avia.library.python.tester.yaml_serializer import DeserializedObjectSaver, Deserializer


fixtures_stack = [{}]


@transaction.atomic
def load_default_fixtures():
    def load_fixture(fixture):
        print 'Loading %s' % fixture

        abs_name = get_project_relative_path(fixture)

        deserialized_objects = Deserializer(open(fixture))

        saver = DeserializedObjectSaver(abs_name)

        for do in deserialized_objects:
            saver.save(do)

        return saver.loaded_objects

    app_modules = []
    for app in get_apps(apps):
        if hasattr(app, '__path__'):
            # It's a 'models/' subpackage
            for path in app.__path__:
                app_modules.append((path, app.__name__))
        else:
            # It's a models.py module
            app_modules.append((app.__file__, app.__name__))

    default_test_fixtures = [
        (os.path.join(os.path.dirname(path), 'tests/default_fixtures'), name)
        for path, name in app_modules
    ]

    default_test_fixtures = filter(lambda p_n: os.path.exists(p_n[0]), default_test_fixtures)

    loaded_fixtures = {}

    try:
        with connection.constraint_checks_disabled():
            for basepath, name in default_test_fixtures:
                for dirpath, _dirnames, filenames in os.walk(basepath):
                    for fname in filenames:
                        if fname.endswith('.yaml'):
                            loaded_fixtures.setdefault(name + '.tests.default_fixtures', {})\
                                .update(load_fixture(os.path.join(dirpath, fname)))

    except (SystemExit, KeyboardInterrupt):
        raise
    except Exception:
        print 'Error loading fixtures'
        traceback.print_exc()
        sys.exit(1)


def get_apps(apps):
    """
    Returns a list of all installed modules that contain models.
    """
    app_configs = apps.get_app_configs()

    return [
        app_config.models_module for app_config in app_configs
        if app_config.models_module is not None
    ]


save_point_stack = []


def copy_fixtures(loaded_fixtures):
    from copy import copy

    new_loaded_fixtures = {}

    for module_name, fixtures_by_model in new_loaded_fixtures.items():
        new_fixtures_by_model = {}
        for model_name, object_by_ids in fixtures_by_model.items():
            new_fixtures_by_model[model_name] = copy(object_by_ids)

        new_loaded_fixtures[module_name] = new_fixtures_by_model

    return new_loaded_fixtures


@pytest.fixture(scope='class', autouse=True)
def class_fixtures(request):
    if not hasattr(request.cls, 'class_fixtures'):
        return

    atomic = transaction_context.enter_atomic()

    loaded_fixtures = copy_fixtures(fixtures_stack[-1])
    module = import_module(request.cls.__module__)
    loaded_fixtures = load_fixtures(request.cls.class_fixtures, module, loaded_fixtures)

    fixtures_stack.append(loaded_fixtures)
    save_point_stack.append(request.cls)

    request.cls.loaded_fixtures = loaded_fixtures

    def finalize():
        fixtures_stack.pop()
        assert save_point_stack.pop() == request.cls

        transaction_context.rollback_atomic(atomic)

    request.addfinalizer(finalize)


class FixtureNode(object):
    def __init__(self, deserializer, name):
        self.deserializer = deserializer
        self.children = []
        self.parents = []
        self.name = name
        self.level = None
        self.base_module = self.name.split(':')[0]

    def get_dependency_abs_name(self, dep_name):
        if ':' not in dep_name:
            return '{}:{}'.format(self.base_module, dep_name)
        else:
            return dep_name


def load_fixtures(fixtures, module, loaded_fixtures):
    if not fixtures:
        return loaded_fixtures

    base_path = os.path.abspath(os.path.dirname(module.__file__))
    base_path = get_project_relative_path(os.path.join(base_path, 'fixtures'))
    base_dir = os.path.dirname(base_path)
    base_module = base_path.replace('/', '.')

    nodes = {}

    for fixture_name in fixtures:
        if ':' in fixture_name:
            module, filename = fixture_name.split(':')

            if '/' in module or module == '..':
                module_dirpath = get_project_relative_path(os.path.join(base_dir, module))
                dirpath = os.path.join(module_dirpath, 'fixtures')
                module = module_dirpath.replace('/', '.')
            else:
                dirpath = os.path.abspath(os.path.dirname(import_module(module).__file__))
                dirpath = get_project_relative_path(os.path.join(dirpath, 'fixtures'))
        else:
            dirpath = base_path
            filename = fixture_name
            module = base_module

        fixture = os.path.join(dirpath, filename)
        print 'Load fixture %s' % fixture

        deserializer = Deserializer(open(os.path.join(settings.PROJECT_PATH, fixture)))

        abs_name = u'{}:{}'.format(module, filename)

        node = FixtureNode(deserializer, abs_name)

        nodes[node.name] = node

    ordered_nodes = _organize_nodes(nodes)

    for node in ordered_nodes:
        if node.name in loaded_fixtures:
            continue

        saver = DeserializedObjectSaver(node.name, loaded_fixtures)

        for do in node.deserializer:
            saver.save(do)

        result = saver.loaded_objects

        loaded_fixtures[node.name] = result

    return loaded_fixtures


def _organize_nodes(nodes):
    fname = None
    try:
        for node in nodes.values():
            for fname in node.deserializer.depends:
                dep_abs_name = node.get_dependency_abs_name(fname)

                nodes[dep_abs_name].children.append(node)
                node.parents.append(nodes[dep_abs_name])
    except KeyError:
        raise Exception('Dependence %s was not found' % fname)

    _add_node_levels(nodes)

    nodelist = nodes.values()
    nodelist.sort(key=lambda n: n.level)

    return nodelist


def _add_node_levels(nodes):
    nodelist = nodes.values()
    level = 1
    while nodelist:
        enter_length = len(nodelist)

        for index, node in enumerate(list(nodelist)):
            on_this_level = True
            for parent in node.parents:
                if parent.level is None:
                    on_this_level = False

            if on_this_level:
                node.level = level

        nodelist = filter(lambda n: n.level is None, nodelist)

        if enter_length == len(nodelist):
            raise Exception('There is unresolved dependencies among fixtures %s', nodes.keys())

        level += 1
