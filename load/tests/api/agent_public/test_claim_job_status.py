import pytest

import grpc
import yandex.cloud.priv.loadtesting.agent.v1.job_service_pb2 as messages
from yandex.cloud.priv.loadtesting.agent.v1.job_service_pb2_grpc import JobServiceStub
from load.projects.cloud.loadtesting.db.tables import JobTable, TankTable, JobStatus
from load.projects.cloud.loadtesting.server.api.agent_public.job import CLOSING_STATUSES


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_db_job_update_status',
    'patch_db_get_tank_by_compute_instance_id',
    'patch_db_job_close_pending_signals',
)
@pytest.mark.parametrize(('status', 'curr_status'), [
    (messages.ClaimJobStatusRequest.POST_PROCESS, JobStatus.CREATED),
    (messages.ClaimJobStatusRequest.INITIATED, JobStatus.CREATED),
    (messages.ClaimJobStatusRequest.PREPARING, JobStatus.CREATED),
    (messages.ClaimJobStatusRequest.NOT_FOUND, JobStatus.CREATED),
    (messages.ClaimJobStatusRequest.RUNNING, JobStatus.CREATED),
    (messages.ClaimJobStatusRequest.FINISHING, JobStatus.CREATED),
    (messages.ClaimJobStatusRequest.FINISHED, JobStatus.CREATED),
    (messages.ClaimJobStatusRequest.PREPARING, JobStatus.RUNNING),
    (messages.ClaimJobStatusRequest.AUTOSTOPPED, JobStatus.RUNNING)
])
def test_claim_job_status(job_stub: JobServiceStub, patch_db_job_get, patch_db_tank_get,
                          patch_db_job_update_status, status, curr_status):
    job = JobTable(
        id="some job",
        tank_id="tid",
        status=curr_status.value
    )
    tank = TankTable(
        folder_id='some_folder_id',
        id='some tank id',
        current_job='some job' if curr_status != JobStatus.CREATED else None)
    patch_db_job_get.return_value = job
    patch_db_tank_get.return_value = tank
    job_stub.ClaimStatus(
        messages.ClaimJobStatusRequest(
            job_id="some job",
            status=status,
        ),
        metadata=(('authorization', 'Bearer bebearer'),),
    )
    patch_db_job_update_status.assert_called_once_with(
        job,
        JobStatus(messages.ClaimJobStatusRequest.JobStatus.Name(status))
    )
    if status in CLOSING_STATUSES:
        assert tank.current_job is None
    else:
        assert tank.current_job == "some job"


@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
    'patch_db_job_update_status',
    'patch_db_get_tank_by_compute_instance_id',
    'patch_db_job_close_pending_signals',
)
@pytest.mark.parametrize('status', [
    messages.ClaimJobStatusRequest.POST_PROCESS,
    messages.ClaimJobStatusRequest.INITIATED,
    messages.ClaimJobStatusRequest.PREPARING,
    messages.ClaimJobStatusRequest.NOT_FOUND,
    messages.ClaimJobStatusRequest.RUNNING,
    messages.ClaimJobStatusRequest.FINISHING,
    messages.ClaimJobStatusRequest.FINISHED,
])
@pytest.mark.parametrize('current_job', ['some other job', ''])
def test_claim_job_status_wrong_job(job_stub: JobServiceStub, patch_db_job_get,
                                    status, patch_db_tank_get, current_job):
    job = JobTable(
        id="some job",
        tank_id="tid",
    )
    patch_db_job_get.return_value = job
    patch_db_tank_get.return_value = TankTable(
        folder_id="fid",
        current_job=current_job,
    )
    try:
        job_stub.ClaimStatus(
            messages.ClaimJobStatusRequest(
                job_id="some job",
                status=status,
            ),
            metadata=(('authorization', 'Bearer bebearer'),),
        )
        raise AssertionError("Exception expected, but not raised.")
    except grpc.RpcError as e:
        assert e.code() == grpc.StatusCode.FAILED_PRECONDITION
