import pytest_bdd
import yatest.common
from .steps import *  # noqa

settings = {"shards": [{"master_hosts": 1, "replica_hosts": 0}, {"master_hosts": 1, "replica_hosts": 0}]}  # using in context in conftest.py
pytest_bdd.scenarios(yatest.common.source_path('mail/sharpei/tests/integration/feature/conninfo_when_two_masters.feature'))
