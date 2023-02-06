import pytest

from crypta.cm.services.common.test_utils import (
    crypta_cm_service,
    helpers,
    id_utils,
    mock_quoter_servers,
)


@pytest.fixture(scope="module")
def mock_quoter_server():
    return mock_quoter_servers.MockQuoterServerDisabled()


@pytest.fixture(scope="module")
@crypta_cm_service.create()
def cm_client_without_quoter():
    return


def test_upload_without_quoter(cm_client_without_quoter, add_prefix_func):
    ids = id_utils.create_ids_for_test(add_prefix_func)
    matched_ids = ids.matched_ids_by_type.values()

    helpers.upload_and_identify(cm_client_without_quoter, ids.ext_id, matched_ids)
