import pytest
import datetime
from load.projects.cloud.loadtesting.server.api.private_v1.job_report import chart_data_to_message, CHARTS, get_job_end_time
from load.projects.cloud.loadtesting.db.tables import JobTable
from yandex.cloud.priv.loadtesting.v1 import tank_job_pb2 as messages


@pytest.mark.usefixtures('patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token')
def test_message(patch_db_job_get):
    job = JobTable(
        tank_id="tid",
        id='some_job_id'
    )
    patch_db_job_get.return_value = job

    data = {'data': {'cases': {'overall': {'111': [2.0, 1.0, 3.0, 2.0, 4.0, 3.0, 5.0, 4.0, 6.0, 5.0]},
                               '10': {'111': [1.0, 0.0, 2.0, 1.0, 2.0, 1.0, 3.0, 2.0, 3.0, 2.0]},
                               '50': {'111': [1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 2.0, 2.0, 3.0, 3.0]}},
                     'ts': [1632495427, 1632495428, 1632495429, 1632495430, 1632495431, 1632495432, 1632495433,
                            1632495434, 1632495435, 1632495436],
                     'responses_per_second': [0, 2.0, 1.0, 3.0, 2.0, 3.0, 5.0, 4.0, 6.0, 0]}}
    instances = [0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
    chart_type = 'NET_CODES'

    result = chart_data_to_message(
        job, data, chart_type, instances,
        CHARTS[chart_type]['ru']['name'], CHARTS[chart_type]['ru']['description']
    )
    expected = messages.TankChart(
        chart_type=chart_type,
        job_id=job.id,
        ts=data['data']['ts'],
        name=CHARTS[chart_type]['ru']['name'],
        description=CHARTS[chart_type]['ru']['description'],
        threads=[0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
    )
    assert result.chart_type == expected.chart_type
    assert result.job_id == expected.job_id
    assert result.name == expected.name
    assert result.description == expected.description
    assert result.ts == expected.ts
    assert result.threads == expected.threads


def test_get_job_end_time():
    job = JobTable(tank_id="tid", id='some_job_id')
    assert get_job_end_time(job) is None
    end_time = datetime.datetime.utcnow()
    job.finished_at = end_time
    assert get_job_end_time(job) == int(end_time.timestamp())
