import collections
import datetime
import logging
import pytz
import unittest.mock
import uuid

from django.test import TestCase
from django.utils import timezone

from cars.fines.models.fine import AutocodeFine, AutocodeFinePhoto
from cars.orders.models import OrderItem
from cars.orders.factories.order_item import OrderItemFactory
from cars.orders.factories.order import OrderFactory
from cars.carsharing.factories.car import CarFactory
from cars.carsharing.models import Car, CarsharingRide
from cars.settings import tests as settings
from cars.users.factories.user import UserFactory
from cars.users.models.user import User

from .autocode_stub import AutoCodeFinesClientStub
from ..core.fines_manager import FinesManager
from ..core.fine_photos_manager import FinePhotosManager
from ..core.fine_collector import FineCollector
from ..factories.fine import FineFactory
from ..factories.fine_feed import (
    FineFeedFactory, FineFeedGibddFactory, FineFeedAmppFactory, FineFeedMadiFactory
)


LOGGER = logging.getLogger(__name__)


def _datetime_localize(dt):
    return pytz.timezone('Europe/Moscow').localize(dt)


def _datetime_localize_to_timestamp(dt):
    return int(
        (
            _datetime_localize(dt) -
            timezone.make_aware(
                datetime.datetime(1970, 1, 1),
                pytz.utc,
            )
        ).total_seconds()
    )


