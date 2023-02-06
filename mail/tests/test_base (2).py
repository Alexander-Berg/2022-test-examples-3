# coding: utf-8
import json
from django.core.urlresolvers import reverse
from django.test import TestCase
from django.test.client import RequestFactory

from mlcore.nwsmtp_connect.utils import get_staff_by_email

class TestAll(TestCase):

    def test_email_to_staff(self):
        #assert get_staff_by_email('volozh@yandex-team.com.tr').login == 'volozh'
        pass
