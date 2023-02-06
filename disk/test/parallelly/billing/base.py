# -*- coding: utf-8 -*-
import datetime
import mock
import time
import datetime

from mpfs.core.billing.constants import PRODUCT_INITIAL_10GB_ID
from test.base import DiskTestCase
from test.base_suit import BillingApiTestCaseMixin
from test.fixtures.users import pdd_user, user_1
from test.helpers.size_units import GB
from test.helpers.stubs.services import BigBillingStub

import mpfs.engine.process
import mpfs.core.services.billing_service
from mpfs.common.errors.billing import BillingError
from mpfs.core.services.billing_service import BillingOrderInfo

from mpfs.common.static.tags.billing import *
from mpfs.common.util import ctimestamp
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.promo_codes.logic.discount import DiscountTemplate
from mpfs.core.promo_codes.logic.discount_manager import DiscountManager
from test.parallelly.social_suit import CommonSocialMethods

db = CollectionRoutedDatabase()

BB_FAKE_BTIME = ctimestamp()
BB_FAKE_BTIME_MSEC = BB_FAKE_BTIME * 1000
BB_FAKE_LBTIME_MSEC = (BB_FAKE_BTIME - 3600) * 1000


def fake_check_order_good(uid, ip, number, pid):
    return BillingOrderInfo({
        STATUS: SUCCESS,
        SUBS_UNTIL: BB_FAKE_BTIME,
        SUBS_UNTIL_MSEC: BB_FAKE_BTIME_MSEC,
        PAYMENT_TS_MSEC: BB_FAKE_LBTIME_MSEC,
        PRODUCT_TYPE: APP,
    })


def fake_check_order_bad(uid, ip, number, pid):
    return BillingOrderInfo({
        STATUS: SUCCESS,
        SUBS_UNTIL: BB_FAKE_BTIME,
        SUBS_UNTIL_MSEC: BB_FAKE_BTIME_MSEC,
        FUCKUP_PERIOD: '2d',
        PAYMENT_TS_MSEC: BB_FAKE_LBTIME_MSEC,
        PRODUCT_TYPE: APP,
    })


def fake_check_order_error(uid, ip, number, pid):
    return BillingOrderInfo({
        STATUS: ERROR,
        SUBS_UNTIL: BB_FAKE_BTIME,
        SUBS_UNTIL_MSEC: BB_FAKE_BTIME_MSEC,
        PAYMENT_TS_MSEC: BB_FAKE_LBTIME_MSEC,
        PRODUCT_TYPE: APP
    })


def fake_check_order_cancelled(uid, ip, number, pid):
    return BillingOrderInfo({
        STATUS: CANCELLED,
        SUBS_UNTIL: BB_FAKE_BTIME,
        SUBS_UNTIL_MSEC: BB_FAKE_BTIME_MSEC,
        PAYMENT_TS_MSEC: BB_FAKE_LBTIME_MSEC,
        PRODUCT_TYPE: APP
    })


def fake_check_order_unpaid(uid, ip, number, pid):
    raise BillingError()


def fake_check_order_subscription_stopped(uid, ip, number, pid):
    return BillingOrderInfo({SUBS_UNTIL: 111, FINISH_TS: 111})


def fake_check_order_subscription_subs_until_in_past(uid, ip, number, pid):
    return BillingOrderInfo({SUBS_UNTIL: 111, STATUS: SUCCESS})


def fake_check_order_subscription_subs_until_in_past_small(uid, ip, number, pid):
    return BillingOrderInfo({SUBS_UNTIL: ctimestamp() - 86400 * 4, STATUS: SUCCESS})


mpfs.core.services.billing_service.BB.log = mpfs.engine.process.get_default_log()
mpfs.core.services.billing_service.BB.check_order = fake_check_order_good


