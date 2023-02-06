# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import operator

from search.priemka.yappy.proto.structures.api_pb2 import ApiCheck, ApiBeta
from search.priemka.yappy.proto.structures.check_pb2 import Check

from search.priemka.yappy.src.model.lineage2_service import utils

from search.priemka.yappy.tests.utils.test_cases import TestCase


class AddDefaultChecksTestCase(TestCase):

    def setUp(self):
        self.beta = ApiBeta(
            checks=[
                ApiCheck(check_class='important', severity=Check.Severity.IMPORTANT),
                ApiCheck(check_class='critical', severity=Check.Severity.CRITICAL),
                ApiCheck(check_class='info', severity=Check.Severity.INFO),
            ]
        )
        self.default_checks = ['default-1', 'default-2']

    def test_add_default_checks(self):
        expected = sorted(
            (
                list(self.beta.checks)
                + [ApiCheck(check_class=c) for c in self.default_checks]
            ),
            key=operator.attrgetter('check_class'),
        )
        utils.add_default_checks(self.beta, self.default_checks)
        result = sorted(self.beta.checks, key=operator.attrgetter('check_class'))

        self.assertEqual(result, expected)

    def test_override_default_check(self):
        check_class = 'critical'
        expected = Check.Severity.CRITICAL
        default_checks = [check_class]
        utils.add_default_checks(self.beta, default_checks)
        result = None
        for c in self.beta.checks:
            if c.check_class == check_class:
                result = c.severity

        self.assertEqual(result, expected)

    def test_add_no_default_checks(self):
        expected = sorted(self.beta.checks, key=operator.attrgetter('check_class'))
        utils.add_default_checks(self.beta, [])
        result = sorted(self.beta.checks, key=operator.attrgetter('check_class'))

        self.assertEqual(result, expected)

    def test_add_default_checks_with_severity(self):
        severity = Check.Severity.INFO
        expected = sorted(
            (
                list(self.beta.checks)
                + [ApiCheck(check_class=c, severity=severity) for c in self.default_checks]
            ),
            key=operator.attrgetter('check_class'),
        )
        utils.add_default_checks(self.beta, self.default_checks, severity)
        result = sorted(self.beta.checks, key=operator.attrgetter('check_class'))

        self.assertEqual(result, expected)
