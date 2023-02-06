from dataclasses import asdict
from datetime import timedelta

import pytest

from sendr_utils import alist, utcnow

from mail.payments.payments.core.entities.enums import ShopType
from mail.payments.payments.core.entities.merchant_oauth import MerchantOAuth
from mail.payments.payments.storage.mappers.merchant_oauth import MerchantOAuthDataDumper, MerchantOAuthDataMapper


class TestMerchantOAuthDataMapper:
    def test_map(self, merchant_oauth):
        row = {
            type(merchant_oauth).__name__ + '__' + key: value
            for key, value in asdict(merchant_oauth).items()
        }
        mapped = MerchantOAuthDataMapper()(row)
        assert mapped == merchant_oauth


class TestMerchantOAuthDataDumper:
    def test_unmap(self, merchant_oauth):
        assert MerchantOAuthDataDumper()(merchant_oauth) == asdict(merchant_oauth)


class TestMerchantOAuthMapper:
    @pytest.fixture
    def now(self, mocker):
        now = utcnow()
        mocker.patch('mail.payments.payments.storage.mappers.merchant_oauth.func.now', mocker.Mock(return_value=now))
        return now

    @pytest.mark.asyncio
    async def test_get(self, merchant_oauth, storage):
        from_db = await storage.merchant_oauth.get(merchant_oauth.uid, merchant_oauth.mode)
        assert from_db == merchant_oauth

    @pytest.mark.asyncio
    async def test_get_not_found(self, storage, merchant_oauth):
        with pytest.raises(MerchantOAuth.DoesNotExist):
            await storage.merchant_oauth.get(merchant_oauth.uid + 1, merchant_oauth.mode)

    @pytest.mark.asyncio
    async def test_get_by_shop_id(self, merchant_oauth, storage):
        from_db = await storage.merchant_oauth.get_by_shop_id(merchant_oauth.uid, merchant_oauth.shop_id)
        assert from_db == merchant_oauth

    @pytest.mark.asyncio
    async def test_get_by_shop_id_not_found(self, storage, merchant_oauth):
        with pytest.raises(MerchantOAuth.DoesNotExist):
            await storage.merchant_oauth.get_by_shop_id(merchant_oauth.uid + 1, merchant_oauth.shop_id)

    @pytest.mark.parametrize('field', ('encrypted_access_token', 'encrypted_refresh_token', 'uid'))
    @pytest.mark.asyncio
    async def test_find(self, field, merchant_oauth, storage):
        from_db = await alist(storage.merchant_oauth.find(filters={field: getattr(merchant_oauth, field)}))
        assert from_db == [merchant_oauth]

    @pytest.mark.parametrize('field', ('decrypted_access_token', 'decrypted_refresh_token', 'expires'))
    @pytest.mark.asyncio
    async def test_save(self, field, merchant_oauth, storage, rands, now):
        setattr(merchant_oauth, field, now if field == 'expires' else rands())
        await storage.merchant_oauth.save(merchant_oauth)
        from_db = await storage.merchant_oauth.get(merchant_oauth.uid, merchant_oauth.mode)
        assert from_db == merchant_oauth

    @pytest.mark.asyncio
    async def test_ignore_created_during_save(self, merchant_oauth, storage):
        merchant_oauth.created = utcnow()
        await storage.merchant_oauth.save(merchant_oauth)
        from_db = await storage.merchant_oauth.get(merchant_oauth.uid, merchant_oauth.mode)
        assert from_db.created != merchant_oauth.created

    @pytest.mark.asyncio
    async def test_delete(self, storage, merchant_oauth):
        await storage.merchant_oauth.get(merchant_oauth.uid, merchant_oauth.mode)
        await storage.merchant_oauth.delete(merchant_oauth)

        with pytest.raises(MerchantOAuth.DoesNotExist):
            await storage.merchant_oauth.get(merchant_oauth.uid, merchant_oauth.mode)

    class BaseTestRefresh:
        @pytest.fixture(params=(
            pytest.param(True, id='poll'),
            pytest.param(False, id='no_poll'),
        ))
        def poll(self, request):
            return request.param

        @pytest.fixture(params=(
            pytest.param(True, id='safe'),
            pytest.param(False, id='no_safe'),
        ))
        def safe(self, request):
            return request.param

        @pytest.fixture(params=(-1, 1))
        def shift_expire(self, request):
            return request.param

        @pytest.fixture(params=(-1, 1))
        def shift_updated(self, request):
            return request.param

        @pytest.fixture
        def is_empty(self, safe, poll, shift_expire, shift_updated):
            if not poll:
                return True
            elif safe:
                return not (shift_updated < 0 < shift_expire)
            else:
                return shift_expire < 0

        @pytest.fixture
        def merchant_oauth_entity(self, merchant, default_merchant_shops, poll, rands):
            merchant_oauth = MerchantOAuth(uid=merchant.uid,
                                           shop_id=default_merchant_shops[ShopType.PROD].shop_id,
                                           poll=poll,
                                           expires=utcnow() + timedelta(days=1))
            merchant_oauth.decrypted_access_token = rands()
            merchant_oauth.decrypted_refresh_token = rands()
            return merchant_oauth

        @pytest.fixture(autouse=True)
        async def setup(self, payments_settings, poll, storage, merchant_oauth, shift_expire, shift_updated):
            updated_delta = (utcnow() - merchant_oauth.updated).total_seconds()
            payments_settings.MERCHANT_OAUTH_REFRESH_THRESHOLD = merchant_oauth.expires_in + shift_expire
            payments_settings.MERCHANT_OAUTH_REFRESH_UPDATED_THRESHOLD = updated_delta + shift_updated

    class TestGetForRefresh(BaseTestRefresh):
        @pytest.mark.asyncio
        async def test_get_for_refresh(self, safe, is_empty, storage, merchant_oauth):
            if is_empty:
                with pytest.raises(MerchantOAuth.DoesNotExist):
                    await storage.merchant_oauth.get_for_refresh(safe=safe)
            else:
                from_db = await storage.merchant_oauth.get_for_refresh(safe=safe)
                assert from_db == merchant_oauth

    class TestCountForRefresh(BaseTestRefresh):
        @pytest.mark.asyncio
        async def test_count_for_refresh(self, safe, is_empty, storage, merchant_oauth):
            assert await storage.merchant_oauth.count_for_refresh(safe=safe) == (0 if is_empty else 1)

    class TestFindByUID:
        @pytest.mark.asyncio
        async def test_not_found(self, storage, randn):
            assert not await alist(storage.merchant_oauth.find_by_uid(randn()))

        @pytest.mark.asyncio
        async def test_found(self, storage, merchant, merchant_oauth):
            assert await alist(storage.merchant_oauth.find_by_uid(merchant.uid)) == [merchant_oauth]
