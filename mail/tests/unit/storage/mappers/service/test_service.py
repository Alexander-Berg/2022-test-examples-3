from datetime import datetime

import pytest

from sendr_utils import alist

from hamcrest import assert_that, greater_than_or_equal_to, has_properties, instance_of

from mail.payments.payments.core.entities.enums import OrderSource
from mail.payments.payments.core.entities.service import OfferSettings, Service, ServiceOptions
from mail.payments.payments.storage.exceptions import ServiceNotFound
from mail.payments.payments.storage.mappers.service.serialization import ServiceDataDumper, ServiceDataMapper


@pytest.fixture
def service_unsaved():
    return Service(name='Music', slug='the-slug', antifraud=True)


@pytest.fixture
def service_dict(service):
    result = {
        attr: getattr(service, attr)
        for attr in [
            'name',
            'slug',
            'antifraud',
            'hidden',
            'service_id',
            'order_moderation_enabled',
            'created',
            'updated',
        ]
    }
    result['options'] = {
        attr: getattr(service.options, attr)
        for attr in [
            'service_fee',
            'allowed_order_sources',
            'allow_create_service_merchants',
            'allow_payment_mode_recurrent',
            'allow_payment_without_3ds',
            'commission',
            'required_acquirer',
            'hide_commission',
            'can_skip_registration',
            'require_online',
            'icon_url'
        ]
    }
    result['options']['offer_settings'] = {
        attr: getattr(service.options.offer_settings, attr)
        for attr in [
            'slug',
            'pdf_template',
            'data_override'
        ]
    }
    return result


class TestServiceDataMapper:
    @pytest.fixture
    def service_options(self):
        return ServiceOptions(
            can_skip_registration=True,
            require_online=False,
            allowed_order_sources=[OrderSource.UI, OrderSource.SERVICE],
            allow_create_service_merchants=False,
            hide_commission=True,
            offer_settings=OfferSettings(pdf_template='123', slug='abc', data_override={'over': 'ride'}),
            service_fee=1,
            commission=210,
            icon_url='path_to_service_icon_url',
        )

    @pytest.fixture
    def service(self, rands, service_options):
        return Service(name=rands(), options=service_options)

    def test_map(self, loop, service, service_dict):
        row = {
            type(service).__name__ + '__' + key: value
            for key, value in service_dict.items()
        }
        mapped = ServiceDataMapper()(row)
        assert mapped == service


class TestServiceDataDumper:
    def test_unmap(self, loop, service, service_dict):
        assert ServiceDataDumper()(service) == service_dict


