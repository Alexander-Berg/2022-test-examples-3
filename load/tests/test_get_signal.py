from unittest.mock import patch

import grpc
import pytest
from datetime import datetime

import yandex.cloud.priv.loadtesting.agent.v1.job_service_pb2 as messages
from yandex.cloud.priv.loadtesting.agent.v1.job_service_pb2_grpc import JobServiceStub
from load.projects.cloud.loadtesting.db.tables import JobTable, TankTable
from load.projects.cloud.loadtesting.server.api.private_v1.job import create_message
from load.projects.cloud.loadtesting.server.api.agent_public.job import Jobber


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token'
)
def test_stop_signal_got(
    job_stub: JobServiceStub, patch_db_job_get,
    patch_db_signal_send_to_tank
):
    patch_db_job_get.return_value = JobTable(id='job_id', folder_id='folder_id')
    patch_db_signal_send_to_tank.return_value.__enter__.return_value = "stop"
    signal_got: messages.JobSignalResponse = job_stub.GetSignal(
        messages.JobSignalRequest(
            job_id="no matter"
        ),
        metadata=(('authorization', 'Bearer bebearer'),)
    )
    assert signal_got.signal == messages.JobSignalResponse.STOP


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token'
)
def test_no_signal_got(
    job_stub: JobServiceStub, patch_db_job_get,
    patch_db_signal_send_to_tank
):
    patch_db_job_get.return_value = JobTable(id='job_id', folder_id='folder_id')
    patch_db_signal_send_to_tank.return_value.__enter__.return_value = None
    signal_got: messages.JobSignalResponse = job_stub.GetSignal(
        messages.JobSignalRequest(
            job_id="no matter"
        ),
        metadata=(('authorization', 'Bearer bebearer'),)
    )
    assert signal_got.signal == messages.JobSignalResponse.SIGNAL_UNSPECIFIED


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token'
)
def test_no_job(
    job_stub: JobServiceStub, patch_db_job_get,
):
    patch_db_job_get.return_value = None
    try:
        messages.JobSignalResponse = job_stub.GetSignal(
            messages.JobSignalRequest(
                job_id="not exist"
            ),
            metadata=(('authorization', 'Bearer bebearer'),)
        )
        raise AssertionError("Exception expected, but is has not been raised.")
    except grpc.RpcError as e:
        assert e.code() == grpc.StatusCode.NOT_FOUND


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_db_job_update_status',
    'patch_db_get_tank_by_compute_instance_id',
)
@pytest.mark.parametrize('status', [
    messages.ClaimJobStatusRequest.FINISHED,
    messages.ClaimJobStatusRequest.NOT_FOUND,
])
def test_job_done(job_stub: JobServiceStub, patch_db_job_get,
                  status, patch_db_job_close_pending_signals,
                  patch_db_tank_get):
    job = JobTable(
        tank_id="tid",
        started_at=datetime(2021, 9, 24, 12, 0),
        finished_at=datetime(2021, 9, 24, 12, 0),
    )
    patch_db_job_get.return_value = job
    patch_db_tank_get.return_value = TankTable(
        folder_id="fid",
        current_job="some job",
    )
    with patch.object(Jobber._ClaimStatus, '_utcnow') as p:
        p.return_value = datetime(2021, 9, 24, 12, 0)
        job_stub.ClaimStatus(
            messages.ClaimJobStatusRequest(
                job_id="some job",
                status=status,
            ),
            metadata=(('authorization', 'Bearer bebearer'),),
        )
    patch_db_job_close_pending_signals.assert_called_once_with(job, create_message(job), error='')
