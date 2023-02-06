# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

import travel.rasp.suggests_tasks.suggests.generate.shared_objects as shared_objs
from travel.rasp.suggests_tasks.suggests.generate.shared_objects import get_obj, set_objs, clear_objs


class TestSharedObjects(object):
    stats_data = {(u'c', 1): {u'w_sett': u'v_sett'}, (u's', 1): {u'w_station': u'v_station'}}

    def test_get_obj(self):
        with pytest.raises(AssertionError):
            get_obj('not_exist')

        set_objs(stat_weights=self.stats_data)
        assert get_obj(u'stat_weights') == self.stats_data

    def test_set_obj(self):
        with pytest.raises(AssertionError):
            set_objs(not_exist='value')

        set_objs(stat_weights=self.stats_data)
        assert shared_objs._objs['stat_weights'] == self.stats_data

        set_objs(**{key: '{}_value'.format(key) for key in shared_objs._objs.keys()})

        for key in shared_objs._objs.keys():
            assert shared_objs._objs[key] == '{}_value'.format(key)

    def test_clear_obj(self):
        set_objs(**{key: 'value' for key in shared_objs._objs.keys()})

        with mock.patch('travel.rasp.suggests_tasks.suggests.generate.shared_objects.set_objs', side_effect=set_objs) as m_cleat_objs:
            clear_objs()
            m_cleat_objs.assert_called_once_with(**{key: None for key in shared_objs._objs.keys()})

        for key in shared_objs._objs.keys():
            assert shared_objs._objs[key] is None
