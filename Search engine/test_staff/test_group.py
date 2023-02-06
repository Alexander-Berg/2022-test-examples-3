# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.core.exceptions import NotFound
from search.martylib.test_utils import TestCase
from search.martylib.staff import StaffClientMock


class TestGroup(TestCase):

    @classmethod
    def setUpClass(cls):
        cls.mock_client = StaffClientMock()

    def test_invalid_id(self):
        self.mock_client.data['v3/groups?service.id=1234'] = {'result': []}
        with self.assertRaisesRegexp(NotFound, 'group with group_id: 1234 does not exist'):
            self.mock_client.get_staff_group_id(abc_service_id=1234)

    def test_valid_id(self):
        self.mock_client.data['v3/groups?service.id=1481'] = {'result': [{'id': 63003}]}
        group_id = self.mock_client.get_staff_group_id(abc_service_id=1481)
        self.assertEqual(group_id, 63003)