class BillingTestCaseMixin(BillingApiTestCaseMixin):
    localhost = '127.0.0.1'

    def bind_user_to_market_for_uid(self, uid, market='RU'):
        args = {
            'uid': uid,
            'market': market,
            'ip': self.localhost,
        }
        return self.billing_ok('client_bind_market', args)

    def create_subscription(self, uid, line='development', pid='test_1kb_for_five_seconds'):
        order_place_args = {
            'uid': uid,
            'pid': pid,
            'line': line,
            'payment_method': 'bankcard',
            'ip': self.localhost,
            'auto': 1,
        }
        with BigBillingStub():
            number = self.billing_ok('order_place', order_place_args)['number']
            with mock.patch('mpfs.core.services.billing_service.BillingServiceRouter.check_order', return_value={'status': 'success'}), \
                    mock.patch('mpfs.frontend.api.auth.TVM2.authorize'):
                self.billing_ok('subscription_process_callback', {'uid': uid, 'number': number, 'status': 'success', 'status_code': ''})

    def create_service(self, uid, line='development', pid='test_1kb_for_five_seconds'):
        order_place_args = {
            'uid': uid,
            'pid': pid,
            'line': line,
            'payment_method': 'bankcard',
            'ip': self.localhost,
            'auto': 0,
        }
        with BigBillingStub():
            number = self.billing_ok('order_place', order_place_args)['number']
            with mock.patch('mpfs.core.services.billing_service.BillingServiceRouter.check_order', return_value={'status': 'success'}), \
                    mock.patch('mpfs.frontend.api.auth.TVM2.authorize'):
                self.billing_ok('order_process_callback', {'uid': uid, 'number': number, 'status': 'success', 'status_code': ''})

    def order_place(self, uid, pid='test_1kb_for_five_seconds'):
        order_place_args = {
            'uid': uid,
            'pid': pid,
            'line': 'development',
            'payment_method': 'bankcard',
            'ip': self.localhost,
            'auto': 0,
        }
        with BigBillingStub():
            number = self.billing_ok('order_place', order_place_args)[0]['number']
            return number


