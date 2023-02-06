# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from common.tester.utils.replace_setting import replace_setting
import travel.rasp.suburban_tasks.suburban_tasks.rzd_hosts as rzd_hosts
from travel.rasp.suburban_tasks.suburban_tasks.rzd_hosts import get_rzd_hosts_and_ports, get_hosts_from_yp


def test_get_hosts_from_yp():
    m_yp_endpoints = mock.Mock(
        return_value=mock.Mock(
            get_hosts=mock.Mock(
                return_value=mock.sentinel.hosts
            )
        )
    )

    with mock.patch.object(rzd_hosts, 'YpEndpoints', m_yp_endpoints):
        result = get_hosts_from_yp()
        assert result is mock.sentinel.hosts
        m_yp_endpoints.assert_called_once_with('suburban_tasks', 'rzd_proxy.DeployUnit1')


def test_get_rzd_hosts_and_ports():
    with mock.patch.object(rzd_hosts, 'get_hosts_from_yp') as m_get_hosts_from_yp:
        m_get_hosts_from_yp.side_effect = [
            ['yprzdhost1', 'yprzdhost2', 'yprzdhost3'],
            Exception('something bad'),
        ]

        with replace_setting('RZD_HOSTS', [('rzdhost1', 555), ('rzdhost2', 556)]), replace_setting('RZD_PORT', 42):
            assert get_rzd_hosts_and_ports() == [('rzdhost1', 555), ('rzdhost2', 556)]  # yp hosts disabled
            with replace_setting('ENABLE_GET_RZD_HOSTS_FROM_YP', True):
                assert get_rzd_hosts_and_ports() == [('yprzdhost1', 42), ('yprzdhost2', 42), ('yprzdhost3', 42)]  # yp hosts enabled
                assert get_rzd_hosts_and_ports() == [('rzdhost1', 555), ('rzdhost2', 556)]  # yp hosts enabled, but got an error
