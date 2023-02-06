# coding: utf-8
import mock

from mpfs.core.billing.order import ArchiveOrder
from mpfs.core.services.trust_payments import PaymentInfo, RefundStatus
from test.helpers.utils import StringWithSuppressedFormating
from test.parallelly.billing.base import BaseBillingTestCase


class RefundTestCase(BaseBillingTestCase):
    @classmethod
    def setup_class(cls):
        super(RefundTestCase, cls).setup_class()
        cls.patch_payment_callback = mock.patch('mpfs.core.billing.interface.orders.get_bb_callbacks',
                                                return_value=(StringWithSuppressedFormating(''),
                                                              StringWithSuppressedFormating('')))
        cls.patch_payment_callback.start()

    @classmethod
    def teardown_class(cls):
        cls.patch_payment_callback.stop()
        super(RefundTestCase, cls).teardown_class()


    def place_order(self, product='test_10GB_for_five_minutes', *args, **kwargs):
        return super(RefundTestCase, self).place_order(product=product, *args, **kwargs)

    @staticmethod
    def patch_trust_method(method, return_value=None):
        return mock.patch(
            'mpfs.core.services.trust_payments.trust_payment_service.%s' % method, return_value=return_value)

    def patch_check_payment(self, **params):
        return self.patch_trust_method('check_payment_by_id', PaymentInfo(params))

    def patch_start_refund(self, **params):
        return self.patch_trust_method('start_refund', RefundStatus(params))

    def patch_get_refund_status(self, **params):
        return self.patch_trust_method('get_refund_status', RefundStatus(params))

    def create_service(self, auto=True):
        self.bind_user_to_market('RU')
        order_id = self.place_order(auto=auto)
        self.pay_order(order_id)
        self.manual_success_callback_on_subscription(order_id)
        return order_id

    def test_create_refund(self):
        order_id = self.create_service()
        services = self.get_services_list()
        assert len(services) == 1

        refund_id = '123'

        order = ArchiveOrder(order_id)
        assert order.get('trust_refund_id') is None

        with self.patch_trust_method('create_refund', refund_id) as create_mock, \
                self.patch_check_payment(payment_status='success') as check_mock, \
                self.patch_start_refund(status='wait_for_notification') as start_mock, \
                self.patch_get_refund_status(status='success') as get_refund_status_mock:
            response = self.billing_ok('refund_order', {'uid': self.uid, 'number': order_id, 'reason': 'qq'})
            assert response['status'] == 'wait_for_notification'
            assert response['can_retry'] is False
            assert response['refund_id'] == refund_id
            assert 'fiscal_receipt_url' in response
            assert 'status_desc' in response
            assert create_mock.called_once()
            assert check_mock.called_once()
            assert start_mock.called_once()
            get_refund_status_mock.assert_not_called()

        order = ArchiveOrder(order_id)
        assert order.get('trust_refund_id') == refund_id

        self.manual_success_callback_on_subscription(order_id, mode='refund_result')

        order = ArchiveOrder(order_id)
        assert order.get('refund_status') == 'success'

        services = self.get_services_list()
        assert len(services) == 0

    def test_get_refund_status(self):
        order_id = self.create_service()
        services = self.get_services_list()
        assert len(services) == 1

        refund_id = '123'

        with self.patch_trust_method('create_refund', refund_id), \
                self.patch_check_payment(payment_status='success'), \
                self.patch_start_refund(status='wait_for_notification'):
            self.billing_ok('refund_order', {'uid': self.uid, 'number': order_id, 'reason': 'qq'})

        with self.patch_get_refund_status(status='success') as get_refund_status_mock:
            response = self.billing_ok('refund_order_status', {'uid': self.uid, 'number': order_id})
            assert response['status'] == 'success'
            assert response['can_retry'] is False
            assert response['refund_id'] == refund_id
            assert 'fiscal_receipt_url' in response
            assert 'status_desc' in response
            assert get_refund_status_mock.called_once()

        self.manual_success_callback_on_subscription(order_id, mode='refund_result')
        services = self.get_services_list()
        assert len(services) == 0

    def test_error_on_already_refunded_order(self):
        order_id = self.create_service()
        services = self.get_services_list()
        assert len(services) == 1

        refund_id = '123'

        with self.patch_trust_method('create_refund', refund_id), \
                self.patch_check_payment(payment_status='success'), \
                self.patch_start_refund(status='wait_for_notification'):
            self.billing_ok('refund_order', {'uid': self.uid, 'number': order_id, 'reason': 'qq'})

        self.manual_success_callback_on_subscription(order_id, mode='refund_result')
        services = self.get_services_list()
        assert len(services) == 0

        self.billing_error('refund_order', {'uid': self.uid, 'number': order_id, 'reason': 'qq'}, code=262)

    def test_error_on_retry_refund(self):
        order_id = self.create_service()
        services = self.get_services_list()
        assert len(services) == 1

        old_refund_id = '123'
        new_refund_id = '234'

        with self.patch_trust_method('create_refund', old_refund_id), \
                self.patch_check_payment(payment_status='success'), \
                self.patch_start_refund(status='wait_for_notification'), \
                self.patch_get_refund_status(status='success') as get_refund_status_mock:
            self.billing_ok('refund_order', {'uid': self.uid, 'number': order_id, 'reason': 'qq'})
            get_refund_status_mock.assert_not_called()

        order = ArchiveOrder(order_id)
        assert order.get('trust_refund_id') == old_refund_id

        with self.patch_trust_method('create_refund', new_refund_id), \
                self.patch_check_payment(payment_status='success'), \
                self.patch_start_refund(status='wait_for_notification'), \
                self.patch_get_refund_status(status='wait_for_notification') as get_refund_status_mock:
            self.billing_error('refund_order', {'uid': self.uid, 'number': order_id, 'reason': 'qq'}, code=262)
            assert get_refund_status_mock.called_once()

        order = ArchiveOrder(order_id)
        assert order.get('trust_refund_id') == old_refund_id

    def test_retry_failed_refund_is_successful(self):
        order_id = self.create_service()
        services = self.get_services_list()
        assert len(services) == 1

        old_refund_id = '123'
        new_refund_id = '234'

        with self.patch_trust_method('create_refund', old_refund_id), \
                self.patch_check_payment(payment_status='success'), \
                self.patch_start_refund(status='wait_for_notification'),\
                self.patch_get_refund_status(status='success') as get_refund_status_mock:
            self.billing_ok('refund_order', {'uid': self.uid, 'number': order_id, 'reason': 'qq'})
            get_refund_status_mock.assert_not_called()

        order = ArchiveOrder(order_id)
        assert order.get('trust_refund_id') == old_refund_id

        with self.patch_trust_method('create_refund', new_refund_id), \
                self.patch_check_payment(payment_status='success'), \
                self.patch_start_refund(status='wait_for_notification'), \
                self.patch_get_refund_status(status='failed') as get_refund_status_mock:
            self.billing_ok('refund_order', {'uid': self.uid, 'number': order_id, 'reason': 'qq'})
            assert get_refund_status_mock.called_once()

        order = ArchiveOrder(order_id)
        assert order.get('trust_refund_id') == new_refund_id

    def test_error_on_not_refundable_payment(self):
        order_id = self.create_service()
        services = self.get_services_list()
        assert len(services) == 1

        with self.patch_check_payment(payment_status='not_authorized'):
            self.billing_error('refund_order', {'uid': self.uid, 'number': order_id, 'reason': 'qq'}, code=264)

    def test_error_on_order_without_payment(self):
        order_id = self.create_service()
        services = self.get_services_list()
        assert len(services) == 1

        with self.patch_trust_method('get_last_payment_id', None):
            self.billing_error('refund_order', {'uid': self.uid, 'number': order_id, 'reason': 'qq'}, code=263)
