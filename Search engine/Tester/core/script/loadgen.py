# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

import random
import string
import typing

from search.resonance.pylib.loadgen import LoadgenClientBase
from search.resonance.tester.core.context import ExecuteContext, RemoteContext
from search.resonance.tester.core.event import EventLog
from search.resonance.tester.proto.script_pb2 import TLoadgenConfig


def generate_urls(config: TLoadgenConfig.TPathGenerator, alphabet=string.ascii_letters + string.digits):
    result = ()
    for url, url_count in config.Paths.items():
        result += (url,) * url_count
    if config.RandomFormat:
        rand = random.Random(config.Seed)
        result = tuple(
            config.RandomFormat.format(
                path=url,
                random=''.join(rand.choices(alphabet, k=config.RandomSize))
            )
            for url in result
        )
    return result


class Loadgen(object):
    config: TLoadgenConfig

    def __init__(self, config: TLoadgenConfig):
        self.config = config

    def start(self, context: ExecuteContext, remote: RemoteContext, log: EventLog) -> typing.List[LoadgenClientBase]:
        loadgens = []
        for upstream in remote.upstreams:
            loadgen = context.create_loadgen()
            loadgen_config = LoadgenClientBase.Config()
            loadgen_config.host = upstream
            loadgen_config.connections = self.config.Connections or 1
            loadgen_config.rps = self.config.Rps or 1
            loadgen_config.threads = self.config.Threads or 1
            if self.config.HasField('PathGenerator'):
                loadgen_config.paths = generate_urls(self.config.PathGenerator)

            if self.config.MultiplyConnections:
                loadgen_config.connections *= len(remote.backends)

            if self.config.MultiplyRps:
                loadgen_config.rps *= len(remote.backends)

            log.loadgen_start(loadgen.id, loadgen_config)
            loadgen.start(loadgen_config)
            loadgens.append(loadgen)
        return loadgens

    @staticmethod
    def stop(loadgens: typing.List[LoadgenClientBase], log: EventLog):
        for loadgen in loadgens:
            log.loadgen_stop(loadgen.id)
            loadgen.stop()
