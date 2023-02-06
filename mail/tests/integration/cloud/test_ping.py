import pytest_bdd
import yatest.common
from ..steps import *  # noqa

settings = {"kind": "cloud", "shards": [{"master_hosts": 1, "replica_hosts": 0}]}  # using in context in conftest.py
pytest_bdd.scenarios(yatest.common.source_path('mail/sharpei/tests/integration/feature/cloud/ping.feature'))
