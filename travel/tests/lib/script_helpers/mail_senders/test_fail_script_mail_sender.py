# -*- coding: utf8 -*-
import pytest

from travel.avia.admin.lib.script_helpers.mail_senders.fail_script_mail_sender import fail_script_mail_sender
from travel.avia.library.python.tester.factories import create_script, create_script_result
from travel.avia.library.python.tester.testcase import TestCase

YOUR_EMAIL = 'your_mail@yandex-team.ru'


class TestManualFailScriptMailSender(TestCase):
    def test(self):
        if YOUR_EMAIL == 'your_mail@yandex-team.ru':
            pytest.skip('manual_test')

        script = create_script(code='some_script')
        script_result = create_script_result(script=script)

        fail_script_mail_sender.send(script, script_result, to_email=YOUR_EMAIL)
