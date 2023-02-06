from django.contrib.auth.models import User
from django.test import TestCase, Client


class TestBaseHtml(TestCase):
    @classmethod
    def setUpClass(cls):
        super().setUpClass()
        cls.client = Client()

        cls.test_user = User(username='test')
        cls.test_user.set_password('test')
        cls.test_user.save()

    def test_app_root_not_authenticated(self):
        response = self.client.get('/')
        self.assertEqual(response.status_code, 302)

    def test_app_root_authenticated(self):
        self.client.login(username='test', password='test')
        response = self.client.get('/')
        self.assertEqual(response.status_code, 200)
        self.assertIn('ydl.min.js', str(response.content), msg='missing ydl.min.js')
        self.assertIn('ydl.min.css', str(response.content), msg='missing ydl.min.css')
