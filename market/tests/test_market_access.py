from concurrent import futures
from market.pylibrary.market_access import AccessClient
from yatest.common.network import PortManager

import grpc
import market.access.server.proto.consumer_pb2 as consumer_pb2
import market.access.server.proto.service_pb2_grpc as service_pb2_grpc
import market.access.server.proto.resource_pb2 as resource_pb2
import pytest


@pytest.fixture
def market_access_service():
    class Service(service_pb2_grpc.IAccessServiceServicer):
        def __init__(self, *args, **kwargs):
            super(Service, self).__init__(*args, **kwargs)
            self.grpc_port = None
            self._consumers = dict()
            self._versions = []

        def inject_consumer(self, consumer):
            self._consumers[consumer.name] = consumer

        def GetConsumer(self, request, context):
            consumer = self._consumers.get(request.name)
            return consumer or consumer_pb2.TConsumer()

        def UpdateConsumer(self, request, context):
            self._consumers[request.name] = request.consumer
            return request.consumer

        def CreateVersion(self, request, context):
            self._versions.append(request.body)
            return self._versions[-1]

    return Service()


@pytest.yield_fixture
def market_access_server(market_access_service):
    market_access_service.grpc_port = PortManager().get_port()
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=1))
    service_pb2_grpc.add_IAccessServiceServicer_to_server(market_access_service, server)
    server.add_insecure_port('localhost:{}'.format(market_access_service.grpc_port))
    server.start()
    yield server
    server.stop(None)


@pytest.fixture
def market_access_client(market_access_service, market_access_server):
    return AccessClient('localhost', market_access_service.grpc_port)


def test_get_consumer(market_access_service, market_access_client):
    CONSUMER_NAME = 'some_consumer'
    expected = consumer_pb2.TConsumer(name=CONSUMER_NAME)
    market_access_service.inject_consumer(expected)
    result = market_access_client.get_consumer(CONSUMER_NAME)
    assert expected == result


def test_update_consumer(market_access_client):
    CONSUMER_NAME = 'other_consumer'
    assert market_access_client.get_consumer(CONSUMER_NAME).name != CONSUMER_NAME
    market_access_client.update_consumer(consumer_pb2.TConsumer(name=CONSUMER_NAME))
    assert market_access_client.get_consumer(CONSUMER_NAME).name == CONSUMER_NAME


def test_create_version(market_access_client):
    version = resource_pb2.TVersion(resource_name='my_cool_resource')
    response = market_access_client.create_version('my_cool_resource', version)
    assert response == version
