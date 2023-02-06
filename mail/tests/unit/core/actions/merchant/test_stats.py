from decimal import Decimal

import pytest

from mail.payments.payments.core.actions.merchant.stats import MerchantOrdersStatsAction, MerchantsOrdersStatsAction
from mail.payments.payments.core.entities.enums import AcquirerType, OrderKind
from mail.payments.payments.core.entities.merchant import MerchantStat
from mail.payments.payments.tests.utils import dummy_async_generator, dummy_coro


class TestMerchantsOrdersStatsAction:
    @pytest.fixture
    def params(self, randdate, service, randitem):
        return {
            'acquirer': randitem(AcquirerType),
            'closed_from': randdate(),
            'closed_to': randdate(),
            'service_id': service.service_id
        }

    @pytest.fixture
    def merchant_stats(self, randn, rands, randitem, randdecimal):
        return [
            MerchantStat(parent_uid=randn(), name=rands(), orders_sum=randdecimal(), orders_kind=randitem(OrderKind))
            for _ in range(randn(min=1, max=10))
        ]

    @pytest.fixture(autouse=True)
    def merchant_mapper_mock(self, mocker, merchant_stats):
        return mocker.patch(
            'mail.payments.payments.storage.mappers.merchant.merchant.MerchantMapper.batch_orders_stats',
            mocker.Mock(return_value=dummy_async_generator(merchant_stats)())
        )

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await MerchantsOrdersStatsAction(**params).run()

        return _inner

    def test_returned(self, merchant_stats, returned):
        assert returned == merchant_stats

    def test_mapper_call(self, merchant_mapper_mock, params, returned, payments_settings):
        merchant_mapper_mock.assert_called_once_with(
            default_commission=Decimal(payments_settings.PAYMENTS_COMMISSION),
            **params,
        )


class TestMerchantOrdersStatsAction:
    @pytest.fixture
    def params(self, randdate, merchant):
        return {
            'uid': merchant.uid,
            'date_from': randdate(),
            'date_to': randdate()
        }

    @pytest.fixture
    def merchant_stats(self, randn, rands, randitem, randdecimal):
        return MerchantStat(orders_sum=randdecimal(), orders_paid_count=randn(), orders_created_count=randn())

    @pytest.fixture(autouse=True)
    def merchant_mapper_mock(self, mocker, merchant_stats):
        return mocker.patch(
            'mail.payments.payments.storage.mappers.merchant.merchant.MerchantMapper.orders_stats',
            mocker.Mock(return_value=dummy_coro(merchant_stats))
        )

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await MerchantOrdersStatsAction(**params).run()

        return _inner

    def test_returned(self, merchant_stats, returned):
        assert returned == merchant_stats

    def test_mapper_call(self, merchant_mapper_mock, params, returned, payments_settings):
        merchant_mapper_mock.assert_called_once_with(
            default_commission=Decimal(payments_settings.PAYMENTS_COMMISSION),
            **params,
        )
