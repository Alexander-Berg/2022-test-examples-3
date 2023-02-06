import re

import pytest

from mail.payments.payments.interactions.refs.exceptions import RefsClientNotFoundError


class TestCbrfBankMethod:
    @pytest.fixture
    def bic(self):
        return '046577674'

    @pytest.fixture
    def name_full(self):
        return 'УРАЛЬСКИЙ БАНК ПАО СБЕРБАНК'

    @pytest.fixture
    def corr(self):
        return '30101810500000000674'

    @pytest.fixture
    def mock_success(self, aioresponses_mocker, bic, name_full, corr, payments_settings):
        return aioresponses_mocker.post(
            re.compile(f'^{payments_settings.REFS_API_URL}/cbrf/.*'),
            payload={
                'data': {
                    'banks': [
                        {
                            'bic': bic,
                            'nameFull': name_full,
                            'corr': corr
                        },
                        {
                            'bic': '044525974',
                            'name_full': 'АО "ТИНЬКОФФ БАНК"',
                            'corr': '30101810145250000974'
                        }
                    ]
                }
            }
        )

    @pytest.fixture
    def mock_empty_fields(self, aioresponses_mocker, bic, payments_settings):
        return aioresponses_mocker.post(
            re.compile(f'^{payments_settings.REFS_API_URL}/cbrf/.*'),
            payload={
                'data': {
                    'banks': [{
                        'bic': bic
                    }]
                }
            }
        )

    @pytest.fixture
    def mock_no_banks(self, aioresponses_mocker, bic, payments_settings):
        return aioresponses_mocker.post(
            re.compile(f'^{payments_settings.REFS_API_URL}/cbrf/.*'),
            payload={
                'data': {
                    'banks': []
                }
            }
        )

    @pytest.fixture
    async def returned_success(self, refs_client, bic, mock_success):
        return await refs_client.cbrf_bank(bic)

    @pytest.fixture
    async def returned_empty(self, refs_client, bic, mock_empty_fields):
        return await refs_client.cbrf_bank(bic)

    @pytest.mark.asyncio
    async def test_bic(self, bic, returned_success):
        assert bic == returned_success.bic

    @pytest.mark.asyncio
    async def test_name_full_success(self, name_full, returned_success):
        assert name_full == returned_success.name_full

    @pytest.mark.asyncio
    async def test_corr_success(self, corr, returned_success):
        assert corr == returned_success.corr

    @pytest.mark.asyncio
    async def test_name_full_empty(self, returned_empty):
        assert '' == returned_empty.name_full

    @pytest.mark.asyncio
    async def test_corr_empty(self, corr, returned_empty):
        assert '' == returned_empty.corr

    @pytest.mark.asyncio
    async def test_banks_not_found(self, refs_client, bic, mock_no_banks):
        with pytest.raises(RefsClientNotFoundError):
            await refs_client.cbrf_bank(bic)

    @pytest.mark.asyncio
    async def test_request(self, mocker, refs_client, bic, mock_success):
        mocker.spy(refs_client, 'post')
        await refs_client.cbrf_bank(bic)

        refs_client.post.assert_called_with(
            'refs_cbrf_bank',
            url=f'{refs_client.BASE_URL}/cbrf/',
            json={'query': f'{{banks(bic:["{bic}"]) {{bic nameFull corr}}}}'}
        )

    @pytest.mark.asyncio
    async def test_empty_bic(self, refs_client):
        with pytest.raises(Exception):
            await refs_client.cbrf_bank("")
