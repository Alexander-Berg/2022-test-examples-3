# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
import pytest

from travel.rasp.library.python.db.utils import get_dc_priority, TimeInterval


@pytest.mark.parametrize('from_dc,to_dc,expected', (
    ('sas', 'sas', 0),
    ('sas', 'man', 4),
    (None, 'sas', 0),
    (None, 'iva', 3),
    ('man', 'myt', 2),
    ('moon', 'sas', 42),
    ('sas', 'moon', 42),
))
def test_get_dc_priority(from_dc, to_dc, expected):
    assert get_dc_priority(from_dc, to_dc, default_value=42) == expected


def test_get_dc_priority_fail():
    with pytest.raises(Exception):
        get_dc_priority('sas', 'moon')

    with pytest.raises(Exception):
        get_dc_priority('moon', 'sas')


def test_time_interval():
    with mock.patch.object(TimeInterval, 'get_current_time') as m_current_time:
        m_current_time.return_value = 0
        ti = TimeInterval(dt=2)

        assert ti.is_time_passed is False
        m_current_time.return_value = 1
        assert ti.is_time_passed is False
        m_current_time.return_value = 2
        assert ti.is_time_passed is True
        m_current_time.return_value = 4
        assert ti.is_time_passed is True

        ti.reset()
        assert ti.is_time_passed is False
        m_current_time.return_value = 5
        assert ti.is_time_passed is False
        m_current_time.return_value = 6
        assert ti.is_time_passed is True
