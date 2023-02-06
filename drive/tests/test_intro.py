from django.contrib.auth.models import Group, Permission
from django.urls import reverse

from cars.admin.models.permissions import AdminPermissions
from cars.admin.permissions import AdminPermissionCode
from .base import AdminAPITestCase


class IntroTestCase(AdminAPITestCase):

    @property
    def url(self):
        return reverse('cars-admin:intro')

    def setUp(self):
        super().setUp()
        permission = Permission.objects.get(
            content_type__model=AdminPermissions._meta.model_name,
            codename=AdminPermissionCode.ACCESS.value,
        )

        group = Group.objects.create(name='test_ring0')
        group.permissions.add(permission)

        self.user.groups.add(group)

    def test_ok(self):
        response = self.client.get(self.url)
        self.assert_response_ok(response)

        self.assertIn('constants', response.data)
        self.assertIn('photo_types', response.data['constants'])
        self.assertIn('user_statuses', response.data['constants'])

        self.assertIn('user', response.data)
        self.assertIn('groups', response.data['user'])
        self.assertIn({'name': 'test_ring0'}, response.data['user']['groups'])
