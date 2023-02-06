# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six

if six.PY2:
    import mock
else:
    import unittest.mock as mock

from typing import AnyStr, Callable   # noqa

from search.martylib.db_utils import prepare_db, clear_db
from search.martylib.test_utils import ServerTestCase, TestCase  # noqa
from search.martylib.unistat.metrics import MetricStorage  # noqa

from search.priemka.yappy.src.model.lineage2_service.service import Lineage2
from search.priemka.yappy.src.yappy_lib.utils import session_scope


class TestCaseWithDB(TestCase):
    def create_test_data(self):
        pass

    @classmethod
    def setUpClass(cls):
        super(TestCaseWithDB, cls).setUpClass()
        clear_db()

    def setUp(self):
        super(TestCaseWithDB, self).setUp()
        prepare_db()
        self.create_test_data()

    def tearDown(self):
        clear_db()
        super(TestCaseWithDB, self).tearDown()


class TestCaseWithStaticDB(TestCase):
    """ For cases where tests will not change data in the DB """
    @classmethod
    def create_test_data(cls):
        pass

    @classmethod
    def setUpClass(cls):
        super(TestCaseWithStaticDB, cls).setUpClass()
        clear_db()
        prepare_db()
        cls.create_test_data()

    @classmethod
    def tearDownClass(cls):
        clear_db()
        super(TestCaseWithStaticDB, cls).tearDownClass()


class LineageIITestCase(TestCaseWithDB):
    ADDITIONAL_LOGGERS = {'yappy'}
    lineage2 = Lineage2()

    def create_test_data(self):
        raise NotImplementedError


class CollectMetricsTestCase(TestCase):

    DB_METRICS_MODULE = 'search.priemka.yappy.src.yappy_lib.db_metrics'

    @classmethod
    def setUpClass(cls):
        super(CollectMetricsTestCase, cls).setUpClass()
        cls.test_data = {}

    @property
    def collect_metrics(self):
        # type: () -> Callable
        raise NotImplementedError

    @property
    def metrics(self):
        # type: () -> MetricStorage
        raise NotImplementedError

    @classmethod
    def mock_db_metrics(cls, func_name, **kwargs):
        # type: (AnyStr) -> mock._patch
        return mock.patch('{}.{}'.format(cls.DB_METRICS_MODULE, func_name), **kwargs)

    def _test(self, metric_names_prefix, mock_metrics):
        # type: (AnyStr, mock.mock._patch) -> None
        metric_names = [key for key in self.test_data.keys() if key.startswith(metric_names_prefix)]
        mock_metrics.return_value = [(key, self.test_data[key]) for key in metric_names]
        with session_scope() as session:
            self.collect_metrics(session)
        metrics = self.metrics
        result = {
            key: metrics.get(metrics.get_metric_name(key)[0])
            for key in metric_names
        }
        expected = {key: self.test_data[key] for key in metric_names}
        self.assertEqual(result, expected)
