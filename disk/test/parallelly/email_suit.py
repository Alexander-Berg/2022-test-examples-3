# coding: utf-8
from copy import deepcopy

import mock
from nose_parameterized import parameterized

from mpfs.config import settings
from mpfs.core.email.logic import send_email_async_by_uid

from test.base import DiskTestCase
from test.helpers.stubs.resources.users_info import DEFAULT_USERS_INFO
from test.helpers.stubs.services import PassportStub


class EmailSenderTestCase(DiskTestCase):

    @parameterized.expand([
        ('ru', True),
        ('by', False),
        ('kz', False),
        ('en', False),
        ('tr', False),
        ('uk', False),
        ('ua', False),
        ('us', False),
        ('nl', False),
        (None, False),
    ])
    def test_not_send_gdpr_unsafe_email(self, country, gdpr_safe):
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['country'] = country
        campaign_settings = {'test': {'enabled': True, 'gdpr_safe': gdpr_safe, 'templates': {'ru': '123'}}}
        with mock.patch.dict(settings.email_sender_campaigns, campaign_settings), \
                mock.patch('mpfs.core.services.email_sender_service.EmailSenderService.send') as send_mock, \
                PassportStub(userinfo=user_info):
            send_email_async_by_uid(self.uid, 'test')
            if gdpr_safe:
                send_mock.assert_called_once()
            else:
                send_mock.assert_not_called()
