# -*- coding: utf8 -*-
import pytest

from travel.avia.library.python.common.utils import environment
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.admin.lib.script_helpers.mail_senders.script_reports_mail_sender import script_reports_mail_sender
from travel.avia.library.python.tester.factories import create_script, create_script_result

YOUR_EMAIL = 'your_mail@yandex-team.ru'


class TestManualScriptReportsMailSender(TestCase):
    def test(self):
        if YOUR_EMAIL == 'your_mail@yandex-team.ru':
            pytest.skip('manual_test')

        now = environment.now()

        script = create_script(code='success_script')
        create_script_result(script=script, started_at=now, finished_at=now, success=True)
        create_script_result(script=script, started_at=now, finished_at=now, success=True)
        create_script_result(script=script, started_at=now, finished_at=now, success=True)

        script = create_script(code='fail_script')
        create_script_result(script=script, started_at=now, finished_at=now, success=False)
        create_script_result(script=script, started_at=now, finished_at=now, success=False)
        create_script_result(script=script, started_at=now, finished_at=now, success=False)

        script = create_script(code='nonstable_script')
        create_script_result(script=script, started_at=now, finished_at=now, success=False)
        create_script_result(script=script, started_at=now, finished_at=now, success=True)

        script_reports_mail_sender.send()
