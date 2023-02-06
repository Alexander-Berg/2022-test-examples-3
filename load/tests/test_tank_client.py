from unittest import mock as py_mock
from unittest.mock import patch

import json
import pytest
from pytest import mark
from yandex.cloud.loadtesting.agent.v1 import job_service_pb2
from yandextank.core.tankcore import JobsStorage

from load.projects.cloud.tank_client.client import LoadTestingGRPCClient, TankApiClient, TankapiException
from load.projects.cloud.tank_client.job import Job, AdditionalJobStatus
from load.projects.cloud.tank_client.service import TankClientService
from load.projects.cloud.tank_client.utils import LoadTestingInternalError, LoadTestingNotFoundError, \
    LoadTestingConnectionError, LoadTestingFailedPreconditionError, TankStatus, Generator

FAKE_AGENT_VERSION = 'some_version'
CONFIG = {'client_workdir': 'dir', 'curr_config': 'config', 'storage_file': 'some_file'}


def raise_(ex):
    raise ex


def _tank_client():
    loadtesting_client = LoadTestingGRPCClient('host', 'port', FAKE_AGENT_VERSION,
                                               'agent_id_file',
                                               'storage_url', 'iam_token_service_url',
                                               True,
                                               None)

    tank_api_client = TankApiClient(None, None)
    jobs_storage = JobsStorage('some_file')

    return TankClientService(loadtesting_client, tank_api_client, jobs_storage, 'dir', 1, None, None)


@pytest.fixture()
def tank_client():
    return _tank_client()


@pytest.fixture()
def tank_client_with_job(tank_client):
    tc = _tank_client()
    tc.job = Job(id_='123', job_config='config', origin=Job.Origin.LT_SERVER)
    return tc


@mark.usefixtures(
    'patch_claim_tank_status',
    'patch_agent_id',
    'patch_download_ammo',
)
@mark.parametrize(('tank_status', 'job', 'exp_job'), [
    ({"success": True, "is_preparing": False, "is_testing": False},
     job_service_pb2.Job(id='123', config='{"valid": "json"}', ammo=job_service_pb2.File(name='ammo', content=b'content')),
     True),
    ({"success": True, "is_preparing": False, "is_testing": False},
     job_service_pb2.Job(id='123', config='{"valid": "json"}',
                         test_data=job_service_pb2.StorageObject(object_storage_bucket='bucket',
                                                                 object_storage_filename='s3')),
     True),
    ({}, job_service_pb2.Job(id='123', config='{"valid": "json"}', ammo=job_service_pb2.File(name='ammo', content=b'content')),
     False)
])
def test_get_job(patch_get_tank_status, patch_get_job, tank_status, job, exp_job, tank_client):
    patch_get_tank_status.return_value = tank_status
    patch_get_job.return_value = job
    if job.HasField('ammo'):
        ammo_name = job.ammo.name
    elif job.HasField('test_data'):
        ammo_name = job.test_data.object_storage_filename
    res_job = tank_client.get_job()
    if exp_job:
        assert res_job.id == job.id
        assert res_job.job_config == json.loads(job.config)
        assert res_job.ammo._path.endswith(ammo_name)
    else:
        assert res_job is None


@mark.usefixtures(
    'patch_claim_tank_status',
    'patch_agent_id',
)
@mark.parametrize(('tank_status'), [
    ({"success": True, "is_preparing": False, "is_testing": True}),
    ({"success": True, "is_preparing": True, "is_testing": False})
])
def test_get_manual_job(patch_get_config, patch_get_tank_status, patch_get_cloud_job_id, patch_get_current_job,
                        tank_status, tank_client):
    patch_get_config.return_value = {"pandora": {"enabled": True}}
    patch_get_tank_status.return_value = tank_status
    patch_get_current_job.return_value = {'test_id': 'curr_job_id'}
    patch_get_cloud_job_id.return_value = 'cloud_id'
    job = tank_client.get_job()
    assert job.id == 'cloud_id'
    assert job.tank_job_id == 'curr_job_id'
    assert job.generator == Generator.PANDORA


