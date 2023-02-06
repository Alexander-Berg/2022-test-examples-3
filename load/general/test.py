import grpc
from datetime import datetime

from google.protobuf.any_pb2 import Any
import yandex.cloud.priv.loadtesting.agent.v1.test_service_pb2 as test_public
import yandex.cloud.priv.loadtesting.agent.v1.test_service_pb2_grpc as test_public_grpc
from yandex.cloud.priv.loadtesting.v1 import tank_job_service_pb2, tank_job_pb2

from load.projects.cloud.loadtesting.logan import lookup_logger
from load.projects.cloud.loadtesting.server.api.common import permissions, handler
from load.projects.cloud.loadtesting.server.api.private_v1 import job as job_service
from load.projects.cloud.loadtesting.server.api.common.utils import authorize


class TestServicer(test_public_grpc.TestServiceServicer):
    def __init__(self):
        self.logger = lookup_logger('TestPublic')

    def Create(self, request: test_public.CreateTestRequest, context: grpc.ServicerContext):
        return self._Create(self.logger).handle(request, context)

    class _Create(handler.BasePublicHandler):
        _handler_name = 'Create'

        def __init__(self, parent_logger):
            super().__init__(parent_logger)
            self._parent_logger = parent_logger

        def proceed(self):
            agent = self.db.tank.get(self.request.agent_instance_id)
            authorize(self.context, self.user_token, agent.folder_id, permissions.TESTS_RUN, self.request_id)

            create_operation = job_service.JobServicer._Create(self._parent_logger).handle(self._create_old_message(self.request, agent), self.context)

            metadata = tank_job_service_pb2.CreateTankJobMetadata()
            create_operation.metadata.Unpack(metadata)
            job_id = metadata.id
            create_metadata = test_public.CreateTestMetadata(test_id=job_id)
            metadata = Any()
            metadata.Pack(create_metadata)
            create_operation.metadata.CopyFrom(metadata)

            tank = self.db.tank.get(self.request.agent_instance_id)
            tank.current_job = job_id

            job = self.db.job.get(job_id)
            job.started_at = datetime.utcnow()  # TODO add new field in request or take from metadata

            return create_operation

        @staticmethod
        def _create_old_message(request: test_public.CreateTestRequest, agent) -> tank_job_service_pb2.CreateTankJobRequest:
            load_type = tank_job_pb2.LoadType.Name(request.load_schedule.load_type)
            load_schedule = []
            for load_schedule_item in request.load_schedule.load_schedule:
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
            schedule = tank_job_pb2.Schedule(load_type=load_type, load_schedule=load_schedule, load_profile=request.load_schedule.load_profile)
            return tank_job_service_pb2.CreateTankJobRequest(
                folder_id=agent.folder_id,
                name=request.name,
                description=request.description,
                labels=request.labels,
                generator=request.generator,
                tank_instance_id=request.agent_instance_id,
                target_address=request.target_address,
                target_port=request.target_port,
                target_version=request.target_version,
                instances=request.instances,
                load_schedule=schedule,
                config=request.config,
                ammo_id=request.ammo_id,
                ammo_urls=request.ammo_urls,
                ammo_headers=request.ammo_headers,
                ammo_type=request.ammo_type,
                ssl=request.ssl
            )

    def Update(self, request: test_public.UpdateTestRequest, context: grpc.ServicerContext):
        return self._Update(self.logger).handle(request, context)

    class _Update(handler.BasePublicHandler):
        _handler_name = 'Update'

        def __init__(self, parent_logger):
            super().__init__(parent_logger)
            self._parent_logger = parent_logger

        def proceed(self):
            job = self.db.job.get(self.request.test_id)
            if not job:
                self.context.abort(grpc.StatusCode.NOT_FOUND, f'Job {self.request.test_id} is not found.')

            old_message = tank_job_service_pb2.UpdateTankJobRequest(
                id=self.request.test_id,
                name=self.request.name,
                description=self.request.description,
                favorite=self.request.favorite,
                target_version=self.request.target_version,
                imbalance_ts=self.request.imbalance_ts,
                imbalance_point=self.request.imbalance_point
            )

            update_operation = job_service.JobServicer._Update(self._parent_logger).handle(old_message, self.context)

            metadata = tank_job_service_pb2.CreateTankJobMetadata()
            update_operation.metadata.Unpack(metadata)
            job_id = metadata.id
            update_metadata = test_public.UpdateTestMetadata(test_id=job_id)
            metadata = Any()
            metadata.Pack(update_metadata)
            update_operation.metadata.CopyFrom(metadata)

            return update_operation