class FinesManagerTestCase(TestCase):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)

    def _set_cars_for_autocode_feed(self):
        created_cars_stses = set()
        self.sts_numbers = []
        self.fines_datetimes = self.get_datetimes_from_fines_feed(self.autocode_client.fines_feed)
        for fine in self.autocode_client.fines_feed:
            sts_number = fine['violationDocumentNumber']
            self.sts_numbers.append(sts_number)
            if sts_number in created_cars_stses:
                continue
            car = CarFactory.create(registration_id=int(sts_number))
            car.save()
            created_cars_stses.add(sts_number)

    def _set_billing_mock_charge_is_passed(self, value):
        billing_client_mock = self.fine_collector._billing_client
        billing_client_mock.charge_is_passed.return_value = value

    def _set_billing_mock_charge_exists(self, value):
        billing_client_mock = self.fine_collector._billing_client
        billing_client_mock.charge_exists.return_value = value

    def _set_order_for_sts(self, sts, *, before, after):
        assert before > after
        timedelta = before - after
        t1, t2, t3, t4 = tuple(after + timedelta * i / 5 for i in range(1, 5))
        carsharing_ride = CarsharingRide(
            car=Car.objects.filter(registration_id=sts).first(),
        )
        carsharing_ride.save()

        order = OrderFactory.create(
            created_at=t1,
            completed_at=t4
        )
        order.save()
        order_items = [
            OrderItemFactory.create(
                order=order,
                type=OrderItem.Type.CARSHARING_RIDE.value,
                carsharing_ride=carsharing_ride,
                started_at=t2,
                finished_at=t3
            )
        ]
        for oi in order_items:
            oi.save()
        return order

    @staticmethod
    def get_datetimes_from_fines_feed(fines_feed):
        return [
            timezone.make_aware(
                datetime.datetime.strptime(fine['violationDateWithTime'],
                                           '%d.%m.%Y %H:%M:%S'),
                pytz.timezone('Europe/Moscow'))
            for fine in fines_feed
        ]

    def setUp(self):
        self.autocode_client = AutoCodeFinesClientStub()
        self._set_cars_for_autocode_feed()
        self.saas_client_stub = unittest.mock.MagicMock()
        self.saas_client_stub.get_nearest_session.return_value = None
        self.fine_collector = FineCollector.from_settings()
        self.photo_manager = FinePhotosManager.from_settings()
        self.fines_manager = FinesManager(
            autocode_client=self.autocode_client,
            fine_collector=self.fine_collector,
            photo_manager=self.photo_manager,
            saas_client=self.saas_client_stub,
        )

    def test_fines_collection(self):
        fines = self.fines_manager.collect_new_fines()
        self.assertEqual(len(fines), len(self.autocode_client.fines_feed))

    def test_has_assignment(self):
        self.autocode_client.load_stub_fines_realdata()
        sts_to_fines_count = collections.Counter(
            f['violationDocumentNumber'] for f in self.autocode_client.fines_feed)

        self._set_cars_for_autocode_feed()

        min_datetime = min(self.get_datetimes_from_fines_feed(self.autocode_client.fines_feed))
        sts_to_order = {
            sts: self._set_order_for_sts(
                sts,
                before=min_datetime,
                after=min_datetime - datetime.timedelta(hours=1)
            )
            for sts in sts_to_fines_count
        }

        fines = self.fines_manager.collect_new_fines()
        self.assertEqual(len(fines), len(self.autocode_client.fines_feed))

        self.fines_manager.save_new_fines(fines)

        # Check that the fine reflected on the respective car, order and user
        for sts, count in sts_to_fines_count.items():
            self.assertEqual(
                Car.objects.filter(registration_id=sts).first().autocode_fines.count(),
                count
            )
            self.assertEqual(sts_to_order[sts].user.autocode_fines.count(),
                             count)

    def test_assign_to_latest_user(self):
        order1 = self._set_order_for_sts(
            sts=self.sts_numbers[0],
            before=self.fines_datetimes[0],
            after=self.fines_datetimes[0] - datetime.timedelta(hours=1)
        )
        order2 = self._set_order_for_sts(
            sts=self.sts_numbers[0],
            before=self.fines_datetimes[0] - datetime.timedelta(hours=1),
            after=self.fines_datetimes[0] - datetime.timedelta(hours=2)
        )

        fines = self.fines_manager.collect_new_fines()
        self.assertEqual(len(fines), len(self.autocode_client.fines_feed))

        LOGGER.info('Sought selected order ID: %s', str(order1.id))
        LOGGER.info('Older order ID: %s', str(order2.id))

        for fine in fines:
            LOGGER.info('Fine id: %s', fine.id)
            if fine.order is not None:
                LOGGER.info('Associated order id: %s', fine.order.id)
                LOGGER.info('Associated order id: %s', fine.order.completed_at)

        self.fines_manager.save_new_fines(fines)

        self.assertEqual(order1.user.autocode_fines.count(), 1)
        self.assertEqual(order2.user.autocode_fines.count(), 0)
        self.assertEqual(
            Car.objects.filter(registration_id=self.sts_numbers[0]).first().autocode_fines.count(),
            1
        )

    def test_confirmations_collection(self):
        fines = self.fines_manager.collect_new_fines()
        self.fines_manager.save_new_fines(fines)

        confirmations = self.fines_manager.collect_fine_payment_confirmations()
        self.assertEqual(len(confirmations), len(self.autocode_client.confirmations_feed))

        num_saved = self.fines_manager.save_new_payment_confirmations(confirmations)
        self.assertEqual(num_saved, 1)

        num_saved = self.fines_manager.save_new_payment_confirmations(confirmations)
        self.assertEqual(num_saved, 0)

    def test_autocode_duplicates(self):
        '''Autocode sends some duplicate fines: they differ only in id. Check, that we create only one of them.'''
        id1 = 111
        id2 = 222
        self.autocode_client.load_stub_fines([{'id': id1}])
        autocode_fine = self.autocode_client.fines_feed[0]
        violation_datetime = self.get_datetimes_from_fines_feed(self.autocode_client.fines_feed)[0]
        sts = autocode_fine['violationDocumentNumber']
        duplicate_fine = autocode_fine.copy()
        duplicate_fine['id'] = id2
        self.autocode_client.fines_feed.append(duplicate_fine)
        self._set_cars_for_autocode_feed()
        order = self._set_order_for_sts(
            sts,
            before=violation_datetime,
            after=violation_datetime - datetime.timedelta(hours=1)
        )
        fines = self.fines_manager.collect_new_fines()
        self.assertEqual(len(fines), 2)

        self.fines_manager.save_new_fines(fines)
        self.assertEqual(order.user.autocode_fines.count(), 1)
        self.assertEqual(
            Car.objects.filter(registration_id=sts).first().autocode_fines.count(),
            1
        )
        self.assertEqual(order.user.autocode_fines.count(), 1)

    def test_collect_fines_charge_full(self):
        '''We charge money and send all the notifications.'''
        self.fine_collector._charge = True
        self.fine_collector._send_email = True
        self.fine_collector._send_sms = True
        self.fine_collector._send_push = True

        fine = FineFactory(
            needs_charge=True,
            charged_at=None,
            charge_email_sent_at=None,
            charge_sms_sent_at=None,
            charge_push_sent_at=None,
        )

        before = timezone.now()
        self._set_billing_mock_charge_exists(False)  # payment transaction is not accepted yet
        self._set_billing_mock_charge_is_passed(False)  # payment transaction is not accepted yet
        self.fines_manager.process_existing_fines()
        after = timezone.now()

        billing_mock = self.fine_collector._billing_client
        self.assertEqual(billing_mock.charge.call_count, 1)
        call_kwargs = billing_mock.charge.call_args[1]
        call_kwargs.pop('comment')
        self.assertEqual(call_kwargs, {
            'session_id': fine.id,
            'user_id': fine.user_id,
            'sum': int(fine.sum_to_pay * 100),
            'charge_type': 'ticket_gibdd',
        })

        fine.refresh_from_db()
        self.assertTrue(fine.charged_at is not None
                        and before <= fine.charged_at <= after)
        self.assertIsNone(fine.charge_passed_at)
        self.assertIsNotNone(fine.charge_email_sent_at)
        self.assertIsNotNone(fine.charge_sms_sent_at)
        self.assertIsNotNone(fine.charge_push_sent_at)

        before_2 = timezone.now()
        self._set_billing_mock_charge_exists(False)
        self._set_billing_mock_charge_is_passed(True)  # payment transaction is accepted now
        self.fines_manager.process_existing_fines()
        after_2 = timezone.now()

        fine.refresh_from_db()
        self.assertTrue(fine.charge_passed_at is not None
                        and before_2 <= fine.charge_passed_at <= after_2)
        self.assertTrue(fine.charge_push_sent_at is not None
                        and before <= fine.charge_push_sent_at <= after)
        self.assertTrue(fine.charge_email_sent_at is not None
                        and before <= fine.charge_email_sent_at <= after)
        self.assertTrue(fine.charge_sms_sent_at is not None
                        and before <= fine.charge_sms_sent_at <= after)

        sender_mock = self.fine_collector._notifier._email._sender
        self.assertEqual(sender_mock.send.call_count, 1)
        call_kwargs = sender_mock.send.call_args[1]
        self.assertEqual(call_kwargs['to_email'], fine.user.email)

        sms_mock = self.fine_collector._notifier._sms._yasms
        self.assertEqual(sms_mock.sendsms.call_count, 1)
        call_kwargs = sms_mock.sendsms.call_args[1]
        self.assertEqual(call_kwargs['phone'], fine.user.phone)

        xiva_mock = self.fine_collector._notifier._push._xiva
        self.assertEqual(xiva_mock.send.call_count, 1)
        call_kwargs = xiva_mock.send.call_args[1]
        self.assertEqual(call_kwargs['user'], str(fine.user.uid))

    def test_collect_fines_disabled(self):
        '''Check that when money charge is disabled, no money are charged, no notifications sent.'''
        self.fine_collector._charge = False
        self.fine_collector._send_email = True
        self.fine_collector._send_sms = True
        self.fine_collector._send_push = True

        fine = FineFactory(
            charged_at=None,
            charge_email_sent_at=None,
            charge_sms_sent_at=None
        )

        self.fines_manager.process_existing_fines()

        billing_mock = self.fine_collector._billing_client
        self.assertEqual(billing_mock.charge.call_count, 0)
        self.assertEqual(billing_mock.charge_is_passed.call_count, 0)

        fine.refresh_from_db()
        self.assertEqual(fine.charged_at, None)
        self.assertEqual(fine.charge_email_sent_at, None)
        self.assertEqual(fine.charge_sms_sent_at, None)

        sender_mock = self.fine_collector._notifier._email._sender
        self.assertEqual(sender_mock.send.call_count, 0)

        sms_mock = self.fine_collector._notifier._sms._yasms
        self.assertEqual(sms_mock.sendsms.call_count, 0)

    def test_collect_fines_incomplete(self):
        '''Sms or email already sent - send the other one'''
        self.fine_collector._charge = True
        self.fine_collector._send_email = True
        self.fine_collector._send_sms = True

        t1 = timezone.now() - datetime.timedelta(hours=2)
        t2 = timezone.now() - datetime.timedelta(hours=1)

        fine_email_sent, fine_sms_sent = (
            FineFactory(
                needs_charge=True,
                charged_at=t1,
                charge_passed_at=t2,
                charge_email_sent_at=t2,
                charge_sms_sent_at=None
            ),
            FineFactory(
                needs_charge=True,
                charged_at=t1,
                charge_passed_at=t2,
                charge_email_sent_at=None,
                charge_sms_sent_at=t2
            ),
        )

        before = timezone.now()
        self.fines_manager.process_existing_fines()
        after = timezone.now()

        fine_email_sent.refresh_from_db()

        self.assertEqual(fine_email_sent.charged_at, t1)
        self.assertEqual(fine_email_sent.charge_email_sent_at, t2)
        self.assertTrue(fine_email_sent.charge_sms_sent_at is not None
                        and before <= fine_email_sent.charge_sms_sent_at <= after)

        fine_sms_sent.refresh_from_db()
        self.assertEqual(fine_sms_sent.charged_at, t1)
        self.assertEqual(fine_sms_sent.charge_sms_sent_at, t2)
        self.assertTrue(fine_sms_sent.charge_sms_sent_at is not None
                        and before <= fine_sms_sent.charge_email_sent_at <= after)

        sender_mock = self.fine_collector._notifier._email._sender
        sms_mock = self.fine_collector._notifier._sms._yasms

        self.assertEqual(sms_mock.sendsms.call_count, 1)
        self.assertEqual(sender_mock.send.call_count, 1)
        sender_call_kwargs = sender_mock.send.call_args[1]
        sms_call_kwargs = sms_mock.sendsms.call_args[1]

        self.assertEqual(sender_call_kwargs['to_email'], fine_sms_sent.user.email)
        self.assertEqual(sms_call_kwargs['phone'], fine_email_sent.user.phone)

    def test_collect_fines_charge_not_passed(self):
        '''Check that we DO send notifications for uncharged fines'''
        self.fine_collector._charge = True
        self.fine_collector._send_email = True
        self.fine_collector._send_sms = True
        self.fine_collector._send_push = True

        t = timezone.now() - datetime.timedelta(hours=2)

        fine = (
            FineFactory(
                needs_charge=True,
                charged_at=t,
                charge_passed_at=None,
                charge_email_sent_at=None,
                charge_sms_sent_at=None,
                charge_push_sent_at=None,
            )
        )

        self._set_billing_mock_charge_exists(False)
        self._set_billing_mock_charge_is_passed(False)
        self.fines_manager.process_existing_fines()

        fine.refresh_from_db()
        self.assertIsNone(fine.charge_passed_at)
        self.assertIsNotNone(fine.charge_email_sent_at)
        self.assertIsNotNone(fine.charge_sms_sent_at)
        self.assertIsNotNone(fine.charge_push_sent_at)

    def test_collect_fines_needs_charge_false(self):
        '''Check that needs_charge fines are not charged'''
        self.fine_collector._charge = True
        self.fine_collector._send_email = True
        self.fine_collector._send_sms = True

        fine = (
            FineFactory(
                needs_charge=False,
                charged_at=None,
                charge_passed_at=None,
                charge_email_sent_at=None,
                charge_sms_sent_at=None,
                charge_push_sent_at=None,
            )
        )

        self.fines_manager.process_existing_fines()

        billing_client_mock = self.fine_collector._billing_client
        self.assertEqual(billing_client_mock.charge.call_count, 0)
        self.assertEqual(billing_client_mock.charge_is_passed.call_count, 0)

        fine.refresh_from_db()
        self.assertIsNone(fine.charged_at)
        self.assertIsNone(fine.charge_passed_at)
        self.assertIsNone(fine.charge_email_sent_at)
        self.assertIsNone(fine.charge_sms_sent_at)
        self.assertIsNone(fine.charge_push_sent_at)

    def test_collect_fines_dont_send_sms(self):
        '''Sms send disabled'''
        self.fine_collector._charge = True
        self.fine_collector._send_email = True
        self.fine_collector._send_sms = False
        self.fine_collector._send_push = True

        fine = FineFactory(
            needs_charge=True,
            charged_at=None,
            charge_email_sent_at=None,
            charge_sms_sent_at=None
        )

        self._set_billing_mock_charge_exists(False)
        self._set_billing_mock_charge_is_passed(True)
        before = timezone.now()
        self.fines_manager.process_existing_fines()
        after = timezone.now()

        fine.refresh_from_db()
        self.assertTrue(fine.charge_passed_at is not None
                        and before <= fine.charge_passed_at <= after)
        self.assertEqual(fine.charge_sms_sent_at, None)
        self.assertTrue(fine.charge_email_sent_at is not None
                        and before <= fine.charge_email_sent_at <= after)
        self.assertTrue(fine.charge_push_sent_at is not None
                        and before <= fine.charge_push_sent_at <= after)
        sender_mock = self.fine_collector._notifier._email._sender
        sms_mock = self.fine_collector._notifier._sms._yasms

        self.assertEqual(sms_mock.sendsms.call_count, 0)
        self.assertEqual(sender_mock.send.call_count, 1)

    def test_collect_fines_sum_to_pay_limit(self):
        self.fine_collector._charge = True
        fines = [
            FineFactory(
                charged_at=None,
                needs_charge=True,
                sum_to_pay=settings.FINES['charge_limit'] + diff,
            )
            for diff in [-1, 0, 1]
        ]

        self.fines_manager.process_existing_fines()

        for fine in fines:
            fine.refresh_from_db()
        self.assertIsNotNone(fines[0].charged_at)
        self.assertIsNotNone(fines[1].charged_at)
        self.assertIsNone(fines[2].charged_at)

    def test_collect_fines_yandexoid_only(self):
        '''Charge only yandexoids'''
        self.fine_collector._yandexoid_only = True

        fine_yandexoid = FineFactory(
            charged_at=None,
            needs_charge=True,
            user__is_yandexoid=True,
        )
        fine_common_user = FineFactory(
            charged_at=None,
            needs_charge=True,
            user__is_yandexoid=False,
        )

        self.fines_manager.process_existing_fines()

        for fine in (fine_yandexoid, fine_common_user):
            fine.refresh_from_db()
        self.assertIsNotNone(fine_yandexoid.charged_at)
        self.assertIsNone(fine_common_user.charged_at)

    def test_collect_fines_drive_only(self):
        '''Charge only drives'''
        self.fine_collector._drive_only = True

        fine_drive = FineFactory(
            needs_charge=True,
            charged_at=None,
            user__tags=['some_tag', 'drive_staff', 'another_tag'],
        )
        fine_common_user = FineFactory(
            needs_charge=True,
            charged_at=None,
            user__tags=['some_tag', 'drive_stuff', 'another_tag'],
        )

        self.fines_manager.process_existing_fines()

        for fine in (fine_drive, fine_common_user):
            fine.refresh_from_db()
        self.assertIsNotNone(fine_drive.charged_at)
        self.assertIsNone(fine_common_user.charged_at)

    def test_collect_fines_emails_only(self):
        '''Charge only yandexoids'''
        email = 'foo@bar.ya'
        self.fine_collector._emails_only = [email]

        fine_email_matches = FineFactory(
            needs_charge=True,
            charged_at=None,
            user__email=email,
        )
        fine_common_user = FineFactory(
            needs_charge=True,
            charged_at=None,
            user__email='other@email.ya',
        )

        self.fines_manager.process_existing_fines()

        for fine in (fine_email_matches, fine_common_user):
            fine.refresh_from_db()
        self.assertIsNotNone(fine_email_matches.charged_at)
        self.assertIsNone(fine_common_user.charged_at)

    def test_collect_fines_violation_time(self):
        '''Check that violation time in email has correct timezone'''
        self.fine_collector._send_email = True

        datetime_utc = datetime.datetime(2018, 4, 4, 11, 12, 13, 0, pytz.UTC)
        datetime_string_format = '4 апреля в 14:12'
        fine = FineFactory(
            needs_charge=True,
            violation_time=datetime_utc,
            charged_at=None,
            charge_email_sent_at=None
        )

        self.fines_manager.process_existing_fines()

        sender_mock = self.fine_collector._notifier._email._sender
        sender_call_kwargs = sender_mock.send.call_args[1]
        self.assertEqual(sender_call_kwargs['args']['strike_date'],
                         datetime_string_format)

    def test_fine_photos(self):
        fine = FineFactory.create(
            needs_charge=False,
            has_photo=True
        )
        self.autocode_client.load_photo_feed()

        self.fines_manager.attach_missing_photos()

        fine.refresh_from_db()

        got_photos = fine.photos.all()

        self.assertEqual(len(got_photos), 2)
        for p in got_photos:
            self.assertTrue(isinstance(p, AutocodeFinePhoto))
            self.assertTrue(p.url.startswith('http'))
        self.assertEqual(
            set(
                self.photo_manager._mds_client.get_object_content(
                    self.photo_manager._mds_bucket_name,
                    self.photo_manager.get_photo_key(fine.id, p.id))
                for p in got_photos
            ),
            {b'<photo0_content>', b'<photo1_content>'}
        )

    def test_fine_photo_exists(self):
        fine = FineFactory.create(
            needs_charge=True,
            has_photo=True
        )
        self.autocode_client.load_photo_feed()

        self.fines_manager.attach_missing_photos()
        fine.refresh_from_db()
        self.assertEqual(len(fine.photos.all()), 2)

        self.fines_manager.attach_missing_photos()
        fine.refresh_from_db()
        self.assertEqual(len(fine.photos.all()), 2)  # photo attaching is idempotent

    def test_fine_no_photo_needed(self):
        fine = FineFactory.create(
            has_photo=False
        )
        self.autocode_client.load_photo_feed()

        self.fines_manager.attach_missing_photos()
        fine.refresh_from_db()
        self.assertEqual(len(fine.photos.all()), 0)

    def test_fine_no_photo_yet(self):
        fine = FineFactory.create(
            has_photo=True
        )
        self.autocode_client.load_no_photo_feed()

        self.fines_manager.attach_missing_photos()
        fine.refresh_from_db()
        self.assertEqual(len(fine.photos.all()), 0)

    def test_fine_coordinates(self):
        violation_datetime = pytz.timezone('Europe/Moscow').localize(
            datetime.datetime(2018, 4, 4)
        )
        epoch_start = pytz.utc.localize(
            datetime.datetime(1970, 1, 1)
        )
        violation_timestamp = int((violation_datetime - epoch_start).total_seconds())
        fine_feed = FineFeedGibddFactory.build(
            violationDateWithTime=violation_datetime.strftime('%d.%m.%Y %H:%M:%S'),
        )
        self.autocode_client.fines_feed = [fine_feed]
        self._set_cars_for_autocode_feed()
        self._set_order_for_sts(
            sts=fine_feed['violationDocumentNumber'],
            before=violation_datetime,
            after=violation_datetime - datetime.timedelta(hours=1)
        )
        violation_latitude = 55.73335648
        violation_longitude = 37.58893204
        other_latitudes = (violation_latitude + d * 0.0001 for d in range(-5, 5) if d)
        other_longitudes = (violation_longitude + d * 0.0001 for d in range(-5, 5) if d)

        self.saas_client_stub.get_tracks.return_value = [
            [
                (violation_timestamp - 100, next(other_latitudes), next(other_longitudes)),
                (violation_timestamp - 98, next(other_latitudes), next(other_longitudes)),
            ],
            [
                (violation_timestamp - 2, next(other_latitudes), next(other_longitudes)),
                (violation_timestamp - 1, violation_latitude, violation_longitude),
                (violation_timestamp + 1, next(other_latitudes), next(other_longitudes)),
            ],
            [
                (violation_timestamp + 1, next(other_latitudes), next(other_longitudes)),
                (violation_timestamp + 2, next(other_latitudes), next(other_longitudes)),
            ],
        ]

        fines = self.fines_manager.collect_new_fines()
        self.fines_manager.save_new_fines(fines)
        fine = AutocodeFine.objects.filter(
            autocode_id=fine_feed['id']
        ).first()
        self.assertEqual(fine.violation_latitude, violation_latitude)
        self.assertEqual(fine.violation_longitude, violation_longitude)

    def test_fine_coordinates_types(self):
        violation_datetime = pytz.timezone('Europe/Moscow').localize(
            datetime.datetime(2018, 4, 4)
        )
        epoch_start = pytz.utc.localize(
            datetime.datetime(1970, 1, 1)
        )
        violation_timestamp = int((violation_datetime - epoch_start).total_seconds())
        fine_feed = FineFeedGibddFactory.build(
            violationDateWithTime=violation_datetime.strftime('%d.%m.%Y %H:%M:%S'),
        )
        self.autocode_client.fines_feed = [fine_feed]
        self._set_cars_for_autocode_feed()
        self._set_order_for_sts(
            sts=fine_feed['violationDocumentNumber'],
            before=violation_datetime,
            after=violation_datetime - datetime.timedelta(hours=1)
        )
        violation_latitude = 55.73335648
        violation_longitude = 37.58893204
        other_latitudes = (violation_latitude + d * 0.0001 for d in range(-5, 5) if d)
        other_longitudes = (violation_longitude + d * 0.0001 for d in range(-5, 5) if d)

        self.saas_client_stub.get_tracks.return_value = [
            zip(map(int, [violation_timestamp - 2, violation_timestamp, violation_timestamp + 1]),
                [next(other_latitudes), violation_latitude, next(other_latitudes)],
                [next(other_longitudes), violation_longitude, next(other_longitudes)])
        ]

        fines = self.fines_manager.collect_new_fines()
        self.fines_manager.save_new_fines(fines)
        fine = AutocodeFine.objects.filter(
            autocode_id=fine_feed['id']
        ).first()
        self.assertEqual(fine.violation_latitude, violation_latitude)
        self.assertEqual(fine.violation_longitude, violation_longitude)

    def test_fine_coordinates_raises(self):
        violation_datetime = pytz.timezone('Europe/Moscow').localize(
            datetime.datetime(2018, 4, 4)
        )
        fine_feed = FineFeedGibddFactory.build(
            violationDateWithTime=violation_datetime.strftime('%d.%m.%Y %H:%M:%S'),
        )
        self.autocode_client.fines_feed = [fine_feed]
        self._set_cars_for_autocode_feed()
        self._set_order_for_sts(
            sts=fine_feed['violationDocumentNumber'],
            before=violation_datetime,
            after=violation_datetime - datetime.timedelta(hours=1)
        )
        self.saas_client_stub.get_tracks.side_effect = Exception(':(')

        fines = self.fines_manager.collect_new_fines()
        self.fines_manager.save_new_fines(fines)
        fine = AutocodeFine.objects.filter(
            autocode_id=fine_feed['id']
        ).first()
        self.assertEqual(fine.violation_latitude, None)
        self.assertEqual(fine.violation_longitude, None)

    def test_is_camera_fixation(self):
        '''Check that we set is_camera_fixation only for Gibdd and Madi fines with correct ruling numbers'''
        fines_we_need = [
            FineFeedGibddFactory.build(),
            FineFeedMadiFactory.build(),
        ]

        fines_we_dont_need = [
            FineFeedAmppFactory.build(),
            FineFeedFactory.build(),
        ]
        for gibdd_prefix in ['188103', '188102', '188104']:
            f = FineFeedGibddFactory.build()
            f['rulingNumber'] = gibdd_prefix + f['rulingNumber'][len(gibdd_prefix):]
            fines_we_dont_need.append(f)
        for madi_prefix in ['035604301051', '035604301021']:
            f = FineFeedGibddFactory.build()
            f['rulingNumber'] = madi_prefix + f['rulingNumber'][len(madi_prefix):]
            fines_we_dont_need.append(f)

        self.autocode_client.fines_feed = fines_we_need + fines_we_dont_need
        self._set_cars_for_autocode_feed()

        fines = self.fines_manager.collect_new_fines()
        self.fines_manager.save_new_fines(fines)

        for feed in fines_we_need:
            self.assertTrue(
                AutocodeFine.objects.filter(
                    autocode_id=feed['id']
                ).first().is_camera_fixation
            )

        for feed in fines_we_dont_need:
            self.assertFalse(
                AutocodeFine.objects.filter(
                    autocode_id=feed['id']
                ).first().is_camera_fixation
            )

    def test_is_after_ride_start_during_order(self):
        sts = self.sts_numbers[0]
        car = Car.objects.filter(registration_id=sts).first()

        to_time = datetime.datetime.now()
        timedelta = datetime.timedelta(hours=1)
        rides = [{}, {}]
        violation_times = [0 for _ in range(6)]
        (
            order_created_at, violation_times[0],
            rides[0]['started_at'], violation_times[1],
            rides[0]['finished_at'], violation_times[2],
            rides[1]['started_at'], violation_times[3],
            rides[1]['finished_at'], violation_times[4],
            order_completed_at, violation_times[5],
        ) = tuple(to_time - timedelta + timedelta * i / 5 for i in range(1, 13))

        order = OrderFactory.create(
            created_at=order_created_at,
            completed_at=order_completed_at,
        )
        order.save()

        rides[0]['ride'], rides[1]['ride'] = tuple(CarsharingRide(car=car) for _ in range(2))
        for r in rides:
            r['ride'].save()

        [
            OrderItemFactory.create(
                order=order,
                type=OrderItem.Type.CARSHARING_RIDE.value,
                carsharing_ride=r['ride'],
                started_at=r['started_at'],
                finished_at=r['finished_at'],
            ).save()
            for r in rides
        ]

        self.autocode_client.fines_feed = [
            FineFeedGibddFactory.build(
                violationDateWithTime=pytz.utc.localize(
                    violation_time
                ).astimezone(
                    pytz.timezone('Europe/Moscow')
                ).strftime('%d.%m.%Y %H:%M:%S'),
                violationDocumentNumber=sts
            )
            for violation_time in violation_times
        ]

        fines = self.fines_manager.collect_new_fines()

        check_list = [f.is_after_ride_start_during_order for f in fines]
        self.assertEqual(check_list, [False, True, True, True, True, False])

    def test_wait_for_photo(self):
        '''If fresh fine needs photo and it is absent yet, wait for photo to appear or for the freshness to expire.'''
        self.fine_collector._charge = True
        fine = FineFactory(
            fine_information_received_at=timezone.now() - datetime.timedelta(seconds=3500),
            needs_charge=True,
            charged_at=None,
            has_photo=True,
        )
        fine.photos.set([])

        self._set_billing_mock_charge_exists(False)
        self._set_billing_mock_charge_is_passed(True)
        self.fines_manager.process_existing_fines()
        fine.refresh_from_db()

        billing_mock = self.fine_collector._billing_client
        self.assertEqual(billing_mock.charge.call_count, 0)
        self.assertIsNone(fine.charged_at)

    def test_dont_wait_for_photo_not_fresh(self):
        '''Fine needs photo, fine doesn't have photo, but it is not fresh: start charging.'''
        self.fine_collector._charge = True

        fine = FineFactory(
            fine_information_received_at=timezone.now() - datetime.timedelta(seconds=3601),
            needs_charge=True,
            charged_at=None,
            has_photo=True,
        )
        fine.photos.set([])

        self._set_billing_mock_charge_exists(False)
        self._set_billing_mock_charge_is_passed(True)
        self.fines_manager.process_existing_fines()
        fine.refresh_from_db()

        billing_mock = self.fine_collector._billing_client
        self.assertEqual(billing_mock.charge.call_count, 1)
        self.assertIsNotNone(fine.charged_at)

    def test_dont_wait_for_photo_nor_photo_expected(self):
        '''Fine doesnt need photo: start charging.'''
        self.fine_collector._charge = True

        fine = FineFactory(
            fine_information_received_at=timezone.now() - datetime.timedelta(seconds=3500),
            needs_charge=True,
            charged_at=None,
            has_photo=False,
        )
        fine.photos.set([])

        self._set_billing_mock_charge_exists(False)
        self._set_billing_mock_charge_is_passed(True)
        self.fines_manager.process_existing_fines()
        fine.refresh_from_db()

        billing_mock = self.fine_collector._billing_client
        self.assertEqual(billing_mock.charge.call_count, 1)
        self.assertIsNotNone(fine.charged_at)

    def test_dont_wait_for_photo_has_photo(self):
        '''Fine has all the photos it needs.'''
        self.fine_collector._charge = True

        fine = FineFactory(
            fine_information_received_at=timezone.now() - datetime.timedelta(seconds=3500),
            needs_charge=True,
            charged_at=None,
            has_photo=True,
        )
        AutocodeFinePhoto(fine=fine, url='<photo_url>').save()

        self._set_billing_mock_charge_exists(False)
        self._set_billing_mock_charge_is_passed(True)
        self.fines_manager.process_existing_fines()
        fine.refresh_from_db()

        billing_mock = self.fine_collector._billing_client
        self.assertEqual(billing_mock.charge.call_count, 1)
        self.assertIsNotNone(fine.charged_at)

    def test_nearest_session(self):
        self.autocode_client.load_stub_fines_realdata()
        fine_feed_element = self.autocode_client.fines_feed[0]

        violation_datetime = datetime.datetime.strptime(
            fine_feed_element['violationDateWithTime'],
            '%d.%m.%Y %H:%M:%S'
        )
        violation_timestamp = _datetime_localize_to_timestamp(
            violation_datetime,
        )
        self._set_cars_for_autocode_feed()
        user = UserFactory.create()
        session_id = uuid.uuid4()
        skipped = self.fines_manager.SKIPPED_LIMIT

        self.saas_client_stub.get_nearest_session.return_value = {
            'session_start': violation_timestamp - 10,
            'skipped': skipped,
            'user_id': user.id,
            'session_id': str(session_id),
        }

        fines = self.fines_manager.collect_new_fines()
        self.fines_manager.save_new_fines(fines)

        fine = AutocodeFine.objects.filter(
            ruling_number=fine_feed_element['rulingNumber'],
        ).first()

        self.assertEquals(fine.session_id, str(session_id))
        self.assertEquals(fine.skipped, skipped)
        self.assertEquals(fine.user, user)
        self.assertEquals(fine.order_id, None)
        self.assertEquals(fine.is_after_ride_start_during_order, False)
        needs_charge = fine.violation_time >= _datetime_localize(datetime.datetime(2018, 9, 7))
        self.assertEquals(fine.needs_charge, needs_charge)
        # self.assertEquals(fine.needs_charge, True)

    def test_skipped_limit_exceeded(self):
        self.autocode_client.load_stub_fines_realdata()
        fine_feed_element = self.autocode_client.fines_feed[0]

        violation_datetime = datetime.datetime.strptime(
            fine_feed_element['violationDateWithTime'],
            '%d.%m.%Y %H:%M:%S'
        )
        violation_timestamp = _datetime_localize_to_timestamp(
            violation_datetime,
        )
        self._set_cars_for_autocode_feed()
        user = UserFactory.create()
        session_id = uuid.uuid4()
        skipped = self.fines_manager.SKIPPED_LIMIT + 1

        self.saas_client_stub.get_nearest_session.return_value = {
            'session_start': violation_timestamp - 10,
            'skipped': skipped,
            'user_id': user.id,
            'session_id': str(session_id),
        }

        fines = self.fines_manager.collect_new_fines()
        self.fines_manager.save_new_fines(fines)

        fine = AutocodeFine.objects.filter(
            ruling_number=fine_feed_element['rulingNumber'],
        ).first()

        self.assertEquals(fine.session_id, str(session_id))
        self.assertEquals(fine.skipped, skipped)
        self.assertEquals(fine.user, user)
        self.assertEquals(fine.order_id, None)
        self.assertEquals(fine.is_after_ride_start_during_order, False)
        self.assertEquals(fine.needs_charge, False)

    def test_choose_order_instead_of_session(self):
        self.autocode_client.load_stub_fines_realdata()
        fine_feed_element = self.autocode_client.fines_feed[0]

        violation_datetime = datetime.datetime.strptime(
            fine_feed_element['violationDateWithTime'],
            '%d.%m.%Y %H:%M:%S'
        )
        localized_violation_datetime = _datetime_localize(violation_datetime)
        violation_timestamp = _datetime_localize_to_timestamp(
            violation_datetime,
        )
        self._set_cars_for_autocode_feed()
        session_user = UserFactory.create()
        session_id = uuid.uuid4()
        skipped = self.fines_manager.SKIPPED_LIMIT

        order = self._set_order_for_sts(
            sts=fine_feed_element['violationDocumentNumber'],
            before=localized_violation_datetime - datetime.timedelta(hours=1),
            after=localized_violation_datetime - datetime.timedelta(hours=2),
        )

        self.saas_client_stub.get_nearest_session.return_value = {
            'session_start': violation_timestamp - 3600 * 3,
            'skipped': skipped,
            'user_id': session_user.id,
            'session_id': str(session_id),
        }

        fines = self.fines_manager.collect_new_fines()
        self.fines_manager.save_new_fines(fines)

        fine = AutocodeFine.objects.filter(
            ruling_number=fine_feed_element['rulingNumber'],
        ).first()

        self.assertEquals(fine.session_id, None)
        self.assertEquals(fine.skipped, None)
        self.assertEquals(fine.user, order.user)
        self.assertEquals(fine.order_id, order.id)
        self.assertEquals(fine.is_after_ride_start_during_order, False)
        self.assertEquals(fine.needs_charge, False)

    def test_choose_session_instead_of_order(self):
        self.autocode_client.load_stub_fines_realdata()
        fine_feed_element = self.autocode_client.fines_feed[0]

        violation_datetime = datetime.datetime.strptime(
            fine_feed_element['violationDateWithTime'],
            '%d.%m.%Y %H:%M:%S'
        )
        localized_violation_datetime = _datetime_localize(violation_datetime)
        violation_timestamp = _datetime_localize_to_timestamp(
            violation_datetime,
        )
        self._set_cars_for_autocode_feed()
        session_user = UserFactory.create()
        session_id = uuid.uuid4()
        skipped = self.fines_manager.SKIPPED_LIMIT

        order = self._set_order_for_sts(
            sts=fine_feed_element['violationDocumentNumber'],
            before=localized_violation_datetime - datetime.timedelta(hours=2),
            after=localized_violation_datetime - datetime.timedelta(hours=3),
        )

        self.saas_client_stub.get_nearest_session.return_value = {
            'session_start': violation_timestamp - 3600 * 1,
            'skipped': skipped,
            'user_id': session_user.id,
            'session_id': str(session_id),
        }

        fines = self.fines_manager.collect_new_fines()
        self.fines_manager.save_new_fines(fines)

        fine = AutocodeFine.objects.filter(
            ruling_number=fine_feed_element['rulingNumber'],
        ).first()

        self.assertEquals(fine.session_id, str(session_id))
        self.assertEquals(fine.skipped, skipped)
        self.assertEquals(fine.user, session_user)
        self.assertEquals(fine.order_id, None)
        self.assertEquals(fine.is_after_ride_start_during_order, False)
        needs_charge = fine.violation_time >= _datetime_localize(datetime.datetime(2018, 9, 7))
        self.assertEquals(fine.needs_charge, needs_charge)
        # self.assertEquals(fine.needs_charge, True)

    def test_choose_session_instead_of_order(self):

        # TODO: remove after fix of old telematics
        self.autocode_client.load_stub_fines_realdata()
        fine_feed_element = self.autocode_client.fines_feed[0]

        violation_datetime = datetime.datetime.strptime(
            fine_feed_element['violationDateWithTime'],
            '%d.%m.%Y %H:%M:%S'
        )
        localized_violation_datetime = _datetime_localize(violation_datetime)
        violation_timestamp = _datetime_localize_to_timestamp(
            violation_datetime,
        )
        self._set_cars_for_autocode_feed()
        session_user = UserFactory.create()
        session_id = uuid.uuid4()
        skipped = 0

        order = self._set_order_for_sts(
            sts=fine_feed_element['violationDocumentNumber'],
            before=localized_violation_datetime - datetime.timedelta(hours=2),
            after=localized_violation_datetime - datetime.timedelta(hours=3),
        )

        self.saas_client_stub.get_nearest_session.return_value = {
            'session_start': violation_timestamp - 3600 * 1,
            'skipped': skipped,
            'user_id': session_user.id,
            'session_id': str(session_id),
        }

        fines = self.fines_manager.collect_new_fines()
        self.fines_manager.save_new_fines(fines)

        fine = AutocodeFine.objects.filter(
            ruling_number=fine_feed_element['rulingNumber'],
        ).first()

        self.assertEquals(fine.session_id, str(session_id))
        self.assertEquals(fine.skipped, skipped)
        self.assertEquals(fine.user, session_user)
        self.assertEquals(fine.order_id, None)
        self.assertEquals(fine.is_after_ride_start_during_order, False)
        self.assertEquals(fine.needs_charge, True)

    def test_choose_session_instead_of_order_skip_limit_exceeded(self):
        self.autocode_client.load_stub_fines_realdata()
        fine_feed_element = self.autocode_client.fines_feed[0]

        violation_datetime = datetime.datetime.strptime(
            fine_feed_element['violationDateWithTime'],
            '%d.%m.%Y %H:%M:%S'
        )
        localized_violation_datetime = _datetime_localize(violation_datetime)
        violation_timestamp = _datetime_localize_to_timestamp(
            violation_datetime,
        )
        self._set_cars_for_autocode_feed()
        session_user = UserFactory.create()
        session_id = uuid.uuid4()
        skipped = self.fines_manager.SKIPPED_LIMIT + 1

        order = self._set_order_for_sts(
            sts=fine_feed_element['violationDocumentNumber'],
            before=localized_violation_datetime - datetime.timedelta(hours=2),
            after=localized_violation_datetime - datetime.timedelta(hours=3),
        )

        self.saas_client_stub.get_nearest_session.return_value = {
            'session_start': violation_timestamp - 3600 * 1,
            'skipped': skipped,
            'user_id': session_user.id,
            'session_id': str(session_id),
        }

        fines = self.fines_manager.collect_new_fines()
        self.fines_manager.save_new_fines(fines)

        fine = AutocodeFine.objects.filter(
            ruling_number=fine_feed_element['rulingNumber'],
        ).first()

        self.assertEquals(fine.session_id, str(session_id))
        self.assertEquals(fine.skipped, skipped)
        self.assertEquals(fine.user, session_user)
        self.assertEquals(fine.order_id, None)
        self.assertEquals(fine.is_after_ride_start_during_order, False)
        self.assertEquals(fine.needs_charge, False)
