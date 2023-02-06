import pytest_bdd
import yatest.common
from .steps import *  # noqa


settings = {"shards": [{"master_hosts": 1, "replica_hosts": 2}], "kind": "mail"}  # used in context in conftest.py

pytest_bdd.scenarios(yatest.common.source_path('mail/sharpei/tests/integration/feature/conninfo_when_shard_has_master_replica_and_unreachable_host.feature'))
