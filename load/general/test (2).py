import grpc

from google.protobuf.any_pb2 import Any
from google.protobuf.json_format import MessageToJson
from yandex.cloud.priv.loadtesting.v2 import test_pb2, test_service_pb2_grpc, test_service_pb2
from load.projects.cloud.loadtesting.db.tables import JobTable
from load.projects.cloud.loadtesting.logan import lookup_logger
from load.projects.cloud.loadtesting.server.api.common import permissions, handler

from load.projects.cloud.loadtesting.server.api.private_v1.job_report import get_monitoring_charts
from load.projects.cloud.loadtesting.server.api.common.utils import authorize

from load.projects.cloud.loadtesting.server.api.private_v1 import job as job_service
from load.projects.cloud.loadtesting.server.api.private_v2.test_comparison import get_tests_comparison
from yandex.cloud.priv.loadtesting.v1 import tank_job_service_pb2, tank_job_pb2, storage_pb2


class TestPrivateServicer(test_service_pb2_grpc.TestServiceServicer):
    def __init__(self):
        self.logger = lookup_logger('TestPrivate')

    class _GetMonitoringReport(handler.BasePrivateHandler):
        _handler_name = 'GetMonitoringReport'

        def proceed(self):
            test = self.db.job.get(self.request.test_id)
            if not test:
                self.context.abort(grpc.StatusCode.NOT_FOUND, f'Test {self.request.test_id} is not found.')
            authorize(self.context, self.user_token, test.folder_id, permissions.TESTS_GET, self.request_id)

            charts = []
            if test.started_at:
                charts = get_monitoring_charts(test, self.lang)

            return test_pb2.MonitoringReport(
                test_id=test.id,
                charts=charts,
                finished=bool(test.finished_at)
            )

    def GetMonitoringReport(self, request, context):
        return self._GetMonitoringReport(self.logger).handle(request, context)

    class _Get(handler.BasePrivateHandler):
        _handler_name = 'Get'

        def __init__(self, parent_logger):
            super().__init__(parent_logger)
            self._parent_logger = parent_logger

        def proceed(self):
            tank_job = job_service.JobServicer._Get(self._parent_logger).handle(tank_job_service_pb2.GetTankJobRequest(id=self.request.test_id), self.context)
            return test_from_tank_job(tank_job)

    def Get(self, request, context):
        return self._Get(self.logger).handle(request, context)

    class _List(handler.BasePrivateHandler):
        _handler_name = 'List'

        def __init__(self, parent_logger):
            super().__init__(parent_logger)
            self._parent_logger = parent_logger

        def proceed(self):
            tank_jobs = job_service.JobServicer._List(self._parent_logger).handle(
                tank_job_service_pb2.ListTankJobsRequest(
                    folder_id=self.request.folder_id,
                    page_size=self.request.page_size,
                    page_token=self.request.page_token,
                    filter=self.request.filter),
                self.context)
            tests = [test_from_tank_job(tank_job) for tank_job in tank_jobs.tank_jobs]
            return test_service_pb2.ListTestsResponse(
                folder_id=tank_jobs.folder_id, tests=tests, next_page_token=tank_jobs.next_page_token)

    def List(self, request, context):
        return self._List(self.logger).handle(request, context)

    class _Create(handler.BasePrivateHandler):
        _handler_name = 'Create'

        def __init__(self, parent_logger):
            super().__init__(parent_logger)
            self._parent_logger = parent_logger

        def proceed(self):
            create_operation = job_service.JobServicer._Create(self._parent_logger).handle(
                self._create_old_message(self.request), self.context)
            if create_operation:
                update_operation_fields(self.db, create_operation, tank_job_service_pb2.CreateTankJobMetadata, test_service_pb2.CreateTestMetadata)
            return create_operation

        @staticmethod
        def _create_old_message(request: test_service_pb2.CreateTestRequest) -> tank_job_service_pb2.CreateTankJobRequest:
            load_type = tank_job_pb2.LoadType.Name(request.form_config.load_schedule.load_type)
            load_schedule = []
            for load_schedule_item in request.form_config.load_schedule.load_schedule:
                schedule_type = tank_job_pb2.ScheduleType.Name(load_schedule_item.type)
                load_schedule.append(tank_job_pb2.LoadSchedule(
                    type=schedule_type,
                    instances=load_schedule_item.instances,
                    duration=load_schedule_item.duration,
                    rps_from=load_schedule_item.rps_from,
                    rps_to=load_schedule_item.rps_to,
                    step=load_schedule_item.step,
                    ops=load_schedule_item.ops,
                    stpd_path=load_schedule_item.stpd_path))
            schedule = tank_job_pb2.Schedule(load_type=load_type, load_schedule=load_schedule,
                                             load_profile=request.form_config.load_schedule.load_profile)
            autostops = []
            for autostop in request.form_config.autostops:
                autostops.append(tank_job_pb2.Autostop(
                    autostop_criteria=autostop.autostop_criteria,
                    autostop_case=autostop.autostop_case,
                    autostop_type=test_pb2.Autostop.AutostopType.Name(autostop.autostop_type)
                ))
            test_data = storage_pb2.StorageObject(
                object_storage_bucket=request.payload_storage_object.object_storage_bucket,
                object_storage_filename=request.payload_storage_object.object_storage_filename
            )
            return tank_job_service_pb2.CreateTankJobRequest(
                folder_id=request.folder_id,
                name=request.name,
                description=request.description,
                labels=request.labels,
                generator=request.form_config.generator,
                tank_instance_id=request.agent_instance_id,
                target_address=request.form_config.target_address,
                target_port=request.form_config.target_port,
                target_version=request.form_config.target_version,
                instances=request.form_config.instances,
                load_schedule=schedule,
                config=request.config,
                ammo_id=request.payload_id,
                ammo_urls=request.form_config.payload_urls,
                ammo_headers=request.form_config.payload_headers,
                ammo_type=request.form_config.payload_type,
                ssl=request.form_config.ssl,
                imbalance_point=request.form_config.imbalance_point,
                imbalance_ts=request.form_config.imbalance_ts,
                logging_log_group_id=request.logging_log_group_id,
                autostops=autostops,
                test_data=test_data
            )

    def Create(self, request, context):
        return self._Create(self.logger).handle(request, context)

    class _Update(handler.BasePrivateHandler):
        _handler_name = 'Update'

        def __init__(self, parent_logger):
            super().__init__(parent_logger)
            self._parent_logger = parent_logger

        def proceed(self):
            operation_update = job_service.JobServicer._Update(self._parent_logger).handle(
                tank_job_service_pb2.UpdateTankJobRequest(
                    id=self.request.test_id,
                    update_mask=self.request.update_mask,
                    name=self.request.name,
                    description=self.request.description,
                    labels=self.request.labels,
                    favorite=self.request.favorite,
                    target_version=self.request.target_version,
                    imbalance_point=self.request.imbalance_point,
                    imbalance_ts=self.request.imbalance_ts,
                    imbalance_comment=self.request.imbalance_comment,
                    imbalance_at=self.request.imbalance_at
                ),
                self.context)
            if operation_update:
                update_operation_fields(self.db, operation_update, tank_job_service_pb2.UpdateTankJobMetadata, test_service_pb2.UpdateTestMetadata)
            return operation_update

    def Update(self, request, context):
        return self._Update(self.logger).handle(request, context)

    class _Stop(handler.BasePrivateHandler):
        _handler_name = 'Stop'

        def __init__(self, parent_logger):
            super().__init__(parent_logger)
            self._parent_logger = parent_logger

        def proceed(self):
            stop_operation = job_service.JobServicer._Stop(self._parent_logger).handle(
                tank_job_service_pb2.StopTankJobRequest(
                    id=self.request.test_id,
                ),
                self.context)
            if stop_operation:
                update_operation_fields(self.db, stop_operation, tank_job_service_pb2.StopTankJobMetadata, test_service_pb2.StopTestMetadata)
            return stop_operation

    def Stop(self, request, context):
        return self._Stop(self.logger).handle(request, context)

    class _Delete(handler.BasePrivateHandler):
        _handler_name = 'Delete'

        def __init__(self, parent_logger):
            super().__init__(parent_logger)
            self._parent_logger = parent_logger

        def proceed(self):
            delete_operation = job_service.JobServicer._Delete(self._parent_logger).handle(
                tank_job_service_pb2.DeleteTankJobRequest(
                    id=self.request.test_id,
                ),
                self.context)
            if delete_operation:
                update_operation_fields(self.db, delete_operation, tank_job_service_pb2.DeleteTankJobMetadata, test_service_pb2.DeleteTestMetadata)
            return delete_operation

    def Delete(self, request, context):
        return self._Delete(self.logger).handle(request, context)

    class _ValidateConfig(handler.BasePrivateHandler):
        _handler_name = 'ValidateConfig'

        def __init__(self, parent_logger):
            super().__init__(parent_logger)
            self._parent_logger = parent_logger

        def proceed(self):
            response = job_service.JobServicer._ValidateConfig(self._parent_logger).handle(
                tank_job_service_pb2.ValidateConfigRequest(
                    config=self.request.config,
                    folder_id=self.request.folder_id
                ),
                self.context)

            return test_service_pb2.ValidateConfigResponse(
                status=test_service_pb2.ValidateConfigResponse.Status.Name(response.status),
                errors=response.errors
            )

    def ValidateConfig(self, request, context):
        return self._ValidateConfig(self.logger).handle(request, context)

    class _GetCreateForm(handler.BasePrivateHandler):
        _handler_name = 'GetCreateForm'

        def proceed(self):
            raise NotImplementedError('CLOUDLOAD-317')

    def GetCreateForm(self, request, context):
        return self._GetCreateForm(self.logger).handle(request, context)

    class _GetConfig(handler.BasePrivateHandler):
        _handler_name = 'GetConfig'

        def proceed(self):
            raise NotImplementedError('CLOUDLOAD-317')

    def GetConfig(self, request, context):
        return self._GetConfig(self.logger).handle(request, context)

    class _UploadConfig(handler.BasePrivateHandler):
        _handler_name = 'UploadConfig'

        def proceed(self):
            raise NotImplementedError('CLOUDLOAD-317')

    def UploadConfig(self, request, context):
        return self._UploadConfig(self.logger).handle(request, context)

    class _GetGenerators(handler.BasePrivateHandler):
        _handler_name = 'GetGenerators'

        def proceed(self):
            raise NotImplementedError('CLOUDLOAD-317')

    def GetGenerators(self, request, context):
        return self._GetGenerators(self.logger).handle(request, context)

    class _GetReport(handler.BasePrivateHandler):
        _handler_name = 'GetReport'

        def proceed(self):
            raise NotImplementedError('CLOUDLOAD-317')

    def GetReport(self, request, context):
        return self._GetReport(self.logger).handle(request, context)

    class _GetChart(handler.BasePrivateHandler):
        _handler_name = 'GetChart'

        def proceed(self):
            raise NotImplementedError('CLOUDLOAD-317')

    def GetChart(self, request, context):
        return self._GetChart(self.logger).handle(request, context)

    class _Compare(handler.BasePrivateHandler):
        _handler_name = 'Compare'

        def proceed(self):
            authorize(self.context, self.user_token, self.request.folder_id, permissions.TESTS_GET, self.request_id)
            tests = self.db.job.filter(
                JobTable.folder_id == self.request.folder_id,
                JobTable.id.in_(self.request.test_ids)
            )
            if not tests:
                raise self.context.abort(
                    grpc.StatusCode.NOT_FOUND,
                    'No tests found.',
                )

            tests_data = get_tests_comparison(
                tests, test_pb2.ChartType.Name(self.request.chart_type), self.request.metrics_names, self.lang,
            )
            return tests_data

    def Compare(self, request, context):
        return self._Compare(self.logger).handle(request, context)


