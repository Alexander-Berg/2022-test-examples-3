# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import httpretty
import mock
import pytest
from django.conf import settings

from common.utils.yasmutil import YasmMetricSender
from travel.rasp.wizards.train_wizard_api.lib.storage_timed_execute import ExecutionTimeout, TimedExecutor
from travel.rasp.wizards.train_wizard_api.tests.helpers.postgres_test_case import TestCase


@pytest.fixture(autouse=True)
@httpretty.activate
def m_yasm():
    httpretty.register_uri(
        httpretty.POST, "http://localhost:{}/".format(settings.YASMAGENT_PORT), body='{"status": "ok"}'
    )

    with mock.patch.object(YasmMetricSender, "_GEO_TAG", new="local"):
        # Чтобы не дергался resource_explorer
        yield


class TestTimedExecutor(TestCase):
    def setUp(self):
        self.executor = TimedExecutor()
        super(TestTimedExecutor, self).setUp()

    def test_execute_with_timeout(self):
        storage = self._storage_store.get("slave")

        self.executor.execute_with_timeout(storage, "SELECT pg_sleep(0.01)", 0.25)

        with pytest.raises(ExecutionTimeout):
            self.executor.execute_with_timeout(storage, "SELECT pg_sleep(0.5)", 0.25)

    def tearDown(self):
        self.executor.close()
        super(TestTimedExecutor, self).tearDown()
