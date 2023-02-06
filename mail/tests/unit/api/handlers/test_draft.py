from copy import deepcopy
from datetime import datetime

import pytest

from hamcrest import assert_that, has_item

from mail.payments.payments.core.entities.enums import PersonType
from mail.payments.payments.tests.utils import MERCHANT_DATA_TEST_CASES


class TestMerchantDraftPost:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.merchant.draft import CreateMerchantDraftAction
        return mock_action(CreateMerchantDraftAction)

    @pytest.fixture(params=MERCHANT_DATA_TEST_CASES)
    def request_json(self, request):
        return deepcopy(request.param)

    @pytest.fixture
    def test_uid(self, unique_rand, randn):
        return unique_rand(randn, basket='uid')

    @pytest.fixture
    async def response(self, payments_client, request_json, test_uid):
        return await payments_client.post(f'/v1/merchant/{test_uid}/draft', json=request_json)

    @pytest.fixture
    def passed_params(self, response, action):
        return action.call_args[1]

    @staticmethod
    def _replace_key(dict_, old_key, new_key):
        if dict_.get(old_key):
            dict_[new_key] = dict_.get(old_key)
            dict_.pop(old_key)

    def test_ok(self, response):
        assert response.status == 200

    def test_params_uid_name(self, passed_params, test_uid, request_json):
        assert all((
            passed_params['uid'] == test_uid,
            passed_params['name'] == request_json['name'])
        )

    def test_params_bank(self, passed_params, request_json):
        if request_json.get('bank') is not None:
            bank_dict = request_json.get('bank')
            self._replace_key(bank_dict, 'correspondentAccount', 'correspondent_account')
            assert passed_params['bank'] == bank_dict

    def test_params_organization(self, passed_params, request_json):
        if request_json.get('organization') is not None:
            org_dict = request_json.get('organization')
            self._replace_key(org_dict, 'englishName', 'english_name')
            self._replace_key(org_dict, 'scheduleText', 'schedule_text')
            self._replace_key(org_dict, 'siteUrl', 'site_url')
            self._replace_key(org_dict, 'fullName', 'full_name')
            passed_params['organization']['type'] = passed_params['organization']['type'].value
            assert passed_params['organization'] == request_json['organization']

    def test_params_addresses(self, passed_params, request_json):
        if request_json.get('addresses') is not None:
            for type_, address in request_json.get('addresses').items():
                address['type'] = type_
                assert_that(passed_params['addresses'], has_item(address))

    def test_params_persons(self, passed_params, request_json):
        if request_json.get('persons') is not None:
            for type_, person in request_json.get('persons').items():
                person['type'] = PersonType(type_)
                self._replace_key(person, 'birthDate', 'birth_date')
                if person.get('birth_date') is not None:
                    person['birth_date'] = datetime.fromisoformat(person['birth_date']).date()
                assert_that(passed_params['persons'], has_item(person))
