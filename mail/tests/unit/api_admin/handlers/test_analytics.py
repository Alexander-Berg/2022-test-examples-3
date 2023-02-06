from datetime import datetime, timezone
from decimal import Decimal

import pytest

from hamcrest import assert_that, contains, equal_to, has_entries, not_none

from mail.payments.payments.core.entities.common import SearchStats
from mail.payments.payments.core.entities.enums import AcquirerType, ModerationStatus, PersonType
from mail.payments.payments.core.entities.keyset import ManagerAnalyticsKeysetEntity
from mail.payments.payments.core.entities.merchant import (
    MerchantsAnalyticsAdminData, MerchantWithAnalytics, OrganizationData, PersonData
)
from mail.payments.payments.core.entities.moderation import ModerationData
from mail.payments.payments.core.entities.service import Service, ServiceMerchant


class TestGetMerchantAnalyticsManagerHandler:
    @pytest.fixture
    def mock_keyset(self):
        return ManagerAnalyticsKeysetEntity(sort_order=[])

    @pytest.fixture
    def action_result(self, merchant, mock_keyset):
        merchant = MerchantWithAnalytics(
            uid=merchant.uid,
            name=merchant.name,
            moderation=merchant.moderation,
            created=merchant.created,
            acquirer=merchant.acquirer,
            blocked=merchant.blocked,
            client_id=merchant.client_id,
            organization=merchant.organization,
            service_merchants=[],
            support_comment=merchant.support_comment,
            contact=merchant.contact,
            payments_total=0,
            payments_success=0,
            payments_refund=0,
            money_success=Decimal('0.0'),
            money_refund=Decimal('0.0'),
        )
        return MerchantsAnalyticsAdminData(
            merchants=[merchant],
            stats=SearchStats(total=1, found=1),
            keyset=mock_keyset,
        )

    @pytest.fixture(autouse=True)
    def action(self, mock_action, action_result):
        from mail.payments.payments.core.actions.manager.analytics import GetMerchantAnalyticsManagerAction
        return mock_action(GetMerchantAnalyticsManagerAction, action_result)

    @pytest.fixture
    def acting_manager(self, manager_assessor):
        return manager_assessor

    @pytest.fixture
    def request_params(self):
        return {}

    @pytest.fixture
    def request_url(self):
        return '/admin/api/v1/analytics'

    @pytest.fixture
    async def response(self, admin_client, request_url, request_params, tvm):
        return await admin_client.get(request_url, params=request_params)

    @pytest.fixture
    async def response_data(self, response):
        return (await response.json())['data']

    @pytest.mark.parametrize('request_params', (
        {'limit': 100},
        {'merchant_uid': 'not-a-number'},
        {'created_from': 'not-a-date'},
        {'acquirer': 'not-an-acquirer'},
        {'sort_by': 'client_id'},
    ))
    def test_error(self, response):
        assert response.status == 400

    @pytest.mark.parametrize('request_params, expected_call', (
        ({}, {'limit': 10}),
        (
            {
                'merchant_uid': 111222,
                'name': 'test-name',
                'blocked': 'false',
                'site_url': 'site-url',
                'acquirer': 'kassa',
                'moderation_status': 'rejected',
            },
            {
                'merchant_uid': 111222,
                'name': 'test-name',
                'blocked': False,
                'site_url': 'site-url',
                'acquirer': AcquirerType.KASSA,
                'limit': 10,
                'moderation_status': ModerationStatus.REJECTED
            },
        ),
        (
            {
                'created_from': '2020-01-01T00:00:00+00:00',
                'created_to': '2020-01-02T00:00:00Z',
                'order_created_from': '2020-01-03T00:00:00+00:00',
                'order_created_to': '2020-01-04T00:00:00Z',
                'order_closed_from': '2020-01-05T00:00:00+00:00',
                'order_closed_to': '2020-01-06T00:00:00Z',
            },
            {
                'created_from': datetime(2020, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
                'created_to': datetime(2020, 1, 2, 0, 0, 0, tzinfo=timezone.utc),
                'order_created_from': datetime(2020, 1, 3, 0, 0, 0, tzinfo=timezone.utc),
                'order_created_to': datetime(2020, 1, 4, 0, 0, 0, tzinfo=timezone.utc),
                'order_closed_from': datetime(2020, 1, 5, 0, 0, 0, tzinfo=timezone.utc),
                'order_closed_to': datetime(2020, 1, 6, 0, 0, 0, tzinfo=timezone.utc),
                'limit': 10,
            },
        ),
        ({'limit': 5}, {'limit': 5}),
        (
            (('service_ids[]', 1), ('service_ids[]', 2), ('service_ids[]', 3)),
            {'service_ids': [1, 2, 3], 'limit': 10}
        ),
    ))
    def test_params(self, response, response_data, action, manager_assessor, expected_call):
        action.assert_called_once_with(
            **expected_call,
            manager_uid=manager_assessor.uid,
        )

    @pytest.mark.parametrize('action_result, expected', (
        pytest.param(
            MerchantsAnalyticsAdminData(
                merchants=[
                    MerchantWithAnalytics(
                        uid=123,
                        name='foo',
                        blocked=True,
                        client_id='clientid',
                        support_comment='comment',
                        acquirer=AcquirerType.TINKOFF,
                        created=datetime(2020, 1, 1, 0, 0, 0, tzinfo=timezone.utc),
                        contact=PersonData(
                            type=PersonType.CEO,
                            email='the-email',

                            name='whatever',
                            phone='whatever',
                            surname='whatever',
                        ),
                        organization=OrganizationData(
                            site_url='siteurl',
                            description='description',

                            type='whatever',
                            name='whatever',
                            english_name='whatever',
                            full_name='whatever',
                            inn='whatever',
                            kpp='whatever',
                            ogrn='whatever',
                        ),
                        moderation=ModerationData(
                            approved=False,
                            has_moderation=True,
                            has_ongoing=True,
                            reasons=[],
                            updated=datetime(2020, 1, 2, 0, 0, 0, tzinfo=timezone.utc),
                        ),
                        payments_success=5,
                        payments_total=10,
                        payments_refund=3,
                        money_refund=Decimal('322.0'),
                        money_success=Decimal('100.0'),
                        service_merchants=[
                            ServiceMerchant(
                                service_merchant_id=4,
                                uid=123,
                                service_id=5,
                                entity_id='entity',
                                description='desc',
                                service=Service(service_id=5, name='service-name'),
                            )
                        ],
                    ),
                ],
                stats=SearchStats(total=1, found=1),
                keyset=ManagerAnalyticsKeysetEntity(created=datetime(2020, 1, 1, 0, 0, 0), sort_order=['created']),
            ),
            has_entries({
                'merchants': contains(
                    has_entries({
                        'name': 'foo',
                        'blocked': True,
                        'client_id': 'clientid',
                        'support_comment': 'comment',
                        'acquirer': AcquirerType.TINKOFF.value,
                        'organization': {
                            'siteUrl': 'siteurl',
                            'description': 'description',
                        },
                        'contact': {
                            'email': 'the-email',
                        },
                        'payments_success': 5,
                        'payments_total': 10,
                        'payments_refund': 3,
                        'money_aov': '20.0',
                        'money_success': '100.0',
                        'money_refund': '322.0',
                        'created': '2020-01-01T00:00:00+00:00',
                        'moderation': {
                            'updated': '2020-01-02T00:00:00+00:00',
                            'status': ModerationStatus.ONGOING.value,
                        },
                        'service_merchants': [
                            {
                                'service_merchant_id': 4,
                                'service': {
                                    'service_id': 5,
                                    'name': 'service-name',
                                }
                            },
                        ]
                    }),
                ),
                'total': 1,
                'found': 1,
                'next': has_entries({
                    'keyset': not_none(),
                }),
            }),
            id='general format',
        ),
        pytest.param(
            MerchantsAnalyticsAdminData(
                merchants=[],
                stats=SearchStats(total=1, found=1),
                keyset=None,
            ),
            equal_to({
                'merchants': [],
                'found': 1,
                'total': 1,
            }),
            id='empty response',
        ),
        pytest.param(
            MerchantsAnalyticsAdminData(
                merchants=[
                    MerchantWithAnalytics(
                        uid=123,
                        name='foo',
                        acquirer=None,
                        blocked=False,
                        created=None,
                        client_id=None,
                        support_comment=None,
                        contact=PersonData(
                            type=PersonType.CEO,
                            email='the-email',
                            name='whatever',
                            phone='whatever',
                            surname='whatever',
                        ),
                        organization=OrganizationData(
                            type='whatever',
                            name='whatever',
                            english_name='whatever',
                            full_name='whatever',
                            inn='whatever',
                            kpp='whatever',
                            ogrn='whatever',
                        ),
                        moderation=ModerationData(
                            approved=False,
                            has_moderation=False,
                            has_ongoing=False,
                            reasons=[],
                            updated=None,
                        ),
                        payments_success=0,
                        payments_refund=0,
                        payments_total=0,
                        money_success=Decimal('0.0'),
                        money_refund=Decimal('0.0'),
                        service_merchants=[],
                    ),
                ],
                stats=SearchStats(total=1, found=1),
                keyset=None,
            ),
            has_entries({
                'merchants': contains(
                    has_entries({
                        'moderation': {
                            'updated': None,
                            'status': ModerationStatus.NONE.value,
                        },
                    }),
                ),
                'total': 1,
                'found': 1,
            }),
            id='no moderation',
        ),
    ))
    def test_returned(self, response_data, expected):
        assert_that(response_data, expected)
