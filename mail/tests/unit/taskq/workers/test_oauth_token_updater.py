from datetime import timedelta

import pytest

from sendr_utils import utcnow

from mail.payments.payments.core.entities.enums import ShopType
from mail.payments.payments.core.entities.merchant_oauth import MerchantOAuth
from mail.payments.payments.core.exceptions import OAuthCodeError, OAuthInvalidGrants
from mail.payments.payments.storage import MerchantOAuthMapper
from mail.payments.payments.taskq.workers.oauth_token_updater import OAuthTokenUpdater


@pytest.fixture
def action_result():
    return None


@pytest.fixture
def action_mock(mock_action, action_result):
    from mail.payments.payments.taskq.workers.oauth_token_updater import OAuthRefreshMerchantAction
    return mock_action(OAuthRefreshMerchantAction, action_result=action_result)


@pytest.fixture
async def oauth_token_updater(app, test_logger):
    oauth_token_updater = OAuthTokenUpdater(logger=test_logger)
    await oauth_token_updater.initialize_worker(app)
    yield oauth_token_updater
    oauth_token_updater.heartbeat_task.cancel()


class TestOAuthTokenUpdater:
    @pytest.fixture(params=(
        pytest.param(True, id='poll'),
        pytest.param(False, id='no_poll'),
    ))
    def poll(self, request):
        return request.param

    @pytest.fixture(params=(-1, 1))
    def shift_expire(self, request):
        return request.param

    @pytest.fixture(params=(-1, 1))
    def shift_updated(self, request):
        return request.param

    @pytest.fixture
    def merchant_oauth_entity(self, merchant, default_merchant_shops, poll, rands):
        merchant_oauth = MerchantOAuth(uid=merchant.uid,
                                       poll=poll,
                                       shop_id=default_merchant_shops[ShopType.PROD].shop_id,
                                       expires=utcnow() + timedelta(days=1))
        merchant_oauth.decrypted_access_token = rands()
        merchant_oauth.decrypted_refresh_token = rands()
        return merchant_oauth

    @pytest.fixture(autouse=True)
    def setup(self, payments_settings, mocker, merchant_oauth, action_mock, shift_expire, shift_updated):
        mocker.spy(MerchantOAuthMapper, 'save')
        updated_delta = (utcnow() - merchant_oauth.updated).total_seconds()
        payments_settings.MERCHANT_OAUTH_REFRESH_THRESHOLD = merchant_oauth.expires_in + shift_expire
        payments_settings.MERCHANT_OAUTH_REFRESH_UPDATED_THRESHOLD = updated_delta + shift_updated

    @pytest.fixture
    def is_empty(self, poll, shift_expire, shift_updated):
        return not poll or not (shift_updated < 0 < shift_expire)

    @pytest.mark.asyncio
    async def test_process_task(self, is_empty, oauth_token_updater):
        assert not is_empty == await oauth_token_updater.process_task()

    class TestProcessTask:
        @pytest.fixture
        def poll(self):
            return True

        @pytest.fixture
        def shift_expire(self):
            return 1

        @pytest.fixture
        def shift_updated(self):
            return -1

        @pytest.mark.asyncio
        async def test_context_storage(self, oauth_token_updater, action_mock):
            await oauth_token_updater.process_task()
            assert action_mock.context.storage is not None

        @pytest.mark.asyncio
        async def test_context(self, oauth_token_updater, action_mock, merchant_oauth):
            await oauth_token_updater.process_task()
            action_mock.assert_called_once_with(merchant_oauth=merchant_oauth)

        @pytest.mark.parametrize('action_result', (OAuthInvalidGrants,))
        @pytest.mark.asyncio
        async def test_disable_polling(self, oauth_token_updater, storage, merchant_oauth):
            await oauth_token_updater.process_task()
            from_db = await storage.merchant_oauth.get_by_shop_id(merchant_oauth.uid, merchant_oauth.shop_id)
            assert from_db.poll is False

        @pytest.mark.parametrize('action_result', (OAuthInvalidGrants, OAuthCodeError))
        @pytest.mark.asyncio
        async def test_update(self, oauth_token_updater, storage, merchant_oauth):
            await oauth_token_updater.process_task()
            MerchantOAuthMapper.save.assert_called_once()
