from datetime import datetime
from decimal import Decimal

import pytest

from hamcrest import assert_that, contains, contains_inanyorder, equal_to, has_properties, match_equality

from mail.payments.payments.core.actions.manager.analytics import GetMerchantAnalyticsManagerAction
from mail.payments.payments.core.actions.merchant.get import GetMerchantAction
from mail.payments.payments.core.entities.enums import AcquirerType, MerchantStatus, ModerationStatus
from mail.payments.payments.core.entities.keyset import KeysetEntry, ManagerAnalyticsKeysetEntity
from mail.payments.payments.core.entities.merchant import MerchantAnalyticsStats
from mail.payments.payments.core.exceptions import KeysetInvalidError, SortByInvalidError
from mail.payments.payments.storage.mappers.merchant import MerchantMapper


@pytest.fixture
def params():
    return {}


@pytest.fixture
async def action(params, manager_actor):
    action = GetMerchantAnalyticsManagerAction(
        manager_uid=manager_actor.uid,
        **params,
    )
    yield action


@pytest.fixture
def manager_type():
    return 'admin'


@pytest.fixture
def manager_actor(managers, manager_type):
    return managers[manager_type]


class TestGetMerchantWithAnalytics:
    @pytest.fixture
    def stats(self):
        return MerchantAnalyticsStats(
            payments_success=2,
            payments_refund=3,
            payments_total=5,
            money_success=Decimal('10.0'),
            money_refund=Decimal('5.0'),
        )

    @pytest.fixture(autouse=True)
    def mock_get_merchant(self, mock_action, merchant):
        return mock_action(GetMerchantAction, merchant)

    @pytest.fixture(autouse=True)
    async def returned(self, action, merchant, stats):
        async with action.storage_setter():
            return await action._get_merchant_with_analytics(
                merchant=merchant,
                stats=stats,
            )

    def test_returned(self, returned, merchant):
        assert_that(
            returned,
            has_properties({
                'uid': merchant.uid,
                'payments_success': 2,
            })
        )

    def test_calls_get_merchant(self, mock_get_merchant, merchant):
        mock_get_merchant.assert_called_once_with(
            merchant=match_equality(
                has_properties({'uid': merchant.uid})
            )
        )

    class TestMoneyFields:
        @pytest.mark.parametrize('manager_type', ('admin', 'accountant'))
        def test_money_fields__accountant_has_money_fields(self, returned):
            assert_that(
                returned,
                has_properties({
                    'money_success': Decimal('10.0'),
                    'money_refund': Decimal('5.0'),
                })
            )

        @pytest.mark.parametrize('manager_type', ('assessor',))
        def test_money_fields__others_has_no_money_fields(self, returned):
            assert_that(
                returned,
                has_properties({
                    'money_success': None,
                    'money_refund': None,
                })
            )


class TestGetKeyset:
    @pytest.fixture
    def params(self, keyset, sort_by):
        return {
            'keyset': keyset,
            'sort_by': sort_by,
        }

    @pytest.fixture
    def returned(self, action):
        return action._get_keyset_filter()

    @pytest.mark.parametrize('sort_by, keyset, expected', (
        ('uid', None, None),
        (
            'uid',
            ManagerAnalyticsKeysetEntity(
                uid=KeysetEntry(barrier=123, order='asc'),
                sort_order=['uid'],
            ),
            [('uid', 'asc', 123)],
        ),
        (
            'created',
            ManagerAnalyticsKeysetEntity(
                uid=KeysetEntry(barrier=123, order='desc'),
                created=KeysetEntry(barrier=datetime(2000, 1, 1), order='desc'),
                sort_order=['created', 'uid'],
            ),
            [('created', 'desc', datetime(2000, 1, 1)), ('uid', 'desc', 123)],
        ),
    ))
    def test_returned(self, returned, expected):
        assert_that(returned, equal_to(expected))

    @pytest.mark.parametrize('sort_by, keyset', (
        pytest.param(
            'updated',
            ManagerAnalyticsKeysetEntity(
                created=KeysetEntry(barrier='2020', order='desc'),
                sort_order=['created'],
            ),
            id='invalid-column',
        ),
    ))
    def test_get_keyset_error(self, action):
        with pytest.raises(KeysetInvalidError):
            action._get_keyset_filter()


