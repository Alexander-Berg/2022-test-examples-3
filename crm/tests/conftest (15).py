import pytest

from crm.agency_cabinet.common.testing import BaseTestClient

pytest_plugins = [
    'smb.common.rmq.rpc.pytest.plugin',
    'crm.agency_cabinet.common.server.common.pytest.plugin',
    'crm.agency_cabinet.common.service_discovery.pytest.plugin'
]


def pytest_collection_modifyitems(items):
    for item in items:
        item.add_marker(pytest.mark.asyncio)


class AuthTestClient(BaseTestClient):
    def __init__(self, service_ticket, *args, **kwargs):
        super().__init__(*args, **kwargs)

        self.service_ticket = service_ticket

    async def request(self, method, path, **kwargs):
        service_ticket = kwargs.pop('service_ticket', self.service_ticket)

        kwargs['auth_headers'] = {
            'X-Ya-Service-Ticket': service_ticket,
        }

        return await super().request(method, path, **kwargs)
