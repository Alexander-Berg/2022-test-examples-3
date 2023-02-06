"""
    Test for Miscellaneous functions
"""

import pytest

from search.mon.wabbajack.libs.utils import misc


@pytest.mark.parametrize(
    'args', [
        {
            'd': {
                'level1': 'level1_value'
            },
            'find': 'level1',
            'expected': 'level1_value'
        },
        {
            'd': {
                'level1': {
                    'level2': 'level2_value'
                }
            },
            'find': 'level2',
            'expected': 'level2_value'
        },
        {
            'd': {
                'level1.1': {'repeated': 1},
                'level1.2': {'repeated': 2},
            },
            'find': 'repeated',
            'expected': 1
        },
        {
            'd': {
                'level': 'level_value'
            },
            'find': 'lvl',
            'expected': {}
        }
    ]
)
def test_nested_find(args):
    assert misc.nested_find(args['d'], args['find']) == args['expected']
