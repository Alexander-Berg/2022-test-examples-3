# coding: utf-8

import pytest
import market.idx.pylibrary.mindexer_core.multitasking.multitasking as multitasking
from market.idx.marketindexer.marketindexer.build_mass_index import MassIndexBuilder, GoodsMassIndexBuilder

from hamcrest import assert_that, has_length


# Хотим сравнивать словари без учёта порядка следования ключей и списки без учёта порядка следования элементов
def assert_objects_are_equal(obj1, obj2):
    def ordered(obj):
        if isinstance(obj, dict):
            return sorted((k, ordered(v)) for k, v in obj.items())
        if isinstance(obj, list):
            return sorted(ordered(x) for x in obj)
        else:
            return obj

    obj_1_ordered = ordered(obj1)
    obj_2_ordered = ordered(obj2)
    assert obj_1_ordered == obj_2_ordered


def test_create_tasks_0():
    task_map = {
        '1': ['2', '3'],
        '2': ['4'],
        '4': [],
        '3': []
    }

    expected_list = [
        {'1': [{'2': [{'4': None}]},
               {'3': None}]},
        {'2': [{'4': None}]},
        {'3': None},
        {'4': None}
    ]

    def task_build(task, dependencies=None):
        return {task: dependencies}

    tasks = multitasking.create_tasks(task_map, task_build, filtered_tasks=['1', '2'])
    assert_objects_are_equal(tasks, expected_list)


def test_create_tasks_postprocess_1():
    task_map = {
        '1': ['2postprocess', '3'],
        '2': ['4'],
        '4': [],
        '3': [],
    }

    postprocess_tasks_map = {
        '2': '2postprocess',
        '3': '3postprocess'
    }

    # Процесс выполнения теперь: 3, 3postprocess, 4, 2, 2postprocess, 1
    t4 = {'4': None}
    t3 = {'3': None}
    t3post = {'3postprocess': [t3]}
    t2 = {'2': [t4]}
    t2post = {'2postprocess': [t2]}
    t1 = {'1': [t2post, t3post]}

    expected_list = [t1, t2post, t2, t3post, t3, t4]

    def task_build(task, dependencies=None):
        return {task: dependencies}

    tasks = multitasking.create_tasks(task_map, task_build, postprocess_tasks_map)
    assert_objects_are_equal(tasks, expected_list)


def test_create_tasks_postprocess_0():
    task_map = {
        '2': []
    }

    postprocess_tasks_map = {
        '2': '2postprocess',
    }

    t2 = {'2': None}
    t2post = {'2postprocess': [t2]}

    expected_list = [t2post, t2]

    def task_build(task, dependencies=None):
        return {task: dependencies}

    tasks = multitasking.create_tasks(task_map, task_build, postprocess_tasks_map)
    assert_objects_are_equal(tasks, expected_list)


def test_create_tasks_postprocess_2():
    task_map = {
        '3': []
    }

    postprocess_tasks_map = {
        '2': '2postprocess',
    }

    t3 = {'3': None}
    expected_list = [t3]

    def task_build(task, dependencies=None):
        return {task: dependencies}

    tasks = multitasking.create_tasks(task_map, task_build, postprocess_tasks_map)
    assert_objects_are_equal(tasks, expected_list)


@pytest.mark.parametrize(
    "pipeline, feature_use_make_vcluster_pictures",
    [
        [MassIndexBuilder, True],
        [GoodsMassIndexBuilder, True],
        [MassIndexBuilder, False],
        [GoodsMassIndexBuilder, False],
    ]
)
def test_create_tasks_to_build(pipeline, feature_use_make_vcluster_pictures):
    """Проверяем что нет 'рекурсивных' зависимостей.
    """
    class Config(object):
        def __init__(self, feature_use_make_vcluster_pictures):
            self.feature_use_make_vcluster_pictures = feature_use_make_vcluster_pictures
            self.separate_cpc = True
            self.first_cpc = None
            self.panther_in_yt = True

    def name(target):
        if isinstance(target, tuple):
            return target[0].__name__ + '_' + '_'.join([arg for arg in target[1]])
        else:
            return target.__name__

    config = Config(feature_use_make_vcluster_pictures=feature_use_make_vcluster_pictures)
    builder = pipeline(config=config, generation_name='1', half_mode=False, scale_mode=False, nparts=1)
    task_map = builder.create_full_task_map()
    multitasking.create_tasks(task_map)
    alldeps = set()
    for deps in task_map.values():
        for dep in deps:
            alldeps.add(dep)
    widow_targets = []
    for target in task_map.keys():
        if target != builder.all_ok:
            if target not in alldeps:
                widow_targets.append(name(target))
    assert_that(widow_targets, has_length(0))
