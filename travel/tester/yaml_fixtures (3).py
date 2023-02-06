# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os
from collections import OrderedDict
from importlib import import_module

import pytest
from django.conf import settings

from library.python import resource

from travel.rasp.library.python.common23.tester import transaction_context
from travel.rasp.library.python.common23.tester.yaml_serializer import DeserializedObjectSaver, Deserializer
from travel.rasp.library.python.common23.utils.files.fileutils import get_project_relative_path


fixtures_stack = [{}]


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

    print('using yaml_fixture')
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

    nodes = OrderedDict()

    for fixture_name in fixtures:
        node = _load_from_resource(fixture_name, base_module)
        if node is None:
            node = _load_from_fs(fixture_name, base_path, base_dir, base_module)

        nodes[node.name] = node

    ordered_nodes = _organize_nodes(nodes)

    for node in ordered_nodes:
        if node.name in loaded_fixtures:
            continue

        print('deserializing', node.name)
        saver = DeserializedObjectSaver(node.name, loaded_fixtures)

        for do in node.deserializer:
            saver.save(do)

        result = saver.loaded_objects

        loaded_fixtures[node.name] = result

    return loaded_fixtures


def _load_from_fs(fixture_name, base_path, base_dir, base_module):
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
    print('Load fixture %s' % fixture)

    deserializer = Deserializer(open(os.path.join(settings.PROJECT_PATH, fixture)))

    abs_name = u'{}:{}'.format(module, filename)

    return FixtureNode(deserializer, abs_name)


def _load_from_resource(fixture_name, base_module):
    paths_to_module = [(fixture_name, base_module)]
    if ':' in fixture_name:
        module, filename = fixture_name.split(':')
        module_path = module.replace('.', '/')
        paths_to_module.extend([
            ('{module}/fixtures/{filename}'.format(module=module_path, filename=filename), module),
            ('{module}/{filename}'.format(module=module_path, filename=filename), module),
        ])
    for path, module in paths_to_module:
        content = resource.resfs_read(path)
        if content is not None:
            return FixtureNode(Deserializer(content), path.replace('/', '.'))
    return None


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

    nodelist = list(nodes.values())
    nodelist.sort(key=lambda n: n.level)

    return nodelist


def _add_node_levels(nodes):
    nodelist = list(nodes.values())
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

        nodelist = [n for n in nodelist if n.level is None]

        if enter_length == len(nodelist):
            raise Exception('There is unresolved dependencies among fixtures %s', list(nodes.keys()))

        level += 1
