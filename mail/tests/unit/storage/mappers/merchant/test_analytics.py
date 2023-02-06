from datetime import datetime, timezone
from decimal import Decimal
from unittest import mock

import pytest

from hamcrest import assert_that, contains, contains_inanyorder, equal_to, has_properties

from mail.payments.payments.core.entities.enums import (
    NDS, AcquirerType, MerchantStatus, MerchantType, ModerationStatus, OrderKind, PayStatus, ShopType
)
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.core.entities.merchant import OrganizationData
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.product import Product
from mail.payments.payments.core.entities.service import ServiceMerchant
from mail.payments.payments.core.entities.shop import Shop, ShopSettings


class TestAnalytics:
    @pytest.fixture
    def create_order(self, storage):
        async def _create_order(**kwargs):
            order = Order(**kwargs)
            if 'created' in kwargs:
                now_mock = mock.Mock(return_value=kwargs['created'])
                with mock.patch('mail.payments.payments.storage.mappers.order.order.func.now', now_mock):
                    return await storage.order.create(order)
            return await storage.order.create(order)

            return order

        return _create_order

    @pytest.fixture(autouse=True)
    async def merchants_and_orders(self, mocker, create_merchant, create_order, service, storage):
        merchants = {
            merchant.uid: merchant
            for merchant in [
                await create_merchant(
                    uid=100, name='the draft', acquirer=AcquirerType.TINKOFF, status=MerchantStatus.DRAFT,
                    created=datetime(2020, 1, 30, tzinfo=timezone.utc),
                ),
                await create_merchant(
                    uid=101, name='the sale guy', acquirer=AcquirerType.KASSA,
                    created=datetime(2020, 1, 29, tzinfo=timezone.utc),
                    organization=OrganizationData(
                        type=MerchantType.OOO,
                        name='Hoofs & Horns',
                        english_name='HH',
                        full_name='H & H',
                        inn='111_inn',
                        kpp='222_kpp',
                        ogrn='1234567890123',
                        site_url='example.com',
                        description='test-merchant-description',
                    ),
                ),
                await create_merchant(
                    uid=102, name='the r$ch', acquirer=AcquirerType.TINKOFF,
                    created=datetime(2020, 1, 28, tzinfo=timezone.utc),
                    organization=OrganizationData(
                        type=MerchantType.OOO,
                        name='Hoofs & Horns',
                        english_name='HH',
                        full_name='H & H',
                        inn='111_inn',
                        kpp='222_kpp',
                        ogrn='1234567890123',
                        site_url='example.test',
                        description='test-merchant-description',
                    ),
                ),
                await create_merchant(
                    uid=103, name='bad guy', acquirer=AcquirerType.KASSA, blocked=True,
                    created=datetime(2020, 1, 27, tzinfo=timezone.utc),
                ),
            ]
        }

        # a merchant has more than one shop usually, but not in this particular case
        uid_shop_id_mapping = {}
        for merchant in merchants.values():
            await storage.product.create(Product(product_id=1,
                                                 uid=merchant.uid,
                                                 name='cheap',
                                                 price=Decimal('1.0'),
                                                 nds=NDS.NDS_0))
            await storage.product.create(Product(product_id=2,
                                                 uid=merchant.uid,
                                                 name='expensive',
                                                 price=Decimal('100.0'),
                                                 nds=NDS.NDS_0))
            shop_entity = Shop(
                uid=merchant.uid,
                name='Не основной',
                is_default=False,
                shop_type=ShopType.PROD,
                settings=ShopSettings(),
            )
            shop = await storage.shop.create(shop_entity)
            uid_shop_id_mapping[merchant.uid] = shop.shop_id

        # the sale guy's orders
        service_merchant = await storage.service_merchant.create(ServiceMerchant(
            service_id=service.service_id,
            uid=101,
            entity_id='some entity id',
            description='',
            enabled=True
        ))
        await create_order(order_id=1,
                           created=datetime(2020, 1, 1, tzinfo=timezone.utc),
                           closed=datetime(2020, 1, 4, tzinfo=timezone.utc),
                           uid=101,
                           shop_id=uid_shop_id_mapping[101],
                           kind=OrderKind.PAY,
                           pay_status=PayStatus.PAID)
        await storage.item.create(Item(uid=101, order_id=1, product_id=1, amount=1))
        await create_order(order_id=2,
                           created=datetime(2020, 1, 2, tzinfo=timezone.utc),
                           closed=datetime(2020, 1, 5, tzinfo=timezone.utc),
                           uid=101,
                           shop_id=uid_shop_id_mapping[101],
                           service_merchant_id=service_merchant.service_merchant_id,
                           kind=OrderKind.PAY,
                           pay_status=PayStatus.PAID)
        await storage.item.create(Item(uid=101, order_id=2, product_id=1, amount=2))
        await create_order(order_id=3,
                           shop_id=uid_shop_id_mapping[101],
                           created=datetime(2020, 1, 3, tzinfo=timezone.utc),
                           closed=datetime(2020, 1, 6, tzinfo=timezone.utc),
                           uid=101,
                           kind=OrderKind.PAY,
                           pay_status=PayStatus.PAID)
        await storage.item.create(Item(uid=101, order_id=3, product_id=1, amount=1))

        # the rich's orders
        await create_order(order_id=1,
                           uid=102,
                           shop_id=uid_shop_id_mapping[102],
                           kind=OrderKind.PAY,
                           pay_status=PayStatus.PAID)
        await storage.item.create(Item(uid=102, order_id=1, product_id=2, amount=3))

    @pytest.mark.asyncio
    async def test_returned(self, storage):
        merchants, keyset = await storage.merchant.get_analytics()
        assert_that(
            merchants,
            contains_inanyorder(
                contains(
                    has_properties({
                        'uid': 100,
                    }),
                    has_properties({
                        'payments_success': 0,
                    }),
                ),
                contains(
                    has_properties({
                        'uid': 101,
                    }),
                    has_properties({
                        'payments_success': 3,
                        'money_success': Decimal('4.0')
                    }),
                ),
                contains(
                    has_properties({
                        'uid': 102,
                    }),
                    has_properties({
                        'payments_success': 1,
                        'money_success': Decimal('300.0')
                    }),
                ),
                contains(
                    has_properties({
                        'uid': 103,
                    }),
                    has_properties({
                        'payments_success': 0,
                    }),
                ),
            )
        )

    @pytest.mark.parametrize('sort_by, desc, expected_uids', (
        ('uid', False, (100, 101, 102, 103)),
        ('uid', True, (103, 102, 101, 100)),
        ('created', False, (103, 102, 101, 100)),
        ('payments_success', False, (100, 103, 102, 101)),
        ('money_success', True, (102, 101, 103, 100)),
    ))
    @pytest.mark.asyncio
    async def test_sort(self, storage, sort_by, desc, expected_uids):
        merchants, keyset = await storage.merchant.get_analytics(sort_by=sort_by, descending=desc)
        assert_that(
            [merchant.uid for merchant, _ in merchants],
            equal_to(list(expected_uids)),
        )

    @pytest.mark.parametrize('sort_by, desc, limit, expected_keyset', (
        ('uid', False, 2, [('uid', 'asc', 101)]),
        ('uid', True, 3, [('uid', 'desc', 101)]),
        ('created', False, 2, [('created', 'asc', datetime(2020, 1, 28, tzinfo=timezone.utc)), ('uid', 'asc', 102)]),
        ('money_success', False, 3, [('money_success', 'asc', Decimal('4.0')), ('uid', 'asc', 101)]),
        ('payments_success', True, 1, [('payments_success', 'desc', 3), ('uid', 'desc', 101)]),
    ))
    @pytest.mark.asyncio
    async def test_returned_keyset(self, storage, sort_by, limit, desc, expected_keyset):
        merchants, keyset = await storage.merchant.get_analytics(sort_by=sort_by, descending=desc, limit=limit)
        assert_that(
            keyset,
            equal_to(expected_keyset),
        )

    @pytest.mark.parametrize('filter_name, filter_value, expected_uids', (
        ('uid', 103, (103,)),
        ('name', 'sal', (101,)),
        ('moderation_status', ModerationStatus.NONE, (100, 101, 102, 103)),
        ('acquirer', AcquirerType.KASSA, (101, 103)),
        ('statuses', [MerchantStatus.ACTIVE, MerchantStatus.NEW, MerchantStatus.INACTIVE], (101, 102, 103)),
        ('blocked', True, (103, )),
        ('site_url', 'test', (102,)),
        ('created_from', datetime(2020, 1, 29, tzinfo=timezone.utc), (100, 101)),
        ('created_to', datetime(2020, 1, 29, tzinfo=timezone.utc), (102, 103)),
    ))
    @pytest.mark.asyncio
    async def test_filter(self, storage, filter_name, filter_value, expected_uids):
        merchants, _ = await storage.merchant.get_analytics(**{filter_name: filter_value})
        assert_that(
            [merchant.uid for merchant, _ in merchants],
            contains_inanyorder(*expected_uids),
        )

    @pytest.mark.parametrize('filter_name, value, expected_analytics', (
        (
            'pay_created_from',
            datetime(2020, 1, 2, tzinfo=timezone.utc),
            {
                'payments_success': 2,
                'payments_total': 2,
                'payments_refund': 0,
                'money_refund': Decimal('0'),
                'money_success': Decimal('3.0'),
            },
        ),
        (
            'pay_created_to',
            datetime(2020, 1, 2, tzinfo=timezone.utc),
            {
                'payments_success': 1,
                'payments_total': 1,
                'payments_refund': 0,
                'money_refund': Decimal('0'),
                'money_success': Decimal('1.0'),
            },
        ),
        (
            'pay_closed_from',
            datetime(2020, 1, 5, tzinfo=timezone.utc),
            {
                'payments_success': 2,
                'payments_total': 2,
                'payments_refund': 0,
                'money_refund': Decimal('0'),
                'money_success': Decimal('3.0'),
            },
        ),
        (
            'pay_closed_to',
            datetime(2020, 1, 5, tzinfo=timezone.utc),
            {
                'payments_success': 1,
                'payments_total': 1,
                'payments_refund': 0,
                'money_refund': Decimal('0'),
                'money_success': Decimal('1.0'),
            },
        ),
        (
            'service_ids',
            'service.service_id',
            {
                'payments_success': 1,
                'payments_total': 1,
                'payments_refund': 0,
                'money_refund': Decimal('0'),
                'money_success': Decimal('2.0'),
            },
        ),
    ))
    class TestAnalyticsFilter:
        @pytest.fixture
        def filter_value(self, filter_name, value, service):
            if filter_name == 'service_ids':
                return (service.service_id,)
            return value

        @pytest.mark.asyncio
        async def test_analytics_filter(self, storage, filter_name, filter_value, expected_analytics):
            merchants, _ = await storage.merchant.get_analytics(uid=101, **{filter_name: filter_value})
            assert_that(
                merchants[0][1],
                has_properties(expected_analytics),
            )

    @pytest.mark.asyncio
    async def test_applies_uid_keyset_properly(self, storage):
        merchant_stats, keyset = await storage.merchant.get_analytics(limit=1, sort_by='uid')
        assert [m.uid for m, _ in merchant_stats] == [100]

        merchant_stats, keyset = await storage.merchant.get_analytics(limit=1, keyset=keyset)
        assert [m.uid for m, _ in merchant_stats] == [101]

        merchant_stats, keyset = await storage.merchant.get_analytics(limit=1, keyset=keyset)
        assert [m.uid for m, _ in merchant_stats] == [102]

        merchant_stats, keyset = await storage.merchant.get_analytics(limit=1, keyset=keyset)
        assert [m.uid for m, _ in merchant_stats] == [103]

        merchant_stats, keyset = await storage.merchant.get_analytics(limit=1, keyset=keyset)
        assert [m.uid for m, _ in merchant_stats] == []

    @pytest.mark.asyncio
    async def test_applies_created_keyset_properly(self, storage):
        merchant_stats, keyset = await storage.merchant.get_analytics(limit=1, sort_by='created', descending=True)
        assert [m.uid for m, _ in merchant_stats] == [100]

        merchant_stats, keyset = await storage.merchant.get_analytics(limit=1, keyset=keyset)
        assert [m.uid for m, _ in merchant_stats] == [101]

        merchant_stats, keyset = await storage.merchant.get_analytics(limit=1, keyset=keyset)
        assert [m.uid for m, _ in merchant_stats] == [102]

        merchant_stats, keyset = await storage.merchant.get_analytics(limit=1, keyset=keyset)
        assert [m.uid for m, _ in merchant_stats] == [103]

        merchant_stats, keyset = await storage.merchant.get_analytics(limit=1, keyset=keyset)
        assert [m.uid for m, _ in merchant_stats] == []

    @pytest.mark.asyncio
    async def test_found(self, storage):
        assert_that(
            await storage.merchant.get_analytics_found(created_from=datetime(2020, 1, 29, tzinfo=timezone.utc)),
            equal_to(2),
        )

    @pytest.mark.asyncio
    async def test_filters_out_merchants_without_services(self,
                                                          storage,
                                                          create_merchant,
                                                          create_service,
                                                          create_service_merchant,
                                                          ):
        services = [await create_service() for _ in range(2)]
        expected_merchants = []

        # Создаем продавца без сервисов. Он не должен попасть в выдачу.
        await create_merchant()

        # Создаем продавца с выключенным сервисом. Он не должен попасть в выдачу.
        m = await create_merchant()
        await create_service_merchant(uid=m.uid, service_id=services[0].service_id, enabled=False)

        # Создаем продавца с включенным, но не переданном в запросе, сервисом. Он не должен попасть в выдачу.
        s = await create_service()
        m = await create_merchant()
        await create_service_merchant(uid=m.uid, service_id=s.service_id, enabled=True)

        # Создаем продавца с одним включеным сервисом.
        m = await create_merchant()
        await create_service_merchant(uid=m.uid, service_id=services[0].service_id, enabled=True)
        expected_merchants.append(m)

        # Создаем продавца с парой включеных сервисов. Он должен попасть в выдачу, причем один раз.
        m = await create_merchant()
        for service in services:
            await create_service_merchant(uid=m.uid, service_id=service.service_id, enabled=True)
        expected_merchants.append(m)

        result, _ = await storage.merchant.get_analytics(service_ids=[s.service_id for s in services])
        result_merchants = [m for m, _ in result]
        assert_that(result_merchants, contains_inanyorder(*expected_merchants))
