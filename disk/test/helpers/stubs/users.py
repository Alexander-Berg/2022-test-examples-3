# -*- coding: utf-8 -*-
import json
import urlparse
import mock

from jinja2 import Template

from mpfs.core.services.passport_service import Passport
from test.unit.core.services.passport_service.test_passport import Response


class MailishUserStub(object):

    def __init__(self, uid, email='', display_name='', regname='', login=''):
        self.uid = uid
        self.is_from_pdd = mock.patch('mpfs.core.services.passport_service.Passport.is_from_pdd', return_value=False)

        with open('fixtures/passport/responses/passport_userinfo_mailish_account_response.json') as f:
            template = Template(f.read())
            userinfo_response = template.render(
                uid=self.uid, email=email,
                display_name=display_name, regname=regname,
                login=login
            )

        original_request = Passport.request

        def _request(*_args, **_kwargs):
            _url = _args[1]
            _parsed_url = urlparse.urlparse(_url)
            parsed_query = urlparse.parse_qs(_parsed_url.query)
            if 'method' in parsed_query and parsed_query['method'] == ['userinfo']:
                return Response(userinfo_response)
            else:
                return original_request(*_args, **_kwargs)

        self.passport_open_url = mock.patch('mpfs.core.services.passport_service.Passport.request', wraps=_request)
        self.send_welcome_mail = mock.patch('mpfs.core.user.standart.StandartUser.send_welcome_mail', return_value=None)
        self.tvm_2_0_enabled = mock.patch('mpfs.core.services.common_service.SERVICES_TVM_2_0_ENABLED', False)

    def __enter__(self):
        self.mocked_passport_is_from_pdd = self.is_from_pdd.start()
        self.mocked_passport_open_url = self.passport_open_url.start()
        self.mocked_user_send_welcome_mail = self.send_welcome_mail.start()
        self.mocked_tvm_2_0_enabled_setting = self.tvm_2_0_enabled.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.is_from_pdd.stop()
        self.passport_open_url.stop()
        self.send_welcome_mail.stop()
        self.tvm_2_0_enabled.stop()
