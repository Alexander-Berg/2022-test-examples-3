from load.projects.cloud.loadtesting.db.tables import JobTable
import pytest
from yandex.cloud.priv.loadtesting.v1 import tank_job_service_pb2 as job_service


@pytest.mark.parametrize('page_size',
                         [1, 2, 9, 17, 20, 25, 0, None])
@pytest.mark.parametrize('filter_',
                         ['', 'only the best', None])
@pytest.mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
)
def test_get_jobs_list(tank_job_service_stub,
                       patch_db_job_get_by_folder,
                       page_size,
                       filter_,
                       ):
    jobs_pull = [
        JobTable(id=f'job_{i}')
        for i in range(100)
    ]
    jobs_in_iteration = page_size or 100  # DEFAULT_PAGE_SIZE
    page_num = 0
    next_page_token = ''

    jobs_received = []
    while True:
        patch_db_job_get_by_folder.return_value = jobs_pull[page_num * jobs_in_iteration:
                                                            (page_num + 1) * jobs_in_iteration]
        page_num += 1
        resp = tank_job_service_stub.List(
            job_service.ListTankJobsRequest(
                folder_id="fofofo",
                page_size=page_size,
                page_token=next_page_token,
                filter=filter_,
            ),
            metadata=(('authorization', 'Bearer bebearer'),),
        )
        jobs_received += resp.tank_jobs
        if resp.next_page_token == '':
            break
        next_page_token = resp.next_page_token

    assert [j.id for j in jobs_pull] == [j.id for j in jobs_received]
