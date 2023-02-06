import pytest

from mail.beagle.beagle.core.exceptions import MailListNotFoundError


class BaseTestNotFound:
    @pytest.fixture
    def returned_func(self, params, action, mocker):
        async def _inner():
            return await action(**params).run()

        return _inner

    @pytest.fixture
    async def returned(self, params, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    async def test_mail_list_not_found(self, params, action, randn):
        params.update({'mail_list_id': randn()})
        with pytest.raises(MailListNotFoundError):
            await action(**params).run()
