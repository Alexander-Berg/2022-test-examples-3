from pytest import mark
from yandex.cloud.priv.loadtesting.v2 import stats_service_pb2 as stats_service


@mark.usefixtures(
    'patch_iam_authenticate',
    'patch_iam_authorize',
    'patch_iam_get_token',
)
@mark.parametrize(('agents_count', 'tests_count'), [(0, 1), (1, 0), (2, 2)])
def test_get_stats(stats_stub, agents_count, tests_count, patch_db_tank_count_for_folder, patch_db_job_count_for_folder):
    patch_db_tank_count_for_folder.return_value = agents_count
    patch_db_job_count_for_folder.return_value = tests_count
    result = stats_stub.Folder(
        stats_service.GetFolderStatsRequest(folder_id='folder_id'),
        metadata=(('authorization', 'Bearer bebearer'),)
    )
    assert result.agents_count == agents_count
    assert result.tests_count == tests_count
