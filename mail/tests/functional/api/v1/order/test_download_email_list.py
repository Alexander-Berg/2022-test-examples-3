import pytest

from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.tests.base import BaseTestMerchantRoles
from mail.payments.payments.utils.helpers import create_csv_writer

from .base import BaseTestOrder


@pytest.mark.usefixtures('moderation')
class TestDownloadEmailList(BaseTestMerchantRoles, BaseTestOrder):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )

    @pytest.fixture
    def header(self):
        return ["Номер строки", "Email", "Оформлен возврат"]

    @pytest.fixture
    def expected_csvfile(self, header):
        writer, output = create_csv_writer()
        writer.writerow(header)
        return output

    @pytest.fixture
    async def response(self, client, multi_order, tvm):
        return await client.get(f'/v1/order/{multi_order.uid}/multi/{multi_order.order_id}/download')

    @pytest.fixture
    async def response_data(self, response):
        return await response.read()

    @pytest.mark.asyncio
    async def test_returns_joined_chunks(self, expected_csvfile, response_data):
        assert response_data.decode('utf-8') == expected_csvfile.getvalue()
