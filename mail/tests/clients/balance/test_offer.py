import re
from xmlrpc import client as xmlrpc

import pytest

from sendr_interactions.clients.balance.entities import CollateralType, OfferConfirmationType, PaymentType
from sendr_pytest.matchers import equal_to

from hamcrest import assert_that, has_entries, is_, match_equality


class TestCreateOffer:
    @pytest.fixture
    def client_id(self):
        return 'test-create-offer-client_id'

    @pytest.fixture
    def person_id(self):
        return 'test-create-offer-person_id'

    @pytest.fixture
    def contract_id(self):
        return 999999999

    @pytest.fixture
    def external_id(self):
        return 'test-create-offer-external_id'

    @pytest.fixture(params=[
        None,
        {'client_id': 'override-client-id', 'person_id': 'override-client-id'},
        {'rand-1': 'rands-2'},
    ])
    def extra_params(self, request):
        return request.param

    @pytest.fixture
    async def returned(self, balance_client, extra_params, client_id, person_id):
        return await balance_client.create_offer(
            uid=111111,
            client_id=client_id,
            person_id=person_id,
            services=[654, 321],
            partner_commission_percentage=1.23,
            currency='XTS',
            firm_id=111,
            manager_code=777777,
            offer_confirmation_type=OfferConfirmationType.NO,
            partner_credit=1,
            payment_term_days=180,
            payment_type=PaymentType.POSTPAYMENT,
            integration_cc='IntegrationCC',
            extra_params=extra_params,
        )

    def test_returned(self, contract_id, returned):
        assert returned[0] == contract_id

    @pytest.fixture(autouse=True)
    def balance_mock(self, aioresponses_mocker, balance_client, create_offer_response):
        return aioresponses_mocker.post(re.compile(rf'{balance_client.BASE_URL}'), body=create_offer_response)


class TestCreateCollateral:
    @pytest.fixture
    def collateral_type(self):
        return CollateralType.GENERAL_TERMINATION

    @pytest.fixture
    def main_contract_id(self):
        return 7777777

    @pytest.fixture
    def contract_id(self):
        return 88888888

    @pytest.fixture
    async def returned(self, balance_client, main_contract_id, collateral_type):
        return await balance_client.create_collateral(
            uid=333, contract_id=main_contract_id, collateral_type=collateral_type
        )

    def test_request_call(self, balance_client, collateral_type, main_contract_id, returned, balance_mock):
        assert_that(
            xmlrpc.loads(balance_mock.call_args.kwargs['data']),
            equal_to(
                (
                    (
                        '333',
                        main_contract_id,
                        collateral_type.value,
                        match_equality(has_entries({'END_DT': is_(xmlrpc.DateTime)})),
                    ),
                    'CreateCollateral',
                )
            )
        )

    def test_returned(self, contract_id, returned):
        assert returned == contract_id

    @pytest.fixture(autouse=True)
    def balance_mock(self, aioresponses_mocker, balance_client, create_collateral_response):
        return aioresponses_mocker.post(re.compile(rf'{balance_client.BASE_URL}'), body=create_collateral_response)
