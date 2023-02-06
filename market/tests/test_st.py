from market.tools.resource_monitor.lib.st import StClient

import mock
from mock import patch
import json
from library.python import resource


@patch.object(StClient, '_get')
def test_get_hierarchy(method):
    method.return_value = json.loads(resource.find('/data/st_dep_info_response.json'))
    x = StClient(token='token')
    hierarchy = x.get_hierarchy('login1')
    method.assert_called()
    assert 'level' in hierarchy
    assert 'department_name' in hierarchy
    assert hierarchy['department_name'] == 'Group of technical research projects'
