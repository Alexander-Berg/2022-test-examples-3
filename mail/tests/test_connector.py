import pytest
from aiohttp import TCPConnector, web

from sendr_interactions.base import AbstractInteractionClient
from sendr_interactions.clients.sd import AbstractSDClient
from sendr_interactions.clients.sd.entities import (
    Endpoint, EndpointResolveStatus, EndpointSet, ResolveEndpointsResponse
)
from sendr_interactions.connector import SDAwareDevTCPConnectorMixin, SDAwareTCPConnectorMixin, sd


class ExampleAbstractClient(AbstractInteractionClient):
    BASE_URL = 'http://host.test'
    SERVICE = 'test'
    DEBUG = True
    REQUEST_RETRY_TIMEOUTS = ()


def test_sd_chooses_prod_connector(mocker):
    mocker.patch('sendr_interactions.connector.yenv.type', 'testing')

    connector = sd({})(TCPConnector)

    assert issubclass(connector, SDAwareTCPConnectorMixin)


def test_sd_dev_chooses_dev_connector(mocker):
    mocker.patch('sendr_interactions.connector.yenv.type', 'development')

    connector = sd({})(TCPConnector)

    assert issubclass(connector, SDAwareDevTCPConnectorMixin)


@pytest.fixture
async def echo_server(aiohttp_raw_server):
    async def handler(request):
        return web.json_response({
            'host': request.headers.get('Host'),
            'success': True,
        })
    return await aiohttp_raw_server(handler)


class TestProdConnector:
    @pytest.mark.asyncio
    async def test_successfully_spoofed(self, mocker, dummy_logger, echo_server):
        sd_config = {
            'host.test': {'hostname': 'host.test', 'host': 'localhost', 'port': echo_server.port}
        }
        mocker.patch('sendr_interactions.connector.yenv.type', 'testing')
        connector = sd(sd_config)(TCPConnector)

        class ExampleClient(ExampleAbstractClient):
            CONNECTOR = connector()

            async def echo(self):
                return await self.get('echo', self.endpoint_url('/hello'))

        resp = await ExampleClient(logger=dummy_logger, request_id='example').echo()

        assert resp['host'] == 'host.test'

    @pytest.mark.asyncio
    async def test_host_not_in_sd_config__should_pass_through(self, mocker, dummy_logger, echo_server):
        sd_config = {}
        mocker.patch('sendr_interactions.connector.yenv.type', 'testing')
        connector = sd(sd_config)(TCPConnector)

        class ExampleClient(ExampleAbstractClient):
            BASE_URL = f'http://localhost:{echo_server.port}'
            CONNECTOR = connector()

            async def echo(self):
                return await self.get('echo', self.endpoint_url('/hello'))

        resp = await ExampleClient(logger=dummy_logger, request_id='example').echo()

        assert resp['success']


class TestDevConnector:
    @pytest.fixture
    def sd_config(self, echo_server):
        return {
            'host.test': {
                'hostname': 'host.test',
                'host': 'whatever',
                'port': echo_server.port + 1,  # точно неправильный
                'endpoint_set_id': 'foobar.api'
            }
        }

    @pytest.fixture(autouse=True)
    def mock_yenv(self, mocker):
        mocker.patch('sendr_interactions.connector.yenv.type', 'development')

    @pytest.fixture
    def client(self, sd_config, dummy_logger):
        connector = sd(sd_config)(TCPConnector)

        class ExampleClient(ExampleAbstractClient):
            CONNECTOR = connector()

            async def echo(self):
                return await self.get('echo', self.endpoint_url('/hello'))

        return ExampleClient(logger=dummy_logger, request_id='example')

    @pytest.mark.asyncio
    async def test_success(self, sd_config, mocker, client, echo_server):
        mocker.patch.object(
            AbstractSDClient,
            'resolve_endpoints',
            mocker.AsyncMock(
                return_value=ResolveEndpointsResponse(
                    resolve_status=EndpointResolveStatus.OK,
                    endpoint_set=EndpointSet(
                        endpoints=[
                            Endpoint(ip6_address='localhost', port=echo_server.port + 1),
                            Endpoint(ip6_address='localhost', port=echo_server.port, ready=True),
                        ]
                    )
                )
            )
        )

        resp = await client.echo()

        assert resp['host'] == 'host.test'

    @pytest.mark.asyncio
    async def test_no_ready_hosts__raises_os_error(self, sd_config, mocker, client, echo_server):
        mocker.patch.object(
            AbstractSDClient,
            'resolve_endpoints',
            mocker.AsyncMock(
                return_value=ResolveEndpointsResponse(
                    resolve_status=EndpointResolveStatus.OK,
                    endpoint_set=EndpointSet(
                        endpoints=[
                            Endpoint(ip6_address='localhost', port=echo_server.port + 1),
                            Endpoint(ip6_address='localhost', port=echo_server.port + 2),
                        ]
                    )
                )
            )
        )

        with pytest.raises(OSError):
            await client.echo()

    @pytest.mark.asyncio
    async def test_resolve_status_bad__raises_os_error(self, sd_config, mocker, client, echo_server):
        mocker.patch.object(
            AbstractSDClient,
            'resolve_endpoints',
            mocker.AsyncMock(
                return_value=ResolveEndpointsResponse(
                    resolve_status=EndpointResolveStatus.NOT_EXISTS,
                    endpoint_set=EndpointSet(
                        endpoints=[
                            Endpoint(ip6_address='localhost', port=echo_server.port),
                        ]
                    )
                )
            )
        )

        with pytest.raises(OSError):
            await client.echo()
