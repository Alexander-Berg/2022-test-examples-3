# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.api_public.tests.old_versions import ApiTestCase


class ApiV2TestCase(ApiTestCase):
    def setUp(self):
        super(ApiV2TestCase, self).setUp()
        self.api_version = 'v2'