@mark.usefixtures(
    'patch_claim_tank_status',
    'patch_agent_id',
)
@mark.parametrize('mock_function', [
    lambda *x: raise_(LoadTestingInternalError),
    lambda *x: raise_(LoadTestingNotFoundError),
    lambda *x: raise_(LoadTestingConnectionError("Service is unavailable")),
    lambda *x: raise_(LoadTestingFailedPreconditionError)
])
def test_get_job_error(patch_get_tank_status, patch_get_job, mock_function, tank_client):
    with py_mock.patch.object(LoadTestingGRPCClient, 'get_job', mock_function):
        patch_get_tank_status.return_value = {"success": True, "is_preparing": False, "is_testing": False}
        job = tank_client.get_job()
        assert job is None


@mark.usefixtures(
    'patch_write_config_file',
    'patch_prepare_job',
    'patch_agent_id',
)
def test_prepare_job(tank_client_with_job):
    with py_mock.patch("builtins.open", new_callable=py_mock.mock_open()):
        tank_client_with_job.prepare_lt_job()


@mark.usefixtures('patch_write_config_file')
@mark.parametrize('run_response', [
    {"success": False},
    {},
])
def test_run_job_tankapi_failed(tank_client_with_job, run_response, patch_claim_job_status):
    with patch.object(TankApiClient, 'prepare_job') as p:
        p.return_value = run_response
        with pytest.raises(TankapiException):
            tank_client_with_job.prepare_lt_job()
    patch_claim_job_status.assert_called_with('123', AdditionalJobStatus.FAILED.name, "Could not run job.", None)


@mark.usefixtures(
    'patch_get_tank_status',
    'patch_claim_tank_status',
    'patch_claim_job_status',
    'patch_agent_id',
)
@mark.parametrize(('job_response', 'signal', 'exp_status'), [
    ({}, None, AdditionalJobStatus.FAILED.name),
    ({"status_code": "FINISHED", "exit_code": 28}, None, AdditionalJobStatus.AUTOSTOPPED.name),
    ({"status_code": "FINISHED"}, None, 'FINISHED'),
    ({"status_code": "FINISHED"}, job_service_pb2.JobSignalResponse.Signal.Value('STOP'), 'STOPPED'),
])
def test_serve_lt_job(
        patch_get_job_status,
        patch_claim_job_status,
        patch_get_job_signal,
        patch_stop_job,
        job_response,
        signal,
        exp_status,
        tank_client_with_job,
):
    patch_get_job_status.return_value = job_response
    patch_get_job_signal.return_value = signal
    if signal is None:
        patch_get_job_signal.return_value = job_service_pb2.JobSignalResponse(
            signal=job_service_pb2.JobSignalResponse.Signal.Value('SIGNAL_UNSPECIFIED')
        )
    else:
        patch_get_job_signal.return_value = job_service_pb2.JobSignalResponse(
            signal=signal
        )
        patch_stop_job.return_value = {'success': True}
    tank_client_with_job.serve_lt_job()
    patch_claim_job_status.assert_called_with('123', exp_status, "", None)


@mark.usefixtures(
    'patch_get_tank_status',
    'patch_claim_tank_status',
    'patch_claim_job_status',
    'patch_agent_id',
)
@mark.parametrize(('call_function', 'exception_to_raise'), [
    ('claim_job_status', LoadTestingNotFoundError),
    ('claim_job_status', LoadTestingFailedPreconditionError),
    ('get_job_signal', LoadTestingNotFoundError)
])
def test_serve_lt_job_error(patch_claim_tank_status, patch_get_job_status, patch_get_job_signal, call_function,
                            exception_to_raise, tank_client_with_job):
    with py_mock.patch.object(LoadTestingGRPCClient, call_function, lambda *x: raise_(exception_to_raise)):
        patch_get_job_status.return_value = {"status_code": "TESTING"}
        if call_function != 'get_job_signal':
            patch_get_job_signal.return_value = job_service_pb2.JobSignalResponse(
                signal=job_service_pb2.JobSignalResponse.Signal.Value('SIGNAL_UNSPECIFIED')
            )
        with pytest.raises(exception_to_raise):
            with tank_client_with_job.reporting_tank_status():
                tank_client_with_job.serve_lt_job()
        patch_claim_tank_status.assert_called()


