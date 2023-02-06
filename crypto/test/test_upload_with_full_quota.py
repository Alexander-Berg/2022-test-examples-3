import pytest

from crypta.cm.services.common.test_utils import (
    crypta_cm_service,
    helpers,
    id_utils,
    mock_quoter_servers,
)


@pytest.fixture(scope="module")
def mock_quoter_server():
    with mock_quoter_servers.MockQuoterServer(is_full=True) as mock:
        yield mock


@pytest.fixture(scope="module")
@crypta_cm_service.create()
def cm_client_with_full_quota():
    return


def test_upload_with_no_quota(cm_client_with_full_quota, add_prefix_func):
    ids = id_utils.create_ids_for_test(add_prefix_func)
    matched_ids = ids.matched_ids_by_type.values()

    helpers.check_upload_failed_due_to_quota(cm_client_with_full_quota, ids.ext_id, matched_ids)
