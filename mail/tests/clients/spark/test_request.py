import pytest

from sendr_interactions.clients.spark.request import SparkRequest


class TestSparkRequest:
    @pytest.fixture
    def make_request_string(self):
        def _inner(content):
            return (
                b'<?xml version=\'1.0\' encoding=\'utf-8\'?>\n'
                b'<soap:Envelope'
                b' xmlns:ifax="http://interfax.ru/ifax"'
                b' xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">'
                b'<soap:Body>'
            ) + content + (
                b'</soap:Body>'
                b'</soap:Envelope>'
            )
        return _inner

    @pytest.fixture
    def auth_request(self, make_request_string):
        return make_request_string(
            b'<ifax:Authmethod>'
            b'<ifax:Login>login</ifax:Login>'
            b'<ifax:Password>password</ifax:Password>'
            b'</ifax:Authmethod>'
        )

    @pytest.fixture
    def end_request(self, make_request_string):
        return make_request_string(b'<ifax:End />')

    @pytest.fixture
    def entrepreneur_request(self, make_request_string):
        return make_request_string(
            b'<ifax:GetEntrepreneurShortReport>'
            b'<ifax:inn>1234567890</ifax:inn>'
            b'</ifax:GetEntrepreneurShortReport>'
        )

    @pytest.fixture
    def company_inn_request(self, make_request_string):
        return make_request_string(
            b'<ifax:GetCompanyShortReport>'
            b'<ifax:inn>1234567890</ifax:inn>'
            b'</ifax:GetCompanyShortReport>'
        )

    @pytest.fixture
    def company_inn_spark_id_request(self, make_request_string):
        return make_request_string(
            b'<ifax:GetCompanyShortReport>'
            b'<ifax:inn>1234567890</ifax:inn>'
            b'<ifax:sparkId>1111</ifax:sparkId>'
            b'</ifax:GetCompanyShortReport>'
        )

    def test_auth(self, auth_request):
        actual_request = SparkRequest.auth(login='login', password='password')
        assert actual_request == auth_request

    def test_end(self, end_request):
        actual_request = SparkRequest.end()
        assert actual_request == end_request

    def test_get_entrepreneur_request(self, entrepreneur_request):
        actual_request = SparkRequest.get_entrepreneur(inn='1234567890')
        assert actual_request == entrepreneur_request

    def test_get_company_inn_request(self, company_inn_request):
        actual_request = SparkRequest.get_company(inn='1234567890')
        assert actual_request == company_inn_request

    def test_get_company_inn_spark_id_zero_request(self, company_inn_request):
        actual_request = SparkRequest.get_company(inn='1234567890', spark_id=0)
        assert actual_request == company_inn_request

    def test_get_company_inn_spark_id_request(self, company_inn_spark_id_request):
        actual_request = SparkRequest.get_company(inn='1234567890', spark_id=1111)
        assert actual_request == company_inn_spark_id_request
