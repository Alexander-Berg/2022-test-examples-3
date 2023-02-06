from datetime import datetime

import pytest

from hamcrest import assert_that, contains, has_entries, is_

from mail.payments.payments.core.entities.enums import AcquirerType
from mail.payments.payments.tests.base import BaseAcquirerTest


class TestCreateOffer:
    @pytest.fixture
    def balance_settings(self, payments_settings, acquirer, randn):
        data = [
            ('partner_commission_pct2', 'BALANCE_COMISSION', 'test-create-offer-BALANCE_COMISSION'),
            ('currency', 'BALANCE_CURRENCY', 'test-create-offer-BALANCE_CURRENCY'),
            ('firm_id', 'BALANCE_FIRM_ID', 'test-create-offer-BALANCE_FIRM_ID'),
            ('manager_code', 'BALANCE_MANAGER_CODE', 'test-create-offer-BALANCE_MANAGER_CODE'),
            (
                'offer_confirmation_type',
                'BALANCE_OFFER_CONFIRMATION_TYPE',
                'test-create-offer-BALANCE_OFFER_CONFIRMATION_TYPE',
            ),
            ('partner_credit', 'BALANCE_PARTNER_CREDIT', 'test-create-offer-BALANCE_PARTNER_CREDIT'),
            ('payment_term', 'BALANCE_PAYMENT_TERM', 'test-create-offer-BALANCE_PAYMENT_TERM'),
            ('payment_type', 'BALANCE_PAYMENT_TYPE', 'test-create-offer-BALANCE_PAYMENT_TYPE'),
        ]

        for _, key, value in data:
            setattr(payments_settings, key, value)
        payments_settings.BALANCE_SERVICE_ID = {item.value: randn() for item in list(AcquirerType)}

        result = {key: value for key, _, value in data}
        result['services'] = payments_settings.BALANCE_SERVICE_ID[acquirer.value]

        return result

    @pytest.fixture
    def client_id(self):
        return 'test-create-offer-client_id'

    @pytest.fixture
    def person_id(self):
        return 'test-create-offer-person_id'

    @pytest.fixture
    def contract_id(self, randn):
        return randn()

    @pytest.fixture
    def external_id(self):
        return 'test-create-offer-external_id'

    @pytest.fixture
    def response_data(self, create_offer_response):
        return create_offer_response

    @pytest.fixture(params=[
        None,
        {'client_id': 'override-client-id', 'person_id': 'override-client-id'},
        {'rand-1': 'rands-2'},
    ])
    def extra_params(self, request):
        return request.param

    @pytest.fixture
    async def returned(self, balance_client, extra_params, merchant_uid, acquirer, client_id, person_id):
        return await balance_client.create_offer(merchant_uid, acquirer, client_id, person_id, extra_params)

    def test_returned(self, contract_id, returned):
        assert returned[0] == contract_id

    class TestRequestCall(BaseAcquirerTest):
        def test_request_call(self,
                              balance_client,
                              merchant_uid,
                              balance_settings,
                              client_id,
                              person_id,
                              returned,
                              extra_params
                              ):
            assert balance_client.call_kwargs == {
                'method_name': 'CreateOffer',
                'data': (
                    str(merchant_uid),
                    {
                        'client_id': client_id,
                        'person_id': person_id,
                        **balance_settings,
                        'services': (balance_settings['services'],),
                        **(extra_params or {})
                    },
                )
            }


class TestCreateCollateral:
    @pytest.fixture
    def collateral_type(self, payments_settings, randn):
        payments_settings.BALANCE_COLLATERAL_TYPE = randn()
        return payments_settings.BALANCE_COLLATERAL_TYPE

    @pytest.fixture
    def main_contract_id(self, randn):
        return randn()

    @pytest.fixture
    def contract_id(self, randn):
        return randn()

    @pytest.fixture
    def response_data(self, create_collateral_response):
        return create_collateral_response

    @pytest.fixture
    async def returned(self, balance_client, merchant_uid, main_contract_id):
        return await balance_client.create_collateral(merchant_uid, main_contract_id)

    def test_request_call(self, balance_client, merchant_uid, collateral_type, main_contract_id, returned):
        assert_that(
            balance_client.call_kwargs,
            has_entries({
                'method_name': 'CreateCollateral',
                'data': contains(
                    str(merchant_uid),
                    main_contract_id,
                    collateral_type,
                    has_entries({'END_DT': is_(datetime)}),
                )
            })
        )

    def test_returned(self, contract_id, returned):
        assert returned == contract_id
