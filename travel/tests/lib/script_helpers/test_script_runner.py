# -*- coding: utf8 -*-
import subprocess
from logging import Logger
from mock import Mock
from datetime import datetime

from typing import cast

from travel.avia.library.python.common.utils import environment
from travel.avia.library.python.common.models.scripts import ScriptResult

from travel.avia.admin.lib.script_helpers.script_runner import ScriptRunner
from travel.avia.admin.lib.script_helpers.environment_detector import EnvironmentDetector
from travel.avia.admin.lib.script_helpers.script_run_logger_factory import ScriptRunLoggerFactory
from travel.avia.admin.lib.script_helpers.mail_senders.fail_script_mail_sender import FailScriptMailSender

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.factories import create_script


def create_instance(klass):
    return cast(klass, Mock(klass.__new__(klass)))


class TestScriptRunnerRun(TestCase):
    def setUp(self):
        self._fake_environment_detector = create_instance(EnvironmentDetector)
        self._fake_script_run_logger_factory = create_instance(ScriptRunLoggerFactory)
        self._fake_subprocess = Mock(subprocess)
        self._fake_environment = Mock(environment)
        self._fake_environment.now = Mock(return_value=datetime(2017, 9, 1))
        self._fake_logger = create_instance(Logger)
        self._fake_fail_script_mail_sender = create_instance(FailScriptMailSender)

        self._script_run_logger_factory = ScriptRunner(
            environment_detector=self._fake_environment_detector,
            script_run_logger_factory=self._fake_script_run_logger_factory,
            subprocess=self._fake_subprocess,
            environment=self._fake_environment,
            logger=self._fake_logger,
            fail_script_mail_sender=self._fake_fail_script_mail_sender,
        )

        self._handler = lambda: 42


class TestScriptRunnerRunScriptInAnotherProcess(TestScriptRunnerRun):
    def test_unknown_script(self):
        result, error = self._script_run_logger_factory.run_in_another_process('unknown_script')
        assert not result
        assert error == 'script [unknown_script] is unregistered'

    def test_disabled_script_in_currenc_environment(self):
        script = create_script(code='disabled_script', enabled_in_production=False)
        self._fake_environment_detector.get_environment_type = Mock(return_value='production')

        result, error = self._script_run_logger_factory.run_in_another_process(script.code)

        assert not result
        assert error == 'script [disabled_script] is disabled in production environment'

    def test_unexecutable_script(self):
        script = create_script(code='unexecutable_script')
        self._fake_environment_detector.get_environment_type = Mock(return_value='production')

        result, error = self._script_run_logger_factory.run_in_another_process(script.code)

        assert not result
        assert error == 'script [unexecutable_script] is not executable'

    def test_normal_script(self):
        script = create_script(code='normal_script', command='echo 1')
        self._fake_environment_detector.get_environment_type = Mock(return_value='production')
        self._fake_subprocess.call = Mock(return_value=0)

        result, error = self._script_run_logger_factory.run_in_another_process(script.code)

        assert result
        assert not error

    def test_fail_script(self):
        script = create_script(code='fail_script', command='echo 1')
        self._fake_environment_detector.get_environment_type = Mock(return_value='production')
        self._fake_subprocess.call = Mock(return_value=1)

        result, error = self._script_run_logger_factory.run_in_another_process(script.code)

        assert not result
        assert error == 'script [fail_script] return unexpected code [1]'

    def test_throw_exception(self):
        script = create_script(code='throw_exception', command='echo 1')
        self._fake_environment_detector.get_environment_type = Mock(return_value='production')

        def subprocess_raise(command, shell):
            raise Exception('some subprocess exception')
        self._fake_subprocess.call = Mock(side_effect=subprocess_raise)

        result, error = self._script_run_logger_factory.run_in_another_process(script.code)

        assert not result
        assert error == (
            'subprocess throw exception [some subprocess exception], ' +
            'when executing script [throw_exception]'
        )


class TestScriptRunnerRunScriptInProcess(TestScriptRunnerRun):
    def test_unknown_script(self):
        try:
            self._script_run_logger_factory.run_in_process(self._handler, 'unknown_script')
        except Exception as e:
            assert e.message == 'Can\'t execute script: unknown_script'
        else:
            assert False, 'must raise exception'

    def test_run_script(self):
        self._fake_script_run_logger_factory.create = Mock(return_value='/path/to/log.log')
        script = create_script(code='some_script')
        self._script_run_logger_factory.run_in_process(self._handler, script.code)

        script_results = list(ScriptResult.objects.all())
        assert len(script_results) == 1
        script_result = script_results[0]
        assert script_result.script.id == script.id
        assert script_result.started_at == datetime(2017, 9, 1)
        assert script_result.finished_at == datetime(2017, 9, 1)
        assert script_result.success

    def test_run_failed_script(self):
        self._fake_script_run_logger_factory.create = Mock(return_value='/path/to/log.log')
        script = create_script(code='some_script')

        def failed_handler():
            raise Exception('throw handler')

        try:
            self._script_run_logger_factory.run_in_process(failed_handler, script.code)
        except Exception as e:
            assert e.message == 'throw handler'
        else:
            assert False, 'must raise exception'

        script_results = list(ScriptResult.objects.all())
        assert len(script_results) == 1
        script_result = script_results[0]
        assert script_result.script.id == script.id
        assert script_result.started_at == datetime(2017, 9, 1)
        assert script_result.finished_at == datetime(2017, 9, 1)
        assert not script_result.success

        assert self._fake_fail_script_mail_sender.send.called
