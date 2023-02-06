# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

from search.resonance.tester.core import InitContext, EventLog, ExecuteContext, RemoteContext


class TestBase(object):
    def prepare(self, context: InitContext):
        raise NotImplementedError()

    def validate_context(self, context: ExecuteContext, remote: RemoteContext):
        pass

    def process(self, context: ExecuteContext, remote: RemoteContext, log: EventLog):
        raise NotImplementedError()
