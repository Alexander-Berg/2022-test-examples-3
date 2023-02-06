# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from builtins import str
import time
from itertools import chain, repeat

import mock
import pytest
from django.conf import settings
from django.db import connection
from hamcrest import assert_that, contains_inanyorder

from travel.rasp.library.python.common23.db import switcher as switcher_module
from travel.rasp.library.python.common23.db.maintenance import read_conf, swap, UnknownRoleError
from travel.rasp.library.python.common23.db.switcher import (
    DbSwitcher, instance_role_alias, work_alias, service_alias, maintenance, log, switcher,
    SyncDbInBackground, get_connection_by_role, get_replica_sync_checker
)
from travel.rasp.library.python.common23.settings import WorkInstance, ServiceInstance

from travel.rasp.library.python.common23.tester.testcase import TestCase
from travel.rasp.library.python.common23.tester.utils.django_databases import mock_django_connection
from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting


@pytest.mark.dbuser
class TestDBSwitcher(TestCase):
    def test_get_db_alias(self):
        switcher = DbSwitcher()
        switcher.roles = {
            settings.WORK_DB: 'work123',
            settings.SERVICE_DB: 'service123',
        }

        assert switcher.get_db_alias(WorkInstance.role) == 'work123'
        assert switcher.get_db_alias(ServiceInstance.role) == 'service123'

        with replace_setting('INSTANCE_ROLE', ServiceInstance):
            assert switcher.get_db_alias() == 'service123'

        with pytest.raises(UnknownRoleError) as ex:
            switcher.get_db_alias('invalid role')
        assert 'unknown role' in str(ex).lower()

    def test_sync_db(self):
        switcher = DbSwitcher()

        def data_updated_handler(sender, **kwargs):
            data_updated_handler.got_signal = True
            assert sender == switcher

        def db_changed_handler(sender, **kwargs):
            db_changed_handler.got_signal = True
            assert sender == switcher

        data_updated_handler.got_signal = False
        db_changed_handler.got_signal = False

        with mock.patch.object(maintenance, 'read_conf', autospec=read_conf) as m_read_conf, \
                replace_setting('INSTANCE_ROLE', WorkInstance), \
                replace_setting('PKG_VERSION', 'ver42'):

            m_read_conf.return_value = {
                settings.WORK_DB: 'work123',
                settings.SERVICE_DB: 'service123',
                'cache_tag': 'tag 123 456'
            }

            switcher.data_updated.connect(data_updated_handler)
            switcher.db_switched.connect(db_changed_handler)

            switcher.sync_db()

            assert switcher.get_db_alias(settings.WORK_DB) == 'work123'
            assert switcher.get_db_alias(settings.SERVICE_DB) == 'service123'
            assert data_updated_handler.got_signal
            assert db_changed_handler.got_signal
            assert settings.CACHEROOT == '/yandex/rasp/ver42/work123/tag_123_456/'

            # Если данные не изменились - сигнал не отправляется
            data_updated_handler.got_signal = False
            db_changed_handler.got_signal = False
            switcher.sync_db()
            assert not data_updated_handler.got_signal
            assert not db_changed_handler.got_signal

            # Проверяем отправку сигнала только при изменении базы
            data_updated_handler.got_signal = False
            db_changed_handler.got_signal = False
            m_read_conf.return_value.update({settings.WORK_DB: 'work456'})
            switcher.sync_db()
            assert data_updated_handler.got_signal
            assert db_changed_handler.got_signal

            # Проверяем, что при изменении только cache_tag, отправляется лишь сигнал data_updated
            data_updated_handler.got_signal = False
            db_changed_handler.got_signal = False
            m_read_conf.return_value.update({'cache_tag': 'tag 000'})
            switcher.sync_db()
            assert data_updated_handler.got_signal
            assert not db_changed_handler.got_signal

    def test_sync_db_fail_on_first_read_conf_error(self):
        with mock.patch.object(maintenance, 'read_conf', autospec=read_conf) as m_read_conf:
            m_read_conf.side_effect = Exception('You go to hell and you die')

            switcher = DbSwitcher()
            switcher.roles = {'work_db': 42, 'service_db': 43}
            with pytest.raises(Exception) as excinfo:
                switcher.sync_db()
            assert excinfo.value.args[0] == 'You go to hell and you die'
            assert switcher.roles == {'work_db': 42, 'service_db': 43}

    def test_sync_db_not_fail_on_next_read_conf_error(self):
        with mock.patch.object(maintenance, 'read_conf', autospec=read_conf) as m_read_conf, \
                mock.patch.object(log, 'exception') as m_log_message:

            m_read_conf.return_value = {
                settings.WORK_DB: 'work123',
                settings.SERVICE_DB: 'service123',
                'cache_tag': 'tag 123 456'
            }
            switcher = DbSwitcher()
            switcher.sync_db()
            got_roles = switcher.roles.copy()

            m_read_conf.side_effect = Exception('You go to hell and you die')
            switcher.sync_db()

            call_args = m_log_message.call_args_list
            assert len(call_args) == 1
            assert 'Unable to update maintenance config' in call_args[0][0][0]

            assert switcher.roles == got_roles

    def test_sync_db_always_fail_on_read_conf_error_setting(self):
        with mock.patch.object(maintenance, 'read_conf', autospec=read_conf) as m_read_conf, \
                replace_setting('MAINTENANCE_DB_CRITICAL', True):

            switcher = DbSwitcher()

            m_read_conf.side_effect = Exception('You go to hell and you die')
            switcher.roles = {'work_db': 42, 'service_db': 43}
            with pytest.raises(Exception) as excinfo:
                switcher.sync_db()
            assert excinfo.value.args[0] == 'You go to hell and you die'
            assert switcher.roles == {'work_db': 42, 'service_db': 43}

            m_read_conf.side_effect = None
            m_read_conf.return_value = {
                settings.WORK_DB: 'work123',
                settings.SERVICE_DB: 'service123',
                'cache_tag': 'tag 123 456'
            }

            switcher.sync_db()
            got_roles = switcher.roles.copy()

            m_read_conf.side_effect = Exception('You go to hell and you die')
            with pytest.raises(Exception) as excinfo:
                switcher.sync_db()
            assert excinfo.value.args[0] == 'You go to hell and you die'
            assert switcher.roles == got_roles

    def test_swap(self):
        switcher = DbSwitcher()

        with mock.patch.object(maintenance, 'swap', autospec=swap) as m_swap, \
                mock.patch.object(DbSwitcher, 'sync_db', autospec=DbSwitcher.sync_db) as m_sync_db:

            switcher.swap(settings.WORK_DB, settings.SERVICE_DB)

            m_swap.assert_called_once_with(settings.WORK_DB, settings.SERVICE_DB)
            m_sync_db.assert_called_once_with(switcher)


