# -*- coding: utf-8 -*-

import os
from importlib import import_module

from django.conf import settings

from travel.avia.admin.lib.fileutils import get_project_relative_path


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
