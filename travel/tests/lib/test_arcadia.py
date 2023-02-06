# coding: utf-8
from __future__ import unicode_literals, absolute_import, print_function, division

import pytest

from travel.avia.admin.lib.arcadia.convert_command import convert_to_arcadia


@pytest.mark.parametrize('command, expected_y_python_entry_point', (
    ('avia_scripts/sync_with_rasp/sync_all.py', 'travel.avia.admin.avia_scripts.sync_with_rasp.sync_all:main'),
))
def test_convert_to_arcadia(command, expected_y_python_entry_point):
    assert convert_to_arcadia(command) == expected_y_python_entry_point
