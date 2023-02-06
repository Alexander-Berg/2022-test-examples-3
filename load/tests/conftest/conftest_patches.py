from unittest.mock import patch

import pytest

from load.projects.cloud.loadtesting import config
from load.projects.cloud.loadtesting.db import TankQueries, JobQueries, AmmoQueries, JobConfigQueries, DB, \
    SignalQueries, PresetQueries, OperationQueries, AgentVersionQueries, StorageQueries
from load.projects.cloud.loadtesting.db.tables import AgentVersionTable, AgentVersionStatus
from load.projects.cloud.loadtesting.server.api.private_v1.job import JobServicer
from load.projects.cloud.loadtesting.server.api.private_v1.tank import TankHandler, TankServicer
from load.projects.cloud.cloud_helper.metadata_compute import SaToken
from load.projects.cloud.cloud_helper.iam import IAM


@pytest.fixture()
def patch_get_tank_method():
    with patch.object(TankHandler, '_get') as get_patch:
        yield get_patch


@pytest.fixture()
def patch_get_compute_instance():
    with patch.object(TankHandler, 'get_compute_instance') as get_patch:
        yield get_patch


@pytest.fixture
def patch_get_preset_resources():
    with patch.object(TankServicer._Create, '_get_preset_resources') as p:
        p.return_value = (1, 1, 1)
        yield p


@pytest.fixture()
def patch_db_agent_version_get_target():
    with patch.object(AgentVersionQueries, 'get_target') as p:
        p.return_value = AgentVersionTable(image_id='target_image_id', status=AgentVersionStatus.TARGET.value)
        yield p


@pytest.fixture()
def patch_db_agent_version_get():
    with patch.object(AgentVersionQueries, 'get') as p:
        p.return_value = AgentVersionTable(image_id='some_image_id')
        yield p


@pytest.fixture()
def patch_list_get_compute_instances():
    with patch.object(TankServicer._List, '_get_compute_instances') as p:
        yield p


@pytest.fixture()
def patch_get_chart_message():
    with patch('load.projects.cloud.loadtesting.server.api.private_v1.job_report.get_chart_message') as p:
        yield p


@pytest.fixture()
def patch_get_clickhouse_client():
    with patch('load.projects.cloud.loadtesting.server.api.private_v2.test_comparison.get_clickhouse_client') as p:
        yield p


@pytest.fixture()
def patch_waiting_for_operation():
    with patch.object(TankHandler, '_waiting_for_operation') as p:
        yield p


@pytest.fixture()
def patch_iam_authenticate():
    with patch.object(IAM, 'authenticate') as iam_auth_patch:
        iam_auth_patch.return_value = 'user_id'
        yield iam_auth_patch


@pytest.fixture()
def patch_iam_authorize():
    with patch.object(IAM, 'authorize') as iam_authorize:
        iam_authorize.return_value = 'user_id'
        yield iam_authorize


@pytest.fixture()
def patch_iam_get_token():
    with patch.object(SaToken, 'get') as p:
        p.return_value = True
        yield p


@pytest.fixture()
def patch_compute_get_instance():
    with patch('load.projects.cloud.cloud_helper.compute.get_instance') as p:
        yield p


@pytest.fixture()
def patch_compute_get_operation():
    with patch('load.projects.cloud.cloud_helper.compute.get_operation') as sa_token:
        yield sa_token


@pytest.fixture()
def patch_compute_create_instance():
    with patch('load.projects.cloud.cloud_helper.compute.create_instance') as p:
        yield p


@pytest.fixture()
def patch_compute_delete_instance():
    with patch('load.projects.cloud.cloud_helper.compute.delete_instance') as p:
        yield p


@pytest.fixture()
def patch_compute_start_instance():
    with patch('load.projects.cloud.cloud_helper.compute.start_instance') as p:
        yield p


@pytest.fixture()
def patch_compute_stop_instance():
    with patch('load.projects.cloud.cloud_helper.compute.stop_instance') as p:
        yield p


@pytest.fixture()
def patch_compute_restart_instance():
    with patch('load.projects.cloud.cloud_helper.compute.restart_instance') as p:
        yield p


@pytest.fixture()
def patch_compute_update_image_id():
    with patch('load.projects.cloud.cloud_helper.compute.update_image_id') as p:
        yield p


@pytest.fixture()
def patch_compute_update_metadata():
    with patch('load.projects.cloud.cloud_helper.compute.update_metadata') as p:
        yield p


@pytest.fixture()
def patch_compute_get_image():
    with patch('load.projects.cloud.cloud_helper.compute.get_image_id') as p:
        yield p


@pytest.fixture(autouse=True)
def patch_compute_get_instance_attributes():
    with patch('load.projects.cloud.cloud_helper.metadata_compute.get_instance_attributes') as p:
        p.return_value = {}
        yield p


