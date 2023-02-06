# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from travel.rasp.library.python.common23.db.switcher import switcher
from travel.rasp.library.python.common23.models.precache import backend
from travel.rasp.library.python.common23.models.precache.backend import (
    ManagersPrecacheBuilder, MethodsPrecacheBuilder, RECACHE_FUNC_UID, setup_precache
)
from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting


class TestMethodsPrecacheBuilder(object):
    def test(self):
        module1 = mock.MagicMock()
        module2 = mock.MagicMock()

        with mock.patch.dict('sys.modules', {'module1': module1, 'module2': module2}):
            builder = MethodsPrecacheBuilder(('module1:method1', 'module2:obj.method2',))
            builder()

        module1.method1.assert_called_once_with()
        module2.obj.method2.assert_called_once_with()


class TestManagersPrecacheBuilder(object):
    def test(self):
        cm1 = mock.MagicMock()
        cm2 = mock.MagicMock()
        builder = ManagersPrecacheBuilder(('module1:manager1', 'module2:obj.manager2',))

        with mock.patch.dict('sys.modules', {
            'module1': mock.Mock(**{'manager1.using_precache.return_value': cm1}),
            'module2': mock.Mock(**{'obj.manager2.using_precache.return_value': cm2})
        }):
            builder()

            assert cm1.mock_calls == [mock.call.__enter__(cm1)]
            assert cm2.mock_calls == [mock.call.__enter__(cm2)]

            builder()

            assert cm1.mock_calls[1:] == [mock.call.__exit__(cm1, None, None, None), mock.call.__enter__(cm1)]
            assert cm2.mock_calls[1:] == [mock.call.__exit__(cm2, None, None, None), mock.call.__enter__(cm2)]


def test_setup_precache_disabled():
    logger = mock.Mock()

    with mock.patch.object(backend, 'precache', None):
        setup_precache(logger=logger)

    logger.info.assert_called_with('Precache is disabled')


def test_setup_precache_signals():
    assert not any(lookup_key[0] == RECACHE_FUNC_UID for lookup_key, _receiver in switcher.data_updated.receivers)

    with mock.patch.object(backend, 'precache') as m_precache:
        setup_precache(logger=mock.Mock())

    m_precache.assert_called_once_with()
    assert any(lookup_key[0] == RECACHE_FUNC_UID for lookup_key, _receiver in switcher.data_updated.receivers)


def test_setup_precache_retries():
    logger = mock.Mock()

    with replace_setting('PRECACHE_SETUP_RETRIES', None), \
            mock.patch.object(backend, 'time_sleep') as m_sleep, \
            mock.patch.object(backend.client, 'captureException') as m_captureException, \
            mock.patch.object(backend, 'precache', side_effect=[RuntimeError(), None]) as m_precache:
        setup_precache(logger=logger)

    m_sleep.assert_called_once_with(30)
    m_captureException.assert_called_once_with()
    assert m_precache.call_count == 2
    assert logger.mock_calls == [mock.call.info('Precaching...'),
                                 mock.call.info('Unhandled exception in precache:', exc_info=True),
                                 mock.call.info('Retrying in 30 seconds'),
                                 mock.call.info('Precaching...')]


def test_setup_precache_limited_retries():
    logger = mock.Mock()

    with replace_setting('PRECACHE_SETUP_RETRIES', 3), \
            mock.patch.object(backend, 'time_sleep'), \
            mock.patch.object(backend.client, 'captureException'), \
            mock.patch.object(backend, 'precache', side_effect=RuntimeError()) as m_precache, \
            pytest.raises(RuntimeError):
        setup_precache(logger=logger)

    assert m_precache.call_count == 3
