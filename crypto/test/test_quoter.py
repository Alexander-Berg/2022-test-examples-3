import grpc
from google.protobuf import json_format

ENV_TYPE_QA_CRYPTA = "qa_crypta"
ENV_TYPE_QA_BIGB = "qa_bigb"


def test_quoter(quoter_client):
    return {
        ENV_TYPE_QA_CRYPTA: json_format.MessageToDict(quoter_client.get_quota_state(ENV_TYPE_QA_CRYPTA)),
        ENV_TYPE_QA_BIGB: json_format.MessageToDict(quoter_client.get_quota_state(ENV_TYPE_QA_BIGB)),
    }


def test_quoter_404(quoter_client):
    code = None
    try:
        quoter_client.get_quota_state("FAKE_ENV_TYPE")
    except grpc.RpcError as e:
        code = e.code()

    assert grpc.StatusCode.NOT_FOUND == code, "RPC error {} is expected".format(grpc.StatusCode.NOT_FOUND)


def test_ping(quoter_client):
    response = quoter_client.ping()
    assert "OK" == response.Message
