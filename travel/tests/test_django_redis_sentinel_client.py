# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
from django.core.cache.backends.base import DEFAULT_TIMEOUT
from redis.client import Redis

from travel.rasp.library.python.common23.db.redis.django_redis_sentinel_client import DjangoRedisSentinelClient, DjangoRedisSentinel


def test_redis_client():
    server = ['redis://host_1:port', 'redis://host_2:port']
    params = {'OPTIONS': {
        'SENTINEL_HOSTS': [('host_1', 'port'), ('host_2', 'port')],
        'SENTINEL_SERVICE_NAME': 'yc_cluster_name',
        'SENTINEL_SOCKET_TIMEOUT': 0.5,
        'CURRENT_DC': 'xxx',
        'PASSWORD': 'pass',
        'SOCKET_TIMEOUT': 5
    }}
    backend = 'django_redis.cache.RedisCache'
    client = DjangoRedisSentinelClient(server, params, backend)
    assert client.sentinel_service_name == 'yc_cluster_name'

    assert isinstance(client.master, Redis)
    assert isinstance(client.slave, Redis)
    assert client.slave.connection_pool.connection_kwargs['socket_timeout'] == 5
    assert client.master.connection_pool.connection_kwargs['socket_timeout'] == 5

    assert isinstance(client.sentinel, DjangoRedisSentinel)
    assert client.sentinel.current_dc == 'xxx'
    assert client.sentinel.sentinels[0].connection_pool.connection_kwargs['socket_timeout'] == 0.5

    with mock.patch('travel.rasp.library.python.common23.db.redis.django_redis_sentinel_client.socket.gethostbyaddr') as m_gethostbyaddr:
        # return all slaves
        m_gethostbyaddr.side_effect = [('yyy-a.b.c', '', ''), ('zzz-a.b.c', '', '')]
        client.sentinel.update_slaves_dc([['ip_1', 'port_1'], ['ip_2', 'port_2']])
        assert client.sentinel.dc_by_ip == {'ip_1': 'yyy', 'ip_2': 'zzz'}
        assert client.sentinel.filter_slaves([
            {'ip': 'ip_1', 'port': 'port_1', 'is_odown': False, 'is_sdown': False},
            {'ip': 'ip_2', 'port': 'port_2', 'is_odown': False, 'is_sdown': False}
        ]) == [('ip_1', 'port_1'), ('ip_2', 'port_2')]

        # return current_dc slave
        m_gethostbyaddr.side_effect = [('xxx-a.b.c', '', '')]
        assert client.sentinel.filter_slaves([
            {'ip': 'ip_1', 'port': 'port', 'is_odown': False, 'is_sdown': False},
            {'ip': 'ip_2', 'port': 'port', 'is_odown': False, 'is_sdown': False},
            {'ip': 'ip_3', 'port': 'port', 'is_odown': False, 'is_sdown': False}
        ]) == [('ip_3', 'port')]

    with mock.patch.object(client, 'call_method') as m_call_method:
        client.get('key', default=123)
        m_call_method.assert_called_once_with('get', 'key', default_value=123, client=None, default=123, version=None)

        client.get('key')
        assert m_call_method.call_args == mock.call('get', 'key', default_value=None, client=None, default=None, version=None)

        client.get_many(['key_1', 'key_2'])
        assert m_call_method.call_args == mock.call('get_many', ['key_1', 'key_2'], default_value={}, client=None, version=None)

        client.set('key', 'value')
        assert m_call_method.call_args == mock.call('set', 'key', 'value', timeout=DEFAULT_TIMEOUT, version=None, client=None, nx=False, xx=False)

        client.set_many({'key_1', 'value_1', 'key_2', 'value_2'})
        assert m_call_method.call_args == mock.call('set_many', {'key_1', 'value_1', 'key_2', 'value_2'}, timeout=DEFAULT_TIMEOUT, version=None, client=None)

        client.delete('key')
        assert m_call_method.call_args == mock.call('delete', 'key', version=None, prefix=None, client=None)

        client.delete_many(['key_1', 'key_2'])
        assert m_call_method.call_args == mock.call('delete_many', ['key_1', 'key_2'], version=None, client=None)

        client.add('key', 'value')
        assert m_call_method.call_args == mock.call('set', 'key', 'value', timeout=DEFAULT_TIMEOUT, version=None, client=None, nx=True, default_value=False)
