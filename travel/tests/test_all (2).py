import pytest
import yarl
from requests.exceptions import HTTPError

from travel.rasp.bus.library.trust_client import Client


TRUST_PAYMENTS_API_URL = yarl.URL('https://trust')


@pytest.fixture
def trust_client():
    return Client(TRUST_PAYMENTS_API_URL, "service_token_123")


class TestOrderPurchase:
    def test_get_payment_data_success(self, responses, trust_client):
        purchase_token = "purchase_token_123"
        payment_data = {"status": "success", "foo": "bar"}
        responses.add(
            responses.GET, str(TRUST_PAYMENTS_API_URL / "payments" / purchase_token), json=payment_data
        )

        assert trust_client.get_payment_data(purchase_token) == payment_data
        assert len(responses.calls) == 1
        assert responses.calls[0].request.headers["X-Service-Token"] == "service_token_123"

    def test_get_payment_data_error(self, responses, trust_client):
        purchase_token = "purchase_token_123"
        payment_data = {"status": "error", "foo": "bar"}
        responses.add(
            responses.GET, str(TRUST_PAYMENTS_API_URL / "payments" / purchase_token), json=payment_data
        )

        with pytest.raises(AssertionError):
            trust_client.get_payment_data(purchase_token)
        assert len(responses.calls) == 1

    def test_get_payment_data_http_error(self, responses, trust_client):
        purchase_token = "purchase_token_123"
        responses.add(responses.GET, str(TRUST_PAYMENTS_API_URL / "payments" / purchase_token), status=500)

        with pytest.raises(HTTPError):
            trust_client.get_payment_data(purchase_token)
        assert len(responses.calls) == 1

    def test_clear_payment_success(self, responses, trust_client):
        purchase_token = "purchase_token_123"
        clear_data = {"status": "success"}
        responses.add(
            responses.POST,
            str(TRUST_PAYMENTS_API_URL / "payments" / purchase_token / "clear"),
            json=clear_data,
        )

        trust_client.clear_payment(purchase_token)
        assert len(responses.calls) == 1
        request = responses.calls[0].request
        assert request.headers["X-Service-Token"] == "service_token_123"
        assert request.body == b"{}"

    def test_clear_payment_error(self, responses, trust_client):
        purchase_token = "purchase_token_123"
        clear_data = {"status": "error"}
        responses.add(
            responses.POST,
            str(TRUST_PAYMENTS_API_URL / "payments" / purchase_token / "clear"),
            json=clear_data,
        )

        with pytest.raises(AssertionError):
            trust_client.clear_payment(purchase_token)
        assert len(responses.calls) == 1

    def test_clear_payment_http_error(self, responses, trust_client):
        purchase_token = "purchase_token_123"
        responses.add(
            responses.POST, str(TRUST_PAYMENTS_API_URL / "payments" / purchase_token / "clear"), status=500
        )

        with pytest.raises(HTTPError):
            trust_client.clear_payment(purchase_token)
        assert len(responses.calls) == 1
