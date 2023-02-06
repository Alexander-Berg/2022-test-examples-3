from concurrent import futures
import sys

import grpc
import yatest.common


class GrpcMockServer:
    def __init__(self, servicer_class):
        self.host = "[::1]"
        self.port = None
        self.port_manager = yatest.common.network.PortManager()

        self.server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
        base_servicer_class = servicer_class.base_servicer_class
        add_service_func = getattr(sys.modules[base_servicer_class.__module__], "add_{}_to_server".format(base_servicer_class.__name__))
        add_service_func(servicer_class(), self.server)

    def __enter__(self):
        self.port = self.port_manager.get_port()
        self.server.add_insecure_port("{}:{}".format(self.host, self.port))

        self.server.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.server.stop(1)
