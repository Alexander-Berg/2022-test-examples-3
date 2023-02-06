import pytest

from mail.payments.payments.interactions.spark import SparkClient
from mail.payments.payments.interactions.spark.exceptions import SparkAuthError, SparkGetInfoError, SparkSessionEndError


@pytest.fixture
def spark_client(client_mocker):
    return client_mocker(SparkClient)


class TestSparkClientBase:
    @pytest.fixture
    def responses(self):
        return []

    @pytest.fixture(autouse=True)
    def setup(self, spark_client, response_mock, responses):
        async def _response_func(*args, **kwargs):
            nonlocal spark_client
            nonlocal responses
            return responses[len(spark_client.calls) - 1]
        response_mock.read = _response_func


class TestSparkClientAuthError(TestSparkClientBase):
    @pytest.fixture
    def responses(self, auth_fail_response, entrepreneur_success_response, end_success_response):
        return [auth_fail_response, entrepreneur_success_response, end_success_response]

    @pytest.mark.asyncio
    async def test_spark_auth_error(self, ip_inn, response_data, spark_client):
        with pytest.raises(SparkAuthError):
            await spark_client.get_info(inn=ip_inn)


class TestSparkClientSessionEndError(TestSparkClientBase):
    @pytest.fixture
    def responses(self, auth_success_response, entrepreneur_success_response, end_fail_response):
        return [auth_success_response, entrepreneur_success_response, end_fail_response]

    @pytest.mark.asyncio
    async def test_spark_session_end_error(self, ip_inn, response_data, spark_client):
        with pytest.raises(SparkSessionEndError):
            await spark_client.get_info(inn=ip_inn)


class TestSparkClientGetEntrepreneurFail(TestSparkClientBase):
    @pytest.fixture
    def responses(self, auth_success_response, entrepreneur_fail_response, end_success_response):
        return [auth_success_response, entrepreneur_fail_response, end_success_response]

    @pytest.mark.asyncio
    async def test_get_entrepreneur_fail(self, ip_inn, spark_client):
        with pytest.raises(SparkGetInfoError):
            await spark_client.get_info(inn=ip_inn)


class TestSparkClienGetEntrepreneurSuccess(TestSparkClientBase):
    @pytest.fixture
    def responses(self, auth_success_response, entrepreneur_success_response, end_success_response):
        return [auth_success_response, entrepreneur_success_response, end_success_response]

    @pytest.mark.asyncio
    async def test_get_entrepreneur_success(self, ip_inn, spark_client, entrepreneur_spark_data):
        spark_data = await spark_client.get_info(inn=ip_inn)
        assert spark_data == entrepreneur_spark_data


class TestSparkClientGetCompanyFail(TestSparkClientBase):
    @pytest.fixture
    def responses(self, auth_success_response, company_fail_response, end_success_response):
        return [auth_success_response, company_fail_response, end_success_response]

    @pytest.mark.asyncio
    async def test_get_company_fail(self, ooo_inn, spark_client):
        with pytest.raises(SparkGetInfoError):
            await spark_client.get_info(inn=ooo_inn)


class TestSparkClienGetCompanySuccess(TestSparkClientBase):
    @pytest.fixture
    def responses(self, auth_success_response, company_success_response, end_success_response):
        return [auth_success_response, company_success_response, end_success_response]

    @pytest.mark.asyncio
    @pytest.mark.parametrize('spark_id,expect_body', [
        pytest.param(None, (
            b'<ifax:GetCompanyShortReport>'
            b'<ifax:inn>1234567890</ifax:inn>'
            b'</ifax:GetCompanyShortReport>'), id='inn'),
        pytest.param(1111, (
            b'<ifax:GetCompanyShortReport>'
            b'<ifax:inn>1234567890</ifax:inn>'
            b'<ifax:sparkId>1111</ifax:sparkId>'
            b'</ifax:GetCompanyShortReport>'), id='inn_spark_id')
    ])
    async def test_get_company_success(self, ooo_inn, spark_client, company_spark_data, spark_id, expect_body):
        spark_data = await spark_client.get_info(inn=ooo_inn, spark_id=spark_id)
        assert spark_data == company_spark_data
        assert expect_body in spark_client.calls[1][1]['data']
