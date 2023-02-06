import pytest

from mail.payments.payments.core.actions.merchant.get_mcc import GetMccInfoInTrustAction
from mail.payments.payments.core.entities.mcc import Mcc, MccGroup
from mail.payments.payments.tests.base import BaseAcquirerTest


class TestGetMccInfoInTrustAction(BaseAcquirerTest):
    @pytest.fixture
    def codes(self):
        return [1234, 2345]

    @pytest.fixture
    def params(self, merchant, codes):
        return {
            'uid': merchant.uid,
            'codes': codes,
        }

    @pytest.fixture
    def mcc_group(self):
        return MccGroup(group_id=1, group_liter='liter', group_name_ru='group_name_ru', group_name='group_name')

    @pytest.fixture
    def mcc_codes(self, codes, mcc_group):
        mcc_codes_list = []
        for code in codes:
            mcc_code = Mcc(
                code=code,
                name_ru='Имя',
                name='Name',
                source='Source',
                clearing_name='Clearing Name',
                mcc_group=mcc_group
            )
            mcc_codes_list.append(mcc_code)
        return mcc_codes_list

    @pytest.fixture(autouse=True)
    def trust_mcc_get_mock(self, shop_type, trust_client_mocker, mcc_codes):
        with trust_client_mocker(shop_type, 'mcc_get', mcc_codes) as mock:
            yield mock

    @pytest.fixture
    async def returned(self, params):
        return await GetMccInfoInTrustAction(**params).run()

    def test_returned(self, mcc_codes, returned):
        assert returned == mcc_codes

    def test_mcc_get_call(self, codes, trust_mcc_get_mock, merchant, acquirer, returned):
        trust_mcc_get_mock.assert_called_once_with(codes=codes, uid=merchant.uid, acquirer=acquirer)
