import pytest_bdd
import yatest.common
from ..steps import *  # noqa

settings = {"shards": [
            {"master_hosts": 1, "replica_hosts": 2},
            {"master_hosts": 1, "replica_hosts": 0},
            {"master_hosts": 1, "replica_hosts": 1},
            {"master_hosts": 1, "replica_hosts": 0}, ], "kind": "datasync"}  # using in context in conftest.py
pytest_bdd.scenarios(yatest.common.source_path('mail/sharpei/tests/integration/feature/datasync_and_disk_tests/create_user_tests.feature'))
