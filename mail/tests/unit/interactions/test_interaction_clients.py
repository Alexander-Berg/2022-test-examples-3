import pytest

from mail.payments.payments.core.entities.enums import ShopType
from mail.payments.payments.interactions import InteractionClients, TrustProductionClient, TrustSandboxClient


@pytest.fixture
def clients():
    return InteractionClients()


@pytest.mark.parametrize('trust_sandbox_payments', (True, False))
@pytest.mark.parametrize('shop_type', list(ShopType))
def test_get_trust_client_global(clients, randn, payments_settings, trust_sandbox_payments, shop_type):
    payments_settings.TRUST_SANDBOX_PAYMENTS = trust_sandbox_payments
    expected = (
        TrustSandboxClient
        if shop_type == ShopType.TEST and trust_sandbox_payments
        else TrustProductionClient
    )
    assert isinstance(clients.get_trust_client(randn(), shop_type), expected)


@pytest.mark.parametrize('trust_sandbox_payments', (True, False))
@pytest.mark.parametrize('shop_type', list(ShopType))
def test_get_trust_client_custom(clients, uid, payments_settings, trust_sandbox_payments, shop_type):
    payments_settings.TRUST_SANDBOX_PAYMENTS = False
    payments_settings.INTERACTION_MERCHANT_SETTINGS[uid] = {'trust_sandbox_payments': trust_sandbox_payments}
    expected = (
        TrustSandboxClient
        if shop_type == ShopType.TEST and trust_sandbox_payments
        else TrustProductionClient
    )
    assert isinstance(clients.get_trust_client(uid, shop_type), expected)
