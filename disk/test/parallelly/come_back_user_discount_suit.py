# -*- coding: utf-8 -*-
import datetime

import mock
import pytest
from dateutil.relativedelta import relativedelta
from nose_parameterized import parameterized

from mpfs.common.static.tags.billing import PRIMARY_2018_DISCOUNT_20
from mpfs.config import settings
from mpfs.core.job_handlers.discount import handle_provide_discount_to_come_back_user
from mpfs.core.promo_codes.logic.discount import DiscountTemplate
from mpfs.core.promo_codes.logic.discount_manager import DiscountManager
from mpfs.core.services.email_sender_service import email_sender
from mpfs.core.user_activity_info.dao import UserActivityInfoDAO
from mpfs.core.user_activity_info.utils import ErrorInfoContainer
from mpfs.core.promo_codes.logic.come_back_users_promo import provide_discount_if_needed
from test.conftest import INIT_USER_IN_POSTGRES, capture_queue_errors
from test.parallelly.json_api.base import CommonJsonApiTestCase


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='PG implementation only')
@mock.patch('mpfs.core.promo_codes.logic.come_back_users_promo.FEATURE_TOGGLES_COMEBACK_USER_DISCOUNT_ENABLED', True)
@mock.patch('mpfs.core.job_handlers.discount.FEATURE_TOGGLES_REDIRECT_COMEBACK_DISCOUNT_TO_PS_BILLING', False)
class ComeBackUserDiscountSuit(CommonJsonApiTestCase):
    def setup_method(self, method):
        super(ComeBackUserDiscountSuit, self).setup_method(method)
        dt = DiscountTemplate.create(PRIMARY_2018_DISCOUNT_20, '', period_timedelta=datetime.timedelta(days=5))
        dt.save()
        self.discount_template_id = dt.id
        self.dao = UserActivityInfoDAO()
        self.campaign_name = settings.email_sender_campaigns['come_back_users']['templates']['ru']

    @parameterized.expand([
        ('ios', ),
        ('android', ),
    ])
    def test_promo_works_for_come_back_user(self, new_platform):
        assert DiscountManager.get_cheapest_available_discount(self.uid) is None

        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
        ], ErrorInfoContainer(), ))

        with mock.patch('mpfs.core.job_handlers.discount.PROMO_COME_BACK_USER_DISCOUNT_DISCOUNT_TEMPLATE_ID', self.discount_template_id), \
                mock.patch.object(email_sender, 'send') as email_sender_send_mock:
            result = self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
                {'uid': self.uid, 'platform_type': new_platform, 'first_activity': '2019-01-01',
                 'last_activity': '2019-05-01'},
            ], ErrorInfoContainer(),)
            for doc in result:
                provide_discount_if_needed(doc['uid'], doc['activity_before_update'], doc['activity_after_update'])

        assert DiscountManager.get_cheapest_available_discount(self.uid).discount_template_id == self.discount_template_id
        assert email_sender_send_mock.called
        assert email_sender_send_mock.call_args[0][0] == self.email
        assert email_sender_send_mock.call_args[0][1] == self.campaign_name

    @parameterized.expand([
        ('ios',),
        ('android',),
    ])
    def test_promo_works_for_new_user(self, new_platform):
        assert DiscountManager.get_cheapest_available_discount(self.uid) is None

        with mock.patch('mpfs.core.job_handlers.discount.PROMO_COME_BACK_USER_DISCOUNT_DISCOUNT_TEMPLATE_ID', self.discount_template_id), \
                mock.patch.object(email_sender, 'send') as email_sender_send_mock:
            result = self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
                {'uid': self.uid, 'platform_type': new_platform, 'first_activity': '2019-01-01',
                 'last_activity': '2019-05-01'},
            ], ErrorInfoContainer(), )
            for doc in result:
                provide_discount_if_needed(doc['uid'], doc['activity_before_update'], doc['activity_after_update'])

        assert DiscountManager.get_cheapest_available_discount(self.uid).discount_template_id == self.discount_template_id
        assert email_sender_send_mock.called
        assert email_sender_send_mock.call_args[0][0] == self.email
        assert email_sender_send_mock.call_args[0][1] == self.campaign_name

    @parameterized.expand([
        ('ios',),
        ('android',),
    ])
    def test_promo_doesnt_work_for_regularly_active_user(self, new_platform):
        assert DiscountManager.get_cheapest_available_discount(self.uid) is None

        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
        ], ErrorInfoContainer(), ))

        with mock.patch('mpfs.core.job_handlers.discount.PROMO_COME_BACK_USER_DISCOUNT_DISCOUNT_TEMPLATE_ID', self.discount_template_id), \
                mock.patch.object(email_sender, 'send') as email_sender_send_mock:
            result = self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
                {'uid': self.uid, 'platform_type': new_platform, 'first_activity': '2019-01-01',
                 'last_activity': '2019-02-01'},
            ], ErrorInfoContainer(), )
            for doc in result:
                provide_discount_if_needed(doc['uid'], doc['activity_before_update'], doc['activity_after_update'])

        assert DiscountManager.get_cheapest_available_discount(self.uid) is None
        assert not email_sender_send_mock.called

    @parameterized.expand([
        ('andr-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
        ('ios-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
    ])
    def test_promo_works_via_user_info_for_come_back_user(self, ycrid):
        assert DiscountManager.get_cheapest_available_discount(self.uid) is None
        old_last_activity = datetime.date.today() + relativedelta(months=-3)

        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2010-01-01', 'last_activity': str(old_last_activity)},
        ], ErrorInfoContainer(), ))

        with mock.patch('mpfs.core.job_handlers.discount.PROMO_COME_BACK_USER_DISCOUNT_DISCOUNT_TEMPLATE_ID', self.discount_template_id), \
                mock.patch('mpfs.engine.process.get_cloud_req_id', return_value=ycrid), \
                mock.patch.object(email_sender, 'send') as email_sender_send_mock:
            self.json_ok('user_info', {'uid': self.uid})

        assert DiscountManager.get_cheapest_available_discount(self.uid).discount_template_id == self.discount_template_id
        assert email_sender_send_mock.called
        assert email_sender_send_mock.call_args[0][0] == self.email
        assert email_sender_send_mock.call_args[0][1] == self.campaign_name

    @parameterized.expand([
        ('andr-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
        ('ios-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
    ])
    def test_promo_works_via_user_info_for_new_user(self, ycrid):
        assert DiscountManager.get_cheapest_available_discount(self.uid) is None

        with mock.patch('mpfs.core.job_handlers.discount.PROMO_COME_BACK_USER_DISCOUNT_DISCOUNT_TEMPLATE_ID', self.discount_template_id), \
                mock.patch('mpfs.engine.process.get_cloud_req_id', return_value=ycrid), \
                mock.patch.object(email_sender, 'send') as email_sender_send_mock:
            self.json_ok('user_info', {'uid': self.uid})

        assert DiscountManager.get_cheapest_available_discount(self.uid).discount_template_id == self.discount_template_id
        assert email_sender_send_mock.called
        assert email_sender_send_mock.call_args[0][0] == self.email
        assert email_sender_send_mock.call_args[0][1] == self.campaign_name

    @parameterized.expand([
        ('andr-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
        ('ios-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
    ])
    def test_promo_doesnt_work_via_user_info_for_regularly_active_user(self, ycrid):
        assert DiscountManager.get_cheapest_available_discount(self.uid) is None
        old_last_activity = datetime.date.today() + relativedelta(months=-3, days=1)

        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2010-01-01', 'last_activity': str(old_last_activity)},
        ], ErrorInfoContainer(), ))

        with mock.patch('mpfs.core.job_handlers.discount.PROMO_COME_BACK_USER_DISCOUNT_DISCOUNT_TEMPLATE_ID', self.discount_template_id), \
                mock.patch('mpfs.engine.process.get_cloud_req_id', return_value=ycrid), \
                mock.patch.object(email_sender, 'send') as email_sender_send_mock:
            self.json_ok('user_info', {'uid': self.uid})

        assert DiscountManager.get_cheapest_available_discount(self.uid) is None
        assert not email_sender_send_mock.called

    @parameterized.expand([
        ('ios',),
        ('android',),
    ])
    def test_provide_discount_repeatedly(self, new_platform):
        old_last_activity = datetime.date.today() + relativedelta(months=-6)

        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2010-01-01',
             'last_activity': str(old_last_activity)},
        ], ErrorInfoContainer(), ))

        old_last_activity = datetime.date.today() + relativedelta(months=-3)
        with mock.patch('mpfs.core.job_handlers.discount.PROMO_COME_BACK_USER_DISCOUNT_DISCOUNT_TEMPLATE_ID', self.discount_template_id):
            result = self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
                {'uid': self.uid, 'platform_type': new_platform, 'first_activity': '2010-01-01',
                 'last_activity': str(old_last_activity)},
            ], ErrorInfoContainer(), )
            for doc in result:
                provide_discount_if_needed(doc['uid'], doc['activity_before_update'], doc['activity_after_update'])

        assert DiscountManager.get_cheapest_available_discount(self.uid).discount_template_id == self.discount_template_id
        DiscountManager.archive_discount(self.uid, self.discount_template_id)

        new_activity = datetime.date.today()
        with mock.patch('mpfs.core.job_handlers.discount.PROMO_COME_BACK_USER_DISCOUNT_DISCOUNT_TEMPLATE_ID',
                        self.discount_template_id), \
                mock.patch.object(email_sender, 'send') as email_sender_send_mock:
            result = self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
                {'uid': self.uid, 'platform_type': new_platform, 'first_activity': '2010-01-01',
                 'last_activity': str(new_activity)},
            ], ErrorInfoContainer(), )
            for doc in result:
                provide_discount_if_needed(doc['uid'], doc['activity_before_update'], doc['activity_after_update'])

        assert DiscountManager.get_cheapest_available_discount(self.uid).discount_template_id == self.discount_template_id
        assert email_sender_send_mock.called
        assert email_sender_send_mock.call_args[0][0] == self.email
        assert email_sender_send_mock.call_args[0][1] == self.campaign_name

    @parameterized.expand([
        ('ios',),
        ('android',),
    ])
    def test_concurrent_attempt_to_provide_discount(self, new_platform):
        old_last_activity = datetime.date.today() + relativedelta(months=-6)

        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid, 'platform_type': 'ios', 'first_activity': '2010-01-01',
             'last_activity': str(old_last_activity)},
        ], ErrorInfoContainer(), ))

        old_last_activity = datetime.date.today() + relativedelta(months=-3)
        with mock.patch('mpfs.core.job_handlers.discount.PROMO_COME_BACK_USER_DISCOUNT_DISCOUNT_TEMPLATE_ID',
                        self.discount_template_id), \
                mock.patch.object(email_sender, 'send') as email_sender_send_mock:
            result = list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
                {'uid': self.uid, 'platform_type': new_platform, 'first_activity': '2010-01-01',
                 'last_activity': str(old_last_activity)},
            ], ErrorInfoContainer(), ))
            for doc in result:
                provide_discount_if_needed(doc['uid'], doc['activity_before_update'], doc['activity_after_update'])

            assert DiscountManager.get_cheapest_available_discount(self.uid).discount_template_id == self.discount_template_id
            assert email_sender_send_mock.called
            assert email_sender_send_mock.call_args[0][0] == self.email
            assert email_sender_send_mock.call_args[0][1] == self.campaign_name
            email_sender_send_mock.reset_mock()

            with capture_queue_errors() as errors:
                for doc in result:
                    provide_discount_if_needed(doc['uid'], doc['activity_before_update'], doc['activity_after_update'])
            assert DiscountManager.get_cheapest_available_discount(self.uid).discount_template_id == self.discount_template_id
            assert not email_sender_send_mock.called
            assert not errors

    @parameterized.expand([
        ('andr-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
        ('ios-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
        ('rest_ios-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
        ('win-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
        ('mac-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
        ('public-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
        ('web-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
    ])
    def test_provide_discount_on_user_init(self, ycrid):
        with mock.patch('mpfs.core.job_handlers.discount.PROMO_COME_BACK_USER_DISCOUNT_DISCOUNT_TEMPLATE_ID',
                        self.discount_template_id), \
                mock.patch('mpfs.engine.process.get_cloud_req_id', return_value=ycrid), \
                mock.patch.object(email_sender, 'send') as email_sender_send_mock:
            self.json_ok('user_init', {'uid': self.uid_1})

        assert DiscountManager.get_cheapest_available_discount(self.uid_1).discount_template_id == self.discount_template_id
        assert email_sender_send_mock.called
        assert email_sender_send_mock.call_args[0][0] == self.email_1
        assert email_sender_send_mock.call_args[0][1] == self.campaign_name

    @parameterized.expand([
        ('rest-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
        ('mpfs-ece8527244bbd2d5535a0ba5f189d0c4-api03v',),
    ])
    def test_do_not_provide_discount_on_user_init_for_wrong_ycrid(self, ycrid):
        with mock.patch('mpfs.core.job_handlers.discount.PROMO_COME_BACK_USER_DISCOUNT_DISCOUNT_TEMPLATE_ID',
                        self.discount_template_id), \
                mock.patch('mpfs.engine.process.get_cloud_req_id', return_value=ycrid), \
                mock.patch.object(email_sender, 'send') as email_sender_send_mock:
            self.json_ok('user_init', {'uid': self.uid_1})

        assert DiscountManager.get_cheapest_available_discount(self.uid_1) is None
        assert not email_sender_send_mock.called

    def test_no_discount_for_blacklisted_users(self):
        ycrid = 'andr-ece8527244bbd2d5535a0ba5f189d0c4-api03v'
        with mock.patch('mpfs.core.job_handlers.discount.PROMO_COME_BACK_USER_DISCOUNT_DISCOUNT_TEMPLATE_ID',
                        self.discount_template_id), \
                mock.patch('mpfs.core.promo_codes.logic.come_back_users_promo.PROMO_COME_BACK_USER_DISCOUNT_BLACK_LIST',
                           [self.uid]), \
                mock.patch('mpfs.engine.process.get_cloud_req_id', return_value=ycrid), \
                mock.patch.object(email_sender, 'send') as email_sender_send_mock:
            self.json_ok('user_init', {'uid': self.uid})
        assert DiscountManager.get_cheapest_available_discount(self.uid) is None
        assert not email_sender_send_mock.called

    @parameterized.expand([
        ('Ru-user:redirect', {'country': 'ru'}, True, False),
        ('Foreign-user: provide', {'country': 'non-ru'}, False, True),
        ('Unknown country: ignore', {}, False, False),
    ])
    def test_billing_redirect(self, name, user_info_result, redirect_expected, discount_expected):
        with mock.patch('mpfs.core.job_handlers.discount.FEATURE_TOGGLES_REDIRECT_COMEBACK_DISCOUNT_TO_PS_BILLING', True), \
            mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active', return_value=True), \
                mock.patch('mpfs.core.services.passport_service.Passport.userinfo', return_value=user_info_result), \
                mock.patch('mpfs.core.services.ps_billing_service.PsBillingService.activate_promo') as activate_promo_mock, \
                mock.patch(
                    'mpfs.core.promo_codes.logic.discount_manager.DiscountManager.add_discount_to_user') as add_discount_mock, \
                mock.patch('mpfs.core.email.logic.send_email_async_by_uid'),\
                mock.patch('mpfs.core.billing.product.ProductCard.get_current_products_for'):
            handle_provide_discount_to_come_back_user(self.uid)
            redirected = activate_promo_mock.called
            discount_provided = add_discount_mock.called
            assert redirected == redirect_expected
            assert discount_provided == discount_expected
