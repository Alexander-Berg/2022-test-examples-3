# -*- coding: utf-8 -*-
import random
import mock
import os

from test.base import DiskTestCase

from mpfs.common.errors import RabbitMQCacheBrokenDataError, RabbitMQCacheFileNotFoundError, ConductorError
from mpfs.common.util import trace_calls
from mpfs.core.services.conductor_service import ConductorService
from mpfs.engine.queue2 import brokers_cache


def get_fake_open_url(current_host_datacenter='ugr'):
    def fake_open_url(self, url):
        if 'groups2hosts' in url:
            return \
                '[{"root_datacenter_name": "myt", "fqdn": "queue01f.dst.yandex.net"}, ' \
                '{"root_datacenter_name": "myt", "fqdn": "queue02f.dst.yandex.net"}, ' \
                '{"root_datacenter_name": "sas", "fqdn": "queue03f.dst.yandex.net"}]'
        elif 'hosts' in url:
            return '{"admins": ["agodin", "ignition", "ivanlook", "pperekalov", "eightn", "ivanov-d-s", "zmey"],' \
                   '"datacenter": "123", "description": "", "fqdn": "freyr.dsd.yandex.net", "group": "disk_dev",' \
                   '"root_datacenter": "%s", "short_name": "freyr.dsd"}' % current_host_datacenter
    return fake_open_url


def fake_open_url_unavailable_host(self, url):
    raise self.api_error()


