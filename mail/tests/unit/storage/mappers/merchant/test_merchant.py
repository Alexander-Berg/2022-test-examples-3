from datetime import datetime, timedelta
from decimal import Decimal

import pytest

from sendr_utils import alist

from hamcrest import assert_that, equal_to, has_length

from mail.payments.payments.core.entities.enums import (
    AcquirerType, APICallbackSignMethod, MerchantStatus, ModerationStatus, ModerationType, OrderKind, OrderSource,
    PayStatus, RefundStatus
)
from mail.payments.payments.core.entities.merchant import Merchant, MerchantStat
from mail.payments.payments.core.entities.not_fetched import NOT_FETCHED
from mail.payments.payments.storage.exceptions import MerchantNotFound
from mail.payments.payments.storage.mappers.merchant import MerchantDataDumper, MerchantDataMapper
from mail.payments.payments.utils.datetime import utcnow


@pytest.fixture
def merchant(merchant):
    merchant.oauth = NOT_FETCHED
    merchant.functionalities = NOT_FETCHED
    return merchant


@pytest.fixture
def merchant_data_dict(merchant):
    return {
        'addresses': {
            address.type: {
                'city': address.city,
                'country': address.country,
                'home': address.home,
                'street': address.street,
                'zip': address.zip,
            }
            for address in merchant.addresses
        },
        'bank': {
            'account': merchant.bank.account,
            'bik': merchant.bank.bik,
            'correspondentAccount': merchant.bank.correspondent_account,
            'name': merchant.bank.name,
        },
        'organization': {
            'type': merchant.organization.type.value,
            'name': merchant.organization.name,
            'englishName': merchant.organization.english_name,
            'fullName': merchant.organization.full_name,
            'inn': merchant.organization.inn,
            'kpp': merchant.organization.kpp,
            'ogrn': merchant.organization.ogrn,
            'scheduleText': merchant.organization.schedule_text,
            'siteUrl': merchant.organization.site_url,
            'description': merchant.organization.description,
        },
        'persons': {
            person.type.value: {
                'name': person.name,
                'surname': person.surname,
                'patronymic': person.patronymic,
                'email': person.email,
                'birthDate': person.birth_date.isoformat(),
                'phone': person.phone,
            }
            for person in merchant.persons
        },
        'username': merchant.username,
        'registered': merchant.registered,
        'fast_moderation': merchant.fast_moderation,
    }


@pytest.fixture
def merchant_documents_list(merchant):
    return [
        {
            'document_type': document.document_type.value,
            'path': document.path,
            'size': document.size,
            'created': document.created.isoformat(),
            'name': document.name,
        }
        for document in merchant.documents
    ]


@pytest.fixture
def merchant_callback_params(merchant, rands):
    return {
        'sign_method': APICallbackSignMethod.ASYMMETRIC.value,
        'secret': '123'
    }


@pytest.fixture
def merchant_options():
    return {
        'order_offline_abandon_period': 42,
        'allow_create_service_merchants': False,
        'allowed_order_sources': [OrderSource.UI.value],
        'hide_commission': True,
        'can_skip_registration': True,
        'offer_settings': {
            'pdf_template': 'templ',
            'slug': 'slug',
            'data_override': {'over': 'ride'},
        },
        'payment_systems': {
            'apple_pay_enabled': True,
            'google_pay_enabled': False,
        },
        'order_max_total_price': None,
    }


@pytest.fixture
def merchant_dict(merchant, merchant_data_dict, merchant_options, merchant_callback_params, merchant_documents_list):
    result = {
        attr: getattr(merchant, attr)
        for attr in [
            'uid',
            'merchant_id',
            'name',
            'revision',
            'trustworthy',
            'status',
            'blocked',
            'created',
            'updated',
            'client_id',
            'person_id',
            'contract_id',
            'submerchant_id',
            'parent_uid',
            'api_callback_url',
            'token',
            'acquirer',
            'dialogs_org_id',
            'support_comment',
            'data_updated_at',
            'data_locked'
        ]
    }
    result['data'] = merchant_data_dict
    result['documents'] = merchant_documents_list
    result['api_callback_params'] = merchant_callback_params
    result['options'] = merchant_options
    return result


class TestMerchantDataMapper:
    def test_map(self, loop, merchant, merchant_dict):
        row = {
            type(merchant).__name__ + '__' + key: value
            for key, value in merchant_dict.items()
        }
        mapped = MerchantDataMapper()(row)
        mapped.load_parent()
        mapped.load_data()
        from dataclasses import asdict
        assert asdict(mapped) == asdict(merchant)