@mark.usefixtures(
    'patch_agent_id',
)
@mark.parametrize(('job_status_response', 'exp_status', 'exp_error', 'exp_error_type'), [
    ({"status_code": "FINISHED"}, AdditionalJobStatus.STOPPED.name, "", None),
    ({"status_code": "TESTING"}, "TESTING", "stopped job is still in progress", None),
])
def test_stop_job(patch_get_job_status, patch_stop_job, job_status_response, exp_status, exp_error,
                  exp_error_type, tank_client_with_job, patch_claim_job_status):
    patch_claim_job_status.return_value = 0
    patch_get_job_status.return_value = job_status_response
    patch_stop_job.return_value = {"success": True}
    tank_client_with_job.serve_stop_signal(stopping_sleep_time=0)
    tank_client_with_job.report_lt_status()
    patch_claim_job_status.assert_called_with(
        tank_client_with_job.job.id,
        exp_status,
        exp_error,
        exp_error_type)


@mark.usefixtures(
    'patch_agent_id',
)
@mark.parametrize(('stop_response', 'exp_status', 'exp_error', 'exp_error_type'), [
    ({"success": False}, AdditionalJobStatus.FAILED.name, "Could not stop job", None),
    ({}, AdditionalJobStatus.FAILED.name, "Could not stop job", None),
    ({"success": False, "error": "Stop error"}, AdditionalJobStatus.FAILED.name, "Could not stop job: Stop error",
     None),
    ({"success": False, "tank_msg": "Stop error"}, AdditionalJobStatus.FAILED.name,
     "Could not stop job: Stop error", 'internal'),
])
def test_stop_job_failed(patch_get_job_status, patch_stop_job, stop_response, exp_status, exp_error,
                         exp_error_type, tank_client_with_job, patch_claim_job_status):
    patch_claim_job_status.return_value = 0
    patch_stop_job.return_value = stop_response
    with pytest.raises(TankapiException):
        tank_client_with_job.serve_stop_signal(stopping_sleep_time=0)
    patch_claim_job_status.assert_called_with(
        tank_client_with_job.job.id,
        exp_status,
        exp_error,
        exp_error_type)


@mark.usefixtures(
    'patch_agent_id',
)
@mark.parametrize(('response', 'exp_error', 'exp_error_type'), [
    ({}, '', None),
    ({'error': 'some error'}, 'some error', None),
    ({'error': 'some error', 'tank_msg': 'some tank_msg'}, 'some error', None),
    ({'tank_msg': 'some tank_msg'}, 'some tank_msg', 'internal'),
    ({'tank_msg': 'some tank_msg', 'exit_code': 1}, 'some tank_msg', 'internal'),
    ({'error': 'some error', 'exit_code': 1}, 'some error', None),
    ({'exit_code': 1}, 'Unknown generator error', None),
    ({'exit_code': 0}, '', None),
])
def test_set_error(response, exp_error, exp_error_type):
    error, error_type = TankApiClient.extract_error(response)
    assert error == exp_error
    assert error_type == exp_error_type


@mark.usefixtures(
    'patch_agent_id',
)
@mark.parametrize(('response', 'exp_status'), [
    ({}, TankStatus.TANK_FAILED),
    ({"success": False}, TankStatus.TANK_FAILED),
    ({"success": False, "is_preparing": False, "is_testing": True}, TankStatus.TANK_FAILED),
    ({"success": "str"}, TankStatus.TANK_FAILED),
    ({"success": True}, TankStatus.TANK_FAILED),
    ({"success": True, "is_preparing": False}, TankStatus.TANK_FAILED),
    ({"success": True, "is_preparing": False, "is_testing": False}, TankStatus.READY_FOR_TEST),
    ({"success": True, "is_preparing": None, "is_testing": False}, TankStatus.READY_FOR_TEST),
    ({"success": True, "is_preparing": True, "is_testing": True}, TankStatus.PREPARING_TEST),
    ({"success": True, "is_preparing": False, "is_testing": True}, TankStatus.TESTING),
])
def test_tank_status(response, exp_status):
    assert TankClientService.tank_status(response) == exp_status


@mark.usefixtures(
    'patch_claim_tank_status',
    'patch_agent_id',
)
@mark.parametrize(('config', 'exp_generator'), [
    ({'pandora': {'enabled': True}}, Generator.PANDORA),
    ({'phantom': {'enabled': True}}, Generator.PHANTOM),
    ({'phantom': {'enabled': False}}, Generator.UNKNOWN),
])
def test_generator(patch_get_tank_status, patch_get_job, config, exp_generator, tank_client):
    assert Job._generator(config) == exp_generator
