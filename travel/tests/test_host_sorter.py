# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from builtins import str
from builtins import range
import random
from unittest import TestCase

import pytest
from mock import Mock, patch

try:
    from travel.rasp.library.python.common23.data_api.dbaas.client import HostInfo
    from travel.rasp.library.python.common23.data_api.dbaas.host_sorter import HostSorter
    has_yandex_cloud_client = True
except ImportError:
    has_yandex_cloud_client = False


class TestHostSorter(TestCase):
    def setUp(self):
        self._fake_logger = Mock()

        self._index = 0

        self._man = self._create_host_info('man')
        self._sas = self._create_host_info('sas')
        self._myt = self._create_host_info('myt')
        self._vla = self._create_host_info('vla')
        self._iva = self._create_host_info('iva')
        self._unknown = self._create_host_info('unknown')

    def _create_host_info(self, dc):
        self._index += 1
        return HostInfo(str(self._index), dc)

    def _run_test(self, dc, host_infos, expected_host_infos):
        with patch.object(random, 'random') as m_random:
            m_random.side_effect = list(range(0, len(host_infos), 1))
            with patch.object(random, 'shuffle'):
                sorter = HostSorter(dc, self._fake_logger)
                assert sorter.sort(host_infos) == expected_host_infos

    @pytest.mark.skipif(not has_yandex_cloud_client, reason="no yandexcloud package")
    def test_sort(self):
        host_infos = [
            self._man,
            self._sas,
            self._myt,
            self._vla,
            self._iva,
        ]

        self._run_test('sas', host_infos, [
            self._sas,
            self._vla,
            self._iva,
            self._myt,
            self._man,
        ])
        self._run_test('iva', host_infos, [
            self._iva,
            self._myt,
            self._sas,
            self._vla,
            self._man,
        ])
        self._run_test('myt', host_infos, [
            self._myt,
            self._iva,
            self._sas,
            self._vla,
            self._man,
        ])
        self._run_test('vla', host_infos, [
            self._vla,
            self._sas,
            self._iva,
            self._myt,
            self._man,
        ])
        self._run_test('man', host_infos, [
            self._man,
            self._iva,
            self._myt,
            self._sas,
            self._vla,
        ])

        assert self._fake_logger.critical.call_count == 0

    @pytest.mark.skipif(not has_yandex_cloud_client, reason="no yandexcloud package")
    def test_unknown_current_dc(self):
        host_infos = [
            self._man,
            self._sas,
            self._myt,
            self._vla,
            self._iva,
        ]

        self._run_test('unknown', host_infos, [
            self._sas,
            self._vla,
            self._iva,
            self._myt,
            self._man,
        ])

        assert self._fake_logger.critical.call_count == 1

    @pytest.mark.skipif(not has_yandex_cloud_client, reason="no yandexcloud package")
    def test_unknown_host_dc(self):
        host_infos = [
            self._sas,
            self._unknown,
            self._myt,
        ]

        self._run_test('sas', host_infos, [
            self._sas,
            self._myt,
            self._unknown,
        ])
        assert self._fake_logger.critical.call_count == 1

    @pytest.mark.skipif(not has_yandex_cloud_client, reason="no yandexcloud package")
    def test_group_by_dc(self):
        host_infos = [
            self._sas,
            self._myt,
            self._sas,
            self._sas,
            self._myt,
        ]

        self._run_test('sas', host_infos, [
            self._sas,
            self._sas,
            self._sas,
            self._myt,
            self._myt
        ])

        assert self._fake_logger.critical.call_count == 0
