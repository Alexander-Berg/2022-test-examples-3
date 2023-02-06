from concurrent import futures
from market.idx.pylibrary.report_control.helpers import MarketAccessHelper
from market.pylibrary.market_access import AccessClient
from yatest.common.network import PortManager

import grpc
import market.access.server.proto.consumer_pb2 as consumer_pb2
import market.access.server.proto.service_pb2_grpc as service_pb2_grpc
import pytest


def _add_consumer(client, consumer_name):
    client.update_consumer(consumer_pb2.TConsumer(name=consumer_name))


@pytest.fixture
def market_access_service():
    class Service(service_pb2_grpc.IAccessServiceServicer):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, **kwargs)
            self.grpc_port = None
            self._consumers = dict()

        def inject_consumer(self, consumer):
            self._consumers[consumer.name] = consumer

        def GetConsumer(self, request, context):
            consumer = self._consumers.get(request.name)
            return consumer or consumer_pb2.TConsumer()

        def UpdateConsumer(self, request, context):
            self._consumers[request.name] = request.consumer
            return request.consumer

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


def test_resources_rollback(market_access_client):
    TS = 1000
    CONSUMER_NAME = 'market_report_market'
    GROUPS = {'report_market@atlantis'}
    ALL_DC = {'man', 'vla'}
    DYNAMICS = {
        MarketAccessHelper.ReportDynamic.MARKET_DYNAMIC,
        MarketAccessHelper.ReportDynamic.LMS_DYNAMIC,
    }
    helper = MarketAccessHelper(market_access_client, ALL_DC)
    _add_consumer(market_access_client, CONSUMER_NAME)

    consumer = market_access_client.get_consumer(CONSUMER_NAME)
    assert len(consumer.options.resource) == 0

    helper.rollback_dynamics_to_moment(TS, GROUPS, ALL_DC, DYNAMICS)
    consumer = market_access_client.get_consumer(CONSUMER_NAME)
    assert len(consumer.options.resource) == len(DYNAMICS)
    assert 'market_dynamic' in consumer.options.resource
    assert 'lms_dynamic' in consumer.options.resource
    assert consumer.options.resource['market_dynamic'].update.to_moment.seconds == TS
    assert consumer.options.resource['lms_dynamic'].update.to_moment.seconds == TS

    helper.clean_up_rollback(GROUPS)
    consumer = market_access_client.get_consumer(CONSUMER_NAME)
    assert consumer.options.resource['market_dynamic'].update.to_moment.seconds == 0
    assert consumer.options.resource['lms_dynamic'].update.to_moment.seconds == 0


def test_tagged_node_resource_rollback(market_access_client):
    TS = [1000, 1500]
    CONSUMER_NAME = 'market_report_market'
    GROUPS = {'report_market@atlantis'}
    ALL_DC = {'man', 'vla'}
    DC_TO_ROLLBACK = {'man'}
    DYNAMICS = {
        MarketAccessHelper.ReportDynamic.MARKET_DYNAMIC,
        MarketAccessHelper.ReportDynamic.LMS_DYNAMIC,
    }
    helper = MarketAccessHelper(market_access_client, ALL_DC)
    _add_consumer(market_access_client, CONSUMER_NAME)

    consumer = market_access_client.get_consumer(CONSUMER_NAME)
    assert len(consumer.options.tagged) == 0

    for ts in TS:
        helper.rollback_dynamics_to_moment(ts, GROUPS, DC_TO_ROLLBACK, DYNAMICS)
        consumer = market_access_client.get_consumer(CONSUMER_NAME)
        assert len(consumer.options.tagged) == len(DC_TO_ROLLBACK)
        assert consumer.options.tagged[0].tag[0] == 'dc:man'

        node = consumer.options.tagged[0].node
        assert node.update.to_moment.seconds == 0
        assert len(node.resource) == len(DYNAMICS)
        assert 'market_dynamic' in node.resource
        assert 'lms_dynamic' in node.resource
        assert node.resource['market_dynamic'].update.to_moment.seconds == ts
        assert node.resource['lms_dynamic'].update.to_moment.seconds == ts

    helper.clean_up_rollback(GROUPS)
    consumer = market_access_client.get_consumer(CONSUMER_NAME)
    node = consumer.options.tagged[0].node
    assert node.resource['market_dynamic'].update.to_moment.seconds == 0
    assert node.resource['lms_dynamic'].update.to_moment.seconds == 0


def test_tagged_node_resource_rollback_multiple_dc(market_access_client):
    TS = 1000
    CONSUMER_NAME = 'market_report_market'
    GROUPS = {'report_market@atlantis'}
    ALL_DC = {'man', 'vla', 'sas'}
    DC_TO_ROLLBACK = {'man', 'vla'}
    DYNAMICS = {
        MarketAccessHelper.ReportDynamic.MARKET_DYNAMIC,
    }
    helper = MarketAccessHelper(market_access_client, ALL_DC)
    _add_consumer(market_access_client, CONSUMER_NAME)

    consumer = market_access_client.get_consumer(CONSUMER_NAME)
    assert len(consumer.options.tagged) == 0

    helper.rollback_dynamics_to_moment(TS, GROUPS, DC_TO_ROLLBACK, DYNAMICS)
    consumer = market_access_client.get_consumer(CONSUMER_NAME)
    assert len(consumer.options.tagged) == len(DC_TO_ROLLBACK)
    assert set(consumer.options.tagged[i].tag[0] for i in range(2)) == {'dc:man', 'dc:vla'}

    for i in range(2):
        node = consumer.options.tagged[i].node
        assert node.update.to_moment.seconds == 0
        assert len(node.resource) == len(DYNAMICS)
        assert 'market_dynamic' in node.resource
        assert node.resource['market_dynamic'].update.to_moment.seconds == TS

    helper.clean_up_rollback(GROUPS)
    consumer = market_access_client.get_consumer(CONSUMER_NAME)
    for i in range(2):
        node = consumer.options.tagged[i].node
        assert node.resource['market_dynamic'].update.to_moment.seconds == 0
