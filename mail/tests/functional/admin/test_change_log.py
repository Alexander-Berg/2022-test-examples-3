from datetime import datetime
from random import choice

import pytest

from mail.payments.payments.core.entities.change_log import ChangeLog, OperationKind

from .base import BaseTestNotAuthorized


class TestGetChangeLogList(BaseTestNotAuthorized):
    @pytest.fixture(autouse=True)
    async def setup(self, unique_rand, randn, storage):
        for _ in range(10):
            await storage.change_log.create(ChangeLog(
                uid=unique_rand(randn, basket='uid'),
                revision=randn(),
                operation=choice(list(OperationKind)),
            ))

    @pytest.fixture
    def tvm_uid(self, manager_assessor):
        return manager_assessor.uid

    @pytest.fixture
    def request_params(self):
        return {}

    @pytest.fixture
    async def response(self, admin_client, request_params, tvm):
        return await admin_client.get('/admin/api/v1/change_log', params=request_params)

    @pytest.mark.parametrize('request_params', (
        {'limit': 5, 'offset': 3},
    ))
    @pytest.mark.asyncio
    async def test_response(self, storage, request_params, response_data):
        change_logs = [cl async for cl in storage.change_log.find(**request_params)]
        for cl in response_data:
            cl['changed_at'] = datetime.fromisoformat(cl['changed_at'])
        assert response_data == [
            {
                'uid': cl.uid,
                'revision': cl.revision,
                'operation': cl.operation.value,
                'changed_at': cl.changed_at,
                'arguments': cl.arguments,
                'info': cl.info,
            }
            for cl in change_logs
        ]
