import datetime
import logging

from rest_framework.test import APITransactionTestCase
from django.utils import timezone
from factory import SubFactory
import pytz

from cars.carsharing.factories.car import CarFactory
from cars.carsharing.factories.ride import CarsharingRideFactory
from cars.carsharing.factories.reservation import CarsharingReservationFactory
from cars.core.datasync import StubDataSyncClient
from cars.users.core.datasync import DataSyncDocumentsClient
from ..core.renaissance import RenaissanceClient
from ..factories.order_item import OrderItemFactory
from ..models import OrderItem, OrderItemTariff
from ..models.order_item_tariff import FixOrderItemTariffParams


LOGGER = logging.getLogger(__name__)


class InsuranceReportTestCase(APITransactionTestCase):

    def setUp(self):
        self.car = CarFactory.create()

    def test_from_settings_constructor(self):
        try:
            RenaissanceClient.from_settings()
        except Exception:
            self.fail('exception while constructing client from settings')

    def test_normal(self):
        matching_completed_item = self._get_valid_ride_for_processing()
        matching_completed_item.save()

        different_type_item = OrderItemFactory.create(
            tariff=OrderItemTariff.objects.create(
                type=OrderItemTariff.Type.FIX.value,
                fix_params=FixOrderItemTariffParams.objects.create(
                    cost=123.45,
                ),
            ),
            type=OrderItem.Type.CARSHARING_RESERVATION.value,
            carsharing_reservation=SubFactory(CarsharingReservationFactory),
            started_at=timezone.now() - datetime.timedelta(minutes=10),
            finished_at=timezone.now(),
        )
        different_type_item.save()

        ds_client = DataSyncDocumentsClient(StubDataSyncClient())
        session = FakeRequestsSession()

        ds_client.update_passport(
            matching_completed_item.order.user.uid,
            {
                'first_name': 'ИМЯ',
                'last_name': 'ФАМИЛИЯ',
                'middle_name': 'ОТЧЕСТВО',
                'birth_date': '2000-01-01T00:00:00.000Z',
            }
        )

        renaissance_client = RenaissanceClient(
            endpoint_url=None,
            datasync_client=ds_client,
            requests_session=session,
        )
        num_reported = renaissance_client.report_new_rides()
        self.assertEqual(num_reported, 1, 'mismatch in number of reported rides')
        doc = session.documents[0]
        self.assertEqual(doc['Key'], str(matching_completed_item.id).upper())
        self.assertEqual(doc['DriverFirstname'], 'ИМЯ')
        self.assertEqual(doc['DriverSurname'], 'ФАМИЛИЯ')
        self.assertEqual(doc['DriverPatronymic'], 'ОТЧЕСТВО')
        self.assertEqual(
            doc['DateOfBirth'],
            datetime.datetime(year=2000, day=1, month=1, tzinfo=pytz.UTC).isoformat()
        )

        LOGGER.info(doc)

        self.assertEqual(
            set(doc.keys()),
            {
                'Key',
                'AgreementPartnerNum',
                'AgreementNum',
                'DriverSurname',
                'DriverFirstname',
                'DriverPatronymic',
                'DateOfBirth',
                'StartTime',
                'EndTime',
                'Cost',
            }
        )

    def test_empty_datasync(self):
        matching_completed_item = self._get_valid_ride_for_processing()
        matching_completed_item.save()

        ds_client = DataSyncDocumentsClient(StubDataSyncClient())
        session = FakeRequestsSession()

        renaissance_client = RenaissanceClient(
            endpoint_url=None,
            datasync_client=ds_client,
            requests_session=session,
        )
        num_reported = renaissance_client.report_new_rides()
        self.assertEqual(num_reported, 1, 'mismatch in number of reported rides')
        doc = session.documents[0]
        self.assertEqual(doc['Key'], str(matching_completed_item.id).upper())

        self.assertEqual(
            set(doc.keys()),
            {
                'Key',
                'AgreementPartnerNum',
                'AgreementNum',
                'StartTime',
                'EndTime',
                'Cost',
            }
        )

        self.assertEqual(
            doc['AgreementPartnerNum'],
            self.car.insurance.agreement_partner_number
        )

        self.assertEqual(
            doc['AgreementNum'],
            self.car.insurance.agreement_number,
        )

    def test_only_active_ride(self):
        incomplete_item = OrderItemFactory.create(
            tariff=OrderItemTariff.objects.create(
                type=OrderItemTariff.Type.FIX.value,
                fix_params=FixOrderItemTariffParams.objects.create(
                    cost=123.45,
                ),
            ),
            type=OrderItem.Type.CARSHARING_RIDE.value,
            carsharing_ride=CarsharingRideFactory(car=self.car),
            started_at=timezone.now() - datetime.timedelta(minutes=10),
            finished_at=None,
        )
        incomplete_item.save()

        ds_client = DataSyncDocumentsClient(StubDataSyncClient())
        session = FakeRequestsSession()

        renaissance_client = RenaissanceClient(
            endpoint_url=None,
            datasync_client=ds_client,
            requests_session=session,
        )

        num_reported = renaissance_client.report_new_rides()

        self.assertEqual(num_reported, 0)

    def _get_valid_ride_for_processing(self):
        return OrderItemFactory.create(
            tariff=OrderItemTariff.objects.create(
                type=OrderItemTariff.Type.FIX.value,
                fix_params=FixOrderItemTariffParams.objects.create(
                    cost=123.45,
                ),
            ),
            type=OrderItem.Type.CARSHARING_RIDE.value,
            carsharing_ride=CarsharingRideFactory(car=self.car),
            started_at=timezone.now() - datetime.timedelta(minutes=10),
            finished_at=timezone.now(),
        )


class FakeRequestsSession:

    class FakeResponse:

        def json(self):
            return {
                'Result': True,
                'Errors': [],
            }

        def raise_for_status(self):
            pass

    def __init__(self):
        self.documents = []

    def post(self, url, json, **kwargs):  # pylint: disable=unused-argument
        LOGGER.info('POST to URL=%s, JSON=%s', url, str(json))
        self.documents.append(json)
        return self.FakeResponse()
