from random import randint

import pytest


class BaseTestNotAuthorized:
    @pytest.mark.parametrize('tvm_uid', [pytest.param(randint(1, 10 ** 9), id='random-tvm_uid')])
    @pytest.mark.asyncio
    async def test_user_not_authorized(self, response):
        assert response.status == 403
