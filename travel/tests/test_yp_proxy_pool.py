# coding: utf8

from __future__ import absolute_import, division, print_function, unicode_literals

import mock
from hamcrest import assert_that, contains

from travel.rasp.library.python.proxy.yp_proxy_pool import YpProxyPool


test_hosts = ['0421.sas.yandex.net', '4234.vla.yandex.net', '8521.man.yandex.net']


def test_dc_order():
    with mock.patch('travel.library.python.yp.endpoints.YpEndpoints.get_hosts', return_value=test_hosts):
        pool = YpProxyPool(
            user='rasp',
            password='raspp',
            client_name='rasp_test_proxy',
            yd_entry_point='rasp_proxy_test.deployUnit',
            current_dc='vla')

        hosts = pool.get_hosts()

        assert_that(hosts, contains('4234.vla.yandex.net', '0421.sas.yandex.net', '8521.man.yandex.net'))


def test_fallback_hosts():
    with mock.patch('travel.library.python.yp.endpoints.YpEndpoints.get_hosts', side_effect=IOError):
        pool = YpProxyPool(
            user='rasp',
            password='raspp',
            client_name='rasp_test_proxy',
            yd_entry_point='rasp_proxy_test.deployUnit',
            current_dc='vla',
            fallback_hosts=['1234.myt.yandex.net'])

        hosts = pool.get_hosts()

        assert_that(hosts, contains('1234.myt.yandex.net'))


def test_http_proxy():
    with mock.patch('travel.library.python.yp.endpoints.YpEndpoints.get_hosts', return_value=['0421.sas.yandex.net']):
        pool = YpProxyPool(user='p_usr', password='p_pwd', yd_entry_point='test')

        proxy = pool.get_http_proxies()

        assert len(proxy) == 1
        assert proxy[0].url == 'http://p_usr:p_pwd@0421.sas.yandex.net:80'


def test_ftp_proxy():
    with mock.patch('travel.library.python.yp.endpoints.YpEndpoints.get_hosts', return_value=['0421.sas.yandex.net']):
        pool = YpProxyPool(user='p_usr', password='p_pwd', yd_entry_point='test')

        proxy = pool.get_ftp_proxies('ftp://localhost:21')

        assert len(proxy) == 1
        assert proxy[0].url == 'ftp://0421.sas.yandex.net:8021'
        assert proxy[0].user == 'p_usr:p_pwd@localhost:21'
