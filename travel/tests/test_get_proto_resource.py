# coding: utf-8
from __future__ import unicode_literals, absolute_import, print_function, division

import httpretty
import mock
import pytest

from travel.avia.library.python.shared_dicts import common
from travel.avia.library.python.shared_dicts.exceptions import GetProtoResourceError
from travel.avia.library.python.shared_dicts.rasp import get_repository, ResourceType


@pytest.mark.httpretty
def test_bad_response():
    url = 'https://proxy.sandbox.yandex-team.ru/123'
    httpretty.register_uri(httpretty.GET, url, status=500)

    with mock.patch.object(common, 'get_resource_proxy', return_value=url):
        with pytest.raises(GetProtoResourceError):
            get_repository(ResourceType.TRAVEL_DICT_RASP_SETTLEMENT_PROD)
