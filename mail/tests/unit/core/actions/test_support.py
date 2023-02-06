import pytest

from mail.beagle.beagle.core.actions.support import SupportAction
from mail.beagle.beagle.core.exceptions import MailListNotFoundError


class TestSupportAction:
    @pytest.fixture
    def returned_func(self, list_uid):
        async def _inner():
            return await SupportAction(list_uid=list_uid).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    class TestSuccess:
        @pytest.fixture
        def list_uid(self, smtp_cache):
            return smtp_cache.uid

        async def test_support(self, list_uid, smtp_cache, returned):
            expected_uids = {subscr.uid for subscr in smtp_cache.value.subscriptions}
            returned_uids = {subscr['uid'] for subscr in returned}
            assert expected_uids == returned_uids

    @pytest.mark.asyncio
    class TestNotFound:
        @pytest.fixture
        def list_uid(self, randn):
            return randn()

        async def test_not_found(self, returned_func):
            with pytest.raises(MailListNotFoundError):
                await returned_func()
