import typing
from collections import defaultdict

import pandas as pd
import numpy as np

from load.projects.lunaparkapi.handlers import report_data as rd
from load.projects.cloud.loadtesting.server.api.private_v1.job_report import CHARTS
from load.projects.cloud.loadtesting.db.connection import get_clickhouse_client
from load.projects.cloud.loadtesting.db.tables import JobTable
from yandex.cloud.priv.loadtesting.v2 import test_pb2 as test_messages


def chart_data_to_message(
    tests: typing.Iterable[JobTable],
    data: pd.DataFrame,
    chart_type: str,
    name: str,
    description: str,
) -> test_messages.TestsComparisonChart:
    cases_data_by_test = defaultdict(list)
    data['t'] -= data.groupby('test_id')['t'].transform(np.min)
    data = data.set_index('t')
    time_range = pd.Index(np.arange(max(data.index) + 1))
    for group, group_data in data.groupby(['test_id', 'tag', 'metric_name']):
        test_id, tag, metric_name = group
        group_data = group_data.reindex(time_range)
        group_data.interpolate(inplace=True, limit_area='inside')
        cases_data_by_test[test_id].append(
            test_messages.MetricData(
                case_name=tag or 'overall',
                metric_name=str(metric_name).removeprefix('metric'),
                metric_value=list(group_data['metric_value']),
            )
        )

    tests_chart_data = []
    for test in tests:
        tests_chart_data.append(
            test_messages.TestComparisonData(
                test_id=test.id,
                responses_per_second=[],  # TODO удалить или заполнять
                cases_data=cases_data_by_test[test.n],
            ),
        )

    return test_messages.TestsComparisonChart(
        chart_type=chart_type,
        name=name,
        description=description,
        ts=list(time_range),
        tests=tests_chart_data,
    )


class _TestsComparison:
    def __init__(
        self, client, tests: typing.Iterable[JobTable], chart_type: str, metrics_names: typing.Iterable[str], lang,
    ):
        self.tests = tests
        self.chart_type = chart_type
        self.lang = lang
        self.client = client
        self.metrics_names = metrics_names

    def get(self):
        return self._get_chart_message(
            self.chart_type,
            CHARTS[self.chart_type][self.lang]['name'],
            CHARTS[self.chart_type][self.lang]['description'],
        )

    def _get_chart_message(self, chart_type: str, name: str, description: str):
        rd_kwargs = dict(
            client=self.client,
            tests_ids=[test.n for test in self.tests],
            metrics_names=self.metrics_names,
        )
        if chart_type == 'QUANTILES':
            data = rd.get_comparison_quantiles_data(**rd_kwargs)
        elif chart_type in ('NET_CODES', 'PROTO_CODES'):
            data = rd.get_comparison_codes_data(**rd_kwargs, chart_type=chart_type)
        elif chart_type == 'INSTANCES':
            data = rd.get_comparison_instances_data(**rd_kwargs)
        else:
            raise ValueError('unknown chart type: {chart_type}'.format(chart_type=chart_type))

        return self.data_to_proto(data, chart_type, name, description)

    def data_to_proto(self, data, chart_type, name, description):
        return chart_data_to_message(self.tests, data, chart_type, name, description)


def get_tests_comparison(tests: typing.Iterable[JobTable], chart_type: str, metrics_names: typing.Iterable[str], lang):
    with get_clickhouse_client() as client:
        return _TestsComparison(client, tests, chart_type, metrics_names, lang).get()