class BaseBillingTestCase(CommonSocialMethods):
    attach_uid = user_1.uid
    pdd_uid = pdd_user.uid
    localhost = 'http://localhost/service/echo'
    DEFAULT_SPACE_LIMIT = 10*GB

    def setup_method(self, method):
        super(BaseBillingTestCase, self).setup_method(method)
        mpfs.core.services.billing_service.BB.check_order = fake_check_order_good

        from mpfs.core.billing.processing import marketing
        marketing.line_export('primary_2013', ctimestamp(), False)
        marketing.line_export('primary_2014', ctimestamp(), False)
        marketing.line_export('development', ctimestamp(), False)

        self.sid = '036dcad2c443e6029a5754e85bcc5eab'
        self.number = '39387057'
        self.service_doc = {
            '_id': self.sid,
            'auto': False,
            'btime': 1512558290,
            'child_sids': None,
            'ctime': 1512558285,
            'enabled': True,
            'group': False,
            'group_name': None,
            'lbtime': 1512558285,
            'mtime': 1512558285,
            'order': '564476235',
            'parent_sid': None,
            'state': None,
            'uid': self.uid,
            'v': 1512558285034209
        }

        cur_time = int(time.time())
        self.stale_order_doc = {
            '_id': self.number,
            'auto': False,
            'bb_pid': u'test_1kb_for_five_seconds_app',
            'ctime': cur_time - 10000,  # Делаем так, чтобы заказ попадал под архивируемые
            'currency': 'RUB',
            'locale': u'ru',
            'market': u'RU',
            'mtime': 1512558285,
            'otype': 'buy_new',
            'payment_method': u'bankcard',
            'price': 30,
            'state': 'new',
            'uid': self.uid,
            'sid': self.sid,
            'v': 1512558285045828
        }

        self.service_attrs_doc = {
            '_id': self.sid,
            'amount': 5368709120,
            'uid': self.uid,
            'v': '1446799496906915',
        }

    def bind_user_to_market(self, market='COM', uid=None):
        uid = uid or self.uid
        args = {
            'uid': uid,
            'market': market,
            'ip': self.localhost,
        }
        result = self.billing_ok('client_bind_market', args)

    def place_order(self, product='test_1kb_for_one_second', pay='bankcard',
                    auto=0, line='development', group_uids=None, group_name=None, domain=None, locale=None):
        args = {
            'uid': self.uid,
            'pid': product,
            'line': line,
            'payment_method': pay,
            'ip': self.localhost,
            'auto': auto,
        }

        if group_uids:
            args['group_uids'] = ','.join(group_uids)
        if group_name:
            args['group_name'] = group_name
        if domain:
            args['tld'] = domain
        if locale:
            args['locale'] = locale

        result = self.billing_ok('order_place', args)
        self.assertTrue(result is not None)
        return result['number']

    def pay_order(self, number=None, domain=None, template_tag=None, pid=None):
        args = {
            'uid': self.uid,
            'number': number,
            'ip': self.localhost,
        }
        if domain:
            args['tld'] = domain
        if template_tag:
            args['template_tag'] = template_tag
        if pid:
            args['pid'] = pid
        return self.billing_ok('order_make_payment', args)

    def manual_success_callback_on_order(self, number=None, mode=None, trust_refund_id=None):
        args = {
            'uid': self.uid,
            'number': number,
            'status': 'success',
            'status_code': '',
        }
        if mode is not None:
            args['mode'] = mode
        if trust_refund_id is not None:
            args['trust_refund_id'] = trust_refund_id
        with mock.patch('mpfs.frontend.api.auth.TVM2.authorize'):
            self.billing_ok('order_process_callback', args)

    def manual_fail_callback_on_order(self, number=None, status=None, status_code=None):
        args = {
            'uid': self.uid,
            'number': number,
            'status': status,
            'status_code': status_code,
        }
        with mock.patch('mpfs.frontend.api.auth.TVM2.authorize'):
            self.billing_ok('order_process_callback', args)

    def manual_success_callback_on_subscription(self, number=None, mode=None, binding_result=None, trust_refund_id=None):
        args = {
            'uid': self.uid,
            'number': number,
            'status': 'success',
            'status_code': '',
        }
        if mode is not None:
            args['mode'] = mode
        if binding_result is not None:
            args['binding_result'] = binding_result
        if trust_refund_id is not None:
            args['trust_refund_id'] = trust_refund_id
        with mock.patch('mpfs.frontend.api.auth.TVM2.authorize'):
            self.billing_ok('subscription_process_callback', args)

    def manual_fail_callback_on_subscription(self, number=None, status=None, status_code=None, mode=None):
        args = {
            'uid': self.uid,
            'number': number,
            'status': status,
            'status_code': status_code,
        }
        if mode is not None:
            args['mode'] = mode
        with mock.patch('mpfs.frontend.api.auth.TVM2.authorize'):
            self.billing_ok('subscription_process_callback', args)

    def manual_success_callback(self, number=None, auto=True):
        if auto:
            self.manual_success_callback_on_subscription(number)
        self.manual_success_callback_on_order(number)

    def get_services_list(self, uid=None, pid=None, with_default_products=False):
        uid = uid if uid else self.uid
        args = {
            'uid': uid,
            'ip': self.localhost,
        }
        services = self.billing_ok('service_list', args)
        if pid is None:
            if with_default_products:
                return services
            else:
                default_pids = [PRODUCT_INITIAL_10GB_ID]
                return [service
                        for service in services
                        if service['name'] not in default_pids]
        return [x for x in services if x['name'] == pid]

    def service_unsubscribe(self, sid):
        args = {
            'uid': self.uid,
            'ip': self.localhost,
            'sid': sid,
        }
        return self.billing_ok('service_unsubscribe', args)

    def service_delete(self, sid=None, pid=None):
        assert sid or pid
        args = {
            'uid': self.uid,
            'ip': self.localhost,
        }
        if sid:
            args['sid'] = sid
        if pid:
            args['pid'] = pid

        result = self.billing_ok('service_delete', args)

        if sid:
            archive_service = db.billing_services_history.find_one({'sid': sid})
        if pid:
            archive_service = db.billing_services_history.find_one({'uid': self.uid, 'pid': pid})

        self.assertTrue(archive_service is not None)

        db.billing_subscriptions.remove({'uid': self.uid, 'sid': sid})

        return result

    def service_create(self, pid=None, line=None, uid=None, market=None):
        uid = uid if uid else self.uid
        args = {
            'uid': uid,
            'line': line,
            'pid': pid,
            'ip': self.localhost,
            'connection_id': '111',
        }
        if market:
            args['market'] = market
        return self.billing_ok('service_create', args).get('sid')

    def service_manual_set_params(self, sid, uid=None, **kwargs):
        update_spec = {'$set': kwargs}

        uid = uid if uid is not None else self.uid
        db.billing_services.update(
            {'_id': sid, 'uid': uid},
            update_spec,
            **mpfs.engine.process.dbctl().fsync_safe_w()
        )

    def create_discount(self, line, description='test', disponsable=True):
        discount = DiscountTemplate.create(line, description=description, disposable=disponsable)
        discount.save()
        return discount.id

    @staticmethod
    def patch_check_order(subs_until_ts_msec,  payment_ts_msec=None, status='success', finish_ts=None):

        check_order_response = BillingOrderInfo({
            SUBS_UNTIL: subs_until_ts_msec / 1000,
            SUBS_UNTIL_MSEC: subs_until_ts_msec,
            STATUS: status,
        })
        if payment_ts_msec is not None:
            check_order_response['payment_ts_msec'] = payment_ts_msec
        if finish_ts is not None:
            check_order_response[FINISH_TS] = finish_ts
        return mock.patch('mpfs.core.services.billing_service.BB.check_order', return_value=check_order_response)

    def give_discount(self, uid, line, period=None, discount_template_id=None, end_datetime=None, disposable=True):
        if line in (PRIMARY_2018, PRIMARY_2019_V4):
            return
        if discount_template_id is None:
            if end_datetime is not None:
                d = DiscountTemplate.create(line, 'test', disposable=disposable, end_datetime=end_datetime)
            else:
                if period is not None:
                    period = datetime.timedelta(seconds=period)
                d = DiscountTemplate.create(line, 'test', disposable=disposable, period_timedelta=period)
            d.save()
            discount_template_id = d.id
        DiscountManager.add_discount_to_user(uid, discount_template_id)
        return discount_template_id
