# -*- coding: utf8 -*-
from datetime import datetime
import ujson

from mock import Mock
from library.python import resource

from travel.avia.library.python.common.utils import environment
from travel.avia.admin.lib.script_helpers.script_reporter import ScriptReporter
from travel.avia.library.python.tester.factories import create_script, create_script_result
from travel.avia.library.python.tester.testcase import TestCase


class TestScriptReporter(TestCase):
    def setUp(self):
        self._fake_environment = Mock(environment)
        self._fake_environment.now = Mock(return_value=datetime(2017, 2, 1, 12, 0))
        self._reporter = ScriptReporter(self._fake_environment)

        daily_failed_script = create_script(code='daily_failed_script')
        create_script_result(
            started_at=datetime(2017, 2, 1, 11, 0),
            finished_at=datetime(2017, 2, 1, 11, 30),
            script=daily_failed_script,
            success=False
        )

        weekly_failed_script = create_script(code='weekly_failed_script')
        create_script_result(
            started_at=datetime(2017, 1, 30),
            finished_at=datetime(2017, 1, 30),
            script=weekly_failed_script,
            success=False
        )

        monthly_failed_script = create_script(code='monthly_failed_script')
        create_script_result(
            started_at=datetime(2017, 1, 10),
            finished_at=datetime(2017, 1, 10),
            script=monthly_failed_script,
            success=False
        )

        yearly_failed_script = create_script(code='yearly_failed_script')
        create_script_result(
            started_at=datetime(2016, 1, 10),
            finished_at=datetime(2016, 1, 10),
            script=yearly_failed_script,
            success=False
        )

        success_script = create_script(code='success_script')
        create_script_result(
            started_at=datetime(2017, 2, 1),
            finished_at=datetime(2017, 2, 1),
            script=success_script,
            success=True
        )

    def test(self):
        content = resource.find('lib/script_helpers/data/script_reporter_data.json')

        expected = ujson.loads(content)

        report = self._reporter.make()
        assert expected == report.__json__()
