# coding: utf8

from __future__ import absolute_import, division, print_function, unicode_literals

import mock

from travel.rasp.library.python.proxy.proxy_pool import ProxyPool


test_hosts = ['sas-0421.yandex.net', 'vla-4234.yandex.net']


def test_http_proxy():
    with mock.patch.object(ProxyPool, 'get_hosts', return_value=test_hosts):
        pool = ProxyPool(user='p_usr', password='p_pwd')

        proxies = pool.get_http_proxies()

        assert len(proxies) == 2
        assert proxies[0].url == 'http://p_usr:p_pwd@sas-0421.yandex.net:80'
        assert proxies[1].url == 'http://p_usr:p_pwd@vla-4234.yandex.net:80'


def test_ftp_proxy():
    with mock.patch.object(ProxyPool, 'get_hosts', return_value=test_hosts):
        pool = ProxyPool(user='p_usr', password='p_pwd')

        proxies = pool.get_ftp_proxies('ftp://avtovokzal66.test.ru', 'ftp_usr', 'ftp_pwd')

        assert len(proxies) == 2

        assert proxies[0].url == 'ftp://sas-0421.yandex.net:8021'
        assert proxies[0].user == 'p_usr:p_pwd:ftp_usr@avtovokzal66.test.ru:21'
        assert proxies[0].password == 'ftp_pwd'

        assert proxies[1].url == 'ftp://vla-4234.yandex.net:8021'
        assert proxies[1].user == 'p_usr:p_pwd:ftp_usr@avtovokzal66.test.ru:21'
        assert proxies[1].password == 'ftp_pwd'


def test_ftp_proxy_with_anonymous_user():
    with mock.patch.object(ProxyPool, 'get_hosts', return_value=test_hosts):
        pool = ProxyPool(user='p_usr', password='p_pwd')

        proxies = pool.get_ftp_proxies('ftp://test.free.public-ftp.ru')

        assert len(proxies) == 2

        assert proxies[0].url == 'ftp://sas-0421.yandex.net:8021'
        assert proxies[0].user == 'p_usr:p_pwd@test.free.public-ftp.ru:21'
        assert proxies[0].password is None

        assert proxies[1].url == 'ftp://vla-4234.yandex.net:8021'
        assert proxies[1].user == 'p_usr:p_pwd@test.free.public-ftp.ru:21'
        assert proxies[1].password is None
