# -*- coding: utf-8 -*-
import datetime
import time
import uuid
from contextlib import contextmanager

import mock
import pytest
from hamcrest import assert_that
from hamcrest import raises, calling
from nose_parameterized import parameterized

import mpfs.core.promo_codes.logic.promo_code_manager
import mpfs.core.promo_codes.logic.promo_code
import mpfs.core.promo_codes.logic.code_generator

from base_suit import BillingApiTestCaseMixin

from mpfs.common.errors import UserIsReadOnly
from mpfs.common.static.tags.billing import PRIMARY_2018_DISCOUNT_10
from mpfs.common.util.experiments.logic import enable_experiment_for_uid
from mpfs.config import settings
from mpfs.core.promo_codes.logic.discount import DiscountTemplate
from mpfs.core.promo_codes.logic.discount_manager import DiscountManager
from test.base import time_machine
from test.conftest import COMMON_DB_IN_POSTGRES
from test.parallelly.billing.base import BillingTestCaseMixin
from test.parallelly.json_api.base import CommonJsonApiTestCase

from base import MpfsBaseTestCase
from mpfs.common.static import codes
from mpfs.common.static.tags import promo_codes
from mpfs.core.promo_codes.dao.promo_codes import (
    PromoCodeArchiveDAO,
    PromoCodeDAO,
)
from mpfs.core.promo_codes.logic.code_generator import PromoCodeGeneratorFactory


PROMO_CODE_RATE_LIMIT = settings.db_rate_limiter['promo_code_activate']['burst']


@contextmanager
def mock_promo_code_product_line():

    before = mpfs.core.promo_codes.logic.promo_code.ServiceProviderPromoCode.default_product_line
    before_generator_line = mpfs.core.promo_codes.logic.code_generator.PROMO_CODE

    try:
        mpfs.core.promo_codes.logic.promo_code.ServiceProviderPromoCode.default_product_line = 'development'
        mpfs.core.promo_codes.logic.code_generator.PROMO_CODE = 'development'
        yield
    finally:
        mpfs.core.promo_codes.logic.promo_code.ServiceProviderPromoCode.default_product_line = before
        mpfs.core.promo_codes.logic.code_generator.PROMO_CODE = before_generator_line


class PromoCodeMethodsMixin(object):
    @staticmethod
    def _generate_promo_code(promo_code=None, pid='test_promo_code_1kb', begin_datetime=None, end_datetime=None,
                             count=1):
        if promo_code is None:
            promo_code = uuid.uuid4().hex.upper()
        cur_time = int(time.time())
        if begin_datetime is None:
            begin_datetime = cur_time - 100
        if end_datetime is None:
            end_datetime = cur_time + 100
        PromoCodeDAO().insert({
            '_id': promo_code,
            'pid': pid,
            'begin_datetime': begin_datetime,
            'end_datetime': end_datetime,
            'count': count,
        })
        return promo_code


