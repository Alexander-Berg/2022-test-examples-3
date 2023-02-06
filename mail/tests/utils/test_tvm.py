import pytest
import responses
from rest_framework import status

from fan.utils.tvm import TvmSession, TvmTicketProducer

pytestmark = pytest.mark.django_db


@pytest.fixture
def dst_tvm_id():
    return 102030


@pytest.fixture
def fake_server():
    with responses.RequestsMock() as mock:
        mock.add_callback(
            responses.GET,
            "http://fake.com",
            callback=lambda request: (
                200,
                request.headers,
                "",
            ),
        )
        yield mock


class TestTvmSession:
    def test_session_passes_tvm_header(self, mock_tvm, dst_tvm_id, fake_server):
        resp = TvmSession(dst_tvm_id).get("http://fake.com")
        assert resp.status_code == status.HTTP_200_OK
        assert resp.headers["X-Ya-Service-Ticket"] == "TEST_TVM_TICKET"


class TestTvmTicketProducer:
    def test_producer_returnes_ticket(self, mock_tvm, dst_tvm_id):
        ticket = TvmTicketProducer(dst_tvm_id).get_ticket()
        assert ticket == "TEST_TVM_TICKET"
