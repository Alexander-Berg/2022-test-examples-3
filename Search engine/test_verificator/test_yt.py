# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.test_utils import TestCase

from search.priemka.yappy.proto.structures import check_pb2, beta_pb2
from search.priemka.yappy.src.processor.modules.verificator.checks import RunResult
from search.priemka.yappy.src.processor.modules.verificator.checks.yt import NodesDoNotExist
from search.priemka.yappy.src.yappy_lib.config_utils import get_test_config


class TestYtChecks(TestCase):
    def test_NodesDoNotExist(self):
        config = get_test_config()
        beta = beta_pb2.Beta()

        test_input = (
            (
                # No arguments.
                (),
                RunResult(success=False, message=r'exception in.*NodesDoNotExist.*', error='.*no paths supplied.*'),
            ),
            (
                # Invalid path.
                ('lol',),
                RunResult(success=False, message=r'exception in.*NodesDoNotExist.*', error='.*argument.*is invalid.*'),
            ),
            (
                # Unsupported cluster.
                ('seneca://tmp', ),
                RunResult(success=False, message=r'exception in.*NodesDoNotExist.*', error='.*argument.*is invalid.*'),
            ),
        )

        for arguments, expected_result in test_input:
            check_runner = NodesDoNotExist(config, check_pb2.Check(check_arguments=arguments))
            result = check_runner.run(beta)

            self.assertEqual(result.success, expected_result.success)

            if expected_result.message:
                self.assertRegexpMatches(result.message, expected_result.message)
            if expected_result.error:
                self.assertRegexpMatches(result.error, expected_result.error)
