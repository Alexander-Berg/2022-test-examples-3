import requests

from crypta.cm.services.common import quoter_clients


ENV_TYPE_QA_CRYPTA = "qa_crypta"
ENV_TYPE_QA_BIGB = "qa_bigb"


def test_quoter_http_proxy(quoter_http_client):
    return {
        ENV_TYPE_QA_CRYPTA: quoter_http_client.get_quota_state(ENV_TYPE_QA_CRYPTA),
        ENV_TYPE_QA_BIGB: quoter_http_client.get_quota_state(ENV_TYPE_QA_BIGB),
    }


def test_quoter_http_proxy_404(quoter_http_client):
    code = None
    try:
        state = quoter_http_client.get_quota_state("FAKE_ENV_TYPE")
    except quoter_clients.QuoterHttpClient.Exception as e:
        code = e.status_code

    assert requests.codes.not_found == code, "Code 404 is expected state = {}".format(state)


def test_ping(quoter_http_client):
    response = quoter_http_client.ping()
    assert "OK" == response["Message"]
