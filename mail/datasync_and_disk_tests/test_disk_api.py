import pytest_bdd
import yatest.common
from ..steps import *  # noqa

settings = {"shards": [{"master_hosts": 1, "replica_hosts": 2}], "kind": "disk"}  # using in context in conftest.py
pytest_bdd.scenarios(yatest.common.source_path('mail/sharpei/tests/integration/feature/datasync_and_disk_tests/disk_tests.feature'))
