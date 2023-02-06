# -*- coding: utf-8 -*-
import random
import mock
import os
import pytest

from test.base import DiskTestCase
from mpfs.core.services.conductor_service import ConductorService
from mpfs.common.util import from_json


def create_tmp_conductor_cache(fn):
    def wrapped(self, *args, **kwargs):
        with mock.patch.object(ConductorService, 'cache_filepath', self.tmp_cache_filename):
            return fn(self, *args, **kwargs)
    return wrapped


class ConductorTestCase(DiskTestCase):
    tmp_cache_filename = None

    def setup_method(self, method):
        if not os.path.exists('/tmp/mpfs'):
            os.mkdir('/tmp/mpfs')

        self.tmp_cache_filename = '/tmp/mpfs/cache_%s' % hex(random.getrandbits(8*10))[2:-1]

    def teardown_method(self, method):
        if os.path.exists(self.tmp_cache_filename):
            os.unlink(self.tmp_cache_filename)

    @staticmethod
    def fake_open_url(self, url):
        if 'groups2hosts' in url:
            last_part = url.split('/')[-1]
            group = last_part.split('?')[0]

            if group == 'disk_mpfs':
                return \
                    '[{"id":22625,"fqdn":"mpfs10h.disk.yandex.net"},{"id":22630,"fqdn":"mpfs10j.disk.yandex.net"},' \
                    '{"id":119985,"fqdn":"mpfs10o.disk.yandex.net"},{"id":116231,"fqdn":"mpfs11h.disk.yandex.net"},' \
                    '{"id":119986,"fqdn":"mpfs11o.disk.yandex.net"},{"id":116232,"fqdn":"mpfs12h.disk.yandex.net"},' \
                    '{"id":119987,"fqdn":"mpfs12o.disk.yandex.net"},{"id":116233,"fqdn":"mpfs13h.disk.yandex.net"},' \
                    '{"id":119988,"fqdn":"mpfs13o.disk.yandex.net"},{"id":116234,"fqdn":"mpfs14h.disk.yandex.net"},' \
                    '{"id":119989,"fqdn":"mpfs14o.disk.yandex.net"},{"id":116235,"fqdn":"mpfs15h.disk.yandex.net"},' \
                    '{"id":119990,"fqdn":"mpfs15o.disk.yandex.net"},{"id":15502,"fqdn":"mpfs1g.disk.yandex.net"},' \
                    '{"id":16146,"fqdn":"mpfs1h.disk.yandex.net"},{"id":19770,"fqdn":"mpfs1j.disk.yandex.net"},' \
                    '{"id":119992,"fqdn":"mpfs1o.disk.yandex.net"},{"id":20286,"fqdn":"mpfs2g.disk.yandex.net"},' \
                    '{"id":20290,"fqdn":"mpfs2h.disk.yandex.net"}]'
            elif group == 'disk_api':
                return '[{"id":42500,"fqdn":"api01d.disk.yandex.net"},{"id":42496,"fqdn":"api01e.disk.yandex.net"},' \
                       '{"id":42504,"fqdn":"api01h.disk.yandex.net"},{"id":42501,"fqdn":"api02d.disk.yandex.net"},' \
                       '{"id":42497,"fqdn":"api02e.disk.yandex.net"},{"id":42505,"fqdn":"api02h.disk.yandex.net"},' \
                       '{"id":42502,"fqdn":"api03d.disk.yandex.net"},{"id":42498,"fqdn":"api03e.disk.yandex.net"},' \
                       '{"id":42506,"fqdn":"api03h.disk.yandex.net"},{"id":42503,"fqdn":"api04d.disk.yandex.net"},' \
                       '{"id":42499,"fqdn":"api04e.disk.yandex.net"},{"id":42507,"fqdn":"api04h.disk.yandex.net"}]'
        elif 'hosts' in url:
            last_part = url.split('/')[-1]
            host = last_part.split('?')[0]

            if host == 'mpfs1g.disk.yandex.net':
                return '[{"group":"disk_mpfs","fqdn":"mpfs1g.disk.yandex.net","datacenter":"myt2",' \
                       '"root_datacenter":"myt","short_name":"mpfs1g.disk","description":"",' \
                       '"admins":["eightn","ignition","agodin","ivanlook","dmiga","pperekalov","ivanov-d-s"]}]'
            elif host == 'mpfs1h.disk.yandex.net':
                return '[{"group":"disk_mpfs","fqdn":"mpfs1h.disk.yandex.net","datacenter":"fol4",' \
                       '"root_datacenter":"fol","short_name":"mpfs1h.disk","description":"disk_mpfs",' \
                       '"admins":["eightn","ignition","agodin","ivanlook","dmiga","pperekalov","ivanov-d-s"]}]'
            else:
                return '[]'

    @create_tmp_conductor_cache
    def test_get_hosts_by_group(self):
        with mock.patch.object(ConductorService, 'open_url', self.fake_open_url):
            conductor = ConductorService()
            hosts = conductor.get_hosts_by_group('disk_api')
            assert len(hosts)

    @create_tmp_conductor_cache
    def test_host_by_fqdn(self):
        with mock.patch.object(ConductorService, 'open_url', self.fake_open_url):
            conductor = ConductorService()

            host = conductor.get_host_by_fqdn('mpfs1g.disk.yandex.net')
            assert host is not None

            host = conductor.get_host_by_fqdn('mpfs1g.disk.rambler.net')
            assert host is None

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    @create_tmp_conductor_cache
    def test_fetch_from_server(self):
        with mock.patch.object(ConductorService, 'open_url', self.fake_open_url):
            conductor = ConductorService()
            conductor.fetch_hosts_from_server(['%disk_api',
                                               '%disk_mpfs',
                                               'mpfs1g.disk.yandex.net',
                                               'mpfs1g.disk.rambler.net'])

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    @create_tmp_conductor_cache
    def test_update_cache(self):
        with mock.patch.object(ConductorService, 'open_url', self.fake_open_url):
            fake_platform_auth = [
                {
                    'name': 'ok-service',
                    'enabled': True,
                    'auth_methods': ['conductor'],
                    'conductor_items': ['%disk_api', '%disk_mpfs'],
                    'allowed_origin_hosts': ['^disk\.yandex\.(ru|ua|by|kz|com|com\.tr)$', 'yandex.net'],
                    'oauth_client_id': '123-test-123',
                    'oauth_client_name': 'internal-test',
                    'oauth_scopes': ['cloud_api:disk.read', 'cloud_api:disk.write'],
                },
                {
                    'name': 'service-without-scopes',
                    'enabled': True,
                    'auth_methods': ['conductor'],
                    'conductor_items': ['mpfs1g.disk.yandex.net'],
                    'allowed_origin_hosts': ['^disk\.yandex\.(ru|ua|by|kz|com|com\.tr)$', 'yandex.net'],
                    'oauth_client_id': '123-test-123',
                    'oauth_client_name': 'internal-test',
                    'oauth_scopes': [],
                }
            ]

            ConductorService(read_cache=False).update_cache(fake_platform_auth)
            fileobj = open(self.tmp_cache_filename)
            data = fileobj.read()

            assert len(data)
            result = from_json(data)

            assert result

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    @create_tmp_conductor_cache
    def test_get_by_ip(self):
        with mock.patch.object(ConductorService, 'open_url', self.fake_open_url):
            fake_platform_auth = [
                {
                    'name': 'ok-service',
                    'enabled': True,
                    'auth_methods': ['conductor'],
                    'conductor_items': ['mpfs1g.disk.yandex.net', '%disk_mpfs'],
                    'allowed_origin_hosts': ['^disk\.yandex\.(ru|ua|by|kz|com|com\.tr)$', 'yandex.net'],
                    'oauth_client_id': '123-test-123',
                    'oauth_client_name': 'internal-test',
                    'oauth_scopes': ['cloud_api:disk.read', 'cloud_api:disk.write'],
                },
            ]

            ConductorService(read_cache=False).update_cache(fake_platform_auth)

            conductor = ConductorService()
            ip = conductor._get_ip_by_hostname('mpfs1g.disk.yandex.net')[0]
            conductor_item = conductor.get_conductor_item_by_ip(ip)

            assert conductor_item == 'mpfs1g.disk.yandex.net'
