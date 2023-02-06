# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

from search.resonance.tester.core import (
    EventLog, ExecuteContext, RemoteContext, ScriptExecutor, TestBase, InitContext
)


class ScriptTest(TestBase):
    script: ScriptExecutor

    def prepare(self, context: InitContext):
        self.script = ScriptExecutor(context, context.args)

    def process(self, context: ExecuteContext, remote: RemoteContext, log: EventLog):
        self.script.execute(context, remote, log)