@pytest.fixture(autouse=True)
def patch_compute_get_instance_metadata():
    with patch('load.projects.cloud.cloud_helper.metadata_compute.get_instance_metadata') as p:
        p.return_value = {}
        yield p


@pytest.fixture()
def patch_aws_upload_fileobj():
    with patch('load.projects.cloud.cloud_helper.aws.upload_fileobj') as p:
        yield p


@pytest.fixture()
def patch_aws_download_file_to_buffer():
    with patch('load.projects.cloud.cloud_helper.aws.download_file_to_buffer') as download_result:
        yield download_result


@pytest.fixture()
def patch_aws_upload_by_presign_url():
    with patch('load.projects.cloud.cloud_helper.aws.upload_by_presign_url') as p:
        yield p


@pytest.fixture()
def patch_aws_delete_bucket():
    with patch('load.projects.cloud.cloud_helper.aws.delete_bucket') as p:
        yield p


@pytest.fixture()
def patch_aws_get_bucket_stats():
    with patch('load.projects.cloud.cloud_helper.aws.get_bucket_stats') as p:
        yield p


@pytest.fixture()
def patch_aws_check_access_to_file():
    with patch('load.projects.cloud.cloud_helper.aws.check_access_to_file') as p:
        yield p


@pytest.fixture()
def patch_ammo_create():
    with patch('load.projects.cloud.loadtesting.server.api.private_v1.ammo.create_ammo') as p:
        yield p


@pytest.fixture()
def patch_user_iam_token():
    with patch('load.projects.cloud.loadtesting.server.api.common.utils.user_iam_token') as p:
        yield p


@pytest.fixture()
def patch_job_create_config():
    with patch.object(JobServicer._Create, '_create_config') as p:
        yield p


@pytest.fixture()
def patch_job_create_job():
    with patch.object(JobServicer._Create, '_create_job') as p:
        yield p


@pytest.fixture()
def patch_db_get_tank_by_compute_instance_id():
    with patch.object(TankQueries, 'get_by_compute_instance_id') as get_tank_patch:
        yield get_tank_patch


@pytest.fixture(autouse=True)
def patch_load_config():
    with patch('load.projects.cloud.env_config.load_config') as p:
        yield p


@pytest.fixture()
def patch_db_tank_add():
    with patch.object(TankQueries, 'add') as p:
        yield p


@pytest.fixture()
def patch_db_tank_get():
    with patch.object(TankQueries, 'get') as p:
        yield p


@pytest.fixture()
def patch_db_tanks_with_version_by_folder():
    with patch.object(DB, 'tanks_with_version_by_folder') as p:
        yield p


@pytest.fixture(autouse=True)
def patch_db_commit():
    with patch.object(DB, 'commit') as p:
        yield p


@pytest.fixture()
def patch_db_tank_delete():
    with patch.object(TankQueries, 'delete') as p:
        p.return_value = True
        yield p


@pytest.fixture()
def patch_db_update_tank_status():
    with patch.object(TankQueries, 'update_status') as p:
        yield p


@pytest.fixture()
def patch_db_tank_set_update_time():
    with patch.object(TankQueries, 'set_update_time') as p:
        yield p


@pytest.fixture()
def patch_db_tank_count_for_folder():
    with patch.object(TankQueries, 'count_for_folder') as p:
        yield p


@pytest.fixture()
def patch_db_job_count_for_folder():
    with patch.object(JobQueries, 'count_for_folder') as p:
        yield p


@pytest.fixture()
def patch_db_job_get_waiting_for_tank():
    with patch.object(JobQueries, 'get_waiting_for_tank') as p:
        yield p


@pytest.fixture()
def patch_db_job_get_by_folder():
    with patch.object(JobQueries, 'get_by_folder') as p:
        yield p


@pytest.fixture()
def patch_db_get_config():
    with patch.object(JobConfigQueries, 'get') as p:
        yield p


@pytest.fixture()
def patch_db_get_ammo():
    with patch.object(AmmoQueries, 'get') as p:
        yield p


@pytest.fixture()
def patch_db_ammo_get_by_name():
    with patch.object(AmmoQueries, 'get_by_name') as p:
        yield p


@pytest.fixture()
def patch_db_add_ammo():
    with patch.object(AmmoQueries, 'add') as p:
        yield p


@pytest.fixture()
def patch_db_job_update_status():
    with patch.object(JobQueries, 'update_status') as p:
        yield p


@pytest.fixture()
def patch_db_tank_update_agent_version():
    with patch.object(TankQueries, 'update_agent_version') as p:
        yield p


@pytest.fixture()
def patch_db_job_get():
    with patch.object(JobQueries, 'get') as p:
        yield p


@pytest.fixture()
def patch_db_job_filter():
    with patch.object(JobQueries, 'filter') as p:
        yield p


@pytest.fixture()
def patch_db_signal_send_to_tank():
    with patch.object(SignalQueries, 'send_to_tank') as p:
        yield p