def test_shortcuts():
    with replace_setting('INSTANCE_ROLE', ServiceInstance):
        assert instance_role_alias() == settings.SERVICE_DB

    switcher.roles[settings.WORK_DB] = 'work42'
    assert work_alias() == 'work42'

    switcher.roles[settings.SERVICE_DB] = 'service43'
    assert service_alias() == 'service43'


def test_sync_with_lazy_reconnect():
    switcher = DbSwitcher()
    switcher.roles = {'work_db': 42, 'service_db': 43}

    with mock.patch.object(maintenance, 'read_conf', autospec=read_conf) as m_read_conf, \
            mock.patch.object(switcher, '_close_connections') as m_close:
        m_read_conf.return_value = {
            settings.WORK_DB: 'work123',
            settings.SERVICE_DB: 'service123',
            'cache_tag': 'tag 123 456'
        }

        switcher.sync_with_lazy_reconnect()
        m_close.assert_called_once_with(sender=switcher, signal=switcher.db_switched)
        switcher.sync_with_lazy_reconnect()
        m_close.assert_called_once_with(sender=switcher, signal=switcher.db_switched)


@pytest.mark.dbuser
def test_get_connection_by_role():
    with mock.patch.object(switcher_module, 'switcher', autospec=True) as m_switcher:
        m_switcher.get_db_alias.return_value = 'default'

        assert get_connection_by_role('some_role') == connection.connection
        m_switcher.get_db_alias.assert_called_once_with('some_role')


@pytest.mark.dbuser
def test_get_replica_sync_checker():
    db_conf = {
        'ENGINE': 'travel.rasp.library.python.common23.db.backends.mysql',
        'CLUSTER': {
            'HOSTS': ['myhost42.yandex.net', 'myhost43.yandex.net', 'myhost44.yandex.net'],
        }
    }

    with mock_django_connection('service42', db_conf) as m_connections, replace_setting('SERVICE_DB', 'service42'):
        with mock.patch.object(switcher_module, 'connections', m_connections):
            replica_sync_checker = get_replica_sync_checker(settings.SERVICE_DB, is_synced=mock.sentinel.is_synced)

            assert_that(replica_sync_checker.hosts, contains_inanyorder('myhost42.yandex.net', 'myhost43.yandex.net', 'myhost44.yandex.net'))
            assert replica_sync_checker.conn_getter == m_connections['service42'].get_connection_to_host
            assert replica_sync_checker.is_synced is mock.sentinel.is_synced

    # replica sync checker should check all hosts, not master only
    db_conf = {
        'ENGINE': 'travel.rasp.library.python.common23.db.backends.mysql',
        'CLUSTER': {
            'HOSTS': ['myhost42.yandex.net', 'myhost43.yandex.net', 'myhost44.yandex.net'],
            'USE_MASTER': True,
            'USE_REPLICAS': False,
        }
    }
    with mock_django_connection('service42', db_conf) as m_connections, replace_setting('SERVICE_DB', 'service42'):
        with mock.patch.object(switcher_module, 'connections', m_connections):
            replica_sync_checker = get_replica_sync_checker(settings.SERVICE_DB, is_synced=mock.sentinel.is_synced)

            assert_that(replica_sync_checker.hosts, contains_inanyorder('myhost42.yandex.net', 'myhost43.yandex.net', 'myhost44.yandex.net'))
            assert replica_sync_checker.conn_getter == m_connections['service42'].get_connection_to_host
            assert replica_sync_checker.is_synced is mock.sentinel.is_synced


class TestSyncDbInBackground(object):
    def test_valid(self):
        with mock.patch.object(DbSwitcher, 'sync_db') as m_sync_db:
            db_syncer = SyncDbInBackground(interval=0.00001)
            db_syncer.start()
            time.sleep(0.01)
            m_sync_db.assert_called()

    def test_not_fail_on_errors(self):
        """
        Проверяем, что, несмотря на ошибки, sync_db продолжает вызываться.
        """
        with mock.patch.object(DbSwitcher, 'sync_db') as m_sync_db:
            m_sync_db.side_effect = chain([Exception, Exception, Exception], repeat(None))

            db_syncer = SyncDbInBackground(interval=0.00001)
            db_syncer.start()
            time.sleep(0.1)
            assert len(m_sync_db.call_args_list) > 3
