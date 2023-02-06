from django.urls import reverse

from cars.users.factories.user import UserFactory
from .base import AdminAPITestCase


class UserTagsTestCase(AdminAPITestCase):

    def setUp(self):
        self.user = UserFactory.create(tags=[])

    def get_global_tag_list_url(self):
        return reverse('cars-admin:user-tag-list')

    def get_user_tag_list_url(self, user):
        return reverse('cars-admin:user-tags', kwargs={'user_id': user.id})

    def assert_user_tags_equal(self, user, tags):
        user.refresh_from_db()
        user_tags = user.tags if user.tags else []
        self.assert_tags_equal(user_tags, tags)

    def assert_tags_equal(self, tags1, tags2):
        self.assertEqual(sorted(tags1), sorted(tags2))

    def test_tag_list(self):
        url = self.get_global_tag_list_url()

        response1 = self.client.get(url)
        self.assert_response_ok(response1)
        self.assertIn('tags', response1.data)
        self.assertEqual(response1.data['tags'], [])

        UserFactory.create(tags=['test1'])
        response2 = self.client.get(url)
        self.assertEqual(response2.data['tags'], ['test1'])

        UserFactory.create(tags=['test2', 'test3'])
        response3 = self.client.get(url)
        self.assertEqual(set(response3.data['tags']), {'test1', 'test2', 'test3'})

    def test_add_tags(self):
        url = self.get_user_tag_list_url(user=self.user)

        response1 = self.client.post(url, data={'tags': [' TAG1 ']})
        self.assert_tags_equal(response1.data['tags'], ['tag1'])
        self.assert_user_tags_equal(self.user, ['tag1'])

        self.client.post(url, data={'tags': [' TAG1 ', 'tag2']})
        self.assert_user_tags_equal(self.user, ['tag1', 'tag2'])

        global_tags_response = self.client.get(self.get_global_tag_list_url())
        self.assert_tags_equal(global_tags_response.data['tags'], ['tag1', 'tag2'])

    def test_remove_tags(self):
        user = UserFactory.create(tags=['tag1', 'tag2', 'tag3'])

        url = self.get_user_tag_list_url(user=user)

        response1 = self.client.delete(url, data={'tags': [' TAG1 ']})
        self.assert_tags_equal(response1.data['tags'], ['tag2', 'tag3'])
        self.assert_user_tags_equal(user, ['tag2', 'tag3'])

        response2 = self.client.delete(url, data={'tags': ['tag1', 'tag2']})
        self.assert_tags_equal(response2.data['tags'], ['tag3'])
        self.assert_user_tags_equal(user, ['tag3'])

        response3 = self.client.delete(url, data={'tags': ['tag1', 'tag2', 'tag3', 'tag4']})
        self.assert_tags_equal(response3.data['tags'], [])
        self.assert_user_tags_equal(user, [])
