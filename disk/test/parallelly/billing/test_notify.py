# -*- coding: utf-8 -*-
import mock

from test.conftest import capture_queue_errors
from test.parallelly.billing.base import (
    BaseBillingTestCase,
    BillingTestCaseMixin,
)


class SubscriptionNotifyTestCase(BillingTestCaseMixin, BaseBillingTestCase):

    def test_dont_send_subscription_prolongate_fail_notify_on_first_payment_fail(self):
        u"""
        Тестируем первую успешную оплату подписки
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True), \
                capture_queue_errors() as errors:
            self.bind_user_to_market('RU')
            number = self.place_order(auto=1)
            self.pay_order(number)

            # письмо не отправляется при первом неудачном платеже
            with mock.patch('mpfs.core.billing.processing.notify.subscription_prolongate_fail') as notify_stub:
                self.manual_fail_callback_on_subscription(number=number)
                notify_stub.assert_not_called()

        assert not errors

    def test_send_subscription_prolongate_fail_notification_workflow(self):
        u"""
        Тестируем первую успешную оплату подписки
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True), \
                capture_queue_errors() as errors:
            self.bind_user_to_market('RU')
            number = self.place_order(auto=1)
            self.pay_order(number)

            # письмо не отправляется при первом платеже
            with mock.patch('mpfs.core.billing.processing.notify.subscription_prolongate_fail') as notify_stub:
                self.manual_success_callback_on_subscription(number=number)
                notify_stub.assert_not_called()

            # письмо отправляется при первом повторном неудачном платеже
            with mock.patch('mpfs.core.billing.processing.notify.subscription_prolongate_fail') as notify_stub:
                self.manual_fail_callback_on_subscription(number=number)
                notify_stub.assert_called_with(self.uid)

            # письмо не отправляется при последующих неудачных платежах
            with mock.patch('mpfs.core.billing.processing.notify.subscription_prolongate_fail') as notify_stub:
                self.manual_fail_callback_on_subscription(number=number)
                notify_stub.assert_not_called()

            # письмо не отправляется при повторном платеже
            with mock.patch('mpfs.core.billing.processing.notify.subscription_prolongate_fail') as notify_stub:
                self.manual_success_callback_on_subscription(number=number)
                notify_stub.assert_not_called()

            # письмо отправляется при первом повторном неудачном платеже
            with mock.patch('mpfs.core.billing.processing.notify.subscription_prolongate_fail') as notify_stub:
                self.manual_fail_callback_on_subscription(number=number)
                notify_stub.assert_called_with(self.uid)

            # письмо не отправляется при последующих неудачных платежах
            with mock.patch('mpfs.core.billing.processing.notify.subscription_prolongate_fail') as notify_stub:
                self.manual_fail_callback_on_subscription(number=number)
                notify_stub.assert_not_called()

        assert not errors