@pytest.fixture()
def patch_db_signal_add():
    with patch.object(SignalQueries, 'add') as p:
        yield p


@pytest.fixture()
def patch_db_job_close_pending_signals():
    with patch.object(JobQueries, 'close_pending_signals') as p:
        yield p


@pytest.fixture()
def patch_db_preset_get():
    with patch.object(PresetQueries, 'get') as p:
        yield p


@pytest.fixture()
def patch_db_preset_list():
    with patch.object(PresetQueries, 'list') as p:
        yield p


@pytest.fixture()
def patch_db_operation_get():
    with patch.object(OperationQueries, 'get') as p:
        yield p


@pytest.fixture()
def patch_db_operation_get_by_folder():
    with patch.object(OperationQueries, 'get_by_folder') as p:
        yield p


@pytest.fixture()
def patch_db_operation_add():
    with patch.object(OperationQueries, 'add') as p:
        yield p


@pytest.fixture()
def patch_db_operation_update():
    with patch.object(OperationQueries, 'update') as p:
        yield p


@pytest.fixture()
def patch_db_operation_add_snapshot():
    with patch.object(OperationQueries, 'add_snapshot') as p:
        yield p


@pytest.fixture()
def patch_db_job_delete():
    with patch.object(JobQueries, 'delete') as p:
        yield p


@pytest.fixture()
def patch_db_job_add():
    with patch.object(JobQueries, 'add') as p:
        yield p


@pytest.fixture()
def patch_db_job_append_error():
    with patch.object(JobQueries, 'append_error') as p:
        yield p


@pytest.fixture()
def patch_db_storage_get():
    with patch.object(StorageQueries, 'get') as p:
        yield p


@pytest.fixture()
def patch_db_storage_get_by_folder():
    with patch.object(StorageQueries, 'get_by_folder') as p:
        yield p


@pytest.fixture()
def patch_db_storage_add():
    with patch.object(StorageQueries, 'add') as p:
        yield p


@pytest.fixture()
def patch_db_storage_delete():
    with patch.object(StorageQueries, 'delete') as p:
        yield p


@pytest.fixture()
def patch_charts_init():
    with patch('load.projects.cloud.loadtesting.server.api.private_v1.job_report._Charts.__init__') as p:
        yield p


@pytest.fixture()
def patch_get_all_monitoring_data():
    with patch('load.projects.cloud.loadtesting.server.api.private_v1.job_report.MonitoringCharts._get_all_data') as p:
        yield p


@pytest.fixture()
def patch_get_monitoring_charts():
    with patch('load.projects.cloud.loadtesting.server.api.private_v1.job_report.get_monitoring_charts') as p:
        yield p


@pytest.fixture(autouse=True)
def prepare_logging_settings():
    config.ENV_CONFIG.PERMANENT_LOG_LEVEL = 'debug'
    config.ENV_CONFIG.TEST_MODE = 'true'


@pytest.fixture(autouse=True)
def patch_cluster_id():
    with patch("load.projects.cloud.loadtesting.config.ENV_CONFIG.CLUSTER_ID", 'abc') as p:
        yield p


@pytest.fixture(autouse=True)
def patch_max_workers():
    with patch("load.projects.cloud.loadtesting.config.ENV_CONFIG.MAX_WORKERS", '2') as p:
        yield p


@pytest.fixture(autouse=True)
def patch_observe_metrics():
    with patch('load.projects.cloud.loadtesting.server.api.common.handler.BaseHandler.observe_metrics') as p:
        yield p


@pytest.fixture()
def do_not_send_own_metrics_to_pushgateway():
    def dummy(*args, **kwargs):
        pass

    with patch('load.projects.cloud.loadtesting.server.api.common.utils.push_to_gateway', new=dummy) as p:
        yield p


# Change autouse to True for debugging purposes.
# Stack Trace will be shorter, so one will able to see unpatched method in Stack Trace.
#
# Если забыть запатчить метод, то sqlalchemy лениво попытается инициализировать соединение.
# Причём сделает это именно внутри метода, который первым попытается сходить в базу.
# Но при этом магия ленивой установки соединения сгенерирует очень глубокий стек вызовов.
# А при возникновени ошибки показывается только часть стек трейса.
# В нашем случае, стек трейс будет полон внутренних вызовов sqlalchemy.
# В этом всём великолепии мы не увидим какой-же метод попытался сходить в базу (оказался незапатчен)
# Эта фикстура позволяет свалить метод, который полез в базу задолго до ленивого соединения с базой.
# В этом случае в незапатченный метод будет достаточно близко к концу стека вызовов.
@pytest.fixture(autouse=True)
def sabotage_db_connection():
    def fake_enter(self):
        return self

    def fake_exit(*args):
        pass

    with patch.object(DB, "__enter__", new=fake_enter):
        with patch.object(DB, "__exit__", new=fake_exit):
            yield