class TestServiceMapper:
    @pytest.mark.asyncio
    async def test_service_mapper_create(self, service_unsaved, storage):
        """Сначала проверяем создание сущности. Появляется ID, и временные метки, а иные значимые свойства на месте."""
        service = await storage.service.create(service_unsaved)
        assert_that(service, has_properties({
            'service_id': instance_of(int),
            'name': service_unsaved.name,
            'created': instance_of(datetime),
            'updated': instance_of(datetime),
        }))

    @pytest.fixture
    async def created_service(self, storage, service_unsaved):
        """Введем фикстуру сохраненного в БД сервиса."""
        return await storage.service.create(service_unsaved)

    @pytest.mark.asyncio
    async def test_get(self, storage, created_service):
        """Созданный сервис можно извлечь по его ID - должны получить эквивалентные сущности."""
        service = await storage.service.get(service_id=created_service.service_id)
        assert service == created_service

    @pytest.mark.asyncio
    async def test_get_raises_not_found(self, storage, created_service):
        with pytest.raises(ServiceNotFound):
            await storage.service.get(service_id=10 * created_service.service_id)

    @pytest.mark.asyncio
    async def test_save(self, storage, created_service):
        """Save действительно сохраняет изменения."""
        new_name = f'Prefixed {created_service.name}'

        created_service.name = new_name
        await storage.service.save(created_service)

        service = await storage.service.get(service_id=created_service.service_id)
        assert_that(service, has_properties({
            'service_id': created_service.service_id,
            'name': new_name,
            'updated': greater_than_or_equal_to(service.updated),
        }))

    class TestFind:
        @pytest.mark.asyncio
        async def test_find(self, storage, created_service):
            assert await alist(storage.service.find()) == [created_service]

        @pytest.fixture
        async def services(self, storage, rands, randn):
            num_services = randn(min=10, max=20)
            return [await storage.service.create(Service(name=rands(), hidden=i % 2)) for i in range(num_services)]

        @pytest.mark.asyncio
        async def test_hidden(self, storage, services):
            hidden = await alist(storage.service.find(hidden=False))
            assert all(service.hidden is False for service in hidden)

        @pytest.mark.asyncio
        async def test_service_id_list(self, storage, services):
            service_ids = [s.service_id for s in services]
            services_fetched = await alist(storage.service.find(service_ids=service_ids))
            assert sorted(services, key=lambda s: s.service_id) == sorted(services_fetched, key=lambda s: s.service_id)

    class TestGetByRelated:
        @pytest.fixture
        def all_kwargs(self, service_client, service_merchant):
            return {
                'service_client_id': service_client.service_client_id,
                'service_client_tvm_id': service_client.tvm_id,
                'service_merchant_id': service_merchant.service_merchant_id,
            }

        @pytest.fixture
        def keys(self):
            return ()

        @pytest.fixture
        def kwargs(self, all_kwargs, keys):
            return {key: all_kwargs[key] for key in keys}

        @pytest.mark.parametrize('keys,loads_client,loads_merchant', (
            (('service_client_id', 'service_merchant_id'), True, True),
            (('service_client_tvm_id', 'service_merchant_id'), True, True),
            (('service_client_id',), True, False),
            (('service_client_tvm_id',), True, False),
            (('service_merchant_id',), False, True),
        ))
        @pytest.mark.asyncio
        async def test_loads(self, storage, service, service_client, service_merchant, kwargs, loads_client,
                             loads_merchant):
            if loads_client:
                service.service_client = service_client
            if loads_merchant:
                service.service_merchant = service_merchant
            returned = await storage.service.get_by_related(**kwargs)
            assert returned == service

        @pytest.mark.asyncio
        async def test_deleted_merchant(self, storage, service, service_merchant):
            await storage.service_merchant.delete(service_merchant)
            with pytest.raises(ServiceNotFound):
                await storage.service.get_by_related(service_merchant_id=service_merchant.service_merchant_id)

        @pytest.mark.asyncio
        async def test_not_found_client_id(self, storage, service, service_client, service_merchant):
            with pytest.raises(ServiceNotFound):
                await storage.service.get_by_related(
                    service_client_id=service_client.service_client_id + 1,
                    service_merchant_id=service_merchant.service_merchant_id,
                )

        @pytest.mark.asyncio
        async def test_not_found_client_tvm_id(self, storage, service, service_client, service_merchant):
            with pytest.raises(ServiceNotFound):
                await storage.service.get_by_related(
                    service_client_tvm_id=service_client.tvm_id + 1,
                    service_merchant_id=service_merchant.service_merchant_id,
                )

        @pytest.mark.asyncio
        async def test_not_found_merchant_id(self, storage, service, service_client, service_merchant):
            with pytest.raises(ServiceNotFound):
                await storage.service.get_by_related(
                    service_client_id=service_client.service_client_id,
                    service_merchant_id=service_merchant.service_merchant_id + 1,
                )

    class TestFindByServiceMerchant:
        @pytest.fixture
        def uid(self, service_merchant):
            return service_merchant.uid

        @pytest.fixture(autouse=True)
        async def create_data(self, service, create_service, create_service_merchant):
            second_service = await create_service()
            await create_service_merchant(service_id=second_service.service_id)
            # refer to same service intentionally to check duplicates
            await create_service_merchant(service_id=second_service.service_id)
            # create not referred service
            await create_service()
            return sorted([service, second_service], key=lambda srv: srv.name)

        @pytest.fixture
        async def returned_services(self, storage, uid):
            return await alist(storage.service.find_by_service_merchants(uid))

        @pytest.mark.asyncio
        async def test_found(self, create_data, returned_services):
            assert create_data == returned_services

        class TestNotFound:
            @pytest.fixture
            def uid(self, uid):
                return uid + 1

            @pytest.mark.asyncio
            async def test_not_found(self, returned_services):
                assert [] == returned_services
