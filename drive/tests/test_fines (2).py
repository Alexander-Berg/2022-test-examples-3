from datetime import timedelta
from uuid import uuid4

from unittest.mock import MagicMock
from django.urls import reverse
from django.utils import timezone

from cars.carsharing.factories import CarFactory
from cars.carsharing.factories.reservation import CarsharingReservationFactory
from cars.fines.core.fines_manager import FinesManager
from cars.fines.factories.fine import FineFactory
from cars.fines.models.fine import AutocodeFinePhoto
from cars.fines.tests.autocode_stub import AutoCodeFinesClientStub
from cars.orders.factories.order import OrderFactory
from cars.orders.factories.order_item import OrderItemFactory
from cars.users.factories.user import UserFactory

from .base import DriveAPITestCase


class FineListTestCase(DriveAPITestCase):

    @property
    def url(self):
        return reverse('drive:user-fines')

    def setUp(self):
        self.car = CarFactory.create()
        self.user = UserFactory.create(uid=1)
        self.another_user = UserFactory.create(uid=2)

    def test_no_fines(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), [])

    def test_user_choice(self):
        charged_fines = [
            FineFactory(
                user=self.user,
                car=self.car,
                charged_at=timezone.now(),
                violation_time=timezone.now() - timedelta(days=i),
            )
            for i in range(2)
        ]
        not_charged_fine = FineFactory(
            needs_charge=False,
            user=self.user,
            car=self.car,
            charged_at=timezone.now(),
            charge_passed_at=None
        )
        another_user_fine = FineFactory(
            needs_charge=True,
            user=self.another_user,
            car=self.car,
            charged_at=timezone.now(),
            charge_passed_at=timezone.now()
        )

        AutocodeFinePhoto(
            url="http://photo_url",
            fine=charged_fines[0]
        ).save()

        response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)

        data = response.json()
        self.assertTrue(isinstance(data, list))
        self.assertEqual(len(data), 2)

        self.assertEqual([f['id'] for f in data],
                         [str(f.id) for f in charged_fines])

        self.assertEqual(data[0]['photos'][0]['url'], "http://photo_url")
        self.assertEqual(data[1]['photos'], [])

    def test_article_koap(self):
        translatable_article = '12.09.2 - Превышение скорости движения ТС от 20 до 40 км/ч'
        human_readable = 'Превышение скорости: немножко'
        not_translatable_article = '11.11.11 - Превышение какое-нибудь необычное'
        charged_fines = [
            FineFactory(
                user=self.user,
                car=self.car,
                needs_charge=True,
                article_koap=article_koap
            )
            for article_koap in (translatable_article, not_translatable_article)
        ]

        response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)

        data = response.json()
        pairs = set((f['article_koap'], f['article_koap_human_readable'])
                    for f in data)
        self.assertEqual(
            pairs,
            set([
                (translatable_article, human_readable),
                (not_translatable_article, not_translatable_article),
            ])
        )

    def test_session(self):
        session_id = uuid4()
        FineFactory(
            user=self.user,
            car=self.car,
            needs_charge=True,
            session_id=session_id,
            order_id=None,
        )
        response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        data = response.json()

        self.assertEqual(
            data[0]['order'],
            {
                'id': str(session_id),
                'created_at': None,
                'completed_at': None,
            }
        )

    def test_order(self):
        order = OrderFactory.create()
        FineFactory(
            user=self.user,
            car=self.car,
            needs_charge=True,
            session_id=None,
            order=order,
        )
        response = self.client.get(self.url)

        self.assertEqual(response.status_code, 200)
        data = response.json()

        self.assertEqual(data[0]['order']['id'], str(order.id))
