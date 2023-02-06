# -*- coding: utf-8 -*-
from travel.avia.stat_admin.lib.grouping import group_tree, group_tree_named


def test_group_tree_named():
    items = [
        {'x': 1, 'y': 3, 'z': 4},
        {'x': 2, 'y': 3, 'z': 5},
        {'x': 2, 'y': 3, 'z': 6},
        {'x': 2, 'y': 4, 'z': 6},
    ]
    e1, e2, e3, e4 = items
    assert group_tree_named(items, 'x.y') == {
        'x': {
            1: {
                'y': {
                    3: [e1],
                },
            },
            2: {
                'y': {
                    3: [e2, e3],
                    4: [e4],
                },
            },
        },
    }


def test_group_tree():
    items = [
        {'x': 1, 'y': 3, 'z': 4},
        {'x': 2, 'y': 3, 'z': 5},
        {'x': 2, 'y': 3, 'z': 6},
        {'x': 2, 'y': 4, 'z': 6},
    ]
    e1, e2, e3, e4 = items
    assert group_tree(items, ['x', 'y']) == {
        1: {
            3: [e1],
        },
        2: {
            3: [e2, e3],
            4: [e4],
        },
    }
