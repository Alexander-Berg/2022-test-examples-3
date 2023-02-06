import pytest

from mail.payments.payments.core.entities.enums import (
    NDS, PAY_METHOD_OFFLINE, PAY_METHOD_YANDEX, PAY_METHODS, ArbitrageStatus, MerchantRole, OrderTimelineEventType,
    PayStatus, TransactionStatus
)


def test_pay_methods():
    assert PAY_METHODS == frozenset({PAY_METHOD_OFFLINE, PAY_METHOD_YANDEX})


class TestMerchantRole:
    @pytest.mark.parametrize('role1', MerchantRole)
    @pytest.mark.parametrize('role2', MerchantRole)
    def test_subroles_included(self, role1, role2):
        is_subrole = role1 in role2.subroles
        is_nested = all((subrole in role2.subroles for subrole in role1.subroles))
        assert role1 == role2 or is_subrole == is_nested

    @pytest.mark.parametrize('role1', MerchantRole)
    @pytest.mark.parametrize('role2', MerchantRole)
    def test_superrole(self, role1, role2):
        is_subrole = role1 in role2.subroles
        is_superrole = role2 in role1.superroles
        assert is_subrole == is_superrole

    @pytest.mark.parametrize('role', MerchantRole)
    def test_same_role(self, role):
        assert all((
            role not in role.subroles,
            role not in role.superroles,
        ))


class TestPayStatus:
    @pytest.mark.parametrize('pay_status', list(PayStatus))
    def test_is_already_paid(self, pay_status):
        assert PayStatus.is_already_paid(pay_status) == (
            pay_status in (PayStatus.PAID, PayStatus.IN_PROGRESS, PayStatus.IN_MODERATION)
        )


class TestArbitrageStatus:
    def test_arbitrage_status(self):
        assert ArbitrageStatus.ACTIVE_STATUSES == {ArbitrageStatus.CONSULTATION, ArbitrageStatus.ESCALATE}


class TestNDS:
    def test_to_arbitrage(self):
        mapping = {
            NDS.NDS_NONE: 'VAT_0',
            NDS.NDS_0: 'VAT_0',
            NDS.NDS_10: 'VAT_10',
            NDS.NDS_10_110: 'VAT_10',
            NDS.NDS_18: 'VAT_20',
            NDS.NDS_18_118: 'VAT_20',
            NDS.NDS_20: 'VAT_20',
            NDS.NDS_20_120: 'VAT_20',
        }

        for nds in list(NDS):
            assert NDS.to_arbitrage(nds) == mapping[nds]


class TestOrderTimelineEventType:
    @pytest.mark.parametrize('transaction_status, expected', [
        (TransactionStatus.ACTIVE, OrderTimelineEventType.PERIODIC_ACTIVE),
        (TransactionStatus.FAILED, OrderTimelineEventType.PERIODIC_FAILED),
        (TransactionStatus.CANCELLED, OrderTimelineEventType.PERIODIC_CANCELLED),
        (TransactionStatus.HELD, OrderTimelineEventType.PERIODIC_HELD),
        (TransactionStatus.CLEARED, OrderTimelineEventType.PERIODIC_CLEARED),
    ])
    def test_from_transaction_status(self, transaction_status, expected):
        assert OrderTimelineEventType.from_transaction_status(transaction_status) == expected
