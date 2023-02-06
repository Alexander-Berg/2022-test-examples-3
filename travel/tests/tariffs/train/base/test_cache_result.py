# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta

import pytest

from common.tester.testcase import TestCase
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.train_api.tariffs.train.base.service import WorkerNetworkError, WorkerParseError, UnknownResultType
from travel.rasp.train_api.tariffs.train.base.worker import WorkerEmptyResultError
from travel.rasp.train_api.tariffs.train.factories.base import create_train_tariffs_query, create_www_setting_cache_timeouts
from travel.rasp.train_api.tariffs.train.im.send_query import ImTariffsResult
from travel.rasp.train_api.train_partners.im.base import ImError


@pytest.mark.dbuser
@replace_setting('TARIFF_SUPPLIERWAIT_TIMEOUT', 10)
def test_cache_timeout():
    create_www_setting_cache_timeouts(**{
        'UFS_CACHE_TIMEOUT': 1,
        'UFS_NETWORK_ERROR_TIMEOUT': 2,
        'UFS_RESPONSE_ERROR_TIMEOUT': 3,
        'UFS_EMPTY_TIMEOUT': 4,
        'UFS_PARSE_ERROR_TIMEOUT': 5,
    })
    im_query = create_train_tariffs_query()

    assert ImTariffsResult(im_query, ImTariffsResult.STATUS_SUCCESS).cache_timeout == 1 * 60
    assert ImTariffsResult(im_query, ImTariffsResult.STATUS_ERROR,
                           error=WorkerNetworkError()).cache_timeout == 0
    assert ImTariffsResult(im_query, ImTariffsResult.STATUS_ERROR,
                           error=WorkerParseError()).cache_timeout == 5 * 60
    assert ImTariffsResult(im_query, ImTariffsResult.STATUS_ERROR,
                           error=ImError(311, 'На заданном направлении (или поезде) мест нет', [])).cache_timeout == 4 * 60
    assert ImTariffsResult(im_query, ImTariffsResult.STATUS_ERROR,
                           error=ImError.from_get_response_error()).cache_timeout == 0
    assert ImTariffsResult(im_query, ImTariffsResult.STATUS_ERROR,
                           error=ImError.from_communication_error()).cache_timeout == 0
    assert ImTariffsResult(im_query, ImTariffsResult.STATUS_ERROR,
                           error=ImError(100500, 'Other Error', [])).cache_timeout == 3 * 60
    assert ImTariffsResult(im_query, ImTariffsResult.STATUS_ERROR,
                           error=WorkerEmptyResultError()).cache_timeout == 4 * 60
    assert ImTariffsResult(im_query, ImTariffsResult.STATUS_PENDING).cache_timeout == 10

    with pytest.raises(UnknownResultType):
        ImTariffsResult(im_query, 'some status').cache_timeout
    with pytest.raises(UnknownResultType):
        ImTariffsResult(im_query, ImTariffsResult.STATUS_ERROR, error=object()).cache_timeout


@pytest.mark.usefixtures('worker_cache_stub')
class TestCache(TestCase):
    @pytest.fixture(autouse=True)
    def add_pytest_fixtures(self, worker_cache_stub):
        self.worker_cache_stub = worker_cache_stub

    def setUp(self):
        self.cache_timeout = 10 * 60
        create_www_setting_cache_timeouts(UFS_CACHE_TIMEOUT=self.cache_timeout // 60)

    def test_cache_success(self):
        query = create_train_tariffs_query()
        result = ImTariffsResult(query, ImTariffsResult.STATUS_SUCCESS)
        result.update_cache()

        assert self.worker_cache_stub.get(query.cache_key) == result
        self.worker_cache_stub.m_global_cache_sync_set.assert_called_with(query.cache_key, result,
                                                                          self.cache_timeout)
        assert not self.worker_cache_stub.m_global_cache_sync_add.called

    @replace_setting('TARIFF_SUPPLIERWAIT_TIMEOUT', 10)
    def test_cache_pending(self):
        query = create_train_tariffs_query()
        result = ImTariffsResult(query, ImTariffsResult.STATUS_PENDING)
        result.update_cache()

        assert self.worker_cache_stub.get(query.cache_key) == result
        self.worker_cache_stub.m_global_cache_sync_add.assert_called_with(query.cache_key, result, 10)
        assert not self.worker_cache_stub.m_global_cache_sync_set.called

    @replace_setting('TARIFF_SUPPLIERWAIT_TIMEOUT', 10)
    def test_override_old_cache(self):
        m_global_cache_sync_set = self.worker_cache_stub.m_global_cache_sync_set
        m_global_cache_sync_add = self.worker_cache_stub.m_global_cache_sync_add
        query = create_train_tariffs_query()

        current_time = datetime(2016, 1, 1)
        with replace_now(current_time):
            result_old = ImTariffsResult(query, ImTariffsResult.STATUS_SUCCESS)
            result_old.update_cache()

        assert self.worker_cache_stub.get(query.cache_key) == result_old
        m_global_cache_sync_set.assert_called_with(query.cache_key, result_old, self.cache_timeout)
        assert not m_global_cache_sync_add.called

        current_time += timedelta(seconds=self.cache_timeout + 1)

        with replace_now(current_time):
            result_new = ImTariffsResult(query, ImTariffsResult.STATUS_PENDING)
            result_new.update_cache()

        assert self.worker_cache_stub.get(query.cache_key) == result_new
        m_global_cache_sync_set.assert_called_with(query.cache_key, result_new, 10)
        assert not m_global_cache_sync_add.called

    @replace_setting('TARIFF_SUPPLIERWAIT_TIMEOUT', 10)
    def test_deleting_pending_result_on_fail(self):
        query = create_train_tariffs_query()
        result = ImTariffsResult(query, ImTariffsResult.STATUS_PENDING)
        result.update_cache()

        assert self.worker_cache_stub.get(query.cache_key) == result
        self.worker_cache_stub.m_global_cache_sync_add.assert_called_with(query.cache_key, result, 10)

        result = ImTariffsResult(
            query,
            ImTariffsResult.STATUS_ERROR,
            error=ImError(2, 'Не удалось получить ответ от поставщика услуг. Попробуйте обратиться позже', [])
        )
        result.update_cache()
        assert self.worker_cache_stub.get(query.cache_key) is None
        self.worker_cache_stub.m_global_cache_sync_delete.assert_called_once_with(query.cache_key)
        assert not self.worker_cache_stub.m_global_cache_sync_set.called
        assert self.worker_cache_stub.m_global_cache_sync_add.call_count == 1

    def test_no_deleting_success_result_on_fail(self):
        query = create_train_tariffs_query()
        good_result = ImTariffsResult(query, ImTariffsResult.STATUS_SUCCESS)
        good_result.update_cache()

        assert self.worker_cache_stub.get(query.cache_key) == good_result
        self.worker_cache_stub.m_global_cache_sync_set.assert_called_with(query.cache_key, good_result,
                                                                          self.cache_timeout)

        result = ImTariffsResult(
            query,
            ImTariffsResult.STATUS_ERROR,
            error=ImError(2, 'Не удалось получить ответ от поставщика услуг. Попробуйте обратиться позже', [])
        )
        result.update_cache()
        assert self.worker_cache_stub.get(query.cache_key) == good_result
        assert not self.worker_cache_stub.m_global_cache_sync_delete.called
        assert self.worker_cache_stub.m_global_cache_sync_set.call_count == 1
        assert not self.worker_cache_stub.m_global_cache_sync_add.called
