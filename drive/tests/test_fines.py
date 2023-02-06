from unittest.mock import MagicMock
from django.urls import reverse

from cars.carsharing.models import Car
from cars.carsharing.factories import CarFactory
from cars.fines.core.fines_manager import FinesManager
from cars.fines.core.fine_photos_manager import FinePhotosManager
from cars.fines.factories.fine import FineFactory
from cars.fines.models.fine import AutocodeFinePhoto
from cars.fines.tests.autocode_stub import AutoCodeFinesClientStub
from .base import AdminAPITestCase


class FineListTestCase(AdminAPITestCase):

    @property
    def url(self):
        return reverse('cars-admin:fine-list')

    def test_no_fines(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.json()['results']), 0)

    def test_has_chargable_fine(self):
        FineFactory.create(
            user_id=self.user.id,
            needs_charge=True,
        )
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            len(response.json()['results']),
            1,
        )

    def test_has_not_chargable_fine(self):
        FineFactory.create(
            user_id=self.user.id,
            needs_charge=False,
        )
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            len(response.json()['results']),
            1,
        )

    def test_fine_photo(self):
        fine1, fine2 = FineFactory(), FineFactory()
        url1, url2, url3 = tuple(['http://photo_{}.url'.format(i) for i in range(3)])
        AutocodeFinePhoto(
            url=url1,
            fine=fine1
        ).save()
        AutocodeFinePhoto(
            url=url2,
            fine=fine1
        ).save()
        AutocodeFinePhoto(
            url=url3,
            fine=fine2
        ).save()

        response = self.client.get(self.url)
        results = response.json()['results']
        self.assertEqual(len(results), 2)
        photo_sets = [set(p['url'] for p in result['photos']) for result in results]
        self.assertIn({url3}, photo_sets)
        self.assertIn({url1, url2}, photo_sets)
