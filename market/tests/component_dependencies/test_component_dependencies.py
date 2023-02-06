# -*- coding: utf-8 -*-

import hashlib
import os
import shutil
import tempfile

import pytest

import yatest.common


def extract_includes_from_file(file_path, arcadia_path):
    includes = list()
    if not os.path.isfile(file_path):
        return includes
    with open(file_path, 'r') as f:
        for line in f:
            line = line.strip()
            if not line.startswith('#'):
                continue
            line = line[1:].strip()
            if not line.startswith('include'):
                continue
            line = line[len('include'):].strip()
            if line.startswith('"'):
                end_pos = line.find('"', 1)
            elif line.startswith('<'):
                end_pos = line.find('>', 1)
            else:
                continue
            if end_pos == -1:
                continue
            line = line[1:end_pos]
            if '/' not in line or '..' in line.split('/'):
                line = os.path.normpath(os.path.join(arcadia_path, line))
            includes.append(line)
    return includes


class TempDir(object):

    def __init__(self):
        self.temp_dir = None

    def __enter__(self):
        self.temp_dir = tempfile.mkdtemp()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        shutil.rmtree(self.temp_dir)

    @property
    def name(self):
        return self.temp_dir


# Сортировка нужна чтобы иметь предсказуемый порядок обхода графа (не зависимый от порядка инклудов или файлов на файловой системе).
# Иначе тест может флакать. После очистки списка исключений она будет не нужна.

class Digraph(object):
    def __init__(self):
        self.adjLists = dict()

    def adj(self, v):
        return sorted(self.adjLists[v])

    def addEdge(self, v, w):
        if v not in self.adjLists:
            self.adjLists[v] = list()
        if w not in self.adjLists:
            self.adjLists[w] = list()
        if w not in self.adjLists[v]:
            self.adjLists[v].append(w)

    def nodes(self):
        return sorted(self.adjLists.keys())


class DigraphCycleDetector(object):
    def __init__(self, g):
        self.g = g
        self.checked_nodes = set()
        self.nodes_on_path = list()

    def _dfs(self, v):
        if v in self.checked_nodes:
            return
        self.checked_nodes.add(v)
        self.nodes_on_path.append(v)
        for w in self.g.adj(v):
            if w in self.nodes_on_path:
                yield self.nodes_on_path[self.nodes_on_path.index(w):]
                continue
            if w in self.checked_nodes:
                continue
            for cycle in self._dfs(w):
                yield cycle
        del self.nodes_on_path[-1]

    def detect(self):
        for v in self.g.nodes():
            for cycle in self._dfs(v):
                yield cycle


def generate_component_dependency_graph(arcadia_rel_path):
    g = Digraph()
    root_path = yatest.common.source_path(arcadia_rel_path)
    for top_dir, _, file_names in os.walk(root_path):
        arcadia_path = os.path.join(arcadia_rel_path, top_dir[len(root_path) + 1:])
        current_component_path = arcadia_path
        while current_component_path.startswith(arcadia_rel_path):
            if os.path.isfile(os.path.join(root_path, current_component_path[len(arcadia_rel_path) + 1:], 'ya.make')):
                break
            current_component_path = os.path.dirname(current_component_path)
        assert current_component_path.startswith(arcadia_rel_path), 'Could not find ya.make for {}'.format(arcadia_path)

        for file_name in file_names:
            for include in extract_includes_from_file(os.path.join(top_dir, file_name), arcadia_path):
                component_path = os.path.dirname(include)
                while component_path.startswith(arcadia_rel_path):
                    if os.path.isfile(os.path.join(root_path, component_path[len(arcadia_rel_path) + 1:], 'ya.make')):
                        if component_path != current_component_path:
                            g.addEdge(current_component_path, component_path)
                        break
                    component_path = os.path.dirname(component_path)
    return g


