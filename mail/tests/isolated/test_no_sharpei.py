import pytest
from furita_api import FuritaApi
from furita import (
    FuritaRequests,
    get_furita,
    get_default_params
)


def test_no_sharpei(context):
    context.create_user('InvalidUser')
    uid = context.get_uid('InvalidUser')

    response = context.furita_api.api_list(uid)

    assert response.status_code == 500

    json_response = response.json()

    assert "status" in json_response
    assert json_response["status"] == "error"
    assert "report" in json_response
    assert json_response["report"] == "connect error"


# HELPERS


@pytest.fixture(scope="module", autouse=True)
def furita_setup(request, context):

    def furita_teardown():
        if (context.furita):
            context.furita.stop()
            context.furita = None
            context.furita_api = None

    params = get_default_params(context.devpack)
    params["__SHARPEI_LOCAL_PORT__"] = "1"
    context.furita = get_furita(params)
    context.furita_api = FuritaApi(FuritaRequests(context.furita.furita_host))
    request.addfinalizer(furita_teardown)
