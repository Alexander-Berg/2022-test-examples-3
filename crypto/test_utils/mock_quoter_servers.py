from concurrent import futures
import grpc
import yatest.common.network

from crypta.cm.services.quoter.grpc import (
    quoter_service_pb2,
    quoter_service_pb2_grpc,
)


class TQuoterServiceServicer(quoter_service_pb2_grpc.TQuoterServiceServicer):
    def __init__(self, is_full):
        self.is_full = is_full

    def GetQuotaState(self, request, context):
        return quoter_service_pb2.TQuotaState(
            IsFull=self.is_full,
            Description=("Quota is full" if self.is_full else "Enough quota") + " for {}".format(request.EnvironmentType)
        )


class MockQuoterServer(object):
    is_enabled = True

    def __init__(self, is_full=False):
        self.is_full = is_full
        self.port_manager = yatest.common.network.PortManager()

    def __enter__(self):
        self._start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self._stop()

    def _start(self):
        self.server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
        quoter_service_pb2_grpc.add_TQuoterServiceServicer_to_server(TQuoterServiceServicer(is_full=self.is_full), self.server)

        self.port = self.port_manager.get_port()
        self.server.add_insecure_port("[::]:{}".format(self.port))
        self.server.start()

    def _stop(self):
        self.port_manager.release()
        self.server.stop(None)

        self.port = None
        self.server = None


class MockQuoterServerDown(object):
    is_enabled = True

    def __init__(self):
        self.port_manager = yatest.common.network.PortManager()

    def __enter__(self):
        self.port = self.port_manager.get_port()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.port_manager.release()


class MockQuoterServerDisabled(object):
    is_enabled = False
