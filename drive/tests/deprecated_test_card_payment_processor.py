import datetime

from django.test import TestCase, TransactionTestCase
from django.utils import timezone

from cars.core.trust import StubTrustClient
from cars.users.factories.user import UserFactory
from ..core.payment_processor import CardPaymentProcessor
from ..factories.card_payment import CardPaymentFactory
from ..models.card_payment import CardPayment


class CardPaymentProcessorTestCaseMixin:

    def setUp(self):
        self.user = UserFactory.create(uid=1)

        self.trust_client = StubTrustClient(default_product_id='777')
        self.trust_client.create_product(
            name='test',
            product_id='777',
            product_type='app',
        )
        self.paymethod_id = self.trust_client.create_payment_method(
            uid=self.user.uid,
            id_='card-1234'
        )['id']

        self.processor = CardPaymentProcessor(
            trust_client=self.trust_client,
            fiscal_title='[test] Yandex.Drive',
            fiscal_nds='nds_18',
            clear_delay=datetime.timedelta(days=1),
        )

        self.payment = self.make_payment()

    def tearDown(self):
        self.trust_client.clear()

    def assert_payment_status_equal(self, payment, status):
        payment = self.update_payment(payment)
        self.assertEqual(payment.get_status(), status, vars(payment))

    def make_payment(self, paymethod_id=None, amount=100):
        if paymethod_id is None:
            paymethod_id = self.paymethod_id

        return CardPaymentFactory.create(
            user=self.user,
            paymethod_id=paymethod_id,
            amount=amount,
        )

    def set_payment_date(self, payment, date):
        payment.created_at = date
        if payment.updated_at:
            payment.updated_at = date
        if payment.started_at:
            payment.started_at = date
        if payment.completed_at:
            payment.completed_at = date
        payment.save()

    def update_payment(self, payment):
        return CardPayment.objects.get(id=payment.id)


class CardPaymentProcessorTransactionTestCase(CardPaymentProcessorTestCaseMixin,
                                              TransactionTestCase):
    """Inherit from TransactionTestCase to handle threaded methods"""

    def test_process_all_initialize(self):
        self.processor.process_all()
        self.assert_payment_status_equal(self.payment, CardPayment.Status.NOT_STARTED)

    def test_wait_for_completion_success(self):
        self.trust_client.set_post_start_action(self.trust_client.PostStartAction.AUTHORIZE)

        payment = self.make_payment()
        payment = self.processor.wait_for_completion([payment], timeout=0.1)[0]

        payment.refresh_from_db()
        self.assert_payment_status_equal(payment, CardPayment.Status.AUTHORIZED)

    def test_wait_for_completion_timeout(self):
        payment = self.make_payment()

        processed_payment = self.processor.wait_for_completion([payment], timeout=0.1)[0]
        self.assertIsNone(processed_payment)

        payment.refresh_from_db()
        self.assert_payment_status_equal(payment, CardPayment.Status.STARTED)


