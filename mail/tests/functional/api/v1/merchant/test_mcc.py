import pytest
from multidict import MultiDict

from hamcrest import assert_that, contains_inanyorder, has_entries

from mail.payments.payments.core.entities.mcc import Mcc, MccGroup


class TestMccGet:
    @pytest.fixture
    def codes(self):
        return [1234, 2345]

    @pytest.fixture
    def params(self, codes):
        p = MultiDict()
        for code in codes:
            p.add('codes[]', code)
        return p

    @pytest.fixture
    async def response(self, client, params, merchant):
        r = await client.get(f'/v1/merchant/{merchant.uid}/mcc', params=params)
        assert r.status == 200
        return await r.json()

    @pytest.fixture
    def mcc_group(self):
        return MccGroup(group_id=1, group_liter='liter', group_name_ru='group_name_ru', group_name='group_name')

    @pytest.fixture
    def mcc_codes(self, codes, mcc_group):
        mcc_codes_list = []
        for code in codes:
            mcc_code = Mcc(code=code, name_ru='Имя', name='Name', source='Source', clearing_name='Clearing Name',
                           mcc_group=mcc_group)
            mcc_codes_list.append(mcc_code)
        return mcc_codes_list

    @pytest.fixture(autouse=True)
    def trust_payment_get_mock(self, shop_type, trust_client_mocker, mcc_codes):
        with trust_client_mocker(shop_type, 'mcc_get', {'result': mcc_codes}) as mock:
            yield mock

    def test_returned(self, params, mcc_codes, response):
        assert_that(
            response['data'],
            contains_inanyorder(*[
                has_entries({
                    'source': mcc_code.source,
                    'name': mcc_code.name,
                    'name_ru': mcc_code.name_ru,
                    'code': mcc_code.code,
                    'clearing_name': mcc_code.clearing_name,
                    'mcc_group': has_entries({
                        'group_id': mcc_code.mcc_group.group_id,
                        'group_liter': mcc_code.mcc_group.group_liter,
                        'group_name_ru': mcc_code.mcc_group.group_name_ru,
                        'group_name': mcc_code.mcc_group.group_name,
                    })
                })
                for mcc_code in mcc_codes
            ])
        )