class TestMakeNextPageKeyset:
    @pytest.mark.parametrize('keyset, expected', (
        (
            (('uid', 'asc', 123),),
            ManagerAnalyticsKeysetEntity(
                uid=KeysetEntry(barrier=123, order='asc'),
                sort_order=['uid'],
            ),
        ),
        (
            (('created', 'desc', '2020'), ('uid', 'asc', 123),),
            ManagerAnalyticsKeysetEntity(
                uid=KeysetEntry(barrier=123, order='asc'),
                created=KeysetEntry(barrier='2020', order='desc'),
                sort_order=['created', 'uid'],
            ),
        ),
    ))
    def test_returned(self, action, keyset, expected):
        assert_that(
            action._make_next_page_keyset(keyset),
            equal_to(expected),
        )


class TestGetMerchants:
    @pytest.fixture
    def stats(self, mocker):
        return mocker.Mock(payments_success=5)

    @pytest.fixture
    def keyset(self, mocker):
        return None

    @pytest.fixture
    def new_keyset(self, mocker):
        return mocker.Mock()

    @pytest.fixture
    def found(self):
        return 4

    @pytest.fixture
    def total(self):
        return 8

    @pytest.fixture(autouse=True)
    def mock_get_keyset_filter(self, mocker, keyset):
        return mocker.patch.object(GetMerchantAnalyticsManagerAction,
                                   '_get_keyset_filter',
                                   mocker.Mock(return_value=keyset))

    @pytest.fixture(autouse=True)
    def mock_get_analytics(self, mocker, coromock, merchant, stats, new_keyset):
        mock = coromock(([(merchant, stats)], new_keyset))
        return mocker.patch.object(MerchantMapper, 'get_analytics', mock)

    @pytest.fixture(autouse=True)
    def mock_get_merchant_with_analytics(self, mocker, coromock):
        return mocker.patch.object(GetMerchantAnalyticsManagerAction,
                                   '_get_merchant_with_analytics',
                                   coromock(mocker.Mock()))

    @pytest.fixture(autouse=True)
    def mock_get_analytics_found(self, mocker, coromock, merchant, stats, keyset, found, total):
        side_effect = iter([found, total])

        async def coro(*args, **kwargs):
            return next(side_effect)

        return mocker.patch.object(MerchantMapper, 'get_analytics_found', mocker.Mock(side_effect=coro))

    @pytest.fixture(autouse=True)
    async def returned(self, action, merchant, stats):
        async with action.storage_setter():
            return await action._get_merchants()

    @pytest.mark.asyncio
    async def test_returned(self, returned, total, found, new_keyset, mock_get_merchant_with_analytics):
        assert_that(
            returned,
            contains(
                [await mock_get_merchant_with_analytics()],
                has_properties(
                    found=found,
                    total=total,
                ),
                new_keyset,
            )
        )

    def test_calls_get_merchant_with_analytics(self, returned, mock_get_merchant_with_analytics, merchant, stats):
        mock_get_merchant_with_analytics.assert_called_once_with(merchant, stats)

    @pytest.mark.parametrize('merchant_uid, params', (
        (
            112233,
            {
                'merchant_uid': 112233,
                'name': 'test-find-call-name',
                'limit': 5,
                'sort_by': 'updated',
                'desc': False,
                'acquirer': AcquirerType.TINKOFF,
                'blocked': True,
                'site_url': 'siteurlurlurl',
                'moderation_status': ModerationStatus.ONGOING,
                'created_from': datetime.now(),
                'created_to': datetime.now(),
                'pay_created_from': datetime.now(),
                'pay_created_to': datetime.now(),
                'pay_closed_from': datetime.now(),
                'pay_closed_to': datetime.now(),
                'service_ids': (1, 2, 3),
            },
        ),
    ))
    class TestMapperCalls:
        def test_mapper_calls__get_analytics_call(self, mocker, params, mock_get_analytics, returned):
            mock_get_analytics.assert_called_once_with(**{
                'uid': params['merchant_uid'],
                'name': params['name'],
                'blocked': params['blocked'],
                'site_url': params['site_url'],
                'limit': params['limit'],
                'sort_by': params['sort_by'],
                'descending': params['desc'],
                'acquirer': params['acquirer'],
                'moderation_status': params['moderation_status'],
                'created_from': params['created_from'],
                'created_to': params['created_to'],
                'pay_created_from': params['pay_created_from'],
                'pay_created_to': params['pay_created_to'],
                'pay_closed_from': params['pay_closed_from'],
                'pay_closed_to': params['pay_closed_to'],
                'service_ids': params['service_ids'],
                'statuses': match_equality(
                    contains_inanyorder(
                        MerchantStatus.ACTIVE, MerchantStatus.INACTIVE, MerchantStatus.NEW
                    )
                ),
                'keyset': None,
            })

        def test_mapper_calls__get_found_calls(self, mocker, params, mock_get_analytics_found, returned):
            mock_get_analytics_found.assert_has_calls(
                (
                    mocker.call(**{
                        'uid': params['merchant_uid'],
                        'name': params['name'],
                        'blocked': params['blocked'],
                        'site_url': params['site_url'],
                        'acquirer': params['acquirer'],
                        'moderation_status': params['moderation_status'],
                        'created_from': params['created_from'],
                        'created_to': params['created_to'],
                        'pay_created_from': params['pay_created_from'],
                        'pay_created_to': params['pay_created_to'],
                        'pay_closed_from': params['pay_closed_from'],
                        'pay_closed_to': params['pay_closed_to'],
                        'service_ids': params['service_ids'],
                        'statuses': match_equality(
                            contains_inanyorder(
                                MerchantStatus.ACTIVE, MerchantStatus.INACTIVE, MerchantStatus.NEW
                            )
                        ),
                    }),
                    mocker.call(
                        statuses=match_equality(
                            contains_inanyorder(
                                MerchantStatus.ACTIVE, MerchantStatus.INACTIVE, MerchantStatus.NEW
                            )
                        ),
                    ),
                ),
                any_order=True,
            )

        class TestWhenKeysetIsNotNull:
            @pytest.fixture
            def keyset(self, mocker):
                return mocker.Mock()

            def test_when_keyset_is_not_null__get_analytics_call(self,
                                                                 mocker,
                                                                 params,
                                                                 mock_get_analytics,
                                                                 keyset,
                                                                 returned
                                                                 ):
                mock_get_analytics.assert_called_once_with(**{
                    'uid': params['merchant_uid'],
                    'name': params['name'],
                    'blocked': params['blocked'],
                    'site_url': params['site_url'],
                    'limit': params['limit'],
                    'sort_by': None,
                    'descending': params['desc'],
                    'acquirer': params['acquirer'],
                    'moderation_status': params['moderation_status'],
                    'created_from': params['created_from'],
                    'created_to': params['created_to'],
                    'pay_created_from': params['pay_created_from'],
                    'pay_created_to': params['pay_created_to'],
                    'pay_closed_from': params['pay_closed_from'],
                    'pay_closed_to': params['pay_closed_to'],
                    'service_ids': params['service_ids'],
                    'statuses': match_equality(
                        contains_inanyorder(
                            MerchantStatus.ACTIVE, MerchantStatus.INACTIVE, MerchantStatus.NEW
                        )
                    ),
                    'keyset': keyset,
                })