class TestComponentDependencies(object):
    def test_extract_includes_from_file(self):
        with TempDir() as tmp_dir:
            module_dir = os.path.join(tmp_dir.name, 'module')
            os.makedirs(module_dir)
            test_file_path = os.path.join(module_dir, 'test.h')
            with open(test_file_path, 'w') as f:
                f.write('''
#include <market/report/library/module/include1.h>
#include "include2.h"
#include "../module2/include.h"  // comment
''')
            includes = extract_includes_from_file(test_file_path, 'market/report/library/module')
            assert len(includes) == 3
            assert includes[0] == 'market/report/library/module/include1.h'
            assert includes[1] == 'market/report/library/module/include2.h'
            assert includes[2] == 'market/report/library/module2/include.h'

    def test_cycle_detector(self):
        g = Digraph()
        g.addEdge('module1', 'module2')
        g.addEdge('module1', 'module3')
        g.addEdge('module2', 'module4')
        g.addEdge('module3', 'module2')
        d = DigraphCycleDetector(g)
        assert not list(d.detect())
        g.addEdge('module4', 'module1')
        d = DigraphCycleDetector(g)
        assert list(d.detect())

    def test_market_report_library_includes(self):
        REPORT_LIBRARY_PATH = 'market/report/library'
        RELEVANCE_LIBRARY_PATH = 'market/report/library/relevance/'
        root_path = yatest.common.source_path(REPORT_LIBRARY_PATH)
        for top_dir, _, file_names in os.walk(root_path):
            arcadia_path = os.path.join(REPORT_LIBRARY_PATH, top_dir[len(root_path) + 1:])
            for file_name in file_names:
                for include in extract_includes_from_file(os.path.join(top_dir, file_name), arcadia_path):
                    arcadia_file_path = os.path.join(arcadia_path, file_name)
                    if (include.startswith('market/report/src/') or
                            (include.startswith(RELEVANCE_LIBRARY_PATH) and not arcadia_file_path.startswith(RELEVANCE_LIBRARY_PATH))):
                        # print >> sys.stderr, "('{}', '{}'),".format(include, arcadia_file_path)
                        assert False, 'Found forbidden dependency "{}" in "{}"'.format(include, arcadia_file_path)

    def test_market_library_includes(self):
        MARKET_LIBRARY_PATH = 'market/library'
        root_path = yatest.common.source_path(MARKET_LIBRARY_PATH)
        for top_dir, _, file_names in os.walk(root_path):
            arcadia_path = os.path.join(MARKET_LIBRARY_PATH, top_dir[len(root_path) + 1:])
            for file_name in file_names:
                for include in extract_includes_from_file(os.path.join(top_dir, file_name), arcadia_path):
                    if include.startswith('market/report/'):
                        arcadia_file_path = os.path.join(arcadia_path, file_name)
                        # print >> sys.stderr, "('{}', '{}'),".format(include, arcadia_file_path)
                        assert False, 'Found forbidden dependency "{}" in "{}"'.format(include, arcadia_file_path)

    dependency_cycles_testdata = [
        'market/report/library',
        'market/library',
    ]

    @pytest.mark.parametrize("arcadia_rel_path", dependency_cycles_testdata)
    def test_dependency_cycles(self, arcadia_rel_path):
        g = generate_component_dependency_graph(arcadia_rel_path)
        for dep_cycle in DigraphCycleDetector(g).detect():
            # print >> sys.stderr, "sorted([{}]),".format(', '.join("'{}'".format(x) for x in dep_cycle))
            assert False, 'Found component dependency cycle: {}'.format(' -> '.join(dep_cycle))

    def test_global_h_modifications(self):
        REFERENCE_GLOBAL_H_MD5 = '80e87f9595262ddd3c15e21977066b47'
        global_h_path = yatest.common.source_path('market/report/src/Global.h')
        md5 = hashlib.md5()
        with open(global_h_path, 'r') as f:
            md5.update(f.read())
        assert md5.hexdigest() == REFERENCE_GLOBAL_H_MD5, u'Не добавляйте новые методы в Global, вместо этого делайте новую библиотеку в report/library/global'
