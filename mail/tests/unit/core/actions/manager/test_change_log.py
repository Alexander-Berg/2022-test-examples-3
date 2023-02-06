from datetime import datetime, timedelta
from random import randint

import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.actions.manager.change_log import GetChangeLogListManagerAction


class TestGetChangeLogListManagerAction:
    @pytest.fixture
    def params(self):
        return {}

    @pytest.fixture
    def returned_func(self, manager_assessor, params):
        async def _inner():
            return await GetChangeLogListManagerAction(
                manager_uid=manager_assessor.uid,
                **params,
            ).run()
        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    def test_returned(self, change_logs, returned):
        assert returned == sorted(change_logs, key=lambda cl: cl.changed_at, reverse=True)

    @pytest.mark.parametrize('params', ({
        'merchant_uid': randint(1, 10 ** 9),
        'changed_at_from': datetime.now(),
        'changed_at_to': datetime.now() + timedelta(hours=1),
        'limit': randint(1, 10 ** 9),
        'offset': randint(1, 10 ** 9),
    },))
    @pytest.mark.asyncio
    async def test_find_call(self, mocker, params, returned_func):
        from mail.payments.payments.storage.mappers.change_log import ChangeLogMapper
        find = mocker.spy(ChangeLogMapper, 'find')
        await returned_func()
        find_kwargs = find.call_args[1]
        assert_that(
            find_kwargs,
            has_entries({
                'uid': params['merchant_uid'],
                'changed_at_from': params['changed_at_from'],
                'changed_at_to': params['changed_at_to'],
                'limit': params['limit'],
                'offset': params['offset'],
            }),
        )