class TestMerchantDataDumper:
    def test_unmap(self, loop, merchant, merchant_dict):
        assert MerchantDataDumper()(merchant) == merchant_dict


class TestMerchantMapper:
    @pytest.fixture
    def now(self, mocker):
        now = utcnow()
        mocker.patch('mail.payments.payments.storage.mappers.merchant.merchant.utcnow', mocker.Mock(return_value=now))
        return now

    @pytest.mark.asyncio
    async def test_get_not_found(self, storage, merchant):
        with pytest.raises(MerchantNotFound):
            await storage.merchant.get(uid=merchant.uid + 1)

    @pytest.mark.asyncio
    async def test_get(self, merchant, storage):
        merchant_fetched = await storage.merchant.get(merchant.uid)
        merchant_fetched.load_parent()
        merchant_fetched.load_data()
        assert merchant_fetched == merchant

    @pytest.mark.asyncio
    async def test_find_by_token(self, merchant, storage):
        result = await storage.merchant.find_by_token(merchant.token)
        result.load_parent()
        result.load_data()
        assert result == merchant

    @pytest.mark.asyncio
    async def test_get_by_merchant_id(self, merchant, storage):
        result = await storage.merchant.get_by_merchant_id(merchant.merchant_id)
        result.load_parent()
        result.load_data()
        assert result == merchant

    @pytest.mark.asyncio
    async def test_find_by_token_not_found(self, storage):
        with pytest.raises(MerchantNotFound):
            await storage.merchant.find_by_token('some invalid token')

    @pytest.mark.asyncio
    async def test_get_merchants_count(self, merchant, storage):
        statuses = [MerchantStatus.NEW, MerchantStatus.ACTIVE, MerchantStatus.INACTIVE]
        assert await storage.merchant.get_found_count(statuses=statuses) == 1

    @pytest.mark.asyncio
    async def test_save(self, storage, merchant, now):
        merchant = await storage.merchant.save(merchant)
        merchant.load_parent()
        merchant.name = 'New name'
        merchant.revision += 1
        saved = await storage.merchant.save(merchant)
        saved.load_parent()
        saved.load_data()
        assert saved == merchant

    class TestBatchOrdersStats:
        @pytest.fixture
        def now(self):
            return utcnow()

        @pytest.fixture(params=(None, 'now'))
        def closed_from(self, request):
            return request.param

        @pytest.fixture(params=(None, 'now', 'yesterday'))
        def closed_to(self, request):
            return request.param

        @pytest.fixture
        def exclude_stats(self):
            return False

        @pytest.fixture
        def default_commission(self):
            return Decimal('0.0215')

        @pytest.fixture
        async def default_order(self, storage, items, order, now, exclude_stats):
            order = await storage.order.get(order.uid, order.order_id)
            order.pay_status = PayStatus.PAID
            order.exclude_stats = exclude_stats
            order.closed = now
            return await storage.order.save(order)

        @pytest.mark.asyncio
        async def test_batch_orders_stats(self, merchant, acquirer, storage, now, closed_from, closed_to, items,
                                          exclude_stats, default_order, default_commission):
            if closed_from == 'now':
                closed_from = now

            if closed_to is not None:
                closed_to = now + (timedelta(days=1) if closed_to == 'now' else timedelta(days=0))

            stats = await alist(
                storage.merchant.batch_orders_stats(
                    acquirer=merchant.acquirer,
                    closed_from=closed_from,
                    closed_to=closed_to,
                    default_commission=default_commission,
                )
            )
            if len(stats) > 0:
                assert (closed_from is None or closed_from <= now) and (closed_to is None or now < closed_to)

        @pytest.mark.parametrize('exclude_stats', [True])
        @pytest.mark.asyncio
        async def test_exclude(self, storage, default_order, default_commission):
            assert len(await alist(storage.merchant.batch_orders_stats(default_commission=default_commission))) == 0

        @pytest.mark.asyncio
        async def test_acquirer(self, merchant, storage, default_order, default_commission):
            for acquirer in AcquirerType:
                assert_that(
                    await alist(
                        storage.merchant.batch_orders_stats(
                            acquirer=acquirer,
                            default_commission=default_commission
                        )
                    ),
                    has_length(1 if acquirer == merchant.acquirer else 0)
                )

        @pytest.mark.asyncio
        async def test_service_id(self, merchant, storage, service,
                                  order_with_service, default_order, default_commission):
            for service_id in [service.service_id, service.service_id + 1]:
                assert_that(
                    await alist(
                        storage.merchant.batch_orders_stats(
                            service_id=service_id,
                            default_commission=default_commission
                        )
                    ),
                    has_length(1 if service_id == service.service_id else 0)
                )

        @pytest.mark.asyncio
        async def test_batch_order_stats_commission(
            self, storage, default_commission, merchant, create_order, create_item, create_product, now
        ):
            product = await create_product(price=5)

            order = await create_order(commission=1000, pay_status=PayStatus.PAID, closed=now)
            await create_item(order=order, product=product, amount=10)

            order = await create_order(pay_status=PayStatus.PAID, closed=now)
            await create_item(order=order, product=product, amount=20)

            stats = await alist(storage.merchant.batch_orders_stats(default_commission=Decimal('0.05')))

            assert_that(
                stats,
                equal_to([
                    MerchantStat(
                        name=merchant.name,
                        orders_sum=Decimal(5 * 10 + 5 * 20),
                        commission=(5 * 10 * Decimal('0.1') + 5 * 20 * Decimal('0.05')),
                        orders_kind=OrderKind.PAY,
                    )
                ])
            )

        @pytest.mark.asyncio
        async def test_batch_order_stats_with_refund(
            self, storage, default_commission, merchant, create_order, create_item, create_product, now
        ):
            product = await create_product(price=5)

            order = await create_order(commission=1000, pay_status=PayStatus.PAID, closed=now)
            await create_item(order=order, product=product, amount=10)

            refund = await create_order(
                kind=OrderKind.REFUND,
                original_order_id=order.order_id,
                pay_status=None,
                refund_status=RefundStatus.COMPLETED,
                closed=now,
            )
            await create_item(order=refund, product=product, amount=5)

            stats = await alist(storage.merchant.batch_orders_stats(default_commission=default_commission))

            assert_that(
                stats,
                equal_to([
                    MerchantStat(
                        name=merchant.name,
                        orders_sum=Decimal(5 * 10),
                        commission=(5 * 10 * Decimal('0.1')),
                        orders_kind=OrderKind.PAY,
                    ),
                    MerchantStat(
                        name=merchant.name,
                        orders_sum=Decimal(5 * 5),
                        commission=Decimal('0'),
                        orders_kind=OrderKind.REFUND,
                    )
                ])
            )

    class TestFind:
        @pytest.fixture
        def setup_data(self):
            return []

        @pytest.fixture(autouse=True)
        async def merchants(self, create_merchant, setup_data):
            return [
                await create_merchant(**data)
                for data in setup_data
            ]

        @pytest.fixture
        def merchant_uids(self, merchants):
            return [merchant.uid for merchant in merchants]

        @pytest.fixture
        def find(self, storage):
            async def _inner(**kwargs):
                return [m async for m in storage.merchant.find(**kwargs)]

            return _inner

        @pytest.mark.asyncio
        @pytest.mark.parametrize('setup_data', (([{'name': 'aaaaaxxABa'}, {'name': 'caBa'}, {'name': 'xxx'}]),))
        @pytest.mark.parametrize('name', ('ab', 'AB', 'Ab', 'aB'))
        async def test_find__filters_name_ilike(self, find, merchants, name):
            assert await find(name=name) == sorted([merchants[0], merchants[1]], key=lambda m: m.uid)

        @pytest.mark.parametrize('setup_data', ([{'name': 'aaaaaxxABa'}, {'name': 'caBa'}, {'name': 'xxx'}],))
        @pytest.mark.asyncio
        async def test_find__get_found_count(self, merchants, storage):
            assert await storage.merchant.get_found_count(name='ab') == 2

        @pytest.mark.parametrize(
            'setup_data',
            [[
                {'acquirer': AcquirerType.TINKOFF},
                {'acquirer': AcquirerType.KASSA},
                {'acquirer': AcquirerType.TINKOFF}
            ]]
        )
        @pytest.mark.asyncio
        async def test_find__acquirer(self, find, merchants):
            expected = [merchants[0], merchants[2]]
            assert await find(acquirer=AcquirerType.TINKOFF) == sorted(expected, key=lambda m: m.uid)

        @pytest.mark.parametrize(
            'setup_data',
            [[
                {'acquirer': AcquirerType.TINKOFF},
                {'acquirer': AcquirerType.KASSA},
            ]]
        )
        @pytest.mark.asyncio
        async def test_find__acquirer_multi(self, find, merchants):
            expected = sorted(merchants, key=lambda m: m.uid)
            assert await find(acquirers=[AcquirerType.TINKOFF, AcquirerType.KASSA]) == expected

        @pytest.mark.parametrize('params', [
            {'created_from': datetime.now() - timedelta(days=1), 'created_to': datetime.now() + timedelta(days=1)},
            {'updated_from': datetime.now() - timedelta(days=1), 'updated_to': datetime.now() + timedelta(days=1)},
        ])
        @pytest.mark.asyncio
        async def test_find__find_by_date(self, params, find, merchant):
            found = await find(**params)
            assert found == [merchant]

        @pytest.mark.parametrize('params', [
            {'created_from': datetime.now() + timedelta(days=1)},
            {'updated_from': datetime.now() + timedelta(days=1)},
            {'created_to': datetime.now() - timedelta(days=1)},
            {'updated_to': datetime.now() - timedelta(days=1)},
        ])
        @pytest.mark.asyncio
        async def test_find__find_by_date_empty(self, params, find, merchant):
            found = await find(**params)
            assert found == []

        @pytest.mark.parametrize('descending', (True, False))
        @pytest.mark.parametrize('setup_data', ([{'name': 'aaaaaxxABa'}, {'name': 'caBa'}, {'name': 'xxx'}],))
        @pytest.mark.parametrize('sort_by', (
            None, 'uid', 'name'
        ))  # TODO: флапает 'created', 'updated' из-за 1 транзакции
        @pytest.mark.asyncio
        async def test_find__sorting(self, find, merchants, merchant_uids, descending, sort_by):
            returned = filter(lambda m: m.uid in merchant_uids, await find(sort_by=sort_by, descending=descending))
            assert list(returned) == sorted(merchants, key=lambda m: getattr(m, sort_by or 'uid'), reverse=descending)

        class TestSimpleFilters:
            @pytest.fixture(params=('uid', 'username', 'client_id', 'submerchant_id'))
            def filter_field(self, request):
                return request.param

            @pytest.fixture
            def filter_values(self, unique_rand, randn, filter_field):
                if filter_field == 'uid':
                    return [unique_rand(randn, basket='uid') for _ in range(3)]
                else:
                    return ['aaa', 'bbb', 'ccc']

            @pytest.fixture
            def setup_data(self, filter_field, filter_values):
                return [{filter_field: value} for value in filter_values]

            @pytest.mark.asyncio
            async def test_simple_filter(self, randn, find, merchants, filter_field):
                position = randn(min=0, max=len(merchants) - 1)
                m = merchants[position]
                value = getattr(m.data, filter_field, None) or getattr(m, filter_field)
                assert await find(**{filter_field: value}) == [merchants[position]]

        class TestParentFilters:
            @pytest.fixture(params=('username', 'client_id', 'submerchant_id'))
            def filter_field(self, request):
                return request.param

            @pytest.fixture
            def filter_value(self):
                return 'value-to-be-filtered-by'

            @pytest.fixture
            async def merchants(self, create_merchant, filter_field, filter_value):
                # Parent has matching filter value, child doesn't. Both match
                parent = await create_merchant(**{filter_field: filter_value})
                child = await create_merchant(parent_uid=parent.uid, **{filter_field: 'bad-value'})
                child.parent = parent
                child.load_parent()

                # Parent doesn't have matching filter value, child does. Both don't match
                bad_parent = await create_merchant(**{filter_field: 'other-bad-value'})
                bad_child = await create_merchant(parent_uid=bad_parent.uid, **{filter_field: filter_value})
                bad_child.parent = parent
                bad_child.load_parent()

                return [parent, child, bad_parent, bad_child]

            @pytest.mark.asyncio
            async def test_parent_filter(self, merchants, find, filter_field, filter_value):
                merchants_by_uid = {m.uid: m for m in merchants}
                found = await find(**{filter_field: filter_value})
                for m in found:
                    m.parent = merchants_by_uid[m.parent_uid] if m.parent_uid else None
                    m.load_parent()
                assert found == sorted(merchants[:2], key=lambda m: m.uid)

        class TestModerationStatusFilter:
            @pytest.fixture
            def setup_data(self):
                return [{} for _ in range(3)]

            @pytest.fixture
            def moderations_data(self, merchant, merchants):
                return [
                    {'moderation_type': ModerationType.MERCHANT, 'approved': False, 'uid': merchant.uid, 'revision': 0},
                    {'moderation_type': ModerationType.ORDER},
                    {'moderation_type': ModerationType.MERCHANT, 'approved': True, 'uid': merchant.uid, 'revision': 1},
                    {
                        'moderation_type': ModerationType.MERCHANT,
                        'approved': False,
                        'uid': merchants[0].uid,
                        'revision': 2,
                    },
                    {
                        'moderation_type': ModerationType.MERCHANT,
                        'approved': False,
                        'uid': merchants[0].uid,
                        'revision': 3,
                    },
                    {
                        'moderation_type': ModerationType.MERCHANT,
                        'approved': False,
                        'uid': merchants[1].uid,
                        'revision': 4,
                    },
                    {
                        'moderation_type': ModerationType.MERCHANT,
                        'approved': None,
                        'uid': merchants[1].uid,
                        'revision': 5,
                    },
                ]

            @pytest.mark.asyncio
            async def test_approved(self, find, merchant, merchants, moderations):
                assert await find(moderation_status=ModerationStatus.APPROVED) == [merchant]

            @pytest.mark.asyncio
            async def test_none(self, find, merchants, moderations):
                assert await find(moderation_status=ModerationStatus.NONE) == [merchants[2]]

            @pytest.mark.asyncio
            async def test_reject(self, find, merchants, moderations):
                assert await find(moderation_status=ModerationStatus.REJECTED) == [merchants[0]]

            @pytest.mark.asyncio
            async def test_ongoing(self, find, merchants, moderations):
                assert await find(moderation_status=ModerationStatus.ONGOING) == [merchants[1]]

        @pytest.mark.parametrize('keyset, expected_uids', (
            ((('created', 'desc', datetime(2020, 2, 1, 0, 0, 0)), ('uid', 'desc', 2000)), (1001, 1002, 1004)),
            ((('created', 'desc', datetime(2020, 1, 15, 0, 0, 0)), ('uid', 'desc', 2000)), (1001,)),
            ((('created', 'asc', datetime(2020, 2, 15, 0, 0, 0)), ('uid', 'desc', 1000)), (1003,)),
            ((('created', 'asc', datetime(2020, 2, 1, 0, 0, 0)), ('uid', 'asc', 1000)), (1002, 1003, 1004)),
            ((('created', 'asc', datetime(2020, 2, 1, 0, 0, 0)), ('uid', 'asc', 1002)), (1003, 1004)),
            ((('updated', 'asc', datetime(2020, 1, 1, 0, 0, 0)), ('uid', 'asc', 1000)), (1001, 1002, 1003, 1004)),
            ((('updated', 'asc', datetime(2020, 2, 1, 0, 0, 0)), ('uid', 'asc', 1000)), (1001, 1003, 1004)),
            ((('updated', 'asc', datetime(2020, 3, 1, 0, 0, 0)), ('uid', 'asc', 1000)), (1003,)),
            ((('updated', 'asc', datetime(2020, 4, 1, 0, 0, 0)), ('uid', 'asc', 1000)), ()),
            ((('updated', 'desc', datetime(2020, 2, 1, 0, 0, 0)), ('uid', 'desc', 2000)), (1001, 1002, 1004)),
            ((('updated', 'desc', datetime(2020, 2, 15, 0, 0, 0)), ('uid', 'desc', 1003)), (1001, 1002, 1004)),
            ((('updated', 'desc', datetime(2020, 1, 15, 0, 0, 0)), ('uid', 'desc', 2000)), (1002,)),
            ((('uid', 'desc', 1003),), (1001, 1002)),
        ))
        class TestKeysetFilters:
            @pytest.fixture
            async def merchants(self, db_conn, create_merchant, merchant):
                await db_conn.execute(f'delete from payments.merchants where uid = {merchant.uid}')
                await create_merchant(
                    uid=1001,
                    created=datetime(2020, 1, 1, 0, 0, 0),
                    updated=datetime(2020, 2, 1, 0, 0, 0),
                )
                await create_merchant(
                    uid=1002,
                    created=datetime(2020, 2, 1, 0, 0, 0),
                    updated=datetime(2020, 1, 1, 0, 0, 0),
                )
                await create_merchant(
                    uid=1003,
                    created=datetime(2020, 3, 1, 0, 0, 0),
                    updated=datetime(2020, 3, 1, 0, 0, 0),
                )
                await create_merchant(
                    uid=1004,
                    created=datetime(2020, 2, 1, 0, 0, 0),
                    updated=datetime(2020, 2, 1, 0, 0, 0),
                )

            @pytest.mark.asyncio
            async def test_keyset_filter(self, merchants, find, keyset, expected_uids):
                found = await find(keyset=keyset)
                found_uids = tuple([item.uid for item in found])
                assert sorted(expected_uids) == sorted(found_uids)

    class TestGetForDataUpdate:
        @pytest.mark.asyncio
        async def test_get_for_data_update__no_merchants(self, db_conn, merchant, storage):
            await db_conn.execute(f'delete from payments.merchants where uid = {merchant.uid}')
            with pytest.raises(MerchantNotFound):
                await storage.merchant.get_for_data_update(older_than_secs=0, locked_older_than_secs=0)

        @pytest.mark.asyncio
        async def test_get_for_data_update__too_fresh(self, merchant, storage):
            with pytest.raises(MerchantNotFound):
                await storage.merchant.get_for_data_update(older_than_secs=60, locked_older_than_secs=60)

        @pytest.mark.asyncio
        async def test_get_for_data_update__data_locked(self, merchant, storage):
            merchant.data_locked = True
            await storage.merchant.save(merchant)

            with pytest.raises(MerchantNotFound):
                await storage.merchant.get_for_data_update(older_than_secs=60, locked_older_than_secs=60)

        @pytest.mark.asyncio
        async def test_get_for_data_update__client_id_none(self, merchant, storage):
            merchant.client_id = None
            await storage.merchant.save(merchant)

            with pytest.raises(MerchantNotFound):
                await storage.merchant.get_for_data_update(older_than_secs=60, locked_older_than_secs=60)

        @pytest.mark.asyncio
        async def test_get_for_data_update__data_locked_too_old(self, merchant, storage):
            merchant.data_updated_at = utcnow() - timedelta(minutes=1)
            merchant.data_locked = True
            await storage.merchant.save(merchant)

            found = await storage.merchant.get_for_data_update(older_than_secs=0, locked_older_than_secs=0)
            assert merchant == found

        @pytest.mark.asyncio
        async def test_get_for_data_update__found(self, merchant, storage):
            merchant.data_updated_at = utcnow() - timedelta(minutes=1)
            await storage.merchant.save(merchant)

            found = await storage.merchant.get_for_data_update(older_than_secs=0, locked_older_than_secs=0)
            assert merchant == found

        @pytest.mark.asyncio
        async def test_get_for_data_update__not_found_with_parent(self, merchant, merchant_with_parent, storage):
            merchant.data_updated_at = utcnow() + timedelta(minutes=1)
            merchant_with_parent.data_updated_at = utcnow() - timedelta(minutes=1)

            await storage.merchant.save(merchant)
            await storage.merchant.save(merchant_with_parent)

            with pytest.raises(MerchantNotFound):
                await storage.merchant.get_for_data_update(older_than_secs=0, locked_older_than_secs=0)

    class TestGetForDataUpdateStats:
        @pytest.mark.asyncio
        async def test_get_for_data_update_stats__no_merchants(self, db_conn, merchant, storage):
            await db_conn.execute(f'delete from payments.merchants where uid = {merchant.uid}')
            with pytest.raises(MerchantNotFound):
                await storage.merchant.get_for_data_update_stats()

        @pytest.mark.asyncio
        async def test_get_for_data_update_stats__client_id_none(self, merchant, storage):
            merchant.client_id = None
            await storage.merchant.save(merchant)

            with pytest.raises(MerchantNotFound):
                await storage.merchant.get_for_data_update_stats()

        @pytest.mark.asyncio
        async def test_get_for_data_update_stats__found(self, merchant, storage):
            merchant.data_updated_at = utcnow() - timedelta(minutes=1)
            await storage.merchant.save(merchant)

            found = await storage.merchant.get_for_data_update_stats()
            assert merchant == found

        @pytest.mark.asyncio
        async def test_get_for_data_update_stats__found_parent(self, merchant, merchant_with_parent, storage):
            merchant.data_updated_at = utcnow() + timedelta(minutes=1)
            merchant_with_parent.data_updated_at = utcnow() - timedelta(minutes=1)

            await storage.merchant.save(merchant)
            await storage.merchant.save(merchant_with_parent)

            found = await storage.merchant.get_for_data_update_stats()
            assert merchant == found

    class TestOrdersStats:
        @pytest.fixture
        def default_commission(self):
            return Decimal('0.0215')

        @pytest.mark.asyncio
        async def test_no_orders(self, merchant, storage, default_commission):
            res = await storage.merchant.orders_stats(merchant.uid, default_commission=default_commission)
            assert res == MerchantStat() and res.money_average == 0

        @pytest.mark.parametrize('set_paid,expected_stats', (
            (False, MerchantStat(orders_created_count=1)),
            (True, MerchantStat(orders_paid_count=1, orders_created_count=1)),
        ))
        @pytest.mark.asyncio
        async def test_order_no_items(self, order, merchant, storage, set_paid, expected_stats, default_commission):
            if set_paid:
                order.pay_status = PayStatus.PAID
                await storage.order.save(order)
            assert expected_stats == await storage.merchant.orders_stats(merchant.uid, default_commission)

        @pytest.mark.asyncio
        async def test_filter_by_uid(self, order, merchant, storage, default_commission):
            assert MerchantStat() == await storage.merchant.orders_stats(merchant.uid + 1, default_commission)

        @pytest.fixture
        async def setup_orders(self, storage, items, order, order_data, create_order, create_items, merchant):
            order_not_paid = await create_order(**order_data)
            await create_items(order_not_paid)

            order_data['pay_status'] = PayStatus.PAID
            order_data['closed'] = utcnow() + timedelta(hours=2)

            order_paid_first = await create_order(**order_data)
            await create_items(order_paid_first)

            order_data['commission'] = 1000
            order_paid_second = await create_order(**order_data)
            await create_items(order_paid_second)

            return order, order_not_paid, order_paid_first, order_paid_second

        @pytest.mark.parametrize('add_from,add_to,paid_indexes,created_indexes', (
            (False, False, [2, 3], [0, 1, 2, 3]),
            (True, False, [2, 3], []),
            (False, True, [], [0, 1, 2, 3]),
            (True, True, [], []),
        ))
        @pytest.mark.asyncio
        async def test_time_bounds(self, setup_orders, storage, merchant, add_from, add_to, paid_indexes,
                                   created_indexes, default_commission):
            time = utcnow() + timedelta(hours=1)
            expected = MerchantStat(
                orders_sum=sum([setup_orders[i].price for i in paid_indexes], Decimal(0)),
                commission=sum([(
                    setup_orders[i].price * (Decimal(setup_orders[i].commission) / 10000
                                             if setup_orders[i].commission else default_commission)
                ) for i in paid_indexes], Decimal(0)),
                orders_paid_count=len(paid_indexes),
                orders_created_count=len(created_indexes),
            )
            assert expected == await storage.merchant.orders_stats(merchant.uid,
                                                                   date_from=time if add_from else None,
                                                                   date_to=time if add_to else None,
                                                                   default_commission=default_commission)

        @pytest.mark.asyncio
        async def test_money_average(self, setup_orders, storage, merchant, default_commission):
            res = await storage.merchant.orders_stats(merchant.uid, default_commission)
            assert res.money_average == (setup_orders[2].price + setup_orders[3].price) / Decimal(2)


class TestMerchantDraft:
    @pytest.fixture
    def merchant_draft_entity(self, merchant_data_draft, merchant_draft_uid):
        return Merchant(
            uid=merchant_draft_uid,
            status=MerchantStatus.DRAFT,
            name='Test merchant draft',
            draft_data=merchant_data_draft,
        )

    @pytest.fixture
    async def created_draft(self, merchant_draft_entity, storage):
        return await storage.merchant.create(merchant_draft_entity)

    def test_created_updated(self, created_draft):
        assert isinstance(created_draft.created, datetime) and isinstance(created_draft.updated, datetime)

    def test_draft_created(self, created_draft, merchant_draft_entity):
        merchant_draft_entity.created = created_draft.created
        merchant_draft_entity.updated = created_draft.updated

        assert created_draft == merchant_draft_entity

    @pytest.mark.asyncio
    async def test_found_count(self, created_draft, storage):
        statuses = [MerchantStatus.NEW, MerchantStatus.ACTIVE, MerchantStatus.INACTIVE]
        assert await storage.merchant.get_found_count(statuses=statuses) == 0
