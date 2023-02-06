# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

from google.protobuf import text_format

from search.priemka.rpslimiter.proto.rpslimiter_config_pb2 import TRpsLimiterConfig

CONFIG_BASE = """
Server {
    Port: 80
    Threads: 3
}

QuotasLoader {
    ReloadInterval: 5000
}

RecordsLoader {
    FilePath: "./records.json"
    ReloadInterval: 5000
}

SyncService {
    SyncInterval: 100
    ParallelRequests: 1
    Timeout: 1000
}

ResponsePolicy {
    NoMatch {
        Allow: true
    }
    NoQuorum {
        Allow: true
    }
    RecordsAge {
        MaxAge: 10000
        Policy {
            Allow: true
        }
    }
    QuotasAge {
        MaxAge: 10000
        Policy {
            Allow: true
        }
    }
}
"""


def generate_rpslimiter_configs(
    localhost,
    ports,
    records_path,
    quotas_path,
    threads=3,
    sync_interval=100,
    sync_parallel=1,
):
    configs = []
    for port in ports:
        config = text_format.Parse(CONFIG_BASE, TRpsLimiterConfig())
        config.Server.Port = port
        config.Server.Threads = threads
        config.RecordsLoader.FilePath = records_path
        config.QuotasLoader.FilePath = quotas_path
        config.SyncService.SyncInterval = sync_interval
        config.SyncService.ParallelRequests = sync_parallel

        for p in ports:
            config.SyncService.Clients.extend(('http://{}:{}'.format(localhost, p),))

        configs.append(config)
    return configs
