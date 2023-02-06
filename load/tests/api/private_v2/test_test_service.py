import grpc

import pandas as pd
from pytest import mark, raises
from google.protobuf.timestamp_pb2 import Timestamp
from load.projects.cloud.loadtesting.db.tables import JobTable, JobStatus, OperationTable, TankTable
from load.projects.cloud.loadtesting.server.api.private_v1.job_config import JobConfig
from load.projects.cloud.loadtesting.server.api.private_v1.job_report import CHARTS
from yandex.cloud.priv.loadtesting.v2.test_service_pb2_grpc import TestServiceStub
import yandex.cloud.priv.loadtesting.v2.test_pb2 as test_message
import yandex.cloud.priv.loadtesting.v2.test_service_pb2 as test_service


COMPARISON_QUANTILES_DB_VALUES = pd.DataFrame(
    [
        (1, '50', 10, 30, 60),
        (1, '', 10, 25, 55),
        (1, '50', 11, 35, 65),
        (1, '', 11, 25, 55),
        (1, '50', 12, 40, 70),
        (1, '', 12, 25, 55),
        (1, '50', 13, 40, 70),
        (1, '', 13, 25, 55),
        (2, '50', 20, 40, 60),
        (2, '', 20, 25, 55),
        (2, '', 21, 25, 55),
        (2, '50', 22, 50, 70),
        (2, '', 22, 25, 55),
    ],
    columns=['test_id', 'tag', 't', 'metric50', 'metric75'],
)
COMPARISON_QUANTILES_EXPECTED_RESULT = test_message.TestsComparisonChart(
    chart_type='QUANTILES',
    name='Квантили времен ответов',
    description='',
    ts=[0, 1, 2, 3],
    tests=[
        test_message.TestComparisonData(
            test_id='1',
            responses_per_second=[],
            cases_data=[
                test_message.MetricData(case_name='overall', metric_name='50', metric_value=[25, 25, 25, 25]),
                test_message.MetricData(case_name='overall', metric_name='75', metric_value=[55, 55, 55, 55]),
                test_message.MetricData(case_name='50', metric_name='50', metric_value=[30, 35, 40, 40]),
                test_message.MetricData(case_name='50', metric_name='75', metric_value=[60, 65, 70, 70]),
            ],
        ),
        test_message.TestComparisonData(
            test_id='2',
            responses_per_second=[],
            cases_data=[
                test_message.MetricData(case_name='overall', metric_name='50', metric_value=[25, 25, 25, float('nan')]),
                test_message.MetricData(case_name='overall', metric_name='75', metric_value=[55, 55, 55, float('nan')]),
                test_message.MetricData(case_name='50', metric_name='50', metric_value=[40, 45, 50, float('nan')]),
                test_message.MetricData(case_name='50', metric_name='75', metric_value=[60, 65, 70, float('nan')]),
            ],
        ),
    ],
)

COMPARISON_CODES_DB_VALUES = pd.DataFrame(
    [
        (1, '50', 10, 0, 0),
        (1, '', 10, 0, 10),
        (1, '50', 11, 0, 10),
        (1, '', 11, 0, 20),
        (1, '50', 10, 200, 5),
        (1, '', 10, 200, 10),
        (1, '50', 11, 200, 10),
        (1, '', 11, 200, 20),

        (2, '50', 10, 0, 10),
        (2, '', 10, 0, 20),
        (2, '50', 10, 200, 15),
        (2, '', 10, 200, 25),
    ],
    columns=['test_id', 'tag', 't', 'metric_name', 'metric_value'],
)

COMPARISON_INSTANCES_DB_VALUES = pd.DataFrame(
    [
        (1, 10, 5),
        (1, 11, 6),
        (1, 12, 7),

        (2, 10, 4),
        (2, 11, 5),
        (2, 12, 6),
    ],
    columns=['test_id', 't', 'metric_value'],
)
COMPARISON_INSTANCES_EXPECTED_RESULT = test_message.TestsComparisonChart(
    chart_type='INSTANCES',
    name='Тестирующие потоки для всего теста',
    description='',
    ts=[0, 1, 2],
    tests=[
        test_message.TestComparisonData(
            test_id='1',
            responses_per_second=[],
            cases_data=[
                test_message.MetricData(case_name='overall', metric_name='instances', metric_value=[5, 6, 7]),
            ],
        ),
        test_message.TestComparisonData(
            test_id='2',
            responses_per_second=[],
            cases_data=[
                test_message.MetricData(case_name='overall', metric_name='instances', metric_value=[4, 5, 6]),
            ],
        ),
    ],
)