class ServicePromoCodeTestCase(PromoCodeMethodsMixin, BillingApiTestCaseMixin, CommonJsonApiTestCase):

    @parameterized.expand([
        ('unlimited', 'test_promo_code_1kb',),
        ('limited', 'test_promo_code_1kb_5sec',)
    ])
    def test_format(self, case_name, pid):
        promo_code = self._generate_promo_code(pid=pid)
        with mock_promo_code_product_line():
            promo_service = self.json_ok('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code})['billing_service']
        if case_name == 'limited':
            assert promo_service.get('expires') is not None
        else:
            assert promo_service.get('expires') is None
        assert promo_service.get('size') == 1024
        assert 'names' in promo_service
        assert all(x in promo_service['names'] for x in ['ru', 'uk', 'tr', 'en'])

    def test_activate_not_used_disposable_promocode(self):
        promo_code = self._generate_promo_code()
        with mock_promo_code_product_line():
            self.json_ok('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code})
        service_list = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        promo_service = {x['name']: x for x in service_list}.get('test_promo_code_1kb')
        assert promo_service is not None
        assert PromoCodeDAO().find_one({'_id': promo_code}) is None

        # проверяем. что правильно отправили в архив
        archived_promo_code = PromoCodeArchiveDAO().find_one({'promo_code': promo_code})
        assert archived_promo_code is not None
        assert archived_promo_code['sid'] == promo_service['sid']
        assert archived_promo_code['uid'] == self.uid
        assert archived_promo_code['pid'] == 'test_promo_code_1kb'
        assert archived_promo_code['activation_timestamp'] is not None
        assert archived_promo_code['status'] == promo_codes.ACTIVATED

    def test_fail_to_activate_missing_promocode(self):
        with mock_promo_code_product_line():
            self.json_error('promo_code_activate', {'uid': self.uid, 'promo_code': 'missing_promocode'}, code=codes.PROMO_CODE_NOT_FOUND)
        service_list = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        assert 'test_promo_code_1kb' not in {x['name'] for x in service_list}

    @parameterized.expand([
        ('too_early_activation', codes.TOO_EARLY_PROMO_CODE_ACTIVATION, 50, 100),
        ('too_late_activation', codes.TOO_LATE_PROMO_CODE_ACTIVATION, -100, -50),
    ])
    def test_fail_to_activate_wrong_time(self, case_name, error_code, begin_datetime_delta, end_datetime_delta):
        self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        cur_time = int(time.time())
        promo_code = self._generate_promo_code(begin_datetime=cur_time + begin_datetime_delta, end_datetime=cur_time + end_datetime_delta)
        with mock_promo_code_product_line():
            resp = self.json_error('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code}, code=error_code)
            assert 'names' in resp['data']
            if case_name == 'too_early_activation':
                assert resp['data']['promo_code_begin_timestamp'] == cur_time + begin_datetime_delta
            else:
                assert resp['data']['promo_code_end_timestamp'] == cur_time + end_datetime_delta
        service_list = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        assert 'test_promo_code_1kb' not in {x['name'] for x in service_list}
        assert PromoCodeDAO().find_one({'_id': promo_code}) is not None

        assert PromoCodeArchiveDAO().find_one({'promo_code': promo_code}) is None

    def test_fail_to_activate_already_activated_promo(self):
        promo_code_1 = self._generate_promo_code()
        promo_code_2 = self._generate_promo_code()
        with mock_promo_code_product_line():
            self.json_ok('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code_1})
            self.json_error('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code_2}, code=codes.USER_ALREADY_HAS_SUCH_PROMO_SERVICE)
        service_list = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        assert 'test_promo_code_1kb' in {x['name'] for x in service_list}
        assert PromoCodeDAO().find_one({'_id': promo_code_2}) is not None

        # проверяем, что правильно отправили в архив
        archived_promo_code_1 = PromoCodeArchiveDAO().find_one({'promo_code': promo_code_1})
        assert PromoCodeArchiveDAO().find_one({'promo_code': promo_code_2}) is None
        assert archived_promo_code_1['status'] == promo_codes.ACTIVATED

    def test_fail_to_activate_already_activated_promo_code(self):
        self.create_user(self.uid_1)
        promo_code = self._generate_promo_code()
        with mock_promo_code_product_line():
            self.json_ok('promo_code_activate', {'uid': self.uid_1, 'promo_code': promo_code})
            self.json_error('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code}, code=codes.ATTEMPT_TO_ACTIVATE_USED_PROMO_CODE)

    def test_reusable_promo_code(self):
        promo_code = self._generate_promo_code(count=2)
        with mock_promo_code_product_line():
            self.json_ok('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code})
        service_list = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        assert 'test_promo_code_1kb' in {x['name'] for x in service_list}
        left_promo_code = PromoCodeDAO().find_one({'_id': promo_code})
        assert left_promo_code is not None and left_promo_code['count'] == 1

        assert len(list(PromoCodeArchiveDAO().find({'promo_code': promo_code}))) == 1

    def test_archiving_reusable_promo_codes(self):
        self.create_user(self.uid_1)
        promo_code = self._generate_promo_code(count=3)
        with mock_promo_code_product_line():
            self.json_ok('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code})
            self.json_ok('promo_code_activate', {'uid': self.uid_1, 'promo_code': promo_code})

        assert PromoCodeDAO().find_one({'_id': promo_code})['count'] == 1
        archived_promo_codes = list(PromoCodeArchiveDAO().find({'promo_code': promo_code}))
        assert len(archived_promo_codes) == 2
        assert all(x['status'] == promo_codes.ACTIVATED for x in archived_promo_codes)

    def test_ratelimiter_working_for_promo_code_activating(self):
        self.create_user(self.uid_1)
        for i in range(PROMO_CODE_RATE_LIMIT):
            self.json_error('promo_code_activate', {'uid': self.uid, 'promo_code': 'fake_code'}, code=codes.PROMO_CODE_NOT_FOUND)
        # следующая попытка приводит к ошибке по частоте запросов
        self.json_error('promo_code_activate', {'uid': self.uid, 'promo_code': 'fake_code'}, code=codes.TOO_MANY_ATTEMPTS_TO_ACTIVATE_PROMO_CODE)

        # но для другого пользователя все работает, как полагается
        self.json_error('promo_code_activate', {'uid': self.uid_1, 'promo_code': 'fake_code'}, code=codes.PROMO_CODE_NOT_FOUND)

    def test_experiment_with_disabled_ratelimiter(self):
        with enable_experiment_for_uid('disable_promo_code_rate_limiter', self.uid):
            for i in range(PROMO_CODE_RATE_LIMIT + 1):
                self.json_error('promo_code_activate', {'uid': self.uid, 'promo_code': 'fake_code'}, code=codes.PROMO_CODE_NOT_FOUND)

    def test_transform_to_uppercase(self):
        promo_code = self._generate_promo_code().lower()
        with mock_promo_code_product_line():
            self.json_ok('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code})

    def test_failed_to_activate_promo_code_0_count(self):
        promo_code = self._generate_promo_code(count=0)
        with mock_promo_code_product_line():
            self.json_error('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code}, code=codes.PROMO_CODE_NOT_FOUND)
        service_list = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        assert 'test_promo_code_1kb' not in {x['name'] for x in service_list}
        assert PromoCodeDAO().find_one({'_id': promo_code}) is not None

    def test_promo_code_service_in_archive_second_activation_failed(self):
        promo_code_1 = self._generate_promo_code(count=1, pid='test_promo_code_1kb_5sec')
        promo_code_2 = self._generate_promo_code(count=1)
        promo_code_3 = self._generate_promo_code(count=1)

        # Была ошибка, при которой поиск в архиве происходил только по уиду, поэтому наличие любой услуги в истории не
        # позволяло активировать промокод
        with mock_promo_code_product_line():
            self.json_ok('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code_1})
        service_list = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        promo_code_service = [x for x in service_list if x['name'] == 'test_promo_code_1kb_5sec'][0]
        self.billing_ok('service_delete', {'uid': self.uid, 'ip': 'localhost', 'sid': promo_code_service['sid']})

        with mock_promo_code_product_line():
            self.json_ok('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code_2})
        service_list = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        promo_code_service = [x for x in service_list if x['name'] == 'test_promo_code_1kb'][0]
        self.billing_ok('service_delete', {'uid': self.uid, 'ip': 'localhost', 'sid': promo_code_service['sid']})
        with mock_promo_code_product_line():
            self.json_error('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code_3}, code=codes.USER_ALREADY_HAS_SUCH_PROMO_SERVICE)


class DiscountPromoCodeTestCase(BillingApiTestCaseMixin, BillingTestCaseMixin, CommonJsonApiTestCase):
    DEFAULT_PERIOD_SECS = 10

    def setup_method(self, method):
        super(DiscountPromoCodeTestCase, self).setup_method(method)
        self.bind_user_to_market_for_uid(self.uid)
        d = DiscountTemplate.create(PRIMARY_2018_DISCOUNT_10, 'test', disposable=True, period_timedelta=datetime.timedelta(seconds=self.DEFAULT_PERIOD_SECS))
        d.save()
        self.discount = d

    @parameterized.expand([
        (True, ),
        (False, ),
    ])
    def test_format(self, disposable):
        d = DiscountTemplate.create(PRIMARY_2018_DISCOUNT_10, 'test', disposable=disposable, period_timedelta=datetime.timedelta(seconds=self.DEFAULT_PERIOD_SECS))
        d.save()
        promo_code = self._generate_promo_code(d.id)
        cur_ts = int(time.time())
        with time_machine(datetime.datetime.fromtimestamp(cur_ts)):
            promo_discount = self.json_ok('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code})['discount']
        assert promo_discount.get('disposable') is disposable
        assert promo_discount.get('end_timestamp') == cur_ts + self.DEFAULT_PERIOD_SECS
        assert promo_discount.get('percentage') == 10

    def test_activate_not_used_disposable_promocode(self):
        promo_code = self._generate_promo_code(self.discount.id)
        self.json_ok('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code})
        service_list = self.billing_ok('verstka_products', {'uid': self.uid})
        assert all('discount' in x for x in service_list['items'])

        # проверяем. что правильно отправили в архив
        archived_promo_code = PromoCodeArchiveDAO().find_one({'promo_code': promo_code})
        assert archived_promo_code is not None
        assert archived_promo_code['discount_template_id'] == self.discount.id
        assert archived_promo_code['uid'] == self.uid
        assert archived_promo_code['activation_timestamp'] is not None
        assert archived_promo_code['status'] == promo_codes.ACTIVATED

    def test_fail_to_activate_already_activated_promo(self):
        promo_code_1 = self._generate_promo_code(self.discount.id)
        promo_code_2 = self._generate_promo_code(self.discount.id)
        self.json_ok('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code_1})
        self.json_error('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code_2}, code=codes.ATTEMPT_TO_ACTIVATE_DISCOUNT_REPEATEDLY)
        service_list = self.billing_ok('verstka_products', {'uid': self.uid})
        assert all('discount' in x for x in service_list['items'])
        assert PromoCodeDAO().find_one({'_id': promo_code_2}) is not None

        # проверяем, что правильно отправили в архив
        archived_promo_code_1 = PromoCodeArchiveDAO().find_one({'promo_code': promo_code_1})
        assert PromoCodeArchiveDAO().find_one({'promo_code': promo_code_2}) is None
        assert archived_promo_code_1['status'] == promo_codes.ACTIVATED

    def test_promo_code_service_in_archive_second_activation_failed(self):
        promo_code_1 = self._generate_promo_code(self.discount.id)
        promo_code_2 = self._generate_promo_code(self.discount.id)

        self.json_ok('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code_1})
        DiscountManager.use_discount(self.uid, self.discount.id)
        self.json_error('promo_code_activate', {'uid': self.uid, 'promo_code': promo_code_2}, code=codes.ATTEMPT_TO_ACTIVATE_DISCOUNT_REPEATEDLY)

    @staticmethod
    def _generate_promo_code(discount_template_id, promo_code=None, begin_datetime=None, end_datetime=None, count=1):
        if promo_code is None:
            promo_code = uuid.uuid4().hex.upper()
        cur_time = int(time.time())
        if begin_datetime is None:
            begin_datetime = cur_time - 100
        if end_datetime is None:
            end_datetime = cur_time + 100
        PromoCodeDAO().insert({
            '_id': promo_code,
            'discount_template_id': discount_template_id,
            'begin_datetime': begin_datetime,
            'end_datetime': end_datetime,
            'count': count,
        })
        return promo_code


class TestPromoCodeGeneratorTestCase(MpfsBaseTestCase):

    def test_generate_service_promo_codes(self):
        begin_timestamp = int(time.time()) - 10
        end_timestamp = int(time.time()) + 10
        pid = 'test_promo_code_1kb'
        with mock_promo_code_product_line():
            p = PromoCodeGeneratorFactory.get_generator(**{'pid': pid, 'begin_timestamp': begin_timestamp, 'end_timestamp': end_timestamp})
            p.generate_and_insert_promo_code()
        promo_codes = list(PromoCodeDAO().find({}))
        assert len(promo_codes) == 1
        assert promo_codes[0]['begin_datetime'] == begin_timestamp
        assert promo_codes[0]['end_datetime'] == end_timestamp
        assert promo_codes[0]['pid'] == pid
        assert promo_codes[0]['count'] == 1

    def test_generate_discount_promo_codes(self):
        begin_timestamp = int(time.time()) - 10
        end_timestamp = int(time.time()) + 10
        d = DiscountTemplate.create(PRIMARY_2018_DISCOUNT_10, 'test', end_datetime=datetime.datetime.fromtimestamp(end_timestamp))
        d.save()
        with mock_promo_code_product_line():
            p = PromoCodeGeneratorFactory.get_generator(**{'discount_template_id': d.id, 'begin_timestamp': begin_timestamp, 'end_timestamp': end_timestamp})
            p.generate_and_insert_promo_code()
        promo_codes = list(PromoCodeDAO().find({}))
        assert len(promo_codes) == 1
        assert promo_codes[0]['begin_datetime'] == begin_timestamp
        assert promo_codes[0]['end_datetime'] == end_timestamp
        assert promo_codes[0]['discount_template_id'] == d.id
        assert promo_codes[0]['count'] == 1

    def test_generate_several_promo_codes(self):
        begin_timestamp = int(time.time()) - 10
        end_timestamp = int(time.time()) + 10
        pid = 'test_promo_code_1kb'
        with mock_promo_code_product_line():
            p = PromoCodeGeneratorFactory.get_generator(**{'pid': pid, 'begin_timestamp': begin_timestamp, 'end_timestamp': end_timestamp})
            for i in range(10):
                p.generate_and_insert_promo_code()
        assert len(list(PromoCodeDAO().find({}))) == 10

    def test_end_timestamp_gt_now(self):
        begin_timestamp = int(time.time()) - 10
        end_timestamp = int(time.time()) - 5
        pid = 'test_promo_code_1kb'
        with mock_promo_code_product_line():
            assert_that(calling(PromoCodeGeneratorFactory.get_generator).with_args(**{'pid': pid, 'begin_timestamp': begin_timestamp, 'end_timestamp': end_timestamp}), raises(ValueError))

    def test_end_timestamp_lt_begin_timestamp(self):
        begin_timestamp = int(time.time()) + 10
        end_timestamp = int(time.time()) + 9
        pid = 'test_promo_code_1kb'
        with mock_promo_code_product_line():
            assert_that(calling(PromoCodeGeneratorFactory.get_generator).with_args(**{'pid': pid, 'begin_timestamp': begin_timestamp, 'end_timestamp': end_timestamp}), raises(ValueError))

    def test_wrong_pid(self):
        begin_timestamp = int(time.time()) - 10
        end_timestamp = int(time.time()) + 10
        pid = 'test_promo_code_1kb_fake'
        with mock_promo_code_product_line():
            assert_that(calling(PromoCodeGeneratorFactory.get_generator).with_args(**{'pid': pid, 'begin_timestamp': begin_timestamp, 'end_timestamp': end_timestamp}), raises(ValueError))

    def test_multiple_activation_num(self):
        begin_timestamp = int(time.time()) - 10
        end_timestamp = int(time.time()) + 10
        pid = 'test_promo_code_1kb'
        activation_num = 10
        with mock_promo_code_product_line():
            p = PromoCodeGeneratorFactory.get_generator(
                **{'pid': pid, 'begin_timestamp': begin_timestamp, 'end_timestamp': end_timestamp, 'activation_num': activation_num})
            p.generate_and_insert_promo_code()
        promo_codes = list(PromoCodeDAO().find({}))
        assert len(promo_codes) == 1
        assert promo_codes[0]['count'] == 10

    def test_pass_code_id(self):
        begin_timestamp = int(time.time()) - 10
        end_timestamp = int(time.time()) + 10
        pid = 'test_promo_code_1kb'
        code_id = 'TEST'
        with mock_promo_code_product_line():
            p = PromoCodeGeneratorFactory.get_generator(
                **{'pid': pid, 'begin_timestamp': begin_timestamp, 'end_timestamp': end_timestamp})
            p.generate_and_insert_promo_code(code_id)
        promo_codes = list(PromoCodeDAO().find({}))
        assert len(promo_codes) == 1
        assert promo_codes[0]['_id'] == code_id

    def test_pass_code_id_lowercase(self):
        begin_timestamp = int(time.time()) - 10
        end_timestamp = int(time.time()) + 10
        pid = 'test_promo_code_1kb'
        code_id = 'test'
        with mock_promo_code_product_line():
            p = PromoCodeGeneratorFactory.get_generator(
                **{'pid': pid, 'begin_timestamp': begin_timestamp, 'end_timestamp': end_timestamp})
            p.generate_and_insert_promo_code(code_id)
        promo_codes = list(PromoCodeDAO().find({}))
        assert len(promo_codes) == 1
        assert promo_codes[0]['_id'] == code_id.upper()


class CommonDBInPGTestCase(MpfsBaseTestCase):
    def test_promocodes(self):
        assert PromoCodeDAO.dao_item_cls.is_migrated_to_postgres == COMMON_DB_IN_POSTGRES or \
               settings.common_pg.get(
                   PromoCodeDAO.dao_item_cls.mongo_collection_name, {}
               ).get('use_pg', False) == COMMON_DB_IN_POSTGRES
        assert PromoCodeArchiveDAO.dao_item_cls.is_migrated_to_postgres == COMMON_DB_IN_POSTGRES or \
               settings.common_pg.get(
                   PromoCodeArchiveDAO.dao_item_cls.mongo_collection_name, {}
               ).get('use_pg', False) == COMMON_DB_IN_POSTGRES


@pytest.mark.skipif(COMMON_DB_IN_POSTGRES, reason='Tests use mongo common')
class PromoCodesReadonlyTestCase(PromoCodeMethodsMixin, MpfsBaseTestCase):

    def test_insert(self):
        with mock.patch.object(PromoCodeDAO.dao_item_cls, 'is_mongo_readonly', return_value=True):
            self.assertRaises(UserIsReadOnly, self._generate_promo_code)
        assert not PromoCodeDAO().count()

    def test_update(self):
        promo_code = self._generate_promo_code()
        with mock.patch.object(PromoCodeDAO.dao_item_cls, 'is_mongo_readonly', return_value=True):
            promo_code_item = PromoCodeDAO().find_promo_code(promo_code)
            self.assertRaises(UserIsReadOnly, PromoCodeDAO().update, promo_code_item, {'$inc': {'count': 10}})

    def test_decrement_count(self):
        promo_code = self._generate_promo_code()
        with mock.patch.object(PromoCodeDAO.dao_item_cls, 'is_mongo_readonly', return_value=True):
            self.assertRaises(UserIsReadOnly, PromoCodeDAO().decrement_count, promo_code)

    def test_remove(self):
        with mock.patch.object(PromoCodeDAO.dao_item_cls, 'is_mongo_readonly', return_value=True):
            self.assertRaises(UserIsReadOnly, PromoCodeDAO().remove)

    def test_read(self):
        promo_code = self._generate_promo_code()
        with mock.patch.object(PromoCodeDAO.dao_item_cls, 'is_mongo_readonly', return_value=True):
            assert PromoCodeDAO().find_promo_code(promo_code)
            assert PromoCodeDAO().find_available_promo_code(promo_code)
            assert PromoCodeDAO().count()