class CardPaymentProcessorTestCase(CardPaymentProcessorTestCaseMixin, TestCase):

    def test_initialize_ok(self):
        self.assert_payment_status_equal(self.payment, CardPayment.Status.DRAFT)

        self.processor.initialize_payment(self.payment)

        self.assert_payment_status_equal(self.payment, CardPayment.Status.NOT_STARTED)

    def test_initialize_twice(self):
        self.processor.initialize_payment(self.payment)

        payment = CardPayment.objects.get(id=self.payment.id)
        self.processor.initialize_payment(payment)

        self.assert_payment_status_equal(self.payment, CardPayment.Status.NOT_STARTED)

    def test_process_initialize(self):
        self.processor.process(self.payment)
        self.assert_payment_status_equal(self.payment, CardPayment.Status.NOT_STARTED)

    def test_initialize_invalid_payment_method(self):
        payment = self.make_payment(paymethod_id='fake')
        self.processor.initialize_payment(payment)
        self.assert_payment_status_equal(payment, CardPayment.Status.INVALID_PAYMENT_METHOD)

    def test_start_ok(self):
        self.processor.initialize_payment(self.payment)
        self.processor.start_payment(self.payment)
        self.assert_payment_status_equal(self.payment, CardPayment.Status.STARTED)

    def test_start_twice(self):
        self.processor.initialize_payment(self.payment)
        self.processor.start_payment(self.payment)
        self.processor.start_payment(self.payment)
        self.assert_payment_status_equal(self.payment, CardPayment.Status.STARTED)

    def test_process_start(self):
        self.processor.initialize_payment(self.payment)
        self.processor.process(self.payment)
        self.assert_payment_status_equal(self.payment, CardPayment.Status.STARTED)

    def test_authorized(self):
        self.processor.initialize_payment(self.payment)
        self.processor.start_payment(self.payment)
        prev_updated_at = self.payment.updated_at

        self.processor.update_started_payment(self.payment)
        payment = self.update_payment(self.payment)
        self.assert_payment_status_equal(payment, CardPayment.Status.STARTED)
        self.assertEqual(payment.updated_at, prev_updated_at)

        self.trust_client.authorize_payment(
            uid=self.payment.user.uid,
            purchase_token=self.payment.purchase_token,
        )
        self.processor.update_started_payment(self.payment)
        payment = self.update_payment(self.payment)
        self.assert_payment_status_equal(payment, CardPayment.Status.AUTHORIZED)
        self.assertGreater(payment.updated_at, prev_updated_at)

    def test_process_started_autorized(self):
        self.processor.initialize_payment(self.payment)
        self.processor.start_payment(self.payment)
        self.trust_client.authorize_payment(
            uid=self.payment.user.uid,
            purchase_token=self.payment.purchase_token,
        )
        self.processor.process(self.payment)
        self.assert_payment_status_equal(self.payment, CardPayment.Status.AUTHORIZED)

    def test_started_not_authorized(self):
        self.processor.initialize_payment(self.payment)
        self.processor.start_payment(self.payment)

        self.trust_client.unauthorize_payment(
            uid=self.payment.user.uid,
            purchase_token=self.payment.purchase_token,
            resp_code='restricted_card',
            resp_desc='Card is blocked, reason: RC=36, reason=Restricted card',
        )
        self.processor.update_started_payment(self.payment)
        payment = self.update_payment(self.payment)
        self.assert_payment_status_equal(payment, CardPayment.Status.NOT_AUTHORIZED)
        self.assertEqual(payment.resp_code, 'restricted_card')
        self.assertIn('RC=36', payment.resp_desc)

    def test_process_authorized_before_clear_delay(self):
        self.processor.initialize_payment(self.payment)
        self.processor.start_payment(self.payment)
        self.trust_client.authorize_payment(
            uid=self.payment.user.uid,
            purchase_token=self.payment.purchase_token,
        )
        self.processor.update_started_payment(self.payment)

        self.set_payment_date(self.payment, timezone.now())
        self.processor.process(self.payment)
        self.assert_payment_status_equal(self.payment, CardPayment.Status.AUTHORIZED)

    def test_process_authorized_after_clear_delay(self):
        self.processor.initialize_payment(self.payment)
        self.processor.start_payment(self.payment)
        self.trust_client.authorize_payment(
            uid=self.payment.user.uid,
            purchase_token=self.payment.purchase_token,
        )
        self.processor.update_started_payment(self.payment)

        self.set_payment_date(self.payment, timezone.now() - datetime.timedelta(days=30))
        self.processor.process(self.payment)
        self.assert_payment_status_equal(self.payment, CardPayment.Status.CLEARED)

    def test_resize_payment(self):
        payment = self.make_payment(amount=10)
        self.processor.initialize_payment(payment)
        self.processor.start_payment(payment)
        self.trust_client.authorize_payment(
            uid=payment.user.uid,
            purchase_token=payment.purchase_token,
        )
        self.processor.update_started_payment(payment)

        self.processor.resize_payment(payment=payment, amount=5)
        self.assertEqual(CardPayment.objects.get(id=payment.id).amount, 5)
        self.assertEqual(CardPayment.objects.get(id=payment.id).orig_amount, 10)
