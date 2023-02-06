import re

import pytest
from aiohttp import TCPConnector
from aioresponses import CallbackResult

from sendr_interactions.clients.sd import AbstractSDClient
from sendr_interactions.clients.sd.entities import (
    Endpoint, EndpointResolveStatus, EndpointSet, ResolveEndpointsResponse
)
from sendr_interactions.exceptions import InteractionResponseError


class SDClient(AbstractSDClient):
    BASE_URL = 'http://sd.test'
    DEBUG = False
    REQUEST_RETRY_TIMEOUTS = ()
    CONNECTOR = TCPConnector()


@pytest.fixture
def client(create_interaction_client):
    return create_interaction_client(SDClient)


class TestResolveEndpoints:
    @pytest.mark.asyncio
    async def test_params(self, client, aioresponses_mocker, resp):
        calls = []

        async def callback(url, **kwargs):
            calls.append({'url': url, 'json': kwargs.get('json', {}), 'headers': kwargs.get('headers', {})})
            return CallbackResult(status=200, payload=resp)

        aioresponses_mocker.post(re.compile(r'.*resolve_endpoints/json[\?]?.*'), callback=callback)

        await client.resolve_endpoints(
            cluster_name='sas',
            client_name='test',
            endpoint_set_id='endpointset.id'
        )

        assert len(calls) == 1
        assert calls[0]['json'] == {
            'cluster_name': 'sas',
            'client_name': 'test',
            'endpoint_set_id': 'endpointset.id',
            'ruid': client.request_id,
        }

    @pytest.mark.asyncio
    async def test_result(self, client, aioresponses_mocker, resp):
        async def callback(url, **kwargs):
            return CallbackResult(status=200, payload=resp)

        aioresponses_mocker.post(re.compile(r'.*resolve_endpoints/json[\?]?.*'), callback=callback)

        result = await client.resolve_endpoints(
            cluster_name='sas',
            client_name='test',
            endpoint_set_id='endpointset.id'
        )

        assert result == ResolveEndpointsResponse(
            timestamp=1742853942598963500,
            endpoint_set=EndpointSet(
                endpoint_set_id="yandexpay-production.api",
                endpoints=[
                    Endpoint(
                        id="1lix41des3hy8nod",
                        fqdn="y46ciuflbpvoogtp.sas.yp-c.yandex.net",
                        ip6_address="2a02:6b8:c14:528e:0:501c:302a:0",
                        port=443,
                        ready=True
                    ),
                    Endpoint(
                        id="7x7o29ovsnypi2dg",
                        fqdn="33beqcwuy72e6cvw.sas.yp-c.yandex.net",
                        ip6_address="2a02:6b8:c23:2891:0:501c:41e0:0",
                        port=443,
                        ready=False,
                    ),
                ]
            ),
            resolve_status=EndpointResolveStatus.OK,
            host="sas3-1451-f89-sas-yp-service-d-419-22443.gencfg-c.yandex.net",
            ruid="16231592233663447919454991409015821",
        )

    @pytest.mark.asyncio
    async def test_error(self, client, aioresponses_mocker, resp_err):
        async def callback(url, **kwargs):
            # Не смущайтесь, что текст ошибки не соответствует коду
            return CallbackResult(status=400, payload=resp_err)

        aioresponses_mocker.post(re.compile(r'.*resolve_endpoints/json[\?]?.*'), callback=callback)

        with pytest.raises(InteractionResponseError) as exc_info:
            await client.resolve_endpoints(
                cluster_name='sas',
                client_name='test',
                endpoint_set_id='endpointset.id'
            )

        assert exc_info.value.status_code == 400

    @pytest.fixture
    def resp(self):
        return {
            "timestamp": 1742853942598963500,
            "endpoint_set": {
                "endpoint_set_id": "yandexpay-production.api",
                "endpoints": [
                    {
                        "id": "1lix41des3hy8nod",
                        "fqdn": "y46ciuflbpvoogtp.sas.yp-c.yandex.net",
                        "ip6_address": "2a02:6b8:c14:528e:0:501c:302a:0",
                        "port": 443,
                        "ready": True
                    },
                    {
                        "id": "7x7o29ovsnypi2dg",
                        "fqdn": "33beqcwuy72e6cvw.sas.yp-c.yandex.net",
                        "ip6_address": "2a02:6b8:c23:2891:0:501c:41e0:0",
                        "port": 443,
                    },
                ]
            },
            "resolve_status": 2,
            "watch_token": "0",
            "host": "sas3-1451-f89-sas-yp-service-d-419-22443.gencfg-c.yandex.net",
            "ruid": "16231592233663447919454991409015821"
        }

    @pytest.fixture
    def resp_err(self):
        return {
            'errors': {
                'originalEstimation': 'Months and years vary in length'
            },
            'errorMessages': [
                'Users [vasya, petya, kolya] do not exist'
            ],
            'statusCode': 422,
        }
