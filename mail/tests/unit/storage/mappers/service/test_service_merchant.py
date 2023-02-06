from datetime import datetime

import psycopg2
import pytest
from dateutil.tz import UTC

from sendr_utils import alist

from hamcrest import assert_that, equal_to, greater_than, greater_than_or_equal_to, has_properties, instance_of, is_not

from mail.payments.payments.core.entities.service import ServiceMerchant
from mail.payments.payments.storage.exceptions import ServiceMerchantNotFound


@pytest.fixture
def service_merchant_unsaved(merchant, service):
    sm = ServiceMerchant(uid=merchant.uid,
                         service_id=service.service_id,
                         entity_id='id436',
                         description='Annotated association between merchant and service')
    return sm


@pytest.mark.asyncio
async def test_mapper_create(service_merchant_unsaved, storage):
    created = await storage.service_merchant.create(service_merchant_unsaved)
    assert_that(created, has_properties({
        'uid': service_merchant_unsaved.uid,
        'service_id': service_merchant_unsaved.service_id,
        'entity_id': service_merchant_unsaved.entity_id,
        'description': service_merchant_unsaved.description,
        'service_merchant_id': instance_of(int),  # assigned id
        'updated': instance_of(datetime),
        'created': instance_of(datetime),
        'deleted': False,
        'revision': instance_of(int),
    }))


@pytest.mark.asyncio
async def test_mapper_get_or_create_creates(service_merchant_unsaved, storage):
    entity_created, created = await storage.service_merchant.get_or_create(
        service_merchant_unsaved,
        lookup_fields=('uid', 'service_id', 'entity_id')
    )
    assert created
    assert_that(entity_created, has_properties({
        'uid': service_merchant_unsaved.uid,
        'service_id': service_merchant_unsaved.service_id,
        'entity_id': service_merchant_unsaved.entity_id,
        'description': service_merchant_unsaved.description,
        'service_merchant_id': instance_of(int),  # assigned id
        'updated': instance_of(datetime),
        'created': instance_of(datetime),
        'deleted': False,
        'revision': instance_of(int),
    }))


@pytest.mark.asyncio
async def test_mapper_get_or_create_gets(service_merchant_unsaved, storage):
    entity_created, _ = await storage.service_merchant.get_or_create(
        service_merchant_unsaved,
        lookup_fields=('uid', 'service_id', 'entity_id')
    )
    fetched, created = await storage.service_merchant.get_or_create(
        service_merchant_unsaved,
        lookup_fields=('uid', 'service_id', 'entity_id')
    )
    assert entity_created == fetched and not created


