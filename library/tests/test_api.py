# coding=utf-8

import sys
import unittest

import vcr

from django_tanker import api


class TankerApiTestCase(unittest.TestCase):
    def setUp(self):
        self.client = api.Tanker(
            project_id='python-django-tanker-test',
            base_url=api.URLS['testing'],
            token='some-oauth-token',
            dry_run=False,
            include_unapproved=True,
        )

    def test_list(self):
        with vcr.use_cassette('tests/fixtures/list.yaml'):
            data = self.client.list()
            self.assertTrue(isinstance(data, dict))
            self.assertIn('horse', data)

    def test_download(self):
        with vcr.use_cassette('tests/fixtures/download.yaml'):
            data = self.client.download('horse', 'ru', False)
            if sys.version_info[0] == 2:
                self.assertTrue(isinstance(data, unicode))
            else:
                self.assertTrue(isinstance(data, str))
            self.assertIn(
                'This file was generated by Yandex.Tanker project',
                data
            )

    def test_upload(self):
        with vcr.use_cassette('tests/fixtures/upload.yaml'):
            result = self.client.upload(
                'upload', 'ru', 'tests/fixtures/django.po', 'update', False
            )
            self.assertTrue(result)