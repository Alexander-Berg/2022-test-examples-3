import pytest
from marshmallow import ValidationError

from mail.payments.payments.api.schemas.bank_requisites import BankRequisitesRequestSchema


class TestBankRequisitesRequestSchema:
    @pytest.fixture
    def bic(self):
        return '046577674'

    def test_success(self, bic):
        actual = BankRequisitesRequestSchema().load({'bic': bic})
        assert bic == actual.data['bic']

    def test_bic_required(self):
        with pytest.raises(ValidationError):
            BankRequisitesRequestSchema().load({})

    @pytest.mark.parametrize('bic', (
        pytest.param(' 123456789', id='space'),
        pytest.param('one23456789', id='letters'),
        pytest.param('', id='empty'),
        pytest.param('88888888', id='short'),
        pytest.param('1111111111', id='long'),
    ))
    def test_bic_invalid(self, bic):
        with pytest.raises(ValidationError):
            BankRequisitesRequestSchema().load({'bic': bic})
