# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

import typing

from search.resonance.tester.core import (
    EventLog, ExecuteContext, RemoteContext, TestBase, InitContext
)

from search.resonance.tester.tests.weight_check.weight_check import WeightCheckCase


class WeightCheckTest(TestBase):
    cases: typing.List[WeightCheckCase]

    def prepare(self, context: InitContext):
        self.cases = [WeightCheckCase(case) for case in context.args.Case]

    def process(self, context: ExecuteContext, remote: RemoteContext, log: EventLog):
        for i in range(len(self.cases)):
            with log.scope('case {}'.format(i)) as case_log:
                self.cases[i].process(context, remote, case_log)