def get_codes_comparison_expected_result(chart_type):
    return test_message.TestsComparisonChart(
        chart_type=chart_type,
        name=CHARTS[chart_type]['ru']['name'],
        description=CHARTS[chart_type]['ru']['description'],
        ts=[0, 1],
        tests=[
            test_message.TestComparisonData(
                test_id='1',
                responses_per_second=[],
                cases_data=[
                    test_message.MetricData(case_name='overall', metric_name='0', metric_value=[10, 20]),
                    test_message.MetricData(case_name='overall', metric_name='200', metric_value=[10, 20]),
                    test_message.MetricData(case_name='50', metric_name='0', metric_value=[0, 10]),
                    test_message.MetricData(case_name='50', metric_name='200', metric_value=[5, 10]),
                ],
            ),
            test_message.TestComparisonData(
                test_id='2',
                responses_per_second=[],
                cases_data=[
                    test_message.MetricData(case_name='overall', metric_name='0', metric_value=[20, float('nan')]),
                    test_message.MetricData(case_name='overall', metric_name='200', metric_value=[25, float('nan')]),
                    test_message.MetricData(case_name='50', metric_name='0', metric_value=[10, float('nan')]),
                    test_message.MetricData(case_name='50', metric_name='200', metric_value=[15, float('nan')]),
                ],
            ),
        ],
    )


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_user_iam_token',
    'patch_charts_init',
    'patch_get_all_monitoring_data',
)
def test_get_monitoring_report(test_private_stub: TestServiceStub, patch_db_job_get, patch_get_monitoring_charts):
    test = JobTable(id="some test", tank_id="tid")
    patch_db_job_get.return_value = test
    patch_get_monitoring_charts.return_value = []

    result = test_private_stub.GetMonitoringReport(
        test_service.GetMonitoringReportRequest(
            test_id='some test',
        ),
        metadata=(('authorization', 'Bearer bebearer'),),
    )
    assert result.test_id == test.id


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_user_iam_token',
    'patch_db_operation_get',
    'patch_db_operation_add',
    'patch_db_job_delete',
)
@mark.parametrize(('status', 'exp_operation'), [
    (JobStatus.RUNNING.value, grpc.StatusCode.FAILED_PRECONDITION),
    (JobStatus.PREPARING.value, grpc.StatusCode.FAILED_PRECONDITION),
    (JobStatus.AUTOSTOPPED.value, True),
    (JobStatus.FAILED.value, True),
    (JobStatus.CREATED.value, True),
    (JobStatus.STOPPED.value, True)]
)
def test_delete_test(test_private_stub, patch_db_job_get, status, exp_operation):
    patch_db_job_get.return_value = JobTable(id='test_id', folder_id='folder_id', status=status)
    try:
        operation = test_private_stub.Delete(
            test_service.DeleteTestRequest(
                test_id='test_id'),
            metadata=(('authorization', 'Bearer bebearer'),))
        assert operation.description == 'Delete Job'
        if exp_operation is not True:
            assert False, 'We should never be here'
    except grpc.RpcError as error:
        assert error.code() == exp_operation


@mark.parametrize(
    ('field_name', 'field_value'),
    [
        ('name', 'New name'),
        ('description', 'New description'),
        ('favorite', True),
        ('favorite', False),
        ('target_version', '1.2'),
        ('imbalance_ts', 1650311116),
        ('imbalance_point', 12),
        ('imbalance_point', 0)
    ]
)
@mark.usefixtures(
    'patch_user_iam_token', 'patch_db_operation_get',
    'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
    'patch_db_operation_add',
)
def test_update_job(test_private_stub, patch_db_job_get, patch_db_job_add, field_name, field_value):
    job = JobTable(id='job_id', folder_id='folder_id')
    patch_db_job_get.return_value = job
    patch_db_job_add.return_value = job
    update_request = test_service.UpdateTestRequest(test_id='job_id')
    update_request.__setattr__(field_name, field_value)

    result = test_private_stub.Update(update_request, metadata=(('authorization', 'Bearer bebearer'),))
    assert result.done is True


