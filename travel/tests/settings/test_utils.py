# -*- coding: utf-8 -*-

import mock
from hamcrest import assert_that
from hamcrest import has_entries
from hamcrest.library.collection.issequence_containinginanyorder import contains_inanyorder

from travel.avia.library.python.common.settings.utils import get_caches_config_by_group


def test_get_caches_config_by_group():
    conductor = mock.Mock()
    conductor.get_group_hosts_by_dc.return_value = {
        'ugr': ['host1', 'host2']
    }
    conductor.get_current_dc.return_value = 'ugr'

    caches = get_caches_config_by_group('some_group', conductor)
    assert_that(caches, has_entries(default=has_entries({
        'LOCATION': contains_inanyorder('inet6:[host1]:11211', 'inet6:[host2]:11211'),
        'BACKEND': 'travel.avia.library.python.common.utils.memcache_backend.MemcachedCache'
    })))

    conductor.get_group_hosts_by_dc.assert_called_once_with('some_group')
    conductor.get_current_dc.assert_called_once_with()
