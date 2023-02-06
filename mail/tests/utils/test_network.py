import pytest
from rest_framework.test import APIRequestFactory

from fan.utils.network import get_client_ip, is_loopback, is_loopback_request

pytestmark = pytest.mark.django_db


TEST_IP = "192.168.1.50"


LOOPBACK_IP_ADDRESSES = (
    "127.0.0.1",
    "0:0:0:0:0:0:0:1",
)

NON_LOOPBACK_IP_ADDRESSES = (
    "0.0.0.0",
    "192.168.1.11",
    "10.0.0.15",
    "2a02:6b8:fc14:4d93:0:696:43fd:0",
)


class TestGetClientIp:
    def test_x_forwarded_for(self, request_mock):
        expected_ip = TEST_IP
        request_mock.META["HTTP_X_FORWARDED_FOR"] = expected_ip
        assert get_client_ip(request_mock) == expected_ip

    def test_remote_addr(self, request_mock):
        expected_ip = TEST_IP
        request_mock.META["REMOTE_ADDR"] = expected_ip
        assert get_client_ip(request_mock) == expected_ip


class TestIsLoopBack:
    @pytest.mark.parametrize("ip", LOOPBACK_IP_ADDRESSES)
    def test_loopback_address(self, ip):
        assert is_loopback(ip) == True

    @pytest.mark.parametrize("ip", NON_LOOPBACK_IP_ADDRESSES)
    def test_non_loopback_address(self, ip):
        assert is_loopback(ip) == False


class TestIsLoopBackRequest:
    @pytest.mark.parametrize("ip", LOOPBACK_IP_ADDRESSES)
    def test_loopback_address(self, request_mock, ip):
        request_mock.META["REMOTE_ADDR"] = ip
        assert is_loopback_request(request_mock) == True

    @pytest.mark.parametrize("ip", NON_LOOPBACK_IP_ADDRESSES)
    def test_non_loopback_address(self, request_mock, ip):
        request_mock.META["REMOTE_ADDR"] = ip
        assert is_loopback_request(request_mock) == False


@pytest.fixture
def request_mock():
    factory = APIRequestFactory()
    return factory.get("/mocked-request")