class TestServiceMerchantMapperRest:
    @pytest.fixture(params=(True, False))
    def iterator(self, request):
        return request.param

    @pytest.fixture
    async def created(self, service_merchant_unsaved, storage, service):
        created = await storage.service_merchant.create(service_merchant_unsaved)
        created.service = service
        return created

    @pytest.fixture
    async def deleted(self, merchant, service, storage):
        sm = ServiceMerchant(uid=merchant.uid,
                             service_id=service.service_id,
                             description='Deleted service merchant association',
                             entity_id='some entity id for deleted association')
        service_merchant = await storage.service_merchant.create(sm)
        deleted = await storage.service_merchant.delete(service_merchant)
        deleted.service = service
        return deleted

    @pytest.mark.parametrize('for_update', [True, False])
    @pytest.mark.parametrize('fetch_service', [True, False])
    @pytest.mark.asyncio
    async def test_get(self, created, storage, for_update, fetch_service, service):
        extracted = await storage.service_merchant.get(created.service_merchant_id,
                                                       for_update=for_update,
                                                       fetch_service=fetch_service)
        if not fetch_service:
            created.service = None

        assert extracted == created

    @pytest.mark.asyncio
    async def test_get_with_uid(self, created, storage):
        extracted = await storage.service_merchant.get(created.service_merchant_id, uid=created.uid)
        assert extracted == created

    @pytest.mark.asyncio
    async def test_get_by_service_id(self, created, storage):
        extracted = await storage.service_merchant.get(created.service_merchant_id, created.service_id)
        assert extracted == created

    @pytest.mark.asyncio
    async def test_get_with_service(self, created, service, storage):
        extracted = await storage.service_merchant.get(created.service_merchant_id,
                                                       fetch_service=True)
        assert extracted.service == service

    @pytest.mark.asyncio
    async def test_get_service_uid_extra(self, storage, created):
        extracted = await storage.service_merchant.get(service_id=created.service_id,
                                                       uid=created.uid,
                                                       entity_id=created.entity_id,
                                                       fetch_service=True)
        assert extracted == created

    @pytest.mark.asyncio
    async def test_get_by_default_ignores_deleted(self, deleted, storage):
        with pytest.raises(ServiceMerchantNotFound):
            await storage.service_merchant.get(deleted.service_merchant_id)

    @pytest.mark.asyncio
    async def test_get_can_see_deleted(self, deleted, storage):
        entity = await storage.service_merchant.get(deleted.service_merchant_id, ignore_deleted=False)
        assert entity == deleted

    @pytest.mark.asyncio
    async def test_get_raises(self, created, storage):
        with pytest.raises(ServiceMerchantNotFound):
            await storage.service_merchant.get(10 * created.service_merchant_id)

    @pytest.mark.asyncio
    async def test_get_invalid_unique_first(self, created, storage):
        with pytest.raises(RuntimeError):
            await storage.service_merchant.get(None)

    @pytest.mark.parametrize('none_index', (0, 1, 2))
    @pytest.mark.asyncio
    async def test_get_invalid_unique_second(self, created, storage, none_index):
        kwargs_values = [1, 2, '']
        kwargs_values[none_index] = None
        service_id, uid, entity_id = kwargs_values

        with pytest.raises(RuntimeError):
            await storage.service_merchant.get(service_id=service_id, uid=uid, entity_id=entity_id)

    @pytest.mark.asyncio
    async def test_get_raises_if_not_found_by_uid(self, created, storage):
        with pytest.raises(ServiceMerchantNotFound):
            await storage.service_merchant.get(created.service_merchant_id, uid=10 * created.uid)

    @pytest.mark.asyncio
    async def test_find(self, service_merchant, storage, iterator):
        result = await alist(storage.service_merchant.find(service_merchant.uid,
                                                           service_merchant.service_id,
                                                           service_merchant.entity_id,
                                                           iterator=iterator))
        assert result == [service_merchant]

    @pytest.mark.asyncio
    async def test_find_with_service(self, service, service_merchant, storage, iterator):
        result = await alist(storage.service_merchant.find(service_merchant.uid,
                                                           service_merchant.service_id,
                                                           service_merchant.entity_id,
                                                           with_service=True,
                                                           iterator=iterator))
        assert result == [service_merchant] and result[0].service == service

    @pytest.mark.asyncio
    async def test_find_limit(self, service_merchant, storage):
        result = await alist(storage.service_merchant.find(limit=0))
        assert result == []

    @pytest.mark.asyncio
    async def test_find_not_found(self, storage, iterator):
        result = await alist(storage.service_merchant.find(999999, 11111, 'ddd', iterator=iterator))
        assert result == []

    @pytest.mark.asyncio
    async def test_create_ignores_specific_fields(self, created, storage):
        _updated = datetime(year=1999, month=1, day=1, tzinfo=UTC)
        _created = datetime(year=1999, month=1, day=1, tzinfo=UTC)
        _deleted = True
        created.updated = _updated
        created.created = _created
        created.deleted = _deleted
        created.entity_id = f'Prefixed {created.entity_id}'
        another = await storage.service_merchant.create(created)
        assert_that(another, has_properties({
            'deleted': not _deleted,
            'updated': is_not(equal_to(_updated)),
            'created': is_not(equal_to(_created)),
            'service_merchant_id': is_not(equal_to(created.service_merchant_id))
        }))

    @pytest.mark.asyncio
    async def test_create_unique_constraint(self, storage, created):
        entity = ServiceMerchant(uid=created.uid,
                                 service_id=created.service_id,
                                 entity_id=created.entity_id,
                                 description='')
        with pytest.raises(psycopg2.IntegrityError):
            await storage.service_merchant.create(entity)

    @pytest.mark.asyncio
    async def test_save(self, created, storage):
        new_description = f'Prefixed {created.description}'
        created.description = new_description
        old_revision = created.revision
        updated = await storage.service_merchant.save(created)
        assert_that(updated, has_properties({
            'service_merchant_id': created.service_merchant_id,
            'description': new_description,
            'updated': greater_than_or_equal_to(created.updated),
            'created': created.created,
            'revision': greater_than(old_revision),
        }))

    @pytest.mark.asyncio
    async def test_save_ignores_specific_fields(self, storage, created):
        _updated = datetime(year=1999, month=1, day=1, tzinfo=UTC)
        _created = datetime(year=1999, month=1, day=1, tzinfo=UTC)
        _deleted = True
        created.updated = _updated
        created.created = _created
        created.deleted = _deleted
        updated = await storage.service_merchant.save(created)
        assert_that(updated, has_properties({
            'deleted': False,
            'updated': is_not(equal_to(_updated)),
            'created': is_not(equal_to(_created)),
        }))

    @pytest.mark.asyncio
    async def test_delete(self, storage, created):
        """Удаление сущности есть установка флага deleted."""
        await storage.service_merchant.delete(created)
        extracted = await storage.service_merchant.get(service_merchant_id=created.service_merchant_id,
                                                       ignore_deleted=False)
        assert_that(extracted, has_properties({
            'deleted': True,
            'service_merchant_id': created.service_merchant_id,
            'updated': greater_than_or_equal_to(created.updated),
            'revision': greater_than_or_equal_to(created.revision),
        }))

    @pytest.mark.asyncio
    async def test_can_create_the_same_entity_after_deletion(self, storage, created):
        """Ограничение уникальности не затрагивает "удаленные" записи."""
        await storage.service_merchant.delete(created)
        await storage.service_merchant.create(created)