class BrokersConductorCacheTestCase(DiskTestCase):
    tmp_cache_filename = None

    def setup_method(self, method):
        if not os.path.exists('/tmp/mpfs'):
            os.mkdir('/tmp/mpfs')

        self.tmp_cache_filename = '/tmp/mpfs/cache_%s' % hex(random.getrandbits(8*10))[2:-1]

    def teardown_method(self, method):
        if os.path.exists(self.tmp_cache_filename):
            os.unlink(self.tmp_cache_filename)

    def test_get_current_host_datacenter_in_conductor(self):
        with mock.patch.object(ConductorService, 'open_url', get_fake_open_url('myt')):
            cache = brokers_cache.BrokersConductorCache()
            assert cache._get_current_host_datacenter_name() == 'myt'

    def test_get_current_host_datacenter_not_in_conductor(self):
        cache = brokers_cache.BrokersConductorCache()
        cache._current_hostname = 'myhost-123456.dst.yandex.net'
        self.assertRaises(ConductorError, cache._get_current_host_datacenter_name)

    def test_get_current_host_datacenter_qloud_env(self):
        with mock.patch.object(brokers_cache, 'QUEUE2_HOST_DATACENTER', 'myt'):
            cache = brokers_cache.BrokersConductorCache()
            cache._current_hostname = 'myhost-123456.dst.yandex.net'
            assert cache._get_current_host_datacenter_name() == 'myt'

    def test_filtering_by_datacenter(self):
        with mock.patch.object(ConductorService, 'open_url', get_fake_open_url('ugr')):
            with mock.patch.object(brokers_cache, 'QUEUE2_RABBITMQ_HOSTS', ['%super_fake_group']):
                cache = brokers_cache.BrokersConductorCache(path=self.tmp_cache_filename)
                cache.load()

                assert len(cache.get_rabbitmq_hosts()) == 3
                assert len(cache.get_rabbitmq_hosts(False)) == 3

        with mock.patch.object(ConductorService, 'open_url', get_fake_open_url('myt')):
            with mock.patch.object(brokers_cache, 'QUEUE2_RABBITMQ_HOSTS', ['%super_fake_group']):
                cache = brokers_cache.BrokersConductorCache()
                cache.load()

                assert len(cache.get_rabbitmq_hosts()) == 2
                assert len(cache.get_rabbitmq_hosts(False)) == 3

    def test_reading_from_file(self):
        cache_data = '{"rabbitmq_hosts": [{"fqdn": "host1", "dc": "ugr"}, {"fqdn": "host2", "dc": "iva"}, ' \
                     '{"fqdn": "host3", "dc": "sas"}], "current_dc": "iva"}'

        with open(self.tmp_cache_filename, 'w') as file_obj:
            file_obj.write(cache_data)

        with mock.patch.object(ConductorService, 'open_url', fake_open_url_unavailable_host):
            with mock.patch.object(brokers_cache, 'QUEUE2_RABBITMQ_HOSTS', ['%super_fake_group']):
                cache = brokers_cache.BrokersConductorCache(path=self.tmp_cache_filename)
                cache.load()

                assert len(cache.get_rabbitmq_hosts()) == 1
                assert len(cache.get_rabbitmq_hosts(False)) == 3

    def test_reading_from_broken_file(self):
        cache_data = '{"rabbitmq_hosts": [{"fqdn": "host1", "dc": "ugr "current_dc": "iva"}'

        with open(self.tmp_cache_filename, 'w') as file_obj:
            file_obj.write(cache_data)

        with mock.patch.object(ConductorService, 'open_url', fake_open_url_unavailable_host):
            with mock.patch.object(brokers_cache, 'QUEUE2_RABBITMQ_HOSTS', ['%super_fake_group']):
                cache = brokers_cache.BrokersConductorCache(path=self.tmp_cache_filename)

                common_exception_raised = False
                cache_exception_raised = False
                try:
                    cache.load()
                except RabbitMQCacheBrokenDataError:
                    cache_exception_raised = True
                except Exception:
                    common_exception_raised = True

                assert cache_exception_raised
                assert not common_exception_raised

    def test_reading_from_non_existent_file(self):
        with mock.patch.object(ConductorService, 'open_url', fake_open_url_unavailable_host):
            with mock.patch.object(brokers_cache, 'QUEUE2_RABBITMQ_HOSTS', ['%super_fake_group']):
                cache = brokers_cache.BrokersConductorCache(path=self.tmp_cache_filename)

                common_exception_raised = False
                cache_exception_raised = False
                try:
                    cache.load()
                except RabbitMQCacheFileNotFoundError:
                    cache_exception_raised = True
                except Exception:
                    common_exception_raised = True

                assert cache_exception_raised
                assert not common_exception_raised

    def test_conductor_switch(self):
        cache_data = '{"rabbitmq_hosts": [{"fqdn": "host1", "dc": "ugr"}, {"fqdn": "host2", "dc": "iva"}, ' \
                     '{"fqdn": "host3", "dc": "sas"}], "current_dc": "iva"}'

        with open(self.tmp_cache_filename, 'w') as file_obj:
            file_obj.write(cache_data)

        # проверяем, что при выставленной QUEUE2_RABBITMQ_RELOAD_CACHE_FROM_CONDUCTOR_ON_START в False не пойдем в
        # кондуктор, а только прочитаем из кеша
        with mock.patch.object(ConductorService, 'open_url', fake_open_url_unavailable_host):
            with mock.patch.object(brokers_cache, 'QUEUE2_RABBITMQ_HOSTS', ['%super_fake_group']):
                with mock.patch.object(brokers_cache, 'QUEUE2_RABBITMQ_RELOAD_CACHE_FROM_CONDUCTOR_ON_START', False):
                    with trace_calls(ConductorService, 'open_url') as tracer:
                        cache = brokers_cache.BrokersConductorCache(path=self.tmp_cache_filename)

                        common_exception_raised = False
                        cache_exception_raised = False
                        try:
                            cache.load()
                        except RabbitMQCacheFileNotFoundError:
                            cache_exception_raised = True
                        except Exception:
                            common_exception_raised = True

                        assert tracer['total_calls'] == 0
                        assert not cache_exception_raised
                        assert not common_exception_raised

        # проверяем, что при выставленной QUEUE2_RABBITMQ_RELOAD_CACHE_FROM_CONDUCTOR_ON_START в False не пойдем в
        # кондуктор, а только прочитаем из кеша, но кеш мы удалим, поэтому должно будет кинуться исключение
        os.unlink(self.tmp_cache_filename)

        with mock.patch.object(ConductorService, 'open_url', fake_open_url_unavailable_host):
            with mock.patch.object(brokers_cache, 'QUEUE2_RABBITMQ_HOSTS', ['%super_fake_group']):
                with mock.patch.object(brokers_cache, 'QUEUE2_RABBITMQ_RELOAD_CACHE_FROM_CONDUCTOR_ON_START', False):
                    with trace_calls(ConductorService, 'open_url') as tracer:
                        cache = brokers_cache.BrokersConductorCache(path=self.tmp_cache_filename)

                        common_exception_raised = False
                        cache_exception_raised = False
                        try:
                            cache.load()
                        except RabbitMQCacheFileNotFoundError:
                            cache_exception_raised = True
                        except Exception:
                            common_exception_raised = True

                        assert tracer['total_calls'] == 0
                        assert cache_exception_raised
                        assert not common_exception_raised