@mark.usefixtures(
    'patch_user_iam_token', 'patch_db_operation_get',
    'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
    'patch_db_operation_add',
)
def test_update_job_imbalance_at(test_private_stub, patch_db_job_get, patch_db_job_add):
    job = JobTable(id='job_id', folder_id='folder_id')
    patch_db_job_get.return_value = job
    patch_db_job_add.return_value = job
    update_request = test_service.UpdateTestRequest(
        test_id='job_id',
        imbalance_at=Timestamp(seconds=1652885870, nanos=255387067),
    )
    result = test_private_stub.Update(update_request, metadata=(('authorization', 'Bearer bebearer'),))
    assert result.done is True


@mark.usefixtures(
    'patch_user_iam_token',
    'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token'
)
def test_valid_config(test_private_stub):
    result = test_private_stub.ValidateConfig(
        test_service.ValidateConfigRequest(
            folder_id='folder_id',
            config="",
        )
    )
    assert result.status == test_service.ValidateConfigResponse.Status.FAILED


@mark.usefixtures(
    'patch_user_iam_token',
    'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
    'patch_db_operation_add', 'patch_db_job_add', 'patch_db_signal_add',
)
def test_stop_test(test_private_stub, patch_db_job_get, patch_db_operation_get):
    patch_db_operation_get.return_value = OperationTable(id='op_id')
    patch_db_job_get.return_value = JobTable(id='test_id', status=JobStatus.RUNNING.value)
    operation = test_private_stub.Stop(
        test_service.StopTestRequest(
            test_id='test_id',
        )
    )
    assert operation.description == "Stop Job"


@mark.usefixtures(
    'patch_user_iam_token',
    'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
)
def test_list_tests(test_private_stub, patch_db_job_get_by_folder):
    patch_db_job_get_by_folder.return_value = [JobTable(id='test_id1'), JobTable(id='test_id2')]
    tests = test_private_stub.List(
        test_service.ListTestsRequest(
            folder_id='folder_id',
        )
    )
    for test, test_id in zip(tests.tests, ['test_id1', 'test_id2']):
        assert test.id == test_id


@mark.usefixtures(
    'patch_user_iam_token',
    'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
)
def test_get_tests(test_private_stub, patch_db_job_get):
    patch_db_job_get.return_value = JobTable(id='test_id')
    test = test_private_stub.Get(
        test_service.GetTestRequest(
            test_id='test_id',
        )
    )
    assert test == test_message.Test(id='test_id', created_at=Timestamp(), started_at=Timestamp(), finished_at=Timestamp(), updated_at=Timestamp(), imbalance_at=Timestamp(), payload_id="")


@mark.usefixtures(
    'patch_iam_authenticate', 'patch_iam_get_token',
    'patch_db_job_add', 'patch_db_operation_add',
    'patch_db_operation_update', 'patch_db_ammo_get_by_name'
)
def test_create_test_operation(test_private_stub, patch_db_tank_get, patch_job_create_config, patch_job_create_job, patch_db_operation_get, patch_db_job_get, patch_iam_authorize):
    patch_db_operation_get.return_value = OperationTable(id='op_id', done_resource_snapshot='{}')
    patch_db_job_get.return_value = JobTable(id='test_id')

    patch_db_tank_get.return_value = TankTable(
        folder_id='folder',
        id='sdc'
    )
    patch_job_create_config.return_value = JobConfig()
    patch_job_create_job.return_value = JobTable()
    operation = test_private_stub.Create(
        test_service.CreateTestRequest(
            folder_id='folder',
            form_config=test_message.Config(generator='PHANTOM', load_schedule={'load_type': 'RPS', 'load_profile': ['const(30, 30)']}),
            agent_instance_id='sdc',
        ),
        metadata=(('authorization', 'Bearer bebearer'),))
    assert operation.description == 'Create Job'
    # check metadata
    metadata = test_service.CreateTestMetadata()
    operation.metadata.Unpack(metadata)
    assert metadata.test_id.startswith('abc')  # created id
    # check response
    response = test_message.Test()
    operation.response.Unpack(response)
    assert response.id == 'test_id'  # id from JobTable
    patch_iam_authorize.assert_called()