class TestGetAnalyticsHandle:
    @pytest.fixture(autouse=True)
    def mock_get_merchants(self, mocker, coromock, merchants, stats, keyset):
        mock = coromock((merchants, stats, keyset))
        return mocker.patch.object(GetMerchantAnalyticsManagerAction, '_get_merchants', mock)

    @pytest.fixture(autouse=True)
    def mock_get_next_page_keyset(self, mocker, returned_keyset):
        mock = mocker.Mock(return_value=returned_keyset)
        return mocker.patch.object(GetMerchantAnalyticsManagerAction, '_make_next_page_keyset', mock)

    @pytest.fixture
    def returned_func(self, params, action, manager_assessor):
        async def _inner():
            return await action.run()
        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.fixture
    def merchants(self, mocker):
        return mocker.Mock()

    @pytest.fixture
    def stats(self, mocker):
        return mocker.Mock()

    @pytest.fixture
    def keyset(self, mocker):
        return mocker.Mock()

    @pytest.fixture
    def returned_keyset(self, mocker):
        return mocker.Mock()

    @pytest.mark.parametrize('params', (
        {'sort_by': 'client_id'},
    ))
    @pytest.mark.asyncio
    async def test_invalid_sort_by(self, returned_func):
        with pytest.raises(SortByInvalidError):
            await returned_func()

    def test_returned(self, returned, merchants, stats, returned_keyset):
        assert_that(
            returned,
            has_properties({
                'merchants': merchants,
                'stats': stats,
                'keyset': returned_keyset,
            })
        )

    def test_calls_get_merchants(self, returned, mock_get_merchants):
        mock_get_merchants.assert_called_once()

    def test_calls_get_next_page_keyset(self, keyset, mock_get_next_page_keyset, returned):
        mock_get_next_page_keyset.assert_called_once_with(keyset)
