from unittest.mock import MagicMock
from django.urls import reverse

from cars.admin.models.push_preset import PushPreset
from .base import AdminAPITestCase


class PushPresetTestCase(AdminAPITestCase):

    @property
    def list_url(self):
        return reverse('cars-admin:push-preset-list')

    @property
    def create_url(self):
        return reverse('cars-admin:push-preset-create')

    def get_modify_url(self, preset_id):
        return reverse(
            'cars-admin:push-preset-modify',
            kwargs={'preset_id': preset_id}
        )

    def test_list(self):
        m1, m2 = 'hi, man', 'hi, girl'
        p1 = PushPreset.objects.create(message=m1)
        p2 = PushPreset.objects.create(message=m2)
        response = self.client.get(self.list_url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.json(), [
                {
                    'id': str(p1.id),
                    'message': m1
                },
                {
                    'id': str(p2.id),
                    'message': m2
                },
            ]
        )

    def test_create_delete_update(self):
        m1, m2 = 'hi!', 'bye!'
        m3 = 'wait...'

        # create 2 presets
        self.client.post(
            self.create_url,
            data={
                'message': m1,
            }
        )
        create_response = self.client.post(
            self.create_url,
            data={
                'message': m2,
            }
        )

        # check create
        self.assertEqual(create_response.status_code, 200)
        self.assertIn('id', create_response.json())
        presets = list(PushPreset.objects.all())
        self.assertEqual(len(presets), 2)
        self.assertEqual(
            set(
                p.message for p in presets
            ),
            set([m1, m2])
        )

        # modify second preset
        preset_id = create_response.json()['id']
        modify_response = self.client.put(
            self.get_modify_url(preset_id=preset_id),
            data={
                'message': m3,
            }
        )
        self.assertEqual(modify_response.status_code, 200)
        presets = list(PushPreset.objects.all())
        self.assertEqual(len(presets), 2)
        self.assertEqual(
            set(
                p.message for p in presets
            ),
            set([m1, m3])
        )
        self.assertEqual(
            PushPreset.objects.filter(id=preset_id).count(),
            1
        )

        # delete second preset
        delete_response = self.client.delete(
            self.get_modify_url(preset_id=preset_id),
        )
        self.assertEqual(delete_response.status_code, 200)
        presets = list(PushPreset.objects.all())
        self.assertEqual(len(presets), 1)
        self.assertEqual(
            set(
                p.message for p in presets
            ),
            set([m1])
        )
