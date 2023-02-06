import pytest

from mail.payments.payments.core.entities.enums import AcquirerType
from mail.payments.payments.core.exceptions import (
    CoreActionDenyError, OAuthAbsentError, TinkoffInvalidSubmerchantIdError
)
from mail.payments.payments.tests.base import BaseTestParent


class BaseTestRequiresModeration(BaseTestParent):
    """
    Inherit this to add require_moderation check.
    """

    @pytest.fixture
    def action_deny_exception(self):
        return CoreActionDenyError

    @pytest.fixture
    def moderations_data(self):
        return [{'approved': True}]

    @pytest.fixture(autouse=True)
    def setup_moderations(self, moderations):
        pass

    @pytest.fixture
    def returned_func(self):
        raise NotImplementedError

    class TestActionDeny:
        @pytest.fixture(params=(
            pytest.param(
                [],
                id='no_moderations',
            ),
            pytest.param(
                [{'approved': False}],
                id='disapproved',
            ),
            pytest.param(
                [{'approved': True}, {'approved': False}],
                id='disapproved_after_approve',
            ),
            pytest.param(
                [{'approved': False}, {'approved': True, 'ignore': True}],
                id='disapproved_with_ignored_approve',
            ),
        ))
        def moderation_items(self, request):
            return request.param

        @pytest.fixture
        def moderations_data(self, moderation_items):
            return moderation_items

        @pytest.mark.asyncio
        async def test_raises_error(self, action_deny_exception, moderations, returned_func):
            with pytest.raises(action_deny_exception):
                await returned_func()


class BaseTestRequiresNoModeration:
    """
    Inherit this to add require_no_moderation check.
    """

    @pytest.fixture
    def action_deny_exception(self):
        return CoreActionDenyError

    @pytest.fixture
    def moderation_data(self):
        return []

    @pytest.fixture(autouse=True)
    def setup_moderations(self, moderations):
        pass

    @pytest.fixture
    def returned_func(self):
        raise NotImplementedError

    class TestActionDeny:
        @pytest.fixture(params=(
            pytest.param(
                [{'approved': True}],
                id='approved',
            ),
            pytest.param(
                [{'approved': False}, {'approved': True}],
                id='approved_after_disapprove',
            ),
            pytest.param(
                [{'approved': True}, {'approved': False, 'ignore': True}],
                id='approved_with_ignored_disapprove',
            )
        ))
        def moderations_data(self, request):
            return request.param

        @pytest.mark.asyncio
        async def test_raises_error(self, action_deny_exception, returned_func):
            with pytest.raises(action_deny_exception):
                await returned_func()


class BaseTrustCredentialsErrorTest:
    @pytest.mark.parametrize('acquirer', (AcquirerType.TINKOFF,))
    @pytest.mark.asyncio
    async def test_error_check_trust_credentials_tinkoff(self, storage, merchant, returned_func):
        for item in (merchant, merchant.parent):
            if item:
                item.submerchant_id = None
                await storage.merchant.save(item)

        with pytest.raises(TinkoffInvalidSubmerchantIdError):
            await returned_func()

    @pytest.mark.parametrize('acquirer', (AcquirerType.KASSA,))
    @pytest.mark.asyncio
    async def test_error_check_trust_credentials_kassa(self, storage, merchant, returned_func):
        for oauth in merchant.oauth:
            await storage.merchant_oauth.delete(oauth)
        merchant.oauth = []

        with pytest.raises(OAuthAbsentError):
            await returned_func()
