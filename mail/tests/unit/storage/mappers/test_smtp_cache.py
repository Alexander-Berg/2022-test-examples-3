import pytest

from mail.beagle.beagle.core.entities.smtp_cache import CacheValue
from mail.beagle.beagle.storage.exceptions import SMTPCacheNotFound
from mail.beagle.beagle.storage.mappers.smtp_cache import SMTPCacheDataDumper, SMTPCacheDataMapper


@pytest.mark.asyncio
class TestSMTPCacheMapper:
    @pytest.fixture
    def func_now(self, mocker, now):
        mocker.patch('mail.beagle.beagle.storage.mappers.smtp_cache.func.now', mocker.Mock(return_value=now))
        return now

    async def test_create_or_update(self, storage, smtp_cache_entity, func_now):
        smtp_cache = await storage.smtp_cache.create_or_update(smtp_cache_entity)
        smtp_cache_entity.created = smtp_cache_entity.updated = func_now
        assert all((
            smtp_cache_entity.org_id == smtp_cache.org_id,
            smtp_cache_entity.uid == smtp_cache.uid,
        ))

    async def test_get(self, storage, smtp_cache):
        assert smtp_cache == await storage.smtp_cache.get(smtp_cache.org_id, smtp_cache.uid)

    async def test_get_by_uid(self, storage, smtp_cache):
        assert smtp_cache == await storage.smtp_cache.get_by_uid(smtp_cache.uid)

    async def test_get_not_found(self, storage, randn):
        with pytest.raises(SMTPCacheNotFound):
            await storage.smtp_cache.get(randn(), randn())

    async def test_delete(self, storage, smtp_cache):
        await storage.smtp_cache.delete(smtp_cache)
        with pytest.raises(SMTPCacheNotFound):
            await storage.smtp_cache.get(smtp_cache.org_id, smtp_cache.uid)

    async def test_deleted_mail_list(self, storage, mail_list, smtp_cache):
        deleted = await storage.mail_list.delete(mail_list)
        with pytest.raises(SMTPCacheNotFound):
            await storage.smtp_cache.get_by_uid(deleted.uid)

    async def test_save(self, storage, smtp_cache, func_now):
        smtp_cache.value = CacheValue(subscriptions=[])
        updated = await storage.smtp_cache.save(smtp_cache)
        smtp_cache.updated = func_now
        assert all((
            smtp_cache == updated,
            smtp_cache == await storage.smtp_cache.get(smtp_cache.org_id, smtp_cache.uid)
        ))


class TestSMTPCacheDataDumper:
    @pytest.fixture
    def subscriptions_dict(self, smtp_cache_value, smtp_cache):
        expected = {
            'subscriptions': [
                {
                    'uid': subscription.uid,
                    'local_part': subscription.local_part,
                    'org_id': subscription.org_id,
                    'params': subscription.params
                } for subscription in smtp_cache_value.subscriptions
            ]
        }
        return expected

    @pytest.fixture
    def smtp_cache_dict(self, smtp_cache_value, smtp_cache, subscriptions_dict):
        expected = {
            'org_id': smtp_cache.org_id,
            'uid': smtp_cache.uid,
            'created': smtp_cache.created,
            'updated': smtp_cache.updated,
            'value': subscriptions_dict
        }
        return expected

    def test_unmap(self, smtp_cache_value, smtp_cache, subscriptions_dict):
        assert subscriptions_dict == SMTPCacheDataDumper()(smtp_cache)['value']

    def test_map(self, smtp_cache_value, smtp_cache, subscriptions_dict, smtp_cache_dict):
        row = {
            type(smtp_cache).__name__ + '__' + key: value
            for key, value in smtp_cache_dict.items()
        }
        mapped = SMTPCacheDataMapper()(row)
        assert mapped.value == smtp_cache.value