def test_from_tank_job(tank_job: tank_job_pb2.TankJob) -> test_pb2.Test:
    generator = test_pb2.Test.Generator.Name(tank_job.generator)
    status = test_pb2.Test.Status.Name(tank_job.status)
    autostops = []
    for autostop in tank_job.autostops:
        autostops.append(test_pb2.Autostop(
            autostop_criteria=autostop.autostop_criteria,
            autostop_case=autostop.autostop_case,
            autostop_type=test_pb2.Autostop.AutostopType.Name(autostop.autostop_type)
        ))
    return test_pb2.Test(
        id=tank_job.id,
        folder_id=tank_job.folder_id,
        name=tank_job.name,
        description=tank_job.description,
        labels=tank_job.labels,
        created_at=tank_job.created_at,
        started_at=tank_job.started_at,
        finished_at=tank_job.finished_at,
        updated_at=tank_job.updated_at,
        generator=generator,
        agent_instance_id=tank_job.tank_instance_id,
        target_address=tank_job.target_address,
        target_port=tank_job.target_port,
        target_version=tank_job.target_version,
        config=tank_job.config,
        payload_urls=tank_job.ammo_urls,
        payload_id=tank_job.ammo_id,
        cases=tank_job.cases,
        status=status,
        errors=tank_job.errors,
        favorite=tank_job.favorite,
        imbalance_point=tank_job.imbalance_point,
        imbalance_ts=tank_job.imbalance_ts,
        imbalance_at=tank_job.imbalance_at,
        autostops=autostops
    )


def update_operation_fields(db, operation, old_metadata_message, new_metadata_message):
    # change metadata
    assert operation is not None
    metadata = old_metadata_message()
    operation.metadata.Unpack(metadata)
    test_id = metadata.id
    new_metadata = Any()
    new_metadata.Pack(new_metadata_message(test_id=test_id))
    operation.metadata.CopyFrom(new_metadata)

    db_operation = db.operation.get(operation.id)
    if db_operation.done_resource_snapshot:
        # change snapshot
        db_job = db.job.get(test_id)
        test_message = test_from_tank_job(job_service.create_message(db_job))
        db_operation.done_resource_snapshot = MessageToJson(test_message)
        db.operation.add(db_operation)

        # change response
        response = Any()
        response.Pack(test_message)
        operation.response.CopyFrom(response)
    return operation