@mark.usefixtures(
    'patch_user_iam_token', 'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
)
def test_compare_quantiles(test_private_stub: TestServiceStub, patch_db_job_filter, patch_get_clickhouse_client):
    test_ids = ['1', '2']
    folder_id = 'folder'
    patch_db_job_filter.return_value = [
        JobTable(n=int(test_id), id=test_id, folder_id=folder_id) for test_id in test_ids
    ]
    patch_get_clickhouse_client.return_value.__enter__.return_value.query_dataframe.return_value = COMPARISON_QUANTILES_DB_VALUES

    compare_result = test_private_stub.Compare(
        test_service.CompareTestsRequest(
            folder_id=folder_id,
            chart_type='QUANTILES',
            test_ids=test_ids,
            metrics_names=['50', '75'],
        ),
    )

    assert str(compare_result) == str(COMPARISON_QUANTILES_EXPECTED_RESULT)


@mark.usefixtures(
    'patch_user_iam_token', 'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
)
def test_compare_quantiles__tests_not_found(
    test_private_stub: TestServiceStub, patch_db_job_filter, patch_get_clickhouse_client,
):
    test_ids = ['1', '2']
    folder_id = 'folder'
    patch_db_job_filter.return_value = []

    with raises(grpc.RpcError) as error:
        test_private_stub.Compare(
            test_service.CompareTestsRequest(
                folder_id=folder_id,
                chart_type='QUANTILES',
                test_ids=test_ids,
                metrics_names=['120'],
            ),
        )
    assert error.value.details() == 'No tests found.'


@mark.usefixtures(
    'patch_user_iam_token', 'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
)
def test_compare_quantiles__wrong_metric(
    test_private_stub: TestServiceStub, patch_db_job_filter, patch_get_clickhouse_client,
):
    test_ids = ['1', '2']
    folder_id = 'folder'
    patch_db_job_filter.return_value = [
        JobTable(n=int(test_id), id=test_id, folder_id=folder_id) for test_id in test_ids
    ]

    with raises(grpc.RpcError) as error:
        test_private_stub.Compare(
            test_service.CompareTestsRequest(
                folder_id=folder_id,
                chart_type='QUANTILES',
                test_ids=test_ids,
                metrics_names=['120'],
            ),
        )
    assert 'Unknown metrics' in error.value.details()


@mark.parametrize('chart_type', ('NET_CODES', 'PROTO_CODES'))
@mark.usefixtures(
    'patch_user_iam_token', 'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token',
)
def test_compare_codes(test_private_stub: TestServiceStub, patch_db_job_filter, patch_get_clickhouse_client, chart_type):
    test_ids = ['1', '2']
    folder_id = 'folder'
    patch_db_job_filter.return_value = [
        JobTable(n=int(test_id), id=test_id, folder_id=folder_id) for test_id in test_ids
    ]
    patch_get_clickhouse_client.return_value.__enter__.return_value.query_dataframe.return_value = COMPARISON_CODES_DB_VALUES

    compare_result = test_private_stub.Compare(
        test_service.CompareTestsRequest(
            folder_id=folder_id,
            chart_type=chart_type,
            test_ids=test_ids,
            metrics_names=['0', '200'],
        ),
    )

    assert str(compare_result) == str(get_codes_comparison_expected_result(chart_type))


@mark.usefixtures('patch_user_iam_token', 'patch_iam_authenticate', 'patch_iam_authorize', 'patch_iam_get_token')
def test_compare_instances(test_private_stub: TestServiceStub, patch_db_job_filter, patch_get_clickhouse_client):
    test_ids = ['1', '2']
    folder_id = 'folder'
    patch_db_job_filter.return_value = [
        JobTable(n=int(test_id), id=test_id, folder_id=folder_id) for test_id in test_ids
    ]
    patch_get_clickhouse_client.return_value.__enter__.return_value.query_dataframe.return_value = COMPARISON_INSTANCES_DB_VALUES

    compare_result = test_private_stub.Compare(
        test_service.CompareTestsRequest(
            folder_id=folder_id,
            chart_type='INSTANCES',
            test_ids=test_ids,
            metrics_names=['instances'],
        ),
    )

    assert str(compare_result) == str(COMPARISON_INSTANCES_EXPECTED_RESULT)
